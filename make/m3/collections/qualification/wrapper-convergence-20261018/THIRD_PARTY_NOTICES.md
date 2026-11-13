<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Source attribution, donor evaluation and dependency notices

This document records the source actually reused by this crate and the
external implementations consulted as comparison evidence. Newly authored
Synexia source and documentation use Apache-2.0 with the canonical copyright
above. Existing source headers and applicable donor notices remain intact;
this document does not replace them or change their licenses.

The canonical repository's [Apache-2.0 LICENSE](https://github.com/hsoliwal/com.synexia/blob/50f48cbbe11f17fb8dc209bac3812f8940a1e267/LICENSE)
was read at Git blob `29f81d812f3e768fa89638d1f72920dbfd1413a8`.
Exact source and license identities for algorithm comparisons are recorded in
[index-candidates/CATALOGUE.tsv](../../algorithm-catalog/wrapper-convergence-20261018/index-candidates/CATALOGUE.tsv).

## Source reused in the admitted work

### Existing Synexia recipes and scope helpers

The recipe host uses the existing canonical Java source owners at
`hsoliwal/com.synexia@98368e107f4a205d852ef958cd48c1ee633219b1`:

| Source | Git blob |
| --- | --- |
| `M3HashPinnedJavaSnapshotRecipe.java` | `3bdeeaea9a212fe0c7d1f544510f87c4cd49eb40` |
| `M3HashPinnedTextSnapshotRecipe.java` | `9d0788d3717e017d58345c5c3e62022c2915a3c8` |
| `M3ScopeInference.java` | `d24729950508857280ea1b56d4e930f05137ec85` |
| `M3EditScope.java` | `274360816db1e44a2f1796a3bc5cdeff6b0ce6db` |

[ENGINE_SOURCES.tsv](recipe-custody/recipe-host/ENGINE_SOURCES.tsv) retains their complete
canonical paths, immutable URLs, SHA-256 identities and Apache-2.0 licensing.
The canonical host compiles these existing files. The receiving host reuses
the four existing public copies under
`m3/arrays/qualification/empty-segments-20261017/recipe-custody/sdk-owner`;
their hashes match the canonical owners. The Java and text portability
fixtures passed using public compiler source inputs only, with no private
source input, as recorded in the [portability summary](evidence/PORTABILITY_SUMMARY.json)
and [exact evidence bundle](evidence/public-custody-portability.json).
These are attributed verification copies, while canonical ownership remains
in Synexia. They introduce neither a duplicate recipe engine nor a collection
runtime dependency on this proof host or on OpenRewrite. The portability
receipt qualifies those two machinery fixtures, not a re-execution of every
copied production or verification packet.

### Existing M3 host and collection owners

The host POM and `M3TaskRecipeTest.java` were adapted from
`hsoliwal/M3jdk21@0d73745d7203123533dce8f8fb264ba2746f6d2d`, under
`m3/arrays/qualification/empty-segments-20261017/recipe-custody/`.
[HARNESS_LINEAGE.json](recipe-custody/recipe-host/HARNESS_LINEAGE.json) supplies both exact
source paths and hashes. Their Apache-2.0 attribution and the documented
adaptation remain part of their custody even though the current host carries
canonical Synexia SPDX fields.

