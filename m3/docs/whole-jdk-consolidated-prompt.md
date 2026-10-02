# Consolidated whole-JDK M3 documentation assignment

Copy the assignment below as one prompt. Its deliverable is documentation and explanation, not implementation. Architecture reference: [whole-jdk-migration-scope.md](whole-jdk-migration-scope.md).

---

Prepare and maintain a coherent documentation-only architecture and migration plan for **the entire M3 JDK**, including all JDK modules, java.util collections, concurrent collections, atomics, compiler/tooling and JVM/native/runtime subsystems. String and shared text atoms are one vertical slice, not the total scope. Other contributors will implement the plan later.

## Scope and working constraints

Work in hsoliwal/M3jdk21 through the authorized GitHub connector. Inspect the current default branch, selected PRs, exact source trees and documentation before writing. Record the inspected full commit IDs and date. Do not assume merged ancestry means code is retained or tests apply. Reconcile concurrent owner proposals without blindly overwriting another contributor's work.

Produce documentation and explanations only. Do not implement code, run a Codex task, modify operational manifests/schemas/recipes, alter pinned READMEs or workflows, change test expectations, merge PRs, install/replace a JDK or publish private source. Preserve upstream and donor notices. Use additive documents where existing explanatory files are pinned. Open a draft PR and read back its exact diff/files. Perform a finite documentation review; do not turn this into broad CI monitoring.

## Whole-JDK architecture

Define a coverage denominator from all source module directories, HotSpot subsystems, platform/CPU variants, native libraries, build/image/package tools, resources, generated sources and generators, tests and external interfaces. The 2026-10-02 base 45f546ff5bcb06a1b2604f14baf998785d98d9a1 has 70 java.* / jdk.* source directories; recheck rather than treating this number as timeless. Require a complete attributed symbol/dependency/ABI census before claiming full coverage.

Give every family/surface a disposition, phase, semantic/storage owner, route eligibility, dependency closure, compatibility risks, evidence and unresolved gates. Distinguish proposed replacement, adapter, specialization, retained dependency, deferred/blocked and justified exclusion. “All parts” requires review of every part, not an unsupported promise that every part can share one representation.

Use canonical M3 backends with public compatibility wrappers where valid. Keep immutable atoms/compositions, mutable stores, Java object identity, value equality, comparator equivalence, arena IDs and persistent image generations distinct. Do not put everything into java.lang or a process-global pool. Preserve module dependencies and bootstrap-safe initialization; keep application/tooling dependencies outside early java.base initialization.

Explain three routes across all eligible families:
1. Explicit opt-in M3 APIs on a stock JVM, with measured boxing/materialization/adaptation boundaries
2. Attributed compiler lowering to the same backend, only with proven type/dispatch/alias/escape/evaluation/exception/identity/ABI semantics and explicit refusal cases
3. Standard APIs backed by M3 internals in a matched complete custom JDK, with affected interpreter/JIT/intrinsic/GC/JNI/JVMTI/CDS/JFR/serviceability and platform gates

Evidence for one route does not certify another.

## Architecture layers and retained String specification

Separate canonical data/lifetime ownership, storage/indexing, algorithms/precomputation, public Java compatibility, compiler transformations, VM/native integration, and diagnostics/evidence. Select shared components by compatible semantics rather than names. Tooling, Maven/OpenRewrite and application frameworks must not enter early `java.base` bootstrap.

Carry forward the complete String contract from the existing text documents: shared immutable lexicon plus VM-local misses, exact UTF-16 semantics, reference-only joins/slices where legal, bounded descriptors, seam-aware encoding and regex, explicit materialization boundaries, and separate text/atom/composition/Java-object identity. Preserve current exceptions, failed gates and unimplemented paths rather than converting them into completion claims.

## Collections are a first-class replacement workstream

Cover List, Map, Set, Queue, Deque, sorted/navigable and Java 21 sequenced families; concrete and abstract implementations; immutable/fixed-size/unmodifiable/synchronized/checked wrappers; legacy collections; views, iterators, spliterators, Arrays/Collections utilities, comparators, streams and collectors. Include concurrent maps/sets/queues, blocking/transfer/delay/priority and copy-on-write variants, atomics/VarHandles and connected synchronization owners.

Explain candidate primitive-array/compact/chunked/persistent backends without claiming automatic superiority. Inventory primitive-to-generic adapters and boxing costs, including null, wrapper identity where observable, NaN and signed-zero rules.

Specify operation-level contracts for mutability, independent object identity, null policy, duplicates, equals/hash, comparators, order, live views versus snapshots, subList/reversed/descending views, map entry mutation, optional operations, fail-fast best effort versus weak consistency/snapshot iteration, spliterator characteristics and stream behavior. Cover callback side effects/reentrancy, compute/merge/bulk operations, exception timing, capacity/overflow/OOM, subclass hooks, reflection, generic/binary linkage and serialization compatibility.

Never value-intern mutable collection objects or replace IdentityHashMap reference equality with value equality. Sharing immutable atoms is not a mutable cross-process collection protocol.

For concurrency, require JMM happens-before and safe-publication reasoning, per-operation atomicity/linearizability where promised, progress/fairness where specified, contention/resize/cancellation/timeout tests and reclamation/ABA/generation rules. Keep live GC references, owner lifetimes, weak-reference behavior and native/off-heap memory safety explicit. Do not infer an atomic snapshot for bulk operations whose contracts do not promise one.

## Measurement and dependency gates

