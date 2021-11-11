package br.ufop.itc2021.algorithm.neighborhood;

import br.ufop.itc2021.model.*;
import br.ufop.itc2021.util.*;

import java.util.*;

public class EjectionChain extends MoveToCompound {

    public final List<Realloc> moves;
    public final int sMax;

    private int nMoves, sBegin, sEnd;
    private final List<Graph> graphs = new ArrayList<>();

    private final List<Realloc> undo = new ArrayList<>();

    public EjectionChain(Instance inst, Random random, int weight) {
        super(inst, random, "EjectionChain", weight);

        moves = new ArrayList<>(inst.nGames * inst.nSlots);
        sMax = inst.phased ? inst.nSlots / 2 : inst.nSlots;
        for (int g = 0; g < inst.nGames; g++) {
            for (int s = 1; s < sMax; s++) {
                moves.add(new Realloc(inst.games[g], s));
            }
        }
        nMoves = moves.size();

        for (int i = 0; i < inst.nSlots; i++) {
            graphs.add(null);
        }
    }

    public void accept() {
        super.accept();
    }

    public long doMove(Solution solution) {
        super.doMove(solution);
        undo.clear();

        Realloc realloc;
        Game game;
        int slot;

        do {
            if (nMoves == 0) return 0;

            // randomly selecting teams and updating number of remaining ones
            int randomMove = random.nextInt(nMoves);
            Util.swap(moves, randomMove, --nMoves);

            // getting move to apply (i.e. game and slot)
            realloc = moves.get(randomMove);
            game = realloc.game;
            slot = realloc.getNewSlot(solution);
        } while (solution.gameSlot[inst.getGame(game.away, game.home).id] == slot);

        // creating graph
        int sBegin = slot >= sMax ? sMax : 0;
        int sEnd = slot >= sMax ? sMax * 2 : sMax;
        for (int s = sBegin; s < sEnd; s++) {
            graphs.set(s, new Graph(solution, s));
        }

        chain(new ReallocFT(solution, realloc));
        //System.out.println("-------");
        assert (checkSolution());

        if (!inChain) solution.updateCost();
        return deltaCost = solution.getCost() - initialTotalCost;
    }

    private boolean checkSolution() {
        boolean error = false;
        int n = -1;
        for (int s = 0; s < inst.nSlots; s++) {
            if (n >= 0 && n != solution.gamesPerSlot.get(s).size()) {
                System.out.printf("Slot %d has a different number of games\n", s);
                error = true;
            }
            n = solution.gamesPerSlot.get(s).size();
        }
        return !error;
    }

    private void chain(ReallocFT move) {
        Stack<ReallocFT> jobs = new Stack<>();
        ReallocFT[] nextJobs = new ReallocFT[2];

        removeGame(move.game, move.fromSlot);
        jobs.add(move);

        while (!jobs.isEmpty()) {
            ReallocFT job = jobs.pop();
            if (solution.hasGame(job.game))
                continue;

            // aliases (to make code more readable)
            Game game = job.game;
            int slot = job.toSlot;

            // looking for a possible slot in case it wasn't specified..
            if (slot == -1)
                for (int s = 0; s < inst.nSlots; s++) {
                    if (solution.gamesPerSlot.get(s).size() < inst.nTeams / 2) {
                        slot = s;
                        break;
                    }
                }

            // adding requested game to the graph
            graphs.get(slot).add(game);
            //System.out.printf("Adding   %s into slot %02d\n", game, slot);

            int nNextJobs = 0;
            // removing home duplicates
            if (graphs.get(slot).degree(game.home) > 1) {
                nextJobs[nNextJobs] = new ReallocFT(graphs.get(slot).get(game.home).get(0), slot);
                removeGame(nextJobs[nNextJobs].game, slot);
                //System.out.printf("Removing %s from slot %02d\n", nextJobs[nNextJobs].game, slot);
                nNextJobs++;
            }
            // removing away duplicates
            if (graphs.get(slot).degree(game.away) > 1) {
                nextJobs[nNextJobs] = new ReallocFT(graphs.get(slot).get(game.away).get(0), slot);
                removeGame(nextJobs[nNextJobs].game, slot);
                //System.out.printf("Removing %s from slot %02d\n", nextJobs[nNextJobs].game, slot);
                nNextJobs++;
            }

            // collecting teams not covered in graph
            int n = 0;
            int[] teams = new int[2];
            for (int i = 0; i < inst.nTeams; i++)
                if (graphs.get(slot).degree(i) == 0)
                    teams[n++] = i;

            Game additionalGame = null;
            if (n == 2) {
                if (!solution.hasGame(teams[0], teams[1]))
                    additionalGame = inst.getGame(teams[0], teams[1]);
                else if (!solution.hasGame(teams[1], teams[0]))
                    additionalGame = inst.getGame(teams[1], teams[0]);
                else if ((solution.gameSlot[inst.getGame(teams[0], teams[1]).id] < sMax && slot >= sMax)
                  || (solution.gameSlot[inst.getGame(teams[0], teams[1]).id] >= sMax && slot < sMax))
                    additionalGame = inst.getGame(teams[1], teams[0]);
                else
                    additionalGame = inst.getGame(teams[0], teams[1]);
            }

            if (nNextJobs == 2) {
                ReallocFT nextJob = nextJobs[1];
                nextJob.toSlot = job.fromSlot;
                assert (job.fromSlot != -1);
                jobs.add(nextJob);
            }
            if (nNextJobs >= 1 && additionalGame != null) {
                ReallocFT nextJob = nextJobs[0];
                nextJob.toSlot = solution.gameSlot[additionalGame.id];
                //assert(solution.gameSlot[additionalGame.id] != -1);
                jobs.add(nextJob);
            }

            if (additionalGame != null) {
                graphs.get(slot).add(additionalGame);
                //System.out.printf("Adding   %s into slot %02d\n", additionalGame, slot);
            }

            // updating solution (removing/add games to new slots)
            if (solution.hasGame(game))
                removeGame(game, job.fromSlot);
            if (solution.hasGame(additionalGame))
                removeGame(additionalGame, solution.gameSlot[additionalGame.id]);
            solution.addGame(game, slot);
            if (additionalGame != null) solution.addGame(additionalGame, slot);
            //System.out.println();
        }
    }

