// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
        return commit(tree, read(tree.file(id), maxBytes, progress, provider));
    }

    /** A bounded per-observation result; never retains file bytes or parser shadows. */
    private record Observation(int id, long crc, long sha0, long sha1, long sha2, long sha3,
            long tokens, long structure, boolean text, boolean structured, IOException failure) { }

    private static Observation read(M3File file, long maxBytes, M3FileProgress progress,
            M3FileChangeProvider provider) throws InterruptedIOException {
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
            return new Observation(file.id(), 0, 0, 0, 0, 0, 0, 0, false, false, failed);
        }
        return new Observation(file.id(), crc32c.value(), digest.word(0), digest.word(1),
                digest.word(2), digest.word(3), binary[0] ? 0 : tokenShadow.hash(),
                structured.isPresent() ? structured.get().structHash() : 0,
                !binary[0], structured.isPresent(), null);
    }

    /** Only the calling coordinator publishes into the tree's non-thread-safe lanes. */
    private boolean commit(M3FileTree tree, Observation value) {
        int id = value.id();
        if (value.failure() != null) {
            tree.fail(id, value.failure());
            for (M3FileFlag flag : CONTENT_FLAGS) tree.setFlag(flag, id, false);
            return false;
        }
        crc[id] = value.crc();
        sha[id * 4] = value.sha0();
        sha[id * 4 + 1] = value.sha1();
        sha[id * 4 + 2] = value.sha2();
        sha[id * 4 + 3] = value.sha3();
        tokens[id] = value.tokens();
        structure[id] = value.structure();
        tree.clearFailure(id);
        tree.setFlag(M3FileFlag.HASHED, id, true);
        tree.setFlag(M3FileFlag.TOKEN_HASHED, id, value.text());
        tree.setFlag(M3FileFlag.STRUCT_HASHED, id, value.structured());
        return true;
    }

    int observeParallel(M3FileTree tree, M3FileTraversal entries, long maxBytes,
            M3FileProgress progress, M3FileChangeProvider provider, int parallelism)
            throws InterruptedIOException {
        int generation = tree.generation();
        AtomicBoolean stopping = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        M3FileProgress shared = new M3FileProgress() {
            @Override public synchronized void worked(long units) { progress.worked(units); }
            @Override public synchronized boolean isCanceled() {
                return stopping.get() || failure.get() != null || Thread.currentThread().isInterrupted()
                        || progress.isCanceled();
            }
        };
        int width = Math.max(1, Math.min(parallelism, tree.rows()));
        var workers = Executors.newFixedThreadPool(width);
        ArrayDeque<Future<Observation>> pending = new ArrayDeque<>();
        int hashed = 0;
        try (var ids = entries.ids().filter(id -> tree.flag(M3FileFlag.REGULAR, id))) {
            progress.begin("hash " + tree.root(), 0);
            var iterator = ids.iterator();
            while (true) {
                check(tree, generation, shared, failure);
                while (pending.size() < width && iterator.hasNext()) {
                    check(tree, generation, shared, failure);
                    int id = iterator.nextInt();
                    M3File file = tree.file(id);
                    M3FileProgress child = shared.split(Math.max(0, tree.size(id)));
                    pending.addLast(workers.submit(() -> {
                        try {
                            shared.checkCanceled();
                            return read(file, maxBytes, child, provider);
                        } catch (InterruptedIOException | RuntimeException | Error fatal) {
                            failure.compareAndSet(null, fatal);
                            throw fatal;
                        }
                    }));
                }
                if (pending.isEmpty()) break;
                Observation next;
                while (true) {
                    check(tree, generation, shared, failure);
                    try {
                        // Poll only to notice cancellation while a source is blocked in I/O.
                        next = pending.getFirst().get(100, TimeUnit.MILLISECONDS);
                        break;
                    } catch (TimeoutException waiting) {
                        // Keep the full outstanding window bounded behind its first result.
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        InterruptedIOException canceled = new InterruptedIOException("hash interrupted");
                        canceled.initCause(interrupted);
                        throw canceled;
                    } catch (ExecutionException failed) {
                        // A sibling can fail while this get is waiting. Its cancellation must not
                        // replace the first parser/provider failure that caused the shutdown.
                        Throwable first = failure.get();
                        rethrow(first != null ? first : failed.getCause());
                        throw new AssertionError("unreachable");
                    }
                }
                check(tree, generation, shared, failure);
                pending.removeFirst();
                if (commit(tree, next)) hashed++;
            }
        } finally {
            stopping.set(true);
            workers.shutdownNow();
            workers.close(); // Joins even when the caller was interrupted; restores its interrupt.
        }
        progress.done();
        return hashed;
    }

    private static void check(M3FileTree tree, int generation, M3FileProgress progress,
            AtomicReference<Throwable> failure) throws InterruptedIOException {
        Throwable fatal = failure.get();
        if (fatal != null) rethrow(fatal);
        boolean canceled = progress.isCanceled();
        fatal = failure.get();
        if (fatal != null) rethrow(fatal);
        if (canceled) throw new InterruptedIOException("hash canceled");
        if (tree.generation() != generation) throw new ConcurrentModificationException();
    }

    private static void rethrow(Throwable failure) throws InterruptedIOException {
        if (failure instanceof InterruptedIOException canceled) throw canceled;
        if (failure instanceof RuntimeException unchecked) throw unchecked;
        if (failure instanceof Error error) throw error;
        throw new IllegalStateException("unexpected observation failure", failure);
    }
}
