// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable generation-scoped registry of all compiled precompute facets. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputeSnapshot.java blob e0e6f9d13518; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public final class M3IndexPrecomputeSnapshot {
  private final String generationId;
  private final EnumMap<M3IndexPrecomputeCapability, M3IndexPrecomputeFacet> facets;
  private final EnumMap<M3IndexPrecomputeCapability, Map<String, M3IndexPrecomputeFacet>> ownedFacets;
  private final long retainedBytes;

  M3IndexPrecomputeSnapshot(
      String generationId,
      Map<M3IndexPrecomputeCapability, ? extends M3IndexPrecomputeFacet> source) {
    this(generationId, source, inferOwnership(source));
  }

  M3IndexPrecomputeSnapshot(
      String generationId,
      Map<M3IndexPrecomputeCapability, ? extends M3IndexPrecomputeFacet> source,
      Map<M3IndexPrecomputeCapability, ? extends Map<String, ? extends M3IndexPrecomputeFacet>>
          ownership) {
    this.generationId = requireId(generationId);
    facets = new EnumMap<>(M3IndexPrecomputeCapability.class);
    ownedFacets = new EnumMap<>(M3IndexPrecomputeCapability.class);
    long bytes = 0;
    Map<String, Long> countedStorage = new java.util.HashMap<>();
    for (var entry : source.entrySet()) {
      M3IndexPrecomputeCapability capability = Objects.requireNonNull(entry.getKey(), "capability");
      M3IndexPrecomputeFacet facet = Objects.requireNonNull(entry.getValue(), "facet");
      if (facet.capability() != capability) {
        throw new IllegalArgumentException("facet capability mismatch: " + capability);
      }
      if (facets.put(capability, facet) != null) {
        throw new IllegalArgumentException("duplicate capability " + capability);
      }

      Map<String, ? extends M3IndexPrecomputeFacet> declared =
          Objects.requireNonNull(ownership.get(capability), "facet ownership");
      LinkedHashMap<String, M3IndexPrecomputeFacet> owners = new LinkedHashMap<>();
      new java.util.TreeMap<>(declared).forEach((ownerId, owned) -> {
        String owner = M3IndexPrecomputeFacetGroup.requireOwnerId(ownerId);
        M3IndexPrecomputeFacet value = Objects.requireNonNull(owned, "owned facet");
        if (value.capability() != capability) {
          throw new IllegalArgumentException("owned facet capability mismatch: " + capability);
        }
        owners.put(owner, value);
      });
      if (owners.isEmpty()) throw new IllegalArgumentException("capability has no semantic owner");
      ownedFacets.put(capability, Map.copyOf(owners));

      for (Map.Entry<String, Long> claim : facet.storageClaims().entrySet()) {
        String key = M3IndexPrecomputeFacetGroup.requireStorageKey(claim.getKey());
        long retained = M3IndexPrecomputeFacetGroup.requireBytes(claim.getValue());
        Long previous = countedStorage.putIfAbsent(key, retained);
        if (previous == null) bytes = Math.addExact(bytes, retained);
        else if (previous.longValue() != retained) {
          throw new IllegalArgumentException(
              "shared storage has inconsistent byte accounting: " + key);
        }
      }
    }
    retainedBytes = bytes;
  }

  public String generationId() { return generationId; }
  public long retainedBytes() { return retainedBytes; }
  public Set<M3IndexPrecomputeCapability> capabilities() { return Set.copyOf(facets.keySet()); }
  public boolean has(M3IndexPrecomputeCapability capability) { return facets.containsKey(capability); }

  public Optional<M3IndexPrecomputeFacet> facet(M3IndexPrecomputeCapability capability) {
    return Optional.ofNullable(facets.get(Objects.requireNonNull(capability, "capability")));
  }

  /** Stable owner-scoped facets for one capability; empty when the capability is absent. */
  public Map<String, M3IndexPrecomputeFacet> ownedFacets(M3IndexPrecomputeCapability capability) {
    Map<String, M3IndexPrecomputeFacet> values =
        ownedFacets.get(Objects.requireNonNull(capability, "capability"));
    return values == null ? Map.of() : values;
  }

  public Optional<M3IndexPrecomputeFacet> ownedFacet(
      M3IndexPrecomputeCapability capability, String ownerId) {
    return Optional.ofNullable(
        ownedFacets(capability).get(M3IndexPrecomputeFacetGroup.requireOwnerId(ownerId)));
  }

  public <T extends M3IndexPrecomputeFacet> T requireOwned(
      M3IndexPrecomputeCapability capability, String ownerId, Class<T> type) {
    M3IndexPrecomputeFacet facet = ownedFacet(capability, ownerId)
        .orElseThrow(() -> new IllegalStateException(
            "capability owner not compiled: " + capability + ":" + ownerId));
    return Objects.requireNonNull(type, "type").cast(facet);
  }

  public <T extends M3IndexPrecomputeFacet> T require(
      M3IndexPrecomputeCapability capability, Class<T> type) {
    M3IndexPrecomputeFacet facet = facets.get(Objects.requireNonNull(capability, "capability"));
    if (facet == null) throw new IllegalStateException("capability not compiled: " + capability);
    return Objects.requireNonNull(type, "type").cast(facet);
  }

  private static Map<
      M3IndexPrecomputeCapability, Map<String, M3IndexPrecomputeFacet>> inferOwnership(
      Map<M3IndexPrecomputeCapability, ? extends M3IndexPrecomputeFacet> source) {
    EnumMap<M3IndexPrecomputeCapability, Map<String, M3IndexPrecomputeFacet>> result =
        new EnumMap<>(M3IndexPrecomputeCapability.class);
    Objects.requireNonNull(source, "source").forEach((capability, facet) -> {
      M3IndexPrecomputeFacet value = Objects.requireNonNull(facet, "facet");
      result.put(
          Objects.requireNonNull(capability, "capability"),
          Map.of(value.getClass().getName(), value));
    });
    return Map.copyOf(result);
  }

  private static String requireId(String raw) {
    String value = Objects.requireNonNull(raw, "generationId").trim();
    if (value.isEmpty()) throw new IllegalArgumentException("blank generationId");
    return value;
  }

  /**
   * Explicit initialization boundary around this existing registry. It neither owns another
   * dictionary nor makes requireReady() a lazy loader. Supply one Startup per declared generation
   * and class-loader domain, and call start() from the application's launch phase.
   */
  public static final class Startup {
    public enum State { NEW, LOADING, READY, FAILED }

    /** Load/attach an already-published generation; do not silently compile missing facts. */
    @FunctionalInterface public interface Loader {
      M3IndexPrecomputeSnapshot load(M3Progress monitor) throws Exception;
    }

    /** Bind native plans, initialize declared capabilities and preload before publication. */
    @FunctionalInterface public interface Preparation {
      void prepare(M3IndexPrecomputeSnapshot snapshot, M3Progress monitor)
          throws Exception;
    }

    private record Publication(State state, M3IndexPrecomputeSnapshot snapshot,
        Throwable failure, long elapsedNanos) { }
    private final String expectedGeneration;
    private final Set<M3IndexPrecomputeCapability> required;
    private final long maxRetainedBytes;
    private final Loader loader;
    private final Preparation preparation;
    private volatile Publication publication = new Publication(State.NEW, null, null, 0);
    private java.util.concurrent.FutureTask<M3IndexPrecomputeSnapshot> task;
    private Thread initializingThread;

    public Startup(String expectedGeneration, Set<M3IndexPrecomputeCapability> required,
        long maxRetainedBytes, Loader loader, Preparation preparation) {
      this.expectedGeneration = requireId(expectedGeneration);
      this.required = Set.copyOf(Objects.requireNonNull(required, "required"));
      if (maxRetainedBytes < 0) throw new IllegalArgumentException("negative startup byte budget");
      this.maxRetainedBytes = maxRetainedBytes;
      this.loader = Objects.requireNonNull(loader, "loader");
      this.preparation = Objects.requireNonNull(preparation, "preparation");
    }

    public State state() { return publication.state(); }
    /** Startup time, separate from first-operation latency. Zero until completion. */
    public long elapsedNanos() { return publication.elapsedNanos(); }

    /**
     * Executes once in the winning caller, never in a background executor. Other startup callers
     * await that same result. Canceling/interruption of a waiting caller does not cancel the owner.
     * Owner failure is terminal for this Startup: retry explicitly with a new instance.
     */
    public M3IndexPrecomputeSnapshot start(M3Progress monitor) {
      M3Progress active = monitor == null
          ? M3Progress.none() : monitor;
      checkpoint(active);
      java.util.concurrent.FutureTask<M3IndexPrecomputeSnapshot> selected;
      boolean run;
      synchronized (this) {
        if (initializingThread == Thread.currentThread())
          throw new IllegalStateException("recursive precompute startup");
        run = task == null;
        if (run) {
          var created = new java.util.concurrent.FutureTask<M3IndexPrecomputeSnapshot>(() -> initialize(active));
          Publication loading = new Publication(State.LOADING, null, null, 0);
          task = created;
          initializingThread = Thread.currentThread();
          publication = loading;
        }
        selected = task;
      }
      if (run) {
        try { selected.run(); }
        finally { synchronized (this) { initializingThread = null; } }
      }
      return await(selected, active);
    }

    /**
     * Readiness check only: no waiting, classpath discovery, loading, building or native binding.
     * Keeping the returned snapshot/facets alive retains their existing owners and mappings.
     */
    public M3IndexPrecomputeSnapshot requireReady() {
      Publication current = publication;
      if (current.state() != State.READY)
        throw new IllegalStateException("precompute is not READY: " + current.state(), current.failure());
      return current.snapshot();
    }

    private M3IndexPrecomputeSnapshot initialize(M3Progress monitor)
        throws Exception {
      long begin = System.nanoTime();
      try {
        checkpoint(monitor);
        M3IndexPrecomputeSnapshot candidate = Objects.requireNonNull(loader.load(monitor), "loaded snapshot");
        validate(candidate, monitor);
        preparation.prepare(candidate, monitor);
        // Recheck declared facts after preparation; lazy owners may have grown or changed.
        validate(candidate, monitor);
        checkpoint(monitor);
        publication = new Publication(State.READY, candidate, null, System.nanoTime() - begin);
        return candidate;
      } catch (Exception | Error failure) {
        publication = new Publication(State.FAILED, null, failure, System.nanoTime() - begin);
        throw failure;
      }
    }

    private void validate(M3IndexPrecomputeSnapshot candidate,
        M3Progress monitor) {
      checkpoint(monitor);
      if (!expectedGeneration.equals(candidate.generationId()))
        throw new IllegalStateException("unexpected precompute generation");
      if (!candidate.facets.keySet().containsAll(required))
        throw new IllegalStateException("required precompute capabilities missing");
      Map<String, Long> storage = new java.util.HashMap<>();
      long bytes = 0;
      for (var entry : candidate.facets.entrySet()) {
        checkpoint(monitor);
        M3IndexPrecomputeFacet facet = entry.getValue();
        if (facet.capability() != entry.getKey()) throw new IllegalStateException("facet contract changed");
        for (Map.Entry<String, Long> claim : facet.storageClaims().entrySet()) {
          String key = M3IndexPrecomputeFacetGroup.requireStorageKey(claim.getKey());
          long retained = M3IndexPrecomputeFacetGroup.requireBytes(claim.getValue());
          Long previous = storage.putIfAbsent(key, retained);
          if (previous == null) bytes = Math.addExact(bytes, retained);
          else if (previous.longValue() != retained)
            throw new IllegalStateException("shared storage has inconsistent byte accounting");
        }
      }
      if (bytes != candidate.retainedBytes() || bytes > maxRetainedBytes)
        throw new IllegalStateException("precompute exceeds its sealed startup byte budget");
    }

    private static M3IndexPrecomputeSnapshot await(
        java.util.concurrent.FutureTask<M3IndexPrecomputeSnapshot> selected,
        M3Progress monitor) {
      for (;;) {
        checkpoint(monitor);
        try { return selected.get(25, java.util.concurrent.TimeUnit.MILLISECONDS); }
        catch (java.util.concurrent.TimeoutException notYetReady) { /* cancellation checkpoint */ }
        catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          var canceled = new java.util.concurrent.CancellationException("startup waiter interrupted");
          canceled.initCause(interrupted);
          throw canceled;
        } catch (java.util.concurrent.ExecutionException failed) {
          Throwable cause = failed.getCause();
          if (cause instanceof RuntimeException runtime) throw runtime;
          if (cause instanceof Error error) throw error;
          throw new IllegalStateException("precompute startup failed", cause);
        }
      }
    }

    private static void checkpoint(M3Progress monitor) {
      monitor.checkCanceled();
      if (Thread.currentThread().isInterrupted())
        throw new java.util.concurrent.CancellationException("precompute startup interrupted");
    }
  }

  /** Optional standard-Java premain adapter. Ordinary launches and existing factories are unchanged. */
  public static final class Bootstrap {
    /** Application-owned configuration: choose the existing loaders, pins, budgets and preparation. */
    public interface Provider { Startup startup(); }
    private static volatile Startup readyStartup;
    private static boolean entered;
    private Bootstrap() { }

    /**
     * Agent argument is the binary name of a Provider with a public no-argument constructor.
     * No bytecode transform or Unsafe is used. Any startup failure escapes premain, preventing main.
     * The holder belongs to the agent's system class loader, not to all possible class loaders.
     */
    public static synchronized void premain(String providerClass) throws Exception {
      if (entered) throw new IllegalStateException("precompute bootstrap already entered");
      entered = true;
      String name = Objects.requireNonNull(providerClass, "startup provider class").trim();
      if (name.isEmpty()) throw new IllegalArgumentException("startup provider class is required");
      Class<? extends Provider> type = Class.forName(name, true, ClassLoader.getSystemClassLoader())
          .asSubclass(Provider.class);
      Startup startup = Objects.requireNonNull(type.getConstructor().newInstance().startup(), "startup");
      startup.start(null);
      readyStartup = startup;
    }

    /** Does not start the JVM hook or initialize missing precomputation. */
    public static M3IndexPrecomputeSnapshot requireReady() {
      Startup current = readyStartup;
      if (current == null) throw new IllegalStateException("precompute bootstrap did not complete");
      return current.requireReady();
    }

    public static long elapsedNanos() {
      Startup current = readyStartup;
      if (current == null) throw new IllegalStateException("precompute bootstrap did not complete");
      return current.elapsedNanos();
    }
  }
}
