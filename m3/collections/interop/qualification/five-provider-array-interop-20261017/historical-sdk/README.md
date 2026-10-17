# Retained SDK-family recipe qualification

**PASS:** the original `sdk-family-names-20261017` recipe completed `clean verify` against its exact historical fixture. All four original OpenRewrite SDK tests ran with zero failures, errors, or skips. The old recipe, its POM, YAML registration, frozen templates, and test assertions remain byte-identical to the current checkout: 31 recipe inputs were checked before and after execution.

The fixture contains all 19 manifest postimages. The original manifest is pinned to SHA-256 `3c992a514044453bc5784abf862701743f784ef67f5cfc4ee076ac073179efd7`. Thirteen generated historical outputs come from the recipe's sealed templates. Its three historical core/native owners are fetched from M3 commit `4e29dc9d4141261e8763fbe9ffd6a20e017b1aff` and checked against their exact manifest hashes. The three recipe-engine KEEP owners come from the unchanged recipe copy. This fixture does not replace or edit installed product sources.

## Executed historical contracts

| Gate | Executed result |
|---|---|
| Real OpenRewrite SDK scheduler, exact postimages, named discovery, fixed point, drift refusal | 4 tests; 0 failures, errors, or skips |
| Metadata self-test with heaps 32m and 128m | 137 checks per heap; 22 historical bindings across five families |
| Source audit | All 18 original factory method trees preserved; 22 mapped signatures |
| Recipe replay, before and after proof | 1,236 checks per run; 13 generated postimages; 32 fixed-point orderings |
| Native index/mask proof with heaps 32m and 128m | 167,145 checks and 4,096 seeded cases per heap |
| Java path with deliberately missing native library | 167,146 checks |
| Incorrect metadata method-name mutant | Rejected by the original assertion |
| Native high-bit truncation mutant | Rejected by the original assertion |

`maven.log`, `surefire-reports/`, and `historical-recipe-evidence/` contain the successful original execution. The exceptions printed by the two negative controls are expected rejections, followed by Maven `BUILD SUCCESS`. The unchanged Python verifier's final `NOT_RUN: ... Maven SDK` text describes that Python subprocess alone: the enclosing Maven execution actually ran the four SDK tests first, as its Surefire report records. Actual current donor runtime tests are separately recorded by the successor qualification.

## Explicit test-host corrections

The first online attempt failed before assertions because the historical POM does not supply SLF4J at runtime. The original POM remains unchanged. The workflow supplies `org.slf4j:slf4j-api:1.7.36` and `org.slf4j:slf4j-simple:1.7.36` using Surefire 3.2.5's documented `maven.test.additionalClasspath` parameter. Both downloaded JARs are SHA-256 checked:

- API: `d3ef575e3e4979678dc01bf1dcce51021493b4d11fb7f1be8ad982877c16a1c0`.
- Simple: `2f39bed943d624dfa8f4102d0571283a10870b6aa36f197a8a506f147010c10f`.

With logging present, the unchanged named-recipe assertion exposed pinned ClassGraph 4.8.165's Java 21 application-loader discovery failure. The diagnostic proves that the original YAML can be read and parsed, while the pinned scanner discovers no classpath entries without access to `jdk.internal.loader`. The workflow passes only `--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED` to the forked test JVM through Surefire `argLine`. The same scanner then finds the existing named recipe. The original named-discovery assertion remains active. Switching Surefire's system classloader alone did not resolve the problem and is not included in the final workflow.

The original failures are retained in `missing-slf4j-*`, `system-loader-*`, and `isolated-loader-*`. The initial offline attempt failed only because Maven's original clean-plugin was uncached; its log is also retained. The successful invocation uses the original recipe and plugin versions with strict Maven checksums and verified HTTPS transport.

## Current successor scope

The retained historical receipt is intentionally evaluated against its exact 22-binding historical fixture. Installed current sources are admitted by the successor's complete source census and SHA-pinned POM and binding metadata, and the current donor suite retains every original SDK-family test.

The proposed current workflow's strict Java 21 compilation was separately executed on the actual current checkout: all 117 core source files compile with `-Xlint:all,-module -Werror`. The manual core JAR now includes `src/main/resources`; its `com/m3/collections/M3_SDK_FAMILY_CATALOG.tsv` entry occurs exactly once and matches the source bytes. The exact thin-classpath binding entry point prints `checks=192 families=5 bindings=33`. `current-workflow/results.json` records these commands and results. The existing final seven-test SDK-family report was checked for completeness without rerunning the full 126-test donor suite.

The final canonical workflow template hash is `5ea668c71d226900c0754b84304638ca73aef5481242947e6620d6d0cb251d52`, replacing target preimage `b2f3763e73f45e1914791f7f1e5af284c29d28bde277c3915f1a895a4e90cb66`. Its source/contract admission is qualified by the root agent's separate final 42-target successor SDK run. No result here claims that the old historical hashes describe the new 33-binding implementation.

See `qualification.json` for the structured receipt, `historical-sdk-fixture.json` for all 19 source pins, `invocation.json` for all 31 unchanged recipe input identities, and `EVIDENCE_SHA256.tsv` for this evidence bundle's file identities. Absolute `/tmp` and workspace paths in executed records identify the actual proof environment; these copied reports are the retained deliverables.
