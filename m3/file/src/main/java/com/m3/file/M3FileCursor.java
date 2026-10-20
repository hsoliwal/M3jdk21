// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Optional;

/**
 * Pull cursor over an {@link M3FileWalk}: same entries, same order as its streams.
 *
 * <p>{@link #nextId()} is allocation-free; {@link #nextEntry()} wraps the facade. {@link #skipSubtree()}
 * prunes the descendants of the entry returned last, the dynamic form of {@code SKIP_SUBTREE}.</p>
 */
public final class M3FileCursor {
    private static final int NOT_PEEKED = -2;
    private static final int END = -1;

    private final M3FileWalk walk;
    private final M3FileWalk.Probe probe;
    private final int end;
    private int position;
    private int peeked = NOT_PEEKED;
    private int last = END;

    M3FileCursor(M3FileWalk walk, int position, int end) {
        this.walk = walk;
        this.probe = walk.probe();
        this.position = position;
        this.end = end;
    }

    public boolean hasNext() {
        if (peeked == NOT_PEEKED) peeked = scan();
        return peeked != END;
    }

    /** Next row id, or {@code -1} at the end. */
    public int nextId() {
        int id = peeked == NOT_PEEKED ? scan() : peeked;
        peeked = NOT_PEEKED;
        last = id;
        return id;
    }

    public Optional<M3File> nextEntry() {
        int id = nextId();
        return id == END ? Optional.empty() : Optional.of(walk.tree().file(id));
    }

    /** Row id returned last, or {@code -1}. */
    public int currentId() {
        return last;
    }

    /** Skips the descendants of the entry returned last. */
    public void skipSubtree() {
        if (last == END) throw new IllegalStateException("no current entry");
        int skipTo = walk.tree().subtreeEnd(last);
        if (peeked == NOT_PEEKED || (peeked != END && peeked < skipTo)) {
            peeked = NOT_PEEKED;
            position = Math.max(position, skipTo);
        }
    }

    private int scan() {
        while (position < end) {
            int id = position;
            position = walk.advance(id);
            if (probe.accepts(id)) return id;
        }
        return END;
    }
}
