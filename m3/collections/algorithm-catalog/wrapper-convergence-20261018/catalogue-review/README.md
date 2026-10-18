# M3 collection owner catalogue and wrapper review

Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
SPDX-License-Identifier: Apache-2.0.

## Scope and authority

This inventory is pinned to `hsoliwal/M3jdk21@0d73745d7203123533dce8f8fb264ba2746f6d2d`. It covers the 129 Java production source files and 10 C/JNI production source files under `m3/collections/src/main`. The Java count includes `package-info.java` and `module-info.java`. Test owners, qualification mirrors, optional interop modules and `java.base` are outside this owner count. Canonical reusable recipe ownership remains `hsoliwal/com.synexia`; this catalogue does not advance the global STRING phase or promote the proving module into the JDK runtime.

The parent `CONTRACT.md`, root `AGENTS.md`, porting invariant, applicable name mappings and existing family catalogues were consulted before this directory was written. The target production checkout is read-only for this catalogue worker. No lint, compilation, test or runtime process is launched by this worker. Parent-executed baseline receipts are imported as evidence with their hashes; they are distinct from source review and candidate qualification.

## Format and honest status

`OWNER_CATALOG.tsv` has one row per production source file. Categories and selected operations come from declarations and documentation inspected in path order; the operation column is a discovery index, not an exhaustive contract checklist. Source hashes identify the exact inventoried body. The five filtered wrapper owners were fully read and reviewed for the stated generic-wrapper scope in this task. The other 134 owners are `INVENTORIED_ONLY_PENDING_REVIEW`, including owners with historical qualification files or separate review by another worker. A complete source read does not mean every operation was qualified.

The inventory reuses categories and donor IDs from `algorithm-catalog/collection-file-review-20261017.tsv` for its 76 owners still present at this baseline, and links the older 114-owner `full-m3-family-inventory-20261017/COLLECTION_SOURCE_INVENTORY.tsv`. Four entries from the older 80-row catalogue are absent from the current source set; their names are not restored or counted. Historical body equality and old source-review/compile assertions remain separate from this task's review and proof status. Newly inventoried categories are local discovery labels, not source-platform topic labels.

`FAMILY_CATALOG.tsv` preserves the six columns and all rows of the current `M3_SDK_FAMILY_CATALOG.tsv`, then adds explicit owner and evidence links. Upstream FQCNs, proposed roles and existing dispositions are not relabelled. In particular, `PENDING`, `JDK_RETAINED`, `M3_BACKEND_ONLY` and `M3_VIEW_ONLY` do not prove provider API substitutability. `FAMILY_DRIFT.tsv` records differences from the older `qualification/m3-sdk-families-20261017/FAMILY_CATALOG.tsv`; historical optional adapters are not silently treated as current core owners.

`PROVENANCE_LINKS.tsv` indexes current declaration-origin notices, exact module mapping records and retained catalogue/recipe metadata references where present. `DISCOVERED_POINTER_ONLY` means the reference was found, not that its donor body, licence compatibility, current postimage or claimed results were requalified here. A mapping's target-hash match is reported separately from review status. The global name-mapping file supplies phase/ownership policy; it is not substituted for a missing per-owner lineage entry.

Existing Synexia `AlgorithmCatalogue`, `PinnedAlgorithmDonorCatalog`, `ProblemCategoryCrosswalk`, `SerialProblemReview` and `M3SerialFileAtomConvergenceRecipe` are the retained integration points documented by that catalogue. This extension adds TSV linkage data; it does not claim those APIs automatically ingest these files or that their runtime methods were executed in this unit.

`WRAPPER_FINDINGS.tsv` contains the admitted strict mutation-refusal repair and five independently named pending witnesses. `BASELINE_EXECUTION.tsv` imports the parent's eight baseline modes: six reproduced defects/pending witnesses and two passing effect oracles. Their source receipt is `work-m3-wrappers-20261018/BASELINE_WRAPPER_WITNESSES.json`, SHA-256 `379b43b118a77c97a2b6a6c27261cc414110a5dcc7d1e7508f40d6d15c768ce9`. `EVIDENCE_SOURCES.tsv` and `SOURCE_MANIFEST.sha256` pin the supporting input bytes. Candidate execution remains pending in this worker's report until the parent appends its own ordered-gate receipts.

