# Synexia Apache-2.0 borrowing contract

## Purpose

M3JDK21 is a delivery target for polished Synexia convergence results. It may receive independently
authored Synexia Apache-2.0 code, JNI/native atoms, fixtures, Maven/OpenRewrite recipes, recipe
tests, documentation and proof metadata through content-addressed handoff crates.

This is deliberate reuse, not a fork of Synexia ownership. `hsoliwal/com.synexia` remains the
canonical convergence workspace for shared capabilities.

## Canonical recipe and module ownership

The borrowing rule is consumer-only for reusable M3 code.

- `hsoliwal/com.synexia` is the canonical Apache-2.0 owner for reusable Maven/OpenRewrite recipe
  implementations, recipe DAGs, atom/pattern/IOP mastery logic and shared M3Index modules.
- `com.synexia:synexia-m3index-db` is the canonical M3Index DB coordinate; the M3JDK21-local
  `m3/indexdb/**` tree is migration/proof residue and must not evolve into a competing owner.
- `com.synexia:synexia-m3index-jdk-bridge` is the canonical reusable JDK-facing M3Index bridge.
- New generic recipe logic must be authored/proved in Synexia first. M3JDK21 may retain only
  JDK-specific backport/receiver mechanics, thin adapters, fixtures, manifests and proof harnesses.
- Historical reusable code under `scope/**`, `atom/**`, `semantic/**` and `a3/**` remains
  usable during migration but has no independent canonical authority.
- A target-side discovery that improves reusable mechanics flows back to Synexia as a recipe/module
  improvement before the dependent M3JDK21 promotion is considered complete.

The machine-readable target policy is
`m3/compatibility/synexia-recipe-home-policy.tsv`. The canonical source-side map is
`.m3/m3jdk21-synexia-borrowing.tsv` in `hsoliwal/com.synexia`.

## Frozen migration residue

Reusable implementations that are already canonical in Synexia are not merely marked non-authoritative;
their current M3JDK21 source bytes are frozen as migration residue.

The machine-readable seal is:

`m3/compatibility/synexia-canonical-residue-gitblobs.tsv`

It records the exact Git blob SHA-1 for every current reusable residue implementation under:

- `scope/**`;
- `atom/**`;
- `semantic/**`;
- `a3/**`;
- the target-local Java21 convergence catalogue/orchestrator copies;
- `m3/indexdb/src/main/java/**`.

The M3 tooling JUnit proof recomputes Git blob identity directly from checkout bytes and requires the
sealed set to equal the current residue set. Therefore:

1. editing a frozen residue implementation fails;
2. adding a new implementation under a frozen residue namespace fails;
3. deleting residue requires an explicit migration update;
4. tests, proof harnesses, JDK-specific receivers and thin target adapters may continue to evolve;
5. reusable behavior improvements must be authored/proved in `hsoliwal/com.synexia` and returned by
   the Apache-2.0 handoff or canonical Synexia Maven/OpenRewrite dependency.

The SHA-1 here is Git object identity, not a cryptographic security claim; source admission continues
to use the existing SHA-256 handoff/preimage gates.

## Receiver sequence

A generated Synexia declarative recipe executes in this order:

1. `M3SynexiaHandoffGuardRecipe` validates the source revision, packet hash, row count, canonical
   target paths and Apache-2.0 rule for M3-owned target surfaces.
2. `M3Jdk21HashPinnedSnapshotRecipe` applies Java postimages from exact preimages and accepts both
   normal M3 ports and recipe/test Java roots.
3. `M3Jdk21HashPinnedTextSnapshotRecipe` applies non-Java UTF-8 postimages from exact preimages.
4. Replaying the crate must reach a fixed point.
5. M3JDK21 build, jtreg and runtime proof remain required before promotion.

The Java receiver accepts `synexia-*` crates in addition to its JDK donor crates. Synexia recipe
code can therefore be transferred directly into:

- `.m3/openrewrite-recipes/src/main/java/**`
- `.m3/openrewrite-recipes/src/test/java/**`
- `m3/tooling/migration-recipes/src/main/java/**`
- `m3/tooling/migration-recipes/src/test/java/**`
- `m3/ports/**`

## License boundary

The M3-owned `m3/**` and `.m3/**` surfaces may contain independently authored Apache-2.0 Synexia
code and recipes. Raw third-party donor material is not converted to Apache-2.0 by the handoff.

Existing OpenJDK-derived files retain their existing OpenJDK license and notice regime. A Synexia
algorithm or design may be adapted into such a file only under the applicable target licensing and
provenance rules; the receiver never claims that an Apache-2.0 label relicenses OpenJDK.

## Authority

A valid handoff packet proves source identity and permitted candidate materialization. It does not
grant merge authority, remove target-local verification, or let M3JDK21 silently redefine Synexia's
canonical shared architecture. Useful target discoveries flow back to Synexia as explicit evidence
or recipe improvements.
