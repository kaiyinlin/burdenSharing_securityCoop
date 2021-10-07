package burdenSharing2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import observer.Observer;
import sim.engine.SimState;
import sweep.ParameterSweeper;
import sweep.SimStateSweep;

public class Experimenter extends Observer {
    //output csv
    String fileDirectory = "/Users/kaiyinlin/Desktop/";

    DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    Date date = new Date();
    String fileName_alliance = dateFormat.format(date) + "alliance";
    String fileName_neighbor = dateFormat.format(date) + "neighbor";
    String fileName_enemy = dateFormat.format(date) + "enemy";
    String fileName_utility = dateFormat.format(date) + "utility";
    //	String fileName_culture = dateFormat.format(date) + "culture";
    String fileName_ALL = dateFormat.format(date) + "ALL";
    //	File allianceCSVFile = new File(fileDirectory + fileName_alliance + ".csv");
    //	File neighborCSVFile = new File("/Users/kaiyinlin/Desktop/" + fileName_neighbor + ".csv");
    //	File enemyCSVFile = new File("/Users/kaiyinlin/Desktop/" + fileName_enemy + ".csv");
    boolean headerWritten_a = false;
    boolean headerWritten_n = false;
    boolean headerWritten_e = false;
    boolean headerWritten_u = false;
    boolean headerWritten_ALL = false;

    String[] header_ALL = {"step", "year", "state_i", "state_j", "cap_i", "cap_j", "cultureSim", "democ_i", "democ_j", "neighbor", "enemy", "ally_ij", "u_ij", "currentU"};
    int stateHasNoNewAllies; //a variable to detect whether the simulation is converged

    // convergence related variable
    double convergeStep = 0;
    int converge = 0;
    int CONV_MAX = 2;  // need to continuous to converge for two times in order to terminate the simulation
    int MAX_ROUND = 15;

    /*
     * set fileDirectory
     */
    public void setFileDirectory(String directory) {
        this.fileDirectory = directory;
    }

    /*
     * Constructor
     */
    public Experimenter(String fileName, String folderName, SimStateSweep state, ParameterSweeper sweeper,
                        String precision, String[] headers) {
        super(fileName, folderName, state, sweeper, precision, headers);
        // TODO Auto-generated constructor stub
    }
    /*
     * ***************************************************************************
     *                  				STEP
     * ***************************************************************************
     */

