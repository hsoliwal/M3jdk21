# A3 V6 mastery admission

## Purpose

A3 already performs JDK-local Atomize -> Patternize -> Absorb preparation with Java 21 parsing,
in-memory compilation, behavior/contract probes, fixed-point replay, regex payload checks and
candidate-only writes under `m3/build`.

This gate adds cross-repository custody for the canonical Synexia M3 V6 recipe-mastery receipt. It
does not replace the A3 laboratory and does not make Maven the OpenJDK product build.

## Boundary

The JDK product oracle remains:

```text
configure -> make -> jtreg -> runtime
```

A3 is authoring/proof tooling only.

## V6 handoff

A3 accepts the strict `M3_RECIPE_MASTERY_FANIN_V6` TSV produced by Synexia.

The receipt binds, among other evidence:

- Atomize/Patternize compiler/behavior convergence;
- recipe schedule coverage;
- permanent counterexample replay;
- LeetCode/HackerRank/GeeksforGeeks donor review and serial pass ledger;
- the 10,000-case regex/string mass matrix;
- the JDK regex oracle root;
- optional Java/JNI signal parity.

A3 does not trust the file merely because it parses. The caller must provide the expected receipt
root from the reviewed work order. A3 recomputes the V6 framed SHA-256 root and requires exact
equality with both the TSV root and the caller-pinned root.

## Additive mastered apply

The existing `A3Apply.run(...)` remains unchanged.

`A3Gate.apply(...)` is the stronger additive lane:

```text
V6 receipt + caller-pinned root
        -> A3Gate admission
        -> unchanged A3Apply FILE candidate
        -> m3/build-only candidate + A3 receipt
```

The gate writes no OpenJDK `src/` or `test/` file.

## Fail-closed conditions

Admission rejects:

- unknown/missing/duplicate TSV keys;
- schema other than V6;
- malformed SHA-256 fields;
- receipt-root mismatch;
- caller-pinned root mismatch;
- `complete != true`;
- any source/semantic/donor-copy/replacement/merge/promotion authority;
- regex case count other than 10,000;
- fewer than three serial challenge-donor ledger rows;
- invalid JNI evidence presence/absence.

The JDK gate intentionally does not reimplement Synexia's mastery algorithms. It verifies the
portable evidence identity and then relies on its own A3/JDK gates.

## Recipe-first delivery

The source-changing delivery is itself a Maven/OpenRewrite recipe:

`com.m3.a3.MasteryV6`

It is create-only and owns the A3Gate implementation and focused JUnit proof. Existing foreign files
at those paths fail closed.

No java.base, HotSpot, native runtime or public JDK API is changed by this tranche.
