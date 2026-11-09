# Whole-JDK M3 architecture and implementation-team handoff

Status: **documentation proposal; whole-JDK implementation and acceptance are incomplete**.
Inspection baseline: `45f546ff5bcb06a1b2604f14baf998785d98d9a1` in this repository, 2026-10-02.
This extends the existing migration handoff; it does not replace its source facts or create another capability registry.

## Reading and authority

Read this programme overview with the [whole-tree inventory and subsystem dispositions](whole-jdk-inventory.md), [collection replacement specification](whole-jdk-collections.md), [text operation specification](whole-jdk-text-operations.md), and [implementation packets, evidence and rollout](whole-jdk-execution.md). The existing [MIndex/MatIndex handoff](mindex-migration-handoff.md), [shared-atom String contract](shared-atom-concatenation.md), [worked text trace](migration-worked-text-trace.md), [acceptance matrix](migration-acceptance-matrix.md), [mapping lifecycle](migration-mapping-lifecycle.md), [worked port decisions](migration-worked-port-decisions.md), and [resume guide](../migration/docs/RESUME.md) remain applicable.

`m3/docs/name-mapping.json` and its existing schema are the retained target naming/capability authority. The proposed fields and packet labels in this document set are review requirements, not a second machine-readable authority or an implemented validator. Preserve the branch-scoped manifest and private source-obligation records until their explicit reconciliation; do not overwrite one with the other.

### Evidence boundary

The complete nonrecursive target `src` tree at the inspection pin contains 73 directories, 70 with module-like names. This is a complete **source-root directory census**, not a claim that 70 modules are built on every platform or that every API has been parsed, type-attributed, audited or migrated. The inventory document lists all 73 roots and identifies the deeper census still required. Selected implementation bodies and existing documentation were inspected; proposed backend models are labelled separately from those observations.

