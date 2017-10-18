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
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Paul Meng
 */
public class J2LTranslator {
    /** The path of the input JSON file */
    public  final String inputPath;
    
    /** Assumption: The file name is also the top-level model name */
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
    private final String NAND           = "NAND";    
    private final String OR             = "OR";
    private final String NOR            = "NOR";
    private final String XOR            = "XOR";
    private final String NXOR           = "NXOR";
    private final String NOT            = "NOT";
    
    /** Math operators */
    private final String SUM            = "Sum";
    private final String MOD            = "mod";    
    private final String ABS            = "Abs";
    private final String SQRT           = "Sqrt";
    private final String MATH           = "Math";
    private final String UNARYMINUS     = "UnaryMinus";
    
    private final String IF             = "If";    
    private final String SWITCH         = "Switch";
    private final String CRITERIA       = "Criteria";
    private final String THRESHOLD      = "Threshold";
    private final String TRDCRIT        = "u2 ~= 0";
    private final String FSTCRIT        = "u2 >= Threshold"; 
    private final String SNDCRIT        = "u2 > Threshold"; 
    private final String UNITDELAY      = "UnitDelay";
        
    private final String X0             = "X0";
    private final String MAX            = "max";
    private final String MIN            = "min";
    private final String GAIN           = "Gain";
    private final String MERGE          = "Merge";
    private final String INPUTS         = "Inputs";    
    private final String MINMAX         = "MinMax";    
    private final String MEMORY         = "Memory";
    
    private final String PRODUCT        = "Product";
    private final String CONSTANT       = "Constant";        
    private final String FUNCTION       = "Function";    
    private final String INITCOND       = "InitialCondition"; 
    private final String IFEXPRESSION   = "IfExpression";
    private final String ELSEIFEXPRS    = "ElseIfExpressions";
            
    /** Blocks information */
    private final String NAME           = "Name";
    private final String TYPE           = "Type";
    private final String PATH           = "Path";
    private final String VALUE          = "Value";
    private final String LOGIC          = "logic";    
    private final String HANDLE         = "Handle";
    private final String INPORT         = "Inport";    
    private final String OUTPORT        = "Outport";    
    private final String CONTENT        = "Content";    
    private final String OPERATOR       = "Operator";
    private final String SRCBLOCK       = "SrcBlock";
    private final String DSTBLOCK       = "DstBlock";
    private final String BLOCKTYPE      = "BlockType";
    private final String SUBSYSTEM      = "SubSystem"; 
    private final String ACTIONPORT     = "ActionPort";
    private final String TERMINATOR     = "Terminator";
    private final String LINEHANDLES    = "LineHandles";     
    private final String CONNECTIVITY   = "PortConnectivity";
    private final String RELATIONALOP   = "relationaloperator";
    private final String PORTDATATYPE   = "CompiledPortDataTypes";    
    
    /** Lustre nodes name for type conversions */
    private final String BOOLTOINT      = "bool_to_int";
    private final String INTTOBOOL      = "int_to_bool";    
    private final String BOOLTOREAL     = "bool_to_real";
    private final String REALTOBOOL     = "real_to_bool";    
                      
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
            LOGGER.log(Level.INFO, "Found a subsystem block: {0}  ", sanitizeName(subsystemNode.equals(this.topLevelNode) ? this.modelName:subsystemNode.get(NAME).asText()));            
            
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
        String lusNodeName = sanitizeName(subsystemNode.equals(this.topLevelNode) ? this.modelName : subsystemNode.get(NAME).asText());                        
        LOGGER.log(Level.INFO, "Start translating the subsystem block: {0}", lusNodeName);        
        
        List<LustreEq>      props           = new ArrayList<>();
        List<LustreVar>     inputs          = new ArrayList<>();        
        List<LustreVar>     locals          = new ArrayList<>();
        List<LustreVar>     outputs         = new ArrayList<>();
        List<LustreEq>      equations       = new ArrayList<>();
                                        
