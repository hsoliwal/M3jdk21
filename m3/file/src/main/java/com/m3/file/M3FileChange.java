// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.StringJoiner;

/**
 * Change bits between two observations of one logical entry.
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

    private M3FileChange() {
    }

    /** {@code "SIZE|MTIME"} style rendering; {@code "NONE"} for zero. */
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
