// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.zip.CRC32C;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

/**
 * Spillable, parent-owned segmented logical array. add(value) handles allocation,
 * headroom sampling and LRU eviction internally. Only the explicitly provided
 * codec's value semantics survive swapping; raw Java object identity does not.
 *
 * All public methods synchronize on this array. Storage is ephemeral and local.
 */
//FLLDCIM: RELOCATE - donor blob 32b1aaeee5f3 at 165ceb5a0202; recipe m3/collections/recipe/residency-paged-array-20261107/relocate.py v4
public final class M3PagedArray<T> implements AutoCloseable {
    private static final int MAGIC = 0x4D335041; // M3PA
    private static final int VERSION = 1;
    /**
     * Only one passive pressure poll per 1,024 successful resident accesses.
     * Between polls, resident accesses perform only a primitive counter/mask
     * check; a poll may sample JVM/JNI pressure and spill bounded cold pages.
     */
    private static final int HOT_PRESSURE_SAMPLE_MASK = 1023;
    private static final int MAX_OPTIONAL_EVICTIONS_PER_POLL = 8;
    /** Successful removal can lack backing after an earlier reentrant spill failure. */
    private static final Object[] EVICTED_WITHOUT_STORAGE = new Object[0];

    public interface Codec<T> {
        void write(DataOutput out, T value) throws IOException;
        T read(DataInput in) throws IOException;
        /** Reject unswappable values at insertion, not during a future LRU eviction. */
        default void validate(T value) { }
        /** Upper bound for one encoded value, or -1 for a codec with unknown shape. */
        default long maximumEncodedBytes() { return -1L; }
    }

    /** Lossless String codec: UTF-16 code units, including unpaired surrogates. */
    public static Codec<String> strings(int maximumCodeUnits) {
        if (maximumCodeUnits < 0) throw new IllegalArgumentException("maximumCodeUnits");
        return new Codec<>() {
            @Override public void validate(String value) {
                if (value.length() > maximumCodeUnits)
                    throw new IllegalArgumentException("String exceeds configured UTF-16 code units");
            }
            @Override public long maximumEncodedBytes() { return 4L + 2L * maximumCodeUnits; }
            @Override public void write(DataOutput out, String value) throws IOException {
                if (value.length() > maximumCodeUnits) throw new IOException("string exceeds codec limit");
                out.writeInt(value.length());
                for (int i = 0; i < value.length(); i++) out.writeChar(value.charAt(i));
            }
            @Override public String read(DataInput in) throws IOException {
                int n = in.readInt();
                if (n < 0 || n > maximumCodeUnits) throw new IOException("invalid string length");
                char[] chars = new char[n];
                for (int i = 0; i < n; i++) chars[i] = in.readChar();
                return new String(chars);
            }
        };
    }

    public record Stats(long size, int residentPages, int totalPages, long spillCount,
                        long pageInCount, M3MemoryPressure.Snapshot heap) { }

    private final Path scratch;
    private final int pageSize;
    private final long estimatedPageBytes;
    private final long minimumHeapHeadroom;
    private final long minimumPhysicalHeadroom;
    private final int maxResidentPages;
    private final long maxSerializedPageBytes;
    private final Codec<T> codec;
    private final M3MemoryPressure.Probe probe;
    private final ArrayList<Page> pages = new ArrayList<>();
    /*
     * CPU-first O(1) hot append: no page-table lookup, access-order map update,
     * native sampler or filesystem operation while this tail remains resident.
     * Both pointers are protected by the outer synchronized API.
     */
    private Page appendTail;
    private Page lastLruAccess;
    private int residentAccesses;
    private long deferredPressureFailures;
    private final LinkedHashMap<Integer, Page> residents = new LinkedHashMap<>(16, .75f, true);
    private final ArrayList<Path> retiredFiles = new ArrayList<>();
    private long size;
    private long spillCount;
    private long pageInCount;
    private boolean closed;
    private boolean constructingValue;

    /**
     * Normal constructor: choose a bounded page length from the codec's maximum
     * encoded element size when known; sample effective JVM Xmx automatically.
     */
    public M3PagedArray(Path scratch, Codec<T> codec) throws IOException {
        this(scratch, defaultPageSize(codec), defaultEstimatedPageBytes(codec),
                32L << 20, 128L << 20, 32L << 20,
                codec, M3MemoryPressure.runtimeWithOptionalNative(), 0);
    }

