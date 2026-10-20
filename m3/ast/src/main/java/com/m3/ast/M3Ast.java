// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.ast;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

/**
 * Immutable parser-neutral AST over canonical M3 atom coordinates.
 *
 * <p>Parser/provider objects are admission-only. The frozen result retains only primitive atom
 * coordinates. Structurally equal immutable subtrees canonicalize to one atom row, so repeated AST
 * structure naturally shares storage without introducing mutable object identity.</p>
 */
public final class M3Ast {
    /**
     * Adapter from a donor/provider AST. Payload must be a stable M3 String/lexicon coordinate;
     * zero means no payload.
     */
    public interface NodeAdapter<N> {
        int type(N node);
        default long payloadId(N node) { return 0L; }
        default long metadataBits(N node) { return 0L; }
        List<N> children(N node);
    }

    private final M3AtomStore atoms;
    private final int[] roots;

    public M3Ast(M3AtomStore atoms, int... roots) {
        this.atoms = Objects.requireNonNull(atoms, "atoms");
        this.roots = Objects.requireNonNull(roots, "roots").clone();
        if (this.roots.length == 0) throw new IllegalArgumentException("AST requires a root");
        for (int root : this.roots) {
            Objects.checkIndex(root, atoms.atomCount());
            if (atoms.domain(root) != M3AtomDomain.AST) {
                throw new IllegalArgumentException("non-AST root atom: " + root);
            }
        }
    }

    public static <N> M3Ast compile(N root, NodeAdapter<N> adapter) {
        Objects.requireNonNull(root, "root");
        M3AtomStore.Builder builder = M3AtomStore.builder();
        int rootAtom = compileInto(root, builder, adapter);
        return new M3Ast(builder.freeze(), rootAtom);
    }

    public static <N> M3Ast compileForest(List<N> roots, NodeAdapter<N> adapter) {
        Objects.requireNonNull(roots, "roots");
        if (roots.isEmpty()) throw new IllegalArgumentException("AST forest is empty");
        M3AtomStore.Builder builder = M3AtomStore.builder();
        int[] admitted = new int[roots.size()];
        IdentityHashMap<N, Integer> compiled = new IdentityHashMap<>();
        IdentityHashMap<N, Boolean> active = new IdentityHashMap<>();
        for (int i = 0; i < admitted.length; i++) {
            admitted[i] = compileNode(
                    Objects.requireNonNull(roots.get(i), "root"),
                    builder,
                    Objects.requireNonNull(adapter, "adapter"),
                    compiled,
                    active);
        }
        return new M3Ast(builder.freeze(), admitted);
    }

    /**
     * Admit one tree into a caller-owned atom builder. Useful when AST and DAG snapshots deliberately
     * share one immutable atom universe.
     */
    public static <N> int compileInto(
            N root,
            M3AtomStore.Builder builder,
            NodeAdapter<N> adapter) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(builder, "builder");
        Objects.requireNonNull(adapter, "adapter");
        return compileNode(
                root, builder, adapter, new IdentityHashMap<>(), new IdentityHashMap<>());
    }

    public M3AtomStore atoms() { return atoms; }
    public int rootCount() { return roots.length; }
    public int root(int ordinal) { return roots[Objects.checkIndex(ordinal, roots.length)]; }
    public int[] roots() { return roots.clone(); }

    public int type(int atomId) {
        requireAst(atomId);
        return atoms.type(atomId);
    }

    public long payloadId(int atomId) {
        requireAst(atomId);
        return atoms.payloadId(atomId);
    }

    public long metadataBits(int atomId) {
        requireAst(atomId);
        return atoms.metadataBits(atomId);
    }

    public int childCount(int atomId) {
        requireAst(atomId);
        return atoms.childCount(atomId);
    }

    public int childAt(int atomId, int ordinal) {
        requireAst(atomId);
        return atoms.childAt(atomId, ordinal);
    }

    public long contentHash(int atomId) {
        requireAst(atomId);
        return atoms.contentHash(atomId);
    }

    public long structureHash(int atomId) {
        requireAst(atomId);
        return atoms.structureHash(atomId);
    }

    public int depth(int atomId) {
        requireAst(atomId);
        return atoms.depth(atomId);
    }

    public int expandedSize(int atomId) {
        requireAst(atomId);
        return atoms.subtreeSize(atomId);
    }

    /**
     * Deterministic preorder over one root. Reused canonical subtrees appear once per occurrence,
     * preserving tree semantics even though their physical atom row is shared.
     */
    public int[] preOrder(int rootOrdinal) {
        int root = root(rootOrdinal);
        int estimated = Math.max(1, atoms.subtreeSize(root));
        int[] output = new int[estimated];
        int[] stack = new int[Math.max(8, atoms.depth(root) * 2)];
        int outputSize = 0;
        int stackSize = 0;
        stack[stackSize++] = root;
        while (stackSize != 0) {
            int current = stack[--stackSize];
            if (outputSize == output.length) {
                output = java.util.Arrays.copyOf(
                        output, output.length + (output.length >>> 1) + 1);
            }
            output[outputSize++] = current;
            int count = atoms.childCount(current);
            while (stackSize + count > stack.length) {
                stack = java.util.Arrays.copyOf(
                        stack, stack.length + (stack.length >>> 1) + count + 1);
            }
            for (int i = count - 1; i >= 0; i--) {
                stack[stackSize++] = atoms.childAt(current, i);
            }
        }
        return outputSize == output.length
                ? output
                : java.util.Arrays.copyOf(output, outputSize);
    }

    /** Projects ordered AST-child relations into an immutable M3 DAG. */
    public M3Dag asDag(long childRelationId) {
        return M3Dag.fromAst(this, childRelationId);
    }

    private void requireAst(int atomId) {
        Objects.checkIndex(atomId, atoms.atomCount());
        if (atoms.domain(atomId) != M3AtomDomain.AST) {
            throw new IllegalArgumentException("atom is not AST: " + atomId);
        }
    }

    private static <N> int compileNode(
            N node,
            M3AtomStore.Builder builder,
            NodeAdapter<N> adapter,
            IdentityHashMap<N, Integer> compiled,
            IdentityHashMap<N, Boolean> active) {
        Integer existing = compiled.get(node);
        if (existing != null) return existing;
        if (active.put(node, Boolean.TRUE) != null) {
            throw new IllegalArgumentException("AST provider exposed a cycle");
        }
        try {
            List<N> children = adapter.children(node);
            if (children == null) children = List.of();
            ArrayList<Integer> childRows = new ArrayList<>(children.size());
            for (N child : children) {
                childRows.add(compileNode(
                        Objects.requireNonNull(child, "AST child"),
                        builder, adapter, compiled, active));
            }
            int[] rows = new int[childRows.size()];
            for (int i = 0; i < rows.length; i++) rows[i] = childRows.get(i);
            int row = builder.intern(
                    M3AtomDomain.AST,
                    adapter.type(node),
                    adapter.payloadId(node),
                    adapter.metadataBits(node),
                    rows);
            compiled.put(node, row);
            return row;
        } finally {
            active.remove(node);
        }
    }
}
