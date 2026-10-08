// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Frozen, hash-bound regex x String corpus: 10,000 generated expressions by 64 generated inputs.
 *
 * <p>The generator and the three root digests are the first-party Synexia
 * {@code Regex10kMatrixMasteryCorpus} (10 bases x 10 repeats x 10 wrappers x 10 tails; 4 prefixes
 * x 4 cores x 4 suffixes). Any drift in cardinality, uniqueness or ordering throws, so every
 * consumer that cites {@link #MATRIX_ROOT} is talking about byte-identical inputs. The corpus
 * carries no regex semantics of its own: {@link java.util.regex.Pattern} stays the oracle (see
 * {@link M3RegexOracleHarness}).</p>
 */
public final class M3RegexOracleCorpus {
    public static final int REGEX_COUNT = 10_000;
    public static final int STRING_COUNT = 64;
    public static final int SHARD_COUNT = 10;
    public static final int REGEX_PER_SHARD = REGEX_COUNT / SHARD_COUNT;
    public static final String REGEX_ROOT =
            "215d0e2d2fdf7b8491cd749fdae29ef254c5bec467e47d898e3158d26617b8a6";
    public static final String STRING_ROOT =
            "4785a7e1ab13050473f5b42f4a31fb241ff1adba6f60dd1fae1d3a5e140ef069";
    public static final String MATRIX_ROOT =
            "e7614598149534e42ea1ff5af8c00dfea1c440ff3d6d94864123974f8c11f998";

    private static final List<String> BASES =
            List.of("a", "b", "[a-c]", "[0-3]", "\\d", "\\w", "\\s", ".",
                    "(?:ab|ba)", "\\Qa.b\\E");
    private static final List<String> REPEATS =
            List.of("%s", "(?:%s)?", "(?:%s)*", "(?:%s)+", "(?:%s){1}",
                    "(?:%s){1,2}", "(?:%s){0,2}", "(?:%s)??", "(?:%s)*?", "(?:%s)+?");
    private static final List<String> WRAPPERS =
            List.of("%s", "^(?:%s)$", "^(?:%s)", "(?:%s)$", "(?:%s)",
                    "(?:%s|z)", "(?:z|%s)", "(?:%s)(?:x)?", "(?:x)?(?:%s)",
                    "(?:%s)(?:y|z)?");
    private static final List<String> TAILS =
            List.of("%s", "(?:%s)a?", "(?:%s)b*", "(?:%s)c+", "(?:%s)d{0,1}",
                    "(?:%s)[0-2]?", "(?:%s)\\d?", "(?:%s)\\w?", "(?:%s)\\s?",
                    "(?:%s)(?:m|n)?");
    private static final List<String> PREFIXES = List.of("", "a", "x", "class ");
    private static final List<String> CORES = List.of("", "b", "123", "é");
    private static final List<String> SUFFIXES =
            List.of("", "\n", "\0", " while(true){} Pattern.compile(\"[\");");

    private M3RegexOracleCorpus() {}

    /** The 10,000 expressions in generation order; throws on cardinality or root drift. */
    public static List<String> expressions() {
        List<String> out = new ArrayList<>(REGEX_COUNT);
        for (String base : BASES) {
            for (String repeat : REPEATS) {
                String repeated = repeat.formatted(base);
                for (String wrapper : WRAPPERS) {
                    String wrapped = wrapper.formatted(repeated);
                    for (String tail : TAILS) {
                        out.add(tail.formatted(wrapped));
                    }
                }
            }
        }
        if (out.size() != REGEX_COUNT || out.stream().distinct().count() != REGEX_COUNT) {
            throw new IllegalStateException("REGEX_10K_CARDINALITY");
        }
        if (!REGEX_ROOT.equals(regexRoot(out))) {
            throw new IllegalStateException("REGEX_10K_ROOT_DRIFT");
        }
        return List.copyOf(out);
    }

    /** The 64 inputs in generation order; throws on cardinality or root drift. */
    public static List<String> strings() {
        List<String> out = new ArrayList<>(STRING_COUNT);
        for (String prefix : PREFIXES) {
            for (String core : CORES) {
                for (String suffix : SUFFIXES) {
                    out.add(prefix + core + suffix);
                }
            }
        }
        if (out.size() != STRING_COUNT || out.stream().distinct().count() != STRING_COUNT) {
            throw new IllegalStateException("STRING_64_CARDINALITY");
        }
        if (!STRING_ROOT.equals(stringRoot(out))) {
            throw new IllegalStateException("STRING_64_ROOT_DRIFT");
        }
        return List.copyOf(out);
    }

    /** Expressions {@code [shard * REGEX_PER_SHARD, (shard + 1) * REGEX_PER_SHARD)}. */
    public static List<String> shard(List<String> expressions, int shard) {
        if (shard < 0 || shard >= SHARD_COUNT || expressions.size() != REGEX_COUNT) {
            throw new IllegalArgumentException("shard " + shard + " of " + expressions.size());
        }
        int start = shard * REGEX_PER_SHARD;
        return expressions.subList(start, start + REGEX_PER_SHARD);
    }

    public static String regexRoot(List<String> regexes) {
        StringBuilder material = new StringBuilder("M3_REGEX_10K_CORPUS_V1\n");
        regexes.forEach(regex -> material
                .append(regex.length()).append(':').append(regex).append('\n'));
        return sha256(material.toString());
    }

    public static String stringRoot(List<String> inputs) {
        StringBuilder material = new StringBuilder("M3_REGEX_64_STRINGS_V1\n");
        inputs.forEach(input -> material
                .append(input.length()).append(':').append(input).append('\n'));
        return sha256(material.toString());
    }

    public static String matrixRoot(List<String> regexes, List<String> inputs) {
        return sha256(
                "M3_REGEX_10K_X_64_MATRIX_V1\n"
                        + regexRoot(regexes) + "\n"
                        + stringRoot(inputs) + "\n3\n");
    }

    static String sha256(String value) {
        return HexFormat.of().formatHex(
                digest().digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
