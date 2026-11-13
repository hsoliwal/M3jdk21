<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# MIndex to M3 SDK scaffold (2026-10-18)

Status: **SCAFFOLD-FIRST RECEIVER**. Every Synexia `MIndex*` declaration is mapped to an `M3*` owner in
the M3 SDK tree; the DATA_STRUCTURE, COLLECTIONS, ARRAYS and PRECOMPUTE families are received as
compiling declaration skeletons in hash-pinned batches of about 25 classes. Atoms (behaviour) are not
received yet: every scaffold member throws the module's shared `M3NotYetReceived` marker. No
`java.*`, `jdk.internal.*` or `src/**` file is touched; java.base families (STRING_CORE, REGEX, JNI,
FACTS, AST) are mapped only.

| File | Role |
| --- | --- |
| `MINDEX_TO_M3_MAP_20261018.tsv` | 2,253 `MIndex*`/`M3Index*` classes at Synexia develop `11c94feb1815`: family, naming-law family, existing M3 target (MAPPED / TOKEN_MATCH / NONE), PROPOSED M3 name, target module, status, third-party deps, live fleet branch/PR, batch |
| `MINDEX_TO_M3_SCAFFOLD_LEDGER.tsv` | one row per received skeleton: batch, status, target path/type, SHA-256, deferred members, donor path, Git blob, donor SHA-256, families, co-donors |
| `../docs/name-mapping.d/m3-mindex-to-m3-scaffold.json` | name-mapping fragment (source -> target lineage) for the serial manifest reducer |

Receiving packages follow `m3-family-naming-target-disposition.tsv`: `com.m3.util` (GENERAL_COLLECTIONS)
and `com.m3.ds` / `com.m3.graph` (SPECIALIZED_DATA_STRUCTURES / DAG_GRAPH_RELATION) in
`m3/ports/collections`, `com.m3.arrays` (ARRAYS) in `m3/arrays`, `com.m3.precompute` (PRECOMPUTE) in
`m3/ports/precompute`; the naming law's preferred `jdk.internal.m3.*` homes stay the post-qualification
targets.

## Admission order (four-leaf handoff, `M3_MINDEX_FOUR_LEAF_HANDOFF_20261017.md`)

1. Donor identity: each donor is pinned by path, Git blob and SHA-256; the projection refuses drift.
2. Canonical recipe: Synexia crate `synexia-openrewrite-recipes/crates/m3-mindex-to-m3-scaffold`
   (`com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe`) applies the typed `ChangeType` copy contract
   of `M3CopyMIndexDeclarationRecipe` (PR #7827) and declares one canonical copy configuration per row
   for reactor execution; postimages, fixed point and refusals are proved in the crate test.
3. Receiving owner: **PROPOSED only**; never inferred from spelling alone, reviewed per row before fill.
4. Atoms are ported later, per owner, with target licensing and module rules.
5. JDK module/API/serialization/GC/concurrency/JNI proofs: NOT RUN.
6. This receiver is a ledger revision; historical provenance and source digests are retained.

Gate per batch: `javac --release 21 -Xlint:all,-module -Werror` over the touched module's
`src/main/java` (existing sources plus skeletons). Not claimed: behaviour, equivalence (NEC),
performance, runtime admission, final names.
