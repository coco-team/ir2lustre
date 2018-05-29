/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
 */

package edu.uiowa.json2lus.lustreAst;

import java.util.List;

/**
 *
 * @author Paul Meng
 */
public class TupleExpr extends LustreExpr {
    public List<LustreExpr> elements;
    
    public TupleExpr(List<? extends LustreExpr> elements) {
        this.elements = (List<LustreExpr>) elements;
    }

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    
}
