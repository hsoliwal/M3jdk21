// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.ast;

import com.synexia.mindex.experiment.MIndexPool;
import com.synexia.mindex.experiment.MIndexString;
import com.synexia.mindex.experiment.TokenDictionary;
import com.synexia.mindex.experiment.TokenSpace;
import com.synexia.mindex.experiment.Tokenizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Caller-owned append-only atom arena. Exact primitive tuples resolve every hash collision.
 * The monitor covers validation and publication; independently held handles never change.
 */
public final class MIndexASTPool extends MIndexASTStore {
    private final MIndexASTLimits limits;
    private final MIndexPool text;
    private final TokenSpace textView;
    private final ArrayList<MIndexASTNode> rows = new ArrayList<>();
    private final ArrayList<MIndexString> symbols = new ArrayList<>();
    private final Map<String, Integer> symbolIds = new HashMap<>();
    private final Set<String> tokens = new HashSet<>();
    private int[] slots = new int[16];
    private int edgeCount;
    private int attributeCount;
    private long symbolChars;

    public MIndexASTPool(MIndexASTLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits");
        text = new MIndexPool(TokenDictionary.of(List.of()), limits.tokens(), limits.symbols());
        textView = TokenSpace.adapt(new Object(), text::size, text::token);
        tokens.add(" ");
    }

    MIndexASTPool(MIndexASTSnapshot base, MIndexASTLimits limits) {
        this(limits);
        if (base.size() > limits.nodes()) { throw capacity("base nodes"); }
        for (MIndexString value : base.symbols) { addBaseSymbol(value.toString()); }
        for (MIndexASTNode row : base.rows) {
            requireCapacity(row.attributes.length / 2, row.children.length, row.depth);
            rows.add(row);
            attributeCount += row.attributes.length / 2;
            edgeCount += row.children.length;
        }
        rebuildSlots();
    }

    public MIndexASTLimits limits() { return limits; }
    @Override public synchronized int size() { return rows.size(); }
    public synchronized int symbolCount() { return symbols.size(); }
    public synchronized int edgeCount() { return edgeCount; }
    public synchronized int attributeCount() { return attributeCount; }
    public synchronized long retainedSymbolChars() { return symbolChars; }
    @Override synchronized MIndexASTNode node(int id) { return rows.get(id); }
    @Override synchronized MIndexString symbol(int id) { return symbols.get(id); }
    public synchronized MIndexAST ref(int id) { return new MIndexAST(this, Objects.checkIndex(id, rows.size())); }

    /** A leaf payload is an explicit attribute, so empty and absent remain distinct. */
    public MIndexAST leaf(String kind, String value) { return intern(kind, Map.of("value", value), List.of()); }

