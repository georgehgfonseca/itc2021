package br.ufop.itc2021.model;

import br.ufop.itc2021.*;
import org.dom4j.*;
import org.dom4j.io.*;

import java.io.*;
import java.time.*;
import java.util.*;

/**
 * This class represents an ITC2021 problem instance.
 *
 * @author George Fonseca and Tulio Toffolo
 */
public class Solution {

    public static final long infeasWeight = 10000000;

    public final Instance inst;

    public final int[] gameSlot;
    public final List<List<Game>> gamesPerSlot;
    public final Game[][] gamesPerTeam;
    public final Set<Game> unallocatedGames;

    public final int[][] nHomeGames;
    public final int[][] nAwayGames;

    private int cost = Integer.MAX_VALUE;
    private int infeas = Integer.MAX_VALUE;
    private boolean updated = false;

    private int costCA1, costCA2, costCA3, costCA4, costGA1, costBR1, costBR2, costFA2, costSE1;
    private int infeasP, infeasCA1, infeasCA2, infeasCA3, infeasCA4, infeasGA1, infeasBR1, infeasBR2, infeasFA2, infeasSE1;

    private int middlePhase;


    /**
     * Instantiates a new Solution.
     *
     * @param inst problem considered.
     */
    public Solution(Instance inst) {
        this.inst = inst;

        gameSlot = new int[inst.nGames];
        Arrays.fill(gameSlot, -1);

        gamesPerSlot = new ArrayList<>(inst.nSlots);
        for (int s = 0; s < inst.nSlots; s++) {
            gamesPerSlot.add(new ArrayList<>(inst.nTeams / 2));
        }

        gamesPerTeam = new Game[inst.nTeams][inst.nSlots];

        unallocatedGames = new HashSet<>(inst.nGames);
        for (int i = 0; i < inst.nGames; i++)
            unallocatedGames.add(inst.games[i]);

        nHomeGames = new int[inst.nTeams][inst.nSlots];
        nAwayGames = new int[inst.nTeams][inst.nSlots];

        middlePhase = inst.phased ? inst.nSlots / 2 : inst.nSlots;
    }

    /**
     * Private constructor used for cloning.
     *
     * @param solution solution to copy from.
     */
    private Solution(Solution solution) {
        this.inst = solution.inst;

        gameSlot = Arrays.copyOf(solution.gameSlot, inst.nGames);

        gamesPerSlot = new ArrayList<>(inst.nSlots);
        for (int s = 0; s < inst.nSlots; s++) {
            gamesPerSlot.add(new ArrayList<>(inst.nTeams / 2));
            for (Game game : solution.gamesPerSlot.get(s))
                gamesPerSlot.get(s).add(game);
        }

        gamesPerTeam = new Game[inst.nTeams][inst.nSlots];
        for (int i = 0; i < inst.nTeams; i++) {
            for (int s = 0; s < inst.nSlots; s++) {
                gamesPerTeam[i][s] = solution.gamesPerTeam[i][s];
            }
        }

        unallocatedGames = new HashSet<>(inst.nGames);
        unallocatedGames.addAll(solution.unallocatedGames);

        nHomeGames = new int[inst.nTeams][inst.nSlots];
        nAwayGames = new int[inst.nTeams][inst.nSlots];
        for (int i = 0; i < inst.nTeams; i++) {
            for (int s = 0; s < inst.nSlots; s++) {
                nHomeGames[i][s] = solution.nHomeGames[i][s];
                nAwayGames[i][s] = solution.nAwayGames[i][s];
            }
        }

        updated = solution.updated;

        cost = solution.cost;
        costCA1 = solution.costCA1;
        costCA2 = solution.costCA2;
        costCA3 = solution.costCA3;
        costCA4 = solution.costCA4;
        costGA1 = solution.costGA1;
        costBR1 = solution.costBR1;
        costBR2 = solution.costBR2;
        costFA2 = solution.costFA2;
        costSE1 = solution.costSE1;

        infeas = solution.infeas;
        infeasP = solution.infeasP;
        infeasCA1 = solution.infeasCA1;
        infeasCA2 = solution.infeasCA2;
        infeasCA3 = solution.infeasCA3;
        infeasCA4 = solution.infeasCA4;
        infeasGA1 = solution.infeasGA1;
        infeasBR1 = solution.infeasBR1;
        infeasBR2 = solution.infeasBR2;
        infeasFA2 = solution.infeasFA2;
        infeasSE1 = solution.infeasSE1;

        middlePhase = solution.middlePhase;
    }

