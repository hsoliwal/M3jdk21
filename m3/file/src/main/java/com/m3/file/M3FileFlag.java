// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

/**
 * Bit lanes of an {@link M3FileTree}: one {@code BitSet} per constant, bit index = row id.
 *
 * <p>Flags are data, not objects; a facade never caches them (DOOP, see {@code m3/file/DESIGN.md}
 * section 3).</p>
 */
public enum M3FileFlag {
    /** Row is a directory by its own attributes; links are never followed. */
    DIR,
    /** Row is a symbolic link; never followed and never {@link #DIR}. */
    LINK,
    /** Dot-name or DOS hidden attribute observed during the scan. */
    HIDDEN,
    /** Directory whose child slice is populated. */
    SCANNED,
    /** A facade for this row currently sits in the residency table. */
    RESIDENT,
    /** A delta touched this row since the last commit (slice 3). */
    DIRTY,
    /** Tombstone: the row keeps its lanes and is skipped by iteration (slice 3). */
    DELETED;

    static final M3FileFlag[] VALUES = values();
}
