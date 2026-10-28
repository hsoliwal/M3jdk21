// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.collections.M3DefaultProgressMonitor;
import com.m3.collections.M3ProgressMonitor;
import com.m3.collections.M3ProgressState;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.junit.jupiter.api.Test;

class M3EclipseProgressMonitorTest {
    @Test
    void rejectsReentrantBeginWithoutBeginningTheOwnerTwice() {
        CallbackOwner owner = new CallbackOwner();
        IProgressMonitor eclipse = M3EclipseProgressMonitor.owned(owner);
        owner.onBegin = () -> assertThrows(IllegalStateException.class,
                () -> eclipse.beginTask("nested", 5));
        eclipse.beginTask("outer", 10);
        assertEquals(1, owner.begins);
        assertEquals("outer", owner.state().taskName());
    }

    @Test
    void reentrantCompletionDoesNotCompleteTwiceOrAcceptMoreWork() {
        CallbackOwner owner = new CallbackOwner();
        IProgressMonitor eclipse = M3EclipseProgressMonitor.owned(owner);
        eclipse.beginTask("outer", 10);
        owner.onDone = () -> {
            eclipse.done();
            eclipse.worked(2);
            eclipse.internalWorked(2.5d);
        };
        eclipse.done();
        assertEquals(1, owner.completions);
        assertEquals(0L, owner.state().worked());
        assertTrue(owner.state().done());
    }

    @Test
    void publishesFractionalRemainderBeforeReentrantWork() {
        CallbackOwner owner = new CallbackOwner();
        IProgressMonitor eclipse = M3EclipseProgressMonitor.borrowed(owner);
        eclipse.internalWorked(0.75d);
        owner.onWorked = ignored -> eclipse.internalWorked(0.5d);
        eclipse.internalWorked(0.5d);
        assertEquals(1L, owner.state().worked(), "1.75 total input credits one whole unit");
        eclipse.internalWorked(0.25d);
        assertEquals(2L, owner.state().worked());
    }

    @Test
    void lifecycleOwnerExceptionsPropagateAndPermitRetry() {
        CallbackOwner owner = new CallbackOwner();
        IProgressMonitor eclipse = M3EclipseProgressMonitor.owned(owner);
        IllegalStateException beginFailure = new IllegalStateException("begin marker");
        owner.onBegin = () -> { throw beginFailure; };
        assertSame(beginFailure, assertThrows(IllegalStateException.class,
                () -> eclipse.beginTask("failed", 10)));
        eclipse.beginTask("retry", 10);
        assertEquals(2, owner.begins);
        IllegalStateException doneFailure = new IllegalStateException("done marker");
        owner.onDone = () -> { throw doneFailure; };
        assertSame(doneFailure, assertThrows(IllegalStateException.class, eclipse::done));
        assertFalse(owner.state().done());
        eclipse.worked(2);
        eclipse.done();
        eclipse.done();
        assertEquals(2, owner.completions);
        assertEquals(2L, owner.state().worked());
    }

