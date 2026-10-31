<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 multi-provider bulk command receiver (2026-10-21)

**Product owner:** `jdk.m3fs`, existing `com.m3.fs` package. **Canonical authoring owner:** Synexia Maven/OpenRewrite hash-pinned recipe `com.synexia.m3.M3FsManagerBulk20261021`. No Synexia runtime dependency. Target branch is additive against `master` at `c831c4a77f0e3e10aac94178217efda882ba4be5`; this is not a String/array/collection receiving-stage promotion.

## Atom inventory

- `M3FsManagerBulk.execute`: manager-resolved ordered command dispatch, preserving existing `M3FsBulk` byte[] opcode/status, String[] source/target and int[] metadata lane owners. No retained per-row carrier nodes.
- `execute(..., first, count, long[])`: physical command-slot interval plus detached 64-bit selection mask. Sparse masks now iterate selected bits using `Long.numberOfTrailingZeros` and word jumps instead of probing every rejected entry. No compacted payload or shuffling of command indices; all indexes are batch-absolute. High unused mask bits are ignored.
- `perform`: existing VFS `resolveFile`/FileObject primitives only; `copyFrom` or `moveTo` retain provider-defined cross-provider semantics. Checked failures remain indexed in original positions.
- `FileSystemManager` default atom: exposes execute/statAll/createFolders/deletePaths/copyAll/moveAll directly in the existing manager interface, with aligned input preflight and no new runtime owner.

## Existing contract preservation and additive default API

`FileSystem`, `AbstractFileSystem`, `M3FsBulk`, Java NIO, Eclipse EFS and public JDK `java.*` owners are untouched. The existing `FileSystemManager` interface has **additive Java default methods only**, leaving every original method signature intact. Providers inherit the defaults and keep their URI resolution and capability rules. An atomic batch is rejected before resolving paths. A provider-specific native bulk hook remains owned by `AbstractFileSystem`. Cancellation is not converted to a row failure, and there is no invented multi-provider transaction or consistent snapshot.

## Local isolated proof

- Java 21 `javac --release 21 -Xlint:all -Werror` source and ten Java behavior oracles: **PASS using minimal API-compatible VFS/JUnit stand-ins**, not the repository's full VFS implementation.
- Ten isolated tests cover heterogeneous routing and metadata, physical mask/window selection, 63/64/127/128-bit boundaries and defensive mask snapshot, checked failure/fail-fast/progress, direct interface default dispatch and aligned copy/move validation, plus 320 deterministic randomized differential windows against the original linear reference and explicit empty/zero windows.
- Source SHA-256 (refined Java): `46ab530ca32c9ee97c3b5d83e04cb58e8e26fc321f78cad5d417dc45208b42b7`.
- Test SHA-256 (refined Java): `a3ec4ed73f40b21dde25c4871bc81af08d94144fcf461ec170431c1f2ba9927a`.

- Original manager interface SHA-256 before: `1ce71e0d467a5f4cada847bb03fffebdb9d4110d13c2cb606788377fa4981e10`; after: `58f117166de0933b0172796f6afb447527bfc918a8fe1ddab2a7efa059724ff2`.
- The new three default-method tests first exposed a false negative in a **local stand-in**: its `M3FsBulk.Builder` omitted real null-target validation. Correcting the stand-in to match the existing product owner restored the expected exception. The source and test fixture retain that adversarial case; this does not establish full real-provider parity.

- Selection algorithm comparison: linear per-position test O(n) versus 64-bit word cursor O(ceil(n/64)+k), with no new JNI crossing. This is a complexity and isolated-correctness claim, not a measured whole-provider performance claim.

**Unexecuted gates / HOLD:** real Synexia OpenRewrite Maven recipe run, source-drift/fixed-point test, `mvn -f m3/fs/pom.xml verify`, actual native SQLite/JNI `-Xcheck:jni` and CTest, full M3JDK21 image/JMOD/jtreg, Eclipse EFS/OSGi, all supported provider families, platform matrix, CPU and heap/native-memory comparisons. No direct JNI symbol was added: arbitrary VFS provider dispatch remains Java, while existing JNI stat/array candidates must be independently measured before promotion.

Review with the companion Synexia recipe and verified target file readbacks before marking ready. This is one serial per-file source-sealed refinement toward the larger combined roadmap, not complete universal filesystem convergence.