## Why unconditional iterator refusal is an admitted defect repair

At the pinned baseline, `M3FilteredLists.java` SHA-256 is `b2dc49d3a9ec43b6e6bd34cf5c6dac53057cf90f42cc9ed506d54822a760fc2d`. Its `readOnlyFiltered` factory documentation, lines 54–55, says it returns a live List view which **explicitly refuses all mutation methods**. The `StrictReadOnlyList` comment, lines 128–130, further requires the explicit read-only boundary to **refuse writes even for empty/no-op calls**. Its concrete write overrides, lines 144–161, throw `UnsupportedOperationException` unconditionally. Its existing `listIterator(int)`, lines 136–138, delegates to `Collections.unmodifiableList(delegate).listIterator(index)`, which already supplies the strict iterator mutation guard. These are pre-existing owner declarations, not a new inference that every JDK iterator must behave this way.

The inherited `AbstractList.iterator()` reaches its state check before this owner's `remove(int)`. Consequently, ordinary `iterator().remove()` before `next()` throws `IllegalStateException`. The parent baseline witness reproduces that exact exception and stack. This contradicts this owner's stronger declared boundary. The admitted candidate discloses one observable defect correction: all ordinary-iterator removal attempts throw `UnsupportedOperationException`, independent of cursor state. It does not describe that exception correction as byte-for-byte baseline behavioral equivalence.

The selected atom wraps `super.iterator()` and forwards `hasNext`, `next` and `forEachRemaining` to that exact cursor. Only `remove` is guarded. This preserves the original read callback demand, exception identity, retry behavior and stored references; a frozen baseline oracle explicitly compares those reads. One additional cursor wrapper is allocated per traversal. No speed or allocation improvement is claimed. Directly routing ordinary iteration through `listIterator()` was refused because it changes predicate demand and failure timing; linear traversal and backing fail-fast remain pending.

## Pending navigation boundaries and donor selection

Bounded navigation uses a query as a range endpoint in two current owners. The parent reproduced the resulting out-of-range endpoint failure for a legal nearest-neighbour query. Existing PR1204 source at `dab450b91928c0e6cc835a783c6b6b1805f70e8b` contains an actual stored-anchor fallback, but its broad `IllegalArgumentException` catch can also catch a caller comparator's failure. The frozen comparator-failure oracle preserves the same marker exception and callback tape. A history of passing tests does not justify swallowing that failure. Bounded read-only maps and mutable sets therefore stay pending an exception-safe owner design.

Likewise, null may be an actual stored member in the middle of a lawful comparator order. The current extreme-only disambiguation fails the frozen witness. Historical actual-extreme comparison avoids a fabricated null probe but adds comparator demand; an iterator presence scan can repeat nested predicates. Neither effect is silently admitted. The null-middle witness stays pending.

## Continuing the catalogue

For each next file, read the complete current body and its contract, compare the relevant existing catalogue candidates and exact donor revisions, document the admitted effect boundary, then prepare one replaceable behavioral atom in the canonical recipe. Preserve licences and attribution. Freeze source and oracle hashes before applying the recipe; execute patch, lint, compile, tests and runtime in the parent's declared order. Record failures and refused candidates without promoting historical counts into current proof. Refresh a row's hash and evidence only when the corresponding owner actually changes; retain the prior row in review history.

Example searches from this directory:

```sh
rg 'FILTERED_|READONLY_' OWNER_CATALOG.tsv WRAPPER_FINDINGS.tsv
rg 'JNI_|MASK_|INDEX_' OWNER_CATALOG.tsv
rg 'INVENTORIED_ONLY_PENDING_REVIEW' OWNER_CATALOG.tsv
rg 'M3FilteredLists|M3LongIndexProjection' PROVENANCE_LINKS.tsv
```
