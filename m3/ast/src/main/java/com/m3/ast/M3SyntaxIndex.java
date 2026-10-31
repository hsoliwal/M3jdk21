// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Optional indexed queries, position-independent syntax-subtree fingerprints,
 * and source-sealed semantic sidecars for the existing M3SyntaxTree owner.
 * No new syntax parser, AST graph, per-node object, or rewrite authority.
 *
 * <p>The bottom-up fingerprint hashes the original source fragments not covered
 * by child nodes, followed by each child's position-independent fingerprint.
 * A byte participates in the digest of its deepest owning node only; every
 * parent hashes 32-byte child digests instead of repeatedly scanning source.
 * Fields, error/missing states and punctuation/trivia in gaps are preserved.
 * Fingerprints nominate reuse; they cannot prove semantic or ABI equivalence.
 */
public final class M3SyntaxIndex {
    private static final int HASH_BYTES = 32;
    private final M3SyntaxTree tree;
    private final byte[] digests;

    public M3SyntaxIndex(M3SyntaxTree tree) {
        this.tree = Objects.requireNonNull(tree, "tree");
        int size = tree.size();
        this.digests = new byte[Math.multiplyExact(size, HASH_BYTES)];
        byte[] source = tree.sourceBytes(); // one transient source read; no retained duplicate
        MessageDigest hash = sha256(); // builder-confined cursor, one digest for every row
        for (int node = size - 1; node >= 0; node--) {
            hash.reset();
            text(hash, "m3.syntax.subtree/1");
            text(hash, tree.grammar());
            text(hash, tree.kind(node));
            text(hash, tree.field(node));
            integer(hash, tree.flags(node));
            int cursor = tree.startByte(node);
            int count = 0;
            for (int child = tree.firstChild(node); child >= 0; child = tree.nextSibling(child)) {
                int childStart = tree.startByte(child), childEnd = tree.endByte(child);
                if (childStart < cursor || childEnd < childStart || childEnd > tree.endByte(node)) {
                    throw new IllegalArgumentException("non-nested source spans at syntax node " + child);
                }
                chunk(hash, source, cursor, childStart);
                hash.update((byte) 0x7f); // explicit child boundary
                hash.update(digests, HASH_BYTES * child, HASH_BYTES);
                cursor = childEnd;
                count++;
            }
            chunk(hash, source, cursor, tree.endByte(node));
            integer(hash, count);
            // Write the exact V1 digest into its existing primitive sidecar; no per-node byte[32].
            try {
                hash.digest(digests, HASH_BYTES * node, HASH_BYTES);
            } catch (DigestException impossible) {
                throw new AssertionError(impossible);
            }
        }
    }

    public M3SyntaxTree tree() { return tree; }
    public String subtreeSha256(int node) {
        Objects.checkIndex(node, tree.size());
        return HexFormat.of().formatHex(digests, HASH_BYTES * node, HASH_BYTES * (node + 1));
    }
    public String rootSha256() { return subtreeSha256(0); }
    public boolean sameCandidateSubtree(int node, M3SyntaxIndex other, int otherNode) {
        Objects.requireNonNull(other, "other");
        Objects.checkIndex(node, tree.size());
        Objects.checkIndex(otherNode, other.tree.size());
        // Hash match is a non-authoritative reuse *candidate*, not semantic equivalence.
        return Arrays.equals(digests, node * HASH_BYTES, (node + 1) * HASH_BYTES,
                other.digests, otherNode * HASH_BYTES, (otherNode + 1) * HASH_BYTES);
    }
    public int firstChildWithField(int parent, String fieldName) {
        Objects.checkIndex(parent, tree.size());
        Objects.requireNonNull(fieldName, "fieldName");
        for (int child = tree.firstChild(parent); child >= 0; child = tree.nextSibling(child)) {
            if (tree.field(child).equals(fieldName)) return child;
        }
        return -1;
    }
    /** Source-order occurrence handles, not per-node heap instances. */
    public int[] nodesOfKind(String grammarKind) {
        Objects.requireNonNull(grammarKind, "grammarKind");
        int[] result = new int[Math.min(16, tree.size())];
        int count = 0;
        for (int i = 0; i < tree.size(); i++) {
            if (grammarKind.equals(tree.kind(i))) {
                if (count == result.length) result = Arrays.copyOf(result, count + (count >>> 1) + 1);
                result[count++] = i;
            }
        }
        return Arrays.copyOf(result, count);
    }

