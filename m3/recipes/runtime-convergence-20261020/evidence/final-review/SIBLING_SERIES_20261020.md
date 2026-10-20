<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Sibling runtime-convergence series: read-only comparison, 2026-10-20

## Decision

The newly discovered sibling series contains useful, separately published work and must be cross-linked rather than recreated or attributed to this packet. It is **not wholly disjoint** from this packet once its parent PR is included. Preserve each series and its evidence. A future combined receiving needs a new canonical per-file refinement with explicit conflict choices and fresh runtime/consumer qualification; neither existing receipt establishes that combination.

The current packet's identity is `com.synexia.m3jdk.RuntimeConvergence20261020`, owned under `synexia-openrewrite-recipes/recipe-crates/m3-runtime-convergence-20261020`. The sibling identity is `com.synexia.m3.Jdk21RuntimeConvergence20261020`, under `synexia-openrewrite-recipes/recipes/m3jdk21-runtime-convergence-20261020`. Their names, directories and principal refinements are distinct despite the similar titles and date.

## Observed PR stack and exact pins

| PR | Observed head | Base or role | Published scope inspected |
| --- | --- | --- | --- |
| [Synexia #12599](https://github.com/hsoliwal/com.synexia/pull/12599) | `48a465a9ce446bdf17e964e2d7ddd10a8188cd41` | Canonical sibling recipe | Seven composed receiving crates for 29 Java/build targets, plus a separately excluded original-Spring donor crate; catalogue, source seals, proof and provenance. |
| [M3JDK21 #1538](https://github.com/hsoliwal/M3jdk21/pull/1538) | `a02f0d912ff9da60a5dbc0a7832d7f4b56260fa0` | Product parent, originally based on `d6d76b563050fc13c4fa79bc494edf29f1614b37` | 15 changed files, including filtered-list/view changes, stable primitive sorting, JNI span transfers, source-JUnit qualification, and a `java.util.SortedSet` refinement. |
| [M3JDK21 #1598](https://github.com/hsoliwal/M3jdk21/pull/1598) | `95f7b5da6daf9b10ca345b21def2cc597eb0b35f` | Base is #1538's head | Seven additional files: COW filtered bulk mutation, indexed Eclipse bundle sorting/windows, and their fixtures. |
| [M3JDK21 #1599](https://github.com/hsoliwal/M3jdk21/pull/1599) | `f8271125fc6c91ee09f92496f3a16f6c27a2bd10` | Base is #1598's head | 22 additional files: module-resource scopes, real Spring/Equinox Connect consumers, native JMOD verification and supporting fixtures. |

All four PRs were observed open, draft and unmerged, and their heads were unchanged when re-read after file-list retrieval. This is a dated observation, not a prediction of future state. `SIBLING_PR_REVIEW_20261020.json` records the API observations, exact product file lists, manifest comparison and Git blobs. All six pages of the canonical PR's file list were read: 540 unique paths, consisting of 538 under its distinct home and two shared central metadata files.

## Direct and inherited overlap

#1598 itself intersects this packet's 29 receiving paths at one test file: `m3/collections/src/test/java/com/m3/collections/M3IndexOrderWindowTest.java`. Both series add that FQCN from ABSENT, with different bytes. This packet's bridge calls the Java oracle; the sibling bridge chooses `--native` when the native owner is available. This packet's owning qualifier runs `M3MaskWindowCurrentProof` and other required-native fixtures, but does not invoke this exact `M3IndexOrderWindowContract` in native mode. Preserve that distinction: existing JNI evidence is substantial, while an explicit run of this particular oracle in both modes is additional evidence. The duplicate file also requires an explicit future source choice and replay.

#1599 has zero direct intersections with this packet's 29 paths. Its inherited parent series still matters: #1538, #1598 and #1599 together change **44 distinct product paths**, with **10 intersections** against this packet. Four overlapping postimages are byte-identical and six differ:

| Shared target | Sibling introduction | Exact relation and future treatment |
| --- | --- | --- |
| `M3LongIndexProjection.java` | #1538 | Identical postimage (`4191858c5aa6e0c1961376e2724fccef0a1d6bcb`); preserve one existing owner. |
| `M3FilteredListMutationProof.java`, `M3FilteredListMutationTest.java`, `M3IndexOrderWindowContract.java` | #1538 | All three postimages are identical to the current packet's corresponding donor fixtures. Reuse the exact files. |
| `M3FilteredLists.java` | #1538 | Different output. The sibling uses the earlier forward cursor probe; this packet also addresses bounded/exhausted/empty provider invalidation with retained regression evidence. Do not replace the repaired owner with the earlier candidate solely because a stack exists. |
| `M3FilteredViews.java` | #1538 | Different reversal/refusal implementation. The sibling also modifies `java.util.SortedSet`; its composed closure differs from this packet's unchanged JDK owner. Compare provider/JDK closure and retained null/navigation/reversal fixtures before composition. |
| `m3_index_mask_compactor.c` | #1538 | Different native algorithms. The sibling narrows each payload transfer to the span between the first and last accepted ordinal in a chunk. This packet shares private window kernels and skips wholly rejected payload chunks. Preserve both evaluations pending a measured, contract-checked combination. |
| Owning collection `verify.py` | #1538 | Different proof composition. The sibling adds span/projection/null-navigation fixtures; this packet adds fresh compiler-output guards and other exact probes. Combining production sources requires preserving applicable proof coverage and source-freshness controls. |
| Source-JUnit launcher `verify_source.py` | #1538 | Different bytes. Both fix repository-relative source custody and receipt fields; the sibling additionally reads the licence source through the manifest. This packet's pinned manifest currently names `LICENSE`, so the observed difference is not itself a failing current input. A composed successor still requires exact seals. |
| `M3IndexOrderWindowTest.java` | #1598 | Different ABSENT additions and backend-selection behavior, as described above. |

The exact before/after blobs are in the JSON comparison. At #1598's head, `M3FilteredNavigableSet` and the Java `M3IndexMaskCompactor` still equal this packet's original preimages; `M3LongIndexProjection` equals its postimage. Other overlapping source states are neither admitted preimage nor postimage. The unchanged receiver must refuse those drifted inputs rather than overwrite them.

The sibling span fixture `m3/collections/src/test/native/m3_compactor_spans.c` explicitly asserts minimal enclosing row transfers. Skipping empty chunks alone does not satisfy that stronger work-count assertion. No benchmark or composed native run was performed in this read-only review, so the correct catalogue disposition is **DEFER_COMPOSITION_FOR_EXACT_COMPARISON**, not a fabricated rejection, speedup or supersession claim.

## Framework and canonical proof implications

#1598 changes `M3EclipseBundleSnapshot` and adds `M3EclipseBundleOrderContract` and `M3EclipseBundleOrderTest`. This packet's framework receipt currently seals a ten-file Java inventory and expects its actually tested 21-test result. The sibling adds sources and changes one sealed dependency; future combined source seals, exact test discovery and Equinox qualification must be regenerated. The current framework gate's refusal on that unqualified combination is expected.

#1599 uses real Spring 6.2.2 and Equinox 3.23.100 for its separate consumer oracle. This packet's framework proof uses Equinox 3.24.300. Neither version-specific consumer result should silently substitute for the other. Its module-resource and JMOD work is useful existing scope; it does not make this packet's full modified-JDK build pass or establish GUI/PDE/platform completeness.

On the canonical side, both PRs append to `docs/M3-SCALE/provenance/M3_FILE_PROVENANCE.tsv` and `synexia-openrewrite-recipes/CANONICAL_RECIPE_HOME.tsv`. The task directories and recipe names do not collide. Future metadata reconciliation must retain both exact append-only suffixes and rerun the unchanged provenance gate against the real resulting commit. Do not replace one complete ledger with the other's older parent-derived copy.

## Evidence limits and publication wording

This supplement inspected current PR metadata, every canonical/product changed-file list, six shared production source blobs, and the relevant patches/native fixture. It did not check out or combine the sibling branches, change any canonical or receiving source, rerun the sibling suites, or certify the PR authors' reported test totals.

Cross-link the sibling series as separately published related work. Describe this packet by its concrete filtered-provider, progress callback, activator lifetime, Java/JNI kernel and qualification refinements. Preserve the original #1538 ancestry and the later #1598/#1599 work; do not present a second broad PR as replacing them. Any later union remains an explicit, source-sealed, qualified refinement with the global String stage and other outstanding gates unchanged.

