/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus.lustreAst;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Paul Meng
 */
public class LustreContract extends LustreAst{
    public final String                             name;
    public List<LustreVar>                          inputs; 
    public List<LustreVar>                          outputs;     
    public final List<LustreExpr>                   assumptions;
    public final List<LustreExpr>                   guarantees;
    public List<LustreVar>                          localVars;  
    public List<LustreEq>                           localEqs; 
    public final Map<String, List<LustreExpr>>      modeToRequires;
    public final Map<String, List<LustreExpr>>      modeToEnsures;    
    
    public LustreContract(String name, List<LustreExpr> assumptions, List<LustreExpr> guarantees, List<LustreVar> inputs, List<LustreVar> outputs, List<LustreVar> localVars, List<LustreEq> localEqs, Map<String, List<LustreExpr>> modeToRequires, Map<String, List<LustreExpr>> modeToEnsures) {
        this.name           = name;
        this.inputs         = inputs;
        this.outputs        = outputs;
        this.localEqs       = new ArrayList<>();
        this.localVars      = new ArrayList<>();
        this.assumptions    = assumptions;
        this.guarantees     = guarantees; 
        this.modeToEnsures  = modeToEnsures;
        this.modeToRequires = modeToRequires;
                
        for(LustreVar var : localVars) {
            this.localVars.add(var);
        }
        for(LustreEq eq : localEqs) {
            this.localEqs.add(eq);
        }        
    }
    
    public LustreContract(String name, List<LustreExpr> assumptions, List<LustreExpr> guarantees, List<LustreVar> inputs, List<LustreVar> outputs, List<LustreVar> localVars, Map<String, List<LustreExpr>> modeToRequires, Map<String, List<LustreExpr>> modeToEnsures) {
        this.name           = name;
        this.inputs         = inputs;
        this.outputs        = outputs;
        this.localVars      = localVars;
        this.assumptions    = assumptions;
        this.guarantees     = guarantees; 
        this.modeToEnsures  = modeToEnsures;
        this.modeToRequires = modeToRequires;
    }    
    
    public LustreContract(String name, List<LustreExpr> assumptions, List<LustreExpr> guarantees, List<LustreVar> inputs, List<LustreVar> outputs, Map<String, List<LustreExpr>> modeToRequires, Map<String, List<LustreExpr>> modeToEnsures) {
        this.name           = name;
        this.inputs         = inputs;
        this.outputs        = outputs;
        this.localVars      = new ArrayList<>();
        this.assumptions    = assumptions;
        this.guarantees     = guarantees; 
        this.modeToEnsures  = modeToEnsures;
        this.modeToRequires = modeToRequires;
    }    
    
    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    
}
