// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import java.util.Arrays;
import java.util.Objects;

/**
 * A bounded, allocation-conscious projection into the original M3ASTIndex.
 * Original parser nodes are visited, not retained; no parallel AST or source exists.
 */
public final class M3ASTExternalIngress {
    private M3ASTExternalIngress() { }

    public interface Nodes<N> {
        String parserId();
        String kind(N node);
        int childCount(N node);
        N child(N node, int index);
        default String childRole(N parent, int index) { return null; }
        /** Must be UTF-16 half-open; return -1 if only byte offsets are known. */
        default int startUtf16(N node) { return -1; }
        default int endUtf16(N node) { return -1; }
        default int portableFlags(N node) { return 0; }
        default boolean recovered(N node) { return false; }
    }

    /**
     * Every occurrence has its own row, even when parser DAG nodes are shared.
     * A cycle expands only until the explicit node/depth budget refuses it.
     * No external source binding is certified here: SOURCE_UNVERIFIED is retained.
     */
    public static <N> M3ASTIndex project(
            M3File source, N root, Nodes<N> adapter, M3ASTIngress.Limits limits) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(adapter, "adapter");
        Objects.requireNonNull(limits, "limits");
        if (source.length() > limits.maxSourceUnits()) {
            throw new IllegalArgumentException("AST source exceeds UTF-16 input limit");
        }
        M3ASTIndex.Builder out = M3ASTIndex.externalBuilder(
                source, adapter.parserId(), limits.maxNodes());
        int capacity = Math.min(32, limits.maxNodes());
        Object[] pending = new Object[capacity];
        String[] roles = new String[capacity];
        int[] parents = new int[capacity], depths = new int[capacity];
        pending[0] = root; parents[0] = -1; depths[0] = 1;
        int size = 1, appended = 0;
        while (size > 0) {
            int top = --size;
            @SuppressWarnings("unchecked")
            N node = (N) pending[top];
            pending[top] = null;
            String role = roles[top];
            roles[top] = null;
            int parent = parents[top], depth = depths[top];
            if (depth > limits.maxDepth()) {
                throw new IllegalArgumentException("external AST depth budget exceeded");
            }
            int start = adapter.startUtf16(node), end = adapter.endUtf16(node);
            int flags = M3ASTIndex.SYNTAX_ONLY | M3ASTIndex.SOURCE_UNVERIFIED;
            if (start >= 0 && end >= 0) flags |= M3ASTIndex.EXACT_RANGE;
            if (adapter.recovered(node)) flags |= M3ASTIndex.RECOVERED;
            int row = out.add(parent, adapter.kind(node), start, end, flags);
            out.portable(row, role, adapter.portableFlags(node));
            appended++;
            int count = adapter.childCount(node);
            if (count < 0 || count > limits.maxNodes() - appended - size) {
                throw new IllegalArgumentException("external AST occurrence budget exceeded");
            }
            if (count != 0 && depth == limits.maxDepth()) {
                throw new IllegalArgumentException("external AST depth budget exceeded");
            }
            for (int i = count - 1; i >= 0; i--) {
                N child = Objects.requireNonNull(adapter.child(node, i), "external child");
                String childRole = adapter.childRole(node, i);
                if (size == pending.length) {
                    int grown = (int) Math.min(limits.maxNodes(),
                            pending.length + (long) (pending.length >>> 1) + 1L);
                    if (grown <= size) throw new IllegalArgumentException("external AST node budget exceeded");
                    pending = Arrays.copyOf(pending, grown);
                    roles = Arrays.copyOf(roles, grown);
                    parents = Arrays.copyOf(parents, grown);
                    depths = Arrays.copyOf(depths, grown);
                }
                pending[size] = child; roles[size] = childRole;
                parents[size] = row; depths[size++] = depth + 1;
            }
        }
        return out.seal();
    }
}
