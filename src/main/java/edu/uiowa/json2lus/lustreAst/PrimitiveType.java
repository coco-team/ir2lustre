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
public class PrimitiveType extends LustreType {
    public final String name;
    public static final PrimitiveType INT   = new PrimitiveType("int");
    public static final PrimitiveType REAL  = new PrimitiveType("real");
    public static final PrimitiveType BOOL  = new PrimitiveType("bool");
    
    private PrimitiveType(String name) {
        this.name = name;
    }       
    
    @Override
    public String toString() {
        return name;
    }   

    @Override
    public int hashCode() {
        int         result  = 1;
        final int   prime   = 31;
        
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof PrimitiveType)) {
            return false;
        }
        
        PrimitiveType other = (PrimitiveType) obj;
        
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }    
}