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
public class LustreContract extends LustreAst {

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    
}