    /**
     * Adds a game to a slot (round).
     *
     * @param game The game to be included.
     * @param slot Slot (round) considered.
     */
    public void addGame(Game game, int slot) {
        if (gameSlot[game.id] >= 0)
            throw new IllegalStateException("Game " + game.toString() + " was already assigned to slot " + gameSlot[game.id]);

        gameSlot[game.id] = slot;
        gamesPerSlot.get(slot).add(game);
        gamesPerTeam[game.home][slot] = game;
        gamesPerTeam[game.away][slot] = game;
        unallocatedGames.remove(game);
        nHomeGames[game.home][slot]++;
        nAwayGames[game.away][slot]++;
        updated = false;
    }

    public void addGame(int home, int away, int slot) {
        addGame(inst.getGame(home, away), slot);
    }

    /**
     * Creates and returns a copy of this solution.
     */
    public Solution clone() {
        return new Solution(this);
    }

    /**
     * Force the change of the solution cost. Be careful: if the inputed costs are invalid, there may be
     * multiple (serious) consequences!!
     *
     * @param infeas infeasibility cost
     * @param cost   solution cost (disregarding infeasibility)
     */
    public void forceUpdateCost(int infeas, int cost) {
        this.infeas = infeas;
        this.cost = cost;
        this.updated = true;
    }

    /**
     * Gets the solution total cost, given by a linear combination of the
     * infeasibility and solution cost. Note the the cost may be outdated
     * if the solution was modified. To ensure that it is updated, call
     * {@link #updateCost()}.
     *
     * @return the solution cost.
     */
    public long getCost() {
        return infeas * infeasWeight + cost;
    }

    public String getCostStr() {
        if (infeas > 0)
            return String.format("%d_%d", infeas, cost);
        return String.valueOf(cost);
    }

    /**
     * Gets the solution SOFT cost. Note the the cost may be outdated
     * if the solution was modified. To ensure that it is updated, call
     * {@link #updateCost()}.
     *
     * @return the solution cost.
     */
    public int getFeasCost() {
        return cost;
    }

    /**
     * Gets the solution infeasibility. Note the this value may be outdated if the
     * solution was modified. To ensure that it is updated, call {@link #updateCost()}.
     *
     * @return the solution infeasibility.
     */
    public int getInfeas() {
        return infeas;
    }

    /**
     * Gets if a certain game is allocated in the solution.
     *
     * @param game the game to be checked.
     * @return true if the game is allocated in the solution and false otherwise.
     */
    public boolean hasGame(Game game) {
        return game != null && gameSlot[game.id] >= 0;
    }

    /**
     * Gets if a certain game (between home x away) is allocated in the solution.
     *
     * @param home the home team index.
     * @param away the away team index.
     * @return true if the game between home x away is allocated in the solution and false otherwise.
     */
    public boolean hasGame(int home, int away) {
        return hasGame(inst.getGame(home, away));
    }

    /**
     * Returns if all games are allocated in the solution, i.e. if it is complete.
     *
     * @return true if all games are allocated in the solution and false otherwise.
     */
    public boolean isComplete() {
        return unallocatedGames.isEmpty();
    }

    /**
     * Returns if this solution if feasible or not.
     *
     * @return true if this solution if feasible and false otherwise.
     */
    public boolean isFeasible() {
        if (!updated) updateCost();
        return infeas == 0;
    }

    /**
     * Reads a solution from a file.
     *
     * @param filePath the input file path.
     * @throws IOException in case any IO error occurs.
     */
    public void read(String filePath) throws DocumentException {
        Document document = new SAXReader().read(filePath);

        List<Node> games = document.selectNodes("//Games/ScheduledMatch");
        for (Node node : games) {
            Element elem = ( Element ) node;
            int home = Integer.parseInt(elem.attributeValue("home"));
            int away = Integer.parseInt(elem.attributeValue("away"));
            int slot = Integer.parseInt(elem.attributeValue("slot"));
            addGame(home, away, slot);
        }

        updateCost();
    }

