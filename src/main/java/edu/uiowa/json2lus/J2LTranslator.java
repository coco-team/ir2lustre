/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.uiowa.json2lus.lustreAst.BinaryExpr;
import edu.uiowa.json2lus.lustreAst.BooleanExpr;
import edu.uiowa.json2lus.lustreAst.IntExpr;
import edu.uiowa.json2lus.lustreAst.IteExpr;
import edu.uiowa.json2lus.lustreAst.LustreEq;
import edu.uiowa.json2lus.lustreAst.LustreExpr;
import edu.uiowa.json2lus.lustreAst.LustreNode;
import edu.uiowa.json2lus.lustreAst.LustreProgram;
import edu.uiowa.json2lus.lustreAst.LustreType;
import edu.uiowa.json2lus.lustreAst.LustreVar;
import edu.uiowa.json2lus.lustreAst.NodeCallExpr;
import edu.uiowa.json2lus.lustreAst.PrimitiveType;
import edu.uiowa.json2lus.lustreAst.RealExpr;
import edu.uiowa.json2lus.lustreAst.UnaryExpr;
import edu.uiowa.json2lus.lustreAst.VarIdExpr;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 *
 * @author Paul Meng
 */
public class J2LTranslator {
    /** The path of the input JSON file */
    public  final String inputPath;
    
    /** Assumption: The file name is also the model name */
    public  final String modelName;
    
    /** Logger */
    private static final Logger LOGGER = Logger.getLogger(J2LTranslator.class.getName());
    
    /** JSON fields */
    
    /** Relational operators */
    private final String GTE            = ">=";
    private final String LTE            = "<=";
    private final String GT             = ">";
    private final String LT             = "<";
    private final String EQ             = "==";
    private final String NEQ            = "~=";     
    
    /** Logical operators */
    private final String AND            = "AND";
    private final String OR             = "OR";
    private final String NOT            = "NOT";
    
    /** Math operators */
    private final String SUM            = "Sum";    
    private final String ABS            = "Abs";
    
    private final String SWITCH         = "Switch";
    private final String CRITERIA       = "Criteria";
    private final String THRESHOLD      = "Threshold";
    private final String TRDCRIT        = "u2 ~= 0";
    private final String FSTCRIT        = "u2 >= Threshold"; 
    private final String SNDCRIT        = "u2 > Threshold"; 
        
    private final String X0             = "X0";
    private final String MAX            = "max";
    private final String MIN            = "min";
    private final String GAIN           = "Gain";
    private final String INPUTS         = "Inputs";
    private final String MINMAX         = "MinMax"; 
    private final String MEMORY         = "Memory";
    private final String PRODUCT        = "Product";
    private final String CONSTANT       = "Constant";        
    private final String FUNCTION       = "Function";            
    
    /** Blocks information */
    private final String NAME           = "Name";
    private final String TYPE           = "Type";
    private final String PATH           = "Path";
    private final String VALUE          = "Value";
    private final String HANDLE         = "Handle";
    private final String INPORT         = "Inport";    
    private final String OUTPORT        = "Outport";    
    private final String CONTENT        = "Content";
    private final String OPERATOR       = "Operator";
    private final String SRCBLOCK       = "SrcBlock";
    private final String DSTBLOCK       = "DstBlock";
    private final String BLOCKTYPE      = "BlockType";
    private final String SUBSYSTEM      = "SubSystem"; 
    private final String TERMINATOR     = "Terminator";
    private final String LINEHANDLES    = "LineHandles";     
    private final String CONNECTIVITY   = "PortConnectivity";
    private final String PORTDATATYPE   = "CompiledPortDataTypes"; 
                      
    private JsonNode                                topLevelNode;    
    private final LustreProgram                     lustreProgram;
    private final Set<JsonNode>                     subsystemNodes;    
    private final Map<String, String>               libNodeNameMap;    
    private final Map<JsonNode, List<JsonNode>>     subsystemPropsMap;
    
    
            
    /**
     * Constructor
     * @param inputPath
     */
    public J2LTranslator(String inputPath) {        
        this.inputPath          = inputPath;        
        this.libNodeNameMap     = new HashMap<>();
        this.subsystemNodes     = new HashSet<>();
        this.subsystemPropsMap  = new HashMap<>();
        this.lustreProgram      = new LustreProgram();
        this.modelName          = inputPath.toLowerCase().endsWith(".json") ? 
                                    inputPath.substring(inputPath.lastIndexOf(File.separator)+1, inputPath.lastIndexOf("."))
                                    : inputPath.substring(inputPath.lastIndexOf(File.separator)+1);                            
    }
    
