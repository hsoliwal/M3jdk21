/*
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Optional, detached provider-field metadata on the authoritative M3ASTIndex.
 *
 * <p>The owning ingress records one edge role per index row, in the same preorder
 * as M3ASTIndex.Builder.add(...). This class NEVER parses source, materializes
 * native AST nodes, changes geometry, or extends the M3ASTIndex public contract.
 * Role IDs are local to one index image, and rows are not stable across edits.
 *
 * <p>Per-row payload is one short role ID and one int position (6 bytes), plus
 * a bounded role-name dictionary. An absent role has ID -1 and position -1.
 * A named scalar field uses position -1; named list items use 0-based positions.
 */
public final class M3ASTFieldRoles {
    private static final int MAX_ROLES = 4096;
    private final M3ASTIndex index;
    private final short[] roles;
    private final int[] positions;
    private final String[] vocabulary;

    private M3ASTFieldRoles(M3ASTIndex index, short[] roles, int[] positions, String[] vocabulary) {
        this.index = index;
        this.roles = roles;
        this.positions = positions;
        this.vocabulary = vocabulary;
    }

    public M3ASTIndex index() { return index; }
    public int nodeCount() { return roles.length; }
    public long primitivePayloadBytes() { return 6L * roles.length; }
    public int roleId(int row) { return roles[Objects.checkIndex(row, roles.length)]; }
    public Optional<String> role(int row) {
        int id = roleId(row);
        return id < 0 ? Optional.empty() : Optional.of(vocabulary[id]);
    }
    public int positionInRole(int row) { return positions[Objects.checkIndex(row, roles.length)]; }
    public int roleVocabularySize() { return vocabulary.length; }
    public String roleVocabularyName(int id) { return vocabulary[Objects.checkIndex(id, vocabulary.length)]; }

    /** Allocation-free child scan, deferring postings/rank indexes to M3ASTRelations. */
    public int firstChildWithRole(int parentRow, String field) {
        Objects.requireNonNull(field, "field");
        Objects.checkIndex(parentRow, index.nodeCount());
        for (int child = index.firstChild(parentRow); child >= 0; child = index.nextSibling(child)) {
            int id = roles[child];
            if (id >= 0 && vocabulary[id].equals(field)) return child;
        }
        return -1;
    }

    public static Recorder recorder(int maxNodes) { return new Recorder(maxNodes); }

    /**
     * Event-local recorder. The native provider and authoritative Builder.add
     * retain traversal authority. Recorder accepts rows in strict ascending order.
     */
    public static final class Recorder {
        private final int maxNodes;
        private short[] roles;
        private int[] positions;
        private String[] vocabulary = new String[16];
        /** Deterministic open-addressed dictionary, containing vocabulary ID + 1. */
        private int[] roleSlots = new int[32];
        private int roleCount;
        private int count;
        private boolean sealed;

        private Recorder(int maxNodes) {
            if (maxNodes < 1 || maxNodes > 20_000_000) throw new IllegalArgumentException("invalid node budget");
            this.maxNodes = maxNodes;
            this.roles = new short[Math.min(32, maxNodes)];
            this.positions = new int[roles.length];
        }

        /**
         * Record exactly one native child field after the corresponding index row
         * was appended. Root and unnamed children use null/-1. Positions on named
         * fields are -1 for scalar or >=0 for list items.
         */
        public void record(int row, String field, int position) {
            if (sealed) throw new IllegalStateException("roles already published");
            if (row != count || count >= maxNodes) throw new IllegalArgumentException("nonsequential or excess row");
            if (row == 0 && (field != null || position != -1))
                throw new IllegalArgumentException("root cannot have a field role");
            if (field == null && position != -1)
                throw new IllegalArgumentException("unnamed field cannot have a position");
            if (position < -1) throw new IllegalArgumentException("negative field position");
            if (count == roles.length) {
                int grown = (int) Math.min(maxNodes, Math.max((long) count + 1, count + (long) (count >>> 1) + 1));
                roles = Arrays.copyOf(roles, grown);
                positions = Arrays.copyOf(positions, grown);
            }
            roles[count] = field == null ? -1 : (short) internRole(field);
            positions[count++] = position;
        }

        /** Bind metadata to precisely the original index image; no geometry copy. */
        public M3ASTFieldRoles seal(M3ASTIndex original) {
            if (sealed) throw new IllegalStateException("roles already published");
            Objects.requireNonNull(original, "original M3ASTIndex");
            if (original.nodeCount() != count || count == 0 || original.parent(0) != -1)
                throw new IllegalArgumentException("roles do not cover original index");
            for (int row = 1; row < count; row++) {
                if (original.parent(row) < 0 || original.parent(row) >= row)
                    throw new IllegalArgumentException("index is not parent-first preorder");
            }
            sealed = true;
            return new M3ASTFieldRoles(original, Arrays.copyOf(roles, count),
                    Arrays.copyOf(positions, count), Arrays.copyOf(vocabulary, roleCount));
        }

        private int internRole(String field) {
            if (field.isEmpty() || field.length() > 128)
                throw new IllegalArgumentException("invalid bounded field role");
            int at = slot(field, roleSlots, vocabulary);
            if (roleSlots[at] != 0) return roleSlots[at] - 1;
            if (roleCount == MAX_ROLES) throw new IllegalArgumentException("field vocabulary budget exceeded");
            if ((roleCount + 1) * 2 >= roleSlots.length) {
                int[] enlarged = new int[roleSlots.length << 1];
                for (int id = 0; id < roleCount; id++)
                    enlarged[slot(vocabulary[id], enlarged, vocabulary)] = id + 1;
                roleSlots = enlarged;
                at = slot(field, roleSlots, vocabulary);
            }
            if (roleCount == vocabulary.length) vocabulary = Arrays.copyOf(vocabulary, vocabulary.length * 2);
            vocabulary[roleCount] = field;
            roleSlots[at] = ++roleCount;
            return roleCount - 1;
        }

        private static int slot(String field, int[] slots, String[] names) {
            int hash = field.hashCode();
            int mask = slots.length - 1;
            int at = (hash ^ (hash >>> 16)) & mask;
            while (slots[at] != 0 && !field.equals(names[slots[at] - 1]))
                at = (at + 1) & mask;
            return at;
        }

    }
}
