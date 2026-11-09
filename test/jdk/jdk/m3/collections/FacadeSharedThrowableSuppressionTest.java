// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Cleanup callbacks may rethrow identical Throwables: keep the original primary. */
//FLLDCIM: RELOCATE - donor blob c26720b9c57a at eaacff5931ef; recipe m3/collections/recipe/transient-facade-20261106/relocate.py v4
final class FacadeSharedThrowableSuppressionTest {
    private static final class Facade {
        long id;
        Facade(long id) { this.id = id; }
    }
    private static TransientFacadeStore.Recycler<Facade> recycler(Runnable scrub, Runnable rebind) {
        return new TransientFacadeStore.Recycler<>() {
            @Override public void scrub(Facade facade) {
                facade.id = -1L;
                scrub.run();
            }
            @Override public void rebind(Facade facade, long id) {
                rebind.run();
                facade.id = id;
            }
        };
    }
    private static TransientFacadeStore<Facade> owner(
            TransientFacadeStore.Recycler<Facade> recycler,
            TransientFacadeStore.Disposer<Facade> disposer) {
        return new TransientFacadeStore<>(Facade::new, disposer,
                TransientFacadeStore.DEFAULT_MODE, recycler, 4);
    }

    @Test void sameScrubAndDisposerExceptionIsPrimary() {
        IllegalStateException shared = new IllegalStateException("shared");
        AtomicInteger attempts = new AtomicInteger();
        try (var store = owner(recycler(() -> { throw shared; }, () -> { }),
                (id, facade) -> { attempts.incrementAndGet(); throw shared; })) {
            assertSame(shared, assertThrows(IllegalStateException.class,
                    () -> store.withInternal(1, f -> { })));
            assertEquals(1, attempts.get());
            assertEquals(0, shared.getSuppressed().length);
            assertEquals(0, store.reusableIdleCount());
            assertEquals(0L, store.stats().disposals());
        }
    }

    @Test void sameRebindAndDisposerExceptionRejectsReuseWithoutConsumerAction() {
        IllegalStateException shared = new IllegalStateException("rebind");
        AtomicInteger calls = new AtomicInteger();
        try (var store = owner(recycler(() -> { }, () -> { throw shared; }),
                (id, facade) -> { throw shared; })) {
            store.withInternal(1, f -> calls.incrementAndGet());
            assertEquals(1, store.reusableIdleCount());
            assertSame(shared, assertThrows(IllegalStateException.class,
                    () -> store.withInternal(2, f -> calls.incrementAndGet())));
            assertEquals(1, calls.get());
            assertEquals(0, store.reusableIdleCount());
            assertEquals(0L, store.reuseCount());
            assertEquals(0, shared.getSuppressed().length);
        }
    }

    @Test void sameCallerAndScrubExceptionPreservesCallerAndDisposesFacade() {
        IllegalStateException shared = new IllegalStateException("caller");
        AtomicInteger disposed = new AtomicInteger();
        try (var store = owner(recycler(() -> { throw shared; }, () -> { }),
                (id, facade) -> disposed.incrementAndGet())) {
            assertSame(shared, assertThrows(IllegalStateException.class,
                    () -> store.withInternal(3, f -> { throw shared; })));
            assertEquals(1, disposed.get());
            assertEquals(0, shared.getSuppressed().length);
            assertEquals(0, store.reusableIdleCount());
        }
    }

    @Test void sharedErrorDuringActionScrubDisposerKeepsExactErrorIdentity() {
        AssertionError shared = new AssertionError("same Error");
        try (var store = owner(recycler(() -> { throw shared; }, () -> { }),
                (id, facade) -> { throw shared; })) {
            assertSame(shared, assertThrows(AssertionError.class,
                    () -> store.withInternal(4, f -> { throw shared; })));
            assertEquals(0, shared.getSuppressed().length);
            assertEquals(0, store.reusableIdleCount());
        }
    }

    @Test void distinctFailuresRetainNestedSuppressionOrder() {
        IllegalStateException caller = new IllegalStateException("caller");
        IllegalArgumentException scrub = new IllegalArgumentException("scrub");
        UnsupportedOperationException disposal = new UnsupportedOperationException("disposal");
        try (var store = owner(recycler(() -> { throw scrub; }, () -> { }),
                (id, facade) -> { throw disposal; })) {
            assertSame(caller, assertThrows(IllegalStateException.class,
                    () -> store.withInternal(5, f -> { throw caller; })));
            assertEquals(1, caller.getSuppressed().length);
            assertSame(scrub, caller.getSuppressed()[0]);
            assertEquals(1, scrub.getSuppressed().length);
            assertSame(disposal, scrub.getSuppressed()[0]);
        }
    }

    @Test void legacyConstructorKeepsDisposerPrecedence() {
        IllegalStateException caller = new IllegalStateException("caller");
        IllegalArgumentException disposal = new IllegalArgumentException("disposal");
        try (var store = new TransientFacadeStore<>(Facade::new,
                (id, facade) -> { throw disposal; }, TransientFacadeStore.DEFAULT_MODE)) {
            assertSame(disposal, assertThrows(IllegalArgumentException.class,
                    () -> store.withInternal(6, f -> { throw caller; })));
        }
    }
}
