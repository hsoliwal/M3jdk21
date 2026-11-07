// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Parent-owned residency for transient object facades over stable logical IDs.
 *
 * <p>The parent remains the semantic owner. This store retains only optional Java facade
 * objects and lifecycle bits; application state belongs in the parent's primitive/columnar
 * lanes. An exposed facade is pinned and its Java identity is stable until logical removal.
 * An unexposed facade may be retained or recreated according to construction flags.</p>
 *
 * <p>This class deliberately does not serialize or swap live facade objects. Cold payload
 * swap belongs to the existing immutable/versioned M3 swap owners. Pressure trimming here
 * only drops unpinned facades that can be reconstructed from parent state.</p>
 */
//FLLDCIM: RELOCATE - donor blob de85b08a83a7 at fd9a233680b4; recipe m3/collections/recipe/transient-facade-20261106/relocate.py v4
//FLLDCIM: RELOCATE - donor blob 2dc527d4efa1 at eaacff5931ef; recipe m3/collections/recipe/transient-facade-20261106/relocate.py v4
public final class M3TransientFacadeStore<T> implements AutoCloseable {
    public static final int RECREATE_UNEXPOSED = 1 << 0;
    public static final int RETAIN_UNEXPOSED = 1 << 1;
    public static final int PIN_EXPOSED = 1 << 2;
    public static final int PRESSURE_TRIMMABLE = 1 << 3;
    /** Sample JVM -Xmx and trim optional facades at admission boundaries. */
    public static final int AUTO_HEAP_PRESSURE = 1 << 4;

    public static final int DEFAULT_MODE =
            RECREATE_UNEXPOSED | PIN_EXPOSED | PRESSURE_TRIMMABLE;

    @FunctionalInterface
    public interface Factory<T> {
        T create(long logicalId);
    }

    @FunctionalInterface
    public interface Disposer<T> {
        void dispose(long logicalId, T facade);
    }

    /** Scrub external references before idle; rebind only proven non-escaping facades. */
    public interface Recycler<T> {
        void scrub(T facade);
        void rebind(T facade, long logicalId);
    }

    public record Stats(
            int resident,
            int pinned,
            long creations,
            long disposals,
            long hits,
            long misses,
            long trims) {
    }

    private final Factory<T> factory;
    private final Disposer<T> disposer;
    private final int mode;
    private final M3FacadePressureController autoPressure;
    private final Recycler<T> recycler;
    private final int maxIdle;
    private static final Object[] NO_FREE_FACADES = new Object[0];
    private static final long[] NO_FREE_IDS = new long[0];
    private Object[] freeFacades = NO_FREE_FACADES;
    private long[] freeIds = NO_FREE_IDS;
    private int freeCount;
    private long reuseCount;

    private long[] ids = new long[4];
    private Object[] facades = new Object[4];
    private short[] flags = new short[4];
    private long[] accessEpochs = new long[4];
    private int size;
    private long epoch;
    private long creations;
    private long disposals;
    private long hits;
    private long misses;
    private long trims;
    private boolean closed;

    public M3TransientFacadeStore(Factory<T> factory) {
        this(factory, (logicalId, facade) -> { }, DEFAULT_MODE);
    }

    public M3TransientFacadeStore(Factory<T> factory, int mode) {
        this(factory, (logicalId, facade) -> { }, mode);
    }

    public M3TransientFacadeStore(
            Factory<T> factory,
            Disposer<T> disposer,
            int mode) {
        this(factory, disposer, mode, M3RuntimeHeap::current);
    }

    /** Deterministic heap injection for same-package contract tests. */
    M3TransientFacadeStore(
            Factory<T> factory,
            Disposer<T> disposer,
            int mode,
            Supplier<M3RuntimeHeap.Snapshot> pressureSource) {
        this(factory, disposer, mode, pressureSource, null, 0);
    }

    /**
     * An explicit non-escape/owner-qualified, bounded recycler. Unlike heap
     * reference observations, a completed internal callback is a lease boundary.
     * Reuse is forbidden for public/pinned facade instances.
     */
    public M3TransientFacadeStore(Factory<T> factory, Disposer<T> disposer,
            int mode, Recycler<T> recycler, int maxIdle) {
        this(factory, disposer, mode, M3RuntimeHeap::current, recycler, maxIdle);
    }

