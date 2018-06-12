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
import edu.uiowa.json2lus.lustreAst.RealExpr;
import edu.uiowa.json2lus.lustreAst.TupleExpr;
import edu.uiowa.json2lus.lustreAst.UnaryExpr;
import edu.uiowa.json2lus.lustreAst.VarIdExpr;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Paul Meng
 */
public class Sf2LTranslator {

    private static final Logger LOGGER = Logger.getLogger(Sf2LTranslator.class.getName());

    /**
     * State flow language features
     */
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

    private final String INITSTATE      = "init_state";
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
    private final String EXCLUSIVEOR    = "EXCLUSIVE_OR";

    /**
     * Truth table related
     */
    private final String LABEL          = "Label";
    private final String INDEX          = "Index";
    private final String ACTION         = "Action";
    private final String DECISIONS      = "Decisions";
    private final String TRUTHTABLE     = "TruthTables";
    private final String CONDITIONS     = "Conditions";
    private final String CONDITIONVAL   = "ConditionValue";

    /**
     * Data structures
     */
    String                      startInitStateName;    
    int                         startStateActiveId;
    String                      centerStateId;
    JsonNode                    centerNode;
    String                      rootSubsysName;
    List<JsonNode>              truthTableNodes;
    List<JsonNode>              locals;
    List<LustreNode>            auxNodes;
    Map<String, Integer>        stateIdToActId;
    List<AutomatonState>        automatonStates;
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
    Map<String, List<String>>   stateIdToOrSubstateIds;
    Map<String, List<String>>   stateIdToAndSubstateIds;

    public Sf2LTranslator() {
        this.startStateActiveId         = 0;
        this.centerNode                 = null;
        this.rootSubsysName             = null;        
        this.centerStateId              = null;
        this.truthTableNodes            = new ArrayList<>();
        this.automatonStates            = new ArrayList<>();
        this.varToInitVal               = new HashMap<>();
        this.stateIdToNode              = new HashMap<>();
        this.junctionIdToNode           = new HashMap<>();
        this.functions                  = new HashMap<>();
        this.events                     = new HashMap<>();
        this.junctIdToExpr              = new HashMap<>();
        this.inputs                     = new HashMap<>();
        this.inPortToId                 = new HashMap<>();
        this.outPortToId                = new HashMap<>();
        this.outputs                    = new HashMap<>();
        this.fcnNameToPath              = new HashMap<>();
        this.locals                     = new ArrayList<>();
        this.auxNodes                   = new ArrayList<>();
        this.stateIdToActId             = new HashMap<>();
        this.stateIdToOrSubstateIds     = new HashMap<>();
        this.stateIdToAndSubstateIds    = new HashMap<>();
    }

    /**
     * Translate the state flow content of subsystem node
     *
     * @param subsystemNode
     * @return
     */
    public List<LustreAst> execute(JsonNode subsystemNode) {
        this.rootSubsysName = J2LUtils.getSanitizedBlkPath(subsystemNode);

        LOGGER.log(Level.INFO, "**************************************** Start translating stateflow chart: {0} ****************************************", this.rootSubsysName);

        List<LustreAst> resultAsts = new ArrayList<>();
        // Collect information about states, junctions, and functions
        collectInfo(subsystemNode);

        // Translate functions
        resultAsts.addAll(translateFunctions());

        // Start translating states and junctions       
        resultAsts.addAll(translateStateFlow(subsystemNode));

        LOGGER.log(Level.INFO, "**************************************** Done ****************************************");
        return resultAsts;
    }