    /**
     * Metadata never alters syntax bytes or claims that different language
     * binding types have equivalent meanings. Keys are provider-namespaced.
     * The sidecar refuses a different syntax projection / source generation.
     */
    public static final class Facts {
        private final String projectionSha256;
        private final int[] nodes, nameIds, valueIds, begins, counts;
        private final String[] names, values;
        private Facts(String seal, int[] nodes, int[] nameIds, int[] valueIds,
                      String[] names, String[] values, int[] begins, int[] counts) {
            this.projectionSha256 = seal;
            this.nodes = nodes; this.nameIds = nameIds; this.valueIds = valueIds;
            this.names = names; this.values = values; this.begins = begins; this.counts = counts;
        }
        public int size() { return nodes.length; }
        public String projectionSha256() { return projectionSha256; }
        public void requireSource(M3SyntaxTree expected) {
            if (!projectionSha256.equals(Objects.requireNonNull(expected, "expected").projectionSha256()))
                throw new IllegalStateException("stale semantic attribution or different parser projection");
        }
        public int count(int node) { return counts[Objects.checkIndex(node, begins.length)]; }
        public String key(int node, int ordinal) {
            int at = row(node, ordinal);
            return names[nameIds[at]];
        }
        public String value(int node, int ordinal) {
            int at = row(node, ordinal);
            return values[valueIds[at]];
        }
        private int row(int node, int ordinal) {
            Objects.checkIndex(ordinal, count(node));
            return begins[node] + ordinal;
        }
        public String first(int node, String name) {
            Objects.requireNonNull(name, "name");
            int from = begins[Objects.checkIndex(node, begins.length)];
            for (int i = 0; i < counts[node]; i++) {
                int row = from + i;
                if (names[nameIds[row]].equals(name)) return values[valueIds[row]];
            }
            return null;
        }
    }
    public static FactBuilder facts(M3SyntaxTree tree) { return new FactBuilder(tree); }
    public static final class FactBuilder {
        private final M3SyntaxTree tree;
        private final List<Fact> facts = new ArrayList<>();
        private final Map<String, Integer> keys = new LinkedHashMap<>();
        private final Map<String, Integer> values = new LinkedHashMap<>();
        private boolean frozen;
        private record Fact(int node, int key, int value, int insertion) { }
        private FactBuilder(M3SyntaxTree tree) { this.tree = Objects.requireNonNull(tree, "tree"); }
        public FactBuilder add(int node, String provider, String property, String value) {
            if (frozen) throw new IllegalStateException("metadata already frozen");
            Objects.checkIndex(node, tree.size());
            String qualified = component(provider) + ":" + component(property);
            String stored = Objects.requireNonNull(value, "value");
            int key = keys.computeIfAbsent(qualified, ignored -> keys.size());
            int val = values.computeIfAbsent(stored, ignored -> values.size());
            facts.add(new Fact(node, key, val, facts.size()));
            return this;
        }
        public Facts freeze() {
            if (frozen) throw new IllegalStateException("metadata already frozen");
            frozen = true;
            facts.sort(Comparator.comparingInt(Fact::node).thenComparingInt(Fact::insertion));
            int[] nodes = new int[facts.size()], names = new int[facts.size()], valuesAt = new int[facts.size()];
            int[] begins = new int[tree.size()], counts = new int[tree.size()];
            for (int i = 0; i < facts.size(); i++) {
                Fact fact = facts.get(i);
                nodes[i] = fact.node; names[i] = fact.key; valuesAt[i] = fact.value;
                counts[fact.node]++;
            }
            int next = 0;
            for (int i = 0; i < begins.length; i++) {
                begins[i] = next;
                next = Math.addExact(next, counts[i]);
            }
            return new Facts(tree.projectionSha256(), nodes, names, valuesAt,
                    keys.keySet().toArray(String[]::new), values.keySet().toArray(String[]::new),
                    begins, counts);
        }
        private static String component(String text) {
            if (text == null || text.isBlank() || text.indexOf(':') >= 0)
                throw new IllegalArgumentException("invalid semantic namespace/key");
            return text;
        }
    }

    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static void chunk(MessageDigest hash, byte[] bytes, int start, int end) {
        integer(hash, end - start);
        hash.update(bytes, start, end - start);
    }
    private static void text(MessageDigest hash, String text) {
        integer(hash, text.length());
        // Non-lossy framing for admitted Java strings including unpaired surrogates.
        for (int i = 0; i < text.length(); i++) {
            int unit = text.charAt(i);
            hash.update((byte) (unit >>> 8)); hash.update((byte) unit);
        }
    }
    private static void integer(MessageDigest hash, int value) {
        hash.update((byte) (value >>> 24)); hash.update((byte) (value >>> 16));
        hash.update((byte) (value >>> 8)); hash.update((byte) value);
    }
}