    /**
     * Removes a game from its current slot (round).
     *
     * @param game The game to be removed.
     */
    public void removeGame(Game game) {
        if (gameSlot[game.id] >= 0) {
            int slot = gameSlot[game.id];

            gameSlot[game.id] = -1;
            gamesPerSlot.get(slot).remove(game);
            if (game == gamesPerTeam[game.home][slot])
                gamesPerTeam[game.home][slot] = null;
            if (game == gamesPerTeam[game.away][slot])
                gamesPerTeam[game.away][slot] = null;
            unallocatedGames.add(game);
            nHomeGames[game.home][slot]--;
            nAwayGames[game.away][slot]--;

            updated = false;
        }
    }

    public boolean samePhase(Game game1, Game game2) {
        return !inst.phased
          || (gameSlot[game1.id] < middlePhase && gameSlot[game2.id] < middlePhase)
          || (gameSlot[game1.id] >= middlePhase && gameSlot[game2.id] >= middlePhase);
    }

    /**
     * Sets a game to a slot (round).
     *
     * @param game The game to be included.
     * @param slot Slot (round) considered.
     */
    public void setGame(Game game, int slot) {
        if (gameSlot[game.id] >= 0) {
            int prevSlot = gameSlot[game.id];
            gamesPerSlot.get(prevSlot).remove(game);
            nHomeGames[game.home][prevSlot]--;
            nAwayGames[game.away][prevSlot]--;
        }
        else {
            unallocatedGames.remove(game);
        }

        gameSlot[game.id] = slot;
        gamesPerSlot.get(slot).add(game);
        gamesPerTeam[game.home][slot] = game;
        gamesPerTeam[game.away][slot] = game;
        nHomeGames[game.home][slot]++;
        nAwayGames[game.away][slot]++;
        updated = false;
    }

    /**
     * Updates the infeasibility and solution cost.
     *
     * @return a linear combination of the infeasibility and solution.
     */
    public long updateCost() {
        if (!unallocatedGames.isEmpty()) return Integer.MAX_VALUE;
        if (updated) return getCost();

        // resetting counters
        costCA1 = costCA2 = costCA3 = costCA4 = costGA1 = costBR1 = costBR2 = costFA2 = costSE1 = 0;
        infeasP = infeasCA1 = infeasCA2 = infeasCA3 = infeasCA4 = infeasGA1 = infeasBR1 = infeasBR2 = infeasFA2 = infeasSE1 = 0;

        // TODO: comment code below to improve performance
        evalBasicConstrs();

        // computing violations of *CAPACITY* constraints
        evalCapacityConstrs();

        // computing violations of *GAME* constraints
        evalGameConstrs();

        // computing violations of *BREAK* constraints
        evalBreakConstrs();

        // computing violations of *FAIRNESS* constraints
        evalFairnessConstrs();

        // computing violations of *SEPARATION* constraints
        evalSeparationConstrs();

        // computing violations of *summing all infeasibilities and costs
        cost = costCA1 + costCA2 + costCA3 + costCA4 + costGA1 + costBR1 + costBR2 + costFA2 + costSE1;
        infeas = infeasP + infeasCA1 + infeasCA2 + infeasCA3 + infeasCA4 + infeasGA1 + infeasBR1 + infeasBR2 + infeasFA2 + infeasSE1;

        updated = true;
        return getCost();
    }

    /**
     * Validates the solution.
     *
     * @param output output stream (example: System.out) to print eventual error
     *               messages.
     * @return true if the solution and its costs are valid and false otherwise.
     */
    public boolean validate(PrintStream output) {
        boolean valid = true;
        updateCost();
        return valid;
    }

