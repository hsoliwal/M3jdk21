// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Delta between two scans of the same logical tree, as change lanes ({@code beforeId}, {@code afterId},
 * {@code bits}) that downstream consumes incrementally.
 *
 * <p>Built by a merge-join of the name-sorted CSR child slices of both trees (no hash maps, no path
 * strings): equal names pair up and go through {@link M3FileChangeProvider#change} (attribute gate first,
 * one read only when it trips); a name only in {@code before} emits {@link M3FileChange#REMOVED} for its
 * whole subtree, a name only in {@code after} emits {@link M3FileChange#ADDED} for its whole subtree and
 * observes those added files once so the next delta can gate on them. Unchanged pairs emit nothing.
 * Rows are in name-sorted pre-order. Changed paths are the hand-off to the build delta ledger (#1568).</p>
 */
public final class M3FileTreeDelta {
    private final M3FileTree before;
    private final M3FileTree after;
    private final M3FileChangeProvider provider;
    private final long maxBytes;
    private final M3FileProgress progress;
    private int[] beforeIds = new int[16];
    private int[] afterIds = new int[16];
    private int[] bits = new int[16];
    private int size;

    private M3FileTreeDelta(M3FileTree before, M3FileTree after, M3FileChangeProvider provider, long maxBytes,
            M3FileProgress progress) {
        this.before = before;
        this.after = after;
        this.provider = provider;
        this.maxBytes = maxBytes;
        this.progress = progress;
    }

    /** Delta from {@code before} to {@code after}; progress units are bytes read (one split per file). */
    public static M3FileTreeDelta between(M3FileTree before, M3FileTree after, long maxBytes, M3FileProgress progress,
            M3FileChangeProvider provider) throws InterruptedIOException {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        if (before.rows() == 0 || after.rows() == 0) throw new IllegalArgumentException("empty tree");
        if (maxBytes < 0) throw new IllegalArgumentException("negative maxBytes");
        M3FileTreeDelta delta = new M3FileTreeDelta(before, after, Objects.requireNonNull(provider, "provider"),
                maxBytes, Objects.requireNonNull(progress, "progress"));
        progress.begin("delta " + after.root(), 0);
        delta.pair(0, 0);
        progress.done();
        return delta;
    }

    public M3FileTree before() {
        return before;
    }

    public M3FileTree after() {
        return after;
    }

    /** Change rows. */
    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /** Row id in {@link #before()}, {@code -1} for an added entry. */
    public int beforeId(int row) {
        Objects.checkIndex(row, size);
        return beforeIds[row];
    }

    /** Row id in {@link #after()}, {@code -1} for a removed entry. */
    public int afterId(int row) {
        Objects.checkIndex(row, size);
        return afterIds[row];
    }

    /** {@link M3FileChange} bits of {@code row}. */
    public int bits(int row) {
        Objects.checkIndex(row, size);
        return bits[row];
    }

    /** Delta rows whose bits intersect {@code mask}. */
    public IntStream rows(int mask) {
        return IntStream.range(0, size).filter(row -> (bits[row] & mask) != 0);
    }

    /** Facades over the delta rows, created on demand. */
    public Stream<M3FileChange> stream() {
        return IntStream.range(0, size).mapToObj(row -> new M3FileChange(this, row));
    }

    /** Cold: current path of each row intersecting {@code mask} (the before path for removals). */
    public Stream<Path> paths(int mask) {
        return rows(mask).mapToObj(this::path);
    }

    Path path(int row) {
        return afterIds[row] >= 0 ? after.path(afterIds[row]) : before.path(beforeIds[row]);
    }

    private void pair(int b, int a) throws InterruptedIOException {
        int change = provider.change(before, b, after, a, maxBytes, progress.split(Math.max(0, after.size(a))));
        if (change != 0) add(b, a, change);
        boolean beforeDir = before.flag(M3FileFlag.DIR, b);
        boolean afterDir = after.flag(M3FileFlag.DIR, a);
        if (beforeDir && afterDir) {
            merge(b, a);
            return;
        }
        if (beforeDir) for (int i = 0; i < before.childCount(b); i++) removed(before.childId(b, i));
        if (afterDir) for (int j = 0; j < after.childCount(a); j++) added(after.childId(a, j));
    }

    private void merge(int b, int a) throws InterruptedIOException {
        int i = 0;
        int j = 0;
        int beforeCount = before.childCount(b);
        int afterCount = after.childCount(a);
        while (i < beforeCount || j < afterCount) {
            int left = i < beforeCount ? before.childId(b, i) : -1;
            int right = j < afterCount ? after.childId(a, j) : -1;
            int order = left < 0 ? 1 : right < 0 ? -1
                    : before.names().compare(before.nameId(left), after.names(), after.nameId(right));
            if (order == 0) {
                if (!before.deleted(left) && !after.deleted(right)) pair(left, right);
                i++;
                j++;
            } else if (order < 0) {
                removed(left);
                i++;
            } else {
                added(right);
                j++;
            }
        }
    }

    private void removed(int b) {
        if (before.deleted(b)) return;
        add(b, -1, M3FileChange.REMOVED);
        for (int i = 0; i < before.childCount(b); i++) removed(before.childId(b, i));
    }

    private void added(int a) throws InterruptedIOException {
        if (after.deleted(a)) return;
        int change = M3FileChange.ADDED;
        if (after.flag(M3FileFlag.REGULAR, a)
                && !after.hashRow(a, maxBytes, progress.split(Math.max(0, after.size(a))), provider)) {
            change |= M3FileChange.UNREADABLE;
        }
        add(-1, a, change);
        for (int j = 0; j < after.childCount(a); j++) added(after.childId(a, j));
    }

    private void add(int b, int a, int change) {
        if (size == bits.length) {
            beforeIds = Arrays.copyOf(beforeIds, size * 2);
            afterIds = Arrays.copyOf(afterIds, size * 2);
            bits = Arrays.copyOf(bits, size * 2);
        }
        beforeIds[size] = b;
        afterIds[size] = a;
        bits[size] = change;
        size++;
    }
}
