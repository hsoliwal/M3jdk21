// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.ast;

import com.synexia.mindex.MIndexString;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

/**
 * Compatibility facade for an explicit kind/payload arena or an immutable attributed coordinate.
 * Coordinate instances allocate no arena arrays. The domains never reinterpret each other's IDs.
 */
public final class MIndexAST {
    private static final int[] NO_CHILDREN = new int[0];
    private static final MIndexString EMPTY = MIndexString.literal("");
    private final Arena arena;
    final MIndexASTStore store;
    private final int id;

    public MIndexAST(int maxNodes, int maxEdges) {
        if (maxNodes < 1 || maxNodes > (1 << 28) || maxEdges < 0) throw new IllegalArgumentException("invalid AST resource limits");
        arena = new Arena(maxNodes, maxEdges); store = null; id = -1;
    }
    MIndexAST(MIndexASTStore store, int id) {
        this.store = Objects.requireNonNull(store, "store"); this.id = id; arena = null;
    }

    public Ref atom(MIndexString kind, MIndexString payload) { return ref(atomId(kind, payload)); }
    public int atomId(MIndexString kind, MIndexString payload) { return internIds(kind, payload, NO_CHILDREN, 0, 0); }
    public Ref node(MIndexString kind, Ref... children) {
        Objects.requireNonNull(children, "children");
        int[] ids = new int[children.length];
        for (int i = 0; i < ids.length; i++) { requireLocal(children[i]); ids[i] = children[i].id; }
        return ref(internIds(kind, EMPTY, ids, 0, ids.length));
    }
    /** Object boundary adapter. Use internIds for repeated primitive admission/lookups. */
    public Ref intern(MIndexString kind, MIndexString payload, List<Ref> children) {
        Objects.requireNonNull(children, "children");
        int[] ids = new int[children.size()];
        for (int i = 0; i < ids.length; i++) { Ref child = children.get(i); requireLocal(child); ids[i] = child.id; }
        return ref(internIds(kind, payload, ids, 0, ids.length));
    }
    /** IDs are local to this arena. The slice is borrowed during this call and copied on admission. */
    public synchronized int internIds(MIndexString kind, MIndexString payload, int[] children, int offset, int count) {
        Arena a = arena();
        Objects.requireNonNull(kind, "kind"); Objects.requireNonNull(payload, "payload");
        Objects.checkFromIndexSize(offset, count, children.length);
        if (kind.isEmpty()) throw new IllegalArgumentException("empty kind");
        int hash = 31 * kind.hashCode() + payload.hashCode();
        for (int i = 0; i < count; i++) hash = 31 * hash + Objects.checkIndex(children[offset + i], a.size);
        int slot = mix(hash) & (a.slots.length - 1);
        int kindId = kind.contentIndex(), payloadId = payload.contentIndex();
        while (a.slots[slot] != 0) {
            int node = a.slots[slot] - 1;
            if (a.hashes[node] == hash && a.kinds[node] == kindId && a.payloads[node] == payloadId
                    && a.lengths[node] == count && Arrays.equals(a.edges, a.starts[node], a.starts[node] + count, children, offset, offset + count)) return node;
            slot = (slot + 1) & (a.slots.length - 1);
        }
        if (a.size == a.maxNodes || count > a.maxEdges - a.edgeCount) throw new IllegalStateException("AST resource limit");
        a.ensureCapacity(count);
        int node = a.size;
        a.kinds[node] = kindId; a.payloads[node] = payloadId; a.starts[node] = a.edgeCount;
        a.lengths[node] = count; a.hashes[node] = hash;
        System.arraycopy(children, offset, a.edges, a.edgeCount, count);
        a.edgeCount += count; a.size++; a.insert(node);
        return node;
    }
    public synchronized int size() { return arena().size; }
    public synchronized int edgeCount() { return arena().edgeCount; }
    public synchronized int materializedReferences() { return arena().referenceCount; }
    public synchronized Ref ref(int node) {
        Arena a = arena(); Objects.checkIndex(node, a.size);
        if (a.refs[node] == null) { a.refs[node] = new Ref(this, node); a.referenceCount++; }
        return a.refs[node];
    }
    public synchronized MIndexString kind(int node) { Arena a = arena(); return MIndexString.fromContentIndex(a.kinds[Objects.checkIndex(node, a.size)]); }
    public synchronized MIndexString payload(int node) { Arena a = arena(); return MIndexString.fromContentIndex(a.payloads[Objects.checkIndex(node, a.size)]); }
    public synchronized int childCount(int node) { Arena a = arena(); return a.lengths[Objects.checkIndex(node, a.size)]; }
    public synchronized int childId(int node, int ordinal) {
        Arena a = arena(); Objects.checkIndex(node, a.size); Objects.checkIndex(ordinal, a.lengths[node]);
        return a.edges[a.starts[node] + ordinal];
    }
    public Ref replaceChild(Ref parent, int ordinal, Ref replacement) {
        requireLocal(parent); requireLocal(replacement);
        return ref(replaceChildId(parent.id, ordinal, replacement.id));
    }
    public synchronized int replaceChildId(int parent, int ordinal, int replacement) {
        Arena a = arena(); Objects.checkIndex(parent, a.size); Objects.checkIndex(replacement, a.size);
        Objects.checkIndex(ordinal, a.lengths[parent]);
        if (a.edges[a.starts[parent] + ordinal] == replacement) return parent;
        int[] children = Arrays.copyOfRange(a.edges, a.starts[parent], a.starts[parent] + a.lengths[parent]);
        children[ordinal] = replacement;
        return internIds(kind(parent), payload(parent), children, 0, children.length);
    }
    public Ref replacePath(Ref root, int[] path, Ref replacement) {
        requireLocal(root); requireLocal(replacement); Objects.requireNonNull(path, "path");
        int[] parents = new int[path.length]; int current = root.id;
        for (int i = 0; i < path.length; i++) { parents[i] = current; current = childId(current, path[i]); }
        current = replacement.id;
        for (int i = path.length - 1; i >= 0; i--) current = replaceChildId(parents[i], path[i], current);
        return ref(current);
    }
    public void requireLocal(Ref value) {
        arena(); if (value == null || value.owner != this) throw new IllegalArgumentException("foreign AST handle");
    }
    private Arena arena() { if (arena == null) throw new IllegalStateException("operation requires kind/payload arena"); return arena; }

