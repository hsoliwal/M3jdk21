<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 table sorted-index mutation refusal — exact source-first receiver

Canonical Synexia [recipe PR #12241](https://github.com/hsoliwal/com.synexia/pull/12241)
and pinned source commit `895b6f15a8e49dfc96ccd649ad569e2fd50338da`.
The source of truth is `com.synexia.m3.Jdk21TableSortGuard20261018`,
reusing incumbent Maven/OpenRewrite `M3HashPinnedJavaSnapshotRecipe`
with full Java source templates, SHA256 pre/postimages, two-cycle fixed-point,
source-drift and occupied-path refusal. `manifest.tsv` here is an exact
product output receipt, not an alternative runtime recipe.

Only `M3IndexedTable.IndexView.sortedByValue` is refined. After each Java
Comparator callback, the existing revision guard must run before selecting
the next index. A final revision check precedes publishing a new `IndexView`.
A mutating comparator that deletes/recycles a slot or replaces an existing
value now fails at the sort call. Stable integer permutations, comparator
exception identity, distinct row/column payload/slot ownership, public
methods and JNI symbols remain unchanged.

Original product preimage git blob: `5dc062e138b273382b4f161fb4c9869d90558390`.
New canonical template Git blob: `ef65261a642da8e473763d57905297d3e0dc5c2c`.
Qualification paths: three exactly sealed target files including first-party
Java21 `M3IndexedTableReentrantSortContract` and JUnit wrapper.
Existing `M3IndexedTableSelfTest` and C17 JNI mask selector are not edited.
The recipe-owned algorithm catalogue is copied below for public review.

**DRAFT / HOLD** until source Maven/OpenRewrite, real Java21 JUnit, original
JNI parity and installed-JDK evidence are observed. GitHub PR creation, source
hash sealing and passing target tests are distinct acceptance gates.
