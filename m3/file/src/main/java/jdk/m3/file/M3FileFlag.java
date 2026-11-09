// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;
//FLLDCIM: RESTORE - source m3/qualification/master-landing-20261028/retained-variants/pr1619/m3/file/src/main/java/jdk/m3/file/M3FileFlag.java blob 10784db10db63d8fe1339615d2a1bb50920f8606 (retained PR #1619 head; parent 0cd0944136c1 blob 40900745a030d0d4c5cb54464d3c546f5f29a753 https://github.com/hsoliwal/M3jdk21/blob/0cd0944136c1/m3/file/src/main/java/com/m3/file/M3FileFlag.java); recipe m3/file/qualification/tree-lanes-port-20261107 RESTORE_EXACT

/**
 * Bit lanes of an {@link M3FileTree}: one {@code BitSet} per constant, bit index = row id.
 *
 * <p>Flags are data, not objects; a facade never caches them (DOOP, see {@code m3/file/DESIGN.md}
 * section 3).</p>
 */
public enum M3FileFlag {
    /** Row is a directory by its own attributes; links are never followed. */
    DIR,
    /** Row is a regular file by its own attributes. */
    REGULAR,
    /** Row is a symbolic link; never followed and never {@link #DIR}. */
    LINK,
    /** Row is an archive (zip, jar, nested archive): a regular file whose entries are its children. */
    ARCHIVE,
    /** Dot-name or DOS hidden attribute observed during the scan. */
    HIDDEN,
    /** Directory whose child slice is populated. */
    SCANNED,
    /** A facade for this row currently sits in the residency table. */
    RESIDENT,
    /** A delta touched this row since the last commit (slice 3). */
    DIRTY,
    /** Tombstone: the row keeps its lanes and is skipped by iteration (slice 3). */
    DELETED,
    /** Raw-byte hash lanes (CRC32C, SHA-256) hold this row's content observation. */
    HASHED,
    /** Token-hash lane is set: the content decoded as text without NUL bytes. */
    TOKEN_HASHED,
    /** Structure-hash lane is set by an {@link M3FileChangeProvider#structure} layer. */
    STRUCT_HASHED,
    /** The last content observation of this row failed (vanished, unreadable, over the limit). */
    FAILED;

    static final M3FileFlag[] VALUES = values();
}
