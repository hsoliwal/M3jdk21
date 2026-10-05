# A3 recipe mastery laboratory

Status: M3JDK21 tooling proof. No JDK product mutation authority.

## Purpose

A3 does not treat a recipe as mature because it works on one clean example. The recipe is trained
offline against generated hostile and ambiguous source shapes, then accepted only through
mechanical compiler/JUnit/fixed-point evidence.

The initial mastered semantic family is the existing private-static pure-int atom:

```text
inventory -> atomize -> patternize / IOP -> document -> fixed point
```

The lab improves confidence in that reusable recipe. It does not edit JDK product files.

## Generated in-memory project

The corpus contains 28 Java compilation units:

- 24 eligible pure-int fixtures;
- four deliberate rejection fixtures.

Eligible fixtures cross six arithmetic expression shapes with four lexical-decoy shapes.

Decoys include:

- comments containing fake Java declarations;
- strings containing fake methods;
- text blocks containing Java-looking source;
- regexes matching Java-looking source;
- fake M3 marker-like text;
- Unicode text including a supplementary emoji.

The recipe must act on the Java LST/AST only. Code-looking text is never semantic authority.

## Multipass permutation/combinations

The three source-changing passes are:

1. atomize;
2. patternize;
3. document.

The laboratory executes all six pass order permutations repeatedly to a bounded fixed point.

It also executes every non-empty subset of the three passes.

For full permutations, the required fan-in is:

- same canonical source as the trusted convergence DAG;
- same runtime behavior;
- Java 21 compiler success;
- second-run fixed point.

Subset schedules are required to remain compiler-safe, behavior-preserving and boundedly stable even
when they intentionally do not produce the complete canonical postimage.

## Compiler and runtime oracle

Every generated project is compiled in memory with the platform Java compiler:

```text
--release 21 -Xlint:all -Werror -proc:none
```

No successful OpenRewrite transformation is accepted merely because it parses.

The compiled bytecode is loaded from memory and executed on deterministic argument vectors covering:

- zero;
- positive and negative values;
- integer extrema;
- dense bit patterns.

Before/after results must be identical.

A deliberate broken-candidate mutant calls a missing method. The mastery test requires the compiler
to reject it and surface the compiler diagnostics. This permanently proves that the compiler, not an
LLM judgment, is the acceptance authority.

## Regex/string precompute signal

Each eligible fixture also evaluates regexes against code-looking strings/text blocks.

The aggregate match signal is frozen before transformation and must remain unchanged under every
pass permutation.

This first signal is intentionally small. Future recipe families may add richer precomputed
structural/regex/search signals, but those signals nominate optimization opportunities only.
AST/LST facts plus compiler/JUnit behavior remain authoritative.

## Recipe-first custody

The laboratory itself is owned by:

`com.m3.rewrite.M3A3RecipeMasteryLabRecipe`

Named recipe:

`com.m3.rewrite.M3A3RecipeMasteryLab`

Maven profile:

`m3-a3-recipe-mastery-lab`

The installer owns exactly two test sources, refuses drift, generates reviewed resources when
absent, and reaches a second-run fixed point.

## Expansion rule

Every new recurring atomizer/patternizer family should add:

1. generated ordinary fixtures;
2. hostile lexical decoys;
3. semantic near-misses;
4. bounded pass-order permutations;
5. bounded pass subsets where meaningful;
6. compiler diagnostics;
7. immutable-baseline runtime/JUnit parity;
8. permanent counterexamples;
9. second-run fixed point.

This is where LLM effort belongs: offline corpus generation, static precompute and signal discovery.
Promotion remains mechanical.
