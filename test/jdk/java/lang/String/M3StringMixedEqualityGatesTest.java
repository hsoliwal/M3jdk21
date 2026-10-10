// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary Mixed-side equality gates: a flat String's cached hash against an M3 String's known
 *          canonical hash (equals) and an allocation-free ASCII-lower fold of the flat region against
 *          prepared ASCII facts (equalsIgnoreCase, regionMatches ignoring case). The gates agree with
 *          the facts they stand in for, never reject equal content, and the consumers match stock
 *          results for hashed and unhashed flat sides
 * @modules java.base/java.lang:+open
 * @run main M3StringMixedEqualityGatesTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringMixedEqualityGatesTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Random;

public class M3StringMixedEqualityGatesTest {
    private static final String[] ATOMS = {
        "a", "bc", "XYZ", "hello", "HELLO", "Hello", " ", "\t", "é", "É", "ÿ", "Ā",
        " ", "🙂", "\ud83d", "\ude42", "\u0000", "abcabc", "ABCABC", "needle", "k",
        "K", "i", "İ", "s", "ſ", "The quick brown fox", "0123456789", "ab", "AB"
    };
    private static long checks;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static Method facts;
    private static Method factsIfPrepared;
    private static Method isWholeOwner;
    private static Method hashKnownToDiffer;
    private static Method asciiLowerHashOrNegative;
    private static Field asciiField;
    private static Field asciiLowerHashField;

    public static void main(String[] args) throws Exception {
        bind();
        hashGate();
        caseGate();
        consumers();
        System.out.println("M3StringMixedEqualityGatesTest checks=" + checks);
    }

    private static void bind() throws Exception {
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Class<?> m3 = Class.forName("java.lang.M3String");
        Class<?> factsClass = Class.forName("java.lang.M3StringFacts");
        internChars = accessible(pool.getDeclaredMethod("internChars", char[].class, int.class, int.class));
        concat = accessible(m3.getDeclaredMethod("concat", m3));
        slice = accessible(m3.getDeclaredMethod("slice", int.class, int.class));
        facts = accessible(m3.getDeclaredMethod("facts"));
        factsIfPrepared = accessible(m3.getDeclaredMethod("factsIfPrepared"));
        isWholeOwner = accessible(m3.getDeclaredMethod("isWholeOwner"));
        hashKnownToDiffer = accessible(m3.getDeclaredMethod("hashKnownToDiffer", int.class));
        asciiLowerHashOrNegative = accessible(factsClass.getDeclaredMethod("asciiLowerHashOrNegative",
                String.class, int.class, int.class));
        asciiField = factsClass.getDeclaredField("ascii");
        asciiField.setAccessible(true);
        asciiLowerHashField = factsClass.getDeclaredField("asciiLowerHash");
        asciiLowerHashField.setAccessible(true);
    }

    /** hashKnownToDiffer: exact against String.hashCode, never true for equal content, silent for unprepared ranges. */
    private static void hashGate() throws Exception {
        Random random = new Random(0x4d3345514841534cL);
        int silentRanges = 0;
        for (int round = 0; round < 3_000; round++) {
            String spelling = spell(random, 1 + random.nextInt(5));
            Object atom = atom(spelling);
            int hash = spelling.hashCode();
            check(!(boolean) hashKnownToDiffer.invoke(atom, hash), "whole owner: equal hash never differs");
            check((boolean) hashKnownToDiffer.invoke(atom, hash ^ 1), "whole owner: different hash is known to differ");
            String other = spell(random, 1 + random.nextInt(3));
            check((boolean) hashKnownToDiffer.invoke(atom, other.hashCode()) == (other.hashCode() != hash),
                    "whole owner agrees with String.hashCode " + show(spelling) + " / " + show(other));
            if (spelling.length() > 1) {
                int begin = random.nextInt(spelling.length());
                int end = begin + random.nextInt(spelling.length() - begin + 1);
                Object range = slice.invoke(atom, begin, end);
                // A short slice may canonicalize to a pooled whole owner whose hash is always known.
                if (!(boolean) isWholeOwner.invoke(range) && factsIfPrepared.invoke(range) == null) {
                    silentRanges++;
                    check(!(boolean) hashKnownToDiffer.invoke(range, 12345), "unprepared range proves nothing");
                }
                facts.invoke(range);
                int rangeHash = spelling.substring(begin, end).hashCode();
                check(!(boolean) hashKnownToDiffer.invoke(range, rangeHash), "prepared range: equal hash never differs");
                check((boolean) hashKnownToDiffer.invoke(range, rangeHash + 7), "prepared range: different hash differs");
            }
            // Tuples: concat yields a whole owner whose canonical hash is composed, not scanned.
            Object tuple = concat.invoke(atom, atom(other));
            int tupleHash = (spelling + other).hashCode();
            check(!(boolean) hashKnownToDiffer.invoke(tuple, tupleHash), "tuple: equal hash never differs");
            check((boolean) hashKnownToDiffer.invoke(tuple, tupleHash ^ 0x5bd1e995), "tuple: different hash differs");
        }
        check(silentRanges > 0, "some ranges were observed unprepared");
    }

