// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.synexia.algorithms.ds.WeightedLRUCache;
import com.synexia.indexstring.ExactStringIndexResolver;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Weight-bounded LRU for partial AST documents and opt-in derived accelerators.
 *
 * <p>The cache owns only strong references to hot residents. Eviction removes cache ownership; a
 * caller already holding a document or accelerator remains safe because that caller keeps its own
 * strong reference. For reclaimable Java parsing, use {@link #isolatedJava21Loader(int,
 * SourceLoader)} so each resident owns an isolated AST pool and exact string resolver. Evicting
 * that resident can then release the complete per-file AST segment after active callers finish.</p>
 *
 * <p>The implementation deliberately reuses Synexia's weighted O(1)-amortized hash-table plus
 * intrusive doubly-linked-list LRU. Entry count and retained-weight limits are independent.
 * Memory-pressure trimming temporarily tightens both limits without changing the configured hard
 * limits.</p>
 */
public final class MIndexASTPartialCache implements AutoCloseable {

  /** Incrementally materialized resident detail. */
  public enum Detail {
    DOCUMENT,
    NAVIGATION,
    SOURCE_INDEX,
    CONSTANT_TIME_LCA,
    FULL
  }

  /** Resident-policy choice. Existing constructors retain strict weighted-LRU behavior. */
  public enum ResidencyPolicy {
    LRU,
    WINDOW_TINY_LFU
  }

  /** Heap-pressure classification used to tighten the LRU working set. */
  public enum PressureLevel {
    NORMAL,
    ELEVATED,
    HIGH,
    CRITICAL,
    EMERGENCY
  }

  /** Stable cache key. Source hash is over logical UTF-16 code units, matching MIndexASTParser. */
  public record Key(
      String sourceRef,
      String sourceUtf16Sha256,
      long astSpecFingerprint) {

    public Key {
      sourceRef = requireText(sourceRef, "sourceRef");
      sourceUtf16Sha256 = requireSha256(sourceUtf16Sha256, "sourceUtf16Sha256");
    }

    /** Use when the caller already has the logical UTF-16 source hash. */
    public static Key java21(String sourceRef, String sourceUtf16Sha256) {
      return new Key(
          sourceRef,
          sourceUtf16Sha256,
          MIndexLanguageSpecs.java21().astSpec().fingerprint());
    }

    /** Hash the supplied source text; use this explicit name when the source is a String. */
    public static Key java21Source(String sourceRef, CharSequence source) {
      return java21(sourceRef, MIndexASTPartialCache.sourceUtf16Sha256(source));
    }

    /** Hash a CharSequence source; String arguments select the hash overload above. */
    public static Key java21(String sourceRef, CharSequence source) {
      return java21Source(sourceRef, source);
    }
  }

  /** Caller-owned source retrieval used by the isolated parser loader. */
  @FunctionalInterface
  public interface SourceLoader {
    CharSequence load(Key key) throws Exception;
  }

  /** General resident loader. */
  @FunctionalInterface
  public interface Loader {
    MIndexASTDocument load(Key key) throws Exception;
  }

  /** Optional loader that can hydrate a requested projection from rebuildable secondary storage. */
  public interface DetailLoader extends Loader {
    Resident load(Key key, Detail detail) throws Exception;

    /** Allows existing callers to continue requesting only the base document. */
    @Override
    default MIndexASTDocument load(Key key) throws Exception {
      return load(key, Detail.DOCUMENT).document();
    }

    /** Allows a deferred upgrade to consult a cold projection before rebuilding it in heap. */
    default Resident upgrade(Key key, Resident resident, Detail detail) throws Exception {
      return resident.upgrade(detail);
    }
  }

  /** Caller-replaceable retained-weight estimate. */
  @FunctionalInterface
  public interface ResidentWeigher {
    long weightOf(Key key, Resident resident);
  }

  /** Immutable heap sample. */
  public record HeapUsage(long usedBytes, long maxBytes) {
    public HeapUsage {
      if (usedBytes < 0L || maxBytes <= 0L || usedBytes > maxBytes) {
        throw new IllegalArgumentException("invalid heap usage");
      }
    }

    public double ratio() {
      return (double) usedBytes / (double) maxBytes;
    }

    public static HeapUsage current() {
      MemoryUsage usage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
      long max = usage.getMax();
      if (max <= 0L) max = usage.getCommitted();
      if (max <= 0L) max = Math.max(1L, usage.getUsed());
      return new HeapUsage(Math.min(usage.getUsed(), max), max);
    }
  }

