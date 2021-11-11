package br.ufop.itc2021.model;

/**
 * This class represents an ITC2021 problem instance.
 *
 * @author George Fonseca and Tulio Toffolo
 */
public final class Game implements Comparable<Game> {

    public final int id;
    public final int home;
    public final int away;

    public Game(int id, int home, int away) {
        this.id = id;
        this.home = home;
        this.away = away;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Game game = ( Game ) o;
        return id == game.id && home == game.home && away == game.away;
    }

    @Override
    public int compareTo(Game game) {
        return Integer.compare(id, game.id);
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("%02dx%02d", home, away);
    }
}
