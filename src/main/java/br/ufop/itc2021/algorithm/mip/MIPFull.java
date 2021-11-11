package br.ufop.itc2021.algorithm.mip;

import br.ufop.itc2021.model.*;
import gurobi.*;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class builds a full MIP model to represent ITC2021 instances.
 *
 * @author George Fonseca and Tulio Toffolo
 */
public class MIPFull {

    public final Instance inst;
    public Solution solution;
    public int phase;
    public long timeLimit;
    public int seed;
    public int mipFocus;
    public int verbose;
    public int threads;
    public int fixOptTimeLimit;

    // model related vars
    private GRBEnv env;
    private GRBModel model;
    public GRBVar[][][]   x;
    private GRBVar[][]     bH;
    private GRBVar[][]     bA;
    private ArrayList<ArrayList<GRBVar>> sCA1;
    private ArrayList<ArrayList<GRBVar>> sCA2;
    private ArrayList<ArrayList<ArrayList<GRBVar>>> sCA3;
    private ArrayList<ArrayList<GRBVar>> sCA4;
    private ArrayList<GRBVar> sGA1;
    private ArrayList<ArrayList<GRBVar>> sBR1;
    private ArrayList<GRBVar> sBR2;
    public ArrayList<ArrayList<ArrayList<ArrayList<GRBVar>>>> sFA2Line;
    public ArrayList<ArrayList<ArrayList<GRBVar>>> sFA2;
    private ArrayList<ArrayList<ArrayList<ArrayList<ArrayList<GRBVar>>>>> sSE1;

    // auxiliary structures
    private HashMap<Integer, Integer> cIndexMap;
    private int s_ub;

