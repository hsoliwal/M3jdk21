<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Wrapper, index and JNI algorithm catalogue

This catalogue records the completed three-atom refinement series against the **tested integration base `f29b0afe7ad253cc0562bc8dfdd48fc1acec710e`**. It preserves the original discovery records and their statuses, then adds an explicit [accepted-atoms table](ACCEPTED_ATOMS.tsv). The first target pull request contains the strict read-only iterator repair. The stable index-order and JNI row-transfer changes belong to its **companion index/JNI pull request**.

The [qualification overview](../../qualification/wrapper-convergence-20261018/README.md) and [current acceptance record](../../qualification/wrapper-convergence-20261018/ACCEPTANCE.json) describe execution for the series. A file inventory or a historical candidate label does not qualify an implementation.

## Searchable records and revision boundaries

| Record | Exact scope |
| --- | --- |
| [Original owner catalogue](catalogue-review/OWNER_CATALOG.tsv) | **139 owners: 129 Java and 10 C**, pinned to `0d73745d7203123533dce8f8fb264ba2746f6d2d`. Five wrapper bodies were fully read for stated operation scopes; 134 owners remained inventory-only in that catalogue. |
| [Integration census](catalogue-integration-f29/OWNER_CENSUS.tsv) | **143 owners: 133 Java and 10 C**, pinned to tested f29. Relative to the original census: 131 unchanged bodies, eight changes, four additions and no removals. This is not a census of later master revisions. |
| [Integration source reads](catalogue-integration-f29/SOURCE_REVIEW.tsv) | Ten complete source reads with scoped discovery: six changed non-wrapper owners and four additions. Two changed wrappers link to their separate review; the other 131 owners remain inventory-only in this overlay. No transformation was admitted by that reading alone. |
| [Donor/candidate comparison](index-candidates/CATALOGUE.tsv) | Sixteen problem-category rows covering stable indirect order, ordered compaction, live/validated wrappers, sorted storage, word masks and bounds. Source/license pins and rejected alternatives are retained exactly. |
| [Family catalogue](catalogue-review/FAMILY_CATALOG.tsv) and [provenance links](catalogue-review/PROVENANCE_LINKS.tsv) | Existing family, ownership, mapping and recipe references. A discovered donor pointer or family classification does not establish API substitutability. |

Java owner counts include module/package descriptors. Test owners, optional interop modules, qualification mirrors and java.base are outside these censuses. Old statements such as `ADAPT_PROOF_REQUIRED` or “parent execution pending” identify their original review phase. They remain unchanged; [ACCEPTED_ATOMS.tsv](ACCEPTED_ATOMS.tsv) and the current acceptance record provide the later disposition.

## Exactly three accepted production atoms

| Atom | Result and preserved boundary | Series part |
| --- | --- | --- |
| `readonly-iterator` | `M3FilteredLists.StrictReadOnlyList` always refuses ordinary-iterator removal with `UnsupportedOperationException`. This is a disclosed correction to the owner's stronger existing read-only contract. Reads still use the original cursor and its callback effects; one guard wrapper is allocated per traversal. | Wrapper PR |
| `stable-order-groups` | Reuses first-party group reversal for the opposite direction of an already sorted projection. Equal-value occurrences remain in their original order. Current address-kernel access and the unknown-order merge path remain authoritative. | Companion index/JNI PR |
| `native-row-suffix` | Starts row-ID transfer at the already known first match and adjusts the local index. State reads, output batching, bounds, cursor packing, validation and both JNI exports retain their existing paths. | Companion index/JNI PR |

The accepted-atoms table gives full pre/post SHA-256 values and the exact source path for each atom. Qualification of these three operations does not qualify every operation in their source files.

## Donor reuse and refused substitutions

The selected stable-group donor is the public [M3 #1164 history](https://github.com/hsoliwal/M3jdk21/pull/1164), with [projection blob at `9b7620f5`](https://github.com/hsoliwal/M3jdk21/blob/9b7620f592248e858628c2489866ec73227d519c/m3/collections/src/main/java/com/m3/collections/M3LongIndexProjection.java). [HISTORY.tsv](index-candidates/HISTORY.tsv) records its equality with the historical canonical Synexia template, while [DONOR_NOTES.md](index-candidates/DONOR_NOTES.md) explains the contract comparison.

Historical two-word native compaction was refused because it loses the current cross-region output batching and predates the bounded surface. Guava, Eclipse Collections, Commons Collections, Spring and Boost remain pinned comparison references; no external implementation body was copied for these changes. LeetCode, HackerRank and GeeksforGeeks supplied problem categories, with their problem/editorial material treated as non-code references. Applicable notices are retained in the [series attribution](../../qualification/wrapper-convergence-20261018/THIRD_PARTY_NOTICES.md).

## Unresolved wrapper findings

[RECONCILIATION.md](catalogue-review/RECONCILIATION.md) and [its execution table](catalogue-review/RECONCILIATION_EXECUTION.tsv) separate original and f29 observations. The f29 numeric bounded-map and bounded-set witnesses pass, while the broad recovery still loses comparator failure identity in the reproduced map witness. The mutable counterpart has a separately recorded source argument; the compound runtime witness aborts before its mutable assertion.

Interior-null navigation, ordinary-iterator predicate work and backing fail-fast witnesses remain unresolved. The admitted removal guard does not repair those paths. The versioned `readonly-policy` gate isolates the accepted guard/read-effect scope while retaining the original compound and standalone failing witnesses.

## Exact copy custody

[CANONICAL_COPY_MANIFEST.tsv](CANONICAL_COPY_MANIFEST.tsv) maps **31 unchanged canonical files** to their public paths with byte counts and SHA-256 values. [CANONICAL_COPIES.sha256](CANONICAL_COPIES.sha256) checks those copies from this directory. [RECIPE_CUSTODY_COPY_MANIFEST.tsv](RECIPE_CUSTODY_COPY_MANIFEST.tsv) separately records the **61 public recipe-custody source copies**. The canonical publication commit is recorded in [HANDOFF.json](../../qualification/wrapper-convergence-20261018/HANDOFF.json).

These exact archives retain their original path references, comments and historical statuses. Workspace/canonical paths in frozen records identify original custody; the public copy manifests, qualification overview and [evidence directory](../../qualification/wrapper-convergence-20261018/evidence/) provide the public handoff. Selected index metadata is copied here; executable templates reside in the qualified recipe custody rather than being duplicated into this catalogue.

Example searches from this directory:

```sh
rg 'MASK_SELECTION|JNI_|INDIRECT_STABLE_SORT' catalogue-review/OWNER_CATALOG.tsv
rg 'INVENTORIED_ONLY_PENDING_REVIEW' catalogue-integration-f29/OWNER_CENSUS.tsv
rg 'REJECT_|EVIDENCE_ONLY' index-candidates/CATALOGUE.tsv
rg 'QUALIFIED_FOR_THIS_SERIES' ACCEPTED_ATOMS.tsv
```

For each subsequent pass, select one owner and operation, consult its retained comparison, establish the contract/effect boundary, and qualify a replaceable recipe through the serial source-patch, lint, compile, test and runtime gates. Preserve unsuccessful alternatives and historical receipts so later passes use the accumulated review.

