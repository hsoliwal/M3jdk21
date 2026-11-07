// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.LongUnaryOperator;

/**
 * Parent-owned flat residency arena for disposable child projections.
 *
 * <p>One arena owns the child-state lanes; a child is addressed by an integer slot, not by a
 * per-child bookkeeping object. Callers access projections through use(), allowing LRU and
 * pressure admission to run at a bounded operation boundary. This is an opt-in contract, not
 * transparent eviction of arbitrary escaped Java references.
 *
 * <p>The arena is thread-safe. A use callback executes while the arena monitor is held. Recursive
 * use is supported, but the active slot is pinned against eviction. Keep callbacks bounded and
 * never retain the supplied projection when STRICT_NO_ESCAPE is enabled.
 */
public final class ObjectResidency<T> {
    public static final int LAZY_CREATE = 1;
    public static final int LRU = 1 << 1;
    public static final int PRESSURE_AWARE = 1 << 2;
    public static final int SWAP_SECONDARY = 1 << 3;
    public static final int RECREATE = 1 << 4;
    public static final int OBSERVE_ONLY = 1 << 5;
    public static final int STRICT_NO_ESCAPE = 1 << 6;
    /** Pin a projection permanently once a raw reference is returned from use() or installed. */
    public static final int PIN_EXPOSED = 1 << 7;
    public static final int ALL_FLAGS = (1 << 8) - 1;
    private static final int SLOT_PINNED = 1 << 6;
    private static final int STATE_MASK = 0x07;

    public enum State { ABSENT, CREATING, RESIDENT, SWAPPING, SWAPPED, RESTORING }

    @FunctionalInterface public interface Factory<T> { T create(int slot) throws Exception; }
    /** Rebinds an exclusively owned projection from its canonical parent state. */
    @FunctionalInterface public interface Reinitializer<T> {
        void reinitialize(T projection, int destinationSlot) throws Exception;
    }
    public interface Codec<T> {
        byte[] encode(T value) throws IOException;
        T decode(int slot, byte[] bytes) throws IOException;
    }
    public interface SpillStore {
        void write(int slot, byte[] bytes) throws IOException;
        byte[] read(int slot) throws IOException;
        void delete(int slot) throws IOException;
    }
    public record HeapSnapshot(long maxBytes, long committedBytes, long usedBytes,
            long estimatedHeadroomBytes) {
        public static HeapSnapshot current() {
            Runtime r = Runtime.getRuntime();
            long committed = r.totalMemory();
            long used = Math.max(0L, committed - r.freeMemory());
            return new HeapSnapshot(r.maxMemory(), committed, used, Math.max(0L, r.maxMemory() - used));
        }
    }
    public record Decision(int slot, State before, State after, long residentBytes,
            long targetBytes, String reason) { }

    private final int flags;
    private final long maxResidentBytes;
    private final long reserveBytes;
    private final Factory<? extends T> factory;
    private final Codec<T> codec;
    private final SpillStore spill;
    private final Object[] resident;
    /** Canonical caller-visible mutable bit/word state, independent of projection residency. */
    private final long[] parentBits;
    private final byte[] states;
    private final long[] weights;
    private final long[] touched;
    /** Optional primitive topology over the existing resident slots; zero encodes no slot. */
    private final int[] lruPreviousEncoded;
    private final int[] lruNextEncoded;
    private int lruFirstEncoded;
    private int lruLastEncoded;
    private long lruLastTick;
    /** Preserve the original signed scan after rollover, ties, or incomplete publication. */
    private boolean lruScanRequired;
    private final int[] active;
    /** Explicit cooperative lease counts, not JVM heap reference counts. */
    private final int[] borrowers;
    /** A caller-installed projection can remain externally reachable. */
    private final boolean[] callerInstalled;
    /** Generation-safe handles; allocate the primitive handle lanes only on first use. */
    private SlotAllocator28 borrowHandles;
    private M3IntLane28 borrowOwner;
    private M3IntLane28 borrowCalls;
    private long residentBytes;
    private long tick;
    private Decision lastDecision;

