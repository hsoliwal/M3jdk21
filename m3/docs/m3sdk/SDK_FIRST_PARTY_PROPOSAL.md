# M3jdk21 SDK: first-party Synexia dependencies, findings and proposal

Scope: read-only research, 2026-10-12. Nothing was modified, committed or built in any repository.

| Input | Revision |
|---|---|
| M3jdk21 worktree `K:\m3-work\wt\m3jdk21-c7` | HEAD `6773c3b24cce` (branch `m3/dependency-source-inline-20261010`; `origin/master` `30c507d2` is an ancestor) |
| hsoliwal/com.synexia | `origin/develop` `b84f38d0ea` |
| hsoliwal/synexia-tornadovm | pin `3bf826714a48d7fdf346ffde8a0fc25c525572c5` (fork master `4cd1057e` has the same `m3/cpu-gpu` tree) |
| beehive-lab/TornadoVM | tag `v6.1.0` → commit `3c6c5f12c02c65a86f5f95c1747caa34b889437e` |

Companion files in this directory:
- `SDK_FIRST_PARTY_INVENTORY.tsv`: one row per artifact.
- `01_consumers.tsv`: who consumes which artifact.
- `02_artifact_findings.tsv`: findings for each artifact.
- `03_license_findings.tsv`: licence state for each target path.
- Raw evidence: `a3_*.tsv`, `tv_*.tsv`, `tv-cpugpu/`, `up_tornado_api_tree.tsv` and `dl-tornado-api-src/`. The last one holds the Central sources jar and blob comparisons.

---

## 1. Key findings

1. **`com.synexia:synexia-jdk-a3-recipes` is already carried as source.** The listing of it as a downstream dependency is stale.
   - `m3/pom.xml` lists `tooling/synexia-jdk-a3-recipes` immediately before `tooling/a3` in the default reactor.
   - That wrapper pom produces exactly this GAV. It compiles from `m3/vendor/synexia/synexia-openrewrite-recipes/src/main/java`, limited to the 8 classes named in the manifest.
   - All 8 vendored blobs match the export manifest, and all 8 are byte-identical on Synexia `develop` today.
   - The manifest blob (`8a3e3d6e`) and the export pom blob (`7beaa39f`) are also unchanged on develop.
   - `PublicA3ExportMirrorTest` re-checks the blob SHA-1 values.
   - Two records are therefore false positives: the row `m3/tooling/a3/pom.xml … NOT_VENDORED` in `DOWNSTREAM_DEPENDENCY_LEDGER.tsv`, and line 65 of `CORRECTIONS.md`.
2. **`com.synexia.m3:cpu-gpu-rv32im` is not in hsoliwal/com.synexia.**
   - It lives in **hsoliwal/synexia-tornadovm** (a public Apache-2.0 fork of beehive-lab/TornadoVM) at `m3/cpu-gpu/`.
   - M3jdk21 CI checks that fork out at `3bf82671` and runs `mvn install` (`.github/workflows/m3-tornadovm-bulk-adapter{,-gpu}.yml`; `m3/compatibility/synexia-tornadovm-bulk-adapter-20261009.tsv`, Tornado PR 7).
   - com.synexia only pins the fork as the submodule `donors/tornadovm` @ `4be6c461`. That commit predates `m3/cpu-gpu` by 145 commits.
   - `synexia-tornadovm-m3` is a different module (`com.synexia.tornadom3`).
3. **cpu-gpu-rv32im has no first-party dependencies and one third-party dependency that is not vendored:** `io.github.beehive-lab:tornado-api:6.1.0-jdk21`.
   - tornado-api depends only on the JDK (`module tornado.api { requires java.management; … }`).
   - The Central sources jar (sha1 `46de9043…`) matches the upstream `v6.1.0` tree blob-for-blob: 216/216 Java files, 33,938 LOC.
