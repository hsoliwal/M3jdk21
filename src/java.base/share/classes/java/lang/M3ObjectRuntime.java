/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file).
 */
package java.lang;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Objects;

import jdk.internal.mindex.M3PrimitiveIdentity;
import jdk.internal.misc.InnocuousThread;
import jdk.internal.misc.VM;

/**
 * JDK-owned observation mechanics. Payloads remain in their original owners.
 * All mutable registry, lane, edge and queue state is guarded by LOCK. User
 * callbacks never execute while holding it. The JNI entrypoints below publish
 * after a cooperating producer commits; they do not implement store interception.
 */
final class M3ObjectRuntime {
    private static final Object LOCK = new Object();

    // A weak value is essential: a strong facade would retain its own weak key.
    private static WeakReference<M3Object>[] owners;
    private static int[] ownerHashes;
    // Primitive bucket chains; a zero link means absent, otherwise index+1.
    // Reachable M3Object facades are NEVER evicted: the index holds weak values.
    private static int[] ownerNext;
    private static int[] ownerBuckets;
    private static int ownerUsed;
    private static int ownerFreeHead;
    private static WeakReference<AccessPlan>[] plans;
    // Bounded weak metadata only. Observed owners, subscriptions and JNI events
    // do not participate in this cache and must never be evicted by it.
    private static final int PLAN_INITIAL_CAPACITY = 16;
    private static final int PLAN_PRESSURED_CAPACITY = 64;
    private static final int PLAN_NORMAL_CAPACITY = 256;
    private static int[] planFrequency;
    private static long[] planLastTouch;
    private static long planClock;
    private static long nextOwnerId;
    private static long publicationStamp;

    // One intrusive pending entry per owner; no bounded queue and no event node.
    private static State first;
    private static State last;
    private static Thread worker;
    private static State[] traversal;

    private M3ObjectRuntime() {
    }

    private static final class Policy {
        static final boolean ENABLED = Boolean.parseBoolean(
                VM.getSavedProperty("jdk.m3.objectEvents.enabled"));
    }

    static boolean enabled() {
        return Policy.ENABLED;
    }

    static M3Object of(Object source) {
        synchronized (LOCK) {
            return ofLocked(source);
        }
    }

    /**
     * Lookup by source identity. Buckets hold only primitive slot indices;
     * the sole facade references are weak. Unlink cleared entries as they
     * are encountered without retaining any object or class loader.
     * LOCK must be held by the caller.
     */
    private static M3Object findLocked(Object source) {
        if (owners == null) return null;
        int hash = System.identityHashCode(source);
        int bucket = ownerBucket(hash, ownerBuckets.length - 1);
        int previous = 0;
        int link = ownerBuckets[bucket];
        while (link != 0) {
            int index = link - 1;
            int following = ownerNext[index];
            WeakReference<M3Object> weak = owners[index];
            M3Object facade = weak == null ? null : weak.get();
            if (facade == null) {
                if (previous == 0) ownerBuckets[bucket] = following;
                else ownerNext[previous - 1] = following;
                owners[index] = null;
                ownerHashes[index] = 0;
                ownerNext[index] = ownerFreeHead;
                ownerFreeHead = link;
                link = following;
                continue;
            }
            if (ownerHashes[index] == hash && facade.source == source) {
                return facade;
            }
            previous = link;
            link = following;
        }
        return null;
    }

    private static int ownerBucket(int hash, int mask) {
        return (hash ^ (hash >>> 16)) & mask;
    }

    @SuppressWarnings("unchecked")
    private static M3Object ofLocked(Object source) {
        M3Object existing = findLocked(source);
        if (existing != null) return existing;
        if (owners == null) {
            // Prepare all lanes before publishing the initialized registry.
            // An allocation failure must leave the null sentinel intact.
            WeakReference<M3Object>[] initialOwners =
                    (WeakReference<M3Object>[]) new WeakReference<?>[16];
            int[] initialHashes = new int[16];
            int[] initialNext = new int[16];
            int[] initialBuckets = new int[32];
            owners = initialOwners;
            ownerHashes = initialHashes;
            ownerNext = initialNext;
            ownerBuckets = initialBuckets;
        }
        if (ownerUsed == owners.length && ownerFreeHead == 0) {
            compactOrGrowOwnersLocked();
        }
        // Prepare both allocations before taking a free-list or new slot.
        // An OutOfMemoryError cannot consume ownerFreeHead or ownerUsed.
        int hash = System.identityHashCode(source);
        int bucket = ownerBucket(hash, ownerBuckets.length - 1);
        M3Object result = new M3Object(source);
        WeakReference<M3Object> prepared = new WeakReference<>(result);
        int slot;
        if (ownerFreeHead != 0) {
            slot = ownerFreeHead - 1;
            ownerFreeHead = ownerNext[slot];
        } else {
            slot = ownerUsed++;
        }
        owners[slot] = prepared;
        ownerHashes[slot] = hash;
        ownerNext[slot] = ownerBuckets[bucket];
        ownerBuckets[bucket] = slot + 1;
        return result;
    }

