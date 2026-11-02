<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 ASCII root qualification — Java 21 isolated proof

Status: **candidate-only / HOLD**. This qualification adds no product Java/JNI
modification or public API. Its executable harness measures the opt-in `int[128]`
root accelerator implemented by M3JDK21 #2318, stacked on #2314 and #2312.
Canonical Synexia recipe owner is the new separate hash-pinned qualification
crate stacked on #15428, #15415 and #15400.

## Source, behavior and algorithm constraints

* Original full `M3StaticGrammarLexicon.java` SHA-256
  `2618970818dcff0fcacf9fc96134e9a718b05d6e64b6d5ca699f9a5ef282d287`.
* Root-optimized full source SHA-256
  `9dd313fac5b35f4357646195607d3e35f584d9728a70848d3440d95a73847ffe`.
* Existing public signatures, grammar digest, lexeme IDs, source UTF-16 offsets,
  source ownership, and native JNI C17 body remain unchanged.
* Reuse `M3StaticGrammarLexicon`, `M3GrammarTokenProjection`,
  `M3AsciiRootPrecomputeProof` and existing independent JNI differential oracle.
* LeetCode #208 (Trie), #14 (Longest Common Prefix), #28 (Substring Search),
  HackerRank Contacts and No Prefix Set, and GeeksforGeeks Trie/Pattern
  Searching are **algorithm categories / test shapes only**. No source or
  editorial from challenge websites is copied. Java 21 JLS §3 is the lexical
  specification authority.

## Local execution evidence (OpenJDK 21.0.12.1, x86_64 Linux)

The baseline and optimized complete Java sources were compiled separately using
`javac --release 21 -Xlint:all -Werror`. Each immutable lexicon used the same
64-bit input-generation seed and vocabulary. Measurements were taken in **separate
JVMs** with `-Xms256m -Xmx256m -XX:+UseSerialGC`, 4096 prebuilt strings,
700,000 warmup calls, 5 rounds × 1,500,000 calls, median of the five.

| Vocabulary / workload | Baseline ns/op | Optimized ns/op | Notes |
|---|---:|---:|---|
| 26 roots, exact hit | 27.869 | 22.370 | isolated benefit |
| 26 roots, longest hit | 33.502 | 22.289 | isolated benefit |
| 26 roots, exact miss | 6.780 | 1.922 | isolated benefit |
| 8 roots, exact hit | 23.595 | 20.997 | isolated benefit |
| 7 roots, exact hit | 25.307 | 23.924 | mixed small-vocabulary results |
| 7 roots, longest hit | 27.979 | 36.139 | optimization absent; host noise/JIT variance |

**Do not extrapolate the timing values to HotSpot/JDK, M3String or application
throughput.** This is not JMH, no cross-platform confidence intervals or
calibrated end-to-end benchmark. Raw measurements are saved in the accompanying
Synexia evidence TSV.

Both original and optimized source images passed the following independent
isolation gates: identical public `javap -public -s` output; the original and
optimized 200,000 seeded lexicon differential checks; 380,254 original token
projection checks per image; optimized root lookup 2,009,472 checks; 50,000 JNI
oracle comparisons in normal `-Xcheck:jni` and 50,000 in `-Xint -Xcheck:jni`.

Hot-path thread allocation testing produced **0 measured allocated bytes for
1,000,000 lookup-loop iterations** against both Java images using the current
Java 21 `ThreadMXBean`; this host result is not an assertion about every JVM.
An externally owned `StringBuilder` was mutated between lookups, proving there
is no stale result or retained input. Four independent concurrent readers of
one immutable grammar produced identical checksums.

## Repeatable local target gate

From this module, run
`bash proofs/verify-ascii-root-qualification-20261102.sh`. The script refuses
an existing `target/m3-ascii-root-qualification` and writes new logs only in the
module-owned Maven target directory. It runs a Java21 strict compile, the
allocation/concurrency test, and deterministic performance samples. It never
modifies product source, public APIs, native code, protected branches, or
existing source files.

## Remaining HOLD gates

Real Maven/OpenRewrite `Recipe.run`, JUnit/Surefire and JaCoCo, complete
M3JDK21 source build/jtreg, cross-platform JNI, JMH crossover and application
benchmarks are **not executed**. Do not merge/promote or claim a production
speedup without those gates and source-custody proof.
