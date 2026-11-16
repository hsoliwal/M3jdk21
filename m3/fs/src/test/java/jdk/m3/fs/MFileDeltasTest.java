/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.fs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Pairwise differential and ordinal-mask tests against the existing delta oracle. */
class MFileDeltasTest {
    private static final int PRESENT_FILE = MFileObservation.EXISTS_KNOWN
            | MFileObservation.EXISTS | MFileObservation.TYPE_KNOWN
            | MFileObservation.FILE | MFileObservation.SIZE_KNOWN
            | MFileObservation.MODIFIED_KNOWN;

    private static MFileObservation file(String key, long size, long modified) {
        return MFileObservation.metadata(key, PRESENT_FILE, size, modified,
                MFileObservation.ChangeStamp.unavailable());
    }

    @Test
    void matchedObservationsRetainExactReasonsAndSelectStableOrdinals() {
        MFileObservation[] before = {
            file("file:a", 4, 1), file("file:b", 3, 1), MFileObservation.absent("file:c")
        };
        MFileObservation[] after = {
            file("file:a", 4, 1), file("file:b", 3, 2), file("file:c", 5, 1)
        };
        MFileDeltas.Batch batch = MFileDeltas.betweenAligned(before, after);
        assertEquals(3, batch.size());
        for (int ordinal = 0; ordinal < 3; ordinal++) {
            assertEquals(MFileDelta.between(before[ordinal], after[ordinal]).reasons(),
                    batch.reasonsAt(ordinal));
        }
        assertArrayEquals(new int[] {1},
                batch.selectedOrdinals(MFileDelta.MODIFIED_CHANGED, MFileDelta.ADDED));
        assertArrayEquals(new int[] {2}, batch.selectedOrdinals(MFileDelta.ADDED, 0));
        assertEquals(3, batch.count(0, 0));
        assertEquals(0, batch.count(MFileDelta.ADDED, MFileDelta.ADDED));
        assertEquals(MFileDelta.State.UNKNOWN, batch.deltaAt(0).content());
    }

    @Test
    void sourceArraysAreNotRetainedAndNoDeltaObjectsAreRetained() {
        MFileObservation original = file("file:s", 1, 1);
        MFileObservation updated = file("file:s", 2, 1);
        MFileObservation[] before = {original};
        MFileObservation[] after = {updated};
        MFileDeltas.Batch batch = MFileDeltas.betweenAligned(before, after);
        long saved = batch.reasonsAt(0);
        before[0] = MFileObservation.absent("file:s");
        after[0] = MFileObservation.absent("file:s");
        assertEquals(saved, batch.reasonsAt(0));
        assertEquals(saved, batch.deltaAt(0).reasons());
        assertNotSame(batch.deltaAt(0), batch.deltaAt(0));
    }

    @Test
    void listOverloadAndCallbacksUseOriginalOrdinalsAndStopOnExceptions() {
        List<MFileObservation> before = List.of(file("a", 1, 1), file("b", 2, 1));
        List<MFileObservation> after = List.of(file("a", 1, 2), file("b", 2, 1));
        MFileDeltas.Batch batch = MFileDeltas.betweenAligned(before, after);
        List<Integer> ordinals = new ArrayList<>();
        batch.forEachOrdinal(MFileDelta.MODIFIED_CHANGED, 0, ordinals::add);
        assertEquals(List.of(0), ordinals);
        AtomicInteger calls = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException("intentional");
        assertEquals(failure, assertThrows(IllegalStateException.class,
                () -> batch.forEachDelta(0, 0, delta -> {
                    calls.incrementAndGet();
                    throw failure;
                })));
        assertEquals(1, calls.get());
        assertThrows(NullPointerException.class, () -> batch.forEachOrdinal(0, 0, null));
        assertThrows(NullPointerException.class, () -> batch.forEachDelta(0, 0, null));
    }

