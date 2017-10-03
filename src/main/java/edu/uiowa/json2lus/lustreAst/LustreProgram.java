/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus.lustreAst;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Paul Meng
 */
public class LustreProgram extends LustreAst {
    List<LustreNode> nodes;
    
    public LustreProgram() {
        this.nodes = new ArrayList<>();
    }
    
    public LustreProgram(List<LustreNode> nodes) {
        this.nodes = nodes;
    }    
    
    public void addNode(LustreNode node) {
        this.nodes.add(node);
    }
    
    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    
}
