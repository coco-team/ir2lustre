/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
 */

package edu.uiowa.json2lus.lustreAst;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Paul Meng
 */
public class ArrayExpr extends LustreExpr {
    public String name;
    public LustreExpr expr;    
    public final List<String> stringDims;
    public final List<Integer> intDims;

    public ArrayExpr(String name, List<String> dimensions) {
        this.name = name;
        this.stringDims = dimensions;
        this.intDims = new ArrayList<>();
    }
    
    public ArrayExpr(String name, int dim) {
        this.name = name;
        this.stringDims = new ArrayList<>();
        this.intDims = new ArrayList<>();
        this.intDims.add(dim);
    }    

    public ArrayExpr(List<Integer> dimensions, String name) {
        this.name = name;
        this.intDims = dimensions;
        this.stringDims = new ArrayList<>();
    } 
    
    public ArrayExpr(LustreExpr expr, List<String> dimensions) {
        this.expr = expr;
        this.name = null;
        this.stringDims = dimensions;
        this.intDims = new ArrayList<>();
    } 
    
    public ArrayExpr(LustreExpr expr, int dim) {
        this.expr = expr;
        this.name = null;
        this.stringDims = new ArrayList<>();
        this.intDims = new ArrayList<>();
        this.intDims.add(dim);
    }     

    public ArrayExpr(List<Integer> dimensions, LustreExpr expr) {
        this.name = null;
        this.expr = expr;
        this.intDims = dimensions;
        this.stringDims = new ArrayList<>();
    }     
    
    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    
}