  /**
   * Dynamic pressure policy.
   *
   * <p>Ratios are heap-used/max thresholds. Fractions are temporary fractions of configured cache
   * entry and weight limits retained at each pressure level.</p>
   */
  public record PressurePolicy(
      double elevatedAt,
      double highAt,
      double criticalAt,
      double emergencyAt,
      double elevatedFraction,
      double highFraction,
      double criticalFraction) {

    public PressurePolicy {
      if (!(elevatedAt > 0.0
          && elevatedAt < highAt
          && highAt < criticalAt
          && criticalAt < emergencyAt
          && emergencyAt <= 1.0)) {
        throw new IllegalArgumentException("pressure thresholds must be increasing in (0,1]");
      }
      if (!(elevatedFraction > 0.0
          && elevatedFraction <= 1.0
          && highFraction > 0.0
          && highFraction <= elevatedFraction
          && criticalFraction > 0.0
          && criticalFraction <= highFraction)) {
        throw new IllegalArgumentException("pressure fractions must be descending in (0,1]");
      }
    }

    public static PressurePolicy defaults() {
      return new PressurePolicy(0.70, 0.82, 0.90, 0.96, 0.75, 0.50, 0.25);
    }

    public PressureLevel level(HeapUsage usage) {
      return level(Objects.requireNonNull(usage, "usage").ratio());
    }

    /** Classify any normalized memory-pressure ratio using the same cache policy thresholds. */
    public PressureLevel level(double ratio) {
      if (!Double.isFinite(ratio) || ratio < 0.0d || ratio > 1.0d) {
        throw new IllegalArgumentException("pressure ratio must be in [0,1]");
      }
      if (ratio >= emergencyAt) return PressureLevel.EMERGENCY;
      if (ratio >= criticalAt) return PressureLevel.CRITICAL;
      if (ratio >= highAt) return PressureLevel.HIGH;
      if (ratio >= elevatedAt) return PressureLevel.ELEVATED;
      return PressureLevel.NORMAL;
    }

    public double retainedFraction(PressureLevel level) {
      return switch (Objects.requireNonNull(level, "level")) {
        case NORMAL -> 1.0;
        case ELEVATED -> elevatedFraction;
        case HIGH -> highFraction;
        case CRITICAL -> criticalFraction;
        case EMERGENCY -> 0.0;
      };
    }
  }

  /** Immutable resident value. Upgrades return a new resident rather than mutating cached weight. */
  public static final class Resident {
    private final MIndexASTDocument document;
    private final MIndexASTPrecompute navigation;
    private final MIndexASTSourceIndex sourceIndex;
    private final MIndexASTLcaSparseTable constantTimeLca;
    private final MIndexASTPoolPrecompute poolPrecompute;

    private Resident(
        MIndexASTDocument document,
        MIndexASTPrecompute navigation,
        MIndexASTSourceIndex sourceIndex,
        MIndexASTLcaSparseTable constantTimeLca,
        MIndexASTPoolPrecompute poolPrecompute) {
      this.document = Objects.requireNonNull(document, "document");
      this.navigation = navigation;
      this.sourceIndex = sourceIndex;
      this.constantTimeLca = constantTimeLca;
      this.poolPrecompute = poolPrecompute;
    }

    public static Resident document(MIndexASTDocument document) {
      return new Resident(document, null, null, null, null);
    }

    static Resident withSourceIndex(
        MIndexASTDocument document,
        MIndexASTSourceIndex sourceIndex) {
      return new Resident(
          Objects.requireNonNull(document, "document"),
          null,
          Objects.requireNonNull(sourceIndex, "sourceIndex"),
          null,
          null);
    }

    static Resident withNavigation(
        MIndexASTDocument document,
        MIndexASTPrecompute navigation) {
      return new Resident(
          Objects.requireNonNull(document, "document"),
          Objects.requireNonNull(navigation, "navigation"),
          null,
          null,
          null);
    }

    static Resident withConstantTimeLca(
        MIndexASTDocument document,
        MIndexASTLcaSparseTable constantTimeLca) {
      return new Resident(
          Objects.requireNonNull(document, "document"),
          null,
          null,
          Objects.requireNonNull(constantTimeLca, "constantTimeLca"),
          null);
    }