    private static <T> int defaultPageSize(Codec<T> codec) {
        Objects.requireNonNull(codec);
        long upper = codec.maximumEncodedBytes();
        if (upper <= 0L) return 256; // caller's custom codec has no proven bound
        // Reserve 3/4 of the serialized page cap for safety and metadata.
        long safe = ((32L << 20) / 4L - 16L) / saturatedAdd(upper, 1L);
        return (int) Math.max(1L, Math.min(256L, safe));
    }

    /**
     * The default resident budget must reflect what one full page can hold.
     * A fixed 64-KiB estimate for 64 x 128-KiB UTF-16 values falsely admitted
     * about 128 times too many pages under the effective JVM -Xmx.
     * This conservative estimate uses the codec's declared encoded maximum,
     * plus references/array overhead; it does NOT invent an exact JVM heap
     * size for arbitrary external user objects. Unbounded custom codecs retain
     * the legacy 64-KiB estimate and need an explicit bounded codec for safety.
     * Sampling remains at new-page/page-in boundaries, never on every add.
     */
    private static <T> long defaultEstimatedPageBytes(Codec<T> codec) {
        int count = defaultPageSize(codec);
        long upper = codec.maximumEncodedBytes();
        if (upper <= 0L) return 65_536L; // Unknown codec: no defensible value-size upper bound.
        if (upper > (32L << 20) - 32L) {
            throw new IllegalArgumentException("element exceeds maximum serialized page");
        }
        long payload = upper > Long.MAX_VALUE / count
                ? Long.MAX_VALUE : upper * count;
        long overhead = 256L + 16L * count;
        return Math.max(65_536L, saturatedAdd(payload, overhead));
    }

    /**
     * Explicit policy is useful for testing and tuning; maxResidentPages=0
     * selects a budget of about one fifth of Xmx (minimum one page).
     */
    public M3PagedArray(Path scratch, int pageSize, long estimatedPageBytes,
                        long minimumHeapHeadroom, long minimumPhysicalHeadroom,
                        long maxSerializedPageBytes, Codec<T> codec,
                        M3MemoryPressure.Probe probe, int maxResidentPages) throws IOException {
        if (pageSize <= 0 || pageSize > 1_048_576 || estimatedPageBytes <= 0
                || minimumHeapHeadroom < 0 || minimumPhysicalHeadroom < 0
                || maxSerializedPageBytes < 32 || maxResidentPages < 0)
            throw new IllegalArgumentException("invalid page or memory policy");
        this.scratch = Objects.requireNonNull(scratch);
        this.codec = Objects.requireNonNull(codec);
        this.probe = Objects.requireNonNull(probe);
        this.pageSize = pageSize;
        this.estimatedPageBytes = estimatedPageBytes;
        this.minimumHeapHeadroom = minimumHeapHeadroom;
        this.minimumPhysicalHeadroom = minimumPhysicalHeadroom;
        this.maxSerializedPageBytes = maxSerializedPageBytes;
        M3MemoryPressure.Snapshot s = probe.sample();
        long computed = Math.max(1L, (s.maxHeap() / 5L) / estimatedPageBytes);
        this.maxResidentPages = maxResidentPages != 0 ? maxResidentPages
                : (int) Math.min(Integer.MAX_VALUE, computed);
        Files.createDirectories(scratch);
    }

