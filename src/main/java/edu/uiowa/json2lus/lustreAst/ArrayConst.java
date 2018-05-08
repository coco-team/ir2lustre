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
public class ArrayConst extends LustreExpr {
    String value;
    List<LustreExpr> exprs;
    
    public ArrayConst(String value) {
        this.value = value;
        this.exprs = new ArrayList<>();
    }
    
    public ArrayConst(List<LustreExpr> exprs) {
        this.exprs = exprs;
    }

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    
}
