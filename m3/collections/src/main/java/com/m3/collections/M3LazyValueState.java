// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: callback mutation guards and coherent lazy state after observer failures.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CancellationException;

/** Optional state lane; values remain exclusively in the incumbent tree's payload array. */
final class M3LazyValueState<K, V> {
    private static final short[] EMPTY = new short[0];
    private final M3LazyValueProvider<? super K, ? extends V> provider;
    private short[] flags = EMPTY;
    private int activeCalls;

    M3LazyValueState(M3LazyValueProvider<? super K, ? extends V> provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    static M3Progress progress(M3Progress monitor) {
        return monitor == null
                ? M3ProgressAdapters.asLegacy(M3SystemProgress.split(0L))
                : monitor;
    }

    static void checkCancelled(M3Progress monitor) {
        if (monitor.isCanceled()) { throw new CancellationException("lazy value load cancelled"); }
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
        writable(); flags[slot] = loaded ? M3LazyValueProvider.LOADED : M3LazyValueProvider.UNLOADED;
    }
    void erased(int slot) { writable(); flags[slot] = M3LazyValueProvider.UNLOADED; }
    void cleared() { writable(); Arrays.fill(flags, M3LazyValueProvider.UNLOADED); }

    V read(int slot, K key, Object[] values, M3Progress monitor) {
        return read(slot, key, values, M3LazyOperation.READ, null, monitor);
    }

    V read(
            int slot,
            K key,
            Object[] values,
            M3LazyOperation operation,
            M3LazyEventPolicy invocationPolicy,
            M3Progress monitor) {
        // Keep the canonical slot stable through policy, monitor and observer callbacks too.
        activeCalls++;
        try {
            return readGuarded(slot, key, values, operation, invocationPolicy, monitor);
        } finally {
            activeCalls--;
        }
    }

    private V readGuarded(
            int slot,
            K key,
            Object[] values,
            M3LazyOperation operation,
            M3LazyEventPolicy invocationPolicy,
            M3Progress monitor) {
        Objects.requireNonNull(operation, "operation");
        M3LazyEventPolicy policy = eventPolicy(invocationPolicy);
        try (M3ProgressScope scope = M3ProgressScope.resolve(monitor, 1L)) {
            M3Progress active = scope.monitor();
            short initial = flags[slot];
            boolean cancellationEventEmitted = false;
            emit(policy, M3LazyEventBits.BEFORE_TOUCH, M3LazyEventGranularity.ELEMENT,
                    slot, initial, initial, operation, active, null);
            try {
                checkCancelled(active);
                if (flags[slot] == M3LazyValueProvider.LOADED) {
                    emit(policy, M3LazyEventBits.CACHE_HIT, M3LazyEventGranularity.ELEMENT,
                            slot, flags[slot], flags[slot], operation, active, null);
                    emit(policy, M3LazyEventBits.AFTER_TOUCH, M3LazyEventGranularity.ELEMENT,
                            slot, initial, flags[slot], operation, active, null);
                    return value(values, slot);
                }
                if (flags[slot] == M3LazyValueProvider.LOADING) {
                    throw new IllegalStateException("recursive lazy value load");
                }

                short oldState = flags[slot];
                flags[slot] = M3LazyValueProvider.LOADING;
                Throwable providerFailure = null;
                try {
                    emit(policy, M3LazyEventBits.CACHE_MISS, M3LazyEventGranularity.ELEMENT,
                            slot, oldState, flags[slot], operation, active, null);
                    emit(policy, M3LazyEventBits.STATE_CHANGED, M3LazyEventGranularity.STATE_TRANSITION,
                            slot, oldState, flags[slot], operation, active, null);
                    emit(policy, M3LazyEventBits.BEFORE_LOAD, M3LazyEventGranularity.OPERATION,
                            slot, oldState, flags[slot], operation, active, null);
                    emit(policy, M3LazyEventBits.PROVIDER_BEGIN, M3LazyEventGranularity.OPERATION,
                            slot, flags[slot], flags[slot], operation, active, null);

                    V result = provider.load(key, active);
                    checkCancelled(active);
                    values[slot] = result;
                    short loading = flags[slot];
                    flags[slot] = M3LazyValueProvider.LOADED;
                    emit(policy, M3LazyEventBits.STATE_CHANGED, M3LazyEventGranularity.STATE_TRANSITION,
                            slot, loading, flags[slot], operation, active, null);
                    emit(policy, M3LazyEventBits.AFTER_LOAD, M3LazyEventGranularity.OPERATION,
                            slot, loading, flags[slot], operation, active, null);
                    emit(policy, M3LazyEventBits.AFTER_TOUCH, M3LazyEventGranularity.ELEMENT,
                            slot, initial, flags[slot], operation, active, null);
                    return result;
                } catch (CancellationException canceled) {
                    providerFailure = canceled;
                    short loading = flags[slot];
                    if (loading != M3LazyValueProvider.LOADED) {
                        values[slot] = null;
                        flags[slot] = M3LazyValueProvider.FAILED;
                        emitFailure(policy, M3LazyEventBits.STATE_CHANGED, M3LazyEventGranularity.STATE_TRANSITION,
                                slot, loading, flags[slot], operation, active, canceled);
                    }
                    cancellationEventEmitted = true;
                    emitFailure(policy, M3LazyEventBits.CANCELLED, M3LazyEventGranularity.OPERATION,
                            slot, loading, flags[slot], operation, active, canceled);
                    throw canceled;
                } catch (RuntimeException | Error problem) {
                    providerFailure = problem;
                    short loading = flags[slot];
                    // A successfully published value stays loaded if an AFTER_* observer fails.
                    if (loading != M3LazyValueProvider.LOADED) {
                        values[slot] = null;
                        flags[slot] = M3LazyValueProvider.FAILED;
                        emitFailure(policy, M3LazyEventBits.STATE_CHANGED, M3LazyEventGranularity.STATE_TRANSITION,
                                slot, loading, flags[slot], operation, active, problem);
                        emitFailure(policy, M3LazyEventBits.LOAD_FAILED, M3LazyEventGranularity.OPERATION,
                                slot, loading, flags[slot], operation, active, problem);
                    }
                    throw problem;
                } finally {
                    if (providerFailure == null) {
                        emit(policy, M3LazyEventBits.PROVIDER_END, M3LazyEventGranularity.OPERATION,
                                slot, flags[slot], flags[slot], operation, active, null);
                    } else {
                        emitFailure(policy, M3LazyEventBits.PROVIDER_END, M3LazyEventGranularity.OPERATION,
                                slot, flags[slot], flags[slot], operation, active, providerFailure);
                    }
                }
            } catch (CancellationException canceled) {
                if (!cancellationEventEmitted) {
                    emitFailure(policy, M3LazyEventBits.CANCELLED, M3LazyEventGranularity.OPERATION,
                            slot, initial, flags[slot], operation, active, canceled);
                }
                throw canceled;
            }
        }
    }

    M3LazyEventPolicy eventPolicy(M3LazyEventPolicy invocationPolicy) {
        activeCalls++;
        try {
            return provider.lazyEventPolicy().overlay(invocationPolicy);
        } finally {
            activeCalls--;
        }
    }

    void emit(
            M3LazyEventPolicy policy,
            long eventBit,
            M3LazyEventGranularity granularity,
            long slot,
            short oldState,
            short newState,
            M3LazyOperation operation,
            M3Progress monitor,
            Throwable failure) {
        activeCalls++;
        try {
            M3LazyEventSink<M3Progress> sink = provider.lazyEventSink();
            M3LazyEvents.emit(
                    policy, sink, eventBit, granularity, slot, oldState, newState,
                    operation, monitor, failure);
        } finally {
            activeCalls--;
        }
    }

    /** Observation failures must not replace a provider's original exception. */
    private void emitFailure(
            M3LazyEventPolicy policy,
            long eventBit,
            M3LazyEventGranularity granularity,
            long slot,
            short oldState,
            short newState,
            M3LazyOperation operation,
            M3Progress monitor,
            Throwable failure) {
        try {
            emit(policy, eventBit, granularity, slot, oldState, newState, operation, monitor, failure);
        } catch (RuntimeException | Error observationFailure) {
            if (observationFailure != failure) {
                failure.addSuppressed(observationFailure);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <V> V value(Object[] values, int slot) { return (V) values[slot]; }

    void verifySlot(int slot, boolean live, Object[] values) {
        short state = flags[slot];
        if (!live && state != M3LazyValueProvider.UNLOADED
                || live && state != M3LazyValueProvider.UNLOADED && state != M3LazyValueProvider.LOADING
                && state != M3LazyValueProvider.LOADED && state != M3LazyValueProvider.FAILED
                || state != M3LazyValueProvider.LOADED && values[slot] != null) {
            throw new AssertionError("lazy slot ownership/state");
        }
    }
}
