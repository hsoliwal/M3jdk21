# Pinned Synexia Apache receiver snapshots

This public tree contains **multiple independently pinned Apache-2.0 Synexia snapshots**. It is
not treated as one checkout at one revision.

## Seed snapshot

- repository: `hsoliwal/com.synexia`
- revision: `62aea466cf2f2bb4668f38aad9343d59525965b5`
- manifest: `m3/synexia-import/synexia-seed-export.tsv`
- manifest root: `ca570e874586c327f98e3982bf9e62a404e87e266b3195df69be06a0b5111ef6`

This snapshot contains the established convergence/JNI/OpenRewrite seed rows and remains immutable.

## Slim public A3 recipe export

- repository: `hsoliwal/com.synexia`
- revision: `a062661e91e769a00c2d027e1f04a7a0c8f9dc28`
- source PR: `9599`
- export manifest:
  `synexia-openrewrite-recipes/src/main/resources/META-INF/m3/jdk-a3-recipe-export.tsv`
- export manifest Git blob:
  `8a3e3d6e802e95dbcc7b0bf83a347887b02d6717`
- target provenance:
  `m3/synexia-import/intakes/jdk-a3-public-export-20261007/SOURCE_PROVENANCE.tsv`

The eight manifest-listed Java sources are mirrored byte-identically below the corresponding
`m3/vendor/synexia/synexia-openrewrite-recipes/` paths. The target-local module
`m3/tooling/synexia-jdk-a3-recipes` compiles only those manifest-listed files into the already
declared `com.synexia:synexia-jdk-a3-recipes:1.0.0-SNAPSHOT` coordinate.

The module is build glue, not a second recipe home.

## Ownership and use

Synexia remains the canonical reusable-recipe/mastery owner. M3JDK21 may retain:

- exact public Apache source snapshots,
- source/export manifests and provenance,
- target build wrappers,
- compatibility receivers,
- JDK-specific tests/adapters.

Reusable recipe changes are developed and proved in Synexia first. A newer public-safe export must
arrive through a new pinned provenance record; existing snapshot history is not rewritten.

For any capability:

```text
pinned Synexia snapshot
  -> exact receiver/provenance verification
  -> target-specific adapter/backport if required
  -> JUnit / compiler / jtreg / runtime / JNI parity as required
  -> fixed point
  -> explicit product promotion
```

The preserved `LICENSE` and `NOTICE` in this directory apply to imported Synexia material.
Third-party donor material with other obligations is not admitted automatically.

OpenJDK source outside `m3/` remains under its existing license regime.
