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
 * This is BurdenSharing2_2021-10-28 (v1.08)
 * New features:
 * (1) have different cost term before and after 1945
 * (3) output common alliance size
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
    //this one is used for random generated input
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
        attribute[2] = democracy;
        //relations
        SRG = new HashSet<Integer>();
        neighbors = new HashSet<Integer>();
        potentialAllies = new HashSet<Integer>();
        currentStepAlliance = new HashSet<Integer>();
        alliance = new HashSet<Integer>();

        //set the color for agent based on the enemyTypeA
        float colorCapability = (float) this.capability;
        if (democracy == 1)
            setColor(state, (float) 0, (float) 0, (float) 1, (float) colorCapability);//typeA, blue color
        else
            setColor(state, (float) 1, (float) 0, (float) 0, (float) colorCapability); //typeB, red color

    }

    //this is used for real world input data
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
        attribute[2] = democracy;
        //relations
        this.SRG = SRG;
        this.alliance = alliance;
        this.neighbors = neighbors;
        currentStepAlliance = new HashSet<Integer>();
        potentialAllies = new HashSet<Integer>();


        //set the color for agent based on the enemyTypeA
        float colorCapability = (float) this.capability;
        if (this.democracy == 1)
            setColor(state, (float) 0, (float) 0, (float) 1, 1);//typeA, blue color
        else
            setColor(state, (float) 1, (float) 0, (float) 0, 1); //typeB, red color

    }


    /*
     * ***************************************************************************
     *                               STEP
     * ***************************************************************************
     */
    @Override
    public void step(SimState state) {
        currentStep = state.schedule.getSteps();
        SimEnvironment Environment = (SimEnvironment) state;
        //calculate the utility for each other agent
        allUtility(Environment, this);

        //*********************END OF CALCULATE THE UTILITY OF ALL******************************
        potentialAllies = Utils.getPotentialAllies((SimEnvironment) state, this.id);
        Agent allyJ = highestRanked((SimEnvironment) state, potentialAllies); //highest rank
        if (currentStepAlliance.isEmpty()) { //if this country has no friend at currentStep
            //go ahead to offer providing process
            provideOffer(Environment, allyJ);
        } else { //if this country already made alliance before his turn, he can make his own choice now!
            //compare this agent's potential allies with current allies
            Agent currentLowestRanked = stepLowestRanked(Environment, this);
            if (Utils.utility(Environment, this, allyJ) > Utils.utility(Environment, this, currentLowestRanked)) { //if current allies are bad
                resetCurrentStepAllies(Environment, this);
                provideOffer(Environment, allyJ);
            } else { //if current allies are good
                provideOffer(Environment, allyJ);
            }
        }
        //**************************END OF MAKING FRIENDS*********************************************************
        //update the allianceList for all agents
        updateAllCurrentStepAllianceList(Environment, Environment.allAgents);
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
                    newAllianceList[state.getIndex(ca)] = 1;
                }
            }
            a.currentStepAllianceList = newAllianceList;
        }
    }

    public void updateAllEnemyList(SimEnvironment state, Map<Integer, Agent> allAgents) {
        for (Agent a : allAgents.values()) {
            Set<Integer> currentEnemy = Utils.getAllEnemies(state, a.id);
            int[] currentEnemyList = Utils.convertSetToList(state, currentEnemy, allAgents.size());
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
     * NeighborList for experimenter to output neighbor matrix
     */
    public int[] neighborList(SimEnvironment state) {
        int[] neighborList = new int[state.agentIdList.size()];
        Arrays.fill(neighborList, 0);
        for (int a : neighbors) {
            neighborList[state.getIndex(a)] = 1;
        }
        return neighborList;
    }


    /*
     * ****************************************************************************************
     * 									Making Offer
     * ****************************************************************************************
     */
    /*
     * Find out friends, and rank their utilities. Choose the highest ranked agents to offer the alliance
     */
    public Agent highestRanked(SimEnvironment state, Set<Integer> potentialAllies) {
        if (potentialAllies.isEmpty() || potentialAllies.size() == 0) {
            return null;
        }
        double utilityBase = -10000;
        Agent maxUtilityAgent = null;
        for (int pA : potentialAllies) {
            int paIndex = state.getIndex(pA);
            double utilityOfpA = utilityOfAll[paIndex];
            if (utilityOfpA > utilityBase) {
                Agent potentialA = state.allAgents.get(pA);
                maxUtilityAgent = potentialA;
                utilityBase = utilityOfpA;
            }
        }
        return maxUtilityAgent;
    }

    /*
     * Find the lowest ranked agent among current STEP allies. 
     * This agent might form alliance with others before his turn. 
     * This agent need to release lower agent to make new friend.
     * 
     */
    public Agent stepLowestRanked(SimEnvironment state, Agent agent) {
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
     * Find the lowest ranked agent among current STATE allies. 
     * This agent evaluate all his allies, compare the lowest one with the potential ally
     * See which one is better
     * 
     */
    public Agent stateLowestRanked(SimEnvironment state, Agent agent) {
    	Set<Integer> currentStateAllies = Utils.getCurrentStateAlliance(state, agent.id);
        if (currentStateAllies.isEmpty() || currentStateAllies.size() == 0) {
            return null;
        }
        double utilityBase = 10000;
        Agent minUtilityAgent = null;
        for (int ca : currentStateAllies) {
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
//        System.out.println("this id : " + this.id + "    currentU: " + currentUtility(state, this));
        if (currentUtility(state, this) > 0.2 || currentUtility(state, this) < -0.2)
            needMorePartners = false;//if needMorePartners == false, then stop making friends
        else needMorePartners = true;
        int whileLoopNum = 0;
        int initialPotentialAlliesSize = this.potentialAllies.size();

        while (needMorePartners && whileLoopNum < initialPotentialAlliesSize) { //making some friends
            Set<Integer> newPotentialAllies = potentialAllies;
            allyJ = highestRanked((SimEnvironment) state, newPotentialAllies); //highest rank
            //offer receiving process
            if (allyJ != null) {
                //provide offer to highest-ranked agent and see if it accepts the offer
                acceptOffer((SimEnvironment) state, allyJ);
                potentialAllies.remove(allyJ.id); //remove the highest ranked ally from potentialAllies list
            }
            if (currentUtility(state, this) > 0.2 || currentUtility(state, this) < -0.2) needMorePartners = false;
            else needMorePartners = true;
            whileLoopNum++;

        }
//        System.out.println("finish this round");
    }

    /*
     * Accept the offer : The potential ally needs to decide if it would like to accept the offer
     */
    public boolean acceptOffer(SimEnvironment state, Agent allyJ) {
        Set<Integer> allyJpotentialAllies = Utils.getPotentialAllies(state, allyJ.id);
        if (allyJ.utilityOfAll == null) {
            allUtility(state, allyJ);
        }
        //check if the offer giver is one of the potential allies
        if (!allyJpotentialAllies.contains(this.id)) {
            //Situation 1 (Rejection): Can't accept offer because of this.id is not in allyJ's potential allies
            return false;
        } else { //allyJ is a qualified potential allies. check the u_ji then
            if (Utils.utility((SimEnvironment) state, allyJ, this) <= 0) {
                //Situation 2 (Rejection): reject the offer because u_ji is less than zero (not worth to form alliance)
                return false;
            } else { //u_ji is greater than 0, check if this allyJ need more friends
                if (currentUtility(state, allyJ) <= 0.2 && currentUtility(state, allyJ) >= - 0.2) {  //need more friends
                    //accept the offer --> u_ji is greater than 0, and allyJ needs more allies
                	//Situation 3 (Acceptance): Recipient accept the offer based on need.
                    this.currentStepAlliance.add(allyJ.id);
                    allyJ.currentStepAlliance.add(this.id);
                    return true;
                } else {    //no need more friends, but check whether ALL current allies are good or the new offer is better
                    Agent currentLowestRanked = stateLowestRanked(state, allyJ);
                    if (currentLowestRanked != null) { //if this allyJ already has some friend
                        //check if the offer is better than current friends
                    	//Situation 4 (Acceptance): Recipient replaces current lowest ally and accept the offer
                        if (Utils.utility((SimEnvironment) state, allyJ, this) > Utils.utility(state, allyJ, currentLowestRanked)) {
                            Set<Integer> currentStateAllies_allyJ = Utils.getCurrentStateAlliance((SimEnvironment)state, allyJ.id);
                        	Set<Integer> currentStateAllies_lowest = Utils.getCurrentStateAlliance((SimEnvironment)state, currentLowestRanked.id);
                            currentStateAllies_allyJ.remove(currentLowestRanked.id);
                            currentStateAllies_lowest.remove(allyJ.id);
                            this.currentStepAlliance.add(allyJ.id);
                            allyJ.currentStepAlliance.add(this.id);
                            return true;
                        } else { //current allies are better, rejection
                        	//Situation 5 (Rejection): reject the offer because current alliance is perfect
                            return false;
                        }
                    } else { //this allyJ doesn't have allies but it doesn't need friends (current U > 0)
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
        if (a.currentStepAlliance.size() == 0 && a.alliance.size() == 0) { //equal to no currentStateAlliance
            U = a.capability - SRGcapability;
        } else {
            Set<Integer> currentStateAlliance = Utils.getCurrentStateAlliance(state, a.id);
			if(state.year < 1945) {
				a.cost = Math.pow(currentStateAlliance.size(), 2.0);
			}
			else {
				a.cost = Math.pow(currentStateAlliance.size(), 1.2);
			}
            
            //calculate the sum of alliacne's utility
            for (int j : currentStateAlliance) {
                double u_ij = a.utilityOfAll[state.getIndex(j)];
                sumU_ij += u_ij;
            }
            U = a.capability + sumU_ij - 0.2 * a.cost - SRGcapability;
        }
        return U;
    }

    /*
     * Reset the currentStepAllies
     * When someone has allies before his turn, and the potential allies has higher u_ij, 
     * this country needs to give up current step allies.
     */
    public void resetCurrentStepAllies(SimEnvironment state, Agent agent) {
        List<Integer> currentStepAllianceList = new ArrayList<Integer>(agent.currentStepAlliance);
        for (int oA : currentStepAllianceList) {
            Agent oldAlly = state.getAgent(oA);
            oldAlly.currentStepAlliance.remove(this.id);
        }
        currentStepAlliance.clear();
    }

}