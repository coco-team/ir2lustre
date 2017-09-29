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
public class LustreVar extends LustreAst {
    public final String     name;
    public final LustreType type;
    
    public LustreVar(String name, LustreType type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public void accept(LustreAstVisitor visitor) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
