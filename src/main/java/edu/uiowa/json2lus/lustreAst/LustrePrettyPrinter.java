/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
 */

package edu.uiowa.json2lus.lustreAst;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Paul Meng
 */
public class LustrePrettyPrinter implements LustreAstVisitor{
    private static final String NL  = System.getProperty("line.separator");    
    private static final String TAB = "  ";
    
    StringBuilder sb = new StringBuilder();
    Map<String, Set<String>> nodeContractsMap = new HashMap<>();
    
    
    public void printLustreProgramToFile(String path) {
        Logger.getLogger(LustrePrettyPrinter.class.getName()).log(Level.INFO, "****************************************************");
        Logger.getLogger(LustrePrettyPrinter.class.getName()).log(Level.INFO, "\n{0}", sb.toString());
        Logger.getLogger(LustrePrettyPrinter.class.getName()).log(Level.INFO, "****************************************************");
        try {
            File output = new File(path);

            if(!output.getAbsoluteFile().exists()) {
                if(!output.getAbsoluteFile().getParentFile().exists()) {
                    output.getAbsoluteFile().getParentFile().createNewFile();
                }
                output.getAbsoluteFile().createNewFile();
            }            
            BufferedWriter bw = new BufferedWriter(new FileWriter(output.getAbsoluteFile()));
            bw.write(sb.toString());
            bw.close();
            Logger.getLogger(LustrePrettyPrinter.class.getName()).log(Level.INFO, "A Lustre program was generated at: {0}", path);
        } catch (IOException e) {
            Logger.getLogger(LustrePrettyPrinter.class.getName()).log(Level.SEVERE, "Dumping to lustre file fails!");
        }        
    }
    
    public void printLustreProgramToFile(LustreProgram lusProg, String path) {
        visit(lusProg);
        printLustreProgramToFile(path);
    }
    
    @Override
    public void visit(LustreProgram program) {
        this.nodeContractsMap = program.nodeContractsMap;
        List<LustreNode> cNodes = new ArrayList<>();
        
        for(LustreEnumType enumType : program.enumTypes) {
            enumType.accept(this);
            sb.append(NL);
        }        
        
        sb.append(NL);                      
              
        for(int i = 0; i < program.nodes.size(); ++i) {
            LustreNode node = program.nodes.get(i);
            
            if(!nodeContractsMap.containsKey(node.name)) {
                node.accept(this);sb.append(NL).append(NL);
            } else {
                cNodes.add(node);
            }
        }
        
        sb.append(NL);
        
        for(LustreContract contract : program.contracts) {
            contract.accept(this);sb.append(NL);
        }  
        
        sb.append(NL);          
              
        for(LustreNode node : cNodes) {
            node.accept(this);sb.append(NL).append(NL);
        }        
    }    

    @Override
    public void visit(LustreNode node) {
        sb.append("node ").append(node.name);
        sb.append("(").append(NL);
        declVariables(node.inputVars);
        sb.append(")").append(NL);
        sb.append("returns (").append(NL);
        declVariables(node.outputVars);
        sb.append(");").append(NL);        
        
        if(this.nodeContractsMap.containsKey(node.name)) {
            int s = 0;
            Set<String>         contracts   = this.nodeContractsMap.get(node.name);
            Iterator<String>    it          = contracts.iterator();
            
            sb.append(NL);
            sb.append("(*@contract ");
            
            while(it.hasNext()) {
                sb.append("import ").append(it.next()).append("(");
                for(int i = 0; i< node.inputVars.size(); i++) {
                    sb.append(node.inputVars.get(i).name);
                    if(i < node.inputVars.size()-1) {
                        sb.append(", ");
                    }
                }                
                sb.append(")");
                sb.append(" returns (");
                for(int i = 0; i< node.outputVars.size(); i++) {
                    sb.append(node.outputVars.get(i).name);
                    if(i < node.outputVars.size()-1) {
                        sb.append(", ");
                    }
                }                  
                sb.append(");");
                if(s < contracts.size()-1) {
                    sb.append(NL);
                }
                ++s;
            }
            sb.append("*)").append(NL).append(NL);            
        }        
        if(!node.localVars.isEmpty()) {
            sb.append("var ").append(NL);
            declVariables(node.localVars);
            sb.append(";").append(NL);
        }
        
        sb.append("let").append(NL); 
        for(LustreEq eq : node.bodyExprs) {
            sb.append("  ");eq.accept(this); sb.append(NL);
        }
        if(!node.propExprs.isEmpty()) {
            sb.append(NL);
        }
        if(node.automata != null) {
            node.automata.accept(this);
        }
        for(LustreEq prop : node.propExprs) {
            for(LustreExpr propVarId : prop.lhs) {
                if(propVarId instanceof VarIdExpr) {
                    sb.append("  ");sb.append("--%PROPERTY ")
                      .append(" \"").append(((VarIdExpr)propVarId).id).append("\" ");
                    for(LustreExpr expr : prop.rhs)
                    {
                        expr.accept(this);
                    }
                    sb.append(";").append(NL);                    
                }
            }
        }
        sb.append("tel;").append(NL).append(NL);        
    }    
    
