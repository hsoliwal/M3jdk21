<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Array defaults and challenge-informed collection kernels

Observation date: **21 October 2026**. Source baseline:
`d814e68f98715ebf71743c00b82f5a9a61223af0` in `hsoliwal/M3jdk21`.

This packet records the requested selection direction: prefer existing compact
array owners, offsets, masks and small algorithmic kernels for M3 collection
defaults where the concrete contract permits them. Keep specialized implementations
available and describe their actual storage strategy. Public Java and provider
class names remain unchanged. A strategy label in this catalogue is descriptive
metadata; it does not rename a class, change a factory or install a new default.

## Deliverables and evidence scope

- [patterns.tsv](patterns.tsv) records **15 official challenge pages**: 12 LeetCode
  and three HackerRank tasks. Each row separates the observed task from our
  independently derived algorithm, cost model and applicability constraints.
- [strategy-catalogue.tsv](strategy-catalogue.tsv) records **51 selected current
  M3 source owners, helpers, view factories and declared scaffolds**, with exact
  baseline source identities and links to existing family-catalogue rows. This
  is a selected strategy inventory, not a census of every class in the package.
- Existing [challenge research](../challenge-patterns-20261018/README.md) and the
  [public-fork inventory](../public-forks-20261019/README.md) remain intact. The
  latter observed 40 public forks but reviewed only **21 selected implementation
  files across eight forks**. This packet neither repeats that source review nor
  expands its completion claim.

Official problem statements were read on the observation date. No editorial,
submitted solution or implementation body was copied from either platform.
The checked LeetCode editorial routes exposed no editorial text. Algorithms and
their bounds below are our analysis unless the task explicitly requires a bound.
The pages nominate patterns; they do not grant implementation-source copying
rights. Independently authored kernels still require the existing recipe,
provenance and contract gates.

This packet contains documentation and catalogue data only. It makes no
compilation, runtime, benchmark, installed-image, full-provider compatibility or
global receiving-phase claim. The repository's String qualification stage and
recipe ownership remain unchanged. Separate implementation evidence must name
its actual source identities, commands and outcomes.

## Current storage already provides several alternatives

The source names alone do not describe the storage:

| Existing owner | Observed strategy | Selection treatment |
| --- | --- | --- |
| `M3LongLongTreeMap` | Sorted parallel `long[]` key/value lanes, binary search, array copying and sorted-batch merge | Existing array candidate for its primitive contract; retain its public name |
| `M3LongTreeSet` | Sorted pages in primitive lanes, with flat maxima/count/slot directories | Existing blocked-array candidate; retain and evaluate directory/growth cost |
| `M3TreeMap` / `M3TreeSet` | Indexed AVL with reference payload arrays and integer links | Retained specialized strategy; it is not a red-black implementation |
| `M3ArrayList`, primitive lists and deques | Dense arrays or rings | Reuse existing owners; preserve mutation epochs, null and callback behavior |
| Hash, identity and ordinal owners | Open addressing, identity-reference lanes, or ordinal values/presence words | Keep their distinct equality and domain contracts |
| `M3BitLane28` | Segmented words plus two primitive Fenwick levels | Existing bitmap/rank owner; no need to invent a second rank framework |
| `M3ConcurrentLongMap` / `M3ConcurrentLongSet` | Immutable sorted primitive snapshots published by CAS | Retain their read-heavy snapshot contract; writes still copy arrays |
| `M3IndexedList`, `M3IndexedLongList` and named reference/range scaffolds | Declared lanes with operations throwing `M3NotYetReceived` | Pending; their class declarations are not working implementations |

The strategy table records class documentation and selected storage declarations,
not a complete body review or fresh correctness proof. `SOURCE_OBSERVED` therefore
does not mean `QUALIFIED`. Source SHA-256 and Git blob identity seal the observations;
they do not establish behavioral equivalence.

The immutable baseline view is deliberate: an independently reviewed source change
can accompany this packet without silently changing what this inventory observed.
A successor observation should pin its new revision and retain this history.

## Cheaper sorted-order work to evaluate first

1. **Bounds and monotone input.** Reuse binary-search lower/upper bounds. An append
   fast path can exploit an incoming key that follows the existing maximum when
   the concrete overwrite/duplicate policy permits it. Searching for an insertion
   point in logarithmic time does not make the subsequent packed-array shift
   logarithmic.
2. **Batch merge.** A sorted batch of `b` entries can merge with `n` existing entries
   in `O(n + b)` work. An unsorted batch additionally requires sorting. Backward
   merging can use already-reserved destination space, but aliasing, validation,
   capacity growth, key identity and duplicate winner rules must be explicit.
3. **Stable compaction.** Filter or deduplicate with read/write cursors. Ordered
   collections must preserve encounter order. LeetCode 27 allows reordering, so
   its permitted swap-delete behavior is not an ordered-collection contract.
4. **Sorted blocks.** Reuse `M3LongTreeSet` before introducing another owner. With
   block capacity `B` and `P` directory entries, insertion may move `O(B)` values
   and a split may move `O(P)` directory entries. Backing-array growth can copy
   a larger arena. Do not advertise every update as `O(log n)` merely because
   the two searches have that bound.