    /**
     * Blazing-fast common case: one cached hot tail, an array store and a count.
     * No LRU hash walk or JNI/heap sample during repeated resident-page appends.
     * Preserve access-order only when a different page was touched in between.
     */
    public synchronized void add(T value) {
        checkOpen();
        checkFactoryCallback();
        if (size == Long.MAX_VALUE) throw new IllegalStateException("array too large");
        if (value != null) codec.validate(value);
        Page tail = appendTail;
        if (tail != null && tail.values != null && tail.count < pageSize) {
            // Keep LRU exact after interleaved reads; skip map work otherwise.
            if (lastLruAccess != tail) {
                residents.get(tail.id);
                lastLruAccess = tail;
            }
            // Runtime.maxMemory/used-heap and optional native hints are sampled
            // periodically, even when no page allocation or cold load occurs.
            maybeTrimResidentPressure(tail);
            tail.values[tail.count++] = value;
            tail.dirty = true;
            size++;
            return;
        }
        // Cold tail, full page or first insert: rare boundary path.
        Page last = pages.isEmpty() ? null : pages.get(pages.size() - 1);
        if (last == null || last.count == pageSize) {
            if (pages.size() == Integer.MAX_VALUE) throw new IllegalStateException("too many pages");
            int id = pages.size();
            Object[] storage;
            try { storage = prepareResidentSpace(null); }
            catch (IOException e) { throw new UncheckedIOException(e); }
            Page next = new Page(id, storage);
            // Publish only after pressure admission and allocation succeeded.
            pages.add(next);
            residents.put(id, next);
            appendTail = next;
            lastLruAccess = next;
            next.values[0] = value;
            next.count = 1;
            size++;
        } else {
            try { load(last); }
            catch (IOException e) { throw new UncheckedIOException(e); }
            appendTail = last;
            last.values[last.count++] = value;
            last.dirty = true;
            size++;
        }
    }

    /**
     * Internal one-value-page append for the existing column parent. The factory
     * is invoked only after the unchanged resident admission; null values retain
     * add(null) semantics. No new authority is published on factory/validation failure.
     * Observations remain readable, but callbacks cannot acquire, mutate or close this owner.
     */
    synchronized void addFromFactory(Supplier<? extends T> factory) {
        checkOpen();
        checkFactoryCallback();
        Objects.requireNonNull(factory, "factory");
        if (pageSize != 1) throw new IllegalStateException("factory append requires one-value pages");
        if (size == Long.MAX_VALUE) throw new IllegalStateException("array too large");
        if (pages.size() == Integer.MAX_VALUE) throw new IllegalStateException("too many pages");
        int id = pages.size();
        constructingValue = true;
        try {
            Object[] storage;
            try { storage = prepareResidentSpace(null); }
            catch (IOException e) { throw new UncheckedIOException(e); }
            Page next = new Page(id, storage);
            T value = factory.get();
            if (value != null) codec.validate(value);
            // Same one-slot append publication as the original add boundary.
            pages.add(next);
            residents.put(id, next);
            appendTail = next;
            lastLruAccess = next;
            next.values[0] = value;
            next.count = 1;
            size++;
        } finally {
            constructingValue = false;
        }
    }

    private void checkFactoryCallback() {
        if (constructingValue)
            throw new IllegalStateException("array factory callback cannot mutate or acquire");
    }

    public synchronized long size() { return size; }

    /** Read-only effective per-page resident reservation used by automatic -Xmx policy. */
    public synchronized long estimatedResidentPageBytes() {
        checkOpen();
        return estimatedPageBytes;
    }

    /** Read-only resident LRU page budget; callers never manage capacity themselves. */
    public synchronized int residentPageLimit() {
        checkOpen();
        return maxResidentPages;
    }

    /** Diagnostics only: failures of optional pressure eviction never lose a hot value. */
    public synchronized long deferredPressureFailures() {
        checkOpen();
        return deferredPressureFailures;
    }

    @SuppressWarnings("unchecked")
    public synchronized T get(long index) {
        Page p = indexed(index);
        try { load(p); }
        catch (IOException e) { throw new UncheckedIOException(e); }
        return (T) p.values[(int) (index % pageSize)];
    }

    /** Mutation of a page already on disk is re-spilled safely on eviction. */
    public synchronized T set(long index, T value) {
        Page p = indexed(index);
        if (value != null) codec.validate(value);
        try { load(p); }
        catch (IOException e) { throw new UncheckedIOException(e); }
        int offset = (int) (index % pageSize);
        @SuppressWarnings("unchecked") T old = (T) p.values[offset];
        p.values[offset] = value;
        p.dirty = true;
        return old;
    }

    /** A leased element's page stays resident until the lease closes. */
    public synchronized Lease pin(long index) {
        Page p = indexed(index);
        try { load(p); }
        catch (IOException e) { throw new UncheckedIOException(e); }
        p.pins++;
        return new Lease(p, (int) (index % pageSize));
    }

