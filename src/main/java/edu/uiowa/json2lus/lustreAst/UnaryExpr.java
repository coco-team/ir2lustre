/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
 */

package edu.uiowa.json2lus.lustreAst;

/**
 *
 * @author Paul Meng
 */
public class UnaryExpr extends LustreExpr {
    public final Op         op;
    public final LustreExpr expr;
    
    public UnaryExpr(Op op, LustreExpr expr) {
        this.op     = op;
        this.expr   = expr;
    }

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    
    public enum Op {	
        
        NOT ("not"),
        PRE ("pre"),
        LAST ("last"),
        NEG("-"),
        REAL("real"),
        INT("int");

        private final String opStr;

        private Op(String str) {
            this.opStr = str;
        }

        @Override
        public String toString() {
            return this.opStr;
        }    
    }    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.op).append(" ");
        sb.append(this.expr);
        return sb.toString();
    }    
}
