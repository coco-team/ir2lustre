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
public class LustreProgram extends LustreAst {
    public List<LustreNode> nodes;
    public List<LustreContract> contracts;
    public List<LustreEnumType> typesDef;
    
    public LustreProgram() {        
        this.nodes      = new ArrayList<>();
        this.contracts  = new ArrayList<>();
        this.typesDef   = new ArrayList<>();
    }
    
    public LustreProgram(List<LustreNode> nodes) {
        this.nodes      = nodes;  
        this.contracts  = new ArrayList<>();
        this.typesDef   = new ArrayList<>();        
    } 

    public LustreProgram(List<LustreNode> nodes, List<LustreEnumType> enums) {
        this.nodes      = nodes;
        this.typesDef   = enums;
        this.contracts  = new ArrayList<>();        
    }     
    
    public LustreProgram(List<LustreNode> nodes, List<LustreEnumType> enums, List<LustreContract> contracts) {
        this.nodes      = nodes;
        this.typesDef   = enums;
        this.contracts  = contracts;        
    }    
    
    public void addNode(LustreNode node) {
        this.nodes.add(node);
    }
    
    public void addContract(LustreContract contract) {
        this.contracts.add(contract);
    }    
    
    public void addNodes(List<LustreNode> nodes) {
        this.nodes.addAll(nodes);
    }  
    
    public void addContracts(List<LustreContract> contracts) {
        this.contracts.addAll(contracts);
    }      
    
    public void addEnumDef(LustreEnumType enumDef) {
        this.typesDef.add(enumDef);
    }
    
    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    
}
