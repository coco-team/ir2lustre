/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
 */

package edu.uiowa.json2lus.lustreAst;

import edu.uiowa.json2lus.J2LTranslator;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Paul Meng
 */
public class BooleanExpr extends LustreExpr {
    
    public final boolean value;

    /** Logger */
    private static final Logger LOGGER = Logger.getLogger(BooleanExpr.class.getName());

    public BooleanExpr(boolean value) {
        this.value = value;
    }
    
    public BooleanExpr(String value)
    {
        if(value.contains("."))
        {
            String rhs = value.substring(value.indexOf(".")+1).trim();
            
            if(Integer.parseInt(rhs) == 0) {
                this.value = Integer.parseInt(value.substring(0, value.indexOf("."))) != 0;
            } else {
                this.value = true;
            }            
        }
        else
        {
            // value can be "true", "false", "1", "0"
            //this.value = value.toLowerCase().equals("true") || Integer.parseInt(value.toLowerCase()) != 0;
            switch (value)
            {
                case "true" : this.value = true; break;
                case "false": this.value = false; break;
                case "1"    : this.value = true; break;
                case "0"    : this.value = false; break;
                default     :
                {
                    try
                    {
                        this.value = Integer.parseInt(value.toLowerCase()) != 0;
                    }
                    catch (NumberFormatException exception)
                    {
                        LOGGER.log(Level.SEVERE, exception.getMessage());
                        throw exception;
                    }
                }
            }
        }
    }    

    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    
    @Override
    public String toString() {
        return String.valueOf(this.value);
    }       
}
