// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;
import java.util.HexFormat;

/**
 * Opt-in frozen, primitive-edge Liang candidate table for one reviewed ASCII
 * language-pattern generation. This is not UAX 14, Unicode grapheme validation,
 * final line layout or a replacement for the Synexia visual-compiler hyphenator.
 *
 * <p>Digits in patterns are weights between letters. Overlapping patterns use
 * the maximum weight at each gap; odd winning scores are candidate cuts,
 * constrained by caller-supplied minimum characters on each side. A dot in a
 * pattern anchors at the beginning/end of a word, not a literal hyphen. No
 * external language patterns are distributed by this class.</p>
 */
public final class M3LiangPatternPrecompute {
    private static final int MAX_PATTERNS = 100_000;
    private static final int MAX_PATTERN_LENGTH = 64;
    private static final int MAX_STATES = 1_000_000;
    private static final int MAX_WORD_LENGTH = 4096;

    private static final class BuildNode {
        final TreeMap<Character, Integer> edges = new TreeMap<>();
        byte[] output;
    }

    private final String language;
    private final String sha256;
    private final int leftMin;
    private final int rightMin;
    private final int patterns;
    private final int[] starts;
    private final char[] units;
    private final int[] targets;
    private final int[] weightsStart;
    private final int[] weightsLength;
    private final byte[] weights;

    private M3LiangPatternPrecompute(String language, String sha256, int leftMin,
            int rightMin, int patterns, int[] starts, char[] units,
            int[] targets, int[] weightsStart, int[] weightsLength, byte[] weights) {
        this.language = language;
        this.sha256 = sha256;
        this.leftMin = leftMin;
        this.rightMin = rightMin;
        this.patterns = patterns;
        this.starts = starts;
        this.units = units;
        this.targets = targets;
        this.weightsStart = weightsStart;
        this.weightsLength = weightsLength;
        this.weights = weights;
    }

    /**
     * Prepares an immutable image; no input list, map or build node survives.
     * Valid patterns contain ASCII lowercase letters, optional edge dots,
     * and single 0..9 weight digits in between. Unsupported input fails closed.
     */
    public static M3LiangPatternPrecompute compile(String languageTag,
            List<String> languagePatterns, int leftMin, int rightMin) {
        String language = Objects.requireNonNull(languageTag, "languageTag")
                .toLowerCase(Locale.ROOT);
        if (!language.matches("[a-z]{2,8}(-[a-z0-9]{2,8})*")) {
            throw new IllegalArgumentException("languageTag");
        }
        Objects.requireNonNull(languagePatterns, "languagePatterns");
        if (leftMin < 1 || rightMin < 1 || leftMin > MAX_WORD_LENGTH || rightMin > MAX_WORD_LENGTH
                || languagePatterns.size() > MAX_PATTERNS) {
            throw new IllegalArgumentException("invalid minima or pattern count");
        }
        TreeMap<String, byte[]> canonical = new TreeMap<>();
        for (String raw : languagePatterns) {
            Objects.requireNonNull(raw, "pattern");
            if (raw.length() == 0 || raw.length() > MAX_PATTERN_LENGTH) {
                throw new IllegalArgumentException("pattern length");
            }
            StringBuilder glyphs = new StringBuilder();
            byte[] parsed = new byte[raw.length() + 1];
            int gap = 0;
            boolean previousDigit = false;
            for (int i = 0; i < raw.length(); i++) {
                char c = raw.charAt(i);
                if (c >= '0' && c <= '9') {
                    if (previousDigit) throw new IllegalArgumentException("consecutive pattern weights");
                    parsed[gap] = (byte) (c - '0');
                    previousDigit = true;
                } else if (c >= 'a' && c <= 'z'
                        || c == '.' && (i == 0 || i == raw.length() - 1)) {
                    glyphs.append(c);
                    gap++;
                    previousDigit = false;
                } else {
                    throw new IllegalArgumentException("unsupported Liang pattern symbol");
                }
            }
            if (glyphs.length() == 0 || glyphs.length() > MAX_PATTERN_LENGTH) {
                throw new IllegalArgumentException("empty pattern glyphs");
            }
            String key = glyphs.toString();
            byte[] output = Arrays.copyOf(parsed, gap + 1);
            byte[] prior = canonical.putIfAbsent(key, output);
            if (prior != null) {
                for (int i = 0; i < prior.length; i++) {
                    prior[i] = (byte) Math.max(prior[i], output[i]);
                }
            }
        }
        String digest = digest(language, leftMin, rightMin, canonical);
        ArrayList<BuildNode> build = new ArrayList<>();
        build.add(new BuildNode());
        for (var row : canonical.entrySet()) {
            int state = 0;
            for (int i = 0; i < row.getKey().length(); i++) {
                char c = row.getKey().charAt(i);
                Integer next = build.get(state).edges.get(c);
                if (next == null) {
                    if (build.size() == MAX_STATES) throw new IllegalArgumentException("pattern image too large");
                    next = build.size();
                    build.get(state).edges.put(c, next);
                    build.add(new BuildNode());
                }
                state = next;
            }
            build.get(state).output = row.getValue();
        }
        int stateCount = build.size();
        int[] starts = new int[stateCount + 1];
        int[] weightsStart = new int[stateCount];
        int[] weightsLength = new int[stateCount];
        int edgeCount = 0;
        int outputCount = 0;
        for (int s = 0; s < stateCount; s++) {
            BuildNode node = build.get(s);
            starts[s] = edgeCount;
            edgeCount = Math.addExact(edgeCount, node.edges.size());
            weightsStart[s] = outputCount;
            if (node.output != null) {
                weightsLength[s] = node.output.length;
                outputCount = Math.addExact(outputCount, node.output.length);
            }
        }
        starts[stateCount] = edgeCount;
        char[] units = new char[edgeCount];
        int[] targets = new int[edgeCount];
        byte[] weights = new byte[outputCount];
        for (int s = 0; s < stateCount; s++) {
            BuildNode node = build.get(s);
            int edge = starts[s];
            for (var e : node.edges.entrySet()) {
                units[edge] = e.getKey();
                targets[edge++] = e.getValue();
            }
            if (node.output != null) {
                System.arraycopy(node.output, 0, weights, weightsStart[s], node.output.length);
            }
        }
        return new M3LiangPatternPrecompute(language, digest, leftMin, rightMin,
                canonical.size(), starts, units, targets, weightsStart, weightsLength, weights);
    }

