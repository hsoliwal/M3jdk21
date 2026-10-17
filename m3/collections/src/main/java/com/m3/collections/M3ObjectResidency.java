// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

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
public final class M3ObjectResidency<T> {
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
    private final int[] active;
    /** Explicit cooperative lease counts, not JVM heap reference counts. */
    private final int[] borrowers;
    /** A caller-installed projection can remain externally reachable. */
    private final boolean[] callerInstalled;
    /** Generation-safe handles; allocate the primitive handle lanes only on first use. */
    private M3SlotAllocator28 borrowHandles;
    private M3IntLane28 borrowOwner;
    private M3IntLane28 borrowCalls;
    private long residentBytes;
    private long tick;
    private Decision lastDecision;

    public M3ObjectResidency(int slots, int flags, long maxResidentBytes, long reserveBytes,
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

    /**
     * Copy a checked, contiguous window of canonical parent-owned state words.
     *
     * <p>This is one cooperative snapshot under the arena monitor, not a
     * projection/GC reference enumeration or a promise about future concurrent
     * writes. The output is caller-owned and can be used by a detached
     * primitive offset/mask selector without exposing any child object.</p>
     */
    public synchronized void copyParentBits(int sourceOffset, long[] target,
                                            int targetOffset, int length) {
        Objects.requireNonNull(target, "target");
        Objects.checkFromIndexSize(sourceOffset, length, parentBits.length);
        Objects.checkFromIndexSize(targetOffset, length, target.length);
        System.arraycopy(parentBits, sourceOffset, target, targetOffset, length);
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
        touched[slot] = ++tick;
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
            M3SlotAllocator28 ids = new M3SlotAllocator28();
            M3IntLane28 owners = new M3IntLane28();
            M3IntLane28 calls = new M3IntLane28();
            borrowOwner = owners;
            borrowCalls = calls;
            borrowHandles = ids;
        }
        long lease = borrowHandles.allocate();
        int index = M3SlotAllocator28.slot(lease);
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
            touched[slot] = ++tick;
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
        residentBytes = Math.addExact(residentBytes, estimatedBytes);
        setState(slot, State.RESIDENT);
        touched[slot] = ++tick;
        decision(slot, State.ABSENT, State.RESIDENT, "installed");
    }

    /** Explicit pressure signal; next access enforces the current JVM headroom. */
    public synchronized void signalMemoryPressure() {
        enforceBudget(-1, 0L);
    }

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
        admit(estimatedBytes, slot);
        try {
            T value;
            if (before == State.SWAPPED) {
                setState(slot, State.RESTORING);
                byte[] bytes = spill.read(slot);
                value = codec.decode(slot, bytes);
                Objects.requireNonNull(value, "decoded projection");
                spill.delete(slot);
            } else {
                if (factory == null) throw new IllegalStateException("no factory for absent slot");
                setState(slot, State.CREATING);
                value = Objects.requireNonNull(factory.create(slot), "created projection");
            }
            resident[slot] = value;
            weights[slot] = estimatedBytes;
            residentBytes = Math.addExact(residentBytes, estimatedBytes);
            setState(slot, State.RESIDENT);
            touched[slot] = ++tick;
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

    private void admit(long incomingBytes, int requestedSlot) {
        long target = targetBudget(HeapSnapshot.current());
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
    }

    private void enforceBudget(int requestedSlot, long incomingBytes) {
        long target = targetBudget(HeapSnapshot.current());
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

    private long targetBudget(HeapSnapshot heap) {
        long target = maxResidentBytes;
        if ((flags & PRESSURE_AWARE) != 0) {
            long available = Math.max(0L, heap.estimatedHeadroomBytes() - reserveBytes);
            target = Math.min(target, available);
        }
        return target;
    }

    private boolean wouldExceed(long incoming, long target) {
        return incoming > target || residentBytes > target - incoming;
    }

    private int leastRecentlyUsed(int excluded) {
        if ((flags & LRU) == 0) return -1;
        int victim = -1;
        long oldest = Long.MAX_VALUE;
        for (int i = 0; i < resident.length; i++) {
            if (i == excluded || resident[i] == null || active[i] != 0 || borrowers[i] != 0
                    || (Byte.toUnsignedInt(states[i]) & SLOT_PINNED) != 0) continue;
            if ((flags & (SWAP_SECONDARY | RECREATE)) == 0) continue;
            if (touched[i] < oldest) { oldest = touched[i]; victim = i; }
        }
        return victim;
    }

    private void evict(int slot) {
        State before = stateAt(slot);
        if (before != State.RESIDENT || active[slot] != 0 || borrowers[slot] != 0) return;
        Object value = resident[slot];
        if ((flags & SWAP_SECONDARY) != 0) {
            setState(slot, State.SWAPPING);
            try {
                @SuppressWarnings("unchecked") T typed = (T) value;
                byte[] snapshot = Objects.requireNonNull(codec.encode(typed), "encoded snapshot");
                spill.write(slot, snapshot);
                resident[slot] = null;
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
