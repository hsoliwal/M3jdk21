/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
/*
 * @test
 * @summary M3JDK21 bounded viewport journal batches, wraparound, stale cursors and stable replay
 * @run main ChangeJournalWindowTest
 */
import java.util.ArrayList;
import java.util.ChangeJournal;
import java.util.List;
import java.util.Random;

public class ChangeJournalWindowTest {
    private static void check(boolean yes, String message) {
        if (!yes) throw new AssertionError(message);
    }

    private static List<String> events(ChangeJournal.Batch batch) {
        List<String> answer = new ArrayList<>();
        batch.forEach((v, k, i, removed, added, previous, next) ->
                answer.add(v + ":" + k + ":" + i + ":" + removed + ":" + added
                        + ":" + previous + ":" + next));
        return answer;
    }

    private static void windowAndWrap() {
        ChangeJournal journal = new ChangeJournal(3);
        for (int i = 0; i < 8; i++) {
            journal.append(ChangeJournal.Kind.INSERT, i, 0, 1, null, "value-" + i);
        }
        check(journal.oldestCursor() == 5, "oldest after rollover");
        ChangeJournal.Batch empty = journal.snapshot(5, 0);
        check(empty.size() == 0 && empty.cursor() == 5, "zero budget");
        ChangeJournal.Batch first = journal.snapshot(5, 2);
        check(first.size() == 2 && first.cursor() == 7, "prefix cursor");
        check(events(first).equals(List.of(
                "6:INSERT:5:0:1:null:value-5", "7:INSERT:6:0:1:null:value-6")), "prefix payload");
        ChangeJournal.Batch last = journal.snapshot(first.cursor(), 2);
        check(last.size() == 1 && last.cursor() == 8, "final cursor");
        check(events(last).equals(List.of("8:INSERT:7:0:1:null:value-7")), "final payload");
        check(events(journal.snapshot(5)).equals(List.of(
                "6:INSERT:5:0:1:null:value-5", "7:INSERT:6:0:1:null:value-6",
                "8:INSERT:7:0:1:null:value-7")), "legacy full snapshot parity");
        journal.append(ChangeJournal.Kind.RESET, 0, 0, 0, null, null);
        check(events(first).size() == 2, "immutable reader snapshot");
        try { journal.snapshot(5, 0); throw new AssertionError("stale zero-limit accepted"); }
        catch (IllegalStateException expected) { /* fail closed */ }
    }

    private static void rejectedBounds() {
        ChangeJournal journal = new ChangeJournal(2);
        try { journal.snapshot(0, -1); throw new AssertionError("negative limit accepted"); }
        catch (IllegalArgumentException expected) { /* required */ }
        try { journal.snapshot(1, 0); throw new AssertionError("future cursor accepted"); }
        catch (IllegalArgumentException expected) { /* required */ }
        journal.append(ChangeJournal.Kind.REPLACE, 0, 1, 1, "old", "new");
        ChangeJournal.Batch batch = journal.snapshot(0, Integer.MAX_VALUE);
        check(batch.size() == 1 && batch.cursor() == 1, "huge limit bounded by retained count");
        batch.forEach((v, k, i, removed, added, p, n) ->
                journal.append(ChangeJournal.Kind.REMOVE, i, 1, 0, n, null));
        check(journal.version() == 2 && batch.size() == 1, "callback outside journal lock");
    }

    private static void differentialRandom() {
        Random random = new Random(0x3b1ac8eL);
        for (int capacity : new int[] { 1, 2, 3, 7, 16, 31 }) {
            ChangeJournal journal = new ChangeJournal(capacity);
            for (int i = 0; i < 1500; i++) {
                journal.append(ChangeJournal.Kind.values()[i % 4], i, i % 3, i % 5, i, "item-" + i);
                long oldest = journal.oldestCursor();
                long after = oldest + random.nextInt((int) (journal.version() - oldest + 1));
                int bound = random.nextInt(capacity + 3);
                ChangeJournal.Batch full = journal.snapshot(after);
                ChangeJournal.Batch partial = journal.snapshot(after, bound);
                List<String> expected = events(full);
                int count = Math.min(bound, expected.size());
                check(partial.size() == count && partial.cursor() == after + count,
                        "differential bounded cursor: capacity=" + capacity + " step=" + i);
                check(events(partial).equals(expected.subList(0, count)),
                        "differential selected events: capacity=" + capacity + " step=" + i);
            }
        }
    }

    public static void main(String[] args) {
        windowAndWrap();
        rejectedBounds();
        differentialRandom();
        System.out.println("PASS ChangeJournalWindowTest");
    }
}
