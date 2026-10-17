/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Objects;

/**
 * One-to-one primitive bimap using two primitive hash indexes.
 *
 * <p>Keys and values are duplicated across two flat primitive tables to provide
 * O(1) lookup in both directions; there is still no object per mapping.
 */
public final class PrimitiveBiMap {
    @FunctionalInterface
    public interface EntryConsumer {
        void accept(long keyBits, long valueBits);
    }

    private final PrimitiveKind keyKind;
    private final PrimitiveKind valueKind;
    private final PrimitivePrimitiveHashMap forward;
    private final PrimitivePrimitiveHashMap reverse;
    private PrimitiveBiMap inverse;

    public PrimitiveBiMap(PrimitiveKind keyKind, PrimitiveKind valueKind) {
        this.keyKind = Objects.requireNonNull(keyKind, "keyKind");
        this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
        forward = new PrimitivePrimitiveHashMap(keyKind, valueKind);
        reverse = new PrimitivePrimitiveHashMap(valueKind, keyKind);
    }

    private PrimitiveBiMap(PrimitiveKind keyKind, PrimitiveKind valueKind,
            PrimitivePrimitiveHashMap forward, PrimitivePrimitiveHashMap reverse,
            PrimitiveBiMap inverse) {
        this.keyKind = keyKind;
        this.valueKind = valueKind;
        this.forward = forward;
        this.reverse = reverse;
        this.inverse = inverse;
    }

    public PrimitiveKind keyKind() { return keyKind; }
    public PrimitiveKind valueKind() { return valueKind; }

    /**
     * Returns a live inverse sharing the same two flat indexes. Changes through
     * either view are immediately visible in the other. The inverse view is cached,
     * so {@code map.inverse().inverse() == map}; no object is retained per mapping.
     * Neither view is thread-safe.
     */
    public PrimitiveBiMap inverse() {
        PrimitiveBiMap view = inverse;
        if (view == null) {
            view = new PrimitiveBiMap(valueKind, keyKind, reverse, forward, this);
            inverse = view;
        }
        return view;
    }

    public int size() { return forward.size(); }
    public boolean isEmpty() { return forward.isEmpty(); }
    public long payloadBytes() { return forward.payloadBytes() + reverse.payloadBytes(); }

    public boolean containsKeyBits(long keyBits) { return forward.containsKeyBits(keyBits); }
    public boolean containsValueBits(long valueBits) { return reverse.containsKeyBits(valueBits); }

    public long getValueOrDefaultBits(long keyBits, long defaultBits) {
        return forward.getOrDefaultBits(keyBits, defaultBits);
    }

    public long getKeyOrDefaultBits(long valueBits, long defaultBits) {
        return reverse.getOrDefaultBits(valueBits, defaultBits);
    }

    /**
     * Inserts only when the key and value are both unused, or already map to each other.
     * Returns false for either-side conflicts.
     */
    public boolean putBits(long keyBits, long valueBits) {
        if (forward.containsKeyBits(keyBits)) {
            return valueKind.equalBits(forward.getOrDefaultBits(keyBits, 0L), valueBits);
        }
        if (reverse.containsKeyBits(valueBits)) return false;
        forward.putBits(keyBits, valueBits, 0L);
        reverse.putBits(valueBits, keyBits, 0L);
        return true;
    }

    public boolean removeKeyBits(long keyBits) {
        if (!forward.containsKeyBits(keyBits)) return false;
        long valueBits = forward.getOrDefaultBits(keyBits, 0L);
        forward.removeOrDefaultBits(keyBits, 0L);
        reverse.removeOrDefaultBits(valueBits, 0L);
        return true;
    }

    public boolean removeValueBits(long valueBits) {
        if (!reverse.containsKeyBits(valueBits)) return false;
        long keyBits = reverse.getOrDefaultBits(valueBits, 0L);
        reverse.removeOrDefaultBits(valueBits, 0L);
        forward.removeOrDefaultBits(keyBits, 0L);
        return true;
    }

    /**
     * Removes by key and returns the removed value, or the caller-provided
     * absent carrier when no mapping existed.
     */
    public long removeKeyOrDefaultBits(long keyBits, long absentValueBits) {
        if (!forward.containsKeyBits(keyBits)) return absentValueBits;
        long oldValue = forward.getOrDefaultBits(keyBits, absentValueBits);
        removeKeyBits(keyBits);
        return oldValue;
    }

