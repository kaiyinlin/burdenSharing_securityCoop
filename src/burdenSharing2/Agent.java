package burdenSharing2;

import java.awt.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Iterator;
import java.util.List;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.util.Bag;
import sweep.GUIStateSweep;

/**
 * This is BurdenSharing2_2021-09-03, which update all matrix after each agent's move. Copy from 2021-08-21.
 * New: (1) adjust the cost term of enemies only (2) use the original A_ij and A_kj parameters
 * 
 * @author kaiyinlin
 */
public class Agent implements Steppable {
    //agent
    long currentStep;
    public Stoppable event;
    int x; //x location, coordinates
    int y; //y location, coordinates
    int id; //agent's id
    double capability; //agent's capability
    int democracy; //determine if a country is democratic or not
    //	boolean enemy;
    int culture;
    double cost; //alliance cost depending on the number of allies
    double[] attribute = new double[3];
    double orderingUtility;


    //relations
    Set<Integer> SRG; //only use for calculate utitlity
    Set<Integer> neighbors;
    Set<Integer> potentialAllies;
    Set<Integer> currentStepAlliance;
    Set<Integer> alliance;


    //data output list (matrices)
    int[] allianceList;
    int[] currentStepAllianceList;
    double[] utilityOfAll;

    /*
     * **********************************************************************
     *                          Constructor
     * **********************************************************************
     */
    public Agent(SimEnvironment state, int x, int y, int id, double capability, int democracy, int culture) {
        super();
        this.x = x;
        this.y = y;
        this.id = id;
        this.capability = capability;
        this.democracy = democracy;
        this.culture = culture;
        //attribute vector
        attribute[0] = capability;
        attribute[1] = (double) culture;
        if (this.democracy == 1)
            attribute[2] = 1;
        else
            attribute[2] = 0;
        //relations
        SRG = new HashSet<Integer>();
        neighbors = new HashSet<Integer>();
        potentialAllies = new HashSet<Integer>();
        currentStepAlliance = new HashSet<Integer>();
        alliance = new HashSet<Integer>();
//		similarCulture = new HashSet<Integer>();

        //set the color for agent based on the enemyTypeA
        float colorCapability = (float) this.capability;
        if (democracy == 1)
            setColor(state, (float) 0, (float) 0, (float) 1, (float) colorCapability);//typeA, blue color
        else
            setColor(state, (float) 1, (float) 0, (float) 0, (float) colorCapability); //typeB, red color

    }

    public Agent(SimEnvironment state, int x, int y, int id, double capability, int democracy, int culture,
                 Set<Integer> neighbors, Set<Integer> alliance, Set<Integer> SRG) {
        super();
        this.x = x;
        this.y = y;
        this.id = id;
        this.capability = capability;
        this.democracy = democracy;
        this.culture = culture;
        //attribute vector
        attribute[0] = capability;
        attribute[1] = (double) culture;
        if (this.democracy == 1)
            attribute[2] = 1;
        else
            attribute[2] = 0;
        //relations
        this.SRG = SRG;
        this.alliance = alliance;
        this.neighbors = neighbors;
        currentStepAlliance = new HashSet<Integer>();
        potentialAllies = new HashSet<Integer>();
//        System.out.println(this.alliance.toString());
//		similarCulture = new HashSet<Integer>();

        //set the color for agent based on the enemyTypeA
        float colorCapability = (float) this.capability;
        if (democracy == 1)
            setColor(state, (float) 0, (float) 0, (float) 1, (float) colorCapability);//typeA, blue color
        else
            setColor(state, (float) 1, (float) 0, (float) 0, (float) colorCapability); //typeB, red color

    }


