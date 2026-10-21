/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser;

import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/** Projects the existing M3AST parse event; the caller owns and runs its javac task. */
public final class M3JavacParser {
    private M3JavacParser() { }

    /**
     * Create a caller-owned operation identity. Reuse this exact instance across
     * consumers to obtain one projection per event from the existing M3AST cache.
     * The node budget bounds both output and the pending traversal stack.
     */
    public static M3AST.Projection<M3JavacTree> projection(int maxNodes) {
        if (maxNodes < 1) throw new IllegalArgumentException("maxNodes must be positive");
        return new M3AST.Projection<>("jdk.m3.parser.javac.preorder.v1",
                unit -> capture(unit, maxNodes));
    }

    /**
     * Register before the task starts, enabling the existing syntax-retention
     * policy. Closing the returned shared handle releases its consumers. For
     * parse/analyze/generate clients, use try-with-resources around this handle.
     */
    public static M3AST attach(JavacTask task, M3AST.Projection<M3JavacTree> projection,
                               Consumer<? super M3JavacTree> consumer) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(projection, "projection");
        Objects.requireNonNull(consumer, "consumer");
        M3AST handle = M3AST.open(task, true);
        handle.addConsumer(new M3AST.Consumer() {
            @Override public void parsed(M3AST.Unit unit) throws Exception {
                consumer.accept(unit.project(projection));
            }
        });
        return handle;
    }

    private static M3JavacTree capture(M3AST.Unit unit, int maxNodes) throws Exception {
        if (unit.phase() != M3AST.Phase.PARSED) {
            throw new IllegalArgumentException("M3JavacParser requires a PARSED event");
        }
        M3AST.Source retained = unit.source();
        M3File source = M3File.of(retained.uri(), retained.text());
        if (!source.utf16Sha256().equals(retained.utf16Sha256())) {
            throw new IllegalStateException("M3AST source seal mismatch");
        }
        SourcePositions positions = unit.trees().getSourcePositions();
        int capacity = Math.min(64, maxNodes);
        int[] kinds = new int[capacity];
        int[] parents = new int[capacity];
        int[] starts = new int[capacity];
        int[] ends = new int[capacity];
        Tree[] pending = new Tree[capacity];
        int[] pendingParents = new int[capacity];
        int pendingSize = 1;
        pending[0] = unit.tree();
        pendingParents[0] = -1;
        int size = 0;
        ArrayList<Tree> children = new ArrayList<>();
        while (pendingSize != 0) {
            Tree current = pending[--pendingSize];
            int parent = pendingParents[pendingSize];
            pending[pendingSize] = null;
            if (size == maxNodes) throw new IllegalStateException("M3AST maxNodes exceeded");
            if (size == kinds.length) {
                int next = grow(kinds.length, maxNodes);
                kinds = Arrays.copyOf(kinds, next);
                parents = Arrays.copyOf(parents, next);
                starts = Arrays.copyOf(starts, next);
                ends = Arrays.copyOf(ends, next);
            }
            int row = size++;
            kinds[row] = current.getKind().ordinal();
            parents[row] = parent;
            starts[row] = offset(positions.getStartPosition(unit.tree(), current), source.length());
            ends[row] = offset(positions.getEndPosition(unit.tree(), current), source.length());
            if (starts[row] >= 0 && ends[row] >= 0 && starts[row] > ends[row]) {
                throw new IllegalStateException("descending javac source range");
            }
            children.clear();
            current.accept(new DirectChildren(maxNodes - size - pendingSize), children);
            for (int index = children.size() - 1; index >= 0; index--) {
                if (pendingSize == pending.length) {
                    int next = grow(pending.length, maxNodes);
                    pending = Arrays.copyOf(pending, next);
                    pendingParents = Arrays.copyOf(pendingParents, next);
                }
                pending[pendingSize] = children.get(index);
                pendingParents[pendingSize++] = row;
            }
        }
        return new M3JavacTree(source, Arrays.copyOf(kinds, size), Arrays.copyOf(parents, size),
                Arrays.copyOf(starts, size), Arrays.copyOf(ends, size));
    }

    private static int offset(long value, int length) {
        if (value == -1) return -1;
        if (value < 0 || value > length) throw new IllegalStateException("javac range outside source");
        return (int) value;
    }

    private static int grow(int current, int limit) {
        return (int) Math.min(limit, current + (long) (current >>> 1) + 1);
    }

    /** Enumerate direct children only; the outer primitive stack owns recursion. */
    private static final class DirectChildren extends TreeScanner<Void, ArrayList<Tree>> {
        private final int remaining;
        DirectChildren(int remaining) { this.remaining = remaining; }
        @Override public Void scan(Tree tree, ArrayList<Tree> children) {
            if (tree != null) {
                if (children.size() == remaining) {
                    throw new IllegalStateException("M3AST maxNodes exceeded");
                }
                children.add(tree);
            }
            return null;
        }
    }
}