    /**
     * Rare full-index sweep. A surviving facade keeps its exact identity;
     * only cleared weak references are dropped. Prepare every lane before
     * committing them under LOCK, so allocation failures cannot partially
     * mutate the existing registry.
     */
    @SuppressWarnings("unchecked")
    private static void compactOrGrowOwnersLocked() {
        int live = 0;
        for (int i = 0; i < ownerUsed; i++) {
            if (owners[i] != null && owners[i].get() != null) live++;
        }
        int capacity;
        if (live > owners.length - owners.length / 4) {
            capacity = Math.multiplyExact(owners.length, 2);
        } else if (owners.length > 16 && live <= owners.length / 4) {
            // Only already-cleared weak slots and unnecessary index lanes shrink.
            int desired = Math.max(16, Math.multiplyExact(live + 1, 2));
            capacity = 16;
            while (capacity < desired) capacity = Math.multiplyExact(capacity, 2);
        } else {
            capacity = owners.length;
        }
        WeakReference<M3Object>[] grownOwners =
                (WeakReference<M3Object>[]) new WeakReference<?>[capacity];
        int[] grownHashes = new int[capacity];
        int[] grownNext = new int[capacity];
        int[] grownBuckets = new int[Math.multiplyExact(capacity, 2)];
        int position = 0;
        for (int i = 0; i < ownerUsed; i++) {
            WeakReference<M3Object> reference = owners[i];
            if (reference == null || reference.get() == null) continue;
            int nextIndex = position++;
            grownOwners[nextIndex] = reference;
            grownHashes[nextIndex] = ownerHashes[i];
            int bucket = ownerBucket(grownHashes[nextIndex], grownBuckets.length - 1);
            grownNext[nextIndex] = grownBuckets[bucket];
            grownBuckets[bucket] = nextIndex + 1;
        }
        owners = grownOwners;
        ownerHashes = grownHashes;
        ownerNext = grownNext;
        ownerBuckets = grownBuckets;
        ownerUsed = position;
        ownerFreeHead = 0;
    }

    @SuppressWarnings("unchecked")
    static AccessPlan plan(MethodHandles.Lookup lookup, Class<?> declaringType,
                           String name, Class<?> type) {
        synchronized (LOCK) {
            if (plans == null) {
                // Do not publish a partially allocated weak-plan cache.
                WeakReference<AccessPlan>[] initialPlans =
                        (WeakReference<AccessPlan>[]) new WeakReference<?>[PLAN_INITIAL_CAPACITY];
                int[] initialFrequency = new int[PLAN_INITIAL_CAPACITY];
                long[] initialLastTouch = new long[PLAN_INITIAL_CAPACITY];
                plans = initialPlans;
                planFrequency = initialFrequency;
                planLastTouch = initialLastTouch;
            }
            // Sharing retains the exact Lookup identity/access authority.
            for (int i = 0; i < plans.length; i++) {
                AccessPlan current = plans[i] == null ? null : plans[i].get();
                if (current != null && current.lookup == lookup
                        && current.matches(declaringType, name, type)) {
                    touchPlanLocked(i);
                    // Heap pressure is sampled only once per 1,024 accesses.
                    if ((planClock & 1023L) == 0L) {
                        int budget = planBudget();
                        if (plans.length > budget) resizePlansLocked(budget);
                    }
                    return current;
                }
            }
            // Admission and true misses sample pressure without a monitor API,
            // management module dependency, explicit GC, or JNI allocation.
            int budget = planBudget();
            if (plans.length > budget) resizePlansLocked(budget);
            int free = -1;
            int live = 0;
            for (int i = 0; i < plans.length; i++) {
                AccessPlan current = plans[i] == null ? null : plans[i].get();
                if (current == null) {
                    if (free < 0) free = i;
                } else {
                    live++;
                }
            }
            if (live < budget) {
                if (free < 0) {
                    int oldSize = plans.length;
                    resizePlansLocked(Math.min(budget, Math.multiplyExact(oldSize, 2)));
                    free = oldSize;
                }
            } else {
                // Frequency wins; least-recently used breaks equal-frequency ties.
                free = planVictimLocked();
            }
            AccessPlan created = new AccessPlan(lookup, declaringType, name, type);
            plans[free] = new WeakReference<>(created);
            planFrequency[free] = 1;
            planLastTouch[free] = nextPlanClockLocked();
            return created;
        }
    }

