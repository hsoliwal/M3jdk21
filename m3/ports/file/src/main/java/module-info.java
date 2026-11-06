// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/**
 * Whole MIndexFile family forward port over canonical JDK String owners and file metadata. The progress
 * contract is the owner jdk.m3.collections.M3ProgressMonitor (m3/qualification/progress-monitor-unify-20261106).
 */
module org.m3jdk.file {
  requires transitive jdk.m3.collections;
  exports jdk.m3.file;
  exports jdk.m3.file.json;
}
