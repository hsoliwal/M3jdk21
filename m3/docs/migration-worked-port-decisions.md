# Worked enhancement-port decisions and family contracts

Status: documentation-only case studies. Fictitious revisions S0/S1/T0/T1 and hypothetical enhancements below are **not repository commits or executed ports**.

## 1. Where the real mapping currently lives

Inspection snapshot: 2026-10-02 02:52 UTC.

- Current master was 26c442d3f1400e01eeb11a44e803b63e1874735e. Its m3 tree does not contain m3/migration.
- [#12's manifest at d543255294ae85e4c8015812a3c8a97aaf498354](https://github.com/hsoliwal/M3jdk21/blob/d543255294ae85e4c8015812a3c8a97aaf498354/m3/migration/manifest.json) contains 15 operational candidate mappings. #12 merged into m3/mindex-integrated-runtime, so its merged status does not establish those files on master.
- [#13 at 84c2711c7ff24d11ba46478a11584194c425553b](https://github.com/hsoliwal/M3jdk21/pull/13) has a nine-file exact-source recipe/tooling closure in its changed-file list. Its broader title or companion description does not establish that a retained-view implementation or maintained migration manifest is present in that exact tree.
- The JSON Schema/example under m3/docs are pedagogical. They are not a second operational mapping registry and must not be populated as a competing authority.

Before implementation, select and reconcile the operational manifest, validator and generated coverage into the intended target branch through its own reviewed change. Keep stable mapping IDs. Do not copy the illustrative schema over the operational schema merely because the names look similar.

The examples below use real IDs from the inspected #12 manifest:
M3-TEXT-EXPLICIT-001, M3-TEXT-SLICE-001, M3-TEXT-TUPLE-001, M3-COMPILER-001, M3-RUNTIME-STRING-001, M3-STRUCT-ATOM-001, M3-STRUCT-AST-001, M3-INTERACTION-001, M3-STRUCT-DAG-001, M3-MATINDEX-001, M3-RESOLVER-001, M3-DICTIONARY-001, M3-JOINED-001, M3-PRECOMPUTE-001 and M3-NATIVE-001.

Those IDs are references to candidate records, not claims of accepted implementation. The inspected records include proposed, blocked, partial and implemented-unverified states. Object/Path/Algorithm/DataStructure/Tool/Library/Framework mapping IDs have not been established by that 15-row manifest; discover and review them rather than inventing completed rows.

## 2. Translate review questions into the existing record fields

| Review question | Existing #12 field(s) to update in an authorized implementation PR |
| --- | --- |
| Which exact source changed? | sources, lastSynchronizedSourceRevision |
| Which target adaptations already exist? | destinations, lastSynchronizedTargetRevision, targetAdaptations |
| Is this a move, split, facade or consolidation? | mappingKind, canonicalOwner, rationale |
| What behavior must survive? | identityRules, contractDifferences, compatibilityObligations |
| Who depends on it? | dependencies, downstreamConsumers, bootstrapConstraints |
| Does it change storage or copied output? | formats, materializationBoundaries, cachedFactOwnership |
| Can the change replay safely? | recipe |
| What cannot yet be reconciled? | conflicts, status |
| What actually ran? | tests, with candidate and evidence |
| Is code movement allowed? | licenseProvenance, portDirection |

The documentation example's source/target objects correspond conceptually to the operational sources/destinations arrays, but the operational model supports multiple entries and per-symbol identity. The proposed lastReviewedSourceCommit is not a substitute for the operational pair lastSynchronizedSourceRevision and lastSynchronizedTargetRevision. A reviewed source may still be deferred, and source synchronization says nothing about a new target-only change. Preserve null target commit fields while unresolved; never substitute the documentation PR's commit as an implementation target.

Do not mechanically convert the different illustrative lifecycle states to operational status strings. Follow the selected manifest's actual validator and reviewed state meanings. Validation, compilation, behavioral equivalence and acceptance remain separate gates.

## 3. Case A: a source rename with unchanged behavior

Hypothesis: source S1 moves a joined-range class to a new package while target T1 already has a facade over the old owner.

Review steps:
1. Compare source S0→S1, including signatures, bodies, service descriptors, reflection names, serialization and generated/native references. A Git rename score is a hint, not proof of semantic identity.
2. Keep M3-JOINED-001's stable ID if the capability is unchanged; update its source path/symbol and exact content identity.
3. Preserve target-only facade and package adaptation recorded at T0→T1. Do not move the public target class just to follow upstream spelling.
4. Add old-name compatibility obligations where callers, images or ABI references depend on them.
5. Regenerate derived coverage from the authoritative manifest, review the diff and retain the drift gate.
6. Advance the synchronized source pin only after every affected dependency/consumer is dispositioned.

Required evidence: source/binary compatibility as promised, reflective/service resolution, exact old/new behavior and recipe drift refusal. A file rename alone is not a completed port.

## 4. Case B: one source owner splits into storage and policy

Hypothesis: source S1 separates a combined local pool into immutable payload storage and configurable admission/eviction policy.

Do not replace one mapping ID with two unrelated completed IDs. Preserve lineage:
- Keep the existing capability record, marking its relationship and unresolved split
- Enumerate the two new source symbols and their dependency edge
- Decide whether target storage remains one owner with a policy adapter, or also splits
- Link newly reviewed capability records without reusing IDs for unrelated meaning
- Preserve live-value lifetime while allowing optional lookup/cache eviction
- Bind cached facts and budgets to the right authority

Apply this to M3-RESOLVER-001 / M3-TEXT-TUPLE-001 / M3-JOINED-001 only where the actual source diff touches those contracts. A policy change must not silently reassign content identity or make local slots portable IDs.

Required evidence: hit/miss behavior, exact collision checks, eviction with live ranges, concurrent admission, accounting and rollback. If the proposed split changes public close semantics, block adoption until that conflict is resolved.

## 5. Case C: upstream fix conflicts with target-only adaptation

Hypothesis:
- Source S0 returns a generic range exception
- Source S1 fixes owning substring to match StringIndexOutOfBoundsException and primitive range retention
- Target T1 already added an adapter that intentionally exposes a stricter surrogate-safe explicit view

This is a semantic conflict, not a text merge problem. Read both S0→S1 and T0→T1:
- M3-TEXT-SLICE-001 must distinguish explicit view from owning substring
- M3-COMPILER-001 must use the String-compatible owning operation where lowering promises that behavior
- The stricter view remains separately named/contracted if retained
- M3-RUNTIME-STRING-001 cannot inherit a source-wrapper-retaining view accidentally

Keep the target adaptation if it serves its declared API, while porting the upstream fix to the appropriate owning operation. If one destination method currently serves both, split the API or introduce a reviewed adapter rather than weakening one contract.

Required evidence: full/empty range, all UTF-16 endpoints including surrogate halves, exact exception class/timing, nested/direct range behavior, wrapper retention, compiler evaluation order and public identity promises. The fact that both paths render the same ordinary text is insufficient.

## 6. Case D: storage-format evolution and a derived sidecar

Hypothesis: an enhancement adds a new language/width lane to a derived checkpoint while canonical arena records stay unchanged.

Separate:
- canonical format authority
- sidecar version and validated prefix/generation
- output geometry coordinate system
- native/GPU ABI consumer expectations
- stale/corrupt sidecar recovery

Map affected work to M3-DICTIONARY-001, M3-RESOLVER-001, M3-PRECOMPUTE-001 and M3-NATIVE-001 as applicable. Do not bump canonical identity merely because the derived format changes. Conversely, do not claim compatibility if old readers reinterpret a new layout.

The documented #7554 language-output shadowing defect is a real caution: metadata-only/no-payload-copy work can still emit wrong geometry. A port must use the reviewed corrected source and targeted paging/language tests, not a description of intended behavior.

Required evidence: old/new readers, corrupt/stale sidecars, tail replay, nonzero start rows, skipped descriptor records, small output buffers, independent language values, 64-bit offset arithmetic and unchanged canonical content.

## 7. Case E: performance enhancement without semantic shortcuts

Hypothesis: source S1 adds a prepared regex prefilter and transition cache.

The port review asks:
- What exact pattern subset and engine semantics does it support?
- Are keys bound to flags, region, boundary state and owner generation?
- Can rejection lose a valid match? Candidate filtering must be sound
- Do captures and replacements still use the declared coordinate system?
- What is the fallback for unsupported constructs?
- Does target T1 have a different memory budget or native backend?

M3-PRECOMPUTE-001 records the validity/ownership of derived facts; M3-JOINED-001 and M3-NATIVE-001 record seam/ABI implications. A faster rejection benchmark is not evidence of complete regex equivalence.

Required evidence: exact oracle parity, seam/region/Unicode cases, cache disabled/enabled/evicted behavior, preparation and retained-memory costs, representative end-to-end CPU and refusal for unsupported features.

## 8. Family-specific semantic decision cards

The following cards are review templates for actual source contracts, not assertions that all families use the same representation.

### Atom store, AST and DAG

- Atom store: is equality within one store, structural content equality, or both through different operations? How are absent payload and an empty payload distinguished?
- AST: are ordered children significant? Can subtrees share atoms? Is donor object identity discarded after admission? What source-range/spec metadata survives?
- DAG: are parallel edges allowed? Which ordering is observable? Are edge mode/kind/type and payload part of identity? How are cycles or missing nodes rejected?

Example: swapping AST children a and b may change meaning even when the set of children is unchanged. Two edges with equal endpoints but different modes must not be collapsed merely by endpoint equality.

Acceptance: exact ordered structure, domain/spec validation, collision-safe identity, malformed-image rejection, deterministic traversal where promised, and lifetime/version fences.

### Interaction

A classifier result is not an interaction-store identity. Preserve the role × role × mode table and persisted value policy separately from graph edge storage.

Example: role pair AST/DAG and role pair FACT/CONTEXT cannot be generalized from a common “connection” spelling without evaluating the classifier contract.

Acceptance: exhaustive finite-table parity, unknown persisted enum handling and independent store-version tests.

### Object

Determine whether a handle denotes immutable admitted value, a versioned state, a live external object or a program/shape descriptor. Never treat an object pool ID as text identity.

Example: two handles with the same numeric row in different pools are not interchangeable; a state update may produce a new version rather than mutate all retained snapshots.

Acceptance: cross-owner rejection/conversion, stale-handle rules, versioned state, adapter lifetime and deterministic program behavior.

### Path

Specify root/relative semantics, segment kinds, escaping, normalization and whether a path is filesystem, graph, query or structural navigation.

Example: graph path segments and a filesystem path containing slash-like text are not equivalent merely because their printed forms match. Normalizing ".." without the right domain can change meaning.

Acceptance: typed segment round-trip, boundary cases, namespace/version binding, traversal order and explicit formatting/materialization.

### Algorithm and DataStructure

An algorithm descriptor/catalog entry is not execution; a data-structure schema is not a mutable instance. Identify preconditions, topology, version, order and error/budget contracts.

Example: sorting an ordered view may return a permutation/projection rather than mutate the canonical owner. A graph algorithm's cached result depends on the graph version and options.

Acceptance: deterministic oracle outputs, invariant checks, cycles where permitted, mutation/alias behavior, version invalidation and provider registration.

### Tool, Library and Framework

Separate metadata/catalog lookup from execution and dependency resolution. Define request/result schemas, provider identity, versions, capabilities and cancellation.

Example: loading a tool descriptor must not accidentally execute a tool. A framework snapshot must not silently pick a different library version because the display name is the same.

Acceptance: registration and compatibility, invalid requests, deterministic dependency resolution, failure/cancellation, snapshot replay and no unintended bootstrap side effects.

### Matrix and specialized text projections

Choose the actual qualified MatIndex owner; define rows/columns/lanes, dimensions, order and equality. A mask/bitwise/range projection expresses a semantic operation over an owner, not automatically a new canonical payload authority.

Example: a matrix transpose can retain storage with an index map, but consumers that require row-major contiguous output need a named materialization. A noncontiguous mask needs capture/source coordinate conversion.

Acceptance: empty/ragged dimensions as specified, overflow, ordering, alias/lifetime, exact projected content, serialization and explicit copy boundaries.

## 9. A reviewable enhancement receipt

Before moving a record's synchronized pins, the review should be able to state:

“Capability [existing mapping ID] compared source S0→S1 and target T0→T1. These source changes were adopted; these were deferred with reasons. These target adaptations remain. Recipe R refused these drift cases and reproduced the reviewed output. Evidence E ran on target T2 in modes M. These family/integration gates remain open.”

S0/S1/T0/T1/T2/R/E/M are placeholders until replaced with real identities and receipts. Never invent a target commit or fill passed because another branch once passed. Source-to-target traceability does not authorize automatic reverse-porting.

## 10. Stop conditions for this documentation batch

This batch supplies worked examples and review decisions. It does not:
- adopt candidate runtime or mapping code into master
- create operational mapping records for unreviewed families
- run compiler, Maven, OpenRewrite, JNI or VM tests
- alter hashes, generated coverage, workflows or acceptance gates
- change licenses or export private source payloads

Implementation contributors can now bind each scenario to the selected operational records and fill exact-head evidence without treating these examples as completed work.
