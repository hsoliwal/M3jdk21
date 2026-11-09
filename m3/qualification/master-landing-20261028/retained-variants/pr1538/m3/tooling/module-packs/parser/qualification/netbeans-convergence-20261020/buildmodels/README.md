<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Existing build-model boundary qualification

This directory contains frozen, finite API-shape fixtures for the existing
`M3ASTAuthoritativeModels` owner. They exercise Maven/Gradle depth limits,
bounded collection/Iterable materialization, and Gradle/Groovy/Kotlin pending
frontiers. The fixtures check exact valid topology, source flags and ranges,
null behavior, and repeated shared occurrences. They are test doubles; they
do not initialize actual Maven, Gradle, Groovy, or Kotlin providers.

## Replay

Use Python 3 and a JDK 21, with the target repository as the working directory.
`--out` must be a fresh path. The runner performs no network access or artifact
resolution and uses only the four existing owner source files and six frozen
Java fixtures.

```sh
python3 m3/tooling/module-packs/parser/qualification/netbeans-convergence-20261020/buildmodels/verify.py \
  --repo-root "$PWD" --jdk-home "$JAVA_HOME" --out /tmp/m3-buildmodels-candidate
```

The candidate must pass all ten cases, totaling 321 Java assertions. The runner
compiles with `--release 21 -Xlint:all -Werror`, checks the exact existing public/
protected API, and writes per-command logs and a receipt containing source,
fixture, toolchain, command, exit, and API identities. The three unchanged
dependency owners are recorded as used; they can receive separately qualified
campaign changes. The primary owner's expected hash remains exact.

To reproduce the nine baseline counterexamples, supply the exact preimage
`M3ASTAuthoritativeModels.java` extracted from
`df532e779056dbec203dde20030657c3d144f5b5` with `--owner-source`, add
`--expect baseline`, and choose another fresh output directory. The runner
requires the recorded baseline failure diagnostics as well as the valid case's
308 successful assertions. Reproducing those counterexamples does not qualify
the baseline for promotion.

`FIXTURES.sha256` seals the six fixture files. `expectations.json` retains
independently observed baseline failures and candidate success markers;
`expected-api.txt` retains the public/protected baseline bytecode surface.
The first exploratory Kotlin assertion wrapper was corrected before the
candidate was authored; the frozen fixture rejects provider probe assertions
even if existing reflection wraps them as `IllegalArgumentException`.

## Separate provider gate

Actual provider qualification remains in
`.github/workflows/m3-ast-real-providers-20261019.yml`, using the pinned test-only
dependencies in `m3/tooling/module-packs/parser/real-providers/pom.xml` and the
existing `M3RealProvidersSmoke`, `M3GradleToolingSmoke`, and `M3KotlinPsiSmoke`.
That gate initializes original JDT/Maven/Groovy/Gradle/Kotlin APIs. Gradle's
smoke probe starts the actual 8.12 tooling distribution/daemon. Do not put the
API-shape fixture classes on that gate's classpath.

The bounded fixture runner establishes Java behavior for these finite cases.
It does not establish source-qualified dependency closure, full JDK/jtreg or
installed-image qualification, comparative performance, JNI qualification,
or completion of the ordered receiving-family phases.

Canonical custody:
`hsoliwal/com.synexia/synexia-openrewrite-recipes/recipes/netbeans-convergence-20261020/buildmodels`,
through `com.synexia.rewrite.M3NetBeansRecipe("buildmodels")` and the existing
source-sealed installer. The manifest seals these additions as previously
absent; receiving files are generated from that recipe.
