/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary Static M3 list range APIs preserve legacy defaults and borrowed progress contracts
 * @modules jdk.m3.collections
 * @run main/othervm CollectionViewsRangeCompatibilityTest
 */
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import jdk.m3.collections.CollectionViews;
import jdk.m3.collections.DefaultProgressMonitor;
import jdk.m3.collections.ProgressMonitor;

public final class CollectionViewsRangeCompatibilityTest {
    private static int checks;

    private static void verify(boolean correct, String message) {
        checks++;
        if (!correct) throw new AssertionError(message);
    }

    private static void throwsType(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("unexpected failure: " + failure, failure);
        }
        throw new AssertionError("expected failure: " + type.getName());
    }

    private interface OldRangeDefault {
        default void forEachRange(int from, int to, Consumer<? super Integer> action) {
            action.accept(999);
        }
    }

    private interface OldMonitoredDefault {
        default void forEachRangeMonitored(int from, int to,
                Consumer<? super Integer> action, ProgressMonitor monitor) {
            action.accept(777);
        }
    }

    private static final class External extends AbstractList<Integer>
            implements OldRangeDefault, OldMonitoredDefault {
        private final int[] values = {1, 2, 3, 4};
        @Override public Integer get(int index) { return values[index]; }
        @Override public int size() { return values.length; }
    }

    private static void verifyRange(List<Integer> values) {
        var received = new ArrayList<Integer>();
        CollectionViews.forEachRange(values, 1, 3, received::add);
        verify(received.equals(List.of(2, 3)), "static window preserves encounter order");
        received.clear();

        DefaultProgressMonitor monitor = new DefaultProgressMonitor();
        monitor.beginTask("borrowed", 10L);
        long generation = monitor.state().generation();
        CollectionViews.forEachRangeMonitored(values, 1, 3, received::add, monitor);
        verify(received.equals(List.of(2, 3)), "monitored window preserves encounter order");
        verify(monitor.state().worked() == 2L, "exact progress credit");
        verify(!monitor.state().done() && monitor.state().generation() == generation,
                "borrowed lifecycle unchanged");

        received.clear();
        DefaultProgressMonitor empty = new DefaultProgressMonitor();
        empty.beginTask("empty", 10L);
        CollectionViews.forEachRangeMonitored(values, 2, 2, received::add, empty);
        verify(received.isEmpty() && empty.state().worked() == 0L, "empty window");

        DefaultProgressMonitor cancelled = new DefaultProgressMonitor();
        cancelled.beginTask("cancelled", 10L);
        cancelled.setCanceled(true);
        throwsType(CancellationException.class, () ->
                CollectionViews.forEachRangeMonitored(values, 0, 3,
                        value -> { throw new AssertionError("callback after cancellation"); },
                        cancelled));
        verify(cancelled.state().worked() == 0L, "no work after cancellation");

        DefaultProgressMonitor failed = new DefaultProgressMonitor();
        failed.beginTask("failure", 10L);
        RuntimeException sentinel = new RuntimeException("callback");
        try {
            CollectionViews.forEachRangeMonitored(values, 0, 2,
                    value -> { throw sentinel; }, failed);
            throw new AssertionError("callback failure omitted");
        } catch (RuntimeException observed) {
            verify(observed == sentinel && failed.state().worked() == 0L,
                    "same exception and no failed callback credit");
        }

        throwsType(IndexOutOfBoundsException.class, () ->
                CollectionViews.forEachRange(values, -1, 2, received::add));
        throwsType(IndexOutOfBoundsException.class, () ->
                CollectionViews.forEachRangeMonitored(values, 0, 90, received::add,
                        new DefaultProgressMonitor()));
        throwsType(NullPointerException.class, () ->
                CollectionViews.forEachRange(values, 0, 1, null));
        throwsType(NullPointerException.class, () ->
                CollectionViews.forEachRangeMonitored(values, 0, 1, received::add, null));
    }

    /** Pre-cancelled ranges must not acquire an expensive iterator. */
    private static void verifyCancelledPreflight() {
        final class Spy extends AbstractList<Integer> {
            private int iteratorCalls;
            @Override public Integer get(int index) { return index + 1; }
            @Override public int size() { return 4; }
            @Override public java.util.ListIterator<Integer> listIterator(int index) {
                iteratorCalls++;
                return super.listIterator(index);
            }
        }
        Spy spy = new Spy();
        DefaultProgressMonitor cancelled = new DefaultProgressMonitor();
        cancelled.beginTask("preflight", 4L);
        cancelled.setCanceled(true);
        throwsType(CancellationException.class, () ->
                CollectionViews.forEachRangeMonitored(spy, 1, 3,
                        value -> { throw new AssertionError("already cancelled"); },
                        cancelled));
        verify(spy.iteratorCalls == 0, "no iterator acquired for pre-cancelled range");
        verify(cancelled.state().worked() == 0, "no credit for pre-cancelled range");
        throwsType(CancellationException.class, () ->
                CollectionViews.forEachRangeMonitored(spy, 2, 2,
                        value -> { throw new AssertionError("empty callback"); },
                        cancelled));
        verify(spy.iteratorCalls == 0, "no iterator acquired for empty cancelled range");
    }

    public static void main(String[] args) {
        verifyCancelledPreflight();
        verifyRange(new ArrayList<>(List.of(1, 2, 3, 4)));
        verifyRange(new LinkedList<>(List.of(1, 2, 3, 4)));
        verifyRange(new External());

        External external = new External();
        var oldRange = new ArrayList<Integer>();
        ((OldRangeDefault) external).forEachRange(1, 3, oldRange::add);
        verify(oldRange.equals(List.of(999)), "legacy concrete dispatch preserved");

        var oldMonitored = new ArrayList<Integer>();
        ((OldMonitoredDefault) external).forEachRangeMonitored(1, 3,
                oldMonitored::add, new DefaultProgressMonitor());
        verify(oldMonitored.equals(List.of(777)), "legacy default dispatch preserved");

        var newRange = new ArrayList<Integer>();
        CollectionViews.forEachRange(external, 1, 3, newRange::add);
        verify(newRange.equals(List.of(2, 3)), "static operation opt-in only");

        Thread.currentThread().interrupt();
        try {
            throwsType(CancellationException.class, () ->
                    CollectionViews.forEachRangeMonitored(external, 0, 2,
                            value -> { throw new AssertionError("interrupt ignored"); },
                            new DefaultProgressMonitor()));
            verify(Thread.currentThread().isInterrupted(), "interrupt flag retained");
        } finally {
            Thread.interrupted();
        }

        System.out.println("M3_STATIC_RANGE_COMPAT_PASS checks=" + checks);
    }
}
