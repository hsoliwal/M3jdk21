/*
 * Copyright (c) 2026, Hitesh Soliwal and Contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation, with the Classpath exception
 * as provided in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details.
 */
package jdk.internal.mindex;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An identity-stable coordinate registry over an existing immutable
 * {@link M3StringBacking}; it never duplicates the backing's text payload.
 *
 * <p>The qualified {@link M3LexiconKey} is an index, not equality proof. Every
 * competing backing coordinate is checked by exact UTF-16 code units, including
 * unpaired surrogates. Content IDs, structural hashes and similarity scores
 * cannot authorize sharing without that check.</p>
 *
 * <p>The caller owns namespace and qualifier allocation. They must distinguish
 * immutable content domains, snapshots, loader/module contexts and schema
 * versions. A semantic occurrence is separately qualified by its runtime owner
 * and generation: no live {@code Class}, object identity, lock, or mutable
 * state is interned. The registry must not outlive its backing owner.</p>
 */
public final class M3QualifiedLexicon {
    private static final String COLLISION = "M3_QUALIFIED_LEXICON_CONTENT_COLLISION";

    private final M3StringBacking backing;
    private final ConcurrentHashMap<M3LexiconKey, Entry> entries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> generations = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1L);

    public M3QualifiedLexicon(M3StringBacking backing) {
        this.backing = Objects.requireNonNull(backing, "backing");
    }

    /**
     * Returns one canonical handle for identical qualified content. Only the
     * winning insertion allocates an entry ID, including under contention.
     */
    public Entry intern(M3LexiconKey key, long backingId) {
        Objects.requireNonNull(key, "key");
        int length = backing.length(backingId);
        if (length < 0) {
            throw new IllegalStateException("backing returned negative UTF-16 length");
        }
        Entry entry = entries.computeIfAbsent(key,
                unused -> new Entry(key, allocateId(), backingId, length));
        return requireEqual(entry, backingId, length);
    }

    /** Finds a handle without resolving or copying its backing text. */
    public Optional<Entry> find(M3LexiconKey key) {
        return Optional.ofNullable(entries.get(Objects.requireNonNull(key, "key")));
    }

    public int size() {
        return entries.size();
    }

    public long generation(long ownerId) {
        checkOwner(ownerId);
        return generations.getOrDefault(ownerId, 0L);
    }

    /** Invalidates a runtime owner's occurrences, not canonical immutable text. */
    public long invalidateOwner(long ownerId) {
        checkOwner(ownerId);
        return generations.compute(ownerId, (id, old) -> {
            if (old == null) {
                return 1L;
            }
            if (old == Long.MAX_VALUE) {
                throw new IllegalStateException("owner generation exhausted");
            }
            return old + 1L;
        });
    }

    /** Emits a qualified semantic occurrence without constructing its Java object. */
    public M3OccurrenceKey occurrence(Entry entry, long ownerId, long occurrenceId) {
        requireOwnedEntry(entry);
        checkOwner(ownerId);
        if (occurrenceId < 0L) {
            throw new IllegalArgumentException("negative occurrence id");
        }
        return new M3OccurrenceKey(entry.key, ownerId,
                generations.getOrDefault(ownerId, 0L), occurrenceId);
    }

    /**
     * Checks key and owner generation, not Java object identity or provenance.
     * The caller must allocate unrecycled owner and namespace coordinates.
     */
    public boolean isCurrent(Entry entry, M3OccurrenceKey occurrence) {
        Objects.requireNonNull(occurrence, "occurrence");
        requireOwnedEntry(entry);
        return entry.key.equals(occurrence.lexiconEntry())
                && generations.getOrDefault(occurrence.ownerId(), 0L)
                    == occurrence.generation();
    }

    private Entry requireEqual(Entry entry, long backingId, int length) {
        if (entry.backingId != backingId) {
            if (entry.utf16Length != length) {
                throw new IllegalArgumentException(COLLISION);
            }
            for (int index = 0; index < length; index++) {
                if (backing.charAt(entry.backingId, index)
                        != backing.charAt(backingId, index)) {
                    throw new IllegalArgumentException(COLLISION);
                }
            }
        }
        return entry;
    }

    private void requireOwnedEntry(Entry entry) {
        Objects.requireNonNull(entry, "entry");
        if (entries.get(entry.key) != entry) {
            throw new IllegalArgumentException("foreign qualified lexicon entry");
        }
    }

    private long allocateId() {
        for (;;) {
            long candidate = nextId.get();
            if (candidate == Long.MAX_VALUE) {
                throw new IllegalStateException("qualified lexicon id exhausted");
            }
            if (nextId.compareAndSet(candidate, candidate + 1L)) {
                return candidate;
            }
        }
    }

    private static void checkOwner(long ownerId) {
        if (ownerId < 0L) {
            throw new IllegalArgumentException("negative owner id");
        }
    }

    /** Stable internal handle containing only a key, backing ID and length. */
    public static final class Entry {
        private final M3LexiconKey key;
        private final long entryId;
        private final long backingId;
        private final int utf16Length;

        private Entry(M3LexiconKey key, long entryId, long backingId, int utf16Length) {
            this.key = key;
            this.entryId = entryId;
            this.backingId = backingId;
            this.utf16Length = utf16Length;
        }

        public M3LexiconKey key() { return key; }
        public long entryId() { return entryId; }
        public long backingId() { return backingId; }
        public int utf16Length() { return utf16Length; }
    }
}