        /** Terminator node in the input subsystem block */
        List<JsonNode>                      propsNodes                  = new ArrayList<>();   
        /** Outport nodes in the input subsystem block */
        List<JsonNode>                      outportNodes                = new ArrayList<>();        
        /** A mapping between a handle and the block node */
        Map<String, JsonNode>               handleToBlkNodeMap          = new HashMap<>();                
        /** A mapping between a block node and its src block handles */
        Map<JsonNode, List<String>>         blkNodeToSrcBlkHandlesMap   = new HashMap<>();  
        /** A mapping between a block node and its dst block handles */
        Map<JsonNode, List<String>>         blkNodeToDstBlkHandlesMap   = new HashMap<>();         
        /** ALL the fields under content field of subsystem block */
        Iterator<Entry<String, JsonNode>>   contentFields               = subsystemNode.get(CONTENT).fields();
               
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
            LustreEq outportEq = translateOutportEquation(outportNode, subsystemNode, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap);
            
            if(outportEq != null) {
                equations.add(outportEq);
            } else {
                LOGGER.log(Level.SEVERE, "A null outport equation was generated by the translation!");
            }              
        }
        for(JsonNode propNode : propsNodes) {
            List<LustreEq> propEqs = translatePropEquation(propNode, subsystemNode, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap);
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
     * @param subsystemNode
     * @param blkNodeToSrcBlkHandlesMap
     * @param blkNodeToDstBlkHandlesMap
     * @param handleToBlkNodeMap
     * @return A list of property equations
     */
    protected List<LustreEq> translatePropEquation(JsonNode propNode, JsonNode subsystemNode, Map<JsonNode, List<String>> blkNodeToSrcBlkHandlesMap, Map<JsonNode, List<String>> blkNodeToDstBlkHandlesMap, Map<String, JsonNode> handleToBlkNodeMap) {
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
                            
                            LustreExpr rhsExpr = translateBlock(srcBlkHdl, subsystemNode, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap);
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
     * @param subsystemNode
     * @param blkNodeToSrcBlkHandlesMap
     * @param blkNodeToDstBlkHandlesMap
     * @param handleToBlkNodeMap
     * @return Outport equation
     */
    protected LustreEq translateOutportEquation(JsonNode outportNode, JsonNode subsystemNode, Map<JsonNode, List<String>> blkNodeToSrcBlkHandlesMap, Map<JsonNode, List<String>> blkNodeToDstBlkHandlesMap, Map<String, JsonNode> handleToBlkNodeMap) {
        LustreEq    eq          = null;
        VarIdExpr   varIdExpr   = new VarIdExpr(outportNode.get(NAME).asText());        
        
        if(blkNodeToSrcBlkHandlesMap.containsKey(outportNode)) {
            List<String> srcBlkHandles = blkNodeToSrcBlkHandlesMap.get(outportNode);
            
            if(srcBlkHandles.size() == 1) {
                eq = new LustreEq(varIdExpr, translateBlock(srcBlkHandles.get(0), subsystemNode, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap));
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
    

    /**
     * 
     * @param blkHandle
     * @param parentSubsystemNode
     * @param blkNodeToSrcBlkHandlesMap
     * @param blkNodeToDstBlkHandlesMap
     * @param handleToBlkNodeMap
     * @return The Lustre expression corresponding to the input JSON block node
     */

    protected LustreExpr translateBlock(String blkHandle, JsonNode parentSubsystemNode, Map<JsonNode, List<String>> blkNodeToSrcBlkHandlesMap,  Map<JsonNode, List<String>> blkNodeToDstBlkHandlesMap, Map<String, JsonNode> handleToBlkNodeMap) {
        LustreExpr  blkExpr     = null;
        JsonNode    blkNode     = null;
        
        if(handleToBlkNodeMap.containsKey(blkHandle)) {
            blkNode = handleToBlkNodeMap.get(blkHandle);
        } else {
            LOGGER.log(Level.SEVERE, "Unexpected: no block in the model with handle {0}", blkHandle);
        }
        
        if(blkNode != null && blkNode.has(BLOCKTYPE)) {
            String              blkType     = blkNode.get(BLOCKTYPE).asText();            
            List<LustreExpr>    inExprs     = new ArrayList<>();            
            List<String>        inHandles   = new ArrayList<>();
            
            if(blkNodeToSrcBlkHandlesMap.containsKey(blkNode)) {
                inHandles = blkNodeToSrcBlkHandlesMap.get(blkNode);
            }
            if(blkType.toLowerCase().equals(LOGIC) || blkType.toLowerCase().equals(RELATIONALOP) || blkType.equals(MATH)) {
                blkType = blkNode.get(OPERATOR).asText();
            }
            // For the merge block, we need to translate it in a special way
            if(!blkType.equals(MERGE)) {
                LustreType highestType = getTheHighestType(inHandles, handleToBlkNodeMap);
                
                for(String inHandle : inHandles) {
                    LustreExpr inExpr = translateBlock(inHandle, parentSubsystemNode, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap);
                                        
                    inExprs.add(inExpr);
                }                
            }                         
            switch(blkType) {
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
                case NOR: {                                                                               
                    blkExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        blkExpr = new UnaryExpr(UnaryExpr.Op.NOT, new BinaryExpr(blkExpr, BinaryExpr.Op.OR, inExprs.get(i)));   
                    }                                        
                    break;
                }                
                case XOR: {                                                                               
                    blkExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.XOR, inExprs.get(i));   
                    }                                        
                    break;
                }                
                case NXOR: {                                                                               
                    blkExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        blkExpr = new UnaryExpr(UnaryExpr.Op.NOT, new BinaryExpr(blkExpr, BinaryExpr.Op.XOR, inExprs.get(i)));   
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
                case NAND: {                                                                             
                    blkExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        blkExpr = new UnaryExpr(UnaryExpr.Op.NOT, new BinaryExpr(blkExpr, BinaryExpr.Op.AND, inExprs.get(i)));   
                    }                                        
                    break;
                }                 
                case ABS: {
                    blkExpr = new NodeCallExpr(createLustreLibNode(blkNode), inExprs.get(0));                   
                    break;                    
                }
                case SQRT: {
                    blkExpr = new NodeCallExpr(createLustreLibNode(blkNode), inExprs.get(0));                   
                    break;                    
                }
                case MOD: {
                    blkExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.MOD, inExprs.get(i));   
                    }                                        
                    break;                  
                } 
                case UNARYMINUS: {
                    blkExpr = new UnaryExpr(UnaryExpr.Op.NEG, inExprs.get(0));                                                                           
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
                    blkExpr = new NodeCallExpr(sanitizeName(blkNode.get(NAME).asText()), inExprs);
                    break;
                }
                case INPORT: {
                    blkExpr = new VarIdExpr(blkNode.get(NAME).asText());
                    break;
                }
                case CONSTANT: {
                    String      value   = blkNode.get(VALUE).asText().trim();
                    LustreType  type    = getBlockOutportType(blkNode);
                    
                    if(isValidConst(value)) {
                        blkExpr = getLustreConst(value, type);
                    //The value of the constant field of CompareToConstant block refers to some field of its parent node
                    } else if(parentSubsystemNode.has(value)) {
                        blkExpr = getLustreConst(parentSubsystemNode.get(value).asText().trim(), type);
                    } else {
                        LOGGER.log(Level.SEVERE, "Unexpected constant value in JSON node: {0}", value);
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
                case MEMORY: 
                case UNITDELAY: {
                    String      init        = blkType.equals(MEMORY) ? blkNode.get(X0).asText() : blkNode.get(INITCOND).asText();                                        
                    LustreType  blkOutType  = getBlockOutportType(blkNode); 
                    LustreExpr  initExpr    = getLustreConst(init, blkOutType);
                    
                    blkExpr     = new UnaryExpr(UnaryExpr.Op.PRE, inExprs.get(0));                      
                    if(initExpr != null) {
                        blkExpr = new BinaryExpr(initExpr, BinaryExpr.Op.ARROW, blkExpr);
                    } else {
                        LOGGER.log(Level.SEVERE, "UNEXPECTED init value : {0} for memory or unit delay block", init);
                    }
                    break;
                }
                case IF: {
                    break;
                }
                // Merge block combines its inputs (conditionally-executed subsystems) into a single output 
                // line whose value at any time is equal to the most recently computed output of its driving blocks
                // Now, we only consider merge block connects with conditionally-executed subsystems from the same IF block
                case MERGE: {
                    String                  ifBlkHandle      = null;                    
                    Map<String, JsonNode>   hdlActSysNodeMap = new HashMap<>();
                    Map<String, LustreExpr> hdlActSysExprMap = new HashMap<>();
                    
                    for(String inHandle : inHandles) {
                        JsonNode inBlk = handleToBlkNodeMap.get(inHandle);
                        
                        if(isIfActionSubsystem(inBlk)) {
                            hdlActSysNodeMap.put(inHandle, inBlk);
                        } else {
                            LOGGER.log(Level.SEVERE, "UNSUPPORTED: input to MERGE block is not a IF-ACTION-SUBSYSTEM!", blkType);
                        }
                    }
                    if(hdlActSysNodeMap.size() >= 2 && hdlActSysNodeMap.size() == inHandles.size()) {
                        // Translate each action block into a node call
                        for(Entry<String, JsonNode> entry : hdlActSysNodeMap.entrySet()) {
                            JsonNode actSubsystemNode = entry.getValue();
                            if(blkNodeToSrcBlkHandlesMap.containsKey(actSubsystemNode)) {
                                List<LustreExpr>    srcExprs    = new ArrayList<>();
                                List<String>        srcHandles  = blkNodeToSrcBlkHandlesMap.get(actSubsystemNode);                                
                                
                                for(String srcHandle : srcHandles) {
                                    if(isIfBlock(handleToBlkNodeMap.get(srcHandle))) {
                                        if(ifBlkHandle == null) {
                                            ifBlkHandle = srcHandle;
                                        } else if(!ifBlkHandle.equals(srcHandle)){
                                            LOGGER.log(Level.SEVERE, "UNEXPECTED: IF-ACTION-SUBSYSTEMs to the same MERGE block connects with different IF blocks", srcHandle);
                                        }
                                    } else {
                                        srcExprs.add(translateBlock(srcHandle, parentSubsystemNode, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap));
                                    }
                                }
                                hdlActSysExprMap.put(entry.getKey(), new NodeCallExpr(sanitizeName(actSubsystemNode.get(NAME).asText()), srcExprs));
                            }                            
                        }                        
                    }
                    if(handleToBlkNodeMap.containsKey(ifBlkHandle)) {
                        blkExpr = translateIfBlock(handleToBlkNodeMap.get(ifBlkHandle), hdlActSysExprMap, parentSubsystemNode, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap);
                    }
                    break;
                }                 
                default:
                    LOGGER.log(Level.SEVERE, "Unsupported block type: {0}!", blkType);
                    break;
            }
        }                       
        
        return blkExpr;
    }
    
    protected LustreExpr translateIfBlock(JsonNode ifBlkNode, Map<String, LustreExpr> hdlActSysExprMap, JsonNode parentSubsystemNode, Map<JsonNode, List<String>> blkNodeToSrcBlkHandlesMap,  Map<JsonNode, List<String>> blkNodeToDstBlkHandlesMap, Map<String, JsonNode> handleToBlkNodeMap) {
        LustreExpr blkExpr = null;
        
        if(ifBlkNode != null) {            
            //Since Simulink checks the validity of ifCondExpr and elseIfCondExpr, we assume they are valid in the input.
            String              ifCondExpr      = ifBlkNode.get(IFEXPRESSION).asText().trim();
            String              elseIfCondExpr  = ifBlkNode.get(ELSEIFEXPRS).asText().trim();
            List<String>        dstBlockHandles = blkNodeToDstBlkHandlesMap.get(ifBlkNode);
            List<LustreExpr>    inExprs         = new ArrayList<>();
            List<LustreExpr>    condExprs       = new ArrayList<>();
            List<String>        condStrExprs    = new ArrayList<>();
                       
            if(ifCondExpr != null && !ifCondExpr.equals("")) {
                condStrExprs.add(ifCondExpr);
            }
            if(elseIfCondExpr != null && !elseIfCondExpr.equals("")) {
                condStrExprs.addAll(Arrays.asList(elseIfCondExpr.split(",")));
            }
                        
            if(blkNodeToSrcBlkHandlesMap.containsKey(ifBlkNode)) {
                for(String inHandle : blkNodeToSrcBlkHandlesMap.get(ifBlkNode)) {
                    inExprs.add(translateBlock(inHandle, parentSubsystemNode, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap));
                }  
            }
            // The number of condition expressions is one less than the number of branches
            if(condStrExprs.size() == hdlActSysExprMap.size()-1) {
                condExprs = buildIteConExprs(condStrExprs, inExprs);
            } else {
                LOGGER.log(Level.SEVERE, "UNEXPECTED: there is a mismatch in IF block between Ite conditions with its outputs!");
            }
        }
        return blkExpr;
    }
    
    protected List<LustreExpr> buildIteConExprs(List<String> condStrExprs, List<LustreExpr> inExprs) {
        List<LustreExpr> condExprs = new ArrayList<>();
        
        for(String condStrExpr : condStrExprs) {
            condExprs.add(buildIteCondExpr(condStrExpr, inExprs));
        }
        return condExprs;
    }
    
    // Assumption: condStrExpr is a valid string representaiton of a condition expression.
    protected LustreExpr buildIteCondExpr(String strExpr, List<LustreExpr> inExprs) {        
        LustreExpr  lusExpr = null;
        String      condStrExpr         = strExpr.trim();
        Matcher     ltMatcher           = Pattern.compile("^[\\(]?[-]?\\s*[\\(]?\\s*u\\d+\\s*[\\)]?\\s*<\\s*[\\(]?\\s*[-]?\\s*[\\(]?\\s*u\\d+[\\)]?$").matcher(condStrExpr);
        Matcher     gtMatcher           = Pattern.compile("^u\\d+\\s*>\\s*u\\d+$").matcher(condStrExpr);
        Matcher     lteMatcher          = Pattern.compile("^u\\d+\\s*<=\\s*u\\d+$").matcher(condStrExpr);
        Matcher     gteMatcher          = Pattern.compile("^u\\d+\\s*>=\\s*u\\d+$").matcher(condStrExpr);
        Matcher     eqMatcher           = Pattern.compile("^u\\d+\\s*==\\s*u\\d+$").matcher(condStrExpr);
        Matcher     neqMatcher          = Pattern.compile("^u\\d+\\s*~=\\s*u\\d+$").matcher(condStrExpr);
        Matcher     andMatcher          = Pattern.compile("^u\\d+\\s*~=\\s*u\\d+$").matcher(condStrExpr);
        Matcher     orMatcher           = Pattern.compile("^u\\d+\\s*~=\\s*u\\d+$").matcher(condStrExpr);
        Matcher     varMatcher          = Pattern.compile("^u\\d+$").matcher(condStrExpr);
        Matcher     minusMatcher        = Pattern.compile("^[-][\\(]\\s*[\\(]*\\s*u\\d+\\s*[\\)]*\\s*[\\)]*$").matcher(condStrExpr);
              
        // conStrExpr is an input var like "u1, u2, ..."
        if(isParathesized(condStrExpr)) {
            lusExpr = buildIteCondExpr(condStrExpr.substring(1, condStrExpr.length()-1), inExprs);
        } else if(minusMatcher.find()) {
            lusExpr = new UnaryExpr(UnaryExpr.Op.NEG, buildIteCondExpr(condStrExpr.substring(1), inExprs));
        } else if(varMatcher.find()) {
            int index = Integer.parseInt(condStrExpr.substring(1));
            
            if(index <= inExprs.size() && index > 0) {
                lusExpr = inExprs.get(index-1);
            } else {
                 LOGGER.log(Level.SEVERE, "UNEXPECTED index in buildIteCondExpr: {0}", index);
            }
        } else if(ltMatcher.find()) {
            
        } else if(gtMatcher.find()) {
            
        } else if(lteMatcher.find()) {
            
        } else if(gteMatcher.find()) {
            
        } else if(eqMatcher.find()) {
            
        } else if(neqMatcher.find()) {
            
        } else if(andMatcher.find()) {

        } else if(orMatcher.find()) {
            
        }         
        return lusExpr;
    }
    
    protected boolean isIfBlock(JsonNode node) {
        if(node != null) {
            if(node.has(BLOCKTYPE) && node.get(BLOCKTYPE).asText().equals(IF)) {
                return true;
            }                        
        }
        return false;        
    }
    
    protected boolean isIfActionSubsystem(JsonNode node) {
        if(node != null) {
            if(node.has(BLOCKTYPE)) {
                if(node.get(BLOCKTYPE).asText().equals(SUBSYSTEM)) {
                    Iterator<Entry<String, JsonNode>> contentFields = node.get(CONTENT).fields();  

                    while(contentFields.hasNext()) {
                        JsonNode fieldNode = contentFields.next().getValue();

                        if(fieldNode.has(BLOCKTYPE) && fieldNode.get(BLOCKTYPE).equals(ACTIONPORT)) {
                            return true;
                        }
                    }
                }
            }            
        }
        return false;
    }

    protected LustreExpr getLustreConst(String value, LustreType type) {
        LustreExpr constExpr = null;
        
        if(isValidConst(value)) {
            if(type == PrimitiveType.REAL) {
                if(!value.contains(".")) {
                    value = value + ".0";
                }
                constExpr = new RealExpr(new BigDecimal(value));
            } else if(type == PrimitiveType.INT) {
                constExpr = new IntExpr(new BigInteger(value));
            } else if(type == PrimitiveType.BOOL) {
                constExpr = new BooleanExpr(value);
            } else {
                LOGGER.log(Level.SEVERE, "Unsupported constant datatype: {0}", type);
            }            
        } else {
            LOGGER.log(Level.SEVERE, "Unexpected constant value: {0}", value);
        } 
        return constExpr;
    }
    
    protected LustreType getSwitchCondType(JsonNode switchBlk) {        
        List<String>        types   = convertJsonValuesToList(switchBlk.get(PORTDATATYPE).get(INPORT));
        Iterator<JsonNode>  connIt  = switchBlk.get(CONNECTIVITY).elements(); 
        // The switch condition is the second port in the port connectivity
        return getLustreTypeFromStrRep(types.get(connIt.next().get(TYPE).asInt()));
    }
    
    /**
     * @param blkNode
     * @return The name of library Lustre node corresponding to the input JsonNode
     */
    protected String createLustreLibNode(JsonNode blkNode) {
        String path     = blkNode.get(PATH).asText();
        String nodeName = path.replace(File.separator,"_");
        String blkType  = blkNode.get(BLOCKTYPE).asText();
        
        switch(blkType) {
            case BOOLTOINT: {
                nodeName = BOOLTOINT;
                if(this.libNodeNameMap.containsKey(BOOLTOINT)) {                    
                    break;
                }
                
                VarIdExpr           inVarExpr   = new VarIdExpr("in");
                VarIdExpr           outVarExpr  = new VarIdExpr("out");   
                List<LustreEq>      bodyExprs   = new ArrayList<>();    

                bodyExprs.add(new LustreEq(outVarExpr, new IteExpr(inVarExpr, new IntExpr(BigInteger.ONE), new IntExpr(BigInteger.ZERO))));
                this.libNodeNameMap.put(BOOLTOINT, BOOLTOINT);
                this.lustreProgram.addNode(new LustreNode(nodeName, new LustreVar("in", PrimitiveType.BOOL), new LustreVar("out", PrimitiveType.INT), bodyExprs));                                
                break;
            }
            case BOOLTOREAL: {
                nodeName = BOOLTOREAL;
                if(this.libNodeNameMap.containsKey(BOOLTOREAL)) {                    
                    break;
                }
                
                VarIdExpr           inVarExpr   = new VarIdExpr("in");
                VarIdExpr           outVarExpr  = new VarIdExpr("out");   
                List<LustreEq>      bodyExprs   = new ArrayList<>();    

                bodyExprs.add(new LustreEq(outVarExpr, new IteExpr(inVarExpr, new RealExpr(BigDecimal.ONE), new RealExpr(BigDecimal.ZERO))));
                this.libNodeNameMap.put(BOOLTOREAL, BOOLTOREAL);
                this.lustreProgram.addNode(new LustreNode(nodeName, new LustreVar("in", PrimitiveType.BOOL), new LustreVar("out", PrimitiveType.REAL), bodyExprs));                                
                break;
            }  
            case INTTOBOOL: {
                nodeName = INTTOBOOL;
                if(this.libNodeNameMap.containsKey(INTTOBOOL)) {                    
                    break;
                }
                
                VarIdExpr           inVarExpr   = new VarIdExpr("in");
                VarIdExpr           outVarExpr  = new VarIdExpr("out");   
                List<LustreEq>      bodyExprs   = new ArrayList<>();    

                bodyExprs.add(new LustreEq(outVarExpr, new IteExpr(new BinaryExpr(inVarExpr, BinaryExpr.Op.NEQ, new IntExpr(new BigInteger("0"))), new BooleanExpr(true), new BooleanExpr(false))));
                this.libNodeNameMap.put(INTTOBOOL, INTTOBOOL);
                this.lustreProgram.addNode(new LustreNode(nodeName, new LustreVar("in", PrimitiveType.INT), new LustreVar("out", PrimitiveType.BOOL), bodyExprs));                                
                break;
            } 
            case REALTOBOOL: {
                nodeName = REALTOBOOL;
                if(this.libNodeNameMap.containsKey(REALTOBOOL)) {                    
                    break;
                }
                
                VarIdExpr           inVarExpr   = new VarIdExpr("in");
                VarIdExpr           outVarExpr  = new VarIdExpr("out");   
                List<LustreEq>      bodyExprs   = new ArrayList<>();    

                bodyExprs.add(new LustreEq(outVarExpr, new IteExpr(new BinaryExpr(inVarExpr, BinaryExpr.Op.NEQ, new RealExpr(BigDecimal.ZERO)), new BooleanExpr(true), new BooleanExpr(false))));
                this.libNodeNameMap.put(REALTOBOOL, REALTOBOOL);
                this.lustreProgram.addNode(new LustreNode(nodeName, new LustreVar("in", PrimitiveType.REAL), new LustreVar("out", PrimitiveType.BOOL), bodyExprs));                                
                break;
            }             
            case ABS: {
                LustreType  outType = getBlockOutportType(blkNode);
                
                if(this.libNodeNameMap.containsKey(ABS+"_"+outType)) {
                    nodeName = this.libNodeNameMap.get(ABS+"_"+outType);
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
                this.libNodeNameMap.put(ABS+"_"+outType, nodeName);
                this.lustreProgram.addNode(new LustreNode(nodeName, new LustreVar("in", outType), new LustreVar("out", outType), bodyExprs));                
                break;
            }
            case SQRT: {
                LustreType  outType = getBlockOutportType(blkNode);
                
                if(this.libNodeNameMap.containsKey(SQRT+"_"+outType)) {
                    nodeName = this.libNodeNameMap.get(SQRT+"_"+outType);
                    break;
                }
                if(outType == PrimitiveType.INT) {
                    nodeName    = SQRT + "_int";              
                } else if(outType == PrimitiveType.REAL) {
                    nodeName    = SQRT + "_real";
                } else {
                    LOGGER.log(Level.SEVERE, "Unsupported library node input and output type: {0}", outType);
                }
                this.libNodeNameMap.put(nodeName, nodeName);
                this.lustreProgram.addNode(new LustreNode(nodeName, new LustreVar("in", outType), new LustreVar("out", outType), new ArrayList<LustreEq>()));                
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
    
    
    protected LustreType getTheHighestType(List<String> handles, Map<String, JsonNode> handleToBlkNodeMap) {        
        LustreType highestType = null;
        
        if(handles != null && handles.size() > 0) {
             highestType = getBlockOutportType(handleToBlkNodeMap.get(handles.get(0)));
             
             for(int i = 1; i < handles.size(); i++) {
                 LustreType hType = getBlockOutportType(handleToBlkNodeMap.get(handles.get(i)));
                 
                 if(highestType.compareTo(hType) < 0) {
                     highestType = hType;
                 }
             }
        }
        return highestType;
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
    
    protected boolean isNum(String str) {
        return str.matches("^[-+]?\\d+(\\.\\d+)?$");
    }
    
    protected boolean isBool(String str) {
        return str.toLowerCase().equals("true") || str.toLowerCase().equals("false") ;
    }    
    
    protected boolean isValidConst(String str) {
        return isNum(str) || isBool(str);
    }
    
    protected boolean isNumType(LustreType type) {
        return type == PrimitiveType.REAL || type == PrimitiveType.INT;
    }  
    
    protected String sanitizeName(String name) {
        return name.replace(" ", "_").replace("\n", "_");
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
    
    protected boolean isParathesized(String s) {
        if(s == null) {
            return false;
        }
        
        String candStr = s.trim();
        
        if(!candStr.startsWith("(") || !candStr.endsWith(")")) {
            return false;
        }
        
        Stack<Character> stack  = new Stack<>();
        
        stack.push('(');
        for(int i = 1; i < candStr.length()-1; i++) {
            char c = candStr.charAt(i);
            if(c == '(') {     
                stack.push(c);
            } else if(c == ')') {
                stack.pop();
                if(stack.isEmpty()) {
                    return false;
                }
            }
        }
        return !stack.isEmpty();
    }      
    
}
