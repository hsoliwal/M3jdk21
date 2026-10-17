<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3JDK21 long-range indexed order statistics receiver

**Canonical source-first recipe and tests:** [hsoliwal/com.synexia PR #12169](https://github.com/hsoliwal/com.synexia/pull/12169),
commit `656ac36484e12ef868ff7e11e3d340d94cfe2c98`,
named recipe `com.synexia.m3.Jdk21LongRangeOrderStats20261017`.
**Stacked product parent:** M3JDK21 [PR #1165](https://github.com/hsoliwal/M3jdk21/pull/1165)
commit `b3ef065b14343fd1aeb3370fa09b0100ead260ab`.
This is NOT a standalone master replay: preimage
`m3/collections/src/main/java/com/m3/collections/M3LongRangeSet.java`
Git blob `5dbc2ec9317c79d801bb4c312f2c0b7ffad6ffbd`,
SHA256 `f865b006888f1d4554b51cf69e7f4d6bac67458982f957f9151af0b4f09eaec8`.
Postimage Git blob `03b6d9fad25ce9bdb920ecc7b5e8f975a4638e11`,
SHA256 `17c6db8bf82e5d908a551fe53850a1d4ca00583fa6783bd093af48598038b0c6`.
Two newly generated test files are ABSENT-before and byte-identical
to Synexia templates:
`M3LongRangeAlgebraContract.java` SHA256
`da109872c25cf9a9a5af6ee38417c4cf5c7d5fcf7444418e499d03d908ecb6fc`,
`M3LongRangeAlgebraTest.java` SHA256
`fb7de589799041359d6a973496e6f7c87ea87d7bc732e51acebd451c6f2af99f`.

## Contract and mechanism

Canonical M3 collection is still two aligned dynamic primitive `long[]`
start/end lanes; no per-range `Entry`, `Node` or boxed cumulative
objects. A single lazy `long[]` stores unsigned cumulative counts,
invalidated by add/remove/clear and safely cloned for detached snapshots.
Exact new rank/select works even for `[Long.MIN_VALUE, Long.MAX_VALUE)`
where cardinality is 2^64-1. Logical ranks are compared using
`Long.compareUnsigned` and returned at an explicit BigInteger API
boundary. Frozen sorted owners support union, intersection, difference,
symmetric difference and bounded complement with two-pointer
linear-time scans. No existing method signature or public JDK
contract is changed. Results are detached independent range unions.

Existing `M3IndexPathMasks.select` still owns 32-bit required/excluded
mask selection and the existing `m3_index_path_masks.c` JNI symbol.
No new native state, object pointer or callback crosses JNI; this
receiver does not add a second mask engine.

## Local proof vs exact-head acceptance

- **Executed on a finite selected-source closure:** Java21
  `javac --release 21 -proc:none -Xlint:all -Werror` PASS.
- Existing range differential oracle **411,913** assertions PASS.
- New deterministic Java `BitSet` differential oracle **1,324,090**
  assertions, 900 seeded two-owner trials, signed extremes and full
  unsigned ordinals: PASS.
- **Limitation:** isolated local test used a **test-only**
  `M3IndexPathMasks` stand-in; no local proof with real M3 JNI.
- Target exact-head Actions is configured to compile the *actual*
  existing M3 Java selector, run all three selected-source oracles,
  compile the existing C17 JNI library, execute real `-Xcheck:jni`
  parity via `M3IndexPathMasksSelfTest --native`, and run owning
  Maven/JUnit tests. An authored workflow is not a passing run.
- Canonical Synexia OpenRewrite class-backed fixed-point/refusal,
  full Maven/JUnit, native portability, installed JDK image,
  jtreg/JCK/GC/JMM and CPU/heap measurement remain **PENDING** until
  actual checked receipts.
- API compatibility with Guava/Eclipse/Apache/Spring and full
  JDK replacement is explicitly unqualified. Retain upstream
  notices/license. No rebase, force push or modification of master.

The new category catalogue is an exact canonical Synexia resource
and indexes LeetCode/HackerRank/GFG source problems as taxonomy only,
without importing editorial or solution code. Promote every FILE
only after the recipe and behavior gates are green.
