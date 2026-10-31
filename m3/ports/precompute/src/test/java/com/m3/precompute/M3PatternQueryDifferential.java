// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * G7r driver for the M3TQ general-pattern absence gate: over the frozen
 * {@link M3RegexOracleCorpus}, every {@code Pattern} search on a {@code String} (gated) must
 * agree with the same search on a non-String {@code CharSequence} (the engine without the gate)
 * in result, spans, {@code hitEnd} and {@code requireEnd}; the digest of the ungated outcomes is
 * printed so a stock JDK run and a {@code --patch-module} run can be compared byte for byte.
 *
 * <p>Run once on the stock JDK 21 and once with {@code --patch-module java.base=<postimages>
 * --add-opens java.base/java.util.regex=ALL-UNNAMED}; the digests must be equal and the patched
 * run reports how many patterns carry a constraint and how many searches were pruned.</p>
 */
public final class M3PatternQueryDifferential {
    private M3PatternQueryDifferential() {}

    public static void main(String[] args) throws Exception {
        List<String> expressions = M3RegexOracleCorpus.expressions();
        List<String> inputs = M3RegexOracleCorpus.strings();
        Field queryField = null;
        try {
            queryField = Pattern.class.getDeclaredField("m3Tq");
            queryField.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            queryField = null;
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long checks = 0;
        long constrained = 0;
        long pruned = 0;
        long found = 0;
        for (String regex : expressions) {
            Pattern pattern = Pattern.compile(regex);
            Object query = queryField == null ? null : queryField.get(pattern);
            boolean hasQuery = query != null && (boolean) query.getClass().getMethod("hasConstraints").invoke(query);
            if (hasQuery) {
                constrained++;
            }
            for (String input : inputs) {
                Matcher gated = pattern.matcher(input);
                Matcher free = pattern.matcher(new Ungated(input));
                boolean g = gated.find();
                boolean f = free.find();
                update(digest, regex, input, f, free.hitEnd(), free.requireEnd(), f ? free.start() : -1, f ? free.end() : -1);
                checks += 3;
                if (g != f || gated.hitEnd() != free.hitEnd() || gated.requireEnd() != free.requireEnd()) {
                    throw new AssertionError("gate differs: " + regex + " on " + input);
                }
                if (f) {
                    found++;
                    checks++;
                    if (gated.start() != free.start() || gated.end() != free.end()) {
                        throw new AssertionError("span differs: " + regex + " on " + input);
                    }
                } else if (hasQuery) {
                    Object facts = Class.forName("jdk.internal.mindex.M3TQ")
                            .getMethod("precompute", CharSequence.class, int.class).invoke(null, input, input.length());
                    boolean allowed = (boolean) query.getClass().getMethod("testPrecomputed", facts.getClass()).invoke(query, facts);
                    if (!allowed) {
                        pruned++;
                    }
                }
            }
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) {
            hex.append(Character.forDigit((b >>> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
        }
        System.out.println("M3_PATTERN_QUERY_DIFFERENTIAL pairs=" + ((long) expressions.size() * inputs.size())
                + " checks=" + checks + " found=" + found + " ungatedDigest=" + hex
                + " gate=" + (queryField == null ? "ABSENT" : "PRESENT")
                + " constrainedPatterns=" + constrained + " prunedSearches=" + pruned);
    }

    private static void update(MessageDigest digest, String regex, String input, boolean found,
                               boolean hitEnd, boolean requireEnd, int start, int end) {
        String row = regex + "\u0000" + input + "\u0000" + found + "\u0000" + hitEnd + "\u0000" + requireEnd
                + "\u0000" + start + "\u0000" + end + "\n";
        digest.update(row.getBytes(StandardCharsets.UTF_8));
    }

    private static final class Ungated implements CharSequence {
        private final String text;

        Ungated(String text) {
            this.text = text;
        }

        @Override
        public int length() {
            return text.length();
        }

        @Override
        public char charAt(int index) {
            return text.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new Ungated(text.substring(start, end));
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