    @Override
    public String toString() {
        return sb.toString();
    }

    @Override
    public void visit(IteExpr expr) {
        sb.append(" (if ");
        expr.ifExpr.accept(this);
        sb.append(" then ");
        expr.thenExpr.accept(this);
        sb.append(" else ");
        expr.elseExpr.accept(this);
        sb.append(")");
    }

    @Override
    public void visit(IntExpr expr) {
        sb.append(expr.value);
    }

    @Override
    public void visit(RealExpr expr) {
        sb.append(expr.value);
    }

    @Override
    public void visit(UnaryExpr expr) {
        sb.append("(");
        sb.append(expr.op).append(" ");
        expr.expr.accept(this);
        sb.append(")");
    }

    @Override
    public void visit(BinaryExpr expr) {
        sb.append("(");
        expr.left.accept(this);
        sb.append(" ").append(expr.op).append(" ");
        expr.right.accept(this);
        sb.append(")");
    }

    @Override
    public void visit(BooleanExpr expr) {
        sb.append(expr.value);
    }

    @Override
    public void visit(NodeCallExpr expr) {
        sb.append(expr.nodeName);
        sb.append("(");
        for(int i = 0; i< expr.parameters.size(); i++) {
            expr.parameters.get(i).accept(this);
            if(i < expr.parameters.size()-1) {
                sb.append(", ");
            }
        }
        sb.append(")");
    }
    
    private void declVariables(List<LustreVar> vars) {
        for(int i = 0; i < vars.size(); i++) {
            vars.get(i).accept(this);
            if(i < vars.size()-1) {
                sb.append("; ");
                sb.append(NL);
            }            
        }
    }

    @Override
    public void visit(LustreContract contract) {
        sb.append("contract ").append(contract.name);
        sb.append(" (").append(NL);
        declVariables(contract.inputs);
        sb.append(")").append(NL);
        sb.append("returns (").append(NL);
        declVariables(contract.outputs);
        sb.append(");").append(NL);
        sb.append("let").append(NL);
        
        if(!contract.localEqs.isEmpty()) {
            for(LustreEq eq : contract.localEqs) {
                sb.append("  var ");
                for(LustreExpr var : eq.lhs) {
                    if(var instanceof VarIdExpr) {
                        LustreVar lustVar = null;
                        for(LustreVar v : contract.localVars) {
                            if(((VarIdExpr)var).id.equals(v.name)) {
                                lustVar = v;
                                break;
                            }
                        }
                        if(lustVar != null) {
                            sb.append(lustVar.name).append(" : ").append(lustVar.type).append(" ");
                        }                        
                    }
                }
                sb.append(" = ");
                for(LustreExpr expr : eq.rhs)
                {
                    expr.accept(this);
                }                                
                sb.append(";").append(NL).append(NL);
            }
        }
        for(Map.Entry<String, LustreExpr> assume : contract.assumptions.entrySet()) {
            sb.append(TAB).append("assume ");
            sb.append(" \"").append(assume.getKey()).append("\" ");
            assume.getValue().accept(this);sb.append(";").append(NL);
        }
        sb.append(NL);
        for(Map.Entry<String, LustreExpr> gurantee : contract.guarantees.entrySet()) {
            sb.append(TAB).append("guarantee ");
            sb.append(" \"").append(gurantee.getKey()).append("\" ");
            gurantee.getValue().accept(this);sb.append(";").append(NL);
        }
        sb.append(NL);
        
        Set<String> modes = new HashSet<>();
        modes.addAll(contract.modeToEnsures.keySet());
        modes.addAll(contract.modeToRequires.keySet());
        
        for(String mode : modes) {
            sb.append(TAB).append("mode ").append(mode).append(" (").append(NL);
            if(contract.modeToRequires.containsKey(mode)) {
                for(LustreExpr require : contract.modeToRequires.get(mode)) {
                    sb.append(TAB).append(TAB).append("require ");
                    require.accept(this);
                    sb.append(";").append(NL);
                }
            }
            if(contract.modeToEnsures.containsKey(mode)) {
                for(Map.Entry<String, LustreExpr> ensure : contract.modeToEnsures.get(mode).entrySet()) {
                    sb.append(TAB).append(TAB).append("ensure ");
                    sb.append(" \"").append(ensure.getKey()).append("\" ");
                    ensure.getValue().accept(this);
                    sb.append(";").append(NL);
                }
            }            
            sb.append(TAB).append(");").append(NL).append(NL);
        }
        sb.append("tel").append(NL);
    }
    
