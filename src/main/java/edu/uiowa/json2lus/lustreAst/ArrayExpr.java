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
public class ArrayExpr extends LustreExpr {
    public final String name;
    public final List<String> dimmensions;

    public ArrayExpr(String name, List<String> dimmensions) {
        this.name = name;
        this.dimmensions = dimmensions;
    }        
    
    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    
}