    public ObjectResidency(int slots, int flags, long maxResidentBytes, long reserveBytes,
            Factory<? extends T> factory, Codec<T> codec, SpillStore spill) {
        if (slots < 0) throw new IllegalArgumentException("slots must be non-negative");
        if ((flags & ~ALL_FLAGS) != 0) throw new IllegalArgumentException("unknown residency flags");
        if (maxResidentBytes < 0 || reserveBytes < 0) throw new IllegalArgumentException("negative memory budget");
        if ((flags & SWAP_SECONDARY) != 0 && (codec == null || spill == null)) {
            throw new IllegalArgumentException("SWAP_SECONDARY requires codec and spill store");
        }
        if ((flags & RECREATE) != 0 && factory == null) {
            throw new IllegalArgumentException("RECREATE requires a factory backed by canonical parent state");
        }
        this.flags = flags;
        this.maxResidentBytes = maxResidentBytes;
        this.reserveBytes = reserveBytes;
        this.factory = factory;
        this.codec = codec;
        this.spill = spill;
        this.resident = new Object[slots];
        this.parentBits = new long[slots];
        this.states = new byte[slots];
        this.weights = new long[slots];
        this.touched = new long[slots];
        this.active = new int[slots];
        this.borrowers = new int[slots];
        this.callerInstalled = new boolean[slots];
        boolean trackLru = (flags & LRU) != 0 && (flags & (SWAP_SECONDARY | RECREATE)) != 0
                && (flags & OBSERVE_ONLY) == 0;
        this.lruPreviousEncoded = trackLru ? new int[slots] : null;
        this.lruNextEncoded = trackLru ? new int[slots] : null;
        Arrays.fill(states, (byte) State.ABSENT.ordinal());
    }

    public int slots() { return resident.length; }

    /**
     * Read the canonical 64-bit parent-owned state word for a logical child slot.
     * This word survives projection release and is not part of the projection's residency weight.
     */
    public synchronized long readParentBits(int slot) {
        checkSlot(slot);
        return parentBits[slot];
    }

    /** Replace the canonical parent-owned state word. Mutations must target this lane, not a disposable projection. */
    public synchronized void writeParentBits(int slot, long value) {
        checkSlot(slot);
        parentBits[slot] = value;
    }

    /** Atomically transform the canonical parent-owned state word under the arena monitor. */
    public synchronized long updateParentBits(int slot, LongUnaryOperator update) {
        checkSlot(slot);
        Objects.requireNonNull(update, "update");
        long next = update.applyAsLong(parentBits[slot]);
        parentBits[slot] = next;
        return next;
    }

    public synchronized long residentBytes() { return residentBytes; }
    public synchronized State state(int slot) { checkSlot(slot); return stateAt(slot); }
    public synchronized Decision lastDecision() { return lastDecision; }

    /**
     * Run a bounded operation against a lazily materialized projection. The result must not expose
     * the projection itself when STRICT_NO_ESCAPE is enabled.
     */
    public synchronized <R> R use(int slot, long estimatedBytes, Function<? super T, ? extends R> operation) {
        checkSlot(slot);
        Objects.requireNonNull(operation, "operation");
        if (estimatedBytes < 0) throw new IllegalArgumentException("estimatedBytes must be non-negative");
        ensureResident(slot, estimatedBytes);
        @SuppressWarnings("unchecked") T value = (T) resident[slot];
        active[slot]++;
        touch(slot);
        try {
            R result = operation.apply(value);
            if ((flags & STRICT_NO_ESCAPE) != 0 && result == value) {
                throw new IllegalStateException("projection escaped STRICT_NO_ESCAPE scope");
            }
            if ((flags & PIN_EXPOSED) != 0 && result == value) {
                states[slot] = (byte) (states[slot] | SLOT_PINNED);
            }
            return result;
        } finally {
            active[slot]--;
        }
    }

