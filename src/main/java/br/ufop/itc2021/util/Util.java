package br.ufop.itc2021.util;

import br.ufop.itc2021.*;
import br.ufop.itc2021.algorithm.neighborhood.*;
import br.ufop.itc2021.model.*;

import java.io.*;
import java.util.*;

/**
 * Simple class with some util methods.
 *
 * @author Tulio Toffolo
 */
public class Util {

    public final static double EPS = 1e-6;

    public static <T> void swapWithLastAndRemove(List<T> list, int index) {
        list.set(index, list.get(list.size() - 1));
        list.remove(list.size() - 1);
    }

    public static <T> void swap(List<T> list, int i, int j) {
        T temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }

    public static <T> void swap(T[] array, int i, int j) {
        T temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    /**
     * Convert a Long number to a readable string.
     *
     * @param value long number.
     * @return the "human-friendly" (readable) number as a String.
     */
    public static String longToString(long value) {
        return value >= 1e11 ? String.format("%.0fG", value / 1e9)
          : value >= 1e7 ? String.format("%.0fM", value / 1e6)
          : (value >= 1e4) ? String.format("%.0fK", value / 1e3)
          : Long.toString(value);
    }

    /**
     * Calls printf after checking if the PrintStream is not null.
     *
     * @param output the output stream.
     * @param format the String with the format.
     * @param args   "printf" arguments.
     */
    public static void safePrintf(PrintStream output, String format, Object... args) {
        if (output != null) {
            output.printf(format, args);
        }
    }

    /**
     * Prints the statistics of a Move using the table stable, after checking that the PrintStream is not null.
     *
     * @param output  the output stream.
     * @param move    the Move considered.
     * @param special some informative String to print after the row.
     */
    public static void safePrintMoveStatistics(PrintStream output, Move move, String special) {
        if (output != null) {
            output.printf("    | %-18s | %8s | %8s | %8s | %8s |\n",
              move.name,
              longToString(move.getNImprovements()),
              longToString(move.getNSideways()),
              longToString(move.getNAccepts()),
              longToString(move.getNRejects())
            );
        }
    }


    /**
     * Prints the current solution status after checking that the PrintStream is not null.
     *
     * @param output       the output stream.
     * @param nIters       the current iteration number.
     * @param bestSolution the best solution object.
     * @param solution     the current solution object.
     * @param special      some informative String to print after the row.
     */
    public static void safePrintStatus(PrintStream output, long nIters, Solution bestSolution, Solution solution, String special) {
        if (output != null) {
            output.printf("    | %8s | %8s | %10s | %10s | %10.2f | %s\n",
              longToString(nIters),
              Main.bestKnown == Integer.MAX_VALUE ? "-" : String.format("%8.2f",
                100. * ( double ) (solution.getCost() - Main.bestKnown) / ( double ) solution.getCost()),
              bestSolution.getCostStr(), solution.getCostStr(),
              (System.currentTimeMillis() - Main.startTimeMillis) / 1000.0, special
            );
        }
    }

    /**
     * Prints the text maintaining the table style,after checking that the PrintStream is not null.
     *
     * @param output  the output stream.
     * @param text    text to print inside the table.
     * @param special some informative String to print after the row.
     */
    public static void safePrintText(PrintStream output, String text, String special) {
        if (output != null) {
            output.printf("    | %-45s | %10.2f | %s\n", text, (System.currentTimeMillis() - Main.startTimeMillis) / 1000.0, special);
        }
    }
}
