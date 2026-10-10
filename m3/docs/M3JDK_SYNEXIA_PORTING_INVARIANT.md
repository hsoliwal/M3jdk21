# M3JDK Synexia Porting Invariant

Status: canonical M3JDK engineering policy.

## Authority split

**Synexia is the canonical donor-convergence and reusable M3 recipe workspace. M3JDK is the canonical JDK product/runtime target.**

Synexia owns reusable Maven/OpenRewrite recipes, recipe/atom contracts, donor convergence,
algorithm/problem catalogues, M3Index/MIndex reusable implementations, and reusable JNI/Java
transformation logic. M3JDK may consume those assets under their reviewed Apache-2.0/provenance
contract, but it must not evolve a competing reusable implementation.

M3JDK remains authoritative for JDK product/build/runtime promotion, target-specific OpenJDK
backports, target naming, bootstrap/runtime integration, jtreg compatibility and final release
decisions.

M3JDK receives reusable capabilities only through an explicit proof-bound handoff/adaptation packet.
Any copied canonical Synexia implementation that still exists in M3JDK is frozen migration residue:
improve the Synexia owner first, then refresh the target handoff; never patch the residue directly.

## Mandatory rule for every LLM/source-changing task

Every source-changing LLM task must have a Maven/OpenRewrite recipe identity and a content-addressed
recipe crate. Work on the recipe and its proof until the resulting postimages are correct. Direct
per-file edits may be useful while exploring, but they are not canonical delivery unless the exact
postimages are owned by the recipe and replay reaches a fixed point.

Acceptance authority is mechanical:

1. exact source/preimage identity;
2. parser/round-trip custody;
3. compiler success;
4. unchanged public/internal contract unless change is explicitly authorized;
5. JUnit/differential behavior;
6. atomization/reconstruction where applicable;
7. Java/JNI differential parity for native lanes;
8. bounded permutation/combination and multipass convergence for transformations;
9. deterministic content roots;
10. rollback/revert path.

An LLM may propose recipes, signals, precomputations, donor adaptations and counterexamples. It may
not convert a failed compiler/test/contract/native gate into a pass.

## Naming

Synexia names are donor names, not target ABI.

Target naming follows M3JDK ownership:

- String-family runtime: `M3String*`.
- Indexed/internal String-adjacent structures: `M3Index*` only when indexed identity is an actual
  target contract.
- General target-owned implementation types: concise `M3*`.
- Never mechanically rename `MIndex*` or `MatIndex*` to `M3*` without a per-type semantic map.
- Never preserve a Synexia package merely because the source implementation was borrowed.

`MIndexString -> M3String` is a proven naming/ownership pattern, not a blanket textual rename rule.

## Promotion ladder

Capabilities move through explicit stages:

    Synexia experiment
      -> M3JDK isolated port (m3/ports/*)
      -> target-owned differential/parity proof
      -> concrete JDK-internal consumer
      -> java.base/bootstrap/JDK replacement only after dedicated compatibility + memory + CPU gates

Skipping stages requires a separately reviewed proof packet; it is never inferred from similarity.

## Precompute rule

Precompute is separated by authority:

- exact immutable facts may prune or answer only what their proof contract establishes;
- lower bounds may reject only when mathematically safe;
- SimHash/MinHash/fuzzy/code-likeness signals rank candidates only;
- regex shape analysis is candidate specialization only;
- JDK `Pattern`/Matcher remains regex authority until a replacement proves each operation and
  mutable matcher state;
- compiler/tests remain Java transformation authority.

Do not put large corpus indexes, pairwise matrices, fuzzy graphs or search-service state inside
`java.lang.String`. Keep them in target-owned sidecars/services and promote only bounded facts that
have a concrete runtime consumer.

## Collections rule

Collections are migrated one concrete semantic owner at a time. There is no universal
`M3Collection` replacement.

For every JDK collection record and prove:

- ordering and encounter order;
- null rules;
- equality/identity semantics;
- iterator/spliterator behavior;
- backed views;
- callback/reentrancy semantics;
- serialization/clone/protected hooks;
- concurrency and memory-model guarantees;
- complexity and retained-memory effects.

M3 primitive/reference lanes may replace storage nodes only behind that membrane. A smaller internal
representation is not accepted if it changes public or supported internal behavior.

## Scope discipline

Default transformation scope is the smallest sealed contract boundary, normally FILE. The exact
authority order is:

    FILE -> VISIBILITY -> PACKAGE -> MODULE -> MULTI_MODULE -> LIBRARY_API

The FILE pass may atomize/patternize repeatedly until its recipe reaches a tested fixed point,
provided the interface, observable behavior and contract remain unchanged. Separate FILE workers
may fan out independently; passes inside one file remain serial and deterministic. Promotion only
occurs when an observed visibility/package/module/reactor/API dependency actually crosses that
boundary, with a new proof gate. An LLM cannot infer broader edit authority for convenience.

## Retirement of copied Synexia recipe sources

The canonical Synexia owner audit and the M3JDK21 exact Git-blob freeze must be reconciled against
one pinned Synexia commit before any copied recipe sources are deleted. The current pinned snapshot
has 17 copied `com.synexia.*` Java files: 11 byte-identical canonical mirrors, two divergent
versions requiring API/behavior/resource parity (hash-pinned Java and segmented-lane native), and
four `com.synexia.rewrite.scope.*` paths without a same-path canonical owner at that pinned commit.

These are typed migration residues, **not** proof of semantic equivalence. Synexia's canonical
recipe-parity manifest records each exact path and Git blob. Do not reintroduce the four missing
packages as duplicate algorithms merely to make the consumer compile. Map them to existing
Synexia scope owners through separately tested adapters or explicit contract-preserving migration.

Consumer retirement requires an exact canonical Git commit/blob pin, an externally resolved
Synexia Maven/OpenRewrite artifact, Java 21 JUnit behavior/contract parity, fixed-point
receipt, and affected target/JDK tests. Until these pass on the exact head, copies remain frozen
and their retention must not be reported as completed removal.

## Donors

Reuse existing Synexia/M3 implementations first. External FOSS and challenge repositories may
provide algorithms, test shapes and design evidence only under explicit provenance/license review.

LeetCode, HackerRank and GeeksforGeeks are category/problem-shape sources for adversarial and
boundary fixtures. Do not copy challenge/editorial bodies merely to enlarge coverage.

## Develop/master history

Preserve ancestry. Do not rebase the historical convergence branch to manufacture a cleaner story.
Forward-merge moving bases, retain source provenance and keep failed/intermediate evidence when it
materially explains the accepted design.

## Completion language

Use scoped states such as PROPOSED, IMPLEMENTED, VERIFIED_SCOPED, PORT_CANDIDATE and PROMOTED.
Never call a family “complete” because one benchmark/test passed. Whole-JDK completion requires
per-owner closure and repository/JDK gates.
