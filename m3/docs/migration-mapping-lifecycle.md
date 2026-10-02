# Durable migration mapping and enhancement porting

Status: proposed review protocol. The example files beside this document are explanatory artifacts, not an implemented migration database, runtime feature or validated port.

## Why the old name map is not enough

[name-mapping.json](name-mapping.json) deliberately records only early provisional directions. Preserve it as historical input. A type name alone cannot express source ownership, multiple targets, merged/split classes, per-operation compatibility or which later enhancement has been ported.

The next mapping must support many-to-many relations:
- One source owner can have a storage target, public facade and native adapter
- Several source types can consolidate only after their contracts are reconciled
- An unchanged source type may remain a dependency instead of moving
- One method may be deferred even when its enclosing class has a target
- A source bug fix is different from a feature port or a behavior-preserving rename

Use stable mapping IDs independent of filenames. Never reuse a retired ID for a different contract.

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
3. Resolve every changed source item to mapping IDs. Unmapped affected items become inventory work; never silently ignore them.
4. Classify each change: bug fix, feature, performance-only change, contract change, format evolution, dependency/security update, test/oracle update or irrelevant to this target.
5. Record target applicability and route A/B/C impact. “Not applicable” needs a reason and review, not an empty target.
6. Check source prerequisites and known defects. A new upstream test is evidence to port and execute, not proof the target passes.
7. Prepare a source-pinned recipe or reviewed manual port. Preserve target-specific changes; do not overwrite a whole file merely because its name maps.
8. Run drift, repeated-application, partial-state and rollback checks, then behavior/integration gates for the exact candidate.
9. Attach the target commit, PR, generated output hashes, command/environment and results to the enhancement record.
10. Advance lastReviewedSourceCommit only after every relevant change in the range has an explicit disposition. Deferred work remains visible.

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