    public final class Lease implements AutoCloseable {
        private final Page page;
        private final int offset;
        private boolean released;
        private Lease(Page page, int offset) { this.page = page; this.offset = offset; }
        @SuppressWarnings("unchecked")
        public T value() {
            synchronized (M3PagedArray.this) {
                if (released || closed) throw new IllegalStateException("lease closed");
                return (T) page.values[offset];
            }
        }
        @Override public void close() {
            synchronized (M3PagedArray.this) {
                if (!released) { page.pins--; released = true; }
            }
        }
    }

    /** Proactive trim; ordinary add/get already apply pressure policy. */
    public synchronized int trimTo(int targetResidentPages) {
        checkOpen();
        checkFactoryCallback();
        if (targetResidentPages < 0) throw new IllegalArgumentException("target");
        int evicted = 0;
        try {
            while (residents.size() > targetResidentPages) {
                if (evictOldest(null) == null) break;
                evicted++;
            }
        } catch (IOException e) { throw new UncheckedIOException(e); }
        return evicted;
    }

    public synchronized Stats stats() {
        checkOpen();
        return new Stats(size, residents.size(), pages.size(), spillCount, pageInCount, probe.sample());
    }

    private Page indexed(long index) {
        checkOpen();
        checkFactoryCallback();
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
        return pages.get((int) (index / pageSize));
    }

    private void load(Page page) throws IOException {
        if (page.values != null) {
            // Existing page is resident; map touch only on an actual new access.
            if (lastLruAccess != page) {
                residents.get(page.id);
                lastLruAccess = page;
            }
            maybeTrimResidentPressure(page);
            return;
        }
        Object[] data = prepareResidentSpace(page);
        // Only the private container is reused; decode still creates codec values.
        // Corruption leaves disk authority intact and publishes no partial array.
        if (data == null) data = new Object[pageSize];
        if (page.disk != null) {
            if (Files.size(page.disk) > maxSerializedPageBytes) throw new IOException("page too large");
            CRC32C checksum = new CRC32C();
            try (DataInputStream in = new DataInputStream(new CheckedInputStream(Files.newInputStream(page.disk), checksum))) {
                if (in.readInt() != MAGIC || in.readInt() != VERSION || in.readInt() != page.id
                        || in.readInt() != page.count) throw new IOException("page header mismatch");
                for (int i = 0; i < page.count; i++) if (in.readBoolean()) data[i] = codec.read(in);
                if (in.read() != -1 || checksum.getValue() != page.checksum)
                    throw new IOException("page checksum or trailing bytes mismatch");
            }
            pageInCount++;
        }
        page.values = data;
        page.dirty = false;
        residents.put(page.id, page);
        lastLruAccess = page;
    }

    /**
     * Best-effort release of optional *other* resident pages when external JVM
     * allocations create pressure without a new collection page boundary.
     *
     * This is deliberately a bounded, non-throwing side effect of a successful
     * resident access. No required value or pinned page is released. A failed
     * codec/disk spill leaves the exact prior resident mapping untouched and
     * can be retried at a later boundary. Hard admission still fails normally
     * when a later allocation cannot reserve memory.
     */
    private void maybeTrimResidentPressure(Page active) {
        if (((++residentAccesses) & HOT_PRESSURE_SAMPLE_MASK) != 0 || residents.size() <= 1)
            return;
        M3MemoryPressure.Snapshot memory;
        try {
            memory = Objects.requireNonNull(probe.sample(), "pressure sample");
        } catch (RuntimeException failure) {
            noteDeferredPressureFailure();
            return; // Telemetry cannot invalidate a previously valid hot access.
        }
        boolean physicalLow = memory.physicalLow(effectivePhysicalMinimum(memory));
        double usedRatio = (double) memory.usedHeap() / (double) memory.maxHeap();
        if (usedRatio < 0.75 && !physicalLow) return;
        // Soft pressure trims only excessive optional retention; hard or native
        // pressure sheds cold residents in bounded batches, never the active page.
        int limit = usedRatio >= 0.90 || physicalLow
                ? Math.min(MAX_OPTIONAL_EVICTIONS_PER_POLL, residents.size() - 1)
                : residents.size() > Math.max(2, maxResidentPages / 2) ? 1 : 0;
        for (int i = 0; i < limit; i++) {
            try {
                if (evictOldest(active) == null) break;
            } catch (IOException | RuntimeException spillFailure) {
                // Optional eviction cannot fail an otherwise valid hot access.
                // Errors remain fatal; required admission/trim failures still propagate.
                noteDeferredPressureFailure();
                break;
            }
        }
    }

