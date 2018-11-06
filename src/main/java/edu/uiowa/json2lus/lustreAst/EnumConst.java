/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
 */

package edu.uiowa.json2lus.lustreAst;


public class EnumConst extends LustreExpr{

    private LustreEnumType  enumType;
    private String          enumValue;

    public EnumConst(LustreEnumType enumType, String enumValue)
    {
        this.enumType   = enumType;
        this.enumValue  = enumValue;
    }

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    @Override
    public String toString()
    {
        return enumValue;
    }
}





