<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Isolated M3 cursor compiler diagnostic

This is a separate diagnostic using the unchanged actual-owner BenchmarkFixture.class. Each shape/access pair runs in its own JVM, with 20 warmup batches, 15 measured batches, and 128 passes. PrintCompilation and PrintInlining are enabled. It does not replace the retained 42-case measurements. All eight processes pass frozen independent checksums.

| Shape | Access | Baseline ns/unit | Candidate2 ns/unit | Before/after wall | Before/after CPU | Allocated B/batch before -> after |
|---|---|---:|---:|---:|---:|---:|
| atom-16384 | forward | 2.369 | 2.844 | 0.833 | 0.830 | 32 -> 48 |
| atom-16384 | random | 2.564 | 2.024 | 1.267 | 1.267 | 32 -> 48 |
| balanced-64-16384 | forward | 4.683 | 3.250 | 1.441 | 1.441 | 32 -> 176 |
| balanced-64-16384 | random | 97.623 | 82.241 | 1.187 | 1.186 | 32 -> 176 |

## What the compiler records establish

- All cursor/traversal compilation events in these diagnostic logs occur before the warmup completion marker.
- Both atom cases inline the 59-byte at method into the hot traversal loop and keep seek out of line in both builds. The isolated atom-forward result is about 20% slower, while isolated random is faster.
- In the final isolated 64-unit forward traversal compilation, baseline inlines seek (141 bytes). Candidate2 leaves seek (160 bytes) out of line as an already compiled medium method; candidate2 nevertheless wins this isolated timing.
- In the final isolated 64-unit random traversal compilation, candidate2 inlines at, seek, and resolveWindow (161 bytes); the rare advance/descend paths remain calls. Candidate2 wins the isolated random timing.

These observations are consistent with profile-sensitive compilation, but do not prove that a particular inlining choice caused the mixed-suite regressions. JVM logging, method profiles, and process layout also distinguish this diagnostic from the 42-case benchmark. No general speedup or non-regression claim follows.

## Design recommendation

For this M3 integration, prefer the smaller full-visible-scalar-window refinement that retains the original five cursor fields and object shape. It addresses repeated backward and intraleaf resolution without M3 pending arrays or adaptive state. Keep the complete retained traversal path in canonical MIndex. Validate that final source once against the frozen actual-owner corpus and benchmark matrix. The data does not justify JVM-specific annotations or workload-size thresholds.
