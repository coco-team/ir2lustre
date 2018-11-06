/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
 */

package edu.uiowa.json2lus.lustreAst;

import java.util.ArrayList;
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
    public List<LustreNode>         nodes;
    public List<LustreContract>     contracts;
    public List<LustreEnumType>     enumTypes;
    public Map<String, Set<String>> nodeContractsMap;
    
    public LustreProgram() {        
        this.nodes              = new ArrayList<>();
        this.contracts          = new ArrayList<>();
        this.enumTypes          = new ArrayList<>();
        this.nodeContractsMap   = new HashMap<>();
    }
    
    public LustreProgram(List<LustreNode> nodes) {
        this.nodes              = nodes;  
        this.contracts          = new ArrayList<>();
        this.enumTypes          = new ArrayList<>();
        this.nodeContractsMap   = new HashMap<>();
    } 

    public LustreProgram(List<LustreNode> nodes, List<LustreEnumType> enums) {
        this.nodes              = nodes;
        this.enumTypes          = enums;
        this.contracts          = new ArrayList<>();   
        this.nodeContractsMap   = new HashMap<>();
    }     
    
    public LustreProgram(List<LustreNode> nodes, List<LustreEnumType> enums, List<LustreContract> contracts) {
        this.nodes              = nodes;
        this.enumTypes          = enums;
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
        this.enumTypes.add(enumDef);
    }
    
    @Override
    public void accept(LustreAstVisitor visitor) {
        visitor.visit(this);
    }
    
}