    /** Deterministic pressure injection for recycler contract tests. */
    M3TransientFacadeStore(Factory<T> factory, Disposer<T> disposer,
            int mode, Supplier<M3RuntimeHeap.Snapshot> pressureSource,
            Recycler<T> recycler, int maxIdle) {
        this.factory = Objects.requireNonNull(factory, "factory");
        this.disposer = Objects.requireNonNull(disposer, "disposer");
        validateMode(mode);
        if (recycler == null ? maxIdle != 0 : maxIdle < 1 || maxIdle > 256) {
            throw new IllegalArgumentException("recycler requires idle capacity 1..256");
        }
        if (recycler != null && (mode & RECREATE_UNEXPOSED) == 0) {
            throw new IllegalArgumentException("recycler requires recreate-unexposed mode");
        }
        this.recycler = recycler;
        this.maxIdle = maxIdle;
        this.mode = mode;
        this.autoPressure = (mode & AUTO_HEAP_PRESSURE) == 0 ? null
                : new M3FacadePressureController(pressureSource);
    }

    public int mode() {
        return mode;
    }

    /** Public/API escape boundary: materialize once and pin identity until logical removal. */
    public synchronized T expose(long logicalId) {
        checkOpen();
        checkId(logicalId);
        int slot = find(logicalId);
        if (slot >= 0) {
            hits = Math.addExact(hits, 1);
            flags[slot] = M3LazyElementFlags.add(
                    flags[slot],
                    (short) (M3LazyElementFlags.MATERIALIZED | M3LazyElementFlags.PINNED));
            touch(slot);
            return value(slot);
        }
        misses = Math.addExact(misses, 1);
        T facade = create(logicalId);
        slot = insertCreated(logicalId, facade);
        flags[slot] = M3LazyElementFlags.add(
                flags[slot],
                (short) (M3LazyElementFlags.MATERIALIZED | M3LazyElementFlags.PINNED));
        touch(slot);
        return facade;
    }

    /**
     * Internal non-escaping use.
     *
     * <p>The action must not retain the supplied facade. Compiler-qualified call sites may use
     * this path when lifetime analysis proves that the reference does not escape.</p>
     */
    public synchronized void withInternal(long logicalId, Consumer<? super T> action) {
        checkOpen();
        checkId(logicalId);
        Objects.requireNonNull(action, "action");
        int slot = find(logicalId);
        if (slot >= 0) {
            hits = Math.addExact(hits, 1);
            touch(slot);
            action.accept(value(slot));
            return;
        }

        misses = Math.addExact(misses, 1);
        // A compiler-qualified internal lease may retain a facade only while
        // pressure permits. Publicly exposed facades remain pinned regardless.
        boolean retain = (mode & RETAIN_UNEXPOSED) != 0
                && (autoPressure == null || autoPressure.mayRetain(this));
        boolean reuseAllowed = recycler != null
                && (autoPressure == null || autoPressure.mayRetain(this));
        T facade = borrowInternal(logicalId, reuseAllowed);
        if (retain) {
            slot = insertCreated(logicalId, facade);
            flags[slot] = M3LazyElementFlags.add(flags[slot], M3LazyElementFlags.MATERIALIZED);
            touch(slot);
            action.accept(facade);
            return;
        }

        // Preserve the original try/finally exception precedence for every
        // legacy constructor. Only the opt-in recycler changes lease cleanup.
        if (recycler == null) {
            try {
                action.accept(facade);
            } finally {
                dispose(logicalId, facade);
            }
            return;
        }

        Throwable thrown = null;
        try {
            action.accept(facade);
        } catch (RuntimeException | Error failure) {
            thrown = failure;
            throw failure;
        } finally {
            try {
                releaseInternal(logicalId, facade, reuseAllowed);
            } catch (RuntimeException | Error failure) {
                if (thrown == null) throw failure;
                suppressDistinct(thrown, failure);
            }
        }
    }

    public synchronized T peekResident(long logicalId) {
        checkOpen();
        checkId(logicalId);
        int slot = find(logicalId);
        return slot < 0 ? null : value(slot);
    }

    public synchronized boolean isResident(long logicalId) {
        checkOpen();
        checkId(logicalId);
        return find(logicalId) >= 0;
    }

