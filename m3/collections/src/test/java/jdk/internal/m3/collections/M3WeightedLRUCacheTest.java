// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class M3WeightedLRUCacheTest {

    @Test
    void evictsLeastRecentlyUsedByEntryLimit() {
        M3WeightedLRUCache<String, String> cache =
                new M3WeightedLRUCache<>(2, 1_000L, (key, value) -> value.length());

        cache.put("a", "111");
        cache.put("b", "22");
        assertTrue(cache.get("a").isPresent());
        cache.put("c", "3");

        assertTrue(cache.get("a").isPresent());
        assertFalse(cache.get("b").isPresent());
        assertTrue(cache.get("c").isPresent());
        assertEquals(List.of("a", "c"), cache.keysInLruOrder());
    }

    @Test
    void trimsByWeightEvenWhenEntryCapacityAllowsMore() {
        M3WeightedLRUCache<String, String> cache =
                new M3WeightedLRUCache<>(10, 6L, (key, value) -> value.length());

        cache.put("a", "111");
        cache.put("b", "22");
        M3WeightedLRUCache.PutResult result = cache.put("c", "44");

        assertTrue(result.admitted());
        assertEquals(1, result.trim().evictedEntries());
        assertFalse(cache.get("a").isPresent());
        assertEquals(4L, cache.weight());
    }

    @Test
    void oversizeAdmissionDoesNotDestroyHotWorkingSet() {
        M3WeightedLRUCache<String, String> cache =
                new M3WeightedLRUCache<>(4, 4L, (key, value) -> value.length());

        cache.put("a", "11");
        cache.put("b", "22");
        M3WeightedLRUCache.PutResult rejected = cache.put("huge", "12345");

        assertFalse(rejected.admitted());
        assertTrue(cache.get("a").isPresent());
        assertTrue(cache.get("b").isPresent());
        assertFalse(cache.get("huge").isPresent());
        assertEquals(1L, cache.snapshot().oversizeRejections());
    }

    @Test
    void temporaryPressureTrimCanDropToZeroWithoutChangingHardConfiguration() {
        M3WeightedLRUCache<String, String> cache =
                new M3WeightedLRUCache<>(4, 100L, (key, value) -> 10L);

        cache.put("a", "a");
        cache.put("b", "b");
        cache.put("c", "c");

        M3WeightedLRUCache.TrimResult trimmed = cache.trimTo(4, 10L);
        assertEquals(2, trimmed.evictedEntries());
        assertEquals(20L, trimmed.evictedWeight());
        assertEquals(1, cache.size());

        cache.put("d", "d");
        assertEquals(2, cache.size());
        assertEquals(100L, cache.maxWeight());
    }

    @Test
    void predicateRemovalPreservesRelativeRecencyOfRemainingEntries() {
        M3WeightedLRUCache<String, String> cache =
                new M3WeightedLRUCache<>(8, 1_000L, (key, value) -> 1L);

        cache.put("a", "a");
        cache.put("b", "b");
        cache.put("c", "c");
        cache.get("a");

        M3WeightedLRUCache.RemoveResult removed =
                cache.removeIf(key -> key.equals("b"));

        assertEquals(1, removed.removedEntries());
        assertEquals(List.of("c", "a"), cache.keysInLruOrder());
    }

    @Test
    void replacingEntryReweightsAndPromotesIt() {
        M3WeightedLRUCache<String, String> cache =
                new M3WeightedLRUCache<>(3, 5L, (key, value) -> value.length());

        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");

        M3WeightedLRUCache.PutResult result = cache.put("a", "111");

        assertTrue(result.admitted());
        assertTrue(result.replaced());
        assertEquals(3L, result.admittedWeight());
        assertEquals(5L, cache.weight());
        assertEquals(List.of("b", "c", "a"), cache.keysInLruOrder());
    }

    @Test
    void rejectsNonPositiveWeightsBeforeMutation() {
        M3WeightedLRUCache<String, String> cache =
                new M3WeightedLRUCache<>(4, 100L, (key, value) -> 0L);

        assertThrows(IllegalArgumentException.class, () -> cache.put("a", "value"));
        assertEquals(0, cache.size());
        assertEquals(0L, cache.weight());
    }

    @Test
    void synchronizedOperationsPreserveBoundsUnderConcurrentAccess() throws Exception {
        M3WeightedLRUCache<String, String> cache =
                new M3WeightedLRUCache<>(32, 320L, (key, value) -> 10L);

        try (var executor = Executors.newFixedThreadPool(4)) {
            ArrayList<Future<?>> futures = new ArrayList<>();
            for (int worker = 0; worker < 4; worker++) {
                int workerId = worker;
                futures.add(
                        executor.submit(
                                () -> {
                                    for (int index = 0; index < 2_000; index++) {
                                        String key = workerId + ":" + (index & 31);
                                        cache.put(key, "v");
                                        cache.get(key);
                                    }
                                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        }

        assertTrue(cache.size() <= cache.maxEntries());
        assertTrue(cache.weight() <= cache.maxWeight());
        assertTrue(cache.snapshot().hits() > 0L);
    }

    @Test
    void peekDoesNotPromoteOrChangeLookupStatistics() {
        M3WeightedLRUCache<String, String> cache =
                new M3WeightedLRUCache<>(3, 100L, (key, value) -> value.length());
        cache.put("a", "1");
        cache.put("b", "22");
        cache.put("c", "333");
        var before = cache.snapshot();
        var orderBefore = cache.keysInLruOrder();

        assertEquals("1", cache.peek("a").orElseThrow());
        assertEquals(1L, cache.peekWeight("a").orElseThrow());

        assertEquals(orderBefore, cache.keysInLruOrder());
        assertEquals(before.hits(), cache.snapshot().hits());
        assertEquals(before.misses(), cache.snapshot().misses());
    }

    @Test
    void lighterMaintenanceReplacementPreservesRecencyAndReleasesWeight() {
        M3WeightedLRUCache<String, String> cache =
                new M3WeightedLRUCache<>(3, 100L, (key, value) -> value.length());
        cache.put("a", "11111");
        cache.put("b", "2222");
        cache.put("c", "333");
        var orderBefore = cache.keysInLruOrder();
        var countersBefore = cache.snapshot();

        M3WeightedLRUCache.MaintenanceReplaceResult result =
                cache.replaceIfLighterPreservingRecency("b", "2");

        assertTrue(result.present());
        assertTrue(result.replaced());
        assertEquals(4L, result.previousWeight());
        assertEquals(1L, result.replacementWeight());
        assertEquals(3L, result.releasedWeight());
        assertEquals(orderBefore, cache.keysInLruOrder());
        assertEquals(9L, cache.weight());
        assertEquals(countersBefore.puts(), cache.snapshot().puts());
        assertEquals(countersBefore.replacements(), cache.snapshot().replacements());
        assertEquals("2", cache.peek("b").orElseThrow());
    }

    @Test
    void heavierMaintenanceReplacementIsRejectedWithoutMutation() {
        M3WeightedLRUCache<String, String> cache =
                new M3WeightedLRUCache<>(2, 10L, (key, value) -> value.length());
        cache.put("a", "1");
        cache.put("b", "22");
        var orderBefore = cache.keysInLruOrder();
        long weightBefore = cache.weight();

        M3WeightedLRUCache.MaintenanceReplaceResult result =
                cache.replaceIfLighterPreservingRecency("a", "12345");

        assertTrue(result.present());
        assertFalse(result.replaced());
        assertEquals(1L, result.previousWeight());
        assertEquals(5L, result.replacementWeight());
        assertEquals(0L, result.releasedWeight());
        assertEquals(orderBefore, cache.keysInLruOrder());
        assertEquals(weightBefore, cache.weight());
        assertEquals("1", cache.peek("a").orElseThrow());
    }

    @Test
    void maintenanceReplacementNeverAdmitsMissingKey() {
        M3WeightedLRUCache<String, String> cache =
                new M3WeightedLRUCache<>(2, 10L, (key, value) -> value.length());

        M3WeightedLRUCache.MaintenanceReplaceResult result =
                cache.replaceIfLighterPreservingRecency("missing", "1");

        assertFalse(result.present());
        assertFalse(result.replaced());
        assertEquals(0, cache.size());
    }

}
