// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.vendor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Python string semantics the oracle ({@code switch_consumers.py}) relies on, reproduced exactly so that every
 * decision and every row byte is the same: {@code str.strip()}, {@code s[:-n]}, {@code sorted()} on code points,
 * and the {@code --default-props k=v,...} parser.
 */
final class PyText {

    /** Characters for which CPython {@code str.isspace()} is true (bidi WS/B/S or category Zs). */
    private static final String PY_WHITESPACE = " \t\n\u000b\f\r\u001c\u001d\u001e\u001f\u0085  "
            + "               　";

    private PyText() {
    }

    static boolean isSpace(char c) {
        return PY_WHITESPACE.indexOf(c) >= 0;
    }

    /** {@code s.strip()}. */
    static String strip(String s) {
        int begin = IntStream.range(0, s.length()).filter(i -> !isSpace(s.charAt(i))).findFirst().orElse(s.length());
        int end = IntStream.iterate(s.length(), i -> i > begin, i -> i - 1)
                .filter(i -> !isSpace(s.charAt(i - 1))).findFirst().orElse(begin);
        return s.substring(begin, end);
    }

    /** {@code s[:-n]} for {@code n >= 0}; note {@code s[:-0] == ""} in Python. */
    static String dropLast(String s, int n) {
        return n == 0 ? "" : s.substring(0, Math.max(0, s.length() - n));
    }

    /** {@code ",".join(sorted(values))} with Python's code-point ordering. */
    static String sortedJoin(Collection<String> values) {
        Comparator<String> byCodePoint = (x, y) -> Arrays.compare(x.codePoints().toArray(), y.codePoints().toArray());
        return values.stream().sorted(byCodePoint).collect(Collectors.joining(","));
    }

    /** {@code dict(x.split("=", 1) for x in s.split(",") if "=" in x)}. */
    static Map<String, String> defaults(String spec) {
        return Arrays.stream(spec.split(",", -1)).filter(x -> x.contains("="))
                .collect(Collectors.toMap(x -> x.substring(0, x.indexOf('=')), x -> x.substring(x.indexOf('=') + 1),
                        (first, last) -> last, LinkedHashMap::new));
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String sha256(String text) {
        return sha256(text.getBytes(StandardCharsets.UTF_8));
    }

    static byte[] readBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String slash(Path path) {
        return path.toString().replace('\\', '/');
    }
}
