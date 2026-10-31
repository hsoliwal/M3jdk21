/*
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser;

import java.util.Arrays;
import java.util.Objects;

/**
 * Capture a bounded detached projection from an already-parsed, caller-owned AST.
 *
 * <p>This performs no parsing, source read, attribution or change to the native AST.
 * It visits native occurrences exactly once in preorder and projects original
 * grammar-field names into a separate primitive sidecar. No native node reference
 * is retained after this method returns. The caller must keep the native tree
 * unmodified until capture returns; source matching is not certified by this API.
 *
 * <p>This is an opt-in extraction facility, not a second AST implementation.
 * Existing native adapters may implement {@link Access} without allocating nodes.
 */
public final class M3ASTProjectionCapture {
    private M3ASTProjectionCapture() { }

    /** Native AST provider owns its node objects and access semantics. */
    public interface Access<N> {
        String kind(N node);
        int childCount(N node);
        N child(N node, int index);
        default String fieldName(N parent, int index) { return null; }
        default int fieldPosition(N parent, int index) { return -1; }
        default int startUtf16(N node) { return -1; }
        default int endUtf16(N node) { return -1; }
        default boolean recovered(N node) { return false; }
    }

    /** Both views borrow the exact same immutable M3ASTIndex occurrence image. */
    public record Result(M3ASTIndex index, M3ASTFieldRoles roles) {
        public Result {
            Objects.requireNonNull(index, "index");
            Objects.requireNonNull(roles, "roles");
            if (index != roles.index())
                throw new IllegalArgumentException("field roles must borrow the same index image");
        }
    }

    /**
     * Bounded nonrecursive capture; native source and semantic binding stay
     * explicitly unverified even when the provider supplies source coordinates.
     * A graph/cycle is not declared a tree: it can only be projected as bounded
     * individual occurrences. A cycle exceeding the depth budget is refused.
     */
    public static <N> Result capture(M3File source, M3ASTIndex.Domain domain, N root,
                                     Access<N> provider, int maxNodes, int maxDepth) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(provider, "provider");
        if (maxNodes < 1 || maxNodes > 20_000_000 || maxDepth < 1 || maxDepth > 100_000)
            throw new IllegalArgumentException("invalid AST projection budgets");
        M3ASTIndex.Builder nodes = M3ASTIndex.builder(source, domain, maxNodes);
        M3ASTFieldRoles.Recorder roles = M3ASTFieldRoles.recorder(maxNodes);
        Object[] pending = new Object[Math.min(32, maxNodes)];
        int[] parents = new int[pending.length];
        int[] depths = new int[pending.length];
        int[] positions = new int[pending.length];
        String[] fields = new String[pending.length];
        pending[0] = root;
        parents[0] = -1;
        depths[0] = 1;
        positions[0] = -1;
        int size = 1, admitted = 0;
        while (size != 0) {
            int at = --size;
            @SuppressWarnings("unchecked") N nativeNode = (N) pending[at];
            pending[at] = null;
            int parent = parents[at], depth = depths[at], position = positions[at];
            String field = fields[at];
            fields[at] = null;
            if (depth > maxDepth) throw new IllegalArgumentException("AST projection depth budget exceeded");
            int start = provider.startUtf16(nativeNode);
            int end = provider.endUtf16(nativeNode);
            int flags = M3ASTIndex.SYNTAX_ONLY | M3ASTIndex.SOURCE_UNVERIFIED;
            if (start >= 0 && end >= start) flags |= M3ASTIndex.EXACT_RANGE;
            if (provider.recovered(nativeNode)) flags |= M3ASTIndex.RECOVERED;
            int row = nodes.add(parent, provider.kind(nativeNode), start, end, flags);
            roles.record(row, field, position);
            admitted++;
            int children = provider.childCount(nativeNode);
            if (children < 0 || children > maxNodes - admitted - size)
                throw new IllegalArgumentException("AST child count exceeds occurrence budget");
            if (children != 0 && depth == maxDepth)
                throw new IllegalArgumentException("AST projection depth budget exceeded");
            for (int child = children - 1; child >= 0; child--) {
                N next = Objects.requireNonNull(provider.child(nativeNode, child), "native AST child");
                String role = provider.fieldName(nativeNode, child);
                int rolePosition = provider.fieldPosition(nativeNode, child);
                if (size == pending.length) {
                    int grown = (int) Math.min(maxNodes,
                            Math.max((long) size + 1, size + (long) (size >>> 1) + 1));
                    if (grown <= size) throw new IllegalArgumentException("AST stack budget exceeded");
                    pending = Arrays.copyOf(pending, grown);
                    parents = Arrays.copyOf(parents, grown);
                    depths = Arrays.copyOf(depths, grown);
                    positions = Arrays.copyOf(positions, grown);
                    fields = Arrays.copyOf(fields, grown);
                }
                pending[size] = next;
                parents[size] = row;
                depths[size] = depth + 1;
                positions[size] = rolePosition;
                fields[size++] = role;
            }
        }
        M3ASTIndex index = nodes.seal();
        return new Result(index, roles.seal(index));
    }
}
