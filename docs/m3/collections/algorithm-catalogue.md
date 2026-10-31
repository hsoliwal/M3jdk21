# M3 collection-wrapper algorithm catalogue

Status: living catalogue and verification contract. Entries are implementation claims only when linked to exact source postimages and test evidence. This catalogue supplements, rather than replaces, the broader source inventory at `m3/collections/qualification/filtered-navigable-consolidation-20261017/CATALOGUE.tsv` and the current bounded-navigation catalogue at `m3/collections/qualification/m3-filtered-bounded-current-20261018/recipe/CATALOGUE.tsv`.

## Invariant

M3 collection wrappers must preserve the wrapped collection's public Java contracts and externally observable behavior. Optimizations may replace internal behavioral atoms only when the composed implementation is observationally equivalent. Process files serially: inventory owner and callers, record the baseline, apply one recipe, run focused contract tests, then run integration tests before proceeding.

Dynamic-array representation is an implementation strategy, not a license to weaken collection contracts. Keep logical size distinct from capacity; use checked offset/index arithmetic; treat state masks as logical state only where the owner defines that encoding; preserve iteration order, comparator semantics, null policy, fail-fast behavior, and view mutability.

## Verified repository decisions (2026-10-18)

| Category | Owner / change | Candidate comparison and decision | Evidence / limitations |
|---|---|---|---|
| COL-001 / COL-003 | `M3FilteredViews`, `M3MutableFilteredViews` | Reuse the shared filtered-view owner and comparator-aware stored-representative lookup. Do not duplicate the algorithm in `M3FilteredNavigableSet`; it remains a compatibility facade. | PR [#1156](https://github.com/hsoliwal/M3jdk21/pull/1156) records a 114-source inventory and category catalogue. Later fixes are in #1244. |
| COL-002 / COL-004 | Read-only `M3FilteredViews.NavigableMapView`; mutable `M3MutableFilteredViews.SetView` | Prefer one normal bounded seek, then comparator-aware recovery only when the source rejects an out-of-range query used to construct the temporary range. This avoids unconditional O(n) scans while retaining correctness for nested/descending views and null-friendly comparators. | Merged PR [#1244](https://github.com/hsoliwal/M3jdk21/pull/1244): its reported exact-head workflow passed strict Java 21 compilation and 90,431 JDK differential checks. The owning Maven/JUnit gate was blocked before tests by unavailable dependency coordinate `org.junit.jupiter:junit-jupiter:5.10.2-m3-source-1`; full module validation remains unproven. |
| COL-005 / COL-006 | `M3IndexMaskCompactor`, `M3CountedBag`, `m3_index_mask_compactor.c` | Emit selected logical positions directly instead of allocating and passing an identity `int[]`. Keep Java as oracle, JNI region-based processing, and an explicit old-native-ABI fallback. Reject boxed streams, extra BitSet materialization, and critical array pinning absent evidence. | Merged PR [#1246](https://github.com/hsoliwal/M3jdk21/pull/1246) describes Java/JNI differential, `-Xcheck:jni`, and old-binary compatibility gates. Treat the PR's own workflow evidence as scoped, not proof of all module/platform gates. |
| COL-007 | Sorted/bulk filtered mutation | Preserve the evolved source capture for `addAll` and `CapturedEntries` for map bulk writes while restoring navigation atoms. Do not replay stale templates that overwrite later behavioral changes. | Explicitly guarded by PR #1244. Exact source-sealed recipe is canonical in Synexia PR #12235. |
| JNI boundary | Primitive mask/index kernels only | Do not move arbitrary Java Predicate/Comparator callbacks across JNI: callback crossings add exception, GC-lifetime, and reentrancy contracts without demonstrated benefit. | Existing primitive owners remain authoritative; no JNI ABI change is required for filtered-view navigation. |

The reported 90,431 check count and merge metadata are recorded from the linked PR's own evidence. They do not establish that every JUnit, JNI, JDK-image, jtreg/JCK, cross-platform, or performance gate passed.

## Problem categories

| ID | Category | Canonical approach | Required edge cases | Complexity target |
|---|---|---|---|---|
| COL-001 | Filtered view membership | Delegate membership to the predicate and backing collection without materializing a second collection | predicate false, duplicate values, nulls where permitted, backing mutation, iterator remove | O(n) traversal; O(1) wrapper storage |
| COL-002 | Sorted range views | Use the source comparator and explicit lower/upper-bound predicates; distinguish inclusive/exclusive endpoints | query below/above bounds, equal endpoints, reversed ranges, empty ranges, null comparator behavior | O(log n) navigation on tree/index-backed owners |
| COL-003 | Comparator-equivalent membership | Use comparator equivalence where SortedSet/NavigableSet contract requires it, not Object.equals alone | distinct objects comparing as zero; asymmetric equals; null accepted by comparator | O(log n) lookup for indexed owners; bounded fallback may be O(n) |
| COL-004 | Bounded navigation | Return null when no element satisfies navigation relation or view bounds; do not leak out-of-range backing elements | lower/higher/floor/ceiling at both edges; empty view; queries outside view | O(log n) ordinary path; documented exceptional recovery |
| COL-005 | Stable mask compaction | Count selected states, allocate exact output, then emit logical positions in ascending order | zero mask, all masks, empty input, offset windows, overflow, old native ABI | O(n + k) time, O(k) output |
| COL-006 | Offset/index arithmetic | Validate ranges using subtraction-based bounds checks and checked addition before indexing | negative offset/length, integer overflow, end-exclusive boundary | O(1) validation |
| COL-007 | Sorted bulk mutation | Apply one comparator-consistent stable ordering strategy and preserve documented mutation/exception behavior | duplicate keys, comparator ties, partial failures, nulls, empty input | O(n log n) sort, O(n) merge where applicable |
| COL-008 | Filter composition | Compose predicates without recursively nesting traversal wrappers where safe | predicate evaluation order, exceptions, deep nesting, short-circuit behavior | O(n × p) upper bound for p predicates; avoid recursion-depth growth |
| COL-009 | Stored representative identity | Apply visibility to the actual stored representative, not a lookup alias | comparator-equivalent non-equal keys; identity sets; null keys | O(log n) seek; O(n) exceptional recovery |
| COL-010 | Primitive selection and state masks | Use existing byte/int/long state lanes and logical offsets before introducing new storage | every required mask, sparse/dense rows, empty windows, native ABI fallback | O(n + k); allocation proportional to output |
| COL-011 | Snapshot versus live view | Name and test the lifetime contract explicitly; never silently materialize a live view | owner mutation, iterator binding, predicate state changes, memory retention | O(1) view construction; snapshot costs must be explicit |
| COL-012 | Map-derived views | Preserve entry immutability, key-set add restrictions, comparator equivalence, and source mutation semantics | entry setValue, key-set add/addAll, values removal, clear, nested ranges | owner-dependent; avoid per-entry retained wrappers |

## Candidate evaluation rules

1. Prefer the existing JDK implementation as the behavioral oracle for standard collection contracts.
2. Compare current owner, JDK oracle, and at least one algorithmic alternative by correctness, asymptotic cost, allocation profile, compatibility, and maintainability.
3. Treat LeetCode, HackerRank, and GeeksforGeeks problem/category references as discovery and taxonomy evidence only. Do not copy challenge statements, editorials, or solution bodies into production code.
4. For GitHub donor code, pin repository, immutable commit, source path, license, copyright/attribution requirements, and the exact adapted atom. Reject candidates with unclear license or incompatible API/behavior. Prefer algorithmic references over source copying when a JDK contract is the actual authority.
5. Compare Guava filtered views, Eclipse Collections selection/ranges, Apache Commons predicated wrappers, and Spring collection adapters only as contract-specific candidates. They are not interchangeable: eager selection, insertion guards, live views, and mutable filtered views have different observable behavior.
6. Java remains the oracle. JNI is optional acceleration and must preserve Java fallback semantics. Native code must validate offsets/lengths, respect JNI region bounds, avoid retaining pointers, and be tested with `-Xcheck:jni`.
7. Do not promote a candidate based on benchmark results alone. Contract and differential tests are mandatory.

## Wrapper contract matrix

Run each applicable case against both the M3 implementation and a JDK reference:

- `subSet`, `headSet`, `tailSet`, and nested views with inclusive/exclusive endpoints.
- `lower`, `floor`, `ceiling`, and `higher` for in-range and out-of-range queries.
- `contains`, `remove`, and entry-set membership for comparator-equivalent but non-equal keys.
- Comparator-defined null support, including null at the beginning, middle, and end.
- Empty views, singleton views, duplicate comparator keys, and reversed bounds.
- Filtered wrappers with predicates that accept none/all/some elements; nested filters; iterator removal; backing collection changes.
- Exceptions and mutation visibility, including unsupported mutation and invalid range arguments.
- Map-derived view contracts: immutable/read-only entries where promised, key-set addition, values removal, bulk-write input capture, and nested range mutation.
- Deterministic iteration order and stable tie ordering where the contract requires it.
- Offset and mask contracts: empty windows, all masks, sparse/dense selections, zero mask, overflow-safe boundaries, and Java/native result parity.

Property/differential tests should generate small comparators and data sets, then compare return values, exceptions, iteration sequences, and resulting backing state. Tests must not assume that all sorted collections accept null; use both null-rejecting natural ordering and explicit null-friendly comparators.

## Recipe work order (serial per file)

For each file:
1. Record path, owner, public signatures, callers, contract tests, Java/JNI boundary, and current SHA-256.
2. Assign problem-category IDs from this catalogue and record incumbent implementation plus candidates considered.
3. Select one atom and explain why alternatives were rejected.
4. Apply the pinned Maven/OpenRewrite recipe to that file only. Do not hand-edit generated output to conceal recipe defects; repair and retest the recipe.
5. Verify unchanged public signatures and compare observable behavior against the JDK oracle.
6. Run focused Java tests, relevant JNI tests, and module integration tests.
7. Record postimage hash, test commands/results, donor provenance, and unresolved limitations.
8. Continue to the next file only after the previous file is green.

## Donor provenance record template

| Field | Required value |
|---|---|
| Category ID | One or more COL-* identifiers |
| Repository and commit | Immutable GitHub commit SHA |
| File / symbol | Exact path and implementation atom |
| License | SPDX identifier and compatibility decision |
| Adaptation | What was reused; what was independently implemented |
| Correctness evidence | Differential/property tests |
| Performance evidence | Reproducible benchmark, allocation and complexity notes |
| Decision | Accept / reject / defer, with rationale |

## Native promotion gate

No native atom is promoted until:
- the Java implementation and JDK differential oracle pass;
- native and Java results match across empty, boundary, overflow, and randomized cases;
- ABI capability detection and old-library fallback are tested;
- JNI local references, array regions, and exception paths are audited;
- the benchmark demonstrates a meaningful improvement without contract regressions.

## Current evidence boundary

The bounded-navigation and mask-position changes are already represented by merged implementation PRs #1244 and #1246; they should be reused, not duplicated. This catalogue PR is documentation-only. Remaining validation gaps include the blocked owning Maven/JUnit dependency resolution and separately pending JNI, whole-JDK/jtreg/JCK, cross-platform, GC/JMM, and benchmark gates. No whole-repository completion, five-provider parity, or upstream JDK/SWT promotion is claimed.
