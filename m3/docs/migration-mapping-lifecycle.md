# Durable migration mapping and enhancement porting

Status: proposed review protocol. The example files beside this document are explanatory artifacts, not an implemented migration database, runtime feature or validated port.


## Current entry points for JDK, specification and Synexia migration — 2026-11-12

<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->

This section connects the existing owners inspected at
M3JDK21 `b98bf8c065818aac3447be165909e2a013325d9a` and Synexia
`510a640feb877b1b7e69d346ae2e4e1a10628506`. It adds no registry, changes no
capability disposition and does not turn the dated observations below into
current acceptance evidence. Resolve a capability in
[`name-mapping.json`](name-mapping.json) `migration.records` before moving,
renaming, formatting or substituting its implementation.

### Connect the source to its existing contract and owner

| Input | Existing authority | Migration use |
| --- | --- | --- |
| OpenJDK, vendor and other JVM source | Synexia's existing [federated donor intake](https://github.com/hsoliwal/com.synexia/blob/510a640feb877b1b7e69d346ae2e4e1a10628506/tools/m3-donor-recipes/JDK_RUNTIME_DONOR_INTAKE.md), `DonorCatalog` and category catalogue | Select exact repository/revision/path, source licence and an applicable donor mode; retain independent VM families and vendor deltas as distinct source relationships. |
| Java specification | [JSR 396: Java SE 21](https://jcp.org/en/jsr/detail?id=396), affected Java 21 specifications and the existing [JSR/JEP/JBS crosswalk](../backports/JSR_JEP_JBS_22_27_RECEIPT_20261022.md) | Bind the required source, binary, API, VM and runtime contract. A specification identifier is not a source patch or test result. |
| JEP feature lineage | [Release authority](../backports/RELEASE_JEP_AUTHORITY.tsv), [JEP catalogue](../backports/JEP_CATALOGUE.tsv), [residue queue](../backports/JEP_RESIDUE_QUEUE.tsv) and [source witness](../backports/JEP_SOURCE_WITNESS.md) | Preserve feature/successor relationships, candidate decisions and exact source-presence evidence. Preview or successor metadata does not grant default Java 21 compatibility. |
| JBS fixes and follow-up changes | Existing `m3/backports/inventory.py`, `compatibility_queue.py`, `UPSTREAM_CHANGE_SEEDS.tsv` and per-change packets | Join commits and affected paths to candidate issue IDs and proof lanes. An ID parsed from a commit subject remains a candidate until its issue relationship is checked. |
| Synexia implementation families | [Full-family catalogue](m3-full-family-catalogue.json), [current full-borrow pin](../m3-import/current-full-borrow/pin.tsv), estate/DAG/phase receipts and stable capability mappings | Preserve source ownership, exact bytes, dependencies and the String → arrays → collections → AST/compiler → remaining-families receiving order. |
| Target form and build | [Name/package contract](M3JDK21_NAME_AND_PACKAGE_CONTRACT.md), [naming workflow](m3jdk21-naming-admission-workflow.md), [Maven control reactor](../pom.xml) and [release contract](../release/release-contract.json) | Place executed output under its actual JDK/module owner; prove target source, module/image packaging and the relevant runtime behavior. |

A JEP may involve several JBS issues and commits; one commit may touch several
capabilities. Keep these relationships in the existing records and their linked
receipts. A list of JDK names, JSRs, JEPs, JBS identifiers or Maven modules cannot
serve as a count of completed ports. The [whole-JDK subsystem matrix](jdk-subsystem-migration-matrix.md)
describes inventory obligations; it does not replace the operational mapping.

### Use the existing verification entry points

From the **M3JDK21 repository root**, check names and source-pin custody:

```sh
python3 m3/tooling/name-contract/check.py
python3 m3/compatibility/check_m3_full_borrow_receiver.py
```

The full-borrow checker retains the original source identities:
`M3JDK21_SYNEXIA_FULL_BORROW_PIN_V1`,
`SYNEXIA_M3JDK21_FULL_BORROW_V1`,
`SYNEXIA_M3JDK21_FULL_BORROW_DAG_V1`,
`SYNEXIA_M3JDK21_PROMOTION_PHASE_V1`, and
`docs/M3-SCALE/invariants/SYNEXIA-M3JDK21-CANONICAL-OWNERSHIP-1.md`.
Product naming rules do not rename a canonical donor schema or provenance
identifier. Its checks still require the pinned bytes, family/DAG/phase
relationships and actual Git baseline ancestry; a successful custody check
does not promote a family or advance a phase.

The existing backport owner produces the JEP witness, proof matrix, execution
waves and scoped programme status:

```sh
mvn -B -ntp -f m3/backports/pom.xml verify
python3 m3/backports/jep_residue.py --root . --include-m3-extensions
```

Use the existing [A3 inventory/plan owner](../tooling/a3/README.md) when source
discovery is needed. These reports retain pending decisions; they do not execute
or accept every JEP. The extension projection preserves Java 21 obligations and
requires separate M3 extension evidence. Historical recipe directories and
authoring profiles in the public tree are tracked migration residue; they do
not authorize running or adding Synexia recipe machinery here.

For that residue, use the existing [recipe custody ledger](../release/LEGACY_RECIPE_CUSTODY_LEDGER.tsv)
and the [current private custody observation](https://github.com/hsoliwal/com.synexia/blob/4f5750e2ca3127211fdf3354dd6978a1a67672f5/synexia-openrewrite-recipes/recipes/m3jdk21-legacy-recipe-custody-20261030/observations/20261112/README.md).
Its recorded decisions are an intake frontier, not evidence that the corresponding
files, callers, tests or runtime capabilities have already moved.

Target-local regex verification uses the actual source-built M3JDK21 image:

```sh
mvn -B -ntp -f m3/runtime-integration/regex-precompute/pom.xml \
  -Dm3.test.jdk=/absolute/path/to/source-built-M3JDK21 verify
```

This module's verifier uses target-owned source and the target image, with no
private checkout or fixture execution dependency. The supplied image also needs
a matching source-build receipt; binary hashes alone do not establish its build
provenance. A stock Java 21 installation
cannot establish M3String runtime acceptance. Preserve the individual mode
results and any refusal; the [regex receiver](../runtime-integration/regex-precompute/README.md)
still needs its independent native, jtreg, lifetime and performance evidence.

The complete target Maven and standalone entry points remain:

```sh
mvn -B -ntp -f m3/pom.xml verify
python3 m3/release/check_standalone.py --root . --release
```

Run these against the candidate's actual checkout and record the command,
toolchain, commit/tree, exit status and artifact hashes. A selected-module pass
does not satisfy `M3_MAVEN_CONTROL_REACTOR`; a Maven reactor pass does not
replace the source-built JDK, installed-image, jtreg, JNI/ABI, GC or performance
gates declared by the release contract. Retain any reported blocker instead of
weakening a baseline or calling an unexecuted gate passed.

### Carry implementation, documentation and evidence together

For each selected behavioral atom, improve and execute its source-pinned
Maven/OpenRewrite recipe in private Synexia. The public target receives the
executed Java/native/build/test/documentation output and passive provenance
receipts. Recipe sources, templates, private mastery fixtures and recipe
improvement memory remain with their canonical Synexia owner.

Follow the [porting invariant](m3jdk21-porting-invariant.md). Public JDK names
and observable contracts remain with their original owners; internal helpers
use the admitted naming map. Describe the operation's behavior, exceptions,
Unicode/range rules, ownership and materialization boundary beside its source.
Update affected package/module documentation, build and installed-image
requirements, attribution and migration links in the same recipe output.
Bind documentation to the source/target revision and capability ID so a later
source change can expose stale explanations.

Record the narrow result actually established: inventoried source, reviewed
contract, executed recipe output, target source verification, installed-runtime
verification, or accepted capability. Link all remaining obligations from the
existing capability and release records. Neither a copied source tree, a new
Maven module, a green fixture nor a merge is a substitute for this chain.

## Operational authority before using these examples

The illustrative schema/example in this directory must not become a competing migration registry. At the 2026-10-02 02:52 inspection, the verified operational candidate is [#12's m3/migration/manifest.json at d543255294ae85e4c8015812a3c8a97aaf498354](https://github.com/hsoliwal/M3jdk21/blob/d543255294ae85e4c8015812a3c8a97aaf498354/m3/migration/manifest.json), on its feature-stack line. That directory is not present in master 26c442d3f1400e01eeb11a44e803b63e1874735e. Reconcile the selected manifest/validator into the intended branch through a reviewed change. [Worked port decisions](migration-worked-port-decisions.md) maps these concepts to its existing fields and stable IDs, with rename/split/conflict examples. No operational mapping is changed by this documentation. This paragraph is the historical 02:52 snapshot; the retained records and newer source-presence facts immediately below supersede any inference that master still lacks all migration tooling.

## Retained mapping records and remaining reconciliation

The top-level mappings array in [name-mapping.json](name-mapping.json) preserves early provisional directions, but the file is no longer only that historical map. At master 45f546ff5bcb06a1b2604f14baf998785d98d9a1 (same map blob as #19 merge 43cf5ed4b97bc9a2832f10a332eb6500ca73ca04), its migration.records contains actual records including m3.prefix-z and synexia.frozen-chars. Preserve their stable IDs, exact pins, target adaptations, sync fields and qualified evidence. The documentation schema/example remains illustrative and must not replace those records.

Retained source and mapping completeness are separate: synexia.frozen-chars is still pending with targets=[] in that pinned map, while #19 retains FrozenChars under m3/ports/indexstring. This is a mapping-reconciliation gap, not evidence that the file is absent or that its acceptance is complete. The #12 m3/migration/manifest.json candidate remains separate; that exact path returns 404 on the current master pin despite other m3/migration tooling now being present. See [the dated retained-content reconciliation](migration-worked-port-decisions.md#retained-content-and-mapping-reconciliation-2026-10-02-0345-utc) for concrete paths and proof boundaries. This documentation does not populate, promote or merge operational records.

A type name alone cannot express source ownership, multiple targets, merged/split classes, per-operation compatibility or which later enhancement has been ported.

Operational mapping reconciliation must support many-to-many relations:
- One source owner can have a storage target, public facade and native adapter
- Several source types can consolidate only after their contracts are reconciled
- An unchanged source type may remain a dependency instead of moving
- One method may be deferred even when its enclosing class has a target
- A source bug fix is different from a feature port or a behavior-preserving rename

Use stable mapping IDs independent of filenames. Never reuse a retired ID for a different contract.

## Atom and pattern lineage within the capability map

The stable capability mapping remains the outer identity. Atomization and patternization add **sub-capability lineage**, not a second registry.

For each sealed behavior/contract boundary, retain or reference:

```text
capability mapping ID
  -> source atom DAG
  -> pattern/version applications
  -> deterministic recipe/adaptation receipts
  -> target atom DAG
  -> target symbols
  -> exact evidence
```

The source and target atom DAGs are semantic decompositions, not file trees. If a dependency cycle cannot yet be separated safely, represent the strongly connected component as one compound atom. This keeps the published dependency graph deterministic and prevents an incomplete decomposition from masquerading as full atom coverage.

Every required source atom must have one explicit disposition: mapped one-to-one, split across target atoms, consolidated with other compatible atoms, retained dependency, deferred/blocked, or not applicable with a reason. Target-only bootstrap, VM, GC, native or compatibility atoms remain visible even when they have no direct donor atom.

A pattern record/version is reusable only when its semantic preconditions and required context match. It identifies refusal cases, transformation/adaptation semantics, recipe/patch identity, postconditions and evidence template. Reusing a pattern never transfers acceptance from a previous owner/candidate automatically.

Until the operational mapping schema/tooling explicitly supports atom/pattern fields, keep these identities in reviewed receipts referenced from the existing capability record. Do not create a parallel atom registry or silently add status semantics.

## Record shape

[migration-mapping.schema.json](migration-mapping.schema.json) is a proposed JSON Schema, not a deployed validator. [migration-mapping.example.json](migration-mapping.example.json) contains one explicitly illustrative record with null target commit and no evidence of implementation.

Each real record needs:
1. Stable ID, family, disposition and evidence state
2. Source repository, full commit, paths, blob identities and qualified symbols
3. Source semantic owner, identity domain and coordinate system
4. Proposed/actual target module, paths and symbols, with target commit only when real
5. Relationship: facade, adapter, relocation, split, consolidation, retained dependency or deferred
6. API/ABI behavior, equality/hash/range/lifetime/serialization contracts
7. Materialization and conversion boundaries
8. Dependency mapping IDs, generated/native/resource surfaces and provenance review
9. Recipe identity/version, exact input/output hashes and replay instructions
10. Tests and evidence tied to source/target commits, mode, environment and artifacts
11. Last reviewed upstream commit, enhancement dispositions, blockers and reviewer decision

Keep “present in source,” “proposed target,” “implemented,” “verified” and “merged” separate. PR merge state and test state are independent dimensions.

The schema intentionally permits null target and source blob fields while a proposal is unresolved. A production validator must reject promotion to verified when required pins, evidence or review are missing. Schema syntax alone cannot verify files exist, hashes match, licensing is satisfied or behavior is equivalent.

## State and review lifecycle

Recommended states:

    discovered -> contract-reviewed -> proposed -> implemented -> verified -> accepted
                         |               |             |
                      blocked         deferred       drifted

These are process states, not an assertion that every branch is already implemented. Record the reason for every blocked/deferred item. accepted requires the owning review policy and all required gates; a contributor must not infer acceptance from absence of a reported failure.

For each transition, retain the previous source/target pins and evidence. On new upstream changes:
- Content/API changes move affected accepted/verified records to needs-review or drifted
- Pure file moves still update provenance/path links while preserving mapping IDs
- Deletions and renamed methods require explicit disposition, not silent record removal
- Reverted changes must not leave a target marked as carrying the reverted feature
- A generated output change must identify the generator input and recipe, not only the generated file

A source commit can affect multiple mappings. A target change can depend on several source commits. Maintain both forward and reverse references so a future contributor can answer “where did this enhancement go?” and “what upstream behavior does this target depend on?”

## Enhancement porting procedure

1. Select a source comparison range from each mapping's last reviewed commit to the chosen new source pin. Do not diff against a moving develop tip during replay.
2. Enumerate changed paths, renamed files, API signatures, image formats, native entry points, generated resources, tests and dependency versions.
3. Resolve every changed source item to mapping IDs and affected semantic atoms. Unmapped affected items/atoms become inventory work; never silently ignore them.
4. Re-evaluate patternization for changed atoms: reuse a reviewed pattern/version only when its semantic preconditions still hold; otherwise version/refuse it or record a one-off. Then classify each change: bug fix, feature, performance-only change, contract change, format evolution, dependency/security update, test/oracle update or irrelevant to this target.
5. Record target applicability and route A/B/C impact. “Not applicable” needs a reason and review, not an empty target.
6. Check source prerequisites and known defects. A new upstream test is evidence to port and execute, not proof the target passes.
7. Prepare or improve the source-pinned reusable recipe/pattern implementation. Preserve target-specific changes; do not overwrite a whole file merely because its name maps. Record source-atom → pattern/version → target-atom edges and target-only adaptation atoms.
8. Run drift, repeated-application, partial-state and rollback checks, then behavior/integration gates for the exact candidate.
9. Attach the target commit, PR, generated output hashes, command/environment and results to the enhancement record.
10. Advance lastReviewedSourceCommit only after every relevant change **and every affected required atom** in the range has an explicit disposition. Deferred work remains visible. Pattern coverage and mapping coverage remain separate from behavioral acceptance.

Do not promise automatic ongoing monitoring merely because this procedure exists. Contributors maintain these records as part of future enhancement PRs; any bot, webhook or CI enforcement is separate implementation work.

## Mechanical recipe requirements

Every recipe must identify:
- Supported source and target baselines, required dependencies and tools
- Expected before hashes and exact transformed regions
- Intended output hashes or a deterministic derivation
- Refusal conditions: drift, unresolved types, ambiguous owner, unsupported mixed state or missing license
- Idempotence: replay yields the same output without duplicate registration or facades
- Partial failure: no accepted mixed snapshot; restart/recovery is explicit
- Rollback: reconstruct the previous reviewed tree without deleting unrelated contributor changes
- Verification commands and gates; no skipped tests disguised as success

For compiler transformations, include the source map and semantic preconditions. For generated JNI headers, service descriptors, module exports and serialized class names, track the producer and all downstream consumers.

## Reviewer questions

- Is the source owner actually the one read at the pin, or just a similar short name?
- Is the target existing, proposed, or a facade over a different owner?
- Does equality depend on content or one local identity namespace?
- Are language IDs, record IDs and graph coordinates being translated explicitly?
- Which conversions allocate or materialize, and who owns the resulting lifetime?
- Does the acceptance evidence execute this exact target commit and mode?
- Are old APIs, image versions and external ABI users preserved?
- Can the next enhancement be located and replayed without rediscovering the design?
- Have licensing and private-source boundaries been reviewed before importing code?

## Required follow-on tooling, not included here

Implementers may add a schema validator, source/target existence checks, dangling dependency detection, reciprocal enhancement links, hash verification and a CI rule preventing unsupported promotion. That tooling must have its own tests. This documentation PR neither installs nor runs it.


## Whole-JDK programme extension (2026-10-02)

The whole-JDK programme is specified by:

- [Whole-JDK M3 architecture](whole-jdk-m3-architecture.md)
- [JDK subsystem migration matrix](jdk-subsystem-migration-matrix.md)
- [Collections replacement specification](collections-replacement-spec.md)
- [String/text replacement specification](string-text-replacement-spec.md)
- [Work packets and durable resume guide](whole-jdk-work-packets-and-resume.md)

These documents extend the scope of migration review; they do **not** establish a new operational registry. Future implementation PRs must reconcile new JDK capabilities into the existing `name-mapping.json` `migration.records` authority and its selected validator/tooling. Do not pre-populate invented “accepted” records merely to make the architecture matrix look complete.

For whole-JDK work, each source inventory row must resolve to a stable capability mapping or an explicit reviewed disposition: replace backend, adapt, reuse, retain pending evidence, platform-specific, blocked, deferred or excluded custody/archive material. A package-level disposition cannot hide symbol-specific JNI, serialization, reflection, subclass, generated-source or VM obligations.

The existing many-to-many mapping rule becomes especially important for JDK work. One public class may project several storage/runtime owners; one M3 primitive may support several public owners; compiler lowering and VM/native consumers may be separate destinations for one source capability. Keep these relationships explicit rather than treating similar class names as a one-to-one rename.

A whole-JDK migration record should additionally identify, when applicable:

- JDK module and bootstrap phase;
- Java public/internal owner;
- HotSpot/GC/JIT consumers;
- JNI/JVMTI/FFM/native consumers;
- generated-source/build producer;
- serialization/reflection/service surfaces;
- Route A/B/C applicability;
- memory-model/linearization obligations;
- exact platform/collector scope.

Promotion still requires exact candidate evidence. An inventory row, a retained file, a successful recipe replay, a compiling tree, a benchmark win and an accepted runtime are distinct states.


## Current-source verification continuation (2026-11-12)

The continuation of [migration verification PR #2953](https://github.com/hsoliwal/M3jdk21/pull/2953)
uses the exact target input `0163b1474862ca463a64f24b45b0c6ca36aa4e73`.
The [passive continuation receipt](../qualification/migration-verification-20261112/continuation-receipt.json)
binds each emitted file to its canonical recipe, immutable Synexia execution source
and before/after digest. The canonical task remains
`synexia-openrewrite-recipes/recipe-crates/m3jdk21-migration-verification-20261112`;
ordinary Java outputs use its typed Java recipe and acceptance/build/data outputs
use its text recipe. No task recipe or execution harness is delivered here.

| Existing owner | Implemented result | Evidence boundary |
| --- | --- | --- |
| `M3StringPositionPrecompute` | Counts the existing byte-offset and long-mask lanes once in retained primitive-byte accounting. | The original invariant-6 budget regression fails before and passes after; field/method descriptors match. Compile-only dependencies isolate layout/accounting, so this does not establish installed String/native behavior or a measured storage reduction. |
| String source and route guards | Checks current mutable-input snapshots, exact cursor/key comparisons, fallback routes, overflow effects and exception-safe publication. | 58 target test methods pass; formerly accepted harmful source mutations remain counterexamples. The unchanged frozen-index owner passes construction and prepared-geometry contracts in normal and interpreted modes. |
| Filtered deque and primitive-sequence verification | Uses current product names and compiles the complete 1,185-source collection module with its original module descriptor. | The object-deque contract performs 140,734 checks and the primitive contract performs 164,104 checks. These finite contracts do not promote other collection/provider families. |
| Existing build workflows | Enables command-scoped Windows long paths, bootstraps the existing pinned JUnit launcher, builds JTReg before consumption, and runs regex Maven verification against the same job's new JDK image. | The 55-module prerequisite reactor was compiled, packaged and installed offline. Full Maven lifecycle, coverage and installed-image results remain separate required evidence. |
| Existing String phase records | Preserves historical ledger prefixes and records exact observed source succession, with strict record widths and immutable provenance names. | Source chronology is not semantic reconstruction or phase acceptance. Unsupported historical transitions retain explicit unresolved dispositions. |
| Recipe ownership classification | Restores canonical schema/predicate values and inventories every current Java file. | 16 acceptance tests pass. The production check still fails for 47 unclassified current classes and two stale historical paths; no classification is inferred from a similar name or a renamed body. |

Use the existing source checks and runtime gates above on the actual receiving
commit. Previously built images at `0163b147...` cannot qualify a later String
postimage. The same-job image verification step records its own source identity,
image and per-mode product receipts. Retain missing Maven plugins, seed snapshot
drift, unresolved public recipe custody and any failing native/runtime gate as
blocking evidence. This continuation does not advance the String → arrays →
collections → AST/compiler sequence, accept every JDK/JSR/JEP/JBS candidate, or
grant release authority.
