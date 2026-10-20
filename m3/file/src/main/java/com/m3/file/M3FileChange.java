// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.nio.file.Path;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * One row of an {@link M3FileTreeDelta} (a thin facade over its lanes), and the change-bit vocabulary.
 *
 * <p>{@link #ADDED}, {@link #REMOVED} and {@link #CONTENT_CHANGED} keep the values of the M3File
 * content-delta vocabulary (PR #1567), so downstream consumers and the build delta ledger (PR #1568)
 * read the same bits. The remaining bits say which attribute or hash lane moved.</p>
 */
public final class M3FileChange {
    public static final int ADDED = 1;
    public static final int REMOVED = 2;
    /** Raw bytes differ: sizes differ or SHA-256 lanes differ, or the old row had no content observation. */
    public static final int CONTENT_CHANGED = 4;
    public static final int SIZE = 8;
    public static final int MTIME = 16;
    public static final int KIND = 32;
    public static final int FILE_KEY = 64;
    /** Token-stream hash differs (a whitespace-only edit keeps it). */
    public static final int TOKENS = 128;
    /** Structure hash (M3AST etc.) differs. */
    public static final int STRUCTURE = 256;
    /** The new content could not be observed; content state unknown. */
    public static final int UNREADABLE = 512;

    private static final String[] NAMES = {"ADDED", "REMOVED", "CONTENT_CHANGED", "SIZE", "MTIME", "KIND",
        "FILE_KEY", "TOKENS", "STRUCTURE", "UNREADABLE"};

    private final M3FileTreeDelta delta;
    private final int row;

    M3FileChange(M3FileTreeDelta delta, int row) {
        this.delta = delta;
        this.row = row;
    }

    public int row() {
        return row;
    }

    public int bits() {
        return delta.bits(row);
    }

    /** True when every bit of {@code mask} is set. */
    public boolean has(int mask) {
        return (bits() & mask) == mask;
    }

    /** Entry in the before tree; empty for an addition. */
    public Optional<M3File> before() {
        int id = delta.beforeId(row);
        return id < 0 ? Optional.empty() : Optional.of(delta.before().file(id));
    }

    /** Entry in the after tree; empty for a removal. */
    public Optional<M3File> after() {
        int id = delta.afterId(row);
        return id < 0 ? Optional.empty() : Optional.of(delta.after().file(id));
    }

    /** The after entry, or the before entry for a removal. */
    public M3File entry() {
        return after().or(this::before).orElseThrow();
    }

    /** Cold: current path (the before path for a removal). */
    public Path path() {
        return delta.path(row);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof M3FileChange that && that.delta == delta && that.row == row;
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(delta) + row;
    }

    @Override
    public String toString() {
        return describe(bits()) + " " + path();
    }

    /** {@code "CONTENT_CHANGED|SIZE"} style rendering in bit order; {@code "NONE"} for zero. */
    public static String describe(int bits) {
        if (bits == 0) return "NONE";
        StringJoiner joiner = new StringJoiner("|");
        for (int i = 0; i < NAMES.length; i++) {
            if ((bits & (1 << i)) != 0) joiner.add(NAMES[i]);
        }
        if ((bits >>> NAMES.length) != 0) joiner.add("0x" + Integer.toHexString(bits >>> NAMES.length << NAMES.length));
        return joiner.toString();
    }
}