    private void noteDeferredPressureFailure() {
        if (deferredPressureFailures != Long.MAX_VALUE) deferredPressureFailures++;
    }

    /**
     * A private page array has no API/codec/lease escape: only its elements do.
     * Transfer one cleared capacity victim directly to the admitted page. The
     * method-local array is unavailable to reentrant callbacks and is never an
     * idle pool; pressure admission must release it before shedding more pages.
     */
    private Object[] prepareResidentSpace(Page incoming) throws IOException {
        Object[] storage = null;
        long releasedEstimate = 0L;
        while (residents.size() >= maxResidentPages) {
            Object[] evicted = evictOldest(incoming);
            if (evicted == null) throw new IllegalStateException("all resident pages pinned");
            if (storage == null && evicted.length == pageSize) {
                Arrays.fill(evicted, null);
                storage = evicted;
            }
            // The admission sample below is taken after these budget evictions.
            // Their reclaimed bytes must not also enter the pending-release estimate.
        }
        M3MemoryPressure.Snapshot memory = probe.sample();
        long observedHeadroom = memory.headroom();
        long required = saturatedAdd(effectiveHeapMinimum(memory), estimatedPageBytes);
        while (saturatedAdd(memory.headroom(), releasedEstimate) < required
                || memory.physicalLow(effectivePhysicalMinimum(memory))) {
            // Pressure shrink releases actual backing instead of retaining an idle slab.
            storage = null;
            if (evictOldest(incoming) == null) {
                if (saturatedAdd(memory.headroom(), releasedEstimate) < required
                        || memory.physicalLow(effectivePhysicalMinimum(memory)))
                    throw new IllegalStateException("memory pressure: no evictable pages");
                break;
            }
            releasedEstimate = saturatedAdd(releasedEstimate, estimatedPageBytes);
            // A fresh sample may already include reclaimed bytes. Discharge that
            // observed recovery from the pending estimate instead of crediting it twice.
            // Unobserved releases still retain the original allocation forecast.
            memory = probe.sample();
            long currentHeadroom = memory.headroom();
            long recovered = Math.max(0L, currentHeadroom - observedHeadroom);
            releasedEstimate = recovered >= releasedEstimate ? 0L : releasedEstimate - recovered;
            observedHeadroom = currentHeadroom;
        }
        // A successful sampled admission already checked current pressure.
        residentAccesses = 0;
        return storage;
    }

    /** Scale a configured heap reserve to actual effective -Xmx. */
    private long effectiveHeapMinimum(M3MemoryPressure.Snapshot memory) {
        return Math.min(minimumHeapHeadroom,
                Math.max(estimatedPageBytes, memory.maxHeap() / 32L));
    }

    /**
     * A fixed 128 MiB reserve cannot be satisfied by an otherwise healthy
     * 32 MiB cgroup. Honor configured headroom as an upper limit, scaled down
     * for smaller container/physical limits; never increase reported RAM.
     */
    private long effectivePhysicalMinimum(M3MemoryPressure.Snapshot snapshot) {
        if (snapshot.physicalTotal() <= 0) return minimumPhysicalHeadroom;
        long proportional = Math.max(estimatedPageBytes, snapshot.physicalTotal() / 32);
        return Math.min(minimumPhysicalHeadroom, Math.min(snapshot.physicalTotal(), proportional));
    }

    private static long saturatedAdd(long a, long b) {
        return a >= Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b;
    }

    /** Return detached capacity or empty success status after eviction; null means no victim. */
    private Object[] evictOldest(Page exclude) throws IOException {
        for (Iterator<Map.Entry<Integer, Page>> it = residents.entrySet().iterator(); it.hasNext();) {
            Page p = it.next().getValue();
            if (p == exclude || p.pins > 0) continue;
            Object[] storage = p.values;
            spill(p); // May throw; page remains unchanged if I/O fails.
            it.remove();
            if (appendTail == p) appendTail = null;
            if (lastLruAccess == p) lastLruAccess = null;
            return storage == null ? EVICTED_WITHOUT_STORAGE : storage;
        }
        return null;
    }

