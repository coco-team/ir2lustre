/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
 */

package edu.uiowa.json2lus.lustreAst;

import java.math.BigDecimal;

/**
 *
 * @author Paul Meng
 */
public class RealExpr extends LustreExpr {
    public final BigDecimal value;
    
    public RealExpr(BigDecimal value) {
        this.value = value;
    }

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    @Override
    public String toString() {
        return this.value.toString();
    }    
}
