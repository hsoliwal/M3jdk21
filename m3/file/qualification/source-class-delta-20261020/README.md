<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Native M3JDK21 source and class delta facts

The authoritative SDK target is `hsoliwal/M3JDK21` — one JDK/HotSpot/JNI product, **not an M3 layer over a stock JDK**. This candidate extends the existing target-owned `com.m3.file.M3File` NIO module by deriving compact file-change facts without retaining text or class payloads and without creating a compiler/AST. It is a bounded source-level admission, not proof of java.base/jdk.compiler or fully linked SDK replacement.

## Actual source contract
- `M3FileDeltaLedger.scan(root,previous,tracked)` observes the entire explicitly supplied tracked set, including source, `.class` and POM/DSL/build-input resources. Omitted prior paths are deletions. Every observed regular file is streamed and exact SHA-256 checked; size and timestamp never alone prove equality.
- One canonical parent-owned set of parallel `String[]`, `byte[]`, `long[]` and indexed `int[]` columns records exact file paths, kind, byte lengths, SHA-256, physical slots and bit-masked status. No duplicate per-row Entry, Cell or Node storage. Existing slots survive edits and deletion/recreation; caller-facing query results are detached.
- `LIVE|CREATED|MODIFIED|REMOVED` statuses are per-generation. A removed path is a tombstone; repeated absence does not re-emit a deletion.
- `save/load` uses versioned `M3DL` image, SHA-256 stream seal, checked absolute root, original path identity, private temporary file and required atomic rename. No silent non-atomic publication. Snapshot load refuses corruption or foreign roots.
- Path traversal and symlink inputs are refused. Concurrent writers without filesystem snapshots can still evade before/after metadata checks in an adversarial ABA replacement; do not claim atomic multi-file snapshots or cross-host power-loss durability.

This ledger only detects **raw byte changes**. It does **not** decide ABI equivalence, dependency closure, annotation-processor impact, javac phase reuse, Maven POM effective-model equivalence or Gradle configuration-cache correctness. Consumers must conservatively rebuild until those compiler-verified facts are wired in. It does not claim re-reading hash bytes has been eliminated.

## Canonical recipe and generated source
- [Synexia canonical recipe draft #12565](https://github.com/hsoliwal/com.synexia/pull/12565), `com.synexia.m3.M3SourceClassDelta20261020`.
- Reuses `M3HashPinnedJavaSnapshotRecipe`, no new transformation engine or runtime dependency.
- Final postimage SHA-256: `M3FileDeltaLedger.java` `4e41f9815edbff0292b11f47f8cc957b76766e515c16bca77cfc5572c08c9c21`; probe `68f1b93e884230a03f20436ff7240eeb5a17bfa227df16c179fb248ed676b59f`.
- The source templates and receiving Git blobs were compared exactly. Future changes must update the canonical recipe, rerun tests and replay serial per file rather than editing the generated postimages as an unsealed independent implementation.

## Java 21 contract check
From the repository root:

```sh
mkdir -p build/m3file-delta-classes
javac --release 21 -Xlint:all,-module -Werror \
  -d build/m3file-delta-classes \
  m3/file/src/main/java/module-info.java \
  m3/file/src/main/java/com/m3/file/M3File.java \
  m3/file/src/main/java/com/m3/file/M3FileDeltaLedger.java \
  m3/file/src/test/java/com/m3/file/M3FileDeltaLedgerContract.java
java --module-path build/m3file-delta-classes \
  -m com.m3.file/com.m3.file.M3FileDeltaLedgerContract
```

Local Java21 `javac` and named-module runtime: **M3_FILE_DELTA_PASS checks=27**. The `.class` fixture deliberately contains arbitrary bytes to prove opaque binary-byte hashing, *not* classfile ABI parsing. The GitHub workflow executes this exact focused path; separate Maven/OpenRewrite and whole-JDK gates remain pending.

## Convergence and remaining work
Reuse [scoped original javac #1559](https://github.com/hsoliwal/M3JDK21/pull/1559), [M3File full-family proposal #1316](https://github.com/hsoliwal/M3JDK21/pull/1316), and filtered/sorted [#1554](https://github.com/hsoliwal/M3JDK21/pull/1554), [#1549](https://github.com/hsoliwal/M3JDK21/pull/1549), [#1551](https://github.com/hsoliwal/M3JDK21/pull/1551). Do not replace their owner state or diverge contracts. Exact ABI reverse graph, compiler phase identity, real Maven/Gradle task and cache workflows, checked JNI/OS parity, full source-built JDK/jtreg, and CPU/heap/native measurements remain **NOT_EXECUTED** for this PR.

See `CATALOG.tsv` here for per-category decisions. Original JDK and provider code keeps its original licensing; first-party derived fact/recipe code Apache-2.0 and preserved attribution. No LeetCode/HackerRank/GeeksforGeeks challenge source copied.
