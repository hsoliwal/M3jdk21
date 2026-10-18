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
| **4: final** | **151** | **2584 / 2585 = 99.96%** | **1933 / 1952 = 99.03%** |

**Gate (99% lines and branches): MET.** Run 4: BUILD SUCCESS, 0 failures (collections 23 tests, viewport-core 128).

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

The guards stay: they are the SWT originals copied by recipe, and changing behaviour-bearing code only to raise a
number is out of scope.
