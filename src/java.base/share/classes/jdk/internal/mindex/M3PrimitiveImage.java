/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 * Target-owned M3JDK implementation. See the primitive identity receiver receipt.
 */
package jdk.internal.mindex;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileDescriptor;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.function.LongConsumer;
import jdk.internal.access.SharedSecrets;
import jdk.internal.misc.Unsafe;

/**
 * Frozen, bounded OS-mapped primitive catalogue. This is not a live multi-writer interner.
 * The directory and committed inode must be trusted and never externally rewritten/truncated.
 * Published images use atomic create-only hard links; unsupported filesystems fail closed.
 *
 * Each record stores (kind: signed 64-bit, exact key: 64-bit), both big-endian. Small scalar
 * arithmetic never consults this optional image. Hot callers carry an image plus an int slot;
 * the Handle allocation below exists only at explicit persistence/transport boundaries.
 */
public final class M3PrimitiveImage implements AutoCloseable {
    private static final long MAGIC = 0x4d335052494d3031L; // M3PRIM01
    private static final int HEADER = 96, RECORD = 16, MAX_RECORDS = 1_000_000;
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();

    /** Exact typed identity, not a hash or process address. A namespace/generation is explicit. */
    public record Handle(UUID namespace, long generation, int kind, long key) {
        public Handle {
            Objects.requireNonNull(namespace, "namespace");
            if (generation <= 0) throw new IllegalArgumentException("generation");
            M3PrimitiveIdentity.checkKey(kind, key);
        }
    }

    private final ByteBuffer memory;
    private final boolean fixedMapping;
    private final UUID namespace;
    private final long generation;
    private final int size;
    private boolean closed;
    private int active;

    private M3PrimitiveImage(ByteBuffer memory, boolean fixedMapping, UUID namespace, long generation, int size) {
        this.memory = memory; this.fixedMapping = fixedMapping;
        this.namespace = namespace; this.generation = generation; this.size = size;
    }

    /**
     * Snapshot, sort and deduplicate eight typed lanes without per-record objects.
     * Input arrays must not be concurrently modified during this call. The final path is never
     * replaced, including in a race with another publisher. Returns the number of unique records.
     * Namespace allocation/authorization belongs to the caller, not the operating system.
     */
    public static int publish(Path destination, UUID namespace, long generation,
                              int[] kinds, long[] keys) throws IOException {
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(kinds, "kinds"); Objects.requireNonNull(keys, "keys");
        if (generation <= 0 || kinds.length != keys.length || keys.length > MAX_RECORDS)
            throw new IllegalArgumentException("image dimensions/generation");
        int[] tags = kinds.clone(); long[] values = keys.clone();
        int[] counts = new int[8];
        for (int i = 0; i < tags.length; i++) {
            M3PrimitiveIdentity.checkKey(tags[i], values[i]); counts[tags[i]]++;
        }
        long[][] lanes = new long[8][];
        for (int k = 0; k < 8; k++) lanes[k] = new long[counts[k]];
        Arrays.fill(counts, 0);
        for (int i = 0; i < tags.length; i++) lanes[tags[i]][counts[tags[i]]++] = values[i];
        int total = 0;
        for (int k = 0; k < 8; k++) {
            Arrays.sort(lanes[k]); int n = 0;
            for (long value : lanes[k]) {
                if (n == 0 || lanes[k][n - 1] != value) lanes[k][n++] = value;
            }
            counts[k] = n; total += n;
        }
        Path target = destination.toAbsolutePath().normalize();
        Path parent = target.getParent();
        if (parent == null || !Files.isDirectory(parent)) throw new IOException("image parent missing");
        Path staged = Files.createTempFile(parent, ".m3-primitive-", ".tmp");
        try {
            ByteBuffer header = ByteBuffer.allocate(HEADER).order(ByteOrder.BIG_ENDIAN);
            header.putLong(MAGIC).putInt(1).putInt(RECORD);
            header.putLong(namespace.getMostSignificantBits()).putLong(namespace.getLeastSignificantBits());
            header.putLong(generation).putLong(total).putLong(0L).putLong(0L);
            MessageDigest hash = sha256(); hash.update(header.array(), 0, 64);
            try (FileChannel channel = FileChannel.open(staged, StandardOpenOption.WRITE)) {
                header.position(0); writeAll(channel, header);
                ByteBuffer record = ByteBuffer.allocate(RECORD).order(ByteOrder.BIG_ENDIAN);
                for (int k = 0; k < 8; k++) for (int i = 0; i < counts[k]; i++) {
                    record.clear(); record.putLong(k).putLong(lanes[k][i]);
                    hash.update(record.array()); record.flip(); writeAll(channel, record);
                }
                header.position(64); header.put(hash.digest()); header.position(0);
                channel.position(0); writeAll(channel, header); channel.force(true);
            }
            // Unlike ATOMIC_MOVE, createLink has an unambiguous no-replacement contract.
            Files.createLink(target, staged);
            return total;
        } finally { Files.deleteIfExists(staged); }
    }

