// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.text.search;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Prepared Aho-Corasick search over the exact UTF-16 units of an M3JDK String.
 * The retained automaton is composed of flat primitive lanes and state offsets;
 * construction-only nodes and maps do not remain in the prepared index.
 *
 * <p>Hits are emitted in increasing end-offset order. At the same end-offset,
 * longer literals precede shorter suffix literals; duplicate literal IDs
 * retain their original input order. Empty literals are intentionally refused.
 * This is a literal operator; regex constructs remain owned by Pattern/Matcher.
 */
public final class MultiLiteralSearch {
    public static final int MAX_PATTERNS = 4_096;
    public static final int MAX_PATTERN_UNITS = 1_000_000;
    public static final int MAX_TEXT_UNITS = 16 * 1024 * 1024;
    public static final int MAX_HITS = 1_000_000;

    private MultiLiteralSearch() { }

    public record Hit(int patternId, int start, int end) { }

    /** A bounded, immutable primitive-lane result; Hit objects are only API projections. */
    public static final class Result {
        private final int[] ids;
        private final int[] starts;
        private final int[] ends;
        private final boolean complete;

        private Result(int[] ids, int[] starts, int[] ends, int size, boolean complete) {
            this.ids = Arrays.copyOf(ids, size);
            this.starts = Arrays.copyOf(starts, size);
            this.ends = Arrays.copyOf(ends, size);
            this.complete = complete;
        }

        public int size() { return ids.length; }
        public boolean complete() { return complete; }
        public Hit hit(int index) { return new Hit(ids[index], starts[index], ends[index]); }
        public List<Hit> hits() {
            List<Hit> result = new ArrayList<>(size());
            for (int i = 0; i < size(); i++) result.add(hit(i));
            return List.copyOf(result);
        }
    }

    private static final class Node {
        private final TreeMap<Character, Integer> edges = new TreeMap<>();
        private final List<Integer> terminalIds = new ArrayList<>();
        private int fail;
        private int outputLink;
    }

    /** Share this immutable plan between independent searches; it retains no source text. */
    public static final class Prepared {
        private final String[] patterns;
        private final int[] failure;
        private final int[] outputLinks;
        private final int[] edgeStarts;
        private final char[] edgeKeys;
        private final int[] edgeTargets;
        private final int[] terminalStarts;
        private final int[] terminalIds;
        private final String sha256;

        private Prepared(String[] patterns, List<Node> states, Runnable checkpoint) {
            this.patterns = patterns;
            failure = new int[states.size()];
            outputLinks = new int[states.size()];
            edgeStarts = new int[states.size() + 1];
            terminalStarts = new int[states.size() + 1];
            for (int i = 0; i < states.size(); i++) {
                if ((i & 4095) == 0) checkpoint.run();
                Node node = states.get(i);
                failure[i] = node.fail;
                outputLinks[i] = node.outputLink;
                edgeStarts[i + 1] = Math.addExact(edgeStarts[i], node.edges.size());
                terminalStarts[i + 1] = Math.addExact(terminalStarts[i], node.terminalIds.size());
            }
            edgeKeys = new char[edgeStarts[states.size()]];
            edgeTargets = new int[edgeKeys.length];
            terminalIds = new int[terminalStarts[states.size()]];
            for (int i = 0; i < states.size(); i++) {
                if ((i & 4095) == 0) checkpoint.run();
                Node node = states.get(i);
                int cursor = edgeStarts[i];
                for (Map.Entry<Character, Integer> edge : node.edges.entrySet()) {
                    edgeKeys[cursor] = edge.getKey();
                    edgeTargets[cursor++] = edge.getValue();
                }
                for (int j = 0; j < node.terminalIds.size(); j++) {
                    terminalIds[terminalStarts[i] + j] = node.terminalIds.get(j);
                }
            }
            MessageDigest hash = digest();
            number(hash, patterns.length);
            for (String pattern : patterns) {
                checkpoint.run();
                number(hash, pattern.length());
                for (int i = 0; i < pattern.length(); i++) {
                    if ((i & 4095) == 0) checkpoint.run();
                    number(hash, pattern.charAt(i));
                }
            }
            checkpoint.run();
            sha256 = HexFormat.of().formatHex(hash.digest());
        }

        public int patternCount() { return patterns.length; }
        public int stateCount() { return failure.length; }
        public String sha256() { return sha256; }

        /** Includes one additional observation when the hit cap is reached. */
        public Result find(String text, int maxHits) { return find(text, maxHits, () -> { }); }

