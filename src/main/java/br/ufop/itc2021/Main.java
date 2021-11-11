package br.ufop.itc2021;

import br.ufop.itc2021.algorithm.constructive.*;
import br.ufop.itc2021.algorithm.heuristic.*;
import br.ufop.itc2021.algorithm.mip.*;
import br.ufop.itc2021.algorithm.neighborhood.*;
import br.ufop.itc2021.model.*;
import br.ufop.itc2021.util.*;
import org.dom4j.*;

import java.io.*;
import java.util.*;

/**
 * This class is the Main class of the program, responsible of parsing the input, instantiating moves and heuristics and
 * printing the results.
 *
 * @author George Fonseca and Tulio Toffolo
 */
public class Main {

    // region solver parameters and default values

    public static long startTimeMillis = System.currentTimeMillis();

    public static boolean validate = false;

    public static String algorithm = "ils";
    public static String inFile;
    public static String outFile = null;

    public static String start = null;

    public static long seed = 0;
    public static long maxIters = ( long ) 1e6;
    public static long timeLimit = 60 * 1000;

    public static int bestKnown = Integer.MAX_VALUE;
    public static int lowerBound = 0;

    // ILS
    public static long rnaMax = 50000;
    public static int itersP = 10;
    public static int p0 = 3;
    public static int pMax = 20;

    // LAHC
    public static int listSize = 1000;
    public static double listMult = 1.2;

    // SA (Simulated Annealing)
    public static double alpha = 0.99;
    public static int saMax = ( int ) 1e7;
    public static double t0 = 1;

    // SCHC
    public static int stepSize = 1000;

    // MIP
    public static int mipFocus = 1;
    public static int verbose = 0;
    public static int threads = 0;
    public static int fixOptTimeLimit = 100;

    // Neighborhoods
    public static boolean[] neighborhoods;
    public static final int maxNeighborhoods = 2;

    static {
        neighborhoods = new boolean[maxNeighborhoods];
        Arrays.fill(neighborhoods, true);
    }

    // endregion solver parameters and default values


