package com.synexia.mindex.experiment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Caller-owned bounded interner. Capacity admission is checked before publishing tokens. */
public final class MIndexPool implements TokenSpace {
    private final List<String> tokens;
    private final Map<String, Integer> tokenIds = new HashMap<>();
    private final Map<Integer, List<MIndexString>> buckets = new HashMap<>();
    private final int maxTokens;
    private final int maxValues;
    private int valueCount;

    public MIndexPool(TokenDictionary base, int maxTokens, int maxValues) {
        Objects.requireNonNull(base, "base");
        if (maxTokens < base.size() || maxValues < 1) { throw new IllegalArgumentException("capacity"); }
        this.maxTokens = maxTokens;
        this.maxValues = maxValues;
        tokens = new ArrayList<>(base.tokens());
        for (int i = 0; i < tokens.size(); i++) { tokenIds.put(tokens.get(i), i); }
    }
    public Object namespace() { return this; }
    public synchronized int size() { return tokens.size(); }
    public synchronized String token(int id) { return tokens.get(id); }
    public synchronized int valueCount() { return valueCount; }

    public synchronized MIndexString intern(String text) {
        Objects.requireNonNull(text, "text");
        List<MIndexString> bucket = buckets.getOrDefault(text.hashCode(), List.of());
        for (MIndexString value : bucket) { if (value.contentEquals(text)) { return value; } }
        List<String> parts = Tokenizer.split(text);
        var newTokens = new LinkedHashSet<>(parts);
        newTokens.removeAll(tokenIds.keySet());
        if (valueCount >= maxValues || newTokens.size() > maxTokens - tokens.size()) {
            throw new CapacityException();
        }
        for (String part : newTokens) { tokenIds.put(part, tokens.size()); tokens.add(part); }
        int[] ids = new int[parts.size()];
        for (int i = 0; i < ids.length; i++) { ids[i] = tokenIds.get(parts.get(i)); }
        MIndexString value = MIndexString.of(this, ids);
        buckets.computeIfAbsent(text.hashCode(), ignored -> new ArrayList<>()).add(value);
        valueCount++;
        return value;
    }

    /** Explicit bounded-pool admission failure. */
    public static final class CapacityException extends IllegalStateException {
        private static final long serialVersionUID = 1L;
        CapacityException() { super("MIndex pool capacity exceeded"); }
    }
}
