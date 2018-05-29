// Generated from Stateflow.g4 by ANTLR 4.7.1
package edu.uiowa.json2lus.stateflowparser.antlr;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link StateflowParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface StateflowVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link StateflowParser#fileDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFileDecl(StateflowParser.FileDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#endStat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEndStat(StateflowParser.EndStatContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#endStatNL}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEndStatNL(StateflowParser.EndStatNLContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#partialFunctionDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartialFunctionDecl(StateflowParser.PartialFunctionDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#functionDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDecl(StateflowParser.FunctionDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#methodDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodDecl(StateflowParser.MethodDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#classDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassDecl(StateflowParser.ClassDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#propBlockDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropBlockDecl(StateflowParser.PropBlockDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#methodBlockDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodBlockDecl(StateflowParser.MethodBlockDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#outArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOutArgs(StateflowParser.OutArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#inArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInArgs(StateflowParser.InArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#prop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProp(StateflowParser.PropContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#dotRef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDotRef(StateflowParser.DotRefContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#statBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatBlock(StateflowParser.StatBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#ifStat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfStat(StateflowParser.IfStatContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#whileStat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileStat(StateflowParser.WhileStatContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#caseStat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCaseStat(StateflowParser.CaseStatContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#stat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStat(StateflowParser.StatContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#arrayExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayExpr(StateflowParser.ArrayExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#cellExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCellExpr(StateflowParser.CellExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#assignmentExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentExpr(StateflowParser.AssignmentExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#booleanExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanExpr(StateflowParser.BooleanExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#andExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAndExpr(StateflowParser.AndExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#equalityExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualityExpr(StateflowParser.EqualityExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#relationalExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationalExpr(StateflowParser.RelationalExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#additiveExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditiveExpr(StateflowParser.AdditiveExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#multExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultExpr(StateflowParser.MultExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#unaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryExpr(StateflowParser.UnaryExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#primaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryExpr(StateflowParser.PrimaryExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#arithExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithExpr(StateflowParser.ArithExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#mExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMExpr(StateflowParser.MExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#uExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUExpr(StateflowParser.UExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue(StateflowParser.ValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr(StateflowParser.ExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#exprList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprList(StateflowParser.ExprListContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#exprArrayList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprArrayList(StateflowParser.ExprArrayListContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#verticalArrayExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVerticalArrayExpr(StateflowParser.VerticalArrayExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link StateflowParser#horizontalArrayExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHorizontalArrayExpr(StateflowParser.HorizontalArrayExprContext ctx);
}