<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# Original M3JDK21 exact-integer signal distance receiver (2026-10-25)

**M3JDK21 is the actual modified Java SDK product, not a separate M3+JDK layer.**
Canonical Maven/OpenRewrite source generation, SHA-256 admission manifests and reusable
algorithm correction memory remain exclusively in `hsoliwal/com.synexia`, recipe
`M3SignalExactIntegerDistanceRecipe` in Synexia PR #13267. This receiving source
contains only exact source/test postimages and original test/CI/receipt outputs.

## Existing-owner composition

`M3SignalExactIntegerDistance` uses exactly the original immutable
`M3SignalColumns` primitive coordinates, `M3SignalQualitySchema` quality
metadata and `M3SignalDependencyEpochs` source lifecycle. It does not duplicate
fact values, create a second progressive metric engine, change the existing
`M3SignalTopK` ordinal ordering or invent JNI exports.

The new distance deliberately supports only `INTEGER` and `LONG` source facts
whose declared reliability is `EXACT_FACT`. It computes the weighted L1
**coordinate** distance as an unsigned 128-bit `(high,low)` pair. `long` values
outside +/-2^53 are exact, including `Long.MIN_VALUE` to `Long.MAX_VALUE`.
Caller weights are positive integers at most 1,000,000; maximum 128 distinct
dimensions. The result uses no lossy floating-point conversion. This is an exact
measurement of stored coordinates, **not a compiler-proven object semantic
metric, an equality certificate or evidence for unverified heuristic pruning**.

Stable bounded nearest-neighbor selection over selected physical rows uses
`int[]` row/encounter-index lanes and `long[]` high/low heaps of size `min(k,n)`.
Repetitions and physical-row encounter ties remain stable. No per-candidate
retained `Map.Entry`, `Node`, wrapper, copied object or metric object is created.
A transient `Distance` API record exists for explicit pair queries only.

The original source version/SHA-256 and generation epoch must remain current;
missing metric coordinates fail closed. External/reflection/native changes and
incomplete attributed dependency graphs require upstream conservative
`invalidateAll()`. Optional `M3ProgressMonitor` cancellation, task progress and
reentrant callbacks never publish partial results or mark work done on failure.

## Verification and admission hold

- **Local scoped PASS:** OpenJDK 21 `javac --release 21 -proc:none -Xlint:all -Werror`
  against exact new Java source and source-compatible first-party API doubles.
  Normal and `-Xint` each passed **22,759** deterministic checks: 600 random
  arbitrary-precision BigInteger independent reference datasets, stable bounded
  Top-K, duplicate source rows, signed 64-bit extrema, multi-weight high limbs,
  missing facts, stale version/digest/epoch, invalid declarations, and cancellation.
- **PENDING:** exact current-head original M3JDK21 source compiler/JUnit module,
  canonical Synexia SDK first-apply/fixed-point/refusal, whole modified JDK,
  jtreg/JCK, jmod packaging, JNI/HotSpot/JIT/GC/CDS, multi-platform and CPU/heap
  benchmark comparisons. A source-compatible double is not installed-JDK proof.
- Keep both existing PRs as **DRAFT/HOLD**. A completed or queued workflow does not
  prove any gate until its actual step/test result is inspected.
