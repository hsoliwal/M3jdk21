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
        System.out.println("M3RX7Test candidate_oracle_checks=" + checks + " PASS");
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
