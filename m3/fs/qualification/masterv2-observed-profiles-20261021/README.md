# M3JDK21 master-V2 SQLite observed profiles — exact original owner

Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0.

M3JDK21 is **one Java21/HotSpot/JNI SDK**. This receiving PR
modifies only the original `com.m3.fs.M3BuildStateStore`, its
`M3BuildStateTest` JUnit proof, and source provenance. It is
directly based on current M3JDK21 `master` and preserves the
version-2 complete-inventory and successful-build source contracts.

## The native SQLite authority does not change

`/m3-build/state` is the single successfully completed build.
The original unprofiled `/m3-build/observed-<successful>` path
and its methods are retained unchanged for binary/API compatibility.

Maven, Gradle and IDE analyzers may stage completed file-input
inventories independently, by choosing the exact pre-existing
`M3BuildState.contextSha256()` as their profile identity.
The new overloads address:

```text
/m3-build/state                                            ONE successful build
/m3-build/observed-initial                                unprofiled candidate
/m3-build/observed-initial-<64hex context SHA256>         cold profiled candidate
/m3-build/observed-<64hex successful>                     unprofiled successor
/m3-build/observed-<64hex successful>-<64hex profile>     profiled successor
```

`readObserved(snapshot, expectedSuccessful, profile, ...)`
requires a pinned M3FS native SQLite snapshot and validates that
the original globally successful head and observed source profile
belong to the same view. `observe(..., profile, ...)`
retains the original completion, source/context and CAS checks,
with no second SQL engine or native pointers. `promoteObserved`
requires caller-certified successful execution of that exact
profile observation and commits the **single** global build head.

A different profile does not become a second successful state;
after a successful promotion its earlier base becomes stale
and must be revalidated. Failed compiler/plugin/build work must
not promote an observed checkpoint.

The source keeps existing public method descriptors, native
JNI signatures, SQLite WAL and original OS file lock; only
three **additive** overloads and bounded private helpers are
introduced. Profile names encode fixed 32-byte digests to
lowercase hex, never arbitrary filesystem path input. A
mismatched embedded M3BuildState context or invalid profile
is refused before any writer mutation.

## One input stream and project SQLite integration

The original earlier feature
[M3JDK21 #1724](https://github.com/hsoliwal/M3jdk21/pull/1724)
and its [multi-context successor #1755](https://github.com/hsoliwal/M3jdk21/pull/1755)
derive `M3File` full content SHA256 and optional
`M3AST`/grammar shadows through one original input traversal.
This master-V2 successor deliberately does **not** copy the older
file/tree scanner or older V1 build-state classes over master.

The follow-on M3File integration must construct the master-V2
`completeInventory()` source evidence, stage it with these
profiled `observe` APIs, perform original Maven/javac semantic
and ABI impact analysis, and call `promoteObserved` only after
successful compilation and plugin execution. The live/project
SQLite native source remains authoritative.

## Canonical recipe, local source receipt and gates

Reuse [Synexia PR #12807](https://github.com/hsoliwal/com.synexia/pull/12807),
third source-sealed `m3jdk21-observed-profiles-masterv2-20261021`
stage `com.synexia.m3.ObservedProfilesMasterV220261021`.
Existing Java `M3HashPinnedJavaSnapshotRecipe` records exact
master-V2 preimages and candidate postimage SHA256. Serially
qualified source changes are the original M3BuildStateStore.java
and M3BuildStateTest.java. No JDK/GPL source bodies or LeetCode,
HackerRank, GeeksforGeeks solutions are copied.

Original JUnit test now includes a scenario in which Maven
and Gradle inventories with distinct exact tool contexts both
stage snapshots inside one native M3FS SQLite project DB, and
checks that promoting Maven invalidates the previous global
successful-base assumption for Gradle. The unprofiled method
remains untouched and invalid context is refused.

**Verified here:** original source preimages and GitHub candidate
postimage equality. **Not executed:** actual full Maven/OpenRewrite
SDK fixed point/source refusal, original M3FS SQLite JNI/JUnit,
complete source-built M3JDK21 HotSpot/JMOD/jtreg, multi-platform
providers, Maven/Gradle integrations and comparative performance.
Do not equate source hashes or a queued CI action with passed
runtime/ABI qualification; keep this a draft.
