<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Review of every current collection source and catalogued provider family

Observation date: **21 October 2026**.

The selection policy is to prefer an existing compact array owner and a small
algorithmic kernel whenever they satisfy the concrete collection contract and
workload. Review every collection for duplicated payload, derivable metadata,
unnecessary temporary arrays and opportunities to share an owner. Keep specialized
implementations available under accurate storage descriptions. Public names and
provider APIs remain stable.

This expands the earlier [51-source selected inventory](../array-defaults-20261021/README.md)
to a complete structural review of the receiving module's Java source census and
all entries in its five-provider family catalogue. The earlier packet remains an
immutable observation of its original baseline.

## Coverage and evidence

- [m3-source-review.tsv](m3-source-review.tsv) accounts for **every one of the 270
  Java files** under `m3/collections/src/main/java`, including storage owners,
  views, helpers, interfaces, infrastructure and unreceived scaffolds. Each row
  records actual retained fields, relevant method evidence, an algorithmic
  alternative, the contract gates and exact source identity. A helper, facade or
  interface is not counted as another independent element store.
- [provider-review.tsv](provider-review.tsv) reviews **all 104 catalogue entries**:
  41 JDK, 23 Guava, 17 Eclipse Collections, 17 Apache Commons Collections and six
  Spring entries. All **100 direct source files and four Eclipse generator
  contract templates** were fetched from explicit revisions and their Git blob
  identities verified. The table separates an interface contract from an owning
  implementation. It does not assert complete provider method compatibility.
- [additional-patterns.tsv](additional-patterns.tsv) adds **10 official problem
  statements** to the earlier [15-pattern catalogue](../array-defaults-20261021/patterns.tsv),
  for **25 challenge references**: 19 LeetCode and six HackerRank problems. The
  algorithm descriptions and tradeoff analysis are independently authored.
- [priorities.tsv](priorities.tsv) groups the most useful next changes, names the
  structure they would remove, and records the proof needed before implementation.
  The per-file table retains the complete set of findings, including lower-priority
  candidates and explicit retain decisions.
- [coverage.json](coverage.json) seals the exact census, role/decision counts,
  provider counts, source revisions and the verification boundaries.

The receiving source baseline is `6ac3d5834844d444979ab1f7b8c85e68982759b7`.
The review began at `d814e68f98715ebf71743c00b82f5a9a61223af0`; the intervening
comparison changed only `M3FilteredDeques.java` in the production Java census.
That file was reread at the newer revision. The reviewed working candidate for
`M3LongLongTreeMap` is identified by its exact after hash, while every other row
describes its baseline bytes. Publication preserves unrelated changes already
merged into the receiving repository.

The provider sources deliberately use the fork revisions already selected by
the existing collection catalogue, with the JDK sources pinned to the original
receiving baseline. Four primitive Eclipse API classes are generated rather than
checked in as Java sources; the review names their actual `.stg` contract owners.
No generated Java compilation or Eclipse adapter qualification is implied.

This is complete coverage of the **270-file receiving census and 104 catalogued
families**. The older public-fork catalogue contains metadata for 40 repositories
and a separate 21-file sample across eight forks. This review does not turn that
historical sample into a claim that every source class in all 40 repositories has
been audited. Additional upstream families enter this same review process when
they enter the receiving catalogue.

## What should become simpler

Prefer removal of duplicated information over an arbitrary reduction in the
number of Java classes or array objects. Different primitive lanes can be the
most compact way to represent different facts. Combining a `byte[]` and `int[]`
into a wider lane can increase payload bytes even if it removes an array header.
Every proposed packing change must count payload width, capacity slack, array
headers, alignment, objects and allocation volume separately.

The current review identifies several concrete mechanisms:

| Mechanism | Sources with a candidate | Structure that can potentially disappear | Required information that stays |
| --- | --- | --- | --- |
| Infer occupancy from an existing legal sentinel | `M3LFUCache`, linked primitive maps, weak-reference maps | A separate occupancy/state lane | Null/full-domain behavior, tombstone rules, reference lifecycle and mutation order |
| Store free-list links in unused slots | `M3IndexedTable`, `M3RowTree`, paged sorted-set storage | Separate free-row/free-column/free-slot arrays | Live order, topology, deterministic reuse and stale-handle protection |
| Represent identity/slices arithmetically | Snapshot, projection, bag-rank and range-view owners | Arrays containing only `0,1,2,...` or an affine slice/reversal | Real permutations, stable equal-value ordering, physical/source identities and prefix aggregates |
| Share one payload between several indexes | `M3IndexedBiMap`, `M3IndexedSetMultiMap`, table families | Duplicate key/value references and per-key owning collections | Forward/reverse lookup, uniqueness, per-direction/per-key order and live views |
| Derive already represented metadata | Object slot store, topology, sparse extents and horizontal window planning | Duplicated handles, aggregates, measured extents or interval ends | Generation validation, overflow behavior and documented query/update costs |
| Keep one active representation | `M3RangeSet`, prepared filter chains | Inactive range capacity/caches or both linked and flattened plans | Switching hysteresis, append cost, safe publication, callback order and aliases |
| Transfer fresh-array ownership or fuse passes | Lists, snapshots, diff/patch, projection and native adapters | Copy-of-a-copy, identity selection buffers or result staging | Array type normalization, detached snapshots, callback counts, failure atomicity and ABI behavior |