    /** asciiLowerHashOrNegative: equals the prepared asciiLowerHash for ASCII regions, -1 otherwise; sound for equalsIgnoreCase. */
    private static void caseGate() throws Exception {
        Random random = new Random(0x4d334341534547L);
        int gatedPairs = 0;
        for (int round = 0; round < 3_000; round++) {
            String spelling = spell(random, 1 + random.nextInt(5));
            int from = spelling.isEmpty() ? 0 : random.nextInt(spelling.length());
            int to = from + random.nextInt(spelling.length() - from + 1);
            String region = spelling.substring(from, to);
            long folded = (long) asciiLowerHashOrNegative.invoke(null, spelling, from, to);
            boolean ascii = region.chars().allMatch(c -> c <= 0x7f);
            if (!ascii) {
                check(folded == -1L, "non-ASCII region folds to -1 " + show(region));
            } else {
                Object prepared = facts.invoke(atom(region));
                check(asciiField.getBoolean(prepared), "ASCII region has ASCII facts");
                check(folded >= 0 && (int) folded == asciiLowerHashField.getInt(prepared),
                        "fold equals prepared asciiLowerHash " + show(region));
            }
            // Soundness: equal ignoring case (JDK rule) implies the gate cannot fire.
            String other = round % 2 == 0 ? mixCase(region, random) : spell(random, 1 + random.nextInt(3));
            if (region.equalsIgnoreCase(other)) {
                Object prepared = facts.invoke(atom(region));
                long otherFold = (long) asciiLowerHashOrNegative.invoke(null, other, 0, other.length());
                boolean gateFires = asciiField.getBoolean(prepared) && otherFold >= 0
                        && (int) otherFold != asciiLowerHashField.getInt(prepared);
                check(!gateFires, "gate fired on equal-ignoring-case content " + show(region) + " / " + show(other));
                gatedPairs++;
            }
        }
        check(gatedPairs > 0, "equal-ignoring-case pairs were exercised");
        // Known cross-script equalities: the gate must stay silent because one side is not ASCII.
        for (String[] pair : new String[][] {{"k", "K"}, {"i", "İ"}, {"s", "ſ"}}) {
            Object prepared = facts.invoke(atom(pair[0]));
            long fold = (long) asciiLowerHashOrNegative.invoke(null, pair[1], 0, 1);
            check(fold == -1L, "non-ASCII counterpart never folds: " + show(pair[1]));
            check(asciiField.getBoolean(prepared), "ASCII side");
        }
    }

    /** Consumers against stock results: M3 receiver (flag-on) with flat sides, hashed or not. */
    private static void consumers() {
        Random random = new Random(0x4d33434f4e53554dL);
        for (int round = 0; round < 3_000; round++) {
            String spelling = spell(random, 1 + random.nextInt(5));
            String receiver = new String(spelling.toCharArray());
            String flat = round % 3 == 0 ? new String(spelling.toCharArray())
                    : round % 3 == 1 ? mixCase(spelling, random) : spell(random, 1 + random.nextInt(4));
            if (round % 2 == 0) flat.hashCode();  // cached hash on the flat side
            boolean expectedEquals = sameChars(spelling, flat);
            check(receiver.equals(flat) == expectedEquals, "equals " + show(spelling) + " / " + show(flat));
            check(flat.equals(receiver) == expectedEquals, "equals reversed");
            boolean expectedIgnoreCase = stockEqualsIgnoreCase(spelling, flat);
            check(receiver.equalsIgnoreCase(flat) == expectedIgnoreCase, "equalsIgnoreCase " + show(spelling) + " / " + show(flat));
            check(flat.equalsIgnoreCase(receiver) == expectedIgnoreCase, "equalsIgnoreCase reversed");
            if (!spelling.isEmpty() && !flat.isEmpty()) {
                int len = 1 + random.nextInt(Math.min(spelling.length(), flat.length()));
                int toffset = random.nextInt(spelling.length() - len + 1);
                int ooffset = random.nextInt(flat.length() - len + 1);
                boolean expectedRegion = stockEqualsIgnoreCase(spelling.substring(toffset, toffset + len),
                        flat.substring(ooffset, ooffset + len));
                check(receiver.regionMatches(true, toffset, flat, ooffset, len) == expectedRegion, "regionMatches ignoreCase");
            }
            check(receiver.toLowerCase(Locale.ROOT).equals(spelling.toLowerCase(Locale.ROOT)), "toLowerCase parity");
        }
    }

    private static boolean sameChars(String left, String right) {
        if (left.length() != right.length()) return false;
        for (int index = 0; index < left.length(); index++) {
            if (left.charAt(index) != right.charAt(index)) return false;
        }
        return true;
    }

    /** The JDK rule, applied per UTF-16 unit as the stock Latin1/UTF16 regionMatches does. */
    private static boolean stockEqualsIgnoreCase(String left, String right) {
        if (left.length() != right.length()) return false;
        for (int index = 0; index < left.length(); index++) {
            char c1 = left.charAt(index);
            char c2 = right.charAt(index);
            if (c1 == c2) continue;
            char u1 = Character.toUpperCase(c1);
            char u2 = Character.toUpperCase(c2);
            if (u1 == u2) continue;
            if (Character.toLowerCase(u1) == Character.toLowerCase(u2)) continue;
            return false;
        }
        return true;
    }

    private static String mixCase(String value, Random random) {
        StringBuilder out = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            out.append(random.nextBoolean() ? Character.toUpperCase(unit) : Character.toLowerCase(unit));
        }
        return out.toString();
    }

    private static Object atom(String spelling) throws Exception {
        char[] chars = spelling.toCharArray();
        return internChars.invoke(null, chars, 0, chars.length);
    }

    private static String spell(Random random, int parts) {
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < parts; index++) out.append(ATOMS[random.nextInt(ATOMS.length)]);
        return out.toString();
    }

    private static String show(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            out.append(unit >= 0x20 && unit <= 0x7e ? String.valueOf(unit) : String.format("\\u%04x", (int) unit));
        }
        return out.append('"').toString();
    }

    private static Method accessible(Method method) {
        method.setAccessible(true);
        return method;
    }

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}
