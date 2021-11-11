package br.ufop.itc2021.algorithm.heuristic;

import br.ufop.itc2021.algorithm.neighborhood.*;
import br.ufop.itc2021.model.*;
import br.ufop.itc2021.util.*;

import java.io.*;
import java.util.*;

/**
 * This class is a Late Acceptance Hill Climbing implementation.
 *
 * @author Tulio Toffolo
 */
public class LAHC extends Heuristic {

    /**
     * LAHC list with solution costs.
     */
    private long[] list;
    private double listMult;


    /**
     * Instantiates a new LAHC.
     *
     * @param inst       problem.
     * @param random     random number generator.
     * @param lowerBound known solution cost lower bound.
     * @param listSize   LAHC list size.
     * @param listMult   multiplier to apply on initial solution cost when 'populating' LAHC's list.
     */
    public LAHC(Instance inst, Random random, int lowerBound, int listSize, double listMult) {
        super(inst, random, "LAHC", lowerBound);

        // initializing the late acceptance list
        list = new long[listSize];
        this.listMult = listMult;
    }

    /**
     * Executes the LAHC.
     *
     * @param initialSolution the initial (input) solution.
     * @param timeLimitMillis the time limit (in milliseconds).
     * @param maxIters        the maximum number of iterations without improvements to execute.
     * @param output
     * @return the best solution encountered by the LAHC.
     */
    public Solution run(Solution initialSolution, long timeLimitMillis, long maxIters, PrintStream output) {
        long finalTimeMillis = System.currentTimeMillis() + timeLimitMillis;

        bestSolution = initialSolution;
        Solution solution = initialSolution.clone();

        // initializing LAHC list
        for (int i = 0; i < list.length; i++)
            list[i] = ( int ) Math.round(initialSolution.getCost() * listMult);

        int nItersWithoutImprovement = 0;
        int positionList = -1;

        while (System.currentTimeMillis() < finalTimeMillis && bestSolution.getCost() > lowerBound) {
            while (System.currentTimeMillis() < finalTimeMillis && nItersWithoutImprovement++ < maxIters && bestSolution.getCost() > lowerBound) {
                positionList = (positionList + 1) % list.length;

                Move move = selectMove(solution);
                long delta = move.doMove(solution);

                // if solution is improved...
                if (delta < 0) {
                    acceptMove(move);
                    nItersWithoutImprovement = 0;

                    if (solution.getCost() < bestSolution.getCost()) {
                        bestSolution = solution.clone();
                        Util.safePrintStatus(output, nIters, bestSolution, solution, "*");
                    }
                }

                // if solution is not improved, but is accepted...
                else if (delta == 0 || solution.getCost() <= list[positionList]) {
                    acceptMove(move);
                }

                // if solution is rejected..
                else {
                    rejectMove(move);
                }

                list[positionList] = solution.getCost();
                nIters++;

                if (nIters % 100000 == 0)
                    Util.safePrintStatus(output, nIters, bestSolution, solution, "");
            }

            if (System.currentTimeMillis() < finalTimeMillis) {
                nItersWithoutImprovement = 0;
                for (int i = 0; i < list.length; i++)
                    list[i] = ( int ) Math.round(initialSolution.getCost() * listMult);
                if (USE_LEARNING) learningAutomata.initProbabilities(getMoves());
                Util.safePrintText(output, "Resetting LAHC list", "");
            }
        }

        return bestSolution;
    }

    /**
     * Returns the string representation of the heuristic.
     *
     * @return the string representation of the heuristic.
     */
    public String toString() {
        return String.format("LAHC (listSize=%s, listMult=%.2f)",
          Util.longToString(list.length), listMult);
    }
}
