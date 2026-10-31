// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.synexia.algorithms.ds.WeightedLRUCache;
import com.synexia.algorithms.ds.WeightedSegmentedLRUCache;
import com.synexia.algorithms.ds.WeightedWindowTinyLfuCache;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Internal resident-policy adapter for {@link MIndexASTPartialCache}.
 *
 * <p>The public partial-AST cache contract historically exposes {@link WeightedLRUCache} evidence.
 * This adapter keeps that evidence surface stable while allowing an explicitly opted-in
 * Window-TinyLFU residency policy. Default construction remains the original weighted LRU.</p>
 */
final class MIndexASTResidentCache {

  enum Policy {
    LRU,
    WINDOW_TINY_LFU
  }

  record PutOutcome(
      boolean admitted,
      boolean replaced,
      boolean frequencyRejected,
      boolean oversizeRejected,
      long admittedWeight,
      WeightedLRUCache.TrimResult trim) {

    PutOutcome {
      if (admittedWeight < 0L) {
        throw new IllegalArgumentException("admittedWeight must be >= 0");
      }
      trim = Objects.requireNonNull(trim, "trim");
      if (admitted && (frequencyRejected || oversizeRejected)) {
        throw new IllegalArgumentException("admitted resident cannot be rejected");
      }
    }
  }

  record AdmissionSnapshot(
      Policy policy,
      long frequencyAdmissions,
      long frequencyRejections,
      long admissionComparisons,
      long sketchDecays,
      long sketchBytes,
      double windowFraction) {

    AdmissionSnapshot {
      policy = Objects.requireNonNull(policy, "policy");
      if (frequencyAdmissions < 0L
          || frequencyRejections < 0L
          || admissionComparisons < 0L
          || sketchDecays < 0L
          || sketchBytes < 0L
          || !Double.isFinite(windowFraction)
          || windowFraction < 0.0d
          || windowFraction >= 1.0d) {
        throw new IllegalArgumentException("invalid partial-AST admission snapshot");
      }
    }
  }

  private interface Backend {
    Optional<MIndexASTPartialCache.Resident> get(MIndexASTPartialCache.Key key);

    PutOutcome put(MIndexASTPartialCache.Key key, MIndexASTPartialCache.Resident value);

    Optional<MIndexASTPartialCache.Resident> remove(MIndexASTPartialCache.Key key);

    WeightedLRUCache.RemoveResult removeIf(
        Predicate<? super MIndexASTPartialCache.Key> predicate);

    WeightedLRUCache.RemoveResult clear();

    WeightedLRUCache.TrimResult trimTo(int entryLimit, long weightLimit);

    boolean containsKey(MIndexASTPartialCache.Key key);

    List<MIndexASTPartialCache.Key> keysInLruOrder();

    int size();

    long weight();

    int maxEntries();

    long maxWeight();

    WeightedLRUCache.Snapshot snapshot();

    AdmissionSnapshot admissionSnapshot();
  }

  private final Backend backend;

  private MIndexASTResidentCache(Backend backend) {
    this.backend = Objects.requireNonNull(backend, "backend");
  }

  static MIndexASTResidentCache create(
      Policy policy,
      int maxEntries,
      long maxWeight,
      MIndexASTPartialCache.ResidentWeigher weigher) {
    Objects.requireNonNull(policy, "policy");
    Objects.requireNonNull(weigher, "weigher");
    return switch (policy) {
      case LRU ->
          new MIndexASTResidentCache(
              new LruBackend(maxEntries, maxWeight, weigher));
      case WINDOW_TINY_LFU ->
          new MIndexASTResidentCache(
              new WindowTinyLfuBackend(maxEntries, maxWeight, weigher));
    };
  }

  Optional<MIndexASTPartialCache.Resident> get(MIndexASTPartialCache.Key key) {
    return backend.get(key);
  }

  PutOutcome put(
      MIndexASTPartialCache.Key key,
      MIndexASTPartialCache.Resident value) {
    return backend.put(key, value);
  }

  Optional<MIndexASTPartialCache.Resident> remove(MIndexASTPartialCache.Key key) {
    return backend.remove(key);
  }

  WeightedLRUCache.RemoveResult removeIf(
      Predicate<? super MIndexASTPartialCache.Key> predicate) {
    return backend.removeIf(predicate);
  }

  WeightedLRUCache.RemoveResult clear() {
    return backend.clear();
  }

  WeightedLRUCache.TrimResult trimTo(int entryLimit, long weightLimit) {
    return backend.trimTo(entryLimit, weightLimit);
  }

  boolean containsKey(MIndexASTPartialCache.Key key) {
    return backend.containsKey(key);
  }

