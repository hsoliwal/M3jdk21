<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 adaptive sparse-range JMH — paired performance review

**Status: PERF_HOLD** (2026-11-25). Do not equate two green JMH workflow jobs
with proof of performance-nonregression. Both source-built JMH runs successfully
executed exactly 64 matching original-dense/optimized pairs; the actual source
remains SHA-256 `d3f78a046a3ff1d1bc27dfebdfd0e82f34de3bc8e9b604fb722c9a728ab77fd8`.

Raw GitHub-hosted source-built result artifacts:

- [Run 45580086807](https://github.com/hsoliwal/M3jdk21/actions/runs/45580086807) (artifact `15008866438`), CSV SHA-256 `84e2e37e47936b9b3765b3c78dc33c1d8cfb5c0363cd720bc3521561467a8f58`.
- [Run 45580077410](https://github.com/hsoliwal/M3jdk21/actions/runs/45580077410) (artifact `15009750143`), CSV SHA-256 `711ec45e2897665256f933c018299a7ec645a16ebc7459b44875abd3eb682f18`.

## Matched source-built JMH measurements

| Distribution | Universe | Updates | Run 1 candidate / original | Run 2 candidate / original |
|---|---:|---:|---:|---:|
| RANDOM | 65,535 | 9,000 | 1.3466× | 1.2229× |
| RANDOM | 65,536 | 9,000 | 1.3345× | 1.3379× |
| RANDOM | 2,000,000 | 16 | 0.000216× | 0.000142× |
| RANDOM | 2,000,000 | 9,000 | 1.0155× | 1.0059× |

The first two workloads use the **retained dense path**; this evidence is
consistent with a JIT/code layout, allocation or benchmark-order change, not
proof that the sparse algorithm is incorrect. Each run has only three measured
JMH samples; the published 99.9% score-error intervals for the two dense
regressions overlap the corresponding baseline intervals. Thus **statistical
significance is not established**. Focused higher-fork/higher-sample
measurements and GC/JFR analysis are required before choosing a code change.

A dependency-free auditor is provided at
`test/micro/org/openjdk/bench/jdk/m3/arrays/audit_intlane_jmh.py`. It strictly
validates JMH 1.37 CSV header, 128 rows (64 pairs), parameter lattice,
method names, units, sample counts, finite scores and 99.9% score errors.
It produces a complete 64-key paired CSV, JSON and Markdown report. By default
it reports without changing the JDK; `--enforce` exits 2 on a repeated
**observed** slowdown exceeding both **20% and 5 µs/op** in every supplied run.
Malformed and duplicate-source evidence is rejected with exit 3. Its hermetic
negative mutation suite has seven passing test methods.

A permanent 128-row comparison snapshot is committed alongside this report
as `IntLaneSparseRangePairedEvidence.csv` (Git blob `722982ef04b6cc1b07d65a99574cd8aa55d3688e`,
SHA-256 `184310702a935dc6d08b3e07f2a25522b9ebba7d875283260c1ec6d81e22ade0`).
This preserves every 64-case original/candidate score, score-error and ratio
across both runs after ephemeral Actions artifacts expire.

The new lightweight GitHub Actions workflow downloads the two original
GitHub-hosted JMH artifacts using read-only permissions, checks their exact
SHA-256 bytes, runs the tests and auditor, and preserves receipts even on
`PERF_HOLD`. For future JMH runs it reviews that run independently via
`workflow_run` with `actions: read`. The code changes no Java runtime method,
JNI symbol or sealed source recipe; this is **qualification infrastructure**.

**Release policy:** broad sparse improvements are measurable and worth retaining,
but these dense random warnings require review. Avoid inventing a global speedup
claim; no JMH numbers justify waiving the Synexia Maven/OpenRewrite proof gate.

## November 25: single-run admission is now explicitly incomplete

The upstream `workflow_run` trigger can supply **only one** successful JMH producer run, even when it contains all 64 baseline/candidate pairs. One run is **not independent replication**, and its slowdown cannot truthfully be labeled `repeatable_regression`. The updated source-audited tool now sets `minimum_independent_runs=2`, returns `INSUFFICIENT_REPLICATION` and exits **2 on `--enforce`** for any single valid input (regardless of whether the observed ratios are good or bad); it never claims a green performance admission or a repeated slowdown from one run. Its JSON preserves `single_run_observational_warnings` for triage. The negative suite has grown from seven to **nine** tests, now including a clean single run and a slow single run. These tests passed locally, and the same historical two-source-built-run dataset still returns exactly `PERF_HOLD runs=2 paired_keys=64 flagged=2`, with a regenerated permanent CSV byte-identical to the committed 128-row snapshot. This is a **qualification correctness fix only**: no Java runtime, JMH benchmark or source-sealed recipe changes.


## Focused source-built JMH remeasurement configured (not yet executed)

A new create-only workflow `.github/workflows/m3jdk-intlane-dense-focused-jmh.yml` reuses the exact SHA-256-pinned JDK source, JMH benchmark and JMH dependencies from the already completed 64-pair source-built runs. OpenJDK's supported `MICRO=OPTIONS` selects exactly the two historical dense-random warnings: `RANDOM` with 9,000 updates and 65,535/65,536 rows. Unlike the earlier one-fork, three-sample runs, it requests **5 forks × 10 two-second measurement iterations** and 5 warmup iterations. The workflow fails closed if it lacks two complete baseline/candidate keys, finite JMH error bars, compatible units or at least 40 measured samples, and uploads the raw results for review.

**This is a planned measurement, not a performance result.** It cannot retroactively qualify historical dense regressions or replace the private Maven/OpenRewrite recipe gate. The Java 21 implementation and JMH benchmark source remain byte-identical to their pinned postimages.


## Focused JMH parser corrected and source tested (2026-11-25)

The earlier focused-workflow inline checker used a one-sided confidence-bar comparison. The **exactly replicated** `audit_intlane_dense_focused.py` now checks both interval boundaries, avoiding a false `overlap=true` label for an obviously faster candidate. It also accepts exactly the 4 original/candidate rows for `RANDOM`/9,000 updates at 65,535 and 65,536 rows, **exactly 50 samples per method** (5 forks × 10 measured iterations), JMH `avgt`/`us/op`/`Threads=1`, and finite positive scores with finite nonnegative 99.9% score errors. Duplicate/missing/incorrect benchmark identifiers, geometry, invalid columns/units, malformed UTF-8 and ZIP evidence fail closed. The review output is deterministic JSON and Markdown with per-case ratios, error bars, and **`PERFORMANCE_REVIEW_HOLD`**, never an automatic release approval.

The hermetic `test_audit_intlane_dense_focused.py` ran **16/16 passing local unit tests** including both directions of disjoint 99.9% score-error intervals. The GitHub workflow pins both exact source SHA-256 values, runs those 16 tests before any expensive source build, then invokes this source-tested validator on the actual two-case JMH CSV and uploads its JSON/Markdown plus raw JMH files. No product Java, benchmark source, JNI, or existing canonical recipe was changed. Focused JMH execution and statistical review remain pending genuine hosted measurements.

## November 25: real five-fork JMH cross-run reconciliation

Both *actual source-built*, five-fork focused JMH workflow attempts have now **completed successfully**, and their original raw `jmh-result.csv` files are committed in `evidence/focused-run-45593591175.csv` (SHA-256 `f4c035a1d8fc77747f581ad2bf7067c09ec472ef9769e1b878f31fec3b9e30a5`) and `evidence/focused-run-45597337526.csv` (SHA-256 `891d987673f06710148a30ef2559cab3be4c344349c5d268919cd835c6937504`).

- [Run 45593591175](https://github.com/hsoliwal/M3jdk21/actions/runs/45593591175), artifact 15015698445; exact JMH CSV SHA-256 as above; JDK21 source-built JMH 1.37, 5 forks, 50 samples per method
- [Run 45597337526](https://github.com/hsoliwal/M3jdk21/actions/runs/45597337526), artifact 15016603256; same source/JMH-seal settings, 5 forks and 50 samples per method

| Distribution / updates / universe | First candidate/original | Second candidate/original | Reported 99.9% JMH score-error intervals overlap |
|---|---:|---:|---|
| RANDOM / 9000 / 65535 | 1.0095× | 1.0701× | Yes / No |
| RANDOM / 9000 / 65536 | 1.0034× | 1.0857× | Yes / No |

Original baseline wall times differed by approximately **33% across runners** (99–101 µs/op versus 65–66 µs/op). This is a reason to focus on **within-run** ratios and investigate host scheduling/cache/JIT/GC; it is not evidence that the implementation itself changed. Neither focused run reproduced the original two 20%+ historical slowdowns, but one source-built run still observed ~7–9% slower candidate scores with non-overlapping *score-error intervals*. **Those intervals are not formal ratio confidence intervals** and the original 64-pair `PERF_HOLD` is **not closed**.

New reusable, dependency-free `compare_intlane_focused_runs.py` (SHA-256 `953db72747bf110fe329f9ef852cee3019539227f93f799dee67288bd25335c5`) pins both original GitHub run IDs, associated PR SHA IDs and **raw CSV SHA-256**, invokes the existing exact 50-sample JMH parser, checks independent inputs, preserves original/candidate scores/errors, ratio direction and overlap, and emits structured `focused-crossrun.json`, a four-row `focused-crossrun.csv`, and a review Markdown document. It **never** outputs a product release/nonregression clearance. The new hermetic `test_compare_intlane_focused_runs.py` has **14 passing local tests**, including missing/duplicate/malformed source, bad scores/units/sample count, mismatched source SHA, duplicate runs and zip-bomb checks. Combined with the preexisting 16 parser tests, the replay has **30/30 passing tests**.

The new **lightweight** `m3jdk-intlane-focused-crossrun.yml` workflow verifies source and permanent raw-data hashes, runs both test suites, regenerates and byte-compares the static Markdown and CSV against those same real measurements, and uploads evidence. This does not rebuild a JDK or automatically promote benchmark results. No runtime Java file, JNI symbol, M3JDK benchmark source, or private Synexia OpenRewrite recipe was changed.
