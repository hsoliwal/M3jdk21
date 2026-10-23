// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.NoSuchFileException;
import java.util.zip.ZipException;

/**
 * Why the last observation of a row failed (the error lane), next to {@link M3FileFlag#FAILED}.
 *
 * <p>Errors are data, not control flow: a scan records unreadable entries as rows, and hashing, delta,
 * {@code wc} and {@code grep} record per-row failures and continue; only cancellation propagates.</p>
 */
public enum M3FileError {
    NONE,
    /** The entry vanished between scan and read. */
    NOT_FOUND,
    ACCESS_DENIED,
    /** A byte limit was exceeded ({@link M3FileLimitException}). */
    LIMIT,
    /** An archive or entry could not be decoded. */
    CORRUPT,
    /** A file-system loop. */
    LOOP,
    /** Any other I/O failure. */
    IO;

    static final M3FileError[] VALUES = values();

    /** Classifies a failure; never {@link #NONE}. */
    public static M3FileError of(IOException failure) {
        if (failure instanceof NoSuchFileException) return NOT_FOUND;
        if (failure instanceof AccessDeniedException) return ACCESS_DENIED;
        if (failure instanceof M3FileLimitException) return LIMIT;
        if (failure instanceof ZipException) return CORRUPT;
        if (failure instanceof FileSystemLoopException) return LOOP;
        return IO;
    }
}
