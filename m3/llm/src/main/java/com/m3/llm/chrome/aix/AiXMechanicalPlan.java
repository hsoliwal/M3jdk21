// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package com.m3.llm.chrome.aix;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic CPU-only preprocessing: exact chunks, mechanical entropy and source-bound identities. */
public final class AiXMechanicalPlan {
    public static final int DEFAULT_CHUNK_BYTES = 12_000;
    public static final int DEFAULT_CHUNK_CHARS = 12_000;
    public static final int MAX_CHUNKS = 16_384;

    private static final Pattern TOKEN = Pattern.compile(
            "[A-Za-z_$][A-Za-z0-9_$]*|\\d+(?:\\.\\d+)?|===|!==|=>|==|!=|<=|>=|&&|\\|\\||\\+\\+|--|[^\\s]",
            Pattern.MULTILINE);
    private static final Pattern SYMBOL = Pattern.compile(
            "\\b(?:async\\s+)?function\\s+([A-Za-z_$][\\w$]*)\\s*\\(|"
                    + "\\bclass\\s+([A-Za-z_$][\\w$]*)\\b|"
                    + "\\b(?:const|let|var)\\s+([A-Za-z_$][\\w$]*)\\s*=\\s*(?:async\\s*)?(?:\\([^)]*\\)|[A-Za-z_$][\\w$]*)\\s*=>|"
                    + "^\\s*(?:async\\s+)?def\\s+([A-Za-z_]\\w*)\\s*\\(|"
                    + "^\\s*class\\s+([A-Za-z_]\\w*)\\b",
            Pattern.MULTILINE);
    private static final Pattern EVENT = Pattern.compile(
            "\\.addEventListener\\(\\s*['\"]([^'\"]+)['\"]|"
                    + "\\.on(click|input|change|keydown|keyup|pointerdown|pointermove|pointerup|pointercancel|submit|toggle|scroll|resize|contextmenu|dblclick|focus|blur)\\s*=");
    private static final List<String> CHROME_AI = List.of(
            "LanguageModel", "Summarizer", "Writer", "Rewriter", "Proofreader", "LanguageDetector", "Translator");

    public record Limits(int chunkBytes, int chunkChars) {
        public Limits {
            if (chunkBytes < 256 || chunkBytes > 12_000 || chunkChars < 256 || chunkChars > 12_000) {
                throw new IllegalArgumentException("chunk limits out of range");
            }
        }
        public static Limits defaults() { return new Limits(DEFAULT_CHUNK_BYTES, DEFAULT_CHUNK_CHARS); }
    }

    public record Entropy(
            int tokens,
            int uniqueTokens,
            int tokenUniquenessPpm,
            int tokenRepetitionPpm,
            long byteEntropyMicrobits,
            List<String> symbols,
            List<String> events,
            Map<String, Integer> chromeAiMentions,
            String root) {
        public Entropy {
            symbols = List.copyOf(symbols);
            events = List.copyOf(events);
            chromeAiMentions = Map.copyOf(chromeAiMentions);
        }
    }

    public record Chunk(
            int ordinal,
            int charStart,
            int charEnd,
            int byteStart,
            int byteEnd,
            String sha256,
            Entropy entropy,
            String text) {
        public Chunk {
            if (ordinal < 0 || charStart < 0 || charEnd < charStart || byteStart < 0 || byteEnd < byteStart) {
                throw new IllegalArgumentException("chunk bounds");
            }
        }
        public int byteLength() { return byteEnd - byteStart; }
        public int charLength() { return charEnd - charStart; }
    }

    public record Plan(
            String schema,
            String sourceName,
            int sourceBytes,
            String sourceSha256,
            Limits limits,
            List<Chunk> chunks,
            String planRoot) {
        public Plan { chunks = List.copyOf(chunks); }
    }

    private AiXMechanicalPlan() {}

    public static Plan build(AiXSourceSnapshot source) {
        return build(source, Limits.defaults());
    }