    /**
     * Acquire an explicit, generation-checked cooperative borrow for a logical child.
     * A long handle is sufficient for an asynchronous event; no Lease object is retained.
     * The caller must release it in a finally block after the last callback. This does
     * not count heap references, JNI globals, Thread locals or hidden escape paths.
     */
    public synchronized long retainProjection(int slot, long estimatedBytes) {
        checkSlot(slot);
        if (estimatedBytes < 0) throw new IllegalArgumentException("negative estimatedBytes");
        if (borrowers[slot] == Integer.MAX_VALUE) {
            throw new IllegalStateException("projection borrow count overflow");
        }
        ensureResident(slot, estimatedBytes);
        if (borrowHandles == null) {
            SlotAllocator28 ids = new SlotAllocator28();
            M3IntLane28 owners = new M3IntLane28();
            M3IntLane28 calls = new M3IntLane28();
            borrowOwner = owners;
            borrowCalls = calls;
            borrowHandles = ids;
        }
        long lease = borrowHandles.allocate();
        int index = SlotAllocator28.slot(lease);
        try {
            borrowOwner.set(index, Math.addExact(slot, 1));
        } catch (RuntimeException | Error failure) {
            borrowHandles.release(lease);
            throw failure;
        }
        borrowers[slot]++;
        return lease;
    }

    /** The number of explicitly retained projection borrows, not object reference count. */
    public synchronized int projectionBorrowCount(int slot) {
        checkSlot(slot);
        return borrowers[slot];
    }

    /** Borrow handles remain unique when an allocator slot is reused (ABA-safe generation). */
    public synchronized void releaseProjection(long lease) {
        if (borrowHandles == null) throw new IllegalStateException("no projection borrows");
        int index = borrowHandles.requireLiveSlot(lease);
        int encodedSlot = borrowOwner.get(index);
        if (encodedSlot <= 0 || encodedSlot > resident.length) {
            throw new IllegalStateException("invalid projection borrower");
        }
        if (borrowCalls.get(index) != 0) {
            throw new IllegalStateException("borrow has a running callback");
        }
        borrowHandles.release(lease); // Fails closed on generation exhaustion.
        borrowOwner.clear(index);
        borrowers[encodedSlot - 1]--;
    }

    /**
     * Resolve a retained borrow back through the owning parent. A callback is executed
     * under the existing arena monitor and must obey the same non-escape contract as use().
     * Callback exceptions release only the active-use pin, not the borrow handle itself.
     */
    public synchronized <R> R useProjection(long lease,
            Function<? super T, ? extends R> operation) {
        Objects.requireNonNull(operation, "operation");
        if (borrowHandles == null) throw new IllegalStateException("no projection borrows");
        int index = borrowHandles.requireLiveSlot(lease);
        int encodedSlot = borrowOwner.get(index);
        if (encodedSlot <= 0 || encodedSlot > resident.length) {
            throw new IllegalStateException("invalid projection borrower");
        }
        int slot = encodedSlot - 1;
        if (stateAt(slot) != State.RESIDENT) {
            throw new IllegalStateException("borrowed projection is not resident");
        }
        int calls = borrowCalls.get(index);
        if (calls == Integer.MAX_VALUE || active[slot] == Integer.MAX_VALUE) {
            throw new IllegalStateException("borrow callback nesting overflow");
        }
        borrowCalls.set(index, calls + 1);
        active[slot]++;
        try {
            @SuppressWarnings("unchecked") T value = (T) resident[slot];
            R result = operation.apply(value);
            if ((flags & STRICT_NO_ESCAPE) != 0 && result == value) {
                throw new IllegalStateException("projection escaped STRICT_NO_ESCAPE scope");
            }
            if ((flags & PIN_EXPOSED) != 0 && result == value) {
                states[slot] = (byte) (states[slot] | SLOT_PINNED);
            }
            touch(slot);
            return result;
        } finally {
            active[slot]--;
            borrowCalls.set(index, calls);
        }
    }