Production postimages adapt existing Apache-2.0 receiving owners:
`M3FilteredLists.java`, `M3LongIndexProjection.java`,
`m3_index_mask_window.c`, and the verification fixture
`m3_mask_window_regions.c`. Their exact preimages are pinned to the original
target revision above and were verified byte-identical at integration revision
`f29b0afe7ad253cc0562bc8dfdd48fc1acec710e`. Original headers are retained in
the postimages and original preimage fixtures. Applicable module sources are
the [M3 collections LICENSE](https://github.com/hsoliwal/M3jdk21/blob/f29b0afe7ad253cc0562bc8dfdd48fc1acec710e/m3/collections/LICENSE)
and [NOTICE](https://github.com/hsoliwal/M3jdk21/blob/f29b0afe7ad253cc0562bc8dfdd48fc1acec710e/m3/collections/NOTICE).

The receiving module's existing notice is retained here for the adapted
collection source custody:

```text
M3 collection lanes
Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>

Adapted from hsoliwal/com.synexia Apache-2.0 collection owners and the
reviewed segmented-lane recipe candidate. Original Synexia notices are
preserved. See provenance.json for exact source/target hashes and changes.
OpenJDK retains its existing licensing. This separately built library does
not replace java.util classes or integrate into java.base.
```

In that retained notice, `provenance.json` refers to the upstream module's
existing provenance file. This crate's additional identities and decisions are
recorded in its source manifests, donor catalogue, qualification receipts and
canonical provenance ledger.

### Historical stable-group donor

The stable equal-value-group reversal atom comes from
[M3JDK21 PR #1164](https://github.com/hsoliwal/M3jdk21/pull/1164), head
`9b7620f592248e858628c2489866ec73227d519c`, and the matching canonical
[Synexia PR #12156](https://github.com/hsoliwal/com.synexia/pull/12156), head
`0d595c9a1b6578bede85e2a3c9cb5025c91bcefc`. Both historical projection
sources share Git blob `114d6bff738ffeee0c432637538b482ef856e60c` and carry
Apache-2.0 attribution. [HISTORY.tsv](../../algorithm-catalog/wrapper-convergence-20261018/index-candidates/HISTORY.tsv) and
[DONOR_NOTES.md](../../algorithm-catalog/wrapper-convergence-20261018/index-candidates/DONOR_NOTES.md) record the comparison and
current routing/contract reconciliation.

The historical two-word native compaction loop was evaluated and refused for
the current batching and bounded-ABI contracts. Historical wrapper fallbacks
from M3JDK21 PR #1204 were also evaluated under comparator and predicate-effect
constraints. Those rejected alternatives are not selected production donor
bodies in this crate.

## External algorithm comparisons: reference-only

No implementation body from the following external algorithm references was
copied into the three admitted production atoms. They supply problem-category,
contract, complexity and license comparison evidence. A future source
adaptation must make its own recorded donor/notice decision; these reference
entries are not blanket intake approval.

| Reference | Recorded license/source | Evaluation use |
| --- | --- | --- |
| Guava filtered collections and immutable sorted sets, revision `9247d70e1638aa88103f727d2c47c66c4f93fd16` | [Apache-2.0 LICENSE](https://github.com/google/guava/blob/9247d70e1638aa88103f727d2c47c66c4f93fd16/LICENSE), blob `d645695673349e3947e8e5ae42332d0ac3164cd7` | Live filtering, mutation boundaries, sorted-array views and comparator effects; duplicate-set semantics differ from physical index occurrences. |
| Eclipse Collections immutable sorted set, revision `00a668d624c870652bf746d10f250a4d37a777f8` | EPL-1.0 or BSD-3-Clause alternative; verified [EDL license](https://github.com/eclipse-collections/eclipse-collections/blob/00a668d624c870652bf746d10f250a4d37a777f8/LICENSE-EDL-1.0.txt), blob `4e492acbfa2054917ae22b35cf6c03e9b3feff76`, plus project NOTICE | Immutable sorted storage and range metadata; no source selected. |
| Apache Commons predicated collections, revision `2d985c27e07bdcbb9516aea44b0dda3f3e487ced` | [Apache-2.0 LICENSE](https://github.com/apache/commons-collections/blob/2d985c27e07bdcbb9516aea44b0dda3f3e487ced/LICENSE.txt), blob `ff9ad4530f5716304e042cbac72cecd7c40f0e0e`; NOTICE identity in the catalogue | Admission validation differs from a filtered view; replacement refused. |
| Spring internal filtered collection, revision `49adc91c7e41eaafb52fc11e1210989894bda1f3` | [Apache-2.0 LICENSE](https://github.com/spring-projects/spring-framework/blob/49adc91c7e41eaafb52fc11e1210989894bda1f3/LICENSE.txt), blob `ff77379631504c17ad0a94dddc6803f0d3fb0fbb` | Delegate mutation and clear semantics differ from the nominated wrapper contract; replacement refused. |
| Boost dynamic bitset, revision `6e5931fef13f0da404c5066918ce89e4736b8842` | [BSL-1.0 LICENSE](https://github.com/boostorg/dynamic_bitset/blob/6e5931fef13f0da404c5066918ce89e4736b8842/LICENSE_1_0.txt), blob `36b7cd93cdfbac762f5be4c6ce276df2ea6305c2` | Word scanning and tail-mask comparison; no native body selected. |

LeetCode Search Insert Position, Remove Element and Move Zeroes; HackerRank
Lower Bound; and the GeeksforGeeks lower/upper-bound article supply problem
taxonomy and counterexamples. The catalogue links the official problem and
terms/copyright pages. No editorial, submitted solution or article
implementation was copied. Access to those pages is not a source-code license.

## Build and proof dependencies

[recipe-host/pom.xml](recipe-custody/recipe-host/pom.xml) declares OpenRewrite 8.17.1, JUnit
Jupiter 5.10.2 and SLF4J 2.0.13 for the recipe proof host. Existing downloaded
dependency distributions retain their own licenses and notices. The following
entries were inspected directly in the actual dependency archives used by the
workspace:

| Archive entry | License evidence SHA-256 |
| --- | --- |
| `rewrite-core-8.17.1.jar!/META-INF/LICENSE` — Apache-2.0 | `f312c4de3f7a4671fda4bd0c0062fe89001cce61429cc7e8c2bebc9218e7ff7f` |
| `junit-jupiter-api-5.10.2.jar!/META-INF/LICENSE.md` — EPL-2.0 | `5aa4cd44c111add178d1c2e2fe36d58a484012c80167df925f826cd64d411bf0` |
| `junit-jupiter-api-5.10.2.jar!/META-INF/LICENSE-notice.md` | `c7d843e0faf8a251278c9220f6b5326a42a01f08b855c1ec871bcef6211a7e2a` |
| `slf4j-api-2.0.13.jar!/META-INF/LICENSE.txt` — MIT-style permission notice | `4e7f90c86ab51278228bce153122f1d8df30149d13ce9ef524c8444a84c32dcc` |

This is evidence for those inspected entries, not a new exhaustive license
bill of materials for every transitive toolchain artifact. Binary build caches
and compiled output are not source donations by this crate. The receiving
module's separate source-owned JUnit bootstrap and its existing upstream
notices are documented in [DEPENDENCIES.md](recipe-custody/recipe-host/DEPENDENCIES.md);
that bootstrap is distinct from the host's Maven coordinates.

## Frozen inputs and final provenance

The original contract, catalogue/notes and superseded v1 proof fixtures are
retained as exact historical qualification inputs where their original hashes
are bound by the recorded receipts. Their explicit `PRESERVED_HISTORY` ledger
rows retain that truth; they are not relabeled current authored documents to
avoid header requirements. Current authored documentation uses the canonical
SPDX fields, and current wrapper qualification names the version 2 sources.

The current [README](README.md) records completed source, SDK, receiving,
runtime and public fixture qualification, with their exact scopes and retained
evidence limits. The authoritative changed-path provenance result and bound
scope are recorded in [provenance/VALIDATION.json](evidence/PROVENANCE_VALIDATION.json);
this notice does not replace that gate. Publication commits and PR references
will be recorded separately when available. Existing donor notices, frozen
input identities and final authored source identities remain subject to their
recorded canonical provenance custody.

Public receiving edition: links above point to the public custody and catalogue. The canonical notice is retained at [the published source commit](https://github.com/hsoliwal/com.synexia/blob/528aaf79cfae79c5dbfe7c307340329d320e8c44/synexia-openrewrite-recipes/recipe-crates/m3-wrapper-convergence-20261018/THIRD_PARTY_NOTICES.md). Its source SHA-256 is 988e18f9ff0029649d098e8f74b9b18ebbd6747ea53ec7f6a3aeacbe456351c7. Original notices and source headers remain intact.
