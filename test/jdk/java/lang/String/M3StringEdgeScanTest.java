/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary strip, trim and isBlank of M3-backed Strings match a flat peer with and without facts
 * @modules java.base/java.lang:+open
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringEdgeScanTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringEdgeScanTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * The white-space edges of an M3-backed String come from a bounded edge scan when no facts are
 * prepared, and from the facts otherwise. Both must give the results of a flat String. Every
 * text is checked as a whole admitted String and as a range of a longer one, first without and
 * then with prepared facts. Edge runs of 63, 64 and 65 units cross the scan bound.
 */
public class M3StringEdgeScanTest {
    private static final char[] UNITS = {
        'a', ' ', '\t', (char) 0x1C, (char) 0xA0, (char) 0x2028, (char) 0x3000, (char) 0xD800,
        (char) 0xDC00
    };
    private static final int EXHAUSTIVE_LENGTH = 4;
    private static final List<Integer> RUNS = List.of(0, 1, 63, 64, 65, 130);
    private static final Field STRING_M3 = optionalField("java.lang.String", "m3");
    private static final List<Function<String, Object>> OPERATIONS = List.of(
            String::strip, String::stripLeading, String::stripTrailing, String::trim,
            String::isBlank);
    private static int checks;
    private static int prepared;

    public static void main(String[] args) throws Exception {
        List<char[]> texts = Stream.concat(exhaustive(), longRuns()).toList();
        for (char[] units : texts) {
            String flat = flat(units);
            checkAll(new String(units), flat);
            checkAll(range(units), flat);
        }
        System.out.println("PASS texts=" + texts.size() + "|checks=" + checks
                + "|preparedFacts=" + prepared);
    }

    private static Stream<char[]> exhaustive() {
        return IntStream.rangeClosed(0, EXHAUSTIVE_LENGTH).boxed()
                .flatMap(length -> IntStream.range(0, (int) Math.pow(UNITS.length, length))
                        .mapToObj(code -> digits(code, length)));
    }

    private static char[] digits(int code, int length) {
        char[] units = new char[length];
        int[] rest = {code};
        IntStream.range(0, length).forEach(index -> {
            units[index] = UNITS[rest[0] % UNITS.length];
            rest[0] /= UNITS.length;
        });
        return units;
    }

    /** Blank runs on both sides of one letter, and all-blank texts, around the scan bound. */
    private static Stream<char[]> longRuns() {
        Stream<char[]> framed = RUNS.stream().flatMap(left -> RUNS.stream()
                .map(right -> (" ".repeat(left) + "a" + "\t".repeat(right)).toCharArray()));
        return Stream.concat(framed, RUNS.stream().map(run -> " ".repeat(run).toCharArray()));
    }

    /** The same text as a range of a longer admitted String. */
    private static String range(char[] units) {
        String framed = new String(("x" + new String(units) + "y").toCharArray());
        return framed.substring(1, 1 + units.length);
    }

    private static void checkAll(String subject, String flat) throws Exception {
        OPERATIONS.forEach(operation -> same(operation.apply(subject), operation.apply(flat)));
        if (prepareFacts(subject)) {
            prepared++;
            OPERATIONS.forEach(operation -> same(operation.apply(subject), operation.apply(flat)));
        }
        boolean unchanged = !flat.isEmpty() && flat.strip().length() == flat.length();
        if (unchanged && (subject.strip() != subject || subject.trim() != subject)) {
            throw new AssertionError("strip and trim of a text without blank edges return this");
        }
    }

    private static void same(Object actual, Object expected) {
        checks++;
        boolean equal = actual instanceof String text
                ? Arrays.equals(units(text), units((String) expected))
                : actual.equals(expected);
        if (!equal) {
            throw new AssertionError("expected " + show(expected) + " but was " + show(actual));
        }
    }

    private static char[] units(String text) {
        char[] units = new char[text.length()];
        IntStream.range(0, units.length).forEach(index -> units[index] = text.charAt(index));
        return units;
    }

    private static String show(Object value) {
        return value instanceof String text ? Arrays.toString(text.chars().toArray())
                : String.valueOf(value);
    }

    /** Prepares the facts of an M3-backed String; false when the String has no M3 owner. */
    private static boolean prepareFacts(String value) throws Exception {
        Object storage = STRING_M3 == null ? null : STRING_M3.get(value);
        if (storage == null) return false;
        Method facts = storage.getClass().getDeclaredMethod("facts");
        facts.setAccessible(true);
        facts.invoke(storage);
        return true;
    }

    /** The per-thread admission guard gives a flat peer, as a bootstrap or refused String is. */
    @SuppressWarnings("unchecked")
    private static String flat(char[] units) throws Exception {
        Field field = optionalField("java.lang.M3String", "ADMITTING");
        if (field == null) return new String(units);
        ThreadLocal<Boolean> admitting = (ThreadLocal<Boolean>) field.get(null);
        Boolean previous = admitting.get();
        admitting.set(Boolean.TRUE);
        try {
            return new String(units);
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
}
