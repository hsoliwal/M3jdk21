<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- Modified 2026 by Hitesh Soliwal and contributors: restore exact collection receiving provenance and verification while retaining current owners and String phase authority. -->
# M3 collection lanes for Java 21

This separately built Apache-2.0 library adapts Synexia's indexed collection owners to
`com.m3.collections`. It has no runtime dependency beyond `java.base`. It lives alongside
the existing M3 text modules and does not modify `java.base` or replace JDK collections.
OpenJDK retains its existing license. See `LICENSE`, `NOTICE` and `provenance.json`.

## Storage and API

`M3Collections` constructs sparse int/long/bit lanes, stable row stores and linked lists,
append arenas, and CAS int/long lanes. A 28-bit address uses 8 region bits, 8 block bits and
12 position bits. Storage grows by publishing/adding pages rather than moving old pages.
Primitive values, topology indexes, presence flags and generation values need no per-row
entry object. Array/directory/page allocations still exist.

```java
import com.m3.collections.M3Collections;

var counters = M3Collections.concurrentLongLane();
counters.prepare(0, 8192);
counters.getAndAdd(4096, 1);
long total = counters.parallelStream(0, 8192).sum();
```

| Owner | Behavior |
| --- | --- |
| M3IntLane28 / M3LongLane28 | Sparse zeros, checked page fill/import/export, overlap-safe range copying; single-threaded. |
| M3BitLane28 | Set-bit traversal, total/range cardinality, exclusive rank and zero-based select; optional explicit native range count. |
| M3SlotAllocator28 / M3LongLaneStore28 | Stable physical slots, generation-tagged handles, live bitmap iteration, caller-array lane export. |
| M3LongLinkedList28 / M3LongArena28 | Existing index-linked order and append/range ownership contracts; no concurrent mutation guarantee. |
| M3ConcurrentIntLane28 / M3ConcurrentLongLane28 | CAS-published stable pages, atomic scalar acquire/release/CAS/exchange/add; fixed-range primitive streams. |
| M3ConcurrentLongMap / M3ConcurrentLongSet | Existing immutable snapshot arrays; direct primitive stream splitting. Changed writes still copy arrays and may retry CAS. |
| M3ConcurrentBitSet | Fixed capacity, atomic word changes, weakly consistent set-bit streams split at word boundaries. |
| M3LongCollection | Primitive stream defaults using the owner's iterator; fallback cannot split. |

CAS lane pages never move or disappear. Prepared scalar operations contain no explicit locks,
blocking waits or Java retry loop; JVM atomics, scheduling and allocation/GC still constrain
progress. First writes can allocate losing publication candidates. Zero release writes and
zero-to-zero CAS retain real memory-effect locations. A collection must still supply its own
multi-index consistency, topology, lifetime and ABA protection. Single-threaded owners do
not become concurrent by sharing this package.

Streams avoid element boxing and payload flattening; pipeline/split objects allocate. CAS
range streams include all addresses, including zeros, with weakly consistent values. Map/set
streams capture immutable snapshots. Bitmap streams are weakly consistent and not SIZED.
Parallel mode is available, not a promise that every workload is faster.

## Representation and regression checklist

This library's primitive lanes are not interchangeable with the reference-bearing
backends of `java.util`. The [JDK collection replacement specification](../docs/collections-replacement-spec.md)
is a design/acceptance document; its proposed changes are not installed by building
this module. Keep mutable owner identity, topology, presence and slot generations
separate from primitive values. A zero can be a valid value; absence needs the owner's
specified representation. Single-threaded, atomic-scalar and snapshot owners have
different consistency contracts.

When documenting or porting an owner, trace its factory, concrete class, tests and
receiver receipt together. Check boundary addresses and overflow, sparse zeros,
range copying overlap, stale/reused handles, iteration order, mutation visibility,
stream splitting and Java/native parity where applicable. For reference payloads,
retain GC-traced Java references or a separately qualified rooted-handle protocol;
primitive IDs alone are not object lifetime management.

Prefer the current receiver qualification below over the historical installer.
Fixed-point/refusal proofs protect source delivery; contract tests protect API
behavior; JNI/UBSan checks protect a bounded native path. None alone proves lock-free
whole-collection progress, a universal speedup or promotion of the JDK backend.

## Historical lane build, recipe and verification

From the repository root, with Java 21 and Maven:

