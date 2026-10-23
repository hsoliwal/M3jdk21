<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21: primitive aligned file-delta batch — review receipt (2026-10-21)

## Runtime owner

This is a direct, additive capability in M3JDK21's existing `jdk.m3fs` / `com.m3.fs` implementation, **not** a Synexia runtime dependency, JDK overlay, or replacement for the existing `MFileDelta.between` oracle.

`MFileDeltas.betweenAligned(MFileObservation[], MFileObservation[])` and the `List` convenience overload use one exact per-ordinal source identity. The batch owns two defensive reference arrays and one `long[]` reason-mask lane; it retains no per-ordinal `MFileDelta`, node, map entry or child facade. The caller can count, select an exact `int[]` of physical ordinals, visit ordinals with an `IntConsumer`, or request deltas only as needed. The public source/target observation types and existing delta contracts remain untouched.

The implementation is serial per file, and its compare/snapshot and mask-selection atoms are individually replaceable. The existing single-pair delta remains the behavioral oracle.

## No false deletion / metadata-equality claims

Both lists must contain the **same number of paired provider addresses in the same order**. The API rejects mismatched keys and missing pairs instead of treating a key omitted from a stream as removed. A provider-confirmed absence must already be represented as `MFileObservation.absent(key)`. A traversal filtered by a predicate, stopped early, failed, or externally modified is not evidence of a complete, atomic namespace scan. Metadata equal does not establish content equal.

The provider address is never embedded in validation exceptions because addresses may contain credentials.

## Complexity and storage

Comparison: O(n) calls to existing `MFileDelta.between`; O(n) retained reference/mask arrays (one `long[]`, two `MFileObservation[]`), no retained entry/delta objects. Each count/ordinal visit: O(n) time, O(1) additional space; ordinal materialization O(n) time and exact O(k) `int[]` space; requesting one full delta recomputes its existing constant-pair evidence comparison. No claim of universal speedup, lock-free iteration, snapshot atomicity or native acceleration.

See [DONORS.tsv](DONORS.tsv) for category-by-category GitHub/LeetCode/HackerRank/GeeksforGeeks judgement and JNI admission deferral.

## Recipe and provenance

Canonical Synexia OpenRewrite owner:
`hsoliwal/com.synexia/synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3fs-aligned-deltas-20261021/`.

Reuse `M3HashPinnedJavaSnapshotRecipe`, manifest exact `ABSENT` preimages, SHA-256 postimages and two Java templates. The named recipe is `com.synexia.rewrite.M3FSAlignedDeltas20261021`. A canonical test checks Java LST emission, fixed point, refusal of occupied targets and template hashes. Source and tests are byte-identical at target. No unreviewed source gets overwritten.

Source SHA-256: `6f9cb6a16b29422ed272f901bf5e76343af5960b1b0ce899393189fb00053d9f`.

Test SHA-256: `cef27216d6706b7e14a32af451c8424760d35a86bf9a3795ad8704f262a1be0d`.

## Gates and status

- **PASSED, isolated**: Java 21 `javac --release 21 -Xlint:all -Werror` and five focused tests against deliberately simplified contract-compatible MFileObservation/MFileDelta stubs. This checks Java syntax, array/mask traversal and callback behavior, **not** the true M3FS evidence engine.
- **PENDING, mandatory**: canonical Synexia Maven/OpenRewrite recipe replay/test, full target `mvn -B -ntp -f m3/fs/pom.xml verify`, real `MFileDelta` differential suite, `-Xcheck:jni`, CTest, JMOD installed-image API verification, native memory/CPU measurements.
- **NOT CLAIMED**: completion of Eclipse workspace refresh/watch, EFS provider lifecycle, SWT/EMF/JDT integrations, or the full M3JDK21 collections replacement programme.

## Follow-on DAG, never overwrite history

First qualify current source/test and canonical recipe, then add complete-inventory witness and safe sorted primitive index merge if required by actual Eclipse refresh/workspace traces. A later measured JNI lane may process 64-bit reason words only after full-width ABI, bounds and fallback tests. Eclipse's original `IResourceDelta` and EFS extension hooks remain authoritative until each corresponding fork has independent contract/provenance validation. Keep M3JDK21's String -> arrays -> collections -> compiler -> other families ordering.