  List<MIndexASTPartialCache.Key> keysInLruOrder() {
    return backend.keysInLruOrder();
  }

  int size() {
    return backend.size();
  }

  long weight() {
    return backend.weight();
  }

  int maxEntries() {
    return backend.maxEntries();
  }

  long maxWeight() {
    return backend.maxWeight();
  }

  WeightedLRUCache.Snapshot snapshot() {
    return backend.snapshot();
  }

  AdmissionSnapshot admissionSnapshot() {
    return backend.admissionSnapshot();
  }

  private static final class LruBackend implements Backend {
    private final WeightedLRUCache<MIndexASTPartialCache.Key, MIndexASTPartialCache.Resident>
        delegate;

    private LruBackend(
        int maxEntries,
        long maxWeight,
        MIndexASTPartialCache.ResidentWeigher weigher) {
      delegate =
          new WeightedLRUCache<>(
              maxEntries,
              maxWeight,
              (key, resident) -> weigher.weightOf(key, resident));
    }

    @Override
    public Optional<MIndexASTPartialCache.Resident> get(MIndexASTPartialCache.Key key) {
      return delegate.get(key);
    }

    @Override
    public PutOutcome put(
        MIndexASTPartialCache.Key key,
        MIndexASTPartialCache.Resident value) {
      WeightedLRUCache.PutResult result = delegate.put(key, value);
      return new PutOutcome(
          result.admitted(),
          result.replaced(),
          false,
          !result.admitted(),
          result.admittedWeight(),
          result.trim());
    }

    @Override
    public Optional<MIndexASTPartialCache.Resident> remove(MIndexASTPartialCache.Key key) {
      return delegate.remove(key);
    }

    @Override
    public WeightedLRUCache.RemoveResult removeIf(
        Predicate<? super MIndexASTPartialCache.Key> predicate) {
      return delegate.removeIf(predicate);
    }

    @Override
    public WeightedLRUCache.RemoveResult clear() {
      return delegate.clear();
    }

    @Override
    public WeightedLRUCache.TrimResult trimTo(int entryLimit, long weightLimit) {
      return delegate.trimTo(entryLimit, weightLimit);
    }

    @Override
    public boolean containsKey(MIndexASTPartialCache.Key key) {
      return delegate.containsKey(key);
    }

    @Override
    public List<MIndexASTPartialCache.Key> keysInLruOrder() {
      return delegate.keysInLruOrder();
    }

    @Override
    public int size() {
      return delegate.size();
    }

    @Override
    public long weight() {
      return delegate.weight();
    }

    @Override
    public int maxEntries() {
      return delegate.maxEntries();
    }

    @Override
    public long maxWeight() {
      return delegate.maxWeight();
    }

    @Override
    public WeightedLRUCache.Snapshot snapshot() {
      return delegate.snapshot();
    }

    @Override
    public AdmissionSnapshot admissionSnapshot() {
      return new AdmissionSnapshot(Policy.LRU, 0L, 0L, 0L, 0L, 0L, 0.0d);
    }
  }

  private static final class WindowTinyLfuBackend implements Backend {
    private static final double PROTECTED_FRACTION = 0.80d;

    private final int maxEntries;
    private final long maxWeight;
    private final MIndexASTPartialCache.ResidentWeigher weigher;
    private final WeightedWindowTinyLfuCache<
            MIndexASTPartialCache.Key, MIndexASTPartialCache.Resident>
        delegate;

    private long logicalHits;
    private long logicalMisses;
    private long logicalPuts;
    private long logicalReplacements;
    private long logicalEvictions;
    private long logicalOversizeRejections;

    private WindowTinyLfuBackend(
        int maxEntries,
        long maxWeight,
        MIndexASTPartialCache.ResidentWeigher weigher) {
      this.maxEntries = maxEntries;
      this.maxWeight = maxWeight;
      this.weigher = Objects.requireNonNull(weigher, "weigher");
      this.delegate =
          new WeightedWindowTinyLfuCache<>(
              maxEntries,
              maxWeight,
              WeightedWindowTinyLfuCache.DEFAULT_WINDOW_FRACTION,
              PROTECTED_FRACTION,
              (key, resident) -> this.weigher.weightOf(key, resident),
              MIndexASTResidentCache::stableKeyHash);
    }

    @Override
    public synchronized Optional<MIndexASTPartialCache.Resident> get(
        MIndexASTPartialCache.Key key) {
      Optional<MIndexASTPartialCache.Resident> result =
          delegate.get(Objects.requireNonNull(key, "key"));
      if (result.isPresent()) logicalHits++;
      else logicalMisses++;
      return result;
    }

