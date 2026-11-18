<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3SDK Machine Oriented Programming — core invariant

**Status:** core product-policy proposal, REVIEW_ONLY until Synexia's
canonical text recipe and relevant target gates actually run.
The intent is a compatible SDK superset of the JDK, ultimately
qualifying separately for an Apache-2.0-only distribution.
Neither the complete JDK atomization nor runtime/license
admission is asserted by this document.

## Authority hierarchy

The controlling Synexia execution canon is
`docs/M3-SCALE/M3Scale.txt` **v3.3 Locked Canon**, observed
Git blob `e513ac2a73365d45d33623db449b0f698bf97746`.
Follow its documentation-before-code, inventory-before-change,
sealed contracts, deterministic partitions, per-file locks,
leaf-first recompression, parallel candidates/serial truth,
ordered proof and fixed-point/receipt laws.

Canonical Java atomizer/Patternizer, source/logic/structure/
SimHash candidates, source-bound L0–L5 documentation,
Maven/OpenRewrite recipes and correction memory remain
exclusively in Synexia. Reuse the already merged
`synexia-openrewrite-recipes/docs/M3_ATOM_HIERARCHY_DOCUMENTATION_PASS_20261118.md`
and `docs/M3-SCALE/invariants/SYNEXIA-M3-RECIPE-CORRECTION-1.md`;
no second parser, private source mirror, pattern engine or
evidence registry in public M3JDK21.

Three M3Scale reference files were supplied and independently
SHA-256 verified. `M3Scale(3).txt` is the historical v3.0
Locked Canon witness; `M3ScaleCodingAgent(3).txt` is
derived coding-agent architecture; `M3ScaleCopy(3).txt`
is an expanded serialized witness. They are not equivalent
files and none silently replaces the live v3.3 canon.
The user's later Foundational Principle expands the MOP SDK
design intent but is not compiler/runtime proof.