    /** Candidate-only cut bitmap; bit i marks a cut before UTF-16 unit i. */
    public long[] candidateBits(CharSequence word) {
        Objects.requireNonNull(word, "word");
        return compute(word, null, word.length());
    }

    /**
     * Candidate-only cuts over the existing non-flattening atom cursor.
     * Caller-provided atom content must be stable throughout this call.
     */
    public long[] candidateBits(M3AtomCursor word) {
        Objects.requireNonNull(word, "word");
        return compute(null, word, word.length());
    }

    private long[] compute(CharSequence source, M3AtomCursor tuple, int n) {
        if (n > MAX_WORD_LENGTH) throw new IllegalArgumentException("word too long");
        for (int i = 0; i < n; i++) asciiLetter(source == null ? tuple.charAt(i) : source.charAt(i));
        long[] result = M3BitLanes.lanes(n + 1);
        if (n < leftMin + rightMin || patterns == 0) return result;
        int[] score = new int[n + 3]; // gaps of virtual '.' + word + '.'
        for (int start = 0; start < n + 2; start++) {
            int state = 0;
            for (int pos = start; pos < n + 2; pos++) {
                char c = pos == 0 || pos == n + 1 ? '.'
                        : asciiLetter(source == null ? tuple.charAt(pos - 1) : source.charAt(pos - 1));
                int edge = Arrays.binarySearch(units, starts[state], starts[state + 1], c);
                if (edge < 0) break;
                state = targets[edge];
                for (int w = 0; w < weightsLength[state]; w++) {
                    int index = start + w;
                    score[index] = Math.max(score[index], weights[weightsStart[state] + w]);
                }
            }
        }
        for (int cut = leftMin; cut <= n - rightMin; cut++) {
            if ((score[cut + 1] & 1) != 0) M3BitLanes.set(result, cut);
        }
        return result;
    }

    private static char asciiLetter(char c) {
        if (c >= 'A' && c <= 'Z') return (char) (c + ('a' - 'A'));
        if (c >= 'a' && c <= 'z') return c;
        throw new IllegalArgumentException("non-ASCII word; language hyphenation authority required");
    }

    private static String digest(String language, int left, int right, TreeMap<String, byte[]> rows) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(("M3-LIANG-ASCII-V1\n" + language + "\n" + left + "\n" + right + "\n")
                    .getBytes(StandardCharsets.UTF_8));
            for (var row : rows.entrySet()) {
                md.update(row.getKey().getBytes(StandardCharsets.US_ASCII));
                md.update((byte) 0);
                md.update(row.getValue());
                md.update((byte) '\n');
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public String language() { return language; }
    public String sha256() { return sha256; }
    public int patternCount() { return patterns; }
    public int stateCount() { return starts.length - 1; }
    public int leftMin() { return leftMin; }
    public int rightMin() { return rightMin; }
}