    /**
     * Drop the arena's strong reference promptly ONLY for an opt-in, reconstructible,
     * caller-nonescaping projection with no active or cooperative borrowers.
     * The GC—not this method—decides when the Java object itself can be reclaimed.
     * STRICT_NO_ESCAPE is a caller/compile-time proof obligation, not a VM escape oracle.
     */
    public synchronized boolean retireUnborrowedProjection(int slot) {
        checkSlot(slot);
        if ((flags & (RECREATE | STRICT_NO_ESCAPE)) != (RECREATE | STRICT_NO_ESCAPE)
                || (flags & SWAP_SECONDARY) != 0 || callerInstalled[slot]
                || (states[slot] & SLOT_PINNED) != 0
                || active[slot] != 0 || borrowers[slot] != 0
                || stateAt(slot) != State.RESIDENT) {
            return false;
        }
        evict(slot);
        return stateAt(slot) == State.ABSENT;
    }

    /**
     * Transfer an otherwise disposable projection directly into another logical slot.
     *
     * <p>This avoids a GC-bound allocation/recreation cycle. It is only legal for a
     * compiler-verified, nonescaping, parent-reconstructible, exclusively owned projection.
     * STRICT_NO_ESCAPE alone cannot prove the absence of indirect or JNI aliases.
     * No reclaimed/unreachable Java object is resurrected and no Java identity is reused
     * while a caller can still observe it. The resetter must fully reconstruct the new
     * projection from the target slot's canonical parent state.
     *
     * <p>Requires LAZY_CREATE (the source becomes ABSENT and must be recreatable from its parent
     * state); refused, without side effects, under OBSERVE_ONLY and for a pinned, active or
     * borrowed destination. While the resetter runs, both slots are CREATING and the source stays
     * resident, counted and pinned; the budget is checked again after the resetter, so a
     * reentrant resetter cannot spend the destination budget twice.
     *
     * <p>On reset failure, both slots become absent. The possibly mutated instance is
     * dropped for ordinary GC rather than returned to the original owner. Runtime exceptions
     * and errors of the resetter propagate unchanged; checked exceptions are wrapped in
     * ResidencyException.
     *
     * @return true if a resident instance was moved without a new allocation
     */
    //FLLDCIM: MRL - residency donor semantics P2 groups C+D, crate m3-doop-reuse-20261030 (rebindUnborrowedProjection of m3-doop-publication-budget-20261030, donor blob 8966285b99e2): the donor rebind semantics under the owner name
    public synchronized boolean reuseUnborrowedProjection(int source, int target,
            long estimatedBytes, Reinitializer<? super T> reset) {
        checkSlot(source);
        checkSlot(target);
        Objects.requireNonNull(reset, "reset");
        if (estimatedBytes < 0) {
            throw new IllegalArgumentException("estimatedBytes must be non-negative");
        }
        if (source == target
                || (flags & (LAZY_CREATE | RECREATE | STRICT_NO_ESCAPE))
                        != (LAZY_CREATE | RECREATE | STRICT_NO_ESCAPE)
                || (flags & (SWAP_SECONDARY | OBSERVE_ONLY)) != 0
                || stateAt(source) != State.RESIDENT || stateAt(target) != State.ABSENT
                || callerInstalled[source] || callerInstalled[target]
                || (states[source] & SLOT_PINNED) != 0 || (states[target] & SLOT_PINNED) != 0
                || active[source] != 0 || borrowers[source] != 0
                || active[target] != 0 || borrowers[target] != 0) {
            return false;
        }
        long oldBytes = weights[source];
        long others = residentBytes - oldBytes;
        long budget = targetBudget();
        if (estimatedBytes > budget || others > budget - estimatedBytes) return false;

        @SuppressWarnings("unchecked") T projection = (T) resident[source];
        active[source] = 1;
        setState(source, State.CREATING);
        setState(target, State.CREATING);
        try {
            reset.reinitialize(projection, target);
            // The callback may reenter unrelated slots or change memory pressure.
            others = residentBytes - oldBytes;
            budget = targetBudget();
            if (estimatedBytes > budget || others > budget - estimatedBytes) {
                throw new BudgetExceededException(estimatedBytes, others, budget);
            }
        } catch (Exception failure) {
            discardFailedRebind(source, target);
            if (failure instanceof RuntimeException runtime) throw runtime;
            throw new ResidencyException("projection reset failed for target slot " + target, failure);
        } catch (Error failure) {
            discardFailedRebind(source, target);
            throw failure;
        }

        active[source] = 0;
        resident[source] = null;
        unlinkLru(source);
        residentBytes -= oldBytes;
        weights[source] = 0L;
        setState(source, State.ABSENT);
        resident[target] = projection;
        weights[target] = estimatedBytes;
        residentBytes += estimatedBytes;
        setState(target, State.RESIDENT);
        touch(target);
        decision(target, State.ABSENT, State.RESIDENT, "reused unborrowed projection");
        return true;
    }