        /**
         * The checkpoint runs periodically and before returning; it may throw
         * to implement a borrowed ProgressMonitor's cancellation policy.
         */
        public Result find(String text, int maxHits, Runnable checkpoint) {
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(checkpoint, "checkpoint");
            if (maxHits < 0 || maxHits > MAX_HITS) throw new IllegalArgumentException("hit budget");
            if (text.length() > MAX_TEXT_UNITS) throw new IllegalArgumentException("text UTF-16 budget");
            int[] ids = new int[Math.min(maxHits, 16)];
            int[] starts = new int[ids.length], ends = new int[ids.length];
            int count = 0, state = 0;
            checkpoint.run();
            for (int at = 0; at < text.length(); at++) {
                if ((at & 4095) == 0) checkpoint.run();
                char unit = text.charAt(at);
                int target = transition(state, unit);
                while (target < 0 && state != 0) {
                    state = failure[state];
                    target = transition(state, unit);
                }
                state = target < 0 ? 0 : target;
                int output = state;
                while (output != 0) {
                    for (int i = terminalStarts[output]; i < terminalStarts[output + 1]; i++) {
                        if (count == maxHits) {
                            checkpoint.run();
                            return new Result(ids, starts, ends, count, false);
                        }
                        if (count == ids.length) {
                            int capacity = (int) Math.min((long) maxHits, 2L * count + 1);
                            ids = Arrays.copyOf(ids, capacity);
                            starts = Arrays.copyOf(starts, capacity);
                            ends = Arrays.copyOf(ends, capacity);
                        }
                        int id = terminalIds[i];
                        ids[count] = id;
                        starts[count] = at + 1 - patterns[id].length();
                        ends[count++] = at + 1;
                    }
                    output = outputLinks[output];
                }
            }
            checkpoint.run();
            return new Result(ids, starts, ends, count, true);
        }

        private int transition(int state, char unit) {
            int at = Arrays.binarySearch(edgeKeys, edgeStarts[state], edgeStarts[state + 1], unit);
            return at < 0 ? -1 : edgeTargets[at];
        }
    }

    public static Prepared prepare(List<String> literals) {
        return prepare(literals, () -> { });
    }

    /** The checkpoint also covers trie assembly, failure links, and plan hashing. */
    public static Prepared prepare(List<String> literals, Runnable checkpoint) {
        Objects.requireNonNull(literals, "literals");
        Objects.requireNonNull(checkpoint, "checkpoint");
        if (literals.size() > MAX_PATTERNS) throw new IllegalArgumentException("pattern count budget");
        checkpoint.run();
        String[] patterns = new String[literals.size()];
        List<Node> states = new ArrayList<>();
        states.add(new Node());
        int totalUnits = 0;
        for (int id = 0; id < patterns.length; id++) {
            if ((id & 255) == 0) checkpoint.run();
            String literal = Objects.requireNonNull(literals.get(id), "literal");
            if (literal.isEmpty()) throw new IllegalArgumentException("empty literal");
            totalUnits = Math.addExact(totalUnits, literal.length());
            if (totalUnits > MAX_PATTERN_UNITS) throw new IllegalArgumentException("pattern UTF-16 budget");
            patterns[id] = literal;
            int state = 0;
            for (int i = 0; i < literal.length(); i++) {
                if ((i & 4095) == 0) checkpoint.run();
                char unit = literal.charAt(i);
                Integer target = states.get(state).edges.get(unit);
                if (target == null) {
                    if (states.size() > MAX_PATTERN_UNITS) throw new IllegalArgumentException("state budget");
                    target = states.size();
                    states.add(new Node());
                    states.get(state).edges.put(unit, target);
                }
                state = target;
            }
            states.get(state).terminalIds.add(id);
        }
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int child : states.get(0).edges.values()) queue.addLast(child);
        int visited = 0;
        while (!queue.isEmpty()) {
            if ((visited++ & 4095) == 0) checkpoint.run();
            int state = queue.removeFirst();
            Node parent = states.get(state);
            for (Map.Entry<Character, Integer> edge : parent.edges.entrySet()) {
                char unit = edge.getKey();
                int child = edge.getValue();
                int ancestor = parent.fail;
                while (ancestor != 0 && !states.get(ancestor).edges.containsKey(unit)) {
                    ancestor = states.get(ancestor).fail;
                }
                Integer candidate = states.get(ancestor).edges.get(unit);
                Node destination = states.get(child);
                destination.fail = candidate == null ? 0 : candidate;
                Node suffix = states.get(destination.fail);
                destination.outputLink = suffix.terminalIds.isEmpty()
                        ? suffix.outputLink : destination.fail;
                queue.addLast(child);
            }
        }
        checkpoint.run();
        return new Prepared(patterns, states, checkpoint);
    }

    private static MessageDigest digest() {
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            hash.update("m3-multi-literal-utf16-v1".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            return hash;
        } catch (NoSuchAlgorithmException failure) {
            throw new AssertionError(failure);
        }
    }

    private static void number(MessageDigest hash, int value) {
        hash.update((byte) (value >>> 24));
        hash.update((byte) (value >>> 16));
        hash.update((byte) (value >>> 8));
        hash.update((byte) value);
    }
}
