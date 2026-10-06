# Owned automata and reasoning precompute in M3JDK

Status: internal implementation mapping.

## Authority chain

```text
external algorithm/reference projects
        |
        v
Synexia-owned implementation
        |
        v
M3JDK internal mechanical counterpart
```

M3JDK does **not** port directly from external donor APIs.

Current source pin:

`hsoliwal/com.synexia@m3/precompute-regex-lucene-reasoning-20261006`
reviewed at commit `8a235ae016e74992e7b80351c5ad8182c5ee7aa8`.

## Name mapping

| Synexia source | M3JDK target |
| --- | --- |
| `MIndexRegexAutomaton` | `jdk.internal.mindex.M3RegexAutomaton` |
| `MIndexTermFst` | `jdk.internal.mindex.M3TermFst` |
| `MIndexReasoningProgram` | `jdk.internal.mindex.M3ReasoningProgram` |
| `MIndexReasoningClosure` | `jdk.internal.mindex.M3ReasoningClosure` |

These are internal implementation types. They are not public `java.lang.String` APIs.

## External lineage

- RE2/J: finite-regex/NFA mechanics reference only.
- Apache Lucene: minimized term automaton/FST mechanics reference only.
- TweetyProject: reasoning categories and behavioral comparison only.

No RE2/J, Lucene or Tweety runtime dependency is present in `java.base`, and none of their
types may appear in M3 runtime signatures.

## M3RegexAutomaton

The target keeps the bounded finite UTF-16 subset and fail-closed behavior:

- Thompson-style NFA construction;
- deterministic range automaton;
- bounded NFA/DFA states;
- exact whole-input match;
- bounded-state substring find;
- prefix-to-accept reachability;
- no backreferences;
- no unsupported counted-quantifier semantics.

This is internal candidate/precompute machinery. A future JDK consumer may use negative facts only
where exact JDK regex semantics for the admitted subset have been independently proven.

## M3TermFst

The target keeps:

- deterministic sorted term ingestion;
- bottom-up equivalent-suffix state folding;
- primitive immutable transition/state arrays;
- exact term lookup;
- prefix existence;
- longest-prefix output.

This is an M3JDK image, not Lucene serialization.

## M3 reasoning language

The target keeps the Synexia-owned finite language:

```text
fact alpha
fact !blocked
rule alpha & !blocked -> ready
support alpha -> ready
attack blocked -> ready
```

The M3 implementation owns its parser, canonical compilation and deterministic least fixed point.
Positive and negative facts can coexist, producing explicit inconsistency rather than silently
discarding a derivation. Proof-rule provenance and support/attack facts are retained internally.

## Invariant

All of these structures are derived internal precompute. They may accelerate or prepare later
operations, but they do not own canonical M3 String spelling, do not alter semantics when absent,
and do not introduce external runtime dependencies.
