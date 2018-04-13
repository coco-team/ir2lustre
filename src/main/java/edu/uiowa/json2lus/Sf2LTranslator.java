/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
 */
package edu.uiowa.json2lus;

import com.fasterxml.jackson.databind.JsonNode;
import edu.uiowa.json2lus.lustreAst.AutomatonIteExpr;
import edu.uiowa.json2lus.lustreAst.AutomatonState;
import edu.uiowa.json2lus.lustreAst.BooleanExpr;
import edu.uiowa.json2lus.lustreAst.LustreAst;
import edu.uiowa.json2lus.lustreAst.LustreAutomaton;
import edu.uiowa.json2lus.lustreAst.LustreExpr;
import edu.uiowa.json2lus.lustreAst.LustreNode;
import edu.uiowa.json2lus.lustreAst.LustreType;
import edu.uiowa.json2lus.lustreAst.LustreVar;
import edu.uiowa.json2lus.lustreAst.VarIdExpr;
import edu.uiowa.json2lus.stateflowparser.StateflowVisitor;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowLexer;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;

/**
 *
 * @author Paul Meng
 */
public class Sf2LTranslator {
    private static final Logger LOGGER = Logger.getLogger(Sf2LTranslator.class.getName());
    
    /** State flow language features */
    private final String ID             = "Id";
    private final String PORT           = "Port";    
    private final String PATH           = "Path";
    private final String DATA           = "Data";
    private final String TYPE           = "Type";
    private final String NAME           = "Name";
    private final String DEST           = "Destination";
    private final String ENTRY          = "Entry";     
    private final String LOCAL          = "Local";
    private final String INPUT          = "Input";     
    private final String SCOPE          = "Scope";
    private final String OUTPUT         = "Output";     
    private final String EVENTS         = "Events";
    private final String STATES         = "States";
    private final String ACTIONS        = "Actions";     
    private final String DATATYPE       = "Datatype";           
    private final String GRAPHFCN       = "GraphicalFunctions";
    private final String JUNCTION       = "Junction"; 
    private final String SUBSTATES      = "Substates";     
    private final String JUNCTIONS      = "Junctions";            
    private final String SFCONTENT      = "StateflowContent";        
    private final String CONDITION      = "Condition";    
    private final String ORIGINPATH     = "Origin_path";
    private final String COMPOSITION    = "Composition"; 
    private final String COMPILEDTYPE   = "CompiledType";       
    private final String DEFAULTTRANS   = "DefaultTransitions";
    private final String OUTTRANSITS    = "OuterTransitions";
    
