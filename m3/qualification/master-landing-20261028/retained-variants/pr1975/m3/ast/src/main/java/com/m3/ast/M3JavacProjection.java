// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

/**
 * A narrow, provider-specific intake into the existing M3SyntaxTree occurrence
 * plane. It consumes public jdk.compiler syntax and source positions; it does
 * NOT parse Java, construct another retained syntax graph, or infer bindings.
 *
 * <p>Javac reports UTF-16 positions, frequently omits comments, and can report
 * missing, synthetic, equal or crossing source ranges. We preserve the complete
 * caller-owned byte image under one synthetic root and project only intervals
 * with a deterministic non-crossing hierarchy. Rejected/unsupported syntax is
 * still present byte-for-byte in the retained original source. The caller's
 * declared coordinate mapper must refuse split surrogate positions.
 */
public final class M3JavacProjection {
    private M3JavacProjection() { }

    public record Result(M3SyntaxTree tree, int visited, int projected, int unpositioned,
                         int crossing, int invalid) {
        public Result {
            Objects.requireNonNull(tree, "tree");
            if (visited < 0 || projected < 0 || unpositioned < 0 || crossing < 0 || invalid < 0
                    || projected + unpositioned + crossing + invalid != visited)
                throw new IllegalArgumentException("invalid projection statistics");
        }
    }
    private static final int DEFAULT_LIMIT = 1_000_000;
    private static final class Occurrence {
        private final String kind;
        private final int from, to, flags, ordinal;
        private final List<Occurrence> children = new ArrayList<>();
        private Occurrence(String kind, int from, int to, int flags, int ordinal) {
            this.kind = kind; this.from = from; this.to = to; this.flags = flags; this.ordinal = ordinal;
        }
    }
    private static final M3SyntaxTree.ForeignNode<Occurrence> ADAPTER =
            new M3SyntaxTree.ForeignNode<>() {
                @Override public String kind(Occurrence n) { return n.kind; }
                @Override public String field(Occurrence n) { return ""; }
                @Override public int startByte(Occurrence n) { return n.from; }
                @Override public int endByte(Occurrence n) { return n.to; }
                @Override public int flags(Occurrence n) { return n.flags; }
                @Override public Iterable<Occurrence> children(Occurrence n) { return n.children; }
            };

    public static Result project(CompilationUnitTree unit, SourcePositions positions,
                                 byte[] sourceBytes, IntUnaryOperator utf16ToByte) {
        return project(unit, positions, sourceBytes, utf16ToByte, DEFAULT_LIMIT);
    }

    public static Result project(CompilationUnitTree unit, SourcePositions positions,
                                 byte[] sourceBytes, IntUnaryOperator utf16ToByte,
                                 int maximumProviderNodes) {
        Objects.requireNonNull(unit, "compilation unit");
        Objects.requireNonNull(positions, "source positions");
        Objects.requireNonNull(sourceBytes, "source bytes");
        Objects.requireNonNull(utf16ToByte, "coordinate converter");
        if (maximumProviderNodes < 1) throw new IllegalArgumentException("empty parser node budget");
        byte[] immutableSource = sourceBytes.clone();
        List<Occurrence> items = new ArrayList<>();
        int[] examined = new int[3]; // visited, unpositioned, invalid
        new TreeScanner<Void, Void>() {
            @Override public Void scan(Tree node, Void context) {
                if (node == null) return null;
                if (++examined[0] > maximumProviderNodes)
                    throw new IllegalStateException("provider syntax node budget exceeded");
                long begin = positions.getStartPosition(unit, node);
                long finish = positions.getEndPosition(unit, node);
                if (begin < 0 || finish < 0) {
                    examined[1]++;
                } else if (begin > Integer.MAX_VALUE || finish > Integer.MAX_VALUE
                        || finish < begin) {
                    examined[2]++;
                } else {
                    // Fail closed on inconsistent native offsets; do not silently
                    // reinterpret UTF-16 coordinates as source-byte offsets.
                    int start = utf16ToByte.applyAsInt((int) begin);
                    int end = utf16ToByte.applyAsInt((int) finish);
                    if (start < 0 || end < start || end > immutableSource.length) {
                        examined[2]++;
                    } else {
                        int flags = M3SyntaxTree.NAMED;
                        if (node.getKind() == Tree.Kind.ERRONEOUS) flags |= M3SyntaxTree.ERROR;
                        items.add(new Occurrence(node.getKind().name(), start, end,
                                flags, examined[0]));
                    }
                }
                return super.scan(node, context);
            }
        }.scan(unit, null);

        // Stable interval laminarization. A crossing span cannot be a syntax
        // child in M3's single-source occurrence tree, but its original text
        // remains intact and it is explicitly counted rather than fabricated.
        items.sort(Comparator.comparingInt((Occurrence n) -> n.from)
                .thenComparing(Comparator.comparingInt((Occurrence n) -> n.to).reversed())
                .thenComparingInt(n -> n.ordinal));
        Occurrence synthetic = new Occurrence("m3.source_image", 0,
                immutableSource.length, M3SyntaxTree.SYNTHETIC, 0);
        ArrayDeque<Occurrence> open = new ArrayDeque<>();
        open.push(synthetic);
        int crossing = 0, admitted = 0;
        for (Occurrence item : items) {
            while (open.size() > 1 && item.from >= open.peek().to) open.pop();
            if (item.from < open.peek().from || item.to > open.peek().to) {
                crossing++;
                continue;
            }
            open.peek().children.add(item);
            open.push(item);
            admitted++;
        }
        M3SyntaxTree result = M3SyntaxTree.project(
                "jdk.compiler/21", immutableSource, synthetic, ADAPTER);
        return new Result(result, examined[0], admitted,
                examined[1], crossing, examined[2]);
    }
}
