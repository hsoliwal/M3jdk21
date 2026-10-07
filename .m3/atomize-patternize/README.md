# M3 atomize/patternize Synexia receiver

M3JDK21 is a public target repository. Synexia is the canonical owner/master workspace for
reusable M3 Maven/OpenRewrite recipes, donor convergence, atomization/patternization mastery,
static-precompute observers, and Java/JNI proof machinery.

M3JDK21 does not fork those reusable owners. It consumes one exact Apache-2.0 Synexia revision,
records provenance/receipts, supplies JDK-specific source-layout and compatibility context, and
retains final OpenJDK build, jtreg, runtime, compatibility, and promotion authority.

## Canonical mastery lane

The default read-only recipe is:

`com.synexia.m3.EveryModuleAtomPatternMastery`

The pinned Synexia recipe resource maps that name to
`com.synexia.rewrite.M3EveryModuleAtomPatternMasteryRecipe`. The composition owns no source
mutation, semantic-equivalence, donor-copy, replacement, merge, or promotion authority.

For the lighter application/inventory lane:

```sh
export SYNEXIA_ATOM_PATTERN_RECIPE=com.synexia.m3.EveryModuleAtomPatternApplication
```

## 1. Bind the exact M3JDK21 target inventory

```sh
bash .m3/atomize-patternize/bootstrap.sh
```

Rebinding is explicit:

```sh
SYNEXIA_REBIND_INVENTORY=1 bash .m3/atomize-patternize/bootstrap.sh
```

The bootstrap does not invoke Maven or OpenRewrite. `BOOTSTRAP_V2` records the exact ordinary
Java/POM bytes plus source-layout ownership.

The receiver distinguishes:

- Maven roots such as `*/src/main/java` and `*/src/test/java`;
- OpenJDK product roots such as `src/<module>/<share|unix|windows|...>/classes`;
- OpenJDK build-tool roots such as `make/<component>/src/classes`;
- OpenJDK test roots such as `test/jdk`, `test/langtools`, and `test/hotspot/jtreg`.

Because M3JDK21 contains both `m3/` Maven modules and the non-Maven OpenJDK product tree, its
expected normal mode is `HYBRID_REACTOR_EXTERNAL`.

The bootstrap stores both the complete Java-root set and the subset with no Maven POM ancestor.
A normal run refuses source/POM content drift, file-set drift, Java-root drift, or external-root
drift before OpenRewrite is allowed to start.

## 2. Obtain the canonical Synexia recipe artifact

Normal execution no longer requires a manually preinstalled recipe JAR.

`run.sh` invokes:

```sh
bash .m3/atomize-patternize/install-synexia-recipes.sh
```

The installer is pinned to:

- repository: `https://github.com/hsoliwal/com.synexia.git`;
- revision: `2ddd629bcc5f037e26afe75a7655773abc79a138`;
- artifact: `com.synexia:synexia-openrewrite-recipes:1.0.0-SNAPSHOT`;
- source license: Apache License 2.0.

Before building, it verifies the exact Synexia commit plus pinned LICENSE, NOTICE, and root-POM Git
blobs. Maven builds `synexia-openrewrite-recipes` with its exact dependency closure into the
isolated repository `.m3/target/atomize-patternize/m2`.

`synexia-recipe-install-receipt.tsv` records:

- source repository/revision;
- Apache-2.0 declaration and LICENSE/NOTICE/root-POM blob identities;
- recipe coordinates;
- built recipe JAR SHA-256;
- build status.

A maintainer may deliberately use a reviewed preinstalled artifact only by setting all of:

```sh
export SYNEXIA_ALLOW_PREINSTALLED_RECIPE=1
export SYNEXIA_RECIPE_VERSION=<reviewed-version>
export SYNEXIA_RECIPE_JAR_SHA256=<reviewed-64-hex>
export SYNEXIA_MAVEN_REPO_LOCAL=<repository-containing-that-artifact>
```

The override is never implicit.

## 3. Execute the content-bound receiver

```sh
bash .m3/atomize-patternize/run.sh
```

The runner rebuilds `source-files.tsv`, verifies it against the bound inventory, verifies the
complete Java-root sets, obtains the pinned Synexia recipe artifact, and then runs OpenRewrite
using `dryRunNoFork` only.

For `HYBRID_REACTOR_EXTERNAL`, execution covers both:

1. bound Maven POMs using their own Maven model;
2. non-Maven OpenJDK Java roots using a temporary Java-21 analysis envelope.

The receipt records source roots, Maven POM count, exact recipe artifact/revision/JAR hash, Maven
analysis-run count, external OpenJDK analysis-run count, and final analysis status.

Generated/build/vendor trees are excluded from the target inventory. File names containing tab,
CR, or LF are refused because the evidence format is line-oriented and exact.

## Offline receiver self-test

The layout classifier has a dependency-free self-test:

```sh
bash .m3/atomize-patternize/self-test.sh
```

It proves Maven source ownership, OpenJDK product/platform/build/test root classification, and the
Maven-vs-external root split without network or Maven.

## Permanent ownership invariant

```text
hsoliwal/com.synexia
  canonical reusable recipe + donor + mastery owner
          |
          | pinned Apache-2.0 source/artifact + receipts
          v
hsoliwal/M3jdk21
  target inventory + JDK adapter + compatibility/jtreg/runtime authority
```

Rules:

1. recurring/refactoring/LLM logic is authored and mastered in Synexia first;
2. M3JDK21 receives proven outputs, receipts, or a thin target-specific receiver;
3. do not duplicate a reusable Synexia recipe implementation in M3JDK21;
4. FILE-local contract-preserving work may fan out independently;
5. refactoring authority grows only through the declared M3 scope ladder;
6. Java/JNI/native candidate evidence never overrides Java/JDK contract truth;
7. OpenJDK source outside `m3/` keeps its original license and obligations;
8. target promotion remains diff -> lint/static analysis -> compile -> jtreg/tests -> runtime and
   compatibility -> benchmark where relevant;
9. evidence/history is additive; convergence does not justify rebase/force-push archaeology loss.

Repeatable M3JDK21 defects are fed back into the Synexia recipe owner first, then replayed against
the target from an exact pinned preimage.

## CI truth

`.github/workflows/m3-synexia-atom-pattern-mastery.yml` contains:

- a fast shell/layout/bootstrap proof job;
- a full pinned-Synexia build + read-only mastery job.

GitHub Actions must actually allocate and run those jobs before any Maven/OpenRewrite/JUnit or
mastery PASS is claimed. A workflow-level red status with zero jobs is infrastructure evidence,
not a code-test result.
