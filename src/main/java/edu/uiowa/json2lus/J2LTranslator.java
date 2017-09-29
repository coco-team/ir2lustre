/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Paul Meng
 */
public class J2LTranslator {
    public  final String inputPath;
    public  final String modelName;
    
    private final Logger logger;
            
    
    public J2LTranslator(String inputPath) {
        this.inputPath  = inputPath;
        
        // Assumption: The file name is also the model name
        this.modelName  = inputPath.toLowerCase().endsWith(".json") ? 
                          inputPath.substring(inputPath.lastIndexOf(File.separator)+1, inputPath.lastIndexOf("."))
                          : inputPath.substring(inputPath.lastIndexOf(File.separator)+1);            
        this.logger     = Logger.getLogger(J2LTranslator.class.getName());
    }
    
    public void execute() throws IOException {
        extractBlocksInfo();
    }    
    
    protected void extractBlocksInfo() throws IOException {
        JsonNode        contentNode = null;
        ObjectMapper    mapper      = new ObjectMapper();
        JsonNode        rootNode    = mapper.readTree(new File(this.inputPath));        

        if(rootNode.has(this.modelName)) {
            contentNode = rootNode.get(this.modelName).get("Content");
        }    
        if(contentNode != null) {
            Iterator<Entry<String, JsonNode>> nodes = contentNode.fields();
            
            while(nodes.hasNext()) {
                Map.Entry<String, JsonNode> entry = nodes.next();   
                
                
//                this.logger.log(Level.INFO, "{0}:{1}", new Object[]{entry.getKey(), entry.getValue()});
            }
        } else {
            this.logger.log(Level.SEVERE, "Cannot find the Cocosim model: {0} content definition in the input JSON file!", this.modelName);
        }
    }
}
