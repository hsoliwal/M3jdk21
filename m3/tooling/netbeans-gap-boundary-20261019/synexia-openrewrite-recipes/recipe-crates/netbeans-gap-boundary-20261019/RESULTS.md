<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Executed results

Main recipe: six JUnit tests, all 32 partial states and fixed point/refusal.
Runtime: 622,359 checks per baseline/candidate in mixed, interpreter and C1 modes;
trace 5cd63654ed10502c. Unchanged NetBeans oracle: 1,440 cases each, trace
67cde5b951cd57fb. Existing HTMLDocument regression passes. All eight compiled
class descriptor dumps (including private fields) and serialized contract traces
match byte-for-byte. Product uses its existing this-escape lint exclusion; all
remaining enabled warnings are errors. The unchanged donor emits three warnings.

For 65,536 equal offsets, counted mark reads are 32,769 -> 18. At 4,096: 2,049 -> 14.
One/two-element and empty read counts preserve the incumbent. No new retained
fields or objects are introduced. Measured owned char/mark storage is identical
(3,277,040 bytes at 65,536); this excludes opaque reference-queue internals and
weak referents. Allocation observations are retained in BENCHMARKS.json.

## CPU diagnostic medians

Three fresh JVMs per variant, five measured rounds per case. Input construction
is outside the timed lookup; complete lookup calls are inside. This is not JMH,
whole-editor latency, or a cross-platform claim. Empty-loop ratios are excluded
because the JIT may eliminate them. All raw data and process RSS/CPU receipts remain.

| Size | Shape | Baseline ns/call | Candidate ns/call | Candidate / baseline |
| --- | --- | ---: | ---: | ---: |
| 1 | equal | 12.02 | 7.88 | 0.655 |
| 1 | unique | 11.68 | 11.64 | 0.996 |
| 1 | mid | 1.36 | 2.10 | 1.539 |
| 1 | runs64 | 1.39 | 1.36 | 0.979 |
| 1 | above | 1.32 | 1.29 | 0.978 |
| 64 | equal | 21.79 | 7.68 | 0.352 |
| 64 | unique | 21.00 | 13.24 | 0.631 |
| 64 | mid | 6.56 | 4.92 | 0.750 |
| 64 | runs64 | 4.74 | 3.43 | 0.723 |
| 64 | above | 4.92 | 3.99 | 0.811 |
| 4096 | equal | 1948.97 | 11.71 | 0.006 |
| 4096 | unique | 40.61 | 27.49 | 0.677 |
| 4096 | mid | 6.75 | 5.47 | 0.810 |
| 4096 | runs64 | 79.03 | 32.22 | 0.408 |
| 4096 | above | 5.36 | 3.90 | 0.728 |
| 65536 | equal | 31165.22 | 24.86 | 0.001 |
| 65536 | unique | 48.71 | 40.84 | 0.838 |
| 65536 | mid | 6.91 | 5.68 | 0.823 |
| 65536 | runs64 | 91.94 | 46.55 | 0.506 |
| 65536 | above | 4.56 | 4.09 | 0.896 |

The first candidate and all failures are retained. Some small/ordinary timing cases
may regress; consult the full table. No native-memory or universal speedup claim.
Full source-built image, full jtreg/tier/platform/JCK/reactor are pending.
