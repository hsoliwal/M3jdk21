// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

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
    public static final int ALL_FLAGS = (1 << 7) - 1;

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
    private final byte[] states;
    private final long[] weights;
    private final long[] touched;
    private final int[] active;
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
        this.states = new byte[slots];
        this.weights = new long[slots];
        this.touched = new long[slots];
        this.active = new int[slots];
        Arrays.fill(states, (byte) State.ABSENT.ordinal());
    }

    public int slots() { return resident.length; }
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
            return result;
        } finally {
            active[slot]--;
        }
    }

    /** Pin a pre-existing value supplied by the parent; no lazy factory is invoked. */
    public synchronized void install(int slot, T value, long estimatedBytes) {
        checkSlot(slot);
        Objects.requireNonNull(value, "value");
        if (estimatedBytes < 0) throw new IllegalArgumentException("estimatedBytes must be non-negative");
        if (stateAt(slot) != State.ABSENT) throw new IllegalStateException("slot is not absent");
        admit(estimatedBytes, slot);
        resident[slot] = value;
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
            if (i == excluded || resident[i] == null || active[i] != 0) continue;
            if ((flags & (SWAP_SECONDARY | RECREATE)) == 0) continue;
            if (touched[i] < oldest) { oldest = touched[i]; victim = i; }
        }
        return victim;
    }

    private void evict(int slot) {
        State before = stateAt(slot);
        if (before != State.RESIDENT || active[slot] != 0) return;
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
        return switch (Byte.toUnsignedInt(states[slot])) {
            case 0 -> State.ABSENT;
            case 1 -> State.CREATING;
            case 2 -> State.RESIDENT;
            case 3 -> State.SWAPPING;
            case 4 -> State.SWAPPED;
            case 5 -> State.RESTORING;
            default -> throw new IllegalStateException("invalid residency state lane");
        };
    }
    private void setState(int slot, State state) { states[slot] = (byte) state.ordinal(); }
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