    /**
     * @param subsystemNode
     */
    public void collectInfo(JsonNode subsystemNode) {
        // Collect information about states, junctions, and functions
        JsonNode automatonNode = subsystemNode.get(SFCONTENT);
        Iterator<Map.Entry<String, JsonNode>> chartFields = automatonNode.fields();

        while (chartFields.hasNext()) {
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
                    collectStateFlowInfo(field.getValue(), STATES);
                    break;
                }
                case JUNCTIONS: {
                    collectStateFlowInfo(field.getValue(), JUNCTIONS);
                    break;
                }
                case GRAPHFCN: {
                    collectStateFlowInfo(field.getValue(), GRAPHFCN);
                    break;
                }
                case TRUTHTABLE: {
                    JsonNode ttNode = field.getValue();
                    if (ttNode.isArray()) {
                        Iterator<JsonNode> ttNodeIt = ttNode.elements();
                        while (ttNodeIt.hasNext()) {
                            this.truthTableNodes.add(ttNodeIt.next());
                        }
                    } else {
                        this.truthTableNodes.add(ttNode);
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    /**
     * @param subsystemNode
     * @return
     */
    public List<LustreAst> translateStateFlow(JsonNode subsystemNode) {        
        // Collect information about states, junctions, and functions
        JsonNode automatonNode = subsystemNode.get(SFCONTENT);
        
        // Set the center state node
        this.centerNode = this.stateIdToNode.get(this.centerStateId);

        // Start translating states       
        List<LustreExpr>    strongTransitExprs      = new ArrayList<>();
        VarIdExpr           curActStateVarId        = new VarIdExpr(ACTSTATEID);
        VarIdExpr           nxtActStateVarIdExpr    = new VarIdExpr(NXTACTSTATEID);
        String              automatonName           = getSanitizedPath(automatonNode);
        String              centerStateName         = getSanitizedStatePath(this.centerNode);
        
        // Final results
        List<LustreAst> resultAsts = new ArrayList<>();
        
        // Create node inputs, locals and outputs
        List<LustreEq>  nodeBodyEqs     = new ArrayList<>();        
        List<LustreVar> finalInputs     = new ArrayList<>();
        List<LustreVar> finalOutputs    = new ArrayList<>();
        List<LustreVar> finalLocals     = new ArrayList<>();
        List<LustreVar> decledLocals    = new ArrayList<>();
        List<LustreVar> decledOutputs   = new ArrayList<>();

        Map<String, LustreType> inputNameToType             = new HashMap<>();
        Map<String, LustreType> localVarNameToType          = new HashMap<>();
        Map<String, LustreType> outputNameToType            = new HashMap<>();
        Map<String, String>     outVarToPreIdMap            = new HashMap<>();
        Map<String, String>     preOutVarToOutVarMap        = new HashMap<>();
        Map<String, String>     preLocalVarToLocalVarMap    = new HashMap<>();
        Map<String, LustreType> preOutVarToTypeMap          = new HashMap<>();
        Map<String, LustreType> preLocalVarToTypeMap        = new HashMap<>();
        Map<String, LustreExpr> varToPreVarExprMap          = new HashMap<>();

        // Create the input variable activeStateId
        finalLocals.add(new LustreVar(curActStateVarId.id, PrimitiveType.INT));
        nodeBodyEqs.add(new LustreEq(curActStateVarId, new BinaryExpr(new IntExpr(0), BinaryExpr.Op.ARROW, new UnaryExpr(UnaryExpr.Op.PRE, nxtActStateVarIdExpr))));

        for (int i = 1; i <= this.inputs.size(); ++i) {
            JsonNode    inputNode   = this.inputs.get(this.inPortToId.get(i));
            String      inputName   = inputNode.get(NAME).asText();
            LustreType  inputType   = J2LUtils.getLustreTypeFromStrRep(inputNode.get(COMPILEDTYPE).asText());
            
            finalInputs.add(new LustreVar(inputName, inputType));
            inputNameToType.put(inputName, inputType);
        }
        for (JsonNode localNode : this.locals) {
            String      localName       = localNode.get(NAME).asText();
            LustreType  localType       = J2LUtils.getLustreTypeFromStrRep(localNode.get(COMPILEDTYPE).asText());
            LustreVar   localVar        = new LustreVar(localName, localType);
            LustreVar   preLocalVar     = new LustreVar(J2LUtils.getFreshVarAtInst(localName, 1), localType);            
            
            finalLocals.add(localVar);
            decledLocals.add(localVar);
            localVarNameToType.put(localName, localType);
            preLocalVarToLocalVarMap.put(preLocalVar.name, localName);
            preLocalVarToTypeMap.put(preLocalVar.name, localType);
        }
        for (int i = 1; i <= this.outputs.size(); ++i) {
            JsonNode    outputNode  = this.outputs.get(this.outPortToId.get(i));
            String      outputName  = outputNode.get(NAME).asText();
            LustreType  outputType  = J2LUtils.getLustreTypeFromStrRep(outputNode.get(COMPILEDTYPE).asText());
            LustreVar   preOutVar   = new LustreVar(J2LUtils.getFreshVarAtInst(outputName, 1), outputType);
            LustreVar   outputVar   = new LustreVar(outputName, outputType);
            
            decledOutputs.add(outputVar);
            finalOutputs.add(outputVar);
            outputNameToType.put(outputName, outputType);
            outVarToPreIdMap.put(outputName, preOutVar.name);
            preOutVarToTypeMap.put(preOutVar.name, outputType);
            preOutVarToOutVarMap.put(preOutVar.name, outputName);
        }
        
        // Add the equations between output variables and its pre(output)
        for(Map.Entry<String, LustreType> preOutVarToType :  preOutVarToTypeMap.entrySet()) {            
            String      preOutVarName   = preOutVarToType.getKey();
            LustreExpr  preOutVarExpr   = new VarIdExpr(preOutVarName);
            LustreType  type            = preOutVarToType.getValue();
            LustreExpr  initValue       = J2LUtils.getInitValueForType(type);
            
            finalLocals.add(new LustreVar(preOutVarName, type));
            varToPreVarExprMap.put(preOutVarToOutVarMap.get(preOutVarName), preOutVarExpr);
            nodeBodyEqs.add(new LustreEq(preOutVarExpr, new BinaryExpr(initValue, BinaryExpr.Op.ARROW, new UnaryExpr(UnaryExpr.Op.PRE, new VarIdExpr(preOutVarToOutVarMap.get(preOutVarName))))));
        }
        
        // Add the equations between local variables and its pre(local)
        for(Map.Entry<String, LustreType> preLocalVarToType :  preLocalVarToTypeMap.entrySet()) {            
            String      preLocalVarName = preLocalVarToType.getKey();
            LustreExpr  preLocalVarExpr = new VarIdExpr(preLocalVarName);
            LustreType  type            = preLocalVarToType.getValue();
            LustreExpr  initValue       = J2LUtils.getInitValueForType(type);
            
            finalLocals.add(new LustreVar(preLocalVarName, type));       
            varToPreVarExprMap.put(preLocalVarToLocalVarMap.get(preLocalVarName), preLocalVarExpr);
            nodeBodyEqs.add(new LustreEq(preLocalVarExpr, new BinaryExpr(initValue, BinaryExpr.Op.ARROW, new UnaryExpr(UnaryExpr.Op.PRE, new VarIdExpr(preLocalVarToLocalVarMap.get(preLocalVarName))))));
        }        

        // Create the output variable nextActiveStateId as the last output
        LustreVar nextActiveStateIdVar = new LustreVar(nxtActStateVarIdExpr.id, PrimitiveType.INT);
        finalLocals.add(nextActiveStateIdVar);
        decledLocals.add(nextActiveStateIdVar);
        varToPreVarExprMap.put(nextActiveStateIdVar.name, curActStateVarId);
        
        // The weak transition expression that every state need to execute
        LustreExpr weakTransitExpr = new AutomatonIteExpr(new BooleanExpr(true), new VarIdExpr(centerStateName), null);

        // Storing information about transitions to junctions
        LinkedHashMap<String, LustreExpr>       transitNameToCond       = new LinkedHashMap<>();
        LinkedHashMap<String, List<LustreEq>>   transitNameToAllActs    = new LinkedHashMap<>();

        // Translating states except for the center state
        for (Map.Entry<String, JsonNode> stateIdNode : this.stateIdToNode.entrySet()) {            
            String      stateId     = stateIdNode.getKey();
            JsonNode    stateNode   = stateIdNode.getValue();            

            // Skip the center state node
            if (stateId.equals(this.centerStateId)) {
                continue;
            }
            
            List<LustreEq>  stateEqs    = new ArrayList<>();
            List<LustreEq>  entryExprs  = new ArrayList<>();
            List<LustreEq>  duringExprs = new ArrayList<>();
            List<LustreEq>  exitExprs   = new ArrayList<>();
            String          stateName   = getSanitizedStatePath(stateNode);            
            LustreExpr      strongExpr  = new BinaryExpr(curActStateVarId, BinaryExpr.Op.EQ, new IntExpr(this.stateIdToActId.get(stateId)));
            JsonNode        actionNode  = stateNode.get(ACTIONS);

            String entryStr = getEntryActionInStr(actionNode);
            String durStr   = getDuringActionInStr(actionNode);
            String exitStr  = getExitActionInStr(actionNode);

            // Add entry expressions
            if (this.stateIdToActId.get(stateId) == this.startStateActiveId) {
                List<LustreEq>  startInitStateEqs = new ArrayList<>();                
                
                if (entryStr != null) {
                    for (LustreAst ast : J2LUtils.parseAndTranslate(entryStr)) {
                        LustreEq eq = (LustreEq) ast;

                        if (eq.getLhs().size() == 1) {
                            this.varToInitVal.put(((VarIdExpr) eq.getLhs().get(0)).id, eq.getRhs());
                        } else {
                            LOGGER.log(Level.SEVERE, "Unhandled case: the left-hand side expression has multiple variables");
                        }
                        entryExprs.add(eq);
                    }
                    // Add the entry expressions to the start state equations
                    startInitStateEqs.addAll(entryExprs);
                }
                
                // Set the next active state id to the start state id
                startInitStateEqs.add(new LustreEq(nxtActStateVarIdExpr, new IntExpr(this.stateIdToActId.get(stateId))));
                
                this.startInitStateName = stateName + "_" + INITSTATE;
                                
                // Add the start init state id
                this.stateIdToActId.put("0", 0);
                
                // Add to the automaton states
                this.automatonStates.add(new AutomatonState(this.startInitStateName, startInitStateEqs, weakTransitExpr));                
            }
            // Add during expressions            
            if (durStr != null) {
                parseAndAddToEqList(durStr, duringExprs);
            }
            // Add exit expressions
            if (exitStr != null) {
                parseAndAddToEqList(exitStr, exitExprs);
            }

            // Add the during expression to the body of the state
            if (!duringExprs.isEmpty()) {
                stateEqs.addAll(duringExprs);
            }

            // Start translating the outer transitions                                             
            List<JsonNode> stateOuterTransits = getStateOuterTransitions(stateNode);

            for (int i = 0; i < stateOuterTransits.size(); ++i) {
                List<LustreExpr>    condExprs       = new ArrayList<>();
                List<LustreEq>      transitActEqs   = new ArrayList<>();
                List<LustreEq>      condActEqs      = new ArrayList<>();

                JsonNode    stateOutTransitNode = stateOuterTransits.get(i);
                String      transitionType      = getStateTransitionType(stateOutTransitNode);
                String      junctOrStateId      = getStateTransitDestId(stateOutTransitNode);
                String      conditionStr        = getCondForStateTransition(stateOutTransitNode);
                String      condActStrs         = getCondActForStateTransition(stateOutTransitNode);
                String      transitActStrs      = getTransitActForStateTransition(stateOutTransitNode);

                // Parse the condition expression
                if (conditionStr != null) {
                    parseAndAddToExprList(conditionStr, condExprs);                    
                }
                // Parse the condition action expression             
                if (condActStrs != null) {
                    parseAndAddToEqList(condActStrs, condActEqs);
                }
                // Parse the transition action expression
                if (transitActStrs != null) {
                    parseAndAddToEqList(transitActStrs, transitActEqs);
                }

                // Translate transitions
                switch (transitionType) {
                    case JUNCTION: {
                        condExprs.add(strongExpr);
                        translateJunctionTransition(nxtActStateVarIdExpr, stateNode, junctOrStateId, condExprs, condActEqs, exitExprs, transitActEqs, transitNameToAllActs, transitNameToCond);
                        break;
                    }
                    case STATE: {
                        if (this.stateIdToNode.containsKey(junctOrStateId)) {
                            List<LustreEq>      transitEqs          = new ArrayList<>();
                            List<LustreExpr>    newConditionExprs   = new ArrayList<>();                            
                            int                 actStateId          = this.stateIdToActId.get(junctOrStateId);
                            JsonNode            destNode            = this.stateIdToNode.get(junctOrStateId);
                            String              destStateName       = getSanitizedPath(destNode);
                            String              transitToStateName  = stateName + "_" + EXIT + "_" + destStateName + "_" + CENTRY;;

                            // Add the condition expressions
                            if (!condExprs.isEmpty()) {
                                newConditionExprs.addAll(condExprs);
                            }
                            // Add the condition action expressions
                            if (!condActEqs.isEmpty()) {
                                transitEqs.addAll(condActEqs);
                            }
                            // Add the exit actions
                            if (!exitExprs.isEmpty()) {
                                transitEqs.addAll(exitExprs);
                            }
                            // Add the transition action expressions
                            if (!transitActEqs.isEmpty()) {
                                transitEqs.addAll(transitActEqs);
                            }

                            // Get the entry expression of the destination state
                            if (destNode.has(ACTIONS) && destNode.get(ACTIONS).has(ENTRY)) {
                                String entryExprStr = getEntryActionInStr(destNode.get(ACTIONS));

                                if (entryExprStr != null) {
                                    parseAndAddToEqList(entryExprStr, transitEqs);
                                }
                            }
                            // Set the next active state
                            transitEqs.add(new LustreEq(nxtActStateVarIdExpr, new IntExpr(actStateId)));

                            // Create strong transition expression
                            newConditionExprs.add(strongExpr);
                            
                            for(LustreExpr toBeSubCondExpr : newConditionExprs) {
                                substituteOutVarWithPreVar(toBeSubCondExpr, outVarToPreIdMap);
                            }
                            // Add the strong transition expression to the global 
                            strongTransitExprs.add(new AutomatonIteExpr(J2LUtils.andExprs(newConditionExprs), new VarIdExpr(transitToStateName), null));

                            // Save the transition state in the autoStates
                            this.automatonStates.add(new AutomatonState(transitToStateName, transitEqs, weakTransitExpr));
                        } else {
                            LOGGER.log(Level.SEVERE, "No state has ID: {0}", junctOrStateId);
                        }
                        break;
                    }
                    default:
                        break;

                }
            }
            
            // Add to the automaton states
            this.automatonStates.add(new AutomatonState(stateName, stateEqs, weakTransitExpr));
        }

        // Create SSA form for each state's equations        
        for (AutomatonState state : this.automatonStates) {
            convertToSSAForm(state.equations, inputNameToType, outputNameToType, localVarNameToType, state.locals, outVarToPreIdMap);
                        
            // Add equations assigning unassgined variables
            addUnassignedVarEqs(state.equations, decledLocals, decledOutputs, varToPreVarExprMap);
        }

        // Create automaton states for transitions to junctions
        for (Map.Entry<String, LustreExpr> transitNameToCondEntry : transitNameToCond.entrySet()) {
            String      transitName      = transitNameToCondEntry.getKey();
            LustreExpr  transitCondExpr  = transitNameToCondEntry.getValue();
            
            // Substitute out variable with out variable at instant 1
            substituteOutVarWithPreVar(transitCondExpr, outVarToPreIdMap);

            if (transitNameToAllActs.containsKey(transitName) && !transitNameToAllActs.get(transitName).isEmpty()) {
                // Add the transition condition to the strong transition condition
                strongTransitExprs.add(new AutomatonIteExpr(transitCondExpr, new VarIdExpr(transitName), null));

                List<LustreVar> transitStateLocals  = new ArrayList<>();
                List<LustreEq>  allActEqs           = transitNameToAllActs.get(transitName);
                

                // Convert to SSA form
                convertToSSAForm(allActEqs, inputNameToType, outputNameToType, localVarNameToType, transitStateLocals, outVarToPreIdMap);
                
                // Add equations to unassigned variables
                addUnassignedVarEqs(allActEqs, decledLocals, decledOutputs, varToPreVarExprMap);
                
                this.automatonStates.add(new AutomatonState(transitName, transitStateLocals, allActEqs, weakTransitExpr));
            }
        }

        // Set the default strong transition conditions to states        
        for (Map.Entry<String, Integer> stateIdToActIdEntry : this.stateIdToActId.entrySet()) {
            String  stateId          = stateIdToActIdEntry.getKey();
            int     stateActiveId    = stateIdToActIdEntry.getValue();
            
            if (stateActiveId == 0) {
                strongTransitExprs.add(0, new AutomatonIteExpr(new BinaryExpr(curActStateVarId, BinaryExpr.Op.EQ, new IntExpr(stateActiveId)), new VarIdExpr(this.startInitStateName), null));
            } else {
                strongTransitExprs.add(new AutomatonIteExpr(new BinaryExpr(curActStateVarId, BinaryExpr.Op.EQ, new IntExpr(stateActiveId)), new VarIdExpr(getSanitizedStatePath(this.stateIdToNode.get(stateId))), null));
            }
        }

        // Create the initial center state
        this.automatonStates.add(new AutomatonState(true, centerStateName, new ArrayList<LustreVar>(), strongTransitExprs, new ArrayList<LustreEq>(), new ArrayList<LustreExpr>()));

        resultAsts.add(new LustreNode(this.rootSubsysName, new LustreAutomaton(automatonName, this.automatonStates), finalInputs, finalOutputs, finalLocals, nodeBodyEqs));
        LOGGER.log(Level.INFO, "******************** Done ********************");
        return resultAsts;
    }
    
    
    /**
     * @param stateEqs
     * @param localVars
     * @param outputVars
     * @param varToPreVarExprMap
     */
    protected void addUnassignedVarEqs(List<LustreEq> stateEqs, List<LustreVar> localVars, List<LustreVar> outputVars, Map<String, LustreExpr> varToPreVarExprMap) {        
        Set<String> assignedVarNames = new HashSet();
        
        for(LustreEq eq : stateEqs) {
            List<LustreExpr> lhsExprs = eq.getLhs();
            
            if(lhsExprs.size() == 1) {
                LustreExpr lhs = lhsExprs.get(0);
                
                if(lhs instanceof VarIdExpr) {
                    assignedVarNames.add(((VarIdExpr) lhs).id);
                } else if(lhs instanceof TupleExpr) {
                    LOGGER.log(Level.SEVERE, "Unsupported lhs tuple expressions!");
                }
            } else {
                LOGGER.log(Level.SEVERE, "Unsupported lhs expressions!");
            }
        }
        
        // Check if there is any variables in the output are not assigned
        for(LustreVar outputVar : outputVars) {
            String outputVarName  = outputVar.name;
            if(!assignedVarNames.contains(outputVarName)) {               
                if(varToPreVarExprMap.containsKey(outputVarName)) {
                    LustreExpr lhsExpr  = new VarIdExpr(outputVarName);
                    stateEqs.add(new LustreEq(Arrays.asList(lhsExpr), varToPreVarExprMap.get(outputVarName)));                    
                }
            }
        }
        
        // Check if there is any variables in the output are not assigned
        for(LustreVar localVar : localVars) {
            String localVarName  = localVar.name;
            if(!assignedVarNames.contains(localVarName)) {
                if(varToPreVarExprMap.containsKey(localVarName)) {
                    LustreExpr lhsExpr  = new VarIdExpr(localVarName);
                    stateEqs.add(new LustreEq(Arrays.asList(lhsExpr), varToPreVarExprMap.get(localVarName)));                    
                }
            }
        }        
    }
    
    /**
     * 
     * @param condExpr
     * @param outVarToPreIdMap
     */
    protected void substituteOutVarWithPreVar(LustreExpr condExpr, Map<String, String> outVarToPreIdMap) {
        if(condExpr instanceof UnaryExpr) {
            substituteOutVarWithPreVar(((UnaryExpr)condExpr).expr, outVarToPreIdMap);
        } else if(condExpr instanceof BinaryExpr) {
            substituteOutVarWithPreVar(((BinaryExpr)condExpr).left, outVarToPreIdMap);
            substituteOutVarWithPreVar(((BinaryExpr)condExpr).right, outVarToPreIdMap);
        } else if(condExpr instanceof VarIdExpr) {
            if(outVarToPreIdMap.containsKey(((VarIdExpr)condExpr).id)) {
                ((VarIdExpr)condExpr).setVarId(outVarToPreIdMap.get(((VarIdExpr)condExpr).id));
            }
        } else {
           LOGGER.log(Level.SEVERE, "Unhandled case: unexpected lustre expression type!"); 
        }
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
        if (this.junctionIdToNode.containsKey(junctionId)) {
            List<LustreExpr>    negPrevCondExprs    = new ArrayList<>();
            JsonNode            junctionNode        = this.junctionIdToNode.get(junctionId);
            String              junctionName        = getSanitizedJunctionPath(junctionNode);
            List<JsonNode>      outerTransitions    = getJunctionOuterTransitions(junctionNode);

            // Handle outer transitions
            for (int i = 0; i < outerTransitions.size(); ++i) {
                JsonNode transNode = outerTransitions.get(i);

                if (transNode.has(DEST)) {
                    String destName         = getJunctionTransitDestName(transNode);
                    String conditionStr     = getCondForJunctionTransition(transNode);
                    String condActStrs      = getCondActForJunctionTransition(transNode);
                    String transitActStrs   = getTransitActForJunctionTransition(transNode);

                    List<LustreExpr>    newCondExprs        = new ArrayList<>();
                    List<LustreEq>      newTransitActEqs    = new ArrayList<>();
                    List<LustreEq>      newCondActEqs       = new ArrayList<>();

                    // Add the previous condition expressions, condition actions, transition actions
                    if (!condExprs.isEmpty()) {
                        newCondExprs.addAll(condExprs);
                    }
                    if (!condActEqs.isEmpty()) {
                        newCondActEqs.addAll(condActEqs);
                    }
                    if (!transitActEqs.isEmpty()) {
                        newTransitActEqs.addAll(transitActEqs);
                    }

                    // Parse the condition expression
                    if (conditionStr != null) {
                        List<LustreAst> parsedCondExprs = J2LUtils.parseAndTranslateStrExpr(conditionStr);
                        if (parsedCondExprs.size() == 1) {
                            LustreExpr condExpr = (LustreExpr) parsedCondExprs.get(0);
                            negPrevCondExprs.add(new UnaryExpr(UnaryExpr.Op.NOT, condExpr));
                            newCondExprs.add(condExpr);
                        } else {
                            LOGGER.log(Level.SEVERE, "Multiple expressions returned from a single expression!");
                        }
                    }
                    // Parse the condition action expression
                    if (condActStrs != null) {
                        parseAndAddToEqList(condActStrs, newCondActEqs);
                    }
                    // Parse the transition action expression
                    if (transitActStrs != null) {
                        parseAndAddToEqList(transitActStrs, newTransitActEqs);
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
            if (!condActEqs.isEmpty()) {
                String transitionName = J2LUtils.mkFreshVarName(getSanitizedStatePath(startStateNode) + "_" + EXIT + "_" + junctionName + "_" + CENTRY);
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
        if (transNode.get(DEST).has(DEST)) {
            String transitType = transNode.get(DEST).get(DEST).get(TYPE).asText();

            switch (transitType) {
                case JUNCTION: {
                    String              junctionName        = getSanitizedJunctionPath(transNode);
                    List<LustreExpr>    negPrevCondExprs    = new ArrayList<>();
                    List<JsonNode>      outerTransitions    = getJunctionOuterTransitions(transNode);

                    for (int i = 0; i < outerTransitions.size(); ++i) {
                        JsonNode outerTransitNode = outerTransitions.get(i);

                        String destJunctName    = getJunctionTransitDestName(outerTransitNode);
                        String condStr          = getCondForJunctionTransition(outerTransitNode);
                        String condActStrs      = getCondActForJunctionTransition(outerTransitNode);
                        String transitActStrs   = getTransitActForJunctionTransition(outerTransitNode);

                        List<LustreEq>      newTransitActEqs    = new ArrayList<>();
                        List<LustreEq>      newCondActEqs       = new ArrayList<>();
                        List<LustreExpr>    newCondExprs        = new ArrayList<>();

                        // Add previous actions and conditions
                        if (!condActEqs.isEmpty()) {
                            newCondActEqs.addAll(condActEqs);
                        }
                        if (!transitActEqs.isEmpty()) {
                            newTransitActEqs.addAll(transitActEqs);
                        }
                        if (!condExprs.isEmpty()) {
                            newCondExprs.addAll(condExprs);
                        }

                        // Parse the current condition expression
                        if (condStr != null) {
                            LustreExpr newCondExpr = (LustreExpr) J2LUtils.parseAndTranslateStrExpr(condStr);
                            negPrevCondExprs.add(new UnaryExpr(UnaryExpr.Op.NOT, newCondExpr));
                            newCondExprs.add(newCondExpr);
                        }
                        // Parse the current condition action expression
                        if (condActStrs != null) {
                            parseAndAddToEqList(condActStrs, newCondActEqs);
                        }
                        // Parse the current transition action expression
                        if (transitActStrs != null) {
                            parseAndAddToEqList(transitActStrs, newTransitActEqs);
                        }
                        // Continue the translation
                        translateJunctionTransition(nextActState, startStateNode, transNode, newTransitActEqs, newCondActEqs, newCondExprs, exitExprs, transitNameToActs, transitNameToCond);

                        // Build the intermediate transitioins
                        String transitionName = J2LUtils.mkFreshVarName(getSanitizedStatePath(startStateNode) + "_" + EXIT + "_" + destJunctName + "_" + CENTRY);
                        newCondActEqs.add(new LustreEq(nextActState, new IntExpr(this.stateIdToActId.get(getNodeId(startStateNode)))));
                        transitNameToActs.put(transitionName, newCondActEqs);
                        transitNameToCond.put(transitionName, J2LUtils.andExprs(condExprs));
                    }
                    // If there are some previous condition action expressions,
                    // we negated all the current conditions and execute the condition
                    // action expressions.
                    if (!condActEqs.isEmpty()) {
                        String transitionName = J2LUtils.mkFreshVarName(getSanitizedStatePath(startStateNode) + "_" + EXIT + "_" + junctionName + "_" + CENTRY);
                        transitNameToActs.put(transitionName, condActEqs);
                        transitNameToCond.put(transitionName, J2LUtils.andExprs(negPrevCondExprs));
                    }
                    break;
                }
                case STATE: {
                    List<LustreEq>  allActEqs       = new ArrayList<>();
                    String          destStateId     = getJunctionTransitDestId(transNode);
                    JsonNode        destStateNode   = this.stateIdToNode.get(destStateId);
                    String          destStateName   = getSanitizedStatePath(destStateNode);

                    // Add the condition actions
                    if (!condActEqs.isEmpty()) {
                        allActEqs.addAll(condActEqs);
                    }
                    // Add the exit equations
                    if (!exitExprs.isEmpty()) {
                        allActEqs.addAll(exitExprs);
                    }
                    // Add the transition actions
                    if (!transitActEqs.isEmpty()) {
                        allActEqs.addAll(transitActEqs);
                    }
                    // Add the entry expression of the destination state
                    if (destStateNode.has(ACTIONS) && destStateNode.get(ACTIONS).has(ENTRY)) {
                        String entryExprStr = getEntryActionInStr(destStateNode.get(ACTIONS));

                        if (entryExprStr != null) {
                            parseAndAddToEqList(entryExprStr, allActEqs);
                        }
                    }
                    // Set the next active state ID
                    allActEqs.add(new LustreEq(nextActState, new IntExpr(this.stateIdToActId.get(getNodeId(destStateNode)))));

                    // Build a new state for this transition
                    String transitionName = J2LUtils.mkFreshVarName(getSanitizedStatePath(startStateNode) + "_" + EXIT + "_" + destStateName + "_" + CENTRY);
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

    /**
     * This function is to translate function and truth table definitions
     *
     * @return
     */
    protected List<LustreNode> translateFunctions() {
        List<LustreNode> fcns = new ArrayList<>();

        // Functions
        for (JsonNode fcnNode : this.functions.values()) {
            String                  fcnName         = getSanitizedName(fcnNode);
            String                  fcnPath         = getSanitizedPath(fcnNode);
            Map<String, LustreType> inputs          = new HashMap<>();
            Map<String, LustreType> outputs         = new HashMap<>();
            Map<String, LustreType> locals          = new HashMap<>();
            List<LustreVar>         fcnLocals       = new ArrayList<>();
            List<LustreVar>         fcnInputs       = new ArrayList<>();
            List<LustreVar>         fcnOutputs      = new ArrayList<>();
            List<LustreEq>          fcnBody         = new ArrayList<>();

            List<LustreEq> noGuardActExprs = new ArrayList<>();
            LinkedHashMap<LustreExpr, List<LustreEq>> condToCondActs = new LinkedHashMap<>();

            // Add the mapping between name and path to the map so that 
            // we can reference it in the nodes
            this.fcnNameToPath.put(fcnName, fcnPath);

            // Get inputs and outputs
            if (fcnNode.has(DATA)) {
                Iterator<JsonNode> dataIt = fcnNode.get(DATA).elements();

                while (dataIt.hasNext()) {
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
            if (fcnNode.has(COMPOSITION) && fcnNode.get(COMPOSITION).has(DEFAULTTRANS)) {
                for (JsonNode defaultTransit : getDefaultTransitions(fcnNode.get(COMPOSITION))) {
                    createFcnBodyDef(defaultTransit, noGuardActExprs, condToCondActs);
                }
            }

            // Create the function body equations
            if (!noGuardActExprs.isEmpty()) {
                convertToSSAForm(noGuardActExprs, inputs, outputs, locals, fcnLocals, new HashMap<String, String>());
                fcnBody.addAll(noGuardActExprs);
            }

            // Create the function body equations
            for (Map.Entry<LustreExpr, List<LustreEq>> map : condToCondActs.entrySet()) {

            }

            fcns.add(new LustreNode(fcnPath, fcnInputs, fcnOutputs, fcnLocals, fcnBody));
        }

        // Start translating truth tables
        for (JsonNode ttNode : this.truthTableNodes) {
            // We do not create SSA form for truth table for now
            String                      fcnName             = getSanitizedName(ttNode);
            String                      fcnPath             = getSanitizedPath(ttNode);            
            List<LustreVar>             fcnLocals           = new ArrayList<>();
            List<LustreVar>             fcnInputs           = new ArrayList<>();
            List<LustreVar>             fcnOutputs          = new ArrayList<>();
            List<LustreEq>              fcnBody             = new ArrayList<>();
            Map<String, List<LustreEq>> labelToActions      = new HashMap<>();
            Map<String, List<LustreEq>> indexToActions      = new HashMap<>();
            Map<LustreExpr, List<LustreEq>> condToActions   = new LinkedHashMap<>();
            
            // Add the mapping between name and path to the map so that 
            // we can reference it in the nodes
            this.fcnNameToPath.put(fcnName, fcnPath);            

            // Get inputs, locals and outputs
            if (ttNode.has(DATA)) {
                Iterator<JsonNode> dataIt = ttNode.get(DATA).elements();

                while (dataIt.hasNext()) {
                    JsonNode dataNode = dataIt.next();
                    String name = dataNode.get(NAME).asText();
                    LustreType type = J2LUtils.getLustreTypeFromStrRep(dataNode.get(COMPILEDTYPE).asText());
                    LustreVar var = new LustreVar(name, type);

                    switch (dataNode.get(SCOPE).asText()) {
                        case INPUT: {
                            fcnInputs.add(var);
                            break;
                        }
                        case OUTPUT: {
                            fcnOutputs.add(var);
                            break;
                        }
                        case LOCAL: {
                            fcnLocals.add(var);
                            break;
                        }
                        default:
                            LOGGER.log(Level.SEVERE, "Unhandled case: the variable is a {0}", dataNode.get(SCOPE).asText());
                            break;
                    }
                }
            }

            // Populate actions
            if (ttNode.has(ACTIONS)) {
                Iterator<JsonNode> actionIt = ttNode.get(ACTIONS).elements();

                while (actionIt.hasNext()) {
                    String label = null;
                    String index = null;
                    JsonNode actionNode = actionIt.next();
                    List<LustreEq> eqs = new ArrayList<>();

                    if (actionNode.has(INDEX) && actionNode.get(INDEX).asText() != null && !actionNode.get(INDEX).asText().equals("")) {
                        index = actionNode.get(INDEX).asText().trim();
                    }
                    if (actionNode.has(LABEL) && actionNode.get(LABEL).asText() != null && !actionNode.get(LABEL).asText().equals("")) {
                        label = actionNode.get(LABEL).asText().trim();
                    }
                    if (actionNode.has(ACTION)) {
                        for (LustreAst eq : J2LUtils.parseAndTranslate(actionNode.get(ACTION).asText())) {
                            if (eq instanceof LustreEq) {
                                eqs.add((LustreEq) eq);
                            }
                        }
                    }
                    if (label != null) {
                        labelToActions.put(label, eqs);
                    }
                    if (index != null) {
                        indexToActions.put(index, eqs);
                    }
                }
            }

            // Get all dicisions
            if (ttNode.has(DECISIONS)) {
                JsonNode decisionsNode = ttNode.get(DECISIONS);

                if (decisionsNode.isArray()) {
                    Iterator<JsonNode> decisionIt = decisionsNode.elements();

                    // Iterate over all decision node
                    while (decisionIt.hasNext()) {
                        populateConditionsAndActions(decisionIt.next(), labelToActions, indexToActions, condToActions);
                    }
                } else {
                    populateConditionsAndActions(decisionsNode, labelToActions, indexToActions, condToActions);
                }
            }
            
            // Get all the keys of condToActions as a list for iteration purpose
            List<LustreExpr> keyList = new ArrayList<>(condToActions.keySet());
            
            // construct if-then-else statement for output variables            
            for(LustreVar outVar : fcnOutputs) {
                LustreExpr  elseAssignment  = null;
                IteExpr     iteExpr         = null;
                String      outVarName      = outVar.name;                

                for(Map.Entry<LustreExpr, List<LustreEq> > condToActsEntry : condToActions.entrySet()) {
                    LustreExpr      condExpr    = condToActsEntry.getKey();
                    List<LustreEq>  actionEqs   = condToActsEntry.getValue();

                    if(isAllTrues(condExpr)) {
                        elseAssignment = getVarAssignment(outVarName, actionEqs);
                    }
                    if(elseAssignment != null) {
                        break;
                    }
                }
                // We assume the last condition expression is the default expression
                int lastNonTrueCondIndex = condToActions.size()-2;
                
                if(lastNonTrueCondIndex >= 0) {
                    LustreExpr lastNonTrueCondExpr  = keyList.get(lastNonTrueCondIndex);
                    LustreExpr lastCondVarAssExpr   = getVarAssignment(outVarName, condToActions.get(lastNonTrueCondExpr));
                    
                    iteExpr = new IteExpr(lastNonTrueCondExpr, lastCondVarAssExpr, elseAssignment);
                    for(int i = condToActions.size()-3; i >= 0; --i) {
                        LustreExpr condExpr             = keyList.get(i);
                        LustreExpr varAssignmentExpr    = getVarAssignment(outVarName, condToActions.get(condExpr));
                        iteExpr = new IteExpr(condExpr, varAssignmentExpr, iteExpr);
                    }                                
                }
                if(iteExpr != null) {
                    fcnBody.add(new LustreEq(new VarIdExpr(outVarName), iteExpr));
                } else {
                    fcnBody.add(new LustreEq(new VarIdExpr(outVarName), elseAssignment));
                }                
            }
            fcns.add(new LustreNode(fcnPath, fcnInputs, fcnOutputs, fcnLocals, fcnBody));            
        }

        return fcns;
    }
    
    protected boolean isAllTrues(LustreExpr condExpr) {
        if(condExpr instanceof BinaryExpr) {
            return isAllTrues(((BinaryExpr) condExpr).left) && isAllTrues(((BinaryExpr) condExpr).right);
        } else if(condExpr instanceof UnaryExpr){
            if(((UnaryExpr) condExpr).op == UnaryExpr.Op.NOT) {
                return !isAllTrues(((UnaryExpr)condExpr).expr);
            }
            return isAllTrues(((UnaryExpr)condExpr).expr);            
        } else if(condExpr instanceof BooleanExpr) {
            return ((BooleanExpr) condExpr).value;
        } else {
            return false;
        }
    }
    
    protected LustreExpr getVarAssignment(String varName, List<LustreEq> actionEqs) {
        LustreExpr  assignmentExpr  = null;
        
        for(LustreEq eq : actionEqs) {
            List<LustreExpr> lhsExprs = eq.getLhs();

            if(lhsExprs.size() == 1) {
                if(lhsExprs.get(0) instanceof VarIdExpr) {
                    if(varName.equals(((VarIdExpr)lhsExprs.get(0)).id)) {
                        assignmentExpr = eq.getRhs();
                        break;
                    }
                }
            }
            if(assignmentExpr != null) {
                break;
            }
        }   
        return assignmentExpr;
    }

    /**
     *
     * @param decisionNode
     * @param labelToActions
     * @param indexToActions
     * @param condToActions
     */
    protected void populateConditionsAndActions(JsonNode decisionNode, Map<String, List<LustreEq>> labelToActions, Map<String, List<LustreEq>> indexToActions, Map<LustreExpr, List<LustreEq>> condToActions) {
        List<LustreExpr> tempConditions = new ArrayList<>();

        // Get all conditions
        if (decisionNode.has(CONDITIONS)) {
            JsonNode        conditionsNode  = decisionNode.get(CONDITIONS);
            List<String>    conditionVals   = new ArrayList<>();

            if (conditionsNode.isArray()) {
                Iterator<JsonNode> conditionsNodeIt = conditionsNode.elements();

                while (conditionsNodeIt.hasNext()) {
                    LustreExpr conditionExpr = null;
                    JsonNode conditionNode = conditionsNodeIt.next();
                    List<LustreAst> asts = J2LUtils.parseAndTranslateStrExpr(conditionNode.get(CONDITION).asText());

                    if (asts.size() == 1) {
                        conditionExpr = (LustreExpr) asts.get(0);
                    } else {
                        LOGGER.log(Level.SEVERE, "Unhandled case: multiple asts returned from a condition expression!!");
                    }
                    // Get the condition values
                    switch (conditionNode.get(CONDITIONVAL).asText()) {
                        case "F": {
                            conditionExpr = new UnaryExpr(UnaryExpr.Op.NOT, conditionExpr);
                            break;
                        }
                        case "-": {
                            conditionExpr = new BooleanExpr(true);
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                    if (conditionExpr != null) {
                        tempConditions.add(conditionExpr);
                    }
                }
            } else {
                LustreExpr conditionExpr = null;
                List<LustreAst> asts = J2LUtils.parseAndTranslateStrExpr(conditionsNode.get(CONDITION).asText());

                if (asts.size() == 1) {
                    conditionExpr = (LustreExpr) asts.get(0);
                } else {
                    LOGGER.log(Level.SEVERE, "Unhandled case: multiple asts returned from a condition expression!!");
                }

                // Get the condition values
                switch (conditionsNode.get(CONDITIONVAL).asText()) {
                    case "F": {
                        conditionExpr = new UnaryExpr(UnaryExpr.Op.NOT, conditionExpr);
                        break;
                    }
                    case "-": {
                        conditionExpr = new BooleanExpr(true);
                        break;
                    }
                    default: {
                        break;
                    }
                }
                if (conditionExpr != null) {
                    tempConditions.add(conditionExpr);
                }
            }
        }

        // Split action strings
        if (decisionNode.has(ACTIONS)) {
            List<LustreEq>  exprs       = new ArrayList<>();
            String          actionStr   = decisionNode.get(ACTIONS).asText();
            String          delimiters  = "[\\t,]";
            String[]        tokens      = actionStr.split(delimiters);

            for (String t : tokens) {
                String trimedKey = t.trim();

                if (labelToActions.containsKey(trimedKey)) {
                    exprs.addAll(labelToActions.get(trimedKey));
                } else if (indexToActions.containsKey(trimedKey)) {
                    exprs.addAll(indexToActions.get(trimedKey));
                } else {
                    LOGGER.log(Level.SEVERE, "Unexpected: no label or index exists for {0}", trimedKey);
                }
            }
            if (!exprs.isEmpty()) {
                condToActions.put(J2LUtils.andExprs(tempConditions), exprs);
            }
        }
    }

    protected void convertToSSAForm(List<LustreEq> equations, Map<String, LustreType> inputs, Map<String, LustreType> outputs, Map<String, LustreType> locals, List<LustreVar> fcnLocals, Map<String, String> outVarIdMap) {
        Map<String, String> varToLastName = new HashMap<>();

        for (int i = 0; i < equations.size(); ++i) {
            LustreExpr          rhs = equations.get(i).getRhs();
            List<LustreExpr>    lhs = equations.get(i).getLhs();

            // Replace the right hand side expression variables            
            replaceRhsVars(i, rhs, varToLastName, new HashMap<String, LustreType>(), outVarIdMap);

            // Replace the first occurence of an output variable with pre(outputVar)
            if (i == 0) {
                Map<String, LustreType> subVarToType = new HashMap<>();
                subVarToType.putAll(locals);
                subVarToType.putAll(outputs);
                replaceRhsVars(0, rhs, varToLastName, subVarToType, outVarIdMap);
            }

            // Replace variables in the lhs expression with new variables
            for (LustreExpr lhsExpr : lhs) {
                if (lhsExpr instanceof VarIdExpr) {
                    String lhsExprName = ((VarIdExpr) lhsExpr).id;

                    if (inputs.containsKey(lhsExprName) || outputs.containsKey(lhsExprName)) {
                        LustreType type;
                        String newVarName = J2LUtils.mkFreshVarName(lhsExprName);

                        if (inputs.containsKey(lhsExprName)) {
                            type = inputs.get(lhsExprName);
                        } else if (outputs.containsKey(lhsExprName)) {
                            type = outputs.get(lhsExprName);
                        } else {
                            type = locals.get(lhsExprName);
                        }

                        ((VarIdExpr) lhsExpr).setVarId(newVarName);
                        varToLastName.put(lhsExprName, newVarName);
                        fcnLocals.add(new LustreVar(newVarName, type));
                    }
                } else if(lhsExpr instanceof TupleExpr){
                    for(int j = 0; j < ((TupleExpr)lhsExpr).elements.size(); ++j) {
                        LustreExpr tupleElement = ((TupleExpr)lhsExpr).elements.get(j);
                        
                        if (tupleElement instanceof VarIdExpr) {
                            String lhsExprName = ((VarIdExpr) tupleElement).id;

                            if (inputs.containsKey(lhsExprName) || outputs.containsKey(lhsExprName)) {
                                LustreType type;
                                String newVarName = J2LUtils.mkFreshVarName(lhsExprName);

                                if (inputs.containsKey(lhsExprName)) {
                                    type = inputs.get(lhsExprName);
                                } else if (outputs.containsKey(lhsExprName)) {
                                    type = outputs.get(lhsExprName);
                                } else {
                                    type = locals.get(lhsExprName);
                                }

                                ((VarIdExpr) tupleElement).setVarId(newVarName);
                                varToLastName.put(lhsExprName, newVarName);
                                fcnLocals.add(new LustreVar(newVarName, type));
                            }
                        } else {
                            LOGGER.log(Level.SEVERE, "Unhandled tuple element case: {0}", tupleElement);
                        }                        
                    }
                } else {
                    LOGGER.log(Level.SEVERE, "Unhandled left hand side expression case: {0}", lhsExpr);
                }
            }
        }
        // Add the equations between output variables and the last substitute variables
        for (Map.Entry<String, String> entry : varToLastName.entrySet()) {
            if (outputs.containsKey(entry.getKey())) {
                equations.add(new LustreEq(new VarIdExpr(entry.getKey()), new VarIdExpr(entry.getValue())));
            }
        }
    }

    protected void replaceRhsVars(int i, LustreExpr expr, Map<String, String> varToLastName, Map<String, LustreType> outputs, Map<String, String> outVarIdMap) {
        if (expr instanceof BinaryExpr) {
            replaceRhsVars(i, ((BinaryExpr) expr).left, varToLastName, outputs, outVarIdMap);
            replaceRhsVars(i, ((BinaryExpr) expr).right, varToLastName, outputs, outVarIdMap);
        } else if (expr instanceof UnaryExpr) {
            replaceRhsVars(i, ((UnaryExpr) expr).expr, varToLastName, outputs, outVarIdMap);
        } else if (expr instanceof IteExpr) {
            replaceRhsVars(i, ((IteExpr) expr).ifExpr, varToLastName, outputs, outVarIdMap);
            replaceRhsVars(i, ((IteExpr) expr).thenExpr, varToLastName, outputs, outVarIdMap);
            replaceRhsVars(i, ((IteExpr) expr).elseExpr, varToLastName, outputs, outVarIdMap);
        } else if (expr instanceof VarIdExpr) {
            String varName = ((VarIdExpr) expr).id;

            if (i != 0) {
                if (varToLastName.containsKey(varName)) {
                    ((VarIdExpr) expr).setVarId(varToLastName.get(varName));
                }
            } else if (outVarIdMap.containsKey(varName)) {
                ((VarIdExpr) expr).setVarId(outVarIdMap.get(varName));
            } else {
                LOGGER.log(Level.WARNING, "Did not replace any variable!");
            }
        } else if (expr instanceof NodeCallExpr) {
            if (this.fcnNameToPath.containsKey(((NodeCallExpr) expr).nodeName)) {
                ((NodeCallExpr) expr).setNodeName(this.fcnNameToPath.get(((NodeCallExpr) expr).nodeName));
            }
            for (LustreExpr e : ((NodeCallExpr) expr).parameters) {
                replaceRhsVars(i, e, varToLastName, outputs, outVarIdMap);
            }
        }
    }

    // You cannot define states in functions, which means you can only make a transition from a junction to another junction
    protected void createFcnBodyDef(JsonNode defaultTransitNode, List<LustreEq> noGuardActExprs, LinkedHashMap<LustreExpr, List<LustreEq>> condToCondTransitAct) {
        String              destJunctionId  = defaultTransitNode.get(DEST).get(ID).asText();
        List<LustreExpr>    condExprs       = new ArrayList<>();
        List<LustreEq>      condActExprs    = new ArrayList<>();
        String              strCond         = getJunctionDefaultTransitCondExprInStr(defaultTransitNode);
        List<String>        strCondActs     = getJunctionDefaultTransitCondActExprInStr(defaultTransitNode);

        // Get condition in string format
        if (strCond != null) {
            condExprs.add((LustreExpr) J2LUtils.parseAndTranslateStrExpr(strCond));
        }

        // Convert condition actions in string to Lustre expressions
        for (String strCondAct : strCondActs) {
            LustreAst       ast = null;
            List<LustreAst> asts = J2LUtils.parseAndTranslateStrExpr(strCondAct);

            if (asts.size() == 1) {
                ast = asts.get(0);
            } else {
                LOGGER.log(Level.SEVERE, "Unhandled case: multiple asts returned from parseAndTranslateStrExpr()!");
            }

            if (ast instanceof LustreEq) {
                condActExprs.add((LustreEq) ast);
            } else {
                LOGGER.log(Level.SEVERE, "Unexpected condition action: {0}", ast);
            }
        }

        if (condExprs.isEmpty()) {
            noGuardActExprs.addAll(condActExprs);
        } else {
            List<LustreEq>  allEqs      = new ArrayList<>();
            LustreExpr      condExpr    = J2LUtils.andExprs(condExprs);
            allEqs.addAll(condActExprs);
            condToCondTransitAct.put(condExpr, allEqs);
        }
        createFcnDef(destJunctionId, condExprs, noGuardActExprs, condToCondTransitAct);
    }

    protected void parseAndAddToEqList(String strExpr, List<LustreEq> eqs) {
        if (strExpr != null) {
            for (LustreAst ast : J2LUtils.parseAndTranslate(strExpr)) {
                if (ast instanceof LustreEq) {
                    eqs.add((LustreEq) ast);
                }
            }
        }
    }

    protected void parseAndAddToExprList(String strExpr, List<LustreExpr> exprs) {
        if (strExpr != null) {
            for (LustreAst ast : J2LUtils.parseAndTranslate(strExpr)) {
                if (ast instanceof LustreExpr) {
                    exprs.add((LustreExpr) ast);
                }
            }
        }
    }

    protected void createFcnDef(String junctionId, List<LustreExpr> condExprs, List<LustreEq> noGuardActExprs, LinkedHashMap<LustreExpr, List<LustreEq>> condToCondTransitAct) {
        JsonNode destJunctionNode = this.junctionIdToNode.get(junctionId);
        List<JsonNode> outerTransits = getJunctionOuterTransitions(destJunctionNode);

        for (JsonNode outTransitNode : outerTransits) {
            String destJunctionId = outTransitNode.get(DEST).get(DEST).get(ID).asText();
            List<LustreExpr> newCondExprs = new ArrayList<>();
            List<LustreEq> condActExprs = new ArrayList<>();

            // Add the previous junction's conditions, condition actions, and transition actions
            // newCondExprs.addAll(condExprs);
            String strCond = getJunctionOutTransitCondExprInStr(outTransitNode);
            String strCondAct = getJunctionOutTransitCondActExprInStr(outTransitNode);

            if (strCond != null) {
                newCondExprs.add((LustreExpr) J2LUtils.parseAndTranslateStrExpr(strCond));
            }
            if (strCondAct != null) {
                parseAndAddToEqList(strCondAct, condActExprs);
            }

            // If the condition expression is empty and so far no conditions generated yet, 
            // we add the condition action to noGuardActExprs
            if (condToCondTransitAct.isEmpty() && newCondExprs.isEmpty()) {
                noGuardActExprs.addAll(condActExprs);
                // If the condition expression is empty, we append the condition action expressions
                // to the previous condition expressions
            } else if (newCondExprs.isEmpty()) {
                List<List<LustreEq>> values = (List<List<LustreEq>>) condToCondTransitAct.values();
                values.get(values.size() - 1).addAll(condActExprs);
            } else {
                List<LustreEq> allEqs = new ArrayList<>();
                allEqs.addAll(condActExprs);
                newCondExprs.addAll(condExprs);
                condToCondTransitAct.put(J2LUtils.andExprs(newCondExprs), allEqs);
            }
            createFcnDef(destJunctionId, newCondExprs, noGuardActExprs, condToCondTransitAct);
        }
    }

    protected String getJunctionTransitDestName(JsonNode transitNode) {
        String name = null;

        if (transitNode.has(DEST)) {
            if (transitNode.get(DEST).has(DEST)) {
                String strName = transitNode.get(DEST).get(DEST).get(NAME).asText();

                if (strName != null && !strName.equals("")) {
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
        if (transitNode.has(DEST)) {
            if (transitNode.get(DEST).has(ID)) {
                String strId = transitNode.get(DEST).get(ID).asText();
                if (strId != null && !strId.equals("")) {
                    id = strId;
                }
            }
        }
        return id;
    }

    protected String getJunctionTransitDestId(JsonNode transitNode) {
        String id = null;
        if (transitNode.has(DEST)) {
            if (transitNode.get(DEST).has(DEST)) {
                String strId = transitNode.get(DEST).get(DEST).get(ID).asText();
                if (strId != null && !strId.equals("")) {
                    id = strId;
                }
            }
        }
        return id;
    }
    
    protected String getCondForJunctionTransition(JsonNode transitNode) {
        String cond = null;
        if (transitNode.has(DEST)) {
            if (transitNode.get(DEST).has(CONDITION)) {
                String strCond = transitNode.get(DEST).get(CONDITION).asText();
                if (strCond != null && !strCond.equals("")) {
                    cond = strCond;
                }
            }
        }
        return cond;
    }

    protected String getCondActForJunctionTransition(JsonNode transitNode) {
        String condActs = null;
        if (transitNode.has(DEST)) {
            if (transitNode.get(DEST).has(CONDITIONACT)) {
                String strCondAct = transitNode.get(DEST).get(CONDITIONACT).asText();

                if (strCondAct != null && !strCondAct.equals("")) {
                    condActs = strCondAct;
                }
            }

        }
        return condActs;
    }

    protected String getTransitActForJunctionTransition(JsonNode transitNode) {
        String transitActs = null;
        if (transitNode.has(DEST)) {
            if (transitNode.get(DEST).has(TRANSITACT)) {
                String strTransitActs = transitNode.get(DEST).get(TRANSITACT).asText();

                if (strTransitActs != null && !strTransitActs.equals("")) {
                    transitActs = strTransitActs;
                }
            }
        }
        return transitActs;
    }
    

    protected String getStateTransitionType(JsonNode transitNode) {
        String type = null;
        if (transitNode.has(DEST)) {
            if (transitNode.get(DEST).has(TYPE)) {
                String strType = transitNode.get(DEST).get(TYPE).asText();
                if (strType != null && !strType.equals("")) {
                    type = strType;
                }
            }
        }
        return type;
    }

    protected String getCondForStateTransition(JsonNode transitNode) {
        String cond = null;
        if (transitNode.has(CONDITION)) {
            String strCond = transitNode.get(CONDITION).asText();
            if (strCond != null && !strCond.equals("")) {
                cond = strCond;
            }
        }
        return cond;
    }

    protected String getCondActForStateTransition(JsonNode transitNode) {
        String condActs = null;
        if (transitNode.has(CONDITIONACT)) {
            String strCondAct = transitNode.get(CONDITIONACT).asText();

            if (strCondAct != null && !strCondAct.equals("")) {
                condActs = strCondAct;
            }            
        }
        return condActs;
    }

    protected String getTransitActForStateTransition(JsonNode transitNode) {
        String transitActs = null;
        if (transitNode.has(TRANSITACT)) {
            String strTransitActs = transitNode.get(TRANSITACT).asText();

            if (strTransitActs != null && !strTransitActs.equals("")) {
                transitActs = strTransitActs;
            }            
        }
        return transitActs;
    }

    protected String getExitActionInStr(JsonNode node) {
        String strExpr = null;
        if (node.has(EXIT)) {
            String strExit = node.get(EXIT).asText();
            if (strExit != null && !strExit.equals("")) {
                strExpr = strExit;
            }
        }
        return strExpr;
    }

    protected String getEntryActionInStr(JsonNode node) {
        String strExpr = null;
        if (node.has(ENTRY)) {
            String strEntry = node.get(ENTRY).asText();
            if (strEntry != null && !strEntry.equals("")) {
                strExpr = strEntry;
            }
        }
        return strExpr;
    }

    protected String getDuringActionInStr(JsonNode node) {
        String strExpr = null;
        if (node.has(DURING)) {
            String strDuring = node.get(DURING).asText();
            if (strDuring != null && !strDuring.equals("")) {
                strExpr = strDuring;
            }
        }
        return strExpr;
    }

    protected String getStateCondExprInStr(JsonNode node) {
        String strExpr = null;
        if (node.has(CONDITION)) {
            String strCond = node.get(CONDITION).asText();
            if (strCond != null && !strCond.equals("")) {
                strExpr = strCond;
            }
        }
        return strExpr;
    }

    protected String getStateCondActExprInStr(JsonNode node) {
        String strExpr = null;
        if (node.has(CONDITIONACT)) {
            String strCondAct = node.get(CONDITIONACT).asText();
            if (strCondAct != null && !strCondAct.equals("")) {
                strExpr = strCondAct;
            }
        }
        return strExpr;
    }

    protected String getStateTranActExprInStr(JsonNode node) {
        String strExpr = null;
        if (node.has(TRANSITACT)) {
            String strTransAct = node.get(TRANSITACT).asText();
            if (strTransAct != null && !strTransAct.equals("")) {
                strExpr = strTransAct;
            }
        }
        return strExpr;
    }

    protected String getJunctionDefaultTransitCondExprInStr(JsonNode node) {
        String strExpr = null;
        if (node.has(CONDITION)) {
            String strCond = node.get(CONDITION).asText();
            if (strCond != null && !strCond.equals("")) {
                strExpr = strCond;
            }
        }
        return strExpr;
    }

    protected List<String> getJunctionDefaultTransitCondActExprInStr(JsonNode node) {
        List<String> strExprs = new ArrayList<>();

        if (node.has(CONDITIONACT)) {
            JsonNode strCondActNode = node.get(CONDITIONACT);

            if (strCondActNode != null) {
                if (strCondActNode.isArray()) {
                    Iterator<JsonNode> actIt = strCondActNode.elements();

                    while (actIt.hasNext()) {
                        strExprs.add(actIt.next().asText());
                    }
                } else if (!strCondActNode.asText().equals("")) {
                    strExprs.add(strCondActNode.asText());
                }
            }
        }
        return strExprs;
    }

    protected String getJunctionDefaultTransitTranActExprInStr(JsonNode node) {
        String strExpr = null;
        if (node.has(TRANSITACT)) {
            String strTransAct = node.get(TRANSITACT).asText();
            if (strTransAct != null && !strTransAct.equals("")) {
                strExpr = strTransAct;
            }
        }
        return strExpr;
    }

    protected String getJunctionOutTransitCondExprInStr(JsonNode node) {
        String strExpr = null;
        if (node.has(DEST) && node.get(DEST).has(CONDITION)) {
            String strCond = node.get(DEST).get(CONDITION).asText();
            if (strCond != null && !strCond.equals("")) {
                strExpr = strCond;
            }
        }
        return strExpr;
    }

    protected String getJunctionOutTransitCondActExprInStr(JsonNode node) {
        String strExpr = null;
        if (node.has(DEST) && node.get(DEST).has(CONDITIONACT)) {
            String strCondAct = node.get(DEST).get(CONDITIONACT).asText();
            if (strCondAct != null && !strCondAct.equals("")) {
                strExpr = strCondAct;
            }
        }
        return strExpr;
    }

    protected String getJunctionOutTransitTranActExprInStr(JsonNode node) {
        String strExpr = null;
        if (node.has(DEST) && node.get(DEST).has(TRANSITACT)) {
            String strTransAct = node.get(DEST).get(TRANSITACT).asText();
            if (strTransAct != null && !strTransAct.equals("")) {
                strExpr = strTransAct;
            }
        }
        return strExpr;
    }

    protected String getSanitizedName(JsonNode node) {
        return J2LUtils.sanitizeName(node.get(NAME).asText());
    }

    protected String getSanitizedPath(JsonNode node) {
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
        if (node.has(OUTTRANSITS)) {
            JsonNode outerTransits = node.get(OUTTRANSITS);

            if (outerTransits != null) {
                if (outerTransits.size() > 0) {
                    if (!outerTransits.isArray()) {
                        transitionNodes.add(outerTransits);
                    } else {
                        Iterator<JsonNode> nodeIt = outerTransits.elements();

                        while (nodeIt.hasNext()) {
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
        if (node.has(OUTTRANSITS)) {
            JsonNode outerTransits = node.get(OUTTRANSITS);

            if (outerTransits != null) {
                if (outerTransits.size() > 0) {
                    if (!outerTransits.isArray()) {
                        transitionNodes.add(outerTransits);
                    } else {
                        Iterator<JsonNode> nodeIt = outerTransits.elements();

                        while (nodeIt.hasNext()) {
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
        if (node.has(DEFAULTTRANS)) {
            JsonNode defaultTransits = node.get(DEFAULTTRANS);

            if (defaultTransits != null) {
                if (defaultTransits.isArray() && defaultTransits.size() > 0) {
                    Iterator<JsonNode> nodeIt = defaultTransits.elements();

                    while (nodeIt.hasNext()) {
                        transitionNodes.add(nodeIt.next());
                    }
                } else {
                    transitionNodes.add(defaultTransits);
                }
            }
        }
        return transitionNodes;
    }

    protected String getSanitizedStatePath(JsonNode stateNode) {
        String name = null;
        if (stateNode.has(PATH)) {
            String strName = stateNode.get(PATH).asText();
            if (strName != null && !strName.equals("")) {
                name = J2LUtils.sanitizeName(strName);
            } else {
                LOGGER.log(Level.SEVERE, "Junction does not have a path!");
            }
        }
        return name;
    }

    protected String getSanitizedStateName(JsonNode stateNode) {
        String name = null;
        if (stateNode.has(NAME)) {
            String strName = stateNode.get(NAME).asText();
            if (strName != null && !strName.equals("")) {
                name = J2LUtils.sanitizeName(strName);
            } else {
                LOGGER.log(Level.SEVERE, "Junction does not have a path!");
            }
        }
        return name;
    }

    protected String getSanitizedJunctionPath(JsonNode junctionNode) {
        String name = null;
        if (junctionNode.has(PATH)) {
            String strName = junctionNode.get(PATH).asText();

            if (strName != null && !strName.equals("")) {
                name = J2LUtils.sanitizeName(strName);
            } else {
                LOGGER.log(Level.SEVERE, "Junction does not have a path!");
            }
        }
        return name;
    }

    protected String getSanitizedJunctionName(JsonNode junctionNode) {
        String name = null;
        if (junctionNode.has(NAME)) {
            String strName = junctionNode.get(NAME).asText();
            if (strName != null && !strName.equals("")) {
                name = J2LUtils.sanitizeName(strName);
            } else {
                LOGGER.log(Level.SEVERE, "Junction does not have a name!");
            }
        }
        return name;
    }

    protected String getNodeId(JsonNode stateNode) {
        String id = null;
        if (stateNode.has(ID)) {
            String strID = stateNode.get(ID).asText();
            if (strID != null && !strID.equals("")) {
                id = strID;
            }
        }
        return id;
    }

    public void collectStateFlowInfo(JsonNode node, String cat) {
        // Process an array portConns
        if (node.isArray()) {
            Iterator<JsonNode> nodeIt = node.elements();

            while (nodeIt.hasNext()) {
                JsonNode    internalNode    = nodeIt.next();
                String      nodeId          = getNodeId(internalNode);

                switch (cat) {
                    case STATES: {
                        // If the state is the entry state, we collect all the information we need here
                        if (J2LUtils.getSanitizedBlkPath(internalNode).equals(this.rootSubsysName)) {
                            JsonNode substateNodeIds = internalNode.get(COMPOSITION).get(STATES);
                            
                            if(substateNodeIds.isArray()) {
                                Iterator<JsonNode>  substateIt = substateNodeIds.elements();

                                while (substateIt.hasNext()) {
                                    JsonNode substateNode = substateIt.next();
                                    this.stateIdToActId.put(substateNode.asText(), substateNode.asInt());
                                }                                
                            } else {
                                this.stateIdToActId.put(substateNodeIds.asText(), substateNodeIds.asInt());
                            }
                            this.centerStateId = nodeId;
                            // Set the start state and its active id
                            this.startStateActiveId = internalNode.get(COMPOSITION).get(DEFAULTTRANS).get(DEST).get(ID).asInt();
//                            this.stateIdToActId.put(internalNode.get(COMPOSITION).get(DEFAULTTRANS).get(DEST).get(ID).asText(), 0);
                        }
                        // Populate the exclusive-or and parallel states maps
                        if (internalNode.has(COMPOSITION)) {
                            List<String>        substateIds     = new ArrayList<>();
                            String              substateType    = internalNode.get(COMPOSITION).get(TYPE).asText();
                            JsonNode            substateNodeIds = internalNode.get(COMPOSITION).get(STATES);
                            Iterator<JsonNode>  substateIt      = substateNodeIds.elements();

                            while (substateIt.hasNext()) {
                                substateIds.add(substateIt.next().asText());
                            }

                            if (!substateIds.isEmpty()) {
                                if (substateType.equals(EXCLUSIVEOR)) {
                                    this.stateIdToOrSubstateIds.put(nodeId, substateIds);
                                } else {
                                    this.stateIdToAndSubstateIds.put(nodeId, substateIds);
                                }
                            }
                        }

                        addToMap(nodeId, internalNode, this.stateIdToNode);
                        break;
                    }
                    case EVENTS: {
                        addToMap(nodeId, internalNode, events);
                        break;
                    }
                    case JUNCTIONS: {
                        addToMap(nodeId, internalNode, this.junctionIdToNode);
                        break;
                    }
                    case GRAPHFCN: {
                        addToMap(nodeId, internalNode, functions);
                        break;
                    }
                    default:
                        break;
                }
            }
            // Process a flattened EVENTS, JUNCTIONS, and so on
        } else {
            String nodeId = getNodeId(node);

            switch (cat) {
                case STATES: {
                    // If the state is the entry state, we collect all the information we need here
                    if (J2LUtils.getSanitizedBlkPath(node).equals(this.rootSubsysName)) {
                        JsonNode substateNodeIds = node.get(COMPOSITION).get(STATES);
                        Iterator<JsonNode> substateIt = substateNodeIds.elements();

                        while (substateIt.hasNext()) {
                            JsonNode substateNode = substateIt.next();
                            this.stateIdToActId.put(substateNode.asText(), substateNode.asInt());
                        }
                        this.centerStateId = getNodeId(node);
                        this.stateIdToActId.put(node.get(COMPOSITION).get(DEFAULTTRANS).get(DEST).get(ID).asText(), 0);
                    }
                    // Populate the exclusive-or and parallel states maps
                    if (node.has(COMPOSITION)) {
                        List<String> substateIds = new ArrayList<>();
                        String substateType = node.get(COMPOSITION).get(TYPE).asText();
                        JsonNode substateNodeIds = node.get(COMPOSITION).get(STATES);
                        Iterator<JsonNode> substateIt = substateNodeIds.elements();

                        while (substateIt.hasNext()) {
                            substateIds.add(substateIt.next().asText());
                        }

                        if (!substateIds.isEmpty()) {
                            if (substateType.equals(EXCLUSIVEOR)) {
                                this.stateIdToOrSubstateIds.put(nodeId, substateIds);
                            } else {
                                this.stateIdToAndSubstateIds.put(nodeId, substateIds);
                            }
                        }
                    }

                    addToMap(nodeId, node, this.stateIdToNode);
                    break;
                }
                case EVENTS: {
                    addToMap(nodeId, node, events);
                    break;
                }
                case JUNCTIONS: {
                    addToMap(nodeId, node, this.junctionIdToNode);
                    break;
                }
                case GRAPHFCN: {
                    addToMap(nodeId, node, functions);
                    break;
                }
                default:
                    break;
            }
        }
    }

    public void addVariables(JsonNode data) {
        // Process an array portConns
        if (data.isArray()) {
            Iterator<JsonNode> dataIt = data.elements();

            while (dataIt.hasNext()) {
                JsonNode dataNode = dataIt.next();
                addVariable(dataNode);
            }
            // Process a normal portConns    
        } else {
            addVariable(data);
        }
    }

    public void addVariable(JsonNode dataNode) {
        String id = dataNode.get(ID).asText();
        int port = dataNode.get(PORT).asInt();

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
        if (map.containsKey(key)) {
            map.get(key).add(value);
        } else {
            map.put(key, Arrays.asList(value));
        }
    }

    public void addToMap(String key, JsonNode value, Map<String, JsonNode> map) {
        if (map.containsKey(key)) {
            LOGGER.log(Level.SEVERE, "Unexpected behavior from input: two states or events or junctions have the same id: {0}", key);
        } else {
            map.put(key, value);
        }
    }
}