    static Resident withPoolPrecompute(
        MIndexASTDocument document,
        MIndexASTPoolPrecompute poolPrecompute) {
      return new Resident(
          Objects.requireNonNull(document, "document"),
          null,
          null,
          null,
          Objects.requireNonNull(poolPrecompute, "poolPrecompute"));
    }

    Resident withSourceIndex(MIndexASTSourceIndex replacement) {
      return new Resident(
          document,
          navigation,
          Objects.requireNonNull(replacement, "sourceIndex"),
          constantTimeLca,
          poolPrecompute);
    }

    Resident withNavigation(MIndexASTPrecompute replacement) {
      return new Resident(
          document,
          Objects.requireNonNull(replacement, "navigation"),
          sourceIndex,
          constantTimeLca,
          poolPrecompute);
    }

    Resident withConstantTimeLca(MIndexASTLcaSparseTable replacement) {
      return new Resident(
          document,
          navigation,
          sourceIndex,
          Objects.requireNonNull(replacement, "constantTimeLca"),
          poolPrecompute);
    }

    Resident withPoolPrecompute(MIndexASTPoolPrecompute replacement) {
      return new Resident(
          document,
          navigation,
          sourceIndex,
          constantTimeLca,
          Objects.requireNonNull(replacement, "poolPrecompute"));
    }

    public MIndexASTDocument document() {
      return document;
    }

    public Optional<MIndexASTPrecompute> navigation() {
      return Optional.ofNullable(navigation);
    }

    public Optional<MIndexASTSourceIndex> sourceIndex() {
      return Optional.ofNullable(sourceIndex);
    }

    public Optional<MIndexASTLcaSparseTable> constantTimeLca() {
      return Optional.ofNullable(constantTimeLca);
    }

    public Optional<MIndexASTPoolPrecompute> poolPrecompute() {
      return Optional.ofNullable(poolPrecompute);
    }

    public boolean satisfies(Detail detail) {
      return switch (Objects.requireNonNull(detail, "detail")) {
        case DOCUMENT -> true;
        case NAVIGATION -> navigation != null;
        case SOURCE_INDEX -> sourceIndex != null;
        case CONSTANT_TIME_LCA -> constantTimeLca != null;
        case FULL ->
            navigation != null
                && sourceIndex != null
                && constantTimeLca != null
                && poolPrecompute != null;
      };
    }

    Resident upgrade(Detail detail) {
      if (satisfies(detail)) return this;
      return switch (detail) {
        case DOCUMENT -> this;
        case NAVIGATION ->
            new Resident(
                document,
                document.precompute(),
                sourceIndex,
                constantTimeLca,
                poolPrecompute);
        case SOURCE_INDEX ->
            new Resident(
                document,
                navigation,
                document.sourceIndex(),
                constantTimeLca,
                poolPrecompute);
        case CONSTANT_TIME_LCA ->
            new Resident(
                document,
                navigation,
                sourceIndex,
                MIndexASTLcaSparseTable.build(document),
                poolPrecompute);
        case FULL ->
            new Resident(
                document,
                navigation == null ? document.precompute() : navigation,
                sourceIndex == null ? document.sourceIndex() : sourceIndex,
                constantTimeLca == null
                    ? MIndexASTLcaSparseTable.build(document)
                    : constantTimeLca,
                poolPrecompute == null
                    ? document.root().pool().precompute()
                    : poolPrecompute);
      };
    }

    /** Default retained-weight estimate. It is intentionally an estimate, not an exact heap claim. */
    public long estimatedRetainedBytes() {
      MIndexASTPool pool = document.root().pool();
      long occurrences = document.occurrenceCount();
      long poolAtoms = pool.size();
      long childHandles = pool.retainedChildHandleCount();
      long labels = pool.labels().size();
      long labelTokens = pool.labels().retainedTokenCount();

      long bytes = 128L;
      bytes = Math.addExact(bytes, Math.multiplyExact(occurrences, 36L));
      bytes = Math.addExact(bytes, Math.multiplyExact(poolAtoms, 80L));
      bytes = Math.addExact(bytes, Math.multiplyExact(childHandles, Integer.BYTES));
      bytes = Math.addExact(bytes, Math.multiplyExact(labels, 96L));
      bytes = Math.addExact(bytes, Math.multiplyExact(labelTokens, Long.BYTES));
      bytes = Math.addExact(bytes, 32L);

      if (navigation != null) {
        bytes = Math.addExact(bytes, navigation.primitivePayloadBytes());
      }
      if (sourceIndex != null) {
        bytes = Math.addExact(bytes, sourceIndex.primitivePayloadBytes());
      }
      if (constantTimeLca != null) {
        bytes = Math.addExact(bytes, constantTimeLca.primitivePayloadBytes());
      }
      if (poolPrecompute != null) {
        bytes = Math.addExact(bytes, poolPrecompute.primitivePayloadBytes());
      }
      return Math.max(1L, bytes);
    }
  }

