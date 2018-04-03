/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
 */

package edu.uiowa.json2lus.lustreAst;

/**
 *
 * @author Paul Meng
 */
public class PrimitiveType extends LustreType {
    public final String name;
    public boolean isConst = false;
    public static final PrimitiveType INT           = new PrimitiveType("int");    
    public static final PrimitiveType REAL          = new PrimitiveType("real");
    public static final PrimitiveType BOOL          = new PrimitiveType("bool");
    public static final PrimitiveType INTCONST      = new PrimitiveType("int", true);    
    public static final PrimitiveType REALCONST     = new PrimitiveType("real", true);    
    public static final PrimitiveType BOOLCONST     = new PrimitiveType("bool", true);    
    
    private PrimitiveType(String name) {
        this.name = name;
    }       
    
    private PrimitiveType(String name, boolean isConst) {
        this.name = name;
        this.isConst = isConst;
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

    @Override
    public int compareTo(LustreType targetType) {
        if((targetType instanceof PrimitiveType)) {
            return 0;
        }
        
        int    result   = -1;
        String thisName = this.name;
        String oName    = ((PrimitiveType)targetType).name;
        
        switch(thisName) {
            case "real" : {
                if(oName.equals("real")) {
                    result = 0;
                } else if(oName.equals("int")) {
                    result = 1;
                } else {
                    result = -1;
                }
                break;
            }
            case "int" : {
                if(oName.equals("real")) {
                    result = -1;
                } else if(oName.equals("int")) {
                    result = 0;
                } else {
                    result = 1;
                }
                break;
            }
            case "bool" : {
                if(oName.equals("real")) {
                    result = -1;
                } else if(oName.equals("int")) {
                    result = -1;
                } else {
                    result = 0;
                }
                break;
            }            
        }
        return result;
    }

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
}
