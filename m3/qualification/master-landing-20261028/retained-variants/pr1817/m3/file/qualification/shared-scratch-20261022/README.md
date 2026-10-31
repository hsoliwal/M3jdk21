# M3JDK21 — Original M3File scratch reuse for native M3FS project inventory

Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0.

M3JDK21 remains the **integrated Java21/HotSpot/JNI SDK product**, never
a separate M3+JDK wrapper. The original M3File observation method,
M3HostFileInventory coordinator, V2 packed arrays/indexes/state masks,
native M3FS SQLite WAL and successful-build admission remain the
authoritative implementation owners.

This stacked draft builds on M3JDK21 #1797 (original JNI mask,
array compaction and filtered/sorted comparator donors), #1782
(original one-stream M3File -> V2 inventory), and #1758
(native project-SQLite observed-versus-successful state).

## Use cases and Synexia donor evidence

The existing private Synexia `synexia-fileindex` NIO streaming hash,
lock-free fanout and WAL/commit protocol docs favor a bounded
reused byte scratch, consumption-driven chunk callbacks and a
publication fence. `synexia-build-inventory` preserves exact
SHA256 and deterministic categorized/structural candidate analysis.
The M3FS precompute catalog already owns mapped immutable primitive
search and index images. These are reviewed **donors for algorithm
choices**, not additional runtime content stores or new JNI APIs.

### Implemented scoped behavioral atoms

1. Existing `M3File.java`: additive
   `observe(long, byte[], M3StreamProgress, ByteObserver)` uses a
   caller-borrowed scratch of 1..1,048,576 bytes. It checks the
   same fileKey, size/mtime stability, full EOF, SHA-256 and
   cancellation as the prior 8KiB allocating overload. Existing
   methods are unchanged. Byte observers are synchronous and
   must never mutate/retain the caller's scratch.
2. Existing `M3HostFileInventory.java`: allocate one 64 KiB
   scratch for the **entire serial project scan**, then reuse
   for each `M3File` admission. No per-file array allocation
   for scratch; no independent hash pass or tree traversal.
3. New `M3FileSharedScratchContract.java`: 256 randomized
   file fixtures including empty, 64KiB boundary and >128KiB,
   SHA-256 equivalence to original API, same callback bytes
   and scratch identity, exact progress/chunk counts, budget
   refusal and invalid buffer rejection.

**This changes per-file scratch allocation traffic, not file
payload ownership or JVM object lifetime.** For 1 million
files, replacing 8KiB scratch/file with one 64KiB scratch removes
approximately 8 GiB of aggregate Java array allocation traffic,
but allocation and performance must be measured on the real
M3JDK21 reactor; this estimate is not a benchmark.

### Durable and semantic invariants

No change to original M3FS native SQLite observation CAS,
`/m3-build/state` successful-build semantics, JNI exports,
M3AST compiler-proven ABI, `M3FileDeltaLedger`, filtered/sorted
collection backing, source metadata or provider symlink policy.

The user may select a filesystem root or native project source
generation, but the inventory caller remains responsible for
complete enumeration and stable input admission. SHA-only
evidence does not authorize skipping Maven or Gradle plugins.

### Source custody, proof and acceptance

Canonical [Synexia #12979](https://github.com/hsoliwal/com.synexia/pull/12979)
recipe `com.synexia.m3.M3FileSharedScratch20261022` contains
exact before/after SHA256, test fixture, Maven/OpenRewrite
fixed-point and drift-refusal test definitions, donor/category
catalogue and MOP correction memory. The two production sources
and one test source were received serially and checked by exact
GitHub template postimages.

**Actual Maven/OpenRewrite, M3File/M3FS Java21/JUnit,
native SQLite/JNI, full HotSpot/JMOD/jtreg and OS-provider
performance gates are NOT RUN.** The PR remains a DRAFT, with
no claimed full SDK qualification or blanket speedup.