    public synchronized boolean isPinned(long logicalId) {
        checkOpen();
        checkId(logicalId);
        int slot = find(logicalId);
        return slot >= 0 && M3LazyElementFlags.has(flags[slot], M3LazyElementFlags.PINNED);
    }

    public synchronized short residencyFlags(long logicalId) {
        checkOpen();
        checkId(logicalId);
        int slot = find(logicalId);
        return slot < 0 ? 0 : flags[slot];
    }

    /**
     * Drops least-recently-used unpinned facade residency down to the requested target.
     *
     * <p>Pinned facade identity always wins over the target. Returns the number of facades
     * actually released.</p>
     */
    public synchronized int releaseOptionalResidency(int targetResident) {
        checkOpen();
        if (targetResident < 0) throw new IllegalArgumentException("targetResident < 0");
        if ((mode & PRESSURE_TRIMMABLE) == 0) return 0;

        int excess = size - targetResident;
        if (excess <= 0) return 0;
        // One primitive heapify and one compacting pass. Do not perform
        // O(size * evictions) repeated shifts during a memory-pressure event.
        int[] victims = M3ResidentTrimPlanner.oldestSlots(flags, accessEpochs, size, excess);
        if (victims.length == 0) return 0;
        //FLLDCIM: MRL - reentrant Disposer during trim disposed the in-flight victim twice, then (0, null); detach first
        Backing detached = detachVictims(victims);
        int released = 0;
        try {
            for (; released < victims.length; released++) {
                @SuppressWarnings("unchecked") T facade = (T) detached.facades()[released];
                dispose(detached.ids()[released], facade);
            }
        } catch (RuntimeException | Error failure) {
            // If a disposer throws, only *successfully* disposed entries leave
            // the store. The remaining IDs and exposed references stay intact.
            returnVictims(detached, released, failure);
            throw failure;
        } finally {
            if (released != 0) trims = Math.addExact(trims, released);
        }
        return released;
    }

    /** Number of scrubbed, idle internal-only facades, not GC reference counts. */
    public synchronized int reusableIdleCount() {
        checkOpen();
        return freeCount;
    }

    public synchronized long reuseCount() {
        checkOpen();
        return reuseCount;
    }

    /** Drop idle capacity deterministically at an owner or pressure boundary. */
    public synchronized int releaseReusable(int targetIdle) {
        checkOpen();
        if (targetIdle < 0) throw new IllegalArgumentException("targetIdle < 0");
        int released = 0;
        while (freeCount > targetIdle) {
            // Detach before invoking user code. A failed disposer must never
            // leave a partially disposed facade eligible for reborrowing.
            int n = --freeCount;
            @SuppressWarnings("unchecked") T facade = (T) freeFacades[n];
            long retiredId = freeIds[n];
            freeFacades[n] = null;
            freeIds[n] = 0L;
            dispose(retiredId, facade);
            released++;
        }
        // Under pressure or explicit retirement, drop optional empty backing too.
        // Active leases and pinned facades remain independent.
        if (targetIdle == 0 && freeCount == 0) {
            freeFacades = NO_FREE_FACADES;
            freeIds = NO_FREE_IDS;
        }
        return released;
    }

    /** Semantic deletion: releases even a pinned facade because its logical element is gone. */
    public synchronized boolean remove(long logicalId) {
        checkOpen();
        checkId(logicalId);
        int slot = find(logicalId);
        if (slot < 0) return false;
        removeSlot(slot, true);
        return true;
    }

    public synchronized int residentCount() {
        checkOpen();
        return size;
    }

