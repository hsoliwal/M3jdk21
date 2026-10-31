// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.fs;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Create-only index from deterministic precompute keys to persisted positive or negative results. */
public final class M3PrecomputeCache {
  private static final int MAX_DESCRIPTOR_BYTES = 256;
  private static final int MAX_BUNDLE_DESCRIPTOR_BYTES = 256 * 1024;
  private final M3DirectoryContentStore store;
  private final Path indexRoot;
  private final Path staging;

  public M3PrecomputeCache(M3DirectoryContentStore store) throws IOException {
    this.store = Objects.requireNonNull(store, "store");
    this.indexRoot = store.root().resolve("precompute-index");
    this.staging = store.root().resolve("staging");
    Files.createDirectories(indexRoot);
  }

  public Optional<M3PrecomputeResult> lookup(M3PrecomputeKey key) throws IOException {
    M3ContentId keyId = Objects.requireNonNull(key, "key").id();
    Path pointer = pointerPath(keyId);
    if (!Files.isRegularFile(pointer)) return Optional.empty();
    String hex = Files.readString(pointer, StandardCharsets.US_ASCII).trim();
    M3ContentId descriptorId = M3ContentId.parse(hex);
    M3ObjectRef descriptor =
        store.ref(descriptorId, M3ObjectKind.PRECOMPUTE_DESCRIPTOR);
    M3PrecomputeResult result =
        M3PrecomputeResult.decode(
            store.readSmall(descriptor, MAX_DESCRIPTOR_BYTES));
    if (!result.keyId().equals(keyId)) {
      throw new IOException("precompute index/key mismatch");
    }
    return Optional.of(result);
  }

  /** One bundle view inside a multi-profile mapped execution set. */
  public record MappedBundle(
      M3ContentId keyId,
      M3FsPrecomputeBundle bundle,
      java.util.List<M3FsPrecomputeBundle.MappedLane> lanes) {
    public MappedBundle {
      keyId = Objects.requireNonNull(keyId, "keyId");
      bundle = Objects.requireNonNull(bundle, "bundle");
      lanes = java.util.List.copyOf(Objects.requireNonNull(lanes, "lanes"));
      if (!bundle.keyId().equals(keyId) || lanes.size() != bundle.lanes().size()) {
        throw new IllegalArgumentException("mapped bundle/key geometry mismatch");
      }
      for (int index = 0; index < lanes.size(); index++) {
        if (!lanes.get(index).lane().equals(bundle.lanes().get(index))) {
          throw new IllegalArgumentException("mapped bundle lane mismatch");
        }
      }
    }

    public M3FsPrecomputeBundle.MappedLane requireLane(String name) {
      String checked = Objects.requireNonNull(name, "name").strip();
      if (checked.isEmpty()) throw new IllegalArgumentException("name");
      return lanes.stream()
          .filter(lane -> lane.lane().name().equals(checked))
          .findFirst()
          .orElseThrow(
              () -> new IllegalArgumentException("missing mapped lane: " + checked));
    }
  }

  /** Multiple bundles sharing one mapping per unique content ID. */
  public record MappedBundleSet(
      java.util.List<MappedBundle> bundles, long uniqueMappedBytes) {
    public MappedBundleSet {
      bundles = java.util.List.copyOf(Objects.requireNonNull(bundles, "bundles"));
      if (bundles.isEmpty() || uniqueMappedBytes < 0L) {
        throw new IllegalArgumentException("mapped bundle set geometry");
      }
    }

    public java.util.List<MappedBundle> bundles(String profile) {
      String checked = Objects.requireNonNull(profile, "profile").strip();
      if (checked.isEmpty()) throw new IllegalArgumentException("profile");
      return bundles.stream()
          .filter(bundle -> bundle.bundle().profile().equals(checked))
          .toList();
    }
  }