  /** Result of one memory-pressure rebalance. */
  public record PressureResult(
      HeapUsage heap,
      PressureLevel level,
      int targetEntries,
      long targetWeight,
      WeightedLRUCache.TrimResult trim,
      WeightedLRUCache.Snapshot before,
      WeightedLRUCache.Snapshot after) {

    public PressureResult {
      heap = Objects.requireNonNull(heap, "heap");
      level = Objects.requireNonNull(level, "level");
      trim = Objects.requireNonNull(trim, "trim");
      before = Objects.requireNonNull(before, "before");
      after = Objects.requireNonNull(after, "after");
    }
  }

  /** Frequency-admission diagnostics; zeroed when strict LRU owns residency. */
  public record AdmissionSnapshot(
      ResidencyPolicy policy,
      long frequencyAdmissions,
      long frequencyRejections,
      long admissionComparisons,
      long sketchDecays,
      long sketchBytes,
      double windowFraction) {
    public AdmissionSnapshot {
      policy = Objects.requireNonNull(policy, "policy");
      if (frequencyAdmissions < 0L
          || frequencyRejections < 0L
          || admissionComparisons < 0L
          || sketchDecays < 0L
          || sketchBytes < 0L
          || !Double.isFinite(windowFraction)
          || windowFraction < 0.0d
          || windowFraction >= 1.0d) {
        throw new IllegalArgumentException("invalid admission snapshot");
      }
    }
  }

  /** Combined cache/loader statistics. */
  public record Snapshot(
      WeightedLRUCache.Snapshot lru,
      long loads,
      long loadFailures,
      long uncachedOversizeLoads,
      long upgrades,
      long upgradeRejections) {

    public Snapshot {
      lru = Objects.requireNonNull(lru, "lru");
      if (loads < 0L
          || loadFailures < 0L
          || uncachedOversizeLoads < 0L
          || upgrades < 0L
          || upgradeRejections < 0L) {
        throw new IllegalArgumentException("negative partial-AST cache statistic");
      }
    }
  }

  /** Checked wrapper preserving the cache key that failed to load/upgrade. */
  @SuppressWarnings("serial")
  public static final class LoadException extends Exception {
    private static final long serialVersionUID = 1L;
    private final Key key;

    private LoadException(Key key, Throwable cause) {
      super("partial AST load failed for " + key.sourceRef(), cause);
      this.key = key;
    }

    public Key key() {
      return key;
    }
  }

  private final MIndexASTResidentCache residents;
  private final ResidencyPolicy residencyPolicy;
  private final Loader loader;
  private final ResidentWeigher weigher;
  private final PressurePolicy pressurePolicy;
  private final ConcurrentHashMap<Key, CompletableFuture<Resident>> inFlightLoads =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UpgradeKey, CompletableFuture<Resident>> inFlightUpgrades =
      new ConcurrentHashMap<>();
  private final AtomicBoolean closed = new AtomicBoolean();
  private final AtomicLong loads = new AtomicLong();
  private final AtomicLong loadFailures = new AtomicLong();
  private final AtomicLong uncachedOversizeLoads = new AtomicLong();
  private final AtomicLong upgrades = new AtomicLong();
  private final AtomicLong upgradeRejections = new AtomicLong();
  private final AtomicLong frequencyAdmissionRejections = new AtomicLong();
  private volatile ActiveBudget activeBudget;

  public MIndexASTPartialCache(
      int maxEntries,
      long maxRetainedBytes,
      Loader loader) {
    this(
        maxEntries,
        maxRetainedBytes,
        loader,
        (key, resident) -> resident.estimatedRetainedBytes(),
        PressurePolicy.defaults());
  }

  public MIndexASTPartialCache(
      int maxEntries,
      long maxRetainedBytes,
      Loader loader,
      ResidentWeigher weigher,
      PressurePolicy pressurePolicy) {
    this(
        maxEntries,
        maxRetainedBytes,
        loader,
        weigher,
        pressurePolicy,
        ResidencyPolicy.LRU);
  }

