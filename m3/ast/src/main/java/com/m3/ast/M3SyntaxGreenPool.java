// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Immutable-content, position-free syntax greens over the existing M3SyntaxTree
 * occurrence plane. Greens are interned by a structural digest nominated by
 * M3SyntaxIndex, then *verified* by exact grammar, kind, field, flags, child
 * handles, gap lengths and retained gap bytes. A SHA collision cannot merge
 * different syntax. No foreign parser or additional source owner is installed.
 *
 * <p>Every green occupies aligned primitive columns, flat child-ID and gap-width
 * arrays, and one append-only literal byte arena. The only retained source text
 * is the node-local gap text not covered by its children; whitespace, comments,
 * skipped text, punctuation and malformed syntax therefore round-trip exactly.
 * The projection acts as a temporary "red" coordinate view; greens contain no
 * parent pointer or absolute source position. A single pool can be reused across
 * source versions and unrelated files while remaining bounded by caller budgets.
 */
public final class M3SyntaxGreenPool {
    private static final int DIGEST_BYTES = 32;
    private final int maxGreens, maxLiteralBytes;
    private int greens, childSize, gapSize, literalSize;
    private String[] grammar = new String[16], kind = new String[16], field = new String[16];
    private int[] flags = new int[16], childStart = new int[16], childCount = new int[16];
    private int[] gapStart = new int[16], literalStart = new int[16], fullWidth = new int[16];
    private byte[] digests = new byte[16 * DIGEST_BYTES], literals = new byte[64];
    private int[] children = new int[32], gaps = new int[32], buckets = new int[32];

    public M3SyntaxGreenPool(int maxGreens, int maxLiteralBytes) {
        if (maxGreens < 1 || maxLiteralBytes < 0)
            throw new IllegalArgumentException("invalid green or retained-byte budget");
        this.maxGreens = maxGreens;
        this.maxLiteralBytes = maxLiteralBytes;
    }
    public M3SyntaxGreenPool() { this(1_000_000, 256 << 20); }

    public static final class Projection {
        private final String sourceSha256, projectionSha256;
        private final int[] occurrenceGreens;
        private Projection(M3SyntaxTree tree, int[] greens) {
            sourceSha256 = tree.sourceSha256();
            projectionSha256 = tree.projectionSha256();
            occurrenceGreens = greens;
        }
        public String sourceSha256() { return sourceSha256; }
        public String projectionSha256() { return projectionSha256; }
        public int occurrences() { return occurrenceGreens.length; }
        public int greenAt(int occurrence) {
            return occurrenceGreens[Objects.checkIndex(occurrence, occurrenceGreens.length)];
        }
        public int rootGreen() { return greenAt(0); }
        public int[] greenHandles() { return occurrenceGreens.clone(); }
        public void requireTree(M3SyntaxTree tree) {
            if (!projectionSha256.equals(Objects.requireNonNull(tree, "tree").projectionSha256()))
                throw new IllegalStateException("green occurrence projection does not match source");
        }
    }

    /** Import an independently parsed tree; all retained green IDs remain stable. */
    public synchronized Projection intern(M3SyntaxTree tree) {
        Objects.requireNonNull(tree, "tree");
        M3SyntaxIndex index = new M3SyntaxIndex(tree);
        byte[] source = tree.sourceBytes();
        int[] occurrence = new int[tree.size()];
        // Parent IDs are resolved only after all children: no recursion or red nodes.
        for (int node = tree.size() - 1; node >= 0; node--) {
            byte[] digest = HexFormat.of().parseHex(index.subtreeSha256(node));
            int slot = slot(digest);
            int current = buckets[slot] - 1;
            while (current >= 0) {
                if (Arrays.equals(digests, current * DIGEST_BYTES,
                        (current + 1) * DIGEST_BYTES, digest, 0, DIGEST_BYTES)
                        && exact(current, tree, node, occurrence, source)) break;
                slot = (slot + 1) & (buckets.length - 1);
                current = buckets[slot] - 1;
            }
            if (current >= 0) {
                occurrence[node] = current;
                continue;
            }
            if (greens >= maxGreens) throw new IllegalStateException("green node budget exceeded");
            // Grow *before* selecting a slot: rebuilding a table changes the slot.
            if ((long) (greens + 1) * 4 >= (long) buckets.length * 3) {
                rehash(buckets.length * 2);
                slot = emptySlot(digest);
            }
            int id = append(tree, node, occurrence, source, digest);
            buckets[slot] = id + 1;
            occurrence[node] = id;
        }
        return new Projection(tree, occurrence);
    }

