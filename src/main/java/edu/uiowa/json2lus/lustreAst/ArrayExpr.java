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
public class ArrayExpr extends LustreExpr {
    public final String name;
    public final List<String> dimmensions;

    public ArrayExpr(String name, List<String> dimmensions) {
        this.name = name;
        this.dimmensions = dimmensions;
    }        
    
    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    
}
