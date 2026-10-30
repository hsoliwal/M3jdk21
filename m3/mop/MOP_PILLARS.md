<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21 MOP pillars: atomize, patternize, refactor

**Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.** Machine Oriented
Programming (MOP) is defined in [SELF_IMPROVING_RECIPES.md](SELF_IMPROVING_RECIPES.md). This
module gives the JDK the first three pillars as one Java 21 tool: a **javac Plugin** and a **CLI**
over the same code, on the compiler's own `com.sun.source` trees. No second AST, lexer, parser,
Maven dependency, JNI or native code is added.

| Pillar | Class | Output |
| --- | --- | --- |
| Atomize | `M3Atomizer`, `M3AtomTyping`, `M3AtomFacts`, `M3Atom` | `FILES.tsv`, `ATOMS.tsv` (typed, content-addressed atoms) |
| Patternize | `M3PatternRegistry`, `M3PatternPlan` | `PATTERNS.tsv`, `PATTERN_PLAN.tsv`, append-only `PATTERN_REGISTRY.tsv` |
| Refactor | `M3RefactorGate`, `M3MopChain` | gate decisions, `CHAIN.tsv`, candidate `M3RecipeMemory` rows |
| Receipts | `M3MopReceipt`, `M3AtomizeRun` | `RECEIPT.jsonl` (`M3-MOP-RECEIPT/1`), `MANIFEST.sha256`, `SUMMARY.txt` |
| Entry points | `M3MopPlugin` (`-Xplugin:M3Mop`), `M3Mop` (CLI) | the same files from both |

## Atoms

One atom per TYPE header (modifiers through `{`), METHOD (including constructors) and FIELD
(a comma group is one atom; record components belong to the TYPE header). Columns:
`atomId path owner kind name exposed startLine endLine startChar endChar inputs output effects
failure limits anchors exactSha256 shapeKey contractSha256`.

- **Address**: path, char and line range, `exactSha256` of the exact span.
- **Typing**: `inputs` (parameters), `output` (return or field type), `effects`
  (`ALLOCATES ASSIGNS CALLS IO MUTATES SYNC THROWS` or `PURE`, syntax evidence only),
  `failure` (declared throws plus `THROW`, or `NONE`).
- **Limits**: `METHOD_15` (body > 15 lines), `LOOP`, `CONST_HOIST` (a non-trivial literal repeated
  in one atom).
- **Contract**: modifiers, annotations, type parameters, parameter and return types, throws and
  default value of a method; modifiers, type and exposed `static final` primitive/String
  initializer of a field; kind, modifiers, supertypes, permits, record components and enum
  constant order of a type. Every text is the exact source span of a javac subtree.
- **Exposed**: public or protected, or implicitly public in an interface, inside a chain of
  accessible types; enum constants are exposed.

### One AST, Plugin == CLI

The Plugin takes the projection when javac's PARSE event finishes, before attribution rewrites
trees (Attr inserts implicit `super()` calls and Annotate rewrites `@A(x)` to `@A(value = x)`), and
publishes a unit only after its ANALYZE event. The CLI parses with a parse-only `JavacTask`. Both
call `M3Atomizer.atomize`, so their `FILES/ATOMS/PATTERNS/MANIFEST` outputs are byte-identical
(contract `M3AtomizeContract`, verify step "dogfood plugin output == CLI output").

## Patterns (`M3-PATTERN/1`)

The shape key is the pre-order list of javac tree kinds of an atom (names and literal values are
not part of the shape). The pattern id is `M3P1-` plus 16 hex of the domain-separated shape key.

| State | Meaning | Plan action |
| --- | --- | --- |
| `KNOWN_PATTERN` | the exact atom bytes recur in the run, or are a registered exemplar | `REUSE_EXISTING_RECIPE` |
| `UNPROVEN_LEAF` | the shape recurs (run or registry) with other bytes; a gate proof is still needed | `PROVE_FILE_GATE` |
| `UNKNOWN_PATTERN` | no recurrence | `KEEP_LEAF_OR_MINT_PATTERN` |

