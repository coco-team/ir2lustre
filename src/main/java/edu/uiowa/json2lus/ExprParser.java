/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus;

import edu.uiowa.json2lus.ExprParser.AstNode;
import org.parboiled.BaseParser;
import static org.parboiled.BaseParser.EOI;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.DontLabel;
import org.parboiled.annotations.SuppressNode;
import org.parboiled.support.StringVar;
import org.parboiled.trees.ImmutableBinaryTreeNode;

/**
 *
 * @author Paul Meng
 */

@SuppressWarnings({"InfiniteRecursion"})
@BuildParseTree
public class ExprParser extends BaseParser<AstNode> {
    
    final Rule AND  = Terminal("&");
    final Rule OR   = Terminal("|");
    final Rule NOT  = Terminal("~");
    final Rule NEG  = Terminal("-");    
    final Rule LT   = Terminal("<");
    final Rule LTE  = Terminal("<=");
    final Rule GT   = Terminal(">");
    final Rule GTE  = Terminal(">=");
    final Rule EQ   = Terminal("==");
    final Rule NEQ  = Terminal("~=");
    final Rule LPAR = Terminal("(");    
    final Rule RPAR = Terminal(")");        
    
    public Rule InputLine() {
        return Sequence(Expression(), EOI);
    }

    Rule Expression() {
        return ConditionalOrExpression();
    }      
    
    Rule ConditionalOrExpression() {
        StringVar op = new StringVar();
        return Sequence(ConditionalAndExpression(),
                        ZeroOrMore(OR, op.set(matchOrDefault(null)), 
                        ConditionalAndExpression(), 
                        push(new AstNode(op.get(), pop(1), pop())))
        );
    }    
    
    Rule ConditionalAndExpression() {
        StringVar op = new StringVar();
        return Sequence(EqualityExpression(),
                        ZeroOrMore(AND, op.set(matchOrDefault(null)), 
                        EqualityExpression(), 
                        push(new AstNode(op.get(), pop(1), pop())))
        );
    }    
    

    Rule EqualityExpression() {
        StringVar op = new StringVar();
        return Sequence(RelationalExpression(),
                        ZeroOrMore(FirstOf(EQ, NEQ), op.set(matchOrDefault(null)), 
                        RelationalExpression(), 
                        push(new AstNode(op.get(), pop(1), pop())))
        );
    }    
    
    Rule RelationalExpression() {
        StringVar op = new StringVar();
        return Sequence(UnaryExpression(),
                        ZeroOrMore(FirstOf(LTE, GTE, LT, GT), op.set(matchOrDefault(null)), 
                        UnaryExpression(), 
                        push(new AstNode(op.get(), pop(1), pop())))
        );
    } 

    Rule UnaryExpression() {
        StringVar op = new StringVar();
        return FirstOf(Sequence(FirstOf(NEG, NOT), op.set(matchOrDefault(null)), 
                                UnaryExpression(), 
                                push(new AstNode(op.get(), pop(), null))), 
                       Primary());
    }  
    
    Rule Primary() {
        return FirstOf(ParExpression(), InputVar(), Numeral());
    }
    
    Rule ParExpression() {
        return Sequence(LPAR, Expression(), RPAR);
    }     

    Rule InputVar() {
        return Sequence(Literal(), push(new AstNode(matchOrDefault(null))));
    }
    
    Rule Numeral() {
        return Sequence(Number(), push(new AstNode(matchOrDefault(null))));
    }

    Rule Number() {
        return Sequence(WhiteSpace(), OneOrMore(CharRange('0', '9'), Optional(".", ZeroOrMore(CharRange('0', '9'))), WhiteSpace())).suppressSubnodes();
    }
    
    Rule Literal() {
        return Sequence(WhiteSpace(), 'u', OneOrMore(Digit()), WhiteSpace()).suppressSubnodes();        
    }

    Rule Digit() {
        return CharRange('1', '9');
    }

    Rule WhiteSpace() {
        return ZeroOrMore(AnyOf(" \t\f"));
    }        

    @SuppressNode
    @DontLabel
    Rule Terminal(String string) {
        return Sequence(string, WhiteSpace()).label('\'' + string + '\'');
    }    

    @Override
    protected Rule fromStringLiteral(String string) {
        return string.endsWith(" ") ?
                Sequence(String(string.substring(0, string.length() - 1)), WhiteSpace()) :
                String(string);
    }
    
    public static class AstNode extends ImmutableBinaryTreeNode<AstNode> {
        public String value;
        public String operator;

        public AstNode(String value) {
            super(null, null);
            this.value = value;
        }

        public AstNode(String operator, AstNode left, AstNode right) {
            super(left, right);
            this.operator = operator;
        }

        public String getValue() {
            if (operator == null) return value;
            switch (operator.trim()) {
                case "<":
                    return left().getValue() + " < " + right().getValue();
                case ">":
                    return left().getValue() + " > " +right().getValue();
                case "<=":
                    return left().getValue() + " <= " + right().getValue();
                case ">=":
                    return left().getValue() + " >= " + right().getValue();
                case "==":
                    return left().getValue() + " == " + right().getValue();
                case "~=":
                    return left().getValue() + " ~= " + right().getValue();
                case "&":
                    return left().getValue() + " & " + right().getValue();                    
                case "|":
                    return left().getValue() + " | " + right().getValue();   
                case "~":
                    return " ~ (" + left().getValue() + ")"; 
                case "-":
                    return " (-" + left().getValue() + ")";                     
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        public String toString() {
            return (operator == null ? "Value " + value : "Operator '" + operator + '\'') + " | " + getValue();
        }
    }    
}
