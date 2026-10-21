<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 array-index refinement contract

Baseline: ac8907bef72caa156c3b3c6740aaefa20db16379.
Task authorization: implement Java/JNI collection refinements and submit reviewable PRs; do not merge.

M3JDK21 is the product. Synexia is a canonical donor/convergence workspace, not a required runtime service. This pass changes existing product sources and imports build-time transformation recipes; it introduces no runtime integration layer. The current separately built collection module must not be misreported as replacement of all java.util backends or as a qualified source-built JDK.

## Synexia M3 invariant: catalogue-driven recipe refinement

Every admitted behavioral transformation is a reusable, validated recipe. Inventory the exact source revision, document its contract, identify the problem category, consult revision-pinned donor/catalogue evidence, compare candidates, and transform serially per file. Preserve public/protected interfaces and observable behavior. Record the selected and rejected algorithms, rationale, source seals, tests, resource observations and unresolved obligations. Corrections extend recipe fixtures and improvement memory; they do not remain only in chat. Decompose only as far as useful semantic replacement and verification require. A matching hash is source custody, not proof of behavioral equivalence.

LeetCode, HackerRank and GeeksforGeeks are algorithm-discovery inputs, not automatic licenses to copy submissions. Preserve donor notices and establish the exact license before receiving source. Use first-party M3/Synexia implementations where already present. Primitive arrays, offsets, indexes and state masks are preferred representations subject to the actual contract and measurements, not a universal performance claim.

## This pass

Existing owner: m3/collections/src/main/java/com/m3/collections/M3ArrayifiedIndexOps.java.
Baseline Git blob: 26c8f2fa2645d94fbf1b5561321b74b09dded877.
Baseline SHA-256: 42b3d760c9dfcf30eba72da193159e0ac6bd7ee06a3de292b132f49bd955ea77.

Refine detached rotation using two array copies; reuse the exclusively owned JNI-returned logical-position array during physical-ID remapping; bulk-copy unopposed stable-merge tails. Preserve gather and Java mask-counting semantics. Reuse M3IndexMaskCompactor and its current JNI binary rather than adding an alternate backend. No new runtime public signatures or JNI exports.

## Contract locks

Preserve checked half-open windows, validation and exception order, signed rotation distances, detached outputs, input immutability, duplicate occurrence identity, unsigned-byte masks, explicit native refusal, legacy-native fallback, stable ties and the exact comparator callback sequence. Logical positions are not physical row IDs. Arbitrary predicates/comparators remain Java callbacks; they are not cached or translated into native masks without a separate admitted contract.

A live filtered view must not become a snapshot. Sorted views retain their comparator, equality/representative rules, null policy and backing-mutation visibility. String-key operations keep UTF-16 semantics and exact content comparison; different owners, ranges or DAG shapes do not imply unequal text. No lookup-only interning, duplicate text pool or raw-address equality substitution is authorized.

## Verification and promotion

Documentation precedes source materialization. Recipe epoch: diff -> source-policy lint -> compile -> tests/fixed point/refusal. Product epoch: emitted diff -> source-policy/C lint -> Java and JNI compile -> tests -> checked runtime. Record limitations of the source-policy linter; javac warnings are independently fatal. No stage inversion or skipped failure. Track pre/post hashes and exact generated outputs; only recipe emissions may become product changes.

Component parity on a stock Java 21 host is not a full M3JDK boot, jtreg, whole-module or cross-platform qualification. The complete trie/radix, filtered/sorted-wrapper, persistent/concurrent/weak/identity collection and all-fork programme remains explicit coverage work; no completion is inferred from this bounded pass.