    @Test
    void rejectsMisalignmentNullAndUnobservedDeletion() {
        MFileObservation a = file("file:a", 1, 1);
        MFileObservation b = file("file:b", 1, 1);
        assertThrows(NullPointerException.class,
                () -> MFileDeltas.betweenAligned((MFileObservation[]) null, new MFileObservation[0]));
        assertThrows(IllegalArgumentException.class,
                () -> MFileDeltas.betweenAligned(new MFileObservation[] {a}, new MFileObservation[0]));
        assertThrows(IllegalArgumentException.class,
                () -> MFileDeltas.betweenAligned(new MFileObservation[] {a}, new MFileObservation[] {b}));
        assertThrows(NullPointerException.class,
                () -> MFileDeltas.betweenAligned(new MFileObservation[] {a}, new MFileObservation[] {null}));
        assertThrows(IndexOutOfBoundsException.class,
                () -> MFileDeltas.betweenAligned(new MFileObservation[0], new MFileObservation[0]).deltaAt(0));
        assertEquals(0, MFileDeltas.betweenAligned(List.of(), List.of()).size());
        // An omitted name is NOT treated as evidence of deletion. It cannot be
        // passed as an unequal-length pair: the provider must observe absence.
        MFileDeltas.Batch explicit = MFileDeltas.betweenAligned(
                new MFileObservation[] {a},
                new MFileObservation[] {MFileObservation.absent("file:a")});
        assertTrue((explicit.reasonsAt(0) & MFileDelta.REMOVED) != 0);
    }

    @Test
    void allReasonMasksStay64BitAndSinglePairOracleMatchesRandomizedBatch() {
        final int n = 4096;
        MFileObservation[] before = new MFileObservation[n];
        MFileObservation[] after = new MFileObservation[n];
        for (int i = 0; i < n; i++) {
            String key = "provider:item:" + i;
            before[i] = file(key, i, i);
            after[i] = i % 11 == 0 ? MFileObservation.absent(key)
                    : file(key, i + (i % 7 == 0 ? 1 : 0), i + (i % 5 == 0 ? 1 : 0));
        }
        MFileDeltas.Batch batch = MFileDeltas.betweenAligned(before, after);
        for (int i = 0; i < n; i++) {
            assertEquals(MFileDelta.between(before[i], after[i]).reasons(), batch.reasonsAt(i));
        }
        int[] direct = batch.selectedOrdinals(MFileDelta.SIZE_CHANGED, MFileDelta.REMOVED);
        int[] visited = new int[direct.length];
        AtomicInteger cursor = new AtomicInteger();
        batch.forEachOrdinal(MFileDelta.SIZE_CHANGED, MFileDelta.REMOVED,
                ordinal -> visited[cursor.getAndIncrement()] = ordinal);
        assertArrayEquals(direct, visited);
        assertEquals(direct.length, batch.count(MFileDelta.SIZE_CHANGED, MFileDelta.REMOVED));
        assertTrue(Arrays.stream(direct).allMatch(i -> i % 7 == 0 && i % 11 != 0));
    }

    @Test
    void completeListingsAlignByKeyAndReportCreatedDeletedChanged() {
        MFileObservation[] before = {file("file:a", 1, 1), file("file:b", 2, 1), file("file:c", 3, 1)};
        MFileObservation[] after = {file("file:b", 2, 2), file("file:c", 3, 1), file("file:d", 4, 1)};
        MFileDeltas.Batch batch = MFileDeltas.betweenCompleteListings(before, after);
        assertEquals(4, batch.size());
        assertArrayEquals(new int[] {0}, batch.selectedOrdinals(MFileDelta.REMOVED, 0));
        assertArrayEquals(new int[] {3}, batch.selectedOrdinals(MFileDelta.ADDED, 0));
        assertArrayEquals(new int[] {1}, batch.selectedOrdinals(MFileDelta.MODIFIED_CHANGED, 0));
        assertEquals(0L, batch.reasonsAt(2) & (MFileDelta.ADDED | MFileDelta.REMOVED
                | MFileDelta.MODIFIED_CHANGED | MFileDelta.SIZE_CHANGED));
        assertEquals("file:a", batch.deltaAt(0).before().sourceKey());
        assertEquals(MFileObservation.Presence.ABSENT, batch.deltaAt(0).after().presence());
        assertEquals(MFileObservation.Presence.ABSENT, batch.deltaAt(3).before().presence());
        assertEquals("file:d", batch.deltaAt(3).after().sourceKey());
        // Inputs are copied: replacing an element afterwards changes nothing.
        long saved = batch.reasonsAt(1);
        after[0] = file("file:b", 9, 9);
        assertEquals(saved, batch.reasonsAt(1));
    }