4. **Licence defect in tornado-api 6.1.0.**
   - `uk/ac/manchester/tornado/api/types/matrix/Matrix8x8Float.java` carries a **GPL-2.0-only** header with no Classpath Exception.
   - Apache-headed `KernelContext.java` imports it, and cpu-gpu uses `KernelContext`. It is therefore on the compile path.
   - The same header is present in v6.0.0 and v7.2.0.
   - `WithCUDAGraph.java` has no header at all. The other 214 files are Apache-2.0.
5. **A third first-party gap that the ledger does not record.**
   - `m3/tooling/migration-recipes/pom.xml` has two opt-in profiles. Each uses `com.synexia:synexia-openrewrite-recipes:1.0.0-SNAPSHOT` as a rewrite-maven-plugin dependency:
     - `m3-synexia-recipe-ownership-audit` activates `com.synexia.rewrite.M3Jdk21RecipeOwnershipAudit`.
     - `m3-synexia-canonical-recipes`.
   - The in-repo module `m3/synexia-import/pure-int-recipe-custody` produces that same GAV, but it holds only 6 `M3PureInt*` classes. So the audit recipe cannot resolve from in-repo source.
   - The audit's real closure is small: 2 Java files plus 1 YAML, depending only on rewrite-core and rewrite-java.
   - The full Synexia module is not portable: 1,761 main Java files (11.5 MB) and 19 first-party module dependencies.
6. **Duplicate classes on the a3 classpath.**
   - `tooling/a3` depends on both `synexia-jdk-a3-recipes` and `migration-recipes`.
   - Both contain the same 7 `com.synexia.rewrite.atom.*` classes, from identical source blobs.
   - Behaviour is identical, but this is a split-package / duplicate-class hazard.

---

## 2. Consumers in M3jdk21 HEAD (every pom mentioning `com.synexia*`)

| Consumer pom | Synexia coordinate | Role / scope | Activation | Resolved in repo? |
|---|---|---|---|---|
| `m3/tooling/a3/pom.xml` | `com.synexia:synexia-jdk-a3-recipes:1.0.0-SNAPSHOT` (`${m3.synexia.a3.version}`) | dependency, compile | default reactor | **yes**: `m3/tooling/synexia-jdk-a3-recipes` |
| `m3/ports/tornadovm-bulk/pom.xml` | `com.synexia.m3:cpu-gpu-rv32im:1.0.0-SNAPSHOT` | dependency, compile | profile `m3-tornado` | **no**: external fork |
| `m3/tooling/migration-recipes/pom.xml` | `com.synexia:synexia-openrewrite-recipes:1.0.0-SNAPSHOT` (`${m3.synexia.version}`) | rewrite-maven-plugin 5.23.1 plugin dependency | profiles `m3-synexia-recipe-ownership-audit`, `m3-synexia-canonical-recipes` (opt-in) | **partial**: same GAV from `pure-int-recipe-custody`, but without the audit recipe |
| `.m3/analysis-pom.xml` | `com.synexia.m3.bootstrap:m3-recipe-first-bootstrap:1.0.0-SNAPSHOT` | rewrite-maven-plugin plugin dependency | standalone pom | yes: `.m3/openrewrite-recipes/pom.xml` |

Other poms only *declare* a `com.synexia.*` groupId for themselves, so they are producers, not consumers:
- `m3/synexia-import/pure-int-recipe-custody` → `com.synexia:synexia-openrewrite-recipes`
- `m3/tooling/synexia-jdk-a3-recipes` → `com.synexia:synexia-jdk-a3-recipes`
- the `com.synexia.verification:*` proof poms under `m3/collections/recipe` and `m3/tooling/migration-recipes/tasks/m3-rc-10f/integration-c0/**`
- `m3/tooling/migration-recipes/apache-reuse` → `com.synexia.build:apache-public-reuse-proof`

`m3/ports/indexstring/pom.xml` mentions `com.synexia.indexstring.*` only as exec main classes.

---

## 3. Artifact details

### 3.1 `com.synexia:synexia-jdk-a3-recipes:1.0.0-SNAPSHOT`

