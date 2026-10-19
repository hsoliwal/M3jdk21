<!-- SPDX-License-Identifier: Apache-2.0 -->
# Viewport collection views: coverage (proving ground)

Scope: every class viewport-everywhere put into `m3/collections`:
- waves 1-2 from SWT;
- wave 3 from Nebula Grid;
- the folded engines (M3WindowRuntime, M3WindowPlan, M3RangeSet, M3TreeTopology, M3TreeProjection, M3TreeWindow,
  M3TreeWindowPlan, M3WindowPlanner, scroll geometry, M3ListModel, M3ListWidth);
- the general views (M3WindowView, M3FlatteningView, M3ListDiff, M3Patch, M3BoundedCache).

`m3/viewport-core` now holds only the adapter interfaces (no executable lines). Per-class numbers:
`WAVE1_COVERAGE.tsv`. The JaCoCo exec, CSV, XML and HTML are in the regenerable scratch folder
`S:/m3-scratch/viewport-everywhere` and are not committed.

| Run | Tests run (Maven count) | Lines | Branches |
|---|---|---|---|
| 1: wave 1, ported tests only (reports on G:, superseded) | 111 | 2159 / 2585 = 83.52% | 1464 / 1952 = 75.00% |
| 4: wave 1 final (reports on G:, superseded) | 151 | 2584 / 2585 = 99.96% | 1933 / 1952 = 99.03% |
| 5: waves 1+2, regenerated on S: | 170 | 3141 / 3142 = 99.97% | 2247 / 2268 = 99.07% |
| **8: after the fold, waves 1-3 + general views, on S: (2026-10-19)** | **183** | **3980 / 3981 = 99.97%** | **2686 / 2706 = 99.26%** |

**Gate (99% lines and branches): MET (run 8).** Command: local reactor `mvn -o verify`, `-Xlint:all,-module
-Werror`, JaCoCo 0.8.15 agent, after a fresh recipe regeneration (pass 2 UNCHANGED 66/66). Result: BUILD SUCCESS, 0
failures.

## Uncovered spots (1 line, 20 branches): all unreachable, with the reason

| Class:line | Missing | Why it cannot be reached |
|---|---|---|
| M3ScrollMetrics:125 | `maxTop == 0` true | `clamped <= maxTop`, so `clamped != 0` implies `maxTop != 0` |
| M3ScrollMetrics:138 | `maxSelection == 0`, `maxTop == 0` true | `clamped` is clamped to `maxSelection`; `maxTop == 0` means the thumb fills the bar, and that case was already taken |
| M3ScrollMetrics:153 | `value/from/to <= 0` (3) | both callers return before `roundedScale` when any of the three is 0 |
| M3WindowRuntime.Selection:226 | `allSelected` with `oldCount >= count` | equal and smaller counts return earlier in `setLogicalCount` |
| M3WindowRuntime.Selection:320, :376 | `capacity != ends.length` | `starts` and `ends` are always reallocated together |
| M3WindowRuntime:607, :666 | loop exhausting 4 passes | bar visibility only switches on (monotone), so the fixed point is reached by the third pass |
| M3BitLanes:152 | `index >= length` | bits at or beyond `length` are always clear |
| M3BitLanes:228 | `shift >= 32`, `words.length == 0` | `shiftUp` reads at most 31 bits below a word start; a shifted lane is never empty |
| M3BitLanes:280 | `start >= end` | every caller passes a non-empty range |
| M3SparseExtents:302 | `i > measured` on loop entry | the Fenwick walk breaks on `step > measured - i` first |
| M3TreeProjection:349, :385 (the 1 line) | loop exit without `return` | `fillWindow` returns when `written == length`, or throws |
| M3TreeTopology:559 | `shifted < Integer.MIN_VALUE` | child indices are never negative and shifts are +1/-1 |
| M3TreeTopology:601 | inconsistent sibling chain | defensive: the chain and the sibling forest are updated together |
| M3RowTree:374 | `delta == 0` with `visibleDelta != 0` | a visible element weighs `height + 1 >= 1`, so a visibility change always changes the weight |

The guards stay: they are the originals copied by recipe, and changing behaviour-bearing code only to raise a number is
out of scope.
