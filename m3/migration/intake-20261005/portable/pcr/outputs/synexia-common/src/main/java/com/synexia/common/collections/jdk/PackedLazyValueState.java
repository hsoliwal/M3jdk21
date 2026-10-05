// SPDX-License-Identifier: Apache-2.0
package com.synexia.common.collections.jdk;

import com.synexia.common.lazy.event.LazyEventBits;
import com.synexia.common.lazy.event.LazyEventGranularity;
import com.synexia.common.lazy.event.LazyEventPolicy;
import com.synexia.common.lazy.event.LazyEventSink;
import com.synexia.common.lazy.event.LazyEvents;
import com.synexia.common.lazy.event.LazyOperation;
import com.synexia.common.progress.ProgressAdapters;
import com.synexia.common.progress.ProgressScope;
import com.synexia.common.progress.SystemProgress;
import com.synexia.common.utils.Progress;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CancellationException;

/** Optional state lane; values remain exclusively in the incumbent tree's payload array. */
final class PackedLazyValueState<K, V> {
    private static final short[] EMPTY = new short[0];
    private final LazyValueProvider<? super K, ? extends V> provider;
    private short[] flags = EMPTY;
    private int activeCalls;
    // Retained only to the greatest read depth, independently of the number of tree slots.
    private int[] activeSlots = SlotArrays.EMPTY_INTS;
    private int readDepth;

