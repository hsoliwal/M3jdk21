<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Filtered and live wrapper contract repair receipt

This change deliberately repairs observable wrapper behavior in M3JDK21's
existing owners. It is reviewed separately from allocation/JNI refinements.
M3JDK21 remains one integrated SDK with unchanged production public/protected
descriptors. No runtime Synexia layer is introduced.

## Exact repairs

| Owner | Repair and preserved boundary |
| --- | --- |
| `java.util.CollectionViews.SelectionSpliterator` | Save and restore enclosing callback/delivered state in `finally`, so nested traversal cannot overwrite the outer result after delivery. |
| Filtered Set / SortedSet spliterators | Preserve DISTINCT and sorted encounter metadata as guaranteed by the backing owner; retain its comparator through splits. Do not invent SIZED or SUBSIZED or evaluate the filter to answer metadata queries. |
| `M3IndexedLiveView` | Detect explicit invalidation during predicate/comparator evaluation before publishing a prepared projection. Preserve finite read-only callback reentry. |

The new `CollectionViews` cursor metadata costs one integer and one comparator
reference per cursor. This is a correctness repair, not an allocation win.

## Donor comparison and rejection

The PR1770 representative-membership candidate is excluded. Against an
`IdentityHashMap.values()` owner, its equality scan reports an absent external
equal reference present and removes the resident reference. It also converts
backing-owner null/type exceptions into `false`. The independent four-observation
diagnostic detects these changes even though the donor's own 342,009-assertion
test passes. Existing generic `contains/remove` authority is preserved here.

The PR1764 blanket preparation/reentry guard is also excluded. The original
owner allows finite read-only predicate/comparator reentry; the donor guard
throws instead. Only its separately validated invalidation checks are adapted.

Both failures and the donor's passing tests remain in canonical correction
memory. A passing donor suite alone is not proof of the receiving contract.

## Canonical receiving and execution

Canonical owner: `hsoliwal/com.synexia`, existing crate
`synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017`, named
recipe `com.synexia.m3jdk.AstBuildReverse20261017`. Effective engines are unchanged
from `408373f4101fdca2fb3625bec8af1234e5fd3e26`.

The review handoff is on
[`m3/parser-array-convergence-20261021`](https://github.com/hsoliwal/com.synexia/tree/m3/parser-array-convergence-20261021/synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017).
`evidence/runtime/convergence-20261021/publication-work/PRODUCT_FRONTIER.json` seals all exact
source epochs, while `RESULTS.md`, `REPLAY.md` and the retained logs describe the
actual qualification. Product inputs start at
`33d2e6ae8048c436bd55b951d9f7dbfab2f3efcb`; publication preserves master
`7f657d6ca05760af7bee7d4d2d9e14ae91ddd942` after exact affected-path rechecks.

`jdk-view-state-20261021` precedes `jdk-view-metadata-20261021` on the same owner.
`views-live-20261021` refines the independent live-view owner. Each dataset passed
five actual OpenRewrite SDK tests, eight preparation/sealing tests and nine
receiver cases, followed by receiving check, apply and immediate fixed point.

Strict Java 21 compilation and protected-API checks passed. The final received
`CollectionViews` passed the original 77-check bootstrap and 14,700-check
contract, the reentrant regression, 4,005 metadata assertions and all four
backing-owner observations. The old nested-state bug and metadata defect were
observed on the original code. The first narrowed diagnostic compilation omitted
the existing `RunSearchJni` declaration; that failed attempt is retained beside
the corrected verifier rather than relabeled as a product failure or success.

Live-view qualification passed 11,721 checks and finite reentry witnesses with
six predicate and six comparator callbacks. The original code fails the new
invalidation witness; the rejected blanket donor fails the finite reentry
witness. Final collection composition also passed with the separately qualified
native peer, but this repair does not depend on that peer or the resource PR.

## Scope and licenses

The source/test frontier is six disjoint files plus this receipt; no stacking
dependency on the resource PR was found. The java.base evidence uses an explicit
host `--patch-module` qualification. It is not a full source-built M3JDK21,
jtreg/provider-wide, HotSpot or other-platform acceptance result.

The existing `CollectionViews` source remains Apache-2.0 with its existing
core-integration licensing/promotion condition. Its two exact donor regression
tests retain GPL-2.0-only with the Classpath exception and their original notices.
The canonical Apache recipe license does not relicense these payloads.
Live-view source/tests retain their Apache notices and explicit modification
lineage. Donor revisions, source hashes, legal texts and rejected experiments
are recorded in the existing canonical catalogue and per-file provenance ledger.
