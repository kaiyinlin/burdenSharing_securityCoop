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
    public int nState = 100;
    public double probOfDemocracy = 0.7; // the probability of being democratic
    public double probOfEnemy = 0.3; // the probability of enemy type; two types of enemyType
    public String fileAddress = "/Users/kaiyinlin/Desktop/";
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

    public SimEnvironment(long seed, Class observer, String fileAddress, String inputDataDirectory){
        super(200, observer);
        setFileAddress(fileAddress);
        setInputDataDirectory(inputDataDirectory);

    }

    /*
     * ***************************************************************************
     * Start a Simulation
     * ***************************************************************************
     */
    public void start() {
        super.start();
        // should I reset all data here? Take a look of kh-model
        this.make2DSpace(spaces.SPARSE, gridWidth, gridHeight); // make the space
        getDataInformation();
        agentIdList = dataInformation.keySet().stream().collect(Collectors.toList());
        makeAgents();
        assignEnemies();
//		assignSimilarCulture();
//		for (Integer ii : agentIdList) {
//			System.out.println("id" + ii);
//			System.out.println(allAgents.get(ii).neighbors.toString());
//		}
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
        System.out.println("inputDataName = " + dataName);

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
        if (inputData == false) {
            int id = 0;
            if (nState > gridWidth * gridHeight) {
                System.out.println("Too many agents! Please reduce the number of agents.");
                return;
            }
            long seed = 20;
            Random r = new Random();
            r.setSeed(seed);
            for (int i = 0; i < Math.sqrt(nState); i++) {

                int xloc = i;
                for (int j = 0; j < Math.sqrt(nState); j++) {
                    int yloc = j;
                    double capability = r.nextDouble();
                    double random = r.nextDouble();
//					System.out.println("demecracy r = " + random);
                    boolean democracy = random < probOfDemocracy;
                    int culture = r.nextInt(7) + 1; // the culture categories range from 1-7
                    Agent c = new Agent(this, xloc, yloc, id, capability, democracy, culture);
                    this.allAgents.put(c.id, c);
//					System.out.println("c.capability = " + c.capability);
                    c.event = schedule.scheduleRepeating(c);
                    sparseSpace.setObjectLocation(c, xloc, yloc);
                    id++;
                } // end of yloc
            } // end of xloc
        } else { //if there is an input data

            // set up the agents
            for (Integer agentId : agentIdList) {
                int xloc = random.nextInt(gridWidth);
                int yloc = random.nextInt(gridHeight);
                int culture = 0;
                InfoIdentifier agentInfo = dataInformation.get(agentId);
                Agent c = new Agent(this, xloc, yloc, agentInfo.getId(), agentInfo.getCapability(),
                        agentInfo.getDemocracy() == 1, culture,
                        agentInfo.getNeighbor(), agentInfo.getAlliance(), agentInfo.getEnemy());
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
//                a.event = this.schedule.scheduleRepeating(this.getAgent(s), o, 1.0);
                o++;
            }

        }

    }

    /*
     * Assign initial enemies when no input data
     */
    public void assignEnemies() {
        if (!inputData) {
            // get all pair of n-agents
            List<Integer> agentIds = new ArrayList<Integer>(this.allAgents.keySet());
            // if they are, update both agents' enemy list
            Random r = new Random();
            long seed = 10;
            r.setSeed(seed);
            for (int i = 0; i < (agentIds.size() - 1); i++) {
                Agent a = this.allAgents.get(agentIds.get(i));
                for (int j = (i + 1); j < agentIds.size(); j++) {
                    Agent opponent = this.allAgents.get(agentIds.get(j));
//					boolean enemy = Math.random() < this.probOfEnemy;
                    double random = r.nextDouble();
//					System.out.println("enemy r = " + random);
                    boolean enemy = random < this.probOfEnemy;
                    if (enemy) {
                        a.SRG.add(agentIds.get(j));
                        opponent.SRG.add(agentIds.get(i));
//						System.out.println("enemy pair: " + agentIds.get(i) + ";   " + agentIds.get(j));
                    }
                }
            }
        }
    }


    /*
     * Assign similar culture
     */

//	public void assignSimilarCulture() {
//		if (dataName.length() > 0) {
//			for (Integer agentId : agentIdList) {
//				Agent a = this.allAgents.get(agentId);
//				InfoIdentifier agentInfo = dataInformation.get(agentId);
//				for(Integer agentID : agentIdList) {
//					Agent b = this.allAgents.get(agentId);
//					if(dataInformation.get(a.id).getCulture().get(b.id) == 1 && !a.equals(b)){
//						a.similarCulture.add(b.id);
//					}
//				}
//			}
//		}
//	}

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
    public double getProbOfDemocracy() {
        return probOfDemocracy;
    }

    public void setProbOfDemocracy(double probOfDemocracy) {
        this.probOfDemocracy = probOfDemocracy;
    }

    public double getProbOfEnemy() {
        return probOfEnemy;
    }

    public void setProbOfEnemy(double probOfEnemy) {
        this.probOfEnemy = probOfEnemy;
    }

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

    public int getnState() {
        return nState;
    }

    public void setnState(int nState) {
        this.nState = nState;
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
