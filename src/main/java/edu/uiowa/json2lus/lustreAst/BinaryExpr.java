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
public class BinaryExpr extends LustreExpr {
    public final Op         op;
    public final LustreExpr left;    
    public final LustreExpr right;
    
    public BinaryExpr(LustreExpr left, Op op, LustreExpr right) {
        this.op     = op;
        this.left   = left;
        this.right  = right;
    }

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    
    public enum Op {
        
        OR ("or"),
        AND ("and"),
        XOR ("xor"),    
        IMPLIES ("=>"),        
        PLUS ("+"),
        MINUS ("-"),
        MULTIPLY ("*"),
        DIVIDE ("/"),
        MOD ("mod"),
        EQ ("="),
        NEQ ("<>"),
        GTE (">="),
        LTE ("<="),
        GT (">"),
        LT ("<"),        
        ARROW ("->"),
        WHEN ("when");    

        private final String opStr;

        private Op(String op) {
            this.opStr = op;
        }

        @Override
        public String toString() {
            return this.opStr;
        }        
    }
}
