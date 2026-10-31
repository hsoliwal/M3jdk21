<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Collection catalogue integration overlay at f29b0afe

## Pinned scope and census

This read-only overlay records `hsoliwal/M3jdk21@f29b0afe7ad253cc0562bc8dfdd48fc1acec710e` against the preserved [original catalogue](../catalogue-review/OWNER_CATALOG.tsv) at `0d73745d7203123533dce8f8fb264ba2746f6d2d`. Every baseline body comes from an exact Git blob. Mutable receiving checkout files are excluded as baseline inputs.

[OWNER_CENSUS.tsv](OWNER_CENSUS.tsv) contains **143 production owners: 133 Java and 10 C** under `m3/collections/src/main`. The Java count includes module/package descriptors. Relative to the original 139-owner census, **131 bodies are unchanged, eight changed, four were added, and none were removed**. Tests, optional integration modules, qualification mirrors and java.base are outside this count. [CENSUS_DELTA.tsv](CENSUS_DELTA.tsv) gives every changed/added path, body hash, Git blob and line count.

All 143 body SHA-256 values agree with the separately retained parent baseline-source-state manifest. This agreement is an inventory check. It does not imply an algorithm review or execution of those owners.

## What was reviewed

The audit read the complete pinned bodies of the six changed non-wrapper owners and all four additions, serially. [SOURCE_REVIEW.tsv](SOURCE_REVIEW.tsv) records these **ten full source reads with scoped discovery**, including the identified atoms and candidate-effect boundaries. The two changed legacy wrappers link to the external wrapper review. The remaining **131 owners are inventory-only in this overlay**.

Original review status and its operation scope remain separate columns in the census. The original catalogue's five scoped wrapper source reviews are preserved as historical evidence. Their status is neither erased nor automatically promoted into f29 qualification. Full body reading also does not establish that every operation has been qualified.

The four new public owners expose these reusable first-party patterns:

| New owner | Discovered category and boundary |
| --- | --- |
| `M3CollectionViews` | Live filtered collection dispatch, persistent predicate composition, stored representatives and detached stable sorting. Reuse must preserve callback order, comparator domains, optional mutation, identity and spliterator characteristics. |
| `M3LongIndexPathMasks` | Long-row selection through signed-stride physical offsets and multiple 32-bit state planes, with bounded resumable windows. Logical ordinals, physical positions and row values remain distinct; this specialization is Java-only. |
| `M3ProgressiveSignalEvidence` | Pair-local bit-state evidence, exact lexicographic order and certified weighted L1 bounds. Source reading does not certify metric assumptions or justify using partial evidence as object equality. |
| `M3SignalDimensions` | Stable primitive passes composed across dimensions. Projection callbacks run once per selected row **per dimension**; caching a provider result once per row changes that schedule. |

The changed `M3IndexMaskCompactor` Java/C pair adds direct logical-position selection and a versioned native capability probe. Its native export uses bounded primitive regions and exact output allocation; Java owns old-library fallback. The new exports belong in the fresh integration ABI baseline. `M3CountedBag` and `M3IndexedListSnapshot` consume position selection, with the snapshot remapping logical ordinals through its current physical positions.

The `M3LongArrayDeque` change skips its mask-retention scan after validation when the required mask is zero. Its other filtering path has a partial-failure commitment contract that must be retained. `M3IndexedTable` adds owner-revision checks after comparator callbacks and before publishing a sorted view. These are source observations; the audit makes no measured speed, allocation or concurrency claim.

Every discovery row is **NO_NEW_TRANSFORMATION_ADMITTED**. Existing first-party implementation is catalogued for later comparison. The `M3SignalDimensions` source declares Synexia MatSignalDimensions PR11656 lineage; that donor pointer is explicitly unretrieved here. No donor body, licensing closure or equivalence proof is inferred from the pointer.

## Wrappers and prior qualification

Detailed f29 wrapper analysis belongs to [RECONCILIATION.md](../catalogue-review/RECONCILIATION.md). [EVIDENCE_LINKS.tsv](EVIDENCE_LINKS.tsv) also pins the parent's eight-mode `BASELINE_WRAPPER_WITNESSES.json`: bounded map/set and read-effect witnesses exit successfully; the other five modes fail. These are imported parent executions. This worker did not run wrapper witnesses during the audit.

[PRODUCTION_PREIMAGES.tsv](PRODUCTION_PREIMAGES.tsv) proves the three admitted production preimages remain byte-identical at f29:

- `M3FilteredLists.java` for `readonly-iterator`.
- `M3LongIndexProjection.java` for `stable-order-groups`.
- `m3_index_mask_window.c` for `native-row-suffix`.

Each row links the original five-test SDK replay receipt and exact preimage hash. Those receipts qualify the original recipe replay scope. They do not replace f29 receiving behavior, API, ABI or resource gates, which are a separate parent activity. Exact SDK binding counts are also distinct from the broader orchestration input snapshots; the evidence table links the additive count clarification.

## Using this overlay

The TSV files start with two SPDX comment lines; tabular readers should skip lines beginning with `#`. Categories are searchable directly, for example:

```sh
rg 'MASK_SELECTION|CAPABILITY_EXPORT' SOURCE_REVIEW.tsv
rg 'FULL_SOURCE_READ_SCOPED_DISCOVERY' OWNER_CENSUS.tsv
```

For the next serial pass, select the owner and operation, consult its existing catalogue and donor evidence, state the contract/effect boundary, then qualify a reusable recipe through the established ordered gates. Discovery entries are candidates for that work; they are not blanket replacement authorization.

[AUDIT_EVENTS.tsv](AUDIT_EVENTS.tsv) records the pinned inventory and serial source reads. Evidence paths declare whether they are overlay-relative, workspace-relative or a declared HTTPS pointer. This audit changed only its new catalogue directory and the separately authorized additive input-count clarification. It launched no lint, compiler, tests, runtime, recipe execution or receiving source edits.
