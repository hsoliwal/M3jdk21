<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 multi-provider bulk command receiver (2026-10-21)

**Product owner:** `jdk.m3fs`, existing `com.m3.fs` package. **Canonical authoring owner:** Synexia Maven/OpenRewrite hash-pinned recipe `com.synexia.m3.M3FsManagerBulk20261021`. No Synexia runtime dependency. Target branch is additive against `master` at `c831c4a77f0e3e10aac94178217efda882ba4be5`; this is not a String/array/collection receiving-stage promotion.

## Atom inventory

- `M3FsManagerBulk.execute`: manager-resolved ordered command dispatch, preserving existing `M3FsBulk` byte[] opcode/status, String[] source/target and int[] metadata lane owners. No retained per-row carrier nodes.
- `execute(..., first, count, long[])`: physical command-slot interval plus detached 64-bit selection mask. No compacted payload or shuffling of command indices; all indexes are batch-absolute. High unused mask bits are ignored.
- `perform`: existing VFS `resolveFile`/FileObject primitives only; `copyFrom` or `moveTo` retain provider-defined cross-provider semantics. Checked failures remain indexed in original positions.

## Existing contracts not changed

`FileSystem`, `FileSystemManager`, `AbstractFileSystem`, `M3FsBulk`, Java NIO, Eclipse EFS and public JDK `java.*` owners are untouched. An atomic batch is rejected before resolving paths. A provider-specific native bulk hook remains owned by `AbstractFileSystem`. Cancellation is not converted to a row failure, and there is no invented multi-provider transaction or consistent snapshot.

## Local isolated proof

- Java 21 `javac --release 21 -Xlint:all -Werror` source and five Java behavior oracles: **PASS using minimal API-compatible VFS/JUnit stand-ins**, not the repository's full VFS implementation.
- Five isolated tests cover heterogeneous routing and metadata, physical mask/window selection, 63/64/127/128-bit boundaries and defensive mask snapshot, checked failure/fail-fast/progress, and pre-I/O refusals.
- Source SHA-256 (new Java): `0c098021950e30b67d47629e59f616e0aca9030da6a479ef8fe918d7a6e48ca0`.
- Test SHA-256 (new Java): `33a7e2c48b5b53de94fe508d42d520c297ca810ab67060210ca0caa42c30beb6`.

**Unexecuted gates / HOLD:** real Synexia OpenRewrite Maven recipe run, source-drift/fixed-point test, `mvn -f m3/fs/pom.xml verify`, actual native SQLite/JNI `-Xcheck:jni` and CTest, full M3JDK21 image/JMOD/jtreg, Eclipse EFS/OSGi, all supported provider families, platform matrix, CPU and heap/native-memory comparisons. No direct JNI symbol was added: arbitrary VFS provider dispatch remains Java, while existing JNI stat/array candidates must be independently measured before promotion.

Review with the companion Synexia recipe and verified target file readbacks before marking ready. This is one serial per-file addition toward the larger combined roadmap, not complete universal filesystem convergence.