    /**
     * The entry point of application.
     *
     * @param args the input arguments
     * @throws IOException if any IO error occurs.
     */
    public static void main(String[] args) throws IOException, DocumentException {
        //System.out.println("Aperter ENTER:");
        //System.in.read();
        Locale.setDefault(new Locale("en-US"));
        readArgs(args);

        Instance inst = new Instance(inFile);
        Random random = new Random(seed);

        // check if solver should be executed only as a validator
        if (validate) {
            Solution solution = new Solution(inst);
            solution.read(start == null ? outFile : start);
            boolean validSolution = solution.validate(System.out);
            if (validSolution) {
                System.out.println("\nSolution for \"" + inst.name + "\" has infeas=" + solution.getInfeas() + " and cost=" + solution.getFeasCost() + "  :)\n");
            }
            else {
                System.out.println("\nSolution is invalid!  :'(\n");
            }
            return;
        }

        Heuristic solver = null;
        switch (algorithm) {
            case "lahc" -> solver = new LAHC(inst, random, lowerBound, listSize, listMult);

            case "lahc-ils" -> solver = new ILS(inst, random, lowerBound, new LAHC(inst, random, lowerBound, listSize, listMult), rnaMax, itersP, p0, pMax)
              .addPertMove(new EjectionChain(inst, random, 1))
              .addPertMove(new SwapHomeTeam(inst, random, 1))
              .addPertMove(new SwapTeams(inst, random, 1));

            case "ils" -> solver = new ILS(inst, random, lowerBound, rnaMax, itersP, p0, pMax)
              .addPertMove(new EjectionChain(inst, random, 1))
              .addPertMove(new SwapHomeTeam(inst, random, 1))
              .addPertMove(new SwapTeams(inst, random, 1));

            case "sa" -> solver = new SA(inst, random, lowerBound, alpha, t0, saMax);

            case "sa-ils" -> solver = new ILS(inst, random, lowerBound, new SA(inst, random, lowerBound, alpha, t0, saMax), rnaMax, itersP, p0, pMax);

            case "schc" -> solver = new SCHC(inst, random, lowerBound, stepSize);

            case "schc-ils" -> solver = new ILS(inst, random, lowerBound, new SCHC(inst, random, lowerBound, stepSize), rnaMax, itersP, p0, pMax);

            case "desc" -> solver = new Descent(inst, random, lowerBound);

            default -> {
                System.exit(-1);
                return;
            }
        }

        System.out.printf("java -jar itc2021.jar");
        for (int i = 0; i < args.length; i++) {
            System.out.printf(" %s", args[i]);
        }

        System.out.printf("\n\n");
        System.out.printf("Instance....: %s\n", inst.name);
        System.out.printf("Algorithm...: %s\n", solver);
        System.out.printf("Other params: maxIters=%s, seed=%d, timeLimit=%.2fs\n\n", Util.longToString(maxIters), seed, timeLimit / 1000.0);
        System.out.printf("    /------------------------------------------------------------\\\n");
        System.out.printf("    | %8s | %8s | %10s | %10s | %10s | %s\n", "Iter", "Gap(%)", "S*", "S'", "Time", "");
        System.out.printf("    |----------|----------|------------|------------|------------|\n");

        // re-starting time counting (after reading files)
        startTimeMillis = System.currentTimeMillis();

        // generating initial solution
        Solution solution;
        if (start == null) {
            solution = SimpleConstructive.circleSolution(inst, random);
        }
        else {
            try {
                solution = new Solution(inst);
                solution.read(start);
            } catch (DocumentException e) {
                System.out.printf("Solution file %s could not be found or read!\nBuilding initial solution instead.\n", start);
                solution = SimpleConstructive.circleSolution(inst, random);
            }
        }

        // creating mip model
        int phase = solution.isFeasible() ? 2 : 1;
        MIPFull mip = new MIPFull(inst, solution, phase, timeLimit, seed, mipFocus, verbose, threads, fixOptTimeLimit);

        // adding moves (neighborhoods)
        createNeighborhoods(inst, random, solver, mip);

        Util.safePrintStatus(System.out, 0, solution, solution, "s0");
        assert solution.validate(System.err);

        // running stochastic local search
        if (solver.getMoves().size() > 0 && solution.getCost() >= lowerBound + Util.EPS)
            solution = solver.run(solution, timeLimit, maxIters, System.out);
        solution.validate(System.err);

        System.out.printf("    \\------------------------------------------------------------/\n\n");

        if (solution.getCost() <= lowerBound + Util.EPS)
            System.out.printf("Optimal solution found!\n\n");

        System.out.printf("Neighborhoods statistics (values in %%):\n\n");
        System.out.printf("    /----------------------------------------------------------------\\\n");
        System.out.printf("    | %-18s | %8s | %8s | %8s | %8s |\n", "Move", "Improvs.", "Sideways", "Accepts", "Rejects");
        System.out.printf("    |--------------------|----------|----------|----------|----------|\n");
        for (Move move : solver.getMoves())
            Util.safePrintMoveStatistics(System.out, move, "");
        System.out.printf("    \\----------------------------------------------------------------/\n\n");

        if (bestKnown != Integer.MAX_VALUE)
            System.out.printf("Best Gap..........: %.4f%%\n", 100 * ( double ) (solution.getCost() - bestKnown) / ( double ) solution.getCost());
        System.out.printf("Best cost.........: %s\n", solution.getCostStr());
        System.out.printf("N. of Iterations..: %d\n", solver.getNIters());
        System.out.printf("Total runtime.....: %.2fs\n", (System.currentTimeMillis() - startTimeMillis) / 1000.0);

        solution.write(outFile);
    }

