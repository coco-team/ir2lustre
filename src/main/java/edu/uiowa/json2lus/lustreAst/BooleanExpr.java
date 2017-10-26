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
        this.value = value.toLowerCase().equals("true") || !value.toLowerCase().equals("0");
    }    

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
}