- **Synexia source:**
  - Module `synexia-openrewrite-recipes/`.
  - Export pom `synexia-openrewrite-recipes/pom-jdk-a3-export.xml` (jar, `finalName synexia-jdk-a3-recipes`).
  - Manifest `src/main/resources/META-INF/m3/jdk-a3-recipe-export.tsv`.
  - Pin `a062661e91e769a00c2d027e1f04a7a0c8f9dc28` (PR 9599).
- **Size:** 8 main classes, 922 LOC: 7 under `com.synexia.rewrite.atom` plus `M3RecipeMasteryPortableReceipt`.
  - The Synexia export also has 7 test classes (57,567 bytes) that M3 does not mirror. M3 instead has its own 83-line `PublicA3ExportMirrorTest`.
- **Dependencies:**
  - Compile: `rewrite-java 8.17.1`. Transitive: `rewrite-core`.
  - Runtime: `rewrite-java-21`.
  - Test: `rewrite-test 8.17.1`, `junit-jupiter 5.10.2`. All of these are already vendored (`THIRD_PARTY_MODULES.tsv`).
  - Imports are limited to `org.openrewrite.*` and `java.*`.
  - First-party closure: **none**.
- **Licence:** `SPDX-License-Identifier: Apache-2.0` on all 8 files. `m3/vendor/synexia/LICENSE` and `NOTICE` are preserved.

### 3.2 `com.synexia.m3:cpu-gpu-rv32im:1.0.0-SNAPSHOT`

- **Source:** `hsoliwal/synexia-tornadovm` `m3/cpu-gpu/pom.xml` @ `3bf82671`, subtree `1a911b32af99b19201e79cc98dcc3c81764d2b8d`.
  - All 26 blobs were downloaded and verified with `git hash-object`.
  - The pom has no parent, `packaging` defaults to jar, Java release is 21, and it compiles with `-Xlint:all -Werror`.
- **Files (26):**

  | Part | Files | LOC |
  |---|---|---|
  | Main | 10 Java | 2,192 |
  | Test | 5 Java | 369 |
  | Other | `pom.xml`, `README.md`, `docs/BULK_PROVIDER.md`, `docs/DESIGN.md` | — |
  | Nested `recipe-bulk-provider/` | 7 files (`com.synexia.m3:cpu-gpu-bulk-provider-recipe`, rewrite-core 8.17.1) | 218 Java |

  The nested recipe module is not a reactor child and no M3 pom consumes it.
- **Dependencies:**
  - Compile: `tornado-api 6.1.0-jdk21`. Only `Rv32iKernel` and `TornadoRv32iSession` import `uk.ac.manchester.tornado.api.*`.
  - Test: `junit-jupiter 5.11.4` (vendored).
  - First-party closure: **none**.
  - The M3 adapter uses `Rv32iBatch`, `Rv32iBulkExecutor`, `Rv32iBulkReceipt` and, in tests, `Rv32iAssembler`.
- **Licence:**
  - SPDX Apache-2.0 on every `.java` and `.md` file.
  - `pom.xml` and the recipe's `manifest.tsv` have no header.
  - There is no LICENSE file inside the subtree. The fork root has `LICENSE_APACHE2` (blob `57bc88a1`, identical to upstream).
  - The README records that the architecture was informed by `Vogtinator/risky-v` and `OpenDGPS/simulacore`, which are unlicensed, and that no code was copied from them.
- **Drift watch:** the unmerged fork branch `fix/m3-cpu-gpu-preview-build-20261011` adds only `m3/cpu-gpu/recipe-preview-build/**` (6 files). It does not touch the provider sources.

### 3.3 `io.github.beehive-lab:tornado-api:6.1.0-jdk21` (third-party, transitive)

- **Upstream:** beehive-lab/TornadoVM `v6.1.0`, `tornado-api/` subtree `df1b8feb`.
  - It is a JPMS module, `tornado.api`. It requires only `java.management` and has no Maven dependencies.
  - Size: 216 Java files, 33,938 LOC, about 1.06 MB.
