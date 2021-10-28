package burdenSharing2;

import spaces.Spaces;
import sweep.SimStateSweep;

import java.util.Map;
import java.util.Random;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class SimEnvironment extends SimStateSweep {

    /*
     * Burden sharing parameters
     */
    public String fileAddress = "/Users/kaiyinlin/Desktop/merger/";
    public String inputDataDirectory = "/Users/kaiyinlin/Desktop/dataByYear/";
    public boolean inputData = true;
    public boolean copyBack = true;
    public boolean deleteFile = true; // delete alliance and year.csv file in the fileAddress after copy back to original directory
    public boolean appendInfo = true;
    public int year = 1816;
    public String dataName;
    public int appendYear;
    public String appendDataName;
    public Map<Integer, Agent> allAgents = new HashMap<Integer, Agent>();
    public Input dataInput;
    public Map<Integer, InfoIdentifier> dataInformation;
    public List<Integer> agentIdList;

    public Experimenter experimenter = null;

    /*
     * *******************************************************************
     * Constructor
     * *******************************************************************
     */
    public SimEnvironment(long seed, Class observer) {
        super(200, observer);
        // TODO Auto-generated constructor stub
    }

    public SimEnvironment(long seed, Class observer,
                          String fileAddress,
                          String inputDataDirectory,
                          int year){
        super(200, observer);
        setFileAddress(fileAddress);
        setInputDataDirectory(inputDataDirectory);
        setYear(year);
    }

    /*
     * ***************************************************************************
     * Start a Simulation
     * ***************************************************************************
     */
    public void start() {
        super.start();
        this.make2DSpace(spaces.SPARSE, gridWidth, gridHeight); // make the space
        getDataInformation();
        agentIdList = dataInformation.keySet().stream().collect(Collectors.toList());
        makeAgents();
        System.out.println("END OF MAKEAGENTS ================");
        if (observer != null) {
            observer.initialize(sparseSpace, Spaces.SPARSE);// initialize the experimenter by calling initialize in the
            // parent class
            experimenter = (Experimenter) observer;// cast observer as experimenter
            experimenter.setFileDirectory(fileAddress);

        }
    }

    /*
     * ***************************************************************************
     * Get Data Information
     * ***************************************************************************
     */

    public void getDataInformation() {
        dataName =  inputDataDirectory + Integer.toString(year) + ".csv";
        dataInput = new Input(dataName);
        this.dataInformation = dataInput.getDataInformation();

        // set up output information
        appendYear = year + 1;
        appendDataName = inputDataDirectory + Integer.toString(appendYear) + ".csv";
    }

    /*
     * ***************************************************************************
     * Making states (Agents) when no input data
     * ***************************************************************************
     */
    public void makeAgents() {
    	for (Integer agentId : agentIdList) {
            int xloc = random.nextInt(gridWidth);
            int yloc = random.nextInt(gridHeight);
            int culture = 0;
            InfoIdentifier agentInfo = dataInformation.get(agentId);
            Agent c = new Agent(this, xloc, yloc, agentInfo.getId(), agentInfo.getCapability(),
                    agentInfo.getDemocracy(), culture,
                    agentInfo.getNeighbor(), agentInfo.getAlliance(), agentInfo.getEnemy());
            if(c.id == 260 || c.id == 345) {
            	System.out.println("agent_id: "+ c.id);
                System.out.println("neighbors: " + c.neighbors.toString());
            }
            
            c.utilityOfAll = new double[agentIdList.size()];
            Arrays.fill(c.utilityOfAll, 0);
            this.allAgents.put(c.id, c);
            sparseSpace.setObjectLocation(c, xloc, yloc);
        }

        // put one by one to schedular
        List<Integer> scheduleList = Utils.getScheduleOrder(this);
        int o = 1;
        for (int s : scheduleList) {
            Agent a = this.getAgent(s);
            schedule.scheduleOnce(allAgents.get(s), o);
            o++;
        }

    }


    /*
     * Get a specific agent by input its id
     */
    public Agent getAgent(int agentId) {
        return allAgents.get(agentId);
    }

    public int getIndex(Integer agentId) {
        ArrayList<Integer> arrlistofOptions = new ArrayList<Integer>(agentIdList);
        return arrlistofOptions.indexOf(agentId);
    }


    /*
     * *****************************************************************************
     * **** Getters and Setters
     * *****************************************************************************
     * ****
     */

    public String getFileAddress() {
        return fileAddress;
    }

    public void setFileAddress(String fileAddress) {
        this.fileAddress = fileAddress;
    }

    public String getInputDataDirectory() {
        return inputDataDirectory;
    }

    public void setInputDataDirectory(String inputDataDirectory) {
        this.inputDataDirectory = inputDataDirectory;
    }

    public boolean isInputData() {
        return inputData;
    }

    public void setAppendInfo(boolean appendInfo) {
        this.appendInfo = appendInfo;
    }

    public boolean isAppendInfo() {
        return appendInfo;
    }

    public void setCopyBack(boolean copyBack) {
        this.copyBack = copyBack;
    }

    public boolean isCopyBack() {
        return copyBack;
    }

    public void setDeleteFile(boolean deleteFile){ this.deleteFile = deleteFile; }

    public boolean isDeleteFile(){ return deleteFile; }

    public void setInputData(boolean inputData) {
        this.inputData = inputData;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

}
