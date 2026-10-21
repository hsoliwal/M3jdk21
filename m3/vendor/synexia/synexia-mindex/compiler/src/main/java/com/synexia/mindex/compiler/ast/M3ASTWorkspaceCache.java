// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.synexia.mindex.compiler.ast;

import com.synexia.mindex.swap.SwapBudgetPlane;
import com.synexia.mindex.swap.SwapStore;
import com.synexia.mindex.swap.TieredSwapCache;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Compilation-scoped identity adapter over the canonical lazy HOT/WARM/COLD cache.
 *
 * <p>The caller assigns stable nonnegative ordinals; digests are never truncated to IDs. All
 * cache policy, pinning, byte/entry ceilings and store I/O remain in {@link TieredSwapCache}.
 * This adapter authenticates the complete source/context/provider/phase frame before reuse.
 * A codec must persist immutable consumer facts, never a live compiler tree or compiler context.</p>
 *
 * <p>A source/context mismatch fails closed. New source sets or option/provider environments
 * require a new context and a separately bound store namespace. The configured payload budget
 * does not bound live javac trees, caller-retained values, codec scratch space or process RSS.</p>
 */
public final class M3ASTWorkspaceCache<T> implements AutoCloseable {
    private static final int MAGIC = 0x4d334143;
    private static final int VERSION = 1;
    private static final int SHA_BYTES = 32;
    private static final int MIN_FRAME_BYTES = 132;

    private record Frame<T>(long id, M3FileSourceFacts key, T value) { }
    private record Request(long id, M3FileSourceFacts key) { }

    /** Creating this reference performs no read; its successful first acquisition drops the loader. */
    public final class Ref {
        private final long id;
        private final M3FileSourceFacts key;
        private TieredSwapCache.Loader<T> loader;

        private Ref(long id, M3FileSourceFacts key, TieredSwapCache.Loader<T> loader) {
            this.id = id;
            this.key = key;
            this.loader = loader;
        }

        public long id() { return id; }
        public M3FileSourceFacts key() { return key; }

        public Lease acquire() throws IOException {
            synchronized (M3ASTWorkspaceCache.this) {
                Lease result = M3ASTWorkspaceCache.this.acquire(id, key, loader);
                loader = null;
                return result;
            }
        }
    }

    /** An active lease protects its cache entry; closing releases the strong reference. */
    public final class Lease implements AutoCloseable {
        private TieredSwapCache<Frame<T>>.Lease delegate;

        private Lease(TieredSwapCache<Frame<T>>.Lease delegate) {
            this.delegate = delegate;
        }

        public T value() {
            synchronized (M3ASTWorkspaceCache.this) {
                return requireOpen().value().value();
            }
        }

        public long id() {
            synchronized (M3ASTWorkspaceCache.this) {
                return requireOpen().id();
            }
        }

        public M3FileSourceFacts key() {
            synchronized (M3ASTWorkspaceCache.this) {
                return requireOpen().value().key();
            }
        }

        public boolean resident() {
            synchronized (M3ASTWorkspaceCache.this) {
                return requireOpen().resident();
            }
        }

        private TieredSwapCache<Frame<T>>.Lease requireOpen() {
            if (delegate == null) throw new IllegalStateException("closed AST lease");
            return delegate;
        }

        @Override public void close() {
            synchronized (M3ASTWorkspaceCache.this) {
                if (delegate == null) return;
                delegate.close();
                delegate = null;
            }
        }
    }

    private final String contextSha256;
    private final FrameCodec frames;
    private final TieredSwapCache<Frame<T>> cache;

    /** Construct an isolated workspace without reading the store or loading native code. */
    public M3ASTWorkspaceCache(
            String contextSha256,
            SwapStore store,
            TieredSwapCache.Codec<T> codec,
            TieredSwapCache.Budget budget,
            boolean ownsStore) {
        this.contextSha256 = M3FileSourceFacts.requireSha256(contextSha256, "contextSha256");
        SwapStore checked = Objects.requireNonNull(store, "store");
        frames = new FrameCodec(Objects.requireNonNull(codec, "codec"), checked.maxRecordBytes());
        cache = new TieredSwapCache<>(checked, frames, Objects.requireNonNull(budget, "budget"),
                TieredSwapCache.Policy.LRU, ownsStore);
    }

    public String contextSha256() { return contextSha256; }

    public synchronized Ref deferred(
            long id, M3FileSourceFacts key, TieredSwapCache.Loader<T> loader) {
        requireKey(id, key);
        cache.ref(id);
        return new Ref(id, key, loader);
    }

    public synchronized Lease acquire(long id, M3FileSourceFacts key) throws IOException {
        return acquire(id, key, null);
    }

    public synchronized Lease acquire(
            long id, M3FileSourceFacts key, TieredSwapCache.Loader<T> loader) throws IOException {
        requireKey(id, key);
        Request previous = frames.request;
        frames.request = new Request(id, key);
        try {
            TieredSwapCache<Frame<T>>.Lease held = cache.acquire(id,
                    loader == null ? null : ordinal -> new Frame<>(ordinal, key,
                            Objects.requireNonNull(loader.load(ordinal), "loader value")));
            try {
                frames.requireRequest(held.value());
                return new Lease(held);
            } catch (IOException | RuntimeException | Error failure) {
                held.close();
                throw failure;
            }
        } finally {
            frames.request = previous;
        }
    }

    public synchronized TieredSwapCache.Pressure readjust(SwapBudgetPlane.Snapshot snapshot) {
        return cache.readjust(snapshot);
    }

