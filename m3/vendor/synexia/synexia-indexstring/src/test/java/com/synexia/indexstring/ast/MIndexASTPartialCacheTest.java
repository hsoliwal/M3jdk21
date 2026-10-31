// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class MIndexASTPartialCacheTest {

  private static final String SOURCE_A =
      """
      package demo;
      class A {
        int f(int x) { return x + 1; }
      }
      """;

  private static final String SOURCE_B =
      """
      package demo;
      class B {
        int f(int x) { return x + 2; }
      }
      """;

  private static final String SOURCE_C =
      """
      package demo;
      class C {
        int f(int x) { return x + 3; }
      }
      """;

  @Test
  void evictsLeastRecentlyUsedResidentInConstantTimeSubstrate() throws Exception {
    AtomicInteger loads = new AtomicInteger();
    Map<String, String> sources =
        Map.of("A.java", SOURCE_A, "B.java", SOURCE_B, "C.java", SOURCE_C);

    MIndexASTPartialCache.Loader loader =
        MIndexASTPartialCache.isolatedJava21Loader(
            0,
            key -> {
              loads.incrementAndGet();
              return sources.get(key.sourceRef());
            });

    try (MIndexASTPartialCache cache =
        new MIndexASTPartialCache(
            2,
            100L,
            loader,
            (key, resident) -> 1L,
            MIndexASTPartialCache.PressurePolicy.defaults())) {

      var a = MIndexASTPartialCache.Key.java21Source("A.java", SOURCE_A);
      var b = MIndexASTPartialCache.Key.java21Source("B.java", SOURCE_B);
      var c = MIndexASTPartialCache.Key.java21Source("C.java", SOURCE_C);

      cache.get(a);
      cache.get(b);
      cache.get(a);
      cache.get(c);

      assertTrue(cache.contains(a));
      assertFalse(cache.contains(b));
      assertTrue(cache.contains(c));
      assertEquals(List.of(a, c), cache.keysInLruOrder());
      assertEquals(3, loads.get());

      cache.get(b);
      assertEquals(4, loads.get());
    }
  }

  @Test
  void evictionDropsCacheOwnershipButDoesNotInvalidateActiveCaller() throws Exception {
    Map<String, String> sources = Map.of("A.java", SOURCE_A, "B.java", SOURCE_B);
    try (MIndexASTPartialCache cache =
        new MIndexASTPartialCache(
            1,
            100L,
            MIndexASTPartialCache.isolatedJava21Loader(
                0, key -> sources.get(key.sourceRef())),
            (key, resident) -> 1L,
            MIndexASTPartialCache.PressurePolicy.defaults())) {
      var aKey = MIndexASTPartialCache.Key.java21Source("A.java", SOURCE_A);
      var bKey = MIndexASTPartialCache.Key.java21Source("B.java", SOURCE_B);
      MIndexASTPartialCache.Resident active = cache.get(aKey);
      int occurrences = active.document().occurrenceCount();

      cache.get(bKey);
      assertFalse(cache.contains(aKey));
      assertTrue(cache.contains(bKey));

      assertEquals(occurrences, active.document().occurrenceCount());
      assertEquals(aKey.sourceUtf16Sha256(), active.document().sourceUtf16Sha256Hex());
    }
  }

  @Test
  void loadedSourceIdentityIsVerifiedBeforeAdmission() throws Exception {
    var key = MIndexASTPartialCache.Key.java21Source("A.java", SOURCE_A);
    try (MIndexASTPartialCache cache =
        new MIndexASTPartialCache(
            2,
            1_000_000L,
            MIndexASTPartialCache.isolatedJava21Loader(0, ignored -> SOURCE_B))) {
      MIndexASTPartialCache.LoadException failure =
          org.junit.jupiter.api.Assertions.assertThrows(
              MIndexASTPartialCache.LoadException.class,
              () -> cache.get(key));
      assertEquals(key, failure.key());
      assertFalse(cache.contains(key));
      assertEquals(1L, cache.snapshot().loadFailures());
    }
  }

  @Test
  void cachesJavacPartialTreeEvenWhenSourceHasSyntaxErrors() throws Exception {
    String broken = "class Broken { void x( { int y = 1; }";
    AtomicInteger loads = new AtomicInteger();
    MIndexASTPartialCache.Key key = MIndexASTPartialCache.Key.java21Source("Broken.java", broken);

    try (MIndexASTPartialCache cache =
        new MIndexASTPartialCache(
            4,
            1_000_000L,
            MIndexASTPartialCache.isolatedJava21Loader(
                0,
                ignored -> {
                  loads.incrementAndGet();
                  return broken;
                }))) {
      MIndexASTPartialCache.Resident first = cache.get(key);
      MIndexASTPartialCache.Resident second = cache.get(key);

      assertTrue(first.document().hasErrors());
      assertEquals(1, loads.get());
      assertEquals(first.document(), second.document());
    }
  }

  @Test
  void upgradesHotResidentWithoutChangingDocumentIdentity() throws Exception {
    MIndexASTPartialCache.Key key = MIndexASTPartialCache.Key.java21Source("A.java", SOURCE_A);

    try (MIndexASTPartialCache cache =
        new MIndexASTPartialCache(
            4,
            10_000_000L,
            MIndexASTPartialCache.isolatedJava21Loader(0, ignored -> SOURCE_A))) {
      MIndexASTPartialCache.Resident base = cache.get(key);
      MIndexASTPartialCache.Resident navigation =
          cache.get(key, MIndexASTPartialCache.Detail.NAVIGATION);
      MIndexASTPartialCache.Resident full =
          cache.get(key, MIndexASTPartialCache.Detail.FULL);

      assertNotSame(base, navigation);
      assertEquals(base.document(), navigation.document());
      assertTrue(navigation.navigation().isPresent());
      assertTrue(full.navigation().isPresent());
      assertTrue(full.sourceIndex().isPresent());
      assertTrue(full.constantTimeLca().isPresent());
      assertTrue(full.poolPrecompute().isPresent());
      assertEquals(2L, cache.snapshot().upgrades());

      cache.get(key, MIndexASTPartialCache.Detail.FULL);
      assertEquals(2L, cache.snapshot().upgrades());
    }
  }

  @Test
  void memoryPressureTemporarilyTightensWeightAndEntryBudgets() throws Exception {
    Map<String, String> sources = new HashMap<>();
    for (int index = 0; index < 6; index++) {
      sources.put(
          "F" + index + ".java",
          "class F" + index + " { int x(){ return " + index + "; } }");
    }

    try (MIndexASTPartialCache cache =
        new MIndexASTPartialCache(
            10,
            100L,
            MIndexASTPartialCache.isolatedJava21Loader(
                0, key -> sources.get(key.sourceRef())),
            (key, resident) -> 10L,
            MIndexASTPartialCache.PressurePolicy.defaults())) {
      for (Map.Entry<String, String> source : sources.entrySet()) {
        cache.get(MIndexASTPartialCache.Key.java21Source(source.getKey(), source.getValue()));
      }

      assertEquals(6, cache.snapshot().lru().entries());
      assertEquals(60L, cache.snapshot().lru().weight());

      MIndexASTPartialCache.PressureResult high =
          cache.rebalance(new MIndexASTPartialCache.HeapUsage(85L, 100L));
      assertEquals(MIndexASTPartialCache.PressureLevel.HIGH, high.level());
      assertEquals(5, high.targetEntries());
      assertEquals(50L, high.targetWeight());
      assertEquals(1, high.trim().evictedEntries());
      assertEquals(5, cache.snapshot().lru().entries());

      MIndexASTPartialCache.PressureResult critical =
          cache.rebalance(new MIndexASTPartialCache.HeapUsage(92L, 100L));
      assertEquals(MIndexASTPartialCache.PressureLevel.CRITICAL, critical.level());
      assertEquals(2, critical.targetEntries());
      assertEquals(25L, critical.targetWeight());
      assertEquals(2, cache.snapshot().lru().entries());

      MIndexASTPartialCache.PressureResult emergency =
          cache.rebalance(new MIndexASTPartialCache.HeapUsage(97L, 100L));
      assertEquals(MIndexASTPartialCache.PressureLevel.EMERGENCY, emergency.level());
      assertEquals(0, emergency.targetEntries());
      assertEquals(0L, emergency.targetWeight());
      assertEquals(0, cache.snapshot().lru().entries());
    }
  }

  @Test
  void pressureBudgetRemainsActiveUntilAHealthierSampleRelaxesIt() throws Exception {
    Map<String, String> sources = new HashMap<>();
    for (int index = 0; index < 8; index++) {
      sources.put(
          "P" + index + ".java",
          "class P" + index + " { int x(){ return " + index + "; } }");
    }

    try (MIndexASTPartialCache cache =
        new MIndexASTPartialCache(
            8,
            80L,
            MIndexASTPartialCache.isolatedJava21Loader(
                0, key -> sources.get(key.sourceRef())),
            (key, resident) -> 10L,
            MIndexASTPartialCache.PressurePolicy.defaults())) {
      for (int index = 0; index < 6; index++) {
        String path = "P" + index + ".java";
        cache.get(MIndexASTPartialCache.Key.java21Source(path, sources.get(path)));
      }

      cache.rebalance(new MIndexASTPartialCache.HeapUsage(85L, 100L));
      assertEquals(4, cache.activeEntryLimit());
      assertEquals(40L, cache.activeWeightLimit());
      assertEquals(4, cache.snapshot().lru().entries());

      for (int index = 6; index < 8; index++) {
        String path = "P" + index + ".java";
        cache.get(MIndexASTPartialCache.Key.java21Source(path, sources.get(path)));
        assertTrue(cache.snapshot().lru().entries() <= 4);
        assertTrue(cache.snapshot().lru().weight() <= 40L);
      }

      cache.rebalance(new MIndexASTPartialCache.HeapUsage(40L, 100L));
      assertEquals(8, cache.activeEntryLimit());
      assertEquals(80L, cache.activeWeightLimit());

      String reloaded = "P0.java";
      cache.get(MIndexASTPartialCache.Key.java21Source(reloaded, sources.get(reloaded)));
      assertTrue(cache.snapshot().lru().entries() > 4);
    }
  }

  @Test
  void concurrentMissesForSameSourcePerformOneParse() throws Exception {
    String source = SOURCE_A;
    MIndexASTPartialCache.Key key = MIndexASTPartialCache.Key.java21Source("A.java", source);
    AtomicInteger loads = new AtomicInteger();
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);

    try (MIndexASTPartialCache cache =
            new MIndexASTPartialCache(
                8,
                10_000_000L,
                MIndexASTPartialCache.isolatedJava21Loader(
                    0,
                    ignored -> {
                      loads.incrementAndGet();
                      entered.countDown();
                      release.await();
                      return source;
                    }));
        var executor = Executors.newFixedThreadPool(6)) {
      List<Future<MIndexASTPartialCache.Resident>> futures =
          java.util.stream.IntStream.range(0, 6)
              .mapToObj(index -> executor.submit(() -> cache.get(key)))
              .toList();

      entered.await();
      release.countDown();
      for (Future<MIndexASTPartialCache.Resident> future : futures) {
        assertEquals(key.sourceUtf16Sha256(), future.get().document().sourceUtf16Sha256Hex());
      }
      assertEquals(1, loads.get());
      assertEquals(1L, cache.snapshot().loads());
    }
  }

  @Test
  void oversizeResidentIsServedButNotRetained() throws Exception {
    AtomicInteger loads = new AtomicInteger();
    MIndexASTPartialCache.Key key = MIndexASTPartialCache.Key.java21Source("A.java", SOURCE_A);

    try (MIndexASTPartialCache cache =
        new MIndexASTPartialCache(
            4,
            10L,
            MIndexASTPartialCache.isolatedJava21Loader(
                0,
                ignored -> {
                  loads.incrementAndGet();
                  return SOURCE_A;
                }),
            (ignoredKey, ignoredResident) -> 11L,
            MIndexASTPartialCache.PressurePolicy.defaults())) {
      assertTrue(cache.get(key).document().occurrenceCount() > 0);
      assertFalse(cache.contains(key));
      assertEquals(1L, cache.snapshot().uncachedOversizeLoads());

      cache.get(key);
      assertEquals(2, loads.get());
    }
  }

  @Test
  void freshLoadRejectsChangedSourceAndPreservesPreviousResident() throws Exception {
    AtomicReference<String> liveSource = new AtomicReference<>(SOURCE_A);
    AtomicInteger loads = new AtomicInteger();
    var key = MIndexASTPartialCache.Key.java21Source("A.java", SOURCE_A);
    try (MIndexASTPartialCache cache =
        new MIndexASTPartialCache(
            2,
            1_000_000L,
            MIndexASTPartialCache.isolatedJava21Loader(
                0,
                ignored -> {
                  loads.incrementAndGet();
                  return liveSource.get();
                }))) {
      MIndexASTPartialCache.Resident resident = cache.get(key);
      liveSource.set(SOURCE_B);
      MIndexASTPartialCache.LoadException changed =
          org.junit.jupiter.api.Assertions.assertThrows(
              MIndexASTPartialCache.LoadException.class, () -> cache.getFresh(key));
      assertEquals(key, changed.key());
      assertEquals(1L, cache.snapshot().loadFailures());
      assertSame(resident, cache.getIfPresent(key).orElseThrow());

      liveSource.set(SOURCE_A);
      MIndexASTPartialCache.Resident freshlyLoaded = cache.getFresh(key);
      assertNotSame(resident.document(), freshlyLoaded.document());
      assertSame(resident, cache.get(key));
      assertEquals(3, loads.get());
      assertEquals(2L, cache.snapshot().loads());
    }
  }

  @Test
  void freshLoadDoesNotWaitForOrJoinInFlightCacheLoad() throws Exception {
    AtomicInteger loads = new AtomicInteger();
    CountDownLatch ordinaryStarted = new CountDownLatch(1);
    CountDownLatch releaseOrdinary = new CountDownLatch(1);
    var key = MIndexASTPartialCache.Key.java21Source("A.java", SOURCE_A);
    var executor = Executors.newFixedThreadPool(2);
    try (MIndexASTPartialCache cache =
        new MIndexASTPartialCache(
            2,
            1_000_000L,
            MIndexASTPartialCache.isolatedJava21Loader(
                0,
                ignored -> {
                  if (loads.incrementAndGet() == 1) {
                    ordinaryStarted.countDown();
                    releaseOrdinary.await();
                  }
                  return SOURCE_A;
                }))) {
      Future<MIndexASTPartialCache.Resident> ordinary =
          executor.submit(() -> cache.get(key));
      assertTrue(ordinaryStarted.await(10, TimeUnit.SECONDS));
      Future<MIndexASTPartialCache.Resident> fresh =
          executor.submit(() -> cache.getFresh(key));
      assertEquals(key.sourceUtf16Sha256(),
          fresh.get(10, TimeUnit.SECONDS).document().sourceUtf16Sha256Hex());
      assertFalse(cache.contains(key));
      releaseOrdinary.countDown();
      assertEquals(key.sourceUtf16Sha256(),
          ordinary.get(10, TimeUnit.SECONDS).document().sourceUtf16Sha256Hex());
      assertEquals(2, loads.get());
      assertEquals(2L, cache.snapshot().loads());
    } finally {
      releaseOrdinary.countDown();
      executor.shutdownNow();
    }
  }
}
