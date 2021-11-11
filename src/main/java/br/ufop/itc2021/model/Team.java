package br.ufop.itc2021.model;

import org.dom4j.*;

/**
 * This class represents an ITC2021 problem instance.
 *
 * @author George Fonseca and Tulio Toffolo
 */
public class Team {

    public final Instance inst;
    public final Element elem;

    public final int id;
    public final String name;

    private Game[] games;
    private int nGames = 0;

    public Team(Instance inst, Node xmlNode) {
        this.inst = inst;
        this.elem = ( Element ) xmlNode;

        this.id = Integer.parseInt(elem.attributeValue("id"));
        this.name = elem.attributeValue("name");
    }

    public Game getGame(int index) {
        assert nGames == 2 * (inst.nTeams - 1);
        return games[index];
    }

    public Game[] getGames() {
        assert nGames == 2 * (inst.nTeams - 1);
        return games;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Team team = ( Team ) o;
        return id == team.id && name.equals(team.name);
    }

    public int hashCode() {
        return id;
    }

    protected void addGame(Game game) {
        if (games == null)
            games = new Game[2 * (inst.nTeams - 1)];
        games[nGames++] = game;
    }
}
