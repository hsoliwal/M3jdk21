# Whole-JDK M3 contributor and resume guide

Status: **documentation-only durable whole-JDK handoff**. This file is the current whole-program resume guide and adds cross-program inventory, mapping and verification mechanics. [../migration/docs/RESUME.md](../migration/docs/RESUME.md) remains the retained bounded MIndex/prefix tranche history and explicitly defers whole-JDK continuation here. This file does not certify an implementation.

## Pinned observations for this handoff

- Target repository: `hsoliwal/M3jdk21`
- Inspected target baseline: `45f546ff5bcb06a1b2604f14baf998785d98d9a1`
- Source-owner repository: `hsoliwal/com.synexia`
- Inspected source-owner baseline: `3db24805d640c72ab1bd637d83561696d99561a0`
- Documentation owner branch: `docs/whole-jdk-m3-scope-20261002`
- Documentation owner PR: #25, `docs: consolidate whole-JDK M3 replacement scope including collections`

These are dated observations. Re-fetch tips before doing implementation work. Never silently substitute a newer commit into an old receipt.

## Read order

1. [whole-jdk-migration-scope.md](whole-jdk-migration-scope.md) — programme scope, routes and top-level invariants.
2. [whole-jdk-subsystem-matrix.md](whole-jdk-subsystem-matrix.md) — complete source-root module denominator and planning dispositions.
3. [whole-jdk-collections-replacement.md](whole-jdk-collections-replacement.md) — concrete collection family design/contract gates.
4. [whole-jdk-work-packets.md](whole-jdk-work-packets.md) — architecture layers, ten passes, subsystem packets, measurements and rollout.
5. [whole-jdk-worked-examples.md](whole-jdk-worked-examples.md) — semantic examples/counterexamples.
6. [mindex-migration-handoff.md](mindex-migration-handoff.md) — existing MIndex/MatIndex family handoff and verified owner distinctions.
7. [shared-atom-concatenation.md](shared-atom-concatenation.md) — String/text storage and compatibility slice.
8. [migration-mapping-lifecycle.md](migration-mapping-lifecycle.md) and [name-mapping.json](name-mapping.json) — mapping protocol and current mapping authority.
9. [migration-acceptance-matrix.md](migration-acceptance-matrix.md) — existing migration acceptance obligations.
10. [../migration/RESUME.md](../migration/RESUME.md) — retained earlier migration resume state.

The copy-ready assignment is [whole-jdk-consolidated-prompt.md](whole-jdk-consolidated-prompt.md).

## Authority rules

### Semantic migration authority

Use the existing `m3/docs/name-mapping.json` / `name-mapping.schema.json` line and the current migration tooling that consumes it. The illustrative `migration-mapping.schema.json` and example do not become a second production registry.

### Recipe/hash authority

`m3/recipes/manifest.json` is a recipe/source-hash authority. Do not reinterpret it as semantic migration state.

### Historical branch manifests

A manifest present on another PR/branch is evidence about that branch only. Reconcile it explicitly before adopting any records. File presence in a merged ancestor and current-tree retention are separate facts.

## Resume algorithm

For each work session:

1. **inventory current state**
   - fetch target default branch and source-owner default branch;
   - fetch this PR/branch if still open;
   - compare current tips to the last pinned observations;
   - list relevant open/merged PRs and mapping-owner changes.
2. **select one leaf capability**
   - resolve its stable mapping ID;
   - enumerate exact source and target symbols/files/native/resources/tests;
   - identify semantic owner and reverse consumers.
3. **read before modifying**
   - fully read high-risk target files and relevant owner docs/source;
   - inspect existing recipes/tests/evidence;
   - do not create a duplicate owner because discovery was incomplete.
4. **record contracts**
   - API/ABI/signatures;
   - equality/identity/order/null/mutation/lifetime;
   - serialization/format/native/build/bootstrap contracts;
   - concurrency/JMM properties where applicable.
5. **write/update documentation first**
   - proposed representation and refusal cases;
   - mapping and dependency impact;
   - exact acceptance/rollback plan.
6. **prepare deterministic transformation**
   - prefer an existing Maven/OpenRewrite recipe crate for Java/source changes;
   - extend or create a reusable recipe only when no owner exists;
   - native/VM/build changes use source-pinned deterministic mechanisms appropriate to those files.
7. **verify in fixed order**
   - diff;
   - lint/static validation;
   - compile/build;
   - focused tests;
   - runtime/exact-image tests;
   - deterministic replay/idempotence;
   - integrity/mapping/evidence audit.
8. **record artifacts**
   - pre/post hashes of every changed file;
   - commands/environment;
   - logs and failures;
   - candidate commit/tree/image;
   - mapping/evidence state.
