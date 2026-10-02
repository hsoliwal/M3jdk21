/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * VM-local weak canonical admission table for stock-JVM M3String values.
 *
 * <p>The table never retains caller String objects or duplicate key payloads. Bucket keys are
 * Java-compatible UTF-16 hashes; exact code-unit comparison resolves collisions. Live values keep
 * their own immutable backing valid even after the weak lookup entry is reclaimed.</p>
 */
final class LocalM3Interner {
    private final LocalM3Arena arena = new LocalM3Arena();
    private final Map<Integer, ArrayList<Entry>> byHash = new HashMap<>();
    private final ReferenceQueue<M3String> reclaimed = new ReferenceQueue<>();

    synchronized M3String intern(String text) {
        Objects.requireNonNull(text, "text");
        expunge();
        int hash = text.hashCode();
        ArrayList<Entry> bucket = byHash.get(hash);
        if (bucket != null) {
            for (int index = bucket.size() - 1; index >= 0; index--) {
                M3String candidate = bucket.get(index).get();
                if (candidate == null) {
                    bucket.remove(index);
                } else if (candidate.contentEquals(text)) {
                    return candidate;
                }
            }
            if (bucket.isEmpty()) byHash.remove(hash);
        }
        M3String created = M3String.fromCanonicalPiece(arena.copyStringUtf16(text), hash);
        byHash.computeIfAbsent(hash, ignored -> new ArrayList<>())
                .add(new Entry(created, reclaimed, hash));
        return created;
    }

    private void expunge() {
        for (Entry dead; (dead = (Entry) reclaimed.poll()) != null; ) {
            ArrayList<Entry> bucket = byHash.get(dead.hash);
            if (bucket == null) continue;
            bucket.remove(dead);
            if (bucket.isEmpty()) byHash.remove(dead.hash);
        }
    }

    private static final class Entry extends WeakReference<M3String> {
        final int hash;

        Entry(M3String value, ReferenceQueue<M3String> queue, int hash) {
            super(value, queue);
            this.hash = hash;
        }
    }
}
