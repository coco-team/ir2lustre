/*
 * This file is part of CoCoSim_IR_Compiler.
 * Copyright (C) 2017-2018  The University of Iowa
 */

package edu.uiowa.json2lus;

import edu.uiowa.json2lus.lustreAst.LustrePrettyPrinter;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author Paul Meng
 */
public class Main {
    
    /**
     * This is the entry point of the program
     * 
     * @param args input parameters
     */
    public static void main(String[] args) {                   
        String              jsonFilePath    = null; 
        String              lusFilePath     = null;
        Options             opts            = constructOptions();               
        HelpFormatter       hf              = new HelpFormatter();        
        CommandLineParser   parser          = new DefaultParser();           
        
        try {
            CommandLine cl  = parser.parse(opts, args);
            
            if(cl.hasOption('h')) {
                hf.printHelp("CocoSim_IR_Compiler", opts, true);
                return;
            }
            if(cl.hasOption('v')) {
                System.out.println("CocoSim_IR_Compiler v0.1");
                return;
            }
            if(cl.hasOption('i')) {
                jsonFilePath = cl.getOptionValue('i');
            } 
            if(cl.hasOption('o')) {
                lusFilePath = cl.getOptionValue('o');
            }             
            if(lusFilePath == null || !lusFilePath.endsWith(".lus")) {
                lusFilePath = new File(jsonFilePath).getAbsolutePath() +".lus";
            }               
            if(validateInput(jsonFilePath)) {
                J2LTranslator       translator  = new J2LTranslator(jsonFilePath);                
                LustrePrettyPrinter ppv         = new LustrePrettyPrinter();

                ppv.printLustreProgramToFile(translator.execute(), lusFilePath);
                translator.dumpMappingInfoToJsonFile(lusFilePath+".json");
            } else {
                System.out.println("Please provide an input json file!\n");
            }
        } catch (ParseException ex) {
            System.err.println( "Parsing failed.  Reason: " + ex.getMessage() );
        }
    }
    
    /**
     * Construct program options menu
     */        
    private static Options constructOptions() {
        Options opts    = new Options();               
                
        opts.addOption("h", "help",     false, "Print this help information");
        opts.addOption("v", "version",  false, "Print tool version information");                
        opts.addOption("m", false, "Print multiple properties in one node!");                
        opts.addOption(Option.builder("i").longOpt("json-file").desc("Input json file path").hasArgs().build());
        opts.addOption(Option.builder("o").longOpt("lustre-file").desc("Output lustre file path").hasArgs().build());
        return opts;
    }

    private static boolean validateInput(String jsonFilePath) {
        if(jsonFilePath == null) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Input JSON file cannot be null!");
            return false;
        } else {
            File jsonFile = new File(jsonFilePath);
            
            if(!jsonFile.isFile()) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "The input JSON file is not a file: {0}", jsonFilePath);
                return false;
            }
            if(!jsonFile.exists()) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "The input JSON file does not exist: {0}", jsonFilePath);
                return false;
            }
            if(!jsonFile.canRead()) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "The input JSON file cannot be read: {0}", jsonFilePath);
                return false;                
            }
        }
        return true;
    }
}
