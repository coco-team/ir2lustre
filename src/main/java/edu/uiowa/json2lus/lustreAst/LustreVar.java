/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
 */

package edu.uiowa.json2lus.lustreAst;

/**
 *
 * @author Paul Meng
 */
public class LustreVar extends LustreAst {
    public final String     name;
    public final LustreType type;
    
    public LustreVar(String name, LustreType type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
}