    @Override
    public void visit(LustreEq equation) {
        if(equation.lhs.size() > 1) {
            sb.append("(");
            for(int i = 0; i < equation.lhs.size(); i++) {
                equation.lhs.get(i).accept(this);
                if(i < equation.lhs.size()-1) {
                    sb.append(", ");
                }
            }
            sb.append(")");
        } else if(equation.lhs.size() == 1) {
            equation.lhs.get(0).accept(this);
        }
        sb.append(" = ");
        for(LustreExpr expr : equation.rhs)
        {
            expr.accept(this);
        }
//        equation.rhs.accept(this);
        sb.append(";");
    }

    @Override
    public void visit(LustreVar var) {
        if(var.type instanceof PrimitiveType) {
            if(((PrimitiveType)var.type).isConst) {            
                sb.append("const");
            }  
        }
        sb.append("  ").append(var.name).append(" : ");
        if(var.type instanceof PrimitiveType) {            
            var.type.accept(this);
        } else if(var.type instanceof LustreEnumType) {
            sb.append(((LustreEnumType)var.type).name);
        } else if(var.type instanceof ArrayType) {
            var.type.accept(this);
        }
    }

    @Override
    public void visit(VarIdExpr varId) {
        sb.append(varId.id);
    }

    @Override
    public void visit(MergeExpr mergeExpr) {
        sb.append("(merge ");
        mergeExpr.clock.accept(this);
        sb.append(NL);
        for(int i = 0; i < mergeExpr.exprs.size(); i++) {
            sb.append("          ");
            mergeExpr.exprs.get(i).accept(this);
            sb.append(NL);            
        }
        sb.append(")");
    }

    @Override
    public void visit(LustreEnumType enumType) {
        sb.append("type ").append(enumType.name).append(" = enum {");
        for(int i = 0; i < enumType.values.size(); i++) {
            sb.append(enumType.values.get(i));
            if(i != enumType.values.size()-1) {
                sb.append(", ");
            }
        }
        sb.append("};");
    }

    @Override
    public void visit(TupleExpr tupExpr) {
        if(tupExpr.elements.size() == 1) {
            tupExpr.elements.get(0).accept(this);
        } else {
            sb.append("(");
            for(int i = 0; i < tupExpr.elements.size(); i++) {
                tupExpr.elements.get(i).accept(this);
                if(i < tupExpr.elements.size()-1) {
                    sb.append(", ");
                }
            }
            sb.append(")");
        }
    }

    @Override
    public void visit(PrimitiveType type) {
        sb.append(type.name);
    }

    @Override
    public void visit(ArrayType array) {
        sb.append(array.type);
        for(int i = array.dimensions.size()-1; i >=0; --i) {
            sb.append("^").append(array.dimensions.get(i));
        }
        for(int i = array.sDimensions.size()-1; i >=0; --i) {
            sb.append("^").append(array.sDimensions.get(i));
        }      
    }

    @Override
    public void visit(ArrayExpr arrayExpr) {
        if(arrayExpr.name != null) {
            sb.append(arrayExpr.name);    
        } else if(arrayExpr.expr != null){
            arrayExpr.expr.accept(this);
        }
        
        if(arrayExpr.exprs != null) {
            visitArrayExpr(0, arrayExpr.intDims, arrayExpr.exprs);
        } else {
            for(int intD : arrayExpr.intDims) {
                sb.append("[");
                sb.append(intD);
                sb.append("]");            
            }
            for(String strD : arrayExpr.stringDims) {
                sb.append("[");
                sb.append(strD);
                sb.append("]");            
            }             
        }              
    }
    
