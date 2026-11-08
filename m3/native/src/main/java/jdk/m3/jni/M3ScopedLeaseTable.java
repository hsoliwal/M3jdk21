// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import java.util.Arrays;
import java.util.Objects;
import jdk.m3.collections.RuntimeHeap;

/**
 * Explicit leases for monotonically assigned M3 logical handles, NOT Java object reference counts.
 * Managed JNI array access is accounted using the existing pin lanes plus one primitive
 * per-ID borrow lane. A pool-held strong slot contributes exactly +1 in the same
 * primitive pin lane. No per-reference Entry, Reference, or Node is allocated.
 * All operations
 * serialize through this table's monitor; scans are intended for occasional residency checkpoints.
 *
 * <p>Zero counts establish only that this table has no outstanding *registered* leases/pins.
 * The owner must independently prove escape/alias/identity/dirty-state/JNI safety before reclaiming
 * any underlying resource. In particular, this cannot authorize disposing reachable Java facades.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-primitives/src/main/java/com/synexia/primitives/M3ScopedLeaseTable.java blob 6e845b2af4a4; recipe m3/native/recipe/scoped-leases-20261106/relocate.py v1
public final class M3ScopedLeaseTable implements AutoCloseable {
    static final byte CLEAN = 0;
    static final byte DIRTY = 1;
    static final byte RETIRED = 2;

    private long[] ids = new long[8];
    private int[] leases = new int[8];
    private int[] pins = new int[8];
    private int[] jniArrayBorrows = new int[8];
    private byte[] jniArrayKinds = new byte[8];
    /** 0 = no pool claim, 1 = idle pool slot, 2 = exclusively checked-out slot. */
    // This reuses the same byte lane; no per-entry borrow or Reference objects.
    /** Pool ownership or exclusive checkout; +1 uses pins[] when idle. */
    private byte[] poolOwners = new byte[8];
    private byte[] states = new byte[8];
    private int size;
    private int registeredJniArrays;
    private int activeJniArrays;
    private long jniLeaseBegins;
    private boolean closed;

    /** Returns a fresh logical ID; IDs never recycle within this owner. */
    public synchronized long allocate() {
        checkOpen();
        int next = Math.addExact(size, 1);
        ensureCapacity(next);
        ids[size] = next;
        states[size] = CLEAN;
        leases[size] = 0;
        pins[size] = 0;
        jniArrayBorrows[size] = 0;
        jniArrayKinds[size] = 0;
        poolOwners[size] = 0;
        size = next;
        return next;
    }

    /** Acquire an explicitly managed lease; never use this to count JVM references. */
    public synchronized void acquire(long id) {
        int slot = slot(id);
        leases[slot] = Math.addExact(leases[slot], 1);
    }

    public synchronized void release(long id) {
        int slot = slot(id);
        if (leases[slot] == 0 || (poolOwners[slot] == 2 && leases[slot] == 1)) {
            throw new IllegalStateException("unbalanced or reserved pool checkout: " + id);
        }
        leases[slot]--;
    }

    /** A pin models an explicit residency/native/UI lease, not a javac escape proof. */
    public synchronized void pin(long id) {
        int slot = slot(id);
        pins[slot] = Math.addExact(pins[slot], 1);
    }

    public synchronized void unpin(long id) {
        int slot = slot(id);
        // Generic unpin must not consume the pool's +1 or registered JNI shares.
        if (pins[slot] <= jniArrayBorrows[slot]
                + (poolOwners[slot] == 1 ? 1 : 0)) {
            throw new IllegalStateException("unbalanced or reserved-owner unpin: " + id);
        }
        pins[slot]--;
    }


    /**
     * Associates one logical resource ID with one managed Java array without retaining a
     * Java reference. The caller owns the array-to-ID mapping and the escape proof.
     * Object arrays are access-scoped, not primitive storage pointers.
     */
    public synchronized void registerJniArray(long id, Object array) {
        Objects.requireNonNull(array, "array");
        Class<?> component = array.getClass().getComponentType();
        if (component == null) throw new IllegalArgumentException("not a Java array");
        int slot = slot(id);
        if (jniArrayKinds[slot] != 0) {
            throw new IllegalStateException("JNI array already registered: " + id);
        }
        if (leases[slot] != 0 || pins[slot] != 0) {
            throw new IllegalStateException("register JNI array before any leases: " + id);
        }
        int next = Math.addExact(registeredJniArrays, 1);
        jniArrayKinds[slot] = (byte) (component.isPrimitive() ? 1 : 2);
        registeredJniArrays = next;
    }

    /**
     * Reserve one managed native access before calling JNI. Both copied and pinned
     * Get*ArrayElements results count until the final Release, even after JNI_COMMIT.
     * Call outside a JNI critical region; do not call Java while a critical pointer is held.
     * Every successful begin must be balanced by endJniArrayAccess, including failures.
     */
    public synchronized void beginJniArrayAccess(long id) {
        int slot = slot(id);
        if (jniArrayKinds[slot] == 0) {
            throw new IllegalStateException("unregistered JNI array: " + id);
        }
        int nextBorrow = Math.addExact(jniArrayBorrows[slot], 1);
        int nextPin = Math.addExact(pins[slot], 1);
        int nextActive = jniArrayBorrows[slot] == 0
                ? Math.addExact(activeJniArrays, 1) : activeJniArrays;
        long nextTotal = Math.addExact(jniLeaseBegins, 1L);
        jniArrayBorrows[slot] = nextBorrow;
        pins[slot] = nextPin;
        activeJniArrays = nextActive;
        jniLeaseBegins = nextTotal;
    }

    /** End only after the final native pointer release and any write-back completes. */
    public synchronized void endJniArrayAccess(long id) {
        int slot = slot(id);
        if (jniArrayBorrows[slot] == 0) {
            throw new IllegalStateException("unbalanced JNI array release: " + id);
        }
        jniArrayBorrows[slot]--;
        pins[slot]--;
        if (jniArrayBorrows[slot] == 0) activeJniArrays--;
    }

    /** Number of managed, nonretired arrays whose owner registered the JNI contract. */
    public synchronized int registeredJniArrayCount() {
        checkOpen();
        return registeredJniArrays;
    }

    /** Distinct managed array IDs with at least one outstanding JNI array access. */
    public synchronized int activeJniArrayCount() {
        checkOpen();
        return activeJniArrays;
    }

    /** Total successful managed JNI access reservations, including nested reservations. */
    public synchronized long jniArrayLeaseBegins() {
        checkOpen();
        return jniLeaseBegins;
    }

    /** Exact outstanding managed JNI access reservations for one logical array ID. */
    public synchronized int jniArrayBorrowCount(long id) {
        return jniArrayBorrows[slot(id)];
    }


    /**
     * Count the one additional managed reference held by an owner's strong
     * array-backed pool slot. Call only while that slot physically owns the
     * referent and under the same owner lock as slot publication.
     * This cooperative count is NOT a VM heap incoming-reference count.
     */
    public synchronized void retainPoolReference(long id) {
        int slot = slot(id);
        if (poolOwners[slot] != 0) {
            throw new IllegalStateException("duplicate pool retain: " + id);
        }
        int nextPins = Math.addExact(pins[slot], 1);
        pins[slot] = nextPins;
        poolOwners[slot] = 1;
    }

    /** Drop exactly the pool's +1 after detaching its physical strong slot. */
    public synchronized void releasePoolReference(long id) {
        int slot = slot(id);
        if (poolOwners[slot] != 1) {
            throw new IllegalStateException("unbalanced pool release: " + id);
        }
        poolOwners[slot] = 0;
        pins[slot]--;
    }

    /** Sum of cooperative registered leases/pins, including the pool's +1. */
    public synchronized long managedReferenceCount(long id) {
        int slot = slot(id);
        return (long) leases[slot] + pins[slot];
    }

    /** 1 only while a pool has registered its single strong ownership slot. */
    public synchronized int poolReferenceCount(long id) {
        return poolOwners[slot(id)] == 1 ? 1 : 0;
    }

    /**
     * Pool-only count 1 is a candidate, not evidence of zero untracked Java
     * aliases or native pointers. Escape/identity/clean-state proof is mandatory
     * before actually rebinding an object.
     */
    public synchronized boolean poolReuseEligible(long id) {
        int slot = slot(id);
        return poolOwners[slot] == 1 && states[slot] == CLEAN
                && leases[slot] == 0 && pins[slot] == 1
                && jniArrayBorrows[slot] == 0;
    }

    /**
     * Atomically transfer the pool's +1 to a single exclusive managed borrower.
     * The physical free slot must be detached by the same owner monitor without
     * invoking user code between the transfer and detachment.
     */
    public synchronized void borrowPoolReference(long id) {
        int slot = slot(id);
        if (!poolReuseEligible(id)) {
            throw new IllegalStateException("pool checkout not exclusive: " + id);
        }
        int nextLease = Math.addExact(leases[slot], 1);
        poolOwners[slot] = 2;
        pins[slot]--;
        leases[slot] = nextLease;
    }

    /** Return the one exclusive managed borrow into its owner's strong pool slot. */
    public synchronized void returnPoolReference(long id) {
        int slot = slot(id);
        if (poolOwners[slot] != 2 || leases[slot] != 1
                || pins[slot] != 0 || jniArrayBorrows[slot] != 0
                || states[slot] != CLEAN) {
            throw new IllegalStateException("pool checkin not exclusive/clean: " + id);
        }
        int nextPin = Math.addExact(pins[slot], 1);
        leases[slot]--;
        pins[slot] = nextPin;
        poolOwners[slot] = 1;
    }

    /** End an exclusive checkout without returning it to an idle pool slot. */
    public synchronized void discardPoolBorrow(long id) {
        int slot = slot(id);
        if (poolOwners[slot] != 2 || leases[slot] != 1
                || pins[slot] != 0 || jniArrayBorrows[slot] != 0) {
            throw new IllegalStateException("pool discard not exclusive: " + id);
        }
        leases[slot]--;
        poolOwners[slot] = 0;
    }

    /** Dirty-state admission is the caller's responsibility; clearing is an explicit commit. */
    public synchronized void markDirty(long id) {
        states[slot(id)] = DIRTY;
    }

    public synchronized void markCommitted(long id) {
        states[slot(id)] = CLEAN;
    }

    /** Candidate only: the owner must independently validate every external lifetime contract. */
    public synchronized boolean eligible(long id) {
        int slot = slot(id);
        return states[slot] == CLEAN && leases[slot] == 0
                && pins[slot] == 0 && jniArrayBorrows[slot] == 0
                && poolOwners[slot] == 0;
    }

    /** Retire the logical ID, never automatically free JNI memory or reachable Java objects. */
    public synchronized boolean tryRetire(long id) {
        int slot = slot(id);
        if (!eligible(id)) return false;
        if (jniArrayKinds[slot] != 0) {
            registeredJniArrays--;
            jniArrayKinds[slot] = 0;
        }
        states[slot] = RETIRED;
        return true;
    }

    /**
     * Finds candidate clean, unpinned, unleased IDs in stable allocation order. Caller owns the
     * output. Passing an array smaller than {@link #size()} fails before writing anything.
     */
    public synchronized int collectEligible(long[] output) {
        return collectEligible(output, false);
    }

    /** Optional JNI lane; falls back to Java when the native library is unavailable. */
    public synchronized int collectEligible(long[] output, boolean preferNative) {
        checkOpen();
        Objects.requireNonNull(output, "output");
        if (output.length < size) throw new IllegalArgumentException("output capacity < size");
        return M3NativeLeaseScan.collect(ids, leases, pins, states, size, output, preferNative);
    }

    /**
     * Bounded, resumable eligible-ID scan with O(output.length) caller scratch space.
     * Unlike collectEligible, the caller need not allocate a long[] of size().
     *
     * <p>The high 32 bits of the result are the next zero-based table index to scan;
     * the low 32 bits are the number of IDs copied into output[0..written).
     * Start with firstIndex=0, and repeat with M3NativeLeaseScan.nextIndex(result)
     * until the returned next index equals size(). Results are only candidates.
     * A sequence of calls is not an atomic snapshot if other threads mutate the table
     * between calls; owners must recheck eligibility before acting on any ID.</p>
     *
     * @throws IllegalArgumentException for an invalid cursor or an empty output buffer
     */
    synchronized long collectEligibleWindow(int firstIndex, long[] output,
                                                   boolean preferNative) {
        checkOpen();
        Objects.requireNonNull(output, "output");
        if (firstIndex < 0 || firstIndex > size || output.length == 0) {
            throw new IllegalArgumentException("invalid scan cursor or empty output");
        }
        return M3NativeLeaseScan.collectWindow(ids, leases, pins, states, size,
                firstIndex, output, preferNative);
    }

    /** Pure Java scan by default; never silently changes existing collection behavior. */
    synchronized long collectEligibleWindow(int firstIndex, long[] output) {
        return collectEligibleWindow(firstIndex, output, false);
    }

    public synchronized int leaseCount(long id) {
        return leases[slot(id)];
    }

    public synchronized int pinCount(long id) {
        return pins[slot(id)];
    }

    public synchronized int size() {
        checkOpen();
        return size;
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        for (int i = 0; i < size; i++) {
            if (leases[i] != 0 || pins[i] != 0 || jniArrayBorrows[i] != 0
                    || states[i] == DIRTY) {
                throw new IllegalStateException("outstanding lease/pin/dirty state for " + ids[i]);
            }
        }
        Arrays.fill(ids, 0L);
        Arrays.fill(leases, 0);
        Arrays.fill(pins, 0);
        Arrays.fill(jniArrayBorrows, 0);
        Arrays.fill(jniArrayKinds, (byte) 0);
        Arrays.fill(poolOwners, (byte) 0);
        Arrays.fill(states, RETIRED);
        registeredJniArrays = 0;
        activeJniArrays = 0;
        size = 0;
        closed = true;
    }

    private int slot(long id) {
        checkOpen();
        if (id < 1 || id > size) throw new IllegalArgumentException("unknown logical ID: " + id);
        int slot = Math.toIntExact(id - 1);
        if (states[slot] == RETIRED) throw new IllegalStateException("retired logical ID: " + id);
        return slot;
    }

    private void checkOpen() {
        if (closed) throw new IllegalStateException("closed lease table");
    }

    /** Publish replacement lanes only after *all* allocations succeed. */
    private void ensureCapacity(int required) {
        if (required <= ids.length) return;
        int preferred = (int) Math.min((long) Integer.MAX_VALUE - 8L,
                Math.max((long) required, (long) ids.length + (ids.length >>> 1) + 1L));
        int capacity = Math.toIntExact(RuntimeHeap.chooseAmortizedCapacity(
                required, preferred, Long.BYTES + Integer.BYTES * 3 + Byte.BYTES * 3));
        long[] nextIds = Arrays.copyOf(ids, capacity);
        int[] nextLeases = Arrays.copyOf(leases, capacity);
        int[] nextPins = Arrays.copyOf(pins, capacity);
        int[] nextJniBorrows = Arrays.copyOf(jniArrayBorrows, capacity);
        byte[] nextJniKinds = Arrays.copyOf(jniArrayKinds, capacity);
        byte[] nextPoolOwners = Arrays.copyOf(poolOwners, capacity);
        byte[] nextStates = Arrays.copyOf(states, capacity);
        ids = nextIds;
        leases = nextLeases;
        pins = nextPins;
        jniArrayBorrows = nextJniBorrows;
        jniArrayKinds = nextJniKinds;
        poolOwners = nextPoolOwners;
        states = nextStates;
    }
}
