<!-- SPDX-License-Identifier: Apache-2.0 -->
# Wave 1 coverage (m3/collections viewport owners + m3/viewport-core)

Runs of 2026-10-18/19 on a local reactor over `m3/collections` and `m3/viewport-core` (`mvn -o verify`,
`-Xlint:all,-module -Werror`) with the JaCoCo 0.8.15 agent. The tests are the 15 recipe-ported SWT tests plus the
direct tests `M3ViewportRuntimeTest`, `M3ViewportPlannerTest`, `M3ScrollGeometryTest`, `M3ViewportCoreEdgesTest`,
`M3BlockCacheTest` and `M3ViewportCollectionsTest`. Per-class numbers: `WAVE1_COVERAGE.tsv`. The CSV, XML and HTML
reports were written to the regenerable scratch folder and are not committed.

| Run | Tests | Lines | Branches |
|---|---|---|---|
| 1: ported tests only | 111 | 2159 / 2585 = 83.52% | 1464 / 1952 = 75.00% |
| 2: + runtime, planner, scroll and block-cache tests | 136 | 2528 / 2585 = 97.79% | 1807 / 1952 = 92.57% |
| 3: + edge tests | 149 | 2584 / 2585 = 99.96% | 1920 / 1952 = 98.36% |
| 4: wave 1 final (reports on G:, superseded) | 151 | 2584 / 2585 = 99.96% | 1933 / 1952 = 99.03% |
| 5: waves 1+2, regenerated on S: | 170 | 3141 / 3142 = 99.97% | 2247 / 2268 = 99.07% |
| **6: waves 1-3, folded into m3/collections, slot-map swap, M3ListDiff (S:)** | **178** | **3810 / 3811 = 99.97%** | **2614 / 2634 = 99.24%** |

**Gate (99% lines and branches): MET (run 5).** Runs 1-4 wrote their JaCoCo exec and reports to G:, which was later
found unhealthy (exFAT dirty bit, random write failures). Run 5 regenerated everything on `S:/m3-scratch/viewport-everywhere`
(2026-10-19) after a fresh recipe regeneration (pass 2 UNCHANGED 66/66): BUILD SUCCESS, 170 tests, 0 failures
(collections 28, viewport-core 142). The wave-1 per-class numbers equal those of run 4. Wave 2 adds M3ListModel,
M3ListWidth and M3SparseItemStore. Run 5 left two reachable branches uncovered, one each in `M3ListModel.isSelected`
and `M3SparseItemStore.insert` on an empty store; tests for both were added after run 5.

## Uncovered spots (1 line, 19 branches): all unreachable, with the reason

| Class:line | Missing | Why it cannot be reached |
|---|---|---|
| M3ScrollMetrics:125 | `maxTop == 0` true | `clamped <= maxTop`, so `clamped != 0` implies `maxTop != 0` |
| M3ScrollMetrics:138 | `maxSelection == 0`, `maxTop == 0` true | `clamped` is clamped to `maxSelection`; `maxTop == 0` means every row is visible, so the thumb fills the bar and `maxSelection == 0` was already taken |
| M3ScrollMetrics:153 | `value/from/to <= 0` (3) | both callers return before `roundedScale` when any of the three is 0 |
| M3ViewportRuntime:226 | `allSelected` with `oldCount >= count` | equal and smaller counts return earlier in `setLogicalCount` |
| M3ViewportRuntime:320, :376 | `capacity != ends.length` | `starts` and `ends` are always reallocated together, so their lengths are equal |
| M3ViewportRuntime:607, :666 | loop exhausting 4 passes | bar visibility only switches on (monotone), so the fixed point is reached by the third pass |
| M3BitLanes:152 | `index >= length` | bits at or beyond `length` are always clear (`setLength`, `remove` and `insert` keep that invariant) |
| M3BitLanes:228 | `shift >= 32`, `words.length == 0` | `shiftUp` reads at most 31 bits below a word start; a lane being shifted is never empty |
| M3BitLanes:280 | `start >= end` | every caller passes a non-empty range |
| M3SparseExtents:302 | `i > measured` on loop entry | the Fenwick walk breaks on `step > measured - i` before `i` can pass `measured` |
| M3TreeProjection:349, :385 (the 1 line) | loop exit without `return` | `fillWindow` returns when `written == length`, or throws on overflow |
| M3TreeTopology:559 | `shifted < Integer.MIN_VALUE` | child indices are never negative and shifts are +1/-1 |
| M3TreeTopology:601 | inconsistent sibling chain | defensive check: the chain and the sibling forest are updated together |
| M3ScrollMetrics, others | (run 5) | the same 19 branches and 1 line as above; nothing new is unreachable in wave 2 |

The guards stay: they are the SWT originals copied by recipe, and changing behaviour-bearing code only to raise a
number is out of scope.
