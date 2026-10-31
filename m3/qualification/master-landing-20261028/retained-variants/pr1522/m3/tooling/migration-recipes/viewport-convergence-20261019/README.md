<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Canonical recipe proof mirror

This is exact proof-tooling custody of
[Synexia #12528](https://github.com/hsoliwal/com.synexia/pull/12528) at
`74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd`. Synexia remains the canonical reusable recipe owner.
M3JDK21 owns the received runtime and its acceptance. This directory introduces
no runtime dependency.

[CANONICAL_SOURCE.tsv](CANONICAL_SOURCE.tsv) records every mirrored path and
Git content identity. The original relative source/resource/POM layout is retained.
Root LICENSE/NOTICE apply to the canonical first-party tooling; per-file source
headers, the current before-fixture notices and target source rights remain intact.

The current source recipe guards 60 paths and changes 21. Its manifest SHA-256 is
`5bb24c679cea5b4d6daddd870323401466419f90ad105a845efe94352e708819`.
The actual OpenRewrite 8.17.1 PlainText SDK ran three tests successfully before
publication. The existing serial kernel ran 7,734 bounded checks. Those receipts
are at the canonical commit; byte-preserving mirroring is not a new SDK run.

From this directory, with a provisioned Java 21/Maven environment:

```sh
mvn -f synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/current-master/pom.xml test
```

The unchanged POM selects the two existing shared engines, the thin current
recipe and its exact test/fixtures. It does not claim JavaLST or whole-reactor
coverage. For the dependency-free serial kernel, compile the original
`M3GpuToolJoinManifest.java` and
`recipes/m3-collection-refinement-20261019/proof/M3CollectionRefinementRecipeProof.java`
with Java 21 into a fresh directory, then use its `qualify` and `emit-file`
commands against the current manifest directory and current-master/before.
The recorded qualification and emission commands are preserved in the canonical
evidence packet.

The two original receiving helpers are also provided at
`m3/collections/qualification/collection-refinement-20261019/`; they were
already source-sealed in the parent canonical recipe. The exact all-native
helper is mirrored under the canonical viewport-convergence/proof path.
Receiving documentation names its explicit native opt-ins and separate scope.

Keep STRING / QUALIFICATION_REQUIRED. This mirror does not advance receiving
phases or establish installed-image, jtreg/JCK, native host or mobile acceptance.
