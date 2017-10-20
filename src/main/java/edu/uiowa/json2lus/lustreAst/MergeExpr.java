/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus.lustreAst;

import java.util.List;

/**
 *
 * @author Paul Meng
 */
public class MergeExpr extends LustreExpr{
    LustreExpr clock;
    List<LustreExpr> exprs;
    
    public MergeExpr(LustreExpr clock, List<LustreExpr> exprs) {
        this.clock = clock;
        this.exprs = exprs;
    }

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
}
