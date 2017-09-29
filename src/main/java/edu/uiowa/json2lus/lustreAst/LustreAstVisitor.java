/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
    public void visit(Contract contract);    
    public void visit(LustreProgram program);    
}
