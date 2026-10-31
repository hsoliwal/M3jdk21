# M3JDK21 integrated SDK — exact per-file hunk superset review (2026-10-21)

This is a source-integrated candidate inside the original M3JDK21 Java21/HotSpot/JNI SDK, **not M3 + JDK**. All operations occurred on the isolated `integration/m3jdk21-source-superset-wave-20261021` branch, using immutable GitHub source Git blobs and original PR patch hunks; **master remains `2779aba3f98f31c324be43f731be7914e1a05935`**.

## Source materialization in this wave

- **28 distinct changed files, 43 exact source hunks:** each was applied against the frozen master file, and the resulting UTF-8 text was independently compared to the donor PR head's exact file text. Original Git blob IDs are recorded in `RECEIVING.tsv`.
- **Full two-parent Git source merges**, exact first- and second-parent readback, for PRs #1618 (3 hunks), #1620 (5), #1629 (19), #1633 (4). Their effective tree changed at every merge, and none of these file sets overlaps another admitted PR in this wave. Git blob postimages are original donor blobs; no hand changes to generated target Java/JNI source.
- **Partial #1621:** 12 added files (12 hunks) reproduced exactly and were added by an ordinary one-parent candidate commit. This is **NOT a merge of original PR #1621**; its changed pre-existing `m3/docs/synexia-recipe-ownership-classification.tsv` carries a no-final-LF-to-LF transition and remains explicitly held in `HELD_HUNKS.tsv`. No second parent for #1621 was used and the original PR remains open.

## Existing recipe and superset authority

Use canonical Synexia `m3-pr-stack-preservation`: parent-first PR chronology, `GitHubPatchMboxGate`, `GitHubPatchHunkChronology`, source-hash-pinned per-file preview `ExactFileHunkReplay` (review PR https://github.com/hsoliwal/com.synexia/pull/12662), `M3PatchHunkRecipe`, `StackConflictResolver`, `IndependentHunkUnion`, `MergeCodeLossGuard`, and `MergeReadback`. The reusable repair recipe must be amended for the exact non-LF file before replaying that held hunk. Original public Java/JNI contracts, callback order, memory ownership and ABI must remain intact.

## Proof and hard holds

The per-file patch replay on the **five original donor PR heads** was executed in the GitHub source toolflow against the old master text and matched exact donor bytes for all 28 admitted paths. Commit parent arrays/tree IDs were checked after each staged Git ref update. Original GitHub PR branches were not modified. The live M3JDK21 master branch was not updated by this wave.

**NOT RUN on the combined staging tree:** OpenRewrite/Maven named recipe/JUnit suite, Java21/HotSpot `make images`, native C/C++ and JNI builds, jtreg/JCK, differential collection tests, full CI/ABI/regression/performance. The source tree is therefore **UNVERIFIED_PRODUCT_CANDIDATE**; it must not be merged to protected master or declared fully completed without gates. Many other open PRs and overlapping hunks remain outstanding in the earlier 482-PR inventory (https://github.com/hsoliwal/com.synexia/pull/12647); this bounded wave makes no all-PR completion claim.

The exact trace per PR/source path is in `RECEIVING.tsv`, the held source in `HELD_HUNKS.tsv`. All source production changes still require qualified canonical recipe application and exact current-head readback before default-branch promotion.
