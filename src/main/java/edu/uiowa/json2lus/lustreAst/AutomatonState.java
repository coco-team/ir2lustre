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
public class AutomatonState extends LustreAst{
    boolean             isInit = false;
    public String              name;
    public List<LustreVar>     locals;  
    public List<LustreEq>      equations;  
    public List<LustreExpr>    strongTrans;  
    public List<LustreExpr>    weakTrans;  

    public AutomatonState(boolean isInit, String name, List<LustreVar> locals, List<LustreExpr> strongTrans, List<LustreEq> equations, List<LustreExpr> weakTrans) {
        this.name           = name;
        this.locals         = locals;
        this.equations      = equations;
        this.isInit         = isInit;
        this.strongTrans    = strongTrans;
        this.weakTrans      = weakTrans;
    } 
    
    public AutomatonState(String name, List<LustreVar> locals, List<LustreEq> equations, List<LustreExpr> weakTrans) {
        this.name           = name;
        this.locals         = locals;
        this.equations      = equations;
        this.strongTrans    = new ArrayList<>();
        this.weakTrans      = weakTrans;
    }

    public AutomatonState(String name, List<LustreEq> equations, List<LustreExpr> weakTrans) {
        this.name           = name;
        this.locals         = new ArrayList<>();
        this.equations      = equations;
        this.strongTrans    = new ArrayList<>();        
        this.weakTrans      = weakTrans;        
    }
    
    public AutomatonState(String name, List<LustreEq> equations, LustreExpr weakTrans) {
        this.name           = name;
        this.locals         = new ArrayList<>();
        this.equations      = equations;
        this.strongTrans    = new ArrayList<>();        
        this.weakTrans      = new ArrayList<>();        
        this.weakTrans.add(weakTrans);
    }  
    
    public AutomatonState(String name, List<LustreVar> locals, List<LustreEq> equations, LustreExpr weakTrans) {
        this.name           = name;
        this.locals         = locals;
        this.equations      = equations;
        this.strongTrans    = new ArrayList<>();        
        this.weakTrans      = new ArrayList<>();        
        this.weakTrans.add(weakTrans);
    }      

    
    public AutomatonState(String name, List<LustreEq> equations) {
        this.name       = name;
        this.locals     = new ArrayList<>();
        this.equations  = equations;
    }    

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
}
