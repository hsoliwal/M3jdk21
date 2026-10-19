<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Integrated M3JDK21 collection refinement — source received, qualification pending

The implementation now resides in its existing M3JDK21 Java/JNI source owners.
This PR is stacked on PR1447 at ffc08348874ffcf3037d1c0dcf02f7685945daea and preserves
its integrated java.util.Collections, M3CollectionViews, M3CollectionMasks and libjava.
There is no new runtime layer, required Synexia service or retained per-entry store.

Thirteen serial FILE commits receive exact previously generated public donor postimages:
seven production sources, two existing tests and four added probes. Every target preimage
and every canonical resource Git blob was rechecked before receiving. No source body was
hand-edited. SOURCE_RECEIVING.json records each parent, source path, pre/post blob and commit.
CANDIDATE.patch remains the original review diff; actual ordinary source paths now contain
those exact postimages. The preparation-only receipt is retained as historical evidence.

Changed production owners:
- M3FilteredViews: comparator-admitted interior-null navigation.
- M3LongIndexProjection: stable opposite-direction sorting by equal-value runs.
- M3LongBitmapFilteredSnapshot: binary-search membership on existing sorted indexes.
- java.util.SortedSet and ReverseOrderSortedSetView: recursively preserved comparator metadata.
- m3_index_mask_window.c: bounded relative ordinals and selected JNI row spans.
- m3_state_mask_histogram.c: bounded batching of signed-stride byte states.

Canonical recipe, source seals, fixtures and donor catalogue remain in
[Synexia PR12474](https://github.com/hsoliwal/com.synexia/pull/12474), revision
5e972536d98ac7f4aab37fb66333ed4696584bdd, named recipe
com.synexia.m3.CollectionComposite20261019. Existing Java/text snapshot engines are reused.
This receiving pass transfers exact existing Results by Git blob. It is explicitly NOT
a claim that the new composite Maven/OpenRewrite recipe was executed or qualified.
All per-file Apache/OpenJDK copyright/license notices remain unchanged.

The catalogue records source/contract comparisons by problem category and references
LeetCode/HackerRank/GeeksforGeeks without copying solution/editorial source. tools4j
shortstring stays reference-only because its format differs from unsigned base36 IDs.

The implementation remains an UNQUALIFIED DRAFT. Composite SDK replay/refusal/fixed point,
Java21 compile/behavior, JNI/ABI, resource/performance, formal review, full source-built
M3JDK21 image/jtreg/platform and remaining file/family passes are NOT_RUN.
Earlier donor measurements are not upgraded to proof for this composition.
K7724888064B remains below8589934592B reserve; S18733383680B, native K temp verified.
No local task output was written. Hosted recipe job117937377232 failed before any step.
No global receiving phase, release readiness or completion of the full request is claimed.
