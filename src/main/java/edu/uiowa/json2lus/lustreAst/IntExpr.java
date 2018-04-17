/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
 */

package edu.uiowa.json2lus.lustreAst;

import java.math.BigInteger;

/**
 *
 * @author Paul Meng
 */
public class IntExpr extends LustreExpr {
    public final BigInteger value;
    
    public IntExpr(BigInteger value) {
        this.value = value;
    }
    
    public IntExpr(int value) {
        this.value = new BigInteger(String.valueOf(value));
    }    

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    
    @Override
    public String toString() {
        return value.toString();
    }    
}
