/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
 */
package edu.uiowa.json2lus;

import com.fasterxml.jackson.databind.JsonNode;
import edu.uiowa.json2lus.lustreAst.AutomatonIteExpr;
import edu.uiowa.json2lus.lustreAst.AutomatonState;
import edu.uiowa.json2lus.lustreAst.BinaryExpr;
import edu.uiowa.json2lus.lustreAst.BooleanExpr;
import edu.uiowa.json2lus.lustreAst.IntExpr;
import edu.uiowa.json2lus.lustreAst.LustreAst;
import edu.uiowa.json2lus.lustreAst.LustreAutomaton;
import edu.uiowa.json2lus.lustreAst.LustreEq;
import edu.uiowa.json2lus.lustreAst.LustreExpr;
import edu.uiowa.json2lus.lustreAst.LustreNode;
import edu.uiowa.json2lus.lustreAst.LustreType;
import edu.uiowa.json2lus.lustreAst.LustreVar;
import edu.uiowa.json2lus.lustreAst.PrimitiveType;
import edu.uiowa.json2lus.lustreAst.VarIdExpr;
import edu.uiowa.json2lus.stateflowparser.StateflowVisitor;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowLexer;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
    private final String CENTRY         = "ENTRY";        
    private final String EXIT           = "EXIT";        
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
    private final String STATE          = "State";
    private final String OUTPUT         = "Output";     
    private final String EVENTS         = "Events";
    private final String STATES         = "States";
    private final String DURING         = "During";    
    private final String ACTIONS        = "Actions";     
    private final String DATATYPE       = "Datatype";           
    private final String GRAPHFCN       = "GraphicalFunctions";
    private final String JUNCTION       = "Junction"; 
    private final String SUBSTATES      = "Substates";     
    private final String JUNCTIONS      = "Junctions";            
    private final String SFCONTENT      = "StateflowContent";        
    private final String CONDITION      = "Condition";    
    private final String TRANSITACT     = "TransitionAction";        
    private final String ORIGINPATH     = "Origin_path";
    private final String COMPOSITION    = "Composition"; 
    private final String COMPILEDTYPE   = "CompiledType";       
    private final String DEFAULTTRANS   = "DefaultTransitions";
    private final String OUTTRANSITS    = "OuterTransitions";
    private final String CONDITIONACT   = "ConditionAction";
    
    
    /** Data structures for constructing a node */
    String                      initStateId     = null;
    String                      entryStateId    = null;
    List<AutomatonState>        autoStates      = new ArrayList<>();        
    Map<String, JsonNode>       stateIdToNode          = new HashMap<>();        
    Map<String, JsonNode>       junctionIdToNode       = new HashMap<>();  
    Map<String, JsonNode>       functions       = new HashMap<>();  
    Map<String, JsonNode>       events          = new HashMap<>();
    Map<String, LustreExpr>     junctIdToExpr     = new HashMap<>();
    Map<String, JsonNode>       inputs          = new HashMap<>();
    Map<Integer, String>        inPortToId      = new HashMap<>();
    Map<Integer, String>        outPortToId     = new HashMap<>();
    Map<String, JsonNode>       outputs         = new HashMap<>(); 
    List<JsonNode>              locals          = new ArrayList<>();
    List<LustreNode>            auxNodes        = new ArrayList<>();
    Map<String, Integer>        stateIdToActId   = new HashMap<>();        

        
    public List<LustreAst> translate(JsonNode subsystemNode) {     
        List<LustreAst> asts = new ArrayList<>();
        String nodeName = J2LUtils.sanitizeName(subsystemNode.get(PATH).asText());
        
        LOGGER.log(Level.INFO, "******************** Start translating stateflow chart with name: {0}", nodeName);
        
        JsonNode automatonNode = subsystemNode.get(SFCONTENT);                
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
                    collectInfo(field.getValue(), GRAPHFCN);
                    break;
                }
                default:
                    break;
            }
        }
        
        // Start translating states       
        JsonNode            initNode                = null;
        LustreExpr          weakTransitExpr         = null;
        List<LustreExpr>    strongTransitExprs      = new ArrayList<>();
        VarIdExpr           actVarId                = new VarIdExpr("activeStateId");        
        String              automatonName           = J2LUtils.sanitizeName(automatonNode.get(PATH).asText());
        
        if(this.stateIdToNode.containsKey(this.entryStateId)) {
            initNode        = this.stateIdToNode.get(this.entryStateId);
            VarIdExpr initStateExpr   = new VarIdExpr(getStateName(initNode));
            // The weak transition expression that every state need to execute
            weakTransitExpr = new AutomatonIteExpr(new BooleanExpr(true), initStateExpr, null);
        }       
        
        // Translating states
        for(JsonNode stateNode : this.stateIdToNode.values()) {
            if(getStateId(stateNode).equals(this.entryStateId)) {
                continue;
            }            
            String  stateId     = getStateId(stateNode);
            String  stateName   = getStateName(stateNode);
                                   
            List<LustreEq>      stateEqs            = new ArrayList<>();
            LustreExpr          strongExpr          = new BinaryExpr(actVarId, BinaryExpr.Op.EQ, new IntExpr(new BigInteger(String.valueOf(this.stateIdToActId.get(stateId)))));
            JsonNode            actionNode          = stateNode.get(ACTIONS);            
            List<LustreEq>      entryExprs          = new ArrayList<>();    
            List<LustreEq>      duringExprs         = new ArrayList<>();    
            List<LustreExpr>    conditionExprs      = new ArrayList<>();
            List<LustreEq>      transitEqs          = new ArrayList<>();     
            List<JsonNode>      stateOuterTransits  = getOuterTransitions(stateNode);
            
            String  entryStr    = getStrEntryExpr(actionNode);
            String  durStr      = getStrDuringExpr(actionNode);  
            String  exitStr     = getStrExitExpr(actionNode);
                        
            if(stateId.equals(this.initStateId)) {
                if(entryStr != null) {
                    for(LustreAst ast : parseAndTranslate(entryStr)) {
                        entryExprs.add((LustreEq)ast);
                    }      
                    stateEqs.addAll(entryExprs);
                }                 
            }            
            if(durStr != null) {
                for(LustreAst ast : parseAndTranslate(durStr)) {
                    duringExprs.add((LustreEq)ast);
                }   
            }                               
            if(exitStr != null) {
                for(LustreAst ast : parseAndTranslate(exitStr)) {
                    transitEqs.add((LustreEq)ast);
                }                     
            }             
            
            // Add to the body of the state
            if(!duringExprs.isEmpty()) {
                stateEqs.addAll(duringExprs);    
            }            
            
            for(JsonNode outTransitNode : stateOuterTransits) {
                String  junctOrStateId      = getTransitToId(outTransitNode);
                String  conditionStr        = getTransitToCond(outTransitNode);
                String  condActStr          = getTransitToCondAct(outTransitNode);
                String  transitActStr       = getTransitToTransitAct(outTransitNode);
                
                if(conditionStr != null) {
                    conditionExprs.add((LustreExpr)parseAndTranslateStrExpr(conditionStr));
                }
                if(condActStr != null) {
                    for(LustreAst ast : parseAndTranslate(condActStr)) {
                        transitEqs.add((LustreEq)ast);
                    }                 
                }  
                if(transitActStr != null) {
                    for(LustreAst ast : parseAndTranslate(transitActStr)) {
                        transitEqs.add((LustreEq)ast);
                    }                
                }                 
                
                // If A state transit to a junction
                if(this.junctionIdToNode.containsKey(junctOrStateId)) {                                        
                    JsonNode            junctionNode        = this.junctionIdToNode.get(junctOrStateId);                        
                    List<JsonNode>      outerTransitions    = getOuterTransitions(junctionNode);
                    
                    // Handle outer transitions
                    for(JsonNode transNode : outerTransitions) {
                        String  transitToStateName  = stateName + "_" + EXIT + "_";                        
                        
                        if(transNode.has(DEST)) {
                            conditionStr    = getTransitToCond(transNode);
                            condActStr      = getTransitToCondAct(transNode);
                            transitActStr   = getTransitToTransitAct(transNode);
                            List<LustreEq>      newTransitEqs     = new ArrayList<>();  
                            List<LustreExpr>    newConditionExprs = new ArrayList<>();  

                            newTransitEqs.addAll(transitEqs);
                            newConditionExprs.addAll(conditionExprs);
                            
                            if(conditionStr != null) {
                                newConditionExprs.add((LustreExpr)parseAndTranslateStrExpr(conditionStr));
                            }
                            if(condActStr != null) {
                                for(LustreAst ast : parseAndTranslate(condActStr)) {
                                    newTransitEqs.add((LustreEq)ast);
                                }                                
                            }  
                            if(transitActStr != null) {
                                for(LustreAst ast : parseAndTranslate(transitActStr)) {
                                    newTransitEqs.add((LustreEq)ast);
                                }                                   
                            }                              

                            if(transNode.get(DEST).has(DEST)) {
                                String transitType = transNode.get(DEST).get(DEST).get(TYPE).asText();

                                switch (transitType) {
                                    case JUNCTION: {
                                        LOGGER.log(Level.SEVERE, "Unhandle case: transition to a junction!");
                                        break;
                                    }
                                    case STATE: {
                                        String      destStateId         = transNode.get(DEST).get(DEST).get(ID).asText();                                            
                                        String      destStateName       = J2LUtils.sanitizeName(transNode.get(DEST).get(DEST).get(NAME).asText());                                            
                                        JsonNode    destStateNode       = this.stateIdToNode.get(destStateId);                                        
                                        
                                        if(destStateNode.has(ACTIONS) && destStateNode.get(ACTIONS).has(ENTRY)) {
                                            String entryExprStr = getStrEntryExpr(destStateNode.get(ACTIONS));
                                            
                                            if(entryExprStr != null) {
                                                for(LustreAst ast : parseAndTranslate(entryExprStr)) {
                                                    newTransitEqs.add((LustreEq) ast);
                                                }                                                                         
                                            }                       
                                        }
                                        
                                        // Set the next active state ID
                                        newTransitEqs.add(new LustreEq(actVarId, new IntExpr(this.stateIdToActId.get(destStateId))));
                                        
                                        // Build the new state name for the transition
                                        transitToStateName += destStateName + "_" + CENTRY;
                                        
                                        // Create strong transition expression and save it to the global
                                        LustreExpr strongTransitExpr = strongExpr;
                                        for(LustreExpr e : newConditionExprs) {
                                            strongTransitExpr = new BinaryExpr(strongTransitExpr, BinaryExpr.Op.AND, e);
                                        }
                                        strongTransitExpr = new AutomatonIteExpr(strongTransitExpr, new VarIdExpr(transitToStateName), null);
                                        strongTransitExprs.add(strongTransitExpr);
                                        
                                        
                                        this.autoStates.add(new AutomatonState(false, transitToStateName, newTransitEqs, weakTransitExpr));
                                        break;
                                    }
                                    default:                                        
                                        LOGGER.log(Level.SEVERE, "Unhandle case: transition to some unknown stuff!");
                                        break;
                                }
                            } 
                        }
                    }
                } else if(this.stateIdToNode.containsKey(junctOrStateId)){                    
                    JsonNode            destNode        = this.stateIdToNode.get(junctOrStateId);
                    String              destStateName   = getName(destNode);
                    List<LustreEq>      newTransitEqs   = new ArrayList<>();                                           
                    String  transitToStateName  = stateName + "_" + EXIT + "_";     
                    List<LustreExpr>    newConditionExprs = new ArrayList<>(); 
                    
                    newConditionExprs.addAll(conditionExprs);
                    newTransitEqs.addAll(transitEqs);                                         
                    
                    // Get the entry expression of the destination state
                    if(destNode.has(ACTIONS) && destNode.get(ACTIONS).has(ENTRY)) {
                        String entryExprStr = getStrEntryExpr(destNode.get(ACTIONS));
                        if(entryExprStr != null) {
                            for(LustreAst ast : parseAndTranslate(entryExprStr)) {
                                newTransitEqs.add((LustreEq) ast);
                            }                                                                         
                        }                       
                    }

                    // Build the new state name for the transition
                    transitToStateName += destStateName + "_" + CENTRY;

                    // Create strong transition expression and save it to the global
                    LustreExpr strongTransitExpr = strongExpr;
                    for(LustreExpr e : newConditionExprs) {
                        strongTransitExpr = new BinaryExpr(strongTransitExpr, BinaryExpr.Op.AND, e);
                    }
                    strongTransitExpr = new AutomatonIteExpr(strongTransitExpr, new VarIdExpr(transitToStateName), null);
                    strongTransitExprs.add(strongTransitExpr);

                    this.autoStates.add(new AutomatonState(false, transitToStateName, newTransitEqs, weakTransitExpr));                
                } else {
                    LOGGER.log(Level.SEVERE, "Unhandle case: transition to some unknown state or junction!");
                }                  
            }
            this.autoStates.add(new AutomatonState(false, stateName, stateEqs, weakTransitExpr));            
        }        
        
        // Create initial state
        for(Map.Entry<String, Integer> entry : this.stateIdToActId.entrySet()) {
            String  stateId = entry.getKey();
            int     actId   = entry.getValue();
            
            strongTransitExprs.add(new AutomatonIteExpr(new BinaryExpr(actVarId, BinaryExpr.Op.EQ, new IntExpr(actId)), new VarIdExpr(getStateName(this.stateIdToNode.get(stateId))), null));
        }
        this.autoStates.add(new AutomatonState(true, getStateName(initNode), new ArrayList<LustreVar>(), strongTransitExprs, new ArrayList<LustreEq>(), new ArrayList<LustreExpr>()));
        
        // Create node inputs and outputs
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
        
        newLocals.add(new LustreVar(actVarId.id, PrimitiveType.INT));
        for(JsonNode localNode : this.locals) {
            String      localName   = localNode.get(NAME).asText();
            LustreType  type        = J2LUtils.getLustreTypeFromStrRep(localNode.get(COMPILEDTYPE).asText());
            newLocals.add(new LustreVar(localName, type));
        }
        