  /**
   * Maps an ordered set of bundles while sharing identical content-addressed lane mappings.
   *
   * <p>If any key is absent the complete set is absent. Mapping budgets apply to unique content
   * bytes, not repeated references from multiple profiles.</p>
   */
  public Optional<MappedBundleSet> lookupMappedBundles(
      java.util.List<M3PrecomputeKey> keys,
      int maxLaneBytes,
      long maxTotalBytes)
      throws IOException {
    java.util.List<M3PrecomputeKey> checkedKeys =
        java.util.List.copyOf(Objects.requireNonNull(keys, "keys"));
    if (checkedKeys.isEmpty() || maxLaneBytes < 0 || maxTotalBytes < 0L) {
      throw new IllegalArgumentException("mapped bundle set arguments");
    }

    java.util.HashSet<M3ContentId> keyIds = new java.util.HashSet<>();
    java.util.ArrayList<M3FsPrecomputeBundle> bundles =
        new java.util.ArrayList<>(checkedKeys.size());
    for (M3PrecomputeKey key : checkedKeys) {
      M3PrecomputeKey checked = Objects.requireNonNull(key, "key");
      if (!keyIds.add(checked.id())) {
        throw new IllegalArgumentException("duplicate precompute key");
      }
      Optional<M3FsPrecomputeBundle> bundle = lookupBundle(checked);
      if (bundle.isEmpty()) return Optional.empty();
      bundles.add(bundle.get());
    }

    java.util.LinkedHashMap<M3ContentId, M3ObjectRef> unique =
        new java.util.LinkedHashMap<>();
    long uniqueBytes = 0L;
    for (M3FsPrecomputeBundle bundle : bundles) {
      for (M3FsPrecomputeBundle.Lane lane : bundle.lanes()) {
        M3ObjectRef ref = lane.object();
        if (ref.size() > maxLaneBytes || ref.size() > Integer.MAX_VALUE) {
          throw new IOException("precompute lane exceeds mapping budget: " + lane.name());
        }
        M3ObjectRef prior = unique.putIfAbsent(ref.id(), ref);
        if (prior != null) {
          if (prior.size() != ref.size() || prior.kind() != ref.kind()) {
            throw new IOException("content ID has inconsistent lane reference");
          }
        } else {
          if (ref.size() > maxTotalBytes - uniqueBytes) {
            throw new IOException("precompute bundle set exceeds total mapping budget");
          }
          uniqueBytes += ref.size();
        }
      }
    }

    java.util.HashMap<M3ContentId, M3MappedObject> mapped =
        new java.util.HashMap<>(unique.size());
    for (M3ObjectRef ref : unique.values()) {
      mapped.put(ref.id(), store.openMapped(ref, maxLaneBytes));
    }

    java.util.ArrayList<MappedBundle> result =
        new java.util.ArrayList<>(bundles.size());
    for (int index = 0; index < bundles.size(); index++) {
      M3FsPrecomputeBundle bundle = bundles.get(index);
      java.util.List<M3FsPrecomputeBundle.MappedLane> lanes =
          bundle.lanes().stream()
              .map(
                  lane ->
                      new M3FsPrecomputeBundle.MappedLane(
                          lane, mapped.get(lane.object().id())))
              .toList();
      result.add(new MappedBundle(checkedKeys.get(index).id(), bundle, lanes));
    }
    return Optional.of(new MappedBundleSet(result, uniqueBytes));
  }

  public Optional<M3FsPrecomputeBundle.MappedView> lookupMappedBundle(
      M3PrecomputeKey key, int maxLaneBytes, long maxTotalBytes)
      throws IOException {
    Optional<M3FsPrecomputeBundle> bundle =
        lookupBundle(Objects.requireNonNull(key, "key"));
    if (bundle.isEmpty()) return Optional.empty();
    return Optional.of(bundle.get().openMapped(store, maxLaneBytes, maxTotalBytes));
  }

