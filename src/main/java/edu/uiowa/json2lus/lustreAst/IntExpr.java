/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus.lustreAst;

import java.math.BigInteger;

/**
 *
 * @author Paul Meng
 */
public class IntExpr extends LustreExpr {
    public final BigInteger value;
    
    public IntExpr(BigInteger value) {
        this.value = value;
    }

    @Override
    public void accept(LustreAstVisitor visitor) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
