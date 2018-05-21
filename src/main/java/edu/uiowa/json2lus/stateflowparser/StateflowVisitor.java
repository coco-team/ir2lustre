/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
 */

package edu.uiowa.json2lus.stateflowparser;

import edu.uiowa.json2lus.lustreAst.BinaryExpr;
import edu.uiowa.json2lus.lustreAst.IntExpr;
import edu.uiowa.json2lus.lustreAst.LustreAst;
import edu.uiowa.json2lus.lustreAst.LustreEq;
import edu.uiowa.json2lus.lustreAst.LustreExpr;
import edu.uiowa.json2lus.lustreAst.NodeCallExpr;
import edu.uiowa.json2lus.lustreAst.RealExpr;
import edu.uiowa.json2lus.lustreAst.TupleExpr;
import edu.uiowa.json2lus.lustreAst.UnaryExpr;
import edu.uiowa.json2lus.lustreAst.VarIdExpr;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowBaseVisitor;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.AdditiveExprContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.AndExprContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.ArithExprContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.ArrayExprContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.AssignmentExprContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.BooleanExprContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.DotRefContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.EqualityExprContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.ExprArrayListContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.ExprContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.FileDeclContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.FunctionDeclContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.HorizontalArrayExprContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.IfStatContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.MExprContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.MultExprContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.PrimaryExprContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.RelationalExprContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.StatBlockContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.StatContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.UExprContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.UnaryExprContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.ValueContext;
import edu.uiowa.json2lus.stateflowparser.antlr.StateflowParser.VerticalArrayExprContext;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Paul Meng
 */
public class StateflowVisitor extends StateflowBaseVisitor {
    private static final Logger LOGGER = Logger.getLogger(StateflowVisitor.class.getName());
    
    @Override
    public List<LustreAst> visitFileDecl(FileDeclContext ctx) {
        List<LustreAst> asts = new ArrayList<>();

        if(ctx.functionDecl() != null && ctx.functionDecl().size() > 0) {
            for(FunctionDeclContext fcnDecl : ctx.functionDecl()) {
                visitFunctionDecl(fcnDecl);
            }            
        } else if(ctx.statBlock() != null && ctx.statBlock().size() > 0) {
            for(StatBlockContext sb : ctx.statBlock()) {
                if(visitStatBlock(sb) != null) {
                    asts.addAll(visitStatBlock(sb));
                } else {
                    LOGGER.log(Level.WARNING, "Generated a null expression but it might not be a big deal!");
                }          
            }
        }
        return asts;
    }  
    
    @Override
    public List<LustreAst> visitStatBlock(StatBlockContext sb) {
        return visitStat(sb.stat());
    }

    @Override
    public List<LustreAst> visitStat(StatContext ctx) {
        List<LustreAst> asts = new ArrayList<>();

        if(ctx.ifStat() != null) {
            asts = visitIfStat(ctx.ifStat());
        } else if(ctx.dotRef() != null) {
            LustreExpr      rhsExpr = null;
            List<LustreAst> rhsAsts = visitExpr(ctx.expr());
            
            if(rhsAsts.size() == 1) {
                rhsExpr = (LustreExpr)rhsAsts.get(0);
            } else {
                LOGGER.log(Level.SEVERE, "Unhandled case for multiple expressions returned from calling visit expressions: {0}", ctx.expr());
            }
            asts.add(new LustreEq(visitDotRef(ctx.dotRef()), rhsExpr));
        } else if(ctx.ID() != null) {
            LOGGER.log(Level.SEVERE, "We do not handle the case : a statement has a label!");
            if(ctx.expr() != null) {
                asts = visitExpr(ctx.expr());
            }
        } else if(ctx.expr() != null) {
            asts = visitExpr(ctx.expr());
        } else {
            LOGGER.log(Level.SEVERE, "Unsupported state flow statement: {0}", ctx.toString());
        }
        return asts;
    } 
    