    /**
     * Removes by value and returns the removed key, or the caller-provided
     * absent carrier when no mapping existed.
     */
    public long removeValueOrDefaultBits(long valueBits, long absentKeyBits) {
        if (!reverse.containsKeyBits(valueBits)) return absentKeyBits;
        long oldKey = reverse.getOrDefaultBits(valueBits, absentKeyBits);
        removeValueBits(valueBits);
        return oldKey;
    }

    public void forEach(EntryConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        forward.forEach(consumer::accept);
    }

    /**
     * Replaces an existing key's value when the new value is not owned by another
     * key. Returns false for an absent key or a value conflict, with no mutation.
     * The legacy insert-only putBits policy is unchanged.
     */
    public boolean replaceBits(long keyBits, long valueBits) {
        if (!forward.containsKeyBits(keyBits)) return false;
        long old = forward.getOrDefaultBits(keyBits, 0L);
        if (valueKind.equalBits(old, valueBits)) return true;
        if (reverse.containsKeyBits(valueBits)) return false;
        forward.putBits(keyBits, valueBits, 0L);
        reverse.removeOrDefaultBits(old, 0L);
        reverse.putBits(valueBits, keyBits, 0L);
        return true;
    }

    /** Conditional replacement; compares values using the declared primitive kind. */
    public boolean replaceBits(long keyBits, long expectedValueBits, long valueBits) {
        return forward.containsKeyBits(keyBits)
                && valueKind.equalBits(forward.getOrDefaultBits(keyBits, 0L), expectedValueBits)
                && replaceBits(keyBits, valueBits);
    }

    /**
     * Map-style put with a strict unique-value policy.
     *
     * <p>An existing key may move to an unused value. If the value belongs to a
     * different key, this method throws {@link IllegalArgumentException} before
     * mutation. The previous stored value is returned, or the caller's unchanged
     * absent carrier. This matches the collision policy used by Guava/Eclipse
     * bimaps without changing the legacy insert-only {@link #putBits(long, long)}.
     */
    public long putStrictBits(long keyBits, long valueBits, long absentValueBits) {
        boolean present = forward.containsKeyBits(keyBits);
        long old = forward.getOrDefaultBits(keyBits, absentValueBits);
        if (present && valueKind.equalBits(old, valueBits)) return old;
        if (reverse.containsKeyBits(valueBits)) {
            throw new IllegalArgumentException("value already belongs to another key");
        }
        if (present) reverse.removeOrDefaultBits(old, 0L);
        forward.putBits(keyBits, valueBits, 0L);
        reverse.putBits(valueBits, keyBits, 0L);
        return old;
    }

    /**
     * Map-style put with value-owner replacement.
     *
     * <p>This is the Commons BidiMap collision policy and is also equivalent to
     * Guava/Eclipse force-put semantics. It delegates to the existing force-put
     * atom so the retained mutation path remains single-owner.
     */
    public long putReplacingBits(long keyBits, long valueBits, long absentValueBits) {
        return forcePutBits(keyBits, valueBits, absentValueBits);
    }

    /**
     * Associates key and value, evicting a different key that currently owns value.
     * Returns key's previous stored value, or the caller's unchanged absent carrier.
     * A semantically identical mapping is retained, including its stored NaN bits.
     */
    public long forcePutBits(long keyBits, long valueBits, long absentValueBits) {
        boolean present = forward.containsKeyBits(keyBits);
        long old = forward.getOrDefaultBits(keyBits, absentValueBits);
        if (present && valueKind.equalBits(old, valueBits)) return old;
        removeKeyBits(keyBits);
        removeValueBits(valueBits);
        forward.putBits(keyBits, valueBits, 0L);
        reverse.putBits(valueBits, keyBits, 0L);
        return old;
    }

    /** Removes a mapping only when both key and expected value match. */
    public boolean removeBits(long keyBits, long expectedValueBits) {
        return forward.containsKeyBits(keyBits)
                && valueKind.equalBits(forward.getOrDefaultBits(keyBits, 0L), expectedValueBits)
                && removeKeyBits(keyBits);
    }

    /** Releases unused index capacity; live inverse views remain valid. */
    public void compact() {
        forward.compact();
        reverse.compact();
    }

    public void clear() {
        forward.clear();
        reverse.clear();
    }
}