    /*
     * ***************************************************************************
     *                               STEP
     * ***************************************************************************
     */
    @Override
    public void step(SimState state) {
        System.out.println("CurrentStep: "+ state.schedule.getTime());
        currentStep = state.schedule.getSteps();
        SimEnvironment Environment = (SimEnvironment) state;
        System.out.println("this.id = " + this.id + "    currentStep = " + state.schedule.getTime() + "   ordering utility = " + this.orderingUtility);
//		System.out.println("last step alliance.size = " + this.alliance.size());
        if (currentStep == 0 && Environment.dataName.length() == 0) {
            //define neighbors
            findNeighbors((SimEnvironment) state); //define neighbors
        }
        //calculate the utility for each other agent
        allUtility(Environment, this);
//		System.out.println("utilityOfAll = " + Arrays.toString(utilityOfAll));
        //*********************END OF CALCULATE THE UTILITY OF ALL******************************
        allianceList = new int[Environment.nState];
        potentialAllies = Utils.getPotentialAllies((SimEnvironment) state, this.id);
//		System.out.println("potentialAllies =" + potentialAllies.toString());
        Agent allyJ = highestRanked((SimEnvironment) state, potentialAllies); //highest rank
        if (currentStepAlliance.isEmpty()) { //if this country has no friend so far
            //check if need MORE partners, if so, provide the offer
            provideOffer(Environment, allyJ);
        } else { //if this country already has some friends
            //compare this agent's potential allies with current allies
            Agent currentLowestRanked = lowestRanked(Environment, this);
            if (Utils.utility(Environment, this, allyJ) > Utils.utility(Environment, this, currentLowestRanked)) { //if current allies are bad
                resetCurrentAllies(Environment, this);
                provideOffer(Environment, allyJ);
            } else { //if current allies are good
                provideOffer(Environment, allyJ);
            }
        }
        //**************************END OF MAKING FRIENDS*********************************************************
        //update the allianceList for all agents
        updateAllCurrentStepAllianceList(Environment, Environment.allAgents);
        updateAllUtility(Environment.allAgents);
        updateAllEnemyList(Environment, Environment.allAgents); //this is only for data checking, should not include to potential ally calculation

    }

    /*
     * **************************************************************************
     *     						utility Of All
     * **************************************************************************
     */
    public void allUtility(SimEnvironment state, Agent a) {
        //calculate the utility for each other agent
        a.utilityOfAll = new double[state.agentIdList.size()];
        for (Agent j : state.allAgents.values()) {
            int index = state.getIndex(j.id);
            if (a.equals(j) || Utils.getAllEnemies(state, a.id).contains(j.id)) {
                a.utilityOfAll[index] = 0;
            } else {
                a.utilityOfAll[index] = Utils.utility((SimEnvironment) state, a, j);
            }

        } //end of utility
    }

    /*
     * **************************************************************************
     *     						update the allianceList
     * **************************************************************************
     */

    public void updateAllCurrentStepAllianceList(SimEnvironment state, Map<Integer, Agent> allAgents) {
        for (Agent a : allAgents.values()) {
            int[] newAllianceList = new int[allAgents.size()];
            Arrays.fill(newAllianceList, 0);
            if (a.currentStepAlliance.size() > 0) {
                for (int ca : a.currentStepAlliance) {
//					a.alliance.add(ca.id);
                    newAllianceList[state.getIndex(ca)] = 1;
                }
            }
            a.currentStepAllianceList = newAllianceList;
//			System.out.println("a.id = " + a.id + "    currentStepAllianceList = " + Arrays.toString(a.currentStepAllianceList));
        }
    }

    public void updateAllEnemyList(SimEnvironment state, Map<Integer, Agent> allAgents) {
        for (Agent a : allAgents.values()) {
            Set<Integer> currentEnemy = Utils.getAllEnemies(state, a.id);
            int[] currentEnemyList = Utils.convertSetToList(state, currentEnemy, allAgents.size());
//			System.out.println("a.id = " + a.id + "    currentEnemy = " + Arrays.toString(currentEnemyList));
        }
    }

    public void updateAllUtility(Map<Integer, Agent> allAgents) {
        for (Agent a : allAgents.values()) {
//			System.out.println("a.id = " + a.id + "    utilityList = " + Arrays.toString(a.utilityOfAll));
        }
    }

    public void setOrderingUtility(double u){
        this.orderingUtility = u;
    }

    /*
     * **************************************************************************
     *     						Coloring the Groups
     * **************************************************************************
     */
    public void setColor(SimEnvironment state, float red, float green, float blue, float opacity) {
        Color c = new Color(red, green, blue, opacity);
        OvalPortrayal2D o = new OvalPortrayal2D(c);
        GUIStateSweep guiState = (GUIStateSweep) state.gui; //I don't quite understand here
        if (state.sparseSpace != null) { //if the groups are in a sparseGrid2D space
            guiState.agentsPortrayalSparseGrid.setPortrayalForObject(this, o); //the second argument is color???
        }
    }

