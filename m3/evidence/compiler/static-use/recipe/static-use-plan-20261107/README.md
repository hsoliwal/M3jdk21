<!-- SPDX-License-Identifier: Apache-2.0 -->
# Recipe static-use-plan-20261107 (RELOCATE_STATIC_USE_PLAN v1)

Census pass 2, leaf **P2-133** (crate `m3-static-use-plan-recovery-20261014`: `M3StaticUsePlan`, `M3StaticUsePlanTest`)
of the first-party donor `hsoliwal/com.synexia` (develop `d5ad5090d87e`), relocated into a new module
`m3/compiler/static-use` (`com.m3:m3-compiler-static-use`, module `org.m3jdk.compiler.staticuse`, exported package
`jdk.m3.compiler.staticuse`), recipe-first and byte-for-byte.

| Owner | Contract |
|---|---|
| `M3StaticUsePlan.evaluate` | compiler-proven facts -> `DIRECT_PARENT` / `LAZY_HANDLE` / `MATERIALIZE` / `REFUSE`; unproven ordering or owner lifetime and unknown escape or effect are refused; observable Java references and published escape materialize |
| `M3StaticUsePlan.referenceMode` | `UNKNOWN`, `KEEP_JVM_REACHABILITY`, `OWNER_EDGE_COUNT_REQUIRED` (shared backing) or `NO_WRAPPER_COUNT`; never frees a Java object |
| `M3StaticUsePlan.IntLoop` | condition-only loop entry bound of an int induction; refuses a step that does not approach the exit and an int wraparound at the condition exit |
| `@Analyze` / `@CompilerSummary` | SOURCE-retention opt-in request; CLASS-retention companion summary (analyzer, source SHA-256, method key, loop bounds, dynamic inputs, unresolved) |

## Decisions ([DECISION.tsv](DECISION.tsv))

* The consumer is the donor's javac annotation processor `M3StaticFactsProcessor`, so the plan joins the `m3/compiler`
  family as its own module, a default module of `m3/pom.xml` (S1). The processor is the next leaf (OMIT row, S3).
* Naming follows the `m3/compiler` siblings with a descriptor (S7, verify.sh gate `siblings`): module
  `org.m3jdk.compiler.<x>`, package `jdk.m3.compiler.<x>`, coordinates `com.m3:m3-compiler-<dir>`, exactly as
  `lifetime-slots` (`org.m3jdk.compiler.lifetime`, `com.m3:m3-compiler-lifetime-slots`) and `nullness-facts`.
* `M3GcUsePlan` (`m3/collections/gc-interop`) is the nearest shape, with different facts and modes: NEC, both kept (S2).
* Lineage: the develop-live 155-line class is the head; the census named the 83-line recovery postimage (S5).
* FOSS ([FOSS_REUSE_DECISION.tsv](FOSS_REUSE_DECISION.tsv)): C2 escape and counted-loop analysis are architectural
  references only; Error Prone / Checker Framework do not fit; no third-party expression.

## Oracles

* Donor `M3StaticUsePlanTest` (5 JUnit 5 tests) in the test phase; the same test compiled from the donor tree runs
  against the donor class through the JUnit console launcher.
* `M3StaticUsePlanDump` (test scope, new, with the JUnit wrapper `M3StaticUsePlanDumpTest`): the whole fact space
  (18,432 facts, both reference modes) against a reference model restated from the contract, 208,112 loop shapes
  (8,112 edge shapes + 200,000 seeded) against a loop simulation or a BigInteger formula with wraparound refusal,
  64 growth-sampling pairs and 3 refusals, and the annotation retention / targets; 385,986 checks.
* Differential ([differential.sh](differential.sh)): donor and relocated dumps byte-identical (default and `-Xint`),
  three one-line mutants refused by the reference model, `javap -p` of 11 classes identical.

## Leaf 2: the javac processor (2026-11-07)

`M3StaticFactsProcessor` (the former OMIT row) joined the module by the same recipe: the opt-in annotation processor
(analyzer `m3-static-facts-v2`) over `@Analyze` methods, which reads real javac trees, admits only literal-start
constant-step `int` for-loops bounded by a literal, an unmutated `int` parameter or an unmutated array parameter's
length (everything else is `UNKNOWN`; escape, runtime counts and callee effects stay `UNPROVEN`), and writes one
`@CompilerSummary` companion per method plus `META-INF/m3/compiler-facts.tsv`. It consumes `M3StaticUsePlan` only
(DECISION.tsv S8). `java.compiler` / `jdk.compiler` are `requires static` (compile time only, S9); there is no service
registration (opt-in, as in the donor).

* Donor oracle `CompilerOwnershipProof` (test scope): 105,991 assertions, 52 javac compilations through the processor.
* `M3StaticFactsProcessorOracle` (test scope, new): one fixture corpus (53 companions, 2 ledgers, 3 diagnostics) through
  the donor and the relocated processor; byte-identical after the declared masking (S10).
* Four processor mutants (single-level parentheses, loop budget 129, mutated parameter bound admitted, stripped source
  hash) are refused by the relocated proof.

## Run

```sh
python -I m3/compiler/static-use/recipe/static-use-plan-20261107/relocate.py --donor-git <com.synexia clone> \
    --donor-rev d5ad5090d87ef8477a56317e60ac0d29c65029f7 [--check]
JAVA_HOME=<JDK 21> bash m3/compiler/static-use/recipe/static-use-plan-20261107/verify.sh <com.synexia clone> \
    d5ad5090d87ef8477a56317e60ac0d29c65029f7 <scratch> <junit-platform-console-standalone jar> [repo [tail]]
```

Gate results: [GATES.tsv](GATES.tsv); lessons: [RECIPE_MEMORY.tsv](RECIPE_MEMORY.tsv).
