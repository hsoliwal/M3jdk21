<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->

# Filtered/sorted stored-representative master convergence — 2026-10-17

**Scope:** Independently licensed `com.m3.collections` product receiver, not `java.base`.
**Branch base:** `master@a918e588dd11d5ff7e0a6d4efaa89bb1679a9798`.
**Prior accepted stacked receiver:** M3JDK21 [#1035](https://github.com/hsoliwal/M3jdk21/pull/1035), based on [#1028](https://github.com/hsoliwal/M3jdk21/pull/1028).
**Canonical first-party recipe:** Synexia [#12009](https://github.com/hsoliwal/com.synexia/pull/12009), pinned branch `m3/filtered-stored-representative-20261017`.

## Mechanical outcome

This is **content convergence** of existing qualified branch/source lineage into the live `master`
branch. The older merged PR #1035 carried ancestry but the live `master` tree still had the exact
older M3FilteredViews and M3MutableFilteredViews sources. Neither donor nor receiver requires
a new wrapper class, collection hierarchy, entry/node owner, public descriptor or JNI ABI.

Input preimages (SHA-256):
- `M3FilteredViews.java`: `0b7f2175dbb34ed087b76a07e93335ee488338d544fd49c332f503acfdd51ca8`
- `M3MutableFilteredViews.java`: `2b3bb42e411202ecb5998f99c7ca34bf5026842ecab2e4c8b64711400c1f4681`

Canonical generated outputs (SHA-256):
- `M3FilteredViews.java`: `fd880291f6119899e530e19b1dc622bb4f1ea51c6c136ab1c510d507c303c761`
- `M3MutableFilteredViews.java`: `bbc218ecd8c87648976501a5f09aa8baa5af94e1ea79fdef1647ac4248f59e65`
- `M3StoredRepresentativeContract.java`: `9686f4d820c5872665d6273faa0de0b327b9689b12696af7827269cb8774e016`
- `M3StoredRepresentativeTest.java`: `b948aca5ae6f14b80027429e6b820800927d3013bebb972fa92b712383af55f3`

The earlier six-file filtered/sorted receiver manifest remains unchanged. The exact successor
`m3/collections/recipe/stored-representative-20261017/manifest.tsv` updates only two
approved source digests and admits two new Java tests. Existing `verify_donation.py` uses
the canonical hash-pinned successor overlay. The exact stored-representative verifier and
refusal tests from #1035 are reused, not reimplemented.

## Recipes, categories, and donors

The **Synexia canonical Maven/OpenRewrite family** uses
`M3HashPinnedJavaSnapshotRecipe` and named recipes:
- `com.synexia.m3.StoredRepresentativeOwnerRepair20261017`
- `com.synexia.m3.Jdk21StoredRepresentativeRepair20261017`
- `com.synexia.m3.Jdk21StoredRepresentativeMetadata20261017`

The canonical searchable algorithm/category ledger is
`synexia-openrewrite-recipes/recipes/m3-filtered-stored-representative-20261017/CATEGORY_REVIEW.tsv`
in Synexia #12009. Target selection rationale:

| Problem category | Existing owner / donor signal | Decision |
| --- | --- | --- |
| Stored representative vs lookup alias | JDK21 TreeSet/TreeMap comparator navigation | Evaluate predicate on actual stored object/key |
| Identity and equals representatives | Identity-backed Set and repeated-equal Collection | Preserve source-owned equality/identity and consider visible stored occurrences |
| Conditional writable filtered Map | JDK Sorted/NavigableMap retained key semantics | Preflight effective stored/new comparator representative before mutation |
| Live view and sorted bounds | Existing M3 wrappers + JDK contract | Preserve source comparator, views, descriptor and mutation boundary |
| Guava filter vs Commons PredicatedSet vs Eclipse select | Different mutation/snapshot semantics | Compare, **do not** conflate APIs or copy foreign source |
| LeetCode / HackerRank / GeeksforGeeks | Problem-category taxonomy only | No editorial or submitted solution source copied |

No native filtering kernel is added. Arbitrary Java predicate/comparator callbacks remain in
Java; existing numerical JNI mask/index primitives remain separate qualified batch mechanisms.

## Execution and admission

Focused target receiver:

```bash
bash m3/collections/qualification/stored-representative-20261017/verify.sh
```

Full `m3/collections` Maven, Java 21 compiler, JUnit, source-inventory, native parity and
appropriate full JDK/JTReg/HotSpot verification are separate gates. The focused script checks
exact SHA-256, executes the actual Java21 source and standalone contract in ordinary and
interpreted mode, and tests refusal of tampered manifests and drift.

**Do not equate** hash identity, historical PR ancestry, an isolated Java21 probe, or
a green donor test with full JDK runtime promotion. `STRING → arrays → collections → AST/compiler`
phase authority remains unchanged: this is an independently scoped collections correction, not
a global phase advance. No rebase/force-push/squash/whole-tree overwrite is authorized.
