<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 additive entryless collection Java/JNI wave 5 — source contract

2026-11-03. This is a **DRAFT/HOLD** integration branch only, created from receiving M3JDK21 superset [#2351](https://github.com/hsoliwal/M3jdk21/pull/2351), frozen head `9c70580bf46610ebe07f059be48f481daf6bd53b` and `master@ce005d25b29fb6246e013bffb462afd5ed070362`. Documentation precedes any second-parent source merge. No default-branch write is authorized by this source-admission step.

## Exact donor review

Donor [#2342](https://github.com/hsoliwal/M3jdk21/pull/2342) source head `bc032a71a737ac1e9410cca059477cbb5bb39d8e` is based on exact frozen master. Complete donor diff is 14 paths: ten additions (source-sealed documentation, workflow, original JUnit/differential tests), four modified existing owner sources (`M3FilteredViews.java`, `M3TreeMap.java`, `M3IndexMaskCompactor.java`, `m3_index_mask_compactor.c`), zero removed/renamed. All modified paths except `M3IndexMaskCompactor.java` have receiving Git blob identical to the frozen master preimage, and unchanged ordered public declaration lines.

The single changed-path collision with #2351 is `M3IndexMaskCompactor.java`. Its receiving tree adds six private empty-result short-circuit branches and a documentation clarification; the donor adds a distinct `selectPositionsPage` API/Java oracle, a lazy optional JNI page-ABI probe and private native declarations near line 110. The independent original per-file Git patches are in nonoverlapping hunks (receiver hunks near lines 25, 169, 238, 312, 335, 387 and 429; donor hunk near line 110). This is a **candidate mechanical three-way merge**, not evidence of tests or binary compatibility.

## Locked contracts and proof method

1. Open a dedicated integration branch from #2351, publish this exact donor specification, and then create one temporary GitHub transport PR targeting only this integration branch. Freeze exact donor SHA; require GitHub `mergeable=true` and original branch head ref, use non-squash native `merge` method with expected-head SHA.
2. After the real two-parent merge, compare the exact prior-receiver-to-merge delta to master-to-#2342: 14 changed filenames/statuses (10 added, 4 modified, zero missing/extra/removals). Verify original borrowed-token Java owner, packed JNI codec C source, and existing #2351 source are unchanged. For `M3IndexMaskCompactor.java`, verify **both** receiver's six empty-result fast paths AND donor's new position-page Java/native code are present; preserve original public declarations and existing native ABI symbols.
3. Explicitly classify new public `selectPositionsPage` and new optional `positionsPageAbiNative`/`selectPositionsPageNative` as **additive, not yet ABI-qualified**. A legacy JNI binary must remain loadable with the new path unused; a caller that requests NATIVE_REQUIRED must fail closed instead of silently falling back. The original `Map.Entry` and `entrySet` externally observable semantics, mutation epochs, ordering, comparator, object ownership, serialization, source/classfile/API compatibility and exact mask/index semantics remain locked.
4. Retain the donor's complete original Git ancestry and non-executable source-sealed provenance data. Record exact Git parents, tree, merged path proofs and source blob witnesses; open a final draft PR to master. Do not merge to master until actual Java 21 compiler and Maven/OpenRewrite SDK/JUnit, native C17 JNI ABI and legacy/current library differential, full reactor, platform tests and benchmarks pass.

LeetCode/HackerRank/GeeksforGeeks are problem-category reference and test sources only, not source-copy authority. The implementation donor is the pinned, first-party source in #2342 and original canonical Synexia/JDK21 recipes; no external challenge solutions are imported.

**STOP on any conflict or overwritten hunk.** Git hash/source custody is necessary but insufficient to prove Java/JNI runtime semantics. This is one bounded leaf; it does not claim all remaining `hsoliwal/*` PRs have been exhaustively merged.
