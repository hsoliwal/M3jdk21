<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# m3/algorithms: the progress contract of an algorithm atom (leaf progress-algorithm-20261108)

The landing lane (com-synexia-d7) is absorbing the challenge-catalogue tooling of `synexia-algo` into m3/tooling
(`recipe/catalogue-kernels-20261107/HANDOFF_4D.tsv` + `recipe/corpus-search-20261108/HANDOFF_D7.tsv`, 304 + 41 rows)
and found that those classes import two types of the donor package `com.synexia.algorithms.core`: `ProgressMonitors`
(41 imports) and `ProgressAlgorithm` (13). The collections lane (com-synexia-a4) hands five monitor-bound structures to
this module for the same reason: they call `ProgressMonitors.nonNull / checkCanceled`. `ProgressMonitors` is already
here as the package-private `M3ProgressChecks` (catalogue-kernels-20261107); `ProgressAlgorithm` had no owner (census4
routed the core package to java.base, lane 4d). This leaf gives both an owner in `jdk.m3.algorithm`, the module of
`M3ProgressMonitor`. Donor `hsoliwal/com.synexia` develop `a7b7bb639802c0042c11cf2a5dc8427edd6a9775` (census4 pin).

## Owner decisions ([DECISION.tsv](DECISION.tsv), 13 rows: the core package and its 7 donor tests)

| decision | rows | where |
|---|---|---|
| ABSORB, this leaf | 1 | `ProgressAlgorithm` -> `jdk.m3.algorithm.M3ProgressAlgorithm`, below |
| ALREADY, edited here | 1 | `ProgressMonitors` = `M3ProgressChecks`, made public by the owner-edit recipe (below) |
| OMIT | 4 + 7 tests | `IAlgorithm` (the older compatibility contract: slf4j logger, `logged/timed/retry/when/map` over child slices), `AbstractAlgorithm` (hooks + name/time/space metadata; the catalog `Shape` rows carry the metadata), `AlgorithmException` (constructors log through slf4j; M3JDK carries no slf4j), `NativeParityGate` (JNI parity gate; the module is pure Java). Their donor users (shapes, eip, gof, microservices, pipeline) decide with their families |

## Class added (recipe [relocate.py](relocate.py), map [RELOCATION_MAP.tsv](RELOCATION_MAP.tsv))

| M3 type | donor | kept / dropped |
|---|---|---|
| `M3ProgressAlgorithm<I, O>` (public functional interface) | `ProgressAlgorithm<I, O>` (blob 0b0d30906fe8) | kept: `O execute(I input, M3ProgressMonitor monitor)`. Dropped by declared rows: `asAlgorithm()` (adapter to the OMIT `IAlgorithm`), `andThen()` (allocates child slices: `ProgressMonitors.child` = the owner `jdk.m3.collections.M3ProgressMonitor.split`; a chain is a lambda over `execute`), the `java.util.Objects` import of the dropped chain. EDIT E1 (hash-pinned): the class comment names the M3 contract instead of the `IAlgorithm` compatibility surface |

No donor test is relocated: the three tests of the core package that touch `ProgressAlgorithm` bind `Searching` /
`Sorting` factories of other packages, the donor `snapshot()`, `IAlgorithm` chains or `AbstractAlgorithm`
(SKIP rows). M3JDK-authored: `M3ProgressAlgorithmTest` (4 tests: execution with the supplied monitor, cancellation and
interruption through `M3ProgressChecks`, lambda composition without dropping the monitor, exactly one abstract method
and a public `M3ProgressChecks`) and `M3ProgressAlgorithmDump` (the differential driver: two lambdas bound to the
contract on both sides).

## Owner edit (recipe [apply.py](apply.py) v2, rows [EDITS.tsv](EDITS.tsv))

`M3ProgressChecks` becomes `public final class` (one REPLACE_LINE row, pinned on the master pre-image
`263458582b2d` -> `34ea003a2628`). Its two members, `nonNull(M3ProgressMonitor)` and `checkCanceled(M3ProgressMonitor)`
(thread interruption first, then the monitor), are unchanged; the explicit private constructor was already there. The
REPLACE_LINE kind is the v2 addition to the lineage apply.py (INSERT_AFTER could not express a visibility change).

With this leaf, a donor class anywhere in M3JDK maps `ProgressMonitors -> M3ProgressChecks`, `ProgressAlgorithm ->
M3ProgressAlgorithm` and `IProgressMonitor -> M3ProgressMonitor` by TYPE rows, and `IProgressMonitor.noop() ->
M3ProgressMonitor.none()` by a MEMBER row, across module boundaries (`org.m3jdk.algorithms` exports
`jdk.m3.algorithm`).

## Gates

[GATES.tsv](GATES.tsv) records every gate with its command, state and evidence; [verify.sh](verify.sh) runs them in
order (no mutants: the relocated type has no bodies). Lineage: corpus-search-20261108 (relocate.py, differential.sh,
javap_parity.py v2, apply.py + REPLACE_LINE). Lessons: [RECIPE_MEMORY.tsv](RECIPE_MEMORY.tsv). Donor-first decision:
[FOSS_REUSE_DECISION.tsv](FOSS_REUSE_DECISION.tsv).

## Next

The contract unblocks three lanes: the m3/tooling catalogue absorption (d7), the five monitor-bound structures
(lane 37 leaf 124: `IntRadix`, `LongRadix`, `PackedLongFenwickTree`, NEC rows for `IntAhoCorasick` and
`IntWaveletMatrix`), and the `ChallengeSupersetKernels` verification (leaf 123: 24 lambda-shaped kernels bound to this
contract, NEC member by member against the landed families).
