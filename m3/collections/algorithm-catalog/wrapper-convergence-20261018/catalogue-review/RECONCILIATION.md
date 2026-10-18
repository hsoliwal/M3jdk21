<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Upstream wrapper reconciliation at f29b0afe

This read-only supplement compares the original audited baseline `0d73745d7203123533dce8f8fb264ba2746f6d2d` with upstream revision `f29b0afe7ad253cc0562bc8dfdd48fc1acec710e`. Both changed owners were fetched through the GitHub connector at that exact revision and read completely, serially. `M3FilteredLists.java` was also fetched and verified byte-identical to the original baseline. The sealed 139-owner census and all frozen witnesses remain untouched.

**The upstream bounded-range changes address the two frozen numeric boundary examples but introduce a comparator-exception regression in both changed owners.** The other three pending findings remain. The parent workflow subsequently executed the frozen modes against this exact integration baseline; its results, inspected below, agree with the source review. No test or runtime was executed by this reviewer.

## Source identities

Paths are within `m3/collections/src/main/java/com/m3/collections/` in `hsoliwal/M3jdk21`.

| Owner at the new revision | Git blob | SHA-256 | Lines |
| --- | --- | --- | ---: |
| [M3FilteredViews.java](https://github.com/hsoliwal/M3jdk21/blob/f29b0afe7ad253cc0562bc8dfdd48fc1acec710e/m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java) | `6ec7a88dafeca87760faee0c808dbd9a93ba1424` | `1061f6a0c789082c7e86078c7ea3c48b97b804150a7ab96c2ff8e28ff6786369` | 502 |
| [M3MutableFilteredViews.java](https://github.com/hsoliwal/M3jdk21/blob/f29b0afe7ad253cc0562bc8dfdd48fc1acec710e/m3/collections/src/main/java/com/m3/collections/M3MutableFilteredViews.java) | `ef1a9538212a04dc513cde0ccf0b3fe4507438d1` | `66442108ee67cb8395f9e4cb7d26990583a33d2e2775347da15acb5cb3d5b62d` | 566 |
| [M3FilteredLists.java](https://github.com/hsoliwal/M3jdk21/blob/f29b0afe7ad253cc0562bc8dfdd48fc1acec710e/m3/collections/src/main/java/com/m3/collections/M3FilteredLists.java) | `e094fdde4a153eab2d1a125785bc55c68187bbd1` | `b2dc49d3a9ec43b6e6bd34cf5c6dac53057cf90f42cc9ed506d54822a760fc2d` | 395 |

## Five original pending witnesses

| Frozen mode | Source conclusion at f29b0afe | Exact boundary |
| --- | --- | --- |
| `bounded-map` | Numeric witness fixed; 64 checks pass | `M3FilteredViews` lines 420–440 catch range construction refusal and scan the bounded owner in the navigation direction. For the frozen natural/reversed integer order, comparison against each stored key selects the correct visible floor/lower/ceiling/higher result. This conclusion does not establish preserved arbitrary comparator failure behavior. |
| `bounded-set` | Numeric witness fixed; 64 checks pass | `M3MutableFilteredViews` lines 208–227 apply the corresponding recovery to mutable sets and map key-set views. The frozen ascending/descending integer examples now follow valid owner iterators. |
| `interior-null` | Remains | `M3FilteredViews` lines 237–250 still require null to occupy an extreme. For the lawful order `-2 < null < 2`, both extrema are nonnull, so `floor(null)` returns null before checking the predicate; the frozen expected result is `-2`. |
| `readonly-linear` | Remains | The strict list owner is byte-identical, including the inherited ordinary iterator and repeated filtered `size/get` scans. Both the original and fresh integration executions recorded 787,456 predicate calls for 1,024 rows. |
| `readonly-failfast` | Remains | The byte-identical strict list still uses its own `AbstractList` cursor without preserving the backing owner's modification count across `get` scans. The original witness's missing `ConcurrentModificationException` remains explained by the same path. |

The original strict iterator refusal defect also remains upstream and is reproduced by the fresh integration execution. The admitted guard's exact preimage is still present, so its narrow application remains reviewable. The unchanged read-effect oracle passes its eight checks against the fresh baseline. Candidate execution after applying the guard is separate evidence.

## Comparator-exception regression

`M3FilteredViews.boundedEntry` catches every `IllegalArgumentException` thrown while evaluating lines 423–424, including comparator or owner iterator-construction failures, at line 425. `M3MutableFilteredViews.SetView.bounded` does the same at lines 211–213. Neither catch classifies the failure before scanning. The caught exception is never rethrown, and recovery compares stored values to the query instead of preserving the failed comparison sequence.

The existing frozen `comparatorFailureIdentity` witness is sufficient. It builds a comparator-backed map with keys `{2,4,6,8}`, obtains `[2,8)`, arms a comparator that records every call and throws a preconstructed marker only for `(5,2)`, then queries `floorEntry(5)` through the read-only map and `floor(5)` through the mutable key set. The baseline throws the same marker and stops with tape `["5:2"]`.

At the new revision, legal range construction throws that marker; the blanket catch consumes it. Recovery iterates from stored key `6` down to `4` and uses comparisons with the query in the second position. The marker condition is not repeated, and the operation can return entry `(4,4)` or element `4`. Additional comparator calls and predicate evaluation occur after the original failure. This is a source-proven loss of failure identity and stopping behavior, not merely an untested out-of-range example. The recovery's comment describing only invalid bounded-range construction is narrower than the code's actual catch scope.

The frozen `comparator-failure` mode checks the map first and aborts there if it fails. A single failing receipt therefore does not by itself execute the mutable assertion; the mutable regression has a separate source argument above. The compound `readonly` mode also includes these comparator checks after its iterator checks. After applying the guard, an integration failure in that compound mode may therefore expose this inherited upstream regression rather than a guard read-path difference. The frozen oracle must remain intact, and qualification must disclose that distinction rather than claim a full pass.

No blanket recovery atom is admitted by this reconciliation. The original refusal to substitute historical broad catches remains justified. Any further fix needs its own explicit contract decision and same-failure/callback evidence; exact comparator authority cannot be inferred merely from an exception's class.

## Fresh integration execution inspected

The parent supplied `work-m3-wrappers-20261018/integration-f29/BASELINE_WRAPPER_WITNESSES.json`, SHA-256 `7665111043081599f0846a6517162499f214e764e5c73077b5ac7b7512d5cbdd`. The reviewer read this aggregate receipt and every referenced stdout/stderr log. Commands use the original frozen wrapper classes, the parent's fresh f29b0afe target classes, Java `21.0.12.1+1`, and `-Xcheck:jni`.

| Frozen mode | Exit | Observed integration result |
| --- | ---: | --- |
| `readonly` | 1 | First refusal assertion fails: `remove before next` throws `IllegalStateException`. Later checks in this compound mode are not executed. |
| `readonly-effects` | 0 | Eight checks pass. |
| `comparator-failure` | 1 | First map assertion fails with missing `IllegalArgumentException`. The mutable assertion is not reached. |
| `bounded-map` | 0 | All 64 checks pass. |
| `bounded-set` | 0 | All 64 checks pass. |
| `interior-null` | 1 | First assertion fails: `floor(null)` expected `-2`, actual null. |
| `readonly-linear` | 1 | Encounter order check completes; the first work bound fails with 787,456 predicate calls. Later traversal variants are not executed. |
| `readonly-failfast` | 1 | First backing-mutation failure assertion reports missing `ConcurrentModificationException`; later assertions are not executed. |

`RECONCILIATION_EXECUTION.tsv` pins these eight imported outcomes and their raw log hashes. It records execution of the original frozen oracle, not the newly versioned policy gate.

The separately prepared `candidates-v2` pair adds `readonly-policy`, selecting only the existing iterator refusal and preserved-read-effect checks for the JUnit gate. The original `readonly` mode, standalone comparator mode, all methods, and the default main entrypoint remain unchanged. This explicit scope isolation permits qualification of the admitted list guard while retaining the comparator regression as a separately failing witness. It is not a claim that the inherited comparator defect is acceptable or fixed; exact differences and pre/post hashes are in `candidates-v2/WRAPPER_V2.diff` and `candidates-v2/PRE_POST.tsv`.

## Evidence linkage

`RECONCILIATION.tsv` supplies machine-searchable revision-specific rows. Original execution status comes from `BASELINE_WRAPPER_WITNESSES.json` (SHA-256 `379b43b118a77c97a2b6a6c27261cc414110a5dcc7d1e7508f40d6d15c768ce9`) and the sealed `BASELINE_EXECUTION.tsv`; it must not be relabelled as execution at the new revision. The separate integration receipt above provides fresh execution status. The original frozen wrapper oracle remains SHA-256 `47e2082cbf6c45b3f54d330fc0f10abdf58c561ae817e0a37ce4c8b7ee246916`. Candidate execution receipts can be attached as a later supplement without rewriting the original inventory or witnesses.
