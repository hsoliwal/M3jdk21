/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
/*
 * @test
 * @summary M3JDK21 packed change journal: bounded history, stale cursor, stable replay
 * @run main ChangeJournalTest
 */
import java.util.ArrayList;
import java.util.ChangeJournal;
import java.util.List;

public class ChangeJournalTest {
    private static void check(boolean condition, String why) {
        if (!condition) throw new AssertionError(why);
    }

    private static void replayAndPayloads() {
        ChangeJournal journal = new ChangeJournal(4);
        check(journal.version() == 0 && journal.oldestCursor() == 0, "empty journal");
        check(journal.append(ChangeJournal.Kind.INSERT, 0, 0, 1, null, "x") == 1, "first version");
        journal.append(ChangeJournal.Kind.REPLACE, 0, 1, 1, "x", null);
        journal.append(ChangeJournal.Kind.REMOVE, 0, 1, 0, null, null);
        ArrayList<String> actual = new ArrayList<>();
        ChangeJournal.Batch snapshot = journal.snapshot(0);
        snapshot.forEach((v, kind, index, removed, added, before, after) -> actual.add(
                v + ":" + kind + ":" + index + ":" + removed + ":" + added + ":" + before + ":" + after));
        check(actual.equals(List.of("1:INSERT:0:0:1:null:x",
                        "2:REPLACE:0:1:1:x:null", "3:REMOVE:0:1:0:null:null")), "event payloads");
        check(snapshot.cursor() == 3 && snapshot.size() == 3, "batch bounds");
        check(journal.snapshot(snapshot.cursor()).size() == 0, "no duplicate replay");
        journal.append(ChangeJournal.Kind.RESET, 0, 3, 1, null, null);
        journal.append(ChangeJournal.Kind.INSERT, 1, 0, 2, null, null);
        check(journal.oldestCursor() == 1, "bounded overwrite");
        try { journal.snapshot(0); throw new AssertionError("lost history accepted"); }
        catch (IllegalStateException expected) { /* stale replay refused */ }
        check(snapshot.size() == 3, "copied batch independent of writer");
    }

    private static void invalidAndEmpty() {
        try { new ChangeJournal(0); throw new AssertionError("bad capacity accepted"); }
        catch (IllegalArgumentException expected) { /* required */ }
        ChangeJournal journal = new ChangeJournal(2);
        try { journal.append(ChangeJournal.Kind.INSERT, -1, 0, 1, null, null);
              throw new AssertionError("negative coordinate accepted"); }
        catch (IllegalArgumentException expected) { /* required */ }
        check(journal.version() == 0, "failed append did not advance");
        try { journal.snapshot(1); throw new AssertionError("future cursor accepted"); }
        catch (IllegalArgumentException expected) { /* required */ }
        journal.append(ChangeJournal.Kind.INSERT, 0, 0, 1, null, "a");
        ChangeJournal.Batch batch = journal.snapshot(0);
        // Replay does not hold the journal monitor: a callback may append another event.
        batch.forEach((v, kind, index, removed, added, before, after) ->
                journal.append(ChangeJournal.Kind.REMOVE, index, 1, 0, after, null));
        check(journal.version() == 2 && batch.size() == 1, "reentrant consumer independent batch");
    }

    public static void main(String[] args) {
        replayAndPayloads();
        invalidAndEmpty();
        System.out.println("PASS ChangeJournalTest");
    }
}