    /**
     * Writes the solution to a file.
     *
     * @param filePath the output file path.
     * @throws IOException in case any IO error occurs.
     */
    public void write(String filePath) throws IOException {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement("Solution");

        Element metaData = root.addElement("MetaData");
        metaData.addElement("InstanceName").addText(Main.inFile);
        metaData.addElement("SolutionName").addText(Main.outFile);
        metaData.addElement("ObjectiveValue")
          .addAttribute("infeasibility", String.valueOf(infeas))
          .addAttribute("objective", cost == Integer.MAX_VALUE ? "inf" : String.valueOf(cost));
        metaData.addElement("Contributor").addText("George Fonseca and Tulio A. M. Toffolo");
        metaData.addElement("Date").addText(LocalDateTime.now().toString());
        metaData.addElement("Remarks")
          .addText(String.format("Solution method: %s; Random seed: %d; Timelimit: %d", Main.algorithm, Main.seed, Main.timeLimit / 1000));

        Element gamesElem = root.addElement("Games");
        for (int s = 0; s < inst.nSlots; s++) {
            for (Game game : gamesPerSlot.get(s)) {
                gamesElem.addElement("ScheduledMatch")
                  .addAttribute("home", String.valueOf(game.home))
                  .addAttribute("away", String.valueOf(game.away))
                  .addAttribute("slot", String.valueOf(s));
            }
        }

        // writing xml to file
        FileWriter out = new FileWriter(filePath);
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setIndentSize(4);
        XMLWriter writer = new XMLWriter(out, format);
        writer.write(document);
        out.close();
    }


    private void evalBasicConstrs() {
        // checking if solution is 'phased' (only if necessary)
        if (inst.phased) {
            this.infeasP = 0;

            for (int g = 0; g < inst.nGames / 2; g++) {
                if ((gameSlot[g] < middlePhase && gameSlot[g + inst.nGames / 2] < middlePhase)
                  || (gameSlot[g] >= middlePhase && gameSlot[g + inst.nGames / 2] >= middlePhase)) {
                    this.infeasP++;
                }
            }
        }
    }