```sh
mvn -f m3/collections/pom.xml test
```

For the complete focused Java/recipe/JNI gate, Linux, Python 3 and GCC/UBSan are also required.
Set `JAVA_HOME` and `JUNIT_CONSOLE_JAR` to real installed Java 21 and Console Standalone 1.13.4
paths. `MVN`, `M2_REPO`, `CC`, and `M3_MAVEN_OFFLINE=1` are optional overrides.

```sh
python3 m3/collections/verify.py
```

This executes real OpenRewrite 8.17.1 recipes, verifies fixed-point/refusal and all 21 sealed
Java postimages, clean-compiles the named module, creates `target/m3-collections.jar`, runs
18 contract tests, compiles JNI with warnings as errors and UBSan, compares Java/native
counts under checked JNI, and runs prepared allocation measurements. No JDK replacement,
OpenJDK build, jtreg completion or whole-repository validation is implied.

The native method is optional; the host must explicitly `System.load` the absolute compiled
library path before `M3BitLane28.cardinalityNative(from, to)`. Default operations stay in
Java. Native code uses bounded JNI copies and retains no heap address between calls.

Production changes belong in the sealed recipe crate under
`m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-collection-lanes`.
Recompute postimage hashes, then rerun the actual recipe and tests. `verify.py --apply`
materializes only admitted, verified outputs after a final source-hash recheck. This initial
crate admits ABSENT or identical source; it refuses drift. A later change needs a new
reviewed before/after crate rather than deleting existing files to force admission.

Named recipes: `com.synexia.rewrite.M3CollectionLanes` and
`com.synexia.rewrite.M3SegmentedLaneNative`. The recipe infrastructure retains its original
Synexia package and notices; it is tooling, not part of the runtime module. Java replacement
uses parsed OpenRewrite LSTs. The Python verifier only orchestrates and copies tested output.

The port maps package/type names and adds equivalent explicit no-argument constructors for
exported classes. The module uses the repository's M3 naming convention; `-Xlint:-module`
excludes only module-name style diagnostics about its terminal digit. All other Java lint
warnings are errors. `evidence/STATUS.md` records exact proof scope and remaining gates.

Source implementation: [Synexia PR #7705](https://github.com/hsoliwal/com.synexia/pull/7705),
commit `1d0deb92f379cc05a001ac48e0e427b0db6c263f`.

## Current separately built receiver qualification (2026-10-08)

The lane installer described above is a frozen 21-file historical recipe. Run its Maven
SDK fixture independently; do not run its installer against the expanded live factory.
Current product verification uses the exact 88-Java installed manifest and preserves
current primitive, identity and enum owners in the existing com.m3.collections package.

```sh
mvn -f m3/collections/recipe/pom.xml verify
JUNIT_CONSOLE_JAR=/path/to/junit-platform-console-standalone-1.12.2.jar \
  python3 m3/collections/verify_donation.py --cost-probe
```

Java 21, Linux and GCC/UBSan are required by the complete product verifier. It runs
113 contract tests and both lazy self-tests, packages notices, checks java.base-only
runtime dependencies, and measures JNI parity and prepared allocation. Diagnostic cost
samples do not establish retained heap or a universal performance advantage.
Canonical recipes and actual source/runtime qualification stay in Synexia's
`m3jdk-packed-collections-receiving-repair-20261008` packet; this target is a receiver.
See `synexia-donation.json`, `name-mapping.json` and `qualification/receiving-repair-20261008.json`.
The global migration remains in STRING. No array/collection/AST phase or JDK backend
is promoted by this independent module proof. Historical records retain their own hashes.

## CPU tool: mask-selected tumbling-window statistics (2026-10-18)

The independent `M3CpuMaskWindowStats` combines the existing
`M3IndexPathMasks` Java/JNI selector with a bounded primitive reduction
over at most 4096 selected occurrences per window. No per-row object,
String payload, duplicate native library or large selected-array copy is
created. It emits exact count, signed ID sum, minimum and maximum to a
callback; duplicate row IDs and negative-stride encounter order survive.
The selector remains the single C17/JNI owner.

Source, caller contract, array/mask geometry, Synexia Maven/OpenRewrite
recipe and exact SHA-256 manifests are documented at
`qualification/CPU_MASK_WINDOW_STATS_20261018.md`.
This is an independent receiver candidate, not migration of java.util
or completion of the global STRING prerequisite.
