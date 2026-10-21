// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.regexcursor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Test-side mirror of {@code m3/ports/precompute/src/main/java/com/m3/precompute/
 * M3RegexOracleCorpus.java} (M3jdk21 master 2f2beb2e; first-party Synexia
 * {@code Regex10kMatrixMasteryCorpus}): 10 bases x 10 repeats x 10 wrappers x 10 tails over
 * 4 prefixes x 4 cores x 4 suffixes. The three root digests pin byte-identical inputs.
 */
final class M3RegexMasteryCorpus {
    static final String REGEX_ROOT = "215d0e2d2fdf7b8491cd749fdae29ef254c5bec467e47d898e3158d26617b8a6";
    static final String STRING_ROOT = "4785a7e1ab13050473f5b42f4a31fb241ff1adba6f60dd1fae1d3a5e140ef069";
    static final String MATRIX_ROOT = "e7614598149534e42ea1ff5af8c00dfea1c440ff3d6d94864123974f8c11f998";

    private static final List<String> BASES = List.of("a", "b", "[a-c]", "[0-3]", "\\d", "\\w", "\\s", ".",
            "(?:ab|ba)", "\\Qa.b\\E");
    private static final List<String> REPEATS = List.of("%s", "(?:%s)?", "(?:%s)*", "(?:%s)+", "(?:%s){1}",
            "(?:%s){1,2}", "(?:%s){0,2}", "(?:%s)??", "(?:%s)*?", "(?:%s)+?");
    private static final List<String> WRAPPERS = List.of("%s", "^(?:%s)$", "^(?:%s)", "(?:%s)$", "(?:%s)",
            "(?:%s|z)", "(?:z|%s)", "(?:%s)(?:x)?", "(?:x)?(?:%s)", "(?:%s)(?:y|z)?");
    private static final List<String> TAILS = List.of("%s", "(?:%s)a?", "(?:%s)b*", "(?:%s)c+", "(?:%s)d{0,1}",
            "(?:%s)[0-2]?", "(?:%s)\\d?", "(?:%s)\\w?", "(?:%s)\\s?", "(?:%s)(?:m|n)?");
    private static final List<String> PREFIXES = List.of("", "a", "x", "class ");
    private static final List<String> CORES = List.of("", "b", "123", "é");
    private static final List<String> SUFFIXES = List.of("", "\n", "\0", " while(true){} Pattern.compile(\"[\");");

    private M3RegexMasteryCorpus() {}

    static List<String> expressions() {
        List<String> out = new ArrayList<>(10_000);
        for (String base : BASES) {
            for (String repeat : REPEATS) {
                for (String wrapper : WRAPPERS) {
                    for (String tail : TAILS) {
                        out.add(tail.formatted(wrapper.formatted(repeat.formatted(base))));
                    }
                }
            }
        }
        if (!REGEX_ROOT.equals(root("M3_REGEX_10K_CORPUS_V1\n", out))) throw new IllegalStateException("REGEX_ROOT");
        return out;
    }

    static List<String> strings() {
        List<String> out = new ArrayList<>(64);
        for (String prefix : PREFIXES) {
            for (String core : CORES) {
                for (String suffix : SUFFIXES) {
                    out.add(prefix + core + suffix);
                }
            }
        }
        if (!STRING_ROOT.equals(root("M3_REGEX_64_STRINGS_V1\n", out))) throw new IllegalStateException("STRING_ROOT");
        return out;
    }

    static String matrixRoot(List<String> regexes, List<String> inputs) {
        return sha256("M3_REGEX_10K_X_64_MATRIX_V1\n" + root("M3_REGEX_10K_CORPUS_V1\n", regexes) + "\n"
                + root("M3_REGEX_64_STRINGS_V1\n", inputs) + "\n3\n");
    }

    private static String root(String header, List<String> values) {
        StringBuilder material = new StringBuilder(header);
        values.forEach(value -> material.append(value.length()).append(':').append(value).append('\n'));
        return sha256(material.toString());
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