    public synchronized int uniqueGreens() { return greens; }
    public synchronized int retainedLiteralBytes() { return literalSize; }
    public synchronized int childReferences() { return childSize; }
    public synchronized int width(int green) { return fullWidth[checked(green)]; }
    public synchronized int childCount(int green) { return childCount[checked(green)]; }
    public synchronized int childGreen(int green, int offset) {
        int g = checked(green);
        return children[childStart[g] + Objects.checkIndex(offset, childCount[g])];
    }
    public synchronized String digestSha256(int green) {
        int at = checked(green) * DIGEST_BYTES;
        return HexFormat.of().formatHex(digests, at, at + DIGEST_BYTES);
    }
    /** Bitwise exact sharing inside this pool, never source-language semantic proof. */
    public synchronized boolean shared(int first, int second) {
        return checked(first) == checked(second);
    }

    /** Iterative full-fidelity reconstruction from the green DAG alone. */
    public synchronized byte[] render(int root, int maximumBytes) {
        int id = checked(root);
        if (maximumBytes < 0 || fullWidth[id] > maximumBytes)
            throw new IllegalArgumentException("render exceeds caller byte budget");
        byte[] result = new byte[fullWidth[id]];
        int[] stack = new int[16], next = new int[16], literalUsed = new int[16];
        int depth = 1, cursor = 0;
        stack[0] = id;
        while (depth > 0) {
            int at = depth - 1, g = stack[at], slot = next[at];
            if (slot > childCount[g]) { depth--; continue; }
            int length = gaps[gapStart[g] + slot];
            System.arraycopy(literals, literalStart[g] + literalUsed[at], result, cursor, length);
            cursor += length;
            literalUsed[at] += length;
            next[at]++;
            if (slot < childCount[g]) {
                if (depth == stack.length) {
                    stack = Arrays.copyOf(stack, stack.length * 2);
                    next = Arrays.copyOf(next, next.length * 2);
                    literalUsed = Arrays.copyOf(literalUsed, literalUsed.length * 2);
                }
                stack[depth] = children[childStart[g] + slot];
                next[depth] = 0;
                literalUsed[depth] = 0;
                depth++;
            }
        }
        if (cursor != result.length) throw new IllegalStateException("green width invariant violated");
        return result;
    }

