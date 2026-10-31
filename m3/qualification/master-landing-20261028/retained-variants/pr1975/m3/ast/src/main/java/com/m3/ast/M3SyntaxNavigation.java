// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable navigation directory for the existing primitive M3SyntaxTree.
 *
 * <p>Direct-child handles are grouped into one flat int[] arena. Parents,
 * grammar-native field/kind strings and source bytes remain exclusively owned
 * by M3SyntaxTree; no retained foreign node, red tree or duplicate AST is built.
 * Preorder subtree exits enable iteration over complete semantic atoms without
 * recursively visiting Java objects. Binary search over ordered direct-child
 * offsets makes byte lookup O(depth * log(maxSiblingCount)) instead of a scan
 * over every sibling at every ancestor.
 *
 * <p>Missing/error nodes with zero width never contain any byte. Equal-position
 * missing nodes can precede a real token and must not hide it. All ranges use
 * original half-open physical source-byte coordinates (not UTF-16 character
 * positions); source generation and projection are SHA-256 sealed.
 */
public final class M3SyntaxNavigation {
    private final M3SyntaxTree tree;
    private final int[] first, counts, children, subtreeExit;
    private final String sourceSha256, projectionSha256;

    public M3SyntaxNavigation(M3SyntaxTree tree) {
        this.tree = Objects.requireNonNull(tree, "tree");
        int n = tree.size();
        if (n < 1) throw new IllegalArgumentException("empty syntax projection");
        sourceSha256 = tree.sourceSha256();
        projectionSha256 = tree.projectionSha256();
        first = new int[n];
        counts = new int[n];
        subtreeExit = new int[n];
        children = new int[n - 1];

        if (tree.parent(0) != -1) throw new IllegalArgumentException("root has parent");
        for (int i = 1; i < n; i++) {
            int p = tree.parent(i);
            if (p < 0 || p >= i) throw new IllegalArgumentException("not a preorder tree");
            counts[p] = Math.addExact(counts[p], 1);
        }
        int running = 0;
        for (int i = 0; i < n; i++) {
            first[i] = running;
            running = Math.addExact(running, counts[i]);
            subtreeExit[i] = i + 1;
        }
        if (running != children.length) throw new AssertionError("child directory incomplete");
        int[] cursors = first.clone();
        for (int i = 1; i < n; i++) {
            int p = tree.parent(i);
            children[cursors[p]++] = i;
        }
        // Every subtree of a valid preorder projection is an ordinal interval.
        // Reverse propagation avoids Java recursion for deeply nested grammars.
        for (int i = n - 1; i > 0; i--) {
            int p = tree.parent(i);
            if (subtreeExit[p] < subtreeExit[i]) subtreeExit[p] = subtreeExit[i];
        }
    }

    public M3SyntaxTree tree() { return tree; }
    public int nodes() { return subtreeExit.length; }
    public String sourceSha256() { return sourceSha256; }
    public String projectionSha256() { return projectionSha256; }

    public void requireTree(M3SyntaxTree expected) {
        if (!projectionSha256.equals(Objects.requireNonNull(expected, "expected").projectionSha256()))
            throw new IllegalStateException("stale syntax navigation directory");
    }
    /** End-exclusive preorder ordinal after this node's complete descendant range. */
    public int subtreeExit(int node) { return subtreeExit[Objects.checkIndex(node, nodes())]; }
    /** True for the node itself and all its descendants. */
    public boolean containsNode(int ancestor, int descendant) {
        Objects.checkIndex(ancestor, nodes());
        Objects.checkIndex(descendant, nodes());
        return ancestor <= descendant && descendant < subtreeExit[ancestor];
    }
    public int directChildren(int parent) { return counts[Objects.checkIndex(parent, nodes())]; }
    /** Child occurrence handle; does not allocate a node wrapper. */
    public int directChild(int parent, int ordinal) {
        Objects.checkIndex(parent, nodes());
        Objects.checkIndex(ordinal, counts[parent]);
        return children[first[parent] + ordinal];
    }
    /** Grammar-specific role names are preserved, not normalized to a common enum. */
    public int firstChildWithField(int parent, String nativeField) {
        Objects.checkIndex(parent, nodes());
        Objects.requireNonNull(nativeField, "nativeField");
        int start = first[parent], limit = start + counts[parent];
        for (int at = start; at < limit; at++) {
            int child = children[at];
            if (nativeField.equals(tree.field(child))) return child;
        }
        return -1;
    }

    /**
     * Deepest positive-width node whose half-open byte range contains offset.
     * Returns -1 at EOF (or for an empty source); otherwise returns the root
     * when offset is inside an unparsed source gap.
     */
    public int deepestAtByte(int offset) {
        Objects.checkIndex(offset, tree.sourceLength() + 1);
        if (offset == tree.sourceLength()) return -1;
        int current = 0;
        for (;;) {
            int next = childContainingByte(current, offset);
            if (next < 0) return current;
            current = next;
        }
    }
    /**
     * Return source-to-root path of occurrence handles, including the root.
     * The source is not decoded or copied. EOF returns an empty path.
     */
    public int[] pathAtByte(int offset) {
        int leaf = deepestAtByte(offset);
        if (leaf < 0) return new int[0];
        int depth = 0;
        for (int n = leaf; n >= 0; n = tree.parent(n)) depth++;
        int[] path = new int[depth];
        for (int n = leaf, i = depth - 1; n >= 0; n = tree.parent(n)) path[i--] = n;
        return path;
    }
    private int childContainingByte(int parent, int offset) {
        int start = first[parent], count = counts[parent];
        // Ordered siblings have monotonically nondecreasing starts; zero-width
        // missing siblings may share a start with the actual following token.
        int lo = 0, hi = count;
        while (lo < hi) {
            int mid = lo + ((hi - lo) >>> 1);
            if (tree.startByte(children[start + mid]) <= offset) lo = mid + 1;
            else hi = mid;
        }
        if (lo == 0) return -1;
        int candidate = children[start + lo - 1];
        return offset < tree.endByte(candidate) ? candidate : -1;
    }

    /** Defensive standalone ordinal index for a single concrete grammar kind. */
    public int[] occurrencesOfKind(String kind) {
        Objects.requireNonNull(kind, "kind");
        int[] result = new int[Math.min(16, nodes())];
        int count = 0;
        for (int node = 0; node < nodes(); node++) {
            if (kind.equals(tree.kind(node))) {
                if (count == result.length) result = Arrays.copyOf(result, count + (count >>> 1) + 1);
                result[count++] = node;
            }
        }
        return Arrays.copyOf(result, count);
    }
}
