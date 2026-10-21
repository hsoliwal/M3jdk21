<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 persistent conjunction AVL receiver — 2026-10-19

Status: DRAFT; contract-preserving atom candidate, not completed M3JDK21 or JDK21 qualification.

## Direct runtime owner, no integration facade

Refined the existing `m3/collections/src/main/java/com/m3/collections/M3LongPredicates.java` persistent conjunction implementation. It is consumed by the same `M3LongFilteredView` and `M3LongSortedViews` owners, with the same Java callback, comparator, logical index, mask and native boundaries. No public API, storage representation, JNI exports, mmap ownership, or java.lang.String semantics are changed. Synexia is a source/recipe workspace, **not a runtime layer**.

Canonical first-party Apache-2.0 recipe and catalogue: https://github.com/hsoliwal/com.synexia/pull/12408
Source-lock canonical commit: `91c87e3345c1c4e3becaa63f07763e1f1cbf7d79`
Recipe name: `com.synexia.m3.M3Jdk21LongPredicateAvl20261019`
Recipe implementation: existing `com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe` with sealed crate `m3jdk21-long-predicate-avl-20261019`.
Target original baseline: `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49` (master).
No external competitor's implementation body was imported.

## Serial source receipt

| Existing file | SHA-256 original | SHA-256 receiver |
| --- | --- | --- |
| `M3LongPredicates.java` | `e5d9d7fca94c4eef0843bb7eb7929c2baba05892207f9336a6da3a089bc882ad` | `59a20296306197c2c9d5878dc118716f36d94bd0e825fd430aba656f4dfe2667` |
| `M3LongDeepFilterSelfTest.java` | `a5df095ddaf792e8dacdb93d674e1dd90294786684af5ca5851951d9d0a9fd6a` | `7cb605353030c09dcb048b7cc44c304a24151faea325e2bd1f41f758b5cdc2bf` |

Both Java files are byte-for-byte the reviewed canonical recipe's sealed templates at the pinned commit. The existing `.github/workflows/m3-long-filter-balanced-20261017.yml` is unchanged and includes both files in the path trigger, compiles their closure with Java 21 `-Xlint:all -Werror`, and invokes the changed selftest in both JIT/default and `-Xint` modes.

## Executed local evidence (not an M3JDK runtime acceptance)

The changed production AVL join atom compiled with Java 21, fatal lint, and passed **41,871** independent standalone assertions under `-Xmx64m -Xss256k`, including 100,000 conjunction appends, order, custom virtual `and`, short circuit, and persistence. The unchanged original fails the logarithmic-bound oracle at 10,000 appends. The expanded existing target selftest is received and staged; a full production-closure host proof, actual Maven SDK recipe replay, matched receiver normal/`-Xint` execution, hosted CI, JNI/JIT/GC/CDS checks and CPU/RSS benchmarks are **NOT_RUN**.

## Acceptance and chronology

Require source-preimage/postimage verification, actual canonical SDK recipe application/fixed point/drift refusal, compile + existing/new oracle + `-Xint` parity, and separate full M3JDK21 integration gates before merge. Keep String-first status; this collection atom does not advance product phases. Converge independently with #1391 and #1408 without losing their valid changes. Do not modify or silently bypass existing source-lock CI failures from older branches.
