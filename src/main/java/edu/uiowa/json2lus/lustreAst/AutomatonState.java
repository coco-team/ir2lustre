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
    String              name;
    List<LustreVar>     locals;  
    List<LustreEq>      equations;  
    List<LustreExpr>    strongTrans;  
    List<LustreExpr>    weakTrans;  

    public AutomatonState(boolean isInit, String name, List<LustreVar> locals, List<LustreExpr> strongTrans, List<LustreEq> equations, List<LustreExpr> weakTrans) {
        this.name           = name;
        this.locals         = locals;
        this.equations      = equations;
        this.isInit         = isInit;
        this.strongTrans    = strongTrans;
        this.weakTrans      = weakTrans;
    } 
    
    public AutomatonState(boolean isInit, String name, List<LustreVar> locals, List<LustreEq> equations, List<LustreExpr> weakTrans) {
        this.name           = name;
        this.locals         = locals;
        this.equations      = equations;
        this.isInit         = isInit;
        this.strongTrans    = new ArrayList<>();
        this.weakTrans      = weakTrans;
    }

    public AutomatonState(boolean isInit, String name, List<LustreEq> equations, List<LustreExpr> weakTrans) {
        this.name           = name;
        this.locals         = new ArrayList<>();
        this.equations      = equations;
        this.isInit         = isInit;
        this.strongTrans    = new ArrayList<>();        
        this.weakTrans      = weakTrans;        
    }
    
    public AutomatonState(boolean isInit, String name, List<LustreEq> equations, LustreExpr weakTrans) {
        this.name           = name;
        this.locals         = new ArrayList<>();
        this.equations      = equations;
        this.isInit         = isInit;
        this.strongTrans    = new ArrayList<>();        
        this.weakTrans      = new ArrayList<>();        
        this.weakTrans.add(weakTrans);
    }    

    
    public AutomatonState(boolean isInit, String name, List<LustreEq> equations) {
        this.isInit     = isInit;
        this.name       = name;
        this.locals     = new ArrayList<>();
        this.equations  = equations;
    }    

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
}
