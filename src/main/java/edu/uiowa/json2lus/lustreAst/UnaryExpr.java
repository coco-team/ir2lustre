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
        NEG("-");

        private final String opStr;

        private Op(String str) {
            this.opStr = str;
        }

        @Override
        public String toString() {
            return this.opStr;
        }    
    }    
}
