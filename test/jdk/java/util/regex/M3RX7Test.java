/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * GPLv2 with the Classpath exception.
 * @test
 * @summary Bounded M3 v7 ASCII DFA execution atom and exact JDK regex fallback
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm -Xcheck:jni M3RX7Test
 * @run main/othervm -Xint -Xcheck:jni M3RX7Test
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3RX7Test
 */

import java.util.Arrays;
import java.util.Random;
import java.util.regex.Pattern;
import jdk.internal.mindex.M3RX7;

public class M3RX7Test {
    private static int checks;
    private static final String ROOT = "a".repeat(64);

    public static void main(String[] args) {
        short[] transitions = new short[3 * 128];
        Arrays.fill(transitions, (short) -1);
        transitions['a'] = 1;
        transitions[1 * 128 + 'b'] = 2;
        transitions[2 * 128 + 'b'] = 2;
        byte[] accepting = {0, 0, 1};
        M3RX7 rx = M3RX7.fromAsciiDfa(ROOT, 0, transitions, accepting);
        transitions['a'] = -1;
        accepting[2] = 0;
        check(rx.programRoot().equals(ROOT), "root identity");
        check(rx.stateCount() == 3, "state geometry");
        check(rx.primitivePayloadBytes() == 3L * 128 * 2 + 3, "memory budget");
        Pattern javaPattern = Pattern.compile("ab+");
        String[] cases = {"", "a", "b", "ab", "abb", "abbb", "abab", "ab\\n", "a\r\nb",
                "ab\u0085", "a\ud83d\ude42b", "a\ud83d", "\ude42b", "\u00e9", "ab".repeat(500)};
        for (String sample : cases) compare(rx, javaPattern, sample);
        Random random = new Random(0x20261014);
        String[] fragments = {"", "a", "b", "ab", "c", "bb", "\ud83d", "\ude42", "\r", "\n"};
        for (int sample = 0; sample < 1600; sample++) {
            String value = "";
            for (int i = 0, count = random.nextInt(14); i < count; i++) {
                value = value.concat(fragments[random.nextInt(fragments.length)]);
            }
            String wrapped = "!".concat(value).concat("?");
            compare(rx, javaPattern, wrapped.substring(1, wrapped.length() - 1));
        }
        check(rx.matchesCandidate("a\ud83db") == -1, "lone surrogate refusal");
        check(rx.matchesCandidate("a\ud83d\ude42b") == -1, "code point refusal");
        check(rx.matchesCandidate("x".repeat(1_000_001)) == -1, "input budget");
        short[] altered = new short[3 * 128];
        Arrays.fill(altered, (short) -1);
        altered['a'] = 1;
        altered[1 * 128 + 'b'] = 2;
        M3RX7 bad = M3RX7.fromAsciiDfa(ROOT, 0, altered, new byte[] {0, 1, 0});
        expect(IllegalStateException.class, () -> bad.matchesVerified("a", javaPattern));
        expect(IllegalArgumentException.class, () -> M3RX7.fromAsciiDfa("bad", 0, altered, new byte[] {0, 0, 1}));
        expect(IllegalArgumentException.class, () -> M3RX7.fromAsciiDfa(ROOT, 3, altered, new byte[] {0, 0, 1}));
        expect(IllegalArgumentException.class, () -> M3RX7.fromAsciiDfa(ROOT, 0, new short[] {1}, new byte[] {0, 1}));
        altered['a'] = 9;
        expect(IllegalArgumentException.class, () -> M3RX7.fromAsciiDfa(ROOT, 0, altered, new byte[] {0, 0, 1}));
        expect(NullPointerException.class, () -> rx.matchesCandidate(null));
        tokenIndexParity();
        m3TokenParity(rx, javaPattern);
        transferAndAdaptiveParity();
        System.out.println("M3RX7Test candidate_oracle_checks=" + checks + " PASS");
    }