    public synchronized TieredSwapCache.Stats stats() { return cache.stats(); }

    public synchronized TieredSwapCache.DynamicStats dynamicStats() { return cache.dynamicStats(); }

    public synchronized List<Long> residentIdsByHeat(int limit) {
        return cache.residentIdsByHeat(limit);
    }

    @Override public synchronized void close() throws IOException { cache.close(); }

    private void requireKey(long id, M3FileSourceFacts key) {
        SwapStore.requireId(id);
        Objects.requireNonNull(key, "key");
        if (!contextSha256.equals(key.contextSha256())) {
            throw new IllegalArgumentException("AST key belongs to a different compilation context");
        }
    }

    /** Request state is access-scoped under the enclosing monitor, including nested loader calls. */
    private final class FrameCodec implements TieredSwapCache.Codec<Frame<T>> {
        private final TieredSwapCache.Codec<T> payload;
        private final int maxRecordBytes;
        private Request request;

        private FrameCodec(TieredSwapCache.Codec<T> payload, int maxRecordBytes) {
            if (maxRecordBytes < MIN_FRAME_BYTES) {
                throw new IllegalArgumentException("store record limit too small for an AST frame");
            }
            this.payload = payload;
            this.maxRecordBytes = maxRecordBytes;
        }

        @Override public byte[] encode(Frame<T> value) throws IOException {
            requireRequest(value);
            byte[] encoded = Objects.requireNonNull(payload.encode(value.value()), "payload bytes");
            long length = 136L + 2L * (value.key().sourceUri().length()
                    + value.key().providerId().length() + value.key().phase().length()) + encoded.length;
            if (length > maxRecordBytes) throw new IOException("AST frame record limit");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream((int) length);
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                out.writeInt(MAGIC);
                out.writeInt(VERSION);
                out.writeLong(value.id());
                writeKey(out, value.key());
                out.writeInt(encoded.length);
                out.write(encoded);
                out.flush();
                out.write(sha256(bytes.toByteArray(), bytes.size()));
            }
            byte[] result = bytes.toByteArray();
            if (result.length > maxRecordBytes) throw new IOException("AST frame record limit");
            return result;
        }

        @Override public Frame<T> decode(byte[] bytes) throws IOException {
            if (bytes.length < MIN_FRAME_BYTES || bytes.length > maxRecordBytes) {
                throw new IOException("AST frame length");
            }
            int bodyLength = bytes.length - SHA_BYTES;
            if (!MessageDigest.isEqual(sha256(bytes, bodyLength),
                    Arrays.copyOfRange(bytes, bodyLength, bytes.length))) {
                throw new IOException("AST frame checksum mismatch");
            }
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes, 0, bodyLength))) {
                if (in.readInt() != MAGIC || in.readInt() != VERSION) {
                    throw new IOException("AST frame format mismatch");
                }
                long id = in.readLong();
                M3FileSourceFacts key = readKey(in);
                requireRequest(new Frame<>(id, key, null));
                int size = in.readInt();
                if (size < 0 || size != in.available()) throw new IOException("AST payload length");
                T value = Objects.requireNonNull(payload.decode(in.readNBytes(size)), "decoded AST payload");
                return new Frame<>(id, key, value);
            } catch (IllegalArgumentException | NullPointerException failure) {
                throw new IOException("invalid AST frame", failure);
            }
        }

        @Override public long weight(Frame<T> value) {
            long weight = payload.weight(Objects.requireNonNull(value.value(), "AST payload"));
            if (weight <= 0) throw new IllegalArgumentException("invalid AST payload weight");
            return Math.addExact(weight, Math.addExact(64L, value.key().retainedBytes()));
        }

        private void requireRequest(Frame<T> value) throws IOException {
            if (request == null || request.id() != value.id() || !request.key().equals(value.key())
                    || !contextSha256.equals(value.key().contextSha256())) {
                throw new IOException("AST source/context/provider/phase/ordinal identity mismatch");
            }
        }
    }

    private static void writeKey(DataOutputStream out, M3FileSourceFacts key) throws IOException {
        writeText(out, key.sourceUri());
        out.write(HexFormat.of().parseHex(key.sourceSha256()));
        out.writeInt(key.utf16Length());
        out.write(HexFormat.of().parseHex(key.contextSha256()));
        writeText(out, key.providerId());
        out.writeInt(key.schemaVersion());
        writeText(out, key.phase());
    }

    private static M3FileSourceFacts readKey(DataInputStream in) throws IOException {
        String uri = readText(in, 16_384);
        String source = readSha(in);
        int length = in.readInt();
        String context = readSha(in);
        String provider = readText(in, 1_024);
        int schema = in.readInt();
        String phase = readText(in, 64);
        return new M3FileSourceFacts(uri, source, length, context, provider, schema, phase);
    }

    private static String readSha(DataInputStream in) throws IOException {
        byte[] digest = in.readNBytes(SHA_BYTES);
        if (digest.length != SHA_BYTES) throw new IOException("truncated AST digest");
        return HexFormat.of().formatHex(digest);
    }

    private static void writeText(DataOutputStream out, String value) throws IOException {
        out.writeInt(value.length());
        for (int i = 0; i < value.length(); i++) out.writeChar(value.charAt(i));
    }

    private static String readText(DataInputStream in, int limit) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > limit || length > in.available() / 2) {
            throw new IOException("AST metadata length");
        }
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) chars[i] = in.readChar();
        return new String(chars);
    }

    private static byte[] sha256(byte[] bytes, int length) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(bytes, 0, length);
            return digest.digest();
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
