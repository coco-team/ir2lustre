/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus.lustreAst;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Paul Meng
 */
public class LustrePrettyPrinter implements LustreAstVisitor{
    private static final String NL = System.getProperty("line.separator");    
    
    StringBuilder sb = new StringBuilder();
    
    public void printLustreProgramToFile(String path) {
        Logger.getLogger(LustrePrettyPrinter.class.getName()).log(Level.INFO, "****************************************************");
        Logger.getLogger(LustrePrettyPrinter.class.getName()).log(Level.INFO, "\n{0}", sb.toString());
        Logger.getLogger(LustrePrettyPrinter.class.getName()).log(Level.INFO, "****************************************************");
        try {
            File output = new File(path);
            if(!output.exists()) {
                if(!output.getParentFile().exists()) {
                    output.getParentFile().createNewFile();
                }
                output.createNewFile();
            }            
            BufferedWriter bw = new BufferedWriter(new FileWriter(path));
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
        for(LustreNode node : program.nodes) {
            node.accept(this);sb.append(NL).append(NL);
        }
    }    

    @Override
    public void visit(LustreNode node) {
        sb.append("node ").append(node.name);
        sb.append("(");
        declVariables(node.inputVars);
        sb.append(")").append(NL);
        sb.append("returns (");
        declVariables(node.outputVars);
        sb.append(");").append(NL);
        
        if(!node.localVars.isEmpty()) {
            sb.append("var ");
            declVariables(node.localVars);
            sb.append(";").append(NL);
        }
        
        sb.append("let").append(NL); 
        for(LustreEq eq : node.bodyExprs) {
            sb.append("  ");eq.accept(this); sb.append(NL);
        }
        for(LustreEq prop : node.propExprs) {
            sb.append("  ");prop.accept(this); sb.append(NL);
        }
        if(!node.propExprs.isEmpty()) {
            sb.append(NL);
        }
        for(LustreEq prop : node.propExprs) {
            for(VarIdExpr propVarId : prop.lhs) {
                sb.append("  ");sb.append("--%PROPERTY ").append(propVarId.id).append(";").append(NL);
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
        sb.append(" if ");
        expr.ifExpr.accept(this);
        sb.append(" then ");
        expr.thenExpr.accept(this);
        sb.append(" else ");
        expr.elseExpr.accept(this);
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
            }
        }
    }

    @Override
    public void visit(Contract contract) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public void visit(LustreContract contract) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        equation.rhs.accept(this);
        sb.append(";");
    }

    @Override
    public void visit(LustreVar var) {
        sb.append(var.name).append(" : ").append(var.type);
    }

    @Override
    public void visit(VarIdExpr varId) {
        sb.append(varId.id);
    }
    
}
