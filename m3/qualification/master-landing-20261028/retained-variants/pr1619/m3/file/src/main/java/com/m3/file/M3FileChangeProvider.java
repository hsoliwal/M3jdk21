// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.InterruptedIOException;
import java.util.Optional;

/**
 * Change detection between two observations of one logical entry, cheapest first.
 *
 * <p>Every method is a default ({@link #ATTRIBUTES} is the plain provider): {@link #attributeChange}
 * compares kind, size, modified time and file key from the scan lanes with no I/O; when that gate stays
 * closed the new row inherits the old content observation and nothing is read. Only when the gate trips
 * is the new row observed once (SHA-256, CRC32C, token hash, {@link #structure} hash) and compared by
 * {@link #contentChange}. Providers with their own change mechanism override as little as they need:
 * a {@code WatchService} or git-index provider overrides {@link #attributeChange}, a zip provider compares
 * central-directory CRC/size there, an M3AST provider returns a layer from {@link #structure}.</p>
 *
 * <p>An attribute gate cannot see an edit that keeps size, modified time and file key (the "racy" case
 * of every stat cache); providers with a stronger mechanism close that gap.</p>
 */
public interface M3FileChangeProvider {
    /** Attribute gate, then raw/token hashes; no structure layer. */
    M3FileChangeProvider ATTRIBUTES = new M3FileChangeProvider() {
    };

    /**
     * {@link M3FileChange} bits from lanes alone; {@code 0} keeps the gate closed. Archive rows also
     * compare their central-directory CRC32 ({@link M3FileTree#entryCrc}): a differing CRC is content.
     */
    default int attributeChange(M3FileTree before, int beforeId, M3FileTree after, int afterId) {
        int bits = 0;
        if (before.kind(beforeId) != after.kind(afterId)) bits |= M3FileChange.KIND;
        if (before.size(beforeId) != after.size(afterId)) bits |= M3FileChange.SIZE;
        if (before.mtime(beforeId) != after.mtime(afterId)) bits |= M3FileChange.MTIME;
        if (before.fileKeyHash(beforeId) != after.fileKeyHash(afterId)) bits |= M3FileChange.FILE_KEY;
        long beforeCrc = before.entryCrc(beforeId);
        long afterCrc = after.entryCrc(afterId);
        if (beforeCrc >= 0 && afterCrc >= 0 && beforeCrc != afterCrc) bits |= M3FileChange.CONTENT_CHANGED;
        return bits;
    }

    /** Structure layer for {@code file} (an M3AST builder, ...); empty when none applies. */
    default Optional<? extends M3FileStructHash> structure(M3File file) {
        return Optional.empty();
    }

    /** Content bits from the hash lanes of two observed rows. */
    default int contentChange(M3FileTree before, int beforeId, M3FileTree after, int afterId) {
        if (!before.flag(M3FileFlag.HASHED, beforeId)) return M3FileChange.CONTENT_CHANGED;
        int bits = 0;
        if (before.size(beforeId) != after.size(afterId)
                || !before.hashes().sameSha(beforeId, after.hashes(), afterId)) {
            bits |= M3FileChange.CONTENT_CHANGED;
        }
        if (differs(before, beforeId, after, afterId, M3FileFlag.TOKEN_HASHED,
                before.tokenHash(beforeId) != after.tokenHash(afterId))) {
            bits |= M3FileChange.TOKENS;
        }
        if (differs(before, beforeId, after, afterId, M3FileFlag.STRUCT_HASHED,
                before.structHash(beforeId) != after.structHash(afterId))) {
            bits |= M3FileChange.STRUCTURE;
        }
        return bits;
    }

    /**
     * Full decision for one matched pair: gate, then (regular files only) one observation of the new row
     * and a lane comparison. Units of {@code progress} are bytes read.
     */
    default int change(M3FileTree before, int beforeId, M3FileTree after, int afterId, long maxBytes,
            M3FileProgress progress) throws InterruptedIOException {
        int bits = attributeChange(before, beforeId, after, afterId);
        if (bits == 0) {
            after.carryHashes(afterId, before, beforeId);
            return 0;
        }
        if (!before.flag(M3FileFlag.REGULAR, beforeId) || !after.flag(M3FileFlag.REGULAR, afterId)) return bits;
        if (!after.hashRow(afterId, maxBytes, progress, this)) return bits | M3FileChange.UNREADABLE;
        return bits | contentChange(before, beforeId, after, afterId);
    }

    private static boolean differs(M3FileTree before, int beforeId, M3FileTree after, int afterId,
            M3FileFlag lane, boolean valuesDiffer) {
        boolean had = before.flag(lane, beforeId);
        boolean has = after.flag(lane, afterId);
        return had != has || (had && valuesDiffer);
    }
}
