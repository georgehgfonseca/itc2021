
package br.ufop.itc2021.algorithm.neighborhood;

import br.ufop.itc2021.model.*;
import br.ufop.itc2021.util.*;

import java.util.*;

/**
 * This class represents a PartialRoundSwap move.
 *
 * @author Tulio Toffolo
 */
public class PartialRoundSwap extends MoveToCompound {

    private final List<PRSwap> moves;
    private int nMoves;

    Game game1, game2, game3, game4;
    int slot1, slot2;

    /**
     * Instantiates a new PartialRoundSwap Move.
     *
     * @param inst     problem.
     * @param random   random number generator.
     * @param weight the weight of this neighborhood.
     */
    public PartialRoundSwap(Instance inst, Random random, int weight) {
        super(inst, random, "PartialRoundSwap", weight);

        moves = new ArrayList<>();
        for (int s = 0; s < inst.nSlots; s++) {
            for (int g1 = 0; g1 < inst.nTeams / 2; g1++) {
                for (int g2 = g1 + 1; g2 < inst.nTeams / 2; g2++) {
                    moves.add(new PRSwap(s, g1, g2, 0, 2, 1, 3));
                    moves.add(new PRSwap(s, g1, g2, 0, 2, 3, 1));
                    moves.add(new PRSwap(s, g1, g2, 0, 3, 1, 2));
                    moves.add(new PRSwap(s, g1, g2, 0, 3, 2, 1));
                    moves.add(new PRSwap(s, g1, g2, 1, 2, 0, 3));
                    moves.add(new PRSwap(s, g1, g2, 1, 2, 3, 0));
                    moves.add(new PRSwap(s, g1, g2, 1, 3, 0, 2));
                    moves.add(new PRSwap(s, g1, g2, 1, 3, 2, 0));
                    moves.add(new PRSwap(s, g1, g2, 2, 0, 1, 3));
                    moves.add(new PRSwap(s, g1, g2, 2, 0, 3, 1));
                    moves.add(new PRSwap(s, g1, g2, 2, 1, 0, 3));
                    moves.add(new PRSwap(s, g1, g2, 2, 1, 3, 0));
                    moves.add(new PRSwap(s, g1, g2, 3, 0, 1, 2));
                    moves.add(new PRSwap(s, g1, g2, 3, 0, 2, 1));
                    moves.add(new PRSwap(s, g1, g2, 3, 1, 0, 2));
                    moves.add(new PRSwap(s, g1, g2, 3, 1, 2, 0));
                }
            }
        }
        nMoves = moves.size();
    }

    public void accept() {
        super.accept();
    }

    public long doMove(Solution solution) {
        super.doMove(solution);

        game3 = game4 = null;
        while (nMoves > 0 && game3 == game4) {
            // randomly selecting game and updating number of remaining ones
            int randomSwap = random.nextInt(nMoves);
            Util.swap(moves, randomSwap, --nMoves);

            // getting games 1 and 2 (the base of this move)
            PRSwap swap = moves.get(randomSwap);
            game1 = solution.gamesPerSlot.get(swap.slot).get(swap.g1);
            game2 = solution.gamesPerSlot.get(swap.slot).get(swap.g2);
            int[] teams = new int[]{ game1.home, game1.away, game2.home, game2.away };
            slot1 = swap.slot;

            // looking for games 3 and 4 (any two games of the teams in the same slot suffices)
            int team1 = teams[swap.team1];
            int team2 = teams[swap.team2];
            int team3 = teams[swap.team3];
            int team4 = teams[swap.team4];
            if (solution.gameSlot[inst.getGame(team1, team2).id] == solution.gameSlot[inst.getGame(team3, team4).id]
              && solution.samePhase(game1, inst.getGame(team1, team2))) {
                slot2 = solution.gameSlot[inst.getGame(team1, team2).id];
                game3 = inst.getGame(team1, team2);
                game4 = inst.getGame(team3, team4);
            }
        }

        // if no possible combination is obtained... then return 0
        if (game3 == game4)
            return 0;

        // swapping partial rounds
        solution.setGame(game1, slot2);
        solution.setGame(game2, slot2);
        solution.setGame(game3, slot1);
        solution.setGame(game4, slot1);

        if (!inChain) solution.updateCost();

        return deltaCost = solution.getCost() - initialTotalCost;
    }

    public boolean hasMove(Solution solution) {
        if (inChain && nMoves == 0) reset();
        return nMoves > 0;
    }

    public void reject() {
        super.reject();

        if (game3 == game4)
            return;

        // swapping partial rounds
        solution.setGame(game1, slot1);
        solution.setGame(game2, slot1);
        solution.setGame(game3, slot2);
        solution.setGame(game4, slot2);

        if (!inChain) solution.forceUpdateCost(initialInfeas, initialFeasCost);
    }

    public void reset() {
        super.reset();
        nMoves = moves.size();
    }


    private class PRSwap {

        public final int slot, g1, g2;
        public final int team1, team2, team3, team4;

        public PRSwap(int slot, int g1, int g2, int team1, int team2, int team3, int team4) {
            this.slot = slot;
            this.g1 = g1;
            this.g2 = g2;
            this.team1 = team1;
            this.team2 = team2;
            this.team3 = team3;
            this.team4 = team4;
        }
    }

    private class PRCombination {

        int team1, team2, team3, team4;

        public PRCombination(int team1, int team2, int team3, int team4) {
            this.team1 = team1;
            this.team2 = team2;
            this.team3 = team3;
            this.team4 = team4;
        }
    }
}
