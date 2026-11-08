/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.bits.LazyEventBits;

/** Independent receiver regressions for caller code crossing a lazy slot boundary. */
final class M3LazyObserverRegressionTest {
    @Test void successfulValueRemainsCoherentWhenCompletionObserverThrows() {
        for (long bit : new long[] {LazyEventBits.AFTER_LOAD, LazyEventBits.AFTER_TOUCH,
                LazyEventBits.PROVIDER_END}) {
            AtomicBoolean first = new AtomicBoolean(true);
            AtomicInteger calls = new AtomicInteger();
            RuntimeException observer = new IllegalArgumentException("observer-" + bit);
            var provider = new LazyValueProvider<Integer, String>() {
                @Override public String load(Integer key, M3Progress monitor) {
                    assertNotNull(monitor); calls.incrementAndGet(); return "v" + key;
                }
                @Override public LazyEventPolicy lazyEventPolicy() {
                    return LazyEventPolicy.of(bit, LazyEventGranularity.STATE_TRANSITION);
                }
                @Override public M3LazyEventSink<M3Progress> lazyEventSink() {
                    return (event, slot, oldState, newState, operation, monitor, failure) -> {
                        if (first.getAndSet(false)) { throw observer; }
                    };
                }
            };
            var map = new M3TreeMap<Integer, String>(null, provider);
            assertTrue(map.defer(1));
            assertSame(observer, assertThrows(RuntimeException.class,
                    () -> map.touch(1, M3Progress.NULL)));
            map.verifyInvariants();
            assertEquals(LazyValueProvider.LOADED, map.valueFlags(1));
            assertEquals("v1", map.touch(1, M3Progress.NULL));
            assertEquals(1, calls.get(), "observer failure must not repeat completed provider work");
            map.verifyInvariants();
        }
    }

    @Test void failureObserversCannotReplaceProviderExceptionOrPoisonRetry() {
        RuntimeException providerFailure = new IllegalArgumentException("provider");
        RuntimeException observerFailure = new IllegalStateException("failure observer");
        RuntimeException finalObserverFailure = new IllegalStateException("final observer");
        AtomicInteger calls = new AtomicInteger();
        var provider = new LazyValueProvider<Integer, String>() {
            @Override public String load(Integer key, M3Progress monitor) {
                if (calls.getAndIncrement() == 0) { throw providerFailure; }
                return "v" + key;
            }
            @Override public LazyEventPolicy lazyEventPolicy() {
                return LazyEventPolicy.of(LazyEventBits.LOAD_FAILED | LazyEventBits.PROVIDER_END,
                        LazyEventGranularity.STATE_TRANSITION);
            }
            @Override public M3LazyEventSink<M3Progress> lazyEventSink() {
                return (event, slot, oldState, newState, operation, monitor, failure) -> {
                    if (failure == null) { return; }
                    if (event == LazyEventBits.LOAD_FAILED) { throw observerFailure; }
                    throw finalObserverFailure;
                };
            }
        };
        var map = new M3TreeMap<Integer, String>(null, provider); map.defer(7);
        assertSame(providerFailure, assertThrows(RuntimeException.class,
                () -> map.touch(7, M3Progress.NULL)));
        assertEquals(List.of(observerFailure, finalObserverFailure),
                List.of(providerFailure.getSuppressed()));
        assertEquals(LazyValueProvider.FAILED, map.valueFlags(7));
        map.verifyInvariants();
        assertEquals("v7", map.touch(7, M3Progress.NULL));
        assertEquals(2, calls.get());
        map.verifyInvariants();
    }

