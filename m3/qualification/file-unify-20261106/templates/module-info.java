// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/**
 * The jdk.m3.file family in one module: the deliberately narrow NIO file-access boundary (file deltas and ledgers,
 * build impact, shadow streams, resource locks, watcher; the former module jdk.m3.file of m3/file) and the whole
 * MIndexFile forward port over canonical JDK String owners and file metadata (M3ByteFile, MappedM3File, the file
 * universe, M3Fmt*, M3JniLibraryLoader; the former module org.m3jdk.file of m3/ports/file). The progress contract is
 * the owner jdk.m3.collections.M3ProgressMonitor (m3/qualification/progress-monitor-unify-20261106).
 */
//FLLDCIM: FILE_UNIFY - union of module-info jdk.m3.file (m3/file, blob 69d747ed4562) and org.m3jdk.file (m3/ports/file, blob 985858e1561f): requires, exports and opens of both; recipe m3/qualification/file-unify-20261106/unify.py v1
module org.m3jdk.file {
  requires transitive jdk.m3.collections;
  exports jdk.m3.file;
  exports jdk.m3.file.json;
}