    @Test
    void completeListingsMatchTheAlignedOracleOnRandomizedListings() {
        java.util.Random random = new java.util.Random(20261116L);
        for (int round = 0; round < 64; round++) {
            java.util.TreeMap<String, MFileObservation> oldByKey = new java.util.TreeMap<>();
            java.util.TreeMap<String, MFileObservation> newByKey = new java.util.TreeMap<>();
            int universe = 1 + random.nextInt(300);
            for (int i = 0; i < universe; i++) {
                String key = "provider:item:" + random.nextInt(400);
                if (random.nextInt(4) != 0) oldByKey.put(key, file(key, random.nextInt(3), random.nextInt(3)));
                if (random.nextInt(4) != 0) newByKey.put(key, file(key, random.nextInt(3), random.nextInt(3)));
            }
            java.util.TreeSet<String> keys = new java.util.TreeSet<>(oldByKey.keySet());
            keys.addAll(newByKey.keySet());
            List<MFileObservation> oracleOld = new ArrayList<>();
            List<MFileObservation> oracleNew = new ArrayList<>();
            for (String key : keys) {
                oracleOld.add(oldByKey.getOrDefault(key, MFileObservation.absent(key)));
                oracleNew.add(newByKey.getOrDefault(key, MFileObservation.absent(key)));
            }
            MFileDeltas.Batch expected = MFileDeltas.betweenAligned(oracleOld, oracleNew);
            MFileDeltas.Batch actual = MFileDeltas.betweenCompleteListings(
                    oldByKey.values().toArray(new MFileObservation[0]),
                    newByKey.values().toArray(new MFileObservation[0]));
            assertEquals(expected.size(), actual.size());
            int ordinal = 0;
            for (String key : keys) {
                assertEquals(expected.reasonsAt(ordinal), actual.reasonsAt(ordinal));
                assertEquals(key, actual.deltaAt(ordinal).before().sourceKey());
                assertEquals(key, actual.deltaAt(ordinal).after().sourceKey());
                ordinal++;
            }
        }
    }

    @Test
    void completeListingsRejectUnsortedRepeatedAndNullInputs() {
        MFileObservation a = file("file:a", 1, 1);
        MFileObservation b = file("file:b", 1, 1);
        MFileObservation[] none = new MFileObservation[0];
        assertThrows(NullPointerException.class, () -> MFileDeltas.betweenCompleteListings(null, none));
        assertThrows(NullPointerException.class, () -> MFileDeltas.betweenCompleteListings(none, null));
        assertThrows(NullPointerException.class,
                () -> MFileDeltas.betweenCompleteListings(new MFileObservation[] {a, null}, none));
        assertThrows(IllegalArgumentException.class,
                () -> MFileDeltas.betweenCompleteListings(new MFileObservation[] {b, a}, none));
        assertThrows(IllegalArgumentException.class,
                () -> MFileDeltas.betweenCompleteListings(none, new MFileObservation[] {a, a}));
        assertEquals(0, MFileDeltas.betweenCompleteListings(none, none).size());
        MFileDeltas.Batch created = MFileDeltas.betweenCompleteListings(none, new MFileObservation[] {a, b});
        assertArrayEquals(new int[] {0, 1}, created.selectedOrdinals(MFileDelta.ADDED, 0));
        MFileDeltas.Batch deleted = MFileDeltas.betweenCompleteListings(new MFileObservation[] {a, b}, none);
        assertArrayEquals(new int[] {0, 1}, deleted.selectedOrdinals(MFileDelta.REMOVED, 0));
        // An explicit absence in a listing pairs with an omission as "still absent", not as a change.
        MFileDeltas.Batch stillAbsent = MFileDeltas.betweenCompleteListings(
                new MFileObservation[] {MFileObservation.absent("file:a")}, none);
        assertEquals(0L, stillAbsent.reasonsAt(0) & (MFileDelta.ADDED | MFileDelta.REMOVED));
    }
}