    @Override
    public List<LustreAst> visitIfStat(IfStatContext ifStat) {
        List<LustreAst> ifExprs     = new ArrayList<>();
        List<LustreAst> statExprs   = new ArrayList<>();
        
        for(ExprContext expr : ifStat.expr()) {
            ifExprs.addAll(visitExpr(expr));
        }
        for(StatBlockContext statBlockExpr : ifStat.statBlock()) {
//            statExprs.addAll(visitStatBlock(statBlockExpr));
        }
        
        return null;
    }
    
    @Override
    public LustreExpr visitDotRef(DotRefContext dotRef) {
        LustreExpr idExpr = null;
                
        if(dotRef.ID().size() == 1) {
            idExpr = new VarIdExpr(dotRef.ID().get(0).getSymbol().getText());
        } else {
            LOGGER.log(Level.SEVERE, "Unsupported state flow statement: {0}", dotRef.toString());
        }
        return idExpr;
    }
    
    @Override
    public List<LustreAst> visitExpr(ExprContext ctx) {
        List<LustreAst> asts = new ArrayList<>();
        
        if(ctx.assignmentExpr() != null) {
            asts.add(visitAssignmentExpr(ctx.assignmentExpr()));
        } else if(ctx.booleanExpr() != null) {
            asts.add(visitBooleanExpr(ctx.booleanExpr()));
        } else if(ctx.arithExpr() != null) {
            asts.add(visitArithExpr(ctx.arithExpr()));
        } else if(ctx.dotRef() != null) {
            if(ctx.dotRef().ID().size() == 1) {
                asts.add(new VarIdExpr(ctx.dotRef().ID().get(0).getSymbol().getText()));
            } else {
                
            }
        } else if(ctx.arrayExpr() != null) {
            asts = visitArrayExpr(ctx.arrayExpr());
        } else {
            LOGGER.log(Level.SEVERE, "Unhandled expression type: {0}", ctx);
        }
        return asts;
    }
    
    @Override
    public List<LustreAst> visitArrayExpr(ArrayExprContext arrayExpr) {
        List<LustreAst> asts = new ArrayList<>();
        if(arrayExpr.exprArrayList() != null) {
            asts.addAll(visitExprArrayList(arrayExpr.exprArrayList()));
        }
        return asts;
    } 

    @Override
    public List<LustreAst> visitExprArrayList(ExprArrayListContext arrayListExpr) {
        List<LustreAst> asts = new ArrayList<>();
        
        if(arrayListExpr.verticalArrayExpr() != null) {
            asts = visitVerticalArrayExpr(arrayListExpr.verticalArrayExpr());
        }
        
        return asts;
    }     

    @Override
    public List<LustreAst> visitVerticalArrayExpr(VerticalArrayExprContext verticalArrayExprCtx) {
        List<LustreAst> asts = new ArrayList<>();
        
        if(verticalArrayExprCtx.horizontalArrayExpr() != null && verticalArrayExprCtx.horizontalArrayExpr().size() > 0) {
            for(HorizontalArrayExprContext hCtx : verticalArrayExprCtx.horizontalArrayExpr()) {
                asts.addAll(visitHorizontalArrayExpr(hCtx));
            }
        }
        return asts;
    }

    @Override
    public List<LustreAst> visitHorizontalArrayExpr(HorizontalArrayExprContext horizontalArrayExprContext) {
        List<LustreAst> asts = new ArrayList<>();  
        if(horizontalArrayExprContext.expr() != null) {
            asts.addAll(visitExpr(horizontalArrayExprContext.expr()));
        }
        if(horizontalArrayExprContext.exprArrayList() != null && horizontalArrayExprContext.exprArrayList().size() > 0) {
            for(ExprArrayListContext eCtx : horizontalArrayExprContext.exprArrayList()) {
                asts.addAll(visitExprArrayList(eCtx));
            }
        }
        return asts;
    }
    
