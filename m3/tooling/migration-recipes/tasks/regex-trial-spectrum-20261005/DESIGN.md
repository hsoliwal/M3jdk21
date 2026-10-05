# M3JDK21 regex/source trial spectrum

## Purpose

This task extends the existing Java-21 Atomize -> Patternize qualification plane with a bounded
regex x source precompute campaign.

The change is tool-plane only. It does not modify OpenJDK product source, HotSpot, JNI ABI,
`configure`, `make`, jtreg, public JDK API, JEP disposition, or backport authority.

OpenJDK's native build remains the product oracle.

## Why

The current atom/pattern spectrum already proves:

- all six orders of the three convergence leaves;
- 324 generated pure-int expressions;
- 225 arithmetic boundary pairs;
- compilation after every intermediate;
- frozen and independent behavior oracles;
- exact public/member surface preservation;
- code-looking String/text-block controls;
- source fixed point.

The next reusable signal is a deterministic finite regex x string trial image. It should identify
difficult lexical combinations before spending compiler/runtime work, while never treating regex
similarity as Java semantic proof.

## Trial image

The test-plane image has two immutable axes:

1. a fixed regex catalogue containing literals, anchors, flags, Unicode, groups, lookaround,
   backreferences, quantifiers and deliberately invalid expressions;
2. a sorted string catalogue containing the generated Java sources plus explicit code-looking and
   regex-looking decoys.

For every pair it precomputes:

- JDK regex validity and syntax-error index;
- `matches`, `lookingAt`, and `find` truth bits;
- regex lexical complexity;
- source lexical complexity;
- a bounded 64-bit character-presence overlap signal;
- deterministic difficulty.

The complete image is SHA-256 framed.

## Hard-pair fan-out / fan-in

The highest-difficulty pairs are selected with a stable ordering:

```text
difficulty descending
pair ordinal ascending
```

Selected pairs are then fanned out to bounded worker threads.

Each worker generates a Java-21 in-memory probe with Base64-framed regex/string bytes and executes:

```text
MemJava javac --release 21 -Xlint:all -Werror
  -> java.util.regex.Pattern
  -> matches / lookingAt / find
  -> expected truth / syntax-index assertion
```

Workers never decide acceptance. Results are joined in canonical pair order and hashed into one
campaign root. A serial replay must produce the same root.

JUnit is the fan-in authority for this tool-plane trial.

## Relationship to Atomize / Patternize

The trial spectrum does not mutate source.

Its purpose is to improve the offline recipe laboratory:

```text
generate source spectrum
  -> precompute regex/source difficulty
  -> select difficult rows
  -> compiler/runtime qualification
  -> run actual Atomize/Patternize permutations
  -> compiler/runtime/surface proof after every intermediate
  -> exact source fixed point
```

Regex facts may nominate stress cases. They cannot authorize a rewrite.

## Recipe-first custody

The additive JUnit campaign is installed by a Maven/OpenRewrite recipe based on the repository's
existing `M3HashPinnedJavaSnapshotRecipe`.

The named recipe is:

`com.m3.rewrite.JdkRegexTrialSpectrum`

The exact test postimage is SHA-256 pinned. The recipe test must prove:

- exact additive generation;
- drift refusal for an occupied target;
- second-run fixed point.

## Completion boundary

Passing this campaign does not mean every JEP or later-JDK change has been absorbed.

The existing JEP/JBS/community catalogue, A3 plan, scope escalation, source-sealed backport packets,
`configure -> make -> jtreg -> runtime` verification, and serial promotion remain authoritative.
