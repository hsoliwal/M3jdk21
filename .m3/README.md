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
- descriptor blob: `868f5fa770cf5c6562869b696a5dd5812c0d4afa`
- donor PR: `hsoliwal/com.synexia#10007`
- receiver recipe: `com.synexia.rewrite.M3Jdk21StringCurrentConvergenceReceiver`
- receiver descriptor blob: `df5f6c458532e9fb0d5eac950d797a3897625489`

This repository is the **thin product/runtime receiver**. It owns `java.base`, HotSpot, JNI,
M3 String runtime precompute, target-specific postimages, differential tests and promotion.
It must not become the canonical home for reusable String recipe semantics and must not add a
Synexia runtime dependency.

