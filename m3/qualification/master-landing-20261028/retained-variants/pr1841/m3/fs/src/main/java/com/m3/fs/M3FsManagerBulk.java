/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import java.util.Arrays;
import java.util.Objects;

import com.m3.fs.vfs.FileObject;
import com.m3.fs.vfs.FileSystemException;
import com.m3.fs.vfs.FileSystemManager;
import com.m3.fs.vfs.Selectors;

/**
 * Ordered bulk namespace operations across the providers registered in a VFS manager.
 *
 * <p>Commands retain their original absolute or manager-relative path strings in
 * {@link M3FsBulk.Batch}; the manager alone resolves each source and destination.
 * This preserves provider-specific path parsing and cross-provider copy/move
 * behavior. There is no cross-provider transaction, consistent snapshot, or
 * automatic retry. Providers with genuine native batching should instead use
 * their filesystem-specific {@code executeM3Batch} hook for homogeneous work.
 *
 * <p>A caller-supplied primitive mask selects physical command slots (bit {@code i}
 * means command {@code i}). The mask is copied before any provider callback,
 * preserving selection even if a callback changes the caller's array. Outside
 * the half-open window, or when the bit is clear, results remain SKIPPED. No
 * per-row Node, Entry, or command wrapper is stored in the execution engine.
 */
public final class M3FsManagerBulk {
    private M3FsManagerBulk() { }

    /** Execute all commands in encounter order. */
    public static M3FsBulk.Result execute(FileSystemManager manager, M3FsBulk.Batch batch) {
        return execute(manager, batch, 0, Objects.requireNonNull(batch, "batch").size(), null);
    }

    /** Execute only command slots whose bits are set, in encounter order. */
    public static M3FsBulk.Result execute(FileSystemManager manager, M3FsBulk.Batch batch,
                                          long[] selectedSlots) {
        return execute(manager, batch, 0, Objects.requireNonNull(batch, "batch").size(), selectedSlots);
    }

    /**
     * Execute selected commands in {@code [first, first + count)}. Mask bits use
     * absolute batch indexes, not window-relative indexes. An absent mask selects
     * every command in the window. Statuses outside the selection are SKIPPED.
     * Each provider operation is individually committed or failed, never batch-atomic.
     */
    public static M3FsBulk.Result execute(FileSystemManager manager, M3FsBulk.Batch batch,
                                          int first, int count, long[] selectedSlots) {
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(batch, "batch");
        Objects.checkFromIndexSize(first, count, batch.size());
        int maskWords = (batch.size() >>> 6) + ((batch.size() & 63) == 0 ? 0 : 1);
        if (selectedSlots != null && selectedSlots.length != maskWords) {
            throw new IllegalArgumentException("selection mask must cover exactly the batch size");
        }
        if (batch.atomic()) {
            throw new UnsupportedOperationException("multi-provider bulk cannot promise atomicity");
        }
        long[] selected = selectedSlots == null ? null : selectedSlots.clone();
        byte[] statuses = new byte[batch.size()];
        Arrays.fill(statuses, M3FsBulk.SKIPPED);
        int[] metadata = new int[batch.size()];
        FileSystemException[] failures = new FileSystemException[batch.size()];
        for (int i = first, end = first + count; i < end;) {
            if (selected != null) {
                int word = i >>> 6;
                long candidates = selected[word] & (-1L << (i & 63));
                if (candidates == 0L) {
                    // Jump over an empty 64-bit word without visiting its rows.
                    // Widen before shifting to avoid overflow at very large batch sizes.
                    i = (int) Math.min((long) end, ((long) word + 1L) << 6);
                    continue;
                }
                i = (word << 6) + Long.numberOfTrailingZeros(candidates);
                if (i >= end) break;
            }
            batch.progress().checkCanceled();
            try {
                statuses[i] = perform(manager, batch, i, metadata);
            } catch (FileSystemException failure) {
                failures[i] = failure;
                statuses[i] = M3FsBulk.FAILED;
                if (batch.failFast()) {
                    batch.progress().worked(1);
                    break;
                }
            }
            batch.progress().worked(1);
            i++;
        }
        return new M3FsBulk.Result(statuses, metadata, failures);
    }

    private static byte perform(FileSystemManager manager, M3FsBulk.Batch batch,
                                int index, int[] metadata) throws FileSystemException {
        FileObject source = manager.resolveFile(batch.source(index));
        return switch (batch.operation(index)) {
            case M3FsBulk.STAT -> {
                int bits = M3FsBulk.META_CHECKED;
                if (source.exists()) {
                    bits |= M3FsBulk.META_EXISTS;
                    if (source.isFile()) bits |= M3FsBulk.META_FILE;
                    if (source.isFolder()) bits |= M3FsBulk.META_DIRECTORY;
                }
                metadata[index] = bits;
                yield M3FsBulk.OK;
            }
            case M3FsBulk.MKDIR -> {
                source.createFolder();
                yield M3FsBulk.OK;
            }
            case M3FsBulk.DELETE -> source.delete() ? M3FsBulk.OK : M3FsBulk.NO_EFFECT;
            case M3FsBulk.DELETE_TREE -> source.deleteAll() > 0 ? M3FsBulk.OK : M3FsBulk.NO_EFFECT;
            case M3FsBulk.COPY -> {
                manager.resolveFile(batch.target(index)).copyFrom(source, Selectors.SELECT_ALL);
                yield M3FsBulk.OK;
            }
            case M3FsBulk.MOVE -> {
                source.moveTo(manager.resolveFile(batch.target(index)));
                yield M3FsBulk.OK;
            }
            default -> throw new IllegalArgumentException("unrecognized bulk opcode");
        };
    }
}