These are source-reviewed candidates, not measured savings. Some require changing
several coordinated methods. For example, `M3LFUCache` rejects null values, so a
non-null value can encode occupancy. However, its current insertion publishes the
value before computing a stamp and marks the state occupied afterward. A naive
replacement would include a newly occupied slot in `Long.MAX_VALUE` rebasing too
early. The candidate must preserve that phase ordering before the state array can
be removed safely.

Likewise, the forward and inverse maps inside `M3IndexedBiMap` retain duplicate
**references** to the same key/value objects. Sharing payload can remove those
references; it does not mean the objects themselves were duplicated. Separate
lookup indexes remain necessary for efficient access in both directions, and the
two views can have different encounter histories after replacement.

## Reusable algorithms and storage choices

| Required operations | First existing representation/kernel to evaluate | Conditions that require additional structure |
| --- | --- | --- |
| Indexed sequence, append, stable filtering | Dense array, `arraycopy`, read/write cursor compaction | Live sublists, stable handles, callback failure behavior, middle insertion cost |
| FIFO/deque/end operations | Circular array with head/tail | Blocking, fairness, transfer/cancellation and concurrent publication protocols |
| Unordered map/set membership | Open-addressed key or key/value slots | Collision handling, null/full-value-domain encoding, deletion and exposed stable positions |
| Encounter-ordered map/set | Shared hash payload with indexed order | Access-order changes, reverse views and constant-time relinking |
| Sorted search and batched updates | Lower/upper bounds, append fast paths, deduplication and backward merge | Flat-array middle shifts remain linear; arbitrary online update guarantees can require blocked or balanced indexes |
| Counts and bounded domains | Distinct keys plus counts; ordinal values; bit words | Universe size, multiplicity/overflow, rank/select and unspecified future keys |
| Multimap/table/bidirectional lookup | One payload owner plus offsets/slot indexes | Live aliases, arbitrary group mutation, pair uniqueness, multiple orderings and cross-index consistency |
| Priority/extremum/top-k | Array heap or selection | Arbitrary deletion may need a slot index; a heap does not provide sorted traversal or predecessor queries |
| Range batches | Sorted endpoints, interval merge; difference array for final-only aggregates | Online queries, open/unbounded endpoints, live range views and overlap semantics |
| Read-heavy concurrent snapshots | One immutable array version, published atomically | Old iterators keep old versions; writers cannot mutate a published array in place |

Reuse existing M3 owners and kernels before introducing a new storage framework.
Adapters with different public APIs can share one owner without erasing their
semantic differences. A helper method or lazy view that stores no elements should
remain a helper/view instead of becoming another materialized collection.

### What the challenge constraints actually permit

The official challenge statements are a source of constrained algorithm problems,
not evidence that one solution replaces an entire general-purpose collection.