- **Fork note:** at `3bf82671` the fork's own `tornado-api` is `6.1.1-jdk21-dev`. It has 6 changed files and 6 added files (the tile API), so it is not the same as the release. cpu-gpu compiles against the **Central 6.1.0-jdk21** release, so the upstream tag is the correct preimage, not the fork.
- **Licence:**
  - The README per-module table says "Tornado-API … Apache 2.0", and the pom `<licenses>` says Apache-2.0.
  - **Exception:** `Matrix8x8Float.java` has a GPL-2.0-only header (see §1.4).
  - `WithCUDAGraph.java` has no header.

### 3.4 `com.synexia:synexia-openrewrite-recipes:1.0.0-SNAPSHOT` (opt-in profiles)

- The YAML recipe `com.synexia.rewrite.M3Jdk21RecipeOwnershipAudit` is defined in `META-INF/rewrite/m3-m3jdk21-recipe-ownership-audit.yml` (blob `2e580606`). It is composed of:
  - `M3Jdk21RecipeOwnershipAuditRecipe.java` (blob `5f21af59`, 263 LOC)
  - `M3Jdk21CanonicalResidueSealRecipe.java` (blob `6c2b2f34`, 390 LOC)
- Both classes import only `org.openrewrite.*` and `java.*`. Their other `M3*` names occur only inside string literals.
- Tests: `M3Jdk21RecipeOwnershipAuditRecipeTest` and `M3Jdk21CanonicalResidueSealRecipeTest`.
- The full module's first-party closure is:
  - `synexia-parent`, plus
  - `m3-java-contracts`, `m3-java-inventory`, `synexia-iop`, `synexia-build-inventory`, `synexia-ccore`, `synexia-algorithms`, `synexia-etl`, `synexia-etl-dfs`, `synexia-flink-datastream`, `synexia-beam-etl`, `synexia-fastsearch`, `synexia-chrome2api-java`, `m3-fast-search`, `synexia-classic-tools`, `synexia-donor-inventory`, `synexia-index-precompute`, `synexia-sap-compat`, `synexia-job`, `synexia-indexstring`.
- The full module also needs third-party artifacts that are not vendored:
  - `rewrite-groovy`, `-gradle`, `-hcl`, `-protobuf`, `-json`, `-properties`, `-maven`
  - `rewrite-static-analysis 1.4.2`, `ecj 3.46.100`, `jdt.core 3.47.0`
  - plus their transitive closures.

---

## 4. What `m3/vendor/synexia/` is

- **Contents (103 files):**
  - `LICENSE` (Apache-2.0), `NOTICE` (the Synexia/CogniX NOTICE with its donor attributions), `README.md`.
  - `.m3/m3-recipe-first-invariants.tsv`.
  - `synexia-code-convergence/`: 5 Java files plus `algorithm_kernel.c`.
  - `synexia-indexstring/`: 73 Java files, JNI `shared-arrays` C++ and CMake, and `THIRD_PARTY_NOTICES.md`.
  - `synexia-openrewrite-recipes/`: 14 Java files and the A3 export manifest.
  - `third-party/mapbox-jni-ownership/LICENSE.txt` (ISC).
- **Pinning model.** The README says this directory holds "multiple independently pinned Apache-2.0 Synexia snapshots". Each snapshot carries its own repository, revision and manifest, with mirrored files kept at their Synexia source paths:

  | Snapshot | Revision | Manifest / provenance |
  |---|---|---|
  | Seed | `62aea466…` | `m3/synexia-import/synexia-seed-export.tsv` (16 rows of sha256 + licence + mode), manifest root `ca570e87…` |
  | A3 export | `a062661e…`, PR 9599 | `m3/synexia-import/intakes/jdk-a3-public-export-20261007/SOURCE_PROVENANCE.tsv` (key/value: repo, revision, PR, export pom and manifest git blobs, licence, target artifact, target source root) |
  | JNI shared-array custody | `857c4cc0…` | `intakes/jni-array-address-receiver-20261007/{SOURCE_PROVENANCE,TARGET_MAP}.tsv`; custody only, not buildable |

  Additional intakes (`ast-navigation`, `regex-precompute-v7`, `string-*` receipts) use `PROVENANCE.json`, `SOURCE_CUSTODY.json` and `FOSS_REUSE_DECISION.tsv`.
