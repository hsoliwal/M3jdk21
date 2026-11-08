# M3JDK21 exact integer codebooks — recipe 2026-11-08

A contract-preserving Java 21 + JNI slice transferring LLM-style weight sharing
into exact conventional integer data. Target owner: `m3/arrays` inside the
integrated M3JDK21 SDK. No VM array, JDK API or existing JNI export changes.

## Implemented

- `IntCodebookArray`: immutable, snapshot-owned `int` codebook in first-occurrence
  order, with a packed 0..15-bit ID plane and O(1) random access. A primitive
  open-addressing transient hash table avoids boxed object entries, and is not
  retained in the data structure. Supports arbitrary signed ints, including
  `Integer.MIN_VALUE` and `Integer.MAX_VALUE`.
- `RAW` fallback automatically selected when `4 * codeCount + ceil(n * bits/8)`
  is not **strictly smaller** than `4 * n` bytes. Both figures exclude all Java
  array/object header overhead; wall-time speedups are not assumed.
- `IntCodebookNative`: optional **required** JNI materialization path only for
  codebook arrays with `length`, dictionary size **and** byte count <= 4096.
  There is no implicit fallback from a `Required` operation. JNI copies bounded
  primitive-array regions, retains no pointers, validates all IDs/padding
  before publishing a result, and preserves input ownership.
- C11 pure codec: separate from JNI, portable to other JNI-capable targets
  subject to native builds and platform qualification.

## Problem-category donor evaluation

| Candidate | Category | Contract comparison / adoption |
| --- | --- | --- |
| [Apache Lucene SortedDocValues](https://lucene.apache.org/core/10_5_2/core/org/apache/lucene/index/SortedDocValues.html) | DICTIONARY_ORDINAL | Concept of unique-value dictionary + ordinal, but Lucene handles binary terms, sorts dictionary, and is index-iterator oriented; no implementation copied (Apache-2.0). |
| [LeetCode 347 Top K Frequent Elements](https://leetcode.com/problems/top-k-frequent-elements/) | HASH_FREQUENCY | Counts cardinality but returns top K, not reversible array storage; problem shape referenced, no solution code copied. |
| [HackerRank Sparse Arrays](https://www.hackerrank.com/challenges/sparse-arrays/problem) | HASH_FREQUENCY | Repeated string frequency queries; not an integer codec, no solution code copied. |
| [GeeksforGeeks Replace Elements by Ranks](https://www.geeksforgeeks.org/dsa/replace-each-element-of-array-with-its-corresponding-rank/) | RANK_ENCODING | Distinct per-occurrence ranks (including duplicates), not unique-value dictionary ordinals. Concept only; no code copied. |

Donor implementation remains first-party Apache-2.0; challenge-site solutions
are not used as copyrighted source-code donors.

## Verification

- **Local Java 21:** `javac --release 21 -Xlint:all -Werror`; independently
  generated oracle arrays of varied lengths/distributions, arbitrary signed
  values, defensive copies, RAW routing, index bounds; **1,819 cases passed**.
- **Local Linux JNI:** same 1,819-case differential run with the new C11/JNI
  decoder loaded by `M3ArrayNative` passed.
- **Local strict C11:** `-Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic`,
  **9,000 native decode cases** plus failed-call destination invariance.
- Run the provided `verify-jdk21.sh` for standalone Java/C/native checks.
- Pending: full Maven reactor and Surefire, JDK jtreg, whole-SDK binary
  compatibility and performance benchmarking, Windows/macOS builds.

## Promotion gates

Keep this as a reviewable isolated atom. Do not replace standard arrays; do not
rely on JNI being present; benchmark codebook construction, random reads,
sequential decode and memory including headers before routing JDK collection
owners or LLM inference kernels through it. Avoid repeated per-element JNI.