    /** Data structures for constructing a node */
    String                      initStateId     = null;
    String                      entryStateId    = null;
    List<AutomatonState>        autoStates      = new ArrayList<>();        
    Map<String, JsonNode>       states          = new HashMap<>();        
    Map<String, JsonNode>       junctions       = new HashMap<>();        
    Map<String, JsonNode>       events          = new HashMap<>();
    Map<String, LustreExpr>     junctToExpr     = new HashMap<>();
    Map<String, JsonNode>       inputs          = new HashMap<>();
    Map<Integer, String>        inPortToId      = new HashMap<>();
    Map<Integer, String>        outPortToId     = new HashMap<>();
    Map<String, JsonNode>       outputs         = new HashMap<>(); 
    List<JsonNode>              locals          = new ArrayList<>();     

        
    public LustreAst translate(JsonNode subsystemNode) {       
        String      nodeName        = J2LUtils.sanitizeName(subsystemNode.get(PATH).asText());
        LOGGER.log(Level.INFO, "******************** Start translating stateflow program: {0}", nodeName);
        
        JsonNode    automatonNode   = subsystemNode.get(SFCONTENT);
        Iterator<Map.Entry<String, JsonNode>> chartFields = automatonNode.fields();

        while(chartFields.hasNext()) {
            Map.Entry<String, JsonNode> field = chartFields.next(); 
            
            switch (field.getKey()) {
                case DATA: {
                    addVariables(field.getValue());
                    break;
                }
                case EVENTS: {
                    break;
                }
                case STATES: {
                    collectInfo(field.getValue(), STATES);
                    break;
                }
                case JUNCTIONS: {
                    collectInfo(field.getValue(), JUNCTIONS);
                    break;
                }
                case GRAPHFCN: {
                    break;
                }
                default:
                    break;
            }
        }
        
        // Start translating states
        String automatonName = J2LUtils.sanitizeName(automatonNode.get(PATH).asText());
        
        for(JsonNode stateNode : this.states.values()) {
            String stateId = stateNode.get(ID).asText();
            
            if(stateId.equals(this.entryStateId)) {
                continue;
            }
            
            boolean             isInit      = false;
            String              name        = J2LUtils.sanitizeName(stateNode.get(PATH).asText());
            JsonNode            actionNode  = stateNode.get(ACTIONS);
            String              entry       = actionNode.get(ENTRY).asText();
            List<LustreAst>     entryExprs  = parseAndTranslate(entry);    
            LustreExpr          strongTrans = null;
            
            if(this.initStateId != null && stateId.equals(this.initStateId)) {
                isInit = true;
            }
            
            if(stateNode.has(OUTTRANSITS) && stateNode.get(OUTTRANSITS).has(DEST)) {
                if(stateNode.get(OUTTRANSITS).get(DEST).get(TYPE).asText().equals(JUNCTION)) {
                    String junctionId = stateNode.get(OUTTRANSITS).get(DEST).get(ID).asText();
                    
                    if(this.junctToExpr.containsKey(junctionId)) {
                        strongTrans = this.junctToExpr.get(junctionId);
                    } else {
                        if(this.junctions.containsKey(junctionId)) {
                            JsonNode            junctionNode        = this.junctions.get(stateNode.get(OUTTRANSITS).get(DEST).get(ID).asText());                        
                            List<JsonNode>      outerTransitions    = getOuterTransitions(junctionNode);
                            List<LustreExpr>    conditionExprs      = new ArrayList<>();
                            List<String>        destStates          = new ArrayList<>();

                            for(JsonNode transNode : outerTransitions) {
                                if(transNode.has(DEST)) {
                                    LustreExpr conditionExpr    = new BooleanExpr(true);
                                    String     conditionStr     = transNode.get(DEST).get(CONDITION).asText();

                                    if(conditionStr != null) {
                                        conditionExpr = (LustreExpr)parseAndTranslateStrExpr(conditionStr);
                                    }
                                    conditionExprs.add(conditionExpr);
                                    if(transNode.has(DEST) && transNode.get(DEST).has(DEST)) {
                                        destStates.add(J2LUtils.sanitizeName(transNode.get(DEST).get(DEST).get(NAME).asText()));
                                    }
                                }
                            }

                            if(conditionExprs.size() == destStates.size()) {
                                LustreExpr iteExpr = new VarIdExpr(destStates.get(destStates.size()-1));
                                iteExpr = new AutomatonIteExpr(conditionExprs.get(conditionExprs.size()-1), iteExpr, null, null);

                                for(int i = conditionExprs.size()-2; i >=0; --i) {
                                    iteExpr = new AutomatonIteExpr(conditionExprs.get(i), new VarIdExpr(destStates.get(i)), null, iteExpr);
                                }
                                strongTrans = iteExpr;
                                this.junctToExpr.put(junctionId, strongTrans);
                            } else {
                                LOGGER.log(Level.SEVERE, "SOMETHING is wrong here!");
                            }
                        }                        
                    }
                }
            }
            this.autoStates.add(new AutomatonState(isInit, name, new ArrayList<LustreVar>(), entryExprs, strongTrans));
        }
        
        List<LustreVar> newInputs    = new ArrayList<>();
        List<LustreVar> newOutputs   = new ArrayList<>();
        List<LustreVar> newLocals    = new ArrayList<>();
        
        for(int i = 1; i <= this.inputs.size(); ++i) {
            JsonNode    inputNode   = this.inputs.get(this.inPortToId.get(i));
            String      inputName   = inputNode.get(NAME).asText();
            LustreType  type        = J2LUtils.getLustreTypeFromStrRep(inputNode.get(COMPILEDTYPE).asText());
            newInputs.add(new LustreVar(inputName, type));
        }
        for(int i = 1; i <= this.outputs.size(); ++i) {
            JsonNode    outputNode  = this.outputs.get(this.outPortToId.get(i));
            String      outputName  = outputNode.get(NAME).asText();
            LustreType  type        = J2LUtils.getLustreTypeFromStrRep(outputNode.get(COMPILEDTYPE).asText());
            newOutputs.add(new LustreVar(outputName, type));            
        }     
        for(JsonNode localNode : this.locals) {
            String      localName   = localNode.get(NAME).asText();
            LustreType  type        = J2LUtils.getLustreTypeFromStrRep(localNode.get(COMPILEDTYPE).asText());
            newLocals.add(new LustreVar(localName, type));
        }
        LOGGER.log(Level.INFO, "******************** Done ********************");
        return new LustreNode(nodeName, new LustreAutomaton(automatonName, this.autoStates), newInputs, newOutputs, newLocals);
    }
    
