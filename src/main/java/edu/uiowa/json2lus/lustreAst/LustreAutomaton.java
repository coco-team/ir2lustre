/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus.lustreAst;

import java.util.List;

/**
 *
 * @author Paul Meng
 */
public class LustreAutomaton extends LustreAst{
    String                   name;
    List<AutomatonState>     states;
    
    public LustreAutomaton(String name, List<AutomatonState> states) {
        this.name       = name;
        this.states     = states;
    }

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
}
