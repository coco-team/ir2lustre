/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.uiowa.json2lus.ExprParser.AstNode;
import edu.uiowa.json2lus.lustreAst.BinaryExpr;
import edu.uiowa.json2lus.lustreAst.BooleanExpr;
import edu.uiowa.json2lus.lustreAst.LustreContract;
import edu.uiowa.json2lus.lustreAst.IntExpr;
import edu.uiowa.json2lus.lustreAst.IteExpr;
import edu.uiowa.json2lus.lustreAst.LustreAst;
import edu.uiowa.json2lus.lustreAst.LustreEnumType;
import edu.uiowa.json2lus.lustreAst.LustreEq;
import edu.uiowa.json2lus.lustreAst.LustreExpr;
import edu.uiowa.json2lus.lustreAst.LustreNode;
import edu.uiowa.json2lus.lustreAst.LustreProgram;
import edu.uiowa.json2lus.lustreAst.LustreType;
import edu.uiowa.json2lus.lustreAst.LustreVar;
import edu.uiowa.json2lus.lustreAst.MergeExpr;
import edu.uiowa.json2lus.lustreAst.NodeCallExpr;
import edu.uiowa.json2lus.lustreAst.PrimitiveType;
import edu.uiowa.json2lus.lustreAst.RealExpr;
import edu.uiowa.json2lus.lustreAst.TupleExpr;
import edu.uiowa.json2lus.lustreAst.UnaryExpr;
import edu.uiowa.json2lus.lustreAst.VarIdExpr;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.parboiled.Parboiled;
import static org.parboiled.errors.ErrorUtils.printParseErrors;
import org.parboiled.parserunners.RecoveringParseRunner;
import org.parboiled.support.ParsingResult;

/**
 *
 * @author Paul Meng
 */
public class J2LTranslator {    
    /** The top-level node name */
    public  String topNodeName;    
    
    /** The path of the input JSON file */
    public  final String inputPath;
    
    public  boolean multProps;
    
    /** Logger */
    private static final Logger LOGGER = Logger.getLogger(J2LTranslator.class.getName());
    
    /** Counter for naming */
    private static int COUNT = 0;
    
    /** Naming for conditional expressions */
    private final String CONDENUM           = "Cond_Enum_";
    private final String CONDEXPR           = "Cond_Expr_";
    private final String CONDEXPRVAR        = "Cond_Expr_Var_";
    private final String CONDEXPRENUMTYPE   = "Cond_Expr_Enum_Type_";
    
    /** JSON fields */
    
    /** Relational operators */
    private final String GTE            = ">=";
    private final String LTE            = "<=";
    private final String GT             = ">";
    private final String LT             = "<";
    private final String EQ             = "==";
    private final String NEQ            = "~=";     
    
    /** Logical operators */
    private final String AND            = "AND";
    private final String NAND           = "NAND";    
    private final String OR             = "OR";
    private final String NOR            = "NOR";
    private final String XOR            = "XOR";
    private final String NXOR           = "NXOR";
    private final String NOT            = "NOT";
    
    /** Math operators */
    private final String SUM            = "Sum";
    private final String MOD            = "mod";    
    private final String ABS            = "Abs";
    private final String SQRT           = "Sqrt";
    private final String MATH           = "Math";
    private final String UNARYMINUS     = "UnaryMinus";
    
    private final String IF             = "If";    
    private final String SWITCH         = "Switch";
    private final String CRITERIA       = "Criteria";
    private final String THRESHOLD      = "Threshold";
    private final String TRDCRIT        = "u2 ~= 0";
    private final String FSTCRIT        = "u2 >= Threshold"; 
    private final String SNDCRIT        = "u2 > Threshold"; 
    private final String UNITDELAY      = "UnitDelay";    

    private final String X0             = "X0";
    private final String MAX            = "max";
    private final String MIN            = "min";
    private final String GAIN           = "Gain";
    private final String FIRST          = "First";            
    private final String MERGE          = "Merge";
    private final String INPUTS         = "Inputs";    
    private final String MINMAX         = "MinMax";    
    private final String MEMORY         = "Memory";
    
    private final String ARROW          = "ArrowOperator";
    private final String PRODUCT        = "Product";
    private final String CONSTANT       = "Constant";        
    private final String FUNCTION       = "Function";    
    private final String INITCOND       = "InitialCondition"; 
    private final String IFEXPRESSION   = "IfExpression";
    private final String ELSEIFEXPRS    = "ElseIfExpressions";
            
    /** Blocks information */
    private final String META           = "meta";
    private final String NAME           = "Name";
    private final String PORT           = "Port";
    private final String TYPE           = "Type";
    private final String PATH           = "Path";
    private final String VALUE          = "Value";
    private final String LOGIC          = "logic";    
    private final String HANDLE         = "Handle";  
    private final String CONTRACT       = "ContractBlock";        
    private final String MSFUNCTION     = "M-S-Function";      
    private final String MODE           = "ContractModeBlock";    
    private final String REQUIRE        = "ContractRequireBlock";    
    private final String ENSURE         = "ContractEnsureBlock";
    private final String ASSUME         = "ContractAssumeBlock";        
    private final String GUARANTEE      = "ContractGuaranteeBlock";    
    private final String VALIDATOR      = "ContractValidatorBlock";    
             
    
    /** Cocosim block type to indicate property block*/    
    private final String ENSURES        = "ensures";
    
    private final String HELD               = "held";
    private final String RESET              = "reset";
    private final String INPORT             = "Inport";    
    private final String OUTPORT            = "Outport";    
    private final String CONTENT            = "Content";    
    private final String OPERATOR           = "Operator";
    private final String MASKTYPE           = "MaskType";
    private final String SRCBLOCK           = "SrcBlock";
    private final String DSTBLOCK           = "DstBlock";
    private final String BLOCKTYPE          = "BlockType";
    private final String SUBSYSTEM          = "SubSystem"; 
    private final String ACTIONPORT         = "ActionPort";                
    private final String TERMINATOR         = "Terminator";
    private final String INITSTATES         = "InitializeStates";        
    private final String LINEHANDLES        = "LineHandles";     
    private final String IFACTIONBLK        = "IfActionBlock";        
    private final String CONNECTIVITY       = "PortConnectivity";
    private final String RELATIONALOP       = "relationaloperator";
    private final String PORTDATATYPE       = "CompiledPortDataTypes";    
    private final String COMPARETOZERO      = "Compare To Zero";
    private final String ANNOTATIONTYPE     = "AnnotationType";
    private final String CONTRACTBLKTYPE    = "ContractBlockType";            
    private final String COMPARETOCONSTANT  = "Compare To Constant";
    private final String LUSTREOPERATORBLK  = "LustreOperatorBlock";    
    
    
    /** Lustre node names for type conversions */
    private final String BOOLTOINT      = "bool_to_int";
    private final String INTTOBOOL      = "int_to_bool";    
    private final String BOOLTOREAL     = "bool_to_real";
    private final String REALTOBOOL     = "real_to_bool";    
    private final String INTTOREAL      = "int_to_real";
                      
    private JsonNode                                topLevelNode;  
    private final List<LustreEq>                    auxNodeEqs;
    private final List<LustreVar>                   auxNodeLocalVars;    
    private final LustreProgram                     lustreProgram;
    private final List<JsonNode>                    subsystemNodes;
    private final Set<JsonNode>                     contractNodes;    
    private final Map<String, String>               libNodeNameMap;    
    private final Map<String, LustreExpr>           auxHdlToPreExprMap;
    private final Map<JsonNode, List<JsonNode>>     subsystemPropsMap;
    private final Map<String, LustreExpr>           hdlToCondExprMap;
    
    
    
            
    /**
     * Constructor
     * @param inputPath
     * @param multProps
     */
    public J2LTranslator(String inputPath, boolean multProps) {  
        this.multProps          = multProps;
        this.inputPath          = inputPath;    
        this.contractNodes      = new HashSet<>();
        this.libNodeNameMap     = new HashMap<>();
        this.subsystemNodes     = new ArrayList<>();
        this.subsystemPropsMap  = new HashMap<>();
        this.auxHdlToPreExprMap = new HashMap<>();
        this.hdlToCondExprMap   = new HashMap<>();
        this.auxNodeEqs         = new ArrayList<>();
        this.auxNodeLocalVars   = new ArrayList<>();        
        this.lustreProgram      = new LustreProgram();
        this.topNodeName          = inputPath.toLowerCase().endsWith(".json") ? 
                                    inputPath.substring(inputPath.lastIndexOf(File.separator)+1, inputPath.lastIndexOf("."))
                                    : inputPath.substring(inputPath.lastIndexOf(File.separator)+1);                            
    }
    
    /**
     * Constructor
     * @param inputPath
     */
    public J2LTranslator(String inputPath) {  
        this.multProps          = false;
        this.inputPath          = inputPath;  
        this.contractNodes      = new HashSet<>();        
        this.libNodeNameMap     = new HashMap<>();
        this.subsystemNodes     = new ArrayList<>();
        this.subsystemPropsMap  = new HashMap<>();
        this.auxHdlToPreExprMap = new HashMap<>();
        this.hdlToCondExprMap   = new HashMap<>();
        this.auxNodeEqs         = new ArrayList<>();
        this.auxNodeLocalVars   = new ArrayList<>();        
        this.lustreProgram      = new LustreProgram();
        this.topNodeName          = inputPath.toLowerCase().endsWith(".json") ? 
                                    inputPath.substring(inputPath.lastIndexOf(File.separator)+1, inputPath.lastIndexOf("."))
                                    : inputPath.substring(inputPath.lastIndexOf(File.separator)+1);                            
    }    
    
    /**
     * Execute the translation process 
     * 
     * @return lustreProgram
     */    
    public LustreProgram execute() {
        J2LTranslator.this.collectSubsytemBlocks();
        // Ensure a bottom-up translation to avoid forward reference issue
        for(int i = this.subsystemNodes.size()-1; i >= 0; i--) {
            for(LustreAst ast : translateSubsystemNode(this.subsystemNodes.get(i))) {
                if(ast instanceof LustreNode) {
                    this.lustreProgram.addNode((LustreNode)ast);
                } else if(ast instanceof LustreContract) {
                    this.lustreProgram.addContract((LustreContract)ast);
                }
            }              
        }
        return this.lustreProgram;
    }    
    
