// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.nlp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Exact signed ID, ranked frequency and frozen-snapshot regression for the private top-k atom. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-indexstring/src/test/java/com/synexia/indexstring/MIndexScopedNgramsTopKAdmissionTest.java blob dc7ed3650647; recipe m3/nlp/recipe/relocate.py v1
class M3ScopedNgramsTopKAdmissionTest {
    private static final M3ScopedNgrams.Scope SCOPE =
            new M3ScopedNgrams.Scope("topk-oracle", "v1", "en", 3, "signed-id");

    @Test
    void rankedSignedIdsAndProbabilitiesRemainExact() {
        var model = new M3ScopedNgrams(SCOPE, 3, 100, 20);
        model.add(SCOPE, new long[]{7, -2}, 5);
        model.add(SCOPE, new long[]{7, 17}, 5);
        model.add(SCOPE, new long[]{7, Long.MIN_VALUE}, 5);
        model.add(SCOPE, new long[]{7, Long.MAX_VALUE}, 1);
        model.add(SCOPE, new long[]{7, 0}, 6);
        var frozen = model.freeze();
        assertEquals(List.of(
                new M3ScopedNgrams.Candidate(0, 6, 6.0 / 22.0),
                new M3ScopedNgrams.Candidate(Long.MIN_VALUE, 5, 5.0 / 22.0),
                new M3ScopedNgrams.Candidate(-2, 5, 5.0 / 22.0)),
                frozen.top(new long[]{7}, 3));
        assertEquals(22, frozen.contextTotal(7));
        assertEquals(5, frozen.count(7, -2));
        assertEquals(5, frozen.top(new long[]{7}, 4096).size());
        assertTrue(frozen.top(new long[]{Long.MAX_VALUE}, 2).isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> frozen.top(new long[]{7}, 3).clear());
    }

    @Test
    void inputValidationAndSourceAtomicityRemainUnchanged() {
        var model = new M3ScopedNgrams(SCOPE, 3, 100, 20);
        model.observe(SCOPE, new long[]{Long.MIN_VALUE, 0, Long.MAX_VALUE}, 2);
        var frozen = model.freeze();
        assertThrows(NullPointerException.class, () -> frozen.top(null, 1));
        for (int k : new int[]{-1, 0, 4097, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> frozen.top(new long[0], k));
        }
        assertThrows(IllegalArgumentException.class,
                () -> frozen.top(new long[64], 1));
        assertThrows(IllegalArgumentException.class,
                () -> model.observe(new M3ScopedNgrams.Scope("different", "v1", "en", 3, "signed-id"), new long[]{1}, 1));
        var original = frozen.top(new long[]{Long.MIN_VALUE}, 2);
        model.add(SCOPE, new long[]{Long.MIN_VALUE, 1}, 100);
        assertEquals(original, frozen.top(new long[]{Long.MIN_VALUE}, 2));
        assertFalse(model.freeze().top(new long[]{Long.MIN_VALUE}, 2).equals(original));
    }

    @Test
    void emptyCandidateAndMultiOrderPrefixIsolation() {
        var model = new M3ScopedNgrams(SCOPE, 4, 100, 20);
        model.observe(SCOPE, new long[]{1, 2, 3, 4}, 3);
        var snapshot = model.freeze();
        assertEquals(List.of(new M3ScopedNgrams.Candidate(2, 3, 1.0)),
                snapshot.top(new long[]{1}, 4));
        assertEquals(List.of(new M3ScopedNgrams.Candidate(3, 3, 1.0)),
                snapshot.top(new long[]{1, 2}, 4));
        assertEquals(List.of(new M3ScopedNgrams.Candidate(4, 3, 1.0)),
                snapshot.top(new long[]{1, 2, 3}, 4));
        assertTrue(snapshot.top(new long[]{1, 2, 3, 4}, 4).isEmpty());
    }
}