    @Test
    void ordinaryWorkFailureRestoresFractionForRetry() {
        CallbackOwner owner = new CallbackOwner();
        IProgressMonitor eclipse = M3EclipseProgressMonitor.borrowed(owner);
        eclipse.internalWorked(0.75d);
        IllegalStateException failure = new IllegalStateException("work marker");
        owner.onWorked = ignored -> { throw failure; };
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> eclipse.internalWorked(0.5d)));
        assertEquals(0L, owner.state().worked());
        eclipse.internalWorked(0.25d);
        assertEquals(1L, owner.state().worked(), "failed delivery retains the original fraction");
    }

    @Test
    void reentrantWorkFailureDoesNotDiscardNestedFraction() {
        CallbackOwner owner = new CallbackOwner();
        IProgressMonitor eclipse = M3EclipseProgressMonitor.borrowed(owner);
        eclipse.internalWorked(0.75d);
        IllegalStateException failure = new IllegalStateException("nested work marker");
        owner.onWorked = ignored -> {
            eclipse.internalWorked(0.5d);
            throw failure;
        };
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> eclipse.internalWorked(0.5d)));
        assertEquals(0L, owner.state().worked());
        eclipse.internalWorked(0.25d);
        assertEquals(1L, owner.state().worked(), "nested updates survive the outer callback failure");
    }

    @Test
    void fullyFailedNestedDeliveryRestoresOuterFractionForRetry() {
        CallbackOwner owner = new CallbackOwner();
        IProgressMonitor eclipse = M3EclipseProgressMonitor.borrowed(owner);
        eclipse.internalWorked(0.75d);
        IllegalStateException failure = new IllegalStateException("nested delivery failed");
        owner.onWorked = ignored -> {
            owner.onWorked = nested -> { throw failure; };
            eclipse.internalWorked(1.0d);
        };
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> eclipse.internalWorked(0.5d)));
        assertEquals(0L, owner.state().worked(), "neither failed callback accepted work");
        eclipse.internalWorked(0.25d);
        assertEquals(1L, owner.state().worked(), "both failed updates restore the original fraction");
    }

    @Test
    void saturatedDeliveryPublishesItsResetBeforeNestedFractions() {
        CallbackOwner owner = new CallbackOwner();
        IProgressMonitor eclipse = M3EclipseProgressMonitor.borrowed(owner);
        eclipse.internalWorked(0.75d);
        owner.onWorked = ignored -> eclipse.internalWorked(0.5d);
        eclipse.internalWorked(Double.MAX_VALUE);
        assertEquals(1, owner.deliveries, "nested half must not consume the old fraction");
        assertEquals(Long.MAX_VALUE, owner.state().worked());
        eclipse.internalWorked(0.5d);
        assertEquals(2, owner.deliveries, "the nested half remains available for the next report");
    }

    @Test
    void workErrorPropagatesUnchangedAndRestoresOrdinaryFraction() {
        CallbackOwner owner = new CallbackOwner();
        IProgressMonitor eclipse = M3EclipseProgressMonitor.borrowed(owner);
        eclipse.internalWorked(0.75d);
        AssertionError failure = new AssertionError("work error marker");
        owner.onWorked = ignored -> { throw failure; };
        assertSame(failure, assertThrows(AssertionError.class,
                () -> eclipse.internalWorked(0.5d)));
        eclipse.internalWorked(0.25d);
        assertEquals(1L, owner.state().worked());
    }

    @Test
    void realSubMonitorSequentialChildrenPreserveBudgetAndBorrowedCompletion() {
        M3DefaultProgressMonitor owner = new M3DefaultProgressMonitor();
        IProgressMonitor eclipse = M3EclipseProgressMonitor.owned(owner);
        SubMonitor root = SubMonitor.convert(eclipse, "compile", 10);
        long parentBudget = owner.state().totalWork();
        SubMonitor parse = root.split(3);
        parse.beginTask("parse", 6);
        parse.worked(3);
        SubMonitor link = root.split(7);
        assertEquals(parentBudget * 3L / 10L, owner.state().worked());
        link.beginTask("link", 7);
        link.worked(7);
        root.done();
        assertEquals(parentBudget, owner.state().worked());
        assertFalse(owner.state().done(), "SubMonitor does not own the original monitor");
        eclipse.done();
        assertTrue(owner.state().done());
    }

    @Test
    void realSlicesReportParallelWorkAndKeepCancellationLocal() throws Exception {
        M3DefaultProgressMonitor owner = new M3DefaultProgressMonitor();
        IProgressMonitor eclipse = M3EclipseProgressMonitor.owned(owner);
        eclipse.beginTask("parallel", 100);
        IProgressMonitor first = eclipse.slice(30);
        IProgressMonitor second = eclipse.slice(70);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var left = executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                completeSlice(first, 300);
                return null;
            });
            var right = executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                completeSlice(second, 700);
                return null;
            });
            try {
                assertTrue(ready.await(10, TimeUnit.SECONDS));
            } finally {
                start.countDown();
            }
            left.get(10, TimeUnit.SECONDS);
            right.get(10, TimeUnit.SECONDS);
        }
        assertEquals(100L, owner.state().worked());
        assertFalse(owner.state().done());
        first.setCanceled(true);
        assertFalse(owner.isCanceled(), "slice cancellation does not cancel its parent");
        eclipse.setCanceled(true);
        assertTrue(second.isCanceled());
        eclipse.done();
        assertTrue(owner.state().done());
    }

    private static void completeSlice(IProgressMonitor monitor, int work) {
        monitor.beginTask("branch", work);
        try {
            for (int i = 0; i < work; i++) monitor.worked(1);
        } finally {
            monitor.done();
        }
    }

    /** Real SPI delegate with one-shot synchronous observer callbacks. */
    private static final class CallbackOwner implements M3ProgressMonitor {
        private final M3DefaultProgressMonitor delegate = new M3DefaultProgressMonitor();
        private Runnable onBegin;
        private Runnable onDone;
        private LongConsumer onWorked;
        private int begins;
        private int completions;
        private int deliveries;

        @Override public void beginTask(String name, long work) {
            begins++;
            Runnable callback = onBegin;
            onBegin = null;
            if (callback != null) callback.run();
            delegate.beginTask(name, work);
        }
        @Override public void done() {
            completions++;
            Runnable callback = onDone;
            onDone = null;
            if (callback != null) callback.run();
            delegate.done();
        }
        @Override public void worked(long work) {
            deliveries++;
            LongConsumer callback = onWorked;
            onWorked = null;
            if (callback != null) callback.accept(work);
            delegate.worked(work);
        }
        @Override public void setProgress(long work, long total) { delegate.setProgress(work, total); }
        @Override public void setTaskName(String name) { delegate.setTaskName(name); }
        @Override public void subTask(String name) { delegate.subTask(name); }
        @Override public boolean isCanceled() { return delegate.isCanceled(); }
        @Override public void setCanceled(boolean value) { delegate.setCanceled(value); }
        @Override public M3ProgressState state() { return delegate.state(); }
    }

    @Test
    void ownedModePreservesEclipseLifecycleAndFractionalProgress() {
        M3DefaultProgressMonitor m3 = new M3DefaultProgressMonitor();
        IProgressMonitor eclipse = M3EclipseProgressMonitor.owned(m3);
        assertSame(m3, ((M3EclipseProgressMonitor) eclipse).owner());
        eclipse.beginTask("compile", 10);
        eclipse.internalWorked(0.25d);
        eclipse.internalWorked(0.75d);
        eclipse.worked(2);
        eclipse.subTask("bindings");
        assertEquals(3L, m3.state().worked());
        assertEquals("bindings", m3.state().subTaskName());
        eclipse.setTaskName("link");
        assertEquals("link", m3.state().taskName());
        eclipse.done();
        eclipse.done();
        eclipse.worked(2);
        assertEquals(3L, m3.state().worked());
        assertTrue(m3.state().done());
        assertThrows(IllegalStateException.class, () -> eclipse.beginTask("again", 10));
    }

    @Test
    void borrowedModeDoesNotChangeParentLifecycle() {
        M3DefaultProgressMonitor m3 = new M3DefaultProgressMonitor();
        m3.beginTask("outer", 20);
        IProgressMonitor eclipse = M3EclipseProgressMonitor.borrowed(m3);
        eclipse.beginTask("jdt", 10);
        eclipse.internalWorked(0.5d);
        eclipse.internalWorked(0.5d);
        eclipse.setTaskName("parser");
        eclipse.worked(2);
        eclipse.done();
        assertEquals("outer", m3.state().taskName());
        assertEquals("parser", m3.state().subTaskName());
        assertFalse(m3.state().done());
        assertEquals(3L, m3.state().worked());
        eclipse.setCanceled(true);
        assertTrue(eclipse.isCanceled());
        assertTrue(m3.isCanceled());
        eclipse.setCanceled(false);
        assertFalse(m3.isCanceled());
    }

    @Test
    void ignoresNonFiniteAndNegativeWorkAndKeepsUnknownTotal() {
        M3DefaultProgressMonitor m3 = new M3DefaultProgressMonitor();
        IProgressMonitor eclipse = M3EclipseProgressMonitor.owned(m3);
        eclipse.beginTask("unknown", IProgressMonitor.UNKNOWN);
        assertEquals(-1L, m3.state().totalWork());
        eclipse.internalWorked(Double.NaN);
        eclipse.internalWorked(Double.POSITIVE_INFINITY);
        eclipse.internalWorked(-12.0d);
        eclipse.worked(-5);
        assertEquals(0L, m3.state().worked());
        eclipse.internalWorked(0.125d);
        eclipse.internalWorked(0.875d);
        assertEquals(1L, m3.state().worked());
    }

    @Test
    void rejectsIllegalLifecycleAndDoesNotStartOnInvalidWork() {
        M3DefaultProgressMonitor m3 = new M3DefaultProgressMonitor();
        IProgressMonitor eclipse = M3EclipseProgressMonitor.owned(m3);
        assertThrows(IllegalArgumentException.class, () -> eclipse.beginTask("bad", -2));
        eclipse.beginTask("good", 1);
        assertEquals("good", m3.state().taskName());
        assertThrows(IllegalStateException.class, () -> eclipse.beginTask("duplicate", 1));
        eclipse.done();
        assertTrue(m3.state().done());
    }

    @Test
    void preservesM3LongOverflowSaturationAndSingleOwner() {
        M3DefaultProgressMonitor m3 = new M3DefaultProgressMonitor();
        IProgressMonitor eclipse = M3EclipseProgressMonitor.borrowed(m3);
        eclipse.internalWorked(1.0e30d);
        assertEquals(Long.MAX_VALUE, m3.state().worked());
        eclipse.internalWorked(2.0d);
        assertEquals(Long.MAX_VALUE, m3.state().worked());
    }
}