    @Override
    public synchronized PutOutcome put(
        MIndexASTPartialCache.Key key,
        MIndexASTPartialCache.Resident value) {
      Objects.requireNonNull(key, "key");
      Objects.requireNonNull(value, "value");
      long candidateWeight = requirePositiveWeight(weigher.weightOf(key, value));
      WeightedWindowTinyLfuCache.PutResult result = delegate.put(key, value);
      if (result.admitted()) {
        logicalPuts++;
        if (result.replaced()) logicalReplacements++;
      }
      logicalEvictions = Math.addExact(logicalEvictions, result.trim().evictedEntries());
      boolean oversize =
          !result.admitted()
              && !result.frequencyRejected()
              && candidateWeight > maxWeight;
      if (oversize) logicalOversizeRejections++;
      return new PutOutcome(
          result.admitted(),
          result.replaced(),
          result.frequencyRejected(),
          oversize,
          result.admittedWeight(),
          toLruTrim(result.trim()));
    }

    @Override
    public synchronized Optional<MIndexASTPartialCache.Resident> remove(
        MIndexASTPartialCache.Key key) {
      return delegate.remove(key);
    }

    @Override
    public synchronized WeightedLRUCache.RemoveResult removeIf(
        Predicate<? super MIndexASTPartialCache.Key> predicate) {
      WeightedSegmentedLRUCache.RemoveResult result =
          delegate.removeIf(predicate);
      return new WeightedLRUCache.RemoveResult(
          result.removedEntries(), result.removedWeight());
    }

    @Override
    public synchronized WeightedLRUCache.RemoveResult clear() {
      WeightedSegmentedLRUCache.RemoveResult result = delegate.clear();
      return new WeightedLRUCache.RemoveResult(
          result.removedEntries(), result.removedWeight());
    }

    @Override
    public synchronized WeightedLRUCache.TrimResult trimTo(
        int entryLimit,
        long weightLimit) {
      WeightedSegmentedLRUCache.TrimResult result =
          delegate.trimTo(entryLimit, weightLimit);
      logicalEvictions = Math.addExact(logicalEvictions, result.evictedEntries());
      return toLruTrim(result);
    }

    @Override
    public synchronized boolean containsKey(MIndexASTPartialCache.Key key) {
      return delegate.containsKey(key);
    }

    @Override
    public synchronized List<MIndexASTPartialCache.Key> keysInLruOrder() {
      return delegate.keysInLruOrder();
    }

    @Override
    public synchronized int size() {
      return delegate.size();
    }

    @Override
    public synchronized long weight() {
      return delegate.weight();
    }

    @Override
    public int maxEntries() {
      return maxEntries;
    }

    @Override
    public long maxWeight() {
      return maxWeight;
    }

    @Override
    public synchronized WeightedLRUCache.Snapshot snapshot() {
      return new WeightedLRUCache.Snapshot(
          delegate.size(),
          delegate.weight(),
          maxEntries,
          maxWeight,
          logicalHits,
          logicalMisses,
          logicalPuts,
          logicalReplacements,
          logicalEvictions,
          logicalOversizeRejections);
    }

    @Override
    public synchronized AdmissionSnapshot admissionSnapshot() {
      WeightedWindowTinyLfuCache.Snapshot snapshot = delegate.snapshot();
      return new AdmissionSnapshot(
          Policy.WINDOW_TINY_LFU,
          snapshot.main().frequencyAdmissions(),
          snapshot.main().frequencyRejections(),
          snapshot.main().admissionComparisons(),
          snapshot.main().sketchDecays(),
          snapshot.main().sketchBytes(),
          delegate.windowFraction());
    }
  }

  private static WeightedLRUCache.TrimResult toLruTrim(
      WeightedSegmentedLRUCache.TrimResult trim) {
    return new WeightedLRUCache.TrimResult(
        trim.evictedEntries(), trim.evictedWeight());
  }

  private static long stableKeyHash(MIndexASTPartialCache.Key key) {
    long hash = 0xcbf29ce484222325L;
    String sourceHash = key.sourceUtf16Sha256();
    for (int index = 0; index < sourceHash.length(); index++) {
      hash ^= sourceHash.charAt(index);
      hash *= 0x100000001b3L;
    }
    hash ^= key.astSpecFingerprint();
    hash *= 0x100000001b3L;
    return mix64(hash);
  }

  private static long mix64(long value) {
    long z = value;
    z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
    z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
    return z ^ (z >>> 31);
  }

  private static long requirePositiveWeight(long weight) {
    if (weight <= 0L) throw new IllegalArgumentException("entry weight must be > 0");
    return weight;
  }
}