    @Test void beforeTouchObserverCannotRetargetRecycledSlot() {
        AtomicReference<M3TreeMap<Integer, String>> owner = new AtomicReference<>();
        AtomicBoolean first = new AtomicBoolean(true);
        var provider = new LazyValueProvider<Integer, String>() {
            @Override public String load(Integer key, M3Progress monitor) { return "v" + key; }
            @Override public LazyEventPolicy lazyEventPolicy() {
                return LazyEventPolicy.of(LazyEventBits.BEFORE_TOUCH, LazyEventGranularity.ELEMENT);
            }
            @Override public M3LazyEventSink<M3Progress> lazyEventSink() {
                return (event, slot, oldState, newState, operation, monitor, failure) -> {
                    if (first.getAndSet(false)) { owner.get().clear(); owner.get().defer(2); }
                };
            }
        };
        var map = new M3TreeMap<Integer, String>(null, provider); owner.set(map); map.defer(1);
        assertThrows(IllegalStateException.class, () -> map.touch(1, M3Progress.NULL));
        assertEquals(List.of(1), new ArrayList<>(map.keySet()));
        assertEquals(LazyValueProvider.UNLOADED, map.valueFlags(1));
        assertEquals("v1", map.touch(1, M3Progress.NULL));
        assertNull(map.get(2));
        map.verifyInvariants();
    }

    @Test void rangeAndCachedValueObserversShareMutationGuard() {
        for (long bit : new long[] {LazyEventBits.RANGE_BEGIN, LazyEventBits.CACHE_HIT,
                LazyEventBits.PROVIDER_END}) {
            AtomicReference<M3TreeMap<Integer, String>> owner = new AtomicReference<>();
            AtomicBoolean armed = new AtomicBoolean(false);
            var provider = new LazyValueProvider<Integer, String>() {
                @Override public String load(Integer key, M3Progress monitor) { return "v" + key; }
                @Override public LazyEventPolicy lazyEventPolicy() {
                    return LazyEventPolicy.of(bit, LazyEventGranularity.STATE_TRANSITION);
                }
                @Override public M3LazyEventSink<M3Progress> lazyEventSink() {
                    return (event, slot, oldState, newState, operation, monitor, failure) -> {
                        if (armed.getAndSet(false)) { owner.get().clear(); }
                    };
                }
            };
            var map = new M3TreeMap<Integer, String>(null, provider); owner.set(map);
            map.defer(1);
            if (bit == LazyEventBits.CACHE_HIT) { assertEquals("v1", map.touch(1, M3Progress.NULL)); }
            armed.set(true);
            assertThrows(IllegalStateException.class, () -> {
                if (bit == LazyEventBits.RANGE_BEGIN) { map.preload(M3Progress.NULL); }
                else { map.touch(1, M3Progress.NULL); }
            });
            assertEquals(List.of(1), new ArrayList<>(map.keySet()));
            map.verifyInvariants();
            assertEquals("v1", map.touch(1, M3Progress.NULL));
        }
    }

    @Test void deferredKeysPreserveNullFailureCancellationAndViewOrder() {
        var order = new ArrayList<Integer>();
        AtomicBoolean fail = new AtomicBoolean(true);
        var map = new M3TreeMap<Integer, String>(null, (key, monitor) -> {
            assertNotNull(monitor); order.add(key);
            if (key == 2 && fail.getAndSet(false)) { throw new IllegalStateException("retry"); }
            return key == 1 ? null : "v" + key;
        });
        for (int i = 1; i <= 4; i++) { assertTrue(map.defer(i)); }
        assertFalse(map.defer(1)); assertNull(map.get(99)); assertTrue(order.isEmpty());
        M3Progress progress = M3Progress.of(3);
        assertThrows(IllegalStateException.class, () -> map.subMap(1, true, 3, true).preload(progress));
        assertEquals(List.of(1, 2), order);
        assertTrue(map.isValueLoaded(1)); assertNull(map.get(1));
        assertEquals(LazyValueProvider.FAILED, map.valueFlags(2));
        assertEquals(LazyValueProvider.UNLOADED, map.valueFlags(3));
        assertEquals(1, progress.getWorkedSoFar()); assertFalse(progress.isDone());
        order.clear(); map.subMap(1, true, 3, true).descendingMap().preload(M3Progress.NULL);
        assertEquals(List.of(3, 2), order);
        assertEquals(LazyValueProvider.UNLOADED, map.valueFlags(4));
        M3Progress canceled = M3Progress.of(1); canceled.cancel();
        assertThrows(CancellationException.class, () -> map.touch(4, canceled));
        assertEquals(LazyValueProvider.UNLOADED, map.valueFlags(4));
        map.verifyInvariants();
    }
}
