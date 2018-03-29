/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
 */

package edu.uiowa.json2lus.lustreAst;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Paul Meng
 */
public class NodeCallExpr extends LustreExpr {
    public String                   nodeName;
    public final List<LustreExpr>   parameters;
    
    public NodeCallExpr(String nodeName, List<LustreExpr> args) {
        this.nodeName   = nodeName;
        this.parameters = args;
    }
    
    public NodeCallExpr(String nodeName, LustreExpr ... args) {
        this.nodeName   = nodeName;
        this.parameters = Arrays.asList(args);
    }  
    
    public void setNodeName(String name) {
        this.nodeName = name;
    }

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    
}
