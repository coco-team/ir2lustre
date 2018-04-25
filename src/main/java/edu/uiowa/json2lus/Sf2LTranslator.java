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
import edu.uiowa.json2lus.lustreAst.IteExpr;
import edu.uiowa.json2lus.lustreAst.LustreAst;
import edu.uiowa.json2lus.lustreAst.LustreAutomaton;
import edu.uiowa.json2lus.lustreAst.LustreEq;
import edu.uiowa.json2lus.lustreAst.LustreExpr;
import edu.uiowa.json2lus.lustreAst.LustreNode;
import edu.uiowa.json2lus.lustreAst.LustreType;
import edu.uiowa.json2lus.lustreAst.LustreVar;
import edu.uiowa.json2lus.lustreAst.PrimitiveType;
import edu.uiowa.json2lus.lustreAst.UnaryExpr;
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
import java.util.Set;
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
    String                      initStateId             = null;
    String                      entryStateId            = null;
    List<AutomatonState>        autoStates              = new ArrayList<>();        
    Map<String, JsonNode>       stateIdToNode           = new HashMap<>();        
    Map<String, JsonNode>       junctionIdToNode        = new HashMap<>();  
    Map<String, JsonNode>       functions               = new HashMap<>();  
    Map<String, JsonNode>       events                  = new HashMap<>();
    Map<String, LustreExpr>     junctIdToExpr           = new HashMap<>();
    Map<String, JsonNode>       inputs                  = new HashMap<>();
    Map<Integer, String>        inPortToId              = new HashMap<>();
    Map<Integer, String>        outPortToId             = new HashMap<>();
    Map<String, JsonNode>       outputs                 = new HashMap<>(); 
    List<JsonNode>              locals                  = new ArrayList<>();
    List<LustreNode>            auxNodes                = new ArrayList<>();
    Map<String, Integer>        stateIdToActId          = new HashMap<>();        

    /**
     * Translate the state flow content of subsystem node
     * @param subsystemNode
     * @return 
     */    
    public List<LustreAst> translate(JsonNode subsystemNode) {     
        List<LustreAst> asts = new ArrayList<>();
        String sfName = J2LUtils.sanitizeName(subsystemNode.get(PATH).asText());
        
        LOGGER.log(Level.INFO, "******************** Start translating stateflow chart with name: {0}", sfName);
        
        // Collect information about states, junctions, and functions
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
        
        // Set the initial state
        if(this.stateIdToNode.containsKey(this.entryStateId)) {
            initNode    = this.stateIdToNode.get(this.entryStateId);
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
            List<LustreEq>      transitActEqs       = new ArrayList<>();     
            List<LustreEq>      condActEqs          = new ArrayList<>();                 
            
            String  entryStr    = getActionStrEntryExpr(actionNode);
            String  durStr      = getActionStrDuringExpr(actionNode);  
            String  exitStr     = getActionStrExitExpr(actionNode);
                        
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
                    transitActEqs.add((LustreEq)ast);
                }                     
            }             
            
            // Add the during expression to the body of the state
            if(!duringExprs.isEmpty()) {
                stateEqs.addAll(duringExprs);    
            }            
            
            // Start translating the outer transitions
            List<JsonNode> stateOuterTransits  = getStateOuterTransitions(stateNode);
            
            for(JsonNode outTransitNode : stateOuterTransits) {
                String  transitionType      = getTransitionType(outTransitNode);
                String  junctOrStateId      = getDestinationId(outTransitNode);
                String  conditionStr        = getCondForTransition(outTransitNode);
//                String  condActStr          = getCondActForTransition(outTransitNode);
                List<String> condActStrs = getCondActForTransition(outTransitNode);                 
                String  transitActStr       = getTransitActForTransition(outTransitNode);
                
                // Parse the condition expression
                if(conditionStr != null) {
                    conditionExprs.add((LustreExpr)parseAndTranslateStrExpr(conditionStr));
                }
                // Parse the condition action expression
                for(String condActStr : condActStrs) {
                    LustreAst ast = parseAndTranslateStrExpr(condActStr);
                    if(ast instanceof LustreEq) {
                        condActEqs.add((LustreEq)ast);
                    }
                }                 
                // Parse the transition action expression
                if(transitActStr != null) {
                    for(LustreAst ast : parseAndTranslate(transitActStr)) {
                        transitActEqs.add((LustreEq)ast);
                    }                
                }     
                
                // Translate transitions
                switch(transitionType) {
                    case JUNCTION: {
                        translateJunctionTransition(junctOrStateId, actVarId, stateName, condActEqs, transitActEqs);
                        break;
                    }
                    case STATE: {
                        if(this.stateIdToNode.containsKey(junctOrStateId)){                    
                            JsonNode            destNode        = this.stateIdToNode.get(junctOrStateId);
                            String              destStateName   = getName(destNode);
                            List<LustreEq>      newTransitEqs   = new ArrayList<>();                                           
                            String  transitToStateName  = stateName + "_" + EXIT + "_";     
                            List<LustreExpr>    newConditionExprs = new ArrayList<>(); 
                            
                            // Add the condition expressions
                            newConditionExprs.addAll(conditionExprs);
                            // Add the condition action expressions
                            newTransitEqs.addAll(condActEqs);
                            // Add the transition action expressions
                            newTransitEqs.addAll(transitActEqs);                                         

                            // Get the entry expression of the destination state
                            if(destNode.has(ACTIONS) && destNode.get(ACTIONS).has(ENTRY)) {
                                String entryExprStr = getActionStrEntryExpr(destNode.get(ACTIONS));
                                if(entryExprStr != null) {
                                    for(LustreAst ast : parseAndTranslate(entryExprStr)) {
                                        newTransitEqs.add((LustreEq) ast);
                                    }                                                                         
                                }                       
                            }

                            // Build the name for the transition from stateNode to destNode
                            transitToStateName += destStateName + "_" + CENTRY;

                            // Create strong transition expression and save it to the global
                            LustreExpr strongTransitExpr = strongExpr;
                            for(LustreExpr e : newConditionExprs) {
                                strongTransitExpr = new BinaryExpr(strongTransitExpr, BinaryExpr.Op.AND, e);
                            }
                            // Add the strong transition expression to the global 
                            strongTransitExprs.add(new AutomatonIteExpr(strongTransitExpr, new VarIdExpr(transitToStateName), null));
                            
                            // Put the transition state to the autoStates
                            this.autoStates.add(new AutomatonState(false, transitToStateName, newTransitEqs, weakTransitExpr));                
                        } else {
                            LOGGER.log(Level.SEVERE, "No state has ID: {0}", junctOrStateId);
                        }                        
                        break;
                    }
                    default:
                        break;
                            
                }                               
            }
            this.autoStates.add(new AutomatonState(false, stateName, stateEqs, weakTransitExpr));            
        }        
        
        // Set the transition conditions
        for(Map.Entry<String, Integer> entry : this.stateIdToActId.entrySet()) {
            strongTransitExprs.add(new AutomatonIteExpr(new BinaryExpr(actVarId, BinaryExpr.Op.EQ, new IntExpr(entry.getValue())), new VarIdExpr(getStateName(this.stateIdToNode.get(entry.getKey()))), null));
        }
        
        // Create the initial state
        this.autoStates.add(new AutomatonState(true, getStateName(initNode), new ArrayList<LustreVar>(), strongTransitExprs, new ArrayList<LustreEq>(), new ArrayList<LustreExpr>()));
        
        // Create node inputs and outputs
        List<LustreVar> newInputs    = new ArrayList<>();
        List<LustreVar> newOutputs   = new ArrayList<>();
        List<LustreVar> newLocals    = new ArrayList<>();
        Map<String, LustreType>     strInputs    = new HashMap<>();
        Map<String, LustreType>     strLocals    = new HashMap<>();
        Map<String, LustreType>     strOutputs   = new LinkedHashMap<>();
        
        for(int i = 1; i <= this.inputs.size(); ++i) {
            JsonNode    inputNode   = this.inputs.get(this.inPortToId.get(i));
            String      inputName   = inputNode.get(NAME).asText();
            LustreType  type        = J2LUtils.getLustreTypeFromStrRep(inputNode.get(COMPILEDTYPE).asText());
            newInputs.add(new LustreVar(inputName, type));
            strInputs.put(inputName, type);
        }
        for(int i = 1; i <= this.outputs.size(); ++i) {
            JsonNode    outputNode  = this.outputs.get(this.outPortToId.get(i));
            String      outputName  = outputNode.get(NAME).asText();
            LustreType  type        = J2LUtils.getLustreTypeFromStrRep(outputNode.get(COMPILEDTYPE).asText());
            newOutputs.add(new LustreVar(outputName+"_final", type));   
            strOutputs.put(outputName, type);
        }
        
        // Create the local variable activeStateId
        newLocals.add(new LustreVar(actVarId.id, PrimitiveType.INT));
        
        for(JsonNode localNode : this.locals) {
            String      localName   = localNode.get(NAME).asText();
            LustreType  type        = J2LUtils.getLustreTypeFromStrRep(localNode.get(COMPILEDTYPE).asText());
            newLocals.add(new LustreVar(localName, type));
            strLocals.put(localName, type);
        }
        
        // Create SSA form for each state's equations        
        List<Map<String, String>> varToLastNames = new ArrayList<>();
        
        for(AutomatonState state : this.autoStates) {      
            Map<String, String> varToLastName = new HashMap<>();
            
            createSSAForm(state.equations, strInputs, strOutputs, strLocals, state.locals, varToLastName);
            varToLastNames.add(varToLastName);
        }
        
        // Finalize outputs
        // Use linked hash map to maintain the key order
        Set<String> keys    = strOutputs.keySet();
        String[]    keyArr  = keys.toArray(new String[0]);        
        
        for(int i = 0; i < keyArr.length; ++i) {
            for(int j = 0; j < this.autoStates.size(); ++j) {
                if(varToLastNames.get(j).containsKey(keyArr[i])) {
                    this.autoStates.get(j).equations.add(new LustreEq(new VarIdExpr(newOutputs.get(i).name), new VarIdExpr(varToLastNames.get(j).get(keyArr[i]))));
                }
            }            
        }
        
        // Create functions
        asts.addAll(createFunctions());
        
        asts.add(new LustreNode(sfName, new LustreAutomaton(automatonName, this.autoStates), newInputs, newOutputs, newLocals));
        LOGGER.log(Level.INFO, "******************** Done ********************");
        return asts;
    }
    
    /**
     * 
     * @param junctionId
     * @param actStateVarId
     * @param stateName
     * @param condActEqs
     * @param transitActEqs
     */
    protected void translateJunctionTransition(String junctionId, VarIdExpr actStateVarId, String stateName, List<LustreEq> condActEqs, List<LustreEq> transitActEqs) {
        // If A state transit to a junction
        if(this.junctionIdToNode.containsKey(junctionId)) {                                                    
            JsonNode            junctionNode        = this.junctionIdToNode.get(junctionId);                        
            List<JsonNode>      outerTransitions    = getJunctionOuterTransitions(junctionNode);
            String              transitToJunctionName  = stateName + "_" + EXIT + "_" + getJunctionName(junctionNode) + "_" + ENTRY;             
            LinkedHashMap<LustreExpr, List<LustreEq>> condToFinalActs   = new LinkedHashMap<>();
            LinkedHashMap<LustreExpr, List<LustreEq>> condToCondActs    = new LinkedHashMap<>();
            
            // Handle outer transitions
            for(JsonNode transNode : outerTransitions) {                
                if(transNode.has(DEST)) {
                    String          conditionStr    = getCondForTransition(transNode);
                    List<String>    condActStrs     = getCondActForTransition(transNode);                    
                    String          transitActStr   = getTransitActForTransition(transNode);
                    List<LustreEq>      newTransitActEqs    = new ArrayList<>();  
                    List<LustreEq>      newCondActEqs       = new ArrayList<>();  
                    List<LustreExpr>    newConditionExprs   = new ArrayList<>();
                    List<LustreExpr>    allConditionExprs   = new ArrayList<>();
                    List<LustreEq>      allTransitActEqs    = new ArrayList<>();

                    newTransitActEqs.addAll(transitActEqs);

                    if(conditionStr != null) {
                        newConditionExprs.add((LustreExpr)parseAndTranslateStrExpr(conditionStr));
                    }
                    for(String condActStr : condActStrs) {
                        LustreAst ast = parseAndTranslateStrExpr(condActStr);
                        if(ast instanceof LustreEq) {
                            newCondActEqs.add((LustreEq)ast);
                        }
                    }                    
                    if(transitActStr != null) {
                        for(LustreAst ast : parseAndTranslate(transitActStr)) {
                            newTransitActEqs.add((LustreEq)ast);
                        }                                   
                    }                              
                    translateJunctionTransition(transNode, actStateVarId, newTransitActEqs, newCondActEqs, newConditionExprs, condToFinalActs);                    
                } else {
                    LOGGER.log(Level.SEVERE, "A junction does not have outer transition destination!");
                }
            }
        } else {
            LOGGER.log(Level.SEVERE, "There is no junction with ID {0}", junctionId);
        }       
    }
    
    protected void translateJunctionTransition(JsonNode transNode, VarIdExpr actStateId, List<LustreEq> transitActEqs, List<LustreEq> condActEqs, List<LustreExpr> conditionExprs, LinkedHashMap<LustreExpr, List<LustreEq>> condToFinalActs) {
        if(transNode.get(DEST).has(DEST)) {
            String transitType = transNode.get(DEST).get(DEST).get(TYPE).asText();

            switch (transitType) {
                case JUNCTION: {
                    List<JsonNode> outerTransitions = getJunctionOuterTransitions(transNode);
                    
                    for(JsonNode outerTransitNode : outerTransitions) {
                        String newConditionStr    = getCondForTransition(outerTransitNode);
                        List<String> newCondActStrs = getCondActForTransition(outerTransitNode);
                        String newTransitActStr   = getTransitActForTransition(outerTransitNode);
                        List<LustreEq>      newTransitActEqs    = new ArrayList<>();  
                        List<LustreEq>      newCondActEqs       = new ArrayList<>();  
                        List<LustreExpr>    newConditionExprs   = new ArrayList<>();       
                        
                        newTransitActEqs.addAll(transitActEqs);
                        newConditionExprs.addAll(conditionExprs);
                        
                        if(newConditionStr != null) {
                            newConditionExprs.add((LustreExpr)parseAndTranslateStrExpr(newConditionStr));
                        }
                        for(String newCondActStr : newCondActStrs) {
                            LustreAst ast = parseAndTranslateStrExpr(newCondActStr);
                            if(ast instanceof LustreEq) {
                                newCondActEqs.add((LustreEq)ast);
                            }
                        }
                        if(newTransitActStr != null) {
                            for(LustreAst ast : parseAndTranslate(newTransitActStr)) {
                                newTransitActEqs.add((LustreEq)ast);
                            }                                   
                        }      
                        
                        translateJunctionTransition(transNode, actStateId, newTransitActEqs, newCondActEqs, newConditionExprs, condToFinalActs);                        
                    }
                    break;
                }
                case STATE: {
                    String      destStateId         = transNode.get(DEST).get(DEST).get(ID).asText();                                            
                    String      destStateName       = J2LUtils.sanitizeName(transNode.get(DEST).get(DEST).get(NAME).asText());                                            
                    JsonNode    destStateNode       = this.stateIdToNode.get(destStateId);     
                    List<LustreEq> newTransitActEqs = new ArrayList<>();                                         
                    
                    // Add the cumulative equations
                    newTransitActEqs.addAll(transitActEqs);
                    
                    // Add the entry expression of the destination state
                    if(destStateNode.has(ACTIONS) && destStateNode.get(ACTIONS).has(ENTRY)) {
                        String entryExprStr = getActionStrEntryExpr(destStateNode.get(ACTIONS));

                        if(entryExprStr != null) {
                            for(LustreAst ast : parseAndTranslate(entryExprStr)) {
                                newTransitActEqs.add((LustreEq) ast);
                            }                                                                         
                        }                       
                    }
                    
                    // Set the next active state ID
                    newTransitActEqs.add(new LustreEq(actStateId, new IntExpr(this.stateIdToActId.get(destStateId))));
                    
                    condToFinalActs.put(J2LUtils.andExprs(conditionExprs), newTransitActEqs);
                    // Build the new state name for the transition

                    break;
                }
                default:                                        
                    LOGGER.log(Level.SEVERE, "Unhandle case: transition to some unknown stuff!");
                    break;
            }
        }         
    }
    
    protected List<LustreNode> createFunctions() {
        List<LustreNode> fcns = new ArrayList<>();
        // Translate functions
        for(JsonNode fcnNode : this.functions.values()) {
            Map<String, LustreType> inputs     = new HashMap<>();
            Map<String, LustreType> outputs    = new HashMap<>();
            Map<String, LustreType> locals     = new HashMap<>();
            List<LustreVar> fcnLocals    = new ArrayList<>();
            List<LustreVar> fcnInputs    = new ArrayList<>();
            List<LustreVar> fcnOutputs   = new ArrayList<>();
            List<LustreEq>  fcnBody      = new ArrayList<>();
            
            List<LustreEq>      noGuardActExprs = new ArrayList<>();
            LinkedHashMap<LustreExpr, List<LustreEq>> condToCondActs = new LinkedHashMap<>();                
                        
            // Get inputs and outputs
            if(fcnNode.has(DATA)) {
                Iterator<JsonNode> dataIt = fcnNode.get(DATA).elements();
                
                while(dataIt.hasNext()) {
                    JsonNode    dataNode    = dataIt.next();
                    String      name        = dataNode.get(NAME).asText();
                    LustreType  type        = J2LUtils.getLustreTypeFromStrRep(dataNode.get(COMPILEDTYPE).asText());
                    LustreVar   var         = new LustreVar(name, type);
                    
                    switch (dataNode.get(SCOPE).asText()) {
                        case INPUT: {
                            fcnInputs.add(var);
                            inputs.put(name, type);
                            break;
                        }
                        case OUTPUT: {
                            fcnOutputs.add(var);
                            outputs.put(name, type);
                            break;
                        }
                        case LOCAL: {
                            fcnLocals.add(var);
                            locals.put(name, type);
                            break;
                        }
                        default:
                            LOGGER.log(Level.SEVERE, "Unhandled case: the variable is a {0}", dataNode.get(SCOPE).asText());
                            break;
                    }
                }
            }
            
            // Collect the function body information
            if(fcnNode.has(COMPOSITION) && fcnNode.get(COMPOSITION).has(DEFAULTTRANS)) {
                for(JsonNode defaultTransit : getDefaultTransitions(fcnNode.get(COMPOSITION))) {
                    createFcnBodyDef(defaultTransit, noGuardActExprs, condToCondActs);
                }
            }
            Map<String, String> varToLastName = new HashMap<>();
            
            // Create the function body equations
            if(!noGuardActExprs.isEmpty()) {
                createSSAForm(noGuardActExprs, inputs, outputs, locals, fcnLocals, varToLastName);
                fcnBody.addAll(noGuardActExprs);
            }
            
            // Create the function body equations
            for(Map.Entry<LustreExpr, List<LustreEq>> map : condToCondActs.entrySet()) {
                
            }

            // Create the final outputs for the function
            List<LustreVar> fcnFinalOutputs = new ArrayList<>();  
            
            for(LustreVar var : fcnOutputs) {
                if(varToLastName.containsKey(var.name)) {
                    fcnFinalOutputs.add(new LustreVar(varToLastName.get(var.name), var.type));
                } else {
                    fcnFinalOutputs.add(var);
                }
            }
            fcns.add(new LustreNode(getName(fcnNode), fcnInputs, fcnFinalOutputs, fcnLocals, fcnBody));
        }        
        return fcns;
    }
    
    protected void createSSAForm(List<LustreEq> equations, Map<String, LustreType> inputs, Map<String, LustreType> outputs, Map<String, LustreType> locals, List<LustreVar> fcnLocals, Map<String, String> varToLastName) {                
        for(int i = 0; i < equations.size(); ++i) {
            LustreExpr          rhs = equations.get(i).getRhs();
            List<LustreExpr>    lhs = equations.get(i).getLhs();
            
            // Replace the right hand side expression variables            
            replaceRhsVars(i, rhs, varToLastName, new HashMap<String, LustreType>());
            
            // Replace the first occurence of an output variable with pre(outputVar)
            if(i == 0) {
                replaceRhsVars(0, rhs, varToLastName, outputs);
            }              
            
            // Replace variables in the lhs expression with new variables
            for(LustreExpr lhsExpr : lhs) {
                if(lhsExpr instanceof VarIdExpr) {
                    String lhsExprName = ((VarIdExpr)lhsExpr).id;
                    
                    if(inputs.containsKey(lhsExprName) || outputs.containsKey(lhsExprName)) {
                        LustreType type;
                        String newVarName = J2LUtils.getFreshVar(lhsExprName);                        
                        
                        if(inputs.containsKey(lhsExprName)) {
                            type = inputs.get(lhsExprName);
                        } else if(outputs.containsKey(lhsExprName)){
                            type = outputs.get(lhsExprName);
                        } else {
                            type = locals.get(lhsExprName);
                        }
                        
                        ((VarIdExpr) lhsExpr).setVarId(newVarName);
                        varToLastName.put(lhsExprName, newVarName);
                        fcnLocals.add(new LustreVar(newVarName, type));
                    }
                } else {
                    LOGGER.log(Level.SEVERE, "Unhandled left hand side expression case: {0}", lhsExpr);
                }
            }                     
        }        
    }
    
    protected void replaceRhsVars(int i, LustreExpr expr, Map<String, String> varToLastName, Map<String, LustreType> outputs) {
        if(expr instanceof BinaryExpr) {
            replaceRhsVars(i, ((BinaryExpr)expr).left, varToLastName, outputs);
            replaceRhsVars(i, ((BinaryExpr)expr).right, varToLastName, outputs);
        } else if(expr instanceof UnaryExpr) {
            replaceRhsVars(i, ((UnaryExpr)expr).expr, varToLastName, outputs);
        } else if(expr instanceof IteExpr) {
            replaceRhsVars(i, ((IteExpr)expr).ifExpr, varToLastName, outputs);
            replaceRhsVars(i, ((IteExpr)expr).thenExpr, varToLastName, outputs);
            replaceRhsVars(i, ((IteExpr)expr).elseExpr, varToLastName, outputs);
        } else if(expr instanceof VarIdExpr) {
            String varName = ((VarIdExpr)expr).id;
            
            if(i != 0) {
                if(varToLastName.containsKey(varName)) {
                    ((VarIdExpr)expr).setVarId(varToLastName.get(varName));
                }             
            } else if(outputs.containsKey(varName)){
                ((VarIdExpr)expr).setVarId("pre("+varName+")");
            }
        }  else {
            LOGGER.log(Level.WARNING, "Unhandled expression case: {0}", expr);
        }
    }
    
    // You cannot define states in functions, which means you can only make a transition from a junction to another junction
    protected void createFcnBodyDef(JsonNode defaultTransitNode, List<LustreEq> noGuardActExprs, LinkedHashMap<LustreExpr, List<LustreEq>> condToCondTransitAct) {
        String      destJunctionId      = defaultTransitNode.get(DEST).get(ID).asText();        
        
        List<LustreExpr>    condExprs           = new ArrayList<>();
        List<LustreEq>      condActExprs        = new ArrayList<>();        
        
        String          strCond         = getJunctionDefaultTransitStrCondExpr(defaultTransitNode);
        List<String>    strCondActs     = getJunctionDefaultTransitStrCondActExpr(defaultTransitNode);
        
        // Get condition in string format
        if(strCond != null) {
            condExprs.add((LustreExpr)parseAndTranslateStrExpr(strCond));
        }
        
        // Convert condition actions in string to Lustre expressions
        for(String strCondAct : strCondActs) {
            LustreAst ast = parseAndTranslateStrExpr(strCondAct);
            
            if(ast instanceof LustreEq) {
                condActExprs.add((LustreEq)ast);
            } else {
                LOGGER.log(Level.SEVERE, "Unexpected condition action: {0}", ast);
            }            
        }

        if(condExprs.isEmpty()) {
            noGuardActExprs.addAll(condActExprs);
        } else {
            List<LustreEq> allEqs   = new ArrayList<>();
            LustreExpr  condExpr    = J2LUtils.andExprs(condExprs);
            allEqs.addAll(condActExprs);
            condToCondTransitAct.put(condExpr, allEqs);
        }
        createFcnDef(destJunctionId, condExprs, noGuardActExprs, condToCondTransitAct);
    }  
 
    
    protected void createFcnDef(String junctionId, List<LustreExpr> condExprs, List<LustreEq> noGuardActExprs, LinkedHashMap<LustreExpr, List<LustreEq>> condToCondTransitAct) {
        JsonNode        destJunctionNode    = this.junctionIdToNode.get(junctionId);        
        List<JsonNode>  outerTransits       = getJunctionOuterTransitions(destJunctionNode);
        
        for(JsonNode outTransitNode : outerTransits) {        
            String              destJunctionId      = outTransitNode.get(DEST).get(DEST).get(ID).asText();                
            List<LustreExpr>    newCondExprs        = new ArrayList<>();
            List<LustreEq>      condActExprs        = new ArrayList<>();

            // Add the previous junction's conditions, condition actions, and transition actions
            // newCondExprs.addAll(condExprs);
            String  strCond     = getJunctionOutTransitStrCondExpr(outTransitNode);
            String  strCondAct  = getJunctionOutTransitStrCondActExpr(outTransitNode);

            if(strCond != null) {
                newCondExprs.add((LustreExpr)parseAndTranslateStrExpr(strCond));
            }
            if(strCondAct != null) {
                for(LustreAst ast : parseAndTranslate(strCondAct)) {
                    if(ast instanceof LustreEq) {
                        condActExprs.add((LustreEq)ast);
                    }
                }
            }      
            
            // If the condition expression is empty and so far no conditions generated yet, 
            // we add the condition action to noGuardActExprs
            if(condToCondTransitAct.isEmpty() && newCondExprs.isEmpty()) {
                noGuardActExprs.addAll(condActExprs);
            // If the condition expression is empty, we append the condition action expressions
            // to the previous condition expressions
            } else if(newCondExprs.isEmpty()) {
                List<List<LustreEq>> values = (List<List<LustreEq>>) condToCondTransitAct.values();
                values.get(values.size()-1).addAll(condActExprs);
            } else {
                List<LustreEq> allEqs   = new ArrayList<>();                
                allEqs.addAll(condActExprs);
                newCondExprs.addAll(condExprs);
                condToCondTransitAct.put(J2LUtils.andExprs(newCondExprs), allEqs);
            } 
            createFcnDef(destJunctionId, newCondExprs, noGuardActExprs, condToCondTransitAct);
        }  
    }      
    
    protected String getDestinationId(JsonNode transitNode) {
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
    
    protected String getTransitionType(JsonNode transitNode) {
        String type = null;
        if(transitNode.has(DEST)) {
            if(transitNode.get(DEST).has(TYPE)) {
                String strType = transitNode.get(DEST).get(TYPE).asText();
                if(strType != null && !strType.equals("")) {
                    type = strType;
                }                                 
            }
        }
        return type;
    }    
    
    protected String getCondForTransition(JsonNode transitNode) {
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
    
    protected List<String> getCondActForTransition(JsonNode transitNode) {
        List<String> condActs = new ArrayList<>();
        if(transitNode.has(DEST)) {
            if(transitNode.get(DEST).has(CONDITIONACT)) {
                JsonNode strCondActNode = transitNode.get(DEST).get(CONDITIONACT);
                
                if(strCondActNode != null) {
                    if(strCondActNode.isArray()) {
                        Iterator<JsonNode> condActIt = strCondActNode.elements();
                        
                        while(condActIt.hasNext()) {
                            condActs.add(condActIt.next().asText());
                        }
                    } else if(!strCondActNode.asText().equals("")) {
                        condActs.add(strCondActNode.asText());
                    }                       
                }                             
            }
        }    
        return condActs;
    }     
    
    protected String getTransitActForTransition(JsonNode transitNode) {
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
    
    
    
    protected String getActionStrExitExpr(JsonNode node) {
        String strExpr = null;
        if(node.has(EXIT)) {
            String strExit = node.get(EXIT).asText();
            if(strExit != null && !strExit.equals("")) {
                strExpr = strExit;
            }                       
        }
        return strExpr;
    }
   
    protected String getActionStrEntryExpr(JsonNode node) {
        String strExpr = null;
        if(node.has(ENTRY)) {
            String strEntry = node.get(ENTRY).asText();
            if(strEntry != null && !strEntry.equals("")) {
                strExpr = strEntry;
            }             
        }
        return strExpr;
    }

    protected String getActionStrDuringExpr(JsonNode node) {
        String strExpr = null;
        if(node.has(DURING)) {
            String strDuring = node.get(DURING).asText();
            if(strDuring != null && !strDuring.equals("")) {
                strExpr = strDuring;
            }               
        }
        return strExpr;
    }

    protected String getStateStrCondExpr(JsonNode node) {
        String strExpr = null;
        if(node.has(CONDITION)) {
            String strCond = node.get(CONDITION).asText();
            if(strCond != null && !strCond.equals("")) {
                strExpr = strCond;
            }                          
        }
        return strExpr;
    } 
    
    protected String getStateStrCondActExpr(JsonNode node) {
        String strExpr = null;
        if(node.has(CONDITIONACT)) {
            String strCondAct = node.get(CONDITIONACT).asText();
            if(strCondAct != null && !strCondAct.equals("")) {
                strExpr = strCondAct;
            }                 
        }
        return strExpr;
    } 

    protected String getStateStrTranActExpr(JsonNode node) {
        String strExpr = null;
        if(node.has(TRANSITACT)) {
            String strTransAct = node.get(TRANSITACT).asText();
            if(strTransAct != null && !strTransAct.equals("")) {
                strExpr = strTransAct;
            }             
        }
        return strExpr;
    }    
    
    protected String getJunctionDefaultTransitStrCondExpr(JsonNode node) {
        String strExpr = null;
        if(node.has(CONDITION)) {
            String strCond = node.get(CONDITION).asText();
            if(strCond != null && !strCond.equals("")) {
                strExpr = strCond;
            }                          
        }
        return strExpr;
    } 
    
    protected List<String> getJunctionDefaultTransitStrCondActExpr(JsonNode node) {
        List<String> strExprs = new ArrayList<>();

        if(node.has(CONDITIONACT)) {
            JsonNode strCondActNode = node.get(CONDITIONACT);
            
            if(strCondActNode != null) {
                if(strCondActNode.isArray()) {
                    Iterator<JsonNode> actIt = strCondActNode.elements();
                    
                    while(actIt.hasNext()) {
                        strExprs.add(actIt.next().asText());
                    }
                } else if(!strCondActNode.equals("")) {                
                    strExprs.add(strCondActNode.asText());
                }                 
            }
        }
        return strExprs;
    } 

    protected String getJunctionDefaultTransitStrTranActExpr(JsonNode node) {
        String strExpr = null;
        if(node.has(TRANSITACT)) {
            String strTransAct = node.get(TRANSITACT).asText();
            if(strTransAct != null && !strTransAct.equals("")) {
                strExpr = strTransAct;
            }             
        }
        return strExpr;
    }     
    
    protected String getJunctionOutTransitStrCondExpr(JsonNode node) {
        String strExpr = null;
        if(node.has(DEST) && node.get(DEST).has(CONDITION)) {
            String strCond = node.get(DEST).get(CONDITION).asText();
            if(strCond != null && !strCond.equals("")) {
                strExpr = strCond;
            }                          
        }
        return strExpr;
    } 
    
    protected String getJunctionOutTransitStrCondActExpr(JsonNode node) {
        String strExpr = null;
        if(node.has(DEST) && node.get(DEST).has(CONDITIONACT)) {
            String strCondAct = node.get(DEST).get(CONDITIONACT).asText();
            if(strCondAct != null && !strCondAct.equals("")) {
                strExpr = strCondAct;
            }                 
        }
        return strExpr;
    } 

    protected String getJunctionOutTransitStrTranActExpr(JsonNode node) {
        String strExpr = null;
        if(node.has(DEST) && node.get(DEST).has(TRANSITACT)) {
            String strTransAct = node.get(DEST).get(TRANSITACT).asText();
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
    
    protected List<JsonNode> getJunctionOuterTransitions(JsonNode node) {
        List<JsonNode> transitionNodes = new ArrayList<>();
        if(node.has(OUTTRANSITS)) {
            JsonNode outerTransits = node.get(OUTTRANSITS);
            
            if(outerTransits != null) {
                if(outerTransits.size()>0) {
                    if(!outerTransits.isArray()) {
                        transitionNodes.add(outerTransits);
                    } else {
                        Iterator<JsonNode> nodeIt = outerTransits.elements();

                        while(nodeIt.hasNext()) {
                            transitionNodes.add(nodeIt.next());
                        }                          
                    }          
                }          
            }            
        }
        return transitionNodes;
    }
    
    protected List<JsonNode> getStateOuterTransitions(JsonNode node) {
        List<JsonNode> transitionNodes = new ArrayList<>();
        if(node.has(OUTTRANSITS)) {
            JsonNode outerTransits = node.get(OUTTRANSITS);
            
            if(outerTransits != null) {
                if(outerTransits.size()>0) {
                    if(!outerTransits.isArray()) {
                        transitionNodes.add(outerTransits);
                    } else {
                        Iterator<JsonNode> nodeIt = outerTransits.elements();

                        while(nodeIt.hasNext()) {
                            transitionNodes.add(nodeIt.next());
                        }                          
                    }          
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
    
    protected String getJunctionName(JsonNode junctionNode) {
        String name = null;
        if(junctionNode.has(PATH)) {
            String strName = junctionNode.get(PATH).asText();
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