    public static Plan build(AiXSourceSnapshot source, Limits limits) {
        String text = source.text();
        int[] bytesAt = byteOffsets(text);
        if (bytesAt[text.length()] != source.size()) {
            throw new IllegalStateException("source UTF-8 offset mismatch");
        }
        List<Chunk> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = start;
            int lastGood = -1;
            while (end < text.length()) {
                int cp = text.codePointAt(end);
                int next = end + Character.charCount(cp);
                int byteLength = bytesAt[next] - bytesAt[start];
                int charLength = next - start;
                if (byteLength > limits.chunkBytes() || charLength > limits.chunkChars()) {
                    break;
                }
                lastGood = next;
                end = next;
            }
            if (lastGood <= start) {
                throw new IllegalArgumentException("single code point exceeds chunk budget");
            }
            int byteStart = bytesAt[start];
            int byteEnd = bytesAt[lastGood];
            String slice = text.substring(start, lastGood);
            byte[] exact = slice.getBytes(StandardCharsets.UTF_8);
            if (exact.length != byteEnd - byteStart) {
                throw new IllegalStateException("chunk byte mapping mismatch");
            }
            Entropy entropy = entropy(exact, slice);
            chunks.add(new Chunk(chunks.size(), start, lastGood, byteStart, byteEnd,
                    AiXSourceSnapshot.sha(exact), entropy, slice));
            if (chunks.size() > MAX_CHUNKS) {
                throw new IllegalArgumentException("chunk count exceeds budget");
            }
            start = lastGood;
        }
        if (text.isEmpty()) {
            chunks.add(new Chunk(0, 0, 0, 0, 0, AiXSourceSnapshot.sha(new byte[0]), entropy(new byte[0], ""), ""));
        }
        verifyCoverage(source, chunks);
        String root = hashCanonical(List.of(
                "aix.java.mechanical-plan/1", source.name(), source.size(), source.sha256(),
                limits.chunkBytes(), limits.chunkChars(),
                chunks.stream().map(c -> List.of(c.ordinal(), c.byteStart(), c.byteEnd(), c.sha256(), c.entropy().root())).toList()));
        return new Plan("aix.java.mechanical-plan/1", source.name(), source.size(), source.sha256(), limits, chunks, root);
    }

    public static void verifyCoverage(AiXSourceSnapshot source, List<Chunk> chunks) {
        if (chunks.isEmpty()) { throw new IllegalArgumentException("no chunks"); }
        int expectedByte = 0;
        int expectedChar = 0;
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            if (chunk.ordinal() != i || chunk.byteStart() != expectedByte || chunk.charStart() != expectedChar) {
                throw new IllegalArgumentException("chunk coverage gap/overlap");
            }
            byte[] exact = chunk.text().getBytes(StandardCharsets.UTF_8);
            if (exact.length != chunk.byteLength() || !AiXSourceSnapshot.sha(exact).equals(chunk.sha256())) {
                throw new IllegalArgumentException("chunk hash mismatch");
            }
            expectedByte = chunk.byteEnd();
            expectedChar = chunk.charEnd();
        }
        if (expectedByte != source.size() || expectedChar != source.text().length()) {
            throw new IllegalArgumentException("incomplete source coverage");
        }
    }

    static Entropy entropy(byte[] bytes, String text) {
        Matcher tokens = TOKEN.matcher(text);
        int tokenCount = 0;
        Set<String> unique = new LinkedHashSet<>();
        while (tokens.find()) {
            tokenCount++;
            unique.add(tokens.group());
        }
        int uniqueCount = unique.size();
        int uniqueness = tokenCount == 0 ? 0 : (int) ((long) uniqueCount * 1_000_000L / tokenCount);
        int repetition = tokenCount == 0 ? 0 : 1_000_000 - uniqueness;
        List<String> symbols = captures(SYMBOL, text);
        List<String> events = captures(EVENT, text);
        Map<String, Integer> chrome = new TreeMap<>();
        for (String api : CHROME_AI) {
            int count = countWord(text, api);
            if (count > 0) { chrome.put(api, count); }
        }
        long microbits = shannonMicrobits(bytes);
        String root = hashCanonical(List.of(tokenCount, uniqueCount, uniqueness, repetition, microbits, symbols, events, chrome));
        return new Entropy(tokenCount, uniqueCount, uniqueness, repetition, microbits, symbols, events, chrome, root);
    }

    private static List<String> captures(Pattern pattern, String text) {
        Set<String> result = new java.util.TreeSet<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String value = matcher.group(i);
                if (value != null && !value.isBlank()) { result.add(value); break; }
            }
        }
        return List.copyOf(result);
    }

    private static int countWord(String text, String word) {
        Matcher matcher = Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(text);
        int count = 0;
        while (matcher.find()) { count++; }
        return count;
    }

    private static long shannonMicrobits(byte[] bytes) {
        if (bytes.length == 0) { return 0; }
        int[] counts = new int[256];
        for (byte value : bytes) { counts[value & 255]++; }
        double bits = 0.0;
        for (int count : counts) {
            if (count == 0) { continue; }
            double p = (double) count / bytes.length;
            bits -= p * (Math.log(p) / Math.log(2.0));
        }
        return Math.round(bits * 1_000_000.0);
    }

    static String hashCanonical(Object value) {
        return AiXSourceSnapshot.sha(canonical(value).getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    static String canonical(Object value) {
        if (value == null) { return "null"; }
        if (value instanceof String s) { return s.length() + ":" + s; }
        if (value instanceof Number || value instanceof Boolean) { return String.valueOf(value); }
        if (value instanceof Map<?, ?> map) {
            StringBuilder out = new StringBuilder("{");
            new TreeMap<>((Map<String, Object>) map).forEach((key, item) ->
                    out.append(canonical(key)).append('=').append(canonical(item)).append(';'));
            return out.append('}').toString();
        }
        if (value instanceof Iterable<?> items) {
            StringBuilder out = new StringBuilder("[");
            for (Object item : items) { out.append(canonical(item)).append(';'); }
            return out.append(']').toString();
        }
        if (value.getClass().isArray()) { return canonical(Arrays.asList((Object[]) value)); }
        return canonical(String.valueOf(value));
    }

    static int[] byteOffsets(String text) {
        int[] offsets = new int[text.length() + 1];
        Arrays.fill(offsets, -1);
        int bytes = 0;
        for (int i = 0; i < text.length();) {
            offsets[i] = bytes;
            int cp = text.codePointAt(i);
            int width = Character.charCount(cp);
            bytes += cp <= 0x7f ? 1 : cp <= 0x7ff ? 2 : cp <= 0xffff ? 3 : 4;
            i += width;
            offsets[i] = bytes;
        }
        offsets[text.length()] = bytes;
        return offsets;
    }
}
