package br.ufop.itc2021.algorithm.heuristic;

import org.apache.commons.math3.util.*;
import br.ufop.itc2021.algorithm.neighborhood.*;
import br.ufop.itc2021.model.*;
import br.ufop.itc2021.util.*;

import java.io.*;
import java.util.*;

/**
 * This class is a Simulated Annealing implementation.
 *
 * @author Tulio Toffolo
 */
public class SA extends Heuristic {

    /**
     * SA parameters.
     */
    private double alpha, t0;
    private int saMax = 10000;

    /**
     * Instantiates a new SA.
     *
     * @param inst       problem reference
     * @param random     random number generator.
     * @param lowerBound known solution cost lower bound.
     * @param alpha      cooling rate for the simulated annealing
     * @param t0         initial temperature, T0
     * @param saMax      number of iterations before update the temperature
     */
    public SA(Instance inst, Random random, int lowerBound, double alpha, double t0, int saMax) {
        super(inst, random, "SA", lowerBound);

        // initializing simulated annealing parameters
        this.alpha = alpha;
        this.t0 = t0;
        this.saMax = saMax;
    }

    /**
     * Executes the Simulated Annealing.
     *
     * @param initialSolution the initial (input) solution.
     * @param timeLimitMillis the time limit (in milliseconds).
     * @param maxIters        the maximum number of iterations without improvements to execute.
     * @param output          output PrintStream for logging purposes.
     * @return the best solution encountered by the SA.
     */
    public Solution run(Solution initialSolution, long timeLimitMillis, long maxIters, PrintStream output) {
        long finalTimeMillis = System.currentTimeMillis() + timeLimitMillis;

        bestSolution = initialSolution;
        Solution solution = initialSolution.clone();

        double temperature = this.t0;
        int itersInTemperature = 0;

        while (System.currentTimeMillis() < finalTimeMillis && bestSolution.getCost() > lowerBound) {
            Move move = selectMove(solution);
            long delta = move.doMove(solution);

            // if solution is improved...
            if (delta < 0) {
                acceptMove(move);

                if (solution.getCost() < bestSolution.getCost()) {
                    bestSolution = solution.clone();
                    Util.safePrintStatus(output, nIters, bestSolution, solution, "*");
                }
            }

            // if solution is not improved, but is accepted...
            else if (delta == 0) {
                acceptMove(move);
            }

            // solution is not improved, but may be accepted with a probability...
            else {
                double x = random.nextDouble();
                if (x < 1 / FastMath.exp(delta / temperature)) {
                    acceptMove(move);
                }

                // if solution is rejected..
                else {
                    rejectMove(move);
                }
            }

            // if necessary, updates temperature
            if (++itersInTemperature >= saMax) {
                itersInTemperature = 0;
                temperature = alpha * temperature;
                if (temperature < Util.EPS) {
                    temperature = t0;
                    Util.safePrintText(output, "Re-heating Simulated Annealing", "");
                }
            }

            nIters++;
            if (nIters % 100000 == 0)
                Util.safePrintStatus(output, nIters, bestSolution, solution, "");
        }

        return bestSolution;
    }

    /**
     * Returns the string representation of this heuristic.
     *
     * @return the string representation of this heuristic (with parameters values).
     */
    public String toString() {
        return String.format("Simulated Annealing (alpha=%.3f, saMax=%s, t0=%s)",
          alpha, Util.longToString(saMax), Util.longToString(( long ) t0));
    }


    private void estimateT0(Solution initialSolution, int nNeighbors, double ratio) {
        //Solution solution = initialSolution.clone();
        //List<Integer> neighborValues = new ArrayList<>(nNeighbors);
        //
        //for (int i = 0; i < nNeighbors; i++) {
        //    Move move = selectMove(solution);
        //    int delta = move.doMove(solution);
        //
        //    neighborValues.add(delta);
        //}
        //neighborValues.sort(Integer::compare);
        //
        //int t = 1;
        //int
        //
        //int idealDelta = neighborValues.get(( int ) (nNeighbors * ratio));
        //1/FastMath.log(delta / temperature);
        //
        //1/ratio
    }
}
