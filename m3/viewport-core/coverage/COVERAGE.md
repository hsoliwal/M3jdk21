<!-- SPDX-License-Identifier: Apache-2.0 -->
# Wave 1 coverage (m3/collections viewport owners + m3/viewport-core)

Run 2026-10-18: local reactor over `m3/collections` and `m3/viewport-core` (`mvn -o verify`,
`-Xlint:all,-module -Werror`), JaCoCo 0.8.15 agent, tests `M3SparseExtentsTest`, `M3ColumnarLanesTest` and every
`com.m3.viewport.*Test`. Result: BUILD SUCCESS, 111 tests, 0 failures, 0 errors, 0 skipped (collections 9,
viewport-core 102). Per-class numbers: `WAVE1_COVERAGE.tsv` (CSV and HTML were written to the regenerable scratch
folder, not committed).

| Scope | Lines | Branches |
|---|---|---|
| All wave-1 classes | 2159 / 2585 = 83.52% | 1464 / 1952 = 75.00% |

**Gate (99% lines and branches): NOT MET yet.** The gap is the SWT tests that were not ported because they need SWT
widgets or the UI differential harness, plus branches that only SWT widget tests reached:

| Class | Lines | Why below 99% | Next step |
|---|---|---|---|
| M3ScrollBarGeometry | 0 / 27 | its SWT test reads expectations from `Test_org_eclipse_swt_widgets_ScrollBar_GeometryDifferential` (UI) | direct JUnit test |
| M3ScrollLayout, M3ViewportRuntime (`solveRows`, layouts) | 0 / 13, 1 / 97 | exercised in SWT by ScrolledComposite/Table widget tests | direct JUnit test |
| M3ViewportPlanner (+ HorizontalWindow) | 27 / 180 | exercised in SWT by Table/List widget tests | direct JUnit test |
| M3BlockCache | 77 / 130 | exercised in SWT by `VirtualPageSource` widget tests | direct JUnit test |
| M3ScrollMetrics, M3RowWindowPlan, M3HeapGrowth, M3BitLanes, M3SelectionModel | 75-97% | partial SWT unit tests | direct JUnit tests for the missing branches |

Tests are added as new test classes (rule 3: new classes are written directly); the recipe-ported tests stay
byte-for-byte recipe output.
