<!-- SPDX-License-Identifier: Apache-2.0 -->
# m3/collections rank-stack follow-up (2026-11-07)

Follow-up of `qualification/rank-stack-restore-20261107` (#2693), closing its first two open items. Both edits are
pinned by `pinned_edits.py` (PINNED_EDITS v1): each names the pre-image blob (the #2693 recipe output: owner
`a32f003a6807` = ADD_ROWS result, contract `284248c40cc2` = retained pr1882 blob), exact old -> new hunks that must
occur once, and the post-image blob; `--check` reports `changed=0`.

| item | file | change |
|---|---|---|
| stale native probe | `M3RankBatchIntoContract.java` (restored test) | `nativeInvalid` probed `boundBatchInto0` mode 0 as invalid; `efa61222bcc` (same pr1882 stack) made mode 0 the in-place lower-bound mode (`mode != 0 && mode != M3_RANK_UPPER && mode != M3_RANK_COUNT`). The probe now uses mode 3 as the invalid mode and asserts that mode 0 writes the lower bounds `[0, 1]` |
| older-library fallback | `M3LongRankBatchNative.java` (restored owner) | the callers of `boundBatchInto0` (`lowerBoundsInto`, `boundBatchInto` behind upperBounds/counts and their Into forms), `rangeCountInto0` and `intersectionCount0` called the native unguarded, so master's 7-export `libm3rankbatch` (opted in) threw `UnsatisfiedLinkError`. They now use the owner's existing per-export pattern (`if (AVAILABLE && !X_ABI_MISSING && ...) { try { native } catch (UnsatisfiedLinkError olderNativeLibrary) { X_ABI_MISSING = true; } }`, as `INTERSECTION_VALUES_ABI_MISSING` and the other restored kernels do) with three new flags `BOUND_BATCH_ABI_MISSING`, `RANGE_COUNT_ABI_MISSING`, `INTERSECTION_COUNT_ABI_MISSING`. Hot path: one volatile boolean read, no allocation; the catch runs once per process |

The #2693 recipe (`../rank-stack-restore-20261107/add_rows.py --check`) now reports the two edited files as changed by
design: its results are this recipe's pre-images. No native code, pom or workflow changes.

## Gates

`GATES.tsv`; `oldlib-gate.sh` (Linux) runs master's library build (blob `97f1b4387f2c`, 7 exports) with the new Java;
the #2693 `native-gate.sh` is re-run unchanged (its STALE row is gone: `M3RankBatchIntoContract` native-required passes).

With master's 7-export library opted in, the `-Pm3-collections-native` lane moves from 12 errors to 7 errors + 2
failures: the three guarded callers' tests (`M3LongLowerBoundWindowTest`, `M3LongRankUpperCountSweepTest`,
`M3LongRankIntersectionTest`) pass; `M3RankBatchIntoTest` and `M3RankIntervalBatchTest` pass every owner-API check
and fail only their final reflective raw-export probe. The 7 remaining errors are contracts that require their own
export whenever a library is loaded (`differenceValues0`, `distinctDifferencePairCount0`, `distinctTripletSumCount0`,
`intersectionWindowCount0`, `unionValues0`, `selectMergedInto0`, `pairSumIndicesInto0`); their owners already fall back
(the digest on the master library equals Java with no `UnsatisfiedLinkError`), so those are test-mode assertions,
outside the three exports of this follow-up.
