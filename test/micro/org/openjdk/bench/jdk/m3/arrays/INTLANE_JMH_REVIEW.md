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

The new lightweight GitHub Actions workflow downloads the two original
GitHub-hosted JMH artifacts using read-only permissions, checks their exact
SHA-256 bytes, runs the tests and auditor, and preserves receipts even on
`PERF_HOLD`. For future JMH runs it reviews that run independently via
`workflow_run` with `actions: read`. The code changes no Java runtime method,
JNI symbol or sealed source recipe; this is **qualification infrastructure**.

**Release policy:** broad sparse improvements are measurable and worth retaining,
but these dense random warnings require review. Avoid inventing a global speedup
claim; no JMH numbers justify waiving the Synexia Maven/OpenRewrite proof gate.
