package br.ufop.itc2021.algorithm.constructive;

import br.ufop.itc2021.model.*;

import java.util.*;

/**
 * This class contains simple constructive procedures for the itc2021 problem.
 *
 * @author Tulio Toffolo
 */
public class SimpleConstructive {

    /**
     * Generates and returns a solution obtained by a randomized version of the circle method.
     *
     * @param inst   the problem
     * @param random random number generator
     * @return the solution generated
     */
    public static Solution circleSolution(Instance inst, Random random) {
        Solution solution = new Solution(inst);

        // creating randomized list of teams
        List<Integer> teams = new ArrayList<>();
        for (int i = 0; i < inst.teams.length; i++)
            teams.add(i);
        Collections.shuffle(teams, random);

        // assigning teams to rounds using circle method
        for (int s = 0; s < inst.nSlots / 2; s++) {
            for (int i = 0; i < inst.nTeams / 2; i++) {
                int home, away;

                if (i == 0) {
                    home = 0;
                    away = (teams.size() - 1) - s;
                }
                else {
                    home = i - s <= 0 ? i - s - 1 : i - s;
                    away = (teams.size() - 1) - i - s <= 0
                      ? (teams.size() - 1) - i - s - 1
                      : (teams.size() - 1) - i - s;
                }

                while (home < 0) home += inst.nTeams;
                while (away < 0) away += inst.nTeams;

                solution.addGame(inst.getGame(home, away), s);
                solution.addGame(inst.getGame(away, home), s + inst.nSlots / 2);
            }
        }

        solution.updateCost();
        return solution;
    }
}