    /** Reset may have partially mutated its input: discard rather than expose it. */
    private void discardFailedRebind(int source, int target) {
        active[source] = 0;
        resident[source] = null;
        unlinkLru(source);
        residentBytes -= weights[source];
        weights[source] = 0L;
        setState(source, State.ABSENT);
        setState(target, State.ABSENT);
        decision(source, State.RESIDENT, State.ABSENT, "failed reset, discard tainted shell");
    }

    /** Pin a pre-existing value supplied by the parent; no lazy factory is invoked. */
    public synchronized void install(int slot, T value, long estimatedBytes) {
        checkSlot(slot);
        Objects.requireNonNull(value, "value");
        if (estimatedBytes < 0) throw new IllegalArgumentException("estimatedBytes must be non-negative");
        if (stateAt(slot) != State.ABSENT) throw new IllegalStateException("slot is not absent");
        admit(estimatedBytes, slot);
        resident[slot] = value;
        callerInstalled[slot] = true;
        if ((flags & PIN_EXPOSED) != 0) states[slot] = (byte) (states[slot] | SLOT_PINNED);
        weights[slot] = estimatedBytes;
        if (residentBytes > Long.MAX_VALUE - estimatedBytes) lruScanRequired = true;
        residentBytes = Math.addExact(residentBytes, estimatedBytes);
        setState(slot, State.RESIDENT);
        touch(slot);
        decision(slot, State.ABSENT, State.RESIDENT, "installed");
    }

    /** Explicit pressure signal; next access enforces the current JVM headroom. */
    public synchronized void signalMemoryPressure() {
        enforceBudget(-1, 0L);
    }