    private void evalCapacityConstrs() {
        for (Constraint c : inst.capacityConstrs) {
            int counter = 0;

            // CA1
            if (c.tag == Constraint.Tag.CA1) {
                if (c.mode.equals("H")) {
                    for (int s : c.slots)
                        counter += nHomeGames[c.teams[0]][s];
                }
                else if (c.mode.equals("A")) {
                    for (int s : c.slots)
                        counter += nAwayGames[c.teams[0]][s];
                }

                if (counter > c.max) {
                    if (c.hard)
                        infeasCA1 += (counter - c.max);
                    else
                        costCA1 += (counter - c.max) * c.penalty;
                }
            }

            // CA2
            else if (c.tag == Constraint.Tag.CA2) {
                if (c.mode1.equals("H")) {
                    int i = c.teams1[0];
                    for (int j : c.teams2) {
                        Game game = inst.getGame(i, j);
                        for (int s : c.slots) {
                            if (gameSlot[game.id] == s) {
                                counter++;
                                break;
                            }
                        }
                    }
                }
                else if (c.mode1.equals("A")) {
                    int i = c.teams1[0];
                    for (int j : c.teams2) {
                        Game game = inst.getGame(j, i);
                        for (int s : c.slots) {
                            if (gameSlot[game.id] == s) {
                                counter++;
                                break;
                            }
                        }
                    }
                }
                else if (c.mode1.equals("HA")) {
                    int i = c.teams1[0];
                    for (int j : c.teams2) {
                        Game game1 = inst.getGame(i, j);
                        for (int s : c.slots) {
                            if (gameSlot[game1.id] == s) {
                                counter++;
                                break;
                            }
                        }
                        Game game2 = inst.getGame(j, i);
                        for (int s : c.slots) {
                            if (gameSlot[game2.id] == s) {
                                counter++;
                                break;
                            }
                        }
                    }
                }

                if (counter > c.max) {
                    if (c.hard)
                        infeasCA2 += (counter - c.max);
                    else
                        costCA2 += (counter - c.max) * c.penalty;
                }
            }

            // CA3
            else if (c.tag == Constraint.Tag.CA3) {
                for (int i : c.teams1) {
                    int[] nTimes = new int[inst.nSlots];

                    if (c.mode1.equals("H")) {
                        // counting games in each slot
                        for (int j : c.teams2) {
                            if (i == j) continue;
                            Game game = inst.getGame(i, j);
                            nTimes[gameSlot[game.id]]++;
                        }
                    }
                    else if (c.mode1.equals("A")) {
                        // counting games in each slot
                        for (int j : c.teams2) {
                            if (i == j) continue;
                            Game game = inst.getGame(j, i);
                            nTimes[gameSlot[game.id]]++;
                        }
                    }
                    else if (c.mode1.equals("HA")) {
                        // counting games in each slot
                        for (int j : c.teams2) {
                            if (i == j) continue;
                            Game game1 = inst.getGame(i, j);
                            nTimes[gameSlot[game1.id]]++;
                            Game game2 = inst.getGame(j, i);
                            nTimes[gameSlot[game2.id]]++;
                        }
                    }

                    // starting with first *c.intp* consecutive slots
                    counter = 0;
                    for (int k = 0; k < c.intp - 1; k++)
                        counter += nTimes[k];

                    // checking all other consecutive slots
                    for (int s = c.intp - 1; s < inst.nSlots; s++) {
                        counter += nTimes[s];
                        if (counter > c.max) {
                            if (c.hard)
                                infeasCA3 += (counter - c.max);
                            else
                                costCA3 += (counter - c.max) * c.penalty;
                        }
                        counter -= nTimes[s - c.intp + 1];
                    }
                }
            }

            // CA4
            else if (c.tag == Constraint.Tag.CA4) {
                int[] nTimes = new int[inst.nSlots];

                if (c.mode1.equals("H")) {
                    // counting games of teams1 X teams2 in each slot
                    for (int i : c.teams1) {
                        for (int j : c.teams2) {
                            if (i == j) continue;
                            Game game = inst.getGame(i, j);
                            nTimes[gameSlot[game.id]]++;
                        }
                    }
                }
                else if (c.mode1.equals("A")) {
                    // counting games of teams1 X teams2 in each slot
                    for (int i : c.teams1) {
                        for (int j : c.teams2) {
                            if (i == j) continue;
                            Game game = inst.getGame(j, i);
                            nTimes[gameSlot[game.id]]++;
                        }
                    }
                }
                else if (c.mode1.equals("HA")) {
                    // counting games of teams1 X teams2 in each slot
                    for (int i : c.teams1) {
                        for (int j : c.teams2) {
                            if (i == j) continue;
                            Game game1 = inst.getGame(i, j);
                            nTimes[gameSlot[game1.id]]++;
                            Game game2 = inst.getGame(j, i);
                            nTimes[gameSlot[game2.id]]++;
                        }
                    }
                }

                if (c.mode2.equals("GLOBAL")) {
                    for (int s : c.slots)
                        counter += nTimes[s];
                    if (counter > c.max) {
                        if (c.hard)
                            infeasCA4 += (counter - c.max);
                        else
                            costCA4 += (counter - c.max) * c.penalty;
                    }
                }
                else if (c.mode2.equals("EVERY")) {
                    for (int s : c.slots) {
                        counter = nTimes[s];
                        if (counter > c.max) {
                            if (c.hard)
                                infeasCA4 += (counter - c.max);
                            else
                                costCA4 += (counter - c.max) * c.penalty;
                        }
                    }
                }
            }
        }
    }

    private void evalGameConstrs() {
        for (Constraint c : inst.gameConstrs) {
            int counter = 0;

            // GA1
            if (c.tag == Constraint.Tag.GA1) {
                for (Game game : c.meetings) {
                    for (int slot : c.slots) {
                        if (gameSlot[game.id] == slot) {
                            counter++;
                        }
                    }
                }

                if (c.min >= 0 && counter < c.min) {
                    if (c.hard)
                        infeasGA1 += (c.min - counter);
                    else
                        costGA1 += (c.min - counter) * c.penalty;
                }
                if (c.max >= 0 && counter > c.max) {
                    if (c.hard)
                        infeasGA1 += (counter - c.max);
                    else
                        costGA1 += (counter - c.max) * c.penalty;
                }
            }
        }
    }