9. **promote only what passed**
   - never transfer a historical receipt automatically;
   - never call a skipped gate PASS;
   - keep blockers/open questions in the mapping/work packet.
10. **resume from last verified stage**
    - do not replay hidden earlier work or infer equivalence from memory.

## Mapping update procedure for future Synexia enhancements

Given mapping capability C last synchronized at source S0 and target T0:

1. pin current source S1 and target T1;
2. diff S0..S1 and enumerate every relevant addition/deletion/rename/signature/format/test change;
3. diff T0..T1 and enumerate target-only divergence;
4. assign every source change one disposition:
   - port unchanged;
   - adapt to target;
   - already independently present, with exact proof;
   - split/merge mapping;
   - intentionally not applicable;
   - blocked/deferred;
5. preserve target-only correctness and bootstrap/VM adaptations;
6. run the exact candidate gates;
7. update mapping source sync only when every source delta has a recorded disposition;
8. bind new evidence to the new target candidate; keep old receipts pinned to old hashes.

Filename equality, structural similarity, hash/signature candidate matches and PR ancestry are discovery aids, not equivalence proof.

## Bootstrap decision checklist

Before a component can enter `java.base` or HotSpot:

- Does it depend on Maven, OpenRewrite, Spring, UI, network, service loading, logging or other post-bootstrap facilities?
- Can construction trigger allocation/class initialization cycles through maps, strings, charsets, exceptions or atomics?
- Is every required class/resource present in the minimal boot image?
- Is failure deterministic and recoverable/fallback-safe during early startup?
- Are owner generations/lifetimes established before any view is published?
- Are GC/barrier/root and native consumers mapped?
- Are CDS/archive and serviceability consumers mapped?

Any “yes/unknown” dependency cycle blocks integration until resolved. Application modules can remain source/reference owners without becoming runtime dependencies.

## Collection decision checklist

Before selecting an existing Synexia collection as a JDK backend, compare:

- arbitrary object-reference support versus primitive-ID domain;
- Java reference identity versus value/canonical identity;
- mutable versus frozen semantics;
- null policy;
- encounter/sorted/access/priority order;
- backed views;
- iterator consistency;
- serialization/subclass behavior;
- concurrency/progress;
- GC reachability;
- adaptive-layout determinism and resize behavior.

A primitive collection with a similar name is not automatically a compatible `java.util` implementation.

## String/text decision checklist

Preserve:
- exact UTF-16 code units;
- content equality/hash/order;
- Java object identity separately from atom/composition identity;
- shared lexicon and VM-local miss ownership;
- no-copy joins/slices only while owner lifetime is valid;
- explicit materialization for array/native/serialization boundaries;
- seam-aware regex/encoding;
- `intern`, monitor, identity-hash and reflection boundaries;
- HotSpot/JNI/CDS/dedup/intrinsic consumers for Route C.

## Open decisions at this documentation snapshot

These are intentionally not resolved by the docs-only PR:

- complete package/symbol/native/resource/test census under each of the 70 JDK modules and HotSpot;
- exact minimal M3 substrate eligible for early `java.base`;
- which `synexia-common`, MIndex or MAT collection engines are semantic matches versus reference/donor material;
- source/license strategy for adapting Apache-2.0 Synexia logic into OpenJDK-licensed target files;
- exact collection-by-collection Route C enablement order after ArrayList/HashMap/reference families;
- concurrency slot-reclamation strategy if stable indexed nodes are attempted;
- serialization compatibility design for changed internal collection layouts;
- complete String compiled-mode/JIT/CDS/JFR/JVMTI/dedup acceptance;
- GC/object-layout changes, which remain blocked pending complete reverse-consumer inventory;
- per-platform native/CPU coverage;
- measured break-even points for each proposed layout;
- whether any candidate is beneficial enough to enable by default.

Treat “retain current JDK owner” as a valid outcome when evidence does not justify replacement.

## Publication and privacy boundary

The target repository is public; `com.synexia` is private at this snapshot. Publish only material that is authorized and license-compatible. Architecture descriptions may reference private owner names and commit IDs where already authorized, but do not copy unrelated private source, datasets, credentials or proprietary fixtures into the public repository.

Challenge/problem catalogues and donor repositories can guide algorithm discovery. Their code is not automatically reusable production source; exact license/provenance review is mandatory.

## Completion checklist for a contributor handoff

A handoff is resume-safe when it records:

- exact pins;
- stable capability IDs;
- current owner and candidate owner;
- changed files/symbols;
- pre/post hashes;
- contracts;
- route scope/refusals;
- executed verification stages and raw logs/artifacts;
- failed/skipped/unrun gates;
- source-to-target sync state;
- unresolved conflicts;
- next leaf action.

If artifacts are absent, report planning/proposed state rather than implementation completion.
