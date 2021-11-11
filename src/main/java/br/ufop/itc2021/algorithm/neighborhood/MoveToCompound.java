package br.ufop.itc2021.algorithm.neighborhood;

import br.ufop.itc2021.model.*;

import java.util.*;

/**
 * This class represents a "Compoundable" Move, i.e. a Move that can be added to
 * a CompoundMove neighborhood. For this, the Move must execute the move when
 * the {@link #doMove(Solution)} is called, instead of only calculating the
 * delta and executing the move if {@link #accept()} is called.
 *
 * @author Tulio Toffolo
 */
public abstract class MoveToCompound extends Move {

    protected boolean inChain = false;

    /**
     * Instantiates a new Move.
     *
     * @param inst   the problem reference.
     * @param random the random number generator.
     */
    public MoveToCompound(Instance inst, Random random) {
        super(inst, random);
    }

    /**
     * Instantiates a new Move.
     *
     * @param inst   the problem reference.
     * @param random the random number generator.
     * @param name   the name of this neighborhood (for debugging purposes).
     * @param weight the weight (weight) of this neighborhood structure.
     *               The larger the value, the higher the weight.
     */
    public MoveToCompound(Instance inst, Random random, String name, int weight) {
        super(inst, random, name, weight);
    }

    /**
     * Sets if this Move is part of a chain (or part of a CompoundedMove).
     *
     * @param inChain true if this Move is part of a chain and false otherwise.
     */
    public void setInChain(boolean inChain) {
        this.inChain = inChain;
    }
}
