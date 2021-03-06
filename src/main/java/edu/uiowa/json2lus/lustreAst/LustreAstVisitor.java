/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
 */

package edu.uiowa.json2lus.lustreAst;

/**
 *
 * @author Paul Meng
 */
public interface LustreAstVisitor {
    public void visit(IteExpr expr);
    public void visit(IntExpr expr);
    public void visit(RealExpr expr);
    public void visit(UnaryExpr expr);  
    public void visit(BinaryExpr expr);    
    public void visit(BooleanExpr expr);    
    public void visit(NodeCallExpr expr);         
    public void visit(LustreNode node);
    public void visit(LustreContract contract);    
    public void visit(LustreProgram program); 
    public void visit(LustreEq equation);
    public void visit(LustreVar var);
    public void visit(VarIdExpr varId);
    public void visit(MergeExpr mergeExpr);
    public void visit(LustreEnumType enumType);

    public void visit(TupleExpr aThis);

    public void visit(PrimitiveType aThis);

    public void visit(ArrayType aThis);

    public void visit(ArrayExpr aThis);

    public void visit(LustreAutomaton aThis);

    public void visit(AutomatonState aThis);

    public void visit(AutomatonIteExpr aThis);

    public void visit(ArrayConst aThis);

    public void visit(EnumConst enumConst);
}
