// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.ast;

import java.util.Arrays;
import java.util.Objects;

/** Primitive traversal bookkeeping proportional to reachable nodes, independent of unrelated IDs. */
public final class MIndexASTTraversal {
    private MIndexASTTraversal() {}
    public static Order order(MIndexAST.Ref root) {
        Objects.requireNonNull(root, "root"); MIndexAST owner = root.owner();
        return order(root.id(), new Access() {
            public int count(int node) { return owner.childCount(node); }
            public int child(int node, int ordinal) { return owner.childId(node, ordinal); }
        });
    }
    public static Order order(MIndexAST root) {
        int id = Objects.requireNonNull(root, "root").contentIndex(); MIndexASTStore owner = root.store;
        return order(id, new Access() {
            public int count(int node) { return owner.node(node).children.length; }
            public int child(int node, int ordinal) { return owner.node(node).children[ordinal]; }
        });
    }
    private static Order order(int root, Access access) {
        int[] stack = new int[16], next = new int[16], output = new int[16];
        int depth = 1, count = 0; stack[0] = root;
        Index ordinals = new Index();
        while (depth != 0) {
            int top = depth - 1, node = stack[top];
            if (next[top] < access.count(node)) {
                int child = access.child(node, next[top]++);
                if (ordinals.get(child) >= 0) continue;
                if (depth == stack.length) { stack = Arrays.copyOf(stack, Math.multiplyExact(depth, 2)); next = Arrays.copyOf(next, stack.length); }
                stack[depth] = child; next[depth] = 0; depth++;
            } else {
                depth--;
                if (count == output.length) output = Arrays.copyOf(output, Math.multiplyExact(count, 2));
                output[count] = node; ordinals.put(node, count++);
            }
        }
        return new Order(Arrays.copyOf(output, count), ordinals);
    }
    public static final class Order {
        private final int[] nodes; private final Index ordinals;
        private Order(int[] nodes, Index ordinals) { this.nodes = nodes; this.ordinals = ordinals; }
        public int size() { return nodes.length; }
        public int nodeId(int ordinal) { return nodes[Objects.checkIndex(ordinal, nodes.length)]; }
        /** -1 means the coordinate is not reachable from the root used to build this order. */
        public int ordinal(int nodeId) { return nodeId < 0 ? -1 : ordinals.get(nodeId); }
        public int indexCapacity() { return ordinals.keys.length; }
    }
    private interface Access { int count(int node); int child(int node, int ordinal); }
    private static final class Index {
        private int[] keys = new int[16], values = new int[16]; private int size;
        int get(int id) { int slot = slot(id); return keys[slot] == 0 ? -1 : values[slot]; }
        void put(int id, int value) {
            if ((size + 1L) * 2 >= keys.length) {
                int[] previousKeys = keys, previousValues = values;
                keys = new int[Math.multiplyExact(keys.length, 2)]; values = new int[keys.length];
                for (int i = 0; i < previousKeys.length; i++) if (previousKeys[i] != 0) {
                    int slot = slot(previousKeys[i] - 1); keys[slot] = previousKeys[i]; values[slot] = previousValues[i];
                }
            }
            int slot = slot(id); if (keys[slot] == 0) size++; keys[slot] = id + 1; values[slot] = value;
        }
        private int slot(int id) {
            int hash = id * 0x9e3779b9; int slot = (hash ^ (hash >>> 16)) & (keys.length - 1);
            while (keys[slot] != 0 && keys[slot] != id + 1) slot = (slot + 1) & (keys.length - 1);
            return slot;
        }
    }
    static boolean sameStructure(MIndexAST left, MIndexAST right) {
        if (left.equals(right)) return true;
        long[] pending = new long[16]; int count = 1; pending[0] = pair(left.contentIndex(), right.contentIndex());
        Pairs visited = new Pairs();
        while (count != 0) {
            long pair = pending[--count]; if (!visited.add(pair)) continue;
            MIndexASTNode a = left.store.node((int) (pair >>> 32)), b = right.store.node((int) pair);
            if (!left.store.symbol(a.kind).equals(right.store.symbol(b.kind)) || a.attributes.length != b.attributes.length || a.children.length != b.children.length) return false;
            for (int i = 0; i < a.attributes.length; i++) if (!left.store.symbol(a.attributes[i]).equals(right.store.symbol(b.attributes[i]))) return false;
            for (int i = 0; i < a.children.length; i++) {
                if (!left.store.symbol(a.roles[i]).equals(right.store.symbol(b.roles[i]))) return false;
                if (count == pending.length) pending = Arrays.copyOf(pending, Math.multiplyExact(count, 2));
                pending[count++] = pair(a.children[i], b.children[i]);
            }
        }
        return true;
    }
    private static long pair(int left, int right) { return ((long) left << 32) | (right & 0xffffffffL); }
    private static final class Pairs {
        private long[] keys = new long[16]; private int size;
        boolean add(long pair) {
            int slot = slot(pair); if (keys[slot] != 0) return false;
            if ((size + 1L) * 2 >= keys.length) {
                long[] old = keys; keys = new long[Math.multiplyExact(old.length, 2)];
                for (long key : old) if (key != 0) keys[slot(key - 1)] = key;
                slot = slot(pair);
            }
            keys[slot] = pair + 1; size++; return true;
        }
        private int slot(long pair) {
            long h = pair ^ (pair >>> 33); h *= 0xff51afd7ed558ccdL; h ^= h >>> 33;
            int slot = (int) h & (keys.length - 1);
            while (keys[slot] != 0 && keys[slot] != pair + 1) slot = (slot + 1) & (keys.length - 1);
            return slot;
        }
    }
}
