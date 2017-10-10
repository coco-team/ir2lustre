/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus.lustreAst;

/**
 *
 * @author Paul Meng
 */
public class BooleanExpr extends LustreExpr {
    
    public final boolean value;
    
    public BooleanExpr(boolean value) {
        this.value = value;
    }
    
    public BooleanExpr(String value) {
        if(value.toLowerCase().equals("true")) {
            this.value = true;
        } else if(value.toLowerCase().equals("false")) { 
            this.value = false;
        } else {
            this.value = true;
        }
    }    

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
}
