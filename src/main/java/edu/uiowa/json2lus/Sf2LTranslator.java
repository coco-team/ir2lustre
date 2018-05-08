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
import edu.uiowa.json2lus.lustreAst.NodeCallExpr;
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
    private final String ACTSTATEID     = "activeStateId";
    private final String NXTACTSTATEID  = "nextActiveStateId";
    
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
    
    
    /** Data structures*/
    String                      initStateId;
    String                      centerStateId;
    List<AutomatonState>        autoStates;        
    Map<String, LustreExpr>     varToInitVal;    
    Map<String, JsonNode>       stateIdToNode;        
    Map<String, JsonNode>       junctionIdToNode;  
    Map<String, JsonNode>       functions;  
    Map<String, JsonNode>       events;
    Map<String, LustreExpr>     junctIdToExpr;
    Map<String, JsonNode>       inputs;
    Map<Integer, String>        inPortToId;
    Map<Integer, String>        outPortToId;
    Map<String, JsonNode>       outputs; 
    Map<String, String>         fcnNameToPath; 
    List<JsonNode>              locals;
    List<LustreNode>            auxNodes;
    Map<String, Integer>        stateIdToActId;    

    public Sf2LTranslator() {
        this.initStateId             = null;
        this.centerStateId            = null;
        this.autoStates              = new ArrayList<>();        
        this.varToInitVal            = new HashMap<>();    
        this.stateIdToNode           = new HashMap<>();        
        this.junctionIdToNode        = new HashMap<>();  
        this.functions               = new HashMap<>();  
        this.events                  = new HashMap<>();
        this.junctIdToExpr           = new HashMap<>();
        this.inputs                  = new HashMap<>();
        this.inPortToId              = new HashMap<>();
        this.outPortToId             = new HashMap<>();
        this.outputs                 = new HashMap<>(); 
        this.fcnNameToPath           = new HashMap<>();
        this.locals                  = new ArrayList<>();
        this.auxNodes                = new ArrayList<>();
        this.stateIdToActId          = new HashMap<>();            
    }

    /**
     * Translate the state flow content of subsystem node
     * @param subsystemNode
     * @return 
     */    
    public List<LustreAst> translate(JsonNode subsystemNode) {     
        List<LustreAst> resultAsts  = new ArrayList<>();
        String          sfName      = J2LUtils.sanitizeName(subsystemNode.get(PATH).asText());
        
        LOGGER.log(Level.INFO, "******************** Start translating stateflow chart: {0} ********************", sfName);                              
        
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
                    LOGGER.log(Level.SEVERE, "Unsupported event yet!");
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
        
        // Create functions
        resultAsts.addAll(createFunctions());          
        
        // Start translating states       
        JsonNode            centerNode              = null;       
        List<LustreExpr>    strongTransitExprs      = new ArrayList<>();
        VarIdExpr           curActStateVarId        = new VarIdExpr(ACTSTATEID);        
        VarIdExpr           nxtActStateVarId        = new VarIdExpr(NXTACTSTATEID);        
        String              automatonName           = getPath(automatonNode);
        
        // Create node inputs, locals and outputs
        List<LustreVar> tempInputs     = new ArrayList<>();
        List<LustreVar> finalInputs    = new ArrayList<>();
        List<LustreVar> finalOutputs   = new ArrayList<>();
        List<LustreVar> finalLocals    = new ArrayList<>();
        
        Map<String, LustreType>     inputNameToType    = new HashMap<>();
        Map<String, LustreType>     localNameToType    = new HashMap<>();
        Map<String, LustreType>     outputNameToType   = new HashMap<>();
        Map<String, String>         outVarIdMap        = new HashMap<>();
      
        // Create the input variable activeStateId
        finalInputs.add(new LustreVar(curActStateVarId.id, PrimitiveType.INT));        
        
        for(int i = 1; i <= this.inputs.size(); ++i) {
            JsonNode    inputNode   = this.inputs.get(this.inPortToId.get(i));
            String      inputName   = inputNode.get(NAME).asText();
            LustreType  type        = J2LUtils.getLustreTypeFromStrRep(inputNode.get(COMPILEDTYPE).asText());            
            tempInputs.add(new LustreVar(inputName, type));
            inputNameToType.put(inputName, type);
        }                      
        for(JsonNode localNode : this.locals) {
            String      localName   = localNode.get(NAME).asText();
            LustreType  type        = J2LUtils.getLustreTypeFromStrRep(localNode.get(COMPILEDTYPE).asText());
            finalLocals.add(new LustreVar(localName, type));
            localNameToType.put(localName, type);
        }      
        for(int i = 1; i <= this.outputs.size(); ++i) {
            JsonNode    outputNode  = this.outputs.get(this.outPortToId.get(i));
            String      outputName  = outputNode.get(NAME).asText();            
            LustreType  type        = J2LUtils.getLustreTypeFromStrRep(outputNode.get(COMPILEDTYPE).asText());
            LustreVar   preOutVar   = new LustreVar(J2LUtils.getFreshVarAtInst(outputName, 1), type);
            finalOutputs.add(new LustreVar(outputName, type));   
            outputNameToType.put(outputName, type);
            finalInputs.add(preOutVar);
            outVarIdMap.put(outputName, preOutVar.name);
        }
        // Add tempInputs to the final input variables
        finalInputs.addAll(tempInputs);
        
        // Create the output variable nextActiveStateId as the last output
        finalOutputs.add(new LustreVar(nxtActStateVarId.id, PrimitiveType.INT));        
        
        // Set the entry state
        if(this.stateIdToNode.containsKey(this.centerStateId)) {
            centerNode = this.stateIdToNode.get(this.centerStateId);
        } 
        
        // The weak transition expression that every state need to execute
        LustreExpr weakTransitExpr = new AutomatonIteExpr(new BooleanExpr(true), new VarIdExpr(getStatePath(centerNode)), null);
        
        // Storing information about transitions to junctions
        LinkedHashMap<String, LustreExpr>       transitNameToCond       = new LinkedHashMap<>();            
        LinkedHashMap<String, List<LustreEq>>   transitNameToAllActs    = new LinkedHashMap<>();        
        
        // Translating states except for the entry state
        for(JsonNode stateNode : this.stateIdToNode.values()) {
            String  stateId = getStateId(stateNode);         
            
            // Skip the entry state node
            if(stateId.equals(this.centerStateId)) {
                continue;
            }     
            
            String  stateName   = getStatePath(stateNode);                                   
            List<LustreEq>      stateEqs            = new ArrayList<>();
            LustreExpr          strongExpr          = new BinaryExpr(curActStateVarId, BinaryExpr.Op.EQ, new IntExpr(new BigInteger(String.valueOf(this.stateIdToActId.get(stateId)))));
            JsonNode            actionNode          = stateNode.get(ACTIONS);            
            List<LustreEq>      entryExprs          = new ArrayList<>();    
            List<LustreEq>      duringExprs         = new ArrayList<>();    
            List<LustreEq>      exitExprs           = new ArrayList<>();                    
            
            String  entryStr    = getActionStrEntryExpr(actionNode);
            String  durStr      = getActionStrDuringExpr(actionNode);  
            String  exitStr     = getActionStrExitExpr(actionNode);
            
            // Add entry expressions
            if(stateId.equals(this.initStateId)) {
                if(entryStr != null) {
                    for(LustreAst ast : parseAndTranslate(entryStr)) {
                        LustreEq eq = (LustreEq)ast;
                        
                        if(eq.getLhs().size() == 1) {
                            varToInitVal.put(((VarIdExpr)eq.getLhs().get(0)).id, eq.getRhs());
                        } else {
                            LOGGER.log(Level.SEVERE, "Unhandled case: the left-hand side expression has multiple variables");
                        }
                        entryExprs.add(eq);
                    }      
                    stateEqs.addAll(entryExprs);
                }                 
            }
            // Add during expressions            
            if(durStr != null) {
                for(LustreAst ast : parseAndTranslate(durStr)) {
                    duringExprs.add((LustreEq)ast);
                }   
            }             
            // Add exit expressions
            if(exitStr != null) {
                for(LustreAst ast : parseAndTranslate(exitStr)) {
                    exitExprs.add((LustreEq)ast);
                }                     
            }             
            
            // Add the during expression to the body of the state
            if(!duringExprs.isEmpty()) {
                stateEqs.addAll(duringExprs);    
            }            
            
            // Start translating the outer transitions                                             
            List<JsonNode> stateOuterTransits  = getStateOuterTransitions(stateNode);                        
            
            for(int i = 0; i < stateOuterTransits.size(); ++i) {
                List<LustreExpr>    condExprs      = new ArrayList<>();
                List<LustreEq>      transitActEqs  = new ArrayList<>();     
                List<LustreEq>      condActEqs     = new ArrayList<>();                   
                
                JsonNode stateOutTransitNode    = stateOuterTransits.get(i);   
                String  transitionType          = getStateTransitionType(stateOutTransitNode);
                String  junctOrStateId          = getStateTransitDestId(stateOutTransitNode);
                String  conditionStr            = getCondForTransition(stateOutTransitNode);
                String  condActStrs             = getCondActForTransition(stateOutTransitNode);                 
                String  transitActStrs          = getTransitActForTransition(stateOutTransitNode);
                
                // Parse the condition expression
                if(conditionStr != null) { 
                    condExprs.add((LustreExpr)parseAndTranslateStrExpr(conditionStr));
                }
                // Parse the condition action expression
                if(condActStrs != null) {
                    for(LustreAst ast : parseAndTranslate(condActStrs)) {
                        if(ast instanceof LustreEq) {
                            condActEqs.add((LustreEq)ast);
                        }                        
                    }
                }                 
                // Parse the transition action expression
                if(transitActStrs != null) {
                    for(LustreAst ast : parseAndTranslate(transitActStrs)) {
                        if(ast instanceof LustreEq) {
                            transitActEqs.add((LustreEq)ast);
                        }                        
                    }
                }                   
                
                // Translate transitions
                switch(transitionType) {
                    case JUNCTION: {
                        condExprs.add(strongExpr);
                        translateJunctionTransition(nxtActStateVarId, stateNode, junctOrStateId, condExprs, condActEqs, exitExprs, transitActEqs, transitNameToAllActs, transitNameToCond);
                        break;
                    }
                    case STATE: {
                        if(this.stateIdToNode.containsKey(junctOrStateId)){       
                            int                 actStateId          = this.stateIdToActId.get(junctOrStateId);
                            JsonNode            destNode            = this.stateIdToNode.get(junctOrStateId);
                            String              destStateName       = getPath(destNode);
                            List<LustreEq>      transitEqs          = new ArrayList<>();                                           
                            List<LustreExpr>    newConditionExprs   = new ArrayList<>();
                            String  transitToStateName  = stateName + "_" + EXIT + "_" + destStateName + "_" + CENTRY;;                                                             
                            
                            // Add the condition expressions
                            if(!condExprs.isEmpty()) {
                                newConditionExprs.addAll(condExprs);
                            }
                            // Add the condition action expressions
                            if(!condActEqs.isEmpty()) {
                                transitEqs.addAll(condActEqs);
                            }
                            // Add the exit actions
                            if(!exitExprs.isEmpty()) {
                                transitEqs.addAll(exitExprs);
                            }
                            // Add the transition action expressions
                            if(!transitActEqs.isEmpty()) {
                                transitEqs.addAll(transitActEqs);                                         
                            }

                            // Get the entry expression of the destination state
                            if(destNode.has(ACTIONS) && destNode.get(ACTIONS).has(ENTRY)) {
                                String entryExprStr = getActionStrEntryExpr(destNode.get(ACTIONS));
                                if(entryExprStr != null) {
                                    for(LustreAst ast : parseAndTranslate(entryExprStr)) {
                                        transitEqs.add((LustreEq) ast);
                                    }                                                                         
                                }                       
                            }
                            // Set the next active state
                            transitEqs.add(new LustreEq(nxtActStateVarId, new IntExpr(actStateId)));
                            
                            // Create strong transition expression
                            newConditionExprs.add(strongExpr);

                            // Add the strong transition expression to the global 
                            strongTransitExprs.add(new AutomatonIteExpr(J2LUtils.andExprs(newConditionExprs), new VarIdExpr(transitToStateName), null));
                            
                            // Save the transition state in the autoStates
                            this.autoStates.add(new AutomatonState(transitToStateName, transitEqs, weakTransitExpr));                
                        } else {
                            LOGGER.log(Level.SEVERE, "No state has ID: {0}", junctOrStateId);
                        }                        
                        break;
                    }
                    default:
                        break;
                            
                }                               
            }
            this.autoStates.add(new AutomatonState(stateName, stateEqs, weakTransitExpr));                                    
        }        

        // Create SSA form for each state's equations        
        for(AutomatonState state : this.autoStates) {
            convertToSSAForm(state.equations, inputNameToType, outputNameToType, localNameToType, state.locals, outVarIdMap);
        }
        
        // Create states for transitions to junctions
        for(Map.Entry<String, LustreExpr> transitNameToCondEntry : transitNameToCond.entrySet()) {
            String transitName = transitNameToCondEntry.getKey();
            
            if(transitNameToAllActs.containsKey(transitName) && !transitNameToAllActs.get(transitName).isEmpty()) {
                // Add the transition condition to the strong transition condition
                strongTransitExprs.add(new AutomatonIteExpr(transitNameToCondEntry.getValue(), new VarIdExpr(transitNameToCondEntry.getKey()), null));

                List<LustreEq>  allActEqs           = transitNameToAllActs.get(transitName);
                List<LustreVar> transitStateLocals  = new ArrayList<>();
                
                // Convert to SSA form
                convertToSSAForm(allActEqs, inputNameToType, outputNameToType, localNameToType, transitStateLocals, outVarIdMap);
                
                this.autoStates.add(new AutomatonState(transitName, transitStateLocals, allActEqs, weakTransitExpr));            
            }           
        }   
        
        // Set the default strong transition conditions to states
        for(Map.Entry<String, Integer> entry : this.stateIdToActId.entrySet()) {
            strongTransitExprs.add(new AutomatonIteExpr(new BinaryExpr(curActStateVarId, BinaryExpr.Op.EQ, new IntExpr(entry.getValue())), new VarIdExpr(getStatePath(this.stateIdToNode.get(entry.getKey()))), null));
        }
        
        // Create the initial state
        this.autoStates.add(new AutomatonState(true, getStatePath(centerNode), new ArrayList<LustreVar>(), strongTransitExprs, new ArrayList<LustreEq>(), new ArrayList<LustreExpr>()));        
        
        resultAsts.add(new LustreNode(sfName, new LustreAutomaton(automatonName, this.autoStates), finalInputs, finalOutputs, finalLocals));
        LOGGER.log(Level.INFO, "******************** Done ********************");
        return resultAsts;
    }
    
    /**
     * 
     * @param nextActState
     * @param startStateNode
     * @param junctionId
     * @param condExprs
     * @param condActEqs
     * @param exitExprs
     * @param transitActEqs
     * @param transitNameToActs
     * @param transitNameToCond
     */
    protected void translateJunctionTransition(VarIdExpr nextActState, JsonNode startStateNode, String junctionId, List<LustreExpr> condExprs, List<LustreEq> condActEqs, List<LustreEq> exitExprs, List<LustreEq> transitActEqs, LinkedHashMap<String, List<LustreEq>> transitNameToActs, LinkedHashMap<String, LustreExpr> transitNameToCond) {
        // If a state transit to a junction
        if(this.junctionIdToNode.containsKey(junctionId)) {                 
            List<LustreExpr>    negPrevCondExprs         = new ArrayList<>();
            JsonNode            junctionNode             = this.junctionIdToNode.get(junctionId);                        
            String              junctionName             = getJunctionPath(junctionNode);
            List<JsonNode>      outerTransitions         = getJunctionOuterTransitions(junctionNode);
            
            // Handle outer transitions
            for(int i = 0; i < outerTransitions.size(); ++i) {
                JsonNode transNode = outerTransitions.get(i);   
                
                if(transNode.has(DEST)) {
                    String    destName        = getJunctionTransitDestName(transNode);
                    String    conditionStr    = getCondForTransition(transNode);
                    String    condActStrs     = getCondActForTransition(transNode);                    
                    String    transitActStrs  = getTransitActForTransition(transNode);

                    List<LustreExpr>    newCondExprs        = new ArrayList<>();
                    List<LustreEq>      newTransitActEqs    = new ArrayList<>();  
                    List<LustreEq>      newCondActEqs       = new ArrayList<>();     
                    
                    // Add the previous condition expressions, condition actions, transition actions
                    if(!condExprs.isEmpty()) {
                        newCondExprs.addAll(condExprs);
                    }
                    if(!condActEqs.isEmpty()) {
                        newCondActEqs.addAll(condActEqs);
                    }
                    if(!transitActEqs.isEmpty()) {
                        newTransitActEqs.addAll(transitActEqs);
                    }
                    
                    // Parse the condition expression
                    if(conditionStr != null) {
                        LustreExpr condExpr = (LustreExpr)parseAndTranslateStrExpr(conditionStr);
                        negPrevCondExprs.add(new UnaryExpr(UnaryExpr.Op.NOT, condExpr));
                        newCondExprs.add(condExpr);
                    }
                    // Parse the condition action expression
                    if(condActStrs != null) {
                        for(LustreAst ast : parseAndTranslate(condActStrs)) {
                            if(ast instanceof LustreEq) {
                                newCondActEqs.add((LustreEq)ast);
                            }                        
                        }
                    }                 
                    // Parse the transition action expression
                    if(transitActStrs != null) {
                        for(LustreAst ast : parseAndTranslate(transitActStrs)) {
                            if(ast instanceof LustreEq) {
                                newTransitActEqs.add((LustreEq)ast);
                            }                        
                        }
                    }
                    
                    // Call the translation recursively
                    translateJunctionTransition(nextActState, startStateNode, transNode, newTransitActEqs, newCondActEqs, newCondExprs, exitExprs, transitNameToActs, transitNameToCond);                                      
                } else {
                    LOGGER.log(Level.SEVERE, "A junction does not have outer transition destination!");
                }
            }
            // If there are some previous condition action expressions,
            // we negated all the current conditions and execute the condition
            // action expressions.
            if(!condActEqs.isEmpty()) {
                String transitionName = J2LUtils.getFreshVarName(getStatePath(startStateNode) + "_" + EXIT + "_" + junctionName + "_" + CENTRY);                    
                transitNameToActs.put(transitionName, condActEqs);
                transitNameToCond.put(transitionName, J2LUtils.andExprs(negPrevCondExprs));                   
            }
        } else {
            LOGGER.log(Level.SEVERE, "There is no junction with ID {0}", junctionId);
        }       
    }

    
    /**
     * 
     * @param nextActState
     * @param startStateNode
     * @param transNode
     * @param transitActEqs
     * @param condActEqs
     * @param condExprs
     * @param exitExprs
     * @param transitNameToActs
     * @param transitNameToCond
     */
    protected void translateJunctionTransition(VarIdExpr nextActState, JsonNode startStateNode, JsonNode transNode, List<LustreEq> transitActEqs, List<LustreEq> condActEqs, List<LustreExpr> condExprs, List<LustreEq> exitExprs, LinkedHashMap<String, List<LustreEq>> transitNameToActs, LinkedHashMap<String, LustreExpr> transitNameToCond) {
        if(transNode.get(DEST).has(DEST)) {
            String transitType = transNode.get(DEST).get(DEST).get(TYPE).asText();

            switch (transitType) {
                case JUNCTION: {
                    String              junctionName     = getJunctionPath(transNode);
                    List<LustreExpr>    negPrevCondExprs = new ArrayList<>();
                    List<JsonNode>      outerTransitions = getJunctionOuterTransitions(transNode);
                    
                    for(int i = 0; i < outerTransitions.size(); ++i) {
                        JsonNode outerTransitNode = outerTransitions.get(i);
                        
                        String destJunctName   = getJunctionTransitDestName(outerTransitNode);
                        String condStr         = getCondForTransition(outerTransitNode);
                        String condActStrs     = getCondActForTransition(outerTransitNode);
                        String transitActStrs  = getTransitActForTransition(outerTransitNode);
                        
                        List<LustreEq>      newTransitActEqs    = new ArrayList<>();  
                        List<LustreEq>      newCondActEqs       = new ArrayList<>();  
                        List<LustreExpr>    newCondExprs        = new ArrayList<>();       
                        
                        // Add previous actions and conditions
                        if(!condActEqs.isEmpty()) {
                            newCondActEqs.addAll(condActEqs);
                        }
                        if(!transitActEqs.isEmpty()) {
                            newTransitActEqs.addAll(transitActEqs);
                        }
                        if(!condExprs.isEmpty()) {
                            newCondExprs.addAll(condExprs);
                        }
                        
                        // Parse the current condition expression
                        if(condStr != null) {
                            LustreExpr newCondExpr = (LustreExpr)parseAndTranslateStrExpr(condStr);
                            negPrevCondExprs.add(new UnaryExpr(UnaryExpr.Op.NOT, newCondExpr));
                            newCondExprs.add(newCondExpr);
                        }                        
                        // Parse the current condition action expression
                        if(condActStrs != null) {
                            for(LustreAst ast : parseAndTranslate(condActStrs)) {
                                if(ast instanceof LustreEq) {
                                    newCondActEqs.add((LustreEq)ast);
                                }                        
                            }
                        }                 
                        // Parse the current transition action expression
                        if(transitActStrs != null) {
                            for(LustreAst ast : parseAndTranslate(transitActStrs)) {
                                if(ast instanceof LustreEq) {
                                    newTransitActEqs.add((LustreEq)ast);
                                }
                            }
                        }
                        // Continue the translation
                        translateJunctionTransition(nextActState, startStateNode, transNode, newTransitActEqs, newCondActEqs, newCondExprs, exitExprs, transitNameToActs, transitNameToCond);
                        
                        // Build the intermediate transitioins
                        String transitionName = J2LUtils.getFreshVarName(getStatePath(startStateNode) + "_" + EXIT + "_" + destJunctName + "_" + CENTRY);                    
                        newCondActEqs.add(new LustreEq(nextActState, new IntExpr(this.stateIdToActId.get(getStateId(startStateNode)))));                            
                        transitNameToActs.put(transitionName, newCondActEqs);
                        transitNameToCond.put(transitionName, J2LUtils.andExprs(condExprs));                                                                                                                        
                    }
                    // If there are some previous condition action expressions,
                    // we negated all the current conditions and execute the condition
                    // action expressions.
                    if(!condActEqs.isEmpty()) {
                        String transitionName = J2LUtils.getFreshVarName(getStatePath(startStateNode) + "_" + EXIT + "_" + junctionName + "_" + CENTRY);                    
                        transitNameToActs.put(transitionName, condActEqs);
                        transitNameToCond.put(transitionName, J2LUtils.andExprs(negPrevCondExprs));                   
                    }                    
                    break;
                }
                case STATE: {
                    List<LustreEq> allActEqs = new ArrayList<>();
                    String      destStateId         = getJunctionTransitDestId(transNode);
                    JsonNode    destStateNode       = this.stateIdToNode.get(destStateId);     
                    String      destStateName       = getStatePath(destStateNode);
                                        
                    // Add the condition actions
                    if(!condActEqs.isEmpty()) {
                        allActEqs.addAll(condActEqs);
                    }
                    // Add the exit equations
                    if(!exitExprs.isEmpty()) {
                        allActEqs.addAll(exitExprs);
                    }
                    // Add the transition actions
                    if(!transitActEqs.isEmpty()) {
                        allActEqs.addAll(transitActEqs);
                    }                                        
                    // Add the entry expression of the destination state
                    if(destStateNode.has(ACTIONS) && destStateNode.get(ACTIONS).has(ENTRY)) {
                        String entryExprStr = getActionStrEntryExpr(destStateNode.get(ACTIONS));

                        if(entryExprStr != null) {
                            for(LustreAst ast : parseAndTranslate(entryExprStr)) {
                                allActEqs.add((LustreEq) ast);
                            }                                                                         
                        }                       
                    }                    
                    // Set the next active state ID
                    allActEqs.add(new LustreEq(nextActState, new IntExpr(this.stateIdToActId.get(getStateId(destStateNode)))));                    
                    
                    // Build a new state for this transition
                    String transitionName = J2LUtils.getFreshVarName(getStatePath(startStateNode) + "_" + EXIT + "_" + destStateName + "_" + CENTRY);                                        
                    transitNameToActs.put(transitionName, allActEqs);
                    transitNameToCond.put(transitionName, J2LUtils.andExprs(condExprs));
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

        for(JsonNode fcnNode : this.functions.values()) {
            String fcnName = getName(fcnNode);
            String fcnPath = getPath(fcnNode);
            Map<String, LustreType> inputs     = new HashMap<>();
            Map<String, LustreType> outputs    = new HashMap<>();
            Map<String, LustreType> locals     = new HashMap<>();
            List<LustreVar> fcnLocals    = new ArrayList<>();
            List<LustreVar> fcnInputs    = new ArrayList<>();
            List<LustreVar> fcnOutputs   = new ArrayList<>();
            List<LustreEq>  fcnBody      = new ArrayList<>();
            
            List<LustreEq>      noGuardActExprs = new ArrayList<>();
            LinkedHashMap<LustreExpr, List<LustreEq>> condToCondActs = new LinkedHashMap<>();                

            // Add the mapping between name and path to the map
            this.fcnNameToPath.put(fcnName, fcnPath);
            
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
                convertToSSAForm(noGuardActExprs, inputs, outputs, locals, fcnLocals, new HashMap<String, String>());
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
            fcns.add(new LustreNode(fcnPath, fcnInputs, fcnFinalOutputs, fcnLocals, fcnBody));
        }        
        return fcns;
    }
    
    protected void convertToSSAForm(List<LustreEq> equations, Map<String, LustreType> inputs, Map<String, LustreType> outputs, Map<String, LustreType> locals, List<LustreVar> fcnLocals, Map<String, String> outVarIdMap) {                
        Map<String, String> varToLastName = new HashMap<>();
        
        for(int i = 0; i < equations.size(); ++i) {
            LustreExpr          rhs = equations.get(i).getRhs();
            List<LustreExpr>    lhs = equations.get(i).getLhs();
            
            // Replace the right hand side expression variables            
            replaceRhsVars(i, rhs, varToLastName, new HashMap<String, LustreType>(), outVarIdMap);
            
            // Replace the first occurence of an output variable with pre(outputVar)
            if(i == 0) {
                Map<String, LustreType> subVarToType = new HashMap<>();
                subVarToType.putAll(locals);
                subVarToType.putAll(outputs);
                replaceRhsVars(0, rhs, varToLastName, subVarToType, outVarIdMap);
            }              
            
            // Replace variables in the lhs expression with new variables
            for(LustreExpr lhsExpr : lhs) {
                if(lhsExpr instanceof VarIdExpr) {
                    String lhsExprName = ((VarIdExpr)lhsExpr).id;
                    
                    if(inputs.containsKey(lhsExprName) || outputs.containsKey(lhsExprName)) {
                        LustreType type;
                        String newVarName = J2LUtils.getFreshVarName(lhsExprName);                        
                        
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
        // Add the equations between output variables and the last substitute variables
        for(Map.Entry<String, String> entry : varToLastName.entrySet()) {
            if(outputs.containsKey(entry.getKey())) {
                equations.add(new LustreEq(new VarIdExpr(entry.getKey()), new VarIdExpr(entry.getValue())));
            }
        }
    }
    
    protected void replaceRhsVars(int i, LustreExpr expr, Map<String, String> varToLastName, Map<String, LustreType> outputs, Map<String, String> outVarIdMap) {
        if(expr instanceof BinaryExpr) {
            replaceRhsVars(i, ((BinaryExpr)expr).left, varToLastName, outputs, outVarIdMap);
            replaceRhsVars(i, ((BinaryExpr)expr).right, varToLastName, outputs, outVarIdMap);
        } else if(expr instanceof UnaryExpr) {
            replaceRhsVars(i, ((UnaryExpr)expr).expr, varToLastName, outputs, outVarIdMap);
        } else if(expr instanceof IteExpr) {
            replaceRhsVars(i, ((IteExpr)expr).ifExpr, varToLastName, outputs, outVarIdMap);
            replaceRhsVars(i, ((IteExpr)expr).thenExpr, varToLastName, outputs, outVarIdMap);
            replaceRhsVars(i, ((IteExpr)expr).elseExpr, varToLastName, outputs, outVarIdMap);
        } else if(expr instanceof VarIdExpr) {
            String varName = ((VarIdExpr)expr).id;
            
            if(i != 0) {
                if(varToLastName.containsKey(varName)) {
                    ((VarIdExpr)expr).setVarId(varToLastName.get(varName));
                }             
            } else if(outVarIdMap.containsKey(varName)){
                ((VarIdExpr)expr).setVarId(outVarIdMap.get(varName));
            } else {
                LOGGER.log(Level.SEVERE, "Unhandled case: do not know which variable to replace");
            }
        } else if(expr instanceof NodeCallExpr) {
            if(this.fcnNameToPath.containsKey(((NodeCallExpr)expr).nodeName)) {
                ((NodeCallExpr)expr).setNodeName(this.fcnNameToPath.get(((NodeCallExpr)expr).nodeName));
            }
            for(LustreExpr e : ((NodeCallExpr)expr).parameters) {
                replaceRhsVars(i, e, varToLastName, outputs, outVarIdMap);
            }
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
    
    
    protected String getJunctionTransitDestName(JsonNode transitNode) {
        String name = null;
        
        if(transitNode.has(DEST)) {
            if(transitNode.get(DEST).has(DEST)) {
                String strName = transitNode.get(DEST).get(DEST).get(NAME).asText();

                if(strName != null && !strName.equals("")) {
                    name = J2LUtils.sanitizeName(strName);
                } else {
                    LOGGER.log(Level.SEVERE, "Destination transition node has no name!");
                }                
            }
        }
        return name;
    }    

    protected String getStateTransitDestId(JsonNode transitNode) {
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
    
    protected String getJunctionTransitDestId(JsonNode transitNode) {
        String id = null;
        if(transitNode.has(DEST)) {
            if(transitNode.get(DEST).has(DEST)) {
                String strId = transitNode.get(DEST).get(DEST).get(ID).asText();
                if(strId != null && !strId.equals("")) {
                    id = strId;
                }                                 
            }
        }
        return id;
    }    
    
    protected String getStateTransitionType(JsonNode transitNode) {
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
    
    protected String getCondActForTransition(JsonNode transitNode) {
        String condActs = null;
        if(transitNode.has(DEST)) {
            if(transitNode.get(DEST).has(CONDITIONACT)) {
                String strCondAct = transitNode.get(DEST).get(CONDITIONACT).asText();
                
                if(strCondAct != null && !strCondAct.equals("")) {
                    condActs = strCondAct;
                }                       
            }                             
            
        }    
        return condActs;
    }     
    
    protected String getTransitActForTransition(JsonNode transitNode) {
        String transitActs = null;
        if(transitNode.has(DEST)) {
            if(transitNode.get(DEST).has(TRANSITACT)) {
                String strTransitActs = transitNode.get(DEST).get(TRANSITACT).asText();
                
                if(strTransitActs != null && !strTransitActs.equals("")) {
                    transitActs = strTransitActs;
                }                             
            }
        }    
        return transitActs;            
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
        return J2LUtils.sanitizeName(node.get(NAME).asText());
    }
    
    protected String getPath(JsonNode node) {
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
    
    protected String getStatePath(JsonNode stateNode) {
        String name = null;
        if(stateNode.has(PATH)) {
            String strName = stateNode.get(PATH).asText();
            if(strName != null && !strName.equals("")) {
                name = J2LUtils.sanitizeName(strName);
            } else {
                LOGGER.log(Level.SEVERE, "Junction does not have a path!");
            }                   
        }
        return name;
    }
    
    protected String getStateName(JsonNode stateNode) {
        String name = null;
        if(stateNode.has(NAME)) {
            String strName = stateNode.get(NAME).asText();
            if(strName != null && !strName.equals("")) {
                name = J2LUtils.sanitizeName(strName);
            } else {
                LOGGER.log(Level.SEVERE, "Junction does not have a path!");
            }                   
        }
        return name;
    }    
    
    protected String getJunctionPath(JsonNode junctionNode) {
        String name = null;
        if(junctionNode.has(PATH)) {
            String strName = junctionNode.get(PATH).asText();
            
            if(strName != null && !strName.equals("")) {
                name = J2LUtils.sanitizeName(strName);
            } else {
                LOGGER.log(Level.SEVERE, "Junction does not have a path!");
            }                
        }
        return name;
    }    
    
    protected String getJunctionName(JsonNode junctionNode) {
        String name = null;
        if(junctionNode.has(NAME)) {
            String strName = junctionNode.get(NAME).asText();
            if(strName != null && !strName.equals("")) {
                name = J2LUtils.sanitizeName(strName);
            } else {
                LOGGER.log(Level.SEVERE, "Junction does not have a name!");
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
                        // If the state is the entry state, we collect all the information we need here
                        if(internalNode.has(COMPOSITION) && internalNode.get(COMPOSITION).has(SUBSTATES) 
                                && internalNode.get(COMPOSITION).get(SUBSTATES).isArray() && internalNode.get(COMPOSITION).get(SUBSTATES).size() > 1) {

                            JsonNode substateNodeIds = internalNode.get(COMPOSITION).get(STATES);
                            Iterator<JsonNode> elementIt = substateNodeIds.elements();
                            
                            while(elementIt.hasNext()) {
                                JsonNode element = elementIt.next();
                                this.stateIdToActId.put(element.asText(), element.asInt());
                            }
                            this.centerStateId  = getStateId(internalNode);  
                            String initId       = internalNode.get(COMPOSITION).get(DEFAULTTRANS).get(DEST).get(ID).asText();
                            this.initStateId    = "0";
                            this.stateIdToActId.put(initId, 0);
                            
                        }
                        addToMap(getStateId(internalNode), internalNode, this.stateIdToNode);
                        break;
                    }
                    case EVENTS: {
                        addToMap(getStateId(internalNode), internalNode, events);
                        break;
                    }
                    case JUNCTIONS: {
                        addToMap(getStateId(internalNode), internalNode, this.junctionIdToNode);
                        break;
                    } 
                    case GRAPHFCN: {
                        addToMap(getStateId(internalNode), internalNode, functions);
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
    
    public void addToMap(String key, JsonNode value, Map<String, JsonNode> map) {
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
