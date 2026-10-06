# Synexia Apache-2.0 borrowing contract

## Purpose

M3JDK21 is a delivery target for polished Synexia convergence results. It may receive independently
authored Synexia Apache-2.0 code, JNI/native atoms, fixtures, Maven/OpenRewrite recipes, recipe
tests, documentation and proof metadata through content-addressed handoff crates.

This is deliberate reuse, not a fork of Synexia ownership. `hsoliwal/com.synexia` remains the
canonical convergence workspace for shared capabilities.

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
