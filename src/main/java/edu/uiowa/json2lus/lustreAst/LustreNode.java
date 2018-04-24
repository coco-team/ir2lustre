/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
 */

package edu.uiowa.json2lus.lustreAst;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Paul Meng
 */
public class LustreNode extends LustreAst {    
    public final String             name;
    public final boolean            isMain;
    public final List<LustreVar>    inputVars;
    public final List<LustreVar>    outputVars;
    public final List<LustreVar>    localVars;
    public final List<LustreEq>     bodyExprs;
    public final List<LustreEq>     propExprs;
    public LustreAutomaton           automata;

    public LustreNode(boolean isMain, String name, List<LustreVar> inputs, List<LustreVar> outputs, List<LustreVar> locals, List<LustreEq> bodyExprs, List<LustreEq> props) {
        this.isMain     = isMain;
        this.name       = name;
        this.inputVars  = inputs;
        this.outputVars = outputs;
        this.localVars  = locals;
        this.bodyExprs  = bodyExprs;
        this.propExprs  = props;
        this.automata   = null;
    }
    
    public LustreNode(String name, List<LustreVar> inputs, List<LustreVar> outputs, List<LustreVar> locals, List<LustreEq> bodyExprs, List<LustreEq> props) {
        this.isMain     = false;
        this.name       = name;
        this.inputVars  = inputs;
        this.outputVars = outputs;
        this.localVars  = locals;
        this.bodyExprs  = bodyExprs;
        this.propExprs  = props;
        this.automata   = null;
    }

        
    public LustreNode(String name, List<LustreVar> inputs, List<LustreVar> outputs, List<LustreVar> locals, List<LustreEq> bodyExprs) {
        this.isMain     = false;
        this.name       = name;
        this.inputVars  = inputs;
        this.outputVars = outputs;
        this.localVars  = locals;
        this.bodyExprs  = bodyExprs;
        this.propExprs  = new ArrayList<>();
        this.automata   = null;
    }

    public LustreNode(boolean isMain, String name, List<LustreVar> inputs, List<LustreVar> outputs, List<LustreEq> bodyExprs) {
        this.name       = name;
        this.isMain     = isMain;
        this.inputVars  = inputs;
        this.outputVars = outputs;
        this.automata   = null;
        this.bodyExprs  = bodyExprs;
        this.localVars  = new ArrayList<>();
        this.propExprs  = new ArrayList<>();            
    }  

    public LustreNode(boolean isMain, String name, LustreVar input, LustreVar output, List<LustreEq> bodyExprs) {
        this.name       = name;
        this.isMain     = isMain;
        this.automata   = null;
        this.inputVars  = Arrays.asList(input);
        this.outputVars = Arrays.asList(output);
        this.bodyExprs  = bodyExprs;
        this.localVars  = new ArrayList<>();
        this.propExprs  = new ArrayList<>();            
    }          

    public LustreNode(String name, LustreVar input, LustreVar output, List<LustreEq> bodyExprs) {
        this.name       = name;
        this.isMain     = false;
        this.automata   = null;
        this.inputVars  = Arrays.asList(input);
        this.outputVars = Arrays.asList(output);
        this.bodyExprs  = bodyExprs;
        this.localVars  = new ArrayList<>();
        this.propExprs  = new ArrayList<>();          
    }

    public LustreNode(String name, List<LustreVar> inputVars, List<LustreVar> outputVars, List<LustreEq> bodyExprs) {
        this.name       = name;
        this.isMain     = false;
        this.automata   = null;
        this.inputVars  = inputVars;
        this.outputVars = outputVars;
        this.bodyExprs  = bodyExprs;
        this.localVars  = new ArrayList<>();
        this.propExprs  = new ArrayList<>();          
    }
    
    public LustreNode(String name, LustreAutomaton automata, List<LustreVar> inputVars, List<LustreVar> outputVars, List<LustreVar> localVars) {
        this.isMain     = false;
        this.name       = name;
        this.automata   = automata;
        this.inputVars  = inputVars;
        this.outputVars = outputVars;
        this.localVars  = localVars;
        this.bodyExprs  = new ArrayList<>();        
        this.propExprs  = new ArrayList<>();         
    }

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
}
