# Source-built M3JDK21 focused dense-range JMH: two-run review

**Status: PERFORMANCE_REVIEW_HOLD — not production clearance.**

Two independent source-built JDK/JMH jobs each collected 50 samples per method for two dense-random geometries.

| Universe | Run 45593591175 candidate/original | Run 45597337526 candidate/original | 99.9% score-error bars overlap (first / second)? |
|---:|---:|---:|---|
| 65,535 | 1.0095× | 1.0701× | true / false |
| 65,536 | 1.0034× | 1.0857× | true / false |

Neither focused run repeats the original 20%+ slowdown. One focused run shows a 7–9% observed slowdown; the other is near parity. Absolute baseline times differ by ~33%, so do not compare absolute timings across jobs.

**These error bars are not a significance test for the ratio.** The historical 64-pair JMH review remains a separate HOLD until JFR/JIT/GC and execution-order effects are evaluated.
