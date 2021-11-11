package br.ufop.itc2021.model;

import org.dom4j.*;
import org.dom4j.io.*;

import java.util.*;

/**
 * This class represents an ITC2021 problem instance.
 *
 * @author George Fonseca and Tulio Toffolo
 */
public class Instance {

    public final String name;

    public final boolean phased;

    public final int nConstrs;
    public final int nGames;
    public final int nLeagues;
    public final int nSlots;
    public final int nTeams;

    public final Team[] teams;
    public final Game[] games;

    // Constraints
    public final Constraint[] constrs;
    public final Constraint[] basicConstrs;
    public final Constraint[] capacityConstrs;
    public final Constraint[] gameConstrs;
    public final Constraint[] breakConstrs;
    public final Constraint[] fairnessConstrs;
    public final Constraint[] separationConstrs;

    /***
     * XML file representation (using dom4j library)
     */
    private final Document xml;


    /**
     * Instantiates a new Problem from a file.
     *
     * @param instancePath the instance file path
     */
    public Instance(String instancePath) throws DocumentException {
        xml = new SAXReader().read(instancePath);

        // reading instance name
        name = xml.selectNodes("//MetaData/InstanceName").get(0).getStringValue();
        phased = xml.selectNodes("//Structure/Format/gameMode").get(0).getStringValue().equals("P");


        // reading leagues, slots and teams
        List<Node> leagueNodes = xml.selectNodes("//Resources/Leagues/league");
        List<Node> slotNodes = xml.selectNodes("//Resources/Slots/slot");
        List<Node> teamNodes = xml.selectNodes("//Resources/Teams/team");

        // useful counters (for easy loop in future)
        nLeagues = leagueNodes.size();
        nSlots = slotNodes.size();
        nTeams = teamNodes.size();
        nGames = (nTeams / 2) * nSlots;

        // creating arrays of teams
        teams = new Team[nTeams];
        Arrays.setAll(teams, i -> new Team(this, teamNodes.get(i)));

        // creating array of games
        games = new Game[nGames];
        int counter = 0;
        for (int i = 0; i < nTeams; i++) {
            for (int j = i + 1; j < nTeams; j++) {
                Game game = new Game(counter, i, j);
                teams[i].addGame(game);
                teams[j].addGame(game);
                games[counter++] = game;
            }
        }
        for (int i = 0; i < nTeams; i++) {
            for (int j = i + 1; j < nTeams; j++) {
                Game game = new Game(counter, j, i);
                teams[i].addGame(game);
                teams[j].addGame(game);
                games[counter++] = game;
            }
        }

        // creating constraints
        List<Constraint> constrList = new ArrayList<>();
        List<Constraint> basicConstrList = new ArrayList<>();
        List<Constraint> capacityConstrList = new ArrayList<>();
        List<Constraint> gameConstrList = new ArrayList<>();
        List<Constraint> breakConstrList = new ArrayList<>();
        List<Constraint> fairnessConstrList = new ArrayList<>();
        List<Constraint> separationConstrList = new ArrayList<>();
        for (Node c : xml.selectNodes("//Constraints/BasicConstraints/*"))
            basicConstrList.add(new Constraint(this, c));
        for (Node c : xml.selectNodes("//Constraints/CapacityConstraints/*"))
            capacityConstrList.add(new Constraint(this, c));
        for (Node c : xml.selectNodes("//Constraints/GameConstraints/*"))
            gameConstrList.add(new Constraint(this, c));
        for (Node c : xml.selectNodes("//Constraints/BreakConstraints/*"))
            breakConstrList.add(new Constraint(this, c));
        for (Node c : xml.selectNodes("//Constraints/FairnessConstraints/*"))
            fairnessConstrList.add(new Constraint(this, c));
        for (Node c : xml.selectNodes("//Constraints/SeparationConstraints/*"))
            separationConstrList.add(new Constraint(this, c));

        // Aggregating all constraints in constrList
        constrList.addAll(basicConstrList);
        constrList.addAll(capacityConstrList);
        constrList.addAll(gameConstrList);
        constrList.addAll(breakConstrList);
        constrList.addAll(fairnessConstrList);
        constrList.addAll(separationConstrList);

        // Creating arrays
        nConstrs = constrList.size();
        constrs = constrList.toArray(new Constraint[constrList.size()]);
        basicConstrs = basicConstrList.toArray(new Constraint[basicConstrList.size()]);
        capacityConstrs = capacityConstrList.toArray(new Constraint[capacityConstrList.size()]);
        gameConstrs = gameConstrList.toArray(new Constraint[gameConstrList.size()]);
        breakConstrs = breakConstrList.toArray(new Constraint[breakConstrList.size()]);
        fairnessConstrs = fairnessConstrList.toArray(new Constraint[fairnessConstrList.size()]);
        separationConstrs = separationConstrList.toArray(new Constraint[separationConstrList.size()]);

        // Pre-processing

    }


    /**
     * Returns the Game object of a certain "home x away" game
     *
     * @param home Home team id
     * @param away Away team id
     * @return the Game object
     */
    public Game getGame(int home, int away) {
        // computing index (not that it depends if home < away or otherwise)
        int index;
        if (home < away)
            index = home * (nTeams - 1) - Math.max(home * (home - 1) / 2, 0) + (away - home - 1);
        else
            index = nGames / 2 + away * (nTeams - 1) - Math.max(away * (away - 1) / 2, 0) + (home - away - 1);

        return games[index];
    }
}
