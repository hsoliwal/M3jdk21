// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Stable semantic capabilities of the native M3FS engine.
 *
 * <p>This is deliberately separate from JNI entry-point capability bits. A feature describes
 * externally meaningful filesystem behavior that can be compared against donor/reference
 * filesystems and used by higher MIndex layers when selecting an implementation strategy.</p>
 */
public enum M3FsFeature {
  EXACT_BLOCK_DEDUP(M3FsNative.FEATURE_EXACT_BLOCK_DEDUP),
  SPARSE_FILES(M3FsNative.FEATURE_SPARSE_FILES),
  BLOCK_CLONE_COW(M3FsNative.FEATURE_BLOCK_CLONE_COW),
  HARD_LINKS(M3FsNative.FEATURE_HARD_LINKS),
  SYMLINKS(M3FsNative.FEATURE_SYMLINKS),
  RETAINED_READ_SNAPSHOT(M3FsNative.FEATURE_RETAINED_READ_SNAPSHOT),
  DIRECTORY_PAGING(M3FsNative.FEATURE_DIRECTORY_PAGING),
  SCATTER_GATHER_WRITE(M3FsNative.FEATURE_SCATTER_GATHER_WRITE),
  WAL_CHECKPOINT(M3FsNative.FEATURE_WAL_CHECKPOINT),
  INTEGRITY_VERIFY(M3FsNative.FEATURE_INTEGRITY_VERIFY),
  ORPHAN_BLOCK_GC(M3FsNative.FEATURE_ORPHAN_BLOCK_GC),
  HOLE_PUNCH(M3FsNative.FEATURE_HOLE_PUNCH),
  ALLOCATED_RANGE_QUERY(M3FsNative.FEATURE_ALLOCATED_RANGE_QUERY),
  SCATTER_GATHER_READ(M3FsNative.FEATURE_SCATTER_GATHER_READ),
  NATIVE_MINDEX(M3FsNative.FEATURE_NATIVE_MINDEX),
  NATIVE_REGEX(M3FsNative.FEATURE_NATIVE_REGEX),
  BLOCK_SCRUB(M3FsNative.FEATURE_BLOCK_SCRUB),
  SET_METADATA(M3FsNative.FEATURE_SET_METADATA),
  ATOMIC_REPLACE(M3FsNative.FEATURE_ATOMIC_REPLACE),
  CHECKED_INODE_IO(M3FsNative.FEATURE_CHECKED_INODE_IO),
  RENAME_NOREPLACE(M3FsNative.FEATURE_RENAME_NOREPLACE);

  private final long bit;

  M3FsFeature(long bit) {
    if (bit == 0L || Long.bitCount(bit) != 1) {
      throw new IllegalArgumentException("feature bit must contain exactly one bit");
    }
    this.bit = bit;
  }

  public long bit() {
    return bit;
  }

  public boolean supported() {
    return (M3FsNative.featureBits() & bit) != 0L;
  }

  public static Set<M3FsFeature> supportedFeatures() {
    long bits = M3FsNative.featureBits();
    EnumSet<M3FsFeature> result = EnumSet.noneOf(M3FsFeature.class);
    for (M3FsFeature feature : values()) {
      if ((bits & feature.bit) != 0L) {
        result.add(feature);
      }
    }
    return Collections.unmodifiableSet(result);
  }

  static long knownMask() {
    long result = 0L;
    for (M3FsFeature feature : values()) {
      if ((result & feature.bit) != 0L) {
        throw new IllegalStateException("duplicate M3FS feature bit: " + feature);
      }
      result |= feature.bit;
    }
    return result;
  }
}
