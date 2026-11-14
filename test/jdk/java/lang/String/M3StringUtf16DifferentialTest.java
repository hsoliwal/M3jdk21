/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors
 *
 * @test
 * @summary Differential UTF-16 String contract over malformed, isolated and paired surrogates
 * @build M3StringUtf16DifferentialCorpus
 * @run main M3StringUtf16DifferentialTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringUtf16DifferentialTest
 */

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class M3StringUtf16DifferentialTest {
    private static long checks;

    public static void main(String[] args) {
        List<char[]> values = M3StringUtf16DifferentialCorpus.values();
        for (int index = 0; index < values.size(); index++) {
            verify(values.get(index), "case-" + index);
        }
        verifyPairs(values);
        System.out.println("M3_STRING_UTF16_DIFFERENTIAL_PASS|values="
                + values.size() + "|checks=" + checks);
    }

    private static void verify(char[] oracle, String label) {
        String flat = M3StringUtf16DifferentialCorpus.fresh(oracle);
        String composed = M3StringUtf16DifferentialCorpus.composed(oracle);

        equal(flat.length(), oracle.length, label + " length");
        equal(composed.length(), oracle.length, label + " composed length");
        check(flat.equals(composed), label + " flat/composed equality");
        equal(flat.hashCode(), composed.hashCode(), label + " hash");
        equal(flat.compareTo(composed), 0, label + " compare");

        for (int i = 0; i < oracle.length; i++) {
            equal(flat.charAt(i), oracle[i], label + " charAt flat " + i);
            equal(composed.charAt(i), oracle[i], label + " charAt composed " + i);
            equal(flat.indexOf(oracle[i]), firstIndex(oracle, oracle[i]), label + " indexOf " + i);
            equal(flat.lastIndexOf(oracle[i]), lastIndex(oracle, oracle[i]), label + " lastIndexOf " + i);
        }

        int step = oracle.length <= 24 ? 1 : Math.max(1, oracle.length / 8);
        for (int begin = 0; begin <= oracle.length; begin += step) {
            for (int end = begin; end <= oracle.length; end += step) {
                int expected = Character.codePointCount(oracle, begin, end - begin);
                equal(flat.codePointCount(begin, end), expected, label + " cp flat " + begin + ":" + end);
                equal(composed.codePointCount(begin, end), expected,
                        label + " cp composed " + begin + ":" + end);
                char[] expectedSlice = Arrays.copyOfRange(oracle, begin, end);
                check(flat.substring(begin, end).equals(new String(expectedSlice)),
                        label + " substring flat " + begin + ":" + end);
                check(composed.substring(begin, end).equals(new String(expectedSlice)),
                        label + " substring composed " + begin + ":" + end);
            }
        }

        String oracleSequence = new String(oracle);
        for (int i = 0; i <= oracle.length; i++) {
            int before = Character.codePointCount(oracle, 0, i);
            int after = Character.codePointCount(oracle, i, oracle.length - i);
            compareOffset(flat, oracleSequence, i, -before, label);
            compareOffset(composed, oracleSequence, i, -before, label);
            compareOffset(flat, oracleSequence, i, after, label);
            compareOffset(composed, oracleSequence, i, after, label);
            compareOffset(flat, oracleSequence, i, -before - 1, label);
            compareOffset(composed, oracleSequence, i, after + 1, label);
        }

        byte[] expectedUtf8 = M3StringUtf16DifferentialCorpus.utf8Oracle(oracle);
        check(Arrays.equals(flat.getBytes(StandardCharsets.UTF_8), expectedUtf8),
                label + " UTF-8 flat");
        check(Arrays.equals(composed.getBytes(StandardCharsets.UTF_8), expectedUtf8),
                label + " UTF-8 composed");
        check(Arrays.equals(flat.toCharArray(), oracle), label + " toCharArray flat");
        check(Arrays.equals(composed.toCharArray(), oracle), label + " toCharArray composed");
    }

    private static void verifyPairs(List<char[]> values) {
        int limit = Math.min(values.size(), 96);
        for (int left = 0; left < limit; left++) {
            String a = M3StringUtf16DifferentialCorpus.composed(values.get(left));
            for (int right = 0; right < limit; right++) {
                String b = M3StringUtf16DifferentialCorpus.composed(values.get(right));
                String ao = new String(values.get(left));
                String bo = new String(values.get(right));
                equal(Integer.signum(a.compareTo(b)), Integer.signum(ao.compareTo(bo)),
                        "pair compare " + left + "/" + right);
                check(a.equals(b) == ao.equals(bo), "pair equals " + left + "/" + right);
                equal(a.indexOf(b), ao.indexOf(bo), "pair indexOf " + left + "/" + right);
                equal(a.lastIndexOf(b), ao.lastIndexOf(bo), "pair lastIndexOf " + left + "/" + right);
            }
        }
    }

    private static void compareOffset(
            String actual, CharSequence oracle, int index, int delta, String label) {
        Integer expected = null;
        boolean expectedFailure = false;
        try {
            expected = Character.offsetByCodePoints(oracle, index, delta);
        } catch (IndexOutOfBoundsException failure) {
            expectedFailure = true;
        }
        try {
            int result = actual.offsetByCodePoints(index, delta);
            check(!expectedFailure && result == expected,
                    label + " offset index=" + index + " delta=" + delta);
        } catch (IndexOutOfBoundsException failure) {
            check(expectedFailure, label + " unexpected offset failure");
        }
    }

    private static int firstIndex(char[] value, char unit) {
        for (int i = 0; i < value.length; i++) if (value[i] == unit) return i;
        return -1;
    }

    private static int lastIndex(char[] value, char unit) {
        for (int i = value.length - 1; i >= 0; i--) if (value[i] == unit) return i;
        return -1;
    }

    private static void equal(int actual, int expected, String label) {
        checks++;
        if (actual != expected) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void equal(char actual, char expected, String label) {
        checks++;
        if (actual != expected) {
            throw new AssertionError(label + " expected=" + (int) expected + " actual=" + (int) actual);
        }
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