    private void evalBreakConstrs() {
        for (Constraint c : inst.breakConstrs) {
            int counter = 0;

            // BR1
            if (c.tag == Constraint.Tag.BR1) {
                if (c.mode2.equals("H")) {
                    for (int i : c.teams) {
                        for (int s : c.slots) {
                            if (s == 0) continue;
                            if (gamesPerTeam[i][s - 1].home == i && gamesPerTeam[i][s].home == i)
                                counter++;
                        }
                    }
                }
                else if (c.mode2.equals("A")) {
                    for (int i : c.teams) {
                        for (int s : c.slots) {
                            if (s == 0) continue;
                            if (gamesPerTeam[i][s - 1].home != i && gamesPerTeam[i][s].home != i)
                                counter++;
                        }
                    }
                }
                else if (c.mode2.equals("HA")) {
                    for (int i : c.teams) {
                        for (int s : c.slots) {
                            if (s == 0) continue;
                            if (gamesPerTeam[i][s - 1].home == i && gamesPerTeam[i][s].home == i)
                                counter++;
                            if (gamesPerTeam[i][s - 1].home != i && gamesPerTeam[i][s].home != i)
                                counter++;
                        }
                    }
                }

                if (counter > c.intp) {
                    if (c.hard)
                        infeasBR1 += (counter - c.intp);
                    else
                        costBR1 += (counter - c.intp) * c.penalty;
                }
            }

            // BR2
            else if (c.tag == Constraint.Tag.BR2) {
                for (int i : c.teams) {
                    for (int s : c.slots) {
                        if (s == 0) continue;
                        if (gamesPerTeam[i][s - 1].home == i && gamesPerTeam[i][s].home == i)
                            counter++;
                        if (gamesPerTeam[i][s - 1].home != i && gamesPerTeam[i][s].home != i)
                            counter++;
                    }
                }

                if (counter > c.intp) {
                    if (c.hard)
                        infeasBR2 += (counter - c.intp);
                    else
                        costBR2 += (counter - c.intp) * c.penalty;
                }
            }
        }
    }

    private void evalFairnessConstrs() {
        for (Constraint c : inst.fairnessConstrs) {
            // FA2
            if (c.tag == Constraint.Tag.FA2) {
                for (int i = 0; i < c.teams.length; i++) {
                    for (int j = i + 1; j < c.teams.length; j++) {
                        int largestDiff = 0;
                        int homeI = 0;
                        int homeJ = 0;
                        for (int s : c.slots) {
                            int diffS = 0;
                            if (gamesPerTeam[i][s].home == i)
                                ++homeI;
                            if (gamesPerTeam[j][s].home == j)
                                ++homeJ;
                            diffS = Math.abs(homeI - homeJ);
                            if (diffS > largestDiff)
                                largestDiff = diffS;
                        }
                        if (largestDiff > c.intp) {
                            if (c.hard)
                                infeasFA2 += largestDiff - c.intp;
                            else
                                costFA2 += (largestDiff - c.intp) * c.penalty;
                        }
                    }
                }
            }
        }
    }

    private void evalSeparationConstrs() {
        for (Constraint c : inst.separationConstrs) {
            // SE1
            if (c.tag == Constraint.Tag.SE1) {
                for (int t1 = 0; t1 < c.teams.length; t1++) {
                    for (int t2 = t1 + 1; t2 < c.teams.length; t2++) {
                        Game game1 = inst.getGame(c.teams[t1], c.teams[t2]);
                        Game game2 = inst.getGame(c.teams[t2], c.teams[t1]);
                        int diff = Math.abs(gameSlot[game1.id] - gameSlot[game2.id]) - 1;
                        if (c.min >= 0 && diff < c.min) {
                            if (c.hard)
                                infeasSE1 += c.min - diff;
                            else
                                costSE1 += (c.min - diff) * c.penalty;
                        }
                        else if (c.max >= 0 && diff > c.max) {
                            if (c.hard)
                                infeasSE1 += diff - c.max;
                            else
                                costSE1 += (diff - c.max) * c.penalty;
                        }
                    }
                }
            }
        }
    }