    public MIPFull(Instance inst, Solution solution, int phase, long timeLimit, long seed, int mipFocus, int verbose, int threads, int fixOptTimeLimit) {
        this.inst = inst;
        this.solution = solution;
        this.phase = phase;
        this.timeLimit = timeLimit;
        this.seed = Math.toIntExact(seed);
        this.mipFocus = mipFocus;
        this.verbose = verbose;
        this.threads = threads;
        this.fixOptTimeLimit = fixOptTimeLimit;
        try {
            createModel();
            //this.model.write(this.inst.name + ".lp");
            if (solution != null)
                importSolution(this.solution);
            setModelParams();
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    public GRBModel getModel() {
        return model;
    }

    public void importSolution(Solution solution)  {
        try {
            for (int s = 0; s < inst.nSlots; s++) {
                for (Game game : inst.games) {
                    x[game.home][game.away][s].set(GRB.DoubleAttr.Start, 0.0);
                }
            }
            for (int s = 0; s < inst.nSlots; s++) {
                for (Game game : solution.gamesPerSlot.get(s)) {
                    x[game.home][game.away][s].set(GRB.DoubleAttr.Start, 1.0);
                }
            }
            model.update();
        } catch (GRBException e) {
            System.err.println("Solution could not be loaded!");
            e.printStackTrace();
        }
    }

    public void exportSolution(Solution solution) throws GRBException {
        for (int s = 0; s < inst.nSlots; s++) {
            for (Game game : inst.games) {
                if (x[game.home][game.away][s].get(GRB.DoubleAttr.X) > 0.999) {
                    solution.setGame(game, s);
                }
            }
        }
        solution.updateCost();
    }


    public void setTimeLimit(int timeLimit) throws GRBException {
        model.set(GRB.DoubleParam.TimeLimit, timeLimit);
    }


    public void setModelParams() throws GRBException {
        //model.write(inst.name + ".lp");
        model.set(GRB.DoubleParam.TimeLimit, timeLimit);
        model.set(GRB.IntParam.Seed, seed);
        model.set(GRB.IntParam.MIPFocus, mipFocus);
        model.set(GRB.IntParam.LogToConsole, verbose);
        model.set(GRB.IntParam.Threads, threads);
    }

    public void solveModel() throws GRBException {
        model.optimize();
    }

    public void createModel() throws GRBException {
        this.env = new GRBEnv();
        this.model = new GRBModel(env);

        // creating vars
        createXVars();
        createBVars();

        // creating slack variables *s*
        initializeSlackVarsArrays();
        createSlackVars();

        // creating constraints
        createConstr1();
        createConstr2();
        createConstrPhased();
        createConstrComputeBreaks();
        createInstanceSpecificConstrs();
    }

    private void createSlackVars() throws GRBException {
        for (int c = 0; c < inst.nConstrs; ++c) {
            Constraint constr = inst.constrs[c];
            // skipping all SOFT constraint if in Phase 1 (in Phase 1 SOFT constraints are ignored)
            if (this.phase == 1 && constr.type.equals("SOFT"))
                continue;

            // setting s_ub to 0 if constraints is HARD in Phase 2 (in Phase 2 HARD constraints must be enforced)
            this.s_ub = 1000;
            if (this.phase == 2 && constr.type.equals("HARD"))
                this.s_ub = 0;

            if (constr.tag == Constraint.Tag.CA1) {
                createSCA1Vars(c, constr);
            }
            if (constr.tag == Constraint.Tag.CA2) {
                createSCA2Vars(c, constr);
            }
            if (constr.tag == Constraint.Tag.CA3) {
                createSCA3Vars(c, constr);
            }
            if (constr.tag == Constraint.Tag.CA4) {
                createSCA4Vars(c, constr);
            }
            if (constr.tag == Constraint.Tag.GA1) {
                createGA1Vars(c, constr);
            }
            if (constr.tag == Constraint.Tag.BR1) {
                createBR1Vars(c, constr);
            }
            if (constr.tag == Constraint.Tag.BR2) {
                createBR2Vars(c, constr);
            }
            if (constr.tag == Constraint.Tag.FA2) {
                createFA2LineVars(c, constr);
                createFA2Vars(c, constr);
            }
            if (constr.tag == Constraint.Tag.SE1) {
                createSE1Vars(c, constr);
            }
        }
    }

    private void createSE1Vars(int c, Constraint constr) throws GRBException {
        this.sSE1.add(new ArrayList<>());
        this.cIndexMap.put(c, this.sSE1.size() - 1);
        for (int i : constr.teams) {
            this.sSE1.get(this.sSE1.size() - 1).add(new ArrayList<>());
            for (int j : constr.teams) {
                this.sSE1.get(this.sSE1.size() - 1).get(this.sSE1.get(this.sSE1.size() - 1).size() - 1).add(new ArrayList<>());
                for (int k = 0; k < inst.nSlots; ++k) {
                    this.sSE1.get(this.sSE1.size() - 1).get(this.sSE1.get(this.sSE1.size() - 1).size() - 1)
                            .get(this.sSE1.get(this.sSE1.size() - 1).get(this.sSE1.get(this.sSE1.size() - 1).size() - 1).size() - 1)
                            .add(new ArrayList<>());
                    for (int k2 = 0; k2 < inst.nSlots; ++k2) {
                        int diff = constr.min - (k - k2 - 1);
                        if (k > k2 && diff > 0) {
                            this.sSE1.get(this.sSE1.size() - 1)
                                    .get(this.sSE1.get(this.sSE1.size() - 1).size() - 1)
                                    .get(this.sSE1.get(this.sSE1.size() - 1).get(this.sSE1.get(this.sSE1.size() - 1).size() - 1).size() - 1)
                                    .get(this.sSE1.get(this.sSE1.size() - 1)
                                            .get(this.sSE1.get(this.sSE1.size() - 1).size() - 1)
                                            .get(this.sSE1.get(this.sSE1.size() - 1).get(this.sSE1.get(this.sSE1.size() - 1).size() - 1).size() - 1).size() - 1)
                                    .add(model.addVar(0, this.s_ub, constr.penalty * diff, GRB.BINARY,
                                            String.format("sSE1(%d,%d,%d,%d,%d)", c, i, j, k, k2)));
                        }
                        else {
                            this.sSE1.get(this.sSE1.size() - 1)
                                    .get(this.sSE1.get(this.sSE1.size() - 1).size() - 1)
                                    .get(this.sSE1.get(this.sSE1.size() - 1).get(this.sSE1.get(this.sSE1.size() - 1).size() - 1).size() - 1)
                                    .get(this.sSE1.get(this.sSE1.size() - 1)
                                            .get(this.sSE1.get(this.sSE1.size() - 1).size() - 1)
                                            .get(this.sSE1.get(this.sSE1.size() - 1).get(this.sSE1.get(this.sSE1.size() - 1).size() - 1).size() - 1).size() - 1)
                                    .add(null);
                        }
                    }
                }
            }
        }
    }

    private void createFA2Vars(int c, Constraint constr) throws GRBException {
        this.sFA2.add(new ArrayList<>());
        this.cIndexMap.put(c, this.sFA2.size() - 1);
        for (int i : constr.teams) {
            this.sFA2.get(this.sFA2.size() - 1).add(new ArrayList<>());
            for (int j : constr.teams) {
                this.sFA2.get(this.sFA2.size() - 1).get(this.sFA2.get(this.sFA2.size() - 1).size() - 1)
                        .add(model.addVar(0, this.s_ub, constr.penalty, GRB.INTEGER, String.format("sFA2(%d,%d,%d)", c, i, j)));
            }
        }
    }

    private void createFA2LineVars(int c, Constraint constr) throws GRBException {
        this.sFA2Line.add(new ArrayList<>());
        this.cIndexMap.put(c, this.sFA2Line.size() - 1);
        for (int i : constr.teams) {
            this.sFA2Line.get(this.sFA2Line.size() - 1).add(new ArrayList<>());
            for (int j : constr.teams) {
                this.sFA2Line.get(this.sFA2Line.size() - 1).get(this.sFA2Line.get(this.sFA2Line.size() - 1).size() - 1).add(new ArrayList<>());
                for (int k : constr.slots) {
                    this.sFA2Line.get(this.sFA2Line.size() - 1).get(this.sFA2Line.get(this.sFA2Line.size() - 1).size() - 1)
                            .get(this.sFA2Line.get(this.sFA2Line.size() - 1).get(this.sFA2Line.get(this.sFA2Line.size() - 1).size() - 1).size() - 1)
                            .add(model.addVar(0, this.s_ub, 0, GRB.INTEGER,
                                    String.format("sFA2Line(%d,%d,%d,%d)", c, i, j, k)));
                }
            }
        }
    }

    private void createBR2Vars(int c, Constraint constr) throws GRBException {
        this.sBR2.add(model.addVar(0, this.s_ub, constr.penalty, GRB.INTEGER,
                String.format("sBR2(%d)", c)));
        this.cIndexMap.put(c, this.sBR2.size() - 1);
    }

    private void createBR1Vars(int c, Constraint constr) throws GRBException {
        this.sBR1.add(new ArrayList<>());
        this.cIndexMap.put(c, this.sBR1.size() - 1);
        for (int i : constr.teams) {
            this.sBR1.get(this.sBR1.size() - 1).
                    add(model.addVar(0, this.s_ub, constr.penalty, GRB.INTEGER,
                            String.format("sBR1(%d,%d)", c, i)));
        }
    }

    private void createGA1Vars(int c, Constraint constr) throws GRBException {
        this.sGA1.add(model.addVar(0, this.s_ub, constr.penalty, GRB.INTEGER,
                        String.format("sGA1(%d)", c)));
        this.cIndexMap.put(c, this.sGA1.size() - 1);
    }

    private void createSCA4Vars(int c, Constraint constr) throws GRBException {
        this.sCA4.add(new ArrayList<>());
        this.cIndexMap.put(c, this.sCA4.size() - 1);
        if (constr.mode2.equals("GLOBAL")) {
            this.sCA4.get(this.sCA4.size() - 1).
                    add(model.addVar(0, this.s_ub, constr.penalty, GRB.INTEGER,
                            String.format("sCA4(%d,G)", c)));
        }
        else if (constr.mode2.equals("EVERY")) {
            for (int k : constr.slots) {
                this.sCA4.get(this.sCA4.size() - 1).
                        add(model.addVar(0, this.s_ub, constr.penalty, GRB.INTEGER,
                                String.format("sCA4(%d,%d)", c, k)));
            }
        }
    }

    private void createSCA3Vars(int c, Constraint constr) throws GRBException {
        this.sCA3.add(new ArrayList<>());
        this.cIndexMap.put(c, this.sCA3.size() - 1);
        for (int i : constr.teams1) {
            this.sCA3.get(this.sCA3.size() - 1).add(new ArrayList<>());
            for (int k = 0; k < inst.nSlots - constr.intp + 1; ++k) {
                this.sCA3.get(this.sCA3.size() - 1).get(this.sCA3.get(this.sCA3.size() - 1).size() - 1).
                        add(model.addVar(0, this.s_ub, constr.penalty, GRB.INTEGER,
                                String.format("sCA3(%d,%d,%d)", c, i, k)));
            }
        }
    }

    private void createSCA2Vars(int c, Constraint constr) throws GRBException {
        this.sCA2.add(new ArrayList<>());
        this.cIndexMap.put(c, this.sCA2.size() - 1);
        for (int i : constr.teams1) {
            this.sCA2.get(this.sCA2.size() - 1).
                    add(model.addVar(0, this.s_ub, constr.penalty, GRB.INTEGER,
                            String.format("sCA2(%d,%d)", c, i)));
        }
    }

    private void createSCA1Vars(int c, Constraint constr) throws GRBException {
        this.sCA1.add(new ArrayList<>());
        this.cIndexMap.put(c, this.sCA1.size() - 1);
        for (int i : constr.teams) {
            this.sCA1.get(this.sCA1.size() - 1).
                    add(model.addVar(0, this.s_ub, constr.penalty, GRB.INTEGER,
                    String.format("sCA1(%d,%d)", c, i)));
        }
    }

    private void createInstanceSpecificConstrs() throws GRBException {
        for (int c = 0; c < inst.nConstrs; ++c) {
            Constraint constr = inst.constrs[c];
            // skipping all SOFT constraint if in Phase 1 (in Phase 1 SOFT constraints are ignored)
            if (this.phase == 1 && constr.type.equals("SOFT"))
                continue;

            // setting s_ub to 0 if constraints is HARD in Phase 2 (in Phase 2 HARD constraints must be enforced)
            int s_ub = Integer.MAX_VALUE;
            if (this.phase == 2 && constr.type.equals("HARD"))
                s_ub = 0;

            if (constr.tag == Constraint.Tag.CA1 && !(this.phase == 1 && constr.type.equals("SOFT"))) {
                createConstrCA1(c, constr);
            }
            if (constr.tag == Constraint.Tag.CA2 && !(this.phase == 1 && constr.type.equals("SOFT"))) {
                createConstrCA2(c, constr);
            }
            if (constr.tag == Constraint.Tag.CA3 && !(this.phase == 1 && constr.type.equals("SOFT"))) {
                createConstrCA3(c, constr);
            }
            if (constr.tag == Constraint.Tag.CA4 && !(this.phase == 1 && constr.type.equals("SOFT"))) {
                createConstrCA4(c, constr);
            }
            if (constr.tag == Constraint.Tag.GA1 && !(this.phase == 1 && constr.type.equals("SOFT"))) {
                createConstrGA1(c, constr);
            }
            if (constr.tag == Constraint.Tag.BR1 && !(this.phase == 1 && constr.type.equals("SOFT"))) {
                createConstrBR1(c, constr);
            }
            if (constr.tag == Constraint.Tag.BR2 && !(this.phase == 1 && constr.type.equals("SOFT"))) {
                createConstrBR2(c, constr);
            }
            if (constr.tag == Constraint.Tag.FA2 && !(this.phase == 1 && constr.type.equals("SOFT"))) {
                createConstrFA2(c, constr);
            }
            if (constr.tag == Constraint.Tag.SE1 && !(this.phase == 1 && constr.type.equals("SOFT"))) {
                createConstrSE1(c, constr);
            }
        }
    }

    private void createConstrSE1(int c, Constraint constr) throws GRBException {
        for (int idxi = 0; idxi < constr.teams.length; ++idxi) {
            int i = constr.teams[idxi];
            for (int idxj = 0; idxj < constr.teams.length; ++idxj) {
                int j = constr.teams[idxj];
                if (i != j) {
                    for (int k = 0; k < inst.nSlots; ++k) {
                        for (int k2 = 0; k2 < inst.nSlots; ++k2) {
                            if (k > k2 && (constr.min - (k - k2 - 1)) > 0) {
                                GRBLinExpr lhs = new GRBLinExpr();
                                lhs.addTerm(1.0, x[i][j][k]);
                                lhs.addTerm(1.0, x[j][i][k2]);
                                GRBLinExpr rhs = new GRBLinExpr();
                                rhs.addConstant(1);
                                rhs.addTerm(1.0, sSE1.get(cIndexMap.get(c))
                                        .get(idxi)
                                        .get(idxj)
                                        .get(k)
                                        .get(k2));
                                model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("cSE1(%d,%d,%d,%d,%d)", c, i, j, k, k2));
                            }
                        }
                    }
                }
            }
        }
    }

    private void createConstrFA2(int c, Constraint constr) throws GRBException {
        if (constr.mode.equals("H")) {
            for (int idxi = 0; idxi < constr.teams.length; ++idxi) {
                int i = constr.teams[idxi];
                for (int idxj = 0; idxj < constr.teams.length; ++idxj) {
                    int j = constr.teams[idxj];
                    if (i != j) {
                        for (int idxk = 0; idxk < constr.slots.length; ++idxk) {
                            int k = constr.slots[idxk];
                            GRBLinExpr lhsLine = new GRBLinExpr();
                            for (int j2 = 0; j2 < inst.nTeams; ++j2) {
                                if (j2 != i) {
                                    for (int k2 = 0; k2 < inst.nSlots; ++k2) {
                                        if (k2 <= k) {
                                            lhsLine.addTerm(1.0, x[i][j2][k2]);
                                        }
                                    }
                                }
                            }
                            for (int i2 = 0; i2 < inst.nTeams; ++i2) {
                                if (i2 != j) {
                                    for (int k2 = 0; k2 < inst.nSlots; ++k2) {
                                        if (k2 <= k) {
                                            lhsLine.addTerm(-1.0, x[j][i2][k2]);
                                        }
                                    }
                                }
                            }
                            GRBLinExpr rhsLine = new GRBLinExpr();
                            rhsLine.addConstant(constr.intp);
                            rhsLine.addTerm(1.0, sFA2Line.get(cIndexMap.get(c)).get(idxi).get(idxj).get(idxk));
                            model.addConstr(lhsLine, GRB.LESS_EQUAL, rhsLine, String.format("cFA2Line(%d,%d,%d,%d)", c, i, j, k));

                            GRBLinExpr lhs = new GRBLinExpr();
                            lhs.addTerm(1.0, sFA2Line.get(cIndexMap.get(c)).get(idxi).get(idxj).get(idxk));
                            GRBLinExpr rhs = new GRBLinExpr();
                            rhs.addTerm(1.0, sFA2.get(cIndexMap.get(c)).get(idxi).get(idxj));
                            model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("cFA2(%d,%d,%d,%d)", c, i, j, k));
                        }
                    }
                }
            }
        }
    }

