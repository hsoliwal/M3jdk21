<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Focused canonical recipe custody: parser

This receiving mirror contains exact files from `hsoliwal/com.synexia` commit
`bf28d16edd4106be7961aefdb895e3e9f3ad30f7` ([canonical PR #12514](https://github.com/hsoliwal/com.synexia/pull/12514)).
`SOURCE_FILES.tsv` names every canonical file, immutable Git blob and SHA-256.
The four source owners, three tests, selected templates and original Maven POM
are byte-identical mirrors. In particular `M3EditScope.java` and the original
text contract test/fixtures are borrowed unchanged from that commit.
There is no receiving adaptation or replacement transformation implementation.

The selected pass is `com.synexia.m3.ParserOwnerCurrent20261019` and its immutable crate is `m3-parser-owner-current-20261019`.
It admits the exact before/after source hashes in its manifests. It produces
only the selected reviewed postimages, then proves each leaf and whole-recipe
fixed point. Java files remain OpenRewrite compilation units. The JDT
JavaBuilder syntax bootstrap, when selected, is restricted to its exact
upstream path, crate and digest pair; its successor must parse losslessly.
Missing, changed, duplicate or wrong-representation existing sources refuse.
The canonical snapshot's generic new-source checks and its runtime proofs are
finite; later strengthening of canonical ABSENT tests is recorded separately.

## Replay the exact selected recipe

Use an actual Java 21 SDK process. The original POM pins OpenRewrite coordinates
8.17.1 and JUnit 5.10.2. Its runtime identity test explicitly verifies the actual
published core JAR SHA-256 and embedded 8.17.0-SNAPSHOT manifest, together with
the Java parser 8.17.1 identity. No binary is relabeled to match a coordinate.
Maven may retrieve these official verification artifacts in a clean cache.
They do not become product runtime dependencies.

From this `recipe-custody` directory, first validate the exact checked-in source:

```sh
sha256sum --check SOURCE_SHA256SUMS.txt
```

Use a new absolute proof directory for each attempt so failed or successful
older evidence remains intact. After the source/hash gate succeeds, compile:

```sh
mvn -B -ntp -f synexia-openrewrite-recipes/verification/parser-collections-sdk-20261019/pom.xml \
  -Dproof.directory=/absolute/fresh/proof-directory \
  -Dm3.crate=m3-parser-owner-current-20261019 -Dm3.recipe=com.synexia.m3.ParserOwnerCurrent20261019 \
  -DskipTests test-compile
```

After strict compilation succeeds, run the already compiled actual SDK tests:

```sh
mvn -B -ntp -f synexia-openrewrite-recipes/verification/parser-collections-sdk-20261019/pom.xml \
  -Dproof.directory=/absolute/fresh/proof-directory \
  -Dm3.crate=m3-parser-owner-current-20261019 -Dm3.recipe=com.synexia.m3.ParserOwnerCurrent20261019 \
  surefire:test
```

Only actual `Recipe.run` returned source files are exported under that proof
directory's `sdk-output/`, alongside `SDK_EXECUTION.tsv`. Source paths there
refer to the target repository in the manifest (M3JDK21 or the original JDT
fork). This replay does not silently apply them to a checkout. Runtime source
builds, API/JNI compatibility and resource measurements remain separate gates.
The adjoining product qualification receipts state their exact tested source
and runtime scope; they do not promote unvisited modules or the blocked
existing mastery lab.

## Canonical authority and rights

This mirror supports receiving review and reproducibility. Future recipes,
source refinement decisions and cumulative category reviews belong to the
canonical Synexia owners and standing invariant mirrored under `docs/`.
The exact source and template headers preserve Apache, OpenJDK GPL and Eclipse
EPL obligations per file. No challenge-site editorial code was copied.
