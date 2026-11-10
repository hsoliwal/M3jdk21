/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.paradigm.context;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;

/**
 * Experimental async field invalidations over explicit observation coordinates.
 *
 * <p>Owners call the capture hooks after successful writes. Ordinary Java or JNI
 * stores are not intercepted. Names resolve once to a reusable field layout;
 * a layout has at most 64 fields. Handles belong to this runtime only, with zero
 * meaning unobserved. The runtime does not retain Java payload objects.
 *
 * <p>Dirty masks coalesce until a dedicated daemon propagates them through the
 * current graph. An edge observes every field of its child. A downstream change
 * sets its parent's content bit for that edge's field. Shared children, aliases
 * and cycles are allowed; events are invalidations, not a history or state snapshot.
 *
 * <p>Capture takes a shared graph lock and primitive atomic operations; it never
 * runs a listener. Structural operations take the exclusive lock, satisfying the
 * existing arena's release/growth quiescence contract. There is no per-write event
 * allocation or event queue: a bounded ready bitmap survives arbitrarily many
 * coalesced writes. Bitmap words are scanned per wave. This is not a wait-free API.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/event/AsyncObjectEvents.java}
 * blob {@code a774884745f4e0e3c25fa2be134615bd0779dff1} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class AsyncObjectEvents implements AutoCloseable {
    private static final VarHandle LONGS = MethodHandles.arrayElementVarHandle(long[].class);
    private static final Subscription[] NO_LISTENERS = new Subscription[0];

    private final IndexArena<FieldLayout> arena = new IndexArena<>();
    private final StampedLock graph = new StampedLock();
    private final AtomicBoolean pending = new AtomicBoolean();
    private final AtomicLong listenerFailures = new AtomicLong();
    private final Object idleMonitor = new Object();
    private final Consumer<Throwable> errorHandler;
    private final Thread dispatcher;
    private volatile boolean dispatching;
    private volatile boolean closed;
    private volatile Throwable dispatcherFailure;

    // Cached event coordinates refer to the existing arena's identity authority.
    // Every access to these live arrays is protected by the graph lock.
    private long[] handles = new long[0];
    private long[] referenceDirty = new long[0];
    private long[] contentDirty = new long[0];
    private long[] readyWords = new long[0];
    private long[] sequences = new long[0];
    private int[] firstIncoming = new int[0];
    private int[] firstOutgoing = new int[0];
    private Subscription[][] listeners = new Subscription[0][];

    // Event-specific reverse observation graph. Links encode edge index + 1.
    private int[] edgeParent = new int[0];
    private int[] edgeChild = new int[0];
    private int[] edgeField = new int[0];
    private int[] nextIncoming = new int[0];
    private int[] previousIncoming = new int[0];
    private int[] nextOutgoing = new int[0];
    private int[] previousOutgoing = new int[0];
    private int edgeHighWater;
    private int freeEdge;

    // Dispatcher-owned reusable work arrays. Structural writers never touch them.
    private int[] touched = new int[0];
    private long[] waveReferences = new long[0];
    private long[] waveContent = new long[0];
    private long[] waveHandles = new long[0];
    private long[] waveSequences = new long[0];
    private FieldLayout[] waveLayouts = new FieldLayout[0];
    private Subscription[][] waveListeners = new Subscription[0][];
    private int touchedCount;
    private ObjectEvent event;

    public AsyncObjectEvents() {
        this(failure -> { });
    }

    /** Error handlers run on the dispatcher; ordinary handler failures are counted and isolated. */
    public AsyncObjectEvents(Consumer<Throwable> errorHandler) {
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
        dispatcher = new Thread(this::run, "synexia-object-events");
        dispatcher.setDaemon(true);
        dispatcher.start();
    }

    /** Cold, immutable name-to-bit mapping shared by any number of registrations. */
    public static final class FieldLayout {
        private final String[] names;

        private FieldLayout(String[] names) {
            if (names.length == 0 || names.length > Long.SIZE) {
                throw new IllegalArgumentException("a prototype layout requires 1..64 fields");
            }
            this.names = names.clone();
            for (int i = 0; i < names.length; i++) {
                String name = Objects.requireNonNull(this.names[i], "field name");
                if (name.isBlank()) throw new IllegalArgumentException("blank field name");
                for (int j = 0; j < i; j++) {
                    if (name.equals(this.names[j])) throw new IllegalArgumentException("duplicate field: " + name);
                }
            }
        }

        public static FieldLayout of(String... names) {
            return new FieldLayout(Objects.requireNonNull(names, "names"));
        }

        public int size() { return names.length; }
        public long allMask() { return names.length == 64 ? -1L : (1L << names.length) - 1L; }

        public String name(int field) {
            Objects.checkIndex(field, names.length);
            return names[field];
        }

        public int fieldId(String name) {
            Objects.requireNonNull(name, "name");
            for (int i = 0; i < names.length; i++) if (names[i].equals(name)) return i;
            throw new IllegalArgumentException("unknown field: " + name);
        }

        public long mask(String... fields) {
            long mask = 0L;
            for (String field : Objects.requireNonNull(fields, "fields")) mask |= 1L << fieldId(field);
            return mask;
        }
    }

    public long register(FieldLayout layout) {
        Objects.requireNonNull(layout, "layout");
        long stamp = graph.writeLock();
        try {
            requireOpen();
            long handle = arena.allocate(0L, 0L);
            ensureNodeCapacity(arena.capacity());
            int slot = ArenaHandle.slot(handle);
            handles[slot] = handle;
            sequences[slot] = 0L;
            listeners[slot] = NO_LISTENERS;
            arena.value(handle, layout);
            return handle;
        } finally {
            graph.unlockWrite(stamp);
        }
    }

    public Subscription subscribe(long handle, long fieldMask, ObjectListener listener) {
        Objects.requireNonNull(listener, "listener");
        long stamp = graph.writeLock();
        try {
            requireOpen();
            int slot = checkedSlot(handle);
            if (fieldMask == 0L || (fieldMask & ~arena.value(handle).allMask()) != 0L) {
                throw new IllegalArgumentException("subscription mask must select fields in this layout");
            }
            Subscription subscription = new Subscription(this, handle, fieldMask, listener);
            Subscription[] before = listeners[slot];
            Subscription[] after = Arrays.copyOf(before, before.length + 1);
            after[before.length] = subscription;
            listeners[slot] = after;
            return subscription;
        } finally {
            graph.unlockWrite(stamp);
        }
    }

    /**
     * Replace one observed reference edge and invalidate its parent in the same
     * topology operation. Actual payload assignment remains the owner's operation.
     * The initial invalidation covers changes that preceded attaching the child.
     */
    public boolean bind(long parent, int field, long child) {
        long stamp = graph.writeLock();
        try {
            requireOpen();
            int parentSlot = checkedSlot(parent);
            long bit = fieldBit(parent, field);
            int childSlot = child == 0L ? -1 : checkedSlot(child);
            int previous = findEdge(parentSlot, field);
            if (previous == 0 && childSlot == -1) return false;
            if (previous != 0 && edgeChild[previous - 1] == childSlot) return false;
            if (previous != 0) removeEdge(previous - 1);
            if (childSlot >= 0) addEdge(parentSlot, field, childSlot);
            record(parentSlot, bit, 0L);
            return true;
        } finally {
            graph.unlockWrite(stamp);
        }
    }

    /** Capture content invalidation after a successful owner write; stale handles are ignored. */
    public boolean changed(long handle, int field) {
        if (handle == 0L) return false;
        long stamp = graph.readLock();
        try {
            requireOpen();
            int slot = liveSlot(handle);
            if (slot < 0) return false;
            record(slot, 0L, fieldBit(handle, field));
            return true;
        } finally {
            graph.unlockRead(stamp);
        }
    }

    /**
     * Capture identity replacement without retaining either reference. For a field
     * with an observed child edge, use bind instead so the graph changes as well.
     */
    public boolean referenceChanged(long handle, int field, Object before, Object after) {
        if (handle == 0L || before == after) return false;
        long stamp = graph.readLock();
        try {
            requireOpen();
            int slot = liveSlot(handle);
            if (slot < 0) return false;
            fieldBit(handle, field);
            if (findEdge(slot, field) != 0) {
                throw new IllegalStateException("use bind to replace an observed child edge");
            }
            record(slot, 1L << field, 0L);
            return true;
        } finally {
            graph.unlockRead(stamp);
        }
    }

    /** Release observation, detach its edges, invalidate incoming parents, and cancel its listeners. */
    public boolean release(long handle) {
        long stamp = graph.writeLock();
        try {
            requireOpen();
            int slot = liveSlot(handle);
            if (slot < 0) return false;
            while (firstOutgoing[slot] != 0) removeEdge(firstOutgoing[slot] - 1);
            while (firstIncoming[slot] != 0) {
                int edge = firstIncoming[slot] - 1;
                record(edgeParent[edge], 0L, 1L << edgeField[edge]);
                removeEdge(edge);
            }
            for (Subscription subscription : listeners[slot]) subscription.deactivate();
            listeners[slot] = NO_LISTENERS;
            handles[slot] = 0L;
            referenceDirty[slot] = 0L;
            contentDirty[slot] = 0L;
            LONGS.getAndBitwiseAnd(readyWords, slot >>> 6, ~(1L << slot));
            arena.release(handle);
            return true;
        } finally {
            graph.unlockWrite(stamp);
        }
    }

    public long listenerFailureCount() { return listenerFailures.get(); }

    /**
     * Wait for current queued work and callback-generated work to become idle.
     * Producers must be externally quiescent for a stable drain boundary.
     * Returns false on timeout or cancellation; does not define a payload snapshot.
     */
    public boolean awaitIdle(Duration timeout) throws InterruptedException {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative()) throw new IllegalArgumentException("negative timeout");
        if (Thread.currentThread() == dispatcher) throw new IllegalStateException("dispatcher cannot await itself");
        long deadline = System.nanoTime() + timeout.toNanos();
        synchronized (idleMonitor) {
            for (;;) {
                if (dispatcherFailure != null) throw new IllegalStateException("dispatcher failed", dispatcherFailure);
                if (closed) return false;
                if (!dispatching && !pending.get()) return true;
                long left = deadline - System.nanoTime();
                if (left <= 0L) return false;
                idleMonitor.wait(left / 1_000_000L, (int) (left % 1_000_000L));
            }
        }
    }

    /** Cancellation without joining the dispatcher; awaitIdle first when delivery is required. */
    @Override
    public void close() {
        long stamp = graph.writeLock();
        try {
            if (closed) return;
            closed = true;
            for (int slot = 0; slot < listeners.length; slot++) {
                Subscription[] current = listeners[slot];
                if (current != null) for (Subscription subscription : current) subscription.deactivate();
                listeners[slot] = NO_LISTENERS;
            }
            pending.set(false);
        } finally {
            graph.unlockWrite(stamp);
        }
        LockSupport.unpark(dispatcher);
        notifyIdle();
    }

    public boolean awaitTermination(Duration timeout) throws InterruptedException {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative()) throw new IllegalArgumentException("negative timeout");
        if (Thread.currentThread() == dispatcher) return false;
        return dispatcher.join(timeout);
    }

    public static final class Subscription implements AutoCloseable {
        private final AsyncObjectEvents owner;
        private final long handle;
        private final long fieldMask;
        private volatile ObjectListener listener;

        private Subscription(AsyncObjectEvents owner, long handle, long fieldMask, ObjectListener listener) {
            this.owner = owner;
            this.handle = handle;
            this.fieldMask = fieldMask;
            this.listener = listener;
        }

        private void deactivate() { listener = null; }

        /** A callback already selected by the dispatcher may complete. */
        @Override
        public void close() {
            long stamp = owner.graph.writeLock();
            try {
                if (listener == null) return;
                deactivate();
                int slot = owner.liveSlot(handle);
                if (slot < 0) return;
                Subscription[] before = owner.listeners[slot];
                for (int i = 0; i < before.length; i++) {
                    if (before[i] != this) continue;
                    Subscription[] after = new Subscription[before.length - 1];
                    System.arraycopy(before, 0, after, 0, i);
                    System.arraycopy(before, i + 1, after, i, before.length - i - 1);
                    owner.listeners[slot] = after.length == 0 ? NO_LISTENERS : after;
                    break;
                }
            } finally {
                owner.graph.unlockWrite(stamp);
            }
        }
    }

    private void record(int slot, long references, long content) {
        if (references != 0L) LONGS.getAndBitwiseOr(referenceDirty, slot, references);
        if (content != 0L) LONGS.getAndBitwiseOr(contentDirty, slot, content);
        // Publication order matters: masks first, ready bit second, wakeup last.
        LONGS.getAndBitwiseOr(readyWords, slot >>> 6, 1L << slot);
        if (!pending.getAndSet(true)) LockSupport.unpark(dispatcher);
    }

    private void run() {
        try {
            while (!closed) {
                // Set active before clearing pending, closing the idle-observer gap.
                setDispatching(true);
                if (pending.getAndSet(false)) drainAndDeliver();
                setDispatching(false);
                Thread.interrupted();
                if (!pending.get() && !closed) LockSupport.park(this);
            }
        } catch (Throwable failure) {
            dispatcherFailure = failure;
            close();
        } finally {
            clearWave();
            setDispatching(false);
        }
    }

    private void drainAndDeliver() {
        clearWave();
        long stamp = graph.readLock();
        try {
            if (closed) return;
            ensureWaveCapacity(handles.length);
            for (int word = 0; word < readyWords.length; word++) {
                // Clear ready before taking masks: concurrent producers can only
                // leave redundant readiness, never an unannounced dirty mask.
                long ready = (long) LONGS.getAndSet(readyWords, word, 0L);
                while (ready != 0L) {
                    int slot = (word << 6) + Long.numberOfTrailingZeros(ready);
                    ready &= ready - 1L;
                    long references = (long) LONGS.getAndSet(referenceDirty, slot, 0L);
                    long content = (long) LONGS.getAndSet(contentDirty, slot, 0L);
                    if ((references | content) == 0L || handles[slot] == 0L) continue;
                    touch(slot);
                    waveReferences[slot] |= references;
                    waveContent[slot] |= content;
                }
            }
            // Every affected node is expanded once. Every incoming edge still
            // contributes its own parent field, including aliases and cycles.
            for (int cursor = 0; cursor < touchedCount; cursor++) {
                int child = touched[cursor];
                for (int link = firstIncoming[child]; link != 0; link = nextIncoming[link - 1]) {
                    int edge = link - 1;
                    int parent = edgeParent[edge];
                    touch(parent);
                    waveContent[parent] |= 1L << edgeField[edge];
                }
            }
            for (int i = 0; i < touchedCount; i++) {
                int slot = touched[i];
                waveHandles[slot] = handles[slot];
                waveSequences[slot] = ++sequences[slot];
                waveLayouts[slot] = arena.value(handles[slot]);
                waveListeners[slot] = listeners[slot];
            }
        } finally {
            graph.unlockRead(stamp);
        }
        for (int i = 0; i < touchedCount && !closed; i++) {
            int slot = touched[i];
            for (Subscription subscription : waveListeners[slot]) {
                ObjectListener listener = subscription.listener;
                if (listener == null || closed) continue;
                long references = waveReferences[slot] & subscription.fieldMask;
                long content = waveContent[slot] & subscription.fieldMask;
                if ((references | content) == 0L) continue;
                if (event == null) event = new ObjectEvent(dispatcher);
                event.begin(waveHandles[slot], references, content, waveSequences[slot], waveLayouts[slot]);
                try {
                    listener.onChange(event);
                } catch (Throwable failure) {
                    reportFailure(failure);
                } finally {
                    event.end();
                    // A callback cannot cancel or contaminate this shared worker
                    // by leaving its interrupt flag set. close owns cancellation.
                    Thread.interrupted();
                }
            }
        }
        clearWave();
    }

    private void reportFailure(Throwable failure) {
        rethrowFatal(failure);
        listenerFailures.incrementAndGet();
        try {
            errorHandler.accept(failure);
        } catch (Throwable handlerFailure) {
            rethrowFatal(handlerFailure);
            listenerFailures.incrementAndGet();
        }
    }

    // Preserve fatal ThreadDeath handling if a caller throws this legacy type.
    @SuppressWarnings("removal")
    private static void rethrowFatal(Throwable failure) {
        if (failure instanceof VirtualMachineError fatal) throw fatal;
        if (failure instanceof ThreadDeath fatal) throw fatal;
    }

    private void notifyIdle() {
        synchronized (idleMonitor) { idleMonitor.notifyAll(); }
    }

    private void setDispatching(boolean active) {
        // The idle predicate must not straddle callback completion and a newly
        // published reentrant wave. Volatile flags alone are not an atomic pair.
        synchronized (idleMonitor) {
            dispatching = active;
            if (!active) idleMonitor.notifyAll();
        }
    }

    private void touch(int slot) {
        if ((waveReferences[slot] | waveContent[slot]) == 0L) touched[touchedCount++] = slot;
    }

    private void clearWave() {
        for (int i = 0; i < touchedCount; i++) {
            int slot = touched[i];
            waveReferences[slot] = 0L;
            waveContent[slot] = 0L;
            waveLayouts[slot] = null;
            waveListeners[slot] = null;
        }
        touchedCount = 0;
    }

    private int liveSlot(long handle) {
        int slot = (int) handle - 1;
        if (handle == 0L || slot < 0 || slot >= handles.length || handles[slot] != handle) return -1;
        return arena.alive(handle) ? slot : -1;
    }

    private int checkedSlot(long handle) {
        int slot = liveSlot(handle);
        if (slot < 0) throw new IllegalArgumentException("invalid or released observation handle");
        return slot;
    }

    private long fieldBit(long handle, int field) {
        Objects.checkIndex(field, arena.value(handle).size());
        return 1L << field;
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("object event runtime is closed", dispatcherFailure);
    }

    private int findEdge(int parent, int field) {
        for (int link = firstOutgoing[parent]; link != 0; link = nextOutgoing[link - 1]) {
            if (edgeField[link - 1] == field) return link;
        }
        return 0;
    }

    private void addEdge(int parent, int field, int child) {
        int edge;
        if (freeEdge != 0) {
            edge = freeEdge - 1;
            freeEdge = nextOutgoing[edge];
        } else {
            ensureEdgeCapacity(Math.addExact(edgeHighWater, 1));
            edge = edgeHighWater++;
        }
        edgeParent[edge] = parent;
        edgeChild[edge] = child;
        edgeField[edge] = field;
        previousIncoming[edge] = 0;
        nextIncoming[edge] = firstIncoming[child];
        if (firstIncoming[child] != 0) previousIncoming[firstIncoming[child] - 1] = edge + 1;
        firstIncoming[child] = edge + 1;
        previousOutgoing[edge] = 0;
        nextOutgoing[edge] = firstOutgoing[parent];
        if (firstOutgoing[parent] != 0) previousOutgoing[firstOutgoing[parent] - 1] = edge + 1;
        firstOutgoing[parent] = edge + 1;
    }

    private void removeEdge(int edge) {
        int before = previousIncoming[edge];
        int after = nextIncoming[edge];
        if (before == 0) firstIncoming[edgeChild[edge]] = after;
        else nextIncoming[before - 1] = after;
        if (after != 0) previousIncoming[after - 1] = before;
        before = previousOutgoing[edge];
        after = nextOutgoing[edge];
        if (before == 0) firstOutgoing[edgeParent[edge]] = after;
        else nextOutgoing[before - 1] = after;
        if (after != 0) previousOutgoing[after - 1] = before;
        nextOutgoing[edge] = freeEdge;
        freeEdge = edge + 1;
    }

    private void ensureNodeCapacity(int needed) {
        if (needed <= handles.length) return;
        handles = Arrays.copyOf(handles, needed);
        referenceDirty = Arrays.copyOf(referenceDirty, needed);
        contentDirty = Arrays.copyOf(contentDirty, needed);
        readyWords = Arrays.copyOf(readyWords, (int) ((needed + 63L) >>> 6));
        sequences = Arrays.copyOf(sequences, needed);
        firstIncoming = Arrays.copyOf(firstIncoming, needed);
        firstOutgoing = Arrays.copyOf(firstOutgoing, needed);
        listeners = Arrays.copyOf(listeners, needed);
    }

    private void ensureEdgeCapacity(int needed) {
        if (needed <= edgeParent.length) return;
        int capacity = Math.max(needed, Math.max(8, Math.multiplyExact(edgeParent.length, 2)));
        edgeParent = Arrays.copyOf(edgeParent, capacity);
        edgeChild = Arrays.copyOf(edgeChild, capacity);
        edgeField = Arrays.copyOf(edgeField, capacity);
        nextIncoming = Arrays.copyOf(nextIncoming, capacity);
        previousIncoming = Arrays.copyOf(previousIncoming, capacity);
        nextOutgoing = Arrays.copyOf(nextOutgoing, capacity);
        previousOutgoing = Arrays.copyOf(previousOutgoing, capacity);
    }

    private void ensureWaveCapacity(int needed) {
        if (needed <= touched.length) return;
        touched = new int[needed];
        waveReferences = new long[needed];
        waveContent = new long[needed];
        waveHandles = new long[needed];
        waveSequences = new long[needed];
        waveLayouts = new FieldLayout[needed];
        waveListeners = new Subscription[needed][];
    }
}