    private int checked(int id) { return Objects.checkIndex(id, greens); }
    private boolean exact(int g, M3SyntaxTree tree, int node, int[] ids, byte[] source) {
        if (!grammar[g].equals(tree.grammar()) || !kind[g].equals(tree.kind(node))
                || !field[g].equals(tree.field(node)) || flags[g] != tree.flags(node)
                || fullWidth[g] != tree.endByte(node) - tree.startByte(node)) return false;
        int at = tree.startByte(node), entry = 0, literalCursor = literalStart[g];
        for (int child = tree.firstChild(node); child >= 0; child = tree.nextSibling(child)) {
            if (entry >= childCount[g] || ids[child] != children[childStart[g] + entry])
                return false;
            int gap = tree.startByte(child) - at;
            if (gap != gaps[gapStart[g] + entry] || !equalBytes(source, at, literals, literalCursor, gap))
                return false;
            literalCursor += gap;
            at = tree.endByte(child);
            entry++;
        }
        int suffix = tree.endByte(node) - at;
        return entry == childCount[g] && suffix == gaps[gapStart[g] + entry]
                && equalBytes(source, at, literals, literalCursor, suffix);
    }
    private int append(M3SyntaxTree tree, int node, int[] ids, byte[] source, byte[] digest) {
        int id = greens;
        int childTotal = 0, localBytes = 0, at = tree.startByte(node);
        for (int child = tree.firstChild(node); child >= 0; child = tree.nextSibling(child)) {
            localBytes = Math.addExact(localBytes, tree.startByte(child) - at);
            childTotal++;
            at = tree.endByte(child);
        }
        localBytes = Math.addExact(localBytes, tree.endByte(node) - at);
        if (localBytes > maxLiteralBytes - literalSize)
            throw new IllegalStateException("green source-literal budget exceeded");
        ensureGreens(greens + 1);
        ensureChildren(Math.addExact(childSize, childTotal));
        ensureGaps(Math.addExact(gapSize, childTotal + 1));
        ensureLiterals(Math.addExact(literalSize, localBytes));
        grammar[id] = tree.grammar();
        kind[id] = tree.kind(node);
        field[id] = tree.field(node);
        flags[id] = tree.flags(node);
        childStart[id] = childSize;
        childCount[id] = childTotal;
        gapStart[id] = gapSize;
        literalStart[id] = literalSize;
        fullWidth[id] = tree.endByte(node) - tree.startByte(node);
        System.arraycopy(digest, 0, digests, id * DIGEST_BYTES, DIGEST_BYTES);
        at = tree.startByte(node);
        for (int child = tree.firstChild(node); child >= 0; child = tree.nextSibling(child)) {
            int len = tree.startByte(child) - at;
            gaps[gapSize++] = len;
            System.arraycopy(source, at, literals, literalSize, len);
            literalSize += len;
            children[childSize++] = ids[child];
            at = tree.endByte(child);
        }
        int trailing = tree.endByte(node) - at;
        gaps[gapSize++] = trailing;
        System.arraycopy(source, at, literals, literalSize, trailing);
        literalSize += trailing;
        greens++;
        return id;
    }
    private static boolean equalBytes(byte[] source, int start, byte[] stored, int offset, int length) {
        return Arrays.equals(source, start, start + length, stored, offset, offset + length);
    }
    private int slot(byte[] d) { return bucket(d, buckets.length - 1); }
    private int emptySlot(byte[] d) {
        int slot = slot(d);
        while (buckets[slot] != 0) slot = (slot + 1) & (buckets.length - 1);
        return slot;
    }
    private void rehash(int count) {
        int[] table = new int[count];
        for (int g = 0; g < greens; g++) {
            int at = bucket(digests, g * DIGEST_BYTES, count - 1);
            while (table[at] != 0) at = (at + 1) & (count - 1);
            table[at] = g + 1;
        }
        buckets = table;
    }
    private static int bucket(byte[] digest, int mask) { return bucket(digest, 0, mask); }
    private static int bucket(byte[] digest, int from, int mask) {
        long x = 0x9e3779b97f4a7c15L;
        for (int i = 0; i < 32; i++) {
            x ^= Byte.toUnsignedLong(digest[from + i]);
            x *= 0x100000001b3L;
        }
        x ^= x >>> 33;
        return (int) x & mask;
    }
    private void ensureGreens(int count) {
        if (count <= grammar.length) return;
        int n = Math.max(count, Math.addExact(grammar.length, grammar.length >>> 1));
        grammar = Arrays.copyOf(grammar, n);
        kind = Arrays.copyOf(kind, n);
        field = Arrays.copyOf(field, n);
        flags = Arrays.copyOf(flags, n);
        childStart = Arrays.copyOf(childStart, n);
        childCount = Arrays.copyOf(childCount, n);
        gapStart = Arrays.copyOf(gapStart, n);
        literalStart = Arrays.copyOf(literalStart, n);
        fullWidth = Arrays.copyOf(fullWidth, n);
        digests = Arrays.copyOf(digests, Math.multiplyExact(n, DIGEST_BYTES));
    }
    private void ensureChildren(int n) {
        if (n > children.length) children = Arrays.copyOf(children, Math.max(n, Math.addExact(children.length, children.length >>> 1)));
    }
    private void ensureGaps(int n) {
        if (n > gaps.length) gaps = Arrays.copyOf(gaps, Math.max(n, Math.addExact(gaps.length, gaps.length >>> 1)));
    }
    private void ensureLiterals(int n) {
        if (n > literals.length) literals = Arrays.copyOf(literals, Math.max(n, Math.addExact(literals.length, literals.length >>> 1)));
    }
}
