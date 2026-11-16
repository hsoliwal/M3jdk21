/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary charAt of deep concatenated Strings reads the right unit through the segment list
 * @run main M3StringSegmentListTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      M3StringSegmentListTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      -XX:-CompactStrings M3StringSegmentListTest
 */

import java.util.Random;
import java.util.stream.IntStream;

/**
 * A concatenated M3-backed String keeps a list of its leaf segments and finds a unit by a
 * binary search over their ends. Every index of ropes built in several shapes is compared with
 * the char array built beside them: appended piece by piece, prepended, joined pairwise,
 * doubled until the leaves outnumber what a list may hold, and ranges cut out of each.
 */
public class M3StringSegmentListTest {
    private static final long SEED = 20261116L;
    private static final int PIECES = 700;
    private static final int DOUBLINGS = 13;
    private static int checks;

    public static void main(String[] args) {
        Random random = new Random(SEED);
        String[] pieces = IntStream.range(0, PIECES).mapToObj(index -> piece(random, index))
                .toArray(String[]::new);
        check(append(pieces), join(pieces));
        check(prepend(pieces), reverseJoin(pieces));
        check(pairwise(pieces, 0, pieces.length), join(pieces));
        String doubled = "abc";
        StringBuilder flat = new StringBuilder("abc");
        for (int round = 0; round < DOUBLINGS; round++) {
            doubled = doubled.concat(doubled);
            flat.append(flat);
        }
        check(doubled, flat.toString().toCharArray());
        System.out.println("PASS checks=" + checks);
    }

    /** Pieces of one to eight chars; some hold a char outside Latin-1. */
    private static String piece(Random random, int index) {
        char[] units = new char[1 + random.nextInt(8)];
        IntStream.range(0, units.length).forEach(at -> units[at] =
                index % 9 == 0 && at == 0 ? (char) 0x3B1 : (char) ('a' + random.nextInt(26)));
        return new String(units);
    }

    private static String append(String[] pieces) {
        String text = "";
        for (String piece : pieces) text = text.concat(piece);
        return text;
    }

    private static String prepend(String[] pieces) {
        String text = "";
        for (String piece : pieces) text = piece.concat(text);
        return text;
    }

    private static String pairwise(String[] pieces, int from, int to) {
        if (to - from == 1) return pieces[from];
        int middle = (from + to) >>> 1;
        return pairwise(pieces, from, middle).concat(pairwise(pieces, middle, to));
    }

    private static char[] join(String[] pieces) {
        StringBuilder all = new StringBuilder();
        for (String piece : pieces) all.append(piece.toCharArray());
        return all.toString().toCharArray();
    }

    private static char[] reverseJoin(String[] pieces) {
        StringBuilder all = new StringBuilder();
        for (int index = pieces.length - 1; index >= 0; index--) {
            all.append(pieces[index].toCharArray());
        }
        return all.toString().toCharArray();
    }

    /** Every index of the rope, of a middle range and of a short tail range. */
    private static void check(String rope, char[] expected) {
        same(rope, expected, 0, expected.length);
        same(rope.substring(expected.length / 3, 2 * expected.length / 3), expected,
                expected.length / 3, 2 * expected.length / 3);
        same(rope.substring(expected.length - 5), expected, expected.length - 5, expected.length);
    }

    private static void same(String text, char[] expected, int from, int to) {
        if (text.length() != to - from) {
            throw new AssertionError("length " + text.length() + ", expected " + (to - from));
        }
        for (int index = to - from - 1; index >= 0; index--) {
            checks++;
            if (text.charAt(index) != expected[from + index]) {
                throw new AssertionError("index " + index + " of the range from " + from);
            }
        }
    }
}