    @Override
    public LustreAst visitAssignmentExpr(AssignmentExprContext assignmentExpr) {        
        LustreAst equation = null;
        if(assignmentExpr.ID().size() == 1) {
            String      id      = assignmentExpr.ID(0).getSymbol().getText();
            VarIdExpr   varId   = new VarIdExpr(id);
            
            if(assignmentExpr.expr().isEmpty()) {
                if(assignmentExpr.PLUSPLUS() != null) {
                    equation = new LustreEq(varId, new BinaryExpr(varId, BinaryExpr.Op.PLUS, new IntExpr(BigInteger.ONE)));                    
                } else if(assignmentExpr.MINUSMINUS() != null) {
                    equation = new LustreEq(varId, new BinaryExpr(varId, BinaryExpr.Op.MINUS, new IntExpr(BigInteger.ONE)));                    
                }
            } else {
                BinaryExpr.Op op = null;
                
                if(assignmentExpr.PLUSASSIGN() != null) {
                    op = BinaryExpr.Op.PLUS;
                } else if(assignmentExpr.MINUSASSIGN() != null) {
                    op = BinaryExpr.Op.MINUS;
                } else if(assignmentExpr.TIMESASSIGN() != null) {
                    op = BinaryExpr.Op.MULTIPLY;
                } else if(assignmentExpr.DIVIDEASSIGN() != null) {
                    op = BinaryExpr.Op.DIVIDE;
                }
                LustreExpr  rhsExpr = null;

                List<LustreAst>   rhsAsts = visitExpr(assignmentExpr.expr(0));         
                
                if(rhsAsts.size()==1) {
                    LustreAst   rhsAst  = rhsAsts.get(0);                    
                    if(rhsAst instanceof LustreExpr) {
                        rhsExpr = (LustreExpr)rhsAst;
                    }                    
                } else {
                    LOGGER.log(Level.SEVERE, "Unhandled case: multiple expressions returned from ANTLR parsing expression!");
                }
              
                if(op != null) {                    
                    equation = new LustreEq(varId, new BinaryExpr(varId, op, rhsExpr));                        
                } else {
                    equation = new LustreEq(varId, rhsExpr);
                }            
            }
        } else {            
            List<LustreExpr>    ids         = new ArrayList<>();
            List<LustreExpr>    inExprs     = new ArrayList<>();
            String              nodeName    = assignmentExpr.ID(assignmentExpr.ID().size()-1).getSymbol().getText();
            
            for(int i = 0; i < assignmentExpr.ID().size()-1; ++i) {
                ids.add(new VarIdExpr(assignmentExpr.ID(i).getSymbol().getText()));
            }
            for(ExprContext expr : assignmentExpr.expr()) {
                LustreAst ast = null;
                List<LustreAst> asts = visitExpr(expr);
                
                if(asts.size() == 1) {
                    ast = asts.get(0);
                } else {
                    LOGGER.log(Level.SEVERE, "Unhandled case for multiple asts returned from visiting expressions!");
                }
                
                if(ast instanceof LustreExpr) {
                    inExprs.add((LustreExpr)ast);
                }
            }
            LustreExpr lhsExpr = null;
            
            if(ids.size() > 1) {
                lhsExpr = new TupleExpr(ids);
            } else if(ids.size() == 1){
                lhsExpr = ids.get(0);
            } else {
                LOGGER.log(Level.SEVERE, "Unexpected lhsExpr is empty!");
            }
            equation = new LustreEq(lhsExpr, new NodeCallExpr(nodeName, inExprs));
        }    
        return equation;
    }      
    
    @Override
    public LustreExpr visitBooleanExpr(BooleanExprContext boolExpr) {
        List<LustreExpr> exprs = new ArrayList<>();

        for(AndExprContext andExpr : boolExpr.andExpr()) {
            exprs.add(visitAndExpr(andExpr));
        }
        if(exprs.size() > 1) {
            LustreExpr expr = exprs.get(0);
            for(int i = 1; i < exprs.size(); ++i) {
                expr = new BinaryExpr(expr, BinaryExpr.Op.OR, exprs.get(i));
            }
            return expr;
        }
        return exprs.get(0);
    }   
    
