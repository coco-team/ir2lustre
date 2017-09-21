/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus.lustre;

import java.util.List;

/**
 *
 * @author Paul Meng
 */
public class LustreNode extends LustreAst {
	public final String name;
	public final List<LustreVar> inputVars;
	public final List<LustreVar> outputVars;
	public final List<LustreVar> localVars;
	public final List<LustreExpr> bodyExprs;
    
        public LustreNode(String name, List<LustreVar> inputs, List<LustreVar> outputs, List<LustreVar> locals, List<LustreExpr> bodyExprs) {
            this.name       = name;
            this.inputVars  = inputs;
            this.outputVars = outputs;
            this.localVars  = locals;
            this.bodyExprs  = bodyExprs;
        }
}
