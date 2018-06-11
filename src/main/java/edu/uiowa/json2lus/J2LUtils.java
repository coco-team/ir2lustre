/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus;

import com.fasterxml.jackson.databind.JsonNode;
import edu.uiowa.json2lus.lustreAst.BinaryExpr;
import edu.uiowa.json2lus.lustreAst.BooleanExpr;
import edu.uiowa.json2lus.lustreAst.IntExpr;
import edu.uiowa.json2lus.lustreAst.LustreAst;
import edu.uiowa.json2lus.lustreAst.LustreExpr;
import edu.uiowa.json2lus.lustreAst.LustreType;
import edu.uiowa.json2lus.lustreAst.PrimitiveType;
import edu.uiowa.json2lus.lustreAst.RealExpr;
import edu.uiowa.json2lus.stateflowparser.StateflowVisitor;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowLexer;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser;
import java.math.BigDecimal;
import java.util.List;
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
public class J2LUtils {
    public static int COUNT = 0;
    private static final String PATH    = "Path";
    private static final Logger LOGGER  = Logger.getLogger(J2LUtils.class.getName());    
    
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

    
    public static LustreExpr andExprs(List<LustreExpr> exprs) {
        LustreExpr andExpr;
        if(exprs != null && exprs.size() > 0) {
            andExpr = exprs.get(0);
            for(int i = 1; i < exprs.size(); ++i) {
                andExpr = new BinaryExpr(andExpr, BinaryExpr.Op.AND, exprs.get(i));
            }
        } else {
            andExpr = new BooleanExpr(true);
        }
        return andExpr;
    }
    
    public static LustreExpr orExprs(List<LustreExpr> exprs) {
        LustreExpr orExpr;
        if(exprs != null && exprs.size() > 0) {
            orExpr = exprs.get(0);
            for(int i = 1; i < exprs.size(); ++i) {
                orExpr = new BinaryExpr(orExpr, BinaryExpr.Op.OR, exprs.get(i));
            }
        } else {
            orExpr = new BooleanExpr(false);
        }
        return orExpr;
    } 

    public static String getSanitizedBlkPath(JsonNode node) {
        String path = null;
        
        if(node.has(PATH)) {
            path = sanitizeName(node.get(PATH).asText());
        }
        return path;
    }
    
    public static String getBlkPath(JsonNode node) {
        String path = null;
        
        if(node.has(PATH)) {
            path = node.get(PATH).asText();
        }
        return path;
    }    
    
    public static String mkFreshVarName(String varName) {
        String newVarName = varName;
        
        if(newVarName == null) {
            newVarName = "_" + (++COUNT);
        } else {
            newVarName += "_"+(++COUNT);
        }
        return newVarName;
    }
    
    public static LustreExpr getInitValueForType(LustreType type) {
        LustreExpr initValue = null;
        
        if(type == PrimitiveType.INT) {
            initValue = new IntExpr(0);
        } else if(type == PrimitiveType.REAL) {
            initValue = new RealExpr(new BigDecimal("0.0"));
        } else if(type == PrimitiveType.BOOL) {
            initValue = new BooleanExpr(true);
        } else {
            LOGGER.log(Level.SEVERE, "Unsupported output type: {0}", type.toString());
        }
        return initValue;
    }    
    
    public static String getFreshVarAtInst(String varName, int instance) {
        String newVarName = varName;
        
        if(newVarName == null) {
            newVarName = "_" + instance;
        } else {
            newVarName += "_" + instance;
        }
        return newVarName;
    }    
    
    public static List<LustreAst> parseAndTranslate(String sf) {
        CharStream          charStream  = CharStreams.fromString(sf);
        StateflowLexer      lexer       = new StateflowLexer(charStream);
        TokenStream         tokens      = new CommonTokenStream(lexer);    
        StateflowParser     parser      = new StateflowParser(tokens);        
        StateflowVisitor    visitor     = new StateflowVisitor();
        
        return visitor.visitFileDecl(parser.fileDecl());
    }
    
    public static List<LustreAst> parseAndTranslateStrExpr(String sf) {
        CharStream          charStream  = CharStreams.fromString(sf);
        StateflowLexer      lexer       = new StateflowLexer(charStream);
        TokenStream         tokens      = new CommonTokenStream(lexer);    
        StateflowParser     parser      = new StateflowParser(tokens);
        
        StateflowVisitor    visitor     = new StateflowVisitor();
        List<LustreAst>     asts        = visitor.visitExpr(parser.expr());
        return asts;
    }       
}