    private void spill(Page p) throws IOException {
        if (!p.dirty && p.disk != null) {
            p.values = null;
            spillCount++;
            return;
        }
        Path tmp = Files.createTempFile(scratch, "m3p-", ".tmp");
        Path target = null;
        CRC32C checksum = new CRC32C();
        try {
            try (FileChannel channel = FileChannel.open(tmp, StandardOpenOption.WRITE);
                 DataOutputStream out = new DataOutputStream(new CheckedOutputStream(
                         new BoundedOutputStream(Channels.newOutputStream(channel), maxSerializedPageBytes), checksum))) {
                out.writeInt(MAGIC);
                out.writeInt(VERSION);
                out.writeInt(p.id);
                out.writeInt(p.count);
                for (int i = 0; i < p.count; i++) {
                    Object o = p.values[i];
                    out.writeBoolean(o != null);
                    if (o != null) {
                        @SuppressWarnings("unchecked") T v = (T) o;
                        codec.write(out, v);
                    }
                }
                out.flush();
                channel.force(true);
            }
            if (Files.size(tmp) > maxSerializedPageBytes) throw new IOException("encoded page exceeds limit");
            target = Files.createTempFile(scratch, "m3p-", ".page");
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException | Error failure) {
            // No page state has changed. Retain the primary failure even if cleanup fails.
            discardFailedSpill(tmp, failure);
            if (target != null) discardFailedSpill(target, failure);
            throw failure;
        }
        // Commit only after complete encoding and atomic publication. Never run
        // rollback cleanup against this new authoritative image after this point.
        Path previous = p.disk;
        p.disk = target;
        p.checksum = checksum.getValue();
        p.values = null;
        p.dirty = false;
        spillCount++;
        if (previous != null) retireSpillFile(previous);
    }

    private void discardFailedSpill(Path path, Throwable primary) {
        try { Files.deleteIfExists(path); }
        catch (IOException | RuntimeException cleanup) {
            if (cleanup != primary) primary.addSuppressed(cleanup);
            retiredFiles.add(path);
        }
    }

    private void retireSpillFile(Path path) {
        try { Files.deleteIfExists(path); }
        catch (IOException | SecurityException cleanup) {
            // Failure to retire an obsolete file must not undo a successful spill.
            retiredFiles.add(path);
        }
    }

    /** Reject oversized serialized images while writing, not after exhausting disk. */
    private static final class BoundedOutputStream extends FilterOutputStream {
        private final long limit;
        private long written;
        BoundedOutputStream(OutputStream delegate, long limit) {
            super(delegate);
            this.limit = limit;
        }
        @Override public void write(int value) throws IOException {
            ensure(1);
            out.write(value);
            written++;
        }
        @Override public void write(byte[] data, int off, int len) throws IOException {
            Objects.checkFromIndexSize(off, len, data.length);
            ensure(len);
            out.write(data, off, len);
            written += len;
        }
        private void ensure(long next) throws IOException {
            if (next > limit - written) throw new IOException("serialized page byte budget exceeded");
        }
    }

    private void checkOpen() {
        if (closed) throw new IllegalStateException("array closed");
    }

    @Override public synchronized void close() throws IOException {
        if (closed) return;
        checkFactoryCallback();
        for (Page p : pages) if (p.pins != 0) throw new IllegalStateException("pinned page " + p.id);
        IOException failure = null;
        for (Page p : pages) {
            try { if (p.disk != null) Files.deleteIfExists(p.disk); }
            catch (IOException e) { if (failure == null) failure = e; else failure.addSuppressed(e); }
            p.values = null;
        }
        for (Path old : retiredFiles) {
            try { Files.deleteIfExists(old); }
            catch (IOException e) { if (failure == null) failure = e; else failure.addSuppressed(e); }
        }
        residents.clear();
        pages.clear();
        appendTail = null;
        lastLruAccess = null;
        closed = true;
        if (failure != null) throw failure;
    }

    private final class Page {
        final int id;
        Object[] values;
        int count;
        int pins;
        boolean dirty = true;
        long checksum;
        Path disk;
        Page(int id, Object[] storage) {
            this.id = id;
            values = storage == null ? new Object[pageSize] : storage;
        }
    }
}
