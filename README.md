### burdenSharing_securityCoop v1.08 : github version



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
* (3) secondary enemies rate: 0.5
* (4) cost term = $0.2 \times T^{1.2}$ (Data_v11); cost term = $0.2 \times T^2$ (Data_v12)

Update 2021-10-27

* (1) if currentU > 0.2 or currentU < -0.2, stop making alliance
* (2) change the sequence of data reading: i --> j
* (3) correct the use of currentStateAlliance and currentStepAlliance. At some circumstance, should use currentStateAlliance instead of currentStepAlliance

Update 2021-11-10
* (1) cost term before 1945 = $$0.2 \times T^2$$ ; cost term after 1945 = $$0.2 \times T^1.2$$ (data v_16 & v_17(with revised enemies)
* (2) re-wiring rules changed. 

Update 2021-11-11
* (1) rule adding: check the enemies and allies at the beginning of the step. If two countries are enemies and allies at the same time, release the alliance.

Update 2021-11-17
* (1) the added rule on 11/11 was wrong. The alliance set was local and not updated for the whole model. Fixed the problem on 11-17.


**Need to use MASON Plus 8 + **

** GitHub -- burdenSharing_securityCoop V6**

