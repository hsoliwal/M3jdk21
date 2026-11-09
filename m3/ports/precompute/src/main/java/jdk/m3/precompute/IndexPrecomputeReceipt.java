// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable proof/telemetry receipt for one compiled precompute generation. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputeReceipt.java blob aeee52dac709; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public record IndexPrecomputeReceipt(
    String generationId,
    Instant createdAt,
    Set<IndexPrecomputeCapability> capabilities,
    long retainedBytes,
    Map<IndexPrecomputeCapability, String> rootHashes,
    Map<IndexPrecomputeKernel, String> selectedAccelerators) {

  public IndexPrecomputeReceipt {
    Objects.requireNonNull(generationId, "generationId");
    Objects.requireNonNull(createdAt, "createdAt");
    capabilities = Set.copyOf(capabilities);
    rootHashes = Map.copyOf(rootHashes);
    selectedAccelerators = Map.copyOf(selectedAccelerators);
    if (retainedBytes < 0) throw new IllegalArgumentException("negative retainedBytes");
  }

  public static IndexPrecomputeReceipt snapshot(
      IndexPrecomputeSnapshot snapshot,
      IndexPrecomputeAccelerators accelerators,
      int representativeBatchSize) {
    return snapshot(snapshot, accelerators, representativeBatchSize, Instant.now());
  }

  public static IndexPrecomputeReceipt snapshot(
      IndexPrecomputeSnapshot snapshot,
      IndexPrecomputeAccelerators accelerators,
      int representativeBatchSize,
      Instant createdAt) {
    Objects.requireNonNull(snapshot, "snapshot");
    Objects.requireNonNull(accelerators, "accelerators");
    Objects.requireNonNull(createdAt, "createdAt");
    if (representativeBatchSize < 0) {
      throw new IllegalArgumentException("negative representativeBatchSize");
    }
    java.util.EnumMap<IndexPrecomputeCapability, String> roots =
        new java.util.EnumMap<>(IndexPrecomputeCapability.class);
    for (IndexPrecomputeCapability capability : snapshot.capabilities()) {
      roots.put(capability, snapshot.facet(capability).orElseThrow().rootHash());
    }
    java.util.EnumMap<IndexPrecomputeKernel, String> selected =
        new java.util.EnumMap<>(IndexPrecomputeKernel.class);
    for (IndexPrecomputeKernel kernel : IndexPrecomputeKernel.values()) {
      selected.put(kernel, accelerators.select(kernel, representativeBatchSize).id());
    }
    return new IndexPrecomputeReceipt(
        snapshot.generationId(),
        createdAt,
        snapshot.capabilities(),
        snapshot.retainedBytes(),
        roots,
        selected);
  }

  /**
   * Reproducible receipt factory for build artifacts.
   *
   * <p>Telemetry callers may keep using {@link #snapshot(M3IndexPrecomputeSnapshot,
   * M3IndexPrecomputeAccelerators, int)}. Sealed build artifacts use the epoch timestamp, while the
   * content root below deliberately excludes wall-clock time.
   */
  public static IndexPrecomputeReceipt deterministic(
      IndexPrecomputeSnapshot snapshot,
      IndexPrecomputeAccelerators accelerators,
      int representativeBatchSize) {
    return snapshot(snapshot, accelerators, representativeBatchSize, Instant.EPOCH);
  }

  /**
   * Timestamp-independent SHA-256 proof identity.
   *
   * <p>The root covers generation ID, actual retained bytes, exact facet roots and selected
   * accelerator IDs in enum order. Two semantically identical builds therefore retain the same
   * root even when telemetry timestamps differ.
   */
  public String rootHash() {
    StringBuilder canonical = new StringBuilder(512);
    canonical.append("INDEX_PRECOMPUTE_RECEIPT_V2\n");
    canonical.append(generationId).append('\n');
    canonical.append(retainedBytes).append('\n');

    ArrayList<IndexPrecomputeCapability> orderedCapabilities =
        new ArrayList<>(capabilities);
    orderedCapabilities.sort(Comparator.comparingInt(Enum::ordinal));
    for (IndexPrecomputeCapability capability : orderedCapabilities) {
      canonical.append("C\t").append(capability.name()).append('\t')
          .append(rootHashes.getOrDefault(capability, "")).append('\n');
    }
    for (IndexPrecomputeKernel kernel : IndexPrecomputeKernel.values()) {
      String selected = selectedAccelerators.get(kernel);
      if (selected != null) {
        canonical.append("K\t").append(kernel.name()).append('\t')
            .append(selected).append('\n');
      }
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(
          digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }
}
