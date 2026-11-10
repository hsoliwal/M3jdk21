// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Copyright © 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.internal.m3.paradigm.context;

import static jdk.internal.m3.paradigm.context.AsyncObjectEvents.FieldLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Executable contracts for the explicit-publication, current-graph prototype. */
@Timeout(value = 20, unit = TimeUnit.SECONDS)
class AsyncObjectEventsTest {
    private static final Duration WAIT = Duration.ofSeconds(5);

    @Test
    void layoutResolvesNamesAndPreservesTheSixtyFourthBit() {
        String[] names = names(64);
        FieldLayout layout = FieldLayout.of(names);
        names[0] = "mutated input";
        assertEquals(64, layout.size());
        assertEquals("f0", layout.name(0));
        assertEquals(63, layout.fieldId("f63"));
        assertEquals(Long.MIN_VALUE, layout.mask("f63"));
        assertEquals(Long.MIN_VALUE | 1L, layout.mask("f0", "f63", "f0"));
        assertEquals(-1L, layout.allMask());
    }

    @Test
    void layoutRejectsInvalidOrAmbiguousFieldDefinitions() {
        assertThrows(IllegalArgumentException.class, () -> FieldLayout.of());
        assertThrows(IllegalArgumentException.class, () -> FieldLayout.of(names(65)));
        assertThrows(IllegalArgumentException.class, () -> FieldLayout.of("same", "same"));
        assertThrows(IllegalArgumentException.class, () -> FieldLayout.of(" "));
        assertThrows(NullPointerException.class, () -> FieldLayout.of((String[]) null));
        assertThrows(NullPointerException.class, () -> FieldLayout.of("good", null));
        FieldLayout layout = FieldLayout.of("known");
        assertThrows(IllegalArgumentException.class, () -> layout.fieldId("unknown"));
        assertThrows(IndexOutOfBoundsException.class, () -> layout.name(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> layout.name(1));
    }

    @Test
    void callbacksUseADifferentThreadAndOnlySelectedFields() throws Exception {
        try (Fixture fixture = new Fixture()) {
            var runtime = fixture.runtime;
            FieldLayout layout = FieldLayout.of("ignored", "selected");
            long handle = runtime.register(layout);
            Thread publisher = Thread.currentThread();
            AtomicReference<Thread> callbackThread = new AtomicReference<>();
            var events = new ConcurrentLinkedQueue<ObjectEvent.Snapshot>();
            runtime.subscribe(handle, layout.mask("selected"), event -> {
                callbackThread.set(Thread.currentThread());
                events.add(event.snapshot());
            });

            assertTrue(runtime.changed(handle, layout.fieldId("ignored")));
            fixture.idle();
            assertTrue(events.isEmpty());
            assertTrue(runtime.changed(handle, layout.fieldId("selected")));
            fixture.idle();

            ObjectEvent.Snapshot event = only(events);
            assertNotSame(publisher, callbackThread.get());
            assertEquals(handle, event.handle());
            assertSame(layout, event.layout());
            assertEquals(0L, event.referenceMask());
            assertEquals(layout.mask("selected"), event.contentMask());
            assertEquals(event.contentMask(), event.changedMask());
        }
    }

    @Test
    void blockedWorkerCoalescesTheUnionOfSelectedReferenceAndContentChanges() throws Exception {
        try (Fixture fixture = new Fixture()) {
            var runtime = fixture.runtime;
            FieldLayout layout = FieldLayout.of("reference", "content", "ignored");
            long handle = runtime.register(layout);
            var events = new ConcurrentLinkedQueue<ObjectEvent.Snapshot>();
            runtime.subscribe(handle, layout.mask("reference", "content"), event -> events.add(event.snapshot()));
            try (WorkerBarrier barrier = new WorkerBarrier(runtime)) {
                barrier.awaitEntered();
                assertTrue(runtime.referenceChanged(handle, 0, null, new Object()));
                for (int i = 0; i < 1_000; i++) {
                    assertTrue(runtime.changed(handle, 1));
                    assertTrue(runtime.changed(handle, 2));
                }
                assertTrue(events.isEmpty(), "the only dispatcher is held by the barrier");
                assertFalse(runtime.awaitIdle(Duration.ZERO));
            }
            fixture.idle();
            ObjectEvent.Snapshot event = only(events);
            assertEquals(layout.mask("reference"), event.referenceMask());
            assertEquals(layout.mask("content"), event.contentMask());
            assertEquals(layout.mask("reference", "content"), event.changedMask());
        }
    }

    @Test
    void equalButDistinctReferencesStillChangeAndIdenticalReferencesDoNot() throws Exception {
        try (Fixture fixture = new Fixture()) {
            var runtime = fixture.runtime;
            long handle = runtime.register(FieldLayout.of("value"));
            var events = new ConcurrentLinkedQueue<ObjectEvent.Snapshot>();
            runtime.subscribe(handle, 1L, event -> events.add(event.snapshot()));
            String before = new String("equal");
            String after = new String("equal");
            assertEquals(before, after);
            assertNotSame(before, after);
            assertFalse(runtime.referenceChanged(handle, 0, before, before));
            assertFalse(runtime.referenceChanged(handle, 0, null, null));
            fixture.idle();
            assertTrue(events.isEmpty());
            assertTrue(runtime.referenceChanged(handle, 0, before, after));
            fixture.idle();
            assertEquals(1L, only(events).referenceMask());
        }
    }

    @Test
    void downstreamChangesUnionAliasedParentFieldsThroughSeveralLevels() throws Exception {
        try (Fixture fixture = new Fixture()) {
            var runtime = fixture.runtime;
            FieldLayout parentLayout = FieldLayout.of("direct", "indirect");
            long parent = runtime.register(parentLayout);
            long intermediate = runtime.register(FieldLayout.of("child"));
            long child = runtime.register(FieldLayout.of("value"));
            assertTrue(runtime.bind(parent, 0, child));
            assertTrue(runtime.bind(parent, 1, intermediate));
            assertTrue(runtime.bind(intermediate, 0, child));
            fixture.idle();
            var parentEvents = new ConcurrentLinkedQueue<ObjectEvent.Snapshot>();
            var intermediateEvents = new ConcurrentLinkedQueue<ObjectEvent.Snapshot>();
            runtime.subscribe(parent, parentLayout.allMask(), event -> parentEvents.add(event.snapshot()));
            runtime.subscribe(intermediate, 1L, event -> intermediateEvents.add(event.snapshot()));
            try (WorkerBarrier barrier = new WorkerBarrier(runtime)) {
                barrier.awaitEntered();
                assertTrue(runtime.changed(child, 0));
            }
            fixture.idle();
            assertEquals(parentLayout.allMask(), contentUnion(parentEvents));
            assertEquals(0L, referenceUnion(parentEvents));
            assertEquals(1L, contentUnion(intermediateEvents));
        }
    }

    @Test
    void sharedChildNotifiesSeveralIndependentParents() throws Exception {
        try (Fixture fixture = new Fixture()) {
            var runtime = fixture.runtime;
            long child = runtime.register(FieldLayout.of("value"));
            long first = runtime.register(FieldLayout.of("child"));
            long second = runtime.register(FieldLayout.of("ignored", "child"));
            runtime.bind(first, 0, child);
            runtime.bind(second, 1, child);
            fixture.idle();
            AtomicLong firstSeen = new AtomicLong();
            AtomicLong secondSeen = new AtomicLong();
            runtime.subscribe(first, 1L, event -> firstSeen.getAndUpdate(mask -> mask | event.contentMask()));
            runtime.subscribe(second, 2L, event -> secondSeen.getAndUpdate(mask -> mask | event.contentMask()));
            runtime.changed(child, 0);
            fixture.idle();
            assertEquals(1L, firstSeen.get());
            assertEquals(2L, secondSeen.get());
        }
    }

    @Test
    void cyclesReachAFiniteFieldMaskFixedPoint() throws Exception {
        try (Fixture fixture = new Fixture()) {
            var runtime = fixture.runtime;
            FieldLayout layout = FieldLayout.of("peer", "value");
            long first = runtime.register(layout);
            long second = runtime.register(layout);
            runtime.bind(first, 0, second);
            runtime.bind(second, 0, first);
            fixture.idle();
            var firstEvents = new ConcurrentLinkedQueue<ObjectEvent.Snapshot>();
            var secondEvents = new ConcurrentLinkedQueue<ObjectEvent.Snapshot>();
            runtime.subscribe(first, layout.allMask(), event -> firstEvents.add(event.snapshot()));
            runtime.subscribe(second, layout.allMask(), event -> secondEvents.add(event.snapshot()));
            try (WorkerBarrier barrier = new WorkerBarrier(runtime)) {
                barrier.awaitEntered();
                runtime.changed(second, 1);
            }
            fixture.idle();
            assertEquals(1L, contentUnion(firstEvents));
            assertEquals(3L, contentUnion(secondEvents));
            assertEquals(0L, referenceUnion(firstEvents));
            assertEquals(0L, referenceUnion(secondEvents));
        }
    }

    @Test
    void rebindBeforeDispatchUsesTheCurrentGraphAndNeverInventsAChildContentEvent() throws Exception {
        try (Fixture fixture = new Fixture()) {
            var runtime = fixture.runtime;
            long parent = runtime.register(FieldLayout.of("child"));
            long oldChild = runtime.register(FieldLayout.of("value"));
            long newChild = runtime.register(FieldLayout.of("value"));
            assertTrue(runtime.bind(parent, 0, oldChild));
            assertFalse(runtime.bind(parent, 0, oldChild));
            fixture.idle();
            var events = new ConcurrentLinkedQueue<ObjectEvent.Snapshot>();
            runtime.subscribe(parent, 1L, event -> events.add(event.snapshot()));
            try (WorkerBarrier barrier = new WorkerBarrier(runtime)) {
                barrier.awaitEntered();
                runtime.changed(oldChild, 0);
                assertTrue(runtime.bind(parent, 0, newChild));
            }
            fixture.idle();
            assertEquals(1L, only(events).referenceMask());
            assertEquals(0L, only(events).contentMask());
            events.clear();
            runtime.changed(oldChild, 0);
            fixture.idle();
            assertTrue(events.isEmpty());
            runtime.changed(newChild, 0);
            fixture.idle();
            assertEquals(1L, only(events).contentMask());
            events.clear();
            assertTrue(runtime.bind(parent, 0, 0L));
            assertFalse(runtime.bind(parent, 0, 0L));
            fixture.idle();
            assertEquals(1L, only(events).referenceMask());
        }
    }

    @Test
    void releaseAndSlotReuseRejectStalePublicationsAndQueuedCallbacks() throws Exception {
        try (Fixture fixture = new Fixture()) {
            var runtime = fixture.runtime;
            long oldHandle = runtime.register(FieldLayout.of("old"));
            AtomicInteger staleCallbacks = new AtomicInteger();
            runtime.subscribe(oldHandle, 1L, event -> staleCallbacks.incrementAndGet());
            var events = new ConcurrentLinkedQueue<ObjectEvent.Snapshot>();
            FieldLayout newLayout = FieldLayout.of("new", "other");
            long newHandle;
            try (WorkerBarrier barrier = new WorkerBarrier(runtime)) {
                barrier.awaitEntered();
                runtime.changed(oldHandle, 0);
                assertTrue(runtime.release(oldHandle));
                newHandle = runtime.register(newLayout);
                assertEquals(ArenaHandle.slot(oldHandle), ArenaHandle.slot(newHandle));
                assertNotEquals(oldHandle, newHandle);
                assertFalse(runtime.changed(oldHandle, 0));
                assertFalse(runtime.release(oldHandle));
                runtime.subscribe(newHandle, 3L, event -> events.add(event.snapshot()));
                runtime.changed(newHandle, 1);
            }
            fixture.idle();
            assertEquals(0, staleCallbacks.get());
            ObjectEvent.Snapshot event = only(events);
            assertEquals(newHandle, event.handle());
            assertSame(newLayout, event.layout());
            assertEquals(2L, event.contentMask());
        }
    }

    @Test
    void concurrentPublishersEventuallyDeliverEveryFieldAcrossRepeatedDrainRaces() throws Exception {
        try (Fixture fixture = new Fixture(); var publishers = Executors.newFixedThreadPool(8)) {
            var runtime = fixture.runtime;
            FieldLayout layout = FieldLayout.of(names(8));
            long handle = runtime.register(layout);
            AtomicLong seen = new AtomicLong();
            runtime.subscribe(handle, layout.allMask(), event -> {
                long mask = event.contentMask();
                seen.getAndUpdate(before -> before | mask);
            });
            for (int round = 0; round < 60; round++) {
                seen.set(0L);
                CountDownLatch start = new CountDownLatch(1);
                List<CompletableFuture<Void>> work = new ArrayList<>();
                for (int field = 0; field < 8; field++) {
                    int selected = field;
                    work.add(CompletableFuture.runAsync(() -> {
                        awaitLatch(start);
                        for (int i = 0; i < 32; i++) {
                            assertTrue(runtime.changed(handle, selected));
                        }
                    }, publishers));
                }
                start.countDown();
                for (var task : work) task.get(5, TimeUnit.SECONDS);
                fixture.idle();
                assertEquals(layout.allMask(), seen.get(), "lost field invalidation in round " + round);
            }
        }
    }

    @Test
    void manyVirtualThreadPublishersUseTheSameObservationContract() throws Exception {
        try (Fixture fixture = new Fixture(); var publishers = Executors.newVirtualThreadPerTaskExecutor()) {
            var runtime = fixture.runtime;
            FieldLayout layout = FieldLayout.of(names(8));
            long handle = runtime.register(layout);
            AtomicLong seen = new AtomicLong();
            runtime.subscribe(handle, layout.allMask(), event -> {
                long mask = event.contentMask();
                seen.getAndUpdate(before -> before | mask);
            });
            CountDownLatch start = new CountDownLatch(1);
            List<CompletableFuture<Void>> work = new ArrayList<>();
            for (int i = 0; i < 256; i++) {
                int field = i & 7;
                work.add(CompletableFuture.runAsync(() -> {
                    awaitLatch(start);
                    assertTrue(runtime.changed(handle, field));
                }, publishers));
            }
            start.countDown();
            for (var task : work) task.get(5, TimeUnit.SECONDS);
            fixture.idle();
            assertEquals(layout.allMask(), seen.get());
        }
    }

    @Test
    void listenerFailuresDoNotPreventOtherListenersOrLaterWaves() throws Exception {
        var failures = new ConcurrentLinkedQueue<Throwable>();
        try (var runtime = new AsyncObjectEvents(failures::add)) {
            long handle = runtime.register(FieldLayout.of("value"));
            IllegalStateException expected = new IllegalStateException("deliberate listener failure");
            AtomicInteger delivered = new AtomicInteger();
            runtime.subscribe(handle, 1L, event -> { throw expected; });
            runtime.subscribe(handle, 1L, event -> delivered.incrementAndGet());
            runtime.changed(handle, 0);
            assertTrue(runtime.awaitIdle(WAIT));
            runtime.changed(handle, 0);
            assertTrue(runtime.awaitIdle(WAIT));
            assertEquals(2, delivered.get());
            assertEquals(2, failures.size());
            for (Throwable failure : failures) assertSame(expected, failure);
        }
    }

    @Test
    void borrowedViewRejectsOffWorkerAccessAndSnapshotSurvivesLaterDelivery() throws Exception {
        try (Fixture fixture = new Fixture()) {
            var runtime = fixture.runtime;
            FieldLayout layout = FieldLayout.of("first", "second");
            long handle = runtime.register(layout);
            var borrowed = new AtomicReference<ObjectEvent>();
            var snapshots = new ConcurrentLinkedQueue<ObjectEvent.Snapshot>();
            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch proceed = new CountDownLatch(1);
            runtime.subscribe(handle, layout.allMask(), event -> {
                borrowed.set(event);
                snapshots.add(event.snapshot());
                entered.countDown();
                awaitLatch(proceed);
            });
            try {
                runtime.changed(handle, 0);
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                assertThrows(IllegalStateException.class, borrowed.get()::handle);
                assertThrows(IllegalStateException.class, borrowed.get()::snapshot);
            } finally {
                proceed.countDown();
            }
            fixture.idle();
            ObjectEvent.Snapshot retained = only(snapshots);
            assertBorrowExpired(borrowed.get());
            runtime.changed(handle, 1);
            fixture.idle();
            assertEquals(2, snapshots.size());
            assertEquals(1L, retained.contentMask());
            assertEquals(1L, retained.changedMask());
            assertEquals(handle, retained.handle());
            assertSame(layout, retained.layout());
            ObjectEvent.Snapshot later = new ArrayList<>(snapshots).get(1);
            assertEquals(2L, later.contentMask());
            assertTrue(later.sequence() > retained.sequence());
            // Borrowing is a contract, not complete escape detection: an old alias used
            // inside a later callback can alias the same active reusable Java facade.
        }
    }

    @Test
    void reentrantMutationCreatesALaterCallbackWithoutRecursiveDelivery() throws Exception {
        try (Fixture fixture = new Fixture()) {
            var runtime = fixture.runtime;
            long handle = runtime.register(FieldLayout.of("initial", "reentrant"));
            AtomicInteger callbacks = new AtomicInteger();
            AtomicInteger depth = new AtomicInteger();
            AtomicInteger maximumDepth = new AtomicInteger();
            var events = new ConcurrentLinkedQueue<ObjectEvent.Snapshot>();
            runtime.subscribe(handle, 3L, event -> {
                int current = depth.incrementAndGet();
                maximumDepth.accumulateAndGet(current, Math::max);
                try {
                    events.add(event.snapshot());
                    if (callbacks.incrementAndGet() == 1) assertTrue(runtime.changed(handle, 1));
                    assertThrows(IllegalStateException.class, () -> runtime.awaitIdle(Duration.ZERO));
                } finally {
                    depth.decrementAndGet();
                }
            });
            runtime.changed(handle, 0);
            fixture.idle();
            assertEquals(2, callbacks.get());
            assertEquals(1, maximumDepth.get());
            assertEquals(3L, contentUnion(events));
        }
    }

    @Test
    void awaitIdleWaitsForEveryReentrantWaveAcrossRepeatedBursts() throws Exception {
        try (Fixture fixture = new Fixture()) {
            var runtime = fixture.runtime;
            long handle = runtime.register(FieldLayout.of("value"));
            AtomicInteger remaining = new AtomicInteger();
            AtomicInteger delivered = new AtomicInteger();
            runtime.subscribe(handle, 1L, event -> {
                delivered.incrementAndGet();
                if (remaining.decrementAndGet() > 0) runtime.changed(handle, 0);
            });
            for (int round = 0; round < 32; round++) {
                remaining.set(64);
                runtime.changed(handle, 0);
                fixture.idle();
                assertEquals(0, remaining.get(), "awaitIdle returned between reentrant waves");
                assertEquals((round + 1) * 64, delivered.get());
            }
        }
    }

    @Test
    void listenerInterruptionDoesNotContaminateOtherListenersOrLaterWaves() throws Exception {
        try (Fixture fixture = new Fixture()) {
            var runtime = fixture.runtime;
            long handle = runtime.register(FieldLayout.of("value"));
            AtomicInteger delivered = new AtomicInteger();
            runtime.subscribe(handle, 1L, event -> Thread.currentThread().interrupt());
            runtime.subscribe(handle, 1L, event -> {
                assertFalse(Thread.currentThread().isInterrupted(), "previous listener leaked interrupt status");
                delivered.incrementAndGet();
            });
            runtime.changed(handle, 0);
            fixture.idle();
            runtime.changed(handle, 0);
            fixture.idle();
            assertEquals(2, delivered.get());
        }
    }

    @Test
    void linkedFieldsRejectTheUnlinkedReferencePublicationApi() throws Exception {
        try (Fixture fixture = new Fixture()) {
            var runtime = fixture.runtime;
            long parent = runtime.register(FieldLayout.of("child"));
            long child = runtime.register(FieldLayout.of("value"));
            runtime.bind(parent, 0, child);
            fixture.idle();
            assertThrows(IllegalStateException.class,
                    () -> runtime.referenceChanged(parent, 0, null, new Object()));
        }
    }

    @Test
    void releasingAChildInvalidatesItsParentsWithoutBindingTheReusedSlot() throws Exception {
        try (Fixture fixture = new Fixture()) {
            var runtime = fixture.runtime;
            long parent = runtime.register(FieldLayout.of("child"));
            long child = runtime.register(FieldLayout.of("value"));
            runtime.bind(parent, 0, child);
            fixture.idle();
            var events = new ConcurrentLinkedQueue<ObjectEvent.Snapshot>();
            runtime.subscribe(parent, 1L, event -> events.add(event.snapshot()));
            assertTrue(runtime.release(child));
            fixture.idle();
            assertEquals(1L, contentUnion(events));
            assertEquals(0L, referenceUnion(events));
            events.clear();
            long replacement = runtime.register(FieldLayout.of("replacement"));
            assertEquals(ArenaHandle.slot(child), ArenaHandle.slot(replacement));
            runtime.changed(replacement, 0);
            fixture.idle();
            assertTrue(events.isEmpty(), "slot reuse must not resurrect the released dependency");
        }
    }

    @Test
    void closingASubscriptionCancelsItsQueuedAndFutureCallbacks() throws Exception {
        try (Fixture fixture = new Fixture()) {
            var runtime = fixture.runtime;
            long handle = runtime.register(FieldLayout.of("value"));
            AtomicInteger callbacks = new AtomicInteger();
            var subscription = runtime.subscribe(handle, 1L, event -> callbacks.incrementAndGet());
            try (WorkerBarrier barrier = new WorkerBarrier(runtime)) {
                barrier.awaitEntered();
                runtime.changed(handle, 0);
                subscription.close();
                subscription.close();
            }
            fixture.idle();
            runtime.changed(handle, 0);
            fixture.idle();
            assertEquals(0, callbacks.get());
        }
    }

    @Test
    void closeReturnsWhileACallbackRunsAndCancelsQueuedWork() throws Exception {
        try (Fixture fixture = new Fixture()) {
            var runtime = fixture.runtime;
            long handle = runtime.register(FieldLayout.of("value"));
            AtomicInteger callbacks = new AtomicInteger();
            runtime.subscribe(handle, 1L, event -> callbacks.incrementAndGet());
            try (WorkerBarrier barrier = new WorkerBarrier(runtime)) {
                barrier.awaitEntered();
                runtime.changed(handle, 0);
                CompletableFuture.runAsync(runtime::close).get(5, TimeUnit.SECONDS);
                assertFalse(runtime.awaitTermination(Duration.ZERO));
                assertFalse(runtime.awaitIdle(Duration.ZERO));
                assertThrows(IllegalStateException.class, () -> runtime.register(FieldLayout.of("late")));
            }
            assertTrue(runtime.awaitTermination(WAIT));
            assertEquals(0, callbacks.get());
        }
    }

    @Test
    void validatesSubscriptionMasksAndFieldCoordinates() throws Exception {
        try (Fixture fixture = new Fixture()) {
            var runtime = fixture.runtime;
            long handle = runtime.register(FieldLayout.of("value"));
            assertThrows(IllegalArgumentException.class, () -> runtime.subscribe(handle, 0L, event -> {}));
            assertThrows(IllegalArgumentException.class, () -> runtime.subscribe(handle, 2L, event -> {}));
            assertThrows(IndexOutOfBoundsException.class, () -> runtime.changed(handle, -1));
            assertThrows(IndexOutOfBoundsException.class, () -> runtime.changed(handle, 1));
            assertFalse(runtime.changed(0L, 0));
            assertFalse(runtime.release(0L));
        }
    }

    private static String[] names(int count) {
        String[] names = new String[count];
        for (int i = 0; i < count; i++) names[i] = "f" + i;
        return names;
    }

    private static ObjectEvent.Snapshot only(ConcurrentLinkedQueue<ObjectEvent.Snapshot> events) {
        assertEquals(1, events.size(), "expected one coalesced callback");
        return events.element();
    }

    private static long contentUnion(Iterable<ObjectEvent.Snapshot> events) {
        long union = 0L;
        for (var event : events) union |= event.contentMask();
        return union;
    }

    private static long referenceUnion(Iterable<ObjectEvent.Snapshot> events) {
        long union = 0L;
        for (var event : events) union |= event.referenceMask();
        return union;
    }

    private static void assertBorrowExpired(ObjectEvent event) {
        assertThrows(IllegalStateException.class, event::handle);
        assertThrows(IllegalStateException.class, event::referenceMask);
        assertThrows(IllegalStateException.class, event::contentMask);
        assertThrows(IllegalStateException.class, event::changedMask);
        assertThrows(IllegalStateException.class, event::sequence);
        assertThrows(IllegalStateException.class, event::layout);
        assertThrows(IllegalStateException.class, event::snapshot);
    }

    /** Bounded even on failure; preserves interruption without opening the barrier early. */
    private static void awaitLatch(CountDownLatch latch) {
        long deadline = System.nanoTime() + WAIT.toNanos();
        boolean interrupted = false;
        try {
            for (;;) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0L) throw new AssertionError("test latch timed out");
                try {
                    if (!latch.await(remaining, TimeUnit.NANOSECONDS)) {
                        throw new AssertionError("test latch timed out");
                    }
                    return;
                } catch (InterruptedException exception) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private static final class WorkerBarrier implements AutoCloseable {
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch proceed = new CountDownLatch(1);

        private WorkerBarrier(AsyncObjectEvents runtime) {
            long gate = runtime.register(FieldLayout.of("gate"));
            runtime.subscribe(gate, 1L, event -> {
                entered.countDown();
                awaitLatch(proceed);
            });
            runtime.changed(gate, 0);
        }

        private void awaitEntered() throws InterruptedException {
            assertTrue(entered.await(5, TimeUnit.SECONDS), "dispatcher did not enter the test barrier");
        }

        @Override
        public void close() {
            proceed.countDown();
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();
        private final AsyncObjectEvents runtime = new AsyncObjectEvents(failures::add);

        private void idle() throws InterruptedException {
            assertTrue(runtime.awaitIdle(WAIT), "dispatcher did not become idle");
            assertTrue(failures.isEmpty(), () -> "callback errors: " + failures);
        }

        @Override
        public void close() {
            runtime.close();
            try {
                assertTrue(runtime.awaitTermination(WAIT), "dispatcher did not terminate");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("interrupted during dispatcher cleanup", exception);
            }
            assertTrue(failures.isEmpty(), () -> "callback errors: " + failures);
        }
    }
}