    public void visitArrayExpr(int curDim, List<Integer> intDims, List<LustreExpr> exprs) {
        if(curDim < intDims.size() - 1) {            
            int dim = intDims.get(curDim);
            sb.append("[");
            for(int i = 0; i < dim; ++i) {
                visitArrayExpr(curDim+1, intDims, exprs);
                if(i != dim-1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
        } else if(curDim == intDims.size() - 1) {             
            int dim = intDims.get(curDim);  
            
            sb.append("[");
            for(int i = 0; i < dim && i < exprs.size(); ++i) {
                exprs.get(i).accept(this);
                if(i != dim-1) {
                    sb.append(", ");
                }                
            }
            sb.append("]");
            //Remove the ones that have been printed
            for(int i = 0; i < dim && i < exprs.size(); ++i) {
                exprs.remove(0);
            }
        }
    }

    @Override
    public void visit(LustreAutomaton automaton) {
        sb.append(NL);
        sb.append("automaton ");
        sb.append(automaton.name);
        sb.append(NL).append(NL);
        
        for(AutomatonState state : automaton.states) {
            visit(state);
        }
    }

    @Override
    public void visit(AutomatonState state) {
        if(state.isInit) {
            sb.append("initial ");
        }
        sb.append("state ");
        sb.append(state.name).append(":").append(NL);
        
        for(LustreExpr strongExpr : state.strongTrans) {
            sb.append(TAB);            
            sb.append("unless ");            
            if(strongExpr instanceof AutomatonIteExpr) {
                visit((AutomatonIteExpr)strongExpr);
            }
            sb.append(";").append(NL);            
        }
        
        if(state.locals.size() > 0) {
            sb.append(TAB);
            sb.append("var ");
        }
        for(LustreVar var : state.locals) {
            sb.append(NL);
            sb.append(TAB).append(TAB);            
            var.accept(this);
            sb.append(";");            
            sb.append(NL);
        }
        sb.append(TAB).append("let").append(NL);
        for(LustreAst eq : state.equations) {            
            if(eq instanceof LustreEq) {
                sb.append(TAB).append(TAB);
                visit((LustreEq)eq);
                sb.append(NL);
            }            
        }
        sb.append(TAB).append("tel").append(NL);
        
        for(LustreExpr weakExpr : state.weakTrans) {
            sb.append(TAB);
            sb.append("until ");
            if(weakExpr instanceof AutomatonIteExpr) {
                visit((AutomatonIteExpr)weakExpr);
                sb.append(";").append(NL);
            }            
        }

        sb.append(NL);
    }

    @Override
    public void visit(AutomatonIteExpr iteExpr) {
        if(iteExpr.elseExpr != null) {
            sb.append("if ");
            iteExpr.ifExpr.accept(this);
            if(iteExpr.restartExpr != null) {
                sb.append(" restart ");
                iteExpr.restartExpr.accept(this);            
            } else if(iteExpr.resumeExpr != null) {
                sb.append(" resume ");
                iteExpr.resumeExpr.accept(this);              
            }
            sb.append(NL);
            if(iteExpr.elseExpr instanceof AutomatonIteExpr) {
                sb.append(TAB+TAB+TAB+TAB);
                sb.append(" els");
                iteExpr.elseExpr.accept(this);
                sb.append(NL);
            } else if(iteExpr.elseExpr == null){   
                sb.append(TAB+TAB+TAB+TAB);
                sb.append(" end;");
            } else {
                sb.append(TAB+TAB+TAB+TAB);
                sb.append(" else ");
                iteExpr.elseExpr.accept(this);
                sb.append(" end;");                           
            }            
        } else {
            iteExpr.ifExpr.accept(this);
            if(iteExpr.restartExpr != null) {
                sb.append(" restart ");
                iteExpr.restartExpr.accept(this);            
            } else if(iteExpr.resumeExpr != null) {
                sb.append(" resume ");
                iteExpr.resumeExpr.accept(this);              
            }            
        }
    }

    @Override
    public void visit(ArrayConst arrConst) {
        if(arrConst.value != null) {
            sb.append(arrConst.value);
        }
        if(arrConst.exprs.size() > 0) {
            sb.append("[");
            for(int i = 0; i < arrConst.exprs.size(); ++i) {
                arrConst.exprs.get(i).accept(this);
                if(i != arrConst.exprs.size()-1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
        }
    }

    @Override
    public void visit(EnumConst enumConst) {
        sb.append(enumConst.toString());
    }
}
