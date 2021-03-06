/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
 */

package edu.uiowa.json2lus.lustreAst;

/**
 *
 * @author Paul Meng
 */
public class VarIdExpr extends LustreExpr {
    public String id;
    
    public VarIdExpr (String id) {
        this.id = id;
    }
    
    public void setVarId (String id) {
        this.id = id;
    }    
    
    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    @Override
    public String toString() {
        return this.id;
    }    
}