    /**
     * Prints the program usage.
     */
    public static void printUsage() {
        System.out.println("Usage: java -jar itc2021.jar <input> <output> [options]");
        System.out.println("    <input>  : Path of the problem input file.");
        System.out.println("    <output> : Path of the (output) solution file.");
        System.out.println();
        System.out.println("Options:");
        System.out.println("    -algorithm <value>     : ils, lahc, lahc-ils, sa, sa-ils, schc or schc-ils (default: " + algorithm + ").");
        System.out.println("    -bestKnown <value>     : best known makespan for RDP output (default: " + bestKnown + ").");
        System.out.println("    -lowerBound <value>    : best known lower bound for Gap output (default: " + lowerBound + ").");
        System.out.println("    -maxIters <value>      : maximum number of consecutive rejections (default: Long.MAXVALUE).");
        System.out.println("    -seed <value>          : random seed (default: " + seed + ").");
        System.out.println("    -start <file_path>     : sets an initial (start) solution for the algorithm.");
        System.out.println("    -time <value>          : time limit in seconds (default: " + timeLimit + ").");
        System.out.println("    -validate              : executes the solver as a validator (existing output file will be checked).");
        System.out.println();
        System.out.println("    ILS parameters:");
        System.out.println("        -rnamax <value> : maximum rejected iterations in the descent phase of ILS (default: " + rnaMax + ").");
        System.out.println("        -itersP <value> : number of iterations per perturbation level for ILS (default: " + itersP + ").");
        System.out.println("        -p0 <value>     : initial perturbation level for ILS (default: " + p0 + ").");
        System.out.println("        -pMax <value>   : maximum steps up (each step of value p0) for ILS perturbation's level (default: " + pMax + ").");
        System.out.println();
        System.out.println("    LAHC parameters:");
        System.out.println("        -listSize <value> : LAHC list size (default: " + listSize + ").");
        System.out.println("        -listMult <value> : Multiplier to apply on initial solution's cost (default: " + listMult + ").");
        System.out.println();
        System.out.println("    SA parameters:");
        System.out.println("        -alpha <value> : cooling rate for the Simulated Annealing (default: " + alpha + ").");
        System.out.println("        -samax <value> : iterations before updating the temperature for Simulated Annealing (default: " + saMax + ").");
        System.out.println("        -t0 <value>    : initial temperature for the Simulated Annealing (default: " + t0 + ").");
        System.out.println();
        System.out.println("    SCHC parameters:");
        System.out.println("        -stepSize <value> : SCHC step size (default: " + stepSize + ").");
        System.out.println();
        System.out.println("    MIP parameters:");
        System.out.println("        -mipFocus <value>   : focus of MIP solver search: 0 for solver default, 1 for feasibility emphasis, 2 for optimality emphasis (default " + mipFocus + ").");
        System.out.println("        -verbose <value>    : print (or not) solver logs on solving process (default: " + verbose + ").");
        System.out.println("        -threads <value>    : number of allowed threads for the solver: 0 stands for all available (default: " + threads + ").");
        System.out.println("        -fixOptTime <value> : time limit for each fix-and-optimize iteration (default: " + fixOptTimeLimit + ").");
        System.out.println();
        System.out.println("    Neighborhoods selection:");
        System.out.println("        -n <id,value> : disables a neighborhood id(0..5) if value = 0 and enables it otherwise.");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("    java -jar itc2021.jar instance.xml solution.xml");
        System.out.println("    java -jar itc2021.jar instance.xml solution.xml -validate");
        System.out.println("    java -jar itc2021.jar instance.xml solution.xml -algorithm sa -lowerbound 100 -alpha 0.98 -samax 1000 -t0 100000");
        System.out.println();
    }

