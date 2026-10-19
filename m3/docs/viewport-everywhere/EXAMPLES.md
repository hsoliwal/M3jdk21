<!-- SPDX-License-Identifier: Apache-2.0 -->
# Viewport collections: runnable examples

Each example runs standalone, without any UI, against the built `m3/collections` classes. It checks its own result
and fails with an `AssertionError` when the answer is wrong. The one exception is the last example, which also opens a
Swing window unless it is run with `--check`.

Run one example (single-file source launcher):

```
java -cp m3/collections/target/classes m3/collections/examples/viewport/WindowViewExample.java
```

To run all of them: the agent script `run-examples.sh` in the viewport-everywhere scratch (results 2026-10-19: all 7 OK;
outputs under `S:/m3-scratch/viewport-everywhere/examples`).

| Example | Collection(s) | What it shows | Recorded output (2026-10-19) |
|---|---|---|---|
| `WindowViewExample` | M3WindowView, M3WindowPlan | a window over a virtual sequence of 10^8 rows (nothing stored); state masks follow rows on a move; a window of a window | scroll by 2: `retiredHead=2 insertedTail=2`; inner window `[row 50000010, row 50000011]` |
| `FlatteningViewExample` | M3FlatteningView | a computed 10^6-node tree (1,000 x 1,000) with expand/collapse | 500 roots expanded: `rows=501000 materialized=500` in 29 ms; collapse: `rows=500000` |
| `ListDiffExample` | M3ListDiff, M3Patch | snapshot A to B as remove/insert runs and moves, applied to a plain list | `remove 2 at 3; remove 1 at 1; insert 1 at 1; insert 1 at 3; insert 2 at 5`, result equals B |
| `RangeSetExample` | M3RangeSet | selection algebra over 10^9 rows: select-all, range, invert, shifting insert/remove | `selected=999999980`, first range `[11, 999999991)` |
| `MaskAndSortExample` | M3BitMask, M3PermutationIndex, M3RadixKey | a filter mask over 10^6 rows (popcount rank, lowest-set-bit compaction) and a two-key LSD radix sort of the kept rows | `visible=333342`, sorted in 141 ms |
| `CacheAndExtentsExample` | M3SparseExtents, M3BoundedCache | variable row heights over an estimate (Fenwick offsets); a CLOCK cache of row renderings | `y(2000)=40025 rowAt=2000`; cache `misses=200 hits=400` |
| `ViewportFromCollectionsSwingExample` | all of the above | a viewport assembled from collections driving a Swing JTable: flattening -> mask filter -> permutation sort -> range-set selection; the table model only maps rows | `tree rows=100100 visible after filter=85785` |

The SWT counterpart of the last example (an SWT Table driven by the same views) goes into the SWT home
`examples/org.eclipse.swt.snippets/.../m3` together with the `org.eclipse.swt.internal.m3.collections` package
(`COLLECTION_EXTRACTION.tsv`, two homes).
