# M3JDK21 migration-recipes receiver surface

Status: target-side receiver / migration residue / proof harness.

Canonical reusable M3/OpenRewrite/Maven recipe implementations live in
`hsoliwal/com.synexia` under the Apache-2.0 Synexia recipe/tooling owners.

This directory is **not** a competing canonical recipe home.

Allowed target-local content:

- JDK-specific backport and receiver recipes;
- thin adapters for Synexia handoff crates;
- exact preimage/postimage fixtures and manifests;
- target-specific Maven/OpenRewrite activation glue;
- JUnit/jtreg/proof harnesses.

Historical reusable implementations under `scope/`, `atom/`, `semantic/` and `a3/`
are migration residue. Their reusable authority is mapped by
`m3/compatibility/synexia-recipe-home-policy.tsv` and
`.m3/m3jdk21-synexia-borrowing.tsv` in the canonical Synexia repository.

Any reusable improvement discovered here must first be authored/proved in Synexia and then
returned to M3JDK21 through an exact pinned Apache-2.0 handoff. M3JDK21 retains final product,
license, compatibility, build, jtreg, runtime and promotion authority.
