/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus.lustreAst;

/**
 *
 * @author Paul Meng
 */
public class AutomatonIteExpr extends LustreExpr {
    public LustreExpr ifExpr;
    public LustreExpr restartExpr;
    public LustreExpr resumeExpr;
    public LustreExpr elseExpr;

    public AutomatonIteExpr(LustreExpr ifExpr, LustreExpr restartExpr, LustreExpr resumeExpr, LustreExpr elseExpr) {
        this.ifExpr         = ifExpr;
        this.restartExpr    = restartExpr;
        this.resumeExpr     = resumeExpr;
        this.elseExpr       = elseExpr;
    }

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("if ");
        sb.append(ifExpr.toString());
        if(this.restartExpr != null) {
            sb.append(" restart \n");
            sb.append("    ");
            sb.append(this.restartExpr.toString());            
        } else {
            sb.append(" resume \n");
            sb.append("    ");
            sb.append(this.resumeExpr.toString());                
        }
        sb.append("\n else \n");
        sb.append("    ");
        sb.append(elseExpr.toString());
        sb.append("\n");
        return sb.toString();        
    }     
    
}
