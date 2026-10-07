# JEP 493 — Linking Run-Time Images without JMODs

Status: dependency-closure / file-atomic candidate inventory. No compatibility or implementation
completion claim.

## Upstream authority

- JEP: 493
- OpenJDK implementation commit:
  `2ec358082f0896480bdbfcb289b4ba2bff0dd828`
- final donor state for materialization: `jdk-24+36`
- implementation commit touched 47 paths.

The selected path set is recorded in `PATHS.txt`. Candidate postimages are generated from the
JDK24 GA tree, not hand-copied from the implementation commit, so cumulative GA follow-up fixes are
retained.

## Why this packet is stacked on the file-atomic generator

JEP 493 mixes:

- Java implementation under `jdk.jlink`;
- new Java runtime-link classes;
- make/autoconf/spec text;
- jlink resource properties;
- TEST.ROOT metadata;
- focused jlink tests.

The M3 generator therefore uses both exact owners:

- Java -> `M3Jdk21HashPinnedSnapshotRecipe`;
- strict UTF-8 text -> `M3Jdk21HashPinnedTextSnapshotRecipe`.

Generation is `--crate-size 1 --include-text`: one physical FILE atom per replay unit. This does
**not** imply that the feature is FILE-scope semantically. The composed JEP packet is at least
MULTI_MODULE/tool-build scope and may be promoted only after dependency closure and build/runtime
proof.

## Dependency audit

Initial source inspection found the imported JDK-internal owners used by the final JDK24 jlink code
already present in Java 21, including `OperatingSystem`, `ModuleBootstrap`, `ModulePath`,
`ModuleReferenceImpl`, `ModuleResolution`, `jdk.internal.opt.CommandLine`, and
`ExcludeJmodSectionPlugin`.

New `JRTArchive`, `LinkableRuntimeImage`, and `runtimelink` classes are part of the feature
itself.

This is only an admission result. Compile and behavior proof remain authoritative.

## Required next proof

1. generate all selected Java/text file atoms from JDK21 -> JDK24 GA;
2. account for every touched path or typed exclusion;
3. review per-file diffs against Java21 preimages;
4. compose the smallest honest feature packet DAG;
5. build `jdk.jlink` / JDK image on Java21 baseline;
6. run runtimeImage jlink jtreg plus existing jlink regressions;
7. prove JMOD-based Java21 behavior remains available;
8. prove linking from a linkable runtime image works;
9. second-pass recipe fixed point;
10. only then move from dependency-closure/materialize to verify/promote.

No Java 21 API/removal or language-semantic change is admitted by this inventory packet.

## Current-master generated-crate execution

The repository retains the executor required to turn the deterministic generator output into an
exact candidate without hand-editing any jlink file:

- generator: `m3/backports/generate_recipe_crates.py`;
- executor: `m3/backports/materialize_generated_crates.py`;
- selected set: this packet's 47-path `PATHS.txt`;
- granularity: `--crate-size 1`;
- lanes: structured Java plus strict UTF-8 text;
- FILE atom authority remains FILE; feature composition remains MULTI_MODULE.

The executor accepts only generated hash-pinned manifests. It rejects path escape, symlinks,
duplicate target ownership, preimage drift, multi-target crates when the FILE gate is enabled, and
postimage hash drift. Exact postimages replay to a fixed point.

The checked-in recovery branch contains no JEP 493 product mutation. The dedicated workflow
generates the crates from pinned `jdk-21+35` and `jdk-24+36` trees, verifies the 47-path
denominator, materializes only in the ephemeral checkout, proves the changed-path fence and fixed
point, then configures with `--enable-linkable-runtime`, builds the image, runs the
runtime-image/legacy jlink jtreg gates, and performs a real no-JMOD runtime-link smoke.

A green workflow is implementation evidence, not automatic promotion authority.

