/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus;

import edu.uiowa.json2lus.lustreAst.LustreType;
import edu.uiowa.json2lus.lustreAst.PrimitiveType;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Paul Meng
 */
public class J2LUtils {
    private static final Logger LOGGER = Logger.getLogger(J2LUtils.class.getName());    
    
    public static String sanitizeName(String name) {
        return name.trim().replace(" ", "_").replace("\n", "_").replace("=", "_")
                .replace(">", "_").replace("<", "_").replace("/", "_").replace("\\", "_")
                .replace("-", "_").replace("+", "_").replace("$", "_").replace("*", "_")
                .replace("(", "_").replace(")", "_").replace(",", "_");
    }    
    
    /**
     * 
     * @param type
     * @return The corresponding Lustre type from its string representation
     */
    public static LustreType getLustreTypeFromStrRep(String type) {
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
            case "sfix64":                
            case "uint32":{
                lusType = PrimitiveType.INT;
                break;                
            }            
            case "boolean": 
            case "bool": {
                lusType = PrimitiveType.BOOL;
                break;                
            }        
            default: {
                LOGGER.log(Level.SEVERE, "Unsupported type: {0}", type);
                break;
            }                
        }
        
        return lusType;
    }    
}