    public static M3PrimitiveImage open(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        MappedByteBuffer mapped = null;
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            long length = channel.size();
            checkLength(length);
            mapped = channel.map(FileChannel.MapMode.READ_ONLY, 0, length);
            M3PrimitiveImage result = validate(mapped, false);
            mapped = null;
            return result;
        } finally { if (mapped != null) UNSAFE.invokeCleaner(mapped); }
    }

    /**
     * Linux matched-runtime opt-in: map this immutable image at exactly {@code baseAddress}.
     * Every participant must use the same image layout, namespace/generation and base address.
     * Address occupancy, unsupported platforms/kernels, and non-exact mappings fail closed;
     * this method never overwrites a mapping or silently falls back to another address.
     * The trusted-directory/immutable-inode contract applies throughout open and use.
     * Reserve/admit early in a controlled launcher; arbitrary processes are not forced to join.
     */
    public static M3PrimitiveImage openFixed(Path path, long baseAddress) throws IOException {
        Objects.requireNonNull(path, "path");
        if (baseAddress <= 0) throw new IllegalArgumentException("positive fixed base required");
        if (Files.isSymbolicLink(path)) throw new IOException("symbolic image path");
        ByteBuffer mapped = null;
        // The private open stream owns the descriptor during mmap; no path is reopened by JNI.
        try (FileInputStream stream = new FileInputStream(path.toFile())) {
            long length = stream.getChannel().size();
            checkLength(length);
            mapped = fixedMap0(stream.getFD(), baseAddress, length);
            M3PrimitiveImage result = validate(mapped, true);
            mapped = null;
            return result;
        } finally { if (mapped != null) fixedUnmap0(mapped); }
    }

    private static native ByteBuffer fixedMap0(FileDescriptor descriptor, long baseAddress, long length)
            throws IOException;
    private static native void fixedUnmap0(ByteBuffer mapped);

    private static void checkLength(long length) throws IOException {
        if (length < HEADER || length > HEADER + (long) RECORD * MAX_RECORDS)
            throw new IOException("invalid primitive image length");
    }

    private static M3PrimitiveImage validate(ByteBuffer mapped, boolean fixed) throws IOException {
        long length = mapped.capacity();
        mapped.order(ByteOrder.BIG_ENDIAN);
            long count = mapped.getLong(40), epoch = mapped.getLong(32);
            if (mapped.getLong(0) != MAGIC || mapped.getInt(8) != 1 || mapped.getInt(12) != RECORD
                    || epoch <= 0 || count < 0 || count > MAX_RECORDS
                    || length != HEADER + RECORD * count || mapped.getLong(48) != 0 || mapped.getLong(56) != 0)
                throw new IOException("invalid primitive image header");
            MessageDigest hash = sha256();
            ByteBuffer view = mapped.duplicate(); view.limit(64); hash.update(view);
            byte[] expected = new byte[32]; view = mapped.duplicate(); view.position(64); view.get(expected);
            view = mapped.duplicate(); view.position(HEADER); hash.update(view);
            if (!MessageDigest.isEqual(expected, hash.digest())) throw new IOException("primitive image digest");
            int previousKind = -1; long previousKey = 0;
            for (int i = 0; i < (int) count; i++) {
                int at = HEADER + RECORD * i;
                long wideKind = mapped.getLong(at), key = mapped.getLong(at + 8);
                if (wideKind < 0 || wideKind > 7) throw new IOException("primitive kind");
                int kind = (int) wideKind;
                try { M3PrimitiveIdentity.checkKey(kind, key); }
                catch (IllegalArgumentException badKey) { throw new IOException("primitive key", badKey); }
                if (kind < previousKind || kind == previousKind && key <= previousKey)
                    throw new IOException("unsorted/duplicate primitive record");
                previousKind = kind; previousKey = key;
            }
            M3PrimitiveImage result = new M3PrimitiveImage(mapped, fixed,
                    new UUID(mapped.getLong(16), mapped.getLong(24)), epoch, (int) count);
        return result;
    }

    private static void writeAll(FileChannel channel, ByteBuffer source) throws IOException {
        while (source.hasRemaining()) channel.write(source);
    }
    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private void requireOpen() { if (closed) throw new IllegalStateException("closed primitive image"); }
    private int offset(int slot) { return HEADER + RECORD * Objects.checkIndex(slot, size); }
    public synchronized int size() { requireOpen(); return size; }
    public synchronized int kind(int slot) { requireOpen(); return (int) memory.getLong(offset(slot)); }
    public synchronized long key(int slot) { requireOpen(); return memory.getLong(offset(slot) + 8); }

    /** Returns a stable local slot, or -1 when this immutable snapshot does not contain the value. */
    public synchronized int find(int kind, long key) {
        requireOpen(); M3PrimitiveIdentity.checkKey(kind, key);
        int low = 0, high = size;
        while (low < high) {
            int mid = (low + high) >>> 1, at = offset(mid);
            int foundKind = (int) memory.getLong(at); long foundKey = memory.getLong(at + 8);
            int cmp = Integer.compare(foundKind, kind);
            if (cmp == 0) cmp = Long.compare(foundKey, key);
            if (cmp < 0) low = mid + 1; else high = mid;
        }
        return low < size && memory.getLong(offset(low)) == kind
                && memory.getLong(offset(low) + 8) == key ? low : -1;
    }

    public synchronized Handle handle(int slot) {
        requireOpen(); int at = offset(slot);
        return new Handle(namespace, generation, (int) memory.getLong(at), memory.getLong(at + 8));
    }
    public synchronized int resolve(Handle handle) {
        requireOpen(); Objects.requireNonNull(handle, "handle");
        if (!namespace.equals(handle.namespace()) || generation != handle.generation())
            throw new IllegalArgumentException("foreign/stale primitive handle");
        return find(handle.kind(), handle.key());
    }

    /**
     * Gives trusted native code the address of the big-endian 8-byte key for this callback only.
     * No borrowed address may be retained. A live lease prevents close/unmapping, including a
     * reentrant close. JNI callers must finish synchronous access before returning.
     */
    public void withAddress(int slot, LongConsumer action) {
        Objects.requireNonNull(action, "action");
        long address;
        synchronized (this) {
            requireOpen(); int at = offset(slot) + 8;
            if (active == Integer.MAX_VALUE) throw new IllegalStateException("primitive lease overflow");
            address = Math.addExact(SharedSecrets.getJavaNioAccess().getBufferAddress(memory), at);
            active++;
        }
        try { action.accept(address); }
        finally {
            synchronized (this) { active--; }
            Reference.reachabilityFence(memory);
        }
    }

    @Override public synchronized void close() {
        if (closed) return;
        if (active != 0) throw new IllegalStateException("primitive image has active leases");
        closed = true;
        if (fixedMapping) fixedUnmap0(memory); else UNSAFE.invokeCleaner(memory);
    }
}
