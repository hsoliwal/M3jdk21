// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.util.Objects;

/**
 * One content-addressed source admitted to a multipass precompute build.
 *
 * <p>The source object is execution-only. Reproducible identity is carried by {@code id} and the
 * caller-supplied SHA-256 root, so manifests never depend on object identity or iteration order.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/0616288bfa7d91eb0abb754c7a29707f10318752/synexia-index-precompute/src/main/java/com/synexia/precompute/IndexPrecomputeSource.java blob 97eb1d929a60; recipe m3/ports/precompute/recipe/planner-family-20261107/relocate.py v4
public record IndexPrecomputeSource(String id, String rootHash, Object value) {
  public IndexPrecomputeSource {
    id = requireText(id, "id");
    rootHash = requireSha256(rootHash, "rootHash");
    Objects.requireNonNull(value, "value");
  }

  public String sourceType() {
    return value.getClass().getName();
  }

  private static String requireText(String value, String field) {
    String normalized = Objects.requireNonNull(value, field).trim();
    if (normalized.isEmpty()) throw new IllegalArgumentException("blank " + field);
    return normalized;
  }

  static String requireSha256(String value, String field) {
    String normalized = Objects.requireNonNull(value, field).trim().toLowerCase(java.util.Locale.ROOT);
    if (!normalized.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException(field + " must be lowercase/uppercase SHA-256 hex");
    }
    return normalized;
  }
}
