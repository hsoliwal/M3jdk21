<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Collection wrapper and path-mask convergence catalogue

This catalogue records a finite recovery and refinement pass against
`hsoliwal/M3JDK21@0d73745d7203123533dce8f8fb264ba2746f6d2d`.
It covers existing filtered collection, sorted/navigable wrapper, primitive
index-projection and staged path-mask owners in the independently licensed
`m3/collections` proving module. It does not establish full collection-family,
repository, Java SE backend, or performance acceptance. The canonical target
receiving plan remains `STRING -> ARRAYS -> COLLECTIONS -> AST_COMPILER ->
REMAINING_FAMILIES`, with `STRING` active and no phase advanced by this pass.

The canonical artifact owner is this Synexia recipe crate. M3JDK21 may receive
an exact recipe-generated copy with revision and source-hash lineage. No new
search engine, fingerprint engine or Synexia runtime dependency is introduced.

## Files and searchable keys

| File | Purpose |
| --- | --- |
| `DONOR_EVALUATION.tsv` | Searchable candidate records using the existing catalogue's first eleven columns, followed by explicit reuse and evidence fields. |
| `DONOR_EVALUATION.json` | The same candidate rows with schema, evidence vocabulary and scope metadata. |
| `SOURCE_PROVENANCE.json` | Exact baseline, donor and catalogue-input identities; canonical recipe lineage; verification boundaries. |
| `COVERAGE.tsv` | Current-pass contract and gate matrix. Pending rows have no invented test receipt or postimage hash. |

Join records using `donor_link_id`; use `category` and `target_owners` to find
operations, and `coverage_ids` to locate the gates that remain necessary.
For example, `rg 'BOUNDED_FILTERED_NAVIGATION|ROW_MASK_FILTER|ALIASED_OUTPUT'
DONOR_EVALUATION.tsv` searches this finite data directly. An existing catalogue
loader may ingest the records through its established data path. These files
are not automatically registered in `AlgorithmCatalogue.search(...)` or the
authoritative `PinnedAlgorithmDonorCatalog` manifest merely by existing here.

The schema extends, rather than replaces, the baseline's
`m3/collections/algorithm-catalog/collection-candidate-comparison-20261017.tsv`.
Related mask/rank/provenance tables are listed and hashed in
`SOURCE_PROVENANCE.json`. Existing Synexia catalogue, problem-category and
serial-review APIs remain the implementation owners. Local curated categories
are not presented as native labels returned by a challenge platform.

## Why recover existing atoms first

PR [#1204](https://github.com/hsoliwal/M3JDK21/pull/1204), head
`dab450b91928c0e6cc835a783c6b6b1805f70e8b`, carries compatible historical wrapper
atoms that differ from the present source. Its recorded canonical Synexia
delivery is `72580cf231dc025965370933f099e914ddc407be`. The source and custody
metadata identify the existing Java/text snapshot recipes and their fixtures.
Merge ancestry and those historical receipts do not prove that the current
target contains or passes the same implementation.

The recovery uses source-bound atoms inside existing owners. It must preserve
newer target changes outside the selected atoms. In particular, a whole-file
historical replacement of `M3LongIndexProjection` could discard newer range and
native-index behavior; its path-mask bridge is evaluated separately. The
current `M3FilteredLists` and primitive owner contracts remain the comparison
baseline, rather than being rolled back to a historical snapshot.

The native intake candidates are recorded independently of #1204. That branch's
path-mask Java and C bodies match the baseline bodies reviewed here; its JNI
history cannot be credited with restoring the omitted long-row, zero-stage or
impossible-mask paths. Current target correctness, ABI, failure behavior and
resource gates must bind the eventual generated postimages.

## Contract and algorithm decisions

Guava's [filtered collection documentation](https://guava.dev/releases/33.7.2-jre/api/docs/com/google/common/collect/Collections2.html)
and [sorted/navigable filter documentation](https://guava.dev/releases/33.7.2-jre/api/docs/com/google/common/collect/Sets.html)
are comparison evidence: live views, visibility-limited mutation, predicate
admission and iterator restrictions are observable semantics. They do not
authorize replacing the target's existing callback timing or writable-iterator
contract. This documentation version does not update the SDK dependency pin.

The following distinctions govern candidate selection:

- **Live predicates and precomputed masks have different validity rules.** A
  predicate may observe mutable element fields or external state without a
  structural collection change. Structural modification tracking alone does
  not justify caching live membership. The path-mask kernels consume explicitly
  stable, pure metadata during a call and return detached data or write to an
  explicit caller-owned output window.
- **Sorted list projections retain duplicate occurrences.** Sorted sets use
  their comparator's equivalence relation. JDK backed-range, exception, reverse
  ordering and stored-representative behavior remain separate proof obligations.
- **Validation decorators are a separate category.** Apache
  [PredicatedCollection](https://commons.apache.org/proper/commons-collections/apidocs/org/apache/commons/collections4/collection/PredicatedCollection.html)
  validates admission instead of hiding arbitrary existing elements. Apache
  [FilterIterator](https://commons.apache.org/proper/commons-collections/apidocs/org/apache/commons/collections4/iterators/FilterIterator.html)
  documents a restriction on removal after lookahead, so it is not an unchanged
  replacement for an iterator with stronger removal behavior.
- **Primitive index and mask owners come first.** Existing M3 projection and
  compaction APIs and JDK array/bit APIs are retained before considering another
  dependency. fastutil stable primitive sorting, bounded-key counting sort and
  compressed bitmaps remain alternatives awaiting a concrete unsatisfied owner
  contract and measured workload.
- **JNI is a separate proof surface.** Keep full signed row IDs, traversal
  geometry, mask-word semantics, output aliases and Java/native failure parity.
  Native access retains no Java pointer after a call. Java predicates,
  comparators and progress callbacks cannot run in a JNI critical region; see
  the [Java 21 JNI specification](https://docs.oracle.com/en/java/javase/21/docs/specs/jni/functions.html#getprimitivearraycritical-releaseprimitivearraycritical).

LeetCode, HackerRank and GeeksforGeeks entries provide problem-shape discovery
and test ideas. No site editorial, submission or article implementation is
copied. Complexity is an algorithm cost model, not a measured speedup. Every
online candidate is `NOT_MEASURED_CURRENT_PASS` unless an actual new receipt is
attached later.

## Qualification and updates

Apply the existing qualified Synexia recipe lane serially to exact target
preimages. If a generated result fails, fix the recipe payload and rerun it.
Keep recipe, fixtures, source and postimage seals, and executed receipts
together. Preserve licensed source notices, including the first-party
`Copyright 2026 Hitesh Soliwal and contributors`; a recipe does not relicense
embedded third-party or OpenJDK material.

`COVERAGE.tsv` starts with current-target execution gates pending. Historical
passes are recorded only as historical source lineage. To mark a gate `PASSED`,
record the actual command or runner, environment, source/postimage digest,
receipt location and receipt hash. A passing unit test cannot fill unrelated
native, platform, resource, reactor or JDK-runtime gates. `EXCLUDED_SCOPE` rows
explain work this finite proving-module pass does not claim to perform.

CPU, allocation/retained heap and native/process memory need their own measured
receipts, including cold preparation, warm reuse and JNI dispatch/copy cost
where applicable. Until then no performance winner or resource improvement is
claimed. Later catalogue passes retain rejected candidates and the reason for
rejection so that discovery and incompatible substitutions are not repeated.
