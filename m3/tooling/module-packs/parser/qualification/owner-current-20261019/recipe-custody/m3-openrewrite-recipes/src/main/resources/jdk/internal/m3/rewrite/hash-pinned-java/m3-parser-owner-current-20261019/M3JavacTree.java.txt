/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser;

import com.sun.source.tree.Tree;
import java.util.Objects;

/**
 * Detached preorder projection of one parsed javac unit into primitive lanes.
 * No live javac tree, compiler context, element, or type is retained. Kind ordinals
 * are interpreted with this running compiler's Tree.Kind; this is not a portable
 * serialized attributed-AST format.
 */
public final class M3JavacTree {
    private static final Tree.Kind[] KIND_BY_ORDINAL = Tree.Kind.values();
    private final M3File source;
    private final int[] kinds;
    private final int[] parents;
    private final int[] starts;
    private final int[] ends;

    M3JavacTree(M3File source, int[] kinds, int[] parents, int[] starts, int[] ends) {
        this.source = Objects.requireNonNull(source, "source");
        this.kinds = kinds;
        this.parents = parents;
        this.starts = starts;
        this.ends = ends;
    }

    public M3File source() { return source; }
    public int nodeCount() { return kinds.length; }
    public Tree.Kind kind(int node) { return KIND_BY_ORDINAL[kinds[check(node)]]; }
    /** Parent row, or -1 for the compilation-unit root. */
    public int parent(int node) { return parents[check(node)]; }
    /** Exact javac UTF-16 start offset, or -1 if unavailable. */
    public int start(int node) { return starts[check(node)]; }
    /** Exact javac UTF-16 end offset, or -1 if unavailable. */
    public int end(int node) { return ends[check(node)]; }
    public boolean hasSourceRange(int node) { return start(node) >= 0 && end(node) >= 0; }

    public String sourceText(int node) {
        if (!hasSourceRange(node)) throw new IllegalStateException("javac source range unavailable");
        return source.subSequence(start(node), end(node));
    }

    public long primitivePayloadBytes() { return 4L * Integer.BYTES * nodeCount(); }
    private int check(int node) { return Objects.checkIndex(node, kinds.length); }
}
