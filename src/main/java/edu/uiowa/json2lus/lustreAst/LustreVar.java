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
}
