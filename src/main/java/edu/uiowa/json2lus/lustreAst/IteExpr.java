/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
 */

package edu.uiowa.json2lus.lustreAst;

/**
 *
 * @author Paul Meng
 */
public class IteExpr extends LustreExpr {
    public final LustreExpr ifExpr;
    public final LustreExpr thenExpr;
    public final LustreExpr elseExpr;

    public IteExpr(LustreExpr ifExpr, LustreExpr thenExpr, LustreExpr elseExpr) {
        this.ifExpr     = ifExpr;
        this.thenExpr   = thenExpr;
        this.elseExpr   = elseExpr;
    }

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("if ");
        sb.append(ifExpr.toString());
        sb.append(" then \n");
        sb.append("    ");
        sb.append(thenExpr.toString());
        sb.append("\n else \n");
        sb.append("    ");
        sb.append(elseExpr.toString());
        sb.append("\n");
        return sb.toString();        
    }        
}