    public boolean hasMove(Solution solution) {
        return nMoves > 0;
    }

    public void reject() {
        super.reject();

        for (int i = undo.size() - 1; i >= 0; i--)
            solution.setGame(undo.get(i).game, undo.get(i).slot);
        undo.clear();
        //solution.forceUpdateCost(initialInfeas, initialFeasCost);

        solution.updateCost();
        assert (solution.getInfeas() == initialInfeas);
        assert (solution.getFeasCost() == initialFeasCost);
    }

    private void removeGame(Game game, int slot) {
        graphs.get(slot).remove(game);
        undo.add(new Realloc(game, slot));
        solution.removeGame(game);
    }

    public void reset() {
        super.reset();
        nMoves = moves.size();
    }

    private class Graph {

        public final Solution solution;
        public final int slot;

        public List<List<Game>> vertices = new ArrayList<>();
        public Set<Game> edges = new HashSet<>();

        public Graph(Solution sol, int slot) {
            this.solution = sol;
            this.slot = slot;

            for (int i = 0; i < sol.inst.nTeams; i++) {
                vertices.add(new ArrayList<>());
                vertices.get(i).add(sol.gamesPerTeam[i][slot]);
                edges.add(sol.gamesPerTeam[i][slot]);
            }
        }

        public void add(Game game) {
            vertices.get(game.home).add(game);
            vertices.get(game.away).add(game);
            edges.add(game);
        }

        public int degree(int vertex) {
            return vertices.get(vertex).size();
        }

        public List<Game> get(int id) {
            return vertices.get(id);
        }

        public boolean is1Factorization() {
            for (int i = 0; i < vertices.size(); i++)
                if (degree(i) != 1)
                    return false;
            return true;
        }

        public void remove(Game game) {
            vertices.get(game.home).remove(game);
            vertices.get(game.away).remove(game);
            edges.remove(game);
        }
    }

    private class Realloc {

        public final Game game;
        public final int slot;

        public Realloc(Game game, int slot) {
            this.game = game;
            this.slot = slot;
        }

        public int getNewSlot(Solution solution) {
            int prevSlot = solution.gameSlot[game.id];
            if (solution.inst.phased) {
                if (prevSlot < solution.inst.nSlots / 2) {
                    return (slot + prevSlot) % (solution.inst.nSlots / 2);
                }
                else {
                    prevSlot -= solution.inst.nSlots / 2;
                    int newSlot = (slot + prevSlot) % (solution.inst.nSlots / 2);
                    return newSlot + solution.inst.nSlots / 2;
                }
            }
            else {
                return (slot + prevSlot) % solution.inst.nSlots;
            }
        }
    }

    private class ReallocFT {

        public final Game game;
        public int fromSlot, toSlot;

        public ReallocFT(Game game, int fromSlot) {
            this.game = game;
            this.fromSlot = fromSlot;
        }

        public ReallocFT(Game game, int fromSlot, int toSlot) {
            this.game = game;
            this.fromSlot = fromSlot;
            this.toSlot = toSlot;
        }

        public ReallocFT(Solution solution, Realloc r) {
            this.game = r.game;
            this.fromSlot = solution.gameSlot[r.game.id];
            this.toSlot = r.getNewSlot(solution);
        }
    }
}
