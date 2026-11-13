<!-- SPDX-License-Identifier: Apache-2.0 -->
# Census group G3: linked-index spliterator (L087), record only

Census leaf L087 (crate `m3-linked-index-spliterator-20261019`, com.synexia develop `ca1aafc4d94f`) proposed 39 changed
lines on `M3SlotSpliterators` (PARTIAL, 23/26 lines present) and 2 on `M3SlotArrays` (PARTIAL, 11/12). Both files are
listed in the crate's `parse-context.tsv` (`path sha256 template`: the parser context the hash-pinned recipe pinned on
2026-10-19), not in its `manifest.tsv` (the three postimages). The census treated every loose `.java.txt` as a
postimage, so the rows are context snapshots that the owners have since evolved past. No source change is made.

| row | verdict | evidence (owner on the base) |
|---|---|---|
| `M3SlotSpliterators` | SUPERSEDED_BY_OWNER (parse context, not a postimage) | the 3 snapshot lines absent from the owner are the older eager-comparator forms (`withComparator(source, comparator)`, `Comparator<? super E> comparator` parameter, `getComparator() { return comparator; }`); the owner keeps a lazy `Supplier<Comparator>` (SortedSet default comparator lookup) and adds `lateBoundOrdered` (late-binding ordered spliterator that does not call `size()` before `findFirst`) and `sorted(SortedSet)`: the snapshot would remove those |
| `M3SlotArrays` | SUPERSEDED_BY_OWNER (parse context, not a postimage) | the 1 absent line is `Math.max(8L, old + (old >> 1) + 1L)` with int arithmetic; the owner computes `(long) old + (old >> 1) + 1L` (no int overflow before the clamp) |
| `M3LinkedList` (manifest postimage) | ALREADY (APPLIED_SUPERSET 58/58) | `LinkedIndexSplit` cursor present |
| `M3LinkedIndexSplitContract`, `M3LinkedIndexSplitTest` (manifest postimages) | ALREADY (APPLIED, byte-equal after normalisation) | `M3LinkedIndexSplitTest`: Tests run 1, Failures 0, Errors 0 in the module run on base `feae0743e6c2` (census group G2 baseline) |

Census lesson (RECIPE_MEMORY.tsv): read `parse-context.tsv` before ranking a loose `.java.txt` as a postimage.