    /** Testable pressure policy; only the already-cacheable weak metadata shrinks. */
    static int planCapacityFor(long used, long maximum) {
        if (maximum <= 0 || maximum == Long.MAX_VALUE || used < 0) {
            return PLAN_NORMAL_CAPACITY;
        }
        if (used >= maximum - maximum / 8) return PLAN_INITIAL_CAPACITY;
        if (used >= maximum - maximum / 4) return PLAN_PRESSURED_CAPACITY;
        return PLAN_NORMAL_CAPACITY;
    }

    private static int planBudget() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        return planCapacityFor(used, runtime.maxMemory());
    }

    private static long nextPlanClockLocked() {
        if (planClock == Long.MAX_VALUE) {
            planClock >>>= 1;
            for (int i = 0; i < planLastTouch.length; i++) {
                planLastTouch[i] >>>= 1;
            }
        }
        long tick = ++planClock;
        // Age on every access, including unique misses: historical hot plans
        // must not retain frequency forever when there are no later hits.
        if ((tick & 4095L) == 0L) {
            for (int i = 0; i < plans.length; i++) {
                if (plans[i] == null || plans[i].get() == null) {
                    planFrequency[i] = 0;
                    planLastTouch[i] = 0;
                } else {
                    planFrequency[i] = Math.max(1, planFrequency[i] >>> 1);
                }
            }
        }
        return tick;
    }

    private static void touchPlanLocked(int slot) {
        if (planFrequency[slot] < Integer.MAX_VALUE) planFrequency[slot]++;
        planLastTouch[slot] = nextPlanClockLocked();
    }

    private static int planVictimLocked() {
        int victim = -1;
        for (int i = 0; i < plans.length; i++) {
            if (plans[i] == null || plans[i].get() == null) return i;
            if (victim < 0 || planFrequency[i] < planFrequency[victim]
                    || (planFrequency[i] == planFrequency[victim]
                    && planLastTouch[i] < planLastTouch[victim])) {
                victim = i;
            }
        }
        if (victim < 0) throw new AssertionError("no plan cache victim");
        return victim;
    }

    /** Rare structural resize; prepare complete array lanes before publication. */
    @SuppressWarnings("unchecked")
    private static void resizePlansLocked(int capacity) {
        WeakReference<AccessPlan>[] nextPlans =
                (WeakReference<AccessPlan>[]) new WeakReference<?>[capacity];
        int[] nextFrequency = new int[capacity];
        long[] nextLastTouch = new long[capacity];
        boolean[] selected = new boolean[plans.length];
        // Retain the highest frequency and most recent reachable descriptors.
        // All plan references remain weak; there is no entry/node allocation.
        for (int slot = 0; slot < capacity; slot++) {
            int best = -1;
            for (int i = 0; i < plans.length; i++) {
                if (selected[i] || plans[i] == null || plans[i].get() == null) continue;
                if (best < 0 || planFrequency[i] > planFrequency[best]
                        || (planFrequency[i] == planFrequency[best]
                        && planLastTouch[i] > planLastTouch[best])) {
                    best = i;
                }
            }
            if (best < 0) break;
            selected[best] = true;
            nextPlans[slot] = plans[best];
            nextFrequency[slot] = planFrequency[best];
            nextLastTouch[slot] = planLastTouch[best];
        }
        plans = nextPlans;
        planFrequency = nextFrequency;
        planLastTouch = nextLastTouch;
    }

    /** A shared descriptor retains authority but resolves no payload eagerly. */
    static final class AccessPlan {
        final MethodHandles.Lookup lookup;
        final Class<?> declaringType;
        final String name;
        final Class<?> type;
        private volatile VarHandle handle;

        AccessPlan(MethodHandles.Lookup lookup, Class<?> declaringType,
                   String name, Class<?> type) {
            this.lookup = lookup;
            this.declaringType = declaringType;
            this.name = name;
            this.type = type;
        }

        boolean matches(Class<?> declaringType, String name, Class<?> type) {
            return this.declaringType == declaringType && this.type == type
                    && this.name.equals(name);
        }

        VarHandle resolve() {
            VarHandle result = handle;
            if (result == null) {
                synchronized (this) {
                    result = handle;
                    if (result == null) {
                        try {
                            // The slot key names the actual declaring class,
                            // not a subclass through which an inherited member
                            // could also be found by findVarHandle.
                            declaringType.getDeclaredField(name);
                            result = lookup.findVarHandle(declaringType, name, type);
                        } catch (NoSuchFieldException | IllegalAccessException ex) {
                            throw new IllegalArgumentException(
                                    "cannot resolve instance field "
                                    + declaringType.getName() + "." + name, ex);
                        }
                        handle = result;
                    }
                }
            }
            return result;
        }

        void checkValue(Object value) {
            Class<?> required = type;
            if (type.isPrimitive()) {
                Objects.requireNonNull(value, "primitive value");
                if (type == boolean.class) required = Boolean.class;
                else if (type == byte.class) required = Byte.class;
                else if (type == short.class) required = Short.class;
                else if (type == char.class) required = Character.class;
                else if (type == int.class) required = Integer.class;
                else if (type == long.class) required = Long.class;
                else if (type == float.class) required = Float.class;
                else if (type == double.class) required = Double.class;
            }
            if (value != null && !required.isInstance(value)) {
                throw new ClassCastException("value is not " + required.getName());
            }
        }

        boolean same(Object left, Object right) {
            if (!type.isPrimitive()) {
                return left == right;
            }
            return bits(left) == bits(right);
        }

        private long bits(Object value) {
            if (type == boolean.class) return M3PrimitiveIdentity.bits((boolean) value);
            if (type == byte.class) return M3PrimitiveIdentity.bits((byte) value);
            if (type == short.class) return M3PrimitiveIdentity.bits((short) value);
            if (type == char.class) return M3PrimitiveIdentity.bits((char) value);
            if (type == int.class) return M3PrimitiveIdentity.bits((int) value);
            if (type == long.class) return M3PrimitiveIdentity.bits((long) value);
            if (type == float.class) return M3PrimitiveIdentity.bits((float) value);
            if (type == double.class) return M3PrimitiveIdentity.bits((double) value);
            throw new AssertionError("unsupported primitive field");
        }
    }

    static void set(M3Object.Field field, Object value) {
        VarHandle handle = field.resolve();
        field.plan.checkValue(value);
        if (!enabled()) {
            handle.getAndSet(field.owner.source, value);
            return;
        }
        synchronized (LOCK) {
            Object previous = handle.getAndSet(field.owner.source, value);
            if (!field.plan.same(previous, value)) {
                fieldChangedLocked(field.owner, field.plan);
            }
        }
    }

    static boolean compareAndSet(M3Object.Field field, Object expected, Object value) {
        VarHandle handle = field.resolve();
        field.plan.checkValue(expected);
        field.plan.checkValue(value);
        if (!enabled()) {
            return handle.compareAndSet(field.owner.source, expected, value);
        }
        synchronized (LOCK) {
            boolean changed = handle.compareAndSet(field.owner.source, expected, value);
            if (changed && !field.plan.same(expected, value)) {
                fieldChangedLocked(field.owner, field.plan);
            }
            return changed;
        }
    }

    private static void fieldChangedLocked(M3Object owner, AccessPlan plan) {
        State state = owner.state;
        if (state != null && state.interested()) {
            int slot = state.findField(plan.declaringType, plan.name, plan.type);
            publishLocked(state, slot, plan.type.isPrimitive()
                    ? ObjectEvent.VALUE : ObjectEvent.REFERENCE);
        }
    }

    static void contentChanged(M3Object owner) {
        if (enabled()) {
            synchronized (LOCK) {
                State state = owner.state;
                if (state != null && state.interested()) {
                    publishLocked(state, -1, ObjectEvent.CONTENT);
                }
            }
        }
    }

    /** JNI producer entrypoint: never creates an unobserved owner. */
    static void notifyContentChanged(Object source) {
        Objects.requireNonNull(source, "source");
        if (enabled()) {
            synchronized (LOCK) {
                M3Object owner = findLocked(source);
                if (owner != null && owner.state != null && owner.state.interested()) {
                    publishLocked(owner.state, -1, ObjectEvent.CONTENT);
                }
            }
        }
    }

    /**
     * JNI producer entrypoint. The producer commits and suppresses no-ops first.
     * Existing field slots are matched without resolving arbitrary access. A
     * known owner can still receive a root invalidation without a field slot.
     */
    static void notifyFieldChanged(Object source, Class<?> declaringType,
                                   String name, Class<?> type, int mask) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(declaringType, "declaringType");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        int kind = type.isPrimitive() ? ObjectEvent.VALUE : ObjectEvent.REFERENCE;
        if (type == void.class || mask != kind || !declaringType.isInstance(source)) {
            throw new IllegalArgumentException("invalid committed field notification");
        }
        if (enabled()) {
            synchronized (LOCK) {
                M3Object owner = findLocked(source);
                if (owner != null && owner.state != null && owner.state.interested()) {
                    int slot = owner.state.findField(declaringType, name, type);
                    publishLocked(owner.state, slot, kind);
                }
            }
        }
    }

    static ObjectSubscription listen(M3Object owner, M3Object.Field field,
                                     int mask, ObjectChangeListener listener) {
        Objects.requireNonNull(listener, "listener");
        if (mask == 0 || (mask & ~ObjectEvent.ALL) != 0) {
            throw new IllegalArgumentException("invalid event mask");
        }
        if (!enabled()) {
            return ObjectSubscription.CLOSED;
        }
        if (field != null) {
            field.resolve(); // Always validate this caller, including receiver limits.
        }
        synchronized (LOCK) {
            ensureWorkerLocked(); // Failure precedes subscription admission.
            State state = stateLocked(owner);
            int fieldSlot = field == null ? -1 : state.fieldSlot(field);
            try {
                int index = state.subscriberSlot();
                ObjectSubscription subscription = new ObjectSubscription(state, index);
                boolean follows = field != null && !field.type().isPrimitive()
                        && (mask & ObjectEvent.CONTENT) != 0;
                if (follows && state.followers[fieldSlot] == 0) {
                    // Allocate/resolve the edge before admitting an unreturned
                    // listener. The remaining lane stores cannot allocate.
                    rebindLocked(state, fieldSlot, false, true);
                }
                state.subscriptions[index] = subscription;
                state.listeners[index] = listener;
                state.selected[index] = field;
                state.requested[index] = mask;
                state.slots[index] = fieldSlot;
                state.subscriberCount++;
                if (fieldSlot >= 0) {
                    state.fieldUsers[fieldSlot]++;
                    if (follows) {
                        state.followers[fieldSlot]++;
                    }
                }
                return subscription;
            } catch (RuntimeException | Error failure) {
                if (fieldSlot >= 0 && state.fieldUsers[fieldSlot] == 0) {
                    detachLocked(state, fieldSlot);
                    state.fields[fieldSlot] = null;
                    state.generations[fieldSlot]++;
                }
                throw failure;
            }
        }
    }

    static void close(ObjectSubscription subscription) {
        synchronized (LOCK) {
            State state = subscription.state;
            if (state == null) {
                return;
            }
            int index = subscription.index;
            if (state.subscriptions[index] != subscription) {
                throw new AssertionError("subscription slot identity lost");
            }
            int slot = state.slots[index];
            M3Object.Field selected = state.selected[index];
            int requested = state.requested[index];
            state.subscriptions[index] = null;
            state.listeners[index] = null;
            state.selected[index] = null;
            state.requested[index] = 0;
            state.clearPending(index);
            state.subscriberCount--;
            subscription.state = null;
            if (slot >= 0) {
                if ((requested & ObjectEvent.CONTENT) != 0
                        && !selected.type().isPrimitive()
                        && --state.followers[slot] == 0) {
                    detachLocked(state, slot);
                }
                if (--state.fieldUsers[slot] == 0) {
                    state.fields[slot] = null;
                    state.generations[slot]++;
                } else if (state.fields[slot] == selected) {
                    for (int i = 0; i < state.subscriptions.length; i++) {
                        if (state.subscriptions[i] != null && state.slots[i] == slot) {
                            state.fields[slot] = state.selected[i];
                            break;
                        }
                    }
                }
            }
            removeIfCleanLocked(state);
        }
    }

    private static State stateLocked(M3Object owner) {
        State state = owner.state;
        if (state == null) {
            state = new State(owner, ++nextOwnerId);
            owner.state = state;
        }
        return state;
    }

    /** Primitive metadata lanes are allocated only for actually admitted work. */
    static final class State {
        final M3Object owner;
        final long id;
        long sequence;
        long visited;
        long waveSequence;
        State next;
        boolean queued;
        int subscriberCount;

        ObjectSubscription[] subscriptions;
        ObjectChangeListener[] listeners;
        M3Object.Field[] selected;
        int[] requested;
        int[] slots;
        int[] pending;
        long[] firstSequences;
        long[] sequences;
        long[] pendingGenerations;
        long[] origins;
        long[] originSequences;
        boolean[] coalesced;

        M3Object.Field[] fields;
        int[] fieldUsers;
        int[] followers;
        long[] generations;
        State[] children;
        int[] childEdges;

        State[] parents;
        int[] parentSlots;
        long[] parentGenerations;
        int parentCount;

        State(M3Object owner, long id) {
            this.owner = owner;
            this.id = id;
        }

        boolean interested() {
            return subscriberCount != 0 || parentCount != 0;
        }

        int subscriberSlot() {
            if (subscriptions != null) {
                for (int i = 0; i < subscriptions.length; i++) {
                    if (subscriptions[i] == null) {
                        return i;
                    }
                }
            }
            int old = subscriptions == null ? 0 : subscriptions.length;
            int size = old == 0 ? 4 : Math.multiplyExact(old, 2);
            ObjectSubscription[] grownSubscriptions = subscriptions == null ? new ObjectSubscription[size]
                    : Arrays.copyOf(subscriptions, size);
            ObjectChangeListener[] grownListeners = listeners == null ? new ObjectChangeListener[size]
                    : Arrays.copyOf(listeners, size);
            M3Object.Field[] grownSelected = selected == null ? new M3Object.Field[size]
                    : Arrays.copyOf(selected, size);
            int[] grownRequested = requested == null ? new int[size] : Arrays.copyOf(requested, size);
            int[] grownSlots = slots == null ? new int[size] : Arrays.copyOf(slots, size);
            int[] grownPending = pending == null ? new int[size] : Arrays.copyOf(pending, size);
            long[] grownFirstSequences = firstSequences == null ? new long[size]
                    : Arrays.copyOf(firstSequences, size);
            long[] grownSequences = sequences == null ? new long[size] : Arrays.copyOf(sequences, size);
            long[] grownPendingGenerations = pendingGenerations == null ? new long[size]
                    : Arrays.copyOf(pendingGenerations, size);
            long[] grownOrigins = origins == null ? new long[size] : Arrays.copyOf(origins, size);
            long[] grownOriginSequences = originSequences == null ? new long[size]
                    : Arrays.copyOf(originSequences, size);
            boolean[] grownCoalesced = coalesced == null ? new boolean[size]
                    : Arrays.copyOf(coalesced, size);
            // Commit a complete lane set; failed growth preserves the old set.
            subscriptions = grownSubscriptions;
            listeners = grownListeners;
            selected = grownSelected;
            requested = grownRequested;
            slots = grownSlots;
            pending = grownPending;
            firstSequences = grownFirstSequences;
            sequences = grownSequences;
            pendingGenerations = grownPendingGenerations;
            origins = grownOrigins;
            originSequences = grownOriginSequences;
            coalesced = grownCoalesced;
            return old;
        }

        int findField(Class<?> declaringType, String name, Class<?> type) {
            if (fields != null) {
                for (int i = 0; i < fields.length; i++) {
                    if (fields[i] != null
                            && fields[i].plan.matches(declaringType, name, type)) {
                        return i;
                    }
                }
            }
            return -1;
        }

        int fieldSlot(M3Object.Field field) {
            int found = findField(field.declaringType(), field.name(), field.type());
            if (found >= 0) {
                return found;
            }
            int free = -1;
            if (fields != null) {
                for (int i = 0; i < fields.length; i++) {
                    if (fields[i] == null) {
                        free = i;
                        break;
                    }
                }
            }
            if (free < 0) {
                int old = fields == null ? 0 : fields.length;
                int size = old == 0 ? 4 : Math.multiplyExact(old, 2);
                M3Object.Field[] grownFields = fields == null ? new M3Object.Field[size]
                        : Arrays.copyOf(fields, size);
                int[] grownFieldUsers = fieldUsers == null ? new int[size]
                        : Arrays.copyOf(fieldUsers, size);
                int[] grownFollowers = followers == null ? new int[size]
                        : Arrays.copyOf(followers, size);
                long[] grownGenerations = generations == null ? new long[size]
                        : Arrays.copyOf(generations, size);
                State[] grownChildren = children == null ? new State[size]
                        : Arrays.copyOf(children, size);
                int[] grownChildEdges = childEdges == null ? new int[size]
                        : Arrays.copyOf(childEdges, size);
                fields = grownFields;
                fieldUsers = grownFieldUsers;
                followers = grownFollowers;
                generations = grownGenerations;
                children = grownChildren;
                childEdges = grownChildEdges;
                free = old;
            }
            fields[free] = field;
            generations[free]++;
            return free;
        }

        void ensureParentCapacity() {
            if (parents == null || parentCount == parents.length) {
                int size = parents == null ? 4 : Math.multiplyExact(parents.length, 2);
                State[] grownParents = parents == null ? new State[size] : Arrays.copyOf(parents, size);
                int[] grownParentSlots = parentSlots == null ? new int[size]
                        : Arrays.copyOf(parentSlots, size);
                long[] grownParentGenerations = parentGenerations == null ? new long[size]
                        : Arrays.copyOf(parentGenerations, size);
                parents = grownParents;
                parentSlots = grownParentSlots;
                parentGenerations = grownParentGenerations;
            }
        }

        void addParent(State parent, int slot) {
            parents[parentCount] = parent;
            parentSlots[parentCount] = slot;
            parentGenerations[parentCount] = parent.generations[slot];
            parent.childEdges[slot] = parentCount++;
        }

        void clearPending(int index) {
            pending[index] = 0;
            firstSequences[index] = 0;
            sequences[index] = 0;
            pendingGenerations[index] = 0;
            origins[index] = 0;
            originSequences[index] = 0;
            coalesced[index] = false;
        }
    }

    private static void detachLocked(State parent, int slot) {
        State child = parent.children[slot];
        if (child == null) {
            return;
        }
        int edge = parent.childEdges[slot];
        int end = --child.parentCount;
        if (edge != end) {
            State moved = child.parents[end];
            int movedSlot = child.parentSlots[end];
            child.parents[edge] = moved;
            child.parentSlots[edge] = movedSlot;
            child.parentGenerations[edge] = child.parentGenerations[end];
            moved.childEdges[movedSlot] = edge;
        }
        child.parents[end] = null;
        child.parentSlots[end] = 0;
        child.parentGenerations[end] = 0;
        parent.children[slot] = null;
        parent.childEdges[slot] = 0;
    }

    private static void rebindLocked(State state, int slot, boolean advance) {
        rebindLocked(state, slot, advance, state.followers[slot] != 0);
    }

    private static void rebindLocked(State state, int slot, boolean advance,
                                     boolean follows) {
        State child = null;
        if (follows) {
            Object value = state.fields[slot].get();
            if (value != null) {
                child = stateLocked(ofLocked(value));
                child.ensureParentCapacity();
            }
        }
        // Only now detach the old binding: all fallible preparation is done.
        if (advance) {
            state.generations[slot]++;
            for (int i = 0; i < state.subscriptions.length; i++) {
                if (state.subscriptions[i] != null && state.slots[i] == slot
                        && (state.pending[i] & ObjectEvent.CONTENT) != 0) {
                    if (state.children[slot] == child) {
                        // A native store can be reconciled before its delayed
                        // reference hook arrives. Content accepted from this
                        // same live child remains valid under the new generation.
                        state.pendingGenerations[i] = state.generations[slot];
                    } else {
                        state.pending[i] &= ~ObjectEvent.CONTENT;
                        if (state.pending[i] == 0) {
                            state.clearPending(i);
                        }
                    }
                }
            }
        }
        detachLocked(state, slot);
        if (child != null) {
            child.addParent(state, slot);
            state.children[slot] = child;
        }
    }

    private static void publishLocked(State source, int slot, int mask) {
        if (slot >= 0 && mask == ObjectEvent.REFERENCE) {
            rebindLocked(source, slot, true);
        }
        long stamp = ++publicationStamp;
        if (stamp == 0) {
            stamp = ++publicationStamp;
        }
        long originSequence = ++source.sequence;
        source.visited = stamp;
        source.waveSequence = originSequence;
        captureLocked(source, slot, mask, source.id, originSequence);

        if (traversal == null) {
            traversal = new State[8];
        }
        int head = 0;
        int tail = 1;
        traversal[0] = source;
        try {
            while (head < tail) {
                State child = traversal[head];
                traversal[head++] = null;
                int edge = 0;
                while (edge < child.parentCount) {
                    State parent = child.parents[edge];
                    int parentSlot = child.parentSlots[edge];
                    if (parent.children[parentSlot] != child
                            || parent.generations[parentSlot]
                                    != child.parentGenerations[edge]) {
                        throw new AssertionError("stale observation edge");
                    }
                    // A raw, unreported replacement must not attribute this old
                    // child's publication to a now-different selected reference.
                    if (parent.fields[parentSlot].get() != child.owner.source) {
                        rebindLocked(parent, parentSlot, true);
                        continue; // Detachment swapped a different edge into this row.
                    }
                    if (parent.visited != stamp) {
                        parent.visited = stamp;
                        parent.waveSequence = ++parent.sequence;
                        if (tail == traversal.length) {
                            traversal = Arrays.copyOf(traversal,
                                    Math.multiplyExact(tail, 2));
                        }
                        traversal[tail++] = parent;
                    }
                    // Owner expansion is deduplicated; alias slots are not.
                    captureLocked(parent, parentSlot, ObjectEvent.CONTENT,
                            source.id, originSequence);
                    edge++;
                }
            }
        } finally {
            Arrays.fill(traversal, head, tail, null);
        }
    }

    private static void captureLocked(State state, int slot, int mask,
                                      long origin, long originSequence) {
        if (state.subscriptions == null) {
            return;
        }
        boolean dirty = false;
        for (int i = 0; i < state.subscriptions.length; i++) {
            if (state.subscriptions[i] == null
                    || (state.selected[i] != null && state.slots[i] != slot)) {
                continue;
            }
            int accepted = state.requested[i] & mask;
            if (accepted == 0) {
                continue;
            }
            long sequence = state.waveSequence;
            if (state.pending[i] == 0) {
                state.firstSequences[i] = sequence;
                state.origins[i] = origin;
                state.originSequences[i] = originSequence;
            } else {
                state.coalesced[i] |= state.sequences[i] != sequence;
                if (state.origins[i] != origin) {
                    state.origins[i] = 0;
                    state.originSequences[i] = 0;
                } else {
                    state.originSequences[i] = originSequence;
                }
            }
            state.pending[i] |= accepted;
            state.sequences[i] = sequence;
            state.pendingGenerations[i] = state.selected[i] == null ? 0
                    : state.generations[state.slots[i]];
            dirty = true;
        }
        if (dirty) {
            enqueueLocked(state);
        }
    }

    private static void enqueueLocked(State state) {
        if (!state.queued) {
            state.queued = true;
            if (last == null) {
                first = state;
            } else {
                last.next = state;
            }
            last = state;
            LOCK.notifyAll();
        }
    }

    private static void removeIfCleanLocked(State state) {
        if (!state.queued) {
            return;
        }
        for (int value : state.pending) {
            if (value != 0) {
                return;
            }
        }
        State previous = null;
        for (State current = first; current != null; current = current.next) {
            if (current == state) {
                if (previous == null) first = current.next;
                else previous.next = current.next;
                if (last == current) last = previous;
                current.next = null;
                current.queued = false;
                return;
            }
            previous = current;
        }
        throw new AssertionError("queued observation owner missing");
    }

    private static void ensureWorkerLocked() {
        if (worker == null) {
            Thread thread = InnocuousThread.newSystemThread(
                    "M3Object-dispatch", M3ObjectRuntime::dispatch);
            thread.setDaemon(true);
            thread.start();
            worker = thread;
        }
    }

    private static void dispatch() {
        for (;;) {
            State state;
            synchronized (LOCK) {
                while (first == null) {
                    try {
                        LOCK.wait();
                    } catch (InterruptedException ignored) {
                        // No caller owns this shared system thread's cancellation.
                    }
                }
                state = first;
                first = state.next;
                if (first == null) {
                    last = null;
                }
                state.next = null;
                state.queued = false;
            }
            try {
                drain(state);
            } catch (Throwable failure) {
                // Keep pending bits discoverable if envelope allocation failed.
                synchronized (LOCK) {
                    enqueueLocked(state);
                }
            } finally {
                state = null; // An idle worker must not retain its previous owner.
                ((InnocuousThread) Thread.currentThread()).eraseThreadLocals();
            }
        }
    }

    private static void drain(State state) {
        int limit;
        synchronized (LOCK) {
            limit = state.subscriptions.length;
        }
        for (int i = 0; i < limit; i++) {
            ObjectChangeListener listener;
            ObjectEvent event;
            synchronized (LOCK) {
                if (state.subscriptions[i] == null || state.pending[i] == 0) {
                    continue;
                }
                event = new ObjectEvent(state.owner.source, state.selected[i],
                        state.pending[i], state.firstSequences[i], state.sequences[i],
                        state.pendingGenerations[i], state.origins[i],
                        state.originSequences[i], state.coalesced[i]);
                listener = state.listeners[i];
                state.clearPending(i); // Delivery is admitted here, before close can win.
            }
            try {
                listener.onChange(event);
            } catch (Throwable ignored) {
                // One subscriber cannot fail the publisher or other subscribers.
            } finally {
                ((InnocuousThread) Thread.currentThread()).eraseThreadLocals();
            }
        }
    }
}
