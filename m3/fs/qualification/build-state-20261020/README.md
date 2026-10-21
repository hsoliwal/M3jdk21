<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21-native persistent build facts — receiving candidate

**M3JDK21 is the product**, not a layer around another JDK. This is an
additive SDK feature in the existing native-SQLite-backed jdk.m3fs module.
It reuses the existing Java/JNI M3FS owner, and adds no competing database,
compiler parser, Maven/Gradle wrapper, native library or changed public JDK API.

## Ownership and semantics

- Existing M3File and M3FileDelta are file and class byte-fact producers.
- M3AST/javac must be the source of class ABI and dependency evidence.
- M3BuildState retains immutable sorted path/role arrays, lengths, packed
  SHA-256 content and optional ABI lanes, long-word known-ABI masks and
  int-array CSR dependency offsets/indexes.
- M3BuildStateStore persists one checksum-sealed state image in the
  pre-existing M3FS SQLite/WAL native namespace. Publication uses the
  existing atomic M3FS rename, a cooperating OS writer lock, and a
  compare-and-swap expected content commitment. Readers may use the
  existing pinned M3FS.openSnapshot view.
- Changed state produces a candidate reverse-dependency invalidation
  mask. It does not execute, cache or skip a Maven/Gradle task.
- Full input inventory and context hashes are caller obligations:
  toolchain, flags, sources, classpath, POMs, profiles, plugin inputs,
  processors and generated outputs. Unknown ABI means conservative
  propagation. Removed identities or changed context force full rebuild.

Each relevant operation remains a replaceable contract-preserving
behavioral atom. Sorted typed identities give deterministic binary search;
reverse CSR + bitsets give O(V+E) graph traversal with no retained
per-row graph nodes. Direct byte modifications remain dirty even when
compiler-proven stable ABI may suppress downstream invalidation.

## Executed and not-executed gates

Executed on OpenJDK 21.0.12.1:
- javac --release 21 -Xlint:all,-module -Werror on two product
  classes, standalone contract and test source using **local compilation
  shims** for M3FS and JUnit: PASS.
- Standalone Java21 graph/codec proof: **15,407 checks PASS**,
  including 180 differential graph fixtures against independent reachability.
- A regression for simultaneously dirty ABI-stable middle nodes exposed
  incorrect worklist suppression on the first candidate. Corrected and
  rerun PASS.
- Java source blobs independently matched their canonical recipe template
  source IDs.

NOT EXECUTED: real Maven/JUnit, native SQLite/JNI, complete JDK image,
HotSpot, jtreg, Windows/Android native ABI, gradle/maven fork behavior,
OpenRewrite SDK Recipe.run and fixed-point receive, warm/cold benchmarks.
No claims of native runtime acceptance or speedup are made.

## Existing reusable canonical recipe

Canonical source home: hsoliwal/com.synexia, in the existing
synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017
using its original hash-pinned Maven/OpenRewrite Java LST recipe engine.
Dataset: m3fs-build-record-20261020. This is candidate template and
manifest custody, not an executed generated SDK Result. Full recipe
execution and exact receive replay are pending. Do not hand-edit generated
receivers to bypass that gate.

Required promotion: original OpenRewrite Maven SDK tests and refusal/fixed
point, real m3/fs native tests with -Xcheck:jni, complete JDK
image and ABI/jtreg gates, Maven and Gradle no-op/body-only/ABI/source
deletion/annotation-processing/parent-POM/profile differential tests and
cold/warm resource benchmarks.

See CATALOGUE.tsv for candidate algorithms, evidence and donor rationale.
No LeetCode/HackerRank/GeeksforGeeks solution code is copied.