    /**
     * Execute the translation process 
     * 
     * @return lustreProgram
     */    
    public LustreProgram execute() {
        collectSubsytemBlocksInfo();
        for(JsonNode node : this.subsystemNodes) {
            this.lustreProgram.addNode(translateSubsystemNode(node));
        }
        return this.lustreProgram;
    }    
    
    /**
     * Collect subsystem blocks information
     */
    protected void collectSubsytemBlocksInfo() {
        JsonNode        rootNode    = null;        
        ObjectMapper    mapper      = new ObjectMapper();
        
        try {
            rootNode = mapper.readTree(new File(this.inputPath));
        } catch (IOException ex) {
            Logger.getLogger(J2LTranslator.class.getName()).log(Level.SEVERE, null, ex);
        }

        if(rootNode != null && rootNode.has(this.modelName)) {
            JsonNode topNode = rootNode.get(this.modelName);
            this.topLevelNode = topNode;
            collectSubsytemBlocksInfo(topNode);               
        } else {
            LOGGER.log(Level.SEVERE, "We are assuming that the root node in the JSON model is the same as the input file name!");
            LOGGER.log(Level.SEVERE, "But we did not find a such field: {0}!", this.modelName);
        }   
    }

    /**
     * Collect subsystem blocks information
     * @param subsystemNode
     */    
    protected void collectSubsytemBlocksInfo(JsonNode subsystemNode) {
        if(subsystemNode != null && !this.subsystemNodes.contains(subsystemNode) && subsystemNode.has(CONTENT)) {
            LOGGER.log(Level.INFO, "Found a subsystem block: {0}  ", subsystemNode.equals(this.topLevelNode) ? this.modelName:subsystemNode.get(NAME).asText());            
            
            this.subsystemNodes.add(subsystemNode);
            Iterator<Entry<String, JsonNode>> nodes = subsystemNode.get(CONTENT).fields();                         
            
            while(nodes.hasNext()) {
                Map.Entry<String, JsonNode> field       = nodes.next(); 
                JsonNode                    fieldNode   = field.getValue();
                
                if(fieldNode.has(BLOCKTYPE)) {
                    String blkType = fieldNode.get(BLOCKTYPE).asText();
                    if(blkType.equals(SUBSYSTEM)) {                        
                        collectSubsytemBlocksInfo(fieldNode);
                    }          
                }
               
            }
        } else {
            LOGGER.log(Level.SEVERE, "Cannot find the Cocosim model: {0} content definition in the input JSON file!", this.modelName);
        }       
    }        
    
