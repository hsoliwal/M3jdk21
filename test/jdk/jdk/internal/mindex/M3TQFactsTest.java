/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Exact M3TQ trigram facts compose without retaining source text
 * @modules java.base/jdk.internal.mindex
 * @run main M3TQFactsTest
 */

import java.util.Arrays;
import java.util.Random;
import jdk.internal.mindex.M3TQ;

public class M3TQFactsTest {
    private static long checks;

    public static void main(String[] args) {
        deterministic();
        randomized();
        System.out.println("M3_TQ_FACTS_PASS|checks=" + checks);
    }

    private static void deterministic() {
        verify("", "");
        verify("a", "b");
        verify("ab", "cd");
        verify("abc", "def");
        verify("ab", "c");
        verify("a", "bc");
        verify("\ud83d", "\ude42x");
        verify("x\ud83d", "\ude42");
        verify("\u0000\uffff", "\u1234");

        M3TQ.Facts source = M3TQ.precompute("abcabcxyz", 64);
        M3TQ.Facts present = M3TQ.precompute("abcxyz", 64);
        M3TQ.Facts absent = M3TQ.precompute("acb", 64);
        check(source.containsAll(present), "present trigram subset");
        check(!source.containsAll(absent), "absent trigram subset");

        long[] exported = source.keys();
        if (exported.length != 0) exported[0] ^= 1L;
        check(source.containsAll(M3TQ.precompute("abc", 64)), "defensive key export");

        expectIAE(() -> M3TQ.precompute("abcdef", 5), "whole budget");
        expectIAE(() -> M3TQ.precompute("abcdef", 1, 6, 4), "range budget");
        expectIOOBE(() -> M3TQ.precompute("abc", -1, 2, 3), "range bounds");
    }

    private static void randomized() {
        Random random = new Random(0x4d33545146414354L);
        char[] alphabet = {
                0, 'a', 'b', 'c', 'X', '\u00ff', '\u0100',
                '\ud83d', '\ude42', '\uffff'
        };
        for (int trial = 0; trial < 5_000; trial++) {
            String left = randomString(random, alphabet, random.nextInt(12));
            String right = randomString(random, alphabet, random.nextInt(12));
            verify(left, right);

            String joined = left + right;
            if (joined.length() >= 3) {
                int start = random.nextInt(joined.length() - 2);
                int length = 3 + random.nextInt(joined.length() - start - 2);
                String slice = joined.substring(start, start + length);
                M3TQ.Facts source = M3TQ.precompute(joined, 64);
                M3TQ.Facts required = M3TQ.precompute(slice, 64);
                check(source.containsAll(required), "random subset " + trial);
            }
        }
    }

    private static void verify(String left, String right) {
        String joined = left + right;
        int budget = Math.max(0, joined.length());
        M3TQ.Facts leftFacts = M3TQ.precompute(left, Math.max(0, left.length()));
        M3TQ.Facts rightFacts = M3TQ.precompute(right, Math.max(0, right.length()));
        M3TQ.Facts composed = leftFacts.concat(rightFacts, budget);
        M3TQ.Facts direct = M3TQ.precompute(joined, budget);

        check(composed.utf16Length() == direct.utf16Length(), "length");
        check(Arrays.equals(composed.keys(), direct.keys()),
                "exact key composition left=" + printable(left) + " right=" + printable(right));
        check(composed.containsAll(direct), "composed contains direct");
        check(direct.containsAll(composed), "direct contains composed");
    }

    private static String randomString(Random random, char[] alphabet, int length) {
        char[] value = new char[length];
        for (int index = 0; index < length; index++) {
            value[index] = alphabet[random.nextInt(alphabet.length)];
        }
        return new String(value);
    }

    private static String printable(String value) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            if (unit >= 0x20 && unit <= 0x7e) result.append(unit);
            else result.append(String.format("\\u%04x", (int) unit));
        }
        return result.toString();
    }

    private static void expectIAE(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError(label + " did not throw");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }

    private static void expectIOOBE(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError(label + " did not throw");
        } catch (IndexOutOfBoundsException expected) {
            checks++;
        }
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
