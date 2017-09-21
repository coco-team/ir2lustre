/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus.lustre;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Paul Meng
 */
public class NodeCallExpr extends LustreExpr {
    public final String             nodeName;
    public final List<LustreExpr>   parameters;
    
    public NodeCallExpr(String nodeName, List<LustreExpr> args) {
        this.nodeName   = nodeName;
        this.parameters = args;
    }
    
    public NodeCallExpr(String nodeName, LustreExpr ... args) {
        this.nodeName   = nodeName;
        this.parameters = Arrays.asList(args);
    }    
    
}
