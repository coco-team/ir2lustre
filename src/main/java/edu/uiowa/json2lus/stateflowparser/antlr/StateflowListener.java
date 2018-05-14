// Generated from Stateflow.g4 by ANTLR 4.7.1
package edu.uiowa.json2lus.stateflowparser.antlr;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link StateflowParser}.
 */
public interface StateflowListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link StateflowParser#fileDecl}.
	 * @param ctx the parse tree
	 */
	void enterFileDecl(StateflowParser.FileDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#fileDecl}.
	 * @param ctx the parse tree
	 */
	void exitFileDecl(StateflowParser.FileDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#endStat}.
	 * @param ctx the parse tree
	 */
	void enterEndStat(StateflowParser.EndStatContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#endStat}.
	 * @param ctx the parse tree
	 */
	void exitEndStat(StateflowParser.EndStatContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#endStatNL}.
	 * @param ctx the parse tree
	 */
	void enterEndStatNL(StateflowParser.EndStatNLContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#endStatNL}.
	 * @param ctx the parse tree
	 */
	void exitEndStatNL(StateflowParser.EndStatNLContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#partialFunctionDecl}.
	 * @param ctx the parse tree
	 */
	void enterPartialFunctionDecl(StateflowParser.PartialFunctionDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#partialFunctionDecl}.
	 * @param ctx the parse tree
	 */
	void exitPartialFunctionDecl(StateflowParser.PartialFunctionDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#functionDecl}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDecl(StateflowParser.FunctionDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#functionDecl}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDecl(StateflowParser.FunctionDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#methodDecl}.
	 * @param ctx the parse tree
	 */
	void enterMethodDecl(StateflowParser.MethodDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#methodDecl}.
	 * @param ctx the parse tree
	 */
	void exitMethodDecl(StateflowParser.MethodDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#classDecl}.
	 * @param ctx the parse tree
	 */
	void enterClassDecl(StateflowParser.ClassDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#classDecl}.
	 * @param ctx the parse tree
	 */
	void exitClassDecl(StateflowParser.ClassDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#propBlockDecl}.
	 * @param ctx the parse tree
	 */
	void enterPropBlockDecl(StateflowParser.PropBlockDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#propBlockDecl}.
	 * @param ctx the parse tree
	 */
	void exitPropBlockDecl(StateflowParser.PropBlockDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#methodBlockDecl}.
	 * @param ctx the parse tree
	 */
	void enterMethodBlockDecl(StateflowParser.MethodBlockDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#methodBlockDecl}.
	 * @param ctx the parse tree
	 */
	void exitMethodBlockDecl(StateflowParser.MethodBlockDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#outArgs}.
	 * @param ctx the parse tree
	 */
	void enterOutArgs(StateflowParser.OutArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#outArgs}.
	 * @param ctx the parse tree
	 */
	void exitOutArgs(StateflowParser.OutArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#inArgs}.
	 * @param ctx the parse tree
	 */
	void enterInArgs(StateflowParser.InArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#inArgs}.
	 * @param ctx the parse tree
	 */
	void exitInArgs(StateflowParser.InArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#prop}.
	 * @param ctx the parse tree
	 */
	void enterProp(StateflowParser.PropContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#prop}.
	 * @param ctx the parse tree
	 */
	void exitProp(StateflowParser.PropContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#dotRef}.
	 * @param ctx the parse tree
	 */
	void enterDotRef(StateflowParser.DotRefContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#dotRef}.
	 * @param ctx the parse tree
	 */
	void exitDotRef(StateflowParser.DotRefContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#statBlock}.
	 * @param ctx the parse tree
	 */
	void enterStatBlock(StateflowParser.StatBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#statBlock}.
	 * @param ctx the parse tree
	 */
	void exitStatBlock(StateflowParser.StatBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#ifStat}.
	 * @param ctx the parse tree
	 */
	void enterIfStat(StateflowParser.IfStatContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#ifStat}.
	 * @param ctx the parse tree
	 */
	void exitIfStat(StateflowParser.IfStatContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#whileStat}.
	 * @param ctx the parse tree
	 */
	void enterWhileStat(StateflowParser.WhileStatContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#whileStat}.
	 * @param ctx the parse tree
	 */
	void exitWhileStat(StateflowParser.WhileStatContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#caseStat}.
	 * @param ctx the parse tree
	 */
	void enterCaseStat(StateflowParser.CaseStatContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#caseStat}.
	 * @param ctx the parse tree
	 */
	void exitCaseStat(StateflowParser.CaseStatContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#stat}.
	 * @param ctx the parse tree
	 */
	void enterStat(StateflowParser.StatContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#stat}.
	 * @param ctx the parse tree
	 */
	void exitStat(StateflowParser.StatContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#arrayExpr}.
	 * @param ctx the parse tree
	 */
	void enterArrayExpr(StateflowParser.ArrayExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#arrayExpr}.
	 * @param ctx the parse tree
	 */
	void exitArrayExpr(StateflowParser.ArrayExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#cellExpr}.
	 * @param ctx the parse tree
	 */
	void enterCellExpr(StateflowParser.CellExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#cellExpr}.
	 * @param ctx the parse tree
	 */
	void exitCellExpr(StateflowParser.CellExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#assignmentExpr}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentExpr(StateflowParser.AssignmentExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#assignmentExpr}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentExpr(StateflowParser.AssignmentExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#booleanExpr}.
	 * @param ctx the parse tree
	 */
	void enterBooleanExpr(StateflowParser.BooleanExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#booleanExpr}.
	 * @param ctx the parse tree
	 */
	void exitBooleanExpr(StateflowParser.BooleanExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#andExpr}.
	 * @param ctx the parse tree
	 */
	void enterAndExpr(StateflowParser.AndExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#andExpr}.
	 * @param ctx the parse tree
	 */
	void exitAndExpr(StateflowParser.AndExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#equalityExpr}.
	 * @param ctx the parse tree
	 */
	void enterEqualityExpr(StateflowParser.EqualityExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#equalityExpr}.
	 * @param ctx the parse tree
	 */
	void exitEqualityExpr(StateflowParser.EqualityExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#relationalExpr}.
	 * @param ctx the parse tree
	 */
	void enterRelationalExpr(StateflowParser.RelationalExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#relationalExpr}.
	 * @param ctx the parse tree
	 */
	void exitRelationalExpr(StateflowParser.RelationalExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#additiveExpr}.
	 * @param ctx the parse tree
	 */
	void enterAdditiveExpr(StateflowParser.AdditiveExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#additiveExpr}.
	 * @param ctx the parse tree
	 */
	void exitAdditiveExpr(StateflowParser.AdditiveExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#multExpr}.
	 * @param ctx the parse tree
	 */
	void enterMultExpr(StateflowParser.MultExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#multExpr}.
	 * @param ctx the parse tree
	 */
	void exitMultExpr(StateflowParser.MultExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#unaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpr(StateflowParser.UnaryExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#unaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpr(StateflowParser.UnaryExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryExpr(StateflowParser.PrimaryExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryExpr(StateflowParser.PrimaryExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#arithExpr}.
	 * @param ctx the parse tree
	 */
	void enterArithExpr(StateflowParser.ArithExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#arithExpr}.
	 * @param ctx the parse tree
	 */
	void exitArithExpr(StateflowParser.ArithExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#mExpr}.
	 * @param ctx the parse tree
	 */
	void enterMExpr(StateflowParser.MExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#mExpr}.
	 * @param ctx the parse tree
	 */
	void exitMExpr(StateflowParser.MExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#uExpr}.
	 * @param ctx the parse tree
	 */
	void enterUExpr(StateflowParser.UExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#uExpr}.
	 * @param ctx the parse tree
	 */
	void exitUExpr(StateflowParser.UExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValue(StateflowParser.ValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValue(StateflowParser.ValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(StateflowParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(StateflowParser.ExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#exprList}.
	 * @param ctx the parse tree
	 */
	void enterExprList(StateflowParser.ExprListContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#exprList}.
	 * @param ctx the parse tree
	 */
	void exitExprList(StateflowParser.ExprListContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#exprArrayList}.
	 * @param ctx the parse tree
	 */
	void enterExprArrayList(StateflowParser.ExprArrayListContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#exprArrayList}.
	 * @param ctx the parse tree
	 */
	void exitExprArrayList(StateflowParser.ExprArrayListContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#verticalArrayExpr}.
	 * @param ctx the parse tree
	 */
	void enterVerticalArrayExpr(StateflowParser.VerticalArrayExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#verticalArrayExpr}.
	 * @param ctx the parse tree
	 */
	void exitVerticalArrayExpr(StateflowParser.VerticalArrayExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link StateflowParser#horizontalArrayExpr}.
	 * @param ctx the parse tree
	 */
	void enterHorizontalArrayExpr(StateflowParser.HorizontalArrayExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link StateflowParser#horizontalArrayExpr}.
	 * @param ctx the parse tree
	 */
	void exitHorizontalArrayExpr(StateflowParser.HorizontalArrayExprContext ctx);
}