- **Build glue:**
  - A target-local wrapper pom points `sourceDirectory` at the vendored path and restricts `<includes>` to the manifest rows. Example: `m3/tooling/synexia-jdk-a3-recipes`, documented in `m3/docs/PUBLIC_A3_RECIPE_ARTIFACT.md`.
  - A mirror test fails closed on any blob drift.
  - Rule: "The module is build glue, not a second recipe home". Changes are made in Synexia first and re-imported under a new pin; snapshot history is never rewritten.
- **Can it carry the remaining artifacts?** Yes, for all Synexia-authored Apache-2.0 material, including cpu-gpu-rv32im.
  - The README already allows one repository per snapshot, so a `hsoliwal/synexia-tornadovm` snapshot fits.
  - It is **not** the right home for third-party tornado-api. That belongs in the `m3/vendor/third_party/` mechanism: per module `PROVENANCE.json`, `SOURCES_MANIFEST.tsv`, `UPSTREAM_LICENSE.*`, `THIRD_PARTY_NOTICES.md` and `verify_source.py`; coordinate `<ver>-m3-source-1`; a row in `THIRD_PARTY_MODULES.tsv`; an API-surface gate.

---

## 5. Licensing rule per target path (`m3/docs/synexia-intake-policy.tsv`)

| Material | Target path | Policy mode | Disposition |
|---|---|---|---|
| A3 recipes (already carried) | `m3/vendor/synexia/**` + `m3/tooling/synexia-jdk-a3-recipes` | `EXTERNAL_RECIPE_TOOL` (Apache-2.0, `m3/tooling/**`, copy allowed in tooling only, no runtime dependency; needs pinned source + recipe hash + fixed-point/drift proof) / `INDEPENDENT_APACHE_MODULE` | ADMITTED |
| cpu-gpu-rv32im | `m3/vendor/synexia/synexia-tornadovm/**` + `m3/ports/cpu-gpu-rv32im` | `INDEPENDENT_APACHE_MODULE` (Apache-2.0, `m3/**` independent module, `ALLOW_WITH_NOTICES`, no runtime dependency of `java.base`; needs LICENSE/NOTICE + module boundary + build/test proof) | ADMISSIBLE |
| Ownership-audit recipe closure | `m3/vendor/synexia/synexia-openrewrite-recipes/**` + a `m3/tooling/**` wrapper | `EXTERNAL_RECIPE_TOOL` | ADMISSIBLE |
| tornado-api 6.1.0 | `m3/vendor/third_party/tornado-api-6.1.0` | not governed by this policy (Synexia intake only); third-party Apache-2.0 regime | ADMISSIBLE except `Matrix8x8Float.java`, which needs review (GPL-2.0-only, no matching policy row → fail closed) |
| Any of the above copied into `src/**` (OpenJDK, GPLv2+CPE) | `src/**` | `TARGET_OWNED_PORT` (no direct copy) or `EXPLICIT_RELICENSE_PORT` (review) | **not proposed**: everything stays under `m3/**` |

---

## 6. Proposal

FOSS-reuse classification for every leaf:
- DIRECT_IMPORT of pinned source (byte-identical), plus a target-local WRAP pom.
- No new architecture and no regenerated code.

