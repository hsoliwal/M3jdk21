# M3 collection-wrapper algorithm catalogue

Status: candidate catalogue and verification contract; entries are not implementation claims until the named tests pass on the target revision.

## Invariant

M3 collection wrappers must preserve the wrapped collection's public Java contracts and externally observable behavior. Optimizations may replace internal behavioral atoms only when the composed implementation is observationally equivalent. Process files serially: inventory owner and callers, record the baseline, apply one recipe, run focused contract tests, then run integration tests before proceeding.

Dynamic-array representation is an implementation strategy, not a license to weaken collection contracts. Keep logical size distinct from capacity; use checked offset/index arithmetic; treat state masks as logical state only where the owner defines that encoding; preserve iteration order, comparator semantics, null policy, fail-fast behavior, and view mutability.

## Problem categories

| ID | Category | Canonical approach | Required edge cases | Complexity target |
|---|---|---|---|---|
| COL-001 | Filtered view membership | Delegate membership to the predicate and backing collection without materializing a second collection | predicate false, duplicate values, nulls where permitted, backing mutation, iterator remove | O(n) traversal; O(1) wrapper storage |
| COL-002 | Sorted range views | Use the source comparator and explicit lower/upper-bound predicates; distinguish inclusive/exclusive endpoints | query below/above bounds, equal endpoints, reversed ranges, empty ranges, null comparator behavior | O(log n) navigation on tree/index-backed owners |
| COL-003 | Comparator-equivalent membership | Use comparator equivalence where the SortedSet/NavigableSet contract requires it, not Object.equals alone | distinct objects comparing as zero; asymmetric equals; null accepted by comparator | O(log n) lookup for indexed owners |
| COL-004 | Bounded navigation | Return null when no element satisfies the navigation relation or view bounds; do not leak out-of-range backing elements | lower/higher/floor/ceiling at both edges; empty view; queries outside view | O(log n) for tree/index-backed owners |
| COL-005 | Stable mask compaction | Count selected states, allocate exact output, then emit logical positions in ascending order | zero mask, all masks, empty input, offset windows, overflow, old native ABI | O(n + k) time, O(k) output |
| COL-006 | Offset/index arithmetic | Validate ranges using subtraction-based bounds checks and checked addition before indexing | negative offset/length, integer overflow, end-exclusive boundary | O(1) validation |
| COL-007 | Sorted bulk mutation | Apply one comparator-consistent stable ordering strategy and preserve documented mutation/exception behavior | duplicate keys, comparator ties, partial failures, nulls, empty input | O(n log n) sort, O(n) merge where applicable |

## Candidate evaluation rules

1. Prefer the existing JDK implementation as the behavioral oracle for standard collection contracts.
2. Compare current owner, JDK oracle, and at least one algorithmic alternative by correctness, asymptotic cost, allocation profile, compatibility, and maintainability.
3. Treat LeetCode, HackerRank, and GeeksforGeeks problem/category references as discovery and taxonomy evidence only. Do not copy challenge statements, editorials, or solution bodies into production code.
4. For GitHub donor code, pin repository, commit, source path, license, copyright/attribution requirements, and the exact adapted atom. Reject candidates with unclear license or incompatible API/behavior.
5. Java remains the oracle. JNI is optional acceleration and must preserve Java fallback semantics. Native code must validate offsets/lengths, respect JNI region bounds, avoid retaining pointers, and be tested with `-Xcheck:jni`.
6. Do not promote a candidate based on benchmark results alone. Contract and differential tests are mandatory.

## Wrapper contract matrix

Run each applicable case against both the M3 implementation and a JDK reference:

- `subSet`, `headSet`, `tailSet`, and nested views with inclusive/exclusive endpoints.
- `lower`, `floor`, `ceiling`, and `higher` for in-range and out-of-range queries.
- `contains`, `remove`, and entry-set membership for comparator-equivalent but non-equal keys.
- Comparator-defined null support, including null at the beginning, middle, and end.
- Empty views, singleton views, duplicate comparator keys, and reversed bounds.
- Filtered wrappers with predicates that accept none/all/some elements; nested filters; iterator removal; backing collection changes.
- Exceptions and mutation visibility, including unsupported mutation and invalid range arguments.
- Deterministic iteration order and stable tie ordering where the contract requires it.

Property/differential tests should generate small comparators and data sets, then compare return values, exceptions, iteration sequences, and resulting backing state. Tests must not assume that all sorted collections accept null; use both null-rejecting natural ordering and explicit null-friendly comparators.

## Recipe work order (serial per file)

For each file:
1. Record path, owner, public signatures, callers, contract tests, Java/JNI boundary, and current SHA-256.
2. Assign problem-category IDs from this catalogue and record existing implementation plus candidates considered.
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

This document defines the reusable review vocabulary and required evidence. It does not assert that any production wrapper defect has been fixed. Link each accepted implementation PR and its CI run here only after the exact target revision and tests have been verified.