The source-authority ledger, exact input hashes, and full
machine-readable pattern/contract proposal are retained
in [Synexia MOP SDK PR #18789](https://github.com/hsoliwal/com.synexia/pull/18789).
Synexia remains the canonical private recipe and evidence
owner; no private implementation, source mirror, or
recipe is copied into this public SDK target.

## MOP-1. Every eligible file consists of documented atoms

Preferred source normal form:

`LOCKED EXTERNAL INTERFACES
 -> SMALL PUBLIC/PROTECTED FACADE
 -> ORDERED COMPOSITION OF VERIFIED ATOMS
 -> PRIVATE STATIC OR PRIVATE INSTANCE ATOMS
 -> EXPLICIT DATA / STATE / NATIVE EFFECTS`

Private static is preferred for behavior independent of
receiver state; private instance atoms retain identity,
field, synchronization and lifecycle contracts. Neither
private nor static alone guarantees purity. A public
method or constructor that cannot be safely reduced
remains a preserved incumbent. No forced splitting
where exceptions, evaluation order, native/GC lifetime,
volatile state, class initialization, binary linkage,
serialization, security, short-circuit or callbacks
would change.

Public APIs, interfaces, signatures, annotations and
overrides are the **semantic skeleton**. Exact inline
Javadoc/comments are the **semantic memory**. Each
atom documents its purpose, preconditions, typed
inputs/outputs, effects, state ownership, error paths,
invariants, test oracles, complexity, performance
envelope and license. Each file has one purpose and
one authoritative implementation owner. The outer
methods compose rather than duplicate proven atoms.

## MOP-2. Top-down Atomizer, physical decomposition

`REPOSITORY -> PROJECT/REACTOR -> MODULE/COMPONENT
 -> PACKAGE/NAMESPACE -> FILE -> CLASS/INTERFACE/TYPE
 -> FIELD/CONSTRUCTOR/INITIALIZER/METHOD/LAMBDA
 -> STATEMENT/EXPRESSION -> BEHAVIORAL ATOM`

Read the actual repository, history, branches, PRs,
module descriptors, types, interfaces, fields, methods,
imports, implementation and reverse callers before
refactoring. A leaf has exact source commit/path,
byte coordinates and SHA-256, contract-bearing
signature, visibility, annotations, effects/exceptions,
thread/GC/JNI ownership, dependencies, comments and
tests. More detailed or sensitive source data belongs
in authorized Synexia inventories, not new JDK-side
candidate catalogues.

Use appropriate parsers for Java, native C/C++/JNI,
build files and other SDK languages; unparsed coverage
stays explicit. Hash/shape or name similarity is only
a candidate match, not semantic equivalence.
Do not split across file boundaries without declaring
the enlarged external contract and affected callers.

## MOP-3. Bottom-up Patternizer, semantic recomposition

`VERIFIED ATOM -> FILE COMPOSITION -> PACKAGE
 -> INTERFACE/API CONTRACT -> MODULE -> PROJECT
 -> REPOSITORY -> SYSTEM/SDK`

Interface/API reconciliation is a semantic gate, not
a fabricated physical directory in the file hierarchy.
Every parent may seal only verified affected children.
The dependency/effect graph, not the source path
alone, determines upward impact.

A reusable pattern has canonical identity and version,
applicability and preconditions, interface/effect
contracts, stable composition DAG, known alternatives,
source/license provenance, counterexamples, actual
behavior and performance receipts, and accurate docs.
Pattern evidence follows
`CANDIDATE -> REPEATED -> PROVEN -> PROMOTABLE ->
CANONICAL`; only designated serial authority can
admit canonical patterns. No automatic equivalence
from LogicHash, StructuralHash, SimHash or a generated
summary. Preserve all previously accepted API behavior.

## MOP-4. Parallel independent files, serial truth

Independent source files may be inventoried and have
candidates prepared in parallel only after exact
contract, reverse dependency and effects boundaries
are sealed. Use sorted deterministic manifests and
partitioning, one active writer and lock per file.
Workers are candidate producers, never the authority
for a canonical write or merge. Cross-file effects,
shared module interfaces or native lifecycle require
a larger locked proof scope. Serialize product
promotion, preserve hashes and exact history, and
resume only from the last verified checkpoint.

## MOP-5. Incremental multipass and verification law

0. Inventory history, actual source/owners/contracts,
   prior verified changes and source hashes
1. Atomize top-down to typed leaf inventories and
   explicit caller and interface relations
2. Identify existing Synexia patterns and competing
   qualified donors; prefer an admitted incumbent
   unless a compatible improvement is demonstrated
3. Document each atom and pattern **before code change**
4. Apply only the canonical source-pinned
   Maven/OpenRewrite recipe serially per file/atom
5. Verify **diff -> lint -> compile -> tests ->
   runtime** in that order, plus applicable
   JNI/GC/ABI/resource, security, concurrency
   and performance gates
6. Recompose bottom-up through changed contracts;
   regenerate only invalidated parent documentation
7. Verify idempotent second pass and fixed point,
   record source/postimage hashes, status, failure
   receipts, last-checked history and next work

Every execution has bounded scope and declared
stop conditions. There is no universal method limit
or number of passes guaranteeing convergence.
A change lacking contract/behavior proof is HOLD;
file-level parallelism does not authorize an
unverified system-wide merge.

## MOP-6. Coding-agent and LLM contract

Agents MUST read the source, inline semantic documentation,
canonical Patternizer registry, caller contracts,
history and previous correction lessons before coding.
Use typed `TaskPacket`, `InventoryRecord`,
`SemanticNode`, `PatternBinding`,
`ContractEnvelope`, `PlanNode`, `Candidate`,
`TypedResidue`, `ProofReceipt`,
`ContinuationState` and `StateRoot` where
owned by the canonical system. New patterns are
typed, unverified candidate residue until admitted.

An LLM need not memorize implementation details:
it consults source-bound patterns and their docs,
then produces only mechanically checkable proposals.
Pattern conformity does not itself guarantee correctness
or optimal code. "Best" means **best of explicitly
evaluated contract-compatible alternatives** using
worst-case complexity, CPU, allocations, retained
memory, GC, concurrency, security and platform
measurements. Never claim global optimality from
shorter code, an atom count or model confidence.

## MOP-7. SDK-wide scope and license boundary

MOP applies to `java.base` and other libraries,
`java.desktop`, HotSpot/JIT/GC, compiler/AST,
JMOD/jlink/jar/launcher, Maven, Gradle, Lucene,
OpenNLP, Felix/Atomos, JNI/OS, build, test and
release tools and verified SDK extensions.
Existing JDK public names, method descriptors,
API/ABI/JMM/JNI contracts and previous fork
features remain a backward-compatible subset.
Runtime admission order remains
`String -> arrays -> collections -> AST/compiler
 -> remaining SDK families`.

New independently authored expression may use
Apache-2.0 when eligible; upstream GPL/Classpath,
EPL and other licenses remain unchanged. LLM-based
atomization or translation does not erase copyright
in code expression or possible patent/contract rights.
Apache-only distribution requires a separate
source/binary, OS and dependency rights closure.

## MOP-8. Truth, receipts and continuation

Record per-file pre/post SHA-256, source and pattern
identity, exact affected APIs, docs, proof DAG,
compiler/lint/build/runtime logs, fixed-point result,
branch/PR/merge ancestry and last history checkpoint.
Report `PASS`, `FAIL`, `NOT_RUN`, `HOLD` or
justified `NOT_APPLICABLE` for each required gate.
Do not relabel a static candidate survey, historical
merge or policy PR as a completed transformation.

## MOP-9. Code as a documented and verifiable semantic algebra

Every eligible source file is a bounded composition of
**identified, contract-bearing semantic atoms**. Each atom
and composition has retrievable intent, interfaces,
dependencies, effects, exceptions, preconditions, state,
resource ownership, tests and proof status. Methods
may compose private/static atoms where appropriate, but
`private`, `static`, `pure`, `stateless` and `deterministic`
are distinct properties. Preserve cohesive source operations,
necessary identity, polymorphism, locks, volatile/JMM
semantics, callbacks and native/JNI/GC lifetime. An unsound
extraction is **HOLD/NO_SAFE_EXTRACTION**, not a refactor.

The MOP structure is bidirectional:

```text
TOP-DOWN PHYSICAL DECOMPOSITION
Repository -> Project -> Module -> Package -> File
           -> Types / Interfaces / Fields / Methods -> Semantic atoms

BOTTOM-UP SEMANTIC RECOMPOSITION
Verified atoms -> File composition -> Package/Interface contracts
               -> Module -> Project -> Repository -> System / SDK
```

An interface/API contract is a semantic boundary, not a
fabricated file-system level. The file is the principal
independent transformation scheduling boundary, not
a guarantee that effects stop at that file. Atomic
verification does not automatically prove file, package,
module or system correctness.

## MOP-10. Incremental multipass and persistent agent memory

A bounded multipass identifies the affected atoms,
traverses their call/effect/dependency closure, and
proposes transformations only within authorized scope.
Verified unaffected boundaries remain sealed.
When a contract or shared effect changes, the proof
frontier expands to the affected parent, interface,
module or runtime boundary; write scope remains
locked until separately admitted. Independent source
candidates may be prepared in parallel, but source
promotion, canonical history and state ratification
are serialized and recheck exact source identities.

Documentation is machine memory, not an afterthought.
Interfaces, annotations and signatures are the
formal skeleton. Javadocs plus atom, pattern, caller,
dependency, effect, source-history and verification
records are the semantic memory. Coding agents first
query those canonical facts, then instantiate admitted
patterns or templates; unsupported novelty becomes
typed semantic residue for separate analysis.
Mechanical retrieval reduces repeated context and
embedding dependencies without forbidding them.

MOP selects the **best demonstrated implementation**
against explicit contract-compatible alternatives and
measured worst-case behavior, CPU, allocation, memory,
GC, concurrency, security and platform evidence.
Structural constraints do not mathematically
guarantee globally optimal code, nor can an LLM
certify its own output. Documentation and source
must converge to an independently verified fixed
point, with accurate source and test receipts.

## MOP-11. Canonical ownership and evidence boundary

The detailed full MOP contract, current-v3.3 versus
uploaded-source reconciliation and proposed machine
records are in
[Synexia PR #18789](https://github.com/hsoliwal/com.synexia/pull/18789).
M3JDK21 contains only the product-facing invariant;
it never takes custody of canonical Maven/OpenRewrite
recipes, private donor images or correction-memory
stores. Existing Java 21 APIs, prior M3 capabilities
and legitimate third-party licensing stay protected.

This product documentation update has no JDK
implementation proof. The Synexia canonical
text-recipe execution, full JDK atomization,
source-built JDK compile/jtreg/JNI/GC/CDS,
cross-platform verification and Apache-only
distribution closure remain **NOT_EXECUTED**.


**This core rule is a proposal until the canonical
Synexia recipe, target JDK compile/jtreg/native
checks and applicable release gates execute.**
