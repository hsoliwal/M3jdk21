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