    private void createConstrBR2(int c, Constraint constr) throws GRBException {
        GRBLinExpr lhs = new GRBLinExpr();
        for (int idxi = 0; idxi < constr.teams.length; ++idxi) {
            int i = constr.teams[idxi];
            for (int idxk = 0; idxk < constr.slots.length; ++idxk) {
                int k = constr.slots[idxk];
                lhs.addTerm(1.0, bH[i][k]);
                lhs.addTerm(1.0, bA[i][k]);
            }
        }
        GRBLinExpr rhs = new GRBLinExpr();
        rhs.addConstant(constr.intp);
        rhs.addTerm(1.0, sBR2.get(cIndexMap.get(c)));
        model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("cBR2(%d)", c));
    }

    private void createConstrBR1(int c, Constraint constr) throws GRBException {
        for (int idxi = 0; idxi < constr.teams.length; ++idxi) {
            int i = constr.teams[idxi];
            if (constr.mode2.equals("H")) {
                GRBLinExpr lhs = new GRBLinExpr();
                for (int idxk = 0; idxk < constr.slots.length; ++idxk) {
                    int k = constr.slots[idxk];
                    lhs.addTerm(1.0, bH[i][k]);
                }
                GRBLinExpr rhs = new GRBLinExpr();
                rhs.addConstant(constr.intp);
                rhs.addTerm(1.0, sBR1.get(cIndexMap.get(c)).get(idxi));
                model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("cBR1(%d,%d)", c, i));
            }
            else if (constr.mode2.equals("A")) {
                GRBLinExpr lhs = new GRBLinExpr();
                for (int idxk = 0; idxk < constr.slots.length; ++idxk) {
                    int k = constr.slots[idxk];
                    lhs.addTerm(1.0, bA[i][k]);
                }
                GRBLinExpr rhs = new GRBLinExpr();
                rhs.addConstant(constr.intp);
                rhs.addTerm(1.0, sBR1.get(cIndexMap.get(c)).get(idxi));
                model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("cBR1(%d,%d)", c, i));
            }
            else if (constr.mode2.equals("HA")) {
                GRBLinExpr lhs = new GRBLinExpr();
                for (int idxk = 0; idxk < constr.slots.length; ++idxk) {
                    int k = constr.slots[idxk];
                    lhs.addTerm(1.0, bH[i][k]);
                    lhs.addTerm(1.0, bA[i][k]);
                }
                GRBLinExpr rhs = new GRBLinExpr();
                rhs.addConstant(constr.intp);
                rhs.addTerm(1.0, sBR1.get(cIndexMap.get(c)).get(idxi));
                model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("cBR1(%d,%d)", c, i));
            }
        }
    }

    private void createConstrGA1(int c, Constraint constr) throws GRBException {
        if (constr.max < constr.meetings.length) {
            GRBLinExpr lhs = new GRBLinExpr();
            for (Game game : constr.meetings) {
                int i = game.home;
                int j = game.away;
                for (int k : constr.slots) {
                    lhs.addTerm(1.0, x[i][j][k]);
                }
            }
            GRBLinExpr rhs = new GRBLinExpr();
            rhs.addConstant(constr.max);
            rhs.addTerm(1.0, sGA1.get(cIndexMap.get(c)));
            model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("cGA1(%d)", c));
        }
        if (constr.min > 0) {
            GRBLinExpr lhs = new GRBLinExpr();
            for (Game game : constr.meetings) {
                int i = game.home;
                int j = game.away;
                for (int k : constr.slots) {
                    lhs.addTerm(1.0, x[i][j][k]);
                }
            }
            GRBLinExpr rhs = new GRBLinExpr();
            rhs.addConstant(constr.min);
            rhs.addTerm(-1.0, sGA1.get(cIndexMap.get(c)));
            model.addConstr(lhs, GRB.GREATER_EQUAL, rhs, String.format("cGA1(%d)", c));
        }
    }

    private void createConstrCA4(int c, Constraint constr) throws GRBException {
        if (constr.mode2.equals("GLOBAL")) {
            if (constr.mode1.equals("H")) {
                GRBLinExpr lhs = new GRBLinExpr();
                for (int idxi = 0; idxi < constr.teams1.length; ++idxi) {
                    int i = constr.teams1[idxi];
                    for (int idxj = 0; idxj < constr.teams2.length; ++idxj) {
                        int j = constr.teams2[idxj];
                        if (i != j) {
                            for (int idxk = 0; idxk < constr.slots.length; ++idxk) {
                                int k = constr.slots[idxk];
                                lhs.addTerm(1.0, x[i][j][k]);
                            }
                        }
                    }
                }
                GRBLinExpr rhs = new GRBLinExpr();
                rhs.addConstant(constr.max);
                rhs.addTerm(1.0, sCA4.get(cIndexMap.get(c)).get(0));
                model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("cCA4(%d)", c));
            }
            else if (constr.mode1.equals("A")) {
                GRBLinExpr lhs = new GRBLinExpr();
                for (int idxi = 0; idxi < constr.teams1.length; ++idxi) {
                    int i = constr.teams1[idxi];
                    for (int idxj = 0; idxj < constr.teams2.length; ++idxj) {
                        int j = constr.teams2[idxj];
                        if (i != j) {
                            for (int idxk = 0; idxk < constr.slots.length; ++idxk) {
                                int k = constr.slots[idxk];
                                lhs.addTerm(1.0, x[j][i][k]);
                            }
                        }
                    }
                }
                GRBLinExpr rhs = new GRBLinExpr();
                rhs.addConstant(constr.max);
                rhs.addTerm(1.0, sCA4.get(cIndexMap.get(c)).get(0));
                model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("cCA4(%d)", c));
            }
            else if (constr.mode1.equals("HA")) {
                GRBLinExpr lhs = new GRBLinExpr();
                for (int idxi = 0; idxi < constr.teams1.length; ++idxi) {
                    int i = constr.teams1[idxi];
                    for (int idxj = 0; idxj < constr.teams2.length; ++idxj) {
                        int j = constr.teams2[idxj];
                        if (i != j) {
                            for (int idxk = 0; idxk < constr.slots.length; ++idxk) {
                                int k = constr.slots[idxk];
                                lhs.addTerm(1.0, x[i][j][k]);
                                lhs.addTerm(1.0, x[j][i][k]);
                            }
                        }
                    }
                }
                GRBLinExpr rhs = new GRBLinExpr();
                rhs.addConstant(constr.max);
                rhs.addTerm(1.0, sCA4.get(cIndexMap.get(c)).get(0));
                model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("cCA4(%d)", c));
            }
        }
        if (constr.mode2.equals("EVERY")) {
            for (int idxk = 0; idxk < constr.slots.length; ++idxk) {
                int k = constr.slots[idxk];
                if (constr.mode1.equals("H")) {
                    GRBLinExpr lhs = new GRBLinExpr();
                    for (int idxi = 0; idxi < constr.teams1.length; ++idxi) {
                        int i = constr.teams1[idxi];
                        for (int idxj = 0; idxj < constr.teams2.length; ++idxj) {
                            int j = constr.teams2[idxj];
                            if (i != j) {
                                lhs.addTerm(1.0, x[i][j][k]);
                            }
                        }
                    }
                    GRBLinExpr rhs = new GRBLinExpr();
                    rhs.addConstant(constr.max);
                    rhs.addTerm(1.0, sCA4.get(cIndexMap.get(c)).get(idxk));
                    model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("cCA4(%d,%d)", c, k));
                }
                else if (constr.mode1.equals("A")) {
                    GRBLinExpr lhs = new GRBLinExpr();
                    for (int idxi = 0; idxi < constr.teams1.length; ++idxi) {
                        int i = constr.teams1[idxi];
                        for (int idxj = 0; idxj < constr.teams2.length; ++idxj) {
                            int j = constr.teams2[idxj];
                            if (i != j) {
                                lhs.addTerm(1.0, x[j][i][k]);
                            }
                        }
                    }
                    GRBLinExpr rhs = new GRBLinExpr();
                    rhs.addConstant(constr.max);
                    rhs.addTerm(1.0, sCA4.get(cIndexMap.get(c)).get(idxk));
                    model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("cCA4(%d,%d)", c, k));
                }
                else if (constr.mode1.equals("HA")) {
                    GRBLinExpr lhs = new GRBLinExpr();
                    for (int idxi = 0; idxi < constr.teams1.length; ++idxi) {
                        int i = constr.teams1[idxi];
                        for (int idxj = 0; idxj < constr.teams2.length; ++idxj) {
                            int j = constr.teams2[idxj];
                            if (i != j) {
                                lhs.addTerm(1.0, x[i][j][k]);
                                lhs.addTerm(1.0, x[j][i][k]);
                            }
                        }
                    }
                    GRBLinExpr rhs = new GRBLinExpr();
                    rhs.addConstant(constr.max);
                    rhs.addTerm(1.0, sCA4.get(cIndexMap.get(c)).get(idxk));
                    model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("cCA4(%d,%d)", c, k));
                }
            }
        }
    }

    private void createConstrComputeBreaks() throws GRBException {
        for (int i = 0; i < inst.nTeams; ++i) {
            for (int k = 0; k < inst.nSlots - 1; ++k) {
                int k2 = k + 1;
                GRBLinExpr lhs1 = new GRBLinExpr();
                GRBLinExpr lhs2 = new GRBLinExpr();
                for (int j = 0; j < inst.nTeams; ++j) {
                    if (i != j) {
                        lhs1.addTerm(1.0, x[i][j][k]);
                        lhs1.addTerm(1.0, x[i][j][k2]);
                        lhs2.addTerm(1.0, x[j][i][k]);
                        lhs2.addTerm(1.0, x[j][i][k2]);
                    }
                }
                GRBLinExpr rhs1 = new GRBLinExpr();
                rhs1.addConstant(1);
                rhs1.addTerm(1.0, bH[i][k2]);
                GRBLinExpr rhs2 = new GRBLinExpr();
                rhs2.addConstant(1);
                rhs2.addTerm(1.0, bA[i][k2]);
                model.addConstr(lhs1, GRB.LESS_EQUAL, rhs1, String.format("c11(%d,%d)", i, k));
                model.addConstr(lhs2, GRB.LESS_EQUAL, rhs2, String.format("c12(%d,%d)", i, k));
            }
        }
    }

    private void createConstr2() throws GRBException {
        for (Game game : inst.games) {
            GRBLinExpr lhs = new GRBLinExpr();
            for (int k = 0; k < inst.nSlots; ++k) {
                lhs.addTerm(1.0, x[game.home][game.away][k]);
            }
            model.addConstr(lhs, GRB.EQUAL, 1, String.format("c2(%d,%d)", game.home, game.away));
        }
    }

    private void createConstrCA3(int c, Constraint constr) throws GRBException {
        for (int idx = 0; idx < constr.teams1.length; ++idx) {
            int i = constr.teams1[idx];
            for (int k = 0; k < inst.nSlots - constr.intp + 1; ++k) {
                if (constr.mode1.equals("H")) {
                    GRBLinExpr lhs = new GRBLinExpr();
                    for (int j : constr.teams2) {
                        if (i != j) {
                            for (int l = 0; l < constr.intp; ++l) {
                                lhs.addTerm(1.0, x[i][j][k + l]);
                            }
                        }
                    }
                    GRBLinExpr rhs = new GRBLinExpr();
                    rhs.addConstant(constr.max);
                    rhs.addTerm(1.0, sCA3.get(cIndexMap.get(c)).get(idx).get(k));
                    model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("cCA3(%d,%d,%d)", c, i, k));
                }
                else if (constr.mode1.equals("A")) {
                    GRBLinExpr lhs = new GRBLinExpr();
                    for (int j : constr.teams2) {
                        if (i != j) {
                            for (int l = 0; l < constr.intp; ++l) {
                                lhs.addTerm(1.0, x[j][i][k + l]);
                            }
                        }
                    }
                    GRBLinExpr rhs = new GRBLinExpr();
                    rhs.addConstant(constr.max);
                    rhs.addTerm(1.0, sCA3.get(cIndexMap.get(c)).get(idx).get(k));
                    model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("cCA3(%d,%d,%d)", c, i, k));
                }
                else if (constr.mode1.equals("HA")) {
                    GRBLinExpr lhs = new GRBLinExpr();
                    for (int j : constr.teams2) {
                        if (i != j) {
                            for (int l = 0; l < constr.intp; ++l) {
                                lhs.addTerm(1.0, x[i][j][k + l]);
                                lhs.addTerm(1.0, x[j][i][k + l]);
                            }
                        }
                    }
                    GRBLinExpr rhs = new GRBLinExpr();
                    rhs.addConstant(constr.max);
                    rhs.addTerm(1.0, sCA3.get(cIndexMap.get(c)).get(idx).get(k));
                    model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("cCA3(%d,%d,%d)", c, i, k));
                }
            }
        }
    }

    private void createConstrCA2(int c, Constraint constr) throws GRBException {
        for (int idx = 0; idx < constr.teams1.length; ++idx) {
            int i = constr.teams1[idx];
            if (constr.mode1.equals("H")) {
                GRBLinExpr lhs = new GRBLinExpr();
                for (int j : constr.teams2) {
                    if (i != j) {
                        for (int k : constr.slots) {
                            lhs.addTerm(1.0, x[i][j][k]);
                        }
                    }
                }
                GRBLinExpr rhs = new GRBLinExpr();
                rhs.addConstant(constr.max);
                rhs.addTerm(1.0, sCA2.get(cIndexMap.get(c)).get(idx));
                model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("cCA2(%d,%d)", c, i));
            }
            else if (constr.mode1.equals("A")) {
                GRBLinExpr lhs = new GRBLinExpr();
                for (int j : constr.teams2) {
                    if (i != j) {
                        for (int k : constr.slots) {
                            lhs.addTerm(1.0, x[j][i][k]);
                        }
                    }
                }
                GRBLinExpr rhs = new GRBLinExpr();
                rhs.addConstant(constr.max);
                rhs.addTerm(1.0, sCA2.get(cIndexMap.get(c)).get(idx));
                model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("cCA2(%d,%d)", c, i));
            }
            else if (constr.mode1.equals("HA")) {
                GRBLinExpr lhs = new GRBLinExpr();
                for (int j : constr.teams2) {
                    if (i != j) {
                        for (int k : constr.slots) {
                            lhs.addTerm(1.0, x[i][j][k]);
                            lhs.addTerm(1.0, x[j][i][k]);
                        }
                    }
                }
                GRBLinExpr rhs = new GRBLinExpr();
                rhs.addConstant(constr.max);
                rhs.addTerm(1.0, sCA2.get(cIndexMap.get(c)).get(idx));
                model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("cCA2(%d,%d)", c, i));
            }
        }
    }

    private void createConstrCA1(int c, Constraint constr) throws GRBException {
        for (int idx = 0; idx < constr.teams.length; ++idx) {
            int i = constr.teams[idx];
            if (constr.mode.equals("H")) {
                GRBLinExpr lhs = new GRBLinExpr();
                for (int j = 0; j < inst.nTeams; ++j) {
                    if (i != j) {
                        for (int k : constr.slots) {
                            lhs.addTerm(1.0, x[i][j][k]);
                        }
                    }
                }
                GRBLinExpr rhs = new GRBLinExpr();
                rhs.addConstant(constr.max);
                rhs.addTerm(1.0, sCA1.get(cIndexMap.get(c)).get(idx));
                model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("cCA1(%d,%d)", c, i));
            } else if (constr.mode.equals("A")) {
                GRBLinExpr lhs = new GRBLinExpr();
                for (int j = 0; j < inst.nTeams; ++j) {
                    if (i != j) {
                        for (int k : constr.slots) {
                            lhs.addTerm(1.0, x[j][i][k]);
                        }
                    }
                }
                GRBLinExpr rhs = new GRBLinExpr();
                rhs.addConstant(constr.max);
                rhs.addTerm(1.0, sCA1.get(cIndexMap.get(c)).get(idx));
                model.addConstr(lhs, GRB.LESS_EQUAL, rhs, String.format("cCA1(%d,%d)", c, i));
            }
        }
    }

    private void createConstr1() throws GRBException {
        for (int i = 0; i < inst.nTeams; ++i) {
            for (int k = 0; k < inst.nSlots; ++k) {
                GRBLinExpr lhs = new GRBLinExpr();
                for (int j = 0; j < inst.nTeams; ++j) {
                    if (i != j) {
                        lhs.addTerm(1.0, x[i][j][k]);
                        lhs.addTerm(1.0, x[j][i][k]);
                    }
                }
                model.addConstr(lhs, GRB.EQUAL, 1, String.format("c1(%d,%d)", i, k));
            }
        }
    }

    private void createConstrPhased() throws GRBException {
        if (inst.phased) {
            for (int k = 0; k < inst.nSlots / 2; ++k) {
                for (Game game : inst.games) {
                    int i = game.home;
                    int j = game.away;
                    GRBLinExpr lhs = new GRBLinExpr();
                    lhs.addTerm(1.0, x[i][j][k]);
                    for (int k2 = 0; k2 < inst.nSlots / 2; ++k2) {
                        if (k != k2) {
                            lhs.addTerm(1.0, x[j][i][k2]);
                        }
                    }
                    model.addConstr(lhs, GRB.LESS_EQUAL, 1, String.format("c3(%d,%d,%d)", k, i, j));
                }
            }
        }
    }

    private void initializeSlackVarsArrays() {
        this.cIndexMap = new HashMap<Integer, Integer>();
        this.sCA1 = new ArrayList<>();
        this.sCA2 = new ArrayList<>();
        this.sCA3 = new ArrayList<>();
        this.sCA4 = new ArrayList<>();
        this.sGA1 = new ArrayList<>();
        this.sBR1 = new ArrayList<>();
        this.sBR2 = new ArrayList<>();
        this.sFA2Line = new ArrayList<>();
        this.sFA2 = new ArrayList<>();
        this.sSE1 = new ArrayList<>();
    }

    private void createBVars() throws GRBException {
        this.bH = new GRBVar[inst.nTeams][inst.nSlots];
        this.bA = new GRBVar[inst.nTeams][inst.nSlots];
        for (Team team : inst.teams) {
            for (int s = 0; s < inst.nSlots; ++s) {
                bH[team.id][s] = model.addVar(0, 1, 0, GRB.BINARY,
                        String.format("bH(%d,%d)", team.id, s));
                bA[team.id][s] = model.addVar(0, 1, 0, GRB.BINARY,
                        String.format("bA(%d,%d)", team.id, s));
            }
        }
    }

    private void createXVars() throws GRBException {
        this.x = new GRBVar[inst.nTeams][inst.nTeams][inst.nSlots];
        for (Game game : inst.games) {
            for (int s = 0; s < inst.nSlots; ++s) {
                x[game.home][game.away][s] = model.addVar(0, 1, 0, GRB.BINARY,
                        String.format("x(%d,%d,%d)", game.home, game.away, s));
            }
        }
    }
}
