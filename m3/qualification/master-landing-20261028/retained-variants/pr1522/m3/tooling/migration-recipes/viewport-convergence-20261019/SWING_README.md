<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Swing recipe proof custody

The parent PR provides the current collections recipe mirror.
[SWING_CANONICAL_SOURCE.tsv](SWING_CANONICAL_SOURCE.tsv) adds 81 exact files from
[Synexia #12528](https://github.com/hsoliwal/com.synexia/pull/12528), commit
`74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd`, for the original three-recipe SDK fixture.
Synexia is the canonical owner; the target retains this exact reproduction copy.

The two Swing manifests are:
`m3-swing-convergence-20261019` (7 guards, 3 changes; SHA256
`11f3d4890741deaa42323a11069c8c2287613dbf1f57db450e80cd8404d3781f`)
and `m3-swing-ui-convergence-20261019` (12 guards, 12 changes; SHA256
`8700ab0a685f1d8ac2890874d13a440461d1a496e92907759cc718a6dc7b6d16`).

Their original actual OpenRewrite SDK test also exercises the historical
`M3ViewConvergence20261019` recipe. That older collections recipe and its
preimages are included solely to preserve the exact nine-test SDK closure.
**Do not apply the historical view recipe to current M3JDK21 collections.**
The parent PR's separately qualified 60-target current recipe owns that receiving.

From this directory, with the original Java21/Maven SDK dependencies provisioned:

```sh
mvn -f synexia-openrewrite-recipes/recipes/m3-collection-refinement-20261019/viewport-convergence/pom.xml test
```

This command is a reproduction instruction; mirroring did not rerun the tests.
The published nine-test receipt remains separate from the three-test current
collections receipt. Every existing mirrored file was copied without source edits.

OpenJDK code and before-fixtures retain GPL-2.0-only WITH Classpath-exception-2.0
headers. Full OpenJDK LICENSE and ADDITIONAL_LICENSE_INFO, module notices and
the inspected donor rights ledger are under the canonical viewport-convergence
`licenses/M3JDK21` directory. Apache-2.0 recipe/tooling custody does not change
the license of embedded target source.

Current source admission proved all 19 Swing guards and the complete original
73-file module equal to the earlier qualified source; selected output is identical.
Use the receiving reports and published receipts for observed EDT, JList, JUnit,
descriptor and source-identity results. Keep default-off list activation and the
documented eventless-mutation/resource limits. Full installed-JDK, jtreg/JCK,
native host and Android/iOS acceptance remain separate.
