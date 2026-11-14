<!-- SPDX-License-Identifier: Apache-2.0 -->
# Scalar LongInterval M3JDK21 receiving candidate — 2026-10-29

**Draft/source candidate, not an installed JDK qualification or completed OpenRewrite replay.**

## Product contract
M3JDK21 is one integrated **one-way superset** of JDK 21. Every ordinary
Java 21 program must retain the applicable JDK source, binary, API and
observable behavior contract on M3JDK21. Stock OpenJDK does not need to run
M3-specific classes. This patch changes no `java.*` class or JNI/HotSpot
symbol and preserves the existing `com.m3.collections.M3LongInterval`
constructor, methods, and `scaffoldDescriptor()`.

## Source identity
- Public base revision when branched: `6821ed995423bba34b66fbcc881f1b224c675427`.
- Original scaffold Git blob: `e605d7e12a12bd07d6db207dcbb8fb989c2630bc`.
- Original scaffold SHA-256: `8d669c2caeece09de656bb4968de90b941aeb78260364d92b9ab492fca8f6acd`.
- Candidate Java Git blob: `34a490c518ffbc8a5b85f74e177c89803b09aaac`.
- Candidate Java SHA-256: `9f82d3725c3409c51500b02b7c1642d45f1b9a4c16d0ddac3b8ff8f83191ddb3`.
- Candidate JUnit Git blob: `eb2f409782ae12790e7227380986362042a1478e`.
- Candidate JUnit SHA-256: `ff5b4e267e03e16c592fff476eaa731cb78b8f575face8e62ddd098b4e2b2f04`.
- All candidate blobs were read back byte-identical to the Synexia template.
- Canonical private recipe: `hsoliwal/com.synexia`,
  named `com.synexia.m3.M3LongIntervalScalar20261029`, using the
  existing `M3HashPinnedJavaSnapshotRecipe` and an exact pre/post manifest.
  The actual Maven/OpenRewrite replay test is **NOT EXECUTED**.
- No private recipe Java/YAML, template engine or improvement memory is
  present in this public repository.

## Algorithm and donor
- Reference: Eclipse Collections `LongInterval` source blob
  `32f1a4e20afe9afebc4337005764d7727bc4d299`, distributed under
  EPL-1.0/EDL-1.0. Concept/behavior reference **only**; no Eclipse implementation
  code or incompatible license is copied into this Apache-2.0 product file.
- Immutable scalar interval: signed `long` from/to/step and `int` count.
  No retained element arrays, entry objects, boxed longs or JNI dependency.
- Overflow-safe unsigned-distance counting and membership;
  `Long.MIN_VALUE` steps handled including reverse projections; reject
  >`Integer.MAX_VALUE` positions under the unchanged `int size()` contract.
- `toArray` is explicit detached materialization; no allocation-free claim
  for that API. Construction/rank/contains remain constant-memory.
- Case taxonomy: arithmetic progression, signed/unsigned boundary algebra,
  JDK-style bounds/callbacks, immutable source facts and zero-materialization
  reverse; no LeetCode/HackerRank solution source copied.

## Evidence and remaining gates
- **PASS:** locally `javac 21.0.12.1 --release 21 -Xlint:all`.
- **PASS:** independent `ArrayList<Long>` / `Math.addExact` reference, fixed
  `Random` seed, 30,000 randomized ranges, 2,515,305 assertions,
  including signed extrema and oversized range refusals.
- **PASS:** all five new Java/JUnit test methods compiled under Java 21
  `-Xlint:all,-serial -Werror` and executed against *local throwing JUnit
  assertion stubs*; **not real JUnit Jupiter/Maven Surefire**.
- **PENDING:** actual private OpenRewrite Maven/JUnit tests and fixed point;
  complete public collections Maven/JUnit plus source module graph; product
  JDK image/jtreg, platform, GC/JNI and CPU/heap benchmark gates.
- **HOLD** release/merge pending full qualification. A green source-only
  test does not establish Java SE or M3JDK21 runtime compatibility.