    /*
     * ******************************************************************************
     * 						      Relationship Recognition
     * ******************************************************************************
     */

    /*
     * Define neighbors
     */
    public Set<Integer> findNeighbors(SimEnvironment state) {
//		Bag allAgents = state.sparseSpace.getAllObjects();
        for (Agent p : state.allAgents.values()) {
//			Agent p = (Agent) allAgents.objs[i]; //look for a potential neighbor p
            if (!(this.equals(p))) { //avoid counting itself as a neighbor
                if (p.x == this.x) {
                    if (this.y == 0) { //deal with torus problem
                        if (p.y == Math.sqrt(state.nState) - 1 || p.y == 1) this.neighbors.add(p.id);
                    } else if (this.y == Math.sqrt(state.nState) - 1) { //deal with torus problem
                        if (p.y == 0 || p.y == Math.sqrt(state.nState) - 2) this.neighbors.add(p.id);
                    } else { //in the middle
                        if (p.y == this.y - 1 || p.y == this.y + 1) { //add two neighbors
                            this.neighbors.add(p.id);
                        }
                    }
                }//end p.x==this.x
                if (p.y == this.y) {
                    if (this.x == 0) {//deal with torus problem
                        if (p.x == Math.sqrt(state.nState) - 1 || p.x == 1) this.neighbors.add(p.id);
                    } else if (this.x == Math.sqrt(state.nState) - 1) {//deal with torus problem
                        if (p.x == 0 || p.x == Math.sqrt(state.nState) - 2) this.neighbors.add(p.id);
                    } else {//in the middle
                        if (p.x == this.x - 1 || p.x == this.x + 1) { //add two neighbors
                            this.neighbors.add(p.id);
                        }
                    }
                } //end p.y=this.y
            }
        }//end of for
//		System.out.println("this id = " + this.id + "how many neighbors = " + neighbors.size());
        return neighbors;
    }

    /*
     * NeighborList for experimenter to output neighbor matrix
     */
    public int[] neighborList(SimEnvironment state) {
        int[] neighborList = new int[state.agentIdList.size()];
        Arrays.fill(neighborList, 0);
        for (int a : neighbors) {
            neighborList[state.getIndex(a)] = 1;
//			System.out.println("neighbor's id = " + a);
        }
        return neighborList;
    }
    /*
     * cultureList for experimenter to output culture matrix
     */
//	public int[] cultureList(SimEnvironment state){
//		Bag allAgents = state.sparseSpace.getAllObjects();
//		int[] cultureList = new int[allAgents.numObjs];
//		Arrays.fill(cultureList, 0);
//		for(int a : similarCulture) {
//			cultureList[state.agentIdList.indexOf(a)] = 1;
//		}
//		System.out.println("similarCulture = " + similarCulture.toString());
//		System.out.println("cultureList = " + Arrays.toString(cultureList));
//		return cultureList;
//	}

    /*
     * ****************************************************************************************
     * 									Making Offer
     * ****************************************************************************************
     */
    /*
     * Find out friends, and rank their utilities. Choose the highest ranked agents to offer the alliance
     */
    public Agent highestRanked(SimEnvironment state, Set<Integer> potentialAllies) {
//		Bag allAgents = state.sparseSpace.getAllObjects(); 
        if (potentialAllies.isEmpty() || potentialAllies.size() == 0) {
            return null;
        }
        double utilityBase = -10000;
        Agent maxUtilityAgent = null;
        for (int pA : potentialAllies) {
            int paIndex = state.getIndex(pA);
            double utilityOfpA = utilityOfAll[paIndex];
            if (utilityOfpA > utilityBase) {
//				System.out.println("replace max Utility Agent, with utility "+ utilityOfpA);
                Agent potentialA = state.allAgents.get(pA);
                maxUtilityAgent = potentialA;
                utilityBase = utilityOfpA;
            }
//			System.out.println("Current Max Utility Agent "+ maxUtilityAgent.id);
//			System.out.println("Current Utility Base "+ utilityBase);
        }
        return maxUtilityAgent;
    }