    @Override
    public LustreExpr visitAndExpr(AndExprContext andExpr) {
        LustreExpr          expr    = null;
        List<LustreExpr>    exprs   = new ArrayList<>();
        
        for(EqualityExprContext eqExpr : andExpr.equalityExpr()) {
            exprs.add(visitEqExpr(eqExpr));
        }
        if(exprs.size() > 1) {
            expr = exprs.get(0);
            for(int i = 1; i < exprs.size(); ++i) {
                expr = new BinaryExpr(expr, BinaryExpr.Op.AND, exprs.get(i));
            }
        } else if(exprs.size() == 1){
            expr = exprs.get(0);
        }
        return expr;
    }
    
    public LustreExpr visitEqExpr(EqualityExprContext eqExpr) {
        LustreExpr          expr    = null;
        List<LustreExpr>    exprs   = new ArrayList<>();
        
        for(RelationalExprContext relExpr : eqExpr.relationalExpr()) {
            exprs.add(visitRelationalExpr(relExpr));
        }
        if(exprs.size() > 1) {
            BinaryExpr.Op op = (eqExpr.EQUALTO() != null) ? BinaryExpr.Op.EQ : BinaryExpr.Op.NEQ;
            expr = exprs.get(0);            
            for(int i = 1; i < exprs.size(); ++i) {
                expr = new BinaryExpr(expr, op, exprs.get(i));
            }
        } else if(exprs.size() == 1){
            expr = exprs.get(0);
        }
        return expr;
    }  
    
    @Override
    public LustreExpr visitRelationalExpr(RelationalExprContext relExpr) {
        LustreExpr          expr    = null;
        List<LustreExpr>    exprs   = new ArrayList<>();
        
        for(AdditiveExprContext addExpr : relExpr.additiveExpr()) {
            exprs.add(visitAdditiveExpr(addExpr));
        }
        if(exprs.size() > 1) {
            BinaryExpr.Op op = null;
            
            if(relExpr.GT() != null) {
                op = BinaryExpr.Op.GT;
            } else if(relExpr.GTE() != null) {
                op = BinaryExpr.Op.GTE;
            } else if(relExpr.LT() != null) {
                op = BinaryExpr.Op.LT;
            } else if(relExpr.LTE() != null) {
                op = BinaryExpr.Op.LTE;
            }
            expr = exprs.get(0);
            for(int i = 1; i < exprs.size(); ++i) {
                expr = new BinaryExpr(expr, op, exprs.get(i));
            }
        } else if(exprs.size() == 1){
            expr = exprs.get(0);
        }
        return expr;
    }      
    
    @Override
    public LustreExpr visitAdditiveExpr(AdditiveExprContext addExpr) {
        LustreExpr          expr    = null;
        List<LustreExpr>    exprs   = new ArrayList<>();
        
        for(MultExprContext multExpr : addExpr.multExpr()) {
            exprs.add(visitMultExpr(multExpr));
        }
        if(exprs.size() > 1) {
            BinaryExpr.Op op = null;
            
            if(addExpr.PLUS() != null) {
                op = BinaryExpr.Op.PLUS;
            } else if(addExpr.MINUS() != null) {
                op = BinaryExpr.Op.MINUS;
            }        
            expr = exprs.get(0);
            for(int i = 1; i < exprs.size(); ++i) {
                expr = new BinaryExpr(expr, op, exprs.get(i));
            }
        } else if(exprs.size() == 1){
            expr = exprs.get(0);
        }
        return expr;
    }  
    
    
    @Override
    public LustreExpr visitMultExpr(MultExprContext multExpr) {
        LustreExpr          expr    = null;
        List<LustreExpr>    exprs   = new ArrayList<>();
        
        for(UnaryExprContext unaryExpr : multExpr.unaryExpr()) {
            exprs.add(visitUnaryExpr(unaryExpr));
        }
        if(exprs.size() > 1) {
            BinaryExpr.Op op = null;
            
            if(multExpr.MTIMES()!= null) {
                op = BinaryExpr.Op.MULTIPLY;
            } else if(multExpr.RDIVIDE() != null) {
                op = BinaryExpr.Op.DIVIDE;
            }        
            expr = exprs.get(0);
            for(int i = 1; i < exprs.size(); ++i) {
                expr = new BinaryExpr(expr, op, exprs.get(i));
            }
        } else if(exprs.size() == 1){
            expr = exprs.get(0);
        }
        return expr;
    }      
    