    // Attributed-coordinate API retained from the merged MIndexASTPool implementation.
    public int contentIndex() { row(); return id; }
    /**
     * Immutable attributed coordinate prefix through this root, sharing existing immutable rows.
     * Later pool appends cannot affect its size, symbols or numeric coordinates. Earlier prefix
     * rows remain necessary to preserve IDs. This copies reference tables, never AST row payloads.
     */
    public MIndexASTSnapshot snapshot() {
        return snapshot(Integer.MAX_VALUE);
    }
    /** Admission checks the coordinate-prefix budget before allocating snapshot reference tables. */
    public MIndexASTSnapshot snapshot(int maxNodes) {
        row();
        if (maxNodes < 1 || id >= maxNodes) throw new IllegalArgumentException("AST snapshot prefix node budget");
        if (store instanceof MIndexASTSnapshot snapshot && snapshot.size() == id + 1) return snapshot;
        if (!(store instanceof MIndexASTPool) && !(store instanceof MIndexASTSnapshot))
            throw new IllegalStateException("unknown attributed AST owner");
        synchronized (store) {
            var rows = new java.util.ArrayList<MIndexASTNode>(id + 1);
            int lastSymbol = -1;
            for (int coordinate = 0; coordinate <= id; coordinate++) {
                MIndexASTNode node = store.node(coordinate);
                rows.add(node);
                lastSymbol = Math.max(lastSymbol, node.kind);
                for (int symbol : node.attributes) lastSymbol = Math.max(lastSymbol, symbol);
                for (int symbol : node.roles) lastSymbol = Math.max(lastSymbol, symbol);
            }
            var symbols = new java.util.ArrayList<com.synexia.mindex.experiment.MIndexString>(lastSymbol + 1);
            for (int symbol = 0; symbol <= lastSymbol; symbol++) symbols.add(store.symbol(symbol));
            return new MIndexASTSnapshot(rows, symbols);
        }
    }
    public com.synexia.mindex.experiment.MIndexString kind() { return store.symbol(row().kind); }
    public int attributeCount() { return row().attributes.length / 2; }
    public com.synexia.mindex.experiment.MIndexString attributeName(int index) { return store.symbol(row().attributes[2 * Objects.checkIndex(index, attributeCount())]); }
    public com.synexia.mindex.experiment.MIndexString attributeValue(int index) { return store.symbol(row().attributes[2 * Objects.checkIndex(index, attributeCount()) + 1]); }
    public int childCount() { return row().children.length; }
    public com.synexia.mindex.experiment.MIndexString childRole(int index) { return store.symbol(row().roles[Objects.checkIndex(index, childCount())]); }
    public MIndexAST child(int index) { return new MIndexAST(store, row().children[Objects.checkIndex(index, childCount())]); }
    public int[] childIndices() { return row().children.clone(); }
    public int depth() { return row().depth; }
    public long expandedSize() { return row().expandedSize; }
    public String fingerprint() { return row().fingerprint; }
    public int reachableAtoms() { return MIndexASTTraversal.order(this).size(); }
    public boolean sameStructure(MIndexAST other) { row(); Objects.requireNonNull(other, "other").row(); return MIndexASTTraversal.sameStructure(this, other); }
    BitSet reachable() {
        MIndexASTTraversal.Order order = MIndexASTTraversal.order(this); BitSet seen = new BitSet();
        for (int i = 0; i < order.size(); i++) seen.set(order.nodeId(i)); return seen;
    }
    private MIndexASTNode row() { if (store == null) throw new IllegalStateException("operation requires attributed coordinate"); return store.node(id); }
    @Override public boolean equals(Object other) {
        return this == other || store != null && other instanceof MIndexAST value && store == value.store && id == value.id;
    }
    @Override public int hashCode() { return store == null ? System.identityHashCode(this) : 31 * System.identityHashCode(store) + id; }
    @Override public String toString() { return store == null ? "MIndexAST[arena,nodes=" + size() + "]" : "MIndexAST[" + id + ", " + kind() + ", children=" + childCount() + "]"; }