    /**
     * 
     * @param subsystemNode
     * @return The Lustre node corresponding to the input subsystem JSON node
     */
    protected LustreNode translateSubsystemNode(JsonNode subsystemNode) {            
        String              lusNodeName     = subsystemNode.equals(this.topLevelNode) ? this.modelName : subsystemNode.get(NAME).asText();        
        List<LustreVar>     inputs          = new ArrayList<>();
        List<LustreVar>     outputs         = new ArrayList<>();
        List<LustreVar>     locals          = new ArrayList<>();
        List<LustreEq>      equations       = new ArrayList<>();
        List<LustreEq>      props           = new ArrayList<>();
        
        /** ALL the fields under content field of subsystem block */
        Iterator<Entry<String, JsonNode>>   contentFields               = subsystemNode.get(CONTENT).fields();        
        /** A mapping between a handle and the block node */
        Map<String, JsonNode>               handleToBlkNodeMap          = new HashMap<>();                
        /** A mapping between a block node and its src block handles */
        Map<JsonNode, List<String>>         blkNodeToSrcBlkHandlesMap   = new HashMap<>();  
        /** A mapping between a block node and its dst block handles */
        Map<JsonNode, List<String>>         blkNodeToDstBlkHandlesMap   = new HashMap<>();          
        /** Outport nodes in the input subsystem block */
        List<JsonNode>                      outportNodes                = new ArrayList<>();    
        
        /** Terminator node in the input subsystem block */
        List<JsonNode>                      propsNodes                  = new ArrayList<>();    
        
        LOGGER.log(Level.INFO, "Start translating subsystem block: {0}!", lusNodeName);
        
        while(contentFields.hasNext()) {
            Map.Entry<String, JsonNode> contField       = contentFields.next();   
            JsonNode                    contBlkNode     = contField.getValue();
            
            if(contBlkNode.has(BLOCKTYPE)) {
                if(contBlkNode.has(HANDLE)) {
                    handleToBlkNodeMap.put(contBlkNode.get(HANDLE).asText(), contBlkNode);
                }
                // Top-level subsystem block does not have port connectivity field
                if(contBlkNode.has(CONNECTIVITY)) {
                    JsonNode        portConns       = contBlkNode.get(CONNECTIVITY);
                    List<String>    srcBlockHanldes = new ArrayList<>();
                    List<String>    dstBlockHanldes = new ArrayList<>();

                    if(portConns.isArray()) {
                        Iterator<JsonNode> portConnIt = portConns.elements();

                        while(portConnIt.hasNext()) {
                            JsonNode connNode = portConnIt.next();
                            srcBlockHanldes.addAll(convertJsonValuesToList(connNode.get(SRCBLOCK)));
                            dstBlockHanldes.addAll(convertJsonValuesToList(connNode.get(DSTBLOCK)));
                        }
                    } else {
                        srcBlockHanldes.addAll(convertJsonValuesToList(portConns.get(SRCBLOCK)));
                        dstBlockHanldes.addAll(convertJsonValuesToList(portConns.get(DSTBLOCK)));            
                    }
                    if(!srcBlockHanldes.isEmpty()) {
                        blkNodeToSrcBlkHandlesMap.put(contBlkNode, srcBlockHanldes);
                    }
                    if(!dstBlockHanldes.isEmpty()) {
                        blkNodeToDstBlkHandlesMap.put(contBlkNode, dstBlockHanldes);    
                    }                     
                }                 
                                
                /** Translate inports and outports of subsystem blocks */
                switch(contBlkNode.get(BLOCKTYPE).asText()) {
                    case INPORT: {                        
                        String      name = contBlkNode.get(NAME).asText();
                        LustreType  type = getLustreTypeFromStrRep(contBlkNode.get(PORTDATATYPE).get(OUTPORT).asText());
                        LustreVar   var  = new LustreVar(name, type);
                        inputs.add(var);
                        break;
                    }
                    case OUTPORT: {
                        String      name = contBlkNode.get(NAME).asText();
                        LustreType  type = getLustreTypeFromStrRep(contBlkNode.get(PORTDATATYPE).get(INPORT).asText());
                        LustreVar   var  = new LustreVar(name, type);
                        outputs.add(var);      
                        outportNodes.add(contBlkNode);
                        break;
                    }
                    case TERMINATOR: {
                        propsNodes.add(contBlkNode);
                        break;
                    }
                    default:
                        break;
                }                 
            }            
        }
        
        // Translate outport equations backwardsly
        for(JsonNode outportNode : outportNodes) {
            LustreEq outportEq = translateOutportEquation(outportNode, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap);
            
            if(outportEq != null) {
                equations.add(outportEq);
            } else {
                LOGGER.log(Level.SEVERE, "A null outport equation was generated by the translation!");
            }              
        }
        for(JsonNode propNode : propsNodes) {
            List<LustreEq> propEqs = translatePropEquation(propNode, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap);
            for(LustreEq eq : propEqs) {
                for(VarIdExpr var : eq.getLhs()) {
                    locals.add(new LustreVar(var.id, PrimitiveType.BOOL));
                }
            }
            props.addAll(propEqs);            
        }
        return new LustreNode(lusNodeName, inputs, outputs, locals, equations, props);
    }
    