  /**
   * Explicit resident-policy constructor.
   *
   * <p>{@link ResidencyPolicy#LRU} is the compatibility default. Window-TinyLFU is opt-in and
   * combines a small temporal LRU window with aging frequency admission so one-pass scans do not
   * displace repeatedly reused partial ASTs.</p>
   */
  public MIndexASTPartialCache(
      int maxEntries,
      long maxRetainedBytes,
      Loader loader,
      ResidentWeigher weigher,
      PressurePolicy pressurePolicy,
      ResidencyPolicy residencyPolicy) {
    this.loader = Objects.requireNonNull(loader, "loader");
    this.weigher = Objects.requireNonNull(weigher, "weigher");
    this.pressurePolicy = Objects.requireNonNull(pressurePolicy, "pressurePolicy");
    this.residencyPolicy = Objects.requireNonNull(residencyPolicy, "residencyPolicy");
    this.residents =
        MIndexASTResidentCache.create(
            toResidentPolicy(residencyPolicy),
            maxEntries,
            maxRetainedBytes,
            (key, resident) -> requirePositiveWeight(this.weigher.weightOf(key, resident)));
    this.activeBudget = new ActiveBudget(maxEntries, maxRetainedBytes);
  }

  /** Opt-in frequency-aware cache without changing the legacy constructor contract. */
  public static MIndexASTPartialCache windowTinyLfu(
      int maxEntries,
      long maxRetainedBytes,
      Loader loader) {
    return new MIndexASTPartialCache(
        maxEntries,
        maxRetainedBytes,
        loader,
        (key, resident) -> resident.estimatedRetainedBytes(),
        PressurePolicy.defaults(),
        ResidencyPolicy.WINDOW_TINY_LFU);
  }

  /** Opt-in frequency-aware cache with caller-owned retained-weight and pressure policies. */
  public static MIndexASTPartialCache windowTinyLfu(
      int maxEntries,
      long maxRetainedBytes,
      Loader loader,
      ResidentWeigher weigher,
      PressurePolicy pressurePolicy) {
    return new MIndexASTPartialCache(
        maxEntries,
        maxRetainedBytes,
        loader,
        weigher,
        pressurePolicy,
        ResidencyPolicy.WINDOW_TINY_LFU);
  }

  /**
   * Reclaimable Java-21 loader.
   *
   * <p>Each loaded file receives a new ExactStringIndexResolver and MIndexASTPool. Cross-file atom
   * sharing is intentionally traded for the ability to reclaim an entire file segment on LRU
   * eviction.</p>
   */
  public static Loader isolatedJava21Loader(
      int languageId,
      SourceLoader sourceLoader) {
    if (languageId < 0) throw new IllegalArgumentException("negative languageId");
    Objects.requireNonNull(sourceLoader, "sourceLoader");
    return key -> {
      CharSequence source = Objects.requireNonNull(sourceLoader.load(key), "source");
      MIndexASTPool pool = new MIndexASTPool(new ExactStringIndexResolver(), languageId);
      return new MIndexASTParser(pool).parse(key.sourceRef(), source);
    };
  }

  /**
   * Opt-in single-source edit lifetime sharing immutable pool atoms across source revisions.
   * This is not a generic LRU Loader; growing pools require separate ownership/weight accounting.
   */
  public static MIndexASTEditSession retainedJava21Session(
      String sourceRef, int languageId, MIndexASTEditSession.Limits limits,
      SourceLoader sourceLoader) {
    return new MIndexASTEditSession(sourceRef, languageId, limits, sourceLoader);
  }

  public Resident get(Key key) throws LoadException {
    return get(key, Detail.DOCUMENT);
  }

  public Resident get(Key key, Detail detail) throws LoadException {
    ensureOpen();
    Key checkedKey = Objects.requireNonNull(key, "key");
    Detail checkedDetail = Objects.requireNonNull(detail, "detail");

    Resident resident =
        residents.get(checkedKey).orElseGet(() -> null);
    if (resident == null) {
      resident = loadDeduplicated(checkedKey, checkedDetail);
    }
    if (!resident.satisfies(checkedDetail)) {
      resident = upgradeDeduplicated(checkedKey, resident, checkedDetail);
    }
    return resident;
  }