    PackedLazyValueState(LazyValueProvider<? super K, ? extends V> provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    static Progress progress(Progress monitor) {
        return monitor == null
                ? ProgressAdapters.asLegacy(SystemProgress.split(0L))
                : monitor;
    }

    static void checkCancelled(Progress monitor) {
        if (monitor == null ? SystemProgress.get().isCanceled() : monitor.isCanceled()) {
            throw new CancellationException("lazy value load cancelled");
        }
    }

    void writable() {
        if (activeCalls != 0) {
            throw new IllegalStateException("mapping mutation during lazy provider callback");
        }
    }

    // The tree allocates its other candidate lanes first; no fallible allocation follows this.
    void grow(int capacity) { writable(); flags = Arrays.copyOf(flags, capacity); }
    int capacity() { return flags.length; }
    short flags(int slot) { return flags[slot]; }
    void admitted(int slot, boolean loaded) {
        writable(); flags[slot] = loaded ? LazyValueProvider.LOADED : LazyValueProvider.UNLOADED;
    }
    void erased(int slot) { writable(); flags[slot] = LazyValueProvider.UNLOADED; }
    void cleared() { writable(); Arrays.fill(flags, LazyValueProvider.UNLOADED); }

    V read(int slot, K key, Object[] values, Progress monitor) {
        return read(slot, key, values, LazyOperation.READ, null, monitor);
    }

    V read(
            int slot,
            K key,
            Object[] values,
            LazyOperation operation,
            LazyEventPolicy invocationPolicy,
            Progress monitor) {
        Objects.requireNonNull(operation, "operation");
        if (activeCalls != 0 && flags[slot] == LazyValueProvider.LOADED) {
            // A callback's cached read is not another external touch or notification cycle.
            checkCancelled(monitor);
            return value(values, slot);
        }
        beginRead(slot);
        try {
            return readGuarded(slot, key, values, operation, invocationPolicy, monitor);
        } finally {
            readDepth--;
            activeCalls--;
        }
    }

    private void beginRead(int slot) {
        for (int depth = 0; depth < readDepth; depth++) {
            if (activeSlots[depth] == slot) {
                throw new IllegalStateException("recursive lazy value load");
            }
        }
        if (readDepth == activeSlots.length) {
            activeSlots = Arrays.copyOf(activeSlots, SlotArrays.grow(activeSlots.length));
        }
        activeSlots[readDepth++] = slot;
        activeCalls++;
    }

    private V readGuarded(
            int slot,
            K key,
            Object[] values,
            LazyOperation operation,
            LazyEventPolicy invocationPolicy,
            Progress monitor) {
        LazyEventPolicy policy = eventPolicy(invocationPolicy);
        try (ProgressScope scope = ProgressScope.resolve(monitor, 1L)) {
            Progress active = scope.monitor();
            short initial = flags[slot];
            boolean cancellationEventEmitted = false;
            emit(policy, LazyEventBits.BEFORE_TOUCH, LazyEventGranularity.ELEMENT,
                    slot, initial, initial, operation, active, null);
            try {
                checkCancelled(active);
                if (flags[slot] == LazyValueProvider.LOADED) {
                    emit(policy, LazyEventBits.CACHE_HIT, LazyEventGranularity.ELEMENT,
                            slot, flags[slot], flags[slot], operation, active, null);
                    emit(policy, LazyEventBits.AFTER_TOUCH, LazyEventGranularity.ELEMENT,
                            slot, initial, flags[slot], operation, active, null);
                    return value(values, slot);
                }
                if (flags[slot] == LazyValueProvider.LOADING) {
                    throw new IllegalStateException("recursive lazy value load");
                }

                short oldState = flags[slot];
                flags[slot] = LazyValueProvider.LOADING;
                Throwable providerFailure = null;
                try {
                    emit(policy, LazyEventBits.CACHE_MISS, LazyEventGranularity.ELEMENT,
                            slot, oldState, flags[slot], operation, active, null);
                    emit(policy, LazyEventBits.STATE_CHANGED, LazyEventGranularity.STATE_TRANSITION,
                            slot, oldState, flags[slot], operation, active, null);
                    emit(policy, LazyEventBits.BEFORE_LOAD, LazyEventGranularity.OPERATION,
                            slot, oldState, flags[slot], operation, active, null);
                    emit(policy, LazyEventBits.PROVIDER_BEGIN, LazyEventGranularity.OPERATION,
                            slot, flags[slot], flags[slot], operation, active, null);

                    V result = provider.load(key, active);
                    checkCancelled(active);
                    values[slot] = result;
                    short loading = flags[slot];
                    flags[slot] = LazyValueProvider.LOADED;
                    emit(policy, LazyEventBits.STATE_CHANGED, LazyEventGranularity.STATE_TRANSITION,
                            slot, loading, flags[slot], operation, active, null);
                    emit(policy, LazyEventBits.AFTER_LOAD, LazyEventGranularity.OPERATION,
                            slot, loading, flags[slot], operation, active, null);
                    emit(policy, LazyEventBits.AFTER_TOUCH, LazyEventGranularity.ELEMENT,
                            slot, initial, flags[slot], operation, active, null);
                    return result;
                } catch (CancellationException canceled) {
                    providerFailure = canceled;
                    short loading = flags[slot];
                    values[slot] = null;
                    flags[slot] = LazyValueProvider.FAILED;
                    emit(policy, LazyEventBits.STATE_CHANGED, LazyEventGranularity.STATE_TRANSITION,
                            slot, loading, flags[slot], operation, active, canceled);
                    cancellationEventEmitted = true;
                    emit(policy, LazyEventBits.CANCELLED, LazyEventGranularity.OPERATION,
                            slot, loading, flags[slot], operation, active, canceled);
                    throw canceled;
                } catch (RuntimeException | Error problem) {
                    providerFailure = problem;
                    short loading = flags[slot];
                    values[slot] = null;
                    flags[slot] = LazyValueProvider.FAILED;
                    emit(policy, LazyEventBits.STATE_CHANGED, LazyEventGranularity.STATE_TRANSITION,
                            slot, loading, flags[slot], operation, active, problem);
                    emit(policy, LazyEventBits.LOAD_FAILED, LazyEventGranularity.OPERATION,
                            slot, loading, flags[slot], operation, active, problem);
                    throw problem;
                } finally {
                    emit(policy, LazyEventBits.PROVIDER_END, LazyEventGranularity.OPERATION,
                            slot, flags[slot], flags[slot], operation, active, providerFailure);
                }
            } catch (CancellationException canceled) {
                if (!cancellationEventEmitted) {
                    emit(policy, LazyEventBits.CANCELLED, LazyEventGranularity.OPERATION,
                            slot, initial, flags[slot], operation, active, canceled);
                }
                throw canceled;
            }
        }
    }

    LazyEventPolicy eventPolicy(LazyEventPolicy invocationPolicy) {
        activeCalls++;
        try {
            return provider.lazyEventPolicy().overlay(invocationPolicy);
        } finally {
            activeCalls--;
        }
    }

    void emit(
            LazyEventPolicy policy,
            long eventBit,
            LazyEventGranularity granularity,
            long slot,
            short oldState,
            short newState,
            LazyOperation operation,
            Progress monitor,
            Throwable failure) {
        activeCalls++;
        try {
            LazyEventSink<Progress> sink = provider.lazyEventSink();
            LazyEvents.emit(
                    policy, sink, eventBit, granularity, slot, oldState, newState,
                    operation, monitor, failure);
        } catch (RuntimeException | Error notificationFailure) {
            if (failure == null) { throw notificationFailure; }
            if (notificationFailure != failure) { failure.addSuppressed(notificationFailure); }
        } finally {
            activeCalls--;
        }
    }

    @SuppressWarnings("unchecked")
    private static <V> V value(Object[] values, int slot) { return (V) values[slot]; }

    void verifySlot(int slot, boolean live, Object[] values) {
        short state = flags[slot];
        if (!live && state != LazyValueProvider.UNLOADED
                || live && state != LazyValueProvider.UNLOADED && state != LazyValueProvider.LOADING
                && state != LazyValueProvider.LOADED && state != LazyValueProvider.FAILED
                || state != LazyValueProvider.LOADED && values[slot] != null) {
            throw new AssertionError("lazy slot ownership/state");
        }
    }
}
