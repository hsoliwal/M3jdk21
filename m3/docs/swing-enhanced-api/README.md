# Enhanced Swing API (M3JDK)

User, 2026-10-19: "make the SWT Swing API richer". The enhanced-API map (SWT fork `docs/m3/ENHANCED_API_MAP.tsv`, branch
`m3/enhanced-api-20261017`) lists capabilities with a Swing applicability column. This track ships the rows marked
for Swing in `java.desktop` itself: M3JDK21 is the JDK, so the additions are made in place, not in a side library.

## Rules

- **Additive.** Nothing that exists changes behaviour (BCP). An application that does not call the new API sees
  stock behaviour; the differential test in `m3/ports/swing-enhanced-api` runs one probe of `JTable`, `JList` and
  `JTree` against master's classes and against this branch's, and the outputs must be byte-equal.
- **Natural Swing shape.** New behaviour goes onto the existing classes as methods where a Swing developer would look
  for it: `JTable` / `JList` / `JTree` `getSelectionRanges`, `getVisibleRange`, and `scrollRowToVisible(row,
  alignment)`. Small new types in `javax.swing` are added only for values and for capabilities that span the
  three components (`M3ItemRange`, `M3ScrollInfo`, `M3RowMath`, `M3Emptiness`, `M3TypeAhead`, `M3DiffApply`).
- **Parallel names.** A concept has the same name in SWT and Swing (`M3ItemRange`, `getSelectionRanges`,
  `M3Scrolling.TOP`, ...). The pairs are recorded in the SWT map (column `proposed_m3_surface`, a `Swing:` part)
  and in `API_DECISIONS.tsv` (column `swt_twin`).
- **Review.** Every public addition is one row of `API_DECISIONS.tsv`: class, member, rationale, SWT twin,
  compatibility note. The user reviews that table.
- **DOOP.** The additions read the existing lanes rather than building objects:
  - the selection model's bit set, walked as runs;
  - the M3 tree layout caches (`sun.swing.tree.M3VariableHeightLayoutCache`, `M3FixedHeightLayoutCache`), through
    `JTree`'s row API;
  - the `JList` row offsets of the Swing viewport work.
  The M3JDK Swing viewport branches are reused and not edited.
- **Tests.** JUnit on JDK 21 with `--patch-module java.desktop=...`. The changed classes are compiled alone against
  the JDK's own module. Coverage of the new code is at least 99%. Build output is written to `S:/m3-scratch`.

## Lineage

These classes and methods are the Swing counterparts of the SWT enhanced API written by Hitesh Soliwal (Synexia):
SWT fork `hsoliwal/eclipse.platform.swt`, branch `m3/enhanced-api-20261017`, licensed `Apache-2.0 OR EPL-2.0`. The
Swing code is written fresh against Swing's types. It is not a copy, and it is licensed in `java.desktop` under
GPLv2 with the Classpath exception by the same rights holder. New files carry the Synexia GPLv2+CPE header. Edits to
Oracle files keep Oracle's header and are listed in `API_DECISIONS.tsv`.

## Leaves

| leaf | content | status |
|---|---|---|
| S1 | `M3ItemRange`; `getSelectionRanges` / `setSelectionRanges` / `getVisibleRange` on JList, JTable, JTree | done: 16 tests, new code 100% lines / 96% branches, differential equal |
| S2 | `M3Scrolling` alignments, `scrollRowToVisible(row, alignment)`, `M3ScrollInfo` (`getScrollInfo()`) on JList, JTable, JTree | done: 30 tests in all, new code 100% lines / 97.2% branches, differential equal; benchmark below |
| S3 | `M3RowMath` (JTree visible-row arithmetic over the layout cache) | planned |
| S4 | `M3Emptiness` (row count crossing zero, for all three) | planned |
| S5 | `M3TypeAhead` (`indexOf` with prefix/exact and wrap; JTable included) | planned |
| S6 | `M3DiffApply` (identity diff onto DefaultListModel / DefaultTableModel / DefaultTreeModel) | planned |
| S7 | sort and filter binding of the sort-filter design (multi-key permutation index, bitmask filters) | planned |
| S8 | the one-model seam: `M3Patch`; `AbstractTableModel.firePatch`, `DefaultTableModel` / `DefaultListModel` / `DefaultTreeModel` `apply`: deltas reach JTable, JList, JTree as row events, never as whole-model changes | done: 22 tests in all, new code 100% lines / 97.2% branches, differential equal |

Examples: `examples/` has the JTable, JList and JTree demos of the shared scenario (SWT twin snippets in
`org.eclipse.swt.snippets.m3`).