`PATTERN_REGISTRY.tsv` (`schema patternId version kind shapeKey exemplarSha256 exemplar state`) is
append-only and versioned: each new exemplar of a pattern id gets the next version; reading refuses
a forged shape key, non-contiguous versions, a registered `UNKNOWN_PATTERN`, duplicates, CR and blank
lines; every append is checked with `M3RecipeMemory.verifyAppendOnly`. The checked-in registry holds
this module's own exemplars; verify requires it to be at a fixed point (re-registering appends 0).
The plan groups (pattern, state), assigns the smallest scope (FILE, PACKAGE, MODULE, PROJECT),
refuses input that no longer matches `MANIFEST.sha256` (`PATTERN_INPUT_DRIFT`) and never grants
promotion authority.

## Refactor gate

Per before/after pair, in order: `REFUSED_DRIFT` (before differs from the atomize sha),
`REFUSED_ANCHOR_LOSS` (a `//FLLDCIM:` anchor line is missing), `REFUSED_PARSE`, `REFUSED_API_DRIFT`
(an exposed contract removed, narrowed, changed or newly exposed), `REFUSED_LIMIT_RATCHET` (any of
METHOD_15, LOOP, CONST_HOIST grows, counted per kind), then the fixed point: the recipe applied to
`after` must give `after` (`REFUSED_NOT_FIXED_POINT`). Without that second application the verdict
is `HOLD_FIXED_POINT_NOT_RUN`, never `ADMITTED`.

Every refusal appends one row to an **`M3RecipeMemory` candidate ledger**
(`*_CANDIDATES.tsv`, default `RECIPE_MEMORY_CANDIDATES.tsv` in the run output) through
`M3RecipeMemory.appendCandidate`: same eight-column schema, lock, atomic rename and byte-prefix proof
as the proven memory, but it can never write `RECIPE_MEMORY.tsv`, and a rerun over the same refusal
appends nothing. Refusals are unqualified candidates, not proven improvements
([RECIPE_MEMORY_RUNTIME.md](RECIPE_MEMORY_RUNTIME.md)).

## Chain

`chain` = atomize (the drift baseline) → pattern plan → gate every proposed file (a recipe's output
mirrored under `--proposal`, its second application under `--again`) → promote all or nothing, only
with `--write`, after re-reading every target → re-atomize and require that exactly the promoted
files changed (none in a dry run) and that a second pass changes nothing. The recipe engine stays
external (OpenRewrite, JDT or any tool); the chain is its admission harness.

## Commands

```sh
CP=<classes containing com.m3.mop and META-INF/services/com.sun.source.util.Plugin>
javac -cp $CP "-Xplugin:M3Mop out=DIR root=DIR [registry=FILE] [register]" ...sources
java -cp $CP com.m3.mop.M3Mop atomize --root=DIR --out=DIR [--registry=FILE] [--register] PATH...
java -cp $CP com.m3.mop.M3Mop plan --atomize=DIR --out=DIR
java -cp $CP com.m3.mop.M3Mop gate --path=P --before=F --after=F [--again=F] [--baseline-sha=HEX] --out=DIR [--recipe=ID]
java -cp $CP com.m3.mop.M3Mop chain --root=DIR --proposal=DIR [--again=DIR] --out=DIR [--write] [--recipe=ID]
java -cp $CP com.m3.mop.M3Mop registry-verify FILE
bash m3/mop/verify.sh          # PASS/FAIL per step; M3_JAVA_HOME selects JDK 21
bash m3/mop/discriminators.sh  # mutation discriminators, PASS = mutant killed
```

Exit status: 0 PASS, 1 REFUSED or FAIL, 2 usage, 3 HOLD. Refresh the registry after a source change
with `atomize --registry=m3/mop/PATTERN_REGISTRY.tsv --register` (rows are only appended).

## Non-claims

Admission is projection evidence (anchors, contracts, limits, parse, fixed point), not runtime
equivalence; effects are syntax evidence without symbol resolution; receipts carry no wall clock.
Lineage: [name-mapping fragment](../docs/name-mapping.d/m3-mop-pillars-20261030.json) and
[FOSS_REUSE_DECISION.tsv](FOSS_REUSE_DECISION.tsv).
