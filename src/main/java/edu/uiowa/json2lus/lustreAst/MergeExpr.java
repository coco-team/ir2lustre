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
