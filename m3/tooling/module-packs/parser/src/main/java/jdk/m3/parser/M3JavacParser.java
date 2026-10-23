/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser;

import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/** Projects the existing M3AST parse event; the caller owns and runs its javac task. */
public final class M3JavacParser {
    private static final M3AST.Projection<M3File> SOURCE = new M3AST.Projection<>(
            "jdk.m3.parser.javac.source.v1", M3JavacParser::captureSource);

    private M3JavacParser() { }

    /**
     * The shared source-only operation identity for an original PARSED event.
     *
     * <p>The existing event memoizer owns the result. This key retains no source
     * or compiler state between events. Source and tree consumers receive the
     * same immutable M3File without another source read or AST traversal.</p>
     *
     * @return the same operation key for requests through M3AST.Unit.project
     */
    public static M3AST.Projection<M3File> sourceProjection() {
        return SOURCE;
    }

    /**
     * Compare the actual parsed source with a caller-owned earlier snapshot.
     *
     * <p>This requests only the shared source projection, never a tree index.
     * It supplies character/URI/byte-seal evidence, not semantic equivalence or
     * authority to skip a build task. The original callback lifetime applies.</p>
     *
     * @param unit the current original compiler PARSED callback view
     * @param previous an actual earlier immutable source snapshot
     * @return exact UTF-16 replacement coordinates and identity evidence
     * @throws Exception if the original view or source projection is unavailable
     */
    public static M3File.SourceDelta changesFrom(M3AST.Unit unit, M3File previous) throws Exception {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(previous, "previous");
        return unit.project(SOURCE).changesFrom(previous);
    }

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

    /** Reuses one M3AST-index projection per original PARSED event and task. */
    public static M3AST.Projection<M3ASTIndex> indexProjection(M3ASTIngress.Limits limits) {
        Objects.requireNonNull(limits, "limits");
        return new M3AST.Projection<>("jdk.m3.parser.javac.index.v1", unit -> {
            if (unit.phase() != M3AST.Phase.PARSED) {
                throw new IllegalArgumentException("javac index requires a PARSED event");
            }
            M3AST.Source frozen = unit.source();
            M3File source = M3File.of(frozen.uri(), frozen.text());
            if (!source.utf16Sha256().equals(frozen.utf16Sha256())) {
                throw new IllegalStateException("M3AST source seal mismatch");
            }
            return M3ASTIngress.javac(source, unit.tree(), unit.trees(), limits);
        });
    }

    /** Register a shared indexed projection; existing M3JavacTree callers are unaffected. */
    public static M3AST attachIndex(JavacTask task,
            M3AST.Projection<M3ASTIndex> projection,
            Consumer<? super M3ASTIndex> consumer) {
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

    /**
     * Caller-owned rich syntax-index operation. Reuse this exact key to share
     * one bounded detached index per PARSED event through the existing M3AST
     * cache. No new parser, task, atom pool or persistent semantic cache exists.
     * The exact compiler input seal, not URI/length equality, establishes the
     * source binding; attributed semantic identity still requires its context.
     */
    public static M3AST.Projection<M3ASTIndex> indexProjection(M3ASTIngress.Limits limits) {
        Objects.requireNonNull(limits, "limits");
        return new M3AST.Projection<>("jdk.m3.parser.javac.details.v1", unit -> {
            if (unit.phase() != M3AST.Phase.PARSED) {
                throw new IllegalArgumentException("M3ASTIndex syntax projection requires a PARSED event");
            }
            M3AST.Source retained = unit.source();
            M3File source = M3File.of(retained.uri(), retained.text());
            if (!source.utf16Sha256().equals(retained.utf16Sha256())) {
                throw new IllegalStateException("M3AST source seal mismatch");
            }
            return M3ASTIngress.javacSealed(source, unit.tree(), unit.trees().getSourcePositions(), limits);
        });
    }

    /** Register a rich index consumer on the same original-task event owner. */
    public static M3AST attachIndex(JavacTask task, M3AST.Projection<M3ASTIndex> projection,
                                    Consumer<? super M3ASTIndex> consumer) {
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

    private static M3File captureSource(M3AST.Unit unit) throws Exception {
        if (unit.phase() != M3AST.Phase.PARSED) {
            throw new IllegalArgumentException("M3JavacParser requires a PARSED event");
        }
        M3AST.Source retained = unit.source();
        M3File source = M3File.of(retained.uri(), retained.text());
        if (!source.utf16Sha256().equals(retained.utf16Sha256())) {
            throw new IllegalStateException("M3AST source seal mismatch");
        }
        return source;
    }

    private static M3JavacTree capture(M3AST.Unit unit, int maxNodes) throws Exception {
        if (unit.phase() != M3AST.Phase.PARSED) {
            throw new IllegalArgumentException("M3JavacParser requires a PARSED event");
        }
        M3File source = unit.project(SOURCE);
        SourcePositions positions = unit.trees().getSourcePositions();
        int capacity = Math.min(64, maxNodes);
        int[] kinds = new int[capacity];
        int[] parents = new int[capacity];
        int[] starts = new int[capacity];
        int[] ends = new int[capacity];
        DirectChildren pending = new DirectChildren(unit.tree(), capacity, maxNodes);
        int size = 0;
        while (pending.size != 0) {
            int parent = pending.parents[pending.size - 1];
            Tree current = pending.pop();
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
            pending.append(current, row, maxNodes - size);
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

    /** One reusable scanner appends direct children to the existing array stack. */
    private static final class DirectChildren extends TreeScanner<Void, Void> {
        private final int limit;
        private Tree[] nodes;
        private int[] parents;
        private int size = 1;
        private int parent;
        private int remaining;

        DirectChildren(Tree root, int capacity, int limit) {
            this.limit = limit;
            nodes = new Tree[capacity];
            parents = new int[capacity];
            nodes[0] = root;
            parents[0] = -1;
        }

        Tree pop() {
            Tree node = nodes[--size];
            nodes[size] = null;
            return node;
        }

        void append(Tree node, int parent, int remaining) {
            this.parent = parent;
            this.remaining = remaining;
            int boundary = size;
            node.accept(this, null);
            // Reverse only the appended segment so LIFO visitation preserves javac order.
            // All entries in this segment have the same parent row.
            for (int left = boundary, right = size - 1; left < right; left++, right--) {
                Tree swap = nodes[left];
                nodes[left] = nodes[right];
                nodes[right] = swap;
            }
        }

        @Override public Void scan(Tree tree, Void ignored) {
            if (tree != null) {
                if (size == remaining) throw new IllegalStateException("M3AST maxNodes exceeded");
                if (size == nodes.length) {
                    int next = grow(nodes.length, limit);
                    nodes = Arrays.copyOf(nodes, next);
                    parents = Arrays.copyOf(parents, next);
                }
                nodes[size] = tree;
                parents[size++] = parent;
            }
            return null;
        }
    }
}
