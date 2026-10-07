# M3 RECIPE_FIRST sidecar

Isolated Java 21/OpenRewrite recipe crate; native build files are untouched.

- Build: `mvn -f .m3/openrewrite-recipes/pom.xml clean install`
- Inventory: `mvn -f .m3/analysis-pom.xml -Pm3-recipe-first-inventory org.openrewrite.maven:rewrite-maven-plugin:dryRun`
- Task lane requires `m3.llm.taskCrateFile` plus exact SHA-256 `m3.llm.taskCrateRoot`.

Source-changing work must be authored as a reusable tested recipe after these gates. Direct LLM target-file editing is not an allowed lane.

Synexia handoff crates use the `synexia-*` namespace. The receiver admits both materialized
M3 ports and reusable OpenRewrite recipe/test sources under this sidecar; the provenance guard runs
before hash-pinned snapshot recipes. M3-owned direct bytes must remain Apache-2.0, and target
promotion still requires the target's own build/test/runtime evidence.
