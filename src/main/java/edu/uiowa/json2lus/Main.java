/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.json2lus;

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
     * Construct program options menu
     */        
    private static Options constructOptions() {
        Options opts    = new Options();               
        opts.addOption(Option.builder("i").longOpt("json-file").desc("Input json file").hasArgs().build());        
        opts.addOption("h", "help",     false, "Print this help information");
        opts.addOption("v", "version",  false, "Print tool version information");                
        return opts;
    }
    
    /**
     * This is the entry point of the program
     * 
     * @param args input parameters
     */
    public static void main(String[] args) {                  
        String              jsonFile    = null;   
        Options             opts        = constructOptions();               
        HelpFormatter       hf          = new HelpFormatter();        
        CommandLineParser   parser      = new DefaultParser();           
        
        try {
            CommandLine cl  = parser.parse(opts, args);
            
            if(cl.hasOption('h')) {
                hf.printHelp(" ", opts);
                return;
            }
            if(cl.hasOption('v')) {
                System.out.println("CocoSim_IR_Compiler 0.1");
                return;
            }
            if(cl.hasOption('i')) {
                jsonFile = cl.getOptionValue('i');
            }          
            if(jsonFile != null) {
                
            } else {
                System.out.println("Please provide an input json file!\n");
                hf.printHelp("CocoSim_IR_Compiler", opts, true);
            }

        } catch (ParseException ex) {
            System.err.println( "Parsing failed.  Reason: " + ex.getMessage() );
        }
    }     
}
