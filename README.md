### burdenSharing_securityCoop V7 - gitHub branch

### process of automatically run

**To run the simulation automatically**, a runnable jar with bash file should be created and place at the same folder (usually on Desktop). The years, name of the jar, input and output directory should be identified in the bashCommend.sh file. The process is following:
* Identify the info in bash file
* open terminal, directory to the folder where the jar file and bash file is.
* use bash bashCommend.sh to run the simulation automatically

**To change the simulation to manually run**, the controller is located at the graphicGUI. The variable is "autoMode".

This is BurdenSharing2_2021-10-12, which update all matrix after each agent's move. Copy from 2021-10-05.
New updates: 
* (1) include original enemies and allies of enemies only. 
* (2) When providing offers: if currentU > 0.2 stop making friends; if currentU < 0 && abs(currentU) > abs(beforeU), stop making friends
* (3) When accepting an offer: if the current utility of allyJ is < 0, accept the offer (data_13)

Updates:
* 2021-10-14: When accepting the offer: if the current utility of allyJ is < 0 and usage of potential ally > 0, accept the offer (data_14)

**Need to use MASON Plus 8 + **



