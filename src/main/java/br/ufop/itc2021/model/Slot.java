package br.ufop.itc2021.model;

/**
 * This class represents an ITC2021 problem instance.
 *
 * @author George Fonseca and Tulio Toffolo
 */
public class Slot implements Comparable<Slot> {

    public final Instance inst;
    public final int id;

    public Slot(Instance inst, int id) {
        this.inst = inst;
        this.id = id;
    }

    @Override
    public int compareTo(Slot slot) {
        return Integer.compare(id, slot.id);
    }
}