Target [draft #23](https://github.com/hsoliwal/M3jdk21/pull/23) and [draft #24](https://github.com/hsoliwal/M3jdk21/pull/24) are independent implementation lines. Their receipts do not certify this documentation tree, each other, or a later merged tree. Historical #6 receipts remain historical. Current source retention and incomplete mapping records are reconciled in the existing mapping documents. Nothing here reports a new build, benchmark, runtime test or conformance pass.

The private source repository was observed at `3db24805d640c72ab1bd637d83561696d99561a0`. Its [inventory-obligation draft #7696](https://github.com/hsoliwal/com.synexia/pull/7696) is a distinct branch-scoped evidence producer, not a public import permission or a replacement target registry. Detailed private owner findings stay in the source-side companion documentation. Access-controlled links are not an instruction to republish their contents.

## 1. Scope: the entire JDK, not a String facade

The programme covers public and internal libraries, runtime consumers, generated code, native implementations, build inputs, tests and tooling. String and collections are the first deep replacement designs, not the scope boundary. The inventory includes concurrency and thread scheduling, streams, IO and networking, numeric and time APIs, reflection and class loading, compiler/interpreter/JIT, GC and metadata, native interfaces, security, desktop/media/accessibility, XML/JDBC/naming/RMI, localization, tools and image assembly.

Every discovered capability needs a disposition with a reason: replace backend, adapt, reuse, retain pending evidence, platform-specific, blocked or deferred. Retaining an unsuitable implementation is a legitimate reviewed disposition, not permission to omit it. A directory-wide disposition is preliminary; mixed modules such as `java.base` must be decomposed into contract-level records before implementation. New or removed source roots reopen coverage review.

The goal is to remove unnecessary headers, structural nodes, boxing, copying, excess capacity and repeated computation **without changing observable Java contracts**. The target is not a universal representation, universal interning, a new framework inside `java.lang`, or a claim that every JDK object should become an integer.

## 2. Reuse ownership before designing storage

Use the semantic-owner distinctions already recorded in the MIndex/MatIndex handoff. Its atom store, joined/frozen text, resolver-local coordinates, AST, DAG, object/shape/state and domain-specific indexes are not interchangeable because their names resemble one another. The source-side lean collection owners provide primitive storage and geometry candidates; a primitive sorted set is not automatically a generic `TreeSet`, and a primitive ring is not automatically a blocking queue.

For each proposed reuse, record its actual qualified source owner, source pin, dependencies, existing tests, identity domain, lifetime, concurrency contract and materialization boundaries. Compare competing implementations before selecting one. Reuse a compatible atom or operation inside an existing owner rather than introduce a parallel collection hierarchy, dictionary, parser, interner, cache or registry. A missing or unverified owner remains an open decision; do not publish an invented class as an implemented component.

The source-side inventory adapter remains the inventory entry point. The retained migration tooling remains the target receipt/reconciliation entry point. OpenRewrite and narrowly source-pinned recipes remain transformation machinery, not runtime dependencies.

### 2.1 Source-first consolidation is a prerequisite, not just donor selection

The required sequence is **the best of Synexia consolidated into Synexia itself, then the best verified Synexia capabilities contributed to M3jdk21**. Do not leave Synexia fragmented while building a separately cleaned collection of its scattered PR variants inside the JDK. The source-side [atomization, patternization and repeated-pass invariant proposal](https://github.com/hsoliwal/com.synexia/blob/ac8adc40986f54a3762b43db093ab420d882d8ea/AGENTS.md#synexia-first-m3jdk21-second) extends the existing agent policy in [private companion #7725](https://github.com/hsoliwal/com.synexia/pull/7725). That documentation commit is not a source implementation/acceptance checkpoint.

Two independently verified stages govern the existing execution packets:

```text
Synexia current owners + competing variants + relevant historical capabilities
    -> repeated contract / atomization / patternization / recipe passes
    -> real Synexia consumer convergence and source verification
    -> pinned integrated Synexia capability checkpoint
    -> reviewed minimal dependency closure and JDK-specific adaptation
    -> repeated target compatibility / integration / measurement passes
    -> accepted M3jdk21 contribution for its explicit route and scope
```

This is an explanatory workflow, not a new executor or implemented gate. Target inventory and exploratory design can run while source consolidation proceeds; they do not bypass source readiness. Each exported capability must first have a retained, verified Synexia owner and the required dependency/consumer closure. Capability-sized checkpoints make progress finite without claiming the whole repository is finished. An unmerged source exploration can supply evidence, but is not labelled a consolidated upstream implementation. Existing target experiments remain recorded with their actual state rather than being deleted or retroactively declared accepted.

### 2.2 Atomization and patternization invariants

**Atomization fixes the contract while making the implementation replaceable.** A cohesive behavioral atom has explicit inputs, outputs, dependencies, effects and ownership. Preserve enclosing API behavior, evaluation/exception order, null/bounds/overflow, mutation/aliasing, identity, synchronization/publication and lifetime. Refine recursively only when the required semantic facts are available. A partial AST does not prove unobserved types/effects. Do not equate smaller files or extra helpers with success, widen visibility for convenience, introduce unmeasured boxing/indirection, or separate operations that require one verified mutation/VM boundary.

**Atom dependency graphs must be deterministic DAGs.** If the inspected behavior contains a genuine cycle, preserve the strongly connected component as one compound atom until a separately reviewed refactor proves that the cycle can be broken without changing the sealed contract. An atom is semantic, not syntactic: it may be smaller than a file or may span several tightly coupled methods/fields/native consumers.

**Patternization consolidates compatible behavior into reusable families used by real consumers.** Compare contracts before extracting shared mechanics. Prefer the existing canonical owner with explicit policies, primitive specializations and adapters. Pattern labels or a catalogue of duplicate implementations are not consumer convergence. Preserve distinct comparator/null/callback/ownership/concurrency policies; do not make a universal abstraction by erasing their differences. Retain useful workload-specific strategies and compatibility facades when justified. Semantic capability preservation does not require keeping redundant active implementation bodies forever, but any retirement needs an explicit recipe, consumer/compatibility proof and preserved lineage/history.

The two invariants apply to all relevant Synexia modules, including the consolidator, inventories, recipes, indexes and verifier themselves. They also apply to JDK adaptation; they do not transfer source proof across a new generic API, native boundary or VM layout.

### 2.3 Repasses must converge through evidence

Repeat the existing source pass cycle: pin and inventory; capture contracts; atomize; compare pattern families; reuse/compose existing owners; improve recipes and refusal tests; generate candidates; verify source behavior and costs; converge consumers; ratify serially; re-inventory affected scopes. Revisit file, package, module, reactor and external-API boundaries because lower-level convergence can expose higher-level common behavior. Review history backward for lost capabilities and forward through relevant merge parents for present retention and interactions; preserve newer fixes when recovering historical candidates. Never rebase develop or substitute ancestry preservation for content verification.

Use existing indexed facts, exact hashes, structural/logic fingerprints and similarity signals to discover candidates and avoid repeating unchanged acquisition. None proves semantic equivalence. Every accepted change invalidates its affected caller/dependency/format/runtime-consumer closure and applicable receipts. Tool, recipe, catalogue, contract or environment changes can invalidate evidence even when a source file is unchanged. Incremental scheduling must retain whole-scope coverage audits; it is not permission to skip unknown consumers.

FLLDCIM leaves remain isolated under exclusive ownership. Independent work fans out aggressively within resource budgets; results return through coordinated fan-in and serial canonical ratification. Recheck changed files before proceeding with dependent work. This is a tooling execution discipline, not a global runtime lock. The core remains deterministic and does not require an LLM.

Each pass records scope/input pins, changed atoms/consumers, recipe decisions, exact outputs, actual tests, regressions, rejected/deferred candidates, blockers and the next invalidated closure in the existing evidence owners. A scope reaches a review checkpoint when there are no new admissible changes under its pinned inputs/policy, or remaining blockers are explicit. A zero-diff recipe replay proves only that recipe's fixed point; it is not proof of global optimality, full coverage or programme completion. New source or evidence reopens the relevant passes.

### 2.4 Best means verified capability and workload fit

Correctness, compatibility, ownership and required source-quality gates precede performance selection. Compare complete costs over declared small/large, cold/warm, adversarial, mutation-heavy and concurrent workloads, including admission, precomputation, retained heap/native/mapped memory, code/dependency overhead and regressions. A newer PR, fewer lines, extra atom classes or one faster benchmark is not sufficient. Keep the best compatible capability superset and measured specializations under coherent ownership rather than force one global winner. Unknown cost remains an open measurement obligation.

Configured precomputed capabilities must complete required load/validation at the established safe startup or view-admission boundary before being exposed as ready; their first operation must not secretly rebuild promised precomputation. Preserve the bootstrap budget below, existing owners, validity/invalidation and resource limits. An explicit not-ready or reviewed fallback state is not precomputed readiness.

Illustration, not an implementation claim: one variant may improve ordered-batch search while another fixes overflow and a third preserves callback-failure semantics. The Synexia consolidation must first retain the compatible combination in its actual owner and consumers, with combined tests and workload evidence. Only that source checkpoint becomes a JDK donor. JDK adaptation must independently preserve its generic/object, view, exception and runtime contracts; a primitive source success is not that proof.

### 2.5 Preserve both lineage hops and return reusable findings upstream

Extend existing mappings and receipts to retain **original variants -> consolidated Synexia owner/checkpoint -> adapted M3jdk21 capability**. Both hops need exact pins/blobs, stable identities, contracts, dependencies, recipes, provenance, target-only changes/conflicts and their own evidence. Source inspected, consolidation candidate, source retained, source verified, target adapted and target verified are distinct questions. This documentation does not invent another schema or mark records accepted.

Within each capability, preserve finer lineage as **source atom(s) -> pattern/version -> recipe/adaptation receipt -> target atom(s) -> target symbol(s) -> exact evidence**. This is many-to-many: one source atom may split, several atoms may consolidate, and target-only bootstrap/VM adaptations may have no direct donor atom. Atom IDs and pattern IDs are subordinate lineage/evidence identities under the stable capability mapping, not a parallel registry. Pattern reuse never transfers acceptance automatically to a different owner or candidate.

Portable shared improvements discovered during JDK work return through reviewed Synexia consolidation before the shared capability is refreshed downstream. Genuinely VM/JDK-specific adapters, layout and intrinsic work remain owned by the JDK; record non-applicability rather than inject those dependencies upstream. Preserve target-only adaptations during later three-way replay. No old receipt automatically certifies a changed source owner, target adaptation or combined tree. These additions define required work; no consolidation, implementation tests, automated enforcement, source publication or merge was performed by this documentation change.

## 3. Seven layers and dependency rules

| Layer | Owns | Must not own |
|---|---|---|
| Canonical data and lifetime | Immutable payload ownership; mutable collection identity; handle namespace/generation; leases or GC reachability | An unrelated spelling copy or a global pool of arbitrary mutable objects |
| Storage and indexes | Primitive lanes, object-reference lanes, segments, ranges, slot tables, dictionaries, tuples, adjacency/index structures | Public equality, null policy or concurrency semantics chosen merely for storage convenience |
| Algorithms and facts | Search, sort, merge, probe, range traversal, summaries, exact precomputation and bounded admission | A second payload authority or assumptions whose validity/invalidation is unspecified |
| Java compatibility | Existing public types, methods, fields where exposed, views, serialization and specified behavior | Silent API changes justified by a faster backend |
| Compiler transformation | Proof-based lowering, source maps, escape/effect checks, refusal and fallback | Global rewriting from a method name or primitive type alone |
| VM and native | Traced references, barriers, layout readers, interpreter/JIT/intrinsics, JNI/FFM and platform ABI | Untraced Java pointers hidden in primitive storage or unsupported mixed JDK components |
| Diagnostics and migration | Inventories, mappings, provenance, recipes, receipts, memory accounting, rollback | Runtime truth inferred from PR state, hashes alone or an unexecuted test |

Dependencies flow toward the smallest required lower layer. A collection may reuse stateless range/probe geometry without depending on text admission, a source scanner or a service registry. A text atom may reuse primitive lanes without using public collection initialization recursively. Internal loops may stay specialized to avoid boxing and callbacks; use streams only where their semantics and measured costs fit.

### Bootstrap budget

Select a minimal dependency-closed subset for bootstrap. Audit static initialization, error construction, logging, locks, allocation, charset loading, property access and service discovery, not just Java imports. No Maven, OpenRewrite, Spring, Eclipse, database, UI, network fetch, dynamic donor selection or application repository access may enter VM bootstrap. The OpenJDK build system remains authoritative for JDK images.

Keep application adapters and transformation tools outside `java.base`. A reviewed internal JDK helper can serve multiple JDK implementations, but its package, exports and initialization order require explicit review. Do not export private Synexia packages or inject the entire framework merely to reuse one algorithm. A Java-only backend must be available where optional native acceleration is unsupported; inability to load acceleration must not corrupt semantics.

Bootstrap must not depend on successful shared-lexicon mapping. Admission after the relevant runtime facilities are available must not retroactively change object identity. Early representation exceptions stay visible and separately measured rather than being described as full canonical storage.

## 4. Minimal storage and lifetime foundations

The following are proposed obligations to be implemented through the existing owners, not names of new classes.

**Primitive lanes.** Define element kind, signedness, byte order, logical size, capacity, bounds and overflow behavior. Sharing index arithmetic is safe only when address models match. A dense position, stable slot, segmented handle and sorted key rank are different coordinates. Float/double operations must select the exact Java comparison/equality rules required by their APIs, including NaNs and signed zero.

**Object references.** Generic collections retain actual Java object references unless an independently proven lowering removes the observation. Packing structural links into `int[]` can reduce node overhead while payload stays in GC-traced `Object[]` lanes. It does not eliminate the payload objects. Integer IDs into a strong object table can add indirection and retention rather than save space. Plain native memory or a `long[]` is not a substitute for GC-traced references. A native handle design needs rooted references, release policy and collector-safe access; modified-VM layouts require matched collector support.

**IDs and handles.** Persistent IDs bind format/namespace/generation and logical coordinates; they never encode a process pointer as a portable identity. Check generation on reusable slots. Define exhausted generations and wraparound before claiming ABA safety. Null and absent must not collide with valid primitive values. Validate all external offsets and multiplied sizes before address arithmetic.

**Immutable atoms and compositions.** Reuse existing canonical immutable payload. Joins/slices retain owners and normalized range metadata. A descriptor can allocate, retain a large backing and require balancing; bounds are part of its contract. Live references must outlive optional cache entries. A cache eviction may change performance, never the text or referenced collection contents.

**Mutable collections.** The collection or snapshot owner controls mutation epochs, resize, slot reuse and publication. An unmodifiable wrapper is not an immutable snapshot. An immutable collection structure can still contain mutable objects; never cache their value-dependent facts as if deep immutability were proven. Mutation of one independently created mutable collection must not affect another through accidental shared writable lanes.

**Shared backing.** Cross-process sharing is limited to suitably authorized immutable file-backed payloads, not Java object graphs. Publish new generations instead of modifying a live mapped generation. Reader-side read-only mappings do not protect against external truncation or replacement. Account for mapping size, page faults, resident/shared pages, descriptor overhead and reclamation on each platform.

**Facts.** Each summary records a validity domain: owner/generation/version, logical range, operation parameters and semantic policy. Pure immutable facts can be reused; mutable facts need owner-maintained invalidation. Hashes, fingerprints and similarity signals are discovery/rejection aids, not proof of exact equality or interchangeable behavior. Text polynomial hash composition is not SHA-256 composition.

## 5. Three independent routes

| Question | A: explicit M3 APIs/adapters | B: compiler lowering | C: matched custom JDK |
|---|---|---|---|
| Deployment | Stock Java 21, separate application/library modules | An explicitly configured compiler/tooling path, with unchanged fallback | Complete internally consistent runtime and tools image |
| Surface | Explicit indexed/primitive APIs or compatible views | Existing source operations only inside proven admission boundaries | Existing ordinary Java APIs behind reviewed internal replacements |
| Main opportunity | Shared atoms, primitive APIs, fewer structural allocations, reusable operation facts | Eliminate safe intermediate representations and generic boundaries in a proven region | Integrate backends with all runtime consumers |
| Main boundary | Stock APIs may require ordinary String, arrays, boxing or copies | Escapes, reflection, callbacks, unknown receivers and effects can defeat substitution | VM layout, GC, compiler/intrinsic, native, serialization and serviceability coupling |
| Refusal | Use established adapter/materialization or reject unsupported explicit operations by their contract | Leave original operation unchanged; record refusal reason | Do not enable the affected subsystem/configuration before complete-image gates pass |
| Evidence | Exact API differential and lifecycle tests on stock JVM | Before/after effect traces, compilation and exact boundary tests | Exact-build runtime acceptance across the supported configuration matrix |

JNI/native acceleration is optional and cross-cutting. It does not create a fourth route or make a stock `char[]` reference describe disjoint segments. Explicit scatter/gather descriptors require aware consumers; existing contiguous ABI consumers require a documented export boundary. See the existing shared-atom contract and the [Java 21 JNI functions specification](https://docs.oracle.com/en/java/javase/21/docs/specs/jni/functions.html).

### Compiler admission contract

Preserve receiver evaluation, argument order, side effects, exceptions, identity, synchronization, escape behavior, class initialization and external method descriptors. Do not infer purity from a collection type or method name. User comparators, predicates, hash/equality implementations and mappers may throw, mutate, reenter or observe invocation order. Constructor, generic cast, array covariance and serialization boundaries also matter.

A transformation needs known effective types and operations, complete relevant effects/escapes, compatible null/bounds/overflow rules, and an exact target owner. Otherwise retain original code. Compiler rejection is an expected safe result, not an invitation to insert a lossy conversion. No new preview feature becomes mandatory just because the target is Java 21; the selected build's feature flags are part of the gate.

## 6. Reuse across non-text subsystems

Indexed ASTs may share source atoms and stable syntax facts, but must retain ordered children, duplicates, source positions, attribution, scopes and versioned dependencies. A DAG projection must retain edge direction, kind, multiplicity where meaningful, ordering where meaningful and graph ownership. Sorting or deduplicating edges merely to fit a set is a semantic change.

**Illustration, not an implemented API:** parsing `f(x(), y())` yields ordered children for the call. Interning the token spellings `f`, `x`, and `y` does not permit swapping the argument edges. Reusing a syntax node's shape does not reuse its resolved declaration across different class loaders or scopes. A precomputed dependency graph can accelerate a build only while its source, attribution and compiler-option commitments remain valid.

Numeric/value APIs require the original overflow, rounding, scale, chronology, locale and identity policies. IO requires ownership, partial transfers, cancellation and position/limit rules. Security-sensitive operations require separate review for secret retention and timing effects; dictionary/cache reuse must not be applied indiscriminately. Desktop and provider interfaces remain platform-specific until their native/resource contracts are inventoried. These families have explicit packets rather than being forced through a text backend.

## 7. Acceptance, mapping and continuation

Follow [execution and evidence](whole-jdk-execution.md). Separate source inspected, source ported, code retained on the target branch, behavior verified, performance measured and promotion accepted. Every later enhancement carries a three-way source/target comparison, reviewed recipe and refreshed receipts. Preserve additive compatible behavior; where contracts conflict, use distinct adapters or an explicit blocked decision rather than an unreviewed union.

Independent source leaves may be inventoried or transformed in parallel under exclusive leaf ownership. Verified results converge through coordinated serial ratification at file, package, module, full build and API boundaries. A leaf lock is not a new global runtime lock for collections. Reuse existing progress-monitor contracts for tooling without changing public JDK signatures.

All examples in this document set are illustrations; proposed tests have not been executed for this documentation change. No source implementation, workflow, operational schema, recipe pin, license or installed JDK is changed. Review the source-root inventory, then close file/symbol/native-consumer coverage and owner/mapping conflicts before promoting an implementation packet.
