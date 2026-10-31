// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Hash lanes of an {@link M3FileTree}: CRC32C, SHA-256 (four big-endian words, the #1567
 * {@code Fingerprint} order), token-stream hash, structure hash and the scan-time file-key hash.
 *
 * <p>One observation pass per row feeds every lane through shadows: SHA-256, CRC32C, a NUL probe
 * (binary files get no token hash), and a UTF-8 layer carrying the token shadow plus the provider's
 * structure layer. A failed observation sets {@link M3FileFlag#FAILED} and clears the hash flags.</p>
 */
final class M3FileHashes {
    private static final long ENTRY_CRC_KNOWN = 1L << 40;
    private static final M3FileFlag[] CONTENT_FLAGS = {M3FileFlag.HASHED, M3FileFlag.TOKEN_HASHED, M3FileFlag.STRUCT_HASHED};

    private long[] crc;
    private long[] sha;
    private long[] tokens;
    private long[] structure;
    private int[] fileKey;
    private long[] entryCrc;

    M3FileHashes(int capacity) {
        crc = new long[capacity];
        sha = new long[capacity * 4];
        tokens = new long[capacity];
        structure = new long[capacity];
        fileKey = new int[capacity];
        entryCrc = new long[capacity];
    }

    void grow(int capacity) {
        crc = Arrays.copyOf(crc, capacity);
        sha = Arrays.copyOf(sha, capacity * 4);
        tokens = Arrays.copyOf(tokens, capacity);
        structure = Arrays.copyOf(structure, capacity);
        fileKey = Arrays.copyOf(fileKey, capacity);
        entryCrc = Arrays.copyOf(entryCrc, capacity);
    }

    long crc(int id) {
        return crc[id];
    }

    long sha(int id, int word) {
        return sha[id * 4 + word];
    }

    long tokens(int id) {
        return tokens[id];
    }

    long structure(int id) {
        return structure[id];
    }

    int fileKey(int id) {
        return fileKey[id];
    }

    void fileKey(int id, Object key) {
        fileKey[id] = key == null ? 0 : key.hashCode();
    }

    /** Archive-directory CRC32, {@code -1} unknown (lane keeps a presence bit above the 32 CRC bits). */
    long entryCrc(int id) {
        long value = entryCrc[id];
        return (value & ENTRY_CRC_KNOWN) == 0 ? -1 : value & 0xffffffffL;
    }

    void entryCrc(int id, long crc32) {
        entryCrc[id] = crc32 < 0 ? 0 : ENTRY_CRC_KNOWN | (crc32 & 0xffffffffL);
    }

    boolean sameSha(int id, M3FileHashes other, int otherId) {
        return Arrays.equals(sha, id * 4, id * 4 + 4, other.sha, otherId * 4, otherId * 4 + 4);
    }

    /** Copies the content observation of {@code from}'s row {@code fromId} into row {@code id} of {@code tree}. */
    void carry(M3FileTree tree, int id, M3FileTree from, int fromId) {
        M3FileHashes source = from.hashes();
        crc[id] = source.crc[fromId];
        System.arraycopy(source.sha, fromId * 4, sha, id * 4, 4);
        tokens[id] = source.tokens[fromId];
        structure[id] = source.structure[fromId];
        for (M3FileFlag flag : CONTENT_FLAGS) {
            tree.setFlag(flag, id, from.flag(flag, fromId));
        }
    }

    /** Observes regular row {@code id} once; false when it is not regular or the observation failed. */
    boolean observe(M3FileTree tree, int id, long maxBytes, M3FileProgress progress, M3FileChangeProvider provider)
            throws InterruptedIOException {
        if (!tree.flag(M3FileFlag.REGULAR, id)) return false;
        M3File file = tree.file(id);
        M3FileDigestShadow digest = M3FileShadow.sha256();
        M3FileCrcShadow crc32c = M3FileShadow.crc32c();
        M3FileTokenShadow tokenShadow = M3FileCharShadow.tokens();
        Optional<? extends M3FileStructHash> structured = provider.structure(file);
        boolean[] binary = {false};
        M3FileShadow nulProbe = (bytes, offset, length) -> {
            for (int i = offset; i < offset + length && !binary[0]; i++) binary[0] = bytes[i] == 0;
        };
        M3FileCharShadow text = structured.isPresent() ? tokenShadow.andThen(structured.get()) : tokenShadow;
        try {
            file.observe(maxBytes, progress, digest, crc32c, nulProbe, M3FileShadow.utf8(text));
        } catch (InterruptedIOException canceled) {
            throw canceled;
        } catch (IOException failed) {
            tree.fail(id, failed);
            tree.setFlag(M3FileFlag.HASHED, id, false);
            tree.setFlag(M3FileFlag.TOKEN_HASHED, id, false);
            tree.setFlag(M3FileFlag.STRUCT_HASHED, id, false);
            return false;
        }
        crc[id] = crc32c.value();
        for (int word = 0; word < 4; word++) sha[id * 4 + word] = digest.word(word);
        tokens[id] = binary[0] ? 0 : tokenShadow.hash();
        structure[id] = structured.isPresent() ? structured.get().structHash() : 0;
        tree.clearFailure(id);
        tree.setFlag(M3FileFlag.HASHED, id, true);
        tree.setFlag(M3FileFlag.TOKEN_HASHED, id, !binary[0]);
        tree.setFlag(M3FileFlag.STRUCT_HASHED, id, structured.isPresent());
        return true;
    }
}
