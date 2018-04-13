/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
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
        if(value.contains(".")) {
            String rhs = value.substring(value.indexOf(".")+1).trim();
            
            if(Integer.parseInt(rhs) == 0) {
                this.value = Integer.parseInt(value.substring(0, value.indexOf("."))) != 0;
            } else {
                this.value = true;
            }            
        } else {
            this.value = value.toLowerCase().equals("true") || Integer.parseInt(value.toLowerCase()) != 0;
        }
    }    

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    
    @Override
    public String toString() {
        return String.valueOf(this.value);
    }       
}
