<!-- SPDX-License-Identifier: Apache-2.0 -->
# LongArrayDeque primitive ring span refinement — exact Java 21 receiver

Owner: `src/jdk.m3.collections/share/classes/jdk/m3/collections/LongArrayDeque.java`
in the integrated `jdk.m3.collections` module, not an external wrapper.
Source baseline: `master@ec67062ba55293d9175757ba39d97112d3cdcf8b`.
Original owner remains Apache-2.0 with its retained attribution.

## Replaceable behavioral atoms

The original `removeLogicalIndex` performed one indexed read/write per
logical slot when shifting either end of a wrapped ring. The reviewed
postimage uses 1–3 `System.arraycopy` spans over the **same owned
`long[]`**, preserving the shorter-direction selection, head, size,
bitmask indexing, zeroing of freed slots, fail-fast epoch, and every
physical slot, including unused capacity. No temporary arrays, nodes,
entry facades, side objects, or JNI.

`clear` uses up to two `Arrays.fill` spans for a wrapped ring, rather
than a per-slot logical-to-physical calculation. Empty clear remains a
zero-change no-op; nonempty clear increments the original epoch once.

## Source-pinned serial review

This receipt commit precedes modification of the production owner. Exact
Git blob preimage/postimage identities and SHA-256 are in `SOURCE_SEALS.tsv`.
The original Java owner is retained byte-for-byte as `preimages/`; the
reviewed full Java owner and its target-owned JUnit test are retained as
`templates/`. No unrelated file, public descriptor, provider contract,
module-info or other collection fork is changed.

Prior Swift Collections segment-copy concepts are referenced only as a
layout pattern; no donor source is copied. LeetCode/HackerRank/GFG
categories are circular-buffer permutation, in-place shift, and
two-span copy, not executable dependencies.

## Executed local proof

`LongDequeRingSpanOracle.java` is an independent self-contained Java21
physical-ring differential oracle. It compiled with:
```
javac --release 21 -Xlint:all -Werror LongDequeRingSpanOracle.java
java -ea LongDequeRingSpanOracle
java -ea -Xint LongDequeRingSpanOracle --quick
```
Results:
- **6,688,180 JIT physical-state assertions passed** over exhaustive
  powers-of-two capacity/head/size/index geometries and randomized
  multi-operation sequences.
- **104,924 interpreter-mode assertions passed** across every small
  power-of-two ring layout and additional seeded random cases.
- Checked entire owned `long[]`, head, logical size and modification
  epoch (not merely encounter-order values), including cold slot contents
  and all scalar error paths.

The production JUnit `LongArrayDequeRingSpanTest` uses the **real
class** and verifies the same physical state against a frozen scalar
baseline, both occurrence-removal directions, wrapped clears, iterator
epochs, no-op absence and a seeded `ArrayDeque` differential.
JDK module/JUnit source compilation and full jtreg/Maven CI are
**NOT YET EXECUTED**. There is no quantitative throughput claim.

## Receiver acceptance

The JDK's public API remains unchanged. Required:
```sh
# In an actual configured OpenJDK 21/M3JDK21 receiver:
jtreg test/jdk/jdk/m3/collections/LongArrayDequeRingSpanTest.java
jtreg test/jdk/jdk/m3/collections/LongArrayDequeTest.java
jtreg test/jdk/jdk/m3/collections/DequeSpanCopyTest.java
```
Run canonical Synexia Maven/OpenRewrite source qualification and full
module build prior to promotion. This receipt alone is not an executed
OpenRewrite recipe or full JDK acceptance.