    @Override
    public void step(SimState state) {
        super.step(state);
//		printDataInConsole();
//		nextInterval((SimEnvironment)state);
        try {
            System.out.println("WRITING!!!!!!!!!!!!!!");
            allianceMatrix(this.fileDirectory, (SimEnvironment) state);
//            neighborMatrix(this.fileDirectory, (SimEnvironment) state);
//            enemyMatrix(this.fileDirectory, (SimEnvironment) state);
//            utilityMatrix(this.fileDirectory, (SimEnvironment) state);
            dataCollection(this.fileDirectory, (SimEnvironment) state);

            // kill the simulation of continue to converge for two times
            System.out.println("stateHasNoNewAllies: " + stateHasNoNewAllies + "; Agents: " + ((SimEnvironment) state).agentIdList.size());
            if (stateHasNoNewAllies == ((SimEnvironment) state).agentIdList.size() && convergeStep == state.schedule.getTime() - 1) {
                converge++;
                convergeStep = state.schedule.getTime();
                System.out.println("converge: " + converge + "; step: " + state.schedule.getTime());
                if (converge == CONV_MAX) {
                    if (((SimEnvironment) state).appendInfo) {
                        appendInputInfo(this.fileDirectory, (SimEnvironment) state);
                    }
                    if (((SimEnvironment) state).deleteFile){
                        deleteFileInFileAddress((SimEnvironment) state);
                    }
//                    state.schedule.clear();
                    state.kill();
                    state.finish();
                }
            } else if (stateHasNoNewAllies == ((SimEnvironment) state).agentIdList.size()) {
                converge++;
                convergeStep = state.schedule.getTime();
            } else {
                converge = 0;
            }

            // kill the simulation if reaches maxround
            if (state.schedule.getTime() == MAX_ROUND) {
                if (((SimEnvironment) state).appendInfo) {
                    appendInputInfo(this.fileDirectory, (SimEnvironment) state);
                }
                System.out.println("Reach Maximum rounds! Terminate the simulation.");
//                state.schedule.clear();
                if (((SimEnvironment) state).deleteFile){
                    deleteFileInFileAddress((SimEnvironment) state);
                }
                state.kill();
                state.finish();
            }

            // schedule for next time
            System.out.println("Schedule Agents");
            List<Integer> scheduleList = Utils.getScheduleOrder((SimEnvironment) state);
//            state.schedule.clear();
            int o = 1;
            for (int s : scheduleList) {
                Agent a = ((SimEnvironment) state).getAgent(s);
//                a.event = state.schedule.scheduleRepeating(((SimEnvironment) state).getAgent(s), o, 1.0);
                state.schedule.scheduleOnce(((SimEnvironment) state).getAgent(s), o);
                o++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("END OF ROUND: " + (state.schedule.getSteps() + 1));
    }

    /*
     * ***************************************************************************
     *                  			Data collection
     * ***************************************************************************
     */
    public boolean nextInterval(SimEnvironment state) {
        for (int i = 0; i < agents.numObjs; i++) {
            Agent a = (Agent) agents.objs[i];
            for (int j = 0; j < agents.numObjs; j++) {
                Agent b = (Agent) agents.objs[j];
                data.add(a.id);//i's id
                data.add(b.id);//j's id
                data.add(a.capability);//i's capability
                data.add(b.capability);//j's capability
                data.add(state.dataInformation.get(a.id).getCulture().get(b.id));//culture similarity
                data.add(a.democracy == true ? 1 : 0);//i's democracy
                data.add(b.democracy == true ? 1 : 0);//j's democracy
                data.add(a.neighborList(state)[j]);//i's neighbors
                Set<Integer> allEnemies = Utils.getAllEnemies((SimEnvironment) state, a.id);
                int[] overallEnemyList = Utils.convertSetToEnemyList((SimEnvironment) state, allEnemies, agents.numObjs, a.SRG);
                data.add(overallEnemyList[j]);//i's enemies
                data.add(a.allianceList[j]);//i's allies
            }
        }
        return true;
    }

    /*
     * ***************************************************************************
     *                  			CSV output
     * ***************************************************************************
     */
    public void dataCollection(String fileDirectory, SimEnvironment state) throws IOException {
//        File dataCollectionCSVFile = new File(fileDirectory + fileName_ALL + ".csv");
        File dataCollectionCSVFile = new File(fileDirectory + state.year + "data.csv");
        if (!dataCollectionCSVFile.exists()) {
            System.out.println("Let's create a new csv file for data collection");
            dataCollectionCSVFile.createNewFile();
        }
        FileWriter writer = new FileWriter(dataCollectionCSVFile.getAbsoluteFile(), true);
        if (headerWritten_ALL == false) {
            for (int i = 0; i < header_ALL.length; i++) {
                if (i != 0) writer.append(",");
                writer.append(header_ALL[i]);
            }
            writer.append('\n');
        }
        headerWritten_ALL = true;
        for (int i = 0; i < state.agentIdList.size(); i++) {
            Agent a = state.getAgent(state.agentIdList.get(i));
            for (int j = 0; j < state.agentIdList.size(); j++) {
                Agent b = state.getAgent(state.agentIdList.get(j));
                int bIndex = state.getIndex(b.id);
                writer.append(Double.toString(state.schedule.getTime() + 1));
                writer.append(",");
                writer.append(Integer.toString(state.year)); //year
                writer.append(",");
                writer.append(Integer.toString(a.id));//i's id
                writer.append(",");
                writer.append(Integer.toString(b.id));//j's id
                writer.append(",");
                writer.append(Double.toString(a.capability));//i's capability
                writer.append(",");
                writer.append(Double.toString(b.capability));//j's capability
                writer.append(",");
                writer.append(Integer.toString(state.dataInformation.get(a.id).getCulture().get(b.id)));//culture similarity
                writer.append(",");
                writer.append(Integer.toString(a.democracy == true ? 1 : 0));//i's democracy
                writer.append(",");
                writer.append(Integer.toString(b.democracy == true ? 1 : 0));//j's democracy
                writer.append(",");
                writer.append(Integer.toString(a.neighborList(state)[bIndex]));//i's neighbors
                writer.append(",");
                Set<Integer> allEnemies = Utils.getAllEnemies((SimEnvironment) state, a.id);
                int[] overallEnemyList = Utils.convertSetToEnemyList((SimEnvironment) state, allEnemies, state.agentIdList.size(), a.SRG);
                writer.append(Integer.toString(overallEnemyList[bIndex]));//i's enemies
                writer.append(",");
                writer.append(Integer.toString(a.allianceList[bIndex]));//i's allies
                writer.append(",");
                writer.append(Double.toString(a.utilityOfAll[bIndex]));
                writer.append(",");
                writer.append(Double.toString(a.currentUtility(state, a))); //i's currentU
                writer.append('\n');
            }
        }
        writer.flush();
        writer.close();
    }

    public void allianceMatrix(String fileDirectory, SimEnvironment state) throws IOException {
        File allianceCSVFile = new File(fileDirectory + fileName_alliance + ".csv");
        if (!allianceCSVFile.exists()) {
            System.out.println("Let's create a new csv file for alliance matrix!");
            allianceCSVFile.createNewFile();
        }
        FileWriter writer = new FileWriter(allianceCSVFile.getAbsoluteFile(), true);
        if (headerWritten_a == false) {
            writer.append("Step");
            writer.append(',');
            writer.append("id");
            writer.append(',');
            for (int i : state.agentIdList) {
                if (i != 0) {
                    writer.append(",");
                }
                writer.append("c" + i);
            }
            writer.append('\n');
        }
        headerWritten_a = true;
        writer.append(" - Current Step Alliance- \n");
        stateHasNoNewAllies = 0;
        for (int i : state.agentIdList) {
            Agent a = state.getAgent(i);
            writer.append(Double.toString(state.schedule.getTime() + 1));
            writer.append(",");
            writer.append("c" + Integer.toString(a.id));
            writer.append(",");
//			System.out.println(Arrays.toString(a.allianceList));
            writer.append(Arrays.toString(a.currentStepAllianceList).replace("[", "").replace("]", ""));
            a.alliance = Stream.of(a.alliance, a.currentStepAlliance)
                    .flatMap(x -> x.stream())
                    .collect(Collectors.toSet());

            int[] newAllianceList = new int[state.agentIdList.size()];
            Arrays.fill(newAllianceList, 0);
            if (a.currentStepAlliance.size() == 0) {
                stateHasNoNewAllies++;
            }
            if (a.alliance.size() > 0) {
                for (int ca : a.alliance) {
//					a.alliance.add(ca.id);
                    newAllianceList[state.getIndex(ca)] = 1;
                }
            }
            a.allianceList = newAllianceList;
            a.currentStepAlliance.clear();
            writer.append('\n');
        }

        writer.append(" - Alliance Matrix- \n");
        for (int i : state.agentIdList) {
            Agent a = state.getAgent(i);
            writer.append(Double.toString(state.schedule.getTime() + 1));
            writer.append(",");
            writer.append("c" + Integer.toString(a.id));
            writer.append(",");
//			System.out.println(Arrays.toString(a.allianceList));
            writer.append(Arrays.toString(a.allianceList).replace("[", "").replace("]", ""));
            writer.append('\n');
        }
        writer.flush();
        writer.close();
    }

    /*
     * neighbor matrix
     */
    public void neighborMatrix(String fileDirectory, SimEnvironment state) throws IOException {

        File neighborCSVFile = new File(fileDirectory + fileName_neighbor + ".csv");
        if (!neighborCSVFile.exists()) {
            System.out.println("Let's create a new csv file for neighbor matrix!");
            neighborCSVFile.createNewFile();
        }
        FileWriter writer = new FileWriter(neighborCSVFile.getAbsoluteFile(), true);
        if (headerWritten_n == false) {
            writer.append("Step");
            writer.append(',');
            writer.append("id");
            writer.append(',');
            writer.append("capability");
            writer.append(',');
            writer.append("democracy");
            writer.append(',');
            for (int i : state.agentIdList) {
                if (state.agentIdList.size() != 0) {
                    writer.append(",");
                }
                writer.append("c" + i);
            }
            writer.append(',');
            writer.append("initialNeed");
            writer.append(',');
            writer.append("currentUtility");
            writer.append('\n');
        }
        headerWritten_n = true;
        for (int i : state.agentIdList) {
            Agent a = state.getAgent(i);
            writer.append(Double.toString(state.schedule.getTime() + 1));
            writer.append(",");
            writer.append("c" + Integer.toString(a.id));
            writer.append(",");
            writer.append(Double.toString(a.attribute[0]));
            writer.append(",");
            writer.append(Double.toString(a.attribute[2]));
            writer.append(",");
//			System.out.println(Arrays.toString(a.neighborList));
            writer.append(Arrays.toString(a.neighborList((SimEnvironment) state)).replace("[", "").replace("]", ""));
            writer.append(",");
            writer.append(Double.toString(Utils.initialNeed((SimEnvironment) state, a)));
            writer.append(",");
            writer.append(Double.toString(a.currentUtility((SimEnvironment) state, a)));
            writer.append('\n');
        }
        writer.flush();
        writer.close();
    }


    /*
     * enemy matrix
     */
    public void enemyMatrix(String fileDirectory, SimEnvironment state) throws IOException {

        File enemyCSVFile = new File(fileDirectory + fileName_enemy + ".csv");
        if (!enemyCSVFile.exists()) {
            System.out.println("Let's create a new csv file for enemy matrix!");
            enemyCSVFile.createNewFile();
        }
        FileWriter writer = new FileWriter(enemyCSVFile.getAbsoluteFile(), true);
        if (headerWritten_e == false) {
            writer.append("Step");
            writer.append(',');
            writer.append("id");
            writer.append(',');
            for (int i : state.agentIdList) {
                if (state.agentIdList.size() != 0) {
                    writer.append(",");
                }
                writer.append("c" + i);
            }
            writer.append('\n');
        }
        headerWritten_e = true;
        for (int i : state.agentIdList) {
            Agent a = state.getAgent(i);
            writer.append(Double.toString(state.schedule.getTime() + 1));
            writer.append(",");
            writer.append("c" + Integer.toString(a.id));
            writer.append(",");
            // get all Enemies
            Set<Integer> allEnemies = Utils.getAllEnemies((SimEnvironment) state, a.id);
            int[] overallEnemyList = Utils.convertSetToEnemyList((SimEnvironment) state, allEnemies, state.agentIdList.size(), a.SRG);
            writer.append(Arrays.toString(overallEnemyList).replace("[", "").replace("]", ""));
            writer.append('\n');
        }
        writer.flush();
        writer.close();
    }

    /*
     * utility matrix
     */
    public void utilityMatrix(String fileDirectory, SimEnvironment state) throws IOException {

        File utilityCSVFile = new File(fileDirectory + fileName_utility + ".csv");
        if (!utilityCSVFile.exists()) {
            System.out.println("Let's create a new csv file for utility matrix!");
            utilityCSVFile.createNewFile();
        }
        FileWriter writer = new FileWriter(utilityCSVFile.getAbsoluteFile(), true);
        if (headerWritten_u == false) {
            writer.append("Step");
            writer.append(',');
            writer.append("id");
            writer.append(',');
            for (int i : state.agentIdList) {
                if (state.agentIdList.size() != 0) {
                    writer.append(",");
                }
                writer.append("c" + i);
            }
            writer.append('\n');
        }
        headerWritten_u = true;
        for (int i : state.agentIdList) {
            Agent a = state.getAgent(i);
            writer.append(Double.toString(state.schedule.getTime() + 1));
            writer.append(",");
            writer.append("c" + Integer.toString(a.id));
            writer.append(",");
            writer.append(Arrays.toString(a.utilityOfAll).replace("[", "").replace("]", ""));
            writer.append('\n');
        }
        writer.flush();
        writer.close();
    }

    public static void appendInputInfo(String fileDirectory, SimEnvironment state) throws IOException {
        String appendDataName = state.appendDataName;
        File outputFileName = new File(fileDirectory + state.appendYear + ".csv");
        Map<Integer, Agent> allAgents = state.allAgents;
        List<String> lines;
        List<String[]> data = null;
        Path path = Paths.get(appendDataName);
        List<String> resultLists = new ArrayList<>();
        String header = null;
        String[] headerArray = null;
        try {
            lines = Files.readAllLines(path);
            data = lines.stream().skip(1).map(line -> line.split(",")).collect(Collectors.toList());
            headerArray = Files.lines(path)
                    .map(s -> s.split(","))
                    .findFirst()
                    .get();
            if (headerArray.length == 10) {
                header = String.join(",", headerArray) + ",alliance\n";
            } else {
                header = String.join(",", headerArray) + "\n";
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // add information and write
        String result;
        FileWriter writer = new FileWriter(outputFileName.getAbsoluteFile());
        writer.write(header);
        for (String[] lst : data) {
            Integer i = Integer.valueOf(lst[1]);
            Integer j = Integer.valueOf(lst[2]); //key
            int a = 0;
            if (allAgents.containsKey(i) && allAgents.get(i).alliance.contains(j)) {
                a = 1;
            }
            if (headerArray.length == 10) {
                result = String.join(",", lst) + "," + a + "\n";
            } else {
                lst[10] = String.valueOf(a);
                result = String.join(",", lst) + "\n";
            }
            resultLists.add(result);
            writer.write(result);

        }
        writer.flush();
        writer.close();

        if (state.copyBack) {
            Path copied = Paths.get(outputFileName.getAbsolutePath());
            Path originalPath = Paths.get(state.appendDataName);
            Files.copy(copied, originalPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("copy to " + state.appendDataName);

        }
    }

    public void deleteFileInFileAddress(SimEnvironment state) throws IOException {
        if (state.deleteFile){
            // list the file that needs to be deleted
            Path outputFile = Paths.get(this.fileDirectory, state.appendYear + ".csv");
            Files.delete(outputFile);
            System.out.println("File " + outputFile.toString() + " deleted");

            Path allianceFile = Paths.get(this.fileDirectory, fileName_alliance + ".csv");
            Files.delete(allianceFile);
            System.out.println("File " + allianceFile.toString() + " deleted");


        }else{
            System.out.println("No files are Deleted in " + state.fileAddress);
        }
    }

    public ArrayList<Integer> commonAlliance(Agent a, Agent b) {
        if (a.currentStepAlliance == null)
            return null;
        Set<Integer> intersection = a.currentStepAlliance.
                stream().distinct().
                filter(b.currentStepAlliance::contains).collect(Collectors.toSet());
        ArrayList<Integer> commonAlliance = new ArrayList<Integer>();
        if (intersection.size() == 0 || intersection == null)
            return null;
        else {
            for (int ca : intersection) {
                commonAlliance.add(ca);
            }
            return commonAlliance;
        }
    }

    public void printDataInConsole() {
        System.out.println("Step   a.id   b.id   commonAlliance");
        for (int i = 0; i < agents.numObjs; i++) {
            Agent a = (Agent) agents.objs[i];
            for (int j = 0; j < agents.numObjs; j++) {
                Agent b = (Agent) agents.objs[j];
                if (a == b) {
                    System.out.println(state.schedule.getSteps() + "   " + a.id + "   " + b.id + "    null");
                } else {
                    System.out.println(state.schedule.getSteps() + "   " + a.id + "   " + b.id + "   " + commonAlliance(a, b));
                }

            }
        }
    }

//	/*
//	 * culture similarity matrix
//	 */
//	public void cultureMatrix(String fileDirectory) throws IOException{
//
//		File cultureCSVFile = new File(fileDirectory + fileName_culture + ".csv");
//		if(!cultureCSVFile.exists()) {
//			System.out.println("Let's create a new csv file for culture similarity matrix!");
//			cultureCSVFile.createNewFile();
//		}
//		FileWriter writer = new FileWriter(cultureCSVFile.getAbsoluteFile(), true);
//		if(headerWritten_c == false) {
//			writer.append("Step");
//			writer.append(',');
//			writer.append("id");
//			writer.append(',');
//			for(int i=0; i<agents.numObjs; i++) {
//				if(i != 0) {
//					writer.append(",");
//				}
//				writer.append("c"+i);
//			}
//			writer.append('\n');
//		}
//		headerWritten_c = true;
//		for(int i=0; i<agents.numObjs; i++) {
//			Agent a = (Agent)agents.objs[i];
//			writer.append(Double.toString(state.schedule.getTime()+1));
//			writer.append(",");
//			writer.append("c" + Integer.toString(a.id));
//			writer.append(",");
//			// get same culture countries
//			writer.append(Arrays.toString(a.cultureList((SimEnvironment)state)).replace("[", "").replace("]", ""));
//			writer.append('\n');
//		}
//		writer.flush();
//		writer.close();
//	}

}