    public synchronized int pinnedCount() {
        checkOpen();
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (M3LazyElementFlags.has(flags[i], M3LazyElementFlags.PINNED)) count++;
        }
        return count;
    }

    public synchronized Stats stats() {
        checkOpen();
        return new Stats(size, pinnedCount(), creations, disposals, hits, misses, trims);
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        // Disposer callbacks may remove or close reentrantly: re-read size after each one.
        while (size > 0) removeSlot(size - 1, true);
        // A nested close() from a Disposer already finished this close.
        if (closed) return;
        releaseReusable(0);
        closed = true;
    }

    private T borrowInternal(long logicalId, boolean allowed) {
        if (!allowed || freeCount == 0) return create(logicalId);
        // Overflow refusal occurs before consuming an idle lease.
        long nextReuseCount = Math.addExact(reuseCount, 1L);
        int n = --freeCount;
        @SuppressWarnings("unchecked") T facade = (T) freeFacades[n];
        long oldId = freeIds[n];
        freeFacades[n] = null;
        freeIds[n] = 0L;
        try {
            recycler.rebind(facade, logicalId);
            // Rebinding is user code; it may close the owner reentrantly.
            if (closed) throw new IllegalStateException("owner closed during recycler rebind");
        } catch (RuntimeException | Error failure) {
            try { dispose(oldId, facade); }
            catch (RuntimeException | Error cleanup) { suppressDistinct(failure, cleanup); }
            throw failure;
        }
        reuseCount = nextReuseCount;
        return facade;
    }

    private void releaseInternal(long logicalId, T facade, boolean allowed) {
        if (!allowed || closed || freeCount == maxIdle) {
            dispose(logicalId, facade);
            return;
        }
        try {
            ensureReusableCapacity(freeCount + 1);
            recycler.scrub(facade);
        } catch (RuntimeException | Error failure) {
            try { dispose(logicalId, facade); }
            catch (RuntimeException | Error cleanup) { suppressDistinct(failure, cleanup); }
            throw failure;
        }
        // Scrub is user code and can close or refill this owner reentrantly.
        // Never install an idle strong reference after the close boundary.
        if (closed || freeCount == maxIdle) {
            dispose(logicalId, facade);
            return;
        }
        freeFacades[freeCount] = facade;
        freeIds[freeCount++] = logicalId;
    }

    //FLLDCIM: MRL - idle lanes grew one at a time (Object[] published before long[]); stage both copies, validate, publish as a pair
    private void ensureReusableCapacity(int required) {
        ensureReusableCapacity(required, ARRAY_COPY);
    }

    /**
     * Stage both idle object and ID lanes before publishing either. A failed
     * allocation must not make a later return believe the pair already grew.
     * Reuse the same source-local fault-injection seam as resident growth.
     */
    void ensureReusableCapacity(int required, BackingCopier copies) {
        if (required <= freeFacades.length) return;
        Objects.requireNonNull(copies, "copies");
        int capacity = Math.min(maxIdle,
                Math.max(required, Math.max(1, freeFacades.length * 2)));
        Object[] nextFacades = Objects.requireNonNull(
                copies.objects(freeFacades, capacity), "idle facade copy");
        long[] nextIds = Objects.requireNonNull(
                copies.longs(freeIds, capacity), "idle ID copy");
        if (nextFacades.length != capacity || nextIds.length != capacity) {
            throw new IllegalStateException("incomplete M3 idle facade backing growth");
        }
        freeFacades = nextFacades;
        freeIds = nextIds;
    }

    /** A callback may rethrow the exact Throwable from cleanup; Java rejects self-suppression. */
    private static void suppressDistinct(Throwable original, Throwable cleanup) {
        if (original != cleanup) original.addSuppressed(cleanup);
    }

    private T create(long logicalId) {
        T facade = Objects.requireNonNull(factory.create(logicalId), "factory result");
        creations = Math.addExact(creations, 1);
        // Factory.create may close() the owner before returning a facade.
        // Dispose unadmitted instances once; do not expose or use them.
        if (closed) {
            IllegalStateException failure =
                    new IllegalStateException("owner closed during factory callback");
            try { dispose(logicalId, facade); }
            catch (RuntimeException | Error cleanup) { suppressDistinct(failure, cleanup); }
            throw failure;
        }
        return facade;
    }

    private void dispose(long logicalId, T facade) {
        disposer.dispose(logicalId, facade);
        disposals = Math.addExact(disposals, 1);
    }

    //FLLDCIM: MRL - a reentrant factory admitted the id first; the outer insert threw and leaked its facade; retire only an unadmitted instance
    /**
     * Admit a factory-produced facade only once. A factory can reenter this
     * synchronized owner and install the same ID before returning.
     */
    private int insertCreated(long logicalId, T facade) {
        try {
            return insert(logicalId, facade);
        } catch (RuntimeException | Error failure) {
            // Failure path only. Never dispose an instance admitted by a nested
            // callback, even when a shared factory returns the same instance.
            boolean alreadyResident = false;
            for (int i = 0; i < size; i++) {
                if (facades[i] == facade) {
                    alreadyResident = true;
                    break;
                }
            }
            if (!alreadyResident) {
                try {
                    dispose(logicalId, facade);
                } catch (RuntimeException | Error cleanup) {
                    suppressDistinct(failure, cleanup);
                }
            }
            throw failure;
        }
    }

    private int insert(long logicalId, T facade) {
        int found = find(logicalId);
        if (found >= 0) throw new IllegalStateException("duplicate facade id: " + logicalId);
        int slot = -found - 1;
        ensureCapacity(size + 1);
        int moved = size - slot;
        if (moved > 0) {
            System.arraycopy(ids, slot, ids, slot + 1, moved);
            System.arraycopy(facades, slot, facades, slot + 1, moved);
            System.arraycopy(flags, slot, flags, slot + 1, moved);
            System.arraycopy(accessEpochs, slot, accessEpochs, slot + 1, moved);
        }
        ids[slot] = logicalId;
        facades[slot] = facade;
        flags[slot] = 0;
        accessEpochs[slot] = 0;
        size++;
        return slot;
    }

    //FLLDCIM: MRL - reentrant Disposer in remove/close disposed a facade twice, then AIOOBE(-1); detach first
    private void removeSlot(int slot, boolean dispose) {
        long logicalId = ids[slot];
        T facade = value(slot);
        short flag = flags[slot];
        long access = accessEpochs[slot];
        // Detach before invoking user code, so a reentrant close()/remove() never sees this slot.
        detachSlot(slot);
        if (!dispose) return;
        try {
            dispose(logicalId, facade);
        } catch (RuntimeException | Error failure) {
            try { restoreSlot(logicalId, facade, flag, access); }
            catch (RuntimeException | Error cleanup) { suppressDistinct(failure, cleanup); }
            throw failure;
        }
    }

    /** A failed Disposer keeps the facade resident with its identity, flags and recency. */
    private boolean restoreSlot(long logicalId, T facade, short flag, long access) {
        // Never re-admit after the close boundary or over a facade the callback re-admitted.
        if (closed || find(logicalId) >= 0) return false;
        int slot = insert(logicalId, facade);
        flags[slot] = flag;
        accessEpochs[slot] = access;
        return true;
    }

    /** Detach every trim victim in one compacting pass before the first Disposer callback. */
    private Backing detachVictims(int[] victims) {
        int count = victims.length;
        Backing detached = new Backing(new long[count], new Object[count], new short[count], new long[count]);
        boolean[] removed = new boolean[size];
        for (int i = 0; i < count; i++) {
            int slot = victims[i];
            detached.ids()[i] = ids[slot];
            detached.facades()[i] = facades[slot];
            detached.flags()[i] = flags[slot];
            detached.accessEpochs()[i] = accessEpochs[slot];
            removed[slot] = true;
        }
        compactRetained(removed);
        return detached;
    }

    /**
     * After a failed trim disposal, the failed victim and the victims not yet offered become
     * resident again. A victim never offered that cannot return (closed owner, id re-admitted
     * by the callback) is disposed once instead.
     */
    private void returnVictims(Backing detached, int failed, Throwable failure) {
        for (int i = failed; i < detached.ids().length; i++) {
            long logicalId = detached.ids()[i];
            @SuppressWarnings("unchecked") T facade = (T) detached.facades()[i];
            try {
                if (!restoreSlot(logicalId, facade, detached.flags()[i], detached.accessEpochs()[i])
                        && i > failed) {
                    dispose(logicalId, facade);
                }
            } catch (RuntimeException | Error cleanup) {
                suppressDistinct(failure, cleanup);
            }
        }
    }

    private void detachSlot(int slot) {
        int moved = size - slot - 1;
        if (moved > 0) {
            System.arraycopy(ids, slot + 1, ids, slot, moved);
            System.arraycopy(facades, slot + 1, facades, slot, moved);
            System.arraycopy(flags, slot + 1, flags, slot, moved);
            System.arraycopy(accessEpochs, slot + 1, accessEpochs, slot, moved);
        }
        int last = --size;
        ids[last] = 0L;
        facades[last] = null;
        flags[last] = 0;
        accessEpochs[last] = 0L;
    }

    private void compactRetained(boolean[] removed) {
        int previousSize = size;
        int retained = 0;
        for (int i = 0; i < previousSize; i++) {
            if (removed[i]) continue;
            if (retained != i) {
                ids[retained] = ids[i];
                facades[retained] = facades[i];
                flags[retained] = flags[i];
                accessEpochs[retained] = accessEpochs[i];
            }
            retained++;
        }
        Arrays.fill(ids, retained, previousSize, 0L);
        Arrays.fill(facades, retained, previousSize, null);
        Arrays.fill(flags, retained, previousSize, (short) 0);
        Arrays.fill(accessEpochs, retained, previousSize, 0L);
        size = retained;
    }

    private int find(long logicalId) {
        return Arrays.binarySearch(ids, 0, size, logicalId);
    }

    private void touch(int slot) {
        accessEpochs[slot] = epoch = Math.addExact(epoch, 1);
    }

    /** Exact backing-copy seam for fault-injection proof; not an application SPI. */
    interface BackingCopier {
        long[] longs(long[] source, int capacity);
        short[] shorts(short[] source, int capacity);
        Object[] objects(Object[] source, int capacity);
    }

    private static final BackingCopier ARRAY_COPY = new BackingCopier() {
        @Override public long[] longs(long[] source, int capacity) {
            return Arrays.copyOf(source, capacity);
        }
        @Override public short[] shorts(short[] source, int capacity) {
            return Arrays.copyOf(source, capacity);
        }
        @Override public Object[] objects(Object[] source, int capacity) {
            return Arrays.copyOf(source, capacity);
        }
    };

    private record Backing(long[] ids, Object[] facades, short[] flags,
            long[] accessEpochs) { }

    private void ensureCapacity(int required) {
        ensureCapacity(required, ARRAY_COPY);
    }

    /**
     * On the allocation boundary, stage all four independent backing arrays
     * before publishing any of them. If an allocator throws or returns an
     * invalid result, the existing store remains readable and writable.
     */
    void ensureCapacity(int required, BackingCopier copies) {
        if (required <= ids.length) return;
        Objects.requireNonNull(copies, "copies");
        int preferred = Math.max(required, Math.max(4, ids.length + (ids.length >>> 1)));
        int next = Math.toIntExact(M3RuntimeHeap.chooseAmortizedCapacity(
                required, preferred, Long.BYTES * 3 + Short.BYTES + Long.BYTES));
        long[] nextIds = Objects.requireNonNull(copies.longs(ids, next), "ids copy");
        Object[] nextFacades = Objects.requireNonNull(copies.objects(facades, next), "facades copy");
        short[] nextFlags = Objects.requireNonNull(copies.shorts(flags, next), "flags copy");
        long[] nextAccess = Objects.requireNonNull(copies.longs(accessEpochs, next), "epochs copy");
        if (nextIds.length != next || nextFacades.length != next
                || nextFlags.length != next || nextAccess.length != next) {
            throw new IllegalStateException("incomplete M3 facade backing growth");
        }
        ids = nextIds;
        facades = nextFacades;
        flags = nextFlags;
        accessEpochs = nextAccess;
    }

    @SuppressWarnings("unchecked")
    private T value(int slot) {
        return (T) facades[slot];
    }

    private void checkOpen() {
        if (closed) throw new IllegalStateException("closed");
    }

    private static void checkId(long logicalId) {
        if (logicalId < 0) throw new IllegalArgumentException("negative logicalId");
    }

    private static void validateMode(int mode) {
        int ownership = mode & (RECREATE_UNEXPOSED | RETAIN_UNEXPOSED);
        if (ownership != RECREATE_UNEXPOSED && ownership != RETAIN_UNEXPOSED) {
            throw new IllegalArgumentException(
                    "exactly one unexposed-facade policy is required");
        }
        if ((mode & PIN_EXPOSED) == 0) {
            throw new IllegalArgumentException("PIN_EXPOSED is required for stable API identity");
        }
        int known = RECREATE_UNEXPOSED | RETAIN_UNEXPOSED | PIN_EXPOSED
                | PRESSURE_TRIMMABLE | AUTO_HEAP_PRESSURE;
        if ((mode & AUTO_HEAP_PRESSURE) != 0
                && (mode & PRESSURE_TRIMMABLE) == 0) {
            throw new IllegalArgumentException("automatic pressure requires trim support");
        }
        if ((mode & ~known) != 0) throw new IllegalArgumentException("unknown mode bits");
    }
}