    private static void m3TokenParity(M3RX7 rx, Pattern pattern) {
        String first = joinRuntime("a", "b", "bb");
        String second = joinRuntime("ab", "bb");
        String third = joinRuntime("abb", "b");
        int a = rx.matchesM3TokensCandidate(first);
        int b = rx.matchesM3TokensCandidate(second);
        int c = rx.matchesM3TokensCandidate(third);
        if (a >= 0 || b >= 0 || c >= 0) {
            check(a == 1 && b == 1 && c == 1, "segmentation-independent M3 token DFA");
            M3RX7.M3TokenPlan plan = rx.prepareM3TokenPlan(first);
            check(plan != null && plan.tokenSequenceLength() > 0, "M3 token sequence retained");
            check(plan.vocabularySize() > 0, "M3 token vocabulary retained");
            check(plan.retainedPrimitiveBytes() > 0, "M3 token primitive accounting");
            check(rx.matchesM3TokensVerified(first, pattern), "M3 token verified parity");
            check(rx.matchesM3TokensCandidate(joinRuntime("a", "\u00e9")) == -1,
                    "non-ASCII M3 token refusal");
        }
    }

    private static String joinRuntime(String... parts) {
        String result = parts[0];
        for (int i = 1; i < parts.length; i++) result = result.concat(parts[i]);
        return result;
    }

    /** Large enough to exercise the existing JNI row builder when its symbol is available. */
    private static void tokenIndexParity() {
        int states = 128;
        short[] dfa = new short[states * 128];
        for (int state = 0; state < states; state++) {
            for (int unit = 0; unit < 128; unit++) {
                dfa[state * 128 + unit] = (short) ((state + unit + 1) & 127);
            }
        }
        byte[] accepting = new byte[states];
        accepting[17] = 1;
        M3RX7 rx = M3RX7.fromAsciiDfa(ROOT, 0, dfa, accepting);
        int tokens = 512;
        byte[] packed = new byte[tokens * 2];
        int[] offsets = new int[tokens];
        int[] lengths = new int[tokens];
        for (int token = 0; token < tokens; token++) {
            offsets[token] = token * 2;
            lengths[token] = 2;
            packed[token * 2] = (byte) (32 + (token & 63));
            packed[token * 2 + 1] = (byte) (32 + ((token >>> 3) & 63));
        }
        M3RX7.TokenIndex index = rx.prepareTokenIndex(packed, offsets, lengths);
        check(index != null && index.tokenCount() == tokens, "large token index");
        for (int state : new int[] {0, 1, 17, 63, 127}) {
            for (int token : new int[] {0, 1, 31, 255, 511}) {
                int cursor = state;
                for (int at = offsets[token], end = at + lengths[token]; at < end; at++) {
                    cursor = dfa[cursor * 128 + Byte.toUnsignedInt(packed[at])];
                }
                check(index.nextState(state, token) == cursor, "token transition parity");
            }
        }
    }

    private static void transferAndAdaptiveParity() {
        short[] transitions = new short[2 * 128];
        Arrays.fill(transitions, (short) -1);
        transitions['a'] = 1;
        transitions[1 * 128 + 'a'] = 1;
        byte[] accepting = {0, 1};
        M3RX7 rx = M3RX7.fromAsciiDfa(ROOT, 0, transitions, accepting);
        M3RX7.TokenIndex index = rx.prepareTokenIndex(new byte[] {'a'}, new int[] {0}, new int[] {1});
        check(index != null, "adaptive token index");
        int[] sequence = new int[4096];
        int direct = index.advance(index.initialState(), sequence, 0, sequence.length);
        int adaptive = index.advanceAdaptive(index.initialState(), sequence, 0, sequence.length);
        check(adaptive == direct && index.isFinal(adaptive), "adaptive repeated-run parity");
        M3RX7.Transfer one = index.transfer(new int[] {0});
        check(one != null && one.repeated(sequence.length).nextState(index.initialState()) == direct,
                "transfer repetition parity");
        check(one.repeated(0).nextState(index.initialState()) == index.initialState(),
                "transfer zero identity");
    }

    private static void compare(M3RX7 rx, Pattern pattern, String text) {
        boolean oracle = pattern.matcher(text).matches();
        int value = rx.matchesCandidate(text);
        if (value >= 0) check(value == (oracle ? 1 : 0), "DFA candidate disagrees: " + text);
        check(rx.matchesVerified(text, pattern) == oracle, "verified JDK parity");
    }
    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
    private static void expect(Class<? extends Throwable> type, Runnable run) {
        try { run.run(); }
        catch (Throwable thrown) { if (type.isInstance(thrown)) return;
            throw new AssertionError("wrong refusal " + thrown, thrown); }
        throw new AssertionError("missing refusal " + type.getName());
    }
}
