<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3JDK21 native agent recipe API — receiving milestone

This is **M3JDK21-owned control-plane Java source**, not an M3 overlay over Java and not an
installed runtime module yet. It must eventually be qualified in the JDK SDK distribution;
shipping a Maven jar or passing finite tests is not that proof. The canonical source and
mechanical receiver recipe live in `hsoliwal/com.synexia`:

- `synexia-m3-recipe/src/main/java/com/synexia/m3/recipe/M3RecipeAgent.java`
- `synexia-m3-recipe/src/main/java/com/synexia/m3/recipe/M3RecipeCatalogue.java`
- `synexia-m3-recipe/scripts/generate_agent_receiver.py`

Do **not** hand-edit these generated target Java files. Fix the canonical recipe/source,
regenerate, prove exact receiver hashes and complete recipe tests, then propose the reviewed
postimages. Check source parity with the generator (both working trees locally available):

```sh
python synexia-m3-recipe/scripts/generate_agent_receiver.py \
  --synexia-root /path/to/com.synexia \
  --target-root /path/to/M3jdk21
```

From the M3JDK21 tree, run the actual Maven qualification gate:

```sh
mvn -f m3/tooling/recipe-agent-api/pom.xml verify
```

A plain Java 21 compiler can independently compile `M3RecipeAgent`, `M3RecipeCatalogue`
and `M3RecipeAgentProof` and run its deterministic refusal proof without Maven/JUnit.

## Supported verified-by-prototype operations

- FILE: literal/regex replace, create, move and delete with explicit preimages.
- FOLDER: glob-scoped literal/regex replace with an **exact sealed file set**.
- Explicit DAG dependency order, missing-dependency/cycle/unordered-write refusals.
- Scope levels FILE/FOLDER/PACKAGE/MODULE/REACTOR/PUBLIC_API, with explicit reviewed promotion bit.
- Dry-run source/destination SHA-256 and fingerprint; application requires the reviewed fingerprint.
- Semantic transformation `SemanticProvider` SPI, discoverable through Java `ServiceLoader`;
  unregistered providers fail closed and output must be idempotent.
- Deterministic catalogue discovery and fail-closed selection; semantic descriptors are
  **not executable** without an installed qualified provider.

The agent is used through the Java API, not through a shell-executing recipe DSL:

```java
M3RecipeAgent agent = new M3RecipeAgent();
var scope = new M3RecipeAgent.Target(M3RecipeAgent.TargetKind.FOLDER,
        "src/main/java", "*.java");
var pre = agent.seal(workspace, scope);
var action = new M3RecipeAgent.Action("add-marker", M3RecipeAgent.Operation.REGEX,
        scope, "oldLabel", "newLabel", "", pre, 1, java.util.List.of(),
        M3RecipeAgent.Authority.FOLDER, "");
var preview = agent.preview(workspace, java.util.List.of(action));
// User/review process inspects preview.changes(), tests, gates and fingerprint.
agent.apply(workspace, java.util.List.of(action), preview.fingerprint(), false);
```

## Required admission gates

The standalone proof is one test, **not** complete admission. Still required for product
promotion: full Maven/JUnit, reusable OpenRewrite/M3AST/javac semantic provider implementations,
compiler ABI and contract oracles, regex hostile-input and native memory fuzzing,
path/TOCTOU adversarial tests, cross-platform performance and JNI gates, JDK source-build,
installation into SDK image, and applicable jtreg. Keep the ordered String → arrays →
collections → AST/compiler receiver boundary unaffected until preceding gates pass.

The apply implementation stages each file and attempts rollback; it is **not** crash-atomic
across multiple files. Java regex may backtrack: only trusted, reviewed recipes should be
admitted; the 2 MiB/file and file count limits do not constitute a regex time limit.
OpenRewrite's textual recipes can discard Java AST structure, so execute type-aware
OpenRewrite/M3AST transformations **before** any text-only recipes when mixed in one DAG.
