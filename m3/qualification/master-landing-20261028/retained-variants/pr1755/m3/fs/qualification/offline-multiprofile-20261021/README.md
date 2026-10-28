# M3JDK21 original-owner multi-consumer offline project checkpoints

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

This is a **forward-recipe successor** to original stacked
[M3JDK21 #1724](https://github.com/hsoliwal/M3jdk21/pull/1724),
not an M3+JDK layer. It retains the original M3File, M3FileTree,
M3BuildState, M3BuildStateStore and M3FS Java/JNI SQLite owners.

Canonical source-sealed recipe continuation:
[Synexia #12807](https://github.com/hsoliwal/com.synexia/pull/12807),
`com.synexia.m3.OfflineMultiProfile20261021` in the second
`m3jdk21-offline-multiprofile-20261021` crate.
Do not overwrite or bypass the previous recipe's exact postimages.

## Corrected ownership and identity

The earlier offline scan used a **single** `/m3-build/offline-scan`
SQLite entry even though its file-state context bound three dimensions:
absolute project URI, stable selection-policy ID and caller toolchain/
compiler-context SHA256. Maven, Gradle and IDE analyzers could
repeatedly invalidate or replace each other's accepted baseline.

The successor uses one independent entry in the SAME per-project M3FS
SQLite database per combined context fingerprint:

```text
/m3-build/state                        compiler-attributed facts (unchanged)
/m3-build/offline-scan                 old unkeyed checkpoint API (preserved)
/m3-build/offline-<64 lowercase hex>   context-keyed completed file observations
```

The 64 lowercase hex characters encode one 32-byte, SHA256-sealed
project/policy/tool context; they are not raw user input or a second
database. `readOffline(fs, context, limits)` checks the saved image
belongs to the exact context and contains neither compiler ABI
proof nor semantic dependency edges. `publishOffline(... context ...)`
requires matching source context, calls the existing native M3FS
SQLite rename / optimistic SHA256 CAS under the same OS lock, and
never writes `/m3-build/state`.

Original unkeyed `readOffline/publishOffline` API is unchanged for
older callers. The scanner now uses keyed overloads; it still
closes one `Stream<M3File>`, computes complete SHA256 from the
same consumed bytes via shadows, and gives consumers added, removed,
changed and full-context rebuild candidates without claiming
javac/HotSpot semantic compatibility.

Nested reactor `module/pom.xml` is correctly classified as
`M3BuildState.BUILD_INPUT`, not a generic RESOURCE. Other
filters, comparator-backed collections, primitive row/mask lanes
and existing Java/JNI exports are untouched.

## Regression evidence, missing proof

The source-sealed candidate updates **three files** serially:
`M3BuildStateStore.java`, `M3OfflineProjectScan.java`, and
`M3OfflineProjectScanTest.java`. Before/after source was checked
against the exact preceding #1724 head, and candidate target
readback matches its Synexia templates byte-for-byte.

The JUnit test now covers alternating Maven/Gradle contexts in
one native SQLite project DB, both warmed baselines unaffected
by the other, rejected mismatched or malformed context, legacy
slot preservation, nested POM role, and original cold/warm,
same-mtime edit, deletion and stale CAS checks.

The following gates **have not been executed** in the combined
source-built M3JDK21 product: Maven/OpenRewrite `Recipe.run`,
source fixed-point and refusal, owning M3FS JNI/JUnit suite,
complete HotSpot `make images`, installed JMOD, jtreg,
Maven/Gradle plugin behavioral tests, multi-platform and
CPU/heap/native-memory benchmarks. Do not infer code qualification,
ABI parity, or performance from source Git blobs alone.

**Important forward merge hold:** current master has a newer
M3BuildState inventory v2 and an observed-versus-successful
checkpoint protocol. Before merging this stacked feature
towards master, adapt the profile key to that newer original
store and retain its successful-build promotion rules. The
old v1 store must NEVER replace the newer master owner.

No project run/watch is required for discovery; generic providers
still need complete hashes to catch same-size, same-mtime edits.
Authoritative provider generation tokens may be admitted later
only through an independently qualified recipe.