    /** Canonical boundary handle; admission by primitive ID does not materialize it. */
    public static final class Ref {
        private final MIndexAST owner; private final int id;
        private Ref(MIndexAST owner, int id) { this.owner = owner; this.id = id; }
        public MIndexAST owner() { return owner; }
        public int id() { return id; }
        public MIndexString kind() { return owner.kind(id); }
        public MIndexString payload() { return owner.payload(id); }
        public int childCount() { return owner.childCount(id); }
        public Ref child(int ordinal) { return owner.ref(owner.childId(id, ordinal)); }
        @Override public boolean equals(Object other) { return this == other || other instanceof Ref ref && owner == ref.owner && id == ref.id; }
        @Override public int hashCode() { return 31 * System.identityHashCode(owner) + id; }
        @Override public String toString() { return "MIndexAST[" + id + "," + kind() + "]"; }
    }
    private static int mix(int value) { int x = value ^ (value >>> 16); x *= 0x7feb352d; return x ^ (x >>> 15); }
    /** Parallel columns and one flat edge arena; no node objects in the primitive storage path. */
    private static final class Arena {
        private final int maxNodes, maxEdges;
        private int[] kinds = new int[16], payloads = new int[16], starts = new int[16], lengths = new int[16], hashes = new int[16];
        private Ref[] refs = new Ref[16];
        private int[] edges = new int[32], slots = new int[32];
        private int size, edgeCount, referenceCount;
        private Arena(int maxNodes, int maxEdges) { this.maxNodes = maxNodes; this.maxEdges = maxEdges; }
        private void ensureCapacity(int count) {
            if (size == kinds.length) {
                int capacity = Math.min(maxNodes, Math.multiplyExact(size, 2));
                kinds = Arrays.copyOf(kinds, capacity); payloads = Arrays.copyOf(payloads, capacity);
                starts = Arrays.copyOf(starts, capacity); lengths = Arrays.copyOf(lengths, capacity);
                hashes = Arrays.copyOf(hashes, capacity); refs = Arrays.copyOf(refs, capacity);
            }
            int required = Math.addExact(edgeCount, count);
            if (required > edges.length) edges = Arrays.copyOf(edges, (int) Math.min(maxEdges, Math.max((long) required, (long) edges.length * 2)));
            if ((size + 1L) * 2 >= slots.length) {
                slots = new int[Math.multiplyExact(slots.length, 2)];
                for (int i = 0; i < size; i++) insert(i);
            }
        }
        private void insert(int node) {
            int slot = mix(hashes[node]) & (slots.length - 1);
            while (slots[slot] != 0) slot = (slot + 1) & (slots.length - 1);
            slots[slot] = node + 1;
        }
    }
}