  public Optional<M3FsPrecomputeBundle> lookupBundle(M3PrecomputeKey key)
      throws IOException {
    M3PrecomputeKey checkedKey = Objects.requireNonNull(key, "key");
    Optional<M3PrecomputeResult> cached = lookup(checkedKey);
    if (cached.isEmpty() || cached.get().isEmpty()) return Optional.empty();
    M3ObjectRef output = cached.get().output();
    if (output.kind() != M3ObjectKind.PRECOMPUTE_BUNDLE_DESCRIPTOR) {
      throw new IOException("precompute result is not a bundle descriptor");
    }
    M3FsPrecomputeBundle bundle =
        M3FsPrecomputeBundle.decode(
            store.readSmall(output, MAX_BUNDLE_DESCRIPTOR_BYTES));
    if (!bundle.keyId().equals(checkedKey.id())) {
      throw new IOException("precompute bundle/key mismatch");
    }
    M3FsPrecomputeCatalog.byId(bundle.profile()).requireCompatible(bundle);
    bundle.requireStore(store);
    return Optional.of(bundle);
  }

  public M3ObjectRef publishBundle(
      M3PrecomputeKey key, M3FsPrecomputeBundle bundle) throws IOException {
    M3PrecomputeKey checkedKey = Objects.requireNonNull(key, "key");
    M3FsPrecomputeBundle checkedBundle =
        Objects.requireNonNull(bundle, "bundle");
    if (!checkedBundle.keyId().equals(checkedKey.id())) {
      throw new IllegalArgumentException("precompute bundle/key mismatch");
    }
    M3FsPrecomputeCatalog.byId(checkedBundle.profile())
        .requireCompatible(checkedBundle);
    checkedBundle.requireStore(store);
    M3ObjectRef descriptor =
        store.put(
            checkedBundle.canonicalBytes(),
            M3ObjectKind.PRECOMPUTE_BUNDLE_DESCRIPTOR);
    publish(M3PrecomputeResult.value(checkedKey, descriptor));
    return descriptor;
  }

  public M3ObjectRef publish(M3PrecomputeResult result) throws IOException {
    M3PrecomputeResult checked = Objects.requireNonNull(result, "result");
    M3ObjectRef descriptor =
        store.put(
            checked.canonicalBytes(), M3ObjectKind.PRECOMPUTE_DESCRIPTOR);
    Path pointer = pointerPath(checked.keyId());
    Files.createDirectories(pointer.getParent());
    byte[] bytes =
        (descriptor.id().hex() + "\n").getBytes(StandardCharsets.US_ASCII);
    Path temp =
        staging.resolve("precompute-" + UUID.randomUUID() + ".tmp");
    Path lockPath =
        pointer.resolveSibling(pointer.getFileName() + ".lock");
    try {
      Files.write(
          temp,
          bytes,
          StandardOpenOption.CREATE_NEW,
          StandardOpenOption.WRITE);
      try (FileChannel lockChannel =
              FileChannel.open(
                  lockPath,
                  StandardOpenOption.CREATE,
                  StandardOpenOption.WRITE);
          java.nio.channels.FileLock lock = lockChannel.lock()) {
        if (!lock.isValid()) {
          throw new IOException("precompute index lock is not valid");
        }
        if (Files.exists(pointer)) {
          String existing =
              Files.readString(pointer, StandardCharsets.US_ASCII).trim();
          if (!existing.equals(descriptor.id().hex())) {
            throw new IOException(
                "deterministic precompute key produced a conflicting result");
          }
        } else {
          moveCreateOnly(temp, pointer);
        }
      }
    } finally {
      Files.deleteIfExists(temp);
    }
    return descriptor;
  }

  private Path pointerPath(M3ContentId keyId) {
    String hex = keyId.hex();
    return indexRoot
        .resolve(hex.substring(0, 2))
        .resolve(hex.substring(2) + ".ref");
  }

  private static void moveCreateOnly(Path source, Path target)
      throws IOException {
    try {
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException unsupported) {
      Files.move(source, target);
    }
  }
}
