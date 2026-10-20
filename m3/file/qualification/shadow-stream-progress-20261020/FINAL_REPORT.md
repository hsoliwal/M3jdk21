<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21: Native shadow streams and subprogress in M3File

Status: **DRAFT / PARTIAL**. One integrated M3JDK21 SDK product; Synexia owns authoring recipes, not the runtime. This target changes the existing `com.m3.file` module, preserving the original NIO `Path` and NOFOLLOW_LINKS owner, and does **not** create a new filesystem service, parser, full-file object graph, JNI symbol or duplicate canonical String store.

## Original owner and exact source
- Canonical Synexia Maven/OpenRewrite PR: [hsoliwal/com.synexia#12598](https://github.com/hsoliwal/com.synexia/pull/12598), recipe `com.synexia.m3.M3NativeShadowStreams20261020` (existing `M3HashPinnedJavaSnapshotRecipe`).
- Baseline M3JDK21 master at `065e3626aac2de62d2f531aec3bed2fca533c9f9`; original `M3File.java` Git blob `c182a3b1526f9fd0ca20c631d97df3048076defd` / SHA-256 `22b6c0e7f2a136b2d43e765ec4a0669c2bc564a7d81f0b31f4f0a13f184e36e4`.
- Generated M3File postimage SHA-256 `93864fe41040396ffa40775ed7489f8e97ae6ee058538294e0626d046b12915c`; progress `e7258d79c6dcfb65c2598ae1480f5d7078a31fde921c4842ea8ab4209754362d`; shadow `b934019aa4a89835d54a991090379abe3c22ca6487e58b3568389b3cabb85afa`; oracle `cc3d5ab685c44ed01436404e782adcbfcb85aae2ab088df11449a0a0b116140a`.

## Implemented behavioral atoms
- `M3StreamProgress`: functional `worked(long)` plus default `cancelled`, `checkCancelled` and bounded `subProgress`. Parent cancellation and quotas propagate.
- `M3ShadowStreams.SharedInput`: one source channel, demand-read shared 8192-byte pages, parent-owned dynamic byte[] backing, independent physical byte-offset cursors, bounded retention and one-source read. A shadow reader may drive source read-ahead but cannot move another cursor's logical position. `mark/reset` replay without reopening. Each shadow can use a child progress monitor.
- `M3ShadowStreams.output`: primary and optional output shadow; only confirmed joint writes report completed work. Failure poisons further writes. Output streams are not a cross-stream atomic transaction.
- `M3File`: additive monitored-input and bounded-shadow-input overloads from the original `openInputStream()`, which is unchanged. No directory traversal, filesystem graph, or metadata-only equality assumptions.
- No per-file retained Entry/Node/Cell. The bounded page arena retains bytes only for an active operation; close releases its references. This is not a persistent copy of the entire filesystem.

## Executed local validation
OpenJDK 21.0.12.1 on Linux. Actual named module `com.m3.file`, source and test from exact canonical templates. Command:

```bash
javac --release 21 -proc:none -Xlint:all,-module -Werror \
  -d build/m3-file-shadows \
  m3/file/src/main/java/module-info.java \
  m3/file/src/main/java/com/m3/file/M3File.java \
  m3/file/src/main/java/com/m3/file/M3StreamProgress.java \
  m3/file/src/main/java/com/m3/file/M3ShadowStreams.java \
  m3/file/src/test/java/com/m3/file/M3ShadowStreamsContract.java
java --module-path build/m3-file-shadows -m com.m3.file/com.m3.file.M3ShadowStreamsContract
java -Xint --module-path build/m3-file-shadows -m com.m3.file/com.m3.file.M3ShadowStreamsContract
```

Both runs: `M3_SHADOW_STREAM_PASS checks=43`. Source replays from one primary input, two layered parser cursors, actual provider-backed `M3File`, nested progress quotas, cancellation, zero and finite budget, output mirroring and known nonatomic shadow-error paths.

## Outstanding gates and overlap avoidance
- Canonical Maven/OpenRewrite actual recipe run, source-drift refusal and fixed-point is **NOT_EXECUTED** locally; GitHub Actions workflow will attempt it, but hosted success must be checked separately.
- Full source-built M3JDK21/HotSpot, JTReg/JCK, installed JMOD, GC/JNI/process-memory, multithreaded stress, Windows/macOS/POSIX provider handling and representative CPU/heap/throughput are **NOT_EXECUTED**.
- This is Java source only. No new JNI work is justified without comparative profiling against existing JNI bulk selectors.
- Original javac/M3AST remains compiler truth. Current parser `M3ASTIndex.Domain` lacks JAVAC and the original `M3JavacTree` projection is separate. Do not introduce another parser; separately implement `M3ASTIngress.javac` over the authoritative `CompilationUnitTree` before using these shadows in static analyzers.
- Reuse [M3JDK21 #1567](https://github.com/hsoliwal/M3JDK21/pull/1567) and [#1568](https://github.com/hsoliwal/M3JDK21/pull/1568) for file deltas and fingerprints. Metadata (mtime, length, provider change attrs) can prefilter, but cannot prove unchanged bytes absent an authoritative stable provider generation. Existing Maven/Gradle/filtered/sorted owners are not modified here.

First-party code and recipes retain Apache-2.0 / Copyright 2026 Hitesh Soliwal and contributors. LeetCode/HackerRank/GeeksforGeeks are problem-category references only; no challenge or incompatible-license donor code was copied.
