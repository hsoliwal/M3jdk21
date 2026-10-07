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

Historical reusable implementations under `scope/`, `atom/` and `a3/` remain migration residue. The former local `com.m3.rewrite.semantic` implementation layer has been retired; target semantic-hash proofs now execute the qualified `com.synexia.rewrite.semantic` mirror. Their reusable authority is mapped by
`m3/compatibility/synexia-recipe-home-policy.tsv` and
`.m3/m3jdk21-synexia-borrowing.tsv` in the canonical Synexia repository.

Any reusable improvement discovered here must first be authored/proved in Synexia and then
returned to M3JDK21 through an exact pinned Apache-2.0 handoff. M3JDK21 retains final product,
license, compatibility, build, jtreg, runtime and promotion authority.


## Execute canonical Synexia recipes

The target module can load the canonical recipe artifact without making it a default dependency.

First build/install the exact reviewed Synexia revision:

    mvn -B -f /path/to/com.synexia/pom.xml \
      -pl synexia-openrewrite-recipes -am install

Then invoke the M3JDK21 receiver with the opt-in profile and an explicit canonical recipe:

    mvn -B -f m3/tooling/migration-recipes/pom.xml \
      -Pm3-synexia-canonical-recipes \
      -Dm3.synexia.version=1.0.0-SNAPSHOT \
      -Drewrite.activeRecipes=com.synexia.rewrite.M3RepositoryJava21ConvergenceRecipe \
      rewrite:dryRun

The profile has no active-by-default activation and selects no recipe automatically. Canonical
Synexia recipe execution therefore remains explicit, source-revision/proof bound and target-reviewed.


## Audit recipe ownership

After installing the exact reviewed Synexia recipe artifact, run the target-local receiver profile:

    mvn -B -f m3/tooling/migration-recipes/pom.xml \
      -Pm3-synexia-recipe-ownership-audit \
      -Dm3.synexia.version=1.0.0-SNAPSHOT \
      rewrite:dryRun

The active recipe is canonical in Synexia:

    com.synexia.rewrite.M3Jdk21RecipeOwnershipAudit

The audit is read-only. It classifies every `com.m3.rewrite.*` source under this receiver tree as
Synexia-canonical compatibility residue, target adapter/proof, or JDK-target-specific backport code.
A new unclassified reusable recipe fails closed. The target profile contains only Maven activation
glue and does not duplicate the canonical audit implementation.