    //FLLDCIM: MRL - census L066, crate m3-doop-reuse-20261030 (m3-doop-publication-budget-20261030, donor blob 8966285b99e2): re-check the admitted budget after the callbacks; delete the spill copy after it
    //FLLDCIM: MRL - residency donor semantics P2 group A, crate m3-doop-reuse-20261030 (m3-doop-callback-error-20261030, donor blob 45caaf707b47): an Error from a callback restores the prior state
    private void ensureResident(int slot, long estimatedBytes) {
        State before = stateAt(slot);
        if (before == State.RESIDENT) {
            if ((flags & PRESSURE_AWARE) != 0) enforceBudget(slot, 0L);
            return;
        }
        if (before == State.CREATING || before == State.RESTORING || before == State.SWAPPING) {
            throw new IllegalStateException("reentrant residency transition for slot " + slot);
        }
        if ((flags & LAZY_CREATE) == 0 && before == State.ABSENT) {
            throw new IllegalStateException("lazy creation is disabled for absent slot " + slot);
        }
        long target = admit(estimatedBytes, slot);
        try {
            T value;
            try {
                if (before == State.SWAPPED) {
                    setState(slot, State.RESTORING);
                    byte[] bytes = spill.read(slot);
                    value = codec.decode(slot, bytes);
                    Objects.requireNonNull(value, "decoded projection");
                } else {
                    if (factory == null) throw new IllegalStateException("no factory for absent slot");
                    setState(slot, State.CREATING);
                    value = Objects.requireNonNull(factory.create(slot), "created projection");
                }
                // Factory/read/decode callbacks may consume the originally admitted capacity.
                if ((flags & OBSERVE_ONLY) == 0 && wouldExceed(estimatedBytes, target)) {
                    throw new BudgetExceededException(estimatedBytes, residentBytes, target);
                }
                if (before == State.SWAPPED) spill.delete(slot);
            } catch (Error failure) {
                // Callbacks have not published a resident value or changed its accounting.
                setState(slot, before);
                throw failure;
            }
            resident[slot] = value;
            weights[slot] = estimatedBytes;
            if (residentBytes > Long.MAX_VALUE - estimatedBytes) lruScanRequired = true;
            residentBytes = Math.addExact(residentBytes, estimatedBytes);
            setState(slot, State.RESIDENT);
            touch(slot);
            decision(slot, before, State.RESIDENT, before == State.SWAPPED ? "restored" : "created");
        } catch (IOException failure) {
            setState(slot, before);
            throw new ResidencyException("projection I/O failed for slot " + slot, failure);
        } catch (RuntimeException failure) {
            setState(slot, before);
            throw failure;
        } catch (Exception failure) {
            setState(slot, before);
            throw new ResidencyException("projection creation failed for slot " + slot, failure);
        }
    }

    //FLLDCIM: MRL - census L066, crate m3-doop-reuse-20261030 (m3-doop-publication-budget-20261030): admission returns the target it admitted against
    //FLLDCIM: MRL - residency victim progress P1, crate m3-doop-reuse-20261030 (m3-doop-reentrant-eviction-20261030, donor blob 0f7bf56e5aa8): claim the requested slot before the eviction callbacks
    private long admit(long incomingBytes, int requestedSlot) {
        // Eviction invokes caller codecs/stores. Claim this request before their callbacks.
        State before = stateAt(requestedSlot);
        setState(requestedSlot, before == State.SWAPPED ? State.RESTORING : State.CREATING);
        try {
            long target = targetBudget();
            while (wouldExceed(incomingBytes, target)) {
                if ((flags & OBSERVE_ONLY) != 0) break;
                int victim = leastRecentlyUsed(requestedSlot);
                if (victim < 0) {
                    throw new BudgetExceededException(incomingBytes, residentBytes, target);
                }
                evict(victim);
            }
            if (wouldExceed(incomingBytes, target) && (flags & OBSERVE_ONLY) == 0) {
                throw new BudgetExceededException(incomingBytes, residentBytes, target);
            }
            return target;
        } finally {
            // Publication remains with the caller; admission restores even on Error.
            setState(requestedSlot, before);
        }
    }

    private void enforceBudget(int requestedSlot, long incomingBytes) {
        long target = targetBudget();
        while (wouldExceed(incomingBytes, target)) {
            if ((flags & OBSERVE_ONLY) != 0) {
                decision(requestedSlot, stateAtOrAbsent(requestedSlot), stateAtOrAbsent(requestedSlot),
                        "observe-only: budget would be exceeded");
                return;
            }
            int victim = leastRecentlyUsed(requestedSlot);
            if (victim < 0) {
                if (incomingBytes > 0) throw new BudgetExceededException(incomingBytes, residentBytes, target);
                decision(requestedSlot, stateAtOrAbsent(requestedSlot), stateAtOrAbsent(requestedSlot),
                        "no eligible victim under pressure");
                return;
            }
            evict(victim);
        }
    }