### Leaf P1: correct the ledger (no code)
1. In `.m3/task-crates/dependency-source-inline-20261010/DOWNSTREAM_DEPENDENCY_LEDGER.tsv`, change the `m3/tooling/a3/pom.xml com.synexia synexia-jdk-a3-recipes` row from `NOT_VENDORED` to an in-reactor first-party status, for example `IN_REACTOR_FIRST_PARTY_SOURCE m3/vendor/synexia/synexia-openrewrite-recipes`.
2. Correct `CORRECTIONS.md` line 65.
3. Make the ledger generator resolve reactor-sibling producers (any pom whose own GAV matches) before it marks a dependency `NOT_VENDORED`.
4. Add the missing gap row: `m3/tooling/migration-recipes/pom.xml` plugin dependency `com.synexia:synexia-openrewrite-recipes` (opt-in profiles).

### Leaf P2: vendor `cpu-gpu-rv32im` as Synexia-authored source
- **Source custody:** `m3/vendor/synexia/synexia-tornadovm/m3/cpu-gpu/**`. This is a byte-identical copy of subtree `1a911b32` at `hsoliwal/synexia-tornadovm@3bf826714a48d7fdf346ffde8a0fc25c525572c5`. Copy all 26 files, including `recipe-bulk-provider/` as custody only.
  - Keeping the source path under a repository prefix avoids collisions with com.synexia snapshots.
- **Provenance intake:** `m3/synexia-import/intakes/cpu-gpu-rv32im-provider-<date>/`, containing:
  - `SOURCE_PROVENANCE.tsv` (same key/value schema as the A3 intake): `source_repository=hsoliwal/synexia-tornadovm`, `source_revision=3bf82671…`, `source_pr=7`, `source_subtree=m3/cpu-gpu`, `source_subtree_tree=1a911b32…`, `license=Apache-2.0`, `license_file_blob=57bc88a1…`, `target_artifact=com.synexia.m3:cpu-gpu-rv32im:1.0.0-SNAPSHOT`, `target_source_root=m3/vendor/synexia/synexia-tornadovm/m3/cpu-gpu`.
  - `SOURCE_MANIFEST.tsv` with path and git blob for each of the 26 files. The values are in `tv_cpugpu_tree.tsv`.
  - `FOSS_REUSE_DECISION.tsv` (DIRECT_IMPORT + WRAP). It should also record the README's risky-v/simulacore "informed-by, no code copied" note.
- **Wrapper module:** `m3/ports/cpu-gpu-rv32im/pom.xml`, coordinate `com.synexia.m3:cpu-gpu-rv32im:1.0.0-SNAPSHOT`, mirroring `m3/tooling/synexia-jdk-a3-recipes`:
  - `sourceDirectory` and `testSourceDirectory` point at the vendored `src/main/java` and `src/test/java`.
  - Dependencies: `tornado-api 6.1.0-jdk21-m3-source-1` (from P3) and `junit-jupiter 5.11.4-m3-source-1`.
  - Surefire passes through `m3.gpu`.
  - A `PublicCpuGpuMirrorTest` recomputes git-blob SHA-1 for every manifest row and fails on any extra file.
- **Reactor:** in the `m3-tornado` profile of `m3/pom.xml`, add `ports/cpu-gpu-rv32im` before `ports/tornadovm-bulk`. `ports/tornadovm-bulk/pom.xml` needs no change.
- **CI:** in `.github/workflows/m3-tornadovm-bulk-adapter.yml` and `-gpu.yml`, delete the external `actions/checkout` of `hsoliwal/synexia-tornadovm` and the `mvn install` of its `m3/cpu-gpu`. Build with `-Pm3-tornado` instead, and add the new paths to the `paths:` filter.
- **Notices:** append a "TornadoVM RV32IM provider snapshot" section to `m3/vendor/synexia/README.md`. Covering the material with the existing `LICENSE` and `NOTICE` is sufficient, because it is Synexia-authored Apache-2.0.
- **Ownership:** canonical ownership stays with hsoliwal/synexia-tornadovm, the same "fix upstream first, re-pin" rule as A3.
  - Optionally, advance com.synexia's `donors/tornadovm` submodule (`4be6c461`) so that Synexia also sees `m3/cpu-gpu`. That is a separate Synexia-side leaf.

