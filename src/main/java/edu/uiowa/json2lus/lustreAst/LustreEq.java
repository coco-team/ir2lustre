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
    List<LustreExpr> lhs;
    LustreExpr      rhs;
    
    
    public LustreEq(List<LustreExpr> lhs, LustreExpr rhs) {
        this.lhs = new ArrayList<>();
        for(LustreExpr var : lhs) {
            this.lhs.add((VarIdExpr)var);
        }
        this.rhs = rhs;
    } 
    
    public LustreEq(LustreExpr lhs, LustreExpr rhs) {
        this.lhs = new ArrayList<>();
        this.lhs.add(lhs);
        this.rhs = rhs;
    } 
    
    public List<LustreExpr> getLhs() {
        return this.lhs;
    }
    
    public LustreExpr getRhs() {
        return this.rhs;
    }    
        
    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    
}