5. **Bounded domains.** Counting arrays, bitmaps and Fenwick counters can answer
   membership, rank and aggregate queries without retaining a node per key.
   Their storage depends on domain size `U`; coordinate compression may require
   an offline key set or rebuild when new keys appear. LeetCode 1649 returns
   insertion-cost counts, not a physically maintained sorted output container.
6. **A narrower query.** A ring, monotone deque, array heap or selection pass can
   serve queue, window-extremum, priority or order-statistic consumers. Such a
   specialization is admissible only when those are the consumer's actual
   required operations; it cannot silently remove sorted navigation.

Reserved gaps and packed-memory-array variants remain further candidates, with
their search, density, redistribution, lifetime and memory costs to be reviewed.
The 15 verified challenge pages do not themselves establish a complete
packed-memory-array implementation or general online sorted-map replacement.

## Five-provider accounting and preserved contracts

The existing [SDK family catalogue](../../src/main/resources/com/m3/collections/M3_SDK_FAMILY_CATALOG.tsv)
has **104 rows** at the baseline: JDK 41, Guava 23, Eclipse Collections 17,
Apache Commons Collections 17 and Spring Framework 6. Its dispositions are
57 `PENDING`, 27 `M3_BACKEND_ONLY`, eight `M3_VIEW_ONLY` and 12 `JDK_RETAINED`.
These are preserved source-catalogue statuses, not new acceptance results.

All 21 distinct existing-owner names in that SDK table have an entry in this
selected strategy inventory. Exact matches to that table and the existing
[JDK family map](../../JDK_FAMILY_MAP.tsv) are recorded separately. An empty match
means that the particular table contains no exact owner association; it does not
mean the class has no consumer. A pending provider row with no owner is not
promoted because a similarly named M3 class exists elsewhere.

The older JDK family map, for example, still marks its `TreeSet` candidate as
`PENDING` although `M3LongTreeSet` now contains a blocked-array body. This packet
records both facts and does not rewrite historical admission status. Unknown
provider compatibility remains unknown until the applicable provider oracle and
target receiving evidence exist.

Default selection must preserve, where applicable:

- equality versus identity, comparator equivalence, null handling and duplicate
  representative/overwrite policy;
- stable encounter order, inclusive/exclusive bounds, live backed views and
  immutable-snapshot distinctions;
- iterator/spliterator mutation behavior, callback order, reentrancy restrictions
  and exception/partial-commit behavior;
- serialization and public/protected descriptors, subclass hooks, weak-reference
  lifetimes and GC-visible payload ownership;
- synchronization, publication, linearization and progress guarantees;
- documented worst-case bounds, with allocation and growth accounted separately.

An array strategy is preferred within that contract. An indexed AVL or another
specialized incumbent remains available where its guarantees are required.
Neither a cosmetic rename nor replacing a documented logarithmic update bound
with a linear shift is a completed contract-preserving migration.

## Table schemas and status interpretation

`patterns.tsv` has one row per verified challenge. `observed_problem_fact` is a
brief paraphrase of the source task. `derived_algorithm`, `time_complexity`,
`auxiliary_space`, `applicable_contract` and `constraints` are engineering analysis.
`PROPOSED_PATTERN` does not say that a matching M3 atom is absent or unqualified;
its use in a new collection context needs its own admission evidence.

`strategy-catalogue.tsv` has one row per selected source file. `strategy_label`
describes storage without changing the public type name. `source_status` separates
an observed current body from a declared scaffold. `selection_status` states a
proposed treatment, independently of `provider_qualification`, which is
`NOT_REQUALIFIED_IN_THIS_PACKET` for every row. Family associations carry the exact
existing disposition in `name:status` form. `candidate_patterns` names research
categories for comparison, not approved replacements.

All TSV fields use one physical line. Semicolons separate multiple associations.
`-` means no exact association or no selected pattern, not successful completion.
Rows are deterministically ordered by pattern identifier or source owner.

## Admission and reproduction

The authorities remain root `AGENTS.md`, the
[porting invariant](../../../docs/m3jdk21-porting-invariant.md), the global
[naming map](../../../docs/name-mapping.json) and the collection
[mapping](../../name-mapping.json). Reusable implementations and recipes remain
canonical in Synexia. Public class names, existing wrappers and retained
specialized structures remain unchanged by this catalogue.

For each actual implementation atom: document the contract and current source;
select the existing owner and compatible pattern; update the canonical recipe;
materialize only an admitted preimage; then lint, compile, test and exercise the
actual runtime in that order. Record before/after hashes, fixed point/refusal,
API/ABI checks and resource evidence separately. The retained wrapper findings in
the [stable-order review](../../algorithm-catalog/stable-order-restoration-20261020/README.md)
remain pending where that record says so; this research does not resolve them.

To check a strategy source identity, read `source_path` at `source_commit`, compute
its raw-byte SHA-256 and compare it with `source_sha256`; compare the Git blob with
`source_git_blob` separately. To check mapping links, match the unchanged owner
name against the two pinned family tables. Schema, identity and link checks are
catalogue validation; compilation, tests and runtime are not applicable to these
three data/documentation files and are not claimed by them.
