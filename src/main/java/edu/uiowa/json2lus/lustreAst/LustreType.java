/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
 */

package edu.uiowa.json2lus.lustreAst;

/**
 *
 * @author Paul Meng
 */
public abstract class LustreType implements Comparable<LustreType> {
    public abstract void accept(LustreAstVisitor visitor);
}
