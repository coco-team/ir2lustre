/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus.lustre;

/**
 *
 * @author Paul Meng
 */
public class IteExpr extends LustreExpr {
	public final LustreExpr ifExpr;
	public final LustreExpr thenExpr;
	public final LustreExpr elseExpr;
        
        public IteExpr(LustreExpr ifExpr, LustreExpr thenExpr, LustreExpr elseExpr) {
            this.ifExpr     = ifExpr;
            this.thenExpr   = thenExpr;
            this.elseExpr   = elseExpr;
        }
}
