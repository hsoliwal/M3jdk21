<!-- SPDX-License-Identifier: Apache-2.0 -->
# m3/collections pr1882 rank-batch stack restored (2026-11-07)

The superset retain merge `d92ed3c91214` (PR #1882) kept master's tree, so the rank-batch stack of PR #1882 (LeetCode
34/35/88, 4, 15, 26/128, 167, 349/350, 532, 658, interval counts, floor indexes, in-place windows) never reached the
live owners; only a byte copy survived under `m3/qualification/master-landing-20261028/retained-variants/pr1882/`.
Census pass-2 C4 (#2616) mapped LC 315 and LC 719 onto master's rank owner and noted that "the rest of the pr1882 rank
stack is outside these rows". Merge-loss audit phase 2 (`CLEAN_0E.tsv`, no ancestry risk) lists that rest as 20 leaves
of `d92ed3c91214` plus four clean rank leaves of other retain merges (26: #2149, 53: #2141, 60: #1515, 62: #1753). This
directory restores them by recipe and records one decision per leaf (`DECISION.tsv`).

| owner | strategy | change |
|---|---|---|
| `M3LongRankBatchNative.java` | 3-way CONFLICT (2 hunks) -> ADD rows | +86 members (+1,086 lines): equal ranges and windows, in-place lower/upper bounds, counts, interval counts, intersection/union/difference values and counts, run statistics, K-diff, merged select, floor indexes, triplet count, closest windows, two-sum, 16 natives |
| `M3LongRankIndex.java` | 3-way CONFLICT (4 hunks) -> ADD rows | +27 members (+288 lines): the public facades of the above and `cachedRunStatistics` |
| `m3_long_rank_batch_jni.c` | 3-way CONFLICT (3 hunks) -> ADD rows | +41 functions/enums (+1,428 lines): 16 JNI exports and their helpers |
| `M3BagRankIndex.java` | 3-way CLEAN (MERGE) + anchor | leaf 26: `occurrenceAt` bisection -> `M3LaneSearch.upperBound` |
| `M3IndexPathRank.java` | 3-way CLEAN (MERGE) + HUNK row + anchors | leaf 26: `selectedSourceOrdinal` bisection -> `M3LaneSearch.upperBound`; leaf 60: low-bit-run fast path |
| `M3LongRangeSet.java` | RECORD only | owner held by session com-synexia-22: leaf 26's range algebra is a HANDOFF row, leaf 53 is PRESENT_EVOLVED |
| 40 test files + `src/test/native/legacy_m3rankbatch.c` | whole files (`RESTORED_FILES.tsv`) | the contracts and JUnit wrappers each origin commit shipped, byte-for-byte from the retained variant |

Existing members are never edited: lost lines inside master's `lowerBounds` / `lowerBounds0` (leaf 2 paging, leaf 38 sweep
call, leaf 40 delegation) stay out and are recorded PRESENT_EVOLVED (identical results, master revised those members in
`bbe5bcfef7b`/`adab56cdc85`).

## Recipe (RESTORE_ADD_ROWS v2)

* `THREE_WAY.tsv`: per owner and step, base = the owner blob at the merge-base of the dropping merge's parents
  (pre-rename path), ours = master, theirs = the retained variant; outcome and hunk count recorded.
* `tools/gen_rows.py` parses top-level members of ours and theirs (keys `name#k` for overloads) and emits each maximal run
  of theirs-only members as SEPARATOR + ANCHOR + MEMBERS rows after the preceding shared member of ours (methods skip back
  over shared fields); origins come from `git blame` of the PR head, mapped to the leaves of `tools/LEAF_ORIGINS.tsv`;
  `tools/HUNKS.tsv` adds the non-member rows (leaf-26 anchors, the leaf-60 statement). `--check` regenerates byte-for-byte.
* `add_rows.py` re-runs every 3-way, checks blob ids and the recorded outcome, builds each owner as pre text + rows
  (slice sha256 prefixes checked), proves "result minus rows == pre" and "member multiset == pre + declared symbols",
  copies `RESTORED_FILES.tsv`, and with `--check` reports `changed=0`.
* `oracle/jdk/m3/collections/RankStackExistingDifferential.java`: digests of the pre-existing API of the four touched
  Java owners (base vs head must be equal). `RankStackRestoredDigest.java`: digest of every restored entry point
  (native vs Java must be equal). `RankStackLegacyAbiProbe.java`: the legacy `equalRanges0` export, which the Java owner
  no longer calls (kept for older consumers since `2b78514275f`), checked reflectively against a Java oracle.
* `native-gate.sh` (Linux): javac -h prototype check, gcc -Werror builds (new, legacy-ABI, master, stub), nm -D, every
  restored contract Java/native/-Xint/-Xcheck:jni, the #2616 contracts on old vs new library, native = Java digests, one
  mutant per restored export.

## Gates

`GATES.tsv`.

## Open items

* `M3RankBatchIntoContract` (restored byte-for-byte) fails in native-required mode at its last probe: `nativeInvalid`
  still lists mode 0 as invalid, but `efa61222bcc` (same stack) made mode 0 the in-place lower-bound mode. The failure
  exists at the PR head; the Java path (default and `-Pm3-collections-native` builds) passes. A one-line contract update
  belongs to the owner lane.
* With an older `m3rankbatch` opted in (`-Dm3.collections.rank.native=true` and master's 7-export library), several
  restored entry points throw `UnsatisfiedLinkError` instead of falling back, as at the PR head: the callers of
  `boundBatchInto0` (lowerBoundsInto, upperBounds/counts and their Into forms), `intersectionCount0` and `rangeCountInto0` have no
  ABI-missing flag (`native-gate.sh` step 4b). Default (no opt-in) and the restored library are unaffected; the fix
  (an ABI flag per caller, as the other restored kernels have) belongs to the owner lane.
* Leaf 26h HANDOFF (M3LongRangeSet range algebra and its three tests) to the M3LongRangeSet owner session; leaf 62 NEC
  (identity-mode rank of PR #1753 needs an owner decision).
* The rank workflows that pin owner blobs (`m3-long-rank-bounds-jni-java21.yml`, `m3-topk-frequency-jni-java21.yml`)
  were already stale on master (#2616 changed the owners); they need a re-pin outside this leaf's scope.
