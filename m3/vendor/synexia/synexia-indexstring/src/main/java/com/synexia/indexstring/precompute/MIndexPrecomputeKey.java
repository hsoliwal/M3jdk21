// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.precompute;

import java.util.Objects;

/**
 * Typed identity for one precomputed artifact.
 *
 * <p>The textual id is stable API identity, the Java type prevents accidental cross-artifact
 * casts, and the schema version is part of cache/image compatibility.</p>
 */
public final class MIndexPrecomputeKey<T> implements Comparable<MIndexPrecomputeKey<?>> {
  private final String id;
  private final Class<T> type;
  private final int schemaVersion;

  private MIndexPrecomputeKey(String id, Class<T> type, int schemaVersion) {
    this.id = MIndexPrecomputeScope.required(id);
    this.type = Objects.requireNonNull(type, "type");
    if (schemaVersion < 1) throw new IllegalArgumentException("schemaVersion");
    this.schemaVersion = schemaVersion;
  }

  public static <T> MIndexPrecomputeKey<T> of(
      String id, Class<T> type, int schemaVersion) {
    return new MIndexPrecomputeKey<>(id, type, schemaVersion);
  }

  public String id() {
    return id;
  }

  public Class<T> type() {
    return type;
  }

  public int schemaVersion() {
    return schemaVersion;
  }

  public String canonicalName() {
    return id + "\u0000" + type.getName() + "\u0000" + schemaVersion;
  }

  @Override
  public int compareTo(MIndexPrecomputeKey<?> other) {
    Objects.requireNonNull(other, "other");
    int compared = id.compareTo(other.id);
    if (compared != 0) return compared;
    compared = type.getName().compareTo(other.type.getName());
    return compared != 0 ? compared : Integer.compare(schemaVersion, other.schemaVersion);
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof MIndexPrecomputeKey<?> that
            && schemaVersion == that.schemaVersion
            && id.equals(that.id)
            && type.equals(that.type);
  }

  @Override
  public int hashCode() {
    int hash = id.hashCode();
    hash = 31 * hash + type.hashCode();
    return 31 * hash + schemaVersion;
  }

  @Override
  public String toString() {
    return id + ":" + type.getSimpleName() + ":v" + schemaVersion;
  }
}
