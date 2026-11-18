<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 semantic impact frontier — source-bound receiving contract

**Schema:** `M3_IMPACT_FRONTIER_V1`. **Status:** candidate tooling, not JDK admission.

`M3ImpactFrontier` is an additive, read-only owner under the existing
`m3/tooling/atom-census` module. It reads the exact `AtomCoverageLedger` and
does not introduce a second parser, source index, graph database, scheduler,
JNI entry point or JDK runtime dependency.

## Input and effect direction

A dependency edge is `dependent -> prerequisite`. On a changed prerequisite,
the algorithm traverses the *reverse* edges to invalidate downstream consumers.
Every reached atom widens to **all atoms in the same source file** because a
file is the canonical independently serialized mutation boundary.
Repeat until no new atoms/files remain. Cycles are bounded by visited bits.

All known dependencies must be present in the input closure. Unknown atoms,
malformed IDs, empty seeds, null edges and excessive input edges are errors.
The plan may not guess absent dependency edges or infer a module from a path.
The `owner` field returned is the owner already in the validated atom ledger,
not a promoted JPMS/module API claim.

## Representation and complexity

The fixed-size in-memory graph uses sorted atom IDs, file indexes and source
owners; file-to-atom and prerequisite-to-dependent offset planes; primitive
adjacency arrays; and one bounded work queue with a visited lane.

For `V` atoms, `E` dependency edges and `F` file identities, building
the canonical relation costs `O(V log V + V log F + E log E)`. After
construction the closure costs `O(V+E)` worst case with `O(V+E+F)`
primitive storage. This is a source-tooling bound, not a JDK runtime benchmark.
No recursive Java stack or per-edge node allocation is required.

Duplicate edges are normalized; caller edge/seed ordering has no effect on
canonical output. The receipt hashes (length-framed UTF-8) the validated
atom/evidence ledger identity, sorted graph edges, seed IDs, affected atom IDs,
file IDs, source-ledger owner IDs and applicable admission findings.

## Failure-closed acceptance

An impacted unknown pattern, missing proof, public contract unlocked without
authorization or native/JNI gate not run remains an explicit blocker from the
existing `AtomCoverageLedger.admissionFindings()`. A blocker-free plan still
is not semantic proof. `mutationAuthority()`, `promotionAuthority()` and
`semanticAuthority()` are all false.

The caller must separately prove diff, lint, compilation, behavioral/ABI
contract, JUnit/jtreg, native runtime/JIT where relevant, performance and
fixed-point. This planner does not change `java.lang.Runnable`, `Callable`,
M3FS, JDT, SWT/Swing or the collection wrappers.

## Recipe custody and verification

The canonical generating recipe is reused from Synexia's
`M3HashPinnedJavaSnapshotRecipe`; this target PR records exact source/template
postimages, tests, task scope and source-bound algorithm selection. The generator
must be corrected and replayed on drift—do not patch its output as an exception.

```sh
bash m3/tooling/atom-census/verify-contract.sh
mvn -B -ntp -f m3/tooling/atom-census/pom.xml -Dtest=M3ImpactFrontierContractTest test
```

The standalone contract test covers same-file expansion, downstream graph
reachability, cycles, duplicate/reversed edges, unknown-seed refusal,
receipt-forgery refusal, pattern/evidence blockers, and an explicit
Java-native-declaration -> JNI_EXPORT -> C-sidecar dependency chain.

The Java 21 smoke check used API-compatible stand-ins for the ledger source;
real-ledger Maven/JUnit and assembled-JDK native gates must execute on the
exact PR head before promotion. A synthetic smoke check is not CI green.

Problem-category and donor decisions are recorded in
`IMPACT_ALGORITHM_CATALOG.tsv`. LeetCode, HackerRank, GeeksforGeeks and
GitHub materials informed *categories only*; no challenge code was copied.
