<!-- SPDX-License-Identifier: Apache-2.0 -->
# Third-party code and attribution under `m3/`

Inventory date: 2026-10-08 (master `c9b07049c5`). Method (CPU only, reproducible):
`git grep -hE '^import' -- 'm3/**/*.java' src/java.base/share/classes/java/lang/M3*.java src/java.base/share/classes/jdk/internal/mindex/*.java`
filtered to packages outside `java.*`, `javax.*`, `jdk.*`, `sun.*`, `com.m3.*`; every `pom.xml` under
`m3/` scanned for `<dependency>` coordinates; every `NOTICE`/`LICENSE` file under `m3/` listed. The
apex run ledger (`docs/M3-SCALE/runs/20261007-m3jdk21-apex/` in com.synexia: `THIRD_PARTY_INLINE_DECISION.tsv`,
`FOSS_REUSE_DECISION.tsv`) carries the per-row evidence.

## 1. Shipped M3 owners: no third-party code

`m3/collections`, `m3/arrays`, `m3/ports/*`, `java.lang.M3*` and `jdk.internal.mindex.*` import only
`java.*`, `jdk.*` and `com.m3.*`. Every donor of a shipped owner is first-party Synexia source
(Apache-2.0, `Copyright 2026 Hitesh Soliwal and contributors`), recorded per leaf in
`m3/compatibility/synexia-*-receipt.json` and `m3/docs/name-mapping.d/*.json`. Nothing in this
section requires a third-party notice.

## 2. Third-party build and test tooling (not shipped in the JDK image)

| Artifact | Version | License (SPDX) | Upstream | Role | Where | Obligation |
| --- | --- | --- | --- | --- | --- | --- |
| OpenRewrite `rewrite-core`, `rewrite-java`, `rewrite-java-21`, `rewrite-test` | 8.17.1 (`<rewrite.version>` in 16 poms) | Apache-2.0 | https://github.com/openrewrite/rewrite | Recipe engine for hash-pinned receivers and proofs | `m3/tooling/migration-recipes`, `m3/tooling/{a3,cb,lane28,tq-sync,module-packs}`, `m3/collections/recipe`, `m3/vendor/synexia` (10 importing files) | Keep license and NOTICE when redistributed; nothing copied into this tree, consumed as a Maven dependency |
| Jackson `jackson-annotations`, `jackson-databind` (transitive via the OpenRewrite BOM) | 2.16.1 | Apache-2.0 | https://github.com/FasterXML/jackson | Recipe option binding (`@JsonProperty`) | `m3/tooling/migration-recipes` (17 importing files), `m3/vendor/synexia` (3), `m3/tooling/module-packs` (1) | As above; transitive dependency, not copied |
| JetBrains `annotations` | 24.1.0 | Apache-2.0 | https://github.com/JetBrains/java-annotations | Declared in 13 tooling poms; **no `org.jetbrains` import exists under `m3/`** (also transitive via OpenRewrite) | `m3/tooling/**` task poms | None beyond the Apache-2.0 notice; declared-unused, kept only inside sealed task packets |
| SLF4J `slf4j-api` | 1.7.36 (18 poms), 2.0.9 (6 poms) | MIT | https://github.com/qos-ch/slf4j | Logging facade required by OpenRewrite; **no `org.slf4j` import exists under `m3/`** | `m3/tooling/**` task poms | MIT notice applies only on redistribution of the library; not redistributed |
| SLF4J `slf4j-simple` | 2.0.13 | MIT | https://github.com/qos-ch/slf4j | Test-scope logger binding for recipe proofs | `m3/collections/recipe/pom.xml` and task poms | As above |
| JUnit Jupiter | 5.10.2 | EPL-2.0 | https://github.com/junit-team/junit5 | Test harness (test scope) | every `m3/**` module with tests | Not distributed with the product; EPL applies to the harness only |
| JUnit Platform Console Standalone | 1.12.2 (sha256 pinned in `.github/workflows/m3-collections.yml`) | EPL-2.0 | https://junit.org/junit5/ | Hosted test runner for the installed collections job | CI only | Downloaded at run time, not in the tree |
| Eclipse Temurin JDK | 21.0.12.1+1 and 21+35 (sha256 pinned in workflows) | GPLv2 with Classpath exception | https://adoptium.net | Pinned compiler for hosted proofs | CI only | Not in the tree |

## 3. Vendored third-party license files

| Path | Covers | License | Status |
| --- | --- | --- | --- |
| `m3/vendor/synexia/third-party/mapbox-jni-ownership/LICENSE.txt` | the external `jni/ownership.hpp` wrapper referenced by the mirrored `shared_arrays.cpp` | ISC (Copyright 2016, Mapbox) | License text kept; the header itself is **referenced, not copied** (see `m3/vendor/synexia/README.md`, "Third-party JNI ownership license") |

## 4. Donor dependencies deliberately never pulled

These libraries are imported by Synexia donor modules but no file importing them has been
ported, adapted or copied into M3JDK21; the owners that depend on them are excluded from the
donor set (NTPRA-LAW: no third-party absorb). Their absence is part of the inventory so a future
port cannot bring them in silently.

| Library | License | Synexia files that import it (develop `296323958b1`) | M3JDK21 disposition |
| --- | --- | --- | --- |
| Eclipse Collections | EPL-1.0 OR EDL-1.0 (BSD-3) | `synexia-common` `DenseCollections`, `DenseIndexedValues`, `DenseObjectBag`, `DensePairs` | EXCLUDE as donors (`collections.dense-facade-donor` EVIDENCE_ONLY); `M3ObjectIdentityMap/Set` and the packed owners come from `synexia-primitives` instead |
| RE2/J | BSD-3-Clause | `synexia-indexstring` `MIndexPatternPlan`, `MIndexRe2Plan`, `MIndexRegex`, `MatIndexRe2Matcher`, `MatIndexRe2Pattern` | EVIDENCE_ONLY (`regex.reference-engine`); `java.util.regex.Pattern` remains the regex authority; the M3TQ gate and the A5 oracle use the JDK engine only |
| commonmark-java | BSD-2-Clause | `synexia-indexstring` `structure/MIndexMarkdownCodec` | not ported; no Markdown codec exists under `m3/` |
| ASM | BSD-3-Clause | `synexia-mindex` (per module pom) | not ported; `jdk.internal.mindex` is first-party |
| Eclipse SWT / JFace / Nebula | EPL-2.0 | `synexia-mindex` UI | not ported |
| JMH | GPLv2 with Classpath exception | `synexia-indexstring` benchmarks only (no main source) | not ported; no timing claims are made |

## 5. Refactoring outcome

No shipped source needed refactoring: the third-party surface of the delivered owners is already
empty, which the import inventory proves rather than asserts. The declared-but-unused `slf4j-api`
and `org.jetbrains:annotations` coordinates live only in sealed task packets whose poms are part of
content-addressed receipts; they are left untouched and recorded here instead. New task crates
should declare only `rewrite-*`, `junit-jupiter` and, when a logger binding is needed for a proof,
`slf4j-simple` in test scope.