    @Override
    public LustreExpr visitUnaryExpr(UnaryExprContext unaryExpr) {
        LustreExpr          expr    = null;
        List<LustreExpr>    exprs   = new ArrayList<>();
        
        if(unaryExpr.primaryExpr() != null) {
            expr = visitPrimaryExpr(unaryExpr.primaryExpr());
        } else if(unaryExpr.arithExpr() != null) {
            expr = visitArithExpr(unaryExpr.arithExpr());
        }
        
        
        if(unaryExpr.NOT() != null) {
            expr = new UnaryExpr(UnaryExpr.Op.NOT, expr);
        } else if(unaryExpr.MINUS() != null) {
            expr = new UnaryExpr(UnaryExpr.Op.NEG, expr);
        }
        return expr;
    }

    @Override
    public LustreExpr visitArithExpr(ArithExprContext addExpr) {
        LustreExpr          expr    = null;
        List<LustreExpr>    exprs   = new ArrayList<>();
        
        for(MExprContext multExpr : addExpr.mExpr()) {
            exprs.add(visitMExpr(multExpr));
        }
        if(exprs.size() > 1) {
            BinaryExpr.Op op = null;
            
            if(addExpr.PLUS() != null) {
                op = BinaryExpr.Op.PLUS;
            } else if(addExpr.MINUS() != null) {
                op = BinaryExpr.Op.MINUS;
            }        
            expr = exprs.get(0);
            for(int i = 1; i < exprs.size(); ++i) {
                expr = new BinaryExpr(expr, op, exprs.get(i));
            }
        } else if(exprs.size() == 1){
            expr = exprs.get(0);
        }
        return expr;
    }      
    
    @Override
    public LustreExpr visitMExpr(MExprContext multExpr) {
        LustreExpr          expr    = null;
        List<LustreExpr>    exprs   = new ArrayList<>();
        
        for(UExprContext unaryExpr : multExpr.uExpr()) {
            exprs.add(visitUExpr(unaryExpr));
        }
        if(exprs.size() > 1) {
            BinaryExpr.Op op = null;
            
            if(multExpr.MTIMES()!= null) {
                op = BinaryExpr.Op.MULTIPLY;
            } else if(multExpr.RDIVIDE() != null) {
                op = BinaryExpr.Op.DIVIDE;
            }        
            expr = exprs.get(0);
            for(int i = 1; i < exprs.size(); ++i) {
                expr = new BinaryExpr(expr, op, exprs.get(i));
            }
        } else if(exprs.size() == 1){
            expr = exprs.get(0);
        }
        return expr;
    }      
    
    @Override
    public LustreExpr visitUExpr(UExprContext uExpr) {
        LustreExpr expr = null;
        
        if(uExpr.arithExpr() != null) {
            expr = visitArithExpr(uExpr.arithExpr());
        } else if(uExpr.value() != null) {
            expr = visitValue(uExpr.value());
        }
        
         if(uExpr.MINUS() != null) {
            expr = new UnaryExpr(UnaryExpr.Op.NEG, expr);
        }
        return expr;
    }    
    
    @Override
    public LustreExpr visitPrimaryExpr(PrimaryExprContext primExpr) {
        LustreExpr expr = null;
        
        if(primExpr.booleanExpr() != null) {
            expr = visitBooleanExpr(primExpr.booleanExpr());
        } else if(primExpr.value() != null) {
            expr = visitValue(primExpr.value());
        }
        
        return expr;
    } 
    
    @Override
    public LustreExpr visitValue(ValueContext value) {
        LustreExpr expr = null;
        
        if(value.INT() != null) {
            expr = new IntExpr(new BigInteger(value.INT().getSymbol().getText()));
        } else if(value.FLOAT() != null) {
            expr = new RealExpr(new BigDecimal(value.FLOAT().getSymbol().getText()));
        } else if(value.ID() != null) {
            expr = new VarIdExpr(value.ID().getSymbol().getText());
        }        
        return expr;
    }     
}
