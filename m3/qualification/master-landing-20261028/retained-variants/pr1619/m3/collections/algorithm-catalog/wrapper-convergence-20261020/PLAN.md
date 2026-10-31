<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0. -->
# M3JDK21 collection and JNI forward reconciliation

## Product and immutable inputs

M3JDK21 is the integrated product. Synexia is the convergence workspace and the
canonical owner of reusable transformation recipes. This work introduces no
Synexia service, Maven dependency, or com.synexia package into the runtime.

Receiving baseline: `hsoliwal/M3JDK21` commit
`2779aba3f98f31c324be43f731be7914e1a05935`.
Recipe owner: `hsoliwal/com.synexia` develop commit
`ec9dc4fdf7d1d1c0fa269f26cafc26fd2d251fbc`, existing
`synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017`.
New, additive dataset: `m3-wrap-jni-20261020`. Historical fixtures and decisions
remain immutable. Engine owners are reused unchanged.

## Contract and storage

Preserve the current public/protected signatures, primitive domains, occurrence
identity, sorted comparator equivalence, live backing, validation precedence,
callback order and failure identity, stable ties, physical offsets, state-mask
interpretation, JNI signatures, and lifetime boundaries. Existing defect repairs
are identified explicitly; a changed incorrect observation is not described as
bit-for-bit behavioral equivalence.

Live views retain their real owner and predicate metadata. Index projections
share payload arrays and manipulate primitive index windows. No retained
per-entry wrappers or second authoritative payload store are introduced. A
snapshot's explicit copy remains a snapshot contract, and predicate metadata is
not cached element state. Unknown owner generations never authorize mask reuse.

## Serial implementation passes

1. Freeze current owners, donor files, regression contracts, source hashes and
   FOSS decisions before receiving any source changes. Build the unchanged
   `m3/collections/pom.xml` with its source-owned JUnit dependency family.
2. Receive the existing strict sequenced endpoint and reversal repairs in
   `M3FilteredViews.java` and `M3FilteredNavigableSet.java`; reuse PR 1554's
   endpoint regression contract and JUnit bridge. Keep existing split metadata.
3. Receive PR 1598's owner-dispatched bulk removal atom in
   `M3CollectionViews.java`. CopyOnWriteArrayList publishes its survivor array
   only after callbacks succeed. Other owners preserve their existing mutation
   and partial-failure behavior. Reuse its occurrence/alias/failure tests.
4. Evaluate PR 1538's cursor validation and stable equal-run reversal. The
   cursor guard is REJECTED: it reads an unrequested lazy-list element and
   still misses empty/exhausted LinkedList.subList structural ABA. Preserve
   its exact source and counterexamples in diagnostics. Receive only the
   stable equal-run reversal in `M3LongIndexProjection.java`, retaining current
   address kernels, offsets and all newer methods. Reuse the stable-index-window
   and allocation probes, including the existing JUnit bridge from PR 1598.
5. Receive PR 1570's deep primitive traversal plan in
   `M3PrimitiveFilteredSequences.java`. Preserve shallow traversal, ordered
   direct predicate calls and exceptions; metadata contains predicates only.
   Run its randomized, hostile-callback and deep-traversal contract.
6. Forward-reconcile the existing proposed `java.util.CollectionViews` owner
   from PR 1551 with qualified current collection atoms. Compile and execute
   it as a bootstrap `java.base` class. Preserve all seven proposed factories;
   do not introduce public M3 facade names. Preserve its actual Apache notice
   and explicit core-intake review gate; a recipe cannot relicense a donor.
7. Receive the existing `jdk.internal.m3.M3IndexMaskWindow` Java and libjava C
   owners from PR 1557, correcting backend availability and zero-work native
   refusal using the existing two JNI exports. Reuse its bounded oracle; add
   direct-scan, offset/mask/alias/guard, missing-symbol and runtime-origin tests.
   Compile generated headers and actual target native sources; execute real
   native parity with `-Xcheck:jni`, including an installed image when viable.
8. Qualify the named OpenRewrite recipe, exact SDK Results, fixed point,
   input-order replay, missing/duplicate/drift refusals, actual receiver and
   second application. Record attributed parse scope separately from semantic
   proof. Apply selected source files serially, preserving all unrelated files.
9. Run the original collection suite, standalone hostile contracts, target
   java.base tests, JNI gates, source-intake checks and independent review.
   Publish reviewable canonical recipe and M3JDK21 receiver pull requests with
   exact source pins, commands, result counts and unresolved gates.

## Deliberately unresolved contract observations

The existing blanket IllegalArgumentException recovery in bounded navigation
can swallow user comparator exceptions. The older neighbor-anchor replacement
is already rejected because it changes the callback schedule. A generic opaque
NavigableMap cannot distinguish these exception origins through its public
interface without further effects. Preserve the exact failure witnesses and
the rejected decision. An owner-level range admission boundary is a separate
future receiver design, not a license to weaken an existing oracle.

The strict read-only list's indexed iterator misses raw-owner invalidation.
Switching it to the delegate iterator changes predicate demand at construction
and traversal. Do not label that substitution contract-preserving. Retain both
frozen effect and fail-fast witnesses and record the missing owner-generation
boundary explicitly.

## Gates and claim boundaries

Run Java 21 compilation with the owning module's warning policy; the original
Maven POM is not replaced with a binary-only dependency shortcut. Tests added
from donors first execute against the unchanged baseline. Counterexamples and
performance observations remain distinct. No speedup, full VM build, global
family completion or Eclipse-wide transformation is inferred from finite tests.

The repository's ordered promotion remains STRING -> ARRAYS -> COLLECTIONS ->
AST_COMPILER -> REMAINING_FAMILIES. Reviewable source proposals and installed
module experiments do not advance that gate. Full target VM/platform/GC/CDS,
license-intake and unvisited Eclipse marks remain explicitly pending wherever
they have not actually been qualified.

## Review-driven scope corrections

The source-owned JUnit platform launcher verifier is restored from existing PR
1566: its manifest stores repository-relative paths, while the old verifier
resolved them relative to `m3/vendor` and then expected a nonexistent revision
field. The unchanged 88-file source/legal inventory and built JAR are the
independent regression input; no JUnit implementation source is changed.

PR 1598's Eclipse bundle-order extension was inspected. Its three new public
sorted view methods are deferred under this pass's interface freeze. Existing
Eclipse marks remain pinned retrieval inputs, not a claim that files have been
visited or transformed.
