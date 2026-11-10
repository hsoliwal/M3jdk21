# A3Lab — compiler-driven Atomize/Patternize mastery

Status: M3JDK21 tool-plane qualification contract.

## Purpose

A3Lab strengthens A3 = Atomize -> Patternize -> Absorb by mastering the retained FILE-local
OpenRewrite atoms against hostile, generated Java projects before any JDK product source is touched.

The laboratory exists because recipe correctness is learned from bounded mechanical counterexamples,
not from repeatedly hand-editing target files.

## Invariant

For every admitted FILE-local source-changing recipe task:

```text
inventory
  -> immutable fixture/static-signal precompute
  -> bounded recipe schedule enumeration
  -> actual Atomize / Patternize recipe execution
  -> Java 21 parse/print
  -> javac -Xlint:all -Werror after every changing pass
  -> public/protected contract observation
  -> runtime behavior observation
  -> fixed-point replay
  -> equal-subset order convergence
  -> permanent counterexample receipt
  -> only then later A3/JEP absorption admission
```

Signals, regexes, donor evidence and LLM proposals can nominate work. They are never semantic
authority. javac/runtime/JUnit and later OpenJDK configure/make/jtreg remain the product oracles.

## Fixture spectrum

The first bounded corpus combines:

- the same pure-int return shape already admitted by A3;
- ordinary String data containing Java-looking code;
- comments containing fake declarations and control flow;
- text blocks containing Java-looking code;
- regex strings containing braces, groups, quantifiers and source-looking tokens;
- direct caller shape;
- Java 21 local-record + lambda + switch-expression caller shape;
- local-class/method-reference syntax;
- six deterministic member orders;
- LF and CRLF line endings.

The Cartesian fixture axis is therefore 48 deterministic source files:
2 lexical hostility modes x 2 line endings x 2 caller shapes x 6 member orders.

Code-looking text must remain data.

## Schedules

For the two retained source-changing atoms:

- Atomize
- Patternize

the laboratory executes every non-empty ordered subset:

```text
A
P
A -> P
P -> A
A -> P -> A
P -> A -> P
```

The repeated schedules deliberately revisit an already-achieved operation. Their final source must
equal the corresponding A+P normal form; an already-completed Atomize or Patternize step is not
allowed to create new work on replay.

Each schedule is cycled from the original fixture until stable or until the bounded pass budget is
exhausted. Equal operation sets must converge to the same rendered normal form regardless of order
or redundant replay. The bounded campaign produces 48 x 6 = 288 result rows.

## Static precompute

Before recipe execution each fixture receives immutable read-only signals:

- SHA-256;
- UTF-16 source length;
- line-ending kind;
- counts for selected code-looking String/comment/text-block/regex markers;
- lambda, switch-expression and local-record shape counts.

The signals are regression discriminators and optimization hints only.

## Regex matrix and in-memory compiler

The compiler/runtime fan-in is fully in memory. `A3MemoryCompiler` uses the platform Java compiler
with:

```text
--release 21 -Xlint:all -Werror -proc:none
```

Generated class bytes, including local/nested classes, are retained in an in-memory file manager and
loaded through a bounded class loader. A3Lab therefore no longer needs temporary source/class files
for each pass application.

`A3RegexMatrix` precompiles a bounded representative regex spectrum once per tooling JVM. The first
matrix contains 16 categories:

- literal matching;
- Java method-shape whitespace;
- keyword alternation;
- quantifiers;
- identifier/call character classes;
- multiline visibility anchors;
- DOTALL method-call shapes;
- positive lookahead;
- fixed-width lookbehind;
- backreferences;
- Unicode letter classes;
- flat brace regions;
- line comments;
- block comments;
- line-break matching;
- quoted M3 atom names.

Each observation evaluates those patterns against 16 fixed subjects plus the fixture's actual
code-looking payload: 16 x 17 = 272 deterministic cells.

For every match the matrix root binds the pattern ID, source, flags, precomputed literal-prefix and
ASCII-presence signals, subject bytes, and exact `find()` start/end coordinates. The precomputed
prefix/mask lanes are optimization hints only. They never certify a rewrite.

Every A3Lab result row now carries `regexMatrixStable=true`. Any matrix-root drift rejects the
schedule alongside compiler, public/protected contract, runtime behavior, lexical-data or fixed-point
drift.

## Counterexamples

Any parser losslessness failure, compile failure, warning, behavior drift, contract drift, cycle,
pass-budget exhaustion or subset non-confluence is retained as a deterministic result row. Later
recipe improvements must replay the same corpus.

## JDK boundary

A3Lab is M3 tooling, not a JDK runtime API.

It must never write `src/` or `test/`. Persisted evidence is restricted to `m3/build/a3/lab/**`.

OpenJDK's build stays authoritative:

```text
configure -> make -> jtreg -> runtime/benchmark
```

A3Lab only raises the quality of the reusable recipe before absorption.