//        asts.addAll(createFunctions());
        asts.add(new LustreNode(nodeName, new LustreAutomaton(automatonName, this.autoStates), newInputs, newOutputs, newLocals));
        LOGGER.log(Level.INFO, "******************** Done ********************");
        return asts;
    }
    
    protected List<LustreNode> createFunctions() {
        List<LustreNode> fcns = new ArrayList<>();
        // Translate functions
        for(JsonNode fcnNode : this.functions.values()) {
            List<LustreVar> fcnInputs    = new ArrayList<>();
            List<LustreVar> fcnOutputs   = new ArrayList<>();  
            List<LustreEq>  fcnBody      = new ArrayList<>();
            
            LinkedHashMap<LustreExpr, LustreEq> condExprs = new LinkedHashMap<>();                            
            LinkedHashMap<LustreExpr, List<LustreEq>> condToCondTransitAct = new LinkedHashMap<>();                
                        
            // Get inputs and outputs
            if(fcnNode.has(DATA)) {
                Iterator<JsonNode> dataIt = fcnNode.get(DATA).elements();
                
                while(dataIt.hasNext()) {
                    JsonNode    dataNode    = dataIt.next();
                    LustreVar   var         = new LustreVar(dataNode.get(NAME).asText(), J2LUtils.getLustreTypeFromStrRep(dataNode.get(COMPILEDTYPE).asText()));
                    
                    switch (dataNode.get(SCOPE).asText()) {
                        case INPUT:
                            fcnInputs.add(var);
                            break;
                        case OUTPUT:
                            fcnOutputs.add(var);
                            break;
                        default:
                            LOGGER.log(Level.SEVERE, "Unhandled case: the variable is a {0}", dataNode.get(SCOPE).asText());
                            break;
                    }
                }
            }
            
            // Create the function body
            if(fcnNode.has(COMPOSITION) && fcnNode.get(COMPOSITION).has(DEFAULTTRANS)) {
                for(JsonNode defaultTransit : getDefaultTransitions(fcnNode.get(COMPOSITION))) {
                    createFcnDef(defaultTransit, condToCondTransitAct);
                }
            }
            // Create the actual body equations
            for(Map.Entry<LustreExpr, List<LustreEq>> map : condToCondTransitAct.entrySet()) {
                for(LustreEq eq : map.getValue()) {
                    
                }
            }
        }        
        return fcns;
    }
    
    // You cannot define states in functions, which means you can only make a transition from junction to junction
    protected void createFcnDef(JsonNode junctionNode, LinkedHashMap<LustreExpr, List<LustreEq>> condToCondTransitAct) {
        String      destJunctionId      = junctionNode.get(DEST).get(ID).asText();
        JsonNode    destJunctionNode    = this.junctionIdToNode.get(destJunctionId);
        
        List<LustreExpr>    condExprs           = new ArrayList<>();
        List<LustreEq>      condActExprs        = new ArrayList<>();
        List<LustreEq>      transitActExprs     = new ArrayList<>();
        
        String  strCond     = getStrCondExpr(junctionNode);
        String  strCondAct  = getStrCondActExpr(junctionNode);
        String  strTranAct  = getStrTranActExpr(junctionNode);
        
        if(strCond != null) {
            condExprs.add((LustreExpr)parseAndTranslateStrExpr(strCond));
        } else {
            condExprs.add(new BooleanExpr(true));
        }
        if(strCondAct != null) {
            for(LustreAst ast : parseAndTranslate(strCondAct)) {
                if(ast instanceof LustreEq) {
                    condActExprs.add((LustreEq)ast);
                }
            }
        }
        if(strTranAct != null) {
            for(LustreAst ast : parseAndTranslate(strTranAct)) {
                if(ast instanceof LustreEq) {
                    transitActExprs.add((LustreEq)ast);
                }
            }
        }
        if(!condActExprs.isEmpty() || !transitActExprs.isEmpty()) {
            List<LustreEq> allEqs = new ArrayList<>();
            LustreExpr  condExpr = J2LUtils.andExprs(condExprs);
            allEqs.addAll(condActExprs);
            allEqs.addAll(transitActExprs);
            condToCondTransitAct.put(condExpr, allEqs);
        }
        createFcnDef(destJunctionId, condExprs, condToCondTransitAct);
    }  
 
    
    protected void createFcnDef(String junctionId, List<LustreExpr> condExprs, LinkedHashMap<LustreExpr, List<LustreEq>> condToCondTransitAct) {
        JsonNode    destJunctionNode    = this.junctionIdToNode.get(junctionId);        
        List<JsonNode> outerTransits = getOuterTransitions(destJunctionNode);
        
        if(!outerTransits.isEmpty()) {
            for(JsonNode outTransitNode : outerTransits) {        
                String              destJunctionId      = outTransitNode.get(DEST).get(DEST).get(ID).asText();                
                List<LustreExpr>    newCondExprs        = new ArrayList<>();
                List<LustreEq>      condActExprs        = new ArrayList<>();
                List<LustreEq>      transitActExprs     = new ArrayList<>();

                // Add the previous junction's conditions, condition actions, and transition actions
                newCondExprs.addAll(condExprs);

                String  strCond     = getStrCondExpr(destJunctionNode);
                String  strCondAct  = getStrCondActExpr(destJunctionNode);
                String  strTranAct  = getStrTranActExpr(destJunctionNode);

                if(strCond != null) {
                    newCondExprs.add((LustreExpr)parseAndTranslateStrExpr(strCond));
                } else {
                    newCondExprs.add(new BooleanExpr(true));
                }
                if(strCondAct != null) {
                    for(LustreAst ast : parseAndTranslate(strCondAct)) {
                        if(ast instanceof LustreEq) {
                            condActExprs.add((LustreEq)ast);
                        }
                    }
                }
                if(strTranAct != null) {
                    for(LustreAst ast : parseAndTranslate(strTranAct)) {
                        if(ast instanceof LustreEq) {
                            transitActExprs.add((LustreEq)ast);
                        }
                    }
                }
                if(!condActExprs.isEmpty() || !transitActExprs.isEmpty()) {            
                    List<LustreEq> allEqs = new ArrayList<>();
                    allEqs.addAll(condActExprs);
                    allEqs.addAll(transitActExprs);
                    condToCondTransitAct.put(J2LUtils.andExprs(condExprs), allEqs);                
                }   
                createFcnDef(destJunctionId, newCondExprs, condToCondTransitAct);
            }            
        }
    }      
    
    protected String getTransitToId(JsonNode transitNode) {
        String id = null;
        if(transitNode.has(DEST)) {
            if(transitNode.get(DEST).has(ID)) {
                String strId = transitNode.get(DEST).get(ID).asText();
                if(strId != null && !strId.equals("")) {
                    id = strId;
                }                                 
            }
        }
        return id;
    }
    
    protected String getTransitToCond(JsonNode transitNode) {
        String cond = null;
        if(transitNode.has(DEST)) {
            if(transitNode.get(DEST).has(CONDITION)) {
                String strCond = transitNode.get(DEST).get(CONDITION).asText();
                if(strCond != null && !strCond.equals("")) {
                    cond = strCond;
                }                
            }
        }   
        return cond;
    }    
    
    protected String getTransitToCondAct(JsonNode transitNode) {
        String condAct = null;
        if(transitNode.has(DEST)) {
            if(transitNode.get(DEST).has(CONDITIONACT)) {
                String strCondAct = transitNode.get(DEST).get(CONDITIONACT).asText();
                if(strCondAct != null && !strCondAct.equals("")) {
                    condAct = strCondAct;
                }                                
            }
        }    
        return condAct;
    }     
    
    protected String getTransitToTransitAct(JsonNode transitNode) {
        String transitAct = null;
        if(transitNode.has(DEST)) {
            if(transitNode.get(DEST).has(TRANSITACT)) {
                String strTransitAct = transitNode.get(DEST).get(TRANSITACT).asText();
                if(strTransitAct != null && !strTransitAct.equals("")) {
                    transitAct = strTransitAct;
                }
            }
        }    
        return transitAct;
    }         
    
    
    
    protected String getStrExitExpr(JsonNode node) {
        String strExpr = null;
        if(node.has(EXIT)) {
            String strExit = node.get(EXIT).asText();
            if(strExit != null && !strExit.equals("")) {
                strExpr = strExit;
            }                       
        }
        return strExpr;
    }
   
    protected String getStrEntryExpr(JsonNode node) {
        String strExpr = null;
        if(node.has(ENTRY)) {
            String strEntry = node.get(ENTRY).asText();
            if(strEntry != null && !strEntry.equals("")) {
                strExpr = strEntry;
            }             
        }
        return strExpr;
    }

    protected String getStrDuringExpr(JsonNode node) {
        String strExpr = null;
        if(node.has(DURING)) {
            String strDuring = node.get(DURING).asText();
            if(strDuring != null && !strDuring.equals("")) {
                strExpr = strDuring;
            }               
        }
        return strExpr;
    }

    protected String getStrCondExpr(JsonNode node) {
        String strExpr = null;
        if(node.has(CONDITION)) {
            String strCond = node.get(CONDITION).asText();
            if(strCond != null && !strCond.equals("")) {
                strExpr = strCond;
            }                          
        }
        return strExpr;
    } 
    
    protected String getStrCondActExpr(JsonNode node) {
        String strExpr = null;
        if(node.has(CONDITIONACT)) {
            String strCondAct = node.get(CONDITIONACT).asText();
            if(strCondAct != null && !strCondAct.equals("")) {
                strExpr = strCondAct;
            }                 
        }
        return strExpr;
    } 

    protected String getStrTranActExpr(JsonNode node) {
        String strExpr = null;
        if(node.has(TRANSITACT)) {
            String strTransAct = node.get(TRANSITACT).asText();
            if(strTransAct != null && !strTransAct.equals("")) {
                strExpr = strTransAct;
            }             
        }
        return strExpr;
    }     
    
    protected String getName(JsonNode node) {
        return J2LUtils.sanitizeName(node.get(PATH).asText());
    }
    
    protected boolean isTransitToJunction(JsonNode stateNode) {
        return stateNode.has(OUTTRANSITS) && stateNode.get(OUTTRANSITS).has(DEST) && stateNode.get(OUTTRANSITS).get(DEST).get(TYPE).asText().equals(JUNCTION);
    }
    
    protected boolean isTransitToState(JsonNode stateNode) {
        return stateNode.has(OUTTRANSITS) && stateNode.get(OUTTRANSITS).has(DEST) && stateNode.get(OUTTRANSITS).get(DEST).get(TYPE).asText().equals(STATE);
    }       
    
    protected List<JsonNode> getOuterTransitions(JsonNode node) {
        List<JsonNode> transitionNodes = new ArrayList<>();
        if(node.has(OUTTRANSITS)) {
            JsonNode outerTransits = node.get(OUTTRANSITS);
            
            if(outerTransits != null) {
                if(outerTransits.isArray() && outerTransits.size()>0) {
                    Iterator<JsonNode> nodeIt = outerTransits.elements();

                    while(nodeIt.hasNext()) {
                        transitionNodes.add(nodeIt.next());
                    }            
                } else {
                    transitionNodes.add(outerTransits);
                }                   
            }            
        }
        return transitionNodes;
    }
    
    protected List<JsonNode> getDefaultTransitions(JsonNode node) {
        List<JsonNode> transitionNodes = new ArrayList<>();
        if(node.has(DEFAULTTRANS)) {
            JsonNode defaultTransits = node.get(DEFAULTTRANS);
            
            if(defaultTransits != null) {
                if(defaultTransits.isArray() && defaultTransits.size()>0) {
                    Iterator<JsonNode> nodeIt = defaultTransits.elements();

                    while(nodeIt.hasNext()) {
                        transitionNodes.add(nodeIt.next());
                    }            
                } else {
                    transitionNodes.add(defaultTransits);
                }                   
            }         
        }
        return transitionNodes;
    }    
    
    protected String getStateName(JsonNode stateNode) {
        String name = null;
        if(stateNode.has(PATH)) {
            String strName = stateNode.get(PATH).asText();
            if(strName != null && !strName.equals("")) {
                name = J2LUtils.sanitizeName(strName);
            }            
        }
        return name;
    }
    
    protected String getStateId(JsonNode stateNode) {
        String id = null;
        if(stateNode.has(ID)) {
            String strID = stateNode.get(ID).asText();
            if(strID != null && !strID.equals("")) {
                id = strID;
            }            
        }
        return id;
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
                            int id = 1;
                            JsonNode substateNodeIds = internalNode.get(COMPOSITION).get(STATES);                            
                            Iterator<JsonNode> elementIt = substateNodeIds.elements();
                            
                            while(elementIt.hasNext()) {
                                JsonNode element = elementIt.next();
                                this.stateIdToActId.put(element.asText(), id);
                                ++id;
                            }
                            this.entryStateId   = getStateId(internalNode);  
                            this.initStateId    = internalNode.get(COMPOSITION).get(DEFAULTTRANS).get(DEST).get(ID).asText();
                        }
                        addToMapToNode(getStateId(internalNode), internalNode, this.stateIdToNode);
                        break;
                    }
                    case EVENTS: {
                        addToMapToNode(getStateId(internalNode), internalNode, events);
                        break;
                    }
                    case JUNCTIONS: {
                        addToMapToNode(getStateId(internalNode), internalNode, this.junctionIdToNode);
                        break;
                    } 
                    case GRAPHFCN: {
                        addToMapToNode(getStateId(internalNode), internalNode, functions);
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
