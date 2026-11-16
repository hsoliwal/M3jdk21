/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary startsWith and endsWith compare a flat receiver with an M3-backed prefix by content
 * @modules java.base/java.lang:+open
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringMixedStartsWithTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringMixedStartsWithTest
 */

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * A range of a UTF-16 coded owner can hold only Latin-1 units and still report the UTF-16 coder.
 * A flat Latin-1 receiver must still match such a prefix, and must not flatten it. Every result
 * is compared with an oracle over the char arrays the Strings were built from.
 */
public class M3StringMixedStartsWithTest {
    private static final char EURO = (char) 0x20AC;
    private static final String BODY = "uro-zone-x";
    private static final Field STRING_M3 = optionalField("java.lang.String", "m3");
    private static int mixedPairs;
    private static int admittedPrefixes;
    private static int checks;

    /** A String and the UTF-16 units it must hold; the units are the oracle input. */
    private record Text(String value, char[] units) {
        static Text admitted(char[] units) {
            return new Text(new String(units), units);
        }

        Text slice(int begin, int end) {
            return new Text(value.substring(begin, end), Arrays.copyOfRange(units, begin, end));
        }
    }

    public static void main(String[] args) throws Exception {
        char[] wideUnits = (EURO + BODY).toCharArray();
        Text wide = Text.admitted(wideUnits);
        Text latin = Text.admitted(BODY.toCharArray());
        List<Text> prefixes = List.of(
                wide.slice(1, 9), wide.slice(1, 11), wide.slice(0, 4), wide.slice(5, 11), wide,
                wide.slice(3, 3), latin.slice(0, 3), latin.slice(4, 10), latin);
        List<Text> receivers = List.of(
                flat(BODY.toCharArray()), flat(wideUnits), flat("uro".toCharArray()),
                flat(new char[0]), flat(("x" + BODY).toCharArray()), latin, wide);
        admittedPrefixes = (int) prefixes.stream().filter(text -> isM3(text.value())).count();
        receivers.forEach(receiver -> prefixes.forEach(prefix -> checkPair(receiver, prefix)));
        if (admittedPrefixes > 0 && mixedPairs == 0) {
            throw new AssertionError("no flat receiver met an M3-backed prefix");
        }
        System.out.println("PASS checks=" + checks + "|mixedPairs=" + mixedPairs
                + "|admittedPrefixes=" + admittedPrefixes);
    }

    private static void checkPair(Text receiver, Text prefix) {
        if (isM3(prefix.value()) && !isM3(receiver.value())) mixedPairs++;
        char[] r = receiver.units();
        char[] p = prefix.units();
        IntStream.rangeClosed(-1, r.length + 1).forEach(offset -> same(
                receiver.value().startsWith(prefix.value(), offset), oracle(r, p, offset),
                "startsWith offset " + offset, receiver, prefix));
        same(receiver.value().startsWith(prefix.value()), oracle(r, p, 0),
                "startsWith", receiver, prefix);
        same(receiver.value().endsWith(prefix.value()), oracle(r, p, r.length - p.length),
                "endsWith", receiver, prefix);
    }

    private static boolean oracle(char[] receiver, char[] prefix, int offset) {
        return offset >= 0
                && offset <= receiver.length - prefix.length
                && IntStream.range(0, prefix.length)
                        .allMatch(index -> receiver[offset + index] == prefix[index]);
    }

    private static void same(boolean actual, boolean expected, String label, Text receiver,
            Text prefix) {
        checks++;
        if (actual != expected) {
            throw new AssertionError(label + ": expected " + expected + " for receiver "
                    + printable(receiver.units()) + " and prefix " + printable(prefix.units()));
        }
    }

    private static boolean isM3(String value) {
        try {
            return STRING_M3 != null && STRING_M3.get(value) != null;
        } catch (IllegalAccessException impossible) {
            throw new AssertionError(impossible);
        }
    }

    /** The per-thread admission guard gives a flat peer, as a bootstrap or refused String is. */
    @SuppressWarnings("unchecked")
    private static Text flat(char[] units) throws Exception {
        Field field = optionalField("java.lang.M3String", "ADMITTING");
        if (field == null) return new Text(new String(units), units);
        ThreadLocal<Boolean> admitting = (ThreadLocal<Boolean>) field.get(null);
        Boolean previous = admitting.get();
        admitting.set(Boolean.TRUE);
        try {
            return new Text(new String(units), units);
        } finally {
            if (previous == null) admitting.remove();
            else admitting.set(previous);
        }
    }

    /** Null on a JDK without the M3 String owners, so the oracle itself can run anywhere. */
    private static Field optionalField(String owner, String name) {
        try {
            Field field = Class.forName(owner).getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ClassNotFoundException | NoSuchFieldException absent) {
            return null;
        }
    }

    private static String printable(char[] units) {
        StringBuilder out = new StringBuilder("[");
        IntStream.range(0, units.length)
                .forEach(index -> out.append(index == 0 ? "" : " ")
                        .append(Integer.toHexString(units[index])));
        return out.append(']').toString();
    }
}
