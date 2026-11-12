// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.io.IOException;
import java.io.EOFException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Private region owner. Native mappings and ordinary files share one fair lease authority. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/36827ee8a798ef6ce07f6fc55a2ac371b1382c06/m3-pmem-sdk/src/main/java/com/synexia/m3/pmem/PmemMapping.java blob e6d8e5ca9730; recipe m3/optane/recipe/pmem-region-core-20261106/relocate.py v2
final class PmemMapping implements PmemRegion {
    /**
     * A FILE search owns its read lease while consuming the window. Keep at
     * most one 64 KiB window per worker thread rather than allocating a fresh
     * source-sized byte[] on each file-local search. No native memory is held,
     * and the per-thread array contains no mapping or file references.
     */
    private static final int FILE_SEARCH_WINDOW_BYTES = 65_536;
    private static final ThreadLocal<byte[]> FILE_SEARCH_WINDOWS =
            ThreadLocal.withInitial(() -> new byte[0]);

    private static byte[] fileSearchWindow(long count) {
        int required = (int) Math.min((long) FILE_SEARCH_WINDOW_BYTES, count);
        byte[] window = FILE_SEARCH_WINDOWS.get();
        if (window.length < required) {
            window = new byte[required];
            FILE_SEARCH_WINDOWS.set(window);
        }
        return window;
    }
    private final ReentrantReadWriteLock leases = new ReentrantReadWriteLock(true);
    private final Path path;
    private final long offset;
    private final long length;
    private final PmemAccess access;
    private final PmemBackend backend;
    private final PmemGranularity granularity;
    private long handle;
    private final FileChannel fileChannel;
    private final FileIdentity fileIdentity;

    /** Provider identities are meaningful only within their captured filesystem. */
    record FileIdentity(java.nio.file.FileSystem fileSystem, Object key) {}

