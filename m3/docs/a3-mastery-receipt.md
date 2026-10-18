# A3M — mastery receipt gate

Status: M3JDK21 tool-plane contract. No OpenJDK product API.

## Purpose

A3M makes the existing recipe-mastery rule mechanically precede A3 absorption.

The active FILE convergence DAG is:

```text
inventory -> Atomize -> Patternize -> Document -> fixed point
```

Before the A3 CLI may apply that DAG to a candidate JDK file, one bounded mastery run must prove the
current tooling owners against the hostile in-memory compiler/runtime/regex corpus.

## Live mastery denominator

The retained 48 deterministic hostile fixtures cross:

- ordinary and code-looking String/comment/text-block/regex data;
- LF and CRLF;
- direct and Java-21 modern caller shapes;
- six member orders.

The schedule set covers every non-empty ordered subset of the three source-changing leaves:

```text
A
P
D

A>P  P>A
A>D  D>A
P>D  D>P

A>P>D  A>D>P
P>A>D  P>D>A
D>A>P  D>P>A
```

The existing `A>P>A` and `P>A>P` stress schedules remain. Total: 17 schedules x 48 fixtures =
816 result rows.

Every changing application is reparsed, compiled with Java 21 `-Xlint:all -Werror`, compared
against public/protected contract and runtime behavior, checked for code-as-data/regex preservation,
and cycled until an unchanged sweep is reached.

## Receipt custody

A3M writes only under `m3/build/a3/mastery/`:

```text
receipt.tsv
pins.tsv
lab/results.tsv
```

The receipt binds:

- the exact lab result table hash;
- fixture/schedule/result counts;
- aggregate recipe applications and compiler invocations;
- changed-result count;
- regex-stable result count;
- one deterministic root over exact source/dependency pins.

Pins include the A3 CLI, fixture/compiler/regex owners, A3M/codec, Atomize/Patternize/Document
recipes, the convergence owner, and the A3/migration-recipes Maven descriptors.

## Apply gate

The A3 CLI `apply` path requires a current A3M receipt.

If a pinned recipe, compiler fixture, regex matrix, convergence owner or dependency descriptor
changes, the receipt becomes stale and absorption fails closed until mastery is rerun.

The mastery root may differ from the candidate root. This allows an isolated fixture checkout to
consume the exact tooling receipt without copying tooling sources into the fixture.

The lower-level programmatic `A3Apply.run(...)` API is retained for compatibility and focused
unit tests. The CLI is the proof-gated absorption front door.

## Authority boundary

A3M grants no:

- OpenJDK source-root write authority;
- API/JEP compatibility authority;
- JNI/native equivalence authority;
- VM/GC/JIT/safepoint authority;
- promotion or merge authority.

After A3 candidate generation, normal scope admission and the native OpenJDK
`configure -> make -> jtreg -> runtime/benchmark` oracle remain mandatory.

## Recipe-first delivery

The Java change is delivered by the named hash-pinned OpenRewrite crate
`com.m3.a3.MasteryReceiptDelivery`.

Workflow/documentation changes are delivered separately by
`com.m3.a3.MasteryReceiptWorkflowDelivery`.

Both must replay to a zero-change second pass and refuse stale preimages.