    /*
     * Find the lowest ranked of current Alliies. If the new offer is better than old alliance, replace the old allies
     */
    public Agent lowestRanked(SimEnvironment state, Agent agent) {
        if (agent.currentStepAlliance.isEmpty() || agent.currentStepAlliance.size() == 0) {
            return null;
        }
        double utilityBase = 10000;
        Agent minUtilityAgent = null;
        for (int ca : agent.currentStepAlliance) {
            Agent currentAlly = state.allAgents.get(ca);
            double utilityOfcurrentAlly = Utils.utility(state, agent, currentAlly);
            if (utilityOfcurrentAlly < utilityBase) {
                minUtilityAgent = currentAlly;
                utilityBase = utilityOfcurrentAlly;
            }
        }
        return minUtilityAgent;
    }

    /*
     * provide the offer:
     */
    public void provideOffer(SimEnvironment state, Agent allyJ) {
        boolean needMorePartners;
        if (currentUtility(state, this) >= 0)
//        if (currentUtility(state, this) >= 0.2 || currentUtility(state, this) <= -0.2)
            needMorePartners = false;//if needMorePartners == false, then stop making friends
        else needMorePartners = true;
        int whileLoopNum = 0;
        int initialPotentialAlliesSize = this.potentialAllies.size();
        if (needMorePartners == false)
//			System.out.println("No Need Partners!");
            if (whileLoopNum == initialPotentialAlliesSize - 1) {
//			System.out.println("whileLoopNum = " + whileLoopNum);
//			System.out.println("LAST LOOP! WHILE LOOP MAXIMUM IS REACHING !!!!!!!!!!!!!!!!!!!!!!!!!");
            }
        while (needMorePartners && whileLoopNum < initialPotentialAlliesSize) { //making some friends
//			System.out.println("whileLoopNum = " + whileLoopNum);
//			System.out.println("maxLoopSize (potential ally size)= " + initialPotentialAlliesSize);
//			System.out.println("need more partners!");
            Set<Integer> newPotentialAllies = potentialAllies;
            allyJ = highestRanked((SimEnvironment) state, newPotentialAllies); //highest rank
            //accepting offer
            if (allyJ != null) {
                //provide offer to highest-ranked agent and see if it accepts the offer
//				System.out.println("highest ranked cadidate id = " + allyJ.id);
                acceptOffer((SimEnvironment) state, allyJ);
                potentialAllies.remove(allyJ.id); //remove the highest ranked ally from potentialAllies list
            }
            if (currentUtility(state, this) >= 0) needMorePartners = false;
//            if (currentUtility(state, this) >= 0.2 || currentUtility(state, this) <= -0.2) needMorePartners = false;
            else needMorePartners = true;
            whileLoopNum++;

        }
    }

