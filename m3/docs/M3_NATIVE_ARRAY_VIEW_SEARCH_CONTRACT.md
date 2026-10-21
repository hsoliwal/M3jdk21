# M3JDK21 native array, view, and composite-search contract

Status: design and verification inventory; **not** an implementation or proof of compatibility.

## Product boundary

M3JDK21 is an OpenJDK-derived JDK product. M3 primitives must converge into the actual Java 21 source and runtime implementation; adapters are temporary test fixtures, not the product architecture. The Synexia repositories are convergence/donor workspaces. No public contract is changed without explicit separate review.

## Structural invariants

* Own logical collection state in primitive/dynamic arrays with offsets, indexed directories, masks and flags. Avoid persistent entry objects; materialize public API facades only where required by Java contracts.
* Prefer immutable payloads and compositional text ranges; materialize contiguous text only at explicitly necessary API/native boundaries.
* Preserve comparator, null, equality, ordering, fail-fast, backed-view, serialization, concurrent visibility, and identity/lifetime semantics. A cached Map.Entry facade must never silently become a different entry after structural mutation.
* Track UTF-16 code-unit coordinates for Java regex compatibility. Separate byte, code point, grapheme, and line/column coordinates.
* JNI ownership, global/local references, GC relocation, pin duration, and native buffer lifetimes require explicit verification; raw Java array addresses must not be retained after release.

## Category catalogue schema

Each candidate record: category, operation, source repository/URL, license/attribution, baseline file and revision, candidate algorithm, preconditions, asymptotic bounds, allocations, compatibility hazards, differential tests, benchmark environment, decision, and measured evidence. Record rejected candidates and reasons. No unverified performance claim.

Initial categories:
- dynamic-array growth, segmentation, compaction, indexed shifts, and pressure-aware spill
- mask-based state, bitmap operations, rank/select, indexed filtered views
- sorted/navigable views, stable ordering, comparator consistency, bound propagation
- collection wrappers, iteration, mutation, spliterators, streams, and parallel keyed partitioning
- rope directories, prefix lengths, long offsets, slice and cursor traversal
- literal search (KMP/Two-Way), multi-pattern search, boundary-spanning search
- regex precompute, bounded-window proof, streaming automata, backreference/context fallback
- JNI native text/array access, release discipline, and lifetime-safe views

## Search correctness gates

1. A rope-backed CharSequence can reuse java.util.regex only for an int-indexed range; no claim of arbitrary-length matcher support.
2. Fixed overlap is valid only when finite match width and all required context are proven. Anchors, lookbehind, backreferences and unbounded matches require separately verified execution.
3. Compare match order, start/end offsets, captures, empty-match advancement, flags and Unicode semantics against JDK regex on bounded flattened fixtures.
4. Repartition every fixture at every feasible leaf boundary; partitioning must not change the logical result.
5. For larger-than-int synthetic sources, verify long-offset arithmetic, overflow checks, cancellation, bounded memory, and non-flattening.

## Wrapper correctness gates

Filtered views: membership predicates, mutation propagation, predicate stability policy, iteration, and concurrent modification.
Sorted/navigable views: comparator, inclusive/exclusive bounds, descending composition, first/last, poll, key/value/entry views.
Unmodifiable/immutable wrappers: prohibited mutation through all views, iterators, default methods and spliterators.
Synchronized wrappers: shared mutex semantics, iteration documentation and visibility.
Entry facades: setValue, equals/hashCode, snapshot versus backed semantics, invalidation and cache safety.

## Mechanical acceptance protocol

Inventory each target file and public contract before changes. Select a catalogue candidate, write a tested Maven/OpenRewrite recipe where appropriate, execute one serial file-local pass, record the diff and diagnostics, compile, run focused differential tests, and benchmark. Keep recipes, tests, provenance and generated changes together in a reviewable PR. Escalate FILE -> PACKAGE -> MODULE -> REACTOR -> API only with separate proof. Do not claim equivalence from compilation alone.

## Planned implementation slices (not yet delivered)

A. Collection storage/view inventory and primitive-array contract tests.
B. Filtered/sorted/navigable wrappers with differential JDK tests.
C. Rope cursor and cross-leaf literal search.
D. Regex state preservation and supported-pattern dispatcher.
E. JNI lifetime-safe native traversal and integration tests.
F. OpenRewrite recipes, category catalogue, and benchmark evidence.

This document defines acceptance criteria only; no source changes, test execution, or performance measurements are implied.
