/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.uiowa.json2lus.lustreAst.BinaryExpr;
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
import edu.uiowa.json2lus.lustreAst.UnaryExpr;
import edu.uiowa.json2lus.lustreAst.VarIdExpr;
import java.io.File;
import java.io.IOException;
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
    
    /** Blocks information */
    private final String NAME           = "Name";
    private final String HANDLE         = "Handle";
    private final String INPORT         = "Inport";    
    private final String OUTPORT        = "Outport";    
    private final String CONTENT        = "Content";
    private final String OPERATOR       = "Operator";
    private final String SRCBLOCK       = "SrcBlock";
    private final String DSTBLOCK       = "DstBlock";
    private final String BLOCKTYPE      = "BlockType";
    private final String SUBSYSTEM      = "SubSystem"; 
    private final String LINEHANDLES    = "LineHandles";        
    private final String CONNECTIVITY   = "PortConnectivity";
    private final String PORTDATATYPE   = "CompiledPortDataTypes";  
        
    
    private LustreProgram           lustreProgram;
    private Set<JsonNode>           subsystemNodes;
    private JsonNode                topLevelNode = null;    
    
            
    /**
     * Constructor
     * @param inputPath
     */
    public J2LTranslator(String inputPath) {        
        this.inputPath          = inputPath;        
        this.modelName          = inputPath.toLowerCase().endsWith(".json") ? 
                                    inputPath.substring(inputPath.lastIndexOf(File.separator)+1, inputPath.lastIndexOf("."))
                                    : inputPath.substring(inputPath.lastIndexOf(File.separator)+1);            
        this.lustreProgram      = new LustreProgram();
        this.subsystemNodes     = new HashSet<>();
    }
    
    /**
     * Execute the translation process 
     * 
     * @return lustreProgram
     */    
    public LustreProgram execute() {
        collectSubsytemBlocksInfo();
        this.subsystemNodes.forEach(node -> this.lustreProgram.addNode(translateSubsystemNode(node)));
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
            LOGGER.log(Level.SEVERE, "JSON root node: {0} content definition is an unexpected format!", rootNode);
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
                    if(fieldNode.get(BLOCKTYPE).asText().equals(SUBSYSTEM)) {                        
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
     * @return 
     */
    protected LustreNode translateSubsystemNode(JsonNode subsystemNode) {                
        String              lusNodeName     = subsystemNode.equals(this.topLevelNode) ? this.modelName : subsystemNode.get(NAME).asText();
        List<LustreVar>     inputs          = new ArrayList<>();
        List<LustreVar>     outputs         = new ArrayList<>();
        List<LustreVar>     locals          = new ArrayList<>();
        List<LustreEq>      equations       = new ArrayList<>();        
        
        List<JsonNode>                      outportNodes                = new ArrayList<>();
        Iterator<Entry<String, JsonNode>>   contentFields               = subsystemNode.get(CONTENT).fields();        
        /** A mapping between block outport handles to the block node */
        Map<List<String>, JsonNode>         outHandleToBlockNodeMap     = new HashMap<>();        
        /** A mapping between a block node to its inport handles */
        Map<JsonNode, List<String>>         blockNodeToInHandlesMap     = new HashMap<>();        
        
        while(contentFields.hasNext()) {
            Map.Entry<String, JsonNode> contField       = contentFields.next();   
            JsonNode                    contFieldNode   = contField.getValue();
            
            if(contFieldNode.has(BLOCKTYPE)) {
                // Top-level subsystem block does not have LineHandles field
                if(contFieldNode.has(LINEHANDLES)) {
                    List<String>        inHandlesList   = new ArrayList<>();
                    List<String>        outHandlesList  = new ArrayList<>();
                    JsonNode            inHandles       = contFieldNode.get(LINEHANDLES).get(INPORT);
                    JsonNode            outHandles      = contFieldNode.get(LINEHANDLES).get(OUTPORT);                    
                    
                    if(inHandles.isArray()) {
                        Iterator<JsonNode> inHandleIt = inHandles.elements();
                        
                        while(inHandleIt.hasNext()) {
                            inHandlesList.add(inHandleIt.next().asText());
                        }                        
                    } else {
                        inHandlesList.add(inHandles.asText());                        
                    }                    
                    if(outHandles.isArray()) {
                        Iterator<JsonNode> outHandleIt = outHandles.elements();   
                        
                        while(outHandleIt.hasNext()) {                  
                            outHandlesList.add(outHandleIt.next().asText());
                        }                         
                    } else {
                        outHandlesList.add(outHandles.asText());                        
                    }   
                    if(!inHandlesList.isEmpty()) {
                        blockNodeToInHandlesMap.put(contFieldNode, inHandlesList);
                    }                    
                    if(!outHandlesList.isEmpty()) {
                        outHandleToBlockNodeMap.put(outHandlesList, contFieldNode);
                    }
                }                 
                
                
                /** Translate inports and outports of subsystem blocks */
                switch(contFieldNode.get(BLOCKTYPE).asText()) {
                    case INPORT: {                        
                        String      name = contFieldNode.get(NAME).asText();
                        LustreType  type = getLustreTypeFromStrRep(contFieldNode.get(PORTDATATYPE).get(OUTPORT).asText());
                        LustreVar   var  = new LustreVar(name, type);
                        inputs.add(var);
                        break;
                    }
                    case OUTPORT: {
                        String      name = contFieldNode.get(NAME).asText();
                        LustreType  type = getLustreTypeFromStrRep(contFieldNode.get(PORTDATATYPE).get(INPORT).asText());
                        LustreVar   var  = new LustreVar(name, type);
                        outputs.add(var);      
                        outportNodes.add(contFieldNode);
                        break;
                    }
                    default:
                        break;
                }                 
            }            
        }
        
        // Translate outport equations backwards
        outportNodes.forEach((outportNode) -> {
            equations.add(translateOutportEquation(outportNode, blockNodeToInHandlesMap, outHandleToBlockNodeMap));
        });
        return new LustreNode(lusNodeName, inputs, outputs, locals, equations);
    }
    
    /**
     * 
     * @param outportNode
     * @param blockToInHandlesMap
     * @param outHandleToBlockMap
     * @return Outport equation
     */
    protected LustreEq translateOutportEquation(JsonNode outportNode, Map<JsonNode, List<String>> blockToInHandlesMap, Map<List<String>, JsonNode> outHandleToBlockMap) {
        LustreEq    eq          = null;
        VarIdExpr   varIdExpr   = new VarIdExpr(outportNode.get(NAME).asText());        
        
        if(blockToInHandlesMap.containsKey(outportNode)) {
            List<String> inHandles = blockToInHandlesMap.get(outportNode);
            
            if(inHandles.size() == 1) {
                eq = new LustreEq(varIdExpr, translateRhsForOutportEq(inHandles, blockToInHandlesMap, outHandleToBlockMap));
            } else {
                //Todo: the merge operator might need this support!
                LOGGER.log(Level.SEVERE, "Not supported: Multiple src blocks connect to the same outport!");
            }
        }
        
        return eq;
    }
    
    /**
     * 
     * @param inHandels
     * @param blockNodeToInHandlesMap
     * @param outHandleToBlockNodeMap
     * @return Right hand side expression of an outport equation
     */
    protected LustreExpr translateRhsForOutportEq(List<String> inHandels, Map<JsonNode, List<String>> blockNodeToInHandlesMap, Map<List<String>, JsonNode> outHandleToBlockNodeMap) {
        // The rhs of an outport is the single output of some block
        if(outHandleToBlockNodeMap.containsKey(inHandels)) {
            return translateRhsForOutportEq(outHandleToBlockNodeMap.get(inHandels), blockNodeToInHandlesMap, outHandleToBlockNodeMap);        
        // The rhs of an outport could be one of the return result of a node call or an idle outport
        } else {
            LOGGER.log(Level.SEVERE, "Not supported yet: The outport might be one of the return result of a node call or the outport is an idle outport");
        }                        
        
        return null;
    }
    
    /**
     * 
     * @param blockNode
     * @param blockNodeToInHandlesMap
     * @param outHandleToBlockNodeMap
     * @return Right hand side expression of an outport equation
     */
    protected LustreExpr translateRhsForOutportEq(JsonNode blockNode, Map<JsonNode, List<String>> blockNodeToInHandlesMap, Map<List<String>, JsonNode> outHandleToBlockNodeMap) {
        LustreExpr rhsExpr = null;
        
        if(blockNode.has(BLOCKTYPE)) {
            String              blockType   = blockNode.get(BLOCKTYPE).asText();
            List<LustreExpr>    inExprs     = new ArrayList<>();            
            List<String>        inHandles   = blockNodeToInHandlesMap.get(blockNode);
            
            if(blockType.toLowerCase().equals("logic") || blockType.toLowerCase().equals("relationaloperator")) {
                blockType = blockNode.get(OPERATOR).asText();
            }
            if(inHandles != null) {
                inHandles.forEach(inHandle -> {
                    inExprs.add(translateRhsForOutportEq(Arrays.asList(inHandle), blockNodeToInHandlesMap, outHandleToBlockNodeMap));
                });                 
            }                         
            switch(blockType) {
                case EQ: {                                                            
                    rhsExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.EQ, inExprs.get(1));                                       
                    break;
                }  
                case NEQ: {                                                            
                    rhsExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.NEQ, inExprs.get(1));                                       
                    break;
                } 
                case GTE: {                                                            
                    rhsExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.GTE, inExprs.get(1));                                       
                    break;
                } 
                case LTE: {                                                            
                    rhsExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.LTE, inExprs.get(1));                                       
                    break;
                }  
                case GT: {                                                            
                    rhsExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.GT, inExprs.get(1));                                       
                    break;
                }  
                case LT: {                                                            
                    rhsExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.LT, inExprs.get(1));                                       
                    break;
                }                  
                case NOT: {                                                                             
                    rhsExpr = new UnaryExpr(UnaryExpr.Op.NOT, inExprs.get(0));                                      
                    break;
                }                   
                case OR: {                                                                               
                    rhsExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        rhsExpr = new BinaryExpr(rhsExpr, BinaryExpr.Op.OR, inExprs.get(i));   
                    }                                        
                    break;
                }                 
                case AND: {                                                                             
                    rhsExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        rhsExpr = new BinaryExpr(rhsExpr, BinaryExpr.Op.AND, inExprs.get(i));   
                    }                                        
                    break;
                }                
                case ABS: {                  
                    rhsExpr = new IteExpr(new BinaryExpr(inExprs.get(0), BinaryExpr.Op.GTE, new IntExpr(new BigInteger("0"))), inExprs.get(0), new UnaryExpr(UnaryExpr.Op.NEG, inExprs.get(0)));                                      
                    break;                    
                }
                case SUM: {                                                                              
                    rhsExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        rhsExpr = new BinaryExpr(rhsExpr, BinaryExpr.Op.PLUS, inExprs.get(i));   
                    }                                        
                    break;
                }
                case SUBSYSTEM: {    
                    rhsExpr = new NodeCallExpr(blockNode.get(NAME).asText(), inExprs);
                    break;
                }
                case INPORT: {
                    rhsExpr = new VarIdExpr(blockNode.get(NAME).asText());
                    break;
                }
                default:
                    LOGGER.log(Level.SEVERE, "Unsupported block type: {0}!", blockType);
                    break;
            }
        }                       
        
        return rhsExpr;
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
            case "double": {
                lusType = PrimitiveType.REAL;
                break;
            }
            case "integer": 
            case "int": {
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
}