  /**
   * Load the current source image independently of retained residents and in-flight cache loads.
   * A caller using this for file custody must hold its file lease before calling it. The supplied
   * {@link Loader} must retrieve the live source rather than return its own stale cached document.
   * This deliberately does not admit the result to the LRU: a previous resident with the same
   * declared key must never substitute for the freshly checked document.
   */
  public Resident getFresh(Key key) throws LoadException {
    ensureOpen();
    Key checkedKey = Objects.requireNonNull(key, "key");
    try {
      MIndexASTDocument document = Objects.requireNonNull(loader.load(checkedKey),
          "loader document");
      validateLoaded(checkedKey, document);
      ensureOpen();
      loads.incrementAndGet();
      return Resident.document(document);
    } catch (Exception failure) {
      loadFailures.incrementAndGet();
      throw failure instanceof LoadException known
          ? known : new LoadException(checkedKey, failure);
    }
  }

  public Optional<Resident> getIfPresent(Key key) {
    ensureOpen();
    return residents.get(Objects.requireNonNull(key, "key"));
  }

  public boolean contains(Key key) {
    ensureOpen();
    return residents.containsKey(Objects.requireNonNull(key, "key"));
  }

  public boolean invalidate(Key key) {
    ensureOpen();
    return residents.remove(Objects.requireNonNull(key, "key")).isPresent();
  }

  public WeightedLRUCache.RemoveResult invalidateSource(String sourceRef) {
    ensureOpen();
    String checked = requireText(sourceRef, "sourceRef");
    return residents.removeIf(key -> key.sourceRef().equals(checked));
  }

  public WeightedLRUCache.RemoveResult clear() {
    ensureOpen();
    return residents.clear();
  }

  public List<Key> keysInLruOrder() {
    ensureOpen();
    return residents.keysInLruOrder();
  }

  /** Tighten the cache working set according to the supplied heap sample. */
  public PressureResult rebalance(HeapUsage heap) {
    return rebalance(heap, 1.0);
  }

  /**
   * Apply a caller-computed working-set ratio under the existing heap-pressure ceiling.
   *
   * <p>The ratio changes the entry and retained-byte budgets together. Pressure may tighten it
   * further, and EMERGENCY always releases the whole heap working set. This remains the sole
   * residency owner; cold storage never decides which resident is MRU.</p>
   */
  public PressureResult rebalance(HeapUsage heap, double desiredRetainedFraction) {
    return rebalance(heap, desiredRetainedFraction, PressureLevel.NORMAL);
  }

  /**
   * Apply heap pressure plus a caller-supplied minimum pressure level, for example JNI
   * process/system memory evidence. External pressure may only tighten the working set.
   */
  public PressureResult rebalance(
      HeapUsage heap,
      double desiredRetainedFraction,
      PressureLevel minimumLevel) {
    ensureOpen();
    HeapUsage checked = Objects.requireNonNull(heap, "heap");
    PressureLevel checkedMinimum = Objects.requireNonNull(minimumLevel, "minimumLevel");
    if (!Double.isFinite(desiredRetainedFraction)
        || desiredRetainedFraction < 0.0
        || desiredRetainedFraction > 1.0) {
      throw new IllegalArgumentException("desiredRetainedFraction must be in [0,1]");
    }
    PressureLevel heapLevel = pressurePolicy.level(checked);
    PressureLevel level =
        heapLevel.ordinal() >= checkedMinimum.ordinal() ? heapLevel : checkedMinimum;
    double fraction =
        Math.min(
            pressurePolicy.retainedFraction(level),
            desiredRetainedFraction);

    WeightedLRUCache.Snapshot before = residents.snapshot();
    int targetEntries = scaledInt(before.maxEntries(), fraction);
    long targetWeight = scaledLong(before.maxWeight(), fraction);
    activeBudget = new ActiveBudget(targetEntries, targetWeight);
    WeightedLRUCache.TrimResult trim = residents.trimTo(targetEntries, targetWeight);
    WeightedLRUCache.Snapshot after = residents.snapshot();
    return new PressureResult(
        checked,
        level,
        targetEntries,
        targetWeight,
        trim,
        before,
        after);
  }

  public PressureResult rebalance() {
    return rebalance(HeapUsage.current());
  }

  public Snapshot snapshot() {
    return new Snapshot(
        residents.snapshot(),
        loads.get(),
        loadFailures.get(),
        uncachedOversizeLoads.get(),
        upgrades.get(),
        upgradeRejections.get());
  }