- [LeetCode 88](https://leetcode.com/problems/merge-sorted-array/) motivates the
  backward-merge kernel already qualified here. Spare capacity and duplicate
  policy must come from the receiving contract.
- [LeetCode 705](https://leetcode.com/problems/design-hashset/) and
  [706](https://leetcode.com/problems/design-hashmap/) bound keys to `0..1,000,000`.
  Direct addressing is simple, but can waste space when only a few keys are live.
  The map's `-1` absence sentinel is valid because that problem excludes negative
  values. Full-domain primitive maps require a different absence encoding.
- [LeetCode 380](https://leetcode.com/problems/insert-delete-getrandom-o1/) permits
  a dense array plus position map and swap deletion. Ordered collections and
  stable physical-slot views cannot adopt swap deletion without repairing their
  observable identities and ordering.
- [LeetCode 981](https://leetcode.com/problems/time-based-key-value-store/)
  guarantees increasing timestamps. Appending and binary searching a history is
  sufficient under that promise; arbitrary out-of-order insertion is additional
  work.
- [LeetCode 146](https://leetcode.com/problems/lru-cache/) and
  [460](https://leetcode.com/problems/lfu-cache/) require average constant-time
  operations. Those bounds can require lookup, recency and frequency indexes.
  A small scanning cache uses fewer structures but has linear scan cost in its
  capacity. The review records that tradeoff rather than borrowing the challenge's
  stronger bound.
- [HackerRank Sparse Arrays](https://www.hackerrank.com/challenges/sparse-arrays/problem)
  needs occurrence counts, so one distinct-key/count store is enough for that
  query. [Array Manipulation](https://www.hackerrank.com/challenges/crush/problem)
  asks only for the final maximum after all updates, allowing an offline
  difference-array pass. Neither task establishes the full online contract of a
  general collection.

No editorial, submitted solution or implementation body from either platform was
copied. The additional rows label the observed statement separately from the
derived algorithm, its cost and its restrictions.

## Retained specializations and accurate labels

An array is a storage layout; an algorithm describes what is maintained in it.
Trees can use integer links in arrays, and a heap is also a tree logically while
occupying one dense array. The review records both dimensions.

- `M3LongLongTreeMap` is a **sorted primitive key/value array map**.
- `M3LongTreeSet` is a **paged sorted primitive set with a flat directory**. A
  split can shift directory entries; its updates are not universally logarithmic.
- `M3TreeMap` and `M3TreeSet` are **indexed AVL specializations**, with their
  balance/navigation metadata retained for their current contracts.
- Queue, cache, reference and concurrent owners retain the protocol state needed
  for their concrete guarantees. GC reference objects cannot be replaced with
  strong references to the same payload without changing reachability.
- Throwing `M3NotYetReceived` scaffolds are labelled as **unreceived scaffolds**.
  Declared arrays in such a source are not a qualified backend and provide no
  measured saving.

These are descriptive labels and selection guidance. This packet does not rename
public classes or automatically promote catalogue entries to provider/default
implementations. The existing statuses remain **57 PENDING, 27 M3_BACKEND_ONLY,
eight M3_VIEW_ONLY and 12 JDK_RETAINED**.

The provider review also preserves differences that a shared kernel must expose
through its adapter: Apache legacy `Bag.remove` removes all copies; Guava and
Apache bidirectional maps resolve value conflicts differently; Spring's
`MultiValueMapAdapter` wraps a caller-supplied map and exposes its list objects;
`LinkedMultiValueMap.clone` shares lists while `deepCopy` copies them. An owning
pooled multimap is a candidate for a different ownership contract, not a silent
replacement for these externally aliased lists.

## Implemented and qualified first change

The [receiving evidence packet](../../qualification/array-defaults-20261021/RESULTS.md)
records the completed `M3LongLongTreeMap.putAllSorted` refinement. It reuses the two
existing destination lanes when capacity permits, eliminating both duplicate-run
normalization buffers and unnecessary destination copies. It validates the full
batch before mutation and preserves last-duplicate-wins values, observable batch
capacity, input ownership and structural-modification behavior. Extreme capacity
overflow hardening is explicitly recorded.

The canonical recipe is in [Synexia draft PR 12705](https://github.com/hsoliwal/com.synexia/pull/12705),
commit `cac5116e3408d6ff306d95654e9f4db6b59a27c1`. Actual OpenRewrite SDK Results
produced the receiving bytes through the existing canonical recipe engines.

Executed evidence includes strict Java 21 compilation; five SDK tests per pass;
the unchanged three-test owner suite on both versions; independent exhaustive,
mixed, invalid-input and callback oracles; exact public/protected descriptors;
and candidate lane-reuse checks with a baseline negative control. The bounded
allocation workload measured **73,792 bytes per baseline call and zero per
candidate call** for a batch that fits spare capacity. This is an allocation
measurement, not a throughput, total retained-heap or every-workload result.

All other proposed reductions in this review are **unimplemented candidates**.
Their required sequence is: freeze the concrete contract; reuse the canonical
recipe mechanism; prove source/preimage/refusal behavior; compile the changed
owner closure; compare behavior with the baseline; and measure the exact resource
claimed to improve. Full module/reactor, provider compatibility and installed-JDK
runtime qualification remain outside the executed first-leaf evidence. The active
runtime phase remains STRING.

## Data interpretation

`role` distinguishes OWNER, VIEW, HELPER, CONTRACT, SCAFFOLD and INFRASTRUCTURE.
`decision` records a retain/consolidate/temporary-removal/view/contract/scaffold
outcome, with `QUALIFIED_CHANGE` and P0 reserved for the executed first leaf. P1 marks a
concrete follow-up worth a bounded contract/recipe investigation; P2 marks a more
conditional or lower-value alternative. Priorities are review judgments, not
performance rankings.

Evidence line ranges refer to the exact hashed source. Array counts describe
declared retained structures; runtime allocation and reachable-object counts can
differ because owners share storage, allocate lazily, grow or retain old snapshots.
The coverage validator checks every source path, hash and catalogue identity and
ensures that no provider status was promoted by this documentation.
