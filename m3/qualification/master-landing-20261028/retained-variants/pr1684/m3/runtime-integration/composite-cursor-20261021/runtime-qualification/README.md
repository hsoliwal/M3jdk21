# Composite String and collection runtime qualification

The source-built M3JDK21 follow-up passes the targeted Java/JNI tests and all
14 selected existing regression configurations. **The owning collections Maven
gate remains red:** both Java and native lanes report 477 tests and fail the
same three filtered/sorted wrapper assertions. The exact baseline reproduces
those failures; this attribution does not turn either gate into a pass.

The implementation is published in [M3JDK21 PR 1684](https://github.com/hsoliwal/M3jdk21/pull/1684).
Its reusable recipes, catalogue decisions, and raw qualification records are in
[Synexia recipe draft PR 12715](https://github.com/hsoliwal/com.synexia/pull/12715).
The tested follow-up source is commit `97adad6154a1b819758d0ae6cf4eb7e3ba45b638`;
its canonical source qualification is `c22cd2b5efb45eade4ff98377082dd3e51e67ed4`.
The receiving build base is `2d37d917b49fe04207948a3a2f174b795b767c8e`.

## Implementation and contract boundary

The Java and native changes are in M3JDK21's existing owners: `M3String`,
`M3StringPrecomputeBridge`, `M3LongIndexProjection`, libjava's `String.c`, and
HotSpot's `jni.cpp`. A separate comment-only `M3StringImmediate` donor prerequisite
allows the ASCII source build. The runtime uses composite coordinates and leaf
continuation for tuple traversal, a monotone primitive-index fast path, the
existing native bulk character copier, and first-unit guards that avoid cursors
when an early mismatch or one-unit result determines the answer.

The private malformed-range correction is a bounded behavior repair: valid
public operations preserve their contract, but malformed private native calls
now reject invalid empty ranges. The expected baseline failure remains in the
evidence. This correction is not described as all-input equivalence.

Filtered/sorted wrapper exception and comparator repairs were not silently
folded into a strict behavior-preserving pass. The catalogue records their
counterexamples and rejected shortcuts. Their failing owning-module tests are
reported below.

## Actual checks

| Gate | Result and exact scope |
| --- | --- |
| Complete source-built images | Baseline, initial candidate, and follow-up Linux x86_64 fastdebug images build successfully. Baseline includes only the comment-only prerequisite. |
| Follow-up functional configurations | Ten test invocations plus a version check pass. M3-enabled runs use actual integrated storage, with CompactStrings enabled and disabled. |
| Keyed collection contracts | 17,577 checks in each M3-enabled compact setting; 17,575 checks in the flag-off reference configuration. |
| Cursor continuation | 113,710 checks in each compact setting. |
| Critical-first JNI access | With `-Xcheck:jni`, 1,523 checks and 170 fixtures, including 161 M3 fixtures, in each enabled compact setting; flag-off has 1,361 checks and 170 fixtures. |
| Private shadow ranges | 1,246 follow-up checks in each compact setting; the malformed-empty-range baseline failure is retained. |
| Existing regression sources | Six unchanged source oracles and all 14 declared configurations pass on baseline, initial, and follow-up images, using the same compiled fixtures. These are fresh-process direct-main runs, not jtreg executions. |
| Public/protected descriptors | String, M3String, and PrecomputeBridge descriptor output matches baseline. |
| Native exports | Initial libjava/libjvm export-name sets match baseline (265 and 244 names). Follow-up native library binaries are identical to the initial candidate. This is bounded interface evidence, not full ABI/JCK qualification. |
| Owning collections Java lane | **Fail:** 142 suites, 477 tests, 3 failures, 0 errors, 2 native-assumption skips. |
| Owning collections native lane | **Fail:** 142 suites, 477 tests, 3 failures, 0 errors, 0 skips, with required native capabilities and `-Xcheck:jni`. |

The full owning collection lanes use the initial candidate image and unchanged
owning POM. The first-unit follow-up does not change the collection postimage.
The separate 14-configuration follow-up regression is not a full owning-module
rerun.

The three failures concern comparator identity on every merged/sorted split and
two strict filtered-view `removeFirst` exception contracts. Four tests in three
selected classes reproduce the same failure class, test name, exception type,
and message on exact baseline sources and the baseline image. Original receipts
remain unchanged; append-only interpretation corrects a class-versus-test count
and equivalent local-file URI spellings in custody diagnostics.

## Measured resource result

The first-unit probe uses the same compiled classes across three actual images
and both compact settings. Each of the six runs passes 599 checks and records
42 samples, for 252 samples total. Allocation and per-thread CPU counters are
supported in every run.

The initial candidate allocates 48 bytes per tuple operand per operation in
the bounded early-mismatch and one-unit cases (96 bytes for two tuple operands).
The follow-up records zero allocated bytes in all 84 measured samples, including
controls, matching the baseline in these cases. This measures allocation traffic;
it does not measure retained heap or native/process memory.

[MEASUREMENTS.tsv](MEASUREMENTS.tsv) retains all 72 median rows: allocation and
thread CPU for each of the 28 workload/mode cases, plus all 16 keyed wall-time
workloads. The canonical comparisons retain every raw sample, range, checksum,
and slower observation. Wall-time data uses three samples per workload on
fastdebug interpreter images. Host load differs materially across stages, so
these diagnostics do not establish a general speedup or isolated timing effect.

The separate projection qualification, including partial-window ownership,
negative cases, resource measurements, and actual JNI/UBSan differential runs,
remains in the [monotone catalogue](../../../collections/algorithm-catalog/monotone-encounter-20261021/README.md).
The [consumer catalogue](../../../collections/algorithm-catalog/string-collection-consumers-20261021/README.md),
[Java owner review](../STRUCTURE-COVERAGE.md), and
[native/VM review](../NATIVE-VM-REVIEW.md) define their bounded coverage.

## Reusable refinement invariant and evidence

For every file, identify the problem and contract, consult the existing category
catalogue and donors, compare correctness, compatibility, complexity, resource
costs, and rights, and qualify replaceable internal atoms with the original
public behavior. Record selected and rejected choices for reuse. Publish the
canonical recipe first, receive exact generated results serially, and verify
zero-change replay. Existing canonical engines and memory remain the owners;
this pass adds no replacement engine or global configuration.

[RESULTS.json](RESULTS.json) binds image and source identities, outcomes, exact
failure signatures, and SHA-256s of canonical raw records. New raw evidence is
appended under
`synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017/evidence/final-runtime-20261021/`.
Earlier baseline and initial observations remain under `evidence/receiving-runtime-20261021/`
in that same crate. This three-file summary is received through the existing
`m3jdk21-runtime-20261021` recipe dataset.

This report does not qualify every repository owner, full jtreg/JCK, release/JIT
throughput, or cross-platform behavior. The recorded SA/adapter and extreme-length
review gaps remain open, as does complete repository-owned offline dependency
closure. The three owning-module failures remain a review gate.
