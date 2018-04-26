/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
 */

package edu.uiowa.json2lus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.uiowa.json2lus.ExprParser.AstNode;
import edu.uiowa.json2lus.lustreAst.ArrayExpr;
import edu.uiowa.json2lus.lustreAst.ArrayType;
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
    private final String LOG            = "log";    
    private final String EXP            = "exp";
    private final String SUM            = "Sum";
    private final String POW            = "pow";
    private final String MOD            = "mod";    
    private final String ABS            = "Abs";
    private final String FLOOR          = "floor";
    private final String CEIL           = "ceil";
    private final String ROUND          = "round";
    private final String FIX            = "fix";
    private final String SQRT           = "Sqrt";
    private final String MATH           = "Math";
    private final String LOG10          = "log10";
    private final String LSQRT          = "sqrt";
    private final String RSQRT          = "rSqrt";
    private final String TENTOU         = "10^u";        
    private final String SQUARE         = "squaer";
    private final String SQRTOP         = "sqrt";
    private final String RECIPROCAL     = "reciprocal";
    private final String SIGNEDSQRT     = "signedSqrt";
    private final String UNARYMINUS     = "UnaryMinus";
    
    private final String IF             = "If";    
    private final String SWITCH         = "Switch";
    private final String CRITERIA       = "Criteria";
    private final String THRESHOLD      = "Threshold";
    private final String TRDCRIT        = "u2 ~= 0";
    private final String FSTCRIT        = "u2 >= Threshold"; 
    private final String SNDCRIT        = "u2 > Threshold"; 
    private final String UNITDELAY      = "UnitDelay";    

    private final String U              = "u";
    private final String Y              = "y";
    private final String X0             = "InitialCondition";
    private final String MAX            = "max";
    private final String MIN            = "min";
    private final String GAIN           = "Gain";
    private final String FIRST          = "First";            
    private final String MERGE          = "Merge";
    private final String INPUTS         = "Inputs";    
    private final String MINMAX         = "MinMax";    
    private final String MEMORY         = "Memory";
    private final String ROUNDING       = "Rounding";       
    
    /** State flow language features */
    private final String OUTPUT         = "Output";
    private final String SFCONTENT      = "StateflowContent";
    
    private final String MUX            = "Mux";
    private final String CONST          = "const";
    private final String ARROW          = "ArrowOperator";
    private final String DEMUX          = "Demux";
    private final String PRODUCT        = "Product";
    private final String COLLAPSE       = "collapse";
    private final String CONSTANT       = "Constant";        
    private final String FUNCTION       = "Function";    
    private final String INITCOND       = "InitialCondition"; 
    private final String IFEXPRESSION   = "IfExpression";
    private final String ELSEIFEXPRS    = "ElseIfExpressions";
    private final String COMTOCONST     = "ComToConst";
    private final String COMTOZERO      = "ComToZero";
    private final String COMPARE        = "Compare";
    private final String MATRIX         = "Matrix(*)";     
    private final String ELEMENTWISE    = "Element-wise(.*)";
    private final String DIMENSION      = "CompiledPortDimensions";       
    
    private final String COLLAPSEDIM        = "CollapseDim"; 
    private final String COLLAPSEMODE       = "CollapseMode"; 
    private final String MULTIPLICATIOIN    = "Multiplication";    
    private final String ALLDIMENSIONS      = "All dimensions";  
    private final String SPECDIMENSIONS     = "Specified dimension";  
    private final String DATATYPECONVERSION = "DataTypeConversion";
    
            
    /** Blocks information */
    private final String META           = "meta";
    private final String DATA           = "Data";
    private final String NAME           = "Name";
    private final String PORT           = "Port";
    private final String TYPE           = "Type";
    private final String PATH           = "Path";
    private final String VALUE          = "Value";
    private final String LOGIC          = "logic";    
    private final String SCOPE          = "Scope";
    private final String HANDLE         = "Handle";  
            
    private final String MSFUNCTION     = "M-S-Function";      
    private final String MODEBLK        = "ContractModeBlock";    
    private final String REQUIREBLK     = "ContractRequireBlock";        
    private final String ENSUREBLK      = "ContractEnsureBlock";
    private final String ASSUMEBLK      = "ContractAssumeBlock";        
    private final String CONTRACTBLK    = "ContractBlock";
    private final String GUARANTEEBLK   = "ContractGuaranteeBlock";    
    private final String VALIDATOR      = "ContractValidatorBlock";    
             
    
    /** CoCoSim block type to indicate property block */            
    private final String HELD               = "held";
    private final String RESET              = "reset";
    private final String LOCAL              = "Local";      
    private final String ORIGIN             = "Origin_path";             
    private final String ENSURES            = "ensures";
    private final String INPORT             = "Inport";    
    private final String OUTPORT            = "Outport";    
    private final String CONTENT            = "Content";    
    private final String OPERATOR           = "Operator";
    private final String MASKTYPE           = "MaskType";
    private final String SRCBLOCK           = "SrcBlock";
    private final String SRCPORT            = "SrcPort";
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
    private final String MATRIXPRODUCT      = "matrix";
    
    /** Lustre program information */            
    private final String INDEX          = "Index";    
    private final String ASSUME         = "assume";
    private final String ENSURE         = "ensure";
    private final String REQUIRE        = "require";     
    private final String NODENAME       = "NodeName";        
    private final String MODENAME       = "ModeName";        
    private final String VARIABLE       = "VariableName"; 
    private final String PROPNAME       = "PropertyName";    
    private final String GUARANTEE      = "guarantee";
    private final String ORIGINPATH     = "OriginPath";     
    private final String CONTRACTNAME   = "ContractName";                             
    
    /** Lustre node names for type conversions */
    private final String INT            = "int";
    private final String REAL           = "real";
    private final String BOOL           = "bool";
    
    private final String BOOLTOINT      = "bool_to_int";
    private final String INTTOBOOL      = "int_to_bool";    
    private final String BOOLTOREAL     = "bool_to_real";
    private final String REALTOBOOL     = "real_to_bool";    
    private final String INTTOREAL      = "real";
    private final String REALTOINT      = "int";
    private final String RESULTMATRIX   = "result_matrix";
                      
    private JsonNode                                topLevelNode;  
    private final List<LustreNode>                  auxLusNode;
    private final List<LustreEq>                    auxNodeEqs;
    private final List<LustreVar>                   auxNodeLocalVars;    
    private final LustreProgram                     lustreProgram;
    private final List<JsonNode>                    subsystemNodes;    
    private final Set<JsonNode>                     contractNodes;    
    private final Map<String, String>               libNodeNameMap;    
    private final Map<String, LustreExpr>           hdlToCondExprMap;
    private final Map<String, LustreExpr>           auxHdlToPreExprMap;    
    private final List<Map<String, String>>         jsonMappingInfo;
    private final Map<JsonNode, List<JsonNode>>     subsystemPropsMap;
    private final Map<String, List<LustreExpr>>     auxHdlToExprMap;
    
    /**
     * Constructor
     * @param inputPath
     */
    public J2LTranslator(String inputPath) {          
        this.inputPath          = inputPath;  
        this.contractNodes      = new HashSet<>();        
        this.libNodeNameMap     = new HashMap<>();
        this.auxLusNode         = new ArrayList<>();
        this.jsonMappingInfo    = new ArrayList<>();
        this.subsystemNodes     = new ArrayList<>();
        this.subsystemPropsMap  = new HashMap<>();
        this.auxHdlToPreExprMap = new HashMap<>();
        this.hdlToCondExprMap   = new HashMap<>();
        this.auxNodeEqs         = new ArrayList<>();
        this.auxNodeLocalVars   = new ArrayList<>();        
        this.lustreProgram      = new LustreProgram();
        this.auxHdlToExprMap    = new HashMap<>();        
        this.topNodeName        = inputPath.toLowerCase().endsWith(".json") ? 
                                    inputPath.substring(inputPath.lastIndexOf(File.separator)+1, inputPath.lastIndexOf("."))
                                    : inputPath.substring(inputPath.lastIndexOf(File.separator)+1);                            
    }    
    
    /**
     * Execute the translation process 
     * 
     * @return lustreProgram
     */    
    public LustreProgram execute() {
        // Collect all subsystem blocks
        collectSubsytemBlocks();
        
        // Ensure a bottom-up translation to avoid forward reference issue
        for(int i = this.subsystemNodes.size()-1; i >= 0; i--) {
            JsonNode subsystemNode = this.subsystemNodes.get(i);
            // Translate each subsystem node
            List<LustreAst> asts = new ArrayList<>();
            
            if(subsystemNode.has(SFCONTENT) && subsystemNode.get(SFCONTENT).size() > 0) {
                Sf2LTranslator sfTranslator = new Sf2LTranslator();                
                asts = sfTranslator.translate(subsystemNode);
            } else if(subsystemNode.has(CONTENT) && subsystemNode.get(CONTENT) != null) {
                asts.add(translateSubsystemNode(subsystemNode));
            }
            for(LustreAst ast : asts) {
                if(ast instanceof LustreNode) {
                    this.lustreProgram.addNode((LustreNode)ast);
                } else if(ast instanceof LustreContract) {
                    this.lustreProgram.addContract((LustreContract)ast);
                    if(!this.auxLusNode.isEmpty()) {
                        this.lustreProgram.addNodes(this.auxLusNode);
                        this.auxLusNode.clear();
                    }
                }                
            }
        }
        return this.lustreProgram;
    }

    /**
     * Dump the properties mapping from Simulink to Lustre to a given JSON file
     * @param path 
     */
    public void dumpMappingInfoToJsonFile(String path) {
        if(this.jsonMappingInfo.isEmpty()) {
            LOGGER.log(Level.INFO, "Nothing to dump: no mapping information was generated!");                        
            return;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        
        try {
            File absoluteOutput  = new File(path).getAbsoluteFile();
            
            if(!absoluteOutput.exists()) {
                if(!absoluteOutput.getParentFile().exists()) {
                    absoluteOutput.getParentFile().createNewFile();
                }
                absoluteOutput.createNewFile();
            }            
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(absoluteOutput))) {
                String json = objectMapper.writeValueAsString(this.jsonMappingInfo);
                bw.write(json);
            }
            LOGGER.log(Level.INFO, "A Simulink to Lustre mapping information file was generated at: {0}", path);                        
        } catch (JsonProcessingException ex) {
            Logger.getLogger(J2LTranslator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(J2LTranslator.class.getName()).log(Level.SEVERE, null, ex);
        }
        
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
            LOGGER.log(Level.INFO, "Found a subsystem block: {0}  ", sanitizeName(subsystemNode.equals(this.topLevelNode) ? this.topNodeName:subsystemNode.get(NAME).asText()));
            this.subsystemNodes.add(subsystemNode);           
            Iterator<Entry<String, JsonNode>> nodes = subsystemNode.get(CONTENT).fields();
            
            while(nodes.hasNext()) {
                JsonNode fieldNode = nodes.next().getValue();
                
                if(getBlkType(fieldNode).equals(SUBSYSTEM)) {
                    collectSubsytemBlocks(fieldNode);
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
     * @param blkNodeToSrcBlkPortsMap
     * @param blkNodeToDstBlkHdlsMap
     * @param hdlToBlkNodeMap
     * @return A list of contracts corresponding to the contract node
     */    
    protected LustreContract translateContractNode(JsonNode contractNode, JsonNode validatorBlkNode, List<LustreVar> inputs, List<LustreVar> outputs, Map<JsonNode, List<String>> blkNodeToSrcBlkHdlsMap, Map<JsonNode, List<Integer>> blkNodeToSrcBlkPortsMap, Map<JsonNode, List<String>> blkNodeToDstBlkHdlsMap, Map<String, JsonNode> hdlToBlkNodeMap) {        
        // Get the contract name
        String contractName = getQualifiedBlkName(contractNode); 
        
        LOGGER.log(Level.INFO, "Start translating the contract block: {0}", contractName);
        
        // Set up data structures for storing contract information
        int aIndex = 1, gIndex = 1;             
        Map<String, List<LustreExpr>>   modeToRequires  = new HashMap<>();
        Map<String, List<LustreExpr>>   modeToEnsures   = new HashMap<>();           
        List<LustreExpr>                assumptions     = new ArrayList<>();            
        List<LustreExpr>                guarantees      = new ArrayList<>();                                  
        List<String>                    inHdls          = blkNodeToSrcBlkHdlsMap.get(validatorBlkNode);
        List<Integer>                   inPorts         = blkNodeToSrcBlkPortsMap.get(validatorBlkNode);         
        
        for(int i = 0; i < inHdls.size(); i++) {
            // Get the in block to the validator block node
            JsonNode    inBlk       = hdlToBlkNodeMap.get(inHdls.get(i));            
            // Get the origin path
            String      originPath  = inBlk.get(ORIGIN).asText();
            
            // Translate assumption blocks
            if(isAssumeBlk(inBlk)) {
                assumptions.add(translateBlock(false, inHdls.get(i), contractNode, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap, new HashSet<String>(), null, inPorts.get(i)));
                addMappingInfo(originPath, null, contractName, null, ASSUME, String.valueOf(aIndex++), null);                
            // Translate guarantee blocks                
            } else if(isGuaranteeBlk(inBlk)) {
                guarantees.add(translateBlock(false, inHdls.get(i), contractNode, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap, new HashSet<String>(), null, inPorts.get(i)));
                addMappingInfo(originPath, null, contractName, null, GUARANTEE, String.valueOf(gIndex++), null);                
            } else if(isModeBlk(inBlk)) {
                // Set up data structures for requires and ensures of modes
                int rIndex = 1, eIndex = 1;
                String modeName                 = getBlkName(inBlk);
                List<LustreExpr>    requires    = new ArrayList<>();
                List<LustreExpr>    ensures     = new ArrayList<>();
                List<String>        modeInHdls  = blkNodeToSrcBlkHdlsMap.get(inBlk);
                List<Integer>       modeInPorts = blkNodeToSrcBlkPortsMap.get(inBlk);
                
                // Translate requires and ensures
                for(int j = 0; j < modeInHdls.size(); j++) {
                    JsonNode    modeInBlk       = hdlToBlkNodeMap.get(modeInHdls.get(j));
                    LustreExpr  modeInBlkExpr   = translateBlock(false, modeInHdls.get(j), contractNode, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap, new HashSet<String>(), null, modeInPorts.get(j));
                    originPath  = modeInBlk.get(ORIGIN).asText();
                    
                    if(isRequireBlk(modeInBlk)) {
                        requires.add(modeInBlkExpr);
                        addMappingInfo(originPath, null, contractName, modeName, REQUIRE, String.valueOf(rIndex++), null);
                    } else if(isEnsureBlk(modeInBlk)) {
                        ensures.add(modeInBlkExpr);
                        addMappingInfo(originPath, null, contractName, modeName, ENSURE, String.valueOf(eIndex++), null);                        
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
        // Clear the outputs because later we will regenerate outputs for contracts
        outputs.clear();        
        
        // Create the contract 
        LustreContract contract = new LustreContract(contractName, assumptions, guarantees, inputs, outputs, this.auxNodeLocalVars, this.auxNodeEqs, modeToRequires, modeToEnsures);
        this.auxNodeEqs.clear();
        this.auxNodeLocalVars.clear();
        this.auxHdlToExprMap.clear();
        this.auxHdlToPreExprMap.clear();             
        return contract;
    }   
   
    /**
     * 
     * @param subsystemNode
     * @return The Lustre node or contract corresponding to the input subsystem JSON node
     */
    protected LustreAst translateSubsystemNode(JsonNode subsystemNode) {
        // Ignore Mode, Lustre operator(arrow), compare to zero, compare to constant and property blocks
        if(isModeBlk(subsystemNode) || isLustreOpBlk(subsystemNode) || isCompareToConst(subsystemNode) || 
           isCompareToZero(subsystemNode) || isPropertyBlk(subsystemNode)) {
            return null;
        }
        
        String lusNodeName = subsystemNode.equals(this.topLevelNode) ? this.topNodeName : getQualifiedBlkName(subsystemNode);         
        
        LOGGER.log(Level.INFO, "Start translating the subsystem block: {0}", lusNodeName);        
        
        /** Necessary information for constructing node or contract */
        List<LustreEq>          props           = new ArrayList<>();
        List<LustreVar>         inputs          = new ArrayList<>();        
        List<LustreVar>         locals          = new ArrayList<>();
        List<LustreVar>         outputs         = new ArrayList<>();
        List<LustreEq>          equations       = new ArrayList<>();
        Map<Integer, JsonNode>  inports         = new HashMap<>();
        Map<Integer, JsonNode>  outports        = new HashMap<>();
        
        /** Validator block */
        JsonNode                            validatorBlk                = null;
        
        /** JSON nodes for contracts */
        List<JsonNode>                      contractNodes               = new ArrayList<>();  
        
        /** Terminator node in the input subsystem block */
        List<JsonNode>                      propNodes                   = new ArrayList<>();   
        
        /** Outport nodes in the input subsystem block */
        List<JsonNode>                      outportNodes                = new ArrayList<>();         
        
        /** A mapping between a handle and the block node */
        Map<String, JsonNode>               hdlToBlkNodeMap             = new HashMap<>();                
        
        /** Mappings between a block node and its src block handles and their port positions */
        Map<JsonNode, List<String>>         blkNodeToSrcBlkHdlsMap      = new HashMap<>();
        Map<JsonNode, List<Integer>>        blkNodeToSrcBlkPortsMap     = new HashMap<>();
        
        /** A mapping between a block node and its dst block handles */
        Map<JsonNode, List<String>>         blkNodeToDstBlkHdlsMap      = new HashMap<>();        
        
        /** ALL the fields under content field of subsystem block */
        Iterator<Entry<String, JsonNode>>   contentFields               = subsystemNode.get(CONTENT).fields();
        
        // Iterate over the contentFields
        while(contentFields.hasNext()) {
            Map.Entry<String, JsonNode> contField       = contentFields.next();   
            JsonNode                    contBlkNode     = contField.getValue();
            
            if(contBlkNode.has(BLOCKTYPE)) {
                String contBlkType = contBlkNode.get(BLOCKTYPE).asText();
                
                // Remember the handle to block node mapping 
                hdlToBlkNodeMap.put(getBlkHandle(contBlkNode), contBlkNode);
                
                // Collect inport, outport, property, contract, validator blocks
                if(contBlkType.equals(INPORT)) {
                    if(!isContractBlk(subsystemNode)) {
                        addMappingInfo(getPath(contBlkNode), lusNodeName, null, null, null, null, getBlkName(contBlkNode));    
                    } else {
                        addMappingInfo(getPath(contBlkNode), null, lusNodeName, null, null, null, getBlkName(contBlkNode));    
                    }                 
                    inports.put(getBlkPortPosition(contBlkNode), contBlkNode);
                } else if(contBlkType.equals(OUTPORT)) {
                    if(!isContractBlk(subsystemNode)) {
                        addMappingInfo(getPath(contBlkNode), lusNodeName, null, null, null, null, getBlkName(contBlkNode));    
                    }                    
                    outports.put(getBlkPortPosition(contBlkNode), contBlkNode);
                } else if(isPropertyBlk(contBlkNode)) {
                    propNodes.add(contBlkNode);
                } else if(isContractBlk(contBlkNode)) {
                    contractNodes.add(contBlkNode);
                } else if(isValidatorBlk(contBlkNode)) {
                    validatorBlk = contBlkNode;
                } 
                // Populate connectivities content block node
                populateConnectivitiesDS(contBlkNode, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHdlsMap);                
            }            
        }
        // Since top-level subsystem block does not have port connectivity field,
        // we only populate connectivities for its internal subsystem nodes
        populateConnectivitiesDS(subsystemNode, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHdlsMap);
        
        // Ensure the positions of input and output are correct
        for(int i = 0; i < inports.size(); i++) {
            String          inportName      = getBlkName(inports.get(i));
            List<Integer>   dimensions      = getDimensions(inports.get(i));
            LustreType      baseType        = getBlkOutportType(inports.get(i));
            boolean         isMatrixMode    = getMatrixMode(inports.get(i), hdlToBlkNodeMap, blkNodeToDstBlkHdlsMap);
            
            
            if(dimensions.size() == 1 && dimensions.get(0) == 1 && !isMatrixMode) {
                inputs.add(new LustreVar(inportName, baseType));                     
            } else {
                ArrayType array = new ArrayType(baseType, dimensions);
                inputs.add(new LustreVar(inportName, array));
            }            
        }
        for(int i = 0; i < outports.size(); i++) {
            String          outportName     = getBlkName(outports.get(i));     
            List<Integer>   dimensions      = getDimensions(outports.get(i));
            LustreType      baseType        = getBlkInportType(outports.get(i));  
            boolean         isMatrixMode    = getMatrixMode(inports.get(i), hdlToBlkNodeMap, blkNodeToDstBlkHdlsMap);
            
            if(dimensions.size() == 1 && dimensions.get(0) == 1 && !isMatrixMode) {
                outputs.add(new LustreVar(outportName, baseType));
            } else {
                ArrayType array = new ArrayType(baseType, dimensions);
                outputs.add(new LustreVar(outportName, array));
            }            
        } 
        
        // Translate contract
        if(isContractBlk(subsystemNode)) {
            // Add the contract mapping info
            addMappingInfo(getPath(subsystemNode), null, getQualifiedBlkName(subsystemNode), null, null, null, null);            
            return translateContractNode(subsystemNode, validatorBlk, inputs, outputs, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap);
        } else {
            // For translated Lustre nodes
            List<LustreAst>     lusNodes    = new ArrayList<>();
            
            // Add the Lustre node mapping info
            addMappingInfo(getPath(subsystemNode), getQualifiedBlkName(subsystemNode), null, null, null, null, null);            
            
            // Attach contracts to nodes
            attachContractsToNode(contractNodes, blkNodeToSrcBlkHdlsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap);
            
            // Add all translated equations
            equations.addAll(translateOutportEquations(new ArrayList<>(outports.values()), subsystemNode, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap));                    
            
            // Translate properties
            for(JsonNode propNode : propNodes) {                
                List<LustreEq> propEqs = translatePropNode(propNode, subsystemNode, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap);
                
                for(LustreEq propEq : propEqs) {
                    for(LustreExpr var : propEq.getLhs()) {
                        if(var instanceof VarIdExpr) {
                            addMappingInfo(propNode.get(ORIGIN).asText(), lusNodeName, null, null, ((VarIdExpr)var).id, null, null);
                        }
                        
                    }                
                    props.add(propEq);                           
                }
            }   
            
            // Add auxilary node equations generated  translation
            equations.addAll(this.auxNodeEqs);
            
            // Add auxilary local variables generated in the translation
            locals.addAll(this.auxNodeLocalVars);               
            
            // Add translated nodes
            LustreNode node = new LustreNode(subsystemNode.equals(this.topLevelNode), lusNodeName, inputs, outputs, locals, equations, props);                                                
            
            // Clear the auxilary information
            this.auxNodeEqs.clear();
            this.auxNodeLocalVars.clear();
            this.auxHdlToExprMap.clear();
            this.auxHdlToPreExprMap.clear();                  
            return node;
        }                
    }
    
    protected boolean getMatrixMode(JsonNode blkNode,  Map<String, JsonNode> hdlBlkNodeMap, Map<JsonNode, List<String>> blkNodeToDstBlkHandlesMap) {
        for(String dstHdl : blkNodeToDstBlkHandlesMap.get(blkNode)) {
            JsonNode dstBlkNode = hdlBlkNodeMap.get(dstHdl);
            
            if(dstBlkNode.has(MULTIPLICATIOIN)) {
                return dstBlkNode.get(MULTIPLICATIOIN).asText().equals(MATRIX);
            }            
        }

        return false;
    }

    /**
     * Connect nodes with their contracts based on the inports of the contract
     * @param contractNodes
     * @param blkNodeToSrcBlkHdlsMap
     * @param blkNodeToDstBlkHandlesMap
     * @param hdlBlkNodeMap
     */
    protected void attachContractsToNode(List<JsonNode> contractNodes, Map<JsonNode, List<String>> blkNodeToSrcBlkHdlsMap, Map<JsonNode, List<String>> blkNodeToDstBlkHandlesMap, Map<String, JsonNode> hdlBlkNodeMap) {
        // Return nothing if contract nodes is null
        if(contractNodes != null) {
            for(JsonNode contractNode : contractNodes) {
                // src is the handle of the source block of the contract
                String          src         = null;                
                // Get the in handles of the contract
                List<String>    srcHdls     = blkNodeToSrcBlkHdlsMap.get(contractNode);
                // Get the contract handle
                String          contractHdl = getBlkHandle(contractNode);
                
                if(srcHdls != null) {
                    for(String srcHdl : srcHdls) {
                        // Get all the destination block handles of srcHdl
                        List<String> dstHdls = blkNodeToDstBlkHandlesMap.get(hdlBlkNodeMap.get(srcHdl));
                        
                        if(dstHdls != null) {
                            for(String dstHdl : dstHdls) {
                                
                                // If the in handles of the contract contains the dst handle
                                // This is to say that there exists a source node with handle "dstHdl"
                                if(srcHdls.contains(dstHdl)) {
                                    
                                    // Get all source handles of the dstHdl node
                                    List<String> sndHdls = blkNodeToSrcBlkHdlsMap.get(hdlBlkNodeMap.get(dstHdl));
                                    
                                    // No source nodes with handles that contains the contract handle
                                    if(sndHdls != null && !sndHdls.contains(contractHdl)) {
                                        src = dstHdl;
                                        break;
                                    }
                                    
                                }
                            }                            
                        }
                        // We found the handle of the node
                        if(src != null) {
                            break;
                        }                       
                    }
                    
                    if(src != null) {
                        // Get the contract name
                        String contractName = getQualifiedBlkName(contractNode);
                        
                        for(LustreContract contract : this.lustreProgram.contracts) {
                            if(contract.name.equals(contractName)) {
                                // Reorganize the inputs and outputs of a contract
                                List<String>        inHdls      = blkNodeToSrcBlkHdlsMap.get(contractNode);
                                List<LustreVar>     newOutputs  = new ArrayList<>();
                                List<LustreVar>     oldInputs   = contract.inputs;                                
                                List<LustreVar>     newInputs   = new ArrayList<>();                                
                                
                                for(int j = 0; j < inHdls.size(); j++) {
                                    if(inHdls.get(j).equals(src)) {
                                        newOutputs.add(oldInputs.get(j));
                                    } else {
                                        newInputs.add(oldInputs.get(j));
                                    }                                    
                                }
                                contract.inputs     = newInputs;
                                contract.outputs    = newOutputs;
                                break;
                            }
                        }
                        
                        // Attach the contract with contractName to the node with handle "src"
                        this.lustreProgram.attachNodeContract(getQualifiedBlkName(hdlBlkNodeMap.get(src)), contractName);
                    }                    
                } else {
                    LOGGER.log(Level.SEVERE, "Found a ghost contract: {0}", getBlkName(contractNode));
                }
            }
        }
    }   
    
    
    /**
     * Populate the maps with the source and destination handles of the input node
     * @param node
     * @param blkNodeToSrcBlkHandlesMap
     * @param blkNodeToSrcBlkPortMap
     * @param blkNodeToDstBlkHandlesMap
     */
    protected void populateConnectivitiesDS(JsonNode node, Map<JsonNode, List<String>> blkNodeToSrcBlkHandlesMap, Map<JsonNode, List<Integer>> blkNodeToSrcBlkPortMap, Map<JsonNode, List<String>> blkNodeToDstBlkHandlesMap) {
        if(node.has(CONNECTIVITY)) {
            // Get the connectivities of node
            JsonNode        portConns       = node.get(CONNECTIVITY);
            List<String>    srcBlkHdls      = new ArrayList<>();
            List<Integer>   srcBlkPorts     = new ArrayList<>();
            List<String>    dstBlkHdls      = new ArrayList<>();

            // Process an array portConns
            if(portConns.isArray()) {
                Iterator<JsonNode> portConnIt = portConns.elements();

                while(portConnIt.hasNext()) {
                    JsonNode connNode = portConnIt.next();
                    srcBlkHdls.addAll(convertJsonValuesToList(connNode.get(SRCBLOCK)));
                    srcBlkPorts.addAll(convertJsonValuesToListInt(connNode.get(SRCPORT)));
                    dstBlkHdls.addAll(convertJsonValuesToList(connNode.get(DSTBLOCK)));
                }
            // Process a normal portConns    
            } else {
                srcBlkHdls.addAll(convertJsonValuesToList(portConns.get(SRCBLOCK)));
                srcBlkPorts.addAll(convertJsonValuesToListInt(portConns.get(SRCPORT)));
                dstBlkHdls.addAll(convertJsonValuesToList(portConns.get(DSTBLOCK)));            
            }
            
            // Add to the maps
            if(!srcBlkHdls.isEmpty()) {
                blkNodeToSrcBlkHandlesMap.put(node, srcBlkHdls);                
            }
            if(!srcBlkPorts.isEmpty()) {
                blkNodeToSrcBlkPortMap.put(node, srcBlkPorts);
            }
            if(!dstBlkHdls.isEmpty()) {
                blkNodeToDstBlkHandlesMap.put(node, dstBlkHdls);    
            }             
        }        
    }
    
    /**
     * @param outportNodes
     * @param subsystemNode
     * @param blkNodeToSrcBlkHdlsMap
     * @param blkNodeToSrcBlkPortsMap
     * @param blkNodeToDstBlkHandlesMap
     * @param handleToBlkNodeMap
     * @return A list of Lustre equations corresponding to the outport nodes
     */    
    protected List<LustreEq> translateOutportEquations(List<JsonNode> outportNodes, JsonNode subsystemNode, Map<JsonNode, List<String>> blkNodeToSrcBlkHdlsMap, Map<JsonNode, List<Integer>> blkNodeToSrcBlkPortsMap, Map<JsonNode, List<String>> blkNodeToDstBlkHandlesMap, Map<String, JsonNode> handleToBlkNodeMap) {
        List<LustreEq>          eqs                 = new ArrayList<>();
        Set<JsonNode>           hasGroupped         = new HashSet<>();
        List<List<JsonNode>>    grouppedOutports    = new ArrayList<>();        
        
        // Group outports based on their incoming ports
        for(int i = 0; i < outportNodes.size(); i++) {
            JsonNode iPort = outportNodes.get(i);

            if(!hasGroupped.contains(iPort)) {                
                List<JsonNode>  group = new ArrayList<>();

                group.add(iPort);
                hasGroupped.add(iPort);
                
                for(int j = i+1; j < outportNodes.size(); j++) {
                    JsonNode jPort = outportNodes.get(j);

                    if(!hasGroupped.contains(jPort)) {
                        if(blkNodeToSrcBlkHdlsMap.containsKey(iPort) && blkNodeToSrcBlkHdlsMap.containsKey(jPort)) {
                            if(blkNodeToSrcBlkHdlsMap.get(iPort).equals(blkNodeToSrcBlkHdlsMap.get(jPort))) {
                                group.add(jPort);
                                hasGroupped.add(jPort);
                            }                            
                        }
                    }
                }  
                grouppedOutports.add(group);
            }
        }
        
        // For each groupped outputs, translate their equations
        for(List<JsonNode> gOutports : grouppedOutports) {
            eqs.addAll(translateOutportEquation(gOutports, subsystemNode, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap));                                                
        }        
        return eqs;
    }

    
    /**
     * 
     * @param propBlk is a subsystem observer block
     * @param subsystemNode
     * @param blkNodeToSrcBlkHdlsMap
     * @param blkNodeToDstBlkHandlesMap
     * @param handleToBlkNodeMap
     * @return A property equation
     */
    protected List<LustreEq> translatePropNode(JsonNode propBlk, JsonNode subsystemNode, Map<JsonNode, List<String>> blkNodeToSrcBlkHdlsMap, Map<JsonNode, List<Integer>> blkNodeToSrcBlkPortsMap, Map<JsonNode, List<String>> blkNodeToDstBlkHandlesMap, Map<String, JsonNode> handleToBlkNodeMap) {
        // All the properties equations
        List<LustreEq> propEqs = new ArrayList<>();

        if(propBlk.has(BLOCKTYPE)) {
            // Get the block type
            String srcBlkType = propBlk.get(BLOCKTYPE).asText();

            if(srcBlkType.equals(SUBSYSTEM)) {
                // Handle to outport variable mapping
                Map<String, VarIdExpr>  hdlToOutportVarExpr     = new HashMap<>();
                
                // Actual input expressions
                List<LustreExpr>        actualInputExpr         = new ArrayList<>();
                
                // Outport nodes
                List<JsonNode>          outportNodes            = getBlksFromSubSystemByType(propBlk, OUTPORT);
                
                // In handles and in ports
                List<String>            inHdls                  = blkNodeToSrcBlkHdlsMap.get(propBlk);
                List<Integer>           inPorts                 = blkNodeToSrcBlkPortsMap.get(propBlk);
                
                // Create a mapping from handle to variable expression
                for(JsonNode outNode : outportNodes) {
                    hdlToOutportVarExpr.put(getBlkHandle(outNode), new VarIdExpr(getQualifiedBlkName(propBlk)+"_"+getBlkName(outNode)));
                }          
                
                // Translate the actual input of the propety block
                for(int j = 0; j < inHdls.size(); j++) {
                    actualInputExpr.add(translateBlock(true, inHdls.get(j), propBlk, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap, new HashSet<String>(), null, inPorts.get(j)));
                }             
                
                // For each property block node, translate it
                for(Map.Entry<String, LustreExpr> entry : translatePropNode(propBlk, actualInputExpr).entrySet()) {
                    propEqs.add(new LustreEq(hdlToOutportVarExpr.get(entry.getKey()), entry.getValue()));
                }                 
            } else {
                LOGGER.log(Level.SEVERE, "Unsupported property block type: {0}!", srcBlkType);
            }
        }        
        return propEqs;
    } 
    
    /**
     * Translate the actual observer block: inline the expressions as properties in the encapsulating node
     * @param propBlk
     * @param actualInputExpr
     * @return a map between the block handle and its corresponding Lustre expression in the observer block
     */
    protected Map<String, LustreExpr> translatePropNode(JsonNode propBlk, List<LustreExpr> actualInputExpr) {                
        LOGGER.log(Level.INFO, "Start translating the property block: {0}", getBlkName(propBlk));        
        /** All outport nodes of propBlk */
        List<JsonNode>                      outportNodes                = new ArrayList<>();
        
        /** Handle to property expression mapping */
        Map<String, LustreExpr>             hdlToPropExpr               = new HashMap<>();                        
        
        /** Input indexed by port position */
        Map<Integer, String>                indexedInportHdls           = new HashMap<>();
        
        /** A mapping between a handle and the block node */
        Map<String, JsonNode>               hdlToBlkNodeMap             = new HashMap<>();                
        
        /** A mapping between a block node and its src block handles */
        Map<JsonNode, List<String>>         blkNodeToSrcBlkHdlsMap      = new HashMap<>();  
        Map<JsonNode, List<Integer>>        blkNodeToSrcBlkPortsMap     = new HashMap<>();  
        
        /** A mapping between a block node and its dst block handles */
        Map<JsonNode, List<String>>         blkNodeToDstBlkHdlsMap      = new HashMap<>();         
        Map<String, LustreExpr>             hdlToActualInputExpr        = new HashMap<>();
        
        /** ALL the fields under content field of subsystem block */
        Iterator<Entry<String, JsonNode>>   contentFields               = propBlk.get(CONTENT).fields();
        
        /** Populate the data structures */
        while(contentFields.hasNext()) {
            Map.Entry<String, JsonNode> contField       = contentFields.next();   
            JsonNode                    contBlkNode     = contField.getValue();
            
            if(contBlkNode.has(BLOCKTYPE)) {
                String contBlkType = contBlkNode.get(BLOCKTYPE).asText();
                
                hdlToBlkNodeMap.put(getBlkHandle(contBlkNode), contBlkNode);                
                if(contBlkType.equals(OUTPORT)) {
                    outportNodes.add(contBlkNode);
                } else if(contBlkType.equals(INPORT)) {
                    indexedInportHdls.put(getBlkPortPosition(contBlkNode), getBlkHandle(contBlkNode));
                }
                // Top-level subsystem block does not have port connectivity field
                populateConnectivitiesDS(contBlkNode, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHdlsMap);                
            }            
        }
        populateConnectivitiesDS(propBlk, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHdlsMap);
        
        // Pair the index with actual input
        for(int i = 0; i < indexedInportHdls.size(); i++) {
            if(indexedInportHdls.containsKey(i)) {
                hdlToActualInputExpr.put(indexedInportHdls.get(i), actualInputExpr.get(i));
            }
        }
        
        // Assume outports of an observer block can only be Boolean type
        for(JsonNode outportNode : outportNodes) {
            if(blkNodeToSrcBlkHdlsMap.containsKey(outportNode)) {
                List<String> inHdls     = blkNodeToSrcBlkHdlsMap.get(outportNode);
                List<Integer> inPorts   = blkNodeToSrcBlkPortsMap.get(outportNode);
                
                // Translate each in-handle
                for(int j = 0 ; j < inHdls.size(); j++) {
                    hdlToPropExpr.put(getBlkHandle(outportNode), translateBlock(false, inHdls.get(j), outportNode, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap, new HashSet<String>(), hdlToActualInputExpr, inPorts.get(j)));
                }
            } else {
                LOGGER.log(Level.WARNING, "A ghost outport in an observer block: {0}", getBlkName(outportNode));
            }
        }
        return hdlToPropExpr;
    }
    
    /**
     * 
     * @param outportNodes
     * @param subsystemNode
     * @param blkNodeToSrcBlkHdlsMap
     * @param blkNodeToSrcBlkPortsMap
     * @param blkNodeToDstBlkHdlsMap
     * @param handleToBlkNodeMap
     * @return The Lustre expression correspond to the outports
     */
    protected List<LustreEq> translateOutportEquation(List<JsonNode> outportNodes, JsonNode subsystemNode, Map<JsonNode, List<String>> blkNodeToSrcBlkHdlsMap, Map<JsonNode, List<Integer>> blkNodeToSrcBlkPortsMap, Map<JsonNode, List<String>> blkNodeToDstBlkHdlsMap, Map<String, JsonNode> handleToBlkNodeMap) {
        // The final equations
        List<LustreEq> eqs = new ArrayList<>();
        
        if(outportNodes != null) {
            if(outportNodes.size() == 1) {
                // The only outport node
                JsonNode outportNode = outportNodes.get(0);       

                if(blkNodeToSrcBlkHdlsMap.containsKey(outportNode)) {                    
                    LustreExpr       varIdExpr      = new VarIdExpr(getBlkName(outportNode));                 
                    List<String>    srcBlkHandles   = blkNodeToSrcBlkHdlsMap.get(outportNode);
                    List<Integer>   srcBlkPorts     = blkNodeToSrcBlkPortsMap.get(outportNode);

                    if(srcBlkHandles.size() == 1 && !srcBlkHandles.get(0).equals("-1")) {
                        eqs.add(new LustreEq(varIdExpr, translateBlock(false, srcBlkHandles.get(0), subsystemNode, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHdlsMap, handleToBlkNodeMap, new HashSet<String>(), null, srcBlkPorts.get(0))));
                    } else {
                        LOGGER.log(Level.SEVERE, "Unexpected: No src blocks connect to the outport: {0}!", getBlkName(outportNode));
                    }
                } else {
                    LOGGER.log(Level.WARNING, "Unexpected: No src blocks connect to the outport: {0}!", getBlkName(outportNode));
                }            
            } else if(outportNodes.size() > 1) {
                // Need to make sure the order of outports is correct
                List<LustreExpr>    orderedOutportVars      = new ArrayList<>();
                List<String>        srcBlkHdls              = blkNodeToSrcBlkHdlsMap.get(outportNodes.get(0));
                List<Integer>       srcBlkPorts             = blkNodeToSrcBlkPortsMap.get(outportNodes.get(0));
                JsonNode            srcBlk                  = handleToBlkNodeMap.get(srcBlkHdls.get(0));
                List<String>        dstBlkHdls              = blkNodeToDstBlkHdlsMap.get(srcBlk);
                LustreExpr          rhs                     = translateBlock(false, srcBlkHdls.get(0), subsystemNode, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHdlsMap, handleToBlkNodeMap, new HashSet<String>(), null, srcBlkPorts.get(0));

                for(JsonNode outportNode : outportNodes) {
                    orderedOutportVars.add(new VarIdExpr(getBlkName(outportNode)));
                }                
                if(isSubsystemBlock(srcBlk) && getBlksFromSubSystemByType(srcBlk, OUTPORT).size()>1) {
                    eqs.add(new LustreEq(orderedOutportVars, rhs));
                } else {
                    eqs.add(new LustreEq(orderedOutportVars.get(0), rhs));
                    for(int i = 1; i < orderedOutportVars.size(); i++) {
                        eqs.add(new LustreEq(orderedOutportVars.get(i), orderedOutportVars.get(0)));
                    }
                }           
            }              
        }      
        return eqs;
    }         

    /**
     * 
     * @param isPropBlk
     * @param blkHdl
     * @param parentSubsystemNode
     * @param blkNodeToSrcBlkHdlsMap
     * @param blkNodeToSrcBlkPortsMap
     * @param blkNodeToDstBlkHdlsMap
     * @param hdlToBlkNodeMap
     * @param visitedHdls
     * @param hdlToActualInputExpr
     * @param portNum
     * @return The Lustre expression corresponding to the input JSON block node
     */

    protected LustreExpr translateBlock(boolean isPropBlk, String blkHdl, JsonNode parentSubsystemNode, Map<JsonNode, List<String>> blkNodeToSrcBlkHdlsMap, Map<JsonNode, List<Integer>> blkNodeToSrcBlkPortsMap, Map<JsonNode, List<String>> blkNodeToDstBlkHdlsMap, Map<String, JsonNode> hdlToBlkNodeMap, Set<String> visitedHdls, Map<String, LustreExpr> hdlToActualInputExpr, int portNum) {        
        // The block node corresponding to blkHdl
        JsonNode    blkNode     = null;
        // The Lustre expreesion for blkNode
        LustreExpr  blkExpr     = null;
                
        // Substitute symbolic blocks with concrete ones
        if(hdlToActualInputExpr != null && hdlToActualInputExpr.containsKey(blkHdl)) {
            return hdlToActualInputExpr.get(blkHdl);
        }            
        
        // Get the actual block
        if(hdlToBlkNodeMap.containsKey(blkHdl)) {
            blkNode = hdlToBlkNodeMap.get(blkHdl);
        } else {
            LOGGER.log(Level.SEVERE, "Unexpected: no block in the model with handle {0}", blkHdl);
        }
        
        LOGGER.log(Level.INFO, "Start translating block: {0}", getBlkName(blkNode));
        
        // This is to simplify the translation for the case where the property block node connect with an outport.
        // Instead of translating the outport's definition, we will directly use the outport variable.
        if(blkNodeToDstBlkHdlsMap.containsKey(blkNode) && isPropBlk) {
            List<String>        dstHdls     = blkNodeToDstBlkHdlsMap.get(blkNode);
            List<LustreExpr>    outportExpr = new ArrayList<>();
            
            for(String hdl : dstHdls) {
                JsonNode dstNode = hdlToBlkNodeMap.get(hdl);
                
                if(dstNode.get(BLOCKTYPE).asText().equals(OUTPORT)) {
                    outportExpr.add(new VarIdExpr(getBlkName(dstNode)));
                }
            }
            if(!outportExpr.isEmpty()) {
                return outportExpr.get(portNum);
            }
        } 
        
        // Start translate the block node expression
        if(blkNode != null && blkNode.has(BLOCKTYPE)) {
            String              blkType     = blkNode.get(BLOCKTYPE).asText();            
            List<LustreExpr>    inExprs     = new ArrayList<>();            
            List<String>        inHdls      = new ArrayList<>();
            List<Integer>       inPorts     = new ArrayList<>();
            
            // Get the source blocks' handles
            if(blkNodeToSrcBlkHdlsMap.containsKey(blkNode)) {
                inHdls  = blkNodeToSrcBlkHdlsMap.get(blkNode);
                inPorts = blkNodeToSrcBlkPortsMap.get(blkNode);
            }
            
            // Get the block type
            if(blkType.toLowerCase().equals(LOGIC) || blkType.toLowerCase().equals(RELATIONALOP) || blkType.equals(MATH)) {
                blkType = blkNode.get(OPERATOR).asText();
            } else if(blkNode.has(LUSTREOPERATORBLK)){
                blkType = blkNode.get(LUSTREOPERATORBLK).asText();
            } else if(isIfActionSubsystem(blkNode)) {
                blkType = IFACTIONBLK;
            } else if(isCompareToConst(blkNode)) {
                blkType = COMTOCONST;
            } else if(isCompareToZero(blkNode)) {
                blkType = COMTOZERO;
            } 
            
            // Inputs to Merge, Memory, UnitDelay, IFACTIONBLOCK, and Subsystem are translated separately
            if(!blkType.equals(MERGE) && !blkType.equals(MEMORY) && !blkType.equals(UNITDELAY) && !blkType.equals(IFACTIONBLK) && !blkType.equals(SUBSYSTEM)) {              
                for(int i = 0; i < inHdls.size(); i++) {
                    inExprs.add(translateBlock(isPropBlk, inHdls.get(i), parentSubsystemNode, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap, visitedHdls, hdlToActualInputExpr, inPorts.get(i)));
                }               
            }                         
            switch(blkType) {
                case EQ: {
                    tryLiftingExprTypes(inExprs, inHdls, hdlToBlkNodeMap);
                    blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.EQ, inExprs.get(1));                                       
                    break;
                }  
                case NEQ: {
                    tryLiftingExprTypes(inExprs, inHdls, hdlToBlkNodeMap);
                    blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.NEQ, inExprs.get(1));                                       
                    break;
                } 
                case GTE: {
                    enforceCorrectTyping(inExprs, inHdls, hdlToBlkNodeMap);
                    blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.GTE, inExprs.get(1));                                       
                    break;
                } 
                case LTE: {  
                    enforceCorrectTyping(inExprs, inHdls, hdlToBlkNodeMap);
                    blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.LTE, inExprs.get(1));                                       
                    break;
                }  
                case GT: { 
                    enforceCorrectTyping(inExprs, inHdls, hdlToBlkNodeMap);
                    blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.GT, inExprs.get(1));                                       
                    break;
                }  
                case LT: {  
                    enforceCorrectTyping(inExprs, inHdls, hdlToBlkNodeMap);
                    blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.LT, inExprs.get(1));                                       
                    break;
                }                  
                case NOT: { 
                    tryLoweringExprTypes(inExprs, inHdls, hdlToBlkNodeMap);
                    blkExpr = new UnaryExpr(UnaryExpr.Op.NOT, inExprs.get(0));                                      
                    break;
                }                   
                case OR: {
                    tryLoweringExprTypes(inExprs, inHdls, hdlToBlkNodeMap);
                    blkExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.OR, inExprs.get(i));   
                    }                                        
                    break;
                }
                case NOR: { 
                    tryLoweringExprTypes(inExprs, inHdls, hdlToBlkNodeMap);
                    blkExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        blkExpr = new UnaryExpr(UnaryExpr.Op.NOT, new BinaryExpr(blkExpr, BinaryExpr.Op.OR, inExprs.get(i)));   
                    }                                        
                    break;
                }                
                case XOR: { 
                    tryLoweringExprTypes(inExprs, inHdls, hdlToBlkNodeMap);
                    blkExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.XOR, inExprs.get(i));   
                    }                                        
                    break;
                }                
                case NXOR: { 
                    tryLoweringExprTypes(inExprs, inHdls, hdlToBlkNodeMap);
                    blkExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        blkExpr = new UnaryExpr(UnaryExpr.Op.NOT, new BinaryExpr(blkExpr, BinaryExpr.Op.XOR, inExprs.get(i)));   
                    }                                        
                    break;
                }                  
                case AND: { 
                    tryLoweringExprTypes(inExprs, inHdls, hdlToBlkNodeMap);
                    blkExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.AND, inExprs.get(i));   
                    }                                        
                    break;
                } 
                case NAND: { 
                    tryLoweringExprTypes(inExprs, inHdls, hdlToBlkNodeMap);
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
                case POW: {
                    blkExpr = new NodeCallExpr(POW, inExprs);
                    break;
                }
                case EXP: {
                    blkExpr = new NodeCallExpr(EXP, inExprs);
                    break;
                } 
                case LOG10: {
                    blkExpr = new NodeCallExpr(LOG10, inExprs);
                    break;
                } 
                case LOG: {
                    blkExpr = new NodeCallExpr(LOG, inExprs);
                    break;
                }
                case TENTOU: {
                    List<LustreExpr> exprs = new ArrayList<>();
                    
                    if(getBlkOutportType(blkNode) == PrimitiveType.INT) {
                        exprs.add(new IntExpr(new BigInteger("10")));
                    } else if(getBlkOutportType(blkNode) == PrimitiveType.REAL) {
                        exprs.add(new RealExpr(new BigDecimal("10.0")));
                    }
                    exprs.add(inExprs.get(0));
                    blkExpr = new NodeCallExpr(POW, exprs);
                    break;
                }                
                case SQUARE: {
                    blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.MULTIPLY, inExprs.get(0));
                    break;
                }
                case RECIPROCAL: {
                    LustreType inType = getBlkOutportType(hdlToBlkNodeMap.get(inHdls.get(0)));
                    
                    if(inType == PrimitiveType.INT) {
                        blkExpr = new BinaryExpr(new IntExpr(BigInteger.ONE), BinaryExpr.Op.DIVIDE, inExprs.get(0));
                    } else if(inType == PrimitiveType.REAL) {
                        blkExpr = new BinaryExpr(new RealExpr(new BigDecimal("1.0")), BinaryExpr.Op.DIVIDE, inExprs.get(0));
                    } else {
                        LOGGER.log(Level.SEVERE, "Unexpected input type to reciprocal operator: {0}", inType);
                    }
                    break;
                }                
                case LSQRT:
                case SQRT: {
                    String op = blkNode.get(OPERATOR).asText();
                    
                    switch (op) {
                        case SQRTOP: {
                            blkExpr = new NodeCallExpr(getOrCreateLustreLibNode(blkNode), inExprs.get(0));
                            break;
                        }
                        case RSQRT: {
                            blkExpr = new BinaryExpr(new RealExpr(new BigDecimal("1.0")), BinaryExpr.Op.DIVIDE, new NodeCallExpr(getOrCreateLustreLibNode(blkNode), inExprs.get(0)));
                            break;
                        }
                        case SIGNEDSQRT: {      
                            blkExpr = new IteExpr(new BinaryExpr(inExprs.get(0), BinaryExpr.Op.GTE, new RealExpr(new BigDecimal("0.0"))), new NodeCallExpr(getOrCreateLustreLibNode(blkNode), inExprs.get(0)), new UnaryExpr(UnaryExpr.Op.NEG, new NodeCallExpr(getOrCreateLustreLibNode(blkNode), inExprs.get(0))));
                            break;
                        }
                        default:
                            LOGGER.log(Level.SEVERE, "Unexpected SQRT operator: {0}", op);
                            break;                    
                    }                   
                    break;                    
                }
                case MOD: {
                    for(String hdl : inHdls) {
                        if(getBlkOutportType(hdlToBlkNodeMap.get(hdl)) == PrimitiveType.REAL) {
                            LOGGER.log(Level.SEVERE, "Unexpected: input {0} to MOD operator is REAL type!", getBlkName(hdlToBlkNodeMap.get(hdl)));
                        }
                    }
                    blkExpr = inExprs.get(0);
                    for(int i = 1; i < inExprs.size(); i++) {
                        blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.MOD, inExprs.get(i));   
                    }                                        
                    break;                  
                } 
                case UNARYMINUS: {
                    enforceTargetType(inExprs, inHdls, getBlkOutportType(blkNode), hdlToBlkNodeMap);
                    blkExpr = new UnaryExpr(UnaryExpr.Op.NEG, inExprs.get(0));                                                                           
                    break;                  
                }
                case ROUNDING: {
                    blkExpr = new NodeCallExpr(getOrCreateLustreLibNode(blkNode), inExprs.get(0));
                    break;
                }
                case DATATYPECONVERSION: {
                    blkExpr = new NodeCallExpr(getOrCreateLustreLibNode(blkNode), inExprs.get(0));
                    break;
                }
                case MUX: {
                    blkExpr = mkMuxExpr(blkNode, inExprs);
                    break;
                }
                case DEMUX: {
                    blkExpr = mkDemuxExpr(blkNode, inExprs);
                    break;
                }                
                case SUM: {
                    blkExpr = mkSumExpr(blkNode, inExprs);
                    break;
                }
                case COMTOZERO: {
                    LustreExpr      constExpr   = null;        
                    LustreType      type        = getBlkInportType(blkNode);                          
                                        
                    if(type == PrimitiveType.INT) {
                        constExpr = new IntExpr(BigInteger.ZERO);
                    } else if(type == PrimitiveType.REAL) {
                        constExpr = new RealExpr(new BigDecimal("0.0"));
                    } else {
                        LOGGER.log(Level.SEVERE, "Unhandled case: compre to zero with type: {0}", type);
                    }
                    blkExpr = mkCompareToConst(blkNode, constExpr, inExprs.get(0));
                    break;
                }
                case COMTOCONST: {                                  
                    blkExpr = mkCompareToConst(blkNode, getLustreConst(blkNode.get(CONST).asText(), getBlkInportType(blkNode)), inExprs.get(0));
                    break;
                }                
                case SUBSYSTEM: {
                    if(this.auxHdlToExprMap.containsKey(blkHdl)) {
                        List<LustreExpr> exprs = this.auxHdlToExprMap.get(blkHdl);
                        
                        if(exprs != null) {
                            blkExpr = exprs.get(portNum);
                        } else {
                            LOGGER.log(Level.SEVERE, "Unexpected null expressions with handle: {0}", blkHdl);
                        }
                    } else {            
                        String                  blkName     = getBlkName(blkNode);
                        List<LustreExpr>        vars        = new ArrayList<>();
                        List<LustreExpr>        inputs      = new ArrayList<>();
                        List<LustreVar>         inputVars   = new ArrayList<>();
                        List<LustreVar>         outputVars  = new ArrayList<>();
                        Map<Integer, String>    portNameMap = getBlkOutportNameAndPort(blkNode);
                        
                        if(visitedHdls != null && !visitedHdls.contains(blkHdl)) { 
                            visitedHdls.add(blkHdl);
                            for(int j = 0; j < inHdls.size(); j++) {
                                inExprs.add(translateBlock(isPropBlk, inHdls.get(j), parentSubsystemNode, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap, visitedHdls, hdlToActualInputExpr, inPorts.get(j)));
                                
                                // If the blkNode is a contract block, we need to treat it specially for the case
                                // where the subsystem returns multiple outputs
                                if( isContractBlk(parentSubsystemNode) ) {
                                    String varName = "input_"+j;
                                    inputVars.add(new LustreVar(varName, getBlkOutportType(hdlToBlkNodeMap.get(inHdls.get(j)))));   
                                    inputs.add(new VarIdExpr(varName));
                                }                                
                            }                                              
                            LustreExpr              nodeCall    = new NodeCallExpr(getQualifiedBlkName(blkNode), inExprs);
                            List<LustreType>        types       = getBlockOutportTypes(blkNode);
                            
                            for(int i = 0; i < types.size(); i++) {
                                LustreVar var = new LustreVar(blkName+"_"+sanitizeName(portNameMap.get(i+1)), types.get(i));
                                
                                vars.add(new VarIdExpr(blkName+"_"+sanitizeName(portNameMap.get(i+1))));
                                this.auxNodeLocalVars.add(var);  
                                
                                if(isContractBlk(parentSubsystemNode)) {
                                    addMappingInfo(getPath(blkNode), null, getBlkName(parentSubsystemNode), null, null, null, var.name);
                                } else {
                                    addMappingInfo(getPath(blkNode), getBlkName(parentSubsystemNode), null, null, null, null, var.name);
                                }                                
                                outputVars.add(var);
                            }   
                            // This is to create auxiliary nodes for node calls in contract that returns multiple output
                            if(vars.size() >= 2 && isContractBlk(parentSubsystemNode)) {
                                int n = 1;
                                String              nodeName = getQualifiedBlkName(blkNode);
                                List<LustreExpr>    newVars = new ArrayList<>();
                                
                                for(int m = 0; m < vars.size(); ++m) {                                    
                                    List<LustreEq>      eqs         = new ArrayList<>();                                     
                                    List<LustreExpr>    elements    = new ArrayList<>();
                                    
                                    for(int l = 0; l < vars.size(); ++l) {
                                        if(m==l) {
                                            elements.add(new VarIdExpr(outputVars.get(m).name));
                                        } else {
                                            elements.add(new VarIdExpr("_"));
                                        }
                                    }
                                    eqs.add(new LustreEq(new TupleExpr(elements), new NodeCallExpr(getQualifiedBlkName(blkNode), inputs)));                                    
                                    LustreNode auxNode = new LustreNode(nodeName+"_"+(m+1), inputVars, Arrays.asList(outputVars.get(m)), eqs);
                                    newVars.add(new NodeCallExpr(auxNode.name, inExprs));
                                    this.auxLusNode.add(auxNode);
                                }
                                this.auxHdlToExprMap.put(blkHdl, newVars);
                                blkExpr = newVars.get(portNum);   
                            } else if(vars.size() >= 2){                                                         
                                this.auxHdlToExprMap.put(blkHdl, vars);
                                this.auxNodeEqs.add(new LustreEq(vars, nodeCall));
                                blkExpr = new TupleExpr(vars);
                            } else {
                                this.auxHdlToExprMap.put(blkHdl, vars);
                                this.auxNodeEqs.add(new LustreEq(vars, nodeCall));
                                blkExpr = vars.get(portNum);                                   
                            }
                        } else {                                                        
                            for(int k = 0; k < portNameMap.size(); k++) {
                                vars.add(new VarIdExpr(blkName+"_"+sanitizeName(portNameMap.get(k+1))));
                            }
                            blkExpr = vars.get(portNum);
                        }                       
                    }                                     
                    break;
                }
                case INPORT: {
                    blkExpr = new VarIdExpr(getBlkName(blkNode));
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
                        LOGGER.log(Level.SEVERE, "Unexpected constant value in JSON node: {0}", blkNode);
                    }
                    break;
                }
                case PRODUCT: {
                    blkExpr = mkProductExpr(blkNode, inExprs);
                    break;
                }
                case GAIN: {
                    String gain = blkNode.get(GAIN).asText();
                    
                    tryLiftingExprTypes(inExprs, inHdls, hdlToBlkNodeMap);
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
                        tryLiftingExprTypes(inExprs, inHdls, hdlToBlkNodeMap);
                        blkExpr = new NodeCallExpr(getOrCreateLustreLibNode(blkNode), inExprs);
                    }
                    break;
                }
                case ARROW: {
                    blkExpr = new BinaryExpr(inExprs.get(0), BinaryExpr.Op.ARROW, inExprs.get(1));
                    break;
                }
                case SWITCH: {
                    blkExpr = mkSwitchExpr(blkNode, inExprs, inHdls, hdlToBlkNodeMap);
                    break;
                }               
                case MEMORY: 
                case UNITDELAY: {
                    if(this.auxHdlToPreExprMap.containsKey(blkHdl)) {
                        blkExpr = this.auxHdlToPreExprMap.get(blkHdl);
                    } else {
                        String      varName     = getBlkName(blkNode);     
                        VarIdExpr   preVarId    = new VarIdExpr(varName);
                        
                        if(visitedHdls != null && !visitedHdls.contains(blkHdl)) { 
                            visitedHdls.add(blkHdl);
                            LustreExpr  inExpr      = translateBlock(isPropBlk, inHdls.get(0), parentSubsystemNode, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap, visitedHdls, hdlToActualInputExpr, inPorts.get(0));
                            String      init        = blkType.equals(MEMORY) ? blkNode.get(X0).asText() : blkNode.get(INITCOND).asText();                                        
                            LustreType  blkOutType  = getBlkOutportType(blkNode); 
                            LustreExpr  initExpr    = getLustreConst(init, blkOutType);                        
                            LustreExpr  preBlkExpr  = new UnaryExpr(UnaryExpr.Op.PRE, inExpr);                      

                            if(initExpr != null) {
                                preBlkExpr = new BinaryExpr(initExpr, BinaryExpr.Op.ARROW, preBlkExpr);
                            } else {
                                LOGGER.log(Level.SEVERE, "UNEXPECTED init value : {0} for memory or unit delay block", init);
                            }       
                            if(isContractBlk(parentSubsystemNode)) {
                                addMappingInfo(getPath(blkNode), null, getBlkName(parentSubsystemNode), null, null, null, varName);
                            } else {
                                addMappingInfo(getPath(blkNode), getBlkName(parentSubsystemNode), null, null, null, null, varName);
                            }                            
                            this.auxNodeLocalVars.add(new LustreVar(varName, blkOutType));
                            this.auxNodeEqs.add(new LustreEq(preVarId, preBlkExpr));
                            this.auxHdlToPreExprMap.put(blkHdl, preVarId);                            
                        }
                        blkExpr = preVarId;
                    }
                    break;
                }
                case IFACTIONBLK: {
                    JsonNode ifBlk = null;
                    
                    for(int i = 0; i < inHdls.size(); i++) {
                        JsonNode inBlk = hdlToBlkNodeMap.get(inHdls.get(i));
                        
                        if(isIfBlock(inBlk)) {
                            ifBlk = inBlk;
                        } else {
                            inExprs.add(translateBlock(isPropBlk, inHdls.get(i), parentSubsystemNode, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap, visitedHdls, hdlToActualInputExpr, inPorts.get(i)));                   
                        }                        
                    } 
                    if(ifBlk != null) {
                        String              actSystName     = getQualifiedBlkName(blkNode);                        
                        LustreExpr          condExpr        = getCondExprFromIfBlk(isPropBlk, ifBlk, blkHdl, parentSubsystemNode, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap);
                        String              initStates      = getInitilizeStates(blkNode);
                        LustreExpr          actSysCall      = new NodeCallExpr(actSystName, inExprs);                        
                        List<LustreExpr>     actSysOuts      = new ArrayList<>();
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
                                if(isContractBlk(parentSubsystemNode)) {
                                    addMappingInfo(getPath(blkNode), null, getBlkName(parentSubsystemNode), null, null, null, varName);
                                } else {
                                    addMappingInfo(getPath(blkNode), getBlkName(parentSubsystemNode), null, null, null, null, varName);
                                }                                 
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
                // In Simulink, we translated IF block with IfActionSubsystem and MERGE blocks together.
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
                    
                    for(String inHandle : inHdls) {
                        JsonNode inBlk = hdlToBlkNodeMap.get(inHandle);
                        
                        if(isIfActionSubsystem(inBlk)) {
                            hdlActSysNodeMap.put(inHandle, inBlk);
                        } else {
                            LOGGER.log(Level.SEVERE, "UNSUPPORTED: input to MERGE block is not a IF-ACTION-SUBSYSTEM!", blkType);
                        }
                    }
                    if(hdlActSysNodeMap.size() >= 2 && hdlActSysNodeMap.size() == inHdls.size()) {
                        // Translate each action block into a node call
                        for(Entry<String, JsonNode> entry : hdlActSysNodeMap.entrySet()) {
                            JsonNode actSubsystemNode = entry.getValue();
                            if(blkNodeToSrcBlkHdlsMap.containsKey(actSubsystemNode)) {
                                List<LustreExpr>    srcExprs    = new ArrayList<>();
                                List<String>        srcHandles  = blkNodeToSrcBlkHdlsMap.get(actSubsystemNode);                                
                                
                                for(int i = 0; i < srcHandles.size(); i++) {
                                    if(isIfBlock(hdlToBlkNodeMap.get(srcHandles.get(i)))) {
                                        if(ifBlkHandle == null) {
                                            ifBlkHandle = srcHandles.get(i);
                                        } else if(!ifBlkHandle.equals(srcHandles.get(i))){
                                            LOGGER.log(Level.SEVERE, "UNEXPECTED: IF-ACTION-SUBSYSTEMs to the same MERGE block connects with different IF blocks", srcHandles.get(i));
                                        }
                                    } else {
                                        srcExprs.add(translateBlock(isPropBlk, srcHandles.get(i), parentSubsystemNode, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap, visitedHdls, hdlToActualInputExpr, inPorts.get(i)));
                                    }                                    
                                }
                                hdlActSysExprMap.put(entry.getKey(), new NodeCallExpr(getQualifiedBlkName(actSubsystemNode), srcExprs));
                            }                            
                        }                        
                    }
                    if(hdlToBlkNodeMap.containsKey(ifBlkHandle)) {
                        blkExpr = translateIfBlock(isPropBlk, hdlToBlkNodeMap.get(ifBlkHandle), hdlActSysExprMap, parentSubsystemNode, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHdlsMap, hdlToBlkNodeMap);
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
    
    /**
     * @param blkNode
     * @param constExpr
     * @param inExpr
     * @return 
     */
    protected LustreExpr mkCompareToConst(JsonNode blkNode, LustreExpr constExpr, LustreExpr inExpr) {
        BinaryExpr.Op   bOp     = null;
        String          op      = blkNode.get(CONTENT).get(COMPARE).get(OPERATOR).asText();

        switch (op) {
            case "==":
                bOp = BinaryExpr.Op.EQ;
                break;
            case "~=":
                bOp = BinaryExpr.Op.NEQ;
                break;
            case ">=":
                bOp = BinaryExpr.Op.GTE;
                break;
            case "<=":
                bOp = BinaryExpr.Op.LTE;
                break;
            case ">":
                bOp = BinaryExpr.Op.GT;
                break;
            case "<":
                bOp = BinaryExpr.Op.LT;
                break;                                
            default:
                break;
        }
        return new BinaryExpr(inExpr, bOp, constExpr);
    }
    
    /**
     * @param blkNode
     * @param inExprs
     * @param inHdls
     * @param hdlToBlkNodeMap
     * @return 
     */
    protected LustreExpr mkSwitchExpr(JsonNode blkNode, List<LustreExpr> inExprs, List<String> inHdls, Map<String, JsonNode> hdlToBlkNodeMap) {
        LustreExpr  condExpr    = null;
        LustreType  condType    = getSwitchCondType(blkNode);

        if(condType == PrimitiveType.BOOL) {
            condExpr = inExprs.get(1);
        } else {                        
            String      threshold       = blkNode.get(THRESHOLD).asText();
            String      criteria        = blkNode.get(CRITERIA).asText();                        
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

        List<String>        newHandles = new ArrayList<>();
        List<LustreExpr>    newInExprs = new ArrayList<>();

        newInExprs.add(inExprs.get(0));
        newInExprs.add(inExprs.get(2));
        newHandles.add(inHdls.get(0));
        newHandles.add(inHdls.get(2));
        enforceTargetType(newInExprs, newHandles, getBlkOutportType(blkNode), hdlToBlkNodeMap);
        return new IteExpr(condExpr, newInExprs.get(0), newInExprs.get(1));        
    }

    /**
     * 
     * @param blkNode
     * @param inExprs
     * @return a Lustre expression for blkNode
     */    
    protected LustreExpr mkMuxExpr(JsonNode blkNode, List<LustreExpr> inExprs) {
        LustreType      baseType        = getBlkOutportType(blkNode);
        List<Integer>   inDimensions    = getInportDimensions(blkNode);
        List<Integer>   outDimensions   = getOutportDimensions(blkNode);
        String          muxName         = getFreshVar("mux_output");
        List<Integer>   newOutDims      = new ArrayList<>();
        List<LustreEq>  eqs             = new ArrayList<>();
        LustreExpr      lhs             = new VarIdExpr(muxName);
        int outDim = outDimensions.get(1);
        
        newOutDims.add(outDimensions.get(1));
        LustreVar muxVar = outDimensions.get(1)>1?new LustreVar(muxName, new ArrayType(baseType, newOutDims)):
                           new LustreVar(muxName, baseType);                
        if(outDim > 1) {            
            List<Integer> newInDims = new ArrayList<>();
            for(int i = 1; i < inDimensions.size();) {
                newInDims.add(inDimensions.get(i));
                i += 2;
            }
            for(int i = 0; i < outDim;) {
                for(int j = 0; j < inExprs.size(); ++j) {
                    if(newInDims.get(j) == 1) {
                        eqs.add(new LustreEq(new ArrayExpr(lhs, i), inExprs.get(j)));
                        ++i;
                    } else {
                        for(int k = 0; k < newInDims.get(j); ++k) {
                            eqs.add(new LustreEq(new ArrayExpr(lhs, i), new ArrayExpr(inExprs.get(j), k)));
                            ++i;
                        }                        
                    }                 
                }
            }
        } else {
            eqs.add(new LustreEq(lhs, inExprs.get(0)));
        }
        this.auxNodeEqs.addAll(eqs);
        this.auxNodeLocalVars.add(muxVar);
        return new VarIdExpr(muxName);
    }
    
    /**
     * 
     * @param blkNode
     * @param inExprs
     * @return 
     */    
    protected LustreExpr mkDemuxExpr(JsonNode blkNode, List<LustreExpr> inExprs) {
        LustreType      baseType        = getBlkOutportType(blkNode);
        List<Integer>   inDimensions    = getInportDimensions(blkNode);
        List<Integer>   outDimensions   = getOutportDimensions(blkNode);
        String          muxName         = getFreshVar("mux_output");
        List<Integer>   newOutDims      = new ArrayList<>();
        
        newOutDims.add(outDimensions.get(1));
        LustreVar muxVar = outDimensions.get(1)>1?new LustreVar(muxName, new ArrayType(baseType, newOutDims)):
                           new LustreVar(muxName, baseType);
        
        int outDim = outDimensions.get(1);
        
        if(outDim > 1) {            
            muxVar = new LustreVar(muxName, new ArrayType(baseType, inDimensions));
        } else {
            
        }
                
        return new VarIdExpr(muxName);
    }    
    
    /**
     * 
     * @param blkNode
     * @param inExprs
     * @return 
     */    
    protected LustreExpr mkProductExpr(JsonNode blkNode, List<LustreExpr> inExprs) {
        boolean oneDimProd = true;
        LustreExpr      blkExpr;        
        String          fcnName     = sanitizeName(getPath(blkNode));
        String          ops         = blkNode.get(INPUTS).asText();
        String          colMode     = blkNode.get(COLLAPSEMODE).asText();
        String          mult        = blkNode.get(MULTIPLICATIOIN).asText();
        List<Integer>       inDimensions    = getInportDimensions(blkNode);
        List<Integer>       outDimensions   = getOutportDimensions(blkNode);
        List<List<Integer>> newInDims       = new ArrayList<>();
        List<LustreType>    inBaseTypes     = getMatrixBlkInportType(blkNode);
        LustreType          outBaseType     = getBlkOutportType(blkNode);
        List<LustreExpr>    exprs           = new ArrayList<>();        
            
        // Get all the values of in-dimensions and create the sum function name
        for(int i = 0; i < inDimensions.size();) {
            int j = inDimensions.get(i);
            List<Integer> dimension = new ArrayList<>();         
            for(int k = i+1; k <= i+j; ++k) {
                dimension.add(inDimensions.get(k));
            }
            newInDims.add(dimension);
            i += (j+1);
        }
        // Get all operators
        if(ops.matches("\\d+")) {
            int numOfInuts = Integer.parseInt(ops);
            ops = "";
            for(int i = 0; i < numOfInuts; ++i) {
                ops += "*";
            }
        } else {
            ops = ops.replaceAll("\\|", "").trim().replaceAll(" ", "").trim();
        }
        
        for(int d : inDimensions) {
            if(d!=1) {
                oneDimProd = false;
                break;
            }
        }
        if(oneDimProd) {
            if(ops.charAt(0) == '/') {
                if(inBaseTypes.get(0) == PrimitiveType.INT) {
                    blkExpr = new BinaryExpr(new IntExpr(BigInteger.ONE), BinaryExpr.Op.DIVIDE, inExprs.get(0));
                } else {
                    blkExpr = new BinaryExpr(new RealExpr(new BigDecimal("1.0")), BinaryExpr.Op.DIVIDE, inExprs.get(0));
                }
                
            } else {
                blkExpr = inExprs.get(0);
            }
            for(int i = 1; i < ops.length(); ++i) {
                if(ops.charAt(i) == '/') {
                    blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.DIVIDE, inExprs.get(i));    
                } else {
                    blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.MULTIPLY, inExprs.get(i));    
                }
            }
            return blkExpr;
        }        
            
        if(mult.equals(MATRIX)) {
            // Create dimensions as input and output variables
            for(List<Integer> ds : newInDims) {
                for(int d : ds) {
                    exprs.add(new IntExpr(BigInteger.valueOf(d)));
                }
            }
            for(int i = 1; i < outDimensions.size(); ++i) {
                exprs.add(new IntExpr(BigInteger.valueOf(outDimensions.get(i))));
            }
            exprs.addAll(inExprs);
            
            if(!this.libNodeNameMap.containsKey(fcnName)) {
                List<String>    inVars  = new ArrayList<>();
                List<LustreVar> inputs  = new ArrayList<>();                
                List<LustreVar> outputs = new ArrayList<>();
                List<LustreEq>  eqs     = new ArrayList<>();
                List<LustreVar> dimVars  = new ArrayList<>();
                List<LustreVar> inputVars  = new ArrayList<>();
                
                for(List<Integer> dims : newInDims) {                    
                    String          name        = getFreshVar("m");                    
                    List<String>    tempVars    = new ArrayList<>();
                    
                    for(int d : dims) {
                        String dimVar = getFreshVar("i");
                        tempVars.add(dimVar);
                        dimVars.add(new LustreVar(dimVar, PrimitiveType.INTCONST));
                    }
                    inVars.add(name);
                    inputVars.add(new LustreVar(name, new ArrayType(tempVars, outBaseType)));
                }                
                int             outportD    = outDimensions.get(0);                
                List<String>    dimNames    = new ArrayList<>();

                for(int i = 1; i < outDimensions.size(); ++i) {
                    String dimVar = getFreshVar("i");
                    dimNames.add(dimVar);    
                    dimVars.add(new LustreVar(dimVar, PrimitiveType.INTCONST));
                }          
                outputs.add(new LustreVar(getFreshVar("m"), new ArrayType(dimNames, outBaseType)));
                inputs.addAll(dimVars);
                inputs.addAll(inputVars);
                
                LustreExpr      rhsExpr     = null;
                // The dimensions of outport
                List<String>    dimensions  = new ArrayList<>();
                
                for(int j = 0; j < outportD; ++j) {
                    dimensions.add(getFreshVar("i"));
                }   
                if(ops.charAt(0)=='/') {
                    rhsExpr = new BinaryExpr(outBaseType == PrimitiveType.INT? new IntExpr(BigInteger.ONE): 
                                                new RealExpr(new BigDecimal("1.0")), BinaryExpr.Op.DIVIDE, new ArrayExpr(inVars.get(0), dimensions));
                } else {
                    rhsExpr = new ArrayExpr(inVars.get(0), dimensions);
                }                
                for(int i = 1; i < ops.length(); ++i) {
                    switch (ops.charAt(i)) {
                        case '*': {
                            if(dimensions.isEmpty()) {
                                
                            } else {
                                rhsExpr = new BinaryExpr(rhsExpr, BinaryExpr.Op.MULTIPLY, new ArrayExpr(inVars.get(i), dimensions));
                            }                            
                            break;
                        }
                        case '/': {
                            if(dimensions.isEmpty()) {
                                
                            } else {
                                rhsExpr = new BinaryExpr(rhsExpr, BinaryExpr.Op.DIVIDE, new ArrayExpr(inVars.get(i), dimensions));
                            }                                                         
                            break;
                        }
                        default:
                            LOGGER.log(Level.SEVERE, "Unexpected operators in PRODUCT blocks parameters: {0}", ops.charAt(i));
                            break;
                    }                                        
                }
                eqs.add(new LustreEq(new ArrayExpr(outputs.get(0).name, dimensions), rhsExpr));
                this.lustreProgram.addNode(new LustreNode(fcnName, inputs, outputs, eqs));
                this.libNodeNameMap.put(fcnName, fcnName);
            }
            blkExpr = new NodeCallExpr(fcnName, exprs);
            
        // Handle element-wise product
        } else if(colMode.equals(ALLDIMENSIONS)){
            
            // If the input is just one matrix, collapse the matrix for all dimensions
            if(ops.length() == 1 && (newInDims.get(0).size() > 1 || newInDims.get(0).get(0) > 1)) {
                List<LustreExpr> blkExprs = new ArrayList<>();
                mkCollapseAllDimExpr(0, newInDims.get(0), ops.charAt(0), new ArrayList<Integer>(), blkExprs, inExprs.get(0), inBaseTypes.get(0));
                blkExpr = blkExprs.get(0);
            } else {
                // Make all the in(out)-dimmension constants as expressions
                // and make them as input
                for(List<Integer> ds : newInDims) {
                    for(int d : ds) {
                        exprs.add(new IntExpr(BigInteger.valueOf(d)));
                    }
                }
                for(int i = 1; i < outDimensions.size(); ++i) {
                    exprs.add(new IntExpr(BigInteger.valueOf(outDimensions.get(i))));
                }

                // Add all inexprs as input
                exprs.addAll(inExprs);     

                if(!this.libNodeNameMap.containsKey(fcnName)) {
                    List<String>    inVarNames      = new ArrayList<>();
                    List<LustreVar> inputs          = new ArrayList<>();
                    List<LustreVar> tempInputs      = new ArrayList<>();
                    List<LustreVar> outputs         = new ArrayList<>();
                    List<LustreEq>  eqs             = new ArrayList<>();
                    List<LustreVar> dimVars         = new ArrayList<>();
                    Set<String>     singleInVar     = new HashSet<>();

                    // Create input and dimension variables
                    for(List<Integer> dims : newInDims) {                    
                        String inVarName = getFreshVar("m");

                        // If the input has dimension of 1*1
                        // we create a variable for it but ignore its dimensions
                        if(dims.size() == 1 && dims.get(0) == 1) {
                            tempInputs.add(new LustreVar(inVarName, inBaseTypes.get(0)));
                            dimVars.add(new LustreVar(getFreshVar("i"), PrimitiveType.INTCONST));
                            singleInVar.add(inVarName);
                        } else {
                            List<String>    tempDimVars    = new ArrayList<>();

                            for(int d : dims) {
                                String dimVar = getFreshVar("i");
                                tempDimVars.add(dimVar);
                                dimVars.add(new LustreVar(dimVar, PrimitiveType.INTCONST));
                            }                        
                            tempInputs.add(new LustreVar(inVarName, new ArrayType(tempDimVars, outBaseType)));                        
                        }
                        inVarNames.add(inVarName);
                    }
                    // Get the value of out dimension and create output variables and dimmension values                             
                    List<String>    dimNames    = new ArrayList<>();
                    // The dimmensions that we are going to use for iterating the index 
                    List<String>    dimensions  = new ArrayList<>();       
                    
                    // Create the output variable
                    if(outDimensions.size() == 2 && outDimensions.get(1) == 1) {                         
                        dimVars.add(new LustreVar(getFreshVar("i"), PrimitiveType.INTCONST));
                        outputs.add(new LustreVar(getFreshVar("m"), outBaseType));
                    } else {
                        int outportD = outDimensions.get(0);   
                        for(int j = 0; j < outportD; ++j) {
                            dimensions.add(getFreshVar("i"));
                        }                    
                        for(int i = 1; i < outDimensions.size(); ++i) {
                            String dimVar = getFreshVar("i");
                            dimNames.add(dimVar);    
                            dimVars.add(new LustreVar(dimVar, PrimitiveType.INTCONST));
                        }                        
                        outputs.add(new LustreVar(getFreshVar("m"), new ArrayType(dimNames, outBaseType)));                    
                    }      
                    // Put dimension variables before actual array variables 
                    // because of the forward reference issue
                    inputs.addAll(dimVars);
                    inputs.addAll(tempInputs);

                    LustreExpr rhsExpr = outBaseType == PrimitiveType.INT? new IntExpr(BigInteger.ONE): new RealExpr(new BigDecimal("1.0"));

                    if(ops.charAt(0)=='/') {
                        if(singleInVar.contains(inVarNames.get(0))) {
                            rhsExpr = new BinaryExpr(rhsExpr, BinaryExpr.Op.DIVIDE, new VarIdExpr(inVarNames.get(0)));
                        } else {
                            rhsExpr = new BinaryExpr(rhsExpr, BinaryExpr.Op.DIVIDE, new ArrayExpr(inVarNames.get(0), dimensions)); 
                        }                    
                    } else {
                        if(singleInVar.contains(inVarNames.get(0))) {
                            rhsExpr = new VarIdExpr(inVarNames.get(0));
                        } else {
                            rhsExpr = new ArrayExpr(inVarNames.get(0), dimensions);
                        }                    
                    }                
                    for(int i = 1; i < ops.length(); ++i) {
                        switch (ops.charAt(i)) {
                            case '*': {    
                                if(singleInVar.contains(inVarNames.get(i))) {
                                    rhsExpr = new BinaryExpr(rhsExpr, BinaryExpr.Op.MULTIPLY, new VarIdExpr(inVarNames.get(i)));
                                } else {
                                    rhsExpr = new BinaryExpr(rhsExpr, BinaryExpr.Op.MULTIPLY, new ArrayExpr(inVarNames.get(i), dimensions));
                                }                             
                                break;
                            }
                            case '/': {
                                if(singleInVar.contains(inVarNames.get(i))) {
                                    rhsExpr = new BinaryExpr(rhsExpr, BinaryExpr.Op.DIVIDE, new VarIdExpr(inVarNames.get(i)));
                                } else {
                                    rhsExpr = new BinaryExpr(rhsExpr, BinaryExpr.Op.DIVIDE, new ArrayExpr(inVarNames.get(i), dimensions));
                                }                               
                                break;
                            }
                            default:
                                LOGGER.log(Level.SEVERE, "Unexpected operators in PRODUCT blocks parameters: {0}", ops.charAt(i));
                                break;
                        }                                        
                    }                  

                    if(dimensions.size() > 0) {
                        eqs.add(new LustreEq(new ArrayExpr(outputs.get(0).name, dimensions), rhsExpr));
                    } else {
                        eqs.add(new LustreEq(new VarIdExpr(outputs.get(0).name), rhsExpr));
                    }

                    this.lustreProgram.addNode(new LustreNode(fcnName, inputs, outputs, eqs));
                    this.libNodeNameMap.put(fcnName, fcnName);
                }
                blkExpr = new NodeCallExpr(fcnName, exprs);                        
            }            
        } else {
            LustreExpr one = null;
            char fstOp = ops.charAt(0);
            
            if(inBaseTypes.get(0) == PrimitiveType.INT) {
                one = new IntExpr(BigInteger.ONE);
            } else if(inBaseTypes.get(0) == PrimitiveType.REAL) {
                one = new RealExpr(new BigDecimal("1.0"));
            }
            blkExpr = one;      
            
            //Input is a int/real constant
            if(inDimensions.size() == 2 && inDimensions.get(1) == 1) {
                if(fstOp == '*') {
                    blkExpr = inExprs.get(0);
                } else {
                    blkExpr = new BinaryExpr(one, BinaryExpr.Op.DIVIDE, inExprs.get(0));
                }
            // Input is an one dimensional array     
            } else if(inDimensions.size() == 2) {
                for(int i = 0; i < inDimensions.get(1); ++i) {
                    if(fstOp == '*') {                        
                        blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.MULTIPLY, new ArrayExpr(inExprs.get(0), i));
                    } else {
                        blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.DIVIDE, new ArrayExpr(inExprs.get(0), i));
                    }                                    
                }
            } else {
                int                 colDim      = blkNode.get(COLLAPSEDIM).asInt();
                List<Integer>       outDims     = new ArrayList<>();
                List<LustreExpr>    rhsExprs    = new ArrayList<>();
                                
                for(int i = 1; i < outDimensions.size(); ++i) {
                    outDims.add(outDimensions.get(i));
                }

                mkCollapseSpecDimExpr(colDim-1, 0, fstOp, inExprs.get(0), inBaseTypes.get(0), newInDims.get(0), rhsExprs, new ArrayList<Integer>());
                blkExpr = new ArrayExpr(outDims, rhsExprs);                
                
            }             
        }
        return blkExpr;
    }
    
    
    /**
     * 
     * @param blkNode
     * @param inExprs
     * @return 
     */    
    protected LustreExpr mkSumExpr(JsonNode blkNode, List<LustreExpr> inExprs) {
        boolean    oneDimSum = true;
        LustreExpr blkExpr  = null;
        String  ops         = blkNode.get(INPUTS).asText();
        String  collapse    = blkNode.get(COLLAPSEMODE).asText();
        List<LustreType>    inBaseTypes     = getMatrixBlkInportType(blkNode);
        LustreType          outBaseType     = getBlkOutportType(blkNode);
        List<Integer>       inDimensions    = getInportDimensions(blkNode);
        List<Integer>       outDimensions   = getOutportDimensions(blkNode);            
        String              fcnName         = sanitizeName(getPath(blkNode));   
        List<List<Integer>> newInDims       = new ArrayList<>();        

        // Get all the values of in-dimensions and create the sum function name
        for(int i = 0; i < inDimensions.size();) {
            int j = inDimensions.get(i);
            List<Integer> dimension = new ArrayList<>();
        
            for(int k = i+1; k <= i+j; ++k) {
                dimension.add(inDimensions.get(k));
            }
            newInDims.add(dimension);
            i += (j+1);
        }      
        
        // Get the operators and process them
        if(ops.matches("\\d+")) {
            int numOfInuts = Integer.parseInt(ops);            
            if(numOfInuts != inExprs.size()) {
                LOGGER.log(Level.SEVERE, "The number of inputs does not match inputs");
            }
            ops = "";
            for(int i = 0; i < numOfInuts; ++i) {
                ops += "+";
            }
        } else {
            ops = ops.replaceAll("\\|", "").trim().replaceAll(" ", "").trim();
        }
        
        for(int d : inDimensions) {
            if(d!=1) {
                oneDimSum = false;
                break;
            }
        }
        if(oneDimSum) {
            if(ops.charAt(0) == '-') {
                blkExpr = new UnaryExpr(UnaryExpr.Op.NEG, inExprs.get(0));
            } else {
                blkExpr = inExprs.get(0);
            }
            for(int i = 1; i < ops.length(); ++i) {
                if(ops.charAt(i) == '+') {
                    blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.PLUS, inExprs.get(i));    
                } else {
                    blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.MINUS, inExprs.get(i));    
                }
            }
            return blkExpr;
        }
        
        //Todo: make sure the types are correct
        if(collapse.equals(ALLDIMENSIONS)) {
            List<LustreExpr>    exprs           = new ArrayList<>();
               
            // Collapse the matrix for all dimensions
            if(ops.length() == 1 && (newInDims.get(0).size() > 1 || newInDims.get(0).get(0) > 1)) {
                List<Integer> finalInDim = newInDims.get(0);
                List<LustreExpr> blkExprs = new ArrayList<>();
                mkCollapseAllDimExpr(0, finalInDim, ops.charAt(0), new ArrayList<Integer>(), blkExprs, inExprs.get(0), inBaseTypes.get(0));
                blkExpr = blkExprs.get(0);
            } else {
                // Make all the in(out)-dimmension constants as expressions
                // and make them as input
                for(List<Integer> ds : newInDims) {
                    for(int d : ds) {
                        exprs.add(new IntExpr(BigInteger.valueOf(d)));
                    }
                }
                for(int i = 1; i < outDimensions.size(); ++i) {
                    exprs.add(new IntExpr(BigInteger.valueOf(outDimensions.get(i))));
                }
                exprs.addAll(inExprs);      
                
                if(!this.libNodeNameMap.containsKey(fcnName)) {
                     // Dimensions
                    List<LustreVar> ds              = new ArrayList<>();
                    List<String>    inVarNames      = new ArrayList<>();
                    List<LustreVar> inputs          = new ArrayList<>();
                    List<LustreVar> locals          = new ArrayList<>();
                    List<LustreVar> tempInputs      = new ArrayList<>();
                    List<LustreVar> outputs         = new ArrayList<>();
                    List<LustreEq>  eqs             = new ArrayList<>();
                    List<LustreVar> dimVars         = new ArrayList<>();
                    Set<String>     singleInVar     = new HashSet<>();

                    // Create input and dimension variables
                    for(List<Integer> dims : newInDims) {                    
                        String inVarName = getFreshVar("m");

                        // If the input has dimension of 1*1
                        // we create a variable for it but ignore its dimensions
                        if(dims.size() == 1 && dims.get(0) == 1) {
                            tempInputs.add(new LustreVar(inVarName, inBaseTypes.get(0)));
                            dimVars.add(new LustreVar(getFreshVar("i"), PrimitiveType.INTCONST));
                            singleInVar.add(inVarName);
                        } else {
                            List<String>    tempDimVars    = new ArrayList<>();

                            for(int d : dims) {
                                String dimVar = getFreshVar("i");
                                tempDimVars.add(dimVar);
                                dimVars.add(new LustreVar(dimVar, PrimitiveType.INTCONST));
                            }                        
                            tempInputs.add(new LustreVar(inVarName, new ArrayType(tempDimVars, outBaseType)));                        
                        }
                        inVarNames.add(inVarName);
                    }
                    // Get the value of out dimension and create output variables and dimmension values                             
                    List<String>    dimNames    = new ArrayList<>();
                    // The dimmensions that we are going to use for iterating the index 
                    List<String>    dimensions  = new ArrayList<>();                
                    // Create the output variable
                    if(outDimensions.size() == 2 && outDimensions.get(1) == 1) {                         
                        dimVars.add(new LustreVar(getFreshVar("i"), PrimitiveType.INTCONST));
                        outputs.add(new LustreVar(getFreshVar("m"), outBaseType));
                    } else {
                        int outportD = outDimensions.get(0);   
                        for(int j = 0; j < outportD; ++j) {
                            dimensions.add(getFreshVar("i"));
                        }                    
                        for(int i = 1; i < outDimensions.size(); ++i) {
                            String dimVar = getFreshVar("i");
                            dimNames.add(dimVar);    
                            dimVars.add(new LustreVar(dimVar, PrimitiveType.INTCONST));
                        }                        
                        outputs.add(new LustreVar(getFreshVar("m"), new ArrayType(dimNames, outBaseType)));                    
                    }      
                    // Put dimension variables before actual array variables 
                    // because of the forward reference issue
                    inputs.addAll(dimVars);
                    inputs.addAll(tempInputs);

                    LustreExpr rhsExpr = null;

                    if(ops.charAt(0)=='-') {
                        if(singleInVar.contains(inVarNames.get(0))) {
                            rhsExpr = new UnaryExpr(UnaryExpr.Op.NEG, new VarIdExpr(inVarNames.get(0)));
                        } else {
                            rhsExpr = new UnaryExpr(UnaryExpr.Op.NEG, new ArrayExpr(inVarNames.get(0), dimensions));
                        }                    
                    } else {
                        if(singleInVar.contains(inVarNames.get(0))) {
                            rhsExpr = new VarIdExpr(inVarNames.get(0));
                        } else {
                            rhsExpr = new ArrayExpr(inVarNames.get(0), dimensions);
                        }                    
                    }                
                    for(int i = 1; i < ops.length(); ++i) {
                        switch (ops.charAt(i)) {
                            case '+': {    
                                if(singleInVar.contains(inVarNames.get(i))) {
                                    rhsExpr = new BinaryExpr(rhsExpr, BinaryExpr.Op.PLUS, new VarIdExpr(inVarNames.get(i)));
                                } else {
                                    rhsExpr = new BinaryExpr(rhsExpr, BinaryExpr.Op.PLUS, new ArrayExpr(inVarNames.get(i), dimensions));
                                }                             
                                break;
                            }
                            case '-': {
                                if(singleInVar.contains(inVarNames.get(i))) {
                                    rhsExpr = new BinaryExpr(rhsExpr, BinaryExpr.Op.MINUS, new VarIdExpr(inVarNames.get(i)));
                                } else {
                                    rhsExpr = new BinaryExpr(rhsExpr, BinaryExpr.Op.MINUS, new ArrayExpr(inVarNames.get(i), dimensions));
                                }                               
                                break;
                            }
                            default:
                                LOGGER.log(Level.SEVERE, "Unexpected operators in PRODUCT blocks parameters: {0}", ops.charAt(i));
                                break;
                        }                                        
                    }  
                    if(dimensions.size() > 0) {
                        eqs.add(new LustreEq(new ArrayExpr(outputs.get(0).name, dimensions), rhsExpr));
                    } else {
                        eqs.add(new LustreEq(new VarIdExpr(outputs.get(0).name), rhsExpr));
                    }

                    this.lustreProgram.addNode(new LustreNode(fcnName, inputs, outputs, eqs));
                    this.libNodeNameMap.put(fcnName, fcnName);
                }
                blkExpr = new NodeCallExpr(fcnName, exprs);                
            }     
        // This is to handle the specified dimension sum
        } else {
            char fstOp = ops.charAt(0);
            
            if(inBaseTypes.get(0) == PrimitiveType.INT) {
                blkExpr = new IntExpr(BigInteger.ZERO);
            } else if(inBaseTypes.get(0) == PrimitiveType.REAL) {
                blkExpr = new RealExpr(new BigDecimal("0.0"));
            }
          
            if(inDimensions.size() == 2 && inDimensions.get(1) == 1) {
                if(fstOp == '+') {
                    blkExpr = inExprs.get(0);
                } else {
                    blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.MINUS, inExprs.get(0));
                }
            } else if(inDimensions.size() == 2) {
                for(int i = 0; i < inDimensions.get(1); ++i) {
                    if(fstOp == '+') {                        
                        blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.PLUS, new ArrayExpr(inExprs.get(0), i));
                    } else {
                        blkExpr = new BinaryExpr(blkExpr, BinaryExpr.Op.MINUS, new ArrayExpr(inExprs.get(0), i));
                    }                                    
                }
            } else {
                int                 colDim      = blkNode.get(COLLAPSEDIM).asInt();
                List<LustreExpr>    rhsExprs    = new ArrayList<>();                   
                List<Integer>       outDims     = new ArrayList<>();
                                
                for(int i = 1; i < outDimensions.size(); ++i) {
                    outDims.add(outDimensions.get(i));
                }
                
                mkCollapseSpecDimExpr(colDim-1, 0, fstOp, inExprs.get(0), inBaseTypes.get(0), newInDims.get(0), rhsExprs, new ArrayList<Integer>());
                blkExpr = new ArrayExpr(outDims, rhsExprs);
            }                              
        }
        return blkExpr;
    }    
   
    
    /**
     * @param colDim
     * @param curDim
     * @param op
     * @param inputExpr
     * @param baseType
     * @param inDims
     * @param rhsExprs
     * @param allDims
     */
    protected void mkCollapseSpecDimExpr(int colDim, int curDim, char op, LustreExpr inputExpr, LustreType baseType, List<Integer> inDims, List<LustreExpr> rhsExprs, List<Integer> allDims) {
        if(curDim == inDims.size()) {
            LustreExpr      rhs = null;            
            
            if(baseType == PrimitiveType.INT) {
                rhs = (op == '+' || op == '-') ? new IntExpr(BigInteger.ZERO) : new IntExpr(BigInteger.ONE);
            } else if(baseType == PrimitiveType.REAL) {
                rhs = (op == '+' || op == '-') ? new RealExpr(new BigDecimal("0.0")) : new RealExpr(new BigDecimal("1.0"));
            } else {
                LOGGER.log(Level.SEVERE, "Unhandled type case: {0}", baseType);
            }
            
            for(int j = 0; j < inDims.get(colDim); ++j) {
                List<Integer> sndNewAllDims = new ArrayList<>();
                sndNewAllDims.addAll(allDims);            
                sndNewAllDims.add(colDim, j);
                
                switch (op) {
                    case '+':
                        rhs = new BinaryExpr(rhs, BinaryExpr.Op.PLUS, new ArrayExpr(sndNewAllDims, inputExpr));
                        break;
                    case '-':
                        rhs = new BinaryExpr(rhs, BinaryExpr.Op.MINUS, new ArrayExpr(sndNewAllDims, inputExpr));
                        break;
                    case '*':
                        rhs = new BinaryExpr(rhs, BinaryExpr.Op.MULTIPLY, new ArrayExpr(sndNewAllDims, inputExpr));
                        break;
                    case '/':
                        rhs = new BinaryExpr(rhs, BinaryExpr.Op.DIVIDE, new ArrayExpr(sndNewAllDims, inputExpr));
                        break;
                    default:
                        break;
                }
            }
            rhsExprs.add(rhs);
        } else if(colDim == curDim) {
            mkCollapseSpecDimExpr(colDim, curDim+1, op, inputExpr, baseType, inDims, rhsExprs, allDims);
        } else {
            for(int i = 0; i < inDims.get(curDim); ++i) {
                List<Integer> newAllDims = new ArrayList<>();
                newAllDims.addAll(allDims);
                newAllDims.add(i);
                mkCollapseSpecDimExpr(colDim, curDim+1, op, inputExpr, baseType, inDims, rhsExprs, newAllDims);
            }
        }
    }
    
    /**
     * @param size
     * @param inDims
     * @param op
     * @param dims
     * @param blkExpr
     * @param inExpr
     */
    protected void mkCollapseAllDimExpr(int size, List<Integer> inDims, char op, List<Integer> dims, List<LustreExpr> blkExpr, LustreExpr inExpr, LustreType baseType) {
        if(size == inDims.size()-1) {            
            List<Integer> newDims = new ArrayList<>();
            newDims.addAll(dims);
            newDims.add(0);
            LustreExpr expr = new ArrayExpr(newDims, inExpr);
            BinaryExpr.Op bOp = null;
            
            switch (op) {
                case '+':
                    bOp = BinaryExpr.Op.PLUS;
                    break;
                case '-': {
                    bOp = BinaryExpr.Op.MINUS;
                    expr = new UnaryExpr(UnaryExpr.Op.NEG, new ArrayExpr(newDims, inExpr));   
                    break;                
                }
                case '*':
                    bOp = BinaryExpr.Op.MULTIPLY;
                    break;
                case '/': {
                    LustreExpr one = baseType == PrimitiveType.INT ? new IntExpr(BigInteger.ONE) : new RealExpr(new BigDecimal("1.0"));
                    expr = new BinaryExpr(one, BinaryExpr.Op.DIVIDE, new ArrayExpr(newDims, inExpr));   
                    bOp = BinaryExpr.Op.DIVIDE;
                    break; 
                }
                default:
                    break;
            }                      
            for(int i = 1; i < inDims.get(size); ++i) {
                List<Integer> freshDims = new ArrayList<>();
                freshDims.addAll(dims);
                freshDims.add(i);
                expr = new BinaryExpr(expr, bOp, new ArrayExpr(freshDims, inExpr));
            }
            if(!blkExpr.isEmpty()) {
                LustreExpr newExpr = blkExpr.get(0);
                newExpr = new BinaryExpr(newExpr, (op == '-' || op == '+') ? BinaryExpr.Op.PLUS:BinaryExpr.Op.MULTIPLY, expr);
                blkExpr.clear();
                blkExpr.add(newExpr);                
            } else {
                blkExpr.add(expr);    
            }
            
        } else {
            List<LustreExpr> exprs = new ArrayList<>();
            
            for(int j = 0; j < inDims.get(size); ++j) {
                List<Integer> newDims = new ArrayList<>();
                newDims.addAll(dims);                
                newDims.add(j);
                mkCollapseAllDimExpr(size+1, inDims, op, newDims, blkExpr, inExpr, baseType);
            }          
        }   
    }
    
    /**
     *
     * @param orgPath
     * @param nodeName
     * @param contractName
     * @param modeName
     * @param propName
     * @param index 
     * @param variable
     */
    protected void addMappingInfo(String orgPath, String nodeName, String contractName, String modeName, String propName, String index, String variable) {
        Map<String, String> mappingInfo = new LinkedHashMap<>();
        
        if(orgPath != null) {
            mappingInfo.put(ORIGINPATH, orgPath);
        }
        if(nodeName != null) {
            mappingInfo.put(NODENAME, nodeName);
        }
        if(contractName != null) {
            mappingInfo.put(CONTRACTNAME, contractName);
        }
        if(modeName != null) {
            mappingInfo.put(MODENAME, modeName);
        }
        if(propName != null) {
            mappingInfo.put(PROPNAME, propName);
        }
        if(index != null) {
            mappingInfo.put(INDEX, index);
        }
        if(variable != null) {
            mappingInfo.put(VARIABLE, variable);
        }        
        if(!mappingInfo.isEmpty()) {
            this.jsonMappingInfo.add(mappingInfo);
        }
        
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
    
    protected LustreExpr getCondExprFromIfBlk(boolean isPropBlk, JsonNode ifBlkNode, String ifActionBlkHdl, JsonNode parentSubsystemNode, Map<JsonNode, List<String>> blkNodeToSrcBlkHdlsMap, Map<JsonNode, List<Integer>> blkNodeToSrcBlkPortsMap, Map<JsonNode, List<String>> blkNodeToDstBlkHandlesMap, Map<String, JsonNode> handleToBlkNodeMap) {
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
            List<String>        dstBlkHdls      = blkNodeToDstBlkHandlesMap.get(ifBlkNode);
                       
            if(ifCondExpr != null && !ifCondExpr.equals("")) {
                condStrExprs.add(ifCondExpr);
            }
            if(elseIfCondExpr != null && !elseIfCondExpr.equals("")) {
                condStrExprs.addAll(Arrays.asList(elseIfCondExpr.split(",")));
            }
                        
            if(blkNodeToSrcBlkHdlsMap.containsKey(ifBlkNode)) {
                List<String>    inHdls  = blkNodeToSrcBlkHdlsMap.get(ifBlkNode);
                List<Integer>   inPorts = blkNodeToSrcBlkPortsMap.get(ifBlkNode);
                
                for(int i = 0; i < inHdls.size(); i++) {
                    inExprs.add(translateBlock(isPropBlk, inHdls.get(i), parentSubsystemNode, blkNodeToSrcBlkHdlsMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap, new HashSet<String>(), null, inPorts.get(i)));
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
    
    protected LustreExpr translateIfBlock(boolean isPropBlk, JsonNode ifBlkNode, Map<String, LustreExpr> hdlActSysExprMap, JsonNode parentSubsystemNode, Map<JsonNode, List<String>> blkNodeToSrcBlkHandlesMap, Map<JsonNode, List<Integer>> blkNodeToSrcBlkPortsMap, Map<JsonNode, List<String>> blkNodeToDstBlkHandlesMap, Map<String, JsonNode> handleToBlkNodeMap) {
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
                List<String>    inHdls  = blkNodeToSrcBlkHandlesMap.get(ifBlkNode);
                List<Integer>   inPorts = blkNodeToSrcBlkPortsMap.get(ifBlkNode);
                
                for(int i = 0; i < inHdls.size(); i++) {
                    inExprs.add(translateBlock(isPropBlk, inHdls.get(i), parentSubsystemNode, blkNodeToSrcBlkHandlesMap, blkNodeToSrcBlkPortsMap, blkNodeToDstBlkHandlesMap, handleToBlkNodeMap, new HashSet<String>(), null, inPorts.get(i)));
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
     * @param type
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
        
        VarIdExpr       inVarExpr   = new VarIdExpr("in");
        VarIdExpr       outVarExpr  = new VarIdExpr("out");   
        List<LustreEq>  bodyExprs   = new ArrayList<>();            
        
        switch(nodeName) {
            case BOOLTOINT: {                 
                bodyExprs.add(new LustreEq(outVarExpr, new IteExpr(inVarExpr, new IntExpr(BigInteger.ONE), new IntExpr(BigInteger.ZERO))));
                this.libNodeNameMap.put(BOOLTOINT, BOOLTOINT);
                this.lustreProgram.addNode(new LustreNode(nodeName, new LustreVar(inVarExpr.id, PrimitiveType.BOOL), new LustreVar(outVarExpr.id, PrimitiveType.INT), bodyExprs));                                
                break;
            }
            case BOOLTOREAL: {
                bodyExprs.add(new LustreEq(outVarExpr, new IteExpr(inVarExpr, new RealExpr(new BigDecimal("1.0")), new RealExpr(new BigDecimal("0.0")))));
                this.libNodeNameMap.put(BOOLTOREAL, BOOLTOREAL);
                this.lustreProgram.addNode(new LustreNode(nodeName, new LustreVar(inVarExpr.id, PrimitiveType.BOOL), new LustreVar(outVarExpr.id, PrimitiveType.REAL), bodyExprs));                                
                break;
            }  
            case INTTOBOOL: {
                bodyExprs.add(new LustreEq(outVarExpr, new IteExpr(new BinaryExpr(inVarExpr, BinaryExpr.Op.NEQ, new IntExpr(new BigInteger("0"))), new BooleanExpr(true), new BooleanExpr(false))));
                this.libNodeNameMap.put(INTTOBOOL, INTTOBOOL);
                this.lustreProgram.addNode(new LustreNode(nodeName, new LustreVar(inVarExpr.id, PrimitiveType.INT), new LustreVar(outVarExpr.id, PrimitiveType.BOOL), bodyExprs));                                
                break;
            } 
            case REALTOBOOL: {
                bodyExprs.add(new LustreEq(outVarExpr, new IteExpr(new BinaryExpr(inVarExpr, BinaryExpr.Op.NEQ, new RealExpr(new BigDecimal("0.0"))), new BooleanExpr(true), new BooleanExpr(false))));
                this.libNodeNameMap.put(REALTOBOOL, REALTOBOOL);
                this.lustreProgram.addNode(new LustreNode(nodeName, new LustreVar(inVarExpr.id, PrimitiveType.REAL), new LustreVar(outVarExpr.id, PrimitiveType.BOOL), bodyExprs));                                
                break;
            } 
            case INTTOREAL: {
                this.libNodeNameMap.put(INTTOREAL, INTTOREAL);
                break;
            } 
            case REALTOINT: {
                this.libNodeNameMap.put(REALTOINT, REALTOINT);
                break;
            }
            case FLOOR: {
                mkFloorNode();
                break;
            }
            case CEIL: {
                mkCeilNode();
                break;
            }  
            case ROUND: {
                mkRoundNode();
                break;
            }
            case FIX: {
                mkFixNode();
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
     * @return The name of the library Lustre node corresponding to the input JsonNode
     */
    protected String getOrCreateLustreLibNode(JsonNode blkNode) {
        String path     = blkNode.get(PATH).asText();
        String nodeName = path.replace(File.separator,"_");
        String blkType  = blkNode.get(BLOCKTYPE).asText();
        
        if(blkType.equals(MATH)) {
            blkType = blkNode.get(OPERATOR).asText();
        }
        switch(blkType) {         
            case ABS: {
                nodeName = mkAbsNode(getBlkOutportType(blkNode));
                break;
            } 
            case SQRTOP:
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
            case ROUNDING: {                
                String op = blkNode.get(OPERATOR).asText();
                
                if(!this.libNodeNameMap.containsKey(op)) {
                    getOrCreateLustreLibNode(op);
                }
                nodeName = op;
                break;
            }
            case DATATYPECONVERSION: {
                String inType   = getBlkInportType(blkNode).toString();
                String outType  = getBlkOutportType(blkNode).toString();
                
                switch(inType) {
                    case BOOL: {
                        switch(outType) {
                            case INT: {
                                if(!this.libNodeNameMap.containsKey(BOOLTOINT)) {
                                    getOrCreateLustreLibNode(BOOLTOINT);
                                }
                                nodeName = BOOLTOINT;
                                break;
                            }
                            case REAL: {
                                if(!this.libNodeNameMap.containsKey(BOOLTOREAL)) {
                                    getOrCreateLustreLibNode(BOOLTOREAL);
                                }
                                nodeName = BOOLTOREAL;
                                break;
                            }                                                  
                        }
                        break;
                    }
                    case INT: {
                        switch(outType) {
                            case BOOL: {
                                if(!this.libNodeNameMap.containsKey(INTTOBOOL)) {
                                    getOrCreateLustreLibNode(INTTOBOOL);
                                }
                                nodeName = INTTOBOOL;
                                break;
                            }
                            case REAL: {
                                nodeName = INTTOREAL;
                                break;
                            }                                                  
                        }
                        break;
                    }                    
                    case REAL: {
                        switch(outType) {
                            case BOOL: {
                                if(!this.libNodeNameMap.containsKey(REALTOBOOL)) {
                                    getOrCreateLustreLibNode(REALTOBOOL);
                                }
                                nodeName = REALTOBOOL;
                                break;
                            }
                            case INT: {
                                nodeName = REALTOINT;
                                break;
                            }                                                  
                        }
                        break;
                    }                          
                }
                // Some unsupported types conversion
                if(inType.equals(outType)) {
                    if(!getBlkStrInType(blkNode).equalsIgnoreCase(getBlkStrOutType(blkNode))) {
                        LOGGER.log(Level.SEVERE, "Unsupported data type conversion from {0} to {1}", new Object[]{getBlkStrInType(blkNode), getBlkStrOutType(blkNode)});
                        nodeName += "_"+DATATYPECONVERSION+"_"+inType+"_to_"+outType;
                        getOrCreateDummyNode(nodeName, J2LUtils.getLustreTypeFromStrRep(inType), J2LUtils.getLustreTypeFromStrRep(outType));
                    }
                }
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
    
    protected void getOrCreateDummyNode(String fcnName, LustreType inType, LustreType outType) {
        if(this.libNodeNameMap.containsKey(fcnName)) {
            return ;
        }
        
        List<LustreVar>     inVars  = new ArrayList<>();
        List<LustreVar>     outVars = new ArrayList<>();
        List<LustreEq>      eqs     = new ArrayList<>();                
        LustreVar in    = new LustreVar("in", inType);
        LustreVar out   = new LustreVar("out", outType);
        inVars.add(in);
        outVars.add(out);
        eqs.add(new LustreEq(new VarIdExpr("out"), new VarIdExpr("in")));
        this.lustreProgram.addNode(new LustreNode(fcnName, inVars, outVars, eqs)); 
        this.libNodeNameMap.put(fcnName, fcnName);
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

    protected boolean isSubsystemBlock(JsonNode node) {
        if(node != null) {
            if(node.has(BLOCKTYPE) && node.get(BLOCKTYPE).asText().equals(SUBSYSTEM)) {
                return true;
            }                                  
        }
        return false;        
    }    
    
    protected boolean isLustreOpBlk(JsonNode node) {
        if(node != null) {
            if(node.has(BLOCKTYPE) && node.get(BLOCKTYPE).asText().equals(SUBSYSTEM)) {
                if(node.has(LUSTREOPERATORBLK)) {
                    String blkType = node.get(LUSTREOPERATORBLK).asText();
                    return blkType.equals(ARROW);
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
                    return blkType.equals(MODEBLK) || blkType.equals(ENSUREBLK) ||
                           blkType.equals(REQUIREBLK) || blkType.equals(ASSUMEBLK) || blkType.equals(GUARANTEEBLK);
                }
            }
        }
        return false;        
    }    
    
    protected boolean isContractBlk(JsonNode node) {
        if(node != null) {
            if(node.has(BLOCKTYPE) && node.get(BLOCKTYPE).asText().equals(SUBSYSTEM)) {
                if(node.has(CONTRACTBLKTYPE) && node.get(CONTRACTBLKTYPE).asText().equals(CONTRACTBLK)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    protected boolean isModeBlk(JsonNode node) {
        if(node != null) {
            if(node.has(BLOCKTYPE) && node.get(BLOCKTYPE).asText().equals(SUBSYSTEM)) {
                if(node.has(CONTRACTBLKTYPE) && node.get(CONTRACTBLKTYPE).asText().equals(MODEBLK)) {
                    return true;
                }
            }
        }
        return false;
    } 
    
    protected boolean isAssumeBlk(JsonNode node) {
        if(node != null) {
            if(node.has(BLOCKTYPE) && node.get(BLOCKTYPE).asText().equals(SUBSYSTEM)) {
                if(node.has(CONTRACTBLKTYPE) && node.get(CONTRACTBLKTYPE).asText().equals(ASSUMEBLK)) {
                    return true;
                }
            }
        }
        return false;
    } 

    protected boolean isGuaranteeBlk(JsonNode node) {
        if(node != null) {
            if(node.has(BLOCKTYPE) && node.get(BLOCKTYPE).asText().equals(SUBSYSTEM)) {
                if(node.has(CONTRACTBLKTYPE) && node.get(CONTRACTBLKTYPE).asText().equals(GUARANTEEBLK)) {
                    return true;
                }
            }
        }
        return false;
    } 

    protected boolean isRequireBlk(JsonNode node) {
        if(node != null) {
            if(node.has(BLOCKTYPE) && node.get(BLOCKTYPE).asText().equals(SUBSYSTEM)) {
                if(node.has(CONTRACTBLKTYPE) && node.get(CONTRACTBLKTYPE).asText().equals(REQUIREBLK)) {
                    return true;
                }
            }
        }
        return false;
    } 

    protected boolean isEnsureBlk(JsonNode node) {
        if(node != null) {
            if(node.has(BLOCKTYPE) && node.get(BLOCKTYPE).asText().equals(SUBSYSTEM)) {
                if(node.has(CONTRACTBLKTYPE) && node.get(CONTRACTBLKTYPE).asText().equals(ENSUREBLK)) {
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
    
    protected void enforceCorrectTyping(List<LustreExpr> inExprs, List<String> inHandles, Map<String, JsonNode> hdlToBlkNodeMap) {
        Object[] results = getTheHighestType(inHandles, hdlToBlkNodeMap);
        
        if(((Boolean)results[0])) {
            LustreType highestType = (LustreType)results[1];
            
            for(int i = 0; i < inHandles.size(); i++) {
                LustreType nodeType = getBlkOutportType(hdlToBlkNodeMap.get(inHandles.get(i)));

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
        } else if(getBlkOutportType(hdlToBlkNodeMap.get(inHandles.get(0))) == PrimitiveType.BOOL) {            
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
    
    protected void enforceTargetType(List<LustreExpr> inExprs, List<String> inHandles, LustreType targetType, Map<String, JsonNode> handleToBlkNodeMap) {
        for(int i = 0; i < inHandles.size(); i++) {
            LustreType nodeType = getBlkOutportType(handleToBlkNodeMap.get(inHandles.get(i)));
            
            if(nodeType != targetType) {
                inExprs.set(i, new NodeCallExpr(getOrCreateLustreLibNode(nodeType.toString().toLowerCase()+"_to_"+targetType.toString().toLowerCase()), inExprs.get(i)));
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
    
    
    protected void mkFloorNode() {
        if(this.libNodeNameMap.containsKey(FLOOR)) {
            return;
        }
        
        VarIdExpr       inVarExpr   = new VarIdExpr("in");
        VarIdExpr       outVarExpr  = new VarIdExpr("out");          
        List<LustreEq>  bodyExprs   = new ArrayList<>();
        
        bodyExprs.add(new LustreEq(outVarExpr, new UnaryExpr(UnaryExpr.Op.REAL, new UnaryExpr(UnaryExpr.Op.INT, inVarExpr))));
        this.lustreProgram.addNode(new LustreNode(FLOOR, new LustreVar(inVarExpr.id, PrimitiveType.REAL), new LustreVar(outVarExpr.id, PrimitiveType.REAL), bodyExprs));
        this.libNodeNameMap.put(FLOOR, FLOOR);
    }
    
    protected String mkAbsNode(LustreType type) {
        String nodeName = ABS+"_"+type;
        
        if(this.libNodeNameMap.containsKey(nodeName)) {
            return nodeName;
        }
        
        VarIdExpr       inVarExpr   = new VarIdExpr("in");
        VarIdExpr       outVarExpr  = new VarIdExpr("out");          
        List<LustreEq>  bodyExprs   = new ArrayList<>();        
        
        if(type == PrimitiveType.INT) {            
            bodyExprs.add(new LustreEq(outVarExpr, new IteExpr(new BinaryExpr(inVarExpr, BinaryExpr.Op.GTE, new IntExpr(BigInteger.ZERO)), inVarExpr, new UnaryExpr(UnaryExpr.Op.NEG, inVarExpr))));            
        } else if(type == PrimitiveType.REAL) {
            bodyExprs.add(new LustreEq(outVarExpr, new IteExpr(new BinaryExpr(inVarExpr, BinaryExpr.Op.GTE, new RealExpr(new BigDecimal("0.0"))), inVarExpr, new UnaryExpr(UnaryExpr.Op.NEG, inVarExpr))));            
        } else {
            LOGGER.log(Level.SEVERE, "Unsupported datatype in absolute value function: {0}", type);
        }
        this.libNodeNameMap.put(nodeName, nodeName);
        this.lustreProgram.addNode(new LustreNode(nodeName, new LustreVar(inVarExpr.id, type), new LustreVar(outVarExpr.id, type), bodyExprs));
        return nodeName;
    }
    
    protected void mkRoundNode() {
        if(this.libNodeNameMap.containsKey(ROUND)) {
            return;
        }
        
        mkCeilNode();
        String absReal = mkAbsNode(PrimitiveType.REAL);                
        VarIdExpr       inVarExpr   = new VarIdExpr("in");
        VarIdExpr       outVarExpr  = new VarIdExpr("out");          
        List<LustreEq>  bodyExprs   = new ArrayList<>();
        
        bodyExprs.add(new LustreEq(outVarExpr, new IteExpr(new BinaryExpr(inVarExpr, BinaryExpr.Op.GTE, new RealExpr(new BigDecimal("0.0"))), new NodeCallExpr(CEIL, new NodeCallExpr(absReal, inVarExpr)), new UnaryExpr(UnaryExpr.Op.NEG, new NodeCallExpr(CEIL, new NodeCallExpr(absReal, inVarExpr))))));
        this.lustreProgram.addNode(new LustreNode(ROUND, new LustreVar(inVarExpr.id, PrimitiveType.REAL), new LustreVar(outVarExpr.id, PrimitiveType.REAL), bodyExprs));
        this.libNodeNameMap.put(ROUND, ROUND);
    }    
    
    protected void mkFixNode() {
        if(this.libNodeNameMap.containsKey(ROUND)) {
            return;
        }
        
        mkCeilNode();
        String absReal = mkAbsNode(PrimitiveType.REAL);                
        VarIdExpr       inVarExpr   = new VarIdExpr("in");
        VarIdExpr       outVarExpr  = new VarIdExpr("out");          
        List<LustreEq>  bodyExprs   = new ArrayList<>();
        
        bodyExprs.add(new LustreEq(outVarExpr, new IteExpr(new BinaryExpr(inVarExpr, BinaryExpr.Op.GTE, new RealExpr(new BigDecimal("0.0"))), new NodeCallExpr(FLOOR, new NodeCallExpr(absReal, inVarExpr)), new UnaryExpr(UnaryExpr.Op.NEG, new NodeCallExpr(FLOOR, new NodeCallExpr(absReal, inVarExpr))))));
        this.lustreProgram.addNode(new LustreNode(FIX, new LustreVar(inVarExpr.id, PrimitiveType.REAL), new LustreVar(outVarExpr.id, PrimitiveType.REAL), bodyExprs));
        this.libNodeNameMap.put(FIX, FIX);
    }     
    
    protected void mkCeilNode() {
        if(!this.libNodeNameMap.containsKey(FLOOR)) {
            mkFloorNode();
        }
        
        VarIdExpr       inVarExpr   = new VarIdExpr("in");
        VarIdExpr       outVarExpr  = new VarIdExpr("out");          
        List<LustreEq>  bodyExprs   = new ArrayList<>();
        
        bodyExprs.add(new LustreEq(outVarExpr, new UnaryExpr(UnaryExpr.Op.NEG, new NodeCallExpr(FLOOR, new UnaryExpr(UnaryExpr.Op.NEG, inVarExpr)))));
        this.lustreProgram.addNode(new LustreNode(CEIL, new LustreVar(inVarExpr.id, PrimitiveType.REAL), new LustreVar(outVarExpr.id, PrimitiveType.REAL), bodyExprs));
        this.libNodeNameMap.put(CEIL, CEIL);
    }    
    

    
    private boolean isCompareToConst(JsonNode blkNode) {
        boolean isCompareToConst = false;
        
        if(blkNode.has(BLOCKTYPE)) {
            String blkType = blkNode.get(BLOCKTYPE).asText(); 

            if(blkType != null && blkType.equals(SUBSYSTEM)) {                                
                if(blkNode.get(CONTENT).size() != 4) {
                    return false;
                }
                
                Iterator<Entry<String, JsonNode>> nodes = blkNode.get(CONTENT).fields();
                
                while(nodes.hasNext()) {
                    Entry<String, JsonNode> node = nodes.next();                
                    String      fieldKey   = node.getKey();
                    JsonNode    fieldNode  = node.getValue();

                    if(fieldKey.equals(CONSTANT) || fieldKey.equals(COMPARE) || fieldKey.equals(U) || fieldKey.equals(Y)) {                        
                        if(fieldNode.has(VALUE)) {
                            isCompareToConst = fieldNode.get(VALUE).asText().equals(CONST);
                        } else {
                            isCompareToConst = true;
                        }
                    } else {
                        isCompareToConst = false;
                    }
                    if(!isCompareToConst) {
                        return isCompareToConst;
                    }
                }            
            }            
        }
        return isCompareToConst;        
    }
    
    private boolean isCompareToZero(JsonNode blkNode) {
        boolean isCompareToZero = false;
        
        if(blkNode.has(BLOCKTYPE)) {
            String blkType = blkNode.get(BLOCKTYPE).asText(); 

            if(blkType != null && blkType.equals(SUBSYSTEM)) {                                
                if(blkNode.get(CONTENT).size() != 4) {
                    return false;
                }
                
                Iterator<Entry<String, JsonNode>> nodes = blkNode.get(CONTENT).fields();
                
                while(nodes.hasNext()) {
                    Entry<String, JsonNode> node = nodes.next();                
                    String      fieldKey   = node.getKey();
                    JsonNode    fieldNode  = node.getValue();

                    if(fieldKey.equals(CONSTANT) || fieldKey.equals(COMPARE) || fieldKey.equals(U) || fieldKey.equals(Y)) {                        
                        if(fieldNode.has(VALUE)) {
                            isCompareToZero = fieldNode.get(VALUE).asInt() == 0;
                        } else {
                            isCompareToZero = true;
                        }
                    } else {
                        isCompareToZero = false;
                    }
                    if(!isCompareToZero) {
                        return isCompareToZero;
                    }
                }            
            }            
        }
        return isCompareToZero;
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
                } else if(value.contains(".")) {
                    String rhs = value.substring(value.indexOf(".")+1).trim();
                    
                    if(Integer.parseInt(rhs) == 0) {
                        newVal = value.substring(0, value.indexOf("."));
                    } else {
                        LOGGER.log(Level.SEVERE, "Unexpected integer constant: {0}", value);
                    }
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
    
    
    protected LustreType getSwitchCondType(JsonNode switchBlk) {        
        List<String>        types   = convertJsonValuesToList(switchBlk.get(PORTDATATYPE).get(INPORT));
        Iterator<JsonNode>  connIt  = switchBlk.get(CONNECTIVITY).elements(); 
        // The switch condition is the second port in the port connectivity
        return J2LUtils.getLustreTypeFromStrRep(types.get(connIt.next().get(TYPE).asInt()));
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
    
    private List<Integer> convertJsonValuesToListInt(JsonNode values) {
        List<Integer> intValues = new ArrayList<>();
        
        if(values.isArray()) {
            Iterator<JsonNode> valuesIt = values.elements();
            
            while(valuesIt.hasNext()) {
                intValues.add(Integer.parseInt(valuesIt.next().asText()));
            }
        } else {
            intValues.add(Integer.parseInt(values.asText()));
        }
        return intValues;
    }    
    
    protected List<String> getBlkOutportHandles(JsonNode blkNode) {
        List<String> outportHdls = new ArrayList<>();

        if(blkNode.has(CONNECTIVITY)) {
            JsonNode portConns = blkNode.get(CONNECTIVITY);

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
            return blkNode.get(ANNOTATIONTYPE).asText().equals(ENSURES);
        }
        return false;
    }

    protected LustreType getBlkOutportType(JsonNode blkNode) {
        return J2LUtils.getLustreTypeFromStrRep(blkNode.get(PORTDATATYPE).get(OUTPORT).asText());
    }   
    
    protected LustreType getBlkInportType(JsonNode blkNode) {
        return J2LUtils.getLustreTypeFromStrRep(blkNode.get(PORTDATATYPE).get(INPORT).asText());     
    }
    
    protected List<LustreType> getMatrixBlkInportType(JsonNode blkNode) {
        JsonNode node = blkNode.get(PORTDATATYPE).get(INPORT);
        List<LustreType> types = new ArrayList<>();
        
        if(node.isArray()) {
            Iterator<JsonNode> nodeIt = node.elements();
            while(nodeIt.hasNext()) {
                JsonNode typeNode = nodeIt.next();
                types.add(J2LUtils.getLustreTypeFromStrRep(typeNode.asText()));
            }
        } else {
            types.add(J2LUtils.getLustreTypeFromStrRep(node.asText()));            
        }   
        return types;
    }    

    protected String getBlkStrInType(JsonNode blkNode) {
        return blkNode.get(PORTDATATYPE).get(INPORT).asText();
    }    
    
    protected String getBlkStrOutType(JsonNode blkNode) {
        return blkNode.get(PORTDATATYPE).get(OUTPORT).asText();
    }        
    
    protected List<LustreType> getBlockOutportTypes(JsonNode blkNode) {
        List<LustreType>    types       = new ArrayList<>();
        List<String>        typeStrs    = convertJsonValuesToList(blkNode.get(PORTDATATYPE).get(OUTPORT));        
        
        for(String typeStr : typeStrs) {
            types.add(J2LUtils.getLustreTypeFromStrRep(typeStr));
        }
        
        return types;
    }    
    
    protected List<LustreType> getBlockInportTypes(JsonNode blkNode) {
        List<LustreType>    types       = new ArrayList<>();
        List<String>        typeStrs    = convertJsonValuesToList(blkNode.get(PORTDATATYPE).get(INPORT));        
        
        for(String typeStr : typeStrs) {
            types.add(J2LUtils.getLustreTypeFromStrRep(typeStr));
        }
        
        return types;
    }  
    
    
    protected Map<Integer, String> getBlkOutportNameAndPort(JsonNode blkNode) {
        Map<Integer, String> names = new HashMap<>();
        
        if(blkNode.has(CONTENT) && blkNode.get(CONTENT).size() > 1) {
            Iterator<Entry<String, JsonNode>> contentFields = blkNode.get(CONTENT).fields();

            while(contentFields.hasNext()) {
                Map.Entry<String, JsonNode> contField       = contentFields.next();   
                JsonNode                    contBlkNode     = contField.getValue();

                if(contBlkNode.has(BLOCKTYPE)) {
                    String contBlkType = contBlkNode.get(BLOCKTYPE).asText();                    
                    if(contBlkType.equals(OUTPORT)) {
                        names.put(contBlkNode.get(PORT).asInt(), contBlkNode.get(NAME).asText());
                    }            
                }            
            }            
        } else if(blkNode.has(SFCONTENT)) {
            JsonNode sfContentNode = blkNode.get(SFCONTENT);
            
            if(sfContentNode.has(DATA)) {
                Iterator<JsonNode> dataFields = sfContentNode.get(DATA).elements();
                
                while(dataFields.hasNext()) {
                    JsonNode dataField = dataFields.next();
                    
                    if(dataField.has(SCOPE) && dataField.get(SCOPE).asText().equals(OUTPUT)) {
                        names.put(dataField.get(PORT).asInt(), dataField.get(NAME).asText());
                    }
                }
            }             
        } else {
            LOGGER.log(Level.SEVERE, "Unexpected: no content or sfcontent definition!");
        }                             
        return names;   
    }    

    
    String getPath(JsonNode blk) {
        String path;
        if(blk.has(PATH)) {
            path = blk.get(PATH).asText();
        } else {
            path = this.topNodeName;
        }
        return path;
    }
    
    String getOriginPath(JsonNode blk) {
        return blk.get(ORIGIN).asText();
    }    

    List<Integer> getInportDimensions(JsonNode blk) {
        List<Integer>   dimentsions = new ArrayList<>();
        JsonNode        inport      = blk.get(DIMENSION).get(INPORT);

        if(inport.isArray()) {
            Iterator<JsonNode> portConnIt = inport.elements();
            while(portConnIt.hasNext()) {
                JsonNode connNode = portConnIt.next();
                dimentsions.add(connNode.asInt());
            }                        
        } else {
            LOGGER.log(Level.SEVERE, "Unexpected: I might miss something here!");
        }
        return dimentsions;
    }
    
    List<Integer> getOutportDimensions(JsonNode blk) {
        List<Integer>   dimentsions = new ArrayList<>();
        JsonNode        outport     = blk.get(DIMENSION).get(OUTPORT);

        if(outport.isArray()) {
            Iterator<JsonNode> portConnIt = outport.elements();
            while(portConnIt.hasNext()) {
                JsonNode connNode = portConnIt.next();
                dimentsions.add(connNode.asInt());
            }                        
        } else {
            LOGGER.log(Level.SEVERE, "Unexpected: I might miss something here!");
        }
        return dimentsions;
    }    
    
    List<Integer> getDimensions(JsonNode blk) {
        List<Integer> dimentsions = new ArrayList<>();
        JsonNode inport   = blk.get(DIMENSION).get(INPORT);
        JsonNode outport  = blk.get(DIMENSION).get(OUTPORT);
        
        if(inport.isArray() && inport.elements().hasNext()) {
            Iterator<JsonNode> portConnIt = inport.elements();
            portConnIt.next();
            while(portConnIt.hasNext()) {
                JsonNode connNode = portConnIt.next();
                dimentsions.add(connNode.asInt());
            }                        
        } else if(outport.isArray() && outport.elements().hasNext()) {
            Iterator<JsonNode> portConnIt = outport.elements();
            portConnIt.next();
            while(portConnIt.hasNext()) {
                JsonNode connNode = portConnIt.next();
                dimentsions.add(connNode.asInt());
            }              
        } else {
            LOGGER.log(Level.SEVERE, "Unexpected: I might miss something here!");
        }
        return dimentsions;
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
    
    protected String getBlkType(JsonNode node) {
        if(node.has(BLOCKTYPE)) {
            return node.get(BLOCKTYPE).asText();
        } else {
            LOGGER.log(Level.SEVERE, "Unexpected: the input block does not have a block type!");
        }
        return "";
    }     
    
    protected String getFreshVar(String name) {
        if(name == null) {
            name = "";
        }
        return name+"_"+(++COUNT);
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
    public String sanitizeName(String name) {
        return name.trim().replace(" ", "_").replace("\n", "_").replace("=", "_")
                .replace(">", "_").replace("<", "_").replace("/", "_").replace("\\", "_")
                .replace("-", "_").replace("+", "_").replace("$", "_").replace("*", "_")
                .replace("(", "_").replace(")", "_").replace(",", "_");
    }    
    
}
