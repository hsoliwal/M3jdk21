/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary String.countMatches counts char values and surrogate pairs in every storage form
 * @modules java.base/java.lang:+open
 * @run main CountMatches
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage CountMatches
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      -XX:-CompactStrings CountMatches
 */

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * countMatches(int) against a count over the char array the String was built from: Latin-1 and
 * UTF-16 text, a char value, a surrogate value, a supplementary code point, a value that the
 * text cannot hold and an invalid one. Each text is checked as a whole String, as a range of a
 * longer one, as a concatenation of two halves and as a flat peer.
 */
public class CountMatches {
    private static final char HIGH = (char) 0xD83D;
    private static final char LOW = (char) 0xDE42;
    private static final int PAIR = Character.toCodePoint(HIGH, LOW);
    private static final List<Integer> LENGTHS =
            List.of(0, 1, 2, 31, 32, 33, 255, 256, 257, 4_097, 33_000);
    private static final List<Integer> WANTED = List.of(
            (int) 'x', (int) 'q', 0xE9, 0x100, (int) HIGH, (int) LOW, PAIR, 0x10000, -1,
            Character.MAX_CODE_POINT + 1);
    private static int checks;

    public static void main(String[] args) throws Exception {
        for (int length : LENGTHS) {
            for (int kind = 0; kind < 3; kind++) {
                char[] units = text(length, kind);
                String whole = new String(units);
                int middle = length / 2;
                check(whole, units);
                check(new String(("<<" + whole + ">>").toCharArray()).substring(2, 2 + length),
                        units);
                check(new String(units, 0, middle)
                        .concat(new String(units, middle, length - middle)), units);
                check(flat(units), units);
            }
        }
        System.out.println("PASS checks=" + checks);
    }

    /** Kind 0 is Latin-1, kind 1 adds wide chars, kind 2 adds pairs and lone surrogates. */
    private static char[] text(int length, int kind) {
        char[] units = new char[length];
        IntStream.range(0, length).forEach(index -> units[index] = unit(index, kind));
        return units;
    }

    private static char unit(int index, int kind) {
        if (index % 5 == 0) return 'x';
        if (index % 7 == 0) return (char) 0xE9;
        if (kind >= 1 && index % 11 == 0) return (char) 0x100;
        if (kind == 2 && index % 13 == 1) return HIGH;
        if (kind == 2 && index % 13 == 2) return LOW;
        if (kind == 2 && index % 17 == 3) return LOW;
        return (char) ('a' + index % 16);
    }

    private static void check(String subject, char[] units) {
        for (int ch : WANTED) {
            checks++;
            int expected = expected(units, ch);
            int actual = subject.countMatches(ch);
            if (actual != expected) {
                throw new AssertionError("U+" + Integer.toHexString(ch) + " in " + units.length
                        + " chars: expected " + expected + " but was " + actual);
            }
        }
    }

    private static int expected(char[] units, int ch) {
        if (!Character.isValidCodePoint(ch)) return 0;
        if (Character.isBmpCodePoint(ch)) {
            return (int) IntStream.range(0, units.length).filter(i -> units[i] == ch).count();
        }
        char high = Character.highSurrogate(ch);
        char low = Character.lowSurrogate(ch);
        return (int) IntStream.range(0, units.length - 1)
                .filter(i -> units[i] == high && units[i + 1] == low).count();
    }

    /** The per-thread admission guard gives a flat peer, as a bootstrap or refused String is. */
    @SuppressWarnings("unchecked")
    private static String flat(char[] units) throws Exception {
        Field field = optionalField("java.lang.M3String", "ADMITTING");
        if (field == null) return new String(Arrays.copyOf(units, units.length));
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

    /** Null on a VM without the M3 String owners, so the test also runs with storage off. */
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