### Leaf P3: vendor third-party `tornado-api 6.1.0` (required by P2)
- **Path and coordinate:** `m3/vendor/third_party/tornado-api-6.1.0/`, coordinate `io.github.beehive-lab:tornado-api:6.1.0-jdk21-m3-source-1`.
  - Preimage: beehive-lab/TornadoVM `v6.1.0` (`3c6c5f12`) `tornado-api/` (subtree `df1b8feb`). This equals the Central sources jar sha1 `46de9043…`, verified 216/216 blobs.
- **Files:** the standard third_party set: `PROVENANCE.json`, `SOURCES_MANIFEST.tsv`, `UPSTREAM_LICENSE.txt` (= `LICENSE_APACHE2` `57bc88a1`), `THIRD_PARTY_NOTICES.md` and `verify_source.py`.
  - Add a row to `THIRD_PARTY_MODULES.tsv`.
  - Run an API-surface gate against the Central jar. Its `module-info` must match, because this is a JPMS module.
- **Build placement:** add the module to `m3/vendor/third_party/pom.xml`, or to a `tornado` profile there if it should stay out of the default third-party build.
- **Licence gate (blocking):** resolve `Matrix8x8Float.java` (GPL-2.0-only, no CPE) before admission. Options, in order of preference:
  1. Ask upstream (beehive-lab) to confirm or correct the header; the module is declared Apache-2.0 and the repository has no plain-GPLv2 licence file. Then vendor unchanged and record the clarification.
  2. Admit the module as `Apache-2.0 AND GPL-2.0-only`. Ship `LICENSE_GPLv2CE` together with an explicit note in the NOTICE, and accept GPLv2 obligations for the optional `m3-tornado` artifacts. This needs an owner decision, because the policy has no row for it.
  3. Exclude the file with an overlay of `KernelContext.java` that removes the `simdgroupMatrix*` methods. The existing `OVERLAY_SOURCES.json` / `excluded_from_compilation` fields support this, but it modifies upstream and changes the API surface.
- **Not vendored:** the GPU runtime (`tornado-runtime` and drivers, GPLv2+CE plus native OpenCL/PTX/SPIR-V backends). It is not a Maven dependency of cpu-gpu-rv32im, and it is only needed on the self-hosted GPU runner. Record it as an environment prerequisite of the optional `-Dm3.gpu=true` gate, not as an SDK dependency.

### Leaf P4: slim export for the ownership-audit profile
- Mirror the audit closure byte-identically under `m3/vendor/synexia/synexia-openrewrite-recipes/src/main/{java,resources}/…` at a new pin (the current develop `b84f38d0` or the merge that introduced it):
  - `M3Jdk21RecipeOwnershipAuditRecipe.java`
  - `M3Jdk21CanonicalResidueSealRecipe.java`
  - `META-INF/rewrite/m3-m3jdk21-recipe-ownership-audit.yml`
  - the 2 tests
- Preferred approach: ask Synexia to publish an export manifest and pom for it first (`pom-jdk-ownership-audit-export.xml`), the same way the A3 export was done.
- Add a wrapper `m3/tooling/synexia-jdk-ownership-audit/` with a **distinct** artifactId, for example `com.synexia:synexia-jdk-ownership-audit-recipes`. Then change the `m3-synexia-recipe-ownership-audit` profile to depend on that artifactId instead of the overloaded `synexia-openrewrite-recipes`.
  - Overloading that GAV already collides with `pure-int-recipe-custody`.
- Leave `m3-synexia-canonical-recipes` as an explicitly external, opt-in profile, documented as **outside the SDK closure**. Vendoring the full module would pull in about 20 Synexia modules and 10+ OpenRewrite language modules, which is not proportionate.

### Leaf P5 (optional hygiene)
- Mirror the 7 Synexia A3 export tests (blobs listed in §3.1 evidence) into the A3 wrapper, so the exact upstream proof runs in M3.
- Remove the duplicate `com.synexia.rewrite.atom.*` classes from `m3/tooling/migration-recipes`, either with an exclude or by making it depend on `synexia-jdk-a3-recipes`. This leaves a single owner on the `tooling/a3` classpath.