    /**
     * 
     * @param propNode
     * @param blkNodeToSrcBlkHandlesMap
     * @param blkNodeToDstBlkHandlesMap
     * @return A list of property equations
     */
    protected List<LustreEq> translatePropEquation(JsonNode propNode, Map<JsonNode, List<String>> blkNodeToSrcBlkHandlesMap, Map<JsonNode, List<String>> blkNodeToDstBlkHandlesMap, Map<String, JsonNode> handleToBlkNodeMap) {
        List<LustreEq> propEqs = new ArrayList<>();
        
        if(blkNodeToSrcBlkHandlesMap.containsKey(propNode)) {
            List<String> srcBlkHandles = blkNodeToSrcBlkHandlesMap.get(propNode);
            
            for(String srcBlkHdl : srcBlkHandles) {
                if(handleToBlkNodeMap.containsKey(srcBlkHdl)) {
                    JsonNode srcBlkNode = handleToBlkNodeMap.get(srcBlkHdl);
                    
                    if(srcBlkNode.has(BLOCKTYPE)) {
                        String srcBlkType = srcBlkNode.get(BLOCKTYPE).asText();
                        
                        if(srcBlkType.equals(SUBSYSTEM)) {
                            List<VarIdExpr> outVarIdExprs   = new ArrayList<>();
                            List<JsonNode>  outportNodes    = getBlksFromSubSystem(srcBlkNode, OUTPORT);
                            
                            for(JsonNode outNode : outportNodes) {
                                outVarIdExprs.add(new VarIdExpr(srcBlkNode.get(NAME).asText()+"_"+outNode.get(NAME).asText()));
                            }
                            
                            LustreExpr rhsExpr = translateBlock(srcBlkHdl, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap);
                            propEqs.add(new LustreEq(outVarIdExprs, rhsExpr));
                        } else {
                            LOGGER.log(Level.SEVERE, "Unsupported property block type: {0}!", srcBlkType);
                        }
                    }
                } else {
                    LOGGER.log(Level.SEVERE, "Unexpected: No block node was found with handle: {0}!", srcBlkHdl);
                }                
            }            
        } else {
            LOGGER.log(Level.WARNING, "Unexpected: No src blocks connect to the terminator : {0}!", propNode.get(NAME).asText());
        }        
        
        return propEqs;
    } 
    
    /**
     * 
     * @param outportNode
     * @param blkNodeToSrcBlkHandlesMap
     * @param blkNodeToDstBlkHandlesMap
     * @return Outport equation
     */
    protected LustreEq translateOutportEquation(JsonNode outportNode, Map<JsonNode, List<String>> blkNodeToSrcBlkHandlesMap, Map<JsonNode, List<String>> blkNodeToDstBlkHandlesMap, Map<String, JsonNode> handleToBlkNodeMap) {
        LustreEq    eq          = null;
        VarIdExpr   varIdExpr   = new VarIdExpr(outportNode.get(NAME).asText());        
        
        if(blkNodeToSrcBlkHandlesMap.containsKey(outportNode)) {
            List<String> srcBlkHandles = blkNodeToSrcBlkHandlesMap.get(outportNode);
            
            if(srcBlkHandles.size() == 1) {
                eq = new LustreEq(varIdExpr, translateBlock(srcBlkHandles.get(0), blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap));
            } else if(srcBlkHandles.size() > 1) {
                LOGGER.log(Level.SEVERE, "Unexpected: Multiple different src blocks connect to a same outport: {0}!", outportNode.get(NAME).asText());
            } else {
                LOGGER.log(Level.SEVERE, "Unexpected: No src blocks connect to the outport: {0}!", outportNode.get(NAME).asText());
            }
        } else {
            LOGGER.log(Level.WARNING, "Unexpected: No src blocks connect to the outport: {0}!", outportNode.get(NAME).asText());
        }
        
        return eq;
    }