  public int maxEntries() {
    return residents.maxEntries();
  }

  public long maxRetainedBytes() {
    return residents.maxWeight();
  }

  public PressurePolicy pressurePolicy() {
    return pressurePolicy;
  }

  public ResidencyPolicy residencyPolicy() {
    return residencyPolicy;
  }

  /** Frequency-admission evidence. LRU returns a zeroed compatibility snapshot. */
  public AdmissionSnapshot admissionSnapshot() {
    MIndexASTResidentCache.AdmissionSnapshot snapshot = residents.admissionSnapshot();
    ResidencyPolicy policy =
        snapshot.policy() == MIndexASTResidentCache.Policy.LRU
            ? ResidencyPolicy.LRU
            : ResidencyPolicy.WINDOW_TINY_LFU;
    return new AdmissionSnapshot(
        policy,
        snapshot.frequencyAdmissions(),
        snapshot.frequencyRejections(),
        snapshot.admissionComparisons(),
        snapshot.sketchDecays(),
        snapshot.sketchBytes(),
        snapshot.windowFraction());
  }

  public long frequencyAdmissionRejections() {
    return frequencyAdmissionRejections.get();
  }

  /** Current temporary entry budget after the most recent pressure sample. */
  public int activeEntryLimit() {
    return activeBudget.entryLimit();
  }

  /** Current temporary retained-weight budget after the most recent pressure sample. */
  public long activeWeightLimit() {
    return activeBudget.weightLimit();
  }

  @Override
  public void close() {
    if (!closed.compareAndSet(false, true)) return;
    residents.clear();
    inFlightLoads.values().forEach(future -> future.cancel(true));
    inFlightLoads.clear();
    inFlightUpgrades.values().forEach(future -> future.cancel(true));
    inFlightUpgrades.clear();
  }

  private Resident loadDeduplicated(Key key, Detail detail) throws LoadException {
    Optional<Resident> present = residents.get(key);
    if (present.isPresent()) return present.orElseThrow();

    CompletableFuture<Resident> mine = new CompletableFuture<>();
    CompletableFuture<Resident> active = inFlightLoads.putIfAbsent(key, mine);
    if (active != null) return await(key, active);

    try {
      Resident afterRace = residents.get(key).orElse(null);
      if (afterRace != null) {
        mine.complete(afterRace);
        return afterRace;
      }

      Resident resident;
      if (loader instanceof DetailLoader detailLoader) {
        resident = Objects.requireNonNull(
            detailLoader.load(key, detail), "detail-aware loader resident");
        validateLoaded(key, resident.document());
      } else {
        MIndexASTDocument document = Objects.requireNonNull(loader.load(key), "loader document");
        validateLoaded(key, document);
        resident = Resident.document(document);
      }
      ensureOpen();
      MIndexASTResidentCache.PutOutcome admission = residents.put(key, resident);
      if (admission.admitted()) enforceActiveBudget();
      loads.incrementAndGet();
      if (admission.frequencyRejected()) {
        frequencyAdmissionRejections.incrementAndGet();
      } else if (admission.oversizeRejected()) {
        uncachedOversizeLoads.incrementAndGet();
      }
      mine.complete(resident);
      return resident;
    } catch (Error failure) {
      // Release joined callers before removing the flight, without swallowing the owner's Error.
      mine.completeExceptionally(failure);
      loadFailures.incrementAndGet();
      throw failure;
    } catch (Exception failure) {
      loadFailures.incrementAndGet();
      LoadException wrapped =
          failure instanceof LoadException known ? known : new LoadException(key, failure);
      mine.completeExceptionally(wrapped);
      throw wrapped;
    } finally {
      inFlightLoads.remove(key, mine);
    }
  }

