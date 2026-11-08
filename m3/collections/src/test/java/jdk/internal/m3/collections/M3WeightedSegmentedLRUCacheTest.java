// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class M3WeightedSegmentedLRUCacheTest {

    @Test
    void probationHitPromotesAndOnePassScanDoesNotImmediatelyEvictHotEntry() {
        M3WeightedSegmentedLRUCache<String, String> cache =
                new M3WeightedSegmentedLRUCache<>(4, 40L, 0.75d, (key, value) -> 10L);

        cache.put("hot", "h");
        assertTrue(cache.get("hot").isPresent());

        cache.put("scan-1", "1");
        cache.put("scan-2", "2");
        cache.put("scan-3", "3");
        cache.put("scan-4", "4");
        cache.put("scan-5", "5");

        assertTrue(cache.get("hot").isPresent());
        assertTrue(cache.snapshot().protectedHits() >= 1L);
        assertEquals(1, cache.snapshot().protectedEntries());
        assertTrue(cache.snapshot().probationEvictions() >= 2L);
        assertEquals(0L, cache.snapshot().protectedEvictions());
    }

    @Test
    void protectedOverflowDemotesItsLruInsteadOfDroppingIt() {
        M3WeightedSegmentedLRUCache<String, String> cache =
                new M3WeightedSegmentedLRUCache<>(4, 100L, 0.50d, (key, value) -> 10L);

        cache.put("a", "a");
        cache.put("b", "b");
        cache.put("c", "c");
        cache.get("a");
        cache.get("b");
        cache.get("c");

        assertEquals(2, cache.snapshot().protectedEntries());
        assertEquals(1, cache.snapshot().probationEntries());
        assertTrue(cache.snapshot().demotions() >= 1L);
        assertTrue(cache.containsKey("a"));
        assertTrue(cache.containsKey("b"));
        assertTrue(cache.containsKey("c"));
    }

    @Test
    void hardPressureEvictsProbationBeforeProtected() {
        M3WeightedSegmentedLRUCache<String, String> cache =
                new M3WeightedSegmentedLRUCache<>(5, 50L, 0.60d, (key, value) -> 10L);

        cache.put("hot-a", "a");
        cache.put("hot-b", "b");
        cache.get("hot-a");
        cache.get("hot-b");
        cache.put("cold-a", "c");
        cache.put("cold-b", "d");

        M3WeightedSegmentedLRUCache.TrimResult trim = cache.trimTo(2, 20L);

        assertEquals(2, trim.evictedEntries());
        assertTrue(cache.containsKey("hot-a"));
        assertTrue(cache.containsKey("hot-b"));
        assertFalse(cache.containsKey("cold-a"));
        assertFalse(cache.containsKey("cold-b"));
        assertEquals(0L, cache.snapshot().protectedEvictions());
    }

    @Test
    void weightPressureKeepsExactAccountingAcrossPromotionDemotionAndReplacement() {
        M3WeightedSegmentedLRUCache<String, String> cache =
                new M3WeightedSegmentedLRUCache<>(
                        8,
                        14L,
                        0.50d,
                        (key, value) -> value.length());

        cache.put("a", "1111");
        cache.put("b", "2222");
        cache.get("a");
        cache.put("c", "3333");
        cache.put("a", "123456");

        var snapshot = cache.snapshot();
        assertTrue(snapshot.weight() <= 14L);
        assertEquals(snapshot.entries(),
                snapshot.probationEntries() + snapshot.protectedEntries());
        assertEquals(snapshot.weight(),
                snapshot.probationWeight() + snapshot.protectedWeight());
        assertEquals(snapshot.hits(),
                snapshot.probationHits() + snapshot.protectedHits());
    }

    @Test
    void oversizeAdmissionDoesNotDestroyProtectedWorkingSet() {
        M3WeightedSegmentedLRUCache<String, String> cache =
                new M3WeightedSegmentedLRUCache<>(4, 4L, 0.75d, (key, value) -> value.length());

        cache.put("hot", "11");
        cache.get("hot");
        var rejected = cache.put("huge", "12345");

        assertFalse(rejected.admitted());
        assertTrue(cache.get("hot").isPresent());
        assertEquals(1L, cache.snapshot().oversizeRejections());
    }

    @Test
    void predicateRemovalAndClearPreserveBoundsAndEvidence() {
        M3WeightedSegmentedLRUCache<String, String> cache =
                new M3WeightedSegmentedLRUCache<>(8, 100L, 0.75d, (key, value) -> 5L);

        cache.put("a", "a");
        cache.put("b", "b");
        cache.get("a");
        cache.put("c", "c");

        var removed = cache.removeIf(key -> key.equals("b"));
        assertEquals(1, removed.removedEntries());
        assertEquals(5L, removed.removedWeight());

        List<String> order = cache.keysInLruOrder();
        assertEquals(2, order.size());
        assertTrue(order.contains("a"));
        assertTrue(order.contains("c"));

        var cleared = cache.clear();
        assertEquals(2, cleared.removedEntries());
        assertEquals(0, cache.size());
        assertEquals(0L, cache.weight());
    }

    @Test
    void admissionPlanIsNonMutatingAndMatchesProbationThenProtectedPriority() {
        M3WeightedSegmentedLRUCache<String, String> cache =
                new M3WeightedSegmentedLRUCache<>(
                        3,
                        10L,
                        0.5d,
                        (key, value) -> value.length());

        cache.put("A", "aa");
        cache.put("B", "bbbb");
        cache.put("C", "cc");
        cache.get("A"); // A earns protected residency.

        var before = cache.snapshot();
        var plan = cache.admissionVictims(10L);
        var after = cache.snapshot();

        assertEquals(before, after);
        assertTrue(plan.size() >= 2);
        assertFalse(plan.getFirst().protectedResident());
        assertEquals("B", plan.getFirst().key());
        assertTrue(
                plan.stream().anyMatch(
                        M3WeightedSegmentedLRUCache.AdmissionVictim::protectedResident));
    }

}