    protected String createLustreLibNode(JsonNode blkNode) {
        String path     = blkNode.get(PATH).asText();
        String nodeName = path.replace(File.separator,"_");
        String blkType  = blkNode.get(BLOCKTYPE).asText();
        
        switch(blkType) {
            case ABS: {
                LustreType  outType = getBlockOutportType(blkNode);
                
                if(this.libNodeNameMap.containsKey("abs_"+outType)) {
                    nodeName = this.libNodeNameMap.get("abs_"+outType);
                    break;
                }
                
                LustreExpr  zeroExpr = null;
                
                if(outType == PrimitiveType.INT) {
                    nodeName    = nodeName + "_int";
                    zeroExpr    = new IntExpr(new BigInteger("0"));                    
                } else if(outType == PrimitiveType.REAL) {
                    nodeName    = nodeName + "_real";
                    zeroExpr    = new RealExpr(new BigDecimal("0.0"));
                } else {
                    LOGGER.log(Level.SEVERE, "Unsupported library node input and output type: {0}", outType);
                }
                
                VarIdExpr           inVarExpr   = new VarIdExpr("in");
                VarIdExpr           outVarExpr  = new VarIdExpr("out");   
                List<LustreEq>      bodyExprs   = new ArrayList<>();                  
    
                bodyExprs.add(new LustreEq(outVarExpr, new IteExpr(new BinaryExpr(inVarExpr, BinaryExpr.Op.GTE, zeroExpr), inVarExpr, new UnaryExpr(UnaryExpr.Op.NEG, inVarExpr))));
                this.libNodeNameMap.put("abs_"+outType, nodeName);
                this.lustreProgram.addNode(new LustreNode(nodeName, new LustreVar("in", outType), new LustreVar("out", outType), bodyExprs));                
                break;
            }
            case MINMAX: {
                String      funcName    = blkNode.get(FUNCTION).asText();
                LustreType  outType     = getBlockOutportType(blkNode);
                
                if(this.libNodeNameMap.containsKey(funcName+"_"+outType)) {
                    nodeName = this.libNodeNameMap.get(funcName+"_"+outType);
                    break;
                }                
                
                LustreExpr          rhsExpr     = null;             
                VarIdExpr           outVarExpr  = new VarIdExpr("out");
                int                 numOfInputs = blkNode.get(INPUTS).asInt();                
                List<LustreType>    inTypes     = getBlockInportType(blkNode);
                List<LustreVar>     inputVars   = new ArrayList<>();
                List<LustreVar>     outputVars  = Arrays.asList(new LustreVar("out", outType));
                List<VarIdExpr>     inExprs     = new ArrayList<>();
                List<LustreEq>      bodyExprs   = new ArrayList<>();   
                
                nodeName = nodeName + "_" + funcName + "_" + outType;
                for(int i = 0; i < inTypes.size(); i++) {
                    String varName = "in_" + i;
                    inputVars.add(new LustreVar(varName, inTypes.get(i)));
                    inExprs.add(new VarIdExpr(varName));
                }

                if(numOfInputs != inExprs.size()) {
                    LOGGER.log(Level.SEVERE, "Inputs to MINMAX blocks does not match the actual inputs!");
                } else if(numOfInputs == 1) {
                    rhsExpr = inExprs.get(0);
                } else if(numOfInputs > 1) {
                    BinaryExpr.Op op = null;

                    switch (funcName) {
                        case MAX: 
                            op = BinaryExpr.Op.GTE;
                            break;
                        case MIN:
                            op = BinaryExpr.Op.LTE;
                            break;
                        default:
                            LOGGER.log(Level.SEVERE, "Unsupported MINMAX function operator type: {0}", funcName);
                            break;
                    }

                    LustreExpr ifCond   = new BinaryExpr(inExprs.get(numOfInputs-2), op, inExprs.get(numOfInputs-1));

                    for(int i = 0; i < numOfInputs-2; i++) {
                        ifCond = new BinaryExpr(new BinaryExpr(inExprs.get(numOfInputs-2), op, inExprs.get(i)), BinaryExpr.Op.AND, ifCond);
                    }
                    rhsExpr = new IteExpr(ifCond, inExprs.get(numOfInputs-2), inExprs.get(numOfInputs-1));

                    for(int i = numOfInputs-3; i >= 0; i--) {
                        ifCond = new BinaryExpr(inExprs.get(i), op, inExprs.get(i+1));

                        for(int j = 0; j < i; j++) {
                            ifCond = new BinaryExpr(new BinaryExpr(inExprs.get(i), op, inExprs.get(j)), BinaryExpr.Op.AND, ifCond);
                        }
                        for(int k = i+2; k < numOfInputs-1; k++) {
                            ifCond = new BinaryExpr(new BinaryExpr(inExprs.get(i), op, inExprs.get(k)), BinaryExpr.Op.AND, ifCond);
                        }
                        rhsExpr = new IteExpr(ifCond, inExprs.get(i), rhsExpr);
                    }                                               
                }          
                bodyExprs.add(new LustreEq(outVarExpr, rhsExpr));
                this.libNodeNameMap.put(funcName+"_"+outType, nodeName);
                this.lustreProgram.addNode(new LustreNode(nodeName, inputVars, outputVars, bodyExprs));                                
                break;
            }
            default:
                LOGGER.log(Level.SEVERE, "Unsupported library node type: {0}", blkType);
                break;
        }
        
        
        return nodeName;
    }
    
    /**
     * 
     * @param blkHandle
     * @param blkNodeToSrcBlkHandlesMap
     * @param blkNodeToDstBlkHandlesMap
     * @param handleToBlkNodeMap
     * @return The Lustre expression corresponding to the input JSON block node
     */

