/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus.lustreAst;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Paul Meng
 */
public class LustreEnumType extends LustreType{
    public final String name;
    public final List<String> values;
    
    public LustreEnumType(String name, List<String> values) {
        this.name   = name;
        this.values = values;
    }   
    
    public LustreEnumType(String name, String ... values) {
        this.name   = name;
        this.values = Arrays.asList(values);
    }
    
    @Override
    public int compareTo(LustreType o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