Define “bloat” as measurable categories: headers/alignment, references, boxing, node/link objects, backing arrays/slack, resize peaks, wrapper/view/iterator metadata, synchronization/cache costs, retained heap, native/mapped memory, GC and code/class footprint. Compare the stock baseline against candidates across sizes, shapes, occupancy, collision patterns, iteration/mutation mixes and contention.

Require pinned environment/image/flags, warmup/forks, latency/throughput/CPU, allocation/retained/peak memory, cold versus warm cost, conversion/precomputation/cleanup and break-even evidence. Report regressions. Lower allocation does not prove faster execution; no blanket speed or percentage claim.

Stage the program through inventory/contracts, independent owners, java.base/bootstrap closure, collection/compiler vertical slices, matched-runtime integration, remaining modules/distribution and ongoing source-port review. Show dependency cycles and blocking prerequisites. Keep earlier text-stage numbering and historical receipts intact.

## One mapping authority and future-port procedure

Inspect actual authorities first. At the cited base, m3/docs/name-mapping.json contains legacy mapping directions plus 25 migration.records; m3/migration/migration.py consumes m3/docs/name-mapping.schema.json. m3/recipes/manifest.json is a recipe/hash authority. The illustrative migration-mapping schema/example is not an operational replacement. A manifest on draft #12's feature stack is not evidence that m3/migration/manifest.json exists on master.

Reuse stable IDs and existing source/target, contract, identity, format/ABI/bootstrap, dependencies, materialization, recipe, tests, provenance, sync and lineage fields. Describe any schema gaps as proposals, with validator/backward-compatibility review required later. Do not add a competing registry or mutate current statuses.

Map all relevant symbols, overloads, formats, serialized names, native entry points, VM layouts/intrinsics, module/service descriptors, resources/generators and test obligations. Track many-to-many adapters/splits/consolidations and reverse consumers. Future enhancements compare pinned source/target baselines, account for every change, preserve target adaptations, use deterministic recipes with drift/idempotence/partial-state/rollback gates and advance synchronization only after explicit dispositions.

## Required documentation topology

Use the existing target documents as one connected specification rather than creating parallel authorities:

- m3/docs/whole-jdk-migration-scope.md — whole-program overview and denominator
- m3/docs/whole-jdk-subsystem-matrix.md — canonical module/source-root planning dispositions
- m3/migration/docs/ARCHITECTURE.md — shared architecture and compatibility laws
- m3/docs/whole-jdk-collections-replacement.md — canonical detailed collection replacement specification
- m3/docs/whole-jdk-work-packets.md — ten-pass dependency-aware implementation plan and work-packet/evidence schema
- m3/docs/whole-jdk-worked-examples.md — concrete semantic examples and counterexamples
- m3/migration/docs/COVERAGE.md — human-readable migration/evidence projection
- m3/migration/docs/ACCEPTANCE.md — promotion gates and retained historical evidence
- m3/docs/whole-jdk-resume.md — durable whole-JDK continuation guide
- m3/docs/name-mapping.json — operational capability/mapping authority
- m3/docs/mindex-migration-handoff.md and the existing String documents — deep retained slice specifications

Do not duplicate operational state in Markdown tables. Planning documents describe scope, disposition candidates, contracts and required evidence; mapping status changes belong to a separately reviewed operational update.

## Required dependency-aware passes

Organize execution and handoff through these ten passes:

1. pin target/source trees and inventory the whole JDK plus relevant Synexia owners;
2. build subsystem, dependency and bootstrap maps;
3. reconcile the canonical mapping authority and assign a disposition to every selected capability;
4. specify minimal primitive storage, identity, ownership and lifetime foundations;
5. specify String and collections deeply, including semantic counterexamples;
6. extend the design across the remaining JDK without forcing one backend everywhere;
7. define independent Route A explicit-API, Route B compiler-lowering and Route C custom-JDK packets;
8. define reusable transformations, differential/concurrency/runtime tests and rollback;
9. measure allocation, retained/peak memory, CPU, throughput, latency, contention and precomputation;
10. audit the full denominator for omissions, duplicate owners, stale mappings, contradictions and unsupported completion claims.

Include concrete worked examples for shared text joins/slices, compact generic maps and boxing boundaries, immutable sharing versus mutable identity, backed views, identity-sensitive keys and collisions, concurrent linearization/visibility, AST/DAG semantic identity, source rename/split/conflict ports and failed gates that block promotion.

## Provenance, licensing and rollout

Pin every donor/source revision and record whether it is reference-only, adapted or copied. Preserve OpenJDK and donor license/notice obligations and private-source publication boundaries. Challenge/problem catalogues are discovery evidence, not automatic production-source permission.

Keep implemented, retained, tested, proposed, blocked and deferred states distinct. Route C replacements require explicit enablement/default-off and rollback plans until exact compatibility, runtime and performance gates justify promotion. Historical receipts remain bound to their exact candidates and never transfer automatically.

## Deliverable and verification

Provide a coherent scope/architecture document, module/subsystem coverage taxonomy, detailed collection compatibility and performance plan, dependency-led acceptance stages and mapping/porting guidance, plus this reusable consolidated prompt. Link related documents rather than duplicating operational truth.

Use inspected repository sources and official Java/JVM specifications for factual claims. Separate observed facts, proposed design, executed evidence and unverified work. Preserve failed tests and historical limitations. Read back the draft PR files/diff, verify documentation-only scope and links, and report the PR and document URLs with a concise summary and exact verification limits. Do not describe a plan or directory census as completed JDK migration.
