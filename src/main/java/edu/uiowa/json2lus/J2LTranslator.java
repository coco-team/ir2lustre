/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.uiowa.json2lus.lustreAst.LustreExpr;
import edu.uiowa.json2lus.lustreAst.LustreNode;
import edu.uiowa.json2lus.lustreAst.LustreType;
import edu.uiowa.json2lus.lustreAst.LustreVar;
import edu.uiowa.json2lus.lustreAst.PrimitiveType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
    private final String NAME           = "Name";
    private final String HANDLE         = "Handle";
    private final String INPORT         = "Inport";    
    private final String OUTPORT        = "Outport";    
    private final String CONTENT        = "Content";
    private final String SRCBLOCK       = "SrcBlock";
    private final String DSTBLOCK       = "DstBlock";
    private final String BLOCKTYPE      = "BlockType";
    private final String SUBSYSTEM      = "SubSystem";  
    private final String PORTDATATYPE   = "CompiledPortDataTypes";  
    
    
    private JsonNode                topLevelNode = null;
    private Set<JsonNode>           subsystemNodes;
    /** A Mapping between a block's handle and its name */
    private Map<String, JsonNode>   blockHandlesMap;    
    
            
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
        this.subsystemNodes     = new HashSet<>();
        this.blockHandlesMap    = new HashMap<>();
    }
    
    /**
     * Execute the translation process 
     * 
     */    
    public void execute() {
        collectSubsytemBlocksInfo();
        this.subsystemNodes.forEach(node -> translateSubsystemNode(node));
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
            JsonNode contentNode = rootNode.get(this.modelName).get(CONTENT);
            
            if(contentNode != null) {
                this.topLevelNode = contentNode;
                collectSubsytemBlocksInfo(contentNode);                
            } else {
                this.logger.log(Level.SEVERE, "Cannot find the Cocosim model: {0} content definition in the input JSON file!", this.modelName);
            }
        } else {
            this.logger.log(Level.SEVERE, "JSON root node: {0} content definition is an unexpected format!", rootNode);
        }   
    }
    
    protected void collectSubsytemBlocksInfo(JsonNode subsystemNode) {
        if(subsystemNode != null && !this.subsystemNodes.contains(subsystemNode)) {
            this.logger.log(Level.INFO, "Found a subsystem node: {0}  ", subsystemNode);
            
            this.subsystemNodes.add(subsystemNode);
            Iterator<Entry<String, JsonNode>> nodes = subsystemNode.fields();
            
            /** Store the handle and subsystem node in the map */
            if(subsystemNode.has(HANDLE)) {
                this.blockHandlesMap.put(subsystemNode.get(HANDLE).asText(), subsystemNode);
            } else {
                this.logger.log(Level.WARNING, "A susbsystem node does not have a handle!");
            }              
            
            while(nodes.hasNext()) {
                Map.Entry<String, JsonNode> field       = nodes.next(); 
                JsonNode                    fieldNode   = field.getValue();
                
                if(fieldNode.has(BLOCKTYPE)) {
                    if(fieldNode.get(BLOCKTYPE).asText().equals(SUBSYSTEM)) {                        
                        collectSubsytemBlocksInfo(fieldNode);
                    }
                    /** Store a block and its handle in the map */
                    if(fieldNode.has(HANDLE)) {
                        this.blockHandlesMap.put(fieldNode.get(HANDLE).asText(), fieldNode);
                    }                     
                }
               
            }
        }
    }
    
    protected void translateSubsystemNode(JsonNode subsystemNode) {                
        String              lusNodeName = subsystemNode.equals(this.topLevelNode) ? this.modelName : subsystemNode.get(NAME).asText();
        List<LustreVar>     inputs      = new ArrayList<>();
        List<LustreVar>     outputs     = new ArrayList<>();
        List<LustreVar>     locals      = new ArrayList<>();
        List<LustreExpr>    equations   = new ArrayList<>();
        Iterator<Entry<String, JsonNode>> fields = subsystemNode.fields();        

        while(fields.hasNext()) {
            Map.Entry<String, JsonNode> field       = fields.next();   
            JsonNode                    fieldNode   = field.getValue();
            
            if(fieldNode.has(BLOCKTYPE)) {                
                switch(fieldNode.get(BLOCKTYPE).asText()) {
                    case INPORT: {                        
                        String      name = fieldNode.get(NAME).asText();
                        LustreType  type = getLustreTypeFromStrRep(fieldNode.get(PORTDATATYPE).get(OUTPORT).asText());
                        LustreVar   var  = new LustreVar(name, type);
                        inputs.add(var);
                        break;
                    }
                    case OUTPORT: {
                        String      name = fieldNode.get(NAME).asText();
                        LustreType  type = getLustreTypeFromStrRep(fieldNode.get(PORTDATATYPE).get(INPORT).asText());
                        LustreVar   var  = new LustreVar(name, type);
                        outputs.add(var);                        
                        break;
                    }
                    case SUM: {
                        break;
                    }
                    case SUBSYSTEM: {
                        break;
                    }
                    default:
                        this.logger.log(Level.SEVERE, "Unsupported JSON block type: {0}", fieldNode.get(BLOCKTYPE));
                        break;
                }                
            }
        }        
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