    //FLLDCIM: MRL - residency fold leaf PS, crate m3-doop-reuse-20261030 (m3-doop-pressure-sampling-20261030, M3JDK 4e229782bf1 blob e8347cd7780d): no heap snapshot without PRESSURE_AWARE
    //FLLDCIM: HPCPOP - residency-pressure-alloc-20261107: the PRESSURE_AWARE budget reads the heap into primitives; no HeapSnapshot per boundary (resident use() hit path 0 B/call under C2, -Xbatch and -Xint)
    private long targetBudget() {
        long target = maxResidentBytes;
        if ((flags & PRESSURE_AWARE) != 0) {
            long available = Math.max(0L, sampledHeadroomBytes() - reserveBytes);
            target = Math.min(target, available);
        }
        return target;
    }

    //FLLDCIM: MRL - residency-pressure-alloc-20261107: HeapSnapshot.current().estimatedHeadroomBytes() without the record (same Runtime reads, same order, same clamps)
    /** The estimated headroom of {@link HeapSnapshot#current()}, sampled into primitives at each call. */
    private static long sampledHeadroomBytes() {
        Runtime runtime = Runtime.getRuntime();
        long committed = runtime.totalMemory();
        long used = Math.max(0L, committed - runtime.freeMemory());
        return Math.max(0L, runtime.maxMemory() - used);
    }

    private boolean wouldExceed(long incoming, long target) {
        return incoming > target || residentBytes > target - incoming;
    }

    private void touch(int slot) {
        touched[slot] = ++tick;
        if (lruNextEncoded == null || lruScanRequired) return;
        if (tick <= lruLastTick || tick == Long.MAX_VALUE) {
            // The old scan compares signed longs, breaks ties by slot, and skips MAX_VALUE.
            lruScanRequired = true;
            return;
        }
        lruLastTick = tick;
        int encoded = slot + 1;
        if (lruLastEncoded == encoded) return;
        unlinkLru(slot);
        lruPreviousEncoded[slot] = lruLastEncoded;
        if (lruLastEncoded == 0) lruFirstEncoded = encoded;
        else lruNextEncoded[lruLastEncoded - 1] = encoded;
        lruLastEncoded = encoded;
    }

    private void unlinkLru(int slot) {
        if (lruNextEncoded == null || lruScanRequired) return;
        int previous = lruPreviousEncoded[slot];
        int next = lruNextEncoded[slot];
        if (previous != 0) lruNextEncoded[previous - 1] = next;
        else if (lruFirstEncoded == slot + 1) lruFirstEncoded = next;
        else return; // A newly published resident has not been linked by touch() yet.
        if (next != 0) lruPreviousEncoded[next - 1] = previous;
        else lruLastEncoded = previous;
        lruPreviousEncoded[slot] = 0;
        lruNextEncoded[slot] = 0;
    }

    //FLLDCIM: MRL - residency victim progress P1, crate m3-doop-reuse-20261030 (m3-doop-reentrant-eviction-20261030, donor blob 0f7bf56e5aa8): a victim must be RESIDENT (a SWAPPING victim made admit() spin)
    private int leastRecentlyUsed(int excluded) {
        if (lruNextEncoded == null || lruScanRequired) return scanLeastRecentlyUsed(excluded);
        for (int encoded = lruFirstEncoded; encoded != 0; encoded = lruNextEncoded[encoded - 1]) {
            int slot = encoded - 1;
            if (slot != excluded && resident[slot] != null && active[slot] == 0 && borrowers[slot] == 0
                    && (Byte.toUnsignedInt(states[slot]) & SLOT_PINNED) == 0
                    && stateAt(slot) == State.RESIDENT) return slot;
        }
        return -1;
    }