    /*
     * Accept the offer : The potential ally needs to decide if it would like to accept the offer
     */
    public boolean acceptOffer(SimEnvironment state, Agent allyJ) {
        Set<Integer> allyJpotentialAllies = Utils.getPotentialAllies(state, allyJ.id);
//		System.out.println("allyJPotentialAllies = " + allyJpotentialAllies.toString()); //checking
        if (allyJ.utilityOfAll == null) {
//			System.out.println("getAllEnemies to evaluate offer = " + Utils.getAllEnemies(state, allyJ.id).toString());
            allUtility(state, allyJ);
        }
        //check if the offer giver is one of the potential allies
        if (!allyJpotentialAllies.contains(this.id)) {
            //reject the offer, situation 1
//			System.out.println("Situation 1 (Rejection): Can't accept offer becuase of this.id is not in allyJ's potential Allies");
            return false;
        } else { //allyJ is a qualified potential allies. 2. check the u_ji
            if (Utils.utility((SimEnvironment) state, allyJ, this) <= 0) {
                //reject the offer, situation 2
//				System.out.println("Situation 2 (Rejection): reject the offer becuase u_ji is less than zero");
                return false;
            } else { //u_ji is greater than 0, check if this allyJ need more friends
                if (currentUtility(state, allyJ) < 0) {  //need more friends
//            	if (currentUtility(state, allyJ) <= 0.2 && currentUtility(state, allyJ) >= -0.2) {
                    //situation 3: accept the offer --> u_ji is greater than 0, and allyJ needs more allies
                    this.currentStepAlliance.add(allyJ.id);
                    allyJ.currentStepAlliance.add(this.id);
                    this.cost = Math.pow(currentStepAlliance.size(), 2);
                    allyJ.cost = Math.pow(allyJ.currentStepAlliance.size(), 2);
//					System.out.println("Situation 3 (Acceptence): Recipient accept the offer based on need.");
//					System.out.println("allyJ's allEnemies = " + Utils.getAllEnemies(state, allyJ.id).toString());
                    return true;
                } else {    //no need more friends, but check whether current allies are good or the new offer is better
                    Agent currentLowestRanked = lowestRanked(state, allyJ);
                    if (currentLowestRanked != null) { //if this allyJ already has some friend
                        //check if the offer is better than current friends
                        if (Utils.utility((SimEnvironment) state, allyJ, this) > Utils.utility(state, allyJ, currentLowestRanked)) {
                            allyJ.currentStepAlliance.remove(currentLowestRanked.id);
                            currentLowestRanked.currentStepAlliance.remove(allyJ.id);
//							System.out.println("remove lowest ranked allies = " + currentLowestRanked.id);
                            this.currentStepAlliance.add(allyJ.id);
                            allyJ.currentStepAlliance.add(this.id);
                            this.cost = Math.pow(currentStepAlliance.size(), 2);
                            allyJ.cost = Math.pow(allyJ.currentStepAlliance.size(), 2);
//							System.out.println("Situation 4 (Acceptence): Recipient replaces current lowest ally and accept the offer");
//							System.out.println("allyJ's allEnemies = " + Utils.getAllEnemies(state, allyJ.id).toString());
                            return true;
                        } else { //current allies are better, rejection
//							System.out.println("Situation 5 (Rejection): reject the offer becuase current alliance is perfect");
                            return false;
                        }
                    } else { //this allyJ doesn't have current allies but it doesn't need friends
//						System.out.println("Situation 6 (Rejection): reject the offer because it doesn't need any friends");
                        return false;
                    }
                } // end of no need more friends
            } //end of u_ji is greater than 0
        } //end of qualified potential ally
    }

    public double currentUtility(SimEnvironment state, Agent a) {
        double SRGcapability = 0;
        double sumU_ij = 0;
        double U = 0; //current utility
        Set<Integer> allEnemies = Utils.getAllEnemies(state, a.id);
        Set<Integer> SRG = a.SRG;
        Set<Integer> secondary = Utils.setDifference(allEnemies, SRG);
        for (int p : SRG) {
            Agent pri = state.allAgents.get(p);
            SRGcapability += pri.capability;
        }
        for(int s : secondary) {
        	Agent sec = state.allAgents.get(s);
        	SRGcapability += 0.5 * sec.capability;
        }
//		System.out.println("SRGcapability = " + SRGcapability);
        if (a.currentStepAlliance.size() == 0 && a.alliance.size() == 0) {
//			System.out.println("#" + a.id + "capability = " + a.capability);
            U = a.capability - SRGcapability;
//			System.out.println("#" + a.id + "  has no allies and the current utility is " + U);
        } else {
            Set<Integer> currentStateAlliance = Utils.getCurrentStateAlliance(state, a.id);
//			System.out.println("currentStateAlliance = " + currentStateAlliance.toString());
			a.cost = Math.pow(currentStateAlliance.size(), 1.2);
            //calculate the sum of alliacne's utility
            for (int j : currentStateAlliance) {
                double u_ij = a.utilityOfAll[state.getIndex(j)];
                sumU_ij += u_ij;
            }
            U = a.capability + sumU_ij - 0.2 * a.cost - SRGcapability;
//			System.out.println("sumU_ij = " + sumU_ij + " a.cost = " + 0.2* a.cost);
//			System.out.println("#" + a.id + "  has some allies and the current utility is " + U);
        }
        return U;
    }

    /*
     * Reset the currentAllies
     */
    public void resetCurrentAllies(SimEnvironment state, Agent agent) {
        List<Integer> currentStepAllianceList = new ArrayList<Integer>(agent.currentStepAlliance);
        for (int oA : currentStepAllianceList) {
            Agent oldAlly = state.getAgent(oA);
            oldAlly.currentStepAlliance.remove(this.id);
        }
        currentStepAlliance.clear();
    }

}