    // region Standard setters and getters

    public int getCostCA1() {
        return costCA1;
    }

    public void setCostCA1(int costCA1) {
        this.cost += costCA1 - this.costCA1;
        this.costCA1 = costCA1;
    }

    public int getCostCA2() {
        return costCA2;
    }

    public void setCostCA2(int costCA2) {
        this.cost += costCA2 - this.costCA2;
        this.costCA2 = costCA2;
    }

    public int getCostCA3() {
        return costCA3;
    }

    public void setCostCA3(int costCA3) {
        this.cost += costCA3 - this.costCA3;
        this.costCA3 = costCA3;
    }

    public int getCostCA4() {
        return costCA4;
    }

    public void setCostCA4(int costCA4) {
        this.cost += costCA4 - this.costCA4;
        this.costCA4 = costCA4;
    }

    public int getCostGA1() {
        return costGA1;
    }

    public void setCostGA1(int costGA1) {
        this.cost += costGA1 - this.costGA1;
        this.costGA1 = costGA1;
    }

    public int getCostBR1() {
        return costBR1;
    }

    public void setCostBR1(int costBR1) {
        this.cost += costBR1 - this.costBR1;
        this.costBR1 = costBR1;
    }

    public int getCostBR2() {
        return costBR2;
    }

    public void setCostBR2(int costBR2) {
        this.cost += costBR2 - this.costBR2;
        this.costBR2 = costBR2;
    }

    public int getCostFA2() {
        return costFA2;
    }

    public void setCostFA2(int costFA2) {
        this.cost += costFA2 - this.costFA2;
        this.costFA2 = costFA2;
    }

    public int getCostSE1() {
        return costSE1;
    }

    public void setCostSE1(int costSE1) {
        this.cost += costSE1 - this.costSE1;
        this.costSE1 = costSE1;
    }

    public int getInfeasCA1() {
        return infeasCA1;
    }

    public void setInfeasCA1(int infeasCA1) {
        this.infeas += infeasCA1 - this.infeasCA1;
        this.infeasCA1 = infeasCA1;
    }

    public int getInfeasCA2() {
        return infeasCA2;
    }

    public void setInfeasCA2(int infeasCA2) {
        this.infeas += infeasCA2 - this.infeasCA2;
        this.infeasCA2 = infeasCA2;
    }

    public int getInfeasCA3() {
        return infeasCA3;
    }

    public void setInfeasCA3(int infeasCA3) {
        this.infeas += infeasCA3 - this.infeasCA3;
        this.infeasCA3 = infeasCA3;
    }

    public int getInfeasCA4() {
        return infeasCA4;
    }

    public void setInfeasCA4(int infeasCA4) {
        this.infeas += infeasCA4 - this.infeasCA4;
        this.infeasCA4 = infeasCA4;
    }

    public int getInfeasGA1() {
        return infeasGA1;
    }

    public void setInfeasGA1(int infeasGA1) {
        this.infeas += infeasGA1 - this.infeasGA1;
        this.infeasGA1 = infeasGA1;
    }

    public int getInfeasBR1() {
        return infeasBR1;
    }

    public void setInfeasBR1(int infeasBR1) {
        this.infeas += infeasBR1 - this.infeasBR1;
        this.infeasBR1 = infeasBR1;
    }

    public int getInfeasBR2() {
        return infeasBR2;
    }

    public void setInfeasBR2(int infeasBR2) {
        this.infeas += infeasBR2 - this.infeasBR2;
        this.infeasBR2 = infeasBR2;
    }

    public int getInfeasFA2() {
        return infeasFA2;
    }

    public void setInfeasFA2(int infeasFA2) {
        this.infeas += infeasFA2 - this.infeasFA2;
        this.infeasFA2 = infeasFA2;
    }

    public int getInfeasSE1() {
        return infeasSE1;
    }

    public void setInfeasSE1(int infeasSE1) {
        this.infeas += infeasSE1 - this.infeasSE1;
        this.infeasSE1 = infeasSE1;
    }

    // endregion Standard setters and getters
}