    //FLLDCIM: MRL - residency victim progress P1, crate m3-doop-reuse-20261030 (m3-doop-reentrant-eviction-20261030, donor blob 0f7bf56e5aa8): the rollover scan skips non-RESIDENT victims too
    private int scanLeastRecentlyUsed(int excluded) {
        if ((flags & LRU) == 0) return -1;
        int victim = -1;
        long oldest = Long.MAX_VALUE;
        for (int i = 0; i < resident.length; i++) {
            if (i == excluded || resident[i] == null || active[i] != 0 || borrowers[i] != 0
                    || (Byte.toUnsignedInt(states[i]) & SLOT_PINNED) != 0
                    || stateAt(i) != State.RESIDENT) continue;
            if ((flags & (SWAP_SECONDARY | RECREATE)) == 0) continue;
            if (touched[i] < oldest) { oldest = touched[i]; victim = i; }
        }
        return victim;
    }

    //FLLDCIM: MRL - residency donor semantics P2 group A, crate m3-doop-reuse-20261030 (m3-doop-callback-error-20261030, donor blob 45caaf707b47): an Error from encode/write retains the resident
    private void evict(int slot) {
        State before = stateAt(slot);
        if (before != State.RESIDENT || active[slot] != 0 || borrowers[slot] != 0) return;
        Object value = resident[slot];
        if ((flags & SWAP_SECONDARY) != 0) {
            setState(slot, State.SWAPPING);
            try {
                @SuppressWarnings("unchecked") T typed = (T) value;
                try {
                    byte[] snapshot = Objects.requireNonNull(codec.encode(typed), "encoded snapshot");
                    spill.write(slot, snapshot);
                } catch (Error failure) {
                    // Encode/write failed before detachment: retain the original resident.
                    setState(slot, before);
                    throw failure;
                }
                resident[slot] = null;
                unlinkLru(slot);
                residentBytes -= weights[slot];
                weights[slot] = 0L;
                setState(slot, State.SWAPPED);
                decision(slot, before, State.SWAPPED, "swapped to secondary store");
                return;
            } catch (IOException | RuntimeException failure) {
                setState(slot, before);
                throw new ResidencyException("swap failed; resident value retained for slot " + slot, failure);
            }
        }
        if ((flags & RECREATE) != 0) {
            resident[slot] = null;
            unlinkLru(slot);
            residentBytes -= weights[slot];
            weights[slot] = 0L;
            setState(slot, State.ABSENT);
            decision(slot, before, State.ABSENT, "projection released; recreate from canonical parent state");
        }
    }

    private State stateAtOrAbsent(int slot) { return slot < 0 ? State.ABSENT : stateAt(slot); }
    private State stateAt(int slot) {
        return switch (Byte.toUnsignedInt(states[slot]) & STATE_MASK) {
            case 0 -> State.ABSENT;
            case 1 -> State.CREATING;
            case 2 -> State.RESIDENT;
            case 3 -> State.SWAPPING;
            case 4 -> State.SWAPPED;
            case 5 -> State.RESTORING;
            default -> throw new IllegalStateException("invalid residency state lane");
        };
    }
    private void setState(int slot, State state) { states[slot] = (byte) ((states[slot] & SLOT_PINNED) | state.ordinal()); }
    private void decision(int slot, State before, State after, String reason) {
        lastDecision = new Decision(slot, before, after, residentBytes, maxResidentBytes, reason);
    }
    private void checkSlot(int slot) {
        if (slot < 0 || slot >= resident.length) throw new IndexOutOfBoundsException("slot " + slot);
    }

    public static final class ResidencyException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public ResidencyException(String message, Throwable cause) { super(message, cause); }
    }
    public static final class BudgetExceededException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public final long requestedBytes;
        public final long residentBytes;
        public final long targetBytes;
        public BudgetExceededException(long requestedBytes, long residentBytes, long targetBytes) {
            super("residency budget exceeded: requested=" + requestedBytes + ", resident="
                    + residentBytes + ", target=" + targetBytes);
            this.requestedBytes = requestedBytes;
            this.residentBytes = residentBytes;
            this.targetBytes = targetBytes;
        }
    }
}

