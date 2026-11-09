<!-- SPDX-License-Identifier: Apache-2.0 -->
# Current M3JDK21 collection source inventory (2026-10-29)

**Bound source identity:** `hsoliwal/M3jdk21` `master` commit
`052ebcf7a26352c0285251eb671ee6c3640fd3ff`.
Subtree `m3/collections/src/main/java/com/m3/collections` Git tree
`80a108bbb7dfbf9398625701952495b5671750d6` (298 direct Java blobs),
plus `module-info.java` in `src/main/java` (one Java blob).
Both direct tree listings were complete and nontruncated.

`SOURCE_DELTA.tsv` contains one row per **299 Java source files**, with the exact
observed Git blob, size, Oct 21 evidence where available, per-file change classification,
and path-level ownership of the specified active collection PRs. It is an
**inventory**, not verification of behavior, performance, or feature parity.

## Exact comparison with previous complete source review

Previous source ledger: [all-collections-20261021](../all-collections-20261021/README.md)
(270 Java files, 104 provider families, 25 challenge references as of Oct 21).
Current baseline: **299** Java files.

| Classification | Files | Meaning |
| --- | ---: | --- |
| UNCHANGED_SINCE_20261021 | 178 | Git blob unchanged |
| CHANGED_SINCE_20261021 | 92 | Git blob differs; re-read contract before recipe |
| ADDED_SINCE_20261021 | 29 | Not in Oct 21 source catalogue; pending source review |
| Removed | 0 | No 2026-10-21 path disappeared |
| Total | 299 | Includes the Java module declaration |

These classifications are exact blob comparisons, **not** a claim that 299
files have been refactored, optimized, or qualified. Existing Oct 21
role/priority/decision columns are historical, not automatically re-certified.

## PR overlap ledger and selection

Open PR owners recorded only when their **changed-file list** includes that
exact current production source. The scoped PRs queried were:
#2115, #2117, #2123, #2127, #2129, #2132, #2134, #2135, #2136 and this
branch #2141. A missing PR owner means no match in this reviewed set,
not proof there are no other branches or unmerged commits.

- #2117 and #2127 both edit `M3LongLongHashMap`: compose both atoms in a
  pinned receiving source rather than accept one branch over the other.
- #2129 owns `M3IntArrayList` and `M3LongArrayList` prefix compaction.
- #2134 owns `M3StateMaskArrays` prefix compaction.
- #2135 owns primitive priority-queue heapification.
- #2123 owns `M3IndexedLongList`; #2136 owns `M3LongInterval`.
- #2141 changes only `M3LongRangeSet` production logic using the
  sealed canonical Synexia #14106 bisection recipe.

File-level catalogue to recipe execution: check Git blob → classify the
collection contract/representation → select existing shared array-index-mask
kernel → compare LeetCode/HackerRank *technique* and source-licensed donors
→ qualify exact OpenRewrite source transform → Java21 compiler/JUnit and
JDK/JNI gates → append-only receiver PR. Never silently rename a published
class, change its binary descriptor or make a challenge's narrow constraints
the generic collection contract.

**Status:** inventory refreshed, source modifications restricted to
`M3LongRangeSet` in this PR; new path reviews, name migration, performance
proof and whole-repo completion remain future qualification work. Do not
merge on inventory evidence alone.
