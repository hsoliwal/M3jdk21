<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 result mask / physical-ordinal receiver — 2026-10-21

**Product authority:** original M3JDK21 `jdk.m3fs`, `com.m3.fs.M3FsBulk.Result`. **Canonical authoring authority:** Synexia recipe `com.synexia.m3.M3FsResultMasks20261021`. No new filesystem, wrapper layer, retained per-command object nodes, Java SE class name, native ABI or Synexia runtime dependency.

## Contract-bearing atoms (serial per file)
1. `M3FsBulk.java`: additive `STATUS_FLAG_*` constants; `Result.selectedCount`, `selectedOrdinals` and `selectedMaskWords`, each with full-result and physical-offset/length overloads. Status and metadata columns remain private, cloned and unchanged. A status beyond OK/NO_EFFECT/FAILED/SKIPPED never matches even if a bit shift aliases a known flag.
2. `M3FsBulkTest.java`: append independent original-provider-oracle tests for byte-for-byte metadata/status/error retention, 63/64/127/128/133 mask geometry, offsets, invalid filters and 520 deterministic randomized linear comparisons. Existing tests and original public/protected methods are untouched.
3. Source-sealed donor category catalogue and bounded execution receipt are target data, not an independent recipe authority.

## Complexity and ownership
`selectedCount` O(window) time, O(1) extra space; `selectedOrdinals` O(window) time with two passes and one detached exact-sized `int[k]`; `selectedMaskWords` O(window) time and O(ceil(parent size/64)) detached words. Selection maintains parent physical indexes, original command order and immutable snapshot semantics. Returned masks are *observations*, not safe automatic retries across arbitrary providers. No sorting/comparator or predicate callback is introduced in JNI.

## Executed bounded proof
- Java 21 `javac --release 21 -Xlint:all -Werror` compiled a self-contained source with exactly these Result selection atoms and an independent linear reference: **PASS**.
- Deterministic execution: **520 windows / 1,560 count/index/mask differential assertions PASS**, plus status bound, input snapshot, 63/64 and invalid-window/flag checks.
- This was **isolated validation** of the candidate atoms, **NOT a compile or test of the exact target M3FS production source or actual JUnit suite**.

## Required gates not executed — DRAFT/HOLD
- Canonical Maven/OpenRewrite `Recipe.run` with exact preimages, drift/missing/duplicate/refusal, fixed-point and permutation tests.
- Actual `mvn -B -ntp -f m3/fs/pom.xml verify` with original VFS provider/JUnit and SQLite/CTest.
- Native `-Xcheck:jni`, installed linked `jdk.m3fs` JMOD/JDK21, Eclipse/OSGi, jtreg, comparative CPU/heap/native-process benchmarks, all providers/OSes.
- No native symbol is introduced or existing SQLite/JNI semantics modified; real native bulk stat remains separate in target PR #1740.

Product changes must be generated from, and read back against, exact Synexia source templates. Neither copied donor snippets nor an isolated oracle grants whole-JDK promotion or completeness.
