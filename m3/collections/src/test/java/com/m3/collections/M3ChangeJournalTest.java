// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/** Executable Java 21 boundary oracle for bounded indexed observation history. */
public final class M3ChangeJournalTest {
    @Test public void exactHistoryAndWraparound() { exerciseHistory(); }
    @Test public void invalidInputsAndDetachedPages() { exerciseBoundaries(); }
    @Test public void serializedConcurrentPublishing() throws Exception { exerciseConcurrency(); }

    public static void main(String[] ignored) throws Exception {
        exerciseHistory();
        exerciseBoundaries();
        exerciseConcurrency();
        System.out.println("M3_CHANGE_JOURNAL_PASS");
    }

    private static void exerciseHistory() {
        M3ChangeJournal journal = new M3ChangeJournal(3);
        equal(0, journal.newestSequence());
        equal(0, journal.oldestRetainedSequence());
        equal(0, journal.snapshotAfter(0, 10).size());
        equal(1, journal.record(-1L, 0x80000000, 0, 255));
        equal(2, journal.record(Long.MAX_VALUE, 7, 255, 0));
        equal(3, journal.record(0, 0, 8, 8));
        M3ChangeJournal.Snapshot first = journal.snapshotAfter(0, 2);
        equal(2, first.size());
        equal(1, first.sequence(0));
        equal(-1, first.identity(0));
        equal(0x80000000, first.changeMask(0));
        equal(255, first.newState(0));
        equal(255, first.oldState(1));
        equal(Long.MAX_VALUE, first.identity(1));
        equal(2, first.nextCursor());
        check(!first.overrun());
        equal(4, journal.record(44, 0xff00ff00, 1, 2));
        equal(2, journal.oldestRetainedSequence());
        equal(3, journal.retainedCount());
        M3ChangeJournal.Snapshot lost = journal.snapshotAfter(0, 10);
        check(lost.overrun());
        equal(1, lost.missedEvents());
        equal(3, lost.size());
        equal(2, lost.sequence(0));
        equal(4, lost.sequence(2));
        equal(44, lost.identity(2));
        equal(0xff00ff00, lost.changeMask(2));
        equal(1, lost.oldState(2));
        equal(2, lost.newState(2));
        M3ChangeJournal.Snapshot rest = journal.snapshotAfter(first.nextCursor(), 1);
        check(!rest.overrun());
        equal(3, rest.sequence(0));
        equal(3, rest.nextCursor());
        equal(4, journal.snapshotAfter(rest.nextCursor(), 1).nextCursor());
        equal(0, journal.snapshotAfter(4, 8).size());
        equal(4, journal.snapshotAfter(4, 8).nextCursor());
        // A detached page survives subsequent wraparound with no cloned payload objects.
        journal.record(55, -1, 2, 3);
        equal(-1, first.identity(0));
        equal(44, lost.identity(2));
    }

    private static void exerciseBoundaries() {
        fails(IllegalArgumentException.class, () -> new M3ChangeJournal(0));
        M3ChangeJournal journal = new M3ChangeJournal(1);
        fails(IllegalArgumentException.class, () -> journal.record(1, 1, -1, 0));
        fails(IllegalArgumentException.class, () -> journal.record(1, 1, 0, 256));
        equal(0, journal.newestSequence());
        fails(IllegalArgumentException.class, () -> journal.snapshotAfter(-1, 1));
        fails(IllegalArgumentException.class, () -> journal.snapshotAfter(1, 1));
        fails(IllegalArgumentException.class, () -> journal.snapshotAfter(0, 0));
        journal.record(1, 0, 0, 0);
        journal.record(2, 0, 0, 0);
        M3ChangeJournal.Snapshot result = journal.snapshotAfter(0, 1);
        equal(1, result.missedEvents());
        equal(2, result.sequence(0));
        fails(IndexOutOfBoundsException.class, () -> result.identity(1));
        fails(IndexOutOfBoundsException.class, () -> result.sequence(-1));
    }

    private static void exerciseConcurrency() throws Exception {
        final int producers = 8, perProducer = 100;
        M3ChangeJournal journal = new M3ChangeJournal(19);
        try (var executor = Executors.newFixedThreadPool(producers)) {
            List<Callable<Void>> jobs = new ArrayList<>();
            for (int i = 0; i < producers; i++) {
                final int id = i;
                jobs.add(() -> {
                    for (int j = 0; j < perProducer; j++)
                        journal.record((((long) id) << 32) | j, 0x80000000, 1, 2);
                    return null;
                });
            }
            for (Future<Void> f : executor.invokeAll(jobs)) f.get();
        }
        equal(producers * perProducer, journal.newestSequence());
        equal(19, journal.retainedCount());
        equal(782, journal.oldestRetainedSequence());
        M3ChangeJournal.Snapshot finalPage = journal.snapshotAfter(0, 19);
        equal(781, finalPage.missedEvents());
        for (int i = 0; i < finalPage.size(); i++) {
            equal(782 + i, finalPage.sequence(i));
            equal(0x80000000, finalPage.changeMask(i));
        }
    }

    private static void check(boolean yes) { if (!yes) throw new AssertionError("expected true"); }
    private static void equal(long expected, long actual) {
        if (expected != actual) throw new AssertionError("expected " + expected + " got " + actual);
    }
    private static void fails(Class<? extends Throwable> wanted, Runnable operation) {
        try { operation.run(); }
        catch (Throwable problem) {
            if (wanted.isInstance(problem)) return;
            throw new AssertionError("wrong exception type", problem);
        }
        throw new AssertionError("missing " + wanted.getSimpleName());
    }
}
