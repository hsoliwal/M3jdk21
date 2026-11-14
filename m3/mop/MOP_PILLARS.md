<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# MOP pillars for M3JDK21: run from Synexia

The three Machine Oriented Programming pillars (atomize, patternize, refactor; see
[SELF_IMPROVING_RECIPES.md](SELF_IMPROVING_RECIPES.md)) are implemented as one Java 21 tool, a
javac Plugin plus a CLI over the compiler's own `com.sun.source` trees, in the Synexia crate

`hsoliwal/com.synexia` : `synexia-openrewrite-recipes/crates/m3-mop/javac-pillars`
(package `com.synexia.rewrite.mop.javac`, branch `m3/mop-javac-pillars-20261030`, PR https://github.com/hsoliwal/com.synexia/pull/14187).

Per [AGENTS.md](../../AGENTS.md) ("Recipe-owned improvement memory is mandatory") recipe execution
machinery and recipe memory stay in Synexia. M3JDK21 runs the tool; it does not carry the sources,
a pattern registry or memory rows. Refusals are recorded by Synexia's own
`com.synexia.rewrite.mop.compiler.M3RecipeMemory` into the recipe's `RECIPE_MEMORY.tsv`; the
M3JDK21 `com.m3.mop.M3RecipeMemory` proof-receipt memory is unchanged and not involved.

## Invocation from an M3JDK21 checkout

```sh
# classes = the crate's src/main/java + the module's mop/compiler/M3RecipeMemory.java,
# compiled with javac --release 21, plus the crate's META-INF/services/com.sun.source.util.Plugin
CP=<javac-pillars classes or jar>

# atomize + patternize inside javac (no second AST): one run per compilation
javac --release 21 -processorpath "$CP" \
  "-Xplugin:M3Mop out=<run-dir> root=<repo-root> registry=<recipe-crate>/PATTERN_REGISTRY.tsv" \
  <sources>

# the same projection from the command line (parse-only javac task)
java -cp "$CP" com.synexia.rewrite.mop.javac.M3Mop atomize --root=<repo-root> --out=<run-dir> m3/mop/src/main/java
java -cp "$CP" com.synexia.rewrite.mop.javac.M3Mop plan    --atomize=<run-dir> --out=<plan-dir>
java -cp "$CP" com.synexia.rewrite.mop.javac.M3Mop gate    --path=<rel> --before=<file> --after=<recipe output> \
  --again=<recipe output applied twice> --out=<gate-dir> --recipe=<recipe id> --memory=<recipe-crate>/RECIPE_MEMORY.tsv
java -cp "$CP" com.synexia.rewrite.mop.javac.M3Mop chain   --root=<repo-root> --proposal=<dir> --again=<dir> \
  --out=<run-dir> [--write] --recipe=<recipe id> --memory=<recipe-crate>/RECIPE_MEMORY.tsv
```

Exit status: 0 PASS, 1 REFUSED or FAIL, 2 usage, 3 HOLD (no fixed-point witness). Outputs:
`FILES.tsv`, `ATOMS.tsv`, `PATTERNS.tsv` (`M3-PATTERN/1`), `MANIFEST.sha256`, `PATTERN_PLAN.tsv`,
`CHAIN.tsv`, `RECEIPT.jsonl` (`M3-MOP-RECEIPT/1`). Write outputs under a run directory, never under
`m3/`; promotion into M3JDK21 sources remains the existing recipe/PR gate.

Lineage: [m3-mop-pillars-20261030.json](../docs/name-mapping.d/m3-mop-pillars-20261030.json).
