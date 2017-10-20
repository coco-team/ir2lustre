/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus;

import edu.uiowa.json2lus.ExprParser.BinaryNode;
import org.parboiled.BaseParser;
import static org.parboiled.BaseParser.EOI;
import org.parboiled.Rule;
import org.parboiled.annotations.DontLabel;
import org.parboiled.annotations.SuppressNode;
import org.parboiled.support.StringVar;
import org.parboiled.trees.ImmutableBinaryTreeNode;

/**
 *
 * @author Paul Meng
 */

public class ExprParser extends BaseParser<BinaryNode> {
    
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
                        ZeroOrMore(OR, op.set(matchOrDefault("")), 
                        ConditionalAndExpression(), 
                        push(new BinaryNode(op.get(), pop(1), pop()))));
    }    
    
    Rule ConditionalAndExpression() {
        StringVar op = new StringVar();
        return Sequence(EqualityExpression(),
                        ZeroOrMore(AND, op.set(matchOrDefault("")), 
                        EqualityExpression(), 
                        push(new BinaryNode(op.get(), pop(1), pop()))));
    }    
    

    Rule EqualityExpression() {
        StringVar op = new StringVar();
        return Sequence(RelationalExpression(),
                        ZeroOrMore(FirstOf(EQ, NEQ), op.set(matchOrDefault("")), 
                        RelationalExpression(), 
                        push(new BinaryNode(op.get(), pop(1), pop()))));
    }    
    
    Rule RelationalExpression() {
        StringVar op = new StringVar();
        return Sequence(UnaryExpression(),
                        ZeroOrMore(FirstOf(LTE, GTE, LT, GT), op.set(matchOrDefault("")), 
                        UnaryExpression(), 
                        push(new BinaryNode(op.get(), pop(1), pop()))));
    } 
    
    Rule UnaryExpression() {
        StringVar op = new StringVar();
        return FirstOf(Sequence(FirstOf(NEG, NOT), op.set(matchOrDefault("")), 
                       UnaryExpression(), 
                       push(new BinaryNode(op.get(), pop(1), pop()))), 
                       Primary());
    }  
    
    Rule Primary() {
        return FirstOf(ParExpression(), InputVar());
    }
    
    Rule ParExpression() {
        return Sequence(LPAR, Expression(), RPAR);
    }     

    Rule InputVar() {
        return Sequence(Literal(), push(new BinaryNode(matchOrDefault("0"))));
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

    public static class BinaryNode extends ImmutableBinaryTreeNode<BinaryNode> {
        public String value;
        public String operator;

        public BinaryNode(String value) {
            super(null, null);
            this.value = value;
        }

        public BinaryNode(String operator, BinaryNode left, BinaryNode right) {
            super(left, right);
            this.operator = operator;
        }

        public String getValue() {
            if (operator == null) return value;
            System.out.println("Getvalue operator: " + operator);
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
                    return left().getValue() + " and " + right().getValue();                    
                case "|":
                    return left().getValue() + " or " + right().getValue();   
                case "~":
                    return " not(" + left().getValue() + ")";                       
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