    /**
     * Reads the input arguments.
     *
     * @param args the input arguments
     */
    public static void readArgs(String args[]) {
        if (args.length < 2) {
            printUsage();
            System.exit(-1);
        }

        int index = -1;

        inFile = args[++index];
        outFile = args[++index];

        while (index < args.length - 1) {
            String option = args[++index].toLowerCase();

            switch (option) {
                case "-algorithm" -> algorithm = args[++index].toLowerCase();
                case "-bestknown" -> bestKnown = Integer.parseInt(args[++index]);
                case "-lowerbound" -> lowerBound = Integer.parseInt(args[++index]);
                case "-maxiters" -> maxIters = Long.parseLong(args[++index]);
                case "-seed" -> seed = Integer.parseInt(args[++index]);
                case "-start" -> start = args[++index];
                case "-time" -> timeLimit = Math.round(Double.parseDouble(args[++index]) * 1000.0);
                case "-validate" -> validate = true;

                // ILS
                case "-rnamax" -> rnaMax = Long.parseLong(args[++index]);
                case "-itersp" -> itersP = Integer.parseInt(args[++index]);
                case "-p0" -> p0 = Integer.parseInt(args[++index]);
                case "-pmax" -> pMax = Integer.parseInt(args[++index]);

                // LAHC
                case "-listsize" -> listSize = Integer.parseInt(args[++index]);
                case "-listmult" -> listMult = Double.parseDouble(args[++index]);

                // SA
                case "-alpha" -> alpha = Double.parseDouble(args[++index]);
                case "-samax" -> saMax = Integer.parseInt(args[++index]);
                case "-t0" -> t0 = Double.parseDouble(args[++index]);

                // SCHC
                case "-stepsize" -> stepSize = Integer.parseInt(args[++index]);

                // MIP
                case "-mipfocus" -> mipFocus = Integer.parseInt(args[++index]);
                case "-verbose" -> verbose = Integer.parseInt(args[++index]);
                case "-threads" -> threads = Integer.parseInt(args[++index]);
                case "-fixopttime" -> fixOptTimeLimit = Integer.parseInt(args[++index]);

                // Neighborhoods selection
                case "-n" -> {
                    String[] values = args[++index].split(",");
                    int i = Integer.parseInt(values[0]) * 4 + Integer.parseInt(values[1]);
                    neighborhoods[i] = values[2].equals("1");
                }

                default -> {
                    printUsage();
                    System.exit(-1);
                }
            }
        }
    }


    private static void createNeighborhoods(Instance inst, Random random, Heuristic solver, MIPFull mip) {
        int defaultWeight = 100;

        solver.addMove(new SlotsFixOpt(inst, random, defaultWeight / 100, mip));
        solver.addMove(new TeamsFixOpt(inst, random, defaultWeight / 100, mip));

//        solver.addMove(new EjectionChain(inst, random, defaultWeight));
        solver.addMove(new PartialRoundSwap(inst, random, defaultWeight));
        solver.addMove(new SwapHomeTeam(inst, random, defaultWeight));
        solver.addMove(new SwapSlots(inst, random, defaultWeight));
        solver.addMove(new SwapTeams(inst, random, defaultWeight));

//        solver.addMove(new CompoundedMove(inst, random, "2-SwapSlots", new MoveToCompound[]{
//          new EjectionChain(inst, random, defaultWeight),
//          new SwapHomeTeam(inst, random, defaultWeight),
//        }, defaultWeight));

        solver.addMove(new CompoundedMove(inst, random, "2-SwapSlots", new MoveToCompound[]{
          new SwapSlots(inst, random, defaultWeight),
          new SwapSlots(inst, random, defaultWeight),
        }, defaultWeight));
        solver.addMove(new CompoundedMove(inst, random, "2-SwapSlots", new MoveToCompound[]{
          new SwapSlots(inst, random, defaultWeight),
          new SwapSlots(inst, random, defaultWeight),
          new SwapSlots(inst, random, defaultWeight),
        }, defaultWeight));

        solver.addMove(new CompoundedMove(inst, random, "2-SwapHomeTeams", new MoveToCompound[]{
          new SwapHomeTeam(inst, random, defaultWeight),
          new SwapHomeTeam(inst, random, defaultWeight),
        }, defaultWeight));
        solver.addMove(new CompoundedMove(inst, random, "2-SwapHomeTeams", new MoveToCompound[]{
          new SwapHomeTeam(inst, random, defaultWeight),
          new SwapHomeTeam(inst, random, defaultWeight),
          new SwapHomeTeam(inst, random, defaultWeight),
        }, defaultWeight));

        if (solver instanceof ILS) {
            ((ILS) solver).addPertMove(new EjectionChain(inst, random, defaultWeight));
            ((ILS) solver).addPertMove(new CompoundedMove(inst, random, "2-SwapSlots", new MoveToCompound[]{
                    new EjectionChain(inst, random, defaultWeight),
                    new SwapHomeTeam(inst, random, defaultWeight),
            }, defaultWeight));
        }
    }
}
