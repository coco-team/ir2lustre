/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus.lustreAst;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Paul Meng
 */
public class LustreEq extends LustreAst {
    List<VarIdExpr> lhs;
    LustreExpr      rhs;
    
    public LustreEq(List<VarIdExpr> lhs, LustreExpr rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }
    
    public LustreEq(VarIdExpr lhs, LustreExpr rhs) {
        this.lhs = new ArrayList<>();
        this.lhs.add(lhs);
        this.rhs = rhs;
    }    
        
    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    
}
