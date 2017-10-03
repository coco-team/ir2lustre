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
import edu.uiowa.json2lus.lustreAst.LustreAst;
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
    private final Logger logger;
    
    /** JSON attributes */
    private final String SUM            = "Sum";
    private final String ABS            = "Abs";
    private final String NAME           = "Name";
    private final String HANDLE         = "Handle";
    private final String INPORT         = "Inport";    
    private final String OUTPORT        = "Outport";    
    private final String CONTENT        = "Content";
    private final String SRCBLOCK       = "SrcBlock";
    private final String DSTBLOCK       = "DstBlock";
    private final String BLOCKTYPE      = "BlockType";
    private final String SUBSYSTEM      = "SubSystem"; 
    private final String LINEHANDLES    = "LineHandles";     
    private final String PORTDATATYPE   = "CompiledPortDataTypes";  
    
    
    private LustreProgram           lustreProgram;
    private JsonNode                topLevelNode = null;
    private Set<JsonNode>           subsystemNodes;
    
            
    /**
     * Constructor
     * @param inputPath
     */
    public J2LTranslator(String inputPath) {        
        this.inputPath          = inputPath;        
        this.modelName          = inputPath.toLowerCase().endsWith(".json") ? 
                                    inputPath.substring(inputPath.lastIndexOf(File.separator)+1, inputPath.lastIndexOf("."))
                                    : inputPath.substring(inputPath.lastIndexOf(File.separator)+1);            
        this.logger             = Logger.getLogger(J2LTranslator.class.getName());
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
    
    protected void collectSubsytemBlocksInfo() {
        JsonNode        rootNode    = null;        
        ObjectMapper    mapper      = new ObjectMapper();
        
        try {
            rootNode = mapper.readTree(new File(this.inputPath));
        } catch (IOException ex) {
            Logger.getLogger(J2LTranslator.class.getName()).log(Level.SEVERE, null, ex);
        }

        if(rootNode != null && rootNode.has(this.modelName)) {
            JsonNode topLevelNode = rootNode.get(this.modelName);
            this.topLevelNode = topLevelNode;
            collectSubsytemBlocksInfo(topLevelNode);               
        } else {
            this.logger.log(Level.SEVERE, "JSON root node: {0} content definition is an unexpected format!", rootNode);
        }   
    }
    
    protected void collectSubsytemBlocksInfo(JsonNode subsystemNode) {
        if(subsystemNode != null && !this.subsystemNodes.contains(subsystemNode) && subsystemNode.has(CONTENT)) {
            this.logger.log(Level.INFO, "Found a subsystem block: {0}  ", subsystemNode.equals(this.topLevelNode) ? this.modelName:subsystemNode.get(NAME).asText());            
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
            this.logger.log(Level.SEVERE, "Cannot find the Cocosim model: {0} content definition in the input JSON file!", this.modelName);
        }       
    }
    
    protected LustreNode translateSubsystemNode(JsonNode subsystemNode) {                
        String              lusNodeName     = subsystemNode.equals(this.topLevelNode) ? this.modelName : subsystemNode.get(NAME).asText();
        List<LustreVar>     inputs          = new ArrayList<>();
        List<LustreVar>     outputs         = new ArrayList<>();
        List<LustreVar>     locals          = new ArrayList<>();
        List<LustreEq>      equations       = new ArrayList<>();
        
        /** A mapping between block outport handles to the block node */
        Map<List<String>, JsonNode> outHandleToBlockNodeMap  = new HashMap<>();
        
        /** A mapping between a block node to its inport handles */
        Map<JsonNode, List<String>> blockNodeToInHandlesMap  = new HashMap<>();
        
        Iterator<Entry<String, JsonNode>> fields = subsystemNode.fields();        

        while(fields.hasNext()) {
            Map.Entry<String, JsonNode> field       = fields.next();   
            JsonNode                    fieldNode   = field.getValue();
            
            /** Populate src block hanldes and outgoing handles */
            if(fieldNode.has(BLOCKTYPE)) {                
                if(fieldNode.has(LINEHANDLES)) {
                    List<String>        inHandlesList   = new ArrayList<>();
                    List<String>        outHandlesList  = new ArrayList<>();
                    JsonNode            inHandles       = fieldNode.get(LINEHANDLES).get(INPORT);
                    JsonNode            outHandles      = fieldNode.get(LINEHANDLES).get(OUTPORT);                    
                    
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
                        blockNodeToInHandlesMap.put(fieldNode, inHandlesList);
                    }                    
                    if(!outHandlesList.isEmpty()) {
                        outHandleToBlockNodeMap.put(outHandlesList, fieldNode);
                    }
                }               
            }
        }
        
        List<JsonNode>                      outportNodes    = new ArrayList<>();
        Iterator<Entry<String, JsonNode>>   contentFields   = subsystemNode.get(CONTENT).fields();
        
        while(contentFields.hasNext()) {
            Map.Entry<String, JsonNode> contField       = contentFields.next();   
            JsonNode                    contFieldNode   = contField.getValue();
            if(contFieldNode.has(BLOCKTYPE)) {
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
                
                
                /** Populate inports and outports to blocks maps*/
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
        
        outportNodes.forEach((outportNode) -> {
            equations.add(translateOutportEquation(outportNode, blockNodeToInHandlesMap, outHandleToBlockNodeMap));
        });
        return new LustreNode(lusNodeName, inputs, outputs, locals, equations);
    }
    
    protected LustreEq translateOutportEquation(JsonNode outportNode, Map<JsonNode, List<String>> blockNodeToInHandlesMap, Map<List<String>, JsonNode> outHandleToBlockNodeMap) {
        LustreEq    eq          = null;
        VarIdExpr   varIdExpr   = new VarIdExpr(outportNode.get(NAME).asText());        
        
        if(blockNodeToInHandlesMap.containsKey(outportNode)) {
            List<String> inHandles = blockNodeToInHandlesMap.get(outportNode);
            
            if(inHandles.size() == 1) {
                eq = new LustreEq(varIdExpr, translateRhsForOutportEq(inHandles, blockNodeToInHandlesMap, outHandleToBlockNodeMap));
            } else {
                //Todo: the merge operator might need this support!
                this.logger.log(Level.SEVERE, "Not supported: Multiple src blocks connect to the same outport!");
            }
        }
        
        return eq;
    }
    
    protected LustreExpr translateRhsForOutportEq(List<String> inHandels, Map<JsonNode, List<String>> blockNodeToInHandlesMap, Map<List<String>, JsonNode> outHandleToBlockNodeMap) {
        // The rhs of an outport is the single output of some block
        if(outHandleToBlockNodeMap.containsKey(inHandels)) {
            return translateRhsForOutportEq(outHandleToBlockNodeMap.get(inHandels), blockNodeToInHandlesMap, outHandleToBlockNodeMap);        
        // The rhs of an outport could be one of the return result of a node call or an idle outport
        } else {
            this.logger.log(Level.SEVERE, "Not supported yet: The rhs of an outport could be one of the return result of a node call or an idle outport");
        }                        
        
        return null;
    }
    
    protected LustreExpr translateRhsForOutportEq(JsonNode blockNode, Map<JsonNode, List<String>> blockNodeToInHandlesMap, Map<List<String>, JsonNode> outHandleToBlockNodeMap) {
        LustreExpr rhsExpr = null;
        
        if(blockNode.has(BLOCKTYPE)) {
            List<LustreExpr>    inExprs     = new ArrayList<>();            
            List<String>        inHandles   = blockNodeToInHandlesMap.get(blockNode);
                        
            switch(blockNode.get(BLOCKTYPE).asText()) {
                case ABS: {
                    inHandles.forEach(inHandle -> {
                        inExprs.add(translateRhsForOutportEq(Arrays.asList(inHandle), blockNodeToInHandlesMap, outHandleToBlockNodeMap));
                    });                    
                    rhsExpr = new IteExpr(new BinaryExpr(inExprs.get(0), BinaryExpr.Op.GTE, new IntExpr(new BigInteger("0"))), inExprs.get(0), new UnaryExpr(UnaryExpr.Op.NEG, inExprs.get(0)));                                      
                    break;                    
                }
                case SUM: {                                                            
                    inHandles.forEach(inHandle -> {
                        inExprs.add(translateRhsForOutportEq(Arrays.asList(inHandle), blockNodeToInHandlesMap, outHandleToBlockNodeMap));
                    });                    
                    rhsExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        rhsExpr = new BinaryExpr(rhsExpr, BinaryExpr.Op.PLUS, inExprs.get(i));   
                    }                                        
                    break;
                }
                case SUBSYSTEM: {
                    inHandles.forEach(inHandle -> {
                        inExprs.add(translateRhsForOutportEq(Arrays.asList(inHandle), blockNodeToInHandlesMap, outHandleToBlockNodeMap));
                    });      
                    rhsExpr = new NodeCallExpr(blockNode.get(NAME).asText(), inExprs);
                    break;
                }
                case INPORT: {
                    rhsExpr = new VarIdExpr(blockNode.get(NAME).asText());
                    break;
                }
                default:
                    break;
            }
        }                       
        
        return rhsExpr;
    }    

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