    /** Normalize and validate all caller data before adding any symbols or atom rows. */
    public synchronized MIndexAST intern(String kind, Map<String, String> attributes, List<MIndexASTEdge> edges) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(attributes, "attributes");
        Objects.requireNonNull(edges, "edges");
        if (attributes.size() > limits.attributes() || edges.size() > limits.edges()) { throw capacity("input row"); }
        TreeMap<String, String> sorted = new TreeMap<>();
        attributes.forEach((name, value) -> sorted.put(Objects.requireNonNull(name, "attribute name"),
                Objects.requireNonNull(value, "attribute value")));
        List<MIndexASTEdge> copied = List.copyOf(edges);
        int depth = 1;
        long expanded = 1;
        int[] children = new int[copied.size()];
        for (int i = 0; i < children.length; i++) {
            MIndexAST child = copied.get(i).child();
            requireLocal(child);
            children[i] = child.contentIndex();
            MIndexASTNode row = rows.get(children[i]);
            if (row.depth >= limits.depth()) { throw capacity("depth"); }
            depth = Math.max(depth, row.depth + 1);
            expanded = saturatedAdd(expanded, row.expandedSize);
        }
        LinkedHashMap<String, Integer> extra = new LinkedHashMap<>();
        int kindId = candidateSymbol(kind, extra);
        int[] attrs = new int[Math.multiplyExact(sorted.size(), 2)];
        int cursor = 0;
        for (var entry : sorted.entrySet()) {
            attrs[cursor++] = candidateSymbol(entry.getKey(), extra);
            attrs[cursor++] = candidateSymbol(entry.getValue(), extra);
        }
        int[] roles = new int[copied.size()];
        for (int i = 0; i < roles.length; i++) { roles[i] = candidateSymbol(copied.get(i).role(), extra); }
        int hash = MIndexASTNode.keyHash(kindId, attrs, roles, children);
        if (extra.isEmpty()) {
            int found = find(hash, kindId, attrs, roles, children);
            if (found >= 0) { return ref(found); }
        }
        requireCapacity(attrs.length / 2, children.length, depth);
        admitSymbols(extra.keySet());
        String fingerprint = MIndexASTFingerprint.of(this, kindId, attrs, roles, children);
        int id = rows.size();
        rows.add(new MIndexASTNode(kindId, attrs, roles, children, depth, expanded, fingerprint));
        attributeCount += attrs.length / 2;
        edgeCount += children.length;
        if (rows.size() > slots.length / 2) { rebuildSlots(); }
        else { insert(id); }
        return ref(id);
    }

    public synchronized MIndexASTSnapshot snapshot() { return new MIndexASTSnapshot(rows, symbols); }

    /** Import exact reachable structure; unrelated numeric IDs are never reused as identities. */
    public MIndexAST importTree(MIndexAST source) {
        Objects.requireNonNull(source, "source");
        if (source.store == this) { return source; }
        // Read foreign stores without holding this pool monitor: reciprocal imports cannot deadlock.
        MIndexASTTraversal.Order order = MIndexASTTraversal.order(source); int[] mapped = new int[order.size()];
        for (int ordinal = 0; ordinal < order.size(); ordinal++) {
            MIndexAST current = new MIndexAST(source.store, order.nodeId(ordinal));
            List<MIndexASTEdge> children = new ArrayList<>(current.childCount());
            for (int i = 0; i < current.childCount(); i++) {
                int child = mapped[order.ordinal(current.child(i).contentIndex())];
                children.add(new MIndexASTEdge(current.childRole(i).toString(), ref(child)));
            }
            mapped[ordinal] = intern(current.kind().toString(), attributes(current), children).contentIndex();
        }
        return ref(mapped[mapped.length - 1]);
    }

    /** Pure DAG substitution: primitive postorder ordinals preserve every shared occurrence. */
    public synchronized MIndexAST replace(MIndexAST root, MIndexAST target, MIndexAST replacement) {
        requireLocal(root); requireLocal(target); requireLocal(replacement);
        if (target.equals(replacement)) { return root; }
        MIndexASTTraversal.Order order = MIndexASTTraversal.order(root);
        if (order.ordinal(target.contentIndex()) < 0) { return root; }
        int[] mapped = new int[order.size()];
        for (int ordinal = 0; ordinal < order.size(); ordinal++) {
            int id = order.nodeId(ordinal);
            if (id == target.contentIndex()) { mapped[ordinal] = replacement.contentIndex(); continue; }
            MIndexASTNode current = rows.get(id); boolean changed = false;
            for (int child : current.children) changed |= mapped[order.ordinal(child)] != child;
            if (!changed) { mapped[ordinal] = id; continue; }
            int[] children = new int[current.children.length];
            for (int i = 0; i < children.length; i++) children[i] = mapped[order.ordinal(current.children[i])];
            mapped[ordinal] = internCoordinates(current, children);
        }
        return ref(mapped[mapped.length - 1]);
    }

    /** Existing symbols/attribute/role lanes are immutable and can be borrowed without copying. */
    private int internCoordinates(MIndexASTNode source, int[] children) {
        int hash = MIndexASTNode.keyHash(source.kind, source.attributes, source.roles, children);
        int found = find(hash, source.kind, source.attributes, source.roles, children);
        if (found >= 0) { return found; }
        int depth = 1; long expanded = 1;
        for (int child : children) {
            MIndexASTNode row = rows.get(child);
            if (row.depth >= limits.depth()) { throw capacity("depth"); }
            depth = Math.max(depth, row.depth + 1); expanded = saturatedAdd(expanded, row.expandedSize);
        }
        requireCapacity(source.attributes.length / 2, children.length, depth);
        String fingerprint = MIndexASTFingerprint.of(this, source.kind, source.attributes, source.roles, children);
        int id = rows.size(); rows.add(new MIndexASTNode(source.kind, source.attributes, source.roles, children, depth, expanded, fingerprint));
        attributeCount += source.attributes.length / 2; edgeCount += children.length;
        if (rows.size() > slots.length / 2) { rebuildSlots(); } else { insert(id); }
        return id;
    }

    private static Map<String, String> attributes(MIndexAST value) {
        Map<String, String> result = new TreeMap<>();
        for (int i = 0; i < value.attributeCount(); i++) {
            result.put(value.attributeName(i).toString(), value.attributeValue(i).toString());
        }
        return result;
    }

    private void requireLocal(MIndexAST value) {
        Objects.requireNonNull(value, "atom");
        if (value.store != this) { throw new IllegalArgumentException("foreign AST domain; use importTree"); }
    }

    private int candidateSymbol(String value, LinkedHashMap<String, Integer> extra) {
        Integer found = symbolIds.get(value);
        if (found != null) { return found; }
        return extra.computeIfAbsent(value, ignored -> Math.addExact(symbols.size(), extra.size()));
    }

    private void requireCapacity(int addedAttributes, int addedEdges, int depth) {
        if (rows.size() >= limits.nodes() || addedAttributes > limits.attributes() - attributeCount
                || addedEdges > limits.edges() - edgeCount || depth > limits.depth()) { throw capacity("atom arena"); }
    }

    private void addBaseSymbol(String value) { admitSymbols(Set.of(value)); }

    private void admitSymbols(Set<String> extra) {
        if (extra.size() > limits.symbols() - symbols.size()) { throw capacity("symbols"); }
        Set<String> newTokens = new HashSet<>();
        long addedChars = 0;
        for (String value : extra) {
            addedChars = Math.addExact(addedChars, value.length());
            if (addedChars > limits.symbolChars() - symbolChars) { throw capacity("symbol characters"); }
            for (String token : Tokenizer.split(value)) { if (!tokens.contains(token)) { newTokens.add(token); } }
            if (newTokens.size() > limits.tokens() - tokens.size()) { throw capacity("tokens"); }
        }
        // The private MIndexPool cannot be mutated by another caller; this preflight matches its admission.
        for (String value : extra) {
            MIndexString indexed = MIndexString.of(textView, text.intern(value).tokenIds());
            symbolIds.put(value, symbols.size());
            symbols.add(indexed);
        }
        tokens.addAll(newTokens);
        symbolChars += addedChars;
    }

    private int find(int hash, int kind, int[] attrs, int[] roles, int[] children) {
        int slot = mix(hash) & (slots.length - 1);
        while (slots[slot] != 0) {
            int id = slots[slot] - 1;
            MIndexASTNode row = rows.get(id);
            if (row.hash == hash && row.matches(kind, attrs, roles, children)) { return id; }
            slot = (slot + 1) & (slots.length - 1);
        }
        return -1;
    }

    private void rebuildSlots() {
        int length = 16;
        while (rows.size() > length / 2) { length = Math.multiplyExact(length, 2); }
        slots = new int[length];
        for (int id = 0; id < rows.size(); id++) { insert(id); }
    }

    private void insert(int id) {
        int slot = mix(rows.get(id).hash) & (slots.length - 1);
        while (slots[slot] != 0) { slot = (slot + 1) & (slots.length - 1); }
        slots[slot] = id + 1;
    }

    private static int mix(int hash) { return hash ^ (hash >>> 16); }
    private static long saturatedAdd(long left, long right) { return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right; }
    private static IllegalStateException capacity(String kind) { return new IllegalStateException("MIndexAST capacity: " + kind); }
}
