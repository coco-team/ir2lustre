# Ir2Lustre
Ir2Lustre is a translator from the intermediate representation (JSON) of CoCoSim models to Lustre programs. 

# Installation

###Install from source
Dependency: [Maven](https://maven.apache.org/install.html)

1. Download and install Maven
2. Download the srouce
3. Change directory to the project main directory on terminal
4. Issue command on terminal: `mvn install`
5. The final binary will be placed in `/ir2lustre/target/CocoSim_IR_Compiler-0.1-jar-with-dependencies.jar`

# Usage

To run the tool, you need to issue the command: 

`java -jar CocoSim_IR_Compiler-0.1-jar-with-dependencies.jar -i \path\to\your\simulink_model`


Below shows the usage information about the tool.

`usage: CocoSim_IR_Compiler-0.1-jar-with-dependencies.jar [-h] [-i <arg>] [-m] [-o <arg>] [-v]`
 `-i, --json-file <arg>     Input json file path`   
 `-h, --help                Print this help information`   
 `-m                        Print multiple properties in one node!`
 `-o, --lustre-file <arg>   Output lustre file path`
 `-v, --version             Print tool version information`