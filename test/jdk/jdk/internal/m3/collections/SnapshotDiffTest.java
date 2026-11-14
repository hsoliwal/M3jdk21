/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * Licensed under the Apache License, Version 2.0.
 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SnapshotDiffTest {

    private static final String SHA =
            "BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD";

    @Test
    void classifiesAndSortsChangesIndependentlyOfMapIterationOrder() {
        Map<String, String> previous = new LinkedHashMap<>();
        previous.put("z", "same");
        previous.put("b", "old");
        previous.put("r", "removed");

        Map<String, String> current = new LinkedHashMap<>();
        current.put("n", "new");
        current.put("z", "same");
        current.put("b", "new-value");
        current.put("a", "new-too");

        SnapshotDelta delta = SnapshotDiff.diff(previous, current);

        assertEquals(List.of("a", "n"), delta.insertedKeys());
        assertEquals(List.of("b"), delta.updatedKeys());
        assertEquals(List.of("r"), delta.removedKeys());
        assertEquals(4, delta.totalChanges());
        assertTrue(delta.hasChanges());
    }

    @Test
    void supportsCallerOwnedDeterministicEquivalence() {
        SnapshotDelta delta = SnapshotDiff.diff(
                Map.of("A", "VALUE"),
                Map.of("A", "value"),
                (left, right) -> left.equalsIgnoreCase(right));

        assertEquals(SnapshotDelta.empty(), delta);
        assertFalse(delta.hasChanges());
    }

    @Test
    void deltaDefensivelyCopiesAndRejectsAmbiguousClassification() {
        var inserted = new java.util.ArrayList<>(List.of("b", "a"));
        SnapshotDelta delta = new SnapshotDelta(inserted, List.of(), List.of());
        inserted.add("later");

        assertEquals(List.of("a", "b"), delta.insertedKeys());
        assertThrows(UnsupportedOperationException.class, () -> delta.insertedKeys().add("x"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SnapshotDelta(List.of("same"), List.of("same"), List.of()));
    }

    @Test
    void snapshotIdentityNormalizesMetadataButNotContentSemantics() {
        SnapshotIdentity identity = new SnapshotIdentity(" source ", " cursor-7 ", SHA, 12);

        assertEquals("source", identity.sourceId());
        assertEquals("cursor-7", identity.sourceVersion());
        assertEquals(SHA.toLowerCase(java.util.Locale.ROOT), identity.contentSha256());
        assertEquals(12, identity.itemCount());
        assertThrows(IllegalArgumentException.class, () -> new SnapshotIdentity("s", "v", "xyz", 1));
        assertThrows(IllegalArgumentException.class, () -> new SnapshotIdentity("s", "v", SHA, -1));
    }

    @Test
    void legacyCountResultProjectsFromRichDelta() {
        DiffResult result = DiffResult.from(
                new SnapshotDelta(List.of("i2", "i1"), List.of("u"), List.of("r")));

        assertEquals(2, result.getInserted());
        assertEquals(1, result.getUpdated());
        assertEquals(1, result.getRemoved());
        assertEquals(4, result.getTotalChanges());
        assertEquals(result, DiffResult.builder().inserted(2).updated(1).removed(1).build());
        assertTrue(result.hasChanges());
    }

    @Test
    void legacyCountResultPreservesRawIntArithmeticAndPositiveChangePredicate() {
        DiffResult negative = DiffResult.builder().inserted(-1).updated(0).removed(0).build();
        assertEquals(-1, negative.totalChanges());
        assertFalse(negative.hasChanges());

        DiffResult overflow = DiffResult.builder().inserted(Integer.MAX_VALUE).updated(1).removed(0).build();
        assertEquals(Integer.MIN_VALUE, overflow.totalChanges());
        assertFalse(overflow.hasChanges());
    }

    @Test
    void nullKeyIsRejectedInsteadOfBecomingAnUnstableIdentity() {
        Map<String, String> current = new LinkedHashMap<>();
        current.put(null, "value");

        assertThrows(NullPointerException.class, () -> SnapshotDiff.diff(Map.of(), current));
    }
}
