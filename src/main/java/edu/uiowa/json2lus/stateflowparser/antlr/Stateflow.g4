
/**
* This file is part of CoCoSim_IR_Compiler.
* Copyright (C) 2017-2018  The University of Iowa
*
* Part of this grammar is from https://github.com/mattmcd/ParseMATLAB
* Author: Paul Meng
*/
grammar Stateflow;

fileDecl  
    : (functionDecl | classDecl)? (functionDecl* | partialFunctionDecl*)
    | partialFunctionDecl*
    | statBlock* 
    | EOF
    ;

endStat
    : (NL|COMMA|SEMI|EOF) NL*
    ;

endStatNL 
    : NL+
    ;

// Function declaration without the closing end
partialFunctionDecl
    : FUNCTION outArgs? ID inArgs? endStat statBlock* 
    ; 

// Normal function declaration including closing end
functionDecl
    : partialFunctionDecl END endStatNL NL*
    ;

// Functions inside method blocks can be comma or semi separated 
methodDecl
    : partialFunctionDecl END endStat
    ;

classDecl
    : CLASSDEF ID endStat 
      (propBlockDecl|methodBlockDecl)* 
      END (EOF|endStat) NL*
    ;

propBlockDecl
    : PROPERTIES endStat prop* END endStat
    ;

methodBlockDecl
    : METHODS endStat methodDecl* END endStat
    ;

outArgs
    : ID EQUALS
    | LBRACK ID (COMMA ID)* RBRACK EQUALS
    ;

inArgs
    : LPAREN ID (COMMA ID)* RPAREN
    | LPAREN RPAREN
    ;

prop
    : ID (EQUALS expr)? endStat
    ;

dotRef
    : ID (DOT ID)*
    ;

statBlock
    : (stat endStat)
    ;

ifStat
    : IF expr endStat statBlock* 
      (ELSEIF expr endStat statBlock*)* 
      (ELSE endStat? statBlock*)?
      END
    ;

whileStat
    : WHILE expr endStat statBlock* END
    ;

caseStat
    : SWITCH expr endStat 
      (CASE expr endStat statBlock*)*
      (OTHERWISE endStat statBlock*)?
      END
    ;

stat
    : dotRef EQUALS expr
    | ifStat
    | whileStat
    | caseStat
    | expr
    | NL
    ;

arrayExpr
    : LBRACK exprArrayList RBRACK
    | LBRACK RBRACK
    ;

cellExpr
    : LBRACE exprArrayList RBRACE
    | LBRACE RBRACE
    ;

assignmentExpr
    : ID (EQUALS | PLUSASSIGN | MINUSASSIGN | TIMESASSIGN | DIVIDEASSIGN) expr
    | ID (PLUSPLUS | MINUSMINUS)
    | (LBRACK)? ID (COMMA ID)* (RBRACK)? EQUALS ID LPAREN expr (COMMA expr) RPAREN
    ;

booleanExpr
    : andExpr (OR andExpr )*;

andExpr 
    : equalityExpr (AND equalityExpr)*;

equalityExpr
    : relationalExpr ((EQUALTO | NEQ) relationalExpr)?;

relationalExpr          
    : additiveExpr ((LT | LTE | GT | GTE) additiveExpr)?;

additiveExpr            
    : multExpr ((PLUS | MINUS) multExpr)*;

multExpr                
    : unaryExpr ((MTIMES|TIMES|MLDIVIDE|LDIVIDE|MRDIVIDE|RDIVIDE|MPOW|POW) unaryExpr)*;

unaryExpr               
    : NOT? primaryExpr
    | (PLUS | MINUS)? arithExpr
    ;

primaryExpr             
    : (LPAREN booleanExpr RPAREN)
    | value
    ;


arithExpr               
    : mExpr ((PLUS | MINUS) mExpr)*;

mExpr                   
    : uExpr ((MTIMES|TIMES|MLDIVIDE|LDIVIDE|MRDIVIDE|RDIVIDE|MPOW|POW) uExpr)*;

uExpr                   
    : (LPAREN arithExpr RPAREN) 
    | (PLUS | MINUS) arithExpr
    | value
    ;    

value
    : INT 
    | FLOAT
    | SCI
    | ID
    ;

expr
    : expr LPAREN exprList RPAREN   
    | booleanExpr
    | arithExpr
    | expr COLON expr
    | dotRef
    | STRING
    | arrayExpr
    | cellExpr
    | LPAREN expr RPAREN
    | assignmentExpr
    ;

exprList
    : expr (',' expr)*
    ;

exprArrayList
    : verticalArrayExpr     // #vcat
    ;

verticalArrayExpr
    : horizontalArrayExpr ((SEMI|NL) horizontalArrayExpr)* // #vcat
    ;

horizontalArrayExpr
    : expr (COMMA? exprArrayList)*    // #hcat
    ;


/*
 * Lexer Rules
 */

// Keywords
FUNCTION                : 'function' ;

CLASSDEF                : 'classdef' ;

PROPERTIES              : 'properties' ;

METHODS                 : 'methods' ; 

END                     : 'end' ;

IF                      : 'if' ;

ELSEIF                  : 'elseif' ;

ELSE                    : 'else' ;

WHILE                   : 'while' ;

SWITCH                  : 'switch' ;

CASE                    : 'case' ; 

OTHERWISE               : 'otherwise' ;

// Symbols
NEQ                     : '!=' ;

EQUALS                  : '=' ;

EQUALTO                 : '==' ;

GT                      : '>' ;

LT                      : '<' ;

GTE                     : '>=' ;

LTE                     : '<=' ;

PLUS                    : '+' ;

MINUS                   : '-' ;

PLUSPLUS                : '++' ;

MINUSMINUS              : '--' ;

PLUSASSIGN              : '+=' ;

MINUSASSIGN             : '-=' ;

TIMESASSIGN             : '*=' ;

DIVIDEASSIGN            : '/=' ;

DOT                     : '.' ;

VECAND                  : '&' ;

VECOR                   : '|' ;

AND                     : '&&' ;

OR                      : '||' ;

LPAREN                  : '(' ;

RPAREN                  : ')' ;

LBRACE                  : '{' ;

RBRACE                  : '}' ;

LBRACK                  : '[' ;

RBRACK                  : ']' ;

MTIMES                  : '*' ;

TIMES                   : '.*' ;

RDIVIDE                 : '/' ;

LDIVIDE                 : '\\' ;

MRDIVIDE                : './' ;

MLDIVIDE                : '.\\' ;

POW                     : '.^' ;

MPOW                    : '^' ;

NOT                     : '~' ;

COLON                   : ':' ;

TRANS                   : '.\'' ;

CTRANS                  : '\'' ;

// General rules
NL                      : '\r'?'\n' ;

fragment LINECONTINUE                   : '...' ;
fragment LETTER                         : [a-zA-Z] ; 
fragment DIGIT                          : [0-9] ; 
fragment ESC                            : '\'\'' ;

COMMENT                 : ('%' | LINECONTINUE) .*? NL -> skip ;

INT                     : DIGIT+;

FLOAT                   : DIGIT+ '.' DIGIT* | '.' DIGIT+;

STRING                  : '\'' (ESC|.)*? '\'' ;

SCI                     : (INT|FLOAT) 'e' INT ;

ID  : LETTER (LETTER|DIGIT|'_')* ;


COMMA : ',' ;

SEMI  : ';' ;


WHITESPACE          : [ \t\r]+ -> skip ;    