    protected LustreExpr translateBlock(String blkHandle, Map<JsonNode, List<String>> blkNodeToSrcBlkHandlesMap,  Map<JsonNode, List<String>> blkNodeToDstBlkHandlesMap, Map<String, JsonNode> handleToBlkNodeMap) {
        LustreExpr  blkExpr     = null;
        JsonNode    blkNode     = null;
        
        if(handleToBlkNodeMap.containsKey(blkHandle)) {
            blkNode = handleToBlkNodeMap.get(blkHandle);
        } else {
            LOGGER.log(Level.SEVERE, "Unexpected: no block in the model with handle {0}", blkHandle);
        }
        
        if(blkNode != null && blkNode.has(BLOCKTYPE)) {
            String              blockType   = blkNode.get(BLOCKTYPE).asText();
            List<LustreExpr>    inExprs     = new ArrayList<>();            
            List<String>        inHandles   = blkNodeToSrcBlkHandlesMap.get(blkNode);
            
            if(blockType.toLowerCase().equals("logic") || blockType.toLowerCase().equals("relationaloperator")) {
                blockType = blkNode.get(OPERATOR).asText();
            }
            if(inHandles != null) {
                for(String inHandle : inHandles) {
                    inExprs.add(translateBlock(inHandle, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap));
                }                
            }                         
            switch(blockType) {
                case EQ: {                                                            
                    blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.EQ, inExprs.get(1));                                       
                    break;
                }  
                case NEQ: {                                                            
                    blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.NEQ, inExprs.get(1));                                       
                    break;
                } 
                case GTE: {                                                            
                    blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.GTE, inExprs.get(1));                                       
                    break;
                } 
                case LTE: {                                                            
                    blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.LTE, inExprs.get(1));                                       
                    break;
                }  
                case GT: {                                                            
                    blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.GT, inExprs.get(1));                                       
                    break;
                }  
                case LT: {                                                            
                    blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.LT, inExprs.get(1));                                       
                    break;
                }                  
                case NOT: {                                                                             
                    blkExpr = new UnaryExpr(UnaryExpr.Op.NOT, inExprs.get(0));                                      
                    break;
                }                   
                case OR: {                                                                               
                    blkExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.OR, inExprs.get(i));   
                    }                                        
                    break;
                }                 
                case AND: {                                                                             
                    blkExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.AND, inExprs.get(i));   
                    }                                        
                    break;
                }                
                case ABS: {
                    blkExpr = new NodeCallExpr(createLustreLibNode(blkNode), inExprs.get(0));                   
                    break;                    
                }
                case SUM: {
                    int     numOfInputs = -1;
                    String  ops         = blkNode.get(INPUTS).asText();
                    
                    
                    if(ops.matches("\\d+")) {
                        numOfInputs = Integer.parseInt(ops);
                    } else {
                        ops = ops.replaceAll("\\|", "").trim();
                    }
                    
                    if(numOfInputs != -1 && numOfInputs != inExprs.size()) {
                        LOGGER.log(Level.SEVERE, "Number of inputs to SUM block does not equal the number of inputs specified in parameters: {0}", numOfInputs);
                    } else if((numOfInputs != -1 && numOfInputs == inExprs.size())) {
                        blkExpr = inExprs.get(0);
                        for(int i = 1; i < inExprs.size(); i++) {
                            blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.PLUS, inExprs.get(i));   
                        }                        
                    } else if(ops != null && ops.length() > 0 && ops.length() == inExprs.size()) {
                        blkExpr = inExprs.get(0);
                        
                        if(ops.charAt(0) == '-') {
                            blkExpr = new UnaryExpr(UnaryExpr.Op.NEG, blkExpr);
                        }
                        for(int i = 1; i < ops.length(); i++) {
                            switch (ops.charAt(i)) {
                                case '+':
                                    blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.PLUS, inExprs.get(i));
                                    break;
                                case '-':
                                    blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.MINUS, inExprs.get(i));
                                    break;
                                default:
                                    LOGGER.log(Level.SEVERE, "Unexpected operators in SUM blocks parameters: {0}", ops.charAt(i));
                                    break;
                            }
                        }
                    } else {
                        LOGGER.log(Level.SEVERE, "Unexpected definition for SUM block: {0}", blkNode);
                    }
                    break;
                }
                case SUBSYSTEM: {    
                    blkExpr = new NodeCallExpr(blkNode.get(NAME).asText(), inExprs);
                    break;
                }
                case INPORT: {
                    blkExpr = new VarIdExpr(blkNode.get(NAME).asText());
                    break;
                }
                case CONSTANT: {
                    String      value   = blkNode.get(VALUE).asText();
                    LustreType  type    = getBlockOutportType(blkNode);
                                        
                    if(type == PrimitiveType.REAL) {
                        if(!value.contains(".")) {
                            value = value + ".0";
                        }
                        blkExpr = new RealExpr(new BigDecimal(value));
                    } else if(type == PrimitiveType.INT) {
                        blkExpr = new IntExpr(new BigInteger(value));
                    } else if(type == PrimitiveType.BOOL) {
                        blkExpr = new BooleanExpr(value);
                    } else {
                        LOGGER.log(Level.SEVERE, "Unsupported constant datatype: {0}", type);
                    }
                    break;
                }
                case PRODUCT: {
                    int     numOfInputs = -1;
                    String  ops         = blkNode.get(INPUTS).asText();
                                        
                    if(ops.matches("\\d+")) {
                        numOfInputs = Integer.parseInt(ops);
                    } else {
                        ops = ops.replaceAll("\\|", "").trim();
                    }
                    
                    if(numOfInputs != -1 && numOfInputs != inExprs.size()) {
                        LOGGER.log(Level.SEVERE, "Number of inputs to PRODUCT block does not equal the number of inputs specified in parameters: {0}", numOfInputs);
                    } else if((numOfInputs != -1 && numOfInputs == inExprs.size())) {
                        blkExpr = inExprs.get(0);
                        for(int i = 1; i < inExprs.size(); i++) {
                            blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.MULTIPLY, inExprs.get(i));   
                        }                        
                    } else if(ops != null && ops.length() > 0 && ops.length() == inExprs.size()) {
                        blkExpr = inExprs.get(0);
                        
                        if(ops.charAt(0) == '/') {
                            blkExpr = new BinaryExpr(new RealExpr(new BigDecimal("1.0")), BinaryExpr.Op.DIVIDE, blkExpr);
                        }
                        for(int i = 1; i < ops.length(); i++) {
                            switch (ops.charAt(i)) {
                                case '*':
                                    blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.MULTIPLY, inExprs.get(i));
                                    break;
                                case '/':
                                    blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.DIVIDE, inExprs.get(i));
                                    break;
                                default:
                                    LOGGER.log(Level.SEVERE, "Unexpected operators in PRODUCT blocks parameters: {0}", ops.charAt(i));
                                    break;
                            }
                        }
                    } else {
                        LOGGER.log(Level.SEVERE, "Unexpected definition for PRODUCT block: {0}", blkNode);
                    }
                    break;
                }
                case GAIN: {
                    String gain = blkNode.get(GAIN).asText();

                    if(gain.contains(".")) {
                        blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.MULTIPLY, new RealExpr(new BigDecimal(gain)));
                    } else {
                        blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.MULTIPLY, new IntExpr(new BigInteger(gain)));
                    }
                    break;
                }                
                case MINMAX: {
                    blkExpr = new NodeCallExpr(createLustreLibNode(blkNode), inExprs);
                    break;
                }
                case SWITCH: {
                    LustreExpr  condExpr    = null;
                    String      criteria    = blkNode.get(CRITERIA).asText();
                    LustreType  condType    = getSwitchCondType(blkNode);
                                       
                    if(condType == PrimitiveType.BOOL) {
                        condExpr = inExprs.get(1);
                    } else if(condType == PrimitiveType.REAL || condType == PrimitiveType.INT) {
                        LustreExpr  condRhsExpr     = null;
                        String      threshold       = blkNode.get(THRESHOLD).asText();
                        
                        if(condType == PrimitiveType.REAL) {
                            if(!threshold.contains(".")) {
                                threshold += ".0";
                            }
                            condRhsExpr = new RealExpr(new BigDecimal(threshold));
                        } else {
                            condRhsExpr = new IntExpr(new BigInteger(threshold));
                        }
                        switch(criteria) {
                            case FSTCRIT: {
                                condExpr = new BinaryExpr(inExprs.get(1), BinaryExpr.Op.GTE, condRhsExpr);
                                break;
                            }
                            case SNDCRIT: {
                                condExpr = new BinaryExpr(inExprs.get(1), BinaryExpr.Op.GT, condRhsExpr);
                                break;
                            }
                            case TRDCRIT: {
                                condExpr = new BinaryExpr(inExprs.get(1), BinaryExpr.Op.NEQ, condRhsExpr);
                                break;
                            }
                            default:
                                break;
                        }
                    } else {
                        LOGGER.log(Level.SEVERE, "UNSUPPORTED condition type: {0}", condType);
                    }
                    blkExpr = new IteExpr(condExpr, inExprs.get(0), inExprs.get(2));
                    break;
                }
                case MEMORY: {
                    LustreExpr  initExpr    = null;
                    String      init        = blkNode.get(X0).asText();                                        
                    LustreType  initType    = getBlockOutportType(blkNode); 
                    
                    blkExpr = new UnaryExpr(UnaryExpr.Op.PRE, inExprs.get(0));                    
                    if(initType == PrimitiveType.REAL) {
                        if(init.matches("^[-+]?\\d+(\\.\\d+)?$")) {
                            if(!init.contains(".")) {
                                init = init + ".0";
                            }
                            initExpr = new RealExpr(new BigDecimal(init));
                        } else {
                            LOGGER.log(Level.SEVERE, "UNSUPPORTED init value: {0}", init);
                        }                        
                    } else if(initType == PrimitiveType.INT) {
                        initExpr = new IntExpr(new BigInteger(init));
                    } else if(initType == PrimitiveType.BOOL) {
                        initExpr = new BooleanExpr(init);
                    } else {
                        LOGGER.log(Level.SEVERE, "UNSUPPORTED init value type: {0}", initType);
                    }
                    if(initExpr != null) {
                        blkExpr = new BinaryExpr(initExpr, BinaryExpr.Op.ARROW, blkExpr);
                    }
                    break;
                }
                default:
                    LOGGER.log(Level.SEVERE, "Unsupported block type: {0}!", blockType);
                    break;
            }
        }                       
        
        return blkExpr;
    }    
    
    protected LustreType getSwitchCondType(JsonNode switchBlk) {        
        List<String>        types   = convertJsonValuesToList(switchBlk.get(PORTDATATYPE).get(INPORT));
        Iterator<JsonNode>  connIt  = switchBlk.get(CONNECTIVITY).elements(); 
        // The switch condition is the second port in the port connectivity
        return getLustreTypeFromStrRep(types.get(connIt.next().get(TYPE).asInt()));
    }
    
    /**
     * 
     * @param type
     * @return The corresponding Lustre type from its string representation
     */
    protected LustreType getLustreTypeFromStrRep(String type) {
        LustreType lusType = null;
        
        switch(type.toLowerCase()) {
            case "float": 
            case "single":                
            case "double": {
                lusType = PrimitiveType.REAL;
                break;
            }
            case "integer": 
            case "int": 
            case "int64":     
            case "int32": 
            case "int16": 
            case "int8": 
            case "uint8":
            case "uint16":
            case "uint32":{
                lusType = PrimitiveType.INT;
                break;                
            }            
            case "boolean": 
            case "bool": {
                lusType = PrimitiveType.BOOL;
                break;                
            }        
            default:
                break;
        }
        
        return lusType;
    }
    
    private List<String> convertJsonValuesToList(JsonNode values) {
        List<String> strValues = new ArrayList<>();
        
        if(values.isArray()) {
            Iterator<JsonNode> valuesIt = values.elements();
            
            while(valuesIt.hasNext()) {
                strValues.add(valuesIt.next().asText());
            }
        } else {
            strValues.add(values.asText());
        }
        return strValues;
    }  
    
    protected LustreType getBlockOutportType(JsonNode blkNode) {
        return getLustreTypeFromStrRep(blkNode.get(PORTDATATYPE).get(OUTPORT).asText());
    }
    
    protected List<LustreType> getBlockInportType(JsonNode blkNode) {
        List<LustreType>    types       = new ArrayList<>();
        List<String>        typeStrs    = convertJsonValuesToList(blkNode.get(PORTDATATYPE).get(INPORT));        
        
        for(String typeStr : typeStrs) {
            types.add(getLustreTypeFromStrRep(typeStr));
        }
        
        return types;
    }    
    
    
    protected List<JsonNode> getBlksFromSubSystem(JsonNode subsystemNode, String blkType) {
        List<JsonNode> blkNodes = new ArrayList();
        Iterator<Entry<String, JsonNode>> contentFields = subsystemNode.get(CONTENT).fields(); 
        
        while(contentFields.hasNext()) {
            Map.Entry<String, JsonNode> contField       = contentFields.next();   
            JsonNode                    contBlkNode     = contField.getValue();
            
            if(contBlkNode.has(BLOCKTYPE) && contBlkNode.get(BLOCKTYPE).asText().equals(blkType)) {
                blkNodes.add(contBlkNode);
            }           
        }
        return blkNodes;
    }    
    
}
