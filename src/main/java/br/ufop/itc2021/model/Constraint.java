package br.ufop.itc2021.model;

import org.dom4j.*;

import java.util.*;

/**
 * This class represents an ITC2021 problem instance.
 *
 * @author George Fonseca and Tulio Toffolo
 */
public class Constraint {

    public final Instance inst;
    public final Element elem;

    public final boolean hard;
    public final int penalty;
    public final String type;
    public final Tag tag;

    // data present on 'some' or 'most' constraints
    public final int intp;
    public final int min, max;
    public final String homeMode, mode, mode1, mode2;
    public final Game[] meetings;
    public final int[] slots;
    public final int[] teams, teams1, teams2;

    public Constraint(Instance inst, Node xmlNode) {
        this.inst = inst;
        this.elem = ( Element ) xmlNode;

        this.penalty = hasAttribute("penalty") ? Integer.parseInt(elem.attributeValue("penalty")) : 0;
        this.type = hasAttribute("type") ? elem.attributeValue("type") : "";
        this.hard = this.type.toUpperCase().equals("HARD");

        // setting constraint' tag (category)
        switch (elem.getName()) {
            case "CA1" -> this.tag = Tag.CA1;
            case "CA2" -> this.tag = Tag.CA2;
            case "CA3" -> this.tag = Tag.CA3;
            case "CA4" -> this.tag = Tag.CA4;
            case "GA1" -> this.tag = Tag.GA1;
            case "BR1" -> this.tag = Tag.BR1;
            case "BR2" -> this.tag = Tag.BR2;
            case "FA2" -> this.tag = Tag.FA2;
            case "SE1" -> this.tag = Tag.SE1;

            default -> {
                this.tag = null;
                throw new IllegalStateException("Invalid ITC2021 constraint.");
            }
        }

        this.intp = hasAttribute("intp") ? Integer.parseInt(elem.attributeValue("intp")) : -1;
        this.min = hasAttribute("min") ? Integer.parseInt(elem.attributeValue("min")) : -1;
        this.max = hasAttribute("max") ? Integer.parseInt(elem.attributeValue("max")) : -1;

        this.homeMode = hasAttribute("homeMode") ? elem.attributeValue("homeMode") : "";
        this.mode = hasAttribute("mode") ? elem.attributeValue("mode") : "";
        this.mode1 = hasAttribute("mode1") ? elem.attributeValue("mode1") : "";
        this.mode2 = hasAttribute("mode2") ? elem.attributeValue("mode2") : "";

        this.meetings = hasAttribute("meetings")
          ? Arrays.stream(elem.attributeValue("meetings").split(";"))
          .map(it -> getGame(it)).toArray(Game[]::new) : new Game[0];

        this.slots = hasAttribute("slots")
          ? Arrays.stream(elem.attributeValue("slots").split(";"))
          .mapToInt(Integer::parseInt).toArray() : new int[0];
        this.teams = hasAttribute("teams")
          ? Arrays.stream(elem.attributeValue("teams").split(";"))
          .mapToInt(Integer::parseInt).toArray() : new int[0];
        this.teams1 = hasAttribute("teams1")
          ? Arrays.stream(elem.attributeValue("teams1").split(";"))
          .mapToInt(Integer::parseInt).toArray() : new int[0];
        this.teams2 = hasAttribute("teams2")
          ? Arrays.stream(elem.attributeValue("teams2").split(";"))
          .mapToInt(Integer::parseInt).toArray() : new int[0];

        // sorting all arrays (for faster search)
        Arrays.sort(meetings);
        Arrays.sort(slots);
        Arrays.sort(teams);
        Arrays.sort(teams1);
        Arrays.sort(teams2);
    }

    private Game getGame(String gameStr) {
        String[] teams = gameStr.split(",");
        return inst.getGame(Integer.parseInt(teams[0]), Integer.parseInt(teams[1]));
    }

    private boolean hasAttribute(String attr) {
        String val = elem.attributeValue(attr);
        return val != null && !val.isBlank() && !val.isEmpty();
    }

    public static enum Tag {
        CA1, CA2, CA3, CA4, GA1, BR1, BR2, FA2, SE1
    }
}
