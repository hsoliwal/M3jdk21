# M3 Java 21 multi-pass OpenRewrite convergence law

Status: execution law for M3JDK21 FILE-local mechanical convergence.

## Canonical entry point

`com.m3.rewrite.M3Java21ConvergenceRecipe` is the single trusted Java 21 FILE-local
OpenRewrite entry point.

The current admitted pass DAG is:

```text
PASS 0  INVENTORY
    -> discover leaves accepted by the same eligibility oracle used by mutation

PASS 1  ATOMIZE
    -> extract an admitted behavioral leaf without changing the FILE contract

PASS 2  PATTERNIZE / IOP
    -> attach the admitted semantic role to the atomized leaf

PASS 3  DOCUMENT
    -> converge Javadoc/semantic memory with the atom and IOP role

PASS 4  FIXED POINT
    -> repeat the ordered OpenRewrite cycle until no source changes remain

PASS 5  PROOF
    -> diff -> compile(Java 21) -> JUnit/OpenRewrite tests -> JaCoCo

PASS 6  PROMOTION
    -> only a proven candidate may be serially promoted
```

## Multi-pass rule

A pass is added only when it has:

1. a mechanically bounded semantic domain;
2. shared discovery/mutation eligibility;
3. an explicit edit scope;
4. behavior/contract-preserving proof for FILE authority;
5. positive and negative JUnit fixtures;
6. idempotence/fixed-point proof;
7. Java 21 compile proof when source is produced;
8. pattern/IOP and documentation convergence where applicable.

The top-level recipe is intentionally additive. New atom/pattern families become new pass children
only after their bounded recipe tests are green.

## Scale rule

OpenRewrite may evaluate independent FILE candidates in parallel. Canonical truth does not advance in
parallel. Scope promotion is explicit:

`FILE -> VISIBILITY -> PACKAGE -> MODULE -> MULTI_MODULE -> LIBRARY`.

If a recipe requires a broader boundary, it is not admitted into the FILE convergence lane until a
separate broader-scope proof exists.

## Single-build rule

The Maven proof command is:

```bash
mvn -B -ntp -f m3/tooling/migration-recipes/pom.xml clean verify
```

That one build compiles the Java 21 recipe implementation, executes OpenRewrite/JUnit proof fixtures,
checks convergence/fixed-point behavior, and enforces configured JaCoCo thresholds.

Repository-wide application remains blocked while this build is red.
