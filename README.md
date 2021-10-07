### process of automatically run

**To run the simulation automatically**, a runnable jar with bash file should be created and place at the same folder (usually on Desktop). The years, name of the jar, input and output directory should be identified in the bashCommend.sh file. The process is following:
* Identify the info in bash file
* open terminal, directory to the folder where the jar file and bash file is.
* use bash bashCommend.sh to run the simulation automatically

**To change the simulation to manually run**, the controller is located at the graphicGUI. The variable is "autoMode".

This is BurdenSharing2_2021-10-05, which update all matrix after each agent's move. Copy from 2021-10-01.
New updates: 
* (1) include original enemies and allies of enemies only. 
* (2) if currentU>=0.2 or <=0.2, stop making friends



**Need to use MASON Plus 8 + **

