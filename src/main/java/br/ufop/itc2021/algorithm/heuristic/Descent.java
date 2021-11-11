package br.ufop.itc2021.algorithm.heuristic;

import br.ufop.itc2021.algorithm.neighborhood.*;
import br.ufop.itc2021.model.*;
import br.ufop.itc2021.util.*;

import java.io.*;
import java.util.*;

/**
 * This class represents a Descent First Improvement Heuristic.
 *
 * @author Tulio Toffolo
 */
public class Descent extends Heuristic {

    /**
     * Instantiates a new Descent (First Improvement Heuristic) object.
     *
     * @param inst problem reference.
     * @param random  random number generator.
     */
    public Descent(Instance inst, Random random, int lowerBound) {
        super(inst, random, "Descent", lowerBound);
    }

    /**
     * Executes the Descent heuristic.
     *
     * @param initialSolution the initial (input) solution.
     * @param timeLimitMillis the time limit (in milliseconds).
     * @param maxIters        the maximum number of iterations without improvements to execute.
     * @param output          output PrintStream for logging purposes.
     * @return the best solution encountered by the heuristic.
     */
    public Solution run(Solution initialSolution, long timeLimitMillis, long maxIters, PrintStream output) {
        long finalTimeMillis = System.currentTimeMillis() + timeLimitMillis;

        bestSolution = initialSolution;
        Solution solution = initialSolution.clone();

        int nItersWithoutImprovement = 0;

        while (System.currentTimeMillis() < finalTimeMillis && nItersWithoutImprovement++ < maxIters && bestSolution.getCost() > lowerBound) {
            Move move = selectMove(solution);
            double delta = move.doMove(solution);

            // if solution is improved...
            if (delta < 0) {
                acceptMove(move);
                nItersWithoutImprovement = 0;

                if (solution.getCost() < bestSolution.getCost()) {
                    bestSolution = solution.clone();
                    Util.safePrintStatus(output, nIters, bestSolution, solution, "*");
                }
            }

            // if a side solution is obtained
            else if (delta == 0) {
                acceptMove(move);
            }

            // if solution is rejected..
            else {
                rejectMove(move);
            }

            nIters++;

            if (nIters % 100000 == 0)
                Util.safePrintStatus(output, nIters, bestSolution, solution, "");
        }

        return bestSolution;
    }
}
