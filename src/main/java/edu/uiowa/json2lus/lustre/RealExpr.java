/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus.lustre;

import java.math.BigDecimal;

/**
 *
 * @author Paul Meng
 */
public class RealExpr extends LustreExpr {
    public final BigDecimal value;
    
    public RealExpr(BigDecimal value) {
        this.value = value;
    }
}
