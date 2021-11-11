package br.ufop.itc2021.algorithm.neighborhood;

import br.ufop.itc2021.model.*;

import java.util.*;

/**
 * This class represents a CompoundedMove, i.e. a neighborhood consisting of a sequence of moves.
 *
 * @author Tulio Toffolo
 */
public class CompoundedMove extends MoveToCompound {

    List<MoveToCompound> moves = new ArrayList<>();

    /**
     * Instantiates a new Compounded move.
     *
     * @param inst     problem.
     * @param random   random number generator.
     * @param name     the name of this neighborhood (for debugging purposes).
     * @param priority the priority (priority) of this neighborhood structure. The larger the value, the higher the
     *                 priority.
     */
    public CompoundedMove(Instance inst, Random random, String name, int priority) {
        super(inst, random, name, priority);
    }

    /**
     * Instantiates a new CompoundedMove.
     *
     * @param inst   problem.
     * @param random random number generator.
     * @param name   the name of this neighborhood (for debugging purposes).
     * @param moves  array with the moves that will compose the CompoundedMove.
     * @param weight the weight (weight) of this neighborhood structure. The larger the value, the higher the
     *               weight.
     */
    public CompoundedMove(Instance inst, Random random, String name, MoveToCompound moves[], int weight) {
        this(inst, random, name, weight);

        for (MoveToCompound move : moves) {
            move.setInChain(true);
            this.moves.add(move);
        }
    }

    public void accept() {
        super.accept();
        for (MoveToCompound move : moves)
            move.accept();
    }

    /**
     * Adds a Move to the Compounded Move.
     *
     * @param move the move to add
     * @return this {@link CompoundedMove} object.
     */
    public CompoundedMove addMove(MoveToCompound move) {
        move.setInChain(true);
        moves.add(move);
        return this;
    }

    public long doMove(Solution solution) {
        super.doMove(solution);
        for (Move move : moves)
            move.doMove(solution);

        if (!inChain) solution.updateCost();
        return deltaCost = solution.getCost() - initialTotalCost;
    }

    public boolean hasMove(Solution solution) {
        for (Move move : moves)
            if (!move.hasMove(solution))
                return false;
        return true;
    }

    public void reject() {
        super.reject();
        for (int i = moves.size() - 1; i >= 0; i--)
            moves.get(i).reject();

        if (!inChain) solution.forceUpdateCost(initialInfeas, initialFeasCost);
    }
}