    /**
     * Collect subsystem blocks information
     */
    protected void collectSubsytemBlocks() {
        JsonNode        rootNode    = null;        
        ObjectMapper    mapper      = new ObjectMapper();
        
        try {
            rootNode = mapper.readTree(new File(this.inputPath));
        } catch (IOException ex) {
            Logger.getLogger(J2LTranslator.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(rootNode != null) {
            Iterator<Entry<String, JsonNode>> nodes = rootNode.fields(); 
            
            while(nodes.hasNext()) {
                Map.Entry<String, JsonNode> field = nodes.next(); 
                
                if(!field.getKey().equals(META)) {
                    this.topLevelNode   = field.getValue();
                    this.topNodeName    = field.getKey();
                    collectSubsytemBlocks(this.topLevelNode);               
                }              
            }
            if(this.topLevelNode == null) {
                LOGGER.log(Level.SEVERE, "Unexpected: no top level node is found in the JSON file!");                
            }
        } else {
            LOGGER.log(Level.SEVERE, "Unexpected: unable to parse the input JSON file!");
        }   
    }

    /**
     * Collect subsystem blocks
     * @param subsystemNode
     */    
    protected void collectSubsytemBlocks(JsonNode subsystemNode) {
        if(subsystemNode != null && !this.subsystemNodes.contains(subsystemNode) && subsystemNode.has(CONTENT)) {
            this.subsystemNodes.add(subsystemNode);
            
            LOGGER.log(Level.INFO, "Found a subsystem block: {0}  ", sanitizeName(subsystemNode.equals(this.topLevelNode) ? this.topNodeName:subsystemNode.get(NAME).asText()));
            Iterator<Entry<String, JsonNode>> nodes = subsystemNode.get(CONTENT).fields();                         
            
            while(nodes.hasNext()) {
                JsonNode fieldNode = nodes.next().getValue();
                
                if(fieldNode.has(BLOCKTYPE)) {
                    if(fieldNode.get(BLOCKTYPE).asText().equals(SUBSYSTEM)) {                        
                        collectSubsytemBlocks(fieldNode);
                    }          
                }               
            }
        } else {
            LOGGER.log(Level.SEVERE, "Cannot find the Cocosim model: {0} content definition in the input JSON file!", this.topNodeName);
        }       
    }    
    
    /**
     * 
     * @param contractNode
     * @param validatorBlkNode
     * @param inputs
     * @param outputs
     * @param blkNodeToSrcBlkHdlsMap
     * @param blkNodeToDstBlkHdlsMap
     * @param hdlToBlkNodeMap
     * @return A list of contracts corresponding to the contract node
     */    
    protected List<LustreAst> translateContractNode(JsonNode contractNode, JsonNode validatorBlkNode, List<LustreVar> inputs, List<LustreVar> outputs, Map<JsonNode, List<String>> blkNodeToSrcBlkHdlsMap, Map<JsonNode, List<String>> blkNodeToDstBlkHdlsMap, Map<String, JsonNode> hdlToBlkNodeMap) {
        String contractName = getQualifiedBlkName(contractNode); 
        LOGGER.log(Level.INFO, "Start translating the contract block: {0}", contractName);
        
        List<LustreAst>                 contracts       = new ArrayList<>();        
        List<LustreExpr>                assumptions     = new ArrayList<>();            
        List<LustreExpr>                guarantees      = new ArrayList<>();        
        Map<String, List<LustreExpr>>   modeToRequires  = new HashMap<>();
        Map<String, List<LustreExpr>>   modeToEnsures   = new HashMap<>();        
        
        for(String inHdl : blkNodeToSrcBlkHdlsMap.get(validatorBlkNode)) {
            JsonNode inBlk = hdlToBlkNodeMap.get(inHdl);
            
            if(isAssumeBlk(inBlk)) {
                assumptions.add(translateBlock(false, inHdl, contractNode, blkNodeToSrcBlkHdlsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap, new HashSet<String>()));
            } else if(isGuaranteeBlk(inBlk)) {
                guarantees.add(translateBlock(false, inHdl, contractNode, blkNodeToSrcBlkHdlsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap, new HashSet<String>()));
            } else if(isModeBlk(inBlk)) {
                String modeName             = getBlkName(inBlk);
                List<LustreExpr> requires   = new ArrayList<>();
                List<LustreExpr> ensures    = new ArrayList<>();
                
                for(String modeInHdl : blkNodeToSrcBlkHdlsMap.get(inBlk)) {
                    JsonNode    modeInBlk       = hdlToBlkNodeMap.get(modeInHdl);
                    LustreExpr  modeInBlkExpr   = translateBlock(false, modeInHdl, contractNode, blkNodeToSrcBlkHdlsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap, new HashSet<String>());
                    
                    if(isRequireBlk(modeInBlk)) {
                        requires.add(modeInBlkExpr);
                    } else if(isEnsureBlk(modeInBlk)) {
                        ensures.add(modeInBlkExpr);
                    }
                }
                if(!requires.isEmpty()) {
                    modeToRequires.put(modeName, requires);
                }
                if(!ensures.isEmpty()) {
                    modeToEnsures.put(modeName, ensures);
                }                
            }
        }
        
        List<LustreVar> newInputs   = new ArrayList<>();
        List<String>    inHdls      = blkNodeToSrcBlkHdlsMap.get(contractNode);        
        
        if(inHdls != null && !inHdls.isEmpty()) {
            outputs.clear();
            for(int i = 0; i < inHdls.size(); i++) {                
                if(isUserDefinedSubsystemBlk(inHdls.get(i))) {
                    outputs.add(inputs.get(i));
                } else {
                    newInputs.add(inputs.get(i));
                }
            }
        }
        
        contracts.add(new LustreContract(contractName, assumptions, guarantees, newInputs, outputs, modeToRequires, modeToEnsures));
        return contracts;
    }   
   
    /**
     * 
     * @param subsystemNode
     * @return The Lustre node or contract corresponding to the input subsystem JSON node
     */
    protected List<LustreAst> translateSubsystemNode(JsonNode subsystemNode) {
        if(isModeBlk(subsystemNode)) {
            return new ArrayList<>();
        }
        
        String lusNodeName = subsystemNode.equals(this.topLevelNode) ? this.topNodeName : getQualifiedBlkName(subsystemNode); 
        
        LOGGER.log(Level.INFO, "Start translating the subsystem block: {0}", lusNodeName);        
        
        List<LustreEq>          props           = new ArrayList<>();
        List<LustreVar>         inputs          = new ArrayList<>();        
        List<LustreVar>         locals          = new ArrayList<>();
        List<LustreVar>         outputs         = new ArrayList<>();
        List<LustreEq>          equations       = new ArrayList<>();
        Map<Integer, JsonNode>  inports         = new HashMap<>();
        Map<Integer, JsonNode>  outports        = new HashMap<>();
        
        JsonNode                            validatorBlk                = null;
        List<JsonNode>                      contractNodes               = new ArrayList<>();  
        /** Terminator node in the input subsystem block */
        List<JsonNode>                      propsNodes                  = new ArrayList<>();   
        /** Outport nodes in the input subsystem block */
        List<JsonNode>                      outportNodes                = new ArrayList<>(); 
        
        /** A mapping between a handle and the block node */
        Map<String, JsonNode>               handleToBlkNodeMap          = new HashMap<>();                
        /** A mapping between a block node and its src block handles */
        Map<JsonNode, List<String>>         blkNodeToSrcBlkHandlesMap   = new HashMap<>();  
        /** A mapping between a block node and its dst block handles */
        Map<JsonNode, List<String>>         blkNodeToDstBlkHandlesMap   = new HashMap<>();         
        /** ALL the fields under content field of subsystem block */
        Iterator<Entry<String, JsonNode>>   contentFields               = subsystemNode.get(CONTENT).fields();
               
        while(contentFields.hasNext()) {
            Map.Entry<String, JsonNode> contField       = contentFields.next();   
            JsonNode                    contBlkNode     = contField.getValue();
            
            if(contBlkNode.has(BLOCKTYPE)) {
                String blkType = contBlkNode.get(BLOCKTYPE).asText();
                
                if(contBlkNode.has(HANDLE)) {
                    handleToBlkNodeMap.put(contBlkNode.get(HANDLE).asText(), contBlkNode);
                } else {
                    LOGGER.log(Level.SEVERE, "Unexpected: A block does not have a handle!");
                }
                if(blkType.equals(INPORT)) {
                    inports.put(getBlkPortPosition(contBlkNode), contBlkNode);
                } else if(blkType.equals(OUTPORT)) {
                    outports.put(getBlkPortPosition(contBlkNode), contBlkNode);
                } else if(isPropertyBlk(contBlkNode)) {
                    propsNodes.add(contBlkNode);
                } else if(isContractBlk(contBlkNode)) {
                    contractNodes.add(contBlkNode);
                } else if(isValidatorBlk(contBlkNode)) {
                    validatorBlk = contBlkNode;
                } 
                // Top-level subsystem block does not have port connectivity field
                populateConnectivities(contBlkNode, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap);                
            }            
        }
        populateConnectivities(subsystemNode, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap);
        
        // Ensure the positions of input and output are correct
        for(int i = 0; i < inports.size(); i++) {
            if(inports.containsKey(i)) {
                JsonNode inportBlk = inports.get(i);
                inputs.add(new LustreVar(getQualifiedBlkName(inportBlk), getBlkOutportType(inportBlk)));                 
            }           
        }
        for(int i = 0; i < outports.size(); i++) {
            JsonNode outportBlk = outports.get(i);
            outputs.add(new LustreVar(getQualifiedBlkName(outportBlk), getLustreTypeFromStrRep(outportBlk.get(PORTDATATYPE).get(INPORT).asText())));            
        }   
        if(isContractBlk(subsystemNode)) {            
            return translateContractNode(subsystemNode, validatorBlk, inputs, outputs, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap);
        } else {
            attachContractsToNode(contractNodes, blkNodeToSrcBlkHandlesMap);
            equations.addAll(translateOutportEquations(new ArrayList<>(outports.values()), subsystemNode, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap));                    
            equations.addAll(this.auxNodeEqs);
            locals.addAll(this.auxNodeLocalVars);
            this.auxNodeEqs.clear();
            this.auxNodeLocalVars.clear();
            this.auxHdlToPreExprMap.clear();

            return processProperties(propsNodes, subsystemNode, lusNodeName, inputs, outputs, locals, equations, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap);            
        }                
    }
    
    /**
     * Connect nodes with their contracts based on the inports of the contract
     * @param contractNodes
     * @param blkNodeToSrcBlkHandlesMap
     */
    protected void attachContractsToNode(List<JsonNode> contractNodes, Map<JsonNode, List<String>> blkNodeToSrcBlkHandlesMap) {
        if(contractNodes != null) {
            for(JsonNode contractNode : contractNodes) {
                List<String> srcHdls = blkNodeToSrcBlkHandlesMap.get(contractNode);
                
                if(srcHdls != null) {
                    for(String srcHdl : srcHdls) {
                        for(JsonNode subsystemNode : this.subsystemNodes) {
                            if(subsystemNode.has(HANDLE)) {
                                if(subsystemNode.get(HANDLE).asText().equals(srcHdl)) {
                                    JsonNode srcNode = null;
                                    
                                    if(subsystemNode.has(MASKTYPE)) {
                                        String maskType = subsystemNode.get(MASKTYPE).asText();
                                        if(!maskType.equals(COMPARETOZERO) && !maskType.equals(COMPARETOCONSTANT)) {
                                            srcNode = subsystemNode;
                                        }
                                    } else {
                                        srcNode = subsystemNode;    
                                    } 
                                    if(srcNode != null) {
                                        this.lustreProgram.attachNodeContract(getQualifiedBlkName(srcNode), getQualifiedBlkName(contractNode));
                                    }
                                }
                            }
                        }                        
                    }
                } else {
                    LOGGER.log(Level.SEVERE, "Found a ghost contract: {0}", getQualifiedBlkName(contractNode));
                }
            }
        }
    }   
    
    /**
     * Populate the two maps with the source and destination handles of the input node
     * @param node
     * @param blkNodeToSrcBlkHandlesMap
     * @param blkNodeToDstBlkHandlesMap
     */
    protected void populateConnectivities(JsonNode node, Map<JsonNode, List<String>> blkNodeToSrcBlkHandlesMap, Map<JsonNode, List<String>> blkNodeToDstBlkHandlesMap) {
        if(node.has(CONNECTIVITY)) {
            JsonNode        portConns       = node.get(CONNECTIVITY);
            List<String>    srcBlockHanldes = new ArrayList<>();
            List<String>    dstBlockHanldes = new ArrayList<>();

            if(portConns.isArray()) {
                Iterator<JsonNode> portConnIt = portConns.elements();

                while(portConnIt.hasNext()) {
                    JsonNode connNode = portConnIt.next();
                    srcBlockHanldes.addAll(convertJsonValuesToList(connNode.get(SRCBLOCK)));
                    dstBlockHanldes.addAll(convertJsonValuesToList(connNode.get(DSTBLOCK)));
                }
            } else {
                srcBlockHanldes.addAll(convertJsonValuesToList(portConns.get(SRCBLOCK)));
                dstBlockHanldes.addAll(convertJsonValuesToList(portConns.get(DSTBLOCK)));            
            }
            if(!srcBlockHanldes.isEmpty()) {
                blkNodeToSrcBlkHandlesMap.put(node, srcBlockHanldes);
            }
            if(!dstBlockHanldes.isEmpty()) {
                blkNodeToDstBlkHandlesMap.put(node, dstBlockHanldes);    
            }             
        }        
    }
    
    /**
     * @param outportNodes
     * @param subsystemNode
     * @param blkNodeToSrcBlkHandlesMap
     * @param blkNodeToDstBlkHandlesMap
     * @param handleToBlkNodeMap
     * @return A list of lustre equations corresponding to the outport nodes
     */    
    protected List<LustreEq> translateOutportEquations(List<JsonNode> outportNodes, JsonNode subsystemNode, Map<JsonNode, List<String>> blkNodeToSrcBlkHandlesMap, Map<JsonNode, List<String>> blkNodeToDstBlkHandlesMap, Map<String, JsonNode> handleToBlkNodeMap) {
        List<LustreEq>          eqs                 = new ArrayList<>();
        Set<JsonNode>           hasGroupped         = new HashSet<>();
        List<List<JsonNode>>    grouppedOutports    = new ArrayList<>();        
        
        for(int i = 0; i < outportNodes.size(); i++) {
            JsonNode iPort = outportNodes.get(i);

            if(!hasGroupped.contains(iPort)) {                
                List<JsonNode>  group = new ArrayList<>();

                group.add(iPort);
                hasGroupped.add(iPort);
                for(int j = i+1; j < outportNodes.size(); j++) {
                    JsonNode jPort = outportNodes.get(j);

                    if(!hasGroupped.contains(jPort)) {
                        if(blkNodeToSrcBlkHandlesMap.containsKey(iPort) && blkNodeToSrcBlkHandlesMap.containsKey(jPort)) {
                            if(blkNodeToSrcBlkHandlesMap.get(iPort).equals(blkNodeToSrcBlkHandlesMap.get(jPort))) {
                                group.add(jPort);
                                hasGroupped.add(jPort);
                            }                            
                        }
                    }
                }  
                grouppedOutports.add(group);
            }
        }       
        for(List<JsonNode> gOutports : grouppedOutports) {
            LustreEq eq = translateOutportEquation(gOutports, subsystemNode, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap);                                                
            if(eq != null) {
                eqs.add(eq);
            }
        }
        
        return eqs;
    }
    
    /**
     * 
     * @param propNodes
     * @param subsystemNode
     * @param lusNodeName
     * @param inputs
     * @param outputs
     * @param locals
     * @param equations
     * @param blkNodeToSrcBlkHandlesMap
     * @param blkNodeToDstBlkHandlesMap
     * @param handleToBlkNodeMap
     * @return a list of Lustre nodes based on properties (one property per node or multiple properties per node)
     */
    protected List<LustreAst> processProperties(List<JsonNode> propNodes, JsonNode subsystemNode, String lusNodeName, List<LustreVar> inputs, List<LustreVar> outputs, List<LustreVar> locals, List<LustreEq> equations, Map<JsonNode, List<String>> blkNodeToSrcBlkHandlesMap, Map<JsonNode, List<String>> blkNodeToDstBlkHandlesMap, Map<String, JsonNode> handleToBlkNodeMap) {
        List<LustreAst> lusNodes = new ArrayList<>();  
        
        if(propNodes.size() > 1 && !this.multProps) {
            for(JsonNode propNode: propNodes) {
                String          newName     = lusNodeName;
                List<LustreEq>  props       = new ArrayList<>();
                List<LustreVar> cInputs     = new ArrayList<>();        
                List<LustreVar> cLocals     = new ArrayList<>();
                List<LustreVar> cOutputs    = new ArrayList<>();
                List<LustreEq>  cEquations  = new ArrayList<>();

                for(LustreVar in : inputs) {
                    cInputs.add(in);
                }
                for(LustreVar out : outputs) {
                    cOutputs.add(out);
                }            
                for(LustreVar local : locals) {
                    cLocals.add(local);
                }
                for(LustreEq eq : equations) {
                    cEquations.add(eq);
                }
                LustreEq propEq = translatePropEquation(propNode, subsystemNode, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap);
                for(VarIdExpr var : propEq.getLhs()) {
                    cLocals.add(new LustreVar(var.id, PrimitiveType.BOOL));
                }
                if(propEq.getRhs() instanceof NodeCallExpr) {
                    newName += "_"+((NodeCallExpr)propEq.getRhs()).nodeName;
                }
                props.add(propEq);            
                lusNodes.add(new LustreNode(subsystemNode.equals(this.topLevelNode), newName, cInputs, cOutputs, cLocals, cEquations, props));            
            }               
        } else {
            String          newName = lusNodeName;
            List<LustreEq>  props   = new ArrayList<>();            
            
            for(JsonNode propNode : propNodes) {
                LustreEq propEq = translatePropEquation(propNode, subsystemNode, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap);
                for(VarIdExpr var : propEq.getLhs()) {
                    locals.add(new LustreVar(var.id, PrimitiveType.BOOL));
                }
                if(propEq.getRhs() instanceof NodeCallExpr) {
                    newName += "_"+((NodeCallExpr)propEq.getRhs()).nodeName;
                }                
                props.add(propEq);            
            }   
            lusNodes.add(new LustreNode(subsystemNode.equals(this.topLevelNode), newName, inputs, outputs, locals, equations, props));            
        } 
        return lusNodes;
    }
    
    /**
     * 
     * @param propBlk is a subsystem block
     * @param subsystemNode
     * @param blkNodeToSrcBlkHandlesMap
     * @param blkNodeToDstBlkHandlesMap
     * @param handleToBlkNodeMap
     * @return A property equation
     */
    protected LustreEq translatePropEquation(JsonNode propBlk, JsonNode subsystemNode, Map<JsonNode, List<String>> blkNodeToSrcBlkHandlesMap, Map<JsonNode, List<String>> blkNodeToDstBlkHandlesMap, Map<String, JsonNode> handleToBlkNodeMap) {
        LustreEq propEq = null;

        if(propBlk.has(BLOCKTYPE)) {
            String srcBlkType = propBlk.get(BLOCKTYPE).asText();

            if(srcBlkType.equals(SUBSYSTEM)) {
                List<VarIdExpr> outVarIdExprs   = new ArrayList<>();
                List<JsonNode>  outportNodes    = getBlksFromSubSystemByType(propBlk, OUTPORT);

                for(JsonNode outNode : outportNodes) {
                    outVarIdExprs.add(new VarIdExpr(getQualifiedBlkName(propBlk)+"_"+getQualifiedBlkName(outNode)));
                }              
                propEq = new LustreEq(outVarIdExprs, translateBlock(true, getBlkHandle(propBlk), subsystemNode, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap, new HashSet<String>()));
            } else {
                LOGGER.log(Level.SEVERE, "Unsupported property block type: {0}!", srcBlkType);
            }
        }        
        return propEq;
    } 
    
    /**
     * 
     * @param outportNodes
     * @param subsystemNode
     * @param blkNodeToSrcBlkHandlesMap
     * @param blkNodeToDstBlkHandlesMap
     * @param handleToBlkNodeMap
     * @return Outport equation
     */
    protected LustreEq translateOutportEquation(List<JsonNode> outportNodes, JsonNode subsystemNode, Map<JsonNode, List<String>> blkNodeToSrcBlkHandlesMap, Map<JsonNode, List<String>> blkNodeToDstBlkHandlesMap, Map<String, JsonNode> handleToBlkNodeMap) {
        LustreEq eq = null;
        
        if(outportNodes.size() == 1) {
            JsonNode    outportNode = outportNodes.get(0);       

            if(blkNodeToSrcBlkHandlesMap.containsKey(outportNode)) {
                VarIdExpr       varIdExpr       = new VarIdExpr(getQualifiedBlkName(outportNode));                 
                List<String>    srcBlkHandles   = blkNodeToSrcBlkHandlesMap.get(outportNode);

                if(srcBlkHandles.size() == 1 && !srcBlkHandles.get(0).equals("-1")) {
                    eq = new LustreEq(varIdExpr, translateBlock(false, srcBlkHandles.get(0), subsystemNode, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap, new HashSet<String>()));
                } else if(srcBlkHandles.size() > 1) {
                    LOGGER.log(Level.SEVERE, "Unexpected: Multiple different src blocks connect to a same outport: {0}!", getQualifiedBlkName(outportNode));
                } else {
                    LOGGER.log(Level.SEVERE, "Unexpected: No src blocks connect to the outport: {0}!", getQualifiedBlkName(outportNode));
                }
            } else {
                LOGGER.log(Level.WARNING, "Unexpected: No src blocks connect to the outport: {0}!", getQualifiedBlkName(outportNode));
            }            
        } else if(outportNodes.size() > 1) {
            // Need to make sure the order of outports is correct
            List<VarIdExpr>     orderedOutportVars  = new ArrayList<>();
            List<String>        srcBlkHandles       = blkNodeToSrcBlkHandlesMap.get(outportNodes.get(0));
            List<String>        dstBlkHandes        = blkNodeToDstBlkHandlesMap.get(handleToBlkNodeMap.get(srcBlkHandles.get(0)));
                        
            for(String hdl : dstBlkHandes) {
                orderedOutportVars.add(new VarIdExpr(getQualifiedBlkName(handleToBlkNodeMap.get(hdl))));
            }
            eq = new LustreEq(orderedOutportVars, translateBlock(false, srcBlkHandles.get(0), subsystemNode, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap, new HashSet<String>()));
        }        
        return eq;
    }                  

    /**
     * 
     * @param isPropBlk
     * @param blkHandle
     * @param parentSubsystemNode
     * @param blkNodeToSrcBlkHdlsMap
     * @param blkNodeToDstBlkHdlsMap
     * @param hdlToBlkNodeMap
     * @param visitedPreHdls
     * @return The Lustre expression corresponding to the input JSON block node
     */

    protected LustreExpr translateBlock(boolean isPropBlk, String blkHandle, JsonNode parentSubsystemNode, Map<JsonNode, List<String>> blkNodeToSrcBlkHdlsMap,  Map<JsonNode, List<String>> blkNodeToDstBlkHdlsMap, Map<String, JsonNode> hdlToBlkNodeMap, Set<String> visitedPreHdls) {
        LustreExpr  blkExpr     = null;
        JsonNode    blkNode     = null;
                 
        if(hdlToBlkNodeMap.containsKey(blkHandle)) {
            blkNode = hdlToBlkNodeMap.get(blkHandle);
        } else {
            LOGGER.log(Level.SEVERE, "Unexpected: no block in the model with handle {0}", blkHandle);
        }
        
        // This is to simplify the translation for the case where the block node connect with an outport.
        // So instead of translating the outport's definition, we will directly use the outport variable.
        if(blkNodeToDstBlkHdlsMap.containsKey(blkNode) && isPropBlk) {
            List<String> dstHdls = blkNodeToDstBlkHdlsMap.get(blkNode);
            
            for(String hdl : dstHdls) {
                JsonNode dstNode = hdlToBlkNodeMap.get(hdl);
                
                if(dstNode.get(BLOCKTYPE).asText().equals(OUTPORT)) {
                    return new VarIdExpr(getQualifiedBlkName(dstNode));
                }
            }
        }        
        if(blkNode != null && blkNode.has(BLOCKTYPE)) {
            String              blkType     = blkNode.get(BLOCKTYPE).asText();            
            List<LustreExpr>    inExprs     = new ArrayList<>();            
            List<String>        inHandles   = new ArrayList<>();
            
            if(blkNodeToSrcBlkHdlsMap.containsKey(blkNode)) {
                inHandles = blkNodeToSrcBlkHdlsMap.get(blkNode);
            }
            if(blkType.toLowerCase().equals(LOGIC) || blkType.toLowerCase().equals(RELATIONALOP) || blkType.equals(MATH)) {
                blkType = blkNode.get(OPERATOR).asText();
            } else if(blkNode.has(LUSTREOPERATORBLK)){
                blkType = blkNode.get(LUSTREOPERATORBLK).asText();
            } else if(isIfActionSubsystem(blkNode)) {
                blkType = IFACTIONBLK;
            }
            // Since the MERGE block always bundles with other blocks (IF, IfActionSubsystem and etc.),
            // the inputs to the MERGE block need to be handled differently.
            if(!blkType.equals(MERGE) && !blkType.equals(MEMORY) && !blkType.equals(UNITDELAY) && !blkType.equals(IFACTIONBLK)) {              
                for(String inHandle : inHandles) {     
                    inExprs.add(translateBlock(isPropBlk, inHandle, parentSubsystemNode, blkNodeToSrcBlkHdlsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap, visitedPreHdls));                   
                }                
            }                         
            switch(blkType) {
                case EQ: {
                    tryLiftingExprTypes(inExprs, inHandles, hdlToBlkNodeMap);
                    blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.EQ, inExprs.get(1));                                       
                    break;
                }  
                case NEQ: {
                    tryLiftingExprTypes(inExprs, inHandles, hdlToBlkNodeMap);
                    blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.NEQ, inExprs.get(1));                                       
                    break;
                } 
                case GTE: {
                    enforceCorrectTyping(inExprs, inHandles, hdlToBlkNodeMap);
                    blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.GTE, inExprs.get(1));                                       
                    break;
                } 
                case LTE: {  
                    enforceCorrectTyping(inExprs, inHandles, hdlToBlkNodeMap);
                    blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.LTE, inExprs.get(1));                                       
                    break;
                }  
                case GT: { 
                    enforceCorrectTyping(inExprs, inHandles, hdlToBlkNodeMap);
                    blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.GT, inExprs.get(1));                                       
                    break;
                }  
                case LT: {  
                    enforceCorrectTyping(inExprs, inHandles, hdlToBlkNodeMap);
                    blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.LT, inExprs.get(1));                                       
                    break;
                }                  
                case NOT: { 
                    tryLoweringExprTypes(inExprs, inHandles, hdlToBlkNodeMap);
                    blkExpr = new UnaryExpr(UnaryExpr.Op.NOT, inExprs.get(0));                                      
                    break;
                }                   
                case OR: {
                    tryLoweringExprTypes(inExprs, inHandles, hdlToBlkNodeMap);
                    blkExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.OR, inExprs.get(i));   
                    }                                        
                    break;
                }
                case NOR: { 
                    tryLoweringExprTypes(inExprs, inHandles, hdlToBlkNodeMap);
                    blkExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        blkExpr = new UnaryExpr(UnaryExpr.Op.NOT, new BinaryExpr(blkExpr, BinaryExpr.Op.OR, inExprs.get(i)));   
                    }                                        
                    break;
                }                
                case XOR: { 
                    tryLoweringExprTypes(inExprs, inHandles, hdlToBlkNodeMap);
                    blkExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.XOR, inExprs.get(i));   
                    }                                        
                    break;
                }                
                case NXOR: { 
                    tryLoweringExprTypes(inExprs, inHandles, hdlToBlkNodeMap);
                    blkExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        blkExpr = new UnaryExpr(UnaryExpr.Op.NOT, new BinaryExpr(blkExpr, BinaryExpr.Op.XOR, inExprs.get(i)));   
                    }                                        
                    break;
                }                  
                case AND: { 
                    tryLoweringExprTypes(inExprs, inHandles, hdlToBlkNodeMap);
                    blkExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.AND, inExprs.get(i));   
                    }                                        
                    break;
                } 
                case NAND: { 
                    tryLoweringExprTypes(inExprs, inHandles, hdlToBlkNodeMap);
                    blkExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        blkExpr = new UnaryExpr(UnaryExpr.Op.NOT, new BinaryExpr(blkExpr, BinaryExpr.Op.AND, inExprs.get(i)));   
                    }                                        
                    break;
                }                 
                case ABS: {
                    blkExpr = new NodeCallExpr(getOrCreateLustreLibNode(blkNode), inExprs.get(0));                   
                    break;                    
                }
                case SQRT: {
                    blkExpr = new NodeCallExpr(getOrCreateLustreLibNode(blkNode), inExprs.get(0));                   
                    break;                    
                }
                case MOD: {
                    for(String hdl : inHandles) {
                        if(getBlkOutportType(hdlToBlkNodeMap.get(hdl)) == PrimitiveType.REAL) {
                            LOGGER.log(Level.SEVERE, "Unexpected: input {0} to MOD operator is REAL type!", getQualifiedBlkName(hdlToBlkNodeMap.get(hdl)));
                        }
                    }
                    blkExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.MOD, inExprs.get(i));   
                    }                                        
                    break;                  
                } 
                case UNARYMINUS: {
                    blkExpr = new UnaryExpr(UnaryExpr.Op.NEG, inExprs.get(0));                                                                           
                    break;                  
                }                 
                case SUM: {
                    int     numOfInputs = -1;
                    String  ops         = blkNode.get(INPUTS).asText();
                    
                    enforceCorrectTyping(inExprs, inHandles, hdlToBlkNodeMap);
                    if(ops.matches("\\d+")) {
                        numOfInputs = Integer.parseInt(ops);
                    } else {
                        ops = ops.replaceAll("\\|", "").trim();
                    }
                    
                    if(numOfInputs != -1 && numOfInputs != inExprs.size()) {
                        LOGGER.log(Level.SEVERE, "Number of inputs to SUM block does not equal the number of inputs specified in parameters: {0}", numOfInputs);
                    } else if((numOfInputs != -1 && numOfInputs == inExprs.size())) {
                        blkExpr = inExprs.get(0);
                        for(int i = 1; i < inExprs.size(); i++) {
                            blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.PLUS, inExprs.get(i));   
                        }                        
                    } else if(ops != null && ops.length() > 0 && ops.length() == inExprs.size()) {
                        blkExpr = inExprs.get(0);
                        
                        if(ops.charAt(0) == '-') {
                            blkExpr = new UnaryExpr(UnaryExpr.Op.NEG, blkExpr);
                        }
                        for(int i = 1; i < ops.length(); i++) {
                            switch (ops.charAt(i)) {
                                case '+':
                                    blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.PLUS, inExprs.get(i));
                                    break;
                                case '-':
                                    blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.MINUS, inExprs.get(i));
                                    break;
                                default:
                                    LOGGER.log(Level.SEVERE, "Unexpected operators in SUM blocks parameters: {0}", ops.charAt(i));
                                    break;
                            }
                        }
                    } else {
                        LOGGER.log(Level.SEVERE, "Unexpected definition for SUM block: {0}", blkNode);
                    }
                    break;
                }
                case SUBSYSTEM: {
                    
                    blkExpr = new NodeCallExpr(getQualifiedBlkName(blkNode), inExprs);
                    break;
                }
                case INPORT: {
                    blkExpr = new VarIdExpr(getQualifiedBlkName(blkNode));
                    break;
                }
                case CONSTANT: {
                    String      value   = blkNode.get(VALUE).asText().trim();
                    LustreType  type    = getBlkOutportType(blkNode);
                    
                    if(isValidConst(value)) {
                        blkExpr = getLustreConst(value, type);
                    //The value of the constant field of CompareToConstant block refers to some field of its parent node
                    } else if(parentSubsystemNode.has(value)) {
                        blkExpr = getLustreConst(parentSubsystemNode.get(value).asText().trim(), type);
                    } else {
                        LOGGER.log(Level.SEVERE, "Unexpected constant value in JSON node: {0}", value);
                    }
                    break;
                }
                case PRODUCT: {
                    int     numOfInputs = -1;
                    String  ops         = blkNode.get(INPUTS).asText();
                    
                    tryLiftingExprTypes(inExprs, inHandles, hdlToBlkNodeMap);
                    if(ops.matches("\\d+")) {
                        numOfInputs = Integer.parseInt(ops);
                    } else {
                        ops = ops.replaceAll("\\|", "").trim();
                    }
                    
                    if(numOfInputs != -1 && numOfInputs != inExprs.size()) {
                        LOGGER.log(Level.SEVERE, "Number of inputs to PRODUCT block does not equal the number of inputs specified in parameters: {0}", numOfInputs);
                    } else if((numOfInputs != -1 && numOfInputs == inExprs.size())) {
                        blkExpr = inExprs.get(0);
                        for(int i = 1; i < inExprs.size(); i++) {
                            blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.MULTIPLY, inExprs.get(i));   
                        }                        
                    } else if(ops != null && ops.length() > 0 && ops.length() == inExprs.size()) {
                        blkExpr = inExprs.get(0);
                        
                        if(ops.charAt(0) == '/') {
                            blkExpr = new BinaryExpr(new RealExpr(new BigDecimal("1.0")), BinaryExpr.Op.DIVIDE, blkExpr);
                        }
                        for(int i = 1; i < ops.length(); i++) {
                            switch (ops.charAt(i)) {
                                case '*':
                                    blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.MULTIPLY, inExprs.get(i));
                                    break;
                                case '/':
                                    blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.DIVIDE, inExprs.get(i));
                                    break;
                                default:
                                    LOGGER.log(Level.SEVERE, "Unexpected operators in PRODUCT blocks parameters: {0}", ops.charAt(i));
                                    break;
                            }
                        }
                    } else {
                        LOGGER.log(Level.SEVERE, "Unexpected definition for PRODUCT block: {0}", blkNode);
                    }
                    break;
                }
                case GAIN: {
                    String gain = blkNode.get(GAIN).asText();
                    
                    tryLiftingExprTypes(inExprs, inHandles, hdlToBlkNodeMap);
                    if(gain.contains(".")) {
                        blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.MULTIPLY, new RealExpr(new BigDecimal(gain)));
                    } else {
                        blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.MULTIPLY, new IntExpr(new BigInteger(gain)));
                    }
                    break;
                }                
                case MINMAX: {                    
                    if(inExprs.size() == 1) {
                        blkExpr = inExprs.get(0);
                    } else if(inExprs.size() > 1) {
                        tryLiftingExprTypes(inExprs, inHandles, hdlToBlkNodeMap);
                        blkExpr = new NodeCallExpr(getOrCreateLustreLibNode(blkNode), inExprs);
                    }
                    break;
                }
                case ARROW: {
                    blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.ARROW, inExprs.get(1));
                    break;
                }
                case SWITCH: {
                    LustreExpr  condExpr    = null;
                    String      criteria    = blkNode.get(CRITERIA).asText();
                    LustreType  condType    = getSwitchCondType(blkNode);
                                       
                    if(condType == PrimitiveType.BOOL) {
                        condExpr = inExprs.get(1);
                    } else {                        
                        String      threshold       = blkNode.get(THRESHOLD).asText();
                        LustreExpr  condRhsExpr     = getLustreConst(threshold, condType);

                        switch(criteria) {
                            case FSTCRIT: {
                                condExpr = new BinaryExpr(inExprs.get(1), BinaryExpr.Op.GTE, condRhsExpr);
                                break;
                            }
                            case SNDCRIT: {
                                condExpr = new BinaryExpr(inExprs.get(1), BinaryExpr.Op.GT, condRhsExpr);
                                break;
                            }
                            case TRDCRIT: {
                                condExpr = new BinaryExpr(inExprs.get(1), BinaryExpr.Op.NEQ, condRhsExpr);
                                break;
                            }
                            default:
                                break;
                        }
                    } 
                    blkExpr = new IteExpr(condExpr, inExprs.get(0), inExprs.get(2));
                    break;
                }               
                case MEMORY: 
                case UNITDELAY: {
                    if(this.auxHdlToPreExprMap.containsKey(blkHandle)) {
                        blkExpr = this.auxHdlToPreExprMap.get(blkHandle);
                    } else {
                        String      varName     = getQualifiedBlkName(blkNode);     
                        VarIdExpr   preVarId    = new VarIdExpr(varName);
                        
                        if(!visitedPreHdls.contains(blkHandle)) { 
                            visitedPreHdls.add(blkHandle);
                            LustreExpr  inExpr      = translateBlock(isPropBlk, inHandles.get(0), parentSubsystemNode, blkNodeToSrcBlkHdlsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap, visitedPreHdls);
                            String      init        = blkType.equals(MEMORY) ? blkNode.get(X0).asText() : blkNode.get(INITCOND).asText();                                        
                            LustreType  blkOutType  = getBlkOutportType(blkNode); 
                            LustreExpr  initExpr    = getLustreConst(init, blkOutType);                        
                            LustreExpr  preBlkExpr  = new UnaryExpr(UnaryExpr.Op.PRE, inExpr);                      

                            if(initExpr != null) {
                                preBlkExpr = new BinaryExpr(initExpr, BinaryExpr.Op.ARROW, preBlkExpr);
                            } else {
                                LOGGER.log(Level.SEVERE, "UNEXPECTED init value : {0} for memory or unit delay block", init);
                            }                            
                            this.auxNodeLocalVars.add(new LustreVar(varName, blkOutType));
                            this.auxNodeEqs.add(new LustreEq(preVarId, preBlkExpr));
                            this.auxHdlToPreExprMap.put(blkHandle, preVarId);                            
                        }
                        blkExpr = preVarId;
                    }
                    break;
                }
                case IFACTIONBLK: {
                    JsonNode ifBlk = null;
                    
                    for(String inHandle : inHandles) {
                        JsonNode inBlk = hdlToBlkNodeMap.get(inHandle);
                        
                        if(isIfBlock(inBlk)) {
                            ifBlk = inBlk;
                        } else {
                            inExprs.add(translateBlock(isPropBlk, inHandle, parentSubsystemNode, blkNodeToSrcBlkHdlsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap, visitedPreHdls));                   
                        }
                    }   
                    if(ifBlk != null) {
                        String              actSystName     = getQualifiedBlkName(blkNode);                        
                        LustreExpr          condExpr        = getCondExprFromIfBlk(isPropBlk, ifBlk, blkHandle, parentSubsystemNode, blkNodeToSrcBlkHdlsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap);
                        String              initStates      = getInitilizeStates(blkNode);
                        LustreExpr          actSysCall      = new NodeCallExpr(actSystName, inExprs);                        
                        List<VarIdExpr>     actSysOuts      = new ArrayList<>();
                        List<LustreExpr>    elseActSysOuts  = new ArrayList<>();
                        List<LustreType>    outTypes        = getBlockOutportTypes(blkNode);
                        LustreExpr          elseExpr        = null;
                        
                        if(blkNodeToDstBlkHdlsMap.containsKey(blkNode)) {
                            for(int i = 1; i <= blkNodeToDstBlkHdlsMap.get(blkNode).size(); i++) {
                                String varName      = actSystName+"_"+i;
                                VarIdExpr varExpr   = new VarIdExpr(varName);
                                
                                actSysOuts.add(varExpr);                                
                                if(initStates.equals(HELD)) {
                                    elseActSysOuts.add(new UnaryExpr(UnaryExpr.Op.PRE, varExpr));
                                } else {                                    
                                    elseActSysOuts.add(new NodeCallExpr(getOrCreateLustreLibNode(FIRST, outTypes.get(i-1)), varExpr));
                                }
                                this.auxNodeLocalVars.add(new LustreVar(varName, outTypes.get(i-1)));
                            }
                            this.auxNodeEqs.add(new LustreEq(actSysOuts, new IteExpr(condExpr, actSysCall, new TupleExpr(elseActSysOuts))));
                            blkExpr = new TupleExpr(actSysOuts);
                        } else {
                            LOGGER.log(Level.SEVERE, "Unexpected: a IF-ACTION-BLOCK does not have outputs!");
                        }                     
                    } else {
                        LOGGER.log(Level.SEVERE, "Unexpected: no if block is connected to a IF-ACTION-BLOCK.");
                    }
                    break;
                }
                // In Simulink, we translated together IF block with IfActionSubsystem and MERGE blocks together.
                // Todo: Need to consider the case that IF block used with IfActionSubsystem together.
                case IF: {
                    break;
                }
                // Merge block combines its inputs (conditionally-executed subsystems) into a single output 
                // line whose value at any time is equal to the most recently computed output of its driving blocks
                // Now, we only consider merge block connects with conditionally-executed subsystems from the same IF block
                case MERGE: {
                    String                  ifBlkHandle      = null;                    
                    Map<String, JsonNode>   hdlActSysNodeMap = new HashMap<>();
                    Map<String, LustreExpr> hdlActSysExprMap = new HashMap<>();
                    
                    for(String inHandle : inHandles) {
                        JsonNode inBlk = hdlToBlkNodeMap.get(inHandle);
                        
                        if(isIfActionSubsystem(inBlk)) {
                            hdlActSysNodeMap.put(inHandle, inBlk);
                        } else {
                            LOGGER.log(Level.SEVERE, "UNSUPPORTED: input to MERGE block is not a IF-ACTION-SUBSYSTEM!", blkType);
                        }
                    }
                    if(hdlActSysNodeMap.size() >= 2 && hdlActSysNodeMap.size() == inHandles.size()) {
                        // Translate each action block into a node call
                        for(Entry<String, JsonNode> entry : hdlActSysNodeMap.entrySet()) {
                            JsonNode actSubsystemNode = entry.getValue();
                            if(blkNodeToSrcBlkHdlsMap.containsKey(actSubsystemNode)) {
                                List<LustreExpr>    srcExprs    = new ArrayList<>();
                                List<String>        srcHandles  = blkNodeToSrcBlkHdlsMap.get(actSubsystemNode);                                
                                
                                for(String srcHandle : srcHandles) {
                                    if(isIfBlock(hdlToBlkNodeMap.get(srcHandle))) {
                                        if(ifBlkHandle == null) {
                                            ifBlkHandle = srcHandle;
                                        } else if(!ifBlkHandle.equals(srcHandle)){
                                            LOGGER.log(Level.SEVERE, "UNEXPECTED: IF-ACTION-SUBSYSTEMs to the same MERGE block connects with different IF blocks", srcHandle);
                                        }
                                    } else {
                                        srcExprs.add(translateBlock(isPropBlk, srcHandle, parentSubsystemNode, blkNodeToSrcBlkHdlsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap, visitedPreHdls));
                                    }
                                }
                                hdlActSysExprMap.put(entry.getKey(), new NodeCallExpr(getQualifiedBlkName(actSubsystemNode), srcExprs));
                            }                            
                        }                        
                    }
                    if(hdlToBlkNodeMap.containsKey(ifBlkHandle)) {
                        blkExpr = translateIfBlock(isPropBlk, hdlToBlkNodeMap.get(ifBlkHandle), hdlActSysExprMap, parentSubsystemNode, blkNodeToSrcBlkHdlsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap);
                    }
                    break;
                }                 
                default:
                    LOGGER.log(Level.SEVERE, "Unsupported block type: {0}!", blkType);
                    break;
            }
        }                       
        
        return blkExpr;
    }
    
    protected String getInitilizeStates(JsonNode ifActionSubsystem) {
        String initializeState = "";
        if(ifActionSubsystem != null) {
            if(ifActionSubsystem.has(BLOCKTYPE)) {
                if(ifActionSubsystem.get(BLOCKTYPE).asText().equals(SUBSYSTEM)) {
                    Iterator<Entry<String, JsonNode>> contentFields = ifActionSubsystem.get(CONTENT).fields();  

                    while(contentFields.hasNext()) {
                        JsonNode fieldNode = contentFields.next().getValue();

                        if(fieldNode.has(BLOCKTYPE) && fieldNode.get(BLOCKTYPE).asText().equals(ACTIONPORT)) {
                            if(fieldNode.has(INITSTATES)) {
                                initializeState = fieldNode.get(INITSTATES).asText();
                                break;
                            }
                        }
                    }
                }
            }            
        }
        return initializeState;
    }
    
    protected LustreExpr getCondExprFromIfBlk(boolean isPropBlk, JsonNode ifBlkNode, String ifActionBlkHdl, JsonNode parentSubsystemNode, Map<JsonNode, List<String>> blkNodeToSrcBlkHandlesMap,  Map<JsonNode, List<String>> blkNodeToDstBlkHandlesMap, Map<String, JsonNode> handleToBlkNodeMap) {
        if(this.hdlToCondExprMap.containsKey(ifActionBlkHdl)) {
            return this.hdlToCondExprMap.get(ifActionBlkHdl);
        }        
        if(ifBlkNode != null) {            
            //Since Simulink checks the validity of ifCondExpr and elseIfCondExpr, we assume the input are valid.
            List<LustreExpr>    inExprs         = new ArrayList<>();
            List<LustreExpr>    condExprs       = new ArrayList<>();
            List<String>        condStrExprs    = new ArrayList<>();            
            String              ifCondExpr      = ifBlkNode.get(IFEXPRESSION).asText().trim();
            String              elseIfCondExpr  = ifBlkNode.get(ELSEIFEXPRS).asText().trim();
            List<String>        dstBlkHdls = blkNodeToDstBlkHandlesMap.get(ifBlkNode);
                       
            if(ifCondExpr != null && !ifCondExpr.equals("")) {
                condStrExprs.add(ifCondExpr);
            }
            if(elseIfCondExpr != null && !elseIfCondExpr.equals("")) {
                condStrExprs.addAll(Arrays.asList(elseIfCondExpr.split(",")));
            }
                        
            if(blkNodeToSrcBlkHandlesMap.containsKey(ifBlkNode)) {
                for(String inHandle : blkNodeToSrcBlkHandlesMap.get(ifBlkNode)) {
                    inExprs.add(translateBlock(isPropBlk, inHandle, parentSubsystemNode, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap, new HashSet<String>()));
                }  
            }
            // If the number of conditional expressions is equal to the number of destination blocks, 
            // it means there is no else branch
            condExprs = convertIteCondExprs(condStrExprs, inExprs); 
            if(!condExprs.isEmpty()) {
                LustreExpr disjAllCondExprs = condExprs.get(0);

                for(int i = 0; i < condExprs.size(); i++) {
                    this.hdlToCondExprMap.put(dstBlkHdls.get(i), condExprs.get(i));
                    if(i > 0) {
                        disjAllCondExprs = new BinaryExpr(disjAllCondExprs, BinaryExpr.Op.OR, condExprs.get(i));
                    }
                }
                if(dstBlkHdls.size() == condExprs.size()+1) {
                    this.hdlToCondExprMap.put(dstBlkHdls.get(dstBlkHdls.size()-1), new UnaryExpr(UnaryExpr.Op.NOT, disjAllCondExprs));
                }                
            }
        }        
        return this.hdlToCondExprMap.get(ifActionBlkHdl);        
    }
    
    protected LustreExpr translateIfBlock(boolean isPropBlk, JsonNode ifBlkNode, Map<String, LustreExpr> hdlActSysExprMap, JsonNode parentSubsystemNode, Map<JsonNode, List<String>> blkNodeToSrcBlkHandlesMap,  Map<JsonNode, List<String>> blkNodeToDstBlkHandlesMap, Map<String, JsonNode> handleToBlkNodeMap) {
        LustreExpr ifBlkExpr = null;
        
        if(ifBlkNode != null) {            
            //Since Simulink checks the validity of ifCondExpr and elseIfCondExpr, we assume the input are valid.
            List<LustreExpr>    inExprs         = new ArrayList<>();
            List<LustreExpr>    condExprs       = new ArrayList<>();
            List<String>        condStrExprs    = new ArrayList<>();            
            String              ifCondExpr      = ifBlkNode.get(IFEXPRESSION).asText().trim();
            String              elseIfCondExpr  = ifBlkNode.get(ELSEIFEXPRS).asText().trim();
            List<String>        dstBlockHandles = blkNodeToDstBlkHandlesMap.get(ifBlkNode);
                       
            if(ifCondExpr != null && !ifCondExpr.equals("")) {
                condStrExprs.add(ifCondExpr);
            }
            if(elseIfCondExpr != null && !elseIfCondExpr.equals("")) {
                condStrExprs.addAll(Arrays.asList(elseIfCondExpr.split(",")));
            }
                        
            if(blkNodeToSrcBlkHandlesMap.containsKey(ifBlkNode)) {
                for(String inHandle : blkNodeToSrcBlkHandlesMap.get(ifBlkNode)) {
                    inExprs.add(translateBlock(isPropBlk, inHandle, parentSubsystemNode, blkNodeToSrcBlkHandlesMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap, new HashSet<String>()));
                }  
            }
            // The number of condition expressions is one less than the number of branches
            if(condStrExprs.size() == hdlActSysExprMap.size()-1) {
                LustreExpr      condEnumExpr    = null;                
                List<String>    enumValues      = new ArrayList<>();
                List<VarIdExpr> enumValueExprs  = new ArrayList<>();                

                condExprs = convertIteCondExprs(condStrExprs, inExprs);                
                for(int i = 0; i <= condExprs.size(); i++){
                    String val = CONDENUM+(COUNT++);
                    enumValues.add(val);
                    enumValueExprs.add(new VarIdExpr(val));
                }
                
                int condExprsSize       = condExprs.size();
                int enumValueExprsSize  = enumValueExprs.size();
                
                String          condVarName = CONDEXPRVAR+(COUNT++);
                LustreEnumType  enumType    = new LustreEnumType(CONDEXPRENUMTYPE+(COUNT++), enumValues);                
                LustreVar       condVar     = new LustreVar(condVarName, enumType);
                VarIdExpr       condVarExpr = new VarIdExpr(condVarName);                
                IteExpr         clockExpr = new IteExpr(condExprs.get(condExprsSize-1), enumValueExprs.get(enumValueExprsSize-2), enumValueExprs.get(enumValueExprsSize-1));
                
                for(int j = condExprsSize-2; j >= 0; j--) {
                    clockExpr = new IteExpr(condExprs.get(j), enumValueExprs.get(j-1), clockExpr);
                }
                
                List<LustreExpr> mergeExprs = new ArrayList<>();
                                
                for(int k = 0; k < dstBlockHandles.size(); k++) {
                    // o = when enum(c)    
                    BinaryExpr outExpr = new BinaryExpr(hdlActSysExprMap.get(dstBlockHandles.get(k)), BinaryExpr.Op.WHEN, new NodeCallExpr(enumValues.get(k), condVarExpr));
                    // enum -> o = when enum(c)
                    outExpr = new BinaryExpr(enumValueExprs.get(k), BinaryExpr.Op.ARROW, outExpr);
                    mergeExprs.add(outExpr);                    
                }
                this.auxNodeLocalVars.add(condVar);
                this.lustreProgram.addEnumDef(enumType);
                this.auxNodeEqs.add(new LustreEq(condVarExpr, clockExpr));                
                ifBlkExpr = new MergeExpr(condVarExpr, mergeExprs);
            } else {
                LOGGER.log(Level.SEVERE, "UNEXPECTED: there is a mismatch in IF block between Ite conditions with its outputs!");
            }
        }
        return ifBlkExpr;
    }
    
    protected List<LustreExpr> convertIteCondExprs(List<String> condStrExprs, List<LustreExpr> inExprs) {
        List<LustreExpr> condExprs = new ArrayList<>();
        
        for(String condStrExpr : condStrExprs) {
            condExprs.add(buildIteCondExpr(condStrExpr, inExprs));
        }
        return condExprs;
    }
    
    // Assumption: condStrExpr is a valid string representaiton of a condition expression.
    protected LustreExpr buildIteCondExpr(String strExpr, List<LustreExpr> inExprs) {        
        String      condStrExpr = strExpr.trim();
        ExprParser  parser      = Parboiled.createParser(ExprParser.class);
        ParsingResult<?> result = new RecoveringParseRunner(parser.InputLine()).run(condStrExpr);
        
        if (result.hasErrors()) {
            LOGGER.log(Level.SEVERE, "****** Parse ITE condition expression errors: {0}", printParseErrors(result));
        }    
        return convertStrSimExprToLusExpr(result.parseTreeRoot.getValue(), inExprs);
    }
    
    protected LustreExpr convertStrSimExprToLusExpr(Object astNode, List<LustreExpr> inExprs) {
        LustreExpr lusExpr = null;
        
        if(astNode instanceof AstNode) {
            // astNode is a leaf node
            if(((AstNode)astNode).operator == null) {
                String  leafNode    = ((AstNode)astNode).value.trim();
                
                if(isNum(leafNode)) {
                    if(leafNode.contains(".")) {
                        lusExpr = new RealExpr(new BigDecimal(leafNode));
                    } else {
                        lusExpr = new IntExpr(new BigInteger(leafNode));
                    }
                } else {
                    int index = Integer.parseInt(leafNode.substring(1).trim());

                    if(index >= 1 && index <= inExprs.size()) {
                        lusExpr = inExprs.get(index-1);
                    } else {
                        LOGGER.log(Level.SEVERE, "UNEXPECTED: the input variable index for ITE epxression is not within the expected range!");
                    }                         
                }                                                          
            } else {
                String              op              = ((AstNode)astNode).operator.trim();
                List<LustreExpr>    childrenExpr    = new ArrayList<>();
                               
                for(AstNode child : ((AstNode)astNode).getChildren()) {
                    childrenExpr.add(convertStrSimExprToLusExpr(child, inExprs));
                }                                
                switch(op) {
                    case "<": {
                        lusExpr = new BinaryExpr(childrenExpr.get(0), BinaryExpr.Op.LT, childrenExpr.get(1));
                        break;
                    }
                    case ">": {
                        lusExpr = new BinaryExpr(childrenExpr.get(0), BinaryExpr.Op.GT, childrenExpr.get(1));
                        break;
                    }
                    case "<=": {
                        lusExpr = new BinaryExpr(childrenExpr.get(0), BinaryExpr.Op.LTE, childrenExpr.get(1));
                        break;
                    }
                    case ">=": {
                        lusExpr = new BinaryExpr(childrenExpr.get(0), BinaryExpr.Op.GTE, childrenExpr.get(1));
                        break;
                    }
                    case "==": {
                        lusExpr = new BinaryExpr(childrenExpr.get(0), BinaryExpr.Op.EQ, childrenExpr.get(1));
                        break;
                    }
                    case "~=": {
                        lusExpr = new BinaryExpr(childrenExpr.get(0), BinaryExpr.Op.NEQ, childrenExpr.get(1));
                        break;
                    }
                    case "&": {
                        lusExpr = new BinaryExpr(childrenExpr.get(0), BinaryExpr.Op.AND, childrenExpr.get(1));
                        break;
                    }
                    case "|": {
                        lusExpr = new BinaryExpr(childrenExpr.get(0), BinaryExpr.Op.OR, childrenExpr.get(1));
                        break;
                    }
                    case "~": {
                        lusExpr = new UnaryExpr(UnaryExpr.Op.NOT, childrenExpr.get(0));
                        break;
                    }
                    case "-": {
                        lusExpr = new UnaryExpr(UnaryExpr.Op.NEG, childrenExpr.get(0));
                        break;
                    }
                    default:
                        LOGGER.log(Level.SEVERE, "UNEXPECTED operator in ITE condition expression: {0}", op);
                        break;
                }
            }
        }
        
        return lusExpr;
    }

    /**
     * @param nodeName
     * @return The name of library Lustre node corresponding to the input JsonNode
     */
    protected String getOrCreateLustreLibNode(String nodeName, LustreType type) {        
        String newName = nodeName +"_"+ type.toString();
        
        if(this.libNodeNameMap.containsKey(newName)) {                    
            return this.libNodeNameMap.get(newName);
        }  
        
        VarIdExpr           inVarExpr   = new VarIdExpr("in");
        VarIdExpr           outVarExpr  = new VarIdExpr("out");   
        List<LustreEq>      bodyExprs   = new ArrayList<>();           
        switch(nodeName) {
            case FIRST: {
                bodyExprs.add(new LustreEq(outVarExpr, new BinaryExpr(inVarExpr, BinaryExpr.Op.ARROW, new UnaryExpr(UnaryExpr.Op.PRE, outVarExpr))));
                this.libNodeNameMap.put(newName, newName);
                this.lustreProgram.addNode(new LustreNode(nodeName, new LustreVar("in", type), new LustreVar("out", type), bodyExprs));                                                
                break;
            }
            default:
                break;
        }
        return newName;
    }
    
    /**
     * @param nodeName
     * @return The name of library Lustre node corresponding to the input JsonNode
     */
    protected String getOrCreateLustreLibNode(String nodeName) {        
        if(this.libNodeNameMap.containsKey(nodeName)) {                    
            return this.libNodeNameMap.get(nodeName);
        }        
        
        VarIdExpr           inVarExpr   = new VarIdExpr("in");
        VarIdExpr           outVarExpr  = new VarIdExpr("out");   
        List<LustreEq>      bodyExprs   = new ArrayList<>();            
        
        switch(nodeName) {
            case BOOLTOINT: {                 
                bodyExprs.add(new LustreEq(outVarExpr, new IteExpr(inVarExpr, new IntExpr(BigInteger.ONE), new IntExpr(BigInteger.ZERO))));
                this.libNodeNameMap.put(BOOLTOINT, BOOLTOINT);
                this.lustreProgram.addNode(new LustreNode(nodeName, new LustreVar("in", PrimitiveType.BOOL), new LustreVar("out", PrimitiveType.INT), bodyExprs));                                
                break;
            }
            case BOOLTOREAL: {
                bodyExprs.add(new LustreEq(outVarExpr, new IteExpr(inVarExpr, new RealExpr(BigDecimal.ONE), new RealExpr(BigDecimal.ZERO))));
                this.libNodeNameMap.put(BOOLTOREAL, BOOLTOREAL);
                this.lustreProgram.addNode(new LustreNode(nodeName, new LustreVar("in", PrimitiveType.BOOL), new LustreVar("out", PrimitiveType.REAL), bodyExprs));                                
                break;
            }  
            case INTTOBOOL: {
                bodyExprs.add(new LustreEq(outVarExpr, new IteExpr(new BinaryExpr(inVarExpr, BinaryExpr.Op.NEQ, new IntExpr(new BigInteger("0"))), new BooleanExpr(true), new BooleanExpr(false))));
                this.libNodeNameMap.put(INTTOBOOL, INTTOBOOL);
                this.lustreProgram.addNode(new LustreNode(nodeName, new LustreVar("in", PrimitiveType.INT), new LustreVar("out", PrimitiveType.BOOL), bodyExprs));                                
                break;
            } 
            case REALTOBOOL: {
                bodyExprs.add(new LustreEq(outVarExpr, new IteExpr(new BinaryExpr(inVarExpr, BinaryExpr.Op.NEQ, new RealExpr(BigDecimal.ZERO)), new BooleanExpr(true), new BooleanExpr(false))));
                this.libNodeNameMap.put(REALTOBOOL, REALTOBOOL);
                this.lustreProgram.addNode(new LustreNode(nodeName, new LustreVar("in", PrimitiveType.REAL), new LustreVar("out", PrimitiveType.BOOL), bodyExprs));                                
                break;
            }
            case INTTOREAL: {                
                this.libNodeNameMap.put(INTTOREAL, INTTOREAL);
                this.lustreProgram.addNode(new LustreNode(nodeName, new LustreVar("in", PrimitiveType.INT), new LustreVar("out", PrimitiveType.REAL), bodyExprs));                                
                break;
            }            
            default:
                LOGGER.log(Level.SEVERE, "Unsupported library node type: {0}", nodeName);
                break;
        }                
        return nodeName;
    }

    
    /**
     * @param blkNode
     * @return The name of library Lustre node corresponding to the input JsonNode
     */
    protected String getOrCreateLustreLibNode(JsonNode blkNode) {
        String path     = blkNode.get(PATH).asText();
        String nodeName = path.replace(File.separator,"_");
        String blkType  = blkNode.get(BLOCKTYPE).asText();
        
        switch(blkType) {         
            case ABS: {
                LustreType  outType = getBlkOutportType(blkNode);
                
                if(this.libNodeNameMap.containsKey(ABS+"_"+outType)) {
                    nodeName = this.libNodeNameMap.get(ABS+"_"+outType);
                    break;
                }
                
                LustreExpr  zeroExpr = null;
                
                if(outType == PrimitiveType.INT) {
                    nodeName    = nodeName + "_int";
                    zeroExpr    = new IntExpr(new BigInteger("0"));                    
                } else if(outType == PrimitiveType.REAL) {
                    nodeName    = nodeName + "_real";
                    zeroExpr    = new RealExpr(new BigDecimal("0.0"));
                } else {
                    LOGGER.log(Level.SEVERE, "Unsupported library node input and output type: {0}", outType);
                }
                
                VarIdExpr           inVarExpr   = new VarIdExpr("in");
                VarIdExpr           outVarExpr  = new VarIdExpr("out");   
                List<LustreEq>      bodyExprs   = new ArrayList<>();                  
    
                bodyExprs.add(new LustreEq(outVarExpr, new IteExpr(new BinaryExpr(inVarExpr, BinaryExpr.Op.GTE, zeroExpr), inVarExpr, new UnaryExpr(UnaryExpr.Op.NEG, inVarExpr))));
                this.libNodeNameMap.put(ABS+"_"+outType, nodeName);
                this.lustreProgram.addNode(new LustreNode(nodeName, new LustreVar("in", outType), new LustreVar("out", outType), bodyExprs));                
                break;
            }
            case SQRT: {
                LustreType  outType = getBlkOutportType(blkNode);
                
                if(this.libNodeNameMap.containsKey(SQRT+"_"+outType)) {
                    nodeName = this.libNodeNameMap.get(SQRT+"_"+outType);
                    break;
                }
                if(outType == PrimitiveType.INT) {
                    nodeName    = SQRT + "_int";              
                } else if(outType == PrimitiveType.REAL) {
                    nodeName    = SQRT + "_real";
                } else {
                    LOGGER.log(Level.SEVERE, "Unsupported library node input and output type: {0}", outType);
                }
                this.libNodeNameMap.put(nodeName, nodeName);
                this.lustreProgram.addNode(new LustreNode(nodeName, new LustreVar("in", outType), new LustreVar("out", outType), new ArrayList<LustreEq>()));                
                break;
            }            
            case MINMAX: {
                String      funcName    = blkNode.get(FUNCTION).asText();
                LustreType  outType     = getBlkOutportType(blkNode);
                
                if(this.libNodeNameMap.containsKey(funcName+"_"+outType)) {
                    nodeName = this.libNodeNameMap.get(funcName+"_"+outType);
                    break;
                }                
                
                LustreExpr          rhsExpr     = null;             
                VarIdExpr           outVarExpr  = new VarIdExpr("out");
                int                 numOfInputs = blkNode.get(INPUTS).asInt();                
                List<LustreType>    inTypes     = getBlockInportTypes(blkNode);
                List<LustreVar>     inputVars   = new ArrayList<>();
                List<LustreVar>     outputVars  = Arrays.asList(new LustreVar("out", outType));
                List<VarIdExpr>     inExprs     = new ArrayList<>();
                List<LustreEq>      bodyExprs   = new ArrayList<>();   
                
                nodeName = nodeName + "_" + funcName + "_" + outType;
                for(int i = 0; i < inTypes.size(); i++) {
                    String varName = "in_" + i;
                    inputVars.add(new LustreVar(varName, inTypes.get(i)));
                    inExprs.add(new VarIdExpr(varName));
                }

                if(numOfInputs != inExprs.size()) {
                    LOGGER.log(Level.SEVERE, "Inputs to MINMAX blocks does not match the actual inputs!");
                } else if(numOfInputs == 1) {
                    rhsExpr = inExprs.get(0);
                } else if(numOfInputs > 1) {
                    BinaryExpr.Op op = null;

                    switch (funcName) {
                        case MAX: 
                            op = BinaryExpr.Op.GTE;
                            break;
                        case MIN:
                            op = BinaryExpr.Op.LTE;
                            break;
                        default:
                            LOGGER.log(Level.SEVERE, "Unsupported MINMAX function operator type: {0}", funcName);
                            break;
                    }

                    LustreExpr ifCond   = new BinaryExpr(inExprs.get(numOfInputs-2), op, inExprs.get(numOfInputs-1));

                    for(int i = 0; i < numOfInputs-2; i++) {
                        ifCond = new BinaryExpr(new BinaryExpr(inExprs.get(numOfInputs-2), op, inExprs.get(i)), BinaryExpr.Op.AND, ifCond);
                    }
                    rhsExpr = new IteExpr(ifCond, inExprs.get(numOfInputs-2), inExprs.get(numOfInputs-1));

                    for(int i = numOfInputs-3; i >= 0; i--) {
                        ifCond = new BinaryExpr(inExprs.get(i), op, inExprs.get(i+1));

                        for(int j = 0; j < i; j++) {
                            ifCond = new BinaryExpr(new BinaryExpr(inExprs.get(i), op, inExprs.get(j)), BinaryExpr.Op.AND, ifCond);
                        }
                        for(int k = i+2; k < numOfInputs-1; k++) {
                            ifCond = new BinaryExpr(new BinaryExpr(inExprs.get(i), op, inExprs.get(k)), BinaryExpr.Op.AND, ifCond);
                        }
                        rhsExpr = new IteExpr(ifCond, inExprs.get(i), rhsExpr);
                    }                                               
                }          
                bodyExprs.add(new LustreEq(outVarExpr, rhsExpr));
                this.libNodeNameMap.put(funcName+"_"+outType, nodeName);
                this.lustreProgram.addNode(new LustreNode(nodeName, inputVars, outputVars, bodyExprs));                                
                break;
            }
            default:
                LOGGER.log(Level.SEVERE, "Unsupported library node type: {0}", blkType);
                break;
        }
        
        
        return nodeName;
    }

    /*************************************************************************************************/
    /*************************************** Utility Functions ***************************************/
    /*************************************************************************************************/
    
    /**
     * @param node
     * @return true if node is a IF block otherwise false
     */    
    protected boolean isIfBlock(JsonNode node) {
        if(node != null) {
            if(node.has(BLOCKTYPE) && node.get(BLOCKTYPE).asText().equals(IF)) {
                return true;
            }                                  
        }
        return false;        
    }
    
    /**
     * This is to check whether the input handle is the handle of some user-defined subsystem node
     * @param hdl
     * @return 
     */
    protected boolean isUserDefinedSubsystemBlk(String hdl) {
        if(hdl != null) {
            for(JsonNode subsystemNode : this.subsystemNodes) {
                if(subsystemNode.has(HANDLE)) {
                    if(subsystemNode.get(HANDLE).asText().equals(hdl)) {
                        if(subsystemNode.has(MASKTYPE)) {
                            String maskType = subsystemNode.get(MASKTYPE).asText();
                            if(!maskType.equals(COMPARETOZERO) && !maskType.equals(COMPARETOCONSTANT)) {
                                return true;
                            }
                        } else {
                            return true;    
                        }                        
                    }
                }
            }
        }
        return false;
    }
        
    
    protected boolean isContractInternalBlk(JsonNode node) {
        if(node != null) {
            if(node.has(BLOCKTYPE) && node.get(BLOCKTYPE).asText().equals(SUBSYSTEM)) {
                if(node.has(CONTRACTBLKTYPE)) {
                    String blkType = node.get(CONTRACTBLKTYPE).asText();                    
                    return blkType.equals(MODE) || blkType.equals(ENSURE) ||
                           blkType.equals(REQUIRE) || blkType.equals(ASSUME) || blkType.equals(GUARANTEE);
                }
            }
        }
        return false;        
    }    
    
    protected boolean isContractBlk(JsonNode node) {
        if(node != null) {
            if(node.has(BLOCKTYPE) && node.get(BLOCKTYPE).asText().equals(SUBSYSTEM)) {
                if(node.has(CONTRACTBLKTYPE) && node.get(CONTRACTBLKTYPE).asText().equals(CONTRACT)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    protected boolean isModeBlk(JsonNode node) {
        if(node != null) {
            if(node.has(BLOCKTYPE) && node.get(BLOCKTYPE).asText().equals(SUBSYSTEM)) {
                if(node.has(CONTRACTBLKTYPE) && node.get(CONTRACTBLKTYPE).asText().equals(MODE)) {
                    return true;
                }
            }
        }
        return false;
    } 
    
    protected boolean isAssumeBlk(JsonNode node) {
        if(node != null) {
            if(node.has(BLOCKTYPE) && node.get(BLOCKTYPE).asText().equals(SUBSYSTEM)) {
                if(node.has(CONTRACTBLKTYPE) && node.get(CONTRACTBLKTYPE).asText().equals(ASSUME)) {
                    return true;
                }
            }
        }
        return false;
    } 

    protected boolean isGuaranteeBlk(JsonNode node) {
        if(node != null) {
            if(node.has(BLOCKTYPE) && node.get(BLOCKTYPE).asText().equals(SUBSYSTEM)) {
                if(node.has(CONTRACTBLKTYPE) && node.get(CONTRACTBLKTYPE).asText().equals(GUARANTEE)) {
                    return true;
                }
            }
        }
        return false;
    } 

    protected boolean isRequireBlk(JsonNode node) {
        if(node != null) {
            if(node.has(BLOCKTYPE) && node.get(BLOCKTYPE).asText().equals(SUBSYSTEM)) {
                if(node.has(CONTRACTBLKTYPE) && node.get(CONTRACTBLKTYPE).asText().equals(REQUIRE)) {
                    return true;
                }
            }
        }
        return false;
    } 

    protected boolean isEnsureBlk(JsonNode node) {
        if(node != null) {
            if(node.has(BLOCKTYPE) && node.get(BLOCKTYPE).asText().equals(SUBSYSTEM)) {
                if(node.has(CONTRACTBLKTYPE) && node.get(CONTRACTBLKTYPE).asText().equals(ENSURE)) {
                    return true;
                }
            }
        }
        return false;
    } 

    protected boolean isValidatorBlk(JsonNode node) {
        if(node != null) {
            if(node.has(BLOCKTYPE) && node.get(BLOCKTYPE).asText().equals(MSFUNCTION)) {
                if(node.has(CONTRACTBLKTYPE) && node.get(CONTRACTBLKTYPE).asText().equals(VALIDATOR)) {
                    return true;
                }
            }
        }
        return false;
    }     
    
    protected void enforceCorrectTyping(List<LustreExpr> inExprs, List<String> inHandles, Map<String, JsonNode> handleToBlkNodeMap) {
        Object[] results = getTheHighestType(inHandles, handleToBlkNodeMap);
        
        if(((Boolean)results[0])) {
            LustreType highestType = (LustreType)results[1];
            
            for(int i = 0; i < inHandles.size(); i++) {
                LustreType nodeType = getBlkOutportType(handleToBlkNodeMap.get(inHandles.get(i)));

                if((nodeType == PrimitiveType.BOOL) && (highestType == PrimitiveType.INT)) {
                    inExprs.set(i, new NodeCallExpr(getOrCreateLustreLibNode(BOOLTOINT), inExprs.get(i)));
                } else if((nodeType == PrimitiveType.BOOL) && (highestType == PrimitiveType.REAL)) {
                    inExprs.set(i, new NodeCallExpr(getOrCreateLustreLibNode(BOOLTOREAL), inExprs.get(i)));
                } else if((nodeType == PrimitiveType.INT) && (highestType == PrimitiveType.REAL)) {
                    inExprs.set(i, new NodeCallExpr(getOrCreateLustreLibNode(INTTOREAL), inExprs.get(i)));
                } else {
                    LOGGER.log(Level.SEVERE, "Unsupported type conversion: {0} to {1}", new Object[]{nodeType, highestType});
                }
            }            
        } else if(getBlkOutportType(handleToBlkNodeMap.get(inHandles.get(0))) == PrimitiveType.BOOL) {            
            for(int i = 0; i < inHandles.size(); i++) {
                inExprs.set(i, new NodeCallExpr(getOrCreateLustreLibNode(BOOLTOINT), inExprs.get(i)));
            }
        }        
    }    
    
    
    protected void tryLiftingExprTypes(List<LustreExpr> inExprs, List<String> inHandles, Map<String, JsonNode> handleToBlkNodeMap) {
        Object[] results = getTheHighestType(inHandles, handleToBlkNodeMap);
        
        if(((Boolean)results[0])) {
            LustreType highestType = (LustreType)results[1];
            
            for(int i = 0; i < inHandles.size(); i++) {
                LustreType nodeType = getBlkOutportType(handleToBlkNodeMap.get(inHandles.get(i)));

                if((nodeType == PrimitiveType.BOOL) && (highestType == PrimitiveType.INT)) {
                    inExprs.set(i, new NodeCallExpr(getOrCreateLustreLibNode(BOOLTOINT), inExprs.get(i)));
                } else if((nodeType == PrimitiveType.BOOL) && (highestType == PrimitiveType.REAL)) {
                    inExprs.set(i, new NodeCallExpr(getOrCreateLustreLibNode(BOOLTOREAL), inExprs.get(i)));
                } else if((nodeType == PrimitiveType.INT) && (highestType == PrimitiveType.REAL)) {
                    inExprs.set(i, new NodeCallExpr(getOrCreateLustreLibNode(INTTOREAL), inExprs.get(i)));
                } else {
                    LOGGER.log(Level.SEVERE, "Unsupported type conversion: {0} to {1}", new Object[]{nodeType, highestType});
                }
            }            
        }
    }
    
    protected void tryLoweringExprTypes(List<LustreExpr> inExprs, List<String> inHandles, Map<String, JsonNode> handleToBlkNodeMap) {
        Object[] results = getTheLowestType(inHandles, handleToBlkNodeMap);        
        if(((Boolean)results[0])) {
            LustreType lowestType = (LustreType)results[1];
            
            for(int i = 0; i < inHandles.size(); i++) {
                LustreType nodeType = getBlkOutportType(handleToBlkNodeMap.get(inHandles.get(i)));

                if((nodeType == PrimitiveType.INT) && (lowestType == PrimitiveType.BOOL)) {
                    inExprs.set(i, new NodeCallExpr(getOrCreateLustreLibNode(INTTOBOOL), inExprs.get(i)));
                } else if((nodeType == PrimitiveType.REAL) && (nodeType == PrimitiveType.BOOL)) {
                    inExprs.set(i, new NodeCallExpr(getOrCreateLustreLibNode(REALTOBOOL), inExprs.get(i)));
                } else if((nodeType == PrimitiveType.REAL) && (nodeType == PrimitiveType.INT)) {
                    LOGGER.log(Level.WARNING, "UNSUPPORTED: cannot lower real type to int type!");
                }
            }            
        }
    } 
        
    
    protected boolean isIfActionSubsystem(JsonNode node) {
        if(node != null) {
            if(node.has(BLOCKTYPE)) {
                if(node.get(BLOCKTYPE).asText().equals(SUBSYSTEM)) {
                    Iterator<Entry<String, JsonNode>> contentFields = node.get(CONTENT).fields();  

                    while(contentFields.hasNext()) {
                        JsonNode fieldNode = contentFields.next().getValue();

                        if(fieldNode.has(BLOCKTYPE) && fieldNode.get(BLOCKTYPE).asText().equals(ACTIONPORT)) {
                            return true;
                        }
                    }
                }
            }            
        }
        return false;
    }

    protected LustreExpr getLustreConst(String value, LustreType type) {
        LustreExpr constExpr = null;
        
        if(isValidConst(value)) {
            if(type == PrimitiveType.REAL) {
                String newVal = value;
                if(value.toLowerCase().equals("true")) {
                    newVal = "1.0";
                } else if(value.toLowerCase().equals("false")) {
                    newVal = "0.0";
                } else if(!value.contains(".")) {
                    newVal += ".0";
                }
                constExpr = new RealExpr(new BigDecimal(newVal));
            } else if(type == PrimitiveType.INT) {
                String newVal = value;
                if(value.toLowerCase().equals("true")) {
                    newVal = "1";
                } else if(value.toLowerCase().equals("false")) {
                    newVal = "0";
                }
                constExpr = new IntExpr(new BigInteger(newVal));
            } else if(type == PrimitiveType.BOOL) {
                constExpr = new BooleanExpr(value);
            } else {
                LOGGER.log(Level.SEVERE, "Unsupported constant datatype: {0}", type);
            }            
        } else {
            LOGGER.log(Level.SEVERE, "Unexpected constant value: {0}", value);
        } 
        return constExpr;
    }    
    
    protected Object[] getTheHighestType(List<String> handles, Map<String, JsonNode> handleToBlkNodeMap) {        
        LustreType highestType      = null;
        boolean    hasDiscrepancy   = false;
        Object[] results = new Object[2];
        
        
        if(handles != null && handles.size() > 0) {
             highestType = getBlkOutportType(handleToBlkNodeMap.get(handles.get(0)));
             
             for(int i = 1; i < handles.size(); i++) {
                 LustreType hType = getBlkOutportType(handleToBlkNodeMap.get(handles.get(i)));
                 
                 if(highestType.compareTo(hType) < 0) {
                     highestType    = hType;
                     hasDiscrepancy = true;
                 }
             }
        }
        results[0] = hasDiscrepancy;
        results[1] = highestType;
        return results;
    }
    
    protected Object[] getTheLowestType(List<String> handles, Map<String, JsonNode> handleToBlkNodeMap) {        
        LustreType lowestType       = null;
        boolean    hasDiscrepancy   = false;
        Object[] results = new Object[2];
        
        
        if(handles != null && handles.size() > 0) {
             lowestType = getBlkOutportType(handleToBlkNodeMap.get(handles.get(0)));
             
             for(int i = 1; i < handles.size(); i++) {
                 LustreType lType = getBlkOutportType(handleToBlkNodeMap.get(handles.get(i)));
                 
                 if(lowestType.compareTo(lType) > 0) {
                     lowestType     = lType;
                     hasDiscrepancy = true;
                 }
             }
        }
        results[0] = hasDiscrepancy;
        results[1] = lowestType;
        return results;
    }    
    
    /**
     * 
     * @param type
     * @return The corresponding Lustre type from its string representation
     */
    protected LustreType getLustreTypeFromStrRep(String type) {
        LustreType lusType = null;
        
        switch(type.toLowerCase()) {
            case "float": 
            case "single":                
            case "double": {
                lusType = PrimitiveType.REAL;
                break;
            }
            case "integer": 
            case "int": 
            case "int64":     
            case "int32": 
            case "int16": 
            case "int8": 
            case "uint8":
            case "uint16":
            case "uint32":{
                lusType = PrimitiveType.INT;
                break;                
            }            
            case "boolean": 
            case "bool": {
                lusType = PrimitiveType.BOOL;
                break;                
            }        
            default:
                break;
        }
        
        return lusType;
    }
    
    
    protected LustreType getSwitchCondType(JsonNode switchBlk) {        
        List<String>        types   = convertJsonValuesToList(switchBlk.get(PORTDATATYPE).get(INPORT));
        Iterator<JsonNode>  connIt  = switchBlk.get(CONNECTIVITY).elements(); 
        // The switch condition is the second port in the port connectivity
        return getLustreTypeFromStrRep(types.get(connIt.next().get(TYPE).asInt()));
    }    
    
    private List<String> convertJsonValuesToList(JsonNode values) {
        List<String> strValues = new ArrayList<>();
        
        if(values.isArray()) {
            Iterator<JsonNode> valuesIt = values.elements();
            
            while(valuesIt.hasNext()) {
                strValues.add(valuesIt.next().asText());
            }
        } else {
            strValues.add(values.asText());
        }
        return strValues;
    }
    
    protected List<String> getBlkOutportHandles(JsonNode blkNode) {
        List<String> outportHdls = new ArrayList<>();

        if(blkNode.has(CONNECTIVITY)) {
            JsonNode        portConns       = blkNode.get(CONNECTIVITY);

            if(portConns.isArray()) {
                Iterator<JsonNode> portConnIt = portConns.elements();

                while(portConnIt.hasNext()) {
                    JsonNode connNode = portConnIt.next();

                    outportHdls.addAll(convertJsonValuesToList(connNode.get(DSTBLOCK)));
                }
            } else {
                outportHdls.addAll(convertJsonValuesToList(portConns.get(DSTBLOCK)));            
            }              
        }      
        return outportHdls;
    }    
    
    protected boolean isPropertyBlk(JsonNode blkNode) {
        if(blkNode.has(ANNOTATIONTYPE)) {
            if(blkNode.get(ANNOTATIONTYPE).asText().equals(ENSURES)) {
                return true;
            }
        }
        return false;
    }
    
    protected LustreType getBlkOutportType(JsonNode blkNode) {
        return getLustreTypeFromStrRep(blkNode.get(PORTDATATYPE).get(OUTPORT).asText());
    }
    
    protected List<LustreType> getBlockOutportTypes(JsonNode blkNode) {
        List<LustreType>    types       = new ArrayList<>();
        List<String>        typeStrs    = convertJsonValuesToList(blkNode.get(PORTDATATYPE).get(OUTPORT));        
        
        for(String typeStr : typeStrs) {
            types.add(getLustreTypeFromStrRep(typeStr));
        }
        
        return types;
    }    
    
    protected List<LustreType> getBlockInportTypes(JsonNode blkNode) {
        List<LustreType>    types       = new ArrayList<>();
        List<String>        typeStrs    = convertJsonValuesToList(blkNode.get(PORTDATATYPE).get(INPORT));        
        
        for(String typeStr : typeStrs) {
            types.add(getLustreTypeFromStrRep(typeStr));
        }
        
        return types;
    }  
    
    
    protected String getBlkName(JsonNode node) {
        if(node.equals(this.topLevelNode)) {
            return this.topNodeName;
        } else if(node.has(NAME)) {
            return sanitizeName(node.get(NAME).asText());
        } else {
            LOGGER.log(Level.SEVERE, "Unexpected: the input block does not have a name!");
        }
        return "";
    }  
    protected String getQualifiedBlkName(JsonNode node) {
        if(node.equals(this.topLevelNode)) {
            return this.topNodeName;
        } else if(node.has(PATH)) {
            return sanitizeName(node.get(PATH).asText());
        } else {
            LOGGER.log(Level.SEVERE, "Unexpected: the input block does not have a name!");
        }
        return "";
    }      
    
    protected String getBlkHandle(JsonNode node) {
        if(node.has(HANDLE)) {
            return node.get(HANDLE).asText();
        } else {
            LOGGER.log(Level.SEVERE, "Unexpected: the input block does not have a handle!");
        }
        return "";
    }   
    
    protected int getBlkPortPosition(JsonNode node) {
        int index = -1;
        if(node.has(PORT)) {
            index = Integer.parseInt(node.get(PORT).asText().trim())-1;
        } else {
            LOGGER.log(Level.SEVERE, "Unexpected: in/out port does not have a port field!");
        }
        return index;
    }
    
    protected boolean isNum(String str) {
        return str.matches("^[-+]?\\d+(\\.\\d+)?$");
    }
    
    protected boolean isBool(String str) {
        return str.toLowerCase().equals("true") || str.toLowerCase().equals("false") ;
    }    
    
    protected boolean isValidConst(String str) {
        return isNum(str) || isBool(str);
    }
    
    protected boolean isNumType(LustreType type) {
        return type == PrimitiveType.REAL || type == PrimitiveType.INT;
    }  
    
    protected String sanitizeName(String name) {
        return name.trim().replace(" ", "_").replace("\n", "_").replace("=", "_")
                .replace(">", "_").replace("<", "_").replace("/", "_").replace("\\", "_")
                .replace("-", "_").replace("+", "_");
    }
    
    protected List<JsonNode> getBlksFromSubSystemByType(JsonNode subsystemNode, String blkType) {
        List<JsonNode> blkNodes = new ArrayList();
        Iterator<Entry<String, JsonNode>> contentFields = subsystemNode.get(CONTENT).fields(); 
        
        while(contentFields.hasNext()) {
            Map.Entry<String, JsonNode> contField       = contentFields.next();   
            JsonNode                    contBlkNode     = contField.getValue();
            
            if(contBlkNode.has(BLOCKTYPE) && contBlkNode.get(BLOCKTYPE).asText().equals(blkType)) {
                blkNodes.add(contBlkNode);
            }           
        }
        return blkNodes;
    }      
    
}
