/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus.lustreAst;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Paul Meng
 */
public class LustreProgram extends LustreAst {
    public List<LustreNode> nodes;
    public List<LustreContract> contracts;
    public List<LustreEnumType> typesDef;
    public Map<String, Set<String>> nodeContractsMap;
    
    public LustreProgram() {        
        this.nodes              = new ArrayList<>();
        this.contracts          = new ArrayList<>();
        this.typesDef           = new ArrayList<>();
        this.nodeContractsMap   = new HashMap<>();
    }
    
    public LustreProgram(List<LustreNode> nodes) {
        this.nodes              = nodes;  
        this.contracts          = new ArrayList<>();
        this.typesDef           = new ArrayList<>();   
        this.nodeContractsMap   = new HashMap<>();
    } 

    public LustreProgram(List<LustreNode> nodes, List<LustreEnumType> enums) {
        this.nodes              = nodes;
        this.typesDef           = enums;
        this.contracts          = new ArrayList<>();   
        this.nodeContractsMap   = new HashMap<>();
    }     
    
    public LustreProgram(List<LustreNode> nodes, List<LustreEnumType> enums, List<LustreContract> contracts) {
        this.nodes              = nodes;
        this.typesDef           = enums;
        this.contracts          = contracts;        
        this.nodeContractsMap   = new HashMap<>();
    }    
    
    public void attachNodeContract(String node, String contract) {
        if(this.nodeContractsMap.containsKey(node)) {
            this.nodeContractsMap.get(node).add(contract);
        } else {
            Set<String> set = new HashSet<>();
            set.add(contract);
            this.nodeContractsMap.put(node, set);
        }
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
