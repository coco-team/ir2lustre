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
public class ArrayType extends LustreType {
    public LustreType       type;
    public List<Integer>    dimensions;
    public List<String>     sDimensions;
    
    public ArrayType(LustreType type, List<Integer> ds) {
        this.type = type;
        this.dimensions = ds;
        this.sDimensions = new ArrayList<>();
    }
    
    public ArrayType(List<String> ds, LustreType type) {
        this.type = type;
        this.sDimensions = ds;
        this.dimensions = new ArrayList<>();
    }    

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public int compareTo(LustreType o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