    static FileIdentity readFileIdentity(Path path) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile()) throw new IOException("PMem path is not a regular file");
        Object key = attributes.fileKey();
        return key == null ? null : new FileIdentity(path.getFileSystem(), key);
    }

    /** Native/native paths do not depend on optional Java provider identity. */
    static FileIdentity optionalFileIdentity(Path path) {
        try { return readFileIdentity(path); }
        catch (IOException | UnsupportedOperationException unavailable) { return null; }
    }

    PmemMapping(Path path, long offset, long length, PmemAccess access, long handle, FileIdentity identity) {
        this.path = path;
        this.offset = offset;
        this.length = length;
        this.access = access;
        this.handle = handle;
        this.fileChannel = null;
        this.fileIdentity = identity;
        // The native handle is private and cannot be changed while this object is live.
        this.backend = PmemBackend.values()[NativePmem.selectedBackend(handle)];
        this.granularity = PmemGranularity.values()[NativePmem.granularity(handle)];
    }

    PmemMapping(Path path, long offset, long length, PmemAccess access, FileChannel channel, FileIdentity identity) {
        this.path = path;
        this.offset = offset;
        this.length = length;
        this.access = access;
        this.fileChannel = channel;
        this.fileIdentity = identity;
        this.backend = PmemBackend.FILE;
        this.granularity = PmemGranularity.PAGE;
    }

    @Override public long length() { return length; }
    @Override public long fileOffset() { return offset; }
    @Override public Path path() { return path; }
    @Override public PmemAccess access() { return access; }
    @Override public PmemBackend backend() { return backend; }
    @Override public PmemGranularity persistenceGranularity() { return granularity; }
    @Override public boolean isOpen() {
        leases.readLock().lock();
        try { return fileChannel != null ? fileChannel.isOpen() : handle != 0; }
        finally { leases.readLock().unlock(); }
    }

    private long requireOpen() {
        if (fileChannel != null ? !fileChannel.isOpen() : handle == 0)
            throw new IllegalStateException("PMem region is closed");
        return handle;
    }
    private void checkSpan(long index, long amount) {
        if (index < 0 || amount < 0 || index > length || amount > length - index)
            throw new IndexOutOfBoundsException("PMem mapping range out of bounds");
    }
    private void writable() {
        if (access != PmemAccess.READ_WRITE)
            throw new IllegalStateException("PMem mapping is read-only");
    }

    @Override public void read(long index, byte[] target, int arrayOffset, int count) {
        Objects.requireNonNull(target);
        Objects.checkFromIndexSize(arrayOffset, count, target.length);
        checkSpan(index, count);
        leases.readLock().lock();
        try { requireOpen(); readLeased(index, target, arrayOffset, count); }
        finally { leases.readLock().unlock(); }
    }
    @Override public void write(long index, byte[] source, int arrayOffset, int count) {
        Objects.requireNonNull(source);
        Objects.checkFromIndexSize(arrayOffset, count, source.length);
        checkSpan(index, count);
        leases.writeLock().lock();
        try { requireOpen(); writable(); writeLeased(index, source, arrayOffset, count); }
        finally { leases.writeLock().unlock(); }
    }
    @Override public void persist(long index, long count) throws IOException {
        checkSpan(index, count);
        leases.writeLock().lock();
        try {
            long h=requireOpen(); writable();
            if (fileChannel != null) { if (count != 0) fileChannel.force(true); }
            else NativePmem.persist(h, index, count);
        }
        finally { leases.writeLock().unlock(); }
    }
    @Override public void close() throws IOException {
        leases.writeLock().lock();
        try {
            if (fileChannel != null) fileChannel.close();
            else if (handle != 0) { long owned = handle; handle = 0; NativePmem.close(owned); }
        }
        finally { leases.writeLock().unlock(); }
    }
    /** Positional I/O is private and is only called while this owner's lease is held. */
    private void readLeased(long index, byte[] target, int arrayOffset, int count) {
        if (fileChannel == null) { NativePmem.read(handle, index, target, arrayOffset, count); return; }
        ByteBuffer bytes = ByteBuffer.wrap(target, arrayOffset, count);
        readFileLeased(index, bytes);
    }

    /** The caller owns the read lease and may reuse this buffer across a batch. */
    private void readFileLeased(long index, ByteBuffer bytes) {
        long position = offset + index;
        try {
            while (bytes.hasRemaining()) {
                int n = fileChannel.read(bytes, position);
                if (n < 0) throw new EOFException("PMem file was truncated during read");
                if (n == 0) throw new IOException("PMem file read made no progress");
                position += n;
            }
        } catch (IOException failure) { throw new UncheckedIOException(failure); }
    }

    private void writeLeased(long index, byte[] source, int arrayOffset, int count) {
        if (fileChannel == null) { NativePmem.write(handle, index, source, arrayOffset, count); return; }
        ByteBuffer bytes = ByteBuffer.wrap(source, arrayOffset, count);
        long position = offset + index;
        try {
            while (bytes.hasRemaining()) {
                int n = fileChannel.write(bytes, position);
                if (n <= 0) throw new IOException("PMem file write made no progress");
                position += n;
            }
        } catch (IOException failure) { throw new UncheckedIOException(failure); }
    }
    /** One owner lease spans shape-validated row reads, including every FILE window. */
    static void floatRowsLE(PmemRegion region, long byteOffset, long rows, int columns,
                            long firstRow, long[] rowIds, int count,
                            float[] target, int targetOffset) {
        if (!(region instanceof PmemMapping mapping))
            throw new IllegalArgumentException("foreign PmemRegion owner");
        mapping.leases.readLock().lock();
        try {
            long h = mapping.requireOpen();
            if (mapping.fileChannel == null) {
                NativePmem.floatRowsLE(h, byteOffset, rows, columns,
                        firstRow, rowIds, count, target, targetOffset);
            } else {
                floatRowsFile(mapping, byteOffset, columns, firstRow,
                        rowIds, count, target, targetOffset);
            }
        } finally { mapping.leases.readLock().unlock(); }
    }

    /** Scalar semantic oracle: one reusable 64 KiB window, no per-row wrappers. */
    private static void floatRowsFile(PmemMapping mapping, long byteOffset, int columns,
                                       long firstRow, long[] rowIds, int count,
                                       float[] target, int targetOffset) {
        if (count == 0) return;
        long rowBytes = (long) columns * Float.BYTES;
        byte[] scratch = new byte[(int) Math.min(65_536L, rowBytes)];
        ByteBuffer window = ByteBuffer.wrap(scratch);
        int output = targetOffset;
        for (int row = 0; row < count; row++) {
            long rowId = rowIds == null ? firstRow + row : rowIds[row];
            long base = byteOffset + rowId * rowBytes;
            for (int column = 0; column < columns;) {
                int amount = Math.min(columns - column, scratch.length / Float.BYTES);
                window.clear().limit(amount * Float.BYTES);
                mapping.readFileLeased(base + (long) column * Float.BYTES, window);
                for (int i = 0; i < amount; i++) {
                    int at = i * Float.BYTES;
                    int bits = (scratch[at] & 255) | ((scratch[at + 1] & 255) << 8)
                            | ((scratch[at + 2] & 255) << 16) | ((scratch[at + 3] & 255) << 24);
                    target[output++] = Float.intBitsToFloat(bits);
                }
                column += amount;
            }
        }
    }

    // M3_TRANSFER_OWNER_BEGIN - generated by PmemTransferRecipe
    private static final java.util.concurrent.atomic.AtomicLong LEASE_ORDER =
            new java.util.concurrent.atomic.AtomicLong();
    private final long leaseOrder = LEASE_ORDER.incrementAndGet();

    /** One shared preflight and ordered lock-acquisition atom for copy/equality. */
    static boolean operate(PmemRegion from, long fromOffset, PmemRegion to,
                           long toOffset, long bytes, boolean copy) {
        return operateMode(from, fromOffset, to, toOffset, bytes, copy ? 1 : 0);
    }

    static boolean operateIfDifferent(PmemRegion from, long fromOffset, PmemRegion to,
                                      long toOffset, long bytes) {
        return operateMode(from, fromOffset, to, toOffset, bytes, 2);
    }

    /** 0=compare, 1=copy, 2=copy if different; one ordered dual-lease atom. */
    private static boolean operateMode(PmemRegion from, long fromOffset, PmemRegion to,
                                       long toOffset, long bytes, int mode) {
        boolean copy = mode != 0;
        Objects.requireNonNull(from, "source");
        Objects.requireNonNull(to, "destination");
        if (!(from instanceof PmemMapping source) || !(to instanceof PmemMapping destination))
            throw new IllegalArgumentException("foreign PmemRegion owner");
        source.checkSpan(fromOffset, bytes);
        destination.checkSpan(toOffset, bytes);
        if (copy) destination.writable();
        if (source == destination) {
            java.util.concurrent.locks.Lock lock = copy
                    ? source.leases.writeLock() : source.leases.readLock();
            lock.lock();
            try { return invokeTransfer(source, fromOffset, destination, toOffset, bytes, mode); }
            finally { lock.unlock(); }
        }
        PmemMapping first = source.leaseOrder < destination.leaseOrder ? source : destination;
        PmemMapping second = first == source ? destination : source;
        java.util.concurrent.locks.Lock a = (first == destination && copy)
                ? first.leases.writeLock() : first.leases.readLock();
        java.util.concurrent.locks.Lock b = (second == destination && copy)
                ? second.leases.writeLock() : second.leases.readLock();
        a.lock();
        try {
            b.lock();
            try { return invokeTransfer(source, fromOffset, destination, toOffset, bytes, mode); }
            finally { b.unlock(); }
        } finally { a.unlock(); }
    }

    private static boolean invokeTransfer(PmemMapping source, long fromOffset,
                                          PmemMapping destination, long toOffset,
                                          long bytes, int mode) {
        long src = source.requireOpen();
        long dst = destination.requireOpen();
        if (source.fileChannel != null || destination.fileChannel != null)
            return transferFiles(source, fromOffset, destination, toOffset, bytes, mode);
        if (mode != 0) {
            destination.writable();
            if (mode == 2) return NativePmem.copyIfDifferent(src, fromOffset, dst, toOffset, bytes);
            NativePmem.copy(src, fromOffset, dst, toOffset, bytes);
            return true;
        }
        return NativePmem.equalRange(src, fromOffset, dst, toOffset, bytes);
    }

    /** Same-owner overlap uses memmove direction; distinct overlapping file aliases are invalid. */
    private static boolean transferFiles(PmemMapping source, long fromOffset,
                                         PmemMapping destination, long toOffset,
                                         long bytes, int mode) {
        if (mode != 0 && source != destination && bytes != 0) {
            long a = source.offset + fromOffset, b = destination.offset + toOffset;
            if ((a <= b ? b - a < bytes : a - b < bytes)) {
                FileIdentity aIdentity = source.fileIdentity, bIdentity = destination.fileIdentity;
                if (aIdentity == null || bIdentity == null || aIdentity.fileSystem() != bIdentity.fileSystem())
                    throw new IllegalStateException("cannot establish stable identity for potentially overlapping file ranges");
                if (aIdentity.key().equals(bIdentity.key()))
                    throw new IllegalArgumentException("overlapping file ranges across distinct mappings");
            }
        }
        byte[] scratch;
        if (mode != 1) {
            if (bytes == 0 || (source == destination && fromOffset == toOffset)) return mode == 0;
            int capacity = (int) Math.min(32_768L, bytes);
            scratch = new byte[capacity];
            byte[] second = new byte[capacity];
            boolean equal = equalFiles(source, fromOffset, destination, toOffset, bytes, scratch, second);
            if (mode == 0) return equal;
            if (equal) return false;
        } else {
            if (bytes == 0) return true;
            scratch = new byte[(int) Math.min(65_536L, bytes)];
        }
        boolean backwards = source == destination && toOffset > fromOffset && toOffset - fromOffset < bytes;
        long remaining = bytes, done = 0;
        while (remaining != 0) {
            int amount = (int) Math.min(scratch.length, remaining);
            long relative = backwards ? remaining - amount : done;
            source.readLeased(fromOffset + relative, scratch, 0, amount);
            destination.writeLeased(toOffset + relative, scratch, 0, amount);
            remaining -= amount;
            done += amount;
        }
        return true;
    }

    /** Combined scratch is at most 64 KiB, including mixed FILE/native comparisons. */
    private static boolean equalFiles(PmemMapping source, long fromOffset,
                                       PmemMapping destination, long toOffset, long bytes,
                                       byte[] a, byte[] b) {
        for (long done = 0; done < bytes;) {
            int amount = (int) Math.min(a.length, bytes - done);
            source.readLeased(fromOffset + done, a, 0, amount);
            destination.readLeased(toOffset + done, b, 0, amount);
            if (!java.util.Arrays.equals(a, 0, amount, b, 0, amount)) return false;
            done += amount;
        }
        return true;
    }

    /** One lease spans the entire search; close waits for its return. */
    static long indexOf(PmemRegion region, long index, long count, byte[] needle) {
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(needle, "needle");
        if (!(region instanceof PmemMapping mapping))
            throw new IllegalArgumentException("foreign PmemRegion owner");
        mapping.checkSpan(index, count);
        if (needle.length > PmemSearch.MAX_PATTERN_BYTES)
            throw new IllegalArgumentException("PMem search pattern exceeds 1 MiB");
        mapping.leases.readLock().lock();
        try {
            long h = mapping.requireOpen();
            return mapping.fileChannel == null ? NativePmem.indexOf(h, index, count, needle)
                    : indexOfFile(mapping, index, count, needle);
        }
        finally { mapping.leases.readLock().unlock(); }
    }

    /** Streaming KMP: O(count + pattern), 64 KiB scan window and O(pattern) prefix table. */
    private static long indexOfFile(PmemMapping mapping, long index, long count, byte[] needle) {
        if (needle.length == 0) return index;
        if (needle.length > count) return -1;
        // FILE short needles match the original native singleton/pair fast
        // paths without allocating a KMP prefix table on every file.
        if (needle.length == 1) {
            byte target = needle[0];
            byte[] window = fileSearchWindow(count);
            for (long done = 0; done < count;) {
                int amount = (int) Math.min(window.length, count - done);
                mapping.readLeased(index + done, window, 0, amount);
                for (int i = 0; i < amount; i++) {
                    if (window[i] == target) return index + done + i;
                }
                done += amount;
            }
            return -1;
        }
        if (needle.length == 2) {
            byte first = needle[0], second = needle[1];
            byte[] window = fileSearchWindow(count);
            boolean previousFirst = false;
            for (long done = 0; done < count;) {
                int amount = (int) Math.min(window.length, count - done);
                mapping.readLeased(index + done, window, 0, amount);
                for (int i = 0; i < amount; i++) {
                    byte current = window[i];
                    if (previousFirst && current == second)
                        return index + done + i - 1;
                    previousFirst = current == first;
                }
                done += amount;
            }
            return -1;
        }
        int[] prefix = new int[needle.length];
        for (int i = 1, matched = 0; i < needle.length; i++) {
            while (matched != 0 && needle[i] != needle[matched]) matched = prefix[matched - 1];
            if (needle[i] == needle[matched]) matched++;
            prefix[i] = matched;
        }
        byte[] window = fileSearchWindow(count);
        int matched = 0;
        for (long done = 0; done < count;) {
            int amount = (int) Math.min(window.length, count - done);
            mapping.readLeased(index + done, window, 0, amount);
            for (int i = 0; i < amount; i++) {
                while (matched != 0 && window[i] != needle[matched]) matched = prefix[matched - 1];
                if (window[i] == needle[matched]) matched++;
                if (matched == needle.length) return index + done + i - needle.length + 1;
            }
            done += amount;
        }
        return -1;
    }

    /** Binary-search atom, including overflow-safe bounds and one read lease. */
    static long boundLongLE(PmemRegion region, long byteOffset, long elements,
                            long key, boolean strictUpperBound) {
        Objects.requireNonNull(region, "region");
        if (!(region instanceof PmemMapping mapping))
            throw new IllegalArgumentException("foreign PmemRegion owner");
        if (elements < 0 || elements > Long.MAX_VALUE / Long.BYTES)
            throw new IndexOutOfBoundsException("PMem long element count out of bounds");
        mapping.checkSpan(byteOffset, elements * Long.BYTES);
        mapping.leases.readLock().lock();
        try {
            long h = mapping.requireOpen();
            if (mapping.fileChannel == null)
                return NativePmem.boundLongLE(h, byteOffset, elements, key, strictUpperBound);
            byte[] value = new byte[Long.BYTES];
            long low = 0, high = elements;
            while (low < high) {
                long middle = low + ((high - low) >>> 1);
                mapping.readLeased(byteOffset + middle * Long.BYTES, value, 0, value.length);
                long decoded = 0;
                for (int i = 0; i < Long.BYTES; i++) decoded |= (value[i] & 255L) << (i * 8);
                if (strictUpperBound ? decoded <= key : decoded < key) low = middle + 1;
                else high = middle;
            }
            return low;
        } finally { mapping.leases.readLock().unlock(); }
    }
    /**
     * Pin an SDK-owned region for an entire multi-window read operation.
     * This reuses the canonical ReentrantReadWriteLock: nested read() and JNI
     * KMP calls are safe, and close()/write()/persist() wait for scan exit.
     * No native handle or direct address escapes this package-private atom.
     */
    static <T> T withReadLease(PmemRegion region, java.util.function.Supplier<T> action) {
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(action, "action");
        if (!(region instanceof PmemMapping mapping))
            throw new IllegalArgumentException("foreign PmemRegion owner");
        mapping.leases.readLock().lock();
        try {
            mapping.requireOpen();
            return action.get();
        } finally { mapping.leases.readLock().unlock(); }
    }

    // M3_TRANSFER_OWNER_END
}
