<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 native Java / HotSpot / JNI / Maven-build-fact superset

M3JDK21 is **one integrated Java21/HotSpot/JNI SDK product**, never an
"M3 + JDK" or a mandatory com.synexia runtime. This stacked review merges
existing product-owned behavior, not wrapper abstractions.

## Original per-file source and immutable ancestry

The product starts at exact original [superset PR #1624](https://github.com/hsoliwal/M3jdk21/pull/1624)
head `d7b9e65241b717a387b25234df4cf21531f07d9f`.

- **Merge 1** `116d735ffbd25a423821599481806c2460752bc3`
  has two parents: previous product head, then original native build-state
  donor #1587 `a16356d918f2ec630e1a2c54f59518222f105333`.
  Adds exactly seven source/evidence files, with no previous source touched.
- **Merge 2** `cd54c4e62f9de664ad2c1458edbad95bed5b181a`
  has two parents: merge 1, then original JDK/HotSpot text-I/O donor #1629
  `5d2bbd7401053eba2f64efe0a71dfb367ce357e6`.
  Applies ten **identical original donor Git blob IDs**. All seven modified
  original core JDK/HotSpot paths were byte-identical to the donor's master
  preimage on the preceding recipient.

The structural Git compare against the original product head reports
**17 changed paths** (ten Java/HotSpot text-I/O and seven native build-state),
**zero removals**. This composition is mechanical; it does not synthesize
source, rename public Java contracts, or alter native JNI exported names.

Exact path/blob/mode/parent source recipe:
[RECEIVING.tsv](RECEIVING.tsv).
Local Git DAG and source-receipt verifier:
[verify_superset.py](verify_superset.py).
Maven verify entry:
[pom.xml](pom.xml). The original recipes remain canonical in Synexia and
their product source/test files remain installed in M3JDK21.

## What is now in the stack

- Existing PR #1624: parent-owned dynamic arrays, offsets, object-state bit
  masks, primitive stable filtered/sorted selection, DOOP M3FileTree,
  original javac/M3AST occurrence lanes, MOP recipe-memory evidence,
  existing JNI bit-mask owners.
- Donor #1587: immutable, packed typed file/class/build-input identities;
  exact content SHA-256 and conditional javac-proven ABI facts; CSR dependency
  indexes and bit masks; conservative reverse impact closure; durable build
  images in **existing M3FS SQLite/JNI**. Maven/Gradle task suppression is
  **not** implemented just because a delta is available.
- Donor #1629: canonical M3String CodeUnitCursor reused inside original
  `java.io.Reader`, `StringReader`, `StringCharBuffer`, internal access
  bridge, and HotSpot `jni.cpp` String copy path. Existing public reader,
  buffer, writer, JNI pin/release and error contracts are preserved.

## Actual proof and limits

Original donor #1587 [GitHub Actions run 39395945381](https://github.com/hsoliwal/M3jdk21/actions/runs/39395945381):
**completed successfully**. Its original M3FS native and Java owner ran
three `M3BuildStateTest` JUnit methods, zero failures/errors/skips, and
**12/12 CTest** native suites passed with JNI checks.

Original donor #1629 source-built separate pristine and candidate Linux
x86-64 server JDK images, ran 12 focused jtreg tests, measured M3 composite
String I/O benchmarks, and checked the actual original public reflection
and 239 JNI entry rows. These are original-donor proof, **not a new
combined-branch JDK image qualification**.

**Not yet proved on this combined head:** complete full-source M3JDK21
images and jtreg; both existing Maven/OpenRewrite recipe qualification chains
executed together; every collection/API/JNI native platform; Windows, macOS,
Android, ARM; combined cold/warm CPU, memory, allocation, JNI and throughput;
original Maven/Gradle/Flink/NetBeans/IDE analyzers sharing persistent facts.
A successful Git merge is not proof of those behaviors.

The held [#1630 filtered copy-on-write and sorted endpoint repair](https://github.com/hsoliwal/M3jdk21/pull/1630)
explicitly changes observable exceptions and declared-reflection surface.
It **must not** be labeled a contract-preserving automatic donor merge.

The [#1608 file/AST/stream continuation](https://github.com/hsoliwal/M3jdk21/pull/1608)
overlaps original indexed file/parser owners. It needs per-file semantic
reconciliation and matching compiler event/ABI/cancellation contracts before
admission. No duplicate AST, second filesystem, competing metadata database
or stream owner is installed by this PR.

**Draft: do not merge** until original combined-branch Maven/Java/JNI/native
and recipe closure gates pass, with hard evidence stored in product Git.