  private Resident upgradeDeduplicated(
      Key key,
      Resident base,
      Detail detail)
      throws LoadException {
    UpgradeKey upgradeKey = new UpgradeKey(key, detail);
    CompletableFuture<Resident> mine = new CompletableFuture<>();
    CompletableFuture<Resident> active = inFlightUpgrades.putIfAbsent(upgradeKey, mine);
    if (active != null) return await(key, active);

    try {
      Resident latest = residents.get(key).orElse(base);
      if (latest.satisfies(detail)) {
        mine.complete(latest);
        return latest;
      }

      Resident upgraded = loader instanceof DetailLoader detailLoader
          ? Objects.requireNonNull(
              detailLoader.upgrade(key, latest, detail), "detail-aware loader upgrade")
          : latest.upgrade(detail);
      validateLoaded(key, upgraded.document());
      ensureOpen();
      MIndexASTResidentCache.PutOutcome admission = residents.put(key, upgraded);
      if (admission.admitted()) enforceActiveBudget();
      upgrades.incrementAndGet();
      if (!admission.admitted()) upgradeRejections.incrementAndGet();
      if (admission.frequencyRejected()) frequencyAdmissionRejections.incrementAndGet();
      mine.complete(upgraded);
      return upgraded;
    } catch (Error failure) {
      // An allocation/linkage failure must not strand callers already awaiting this upgrade.
      mine.completeExceptionally(failure);
      throw failure;
    } catch (Exception failure) {
      LoadException wrapped = new LoadException(key, failure);
      mine.completeExceptionally(wrapped);
      throw wrapped;
    } finally {
      inFlightUpgrades.remove(upgradeKey, mine);
    }
  }

  /**
   * Keep the last pressure decision active between sampler ticks.
   *
   * <p>Without this gate, one admission immediately after a HIGH/CRITICAL trim could regrow to the
   * hard cache limit before the next memory-pressure sample.</p>
   */
  private void enforceActiveBudget() {
    ActiveBudget budget = activeBudget;
    residents.trimTo(budget.entryLimit(), budget.weightLimit());
  }

  private static Resident await(
      Key key,
      CompletableFuture<Resident> future)
      throws LoadException {
    try {
      return future.get();
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new LoadException(key, interrupted);
    } catch (ExecutionException failed) {
      Throwable cause = failed.getCause();
      if (cause instanceof LoadException known) throw known;
      throw new LoadException(key, cause == null ? failed : cause);
    } catch (java.util.concurrent.CancellationException cancelled) {
      throw new LoadException(key, cancelled);
    }
  }

  private static void validateLoaded(
      Key key,
      MIndexASTDocument document) {
    if (!key.sourceUtf16Sha256().equals(document.sourceUtf16Sha256Hex())) {
      throw new IllegalArgumentException(
          "partial AST source hash mismatch for " + key.sourceRef());
    }
    long actualFingerprint = document.root().astSpec().fingerprint();
    if (actualFingerprint != key.astSpecFingerprint()) {
      throw new IllegalArgumentException(
          "partial AST language/spec fingerprint mismatch for " + key.sourceRef());
    }
  }

  private void ensureOpen() {
    if (closed.get()) throw new IllegalStateException("partial AST cache is closed");
  }

  private static int scaledInt(int value, double fraction) {
    if (fraction <= 0.0) return 0;
    return Math.max(1, (int) Math.floor(value * fraction));
  }

  private static long scaledLong(long value, double fraction) {
    if (fraction <= 0.0) return 0L;
    return Math.max(1L, (long) Math.floor(value * fraction));
  }

  private static MIndexASTResidentCache.Policy toResidentPolicy(
      ResidencyPolicy policy) {
    return switch (Objects.requireNonNull(policy, "policy")) {
      case LRU -> MIndexASTResidentCache.Policy.LRU;
      case WINDOW_TINY_LFU -> MIndexASTResidentCache.Policy.WINDOW_TINY_LFU;
    };
  }

  private static long requirePositiveWeight(long weight) {
    if (weight <= 0L) {
      throw new IllegalArgumentException("resident weight must be > 0");
    }
    return weight;
  }

  private static String sourceUtf16Sha256(CharSequence source) {
    Objects.requireNonNull(source, "source");
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      for (int index = 0; index < source.length(); index++) {
        char unit = source.charAt(index);
        digest.update((byte) (unit >>> Byte.SIZE));
        digest.update((byte) unit);
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }

  private static String requireText(String value, String name) {
    String checked = Objects.requireNonNull(value, name).trim();
    if (checked.isEmpty()) throw new IllegalArgumentException(name + " must not be empty");
    return checked;
  }

  private static String requireSha256(String value, String name) {
    String checked = requireText(value, name);
    if (!checked.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException(name + " must be lowercase SHA-256 hex");
    }
    return checked;
  }

  private record ActiveBudget(int entryLimit, long weightLimit) {
    private ActiveBudget {
      if (entryLimit < 0 || weightLimit < 0L) {
        throw new IllegalArgumentException("negative active cache budget");
      }
    }
  }

  private record UpgradeKey(Key key, Detail detail) {}
}