    protected List<JsonNode> getOuterTransitions(JsonNode junctionNode) {
        List<JsonNode> transitionNodes = new ArrayList<>();
        
        if(junctionNode.get(OUTTRANSITS).isArray()) {
            Iterator<JsonNode> nodeIt = junctionNode.get(OUTTRANSITS).elements();
            
            while(nodeIt.hasNext()) {
                transitionNodes.add(nodeIt.next());
            }            
        } else {
            transitionNodes.add(junctionNode.get(OUTTRANSITS));
        }
        return transitionNodes;
    }
    
    protected String getStateName(JsonNode stateNode) {
        return J2LUtils.sanitizeName(stateNode.get(PATH).asText());
    }
    
    public void collectInfo(JsonNode node, String cat) {
        // Process an array portConns
        if(node.isArray()) {
            Iterator<JsonNode> nodeIt = node.elements();
            
            while(nodeIt.hasNext()) {
                JsonNode internalNode = nodeIt.next();
                
                switch(cat) {
                    case STATES: {
                        if(internalNode.has(COMPOSITION) && internalNode.get(COMPOSITION).has(SUBSTATES) 
                                && internalNode.get(COMPOSITION).get(SUBSTATES).isArray() && internalNode.get(COMPOSITION).get(SUBSTATES).size() > 1) {
                            this.initStateId    = internalNode.get(COMPOSITION).get(DEFAULTTRANS).get(DEST).get(ID).asText();
                            this.entryStateId   = internalNode.get(ID).asText();
                        }
                        addToMapToNode(internalNode.get(ID).asText(), internalNode, states);
                        break;
                    }
                    case EVENTS: {
                        addToMapToNode(internalNode.get(ID).asText(), internalNode, events);
                        break;
                    }
                    case JUNCTIONS: {
                        addToMapToNode(internalNode.get(ID).asText(), internalNode, junctions);
                        break;
                    }   
                    default:
                        break;
                }
            }
        // Process a normal portConns    
        } else {  
        }           
    }
    
    public void addVariables(JsonNode data) {
        // Process an array portConns
        if(data.isArray()) {
            Iterator<JsonNode> dataIt = data.elements();
            
            while(dataIt.hasNext()) {
                JsonNode dataNode   = dataIt.next();
                addVariable(dataNode);
            }
        // Process a normal portConns    
        } else {
            addVariable(data);
        }        
    }
    
    public void addVariable(JsonNode dataNode) {
        String   id     = dataNode.get(ID).asText();
        int      port   = dataNode.get(PORT).asInt();

        switch (dataNode.get(SCOPE).asText()) {
            case INPUT: {
                this.inPortToId.put(port, id);
                this.inputs.put(id, dataNode);
                break;
            }
            case OUTPUT: {
                this.outPortToId.put(port, id);
                this.outputs.put(id, dataNode);
                break;
            }
            case LOCAL: {    
                this.locals.add(dataNode);
                break;
            }
            default:                        
                break;
        }        
    }
    
    public void addToMapToNodeList(String key, JsonNode value, Map<String, List<JsonNode>> map) {
        if(map.containsKey(key)) {
            map.get(key).add(value);
        } else {
            map.put(key, Arrays.asList(value));
        }
    }
    
    public void addToMapToNode(String key, JsonNode value, Map<String, JsonNode> map) {
        if(map.containsKey(key)) {
            LOGGER.log(Level.SEVERE, "Unexpected behavior from input: two states or events or junctions have the same id: {0}", key);
        } else {
            map.put(key, value);
        }
    }    
    
    public List<LustreAst> parseAndTranslate(String sf) {
        CharStream          charStream  = CharStreams.fromString(sf);
        StateflowLexer      lexer       = new StateflowLexer(charStream);
        TokenStream         tokens      = new CommonTokenStream(lexer);    
        StateflowParser     parser      = new StateflowParser(tokens);
        
        StateflowVisitor    visitor     = new StateflowVisitor();
        List<LustreAst>     asts        = visitor.visitFileDecl(parser.fileDecl());
        return asts;
    }
    
    public LustreAst parseAndTranslateStrExpr(String sf) {
        CharStream          charStream  = CharStreams.fromString(sf);
        StateflowLexer      lexer       = new StateflowLexer(charStream);
        TokenStream         tokens      = new CommonTokenStream(lexer);    
        StateflowParser     parser      = new StateflowParser(tokens);
        
        StateflowVisitor    visitor     = new StateflowVisitor();
        LustreAst           ast         = visitor.visitExpr(parser.expr());
        return ast;
    }    
}