---

## 7. Third-party closure added

| Leaf | New third-party modules | Already vendored and reused |
|---|---|---|
| P1 | none | — |
| P2 | none directly | junit-jupiter 5.11.4 (+engine/launcher) |
| P3 | **`io.github.beehive-lab:tornado-api:6.1.0-jdk21`** (216 Java files, 33.9 kLOC, no Maven dependencies) | — |
| P4 | none | rewrite-core / rewrite-java / rewrite-java-21 / rewrite-test 8.17.1, junit-jupiter 5.10.2 |

Net result: one new third-party module (tornado-api). It is used only by the optional `m3-tornado` profile.

---

## 8. Risks

1. **Licence (blocking for P3):** the GPL-2.0-only `Matrix8x8Float.java` inside an Apache-declared module (see P3 options). `WithCUDAGraph.java` has no header.
2. **Version split:** the fork's own `tornado-api` is `6.1.1-jdk21-dev` and differs from Central 6.1.0. Vendoring from the fork instead of the upstream tag would silently change the API that cpu-gpu was proven against.
3. **Cross-repository ownership:** cpu-gpu-rv32im's canonical home is hsoliwal/synexia-tornadovm, not com.synexia. Its "fix upstream first" path is a third repository, and com.synexia's submodule pin is stale (`4be6c461`).
4. **Unpinned snapshots:** `${m3.synexia.version}` / `1.0.0-SNAPSHOT` in the migration-recipes profiles resolve whatever happens to be in `~/.m2`. The custody module `pure-int-recipe-custody` installs a *different* class set under the same GAV, so resolution is non-deterministic until P4 gives the audit profile its own artifactId.
5. **Ledger trust:** the downstream ledger's `NOT_VENDORED` status for A3 was wrong, so other `NOT_VENDORED` rows should be re-checked against reactor-local producers. The rows for `m3/tooling/module-packs/{kernel,recipes}` junit-jupiter lack a version.
6. **GPU gate environment:** physical-GPU parity still needs an installed TornadoVM runtime and drivers on the runner. These are GPLv2+CE and native, so they are not source-vendorable as part of this task. This is acceptable because the adapter is not part of `java.base` and is opt-in.
7. **Duplicate classes:** the same `com.synexia.rewrite.atom.*` FQCNs are produced by two jars on the `tooling/a3` classpath.

---

## 9. Evidence index

| Claim | Evidence |
|---|---|
| A3 mirror exact | `a3_manifest.tsv` vs `a3_vendored_blobs.tsv` (8/8), and `a3_develop_blobs.tsv` (8/8 on develop) |
| A3 closure | `a3_imports.txt`, `a3_files.tsv` |
| cpu-gpu source | `tv_cpugpu_tree.tsv`, `tv-cpugpu/` (blobs re-hashed: ALL BLOBS VERIFIED), `tv_files.tsv` |
| tornado-api preimage | `up_tornado_api_tree.tsv` (upstream v6.1.0); `dl-tornado-api-src/central_blobs.tsv` vs `upstream_blobs.tsv` (216/216) |
| tornado-api fork drift | `tv_tornado_api_tree.tsv` vs `up_tornado_api_tree.tsv` (6 files differ: pom, module-info, KernelContext, TaskGraph, TaskGraphInterface, TaskPackage; 6 files exist only in the fork: `exceptions/TornadoDeviceTileNotSupported` and `tile/{DType,PartitionView,TensorView,Tile,TileContext}`) |
| Audit closure | `M3Jdk21RecipeOwnershipAuditRecipe.java.txt`, `M3Jdk21CanonicalResidueSealRecipe.java.txt` |
| Policy | `synexia-intake-policy.snapshot.tsv` |
| Vendored third-party list | `THIRD_PARTY_MODULES.snapshot.tsv` |
