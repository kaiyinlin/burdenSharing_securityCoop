package burdenSharing2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.io.FileWriter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
    /*
     * *****************************************************************************
     * *********** initial Need
     * *****************************************************************************
     * ***********
     */
    public static double initialNeed(SimEnvironment state, Agent a) {
        double SRGcapability = 0;
        for (int e : a.SRG) {
            Agent enemy = state.allAgents.get(e);
            SRGcapability += enemy.capability;
        }
        double initialNeed = a.capability - SRGcapability;
        return initialNeed;
    }
    /*
     * *****************************************************************************
     * *********** Enemies and Alliances
     * *****************************************************************************
     * ***********
     */

    /*
     * All Enemies - Calculate all allies' enemy into enemyList all enemies =
     * individual enemies + friends' enemies + enemies' friend
     * update: Version 1.06 define SRGs as original enemies + enemies' friends; 
     * 		friend's enemies are excluded
     */
    private static Set<Integer> getExpandedEnemySet(Map<Integer, Agent> allAgents, Set<Integer> enemiesSet) {

        Set<Integer> finalEnemyAllianceSet = enemiesSet;

        for (int ii : enemiesSet) {
            // for each enemy, get the friends from previous step and the current step
            Set<Integer> enemyPreviousAlliances = allAgents.get(ii).alliance;
            Set<Integer> enemyCurrentStepAlliances = allAgents.get(ii).currentStepAlliance;

            Set<Integer> enemyAllAlliances = new HashSet<Integer>();
            enemyAllAlliances = Stream.of(enemyPreviousAlliances, enemyCurrentStepAlliances).flatMap(x -> x.stream())
                    .collect(Collectors.toSet());

            finalEnemyAllianceSet = Stream.of(finalEnemyAllianceSet, enemyAllAlliances).flatMap(x -> x.stream())
                    .collect(Collectors.toSet());

        }
        return finalEnemyAllianceSet;

    }

    /*
     * Get the difference between two sets
     */
    public static <T> Set<T> setDifference(final Set<T> setOne, final Set<T> setTwo) {
        Set<T> result = new HashSet<T>(setOne);
        result.removeAll(setTwo);
        return result;
    }

    public static Set<Integer> getAllEnemies(SimEnvironment state, int agentId) {
        Map<Integer, Agent> allAgents = state.allAgents;
        Agent targetAgent = allAgents.get(agentId);
        Set<Integer> allEnemies = new HashSet<Integer>();
        Set<Integer> expandedEnemies = new HashSet<Integer>();

        // ------------------------------------------------------------------------------------
        // //
        // get the first degree enemy = individual enemies + friends' enemies + enemies'
        // friend
        // ------------------------------------------------------------------------------------
        // //
        Set<Integer> allAlliances = Stream.of(targetAgent.alliance, targetAgent.currentStepAlliance)
                .flatMap(x -> x.stream()).collect(Collectors.toSet());

        // adding current personal enemies
        allEnemies = Stream.of(allEnemies, targetAgent.SRG).flatMap(x -> x.stream()).collect(Collectors.toSet());
        // adding all friends' enemies
//        for (int ii : allAlliances) {
//            Set<Integer> currentAgentEnemies = allAgents.get(ii).SRG;
//            allEnemies = Stream.of(allEnemies, currentAgentEnemies).flatMap(x -> x.stream())
//                    .collect(Collectors.toSet());
//        }

        // adding all enemies' friend
        for (int ii : targetAgent.SRG) {

            // for each enemy, get the friends from previous step and the current step
            Set<Integer> enemyPreviousAlliances = allAgents.get(ii).alliance;
            Set<Integer> enemyCurrentStepAlliances = allAgents.get(ii).currentStepAlliance;

            Set<Integer> enemyAllAlliances = new HashSet<Integer>();
            enemyAllAlliances = Stream.of(enemyPreviousAlliances, enemyCurrentStepAlliances).flatMap(x -> x.stream())
                    .collect(Collectors.toSet());
            allEnemies = Stream.of(allEnemies, enemyAllAlliances).flatMap(x -> x.stream()).collect(Collectors.toSet());

        }

        // ------------------------------------------------------------------------------------
        // //
        // Start to expand the enemies list from the very first enemy set
        // Stop when we can't find any set difference
        // ------------------------------------------------------------------------------------
        // //

//        expandedEnemies = getExpandedEnemySet(allAgents, allEnemies);
//
//        while (setDifference(expandedEnemies, allEnemies).size() > 0) {
//            allEnemies = Stream.of(allEnemies, expandedEnemies).flatMap(x -> x.stream()).collect(Collectors.toSet());
//            expandedEnemies = getExpandedEnemySet(allAgents, allEnemies);
//        }

        return allEnemies;
    }

    /*
     * Find potential allies = totalAgents - allEnemies
     */
    public static Set<Integer> getPotentialAllies(SimEnvironment state, int agentId) {
        // get the current agent
        Agent targetAgent = state.allAgents.get(agentId);

        // calculate the enemy of current Agent
//        Set<Integer> allEnemies = getAllEnemies(state, agentId);
        Set<Integer> potentialAllies = new HashSet<>(state.allAgents.keySet());
        potentialAllies.removeAll(targetAgent.SRG);//remove primary enemies
        potentialAllies.removeAll(targetAgent.alliance); // remove current friend from the potential list
        potentialAllies.remove(agentId); // remove itself
        return potentialAllies;
    }

    /*
     * Find current status Allies current status Allies = alliance +
     * currentStepAlliance
     */
    public static Set<Integer> getCurrentStateAlliance(SimEnvironment state, int agentId) {
        Agent targetAgent = state.allAgents.get(agentId);

        Set<Integer> allAlliance = new HashSet<Integer>();
        allAlliance = Stream.of(targetAgent.alliance, targetAgent.currentStepAlliance).flatMap(x -> x.stream())
                .collect(Collectors.toSet());

        return allAlliance;
    }

    /*
     * return 1 if they have common enemy, return 0 if not
     */

    public static int commonEnemy(SimEnvironment state, Agent i, Agent j) {
        Set<Integer> enemyI = getAllEnemies(state, i.id);
        Set<Integer> enemyJ = getAllEnemies(state, j.id);

        Set<Integer> intersection = enemyI.stream().distinct().filter(enemyJ::contains).collect(Collectors.toSet());
        if (intersection.size() > 0) {
            return 1;
        } else {
            return 0;
        }
    }

    /*
     * Common neighbors
     */
    public static int enemyNeighbor(SimEnvironment state, Agent i, Agent j) {
        Set<Integer> enemyI = getAllEnemies(state, i.id);
        Set<Integer> intersection = enemyI.stream().distinct().filter(j.neighbors::contains)
                .collect(Collectors.toSet());
        if (intersection.size() > 0) {
            return 1;
        } else {
            return 0;
        }
    }

    /*
     * common alliance
     * Update 2021-10-27:
     * v1.06 presents common alliance as integers, not binary
     */
    public static int commonAlliance(SimEnvironment state, Agent i, Agent j) {
        Set<Integer> allianceI = getCurrentStateAlliance(state, i.id);
        Set<Integer> allianceJ = getCurrentStateAlliance(state, j.id);

        if (allianceI.size() == 0 || allianceJ.size() == 0) { // at first time-step, no country has alliance yet
            return 0;
        }

        Set<Integer> intersection = allianceI.stream().distinct().filter(allianceJ::contains)
                .collect(Collectors.toSet());

        if (intersection.size() > 0) {
            return intersection.size();
        } else {
            return 0;
        }
    }

    /*
     * *****************************************************************************
     * *********** Utility function -- Attractiveness + Prevention + Trust
     * *****************************************************************************
     * ***********
     */

    public static double utility(SimEnvironment state, Agent i, Agent j) {
        double u_ij;
        u_ij = j.capability * (0.4 * attractiveness(state, i, j) + 0.2 * prevention(state, i, j) + 0.4 * trust(state, i, j));
        
        return u_ij;
    }

    /*
     * Attractiveness (A_ij)
     */
    public static double attractiveness(SimEnvironment state, Agent i, Agent j) {
        int EE = commonEnemy(state, i, j); // common enemy
        int Dj = 0; // democracy value
        int S = 0; // common culture
        int NE = enemyNeighbor(state, i, j); // if j is geographically contiguous to at least one of i’s enemies
        int T = commonAlliance(state, i, j); // common alliance
        double A_ij = 0;
        // Democracy value: D is assigned a value of 1 if j is a democracy 
        //and zero otherwise
        if (j.democracy == 1)
            Dj = 1;
        else if (j.democracy == 0)
            Dj = 0;
        // common culture: S is assigned a value of 1 if i see "i and j are culturally similar"
        // and zero otherwise
        //Update 10-27-2021: not necessary to be symmetric
        if (state.dataName.length() == 0) {
            S = i.culture == j.culture ? 1 : 0;
        } else {
            S = state.dataInformation.get(i.id).getCulture().get(j.id) == 1 ? 1 : 0;
        }
        if (i.democracy == 1) { // if demo_i = 1
            A_ij = 0.2 * EE + 0.3 * Dj + 0.1 * S + 0.2 * NE + 0.2 * T;
        } else { // if demo_j = 0
            A_ij = 0.3 * EE + 0.1 * Dj + 0.1 * S + 0.3 * NE + 0.2 * T;
        }

        return A_ij;
    }

    /*
     * A_{kj}
     */
    public static double prevention(SimEnvironment state, Agent i, Agent j) {
        double A_kj = 0;

        Set<Integer> enemyI = getAllEnemies(state, i.id);
        for (int e : enemyI) { // k are the enemies of i
            Agent k = state.allAgents.get(e);
            int EE = commonEnemy(state, k, j); // common enemy between j and k
            int Dj = 0; // democracy value of j
            int S = 0; // common culture between j and k
            int NE = enemyNeighbor(state, k, j); // if j is geographically contiguous to at least one of k’s enemies
            int T = commonAlliance(state, k, j); // common alliance between j and k
            // Democracy value: 
            //D is assigned a value of 1 if j is a democracy and zero otherwise
            Dj = j.democracy == 1 ? 1 : 0;
            // common culture: S is assigned a value of 1 if i see " i and j are culturally similar"
            // and zero otherwise
            if (state.dataName.length() == 0) {
                S = k.culture == j.culture ? 1 : 0;
            } else {
                S = state.dataInformation.get(k.id).getCulture().get(j.id) == 1 ? 1 : 0;
            }

            if (i.democracy == 1) { // if demo_i = 1
                A_kj += 0.2 * EE + 0.3 * Dj + 0.1 * S + 0.2 * NE + 0.2 * T;
            } else { // if demo_i = 0
                A_kj += 0.3 * EE + 0.1 * Dj + 0.1 * S + 0.3 * NE + 0.2 * T;
            }
        }
        
        return A_kj;
    }

    /*
     * Trust
     */
    public static double trust(SimEnvironment state, Agent i, Agent j) {
        double R_j = 0;
        Set<Integer> currentStateAlliance_I = getCurrentStateAlliance(state, i.id);
        Set<Integer> currentStateAlliance_J = getCurrentStateAlliance(state, j.id);
        if (currentStateAlliance_I.size() == 0 || currentStateAlliance_J == null) // if j doesn't have any allies, then
            // there is no trust term
            return 0;
        else {
            for (int l : currentStateAlliance_J) { // if j has some allies, use u_il*1 to evaluate the trust term
                double u_il = i.utilityOfAll[state.agentIdList.indexOf(l)];
                R_j += u_il;
            }
        }
        return R_j;

    }

    /*
     * *****************************************************************************
     * *********** Other Utility Function
     * *****************************************************************************
     * ***********
     */

    public static int[] convertSetToList(SimEnvironment state, Set<Integer> targetSet, int totalAgentCount) {
        int[] newList = new int[totalAgentCount];
        Arrays.fill(newList, 0);
        for (int s : targetSet) {
            newList[state.getIndex(s)] = 1;
        }
        return newList;
    }

    public static int[] convertSetToEnemyList(SimEnvironment state, Set<Integer> targetSet, int totalAgentCount, Set<Integer> originSRG) {
        int[] newList = new int[totalAgentCount];
        Arrays.fill(newList, 0);
        for (int s : targetSet) { //all enemies
            newList[state.getIndex(s)] = 2;
        }
        for (int o : originSRG) {
            newList[state.getIndex(o)] = 1;
        }
        return newList;
    }

    /*
    Calculate a list for scheduling order
    order the agent by utility from smallest to largest
     */
    public static List<Integer> getScheduleOrder(SimEnvironment state) {
        Map<Integer, Double> allUtilities = new HashMap<>(); //<AgentId, utility>
        for (Agent a : state.allAgents.values()) {
            double utils = a.currentUtility(state, a);
            a.setOrderingUtility(utils);
            allUtilities.put(a.id, utils);
        }
        Map<Integer, Double> sorted =
                allUtilities.entrySet().stream().sorted(Map.Entry.comparingByValue())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        return new ArrayList<>(sorted.keySet());
    }

}
