/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
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
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(lhs.size() > 1) {
            sb.append("(");
        }
        sb.append(lhs.get(0));
        for(int i = 0; i < lhs.size(); ++i) {
            sb.append(", ");
            sb.append(lhs.get(i));
        }
        if(lhs.size() > 1) {
            sb.append(")");
        }        
        sb.append(" = ");
        sb.append(this.rhs.toString());
        return sb.toString();
    }    
}
