// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Weak canonicalization for immutable values when GC reclaimability is more important than stable
 * ids. Equal live values share one representative while that representative remains reachable.
 */
//FLLDCIM: RELOCATE - donor blob 94b300cf78a8 at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/recipe/immutable-pools-20261106/PROVENANCE.tsv); recipe m3/collections/recipe/immutable-pools-20261106/relocate.py v2
public final class WeakValueCanonicalizer<T> {
    private final Map<T, WeakReference<T>> values = new WeakHashMap<>();
    private long hits;
    private long inserts;

    /** Explicit constructor: the exported module package lint (missing-explicit-ctor) refuses the default one. */
    public WeakValueCanonicalizer() { }

    public synchronized T canonicalize(T value) {
        Objects.requireNonNull(value, "value");
        WeakReference<T> reference = values.get(value);
        T existing = reference == null ? null : reference.get();
        if (existing != null) {
            hits++;
            return existing;
        }
        values.put(value, new WeakReference<>(value));
        inserts++;
        return value;
    }

    /** Approximate because WeakHashMap expunges collected keys opportunistically. */
    public synchronized int approximateSize() {
        return values.size();
    }

    public synchronized long hits() { return hits; }
    public synchronized long inserts() { return inserts; }

    public synchronized void clear() {
        values.clear();
    }
}