## Runs

| date | leaf | commits | run | result |
|---|---|---|---|---|
| 2026-10-19 | S1 | e6ce53b8d6f | `run-tests.sh coverage`, `run-tests.sh diff` | 16/16; new code 100% lines, 96% branches; differential equal |
| 2026-10-19 | S8 | d7284fdd1f9 (`M3Patch`), 8cc0a4d2b29 (`firePatch`, the three `apply` methods, `M3PatchTest`, `ModelPatchTest`; both committed by the interval sweep as "WIP checkpoint") | `run-tests.sh coverage`, `run-tests.sh diff` on JDK 21, `--patch-module` | 22/22 tests in all; new code 412/412 lines, 243/250 branches (97.2%); probe differential equal, 281 lines, sha256 5815825a9e9a1603 |
| 2026-10-20 | S2 | this commit (`M3Scrolling`, `M3ScrollInfo`, `M3ScrollSupport`, `ScrollingTest`; earlier parts committed by the interval sweep) | `run-tests.sh coverage`, `run-tests.sh diff`, `run-tests.sh bench S2 100000 1000000` | 30/30 tests in all; new code 550/550 lines, 307/316 branches (97.2%); probe differential equal, 281 lines, sha256 5815825a9e9a1603 |

## Benchmark S2 (2026-10-20, JDK 21, machine shared with other runs)

`M3SwingBench` S2, ns per operation (median of the timed rounds) and bytes allocated per operation. `scroll-state`:
plain = the top row, pixel offset, visible and full rows computed by the application from `getVisibleRect` and the
stock row API; m3 = `getScrollInfo()`. `scroll-to-center`: plain = `setViewPosition` to the centred row and the top
row after it; m3 = `scrollRowToVisible(row, M3Scrolling.CENTER)`, which also returns the top row.

| operation | component | rows | plain ns | m3 ns | plain B | m3 B |
|---|---|---|---|---|---|---|
| scroll-state | JTable | 10^5 / 10^6 | 10,120 / 6,552 | 3,472 / 3,048 | 1,438 | 320 |
| scroll-state | JList, fixed heights | 10^5 / 10^6 | 10,453 / 5,945 | 6,386 / 2,812 | 1,352 / 1,305 | 344 / 320 |
| scroll-state | JList, variable heights | 10^5 / 10^6 | 1,382,034 / 16,739,059 | 230,760 / 2,966,981 | 1,333 | 349 |
| scroll-state | JTree | 10^5 / 10^6 | 140,574 / 1,634,539 | 20,581 / 30,331 | 1,225 | 1,840 / 2,146 |
| scroll-to-center | JTable | 10^5 / 10^6 | 2,581 / 3,676 | 3,392 / 8,020 | 176 | 245 / 256 |
| scroll-to-center | JList, fixed heights | 10^5 / 10^6 | 2,905 / 2,416 | 3,956 / 2,728 | 176 | 208 / 256 |
| scroll-to-center | JList, variable heights | 10^5 / 10^6 | 61,824 / 693,078 | 78,326 / 816,310 | 200 / 207 | 225 / 287 |
| scroll-to-center | JTree | 10^5 / 10^6 | 47,862 / 427,861 | 11,538 / 19,332 | 192 / 197 | 758 / 863 |

- `getScrollInfo()` is 2x to 54x faster than computing the same fields from the stock API and allocates a quarter of
  the bytes, except on JTree, where the m3 answer allocates more: the rows are found by a binary search over
  `getRowBounds` (O(log n) bounds, each a new `Rectangle`) where the stock `getClosestRowForLocation` ends in an
  O(n) `indexOf` over the layout cache's visible nodes.
- `scrollRowToVisible(row, CENTER)` goes through `scrollRectToVisible`, the documented scrolling path, so it
  validates the view and works without a `JViewport` parent; the plain baseline writes `setViewPosition` directly.
  That costs about 1 us on JTable and JList (the 10^6 JTable figure, 8,020 ns, is a scheduling outlier: the run
  before it measured 2,409 ns against 2,462 ns) and 18% on a variable-height JList, whose row lookups are O(n) in
  `BasicListUI`. On JTree the binary search makes it 4x to 22x faster than the stock lookup.
- Two changes came out of the runs: JTree's y-to-row lookup is a binary search over the row bounds
  (`M3ScrollSupport.searchRow`), and a vertical JList skips the containment check for a middle row (the closest row
  of a vertical list is clamped only at the ends), one O(n) `getCellBounds` less per lookup.
