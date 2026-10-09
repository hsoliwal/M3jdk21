# M3 RECIPE_FIRST sidecar

Isolated Java 21/OpenRewrite recipe crate; native build files are untouched.

- Build: `mvn -f .m3/openrewrite-recipes/pom.xml clean install`
- Inventory: `mvn -f .m3/analysis-pom.xml -Pm3-recipe-first-inventory org.openrewrite.maven:rewrite-maven-plugin:dryRun`
- Task lane requires `m3.llm.taskCrateFile` plus exact SHA-256 `m3.llm.taskCrateRoot`.

Source-changing work must be authored as a reusable tested recipe after these gates. Direct LLM target-file editing is not an allowed lane.

## Synexia canonical String recipe receiver

Reusable **M3 String** recipe semantics and donor/provenance custody are canonical in
`hsoliwal/com.synexia`.

Current pinned donor packet:

- recipe: `com.synexia.rewrite.M3Jdk21StringCurrentConvergence`
- Synexia branch: `m3/m3jdk21-string-current-convergence-20261009`
- descriptor blob: `d5315dee9660754b36635f1cdb6de81c45ae8108`
- donor PR: `hsoliwal/com.synexia#10007`

This repository is the **thin product/runtime receiver**. It owns `java.base`, HotSpot, JNI,
M3 String runtime precompute, target-specific postimages, differential tests and promotion.
It must not become the canonical home for reusable String recipe semantics and must not add a
Synexia runtime dependency.

