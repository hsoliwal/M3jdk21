<!-- SPDX-License-Identifier: Apache-2.0 -->
# Maven 4 completed models in the existing M3AST owner

`M3ASTAuthoritativeModels.maven4Effective` accepts the original
`org.apache.maven.api.services.ModelBuilderResult` through Maven 4's public
interface. The method obtains its completed effective model once and publishes
the same immutable `M3ASTIndex` used by the existing Maven 3 route. The caller
retains ownership of the actual builder, session, model, repositories and
compiler objects. The projection preserves the caller's existing `M3File`.

This addition does not construct a parser, `ModelBuilder`, resolver or project
session. The index contains occurrence kinds and primitive parent/sibling/range
coordinates. Effective values can originate in parent POMs, imported BOMs and
activated profiles, so all coordinates remain `(-1, -1)` with
`SOURCE_UNVERIFIED`. The index is unsuitable as authority for a source rewrite,
dependency resolution, classpath construction or build execution.

## Exact donor and ownership decisions

The qualified provider is the Apache-2.0
[hsoliwal/maven fork at 2993571eedbe96e06a737eaa8ae47cf112b255de](https://github.com/hsoliwal/maven/tree/2993571eedbe96e06a737eaa8ae47cf112b255de),
whose POM declares `4.1.0-SNAPSHOT`. Its
[ModelBuilderResult](https://github.com/hsoliwal/maven/blob/2993571eedbe96e06a737eaa8ae47cf112b255de/api/maven-api-core/src/main/java/org/apache/maven/api/services/ModelBuilderResult.java)
and [DefaultModelBuilder](https://github.com/hsoliwal/maven/blob/2993571eedbe96e06a737eaa8ae47cf112b255de/impl/maven-impl/src/main/java/org/apache/maven/impl/model/DefaultModelBuilder.java)
remain the authorities for completed models. No Maven implementation source or
binary is copied into `jdk.m3.parser`; the optional invocation boundary uses
public reflection, as the existing provider routes already do. The original
Apache license and notice remain in the source-built provider JARs. The M3
owner, tests and build metadata retain their existing Apache-2.0 notices.

| Choice | Contract and decision |
| --- | --- |
| Completed Maven 4 API result | Selected. Preserves the exact builder's inheritance, profile, interpolation and BOM results. |
| Raw POM XML or validation-only result | Insufficient for an effective model. A result whose effective model is absent is refused. |
| Maven 3 `ModelBuildingResult` | Retains its separate existing entry point. The 3 and 4 result types are not interchangeable. |
| A second model builder or inferred classpath | Unnecessary duplicate authority; not introduced. |
| Maven 4 `subprojects` and legacy `modules` | Use `subprojects` when nonempty, otherwise `modules`, matching the original fork's selection rule. Mixed declarations are subject to the original Maven validator. |
| Existing M3 index builder | Reused. The new route applies source/node limits and checks actual projected depth before returning the index. |

The shared projection keeps the Maven 3 getter order and output. JDT's original
AST and captured-character checks are unchanged. The new public method is the
only public callable added to this class. Effective-model occurrences remain
at most four levels deep; the final depth check walks those existing parent
ordinals without another index directory. Work is linear in projected
occurrences. No timing, allocation or whole-build speed improvement is claimed.

## Required qualification

`verify.py` pins the four baseline M3 owners from M3JDK21 commit
`2779aba3f98f31c324be43f731be7914e1a05935` and the exact successor owner bytes.
It refuses a different or dirty Maven fork. It builds the fork's required
19-module reactor and requires the real `DefaultModelBuilderTest` suite to run
41 tests with zero failures, errors or skips. An optional online provisioning
pass may populate dependencies; the acceptance pass always runs offline.

The provider classpath uses 16 source-built fork JARs, excludes all fork test
class directories, and records every external JAR's SHA-256 and size. External
Maven build plugins/dependencies and the incumbent Maven 3.9.11/JDT 3.40.0
libraries are cached artifacts; they are not represented as source builds.
The fixture does not contain provider stubs. A single delegating proxy counts
accesses to an actual completed result and returns its original model.

The standalone fixture runs both the frozen baseline and the emitted owner:

- The baseline has 13 checks, including real Maven 3 and JDT projections and
  the absence of the explicit Maven 4 entry.
- The successor has 102 checks covering inherited/interpolated dependencies,
  an offline local BOM import, an activated profile, plugin execution and
  dependency occurrences, legacy modules and Maven 4.1 subprojects.
- Real validation-only results, foreign/null providers and exhausted source,
  node and depth budgets are refused. Model projection still succeeds after
  the original POM is changed to invalid XML, proving that the completed result
  is consumed without rereading the file.
- Full incumbent Maven 3/JDT index shapes must match across both runs. A public
  descriptor comparison allows exactly the new `maven4Effective` entry.
- Every source and provider artifact is read back after qualification. A
  failed attempt leaves its command/log evidence and no `PROOF.json` pass.

The existing `m3-ast-real-providers-20261019.yml` workflow includes a separate
Maven 4 job with this exact source pin and these requirements. The original
JDT/Maven 3/Groovy/Kotlin/Gradle/Jackson job remains intact.

Example after the exact fork and baseline trees have been prepared:

```sh
python3 m3/tooling/module-packs/parser/real-providers/maven4/verify.py \
  --repo-root . --baseline-root /path/to/frozen-m3-baseline \
  --fork-root /path/to/pinned-maven-fork --jdk-home /path/to/jdk21 \
  --maven /path/to/maven/bin/mvn --cache /path/to/task-maven-cache \
  --out /path/to/absent-proof-directory
```

For an empty dependency cache, add `--online-bootstrap`; a proxy may be supplied
with `--settings`. The final proof still requires an offline pass. The fixture
creates private offline Maven settings and an isolated local fixture repository.

## Separate gates

This is a host Java 21 model-provider proof. The complete parser module,
compiler patch and JMOD/image tests retain their owning gate. Full Maven 4
distribution packaging, plugin/extension realms, every model-builder case,
custom Maven Resolver/m2e/Tycho/mvnd forks, current custom JDT-fork source builds,
live Eclipse workspace reconciliation, the rebuilt M3JDK21/HotSpot, and other
platforms require separate acceptance. No exact-head GitHub CI or publication
result is implied by a local receipt.
