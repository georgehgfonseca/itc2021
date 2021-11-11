package br.ufop.itc2021.algorithm.heuristic;

import br.ufop.itc2021.algorithm.learning.*;
import br.ufop.itc2021.algorithm.neighborhood.*;
import br.ufop.itc2021.model.*;

import java.io.*;
import java.util.*;

/**
 * This abstract class represents a Heuristic (or Local Search method). The basic methods and neighborhood selection are
 * included.
 *
 * @author Tulio Toffolo
 */
public abstract class Heuristic {

    public final static boolean USE_LEARNING = false;

    public final Instance inst;
    public final Random random;
    public final String name;
    public final int lowerBound;

    protected final List<Move> moves = new ArrayList<>();

    protected Solution bestSolution;
    protected int sumWeights = 0;
    protected long nIters = 0;

    protected LearningAutomata learningAutomata = null;


    /**
     * Instantiates a new Heuristic.
     *
     * @param inst   the problem reference.
     * @param random the random number generator.
     * @param name   the name
     */
    public Heuristic(Instance inst, Random random, String name, int lowerBound) {
        this.inst = inst;
        this.random = random;
        this.name = name;
        this.lowerBound = lowerBound;

        if (USE_LEARNING) this.learningAutomata = new LearningAutomata(random, this);
    }

    /**
     * Adds a move to the heuristic.
     *
     * @param move the move to be added.
     */
    public void addMove(Move move) {
        moves.add(move);
        moves.sort((a, b) -> -Integer.compare(a.getWeight(), b.getWeight()));
        sumWeights += move.getWeight();

        if (USE_LEARNING) learningAutomata.initProbabilities(getMoves());
    }

    /**
     * Accepts move and updates learning algorithm (if present).
     *
     * @param move the move to be accepted.
     */
    public void acceptMove(Move move) {
        move.accept();
        resetMoves();

        //if (USE_LEARNING && move.getDeltaCost() < 0) learningAutomata.updateProbabilities(1.0);
    }

    /**
     * Returns whether the local search is 'out of moves', meaning that there are no possible moves
     * to be performed.
     *
     * @param solution the solution.
     * @return true if there are no more moves to be performed and false otherwise.
     */
    public boolean isOutOfMoves(Solution solution) {
        for (Move move : moves)
            if (move.hasMove(solution))
                return false;
        return true;
    }

    /**
     * Rejects move and updates learning algorithm (if present).
     *
     * @param move the move to be rejected.
     */
    public void rejectMove(Move move) {
        move.reject();

        //if (USE_LEARNING) learningAutomata.updateProbabilities(0.0);
    }

    /**
     * Resets all moves considered by the heuristic.
     */
    public void resetMoves() {
        for (Move move : moves)
            move.reset();
    }


    /**
     * Runs the local search, returning the best solution obtained..
     *
     * @param solution        the initial (input) solution.
     * @param timeLimitMillis the time limit in milliseconds.
     * @param maxIters        the maximum number of iterations to execute.
     * @param output          the output
     * @return the solution
     */
    public abstract Solution run(Solution solution, long timeLimitMillis, long maxIters, PrintStream output);


    /**
     * Selects move.
     *
     * @param solution the solution
     * @return a randomly selected move (neighborhood), considering the provided weights.
     */
    protected Move selectMove(Solution solution) {
        Move move;

        if (USE_LEARNING) {
            move = moves.get(learningAutomata.nextAction());
            while (!move.hasMove(solution))
                move = moves.get(learningAutomata.nextAction());
        }
        else {
            int i, w;
            do {
                w = random.nextInt(sumWeights);
                for (i = 0; i < moves.size(); i++) {
                    if (w < moves.get(i).getWeight()) break;
                    w -= moves.get(i).getWeight();
                }
                move = moves.get(i);
            } while (!move.hasMove(solution));
        }

        return move;
    }


    // region getters and setters

    /**
     * Gets best solution.
     *
     * @return the best solution obtained so far.
     */
    public Solution getBestSolution() {
        return bestSolution;
    }

    /**
     * Returns an unmodifiableList with the moves in the heuristic.
     *
     * @return an unmodifiableList with the moves in the heuristic.
     */
    public List<Move> getMoves() {
        return Collections.unmodifiableList(moves);
    }

    /**
     * Gets the number of iterations executed.
     *
     * @return the n iters
     */
    public long getNIters() {
        return nIters;
    }

    /**
     * Returns the string representation of the heuristic.
     *
     * @return the string representation of the heuristic.
     */
    public String toString() {
        return name;
    }

    // endregion getters and setters
}
