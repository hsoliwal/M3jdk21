<!-- SPDX-License-Identifier: Apache-2.0 -->
# m3/tooling/jmod-recipes — every vendored library as a JMOD, descriptors written by OpenRewrite

Design: `m3/docs/m3sdk/m3-parallel-vcpu-design.md` §6 (SDK packaging as JMODs).

| Step | Tool | Output |
|---|---|---|
| 1. Named modules | `tools/jmod_image.py`: `jar --describe-module` (with `--release 21` for descriptors under `META-INF/versions`) over every Maven-built vendored jar; one version per module name; unresolved `requires` dropped | `JMOD_IMAGE.tsv` |
| 2. Split packages | `tools/split_packages.py`: packages per jar (release-21 view); merge groups | `SPLIT_PACKAGES.tsv`, `MERGE_GROUPS.tsv` |
| 3. Descriptor facts | `tools/descriptor_gen.py`: for jars without a descriptor, keep one version per artifact, drop jars whose packages a named module owns, merge each split-package group into one jar, then `jdeps --generate-module-info`. The jdeps output is normalized: `exports` of packages that contain only `package-info` are dropped, and so are `provides` clauses whose classes are absent | `DESCRIPTOR_MANIFEST.tsv` (module, artifacts, target, sha256), `DESCRIPTORS.tsv` |
| 4. Write descriptors | **OpenRewrite** `M3ModuleDescriptorRecipe` (a `ScanningRecipe` on the vendored rewrite-core 8.17.1) run by `M3ModuleDescriptorApply`. It writes each `module-info.java` into `m3/vendor/third_party-m3/descriptors/<module>/`. The upstream modules are never touched | receipt TSV (before/after sha256, `FIXED_POINT`) |
| 5. Package | `tools/jmod_pack.py`: multi-release jars are flattened to the release-21 view; each descriptor is compiled with `javac --patch-module`; then `jmod create`, `jlink java.se` + every vendored module, and `java --validate-modules` | `JMOD_PACK.tsv`, the image |

Recipe contract:
- A generated descriptor whose bytes differ from its pin is refused before anything is written.
- An absent target is created.
- A target that already holds the pinned bytes is left unchanged, so the second run makes 0 changes.
- A target with other bytes is never overwritten; the run reports `REFUSED` and exits with 3.

Tests (`mvn -o verify`): generate then fixed point, drift refusal, tampered-input refusal.

Evidence (`evidence/`, 2026-10-12, JDK 21.0.9, Windows):

| Item | Result |
|---|---|
| Named vendored modules (published descriptors) | 54, 31 of them multi-release flattened to the release-21 view |
| Jars without a descriptor | 33 descriptors written by the recipe. Merges: `org.openrewrite` = rewrite-core + rewrite-test, and `org.openrewrite.java` = rewrite-java + rewrite-java-21 (JPMS forbids their shared packages) |
| Older lines left out of the image | antlr4-runtime 4.11.1, jna 5.14.0, tomlj 1.1.1, checker-qual 3.37.0, slf4j-api 1.7.36 (see R2 convergence) |
| Recipe | first run: 33 added and fixed point; re-applying identical facts: 0 changes; changed facts on the old layer: `REFUSED` |
| Image | 119 modules = `java.se` closure + 87 vendored JMODs; `java --validate-modules` rc 0; 253 MB |

The jlink'ed image itself is a build output and is not committed.
