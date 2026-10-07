# M3JDK full precompute — second-pass port

Status: isolated Apache-2.0 target port. This module is **not** a `java.lang.String`
replacement and is not on the bootstrap path. It is the M3JDK-owned landing zone for
broader static precompute that must not bloat `M3String` or leak Synexia runtime
packages into the JDK.

## Ownership split

Existing JDK runtime owners remain authoritative:

- `java.lang.M3StringFacts`: fixed-size String-semantic geometry and safe filters;
- `java.lang.M3StringSearchPrecompute`: bounded exact-search preparation;
- `java.lang.M3StringPositionPrecompute`: bounded exact position masks;
- `jdk.internal.mindex.M3TQ`: exact trigram membership and candidate-only regex absence gates.

This port owns the broader second-pass fabric:

- `M3TextMetrics`: composable UTF-16/UTF-8/code-point/Java-hash geometry;
- `M3TextSignals`: character, bigram and trigram histograms, safe edit lower bounds,
  SimHash and MinHash/Jaccard sketches;
- `M3RegexShape`: fail-closed literal/prefix/suffix/exact regex recognition;
- `M3EditDistancePlan`: exact bounded Levenshtein with single-word Myers for short
  patterns plus reusable DP/banded workspaces;
- `M3CodeTextSignals`: code-looking and regex-looking signal image for fixture
  generation and candidate routing;
- `M3SimilarityPrecompute`: pairwise safe/approximate signal packet;
- Java and optional JNI batch providers for lexical signals and cheap SimHash/length ranking.

Approximate signals **never** prove equality, edit thresholds, Java syntax or regex
matches. Exact String/regex/compiler/test behavior remains authority.

## Second-pass improvements over Synexia

M3JDK does not merely prefix donor classes:

1. target names are M3-owned rather than donor ABI;
2. trigram multiset facts strengthen the safe edit-distance lower bound;
3. fixed boundary bigrams make seam trigram composition exact without rereading text;
4. short patterns use a precomputed Myers bit-vector verifier;
5. DP/banded work has an explicit cell budget, not merely a length-delta bound;
6. approximate ranking and exact verification have separate types and contracts;
7. progress/cancellation is target-owned and dependency-free;
8. JNI is optional and parity-tested against the Java reference;
9. large posting trees, semantic graphs, dictionary images and fuzzy indexes stay
   outside `java.lang.String`;
10. empty/zero-width regex shapes fail closed instead of taking a literal shortcut.

## Build

Java proof:

```sh
mvn -B -ntp -f m3/ports/precompute/pom.xml test
```

Java + native parity:

```sh
mvn -B -ntp -f m3/ports/precompute/pom.xml -Pnative test
```

The native profile requires CMake, a C11 compiler and JNI headers. Without that
profile, the JNI parity test is skipped by assumption rather than reported as a pass.

## Promotion

Individual atoms may later graduate into `java.base` or another JDK module only when
a concrete consumer exists and differential/API/memory/performance gates justify the
retention cost. This module is not permission to add all precompute state to every
String.


## Regex/String trial V2

The V2 port closes the later Synexia recipe-mastery delta without importing another native ABI.

Target-owned additions:

- `M3RegexStringSignals`: immutable static regex/String feature packet. Plain literals retain exact
  `indexOf`/full-equality facts and are checked against JDK `Pattern`; all other regex features are
  syntax observations only.
- `M3RegexStringTrialImage`: bounded regex × String matrix using JDK `Pattern` for exact
  `matches`, `lookingAt` and `find`; M3 code-text, SimHash and MinHash/Jaccard signals rank
  difficult cases for later compiler/JUnit/recipe work.
- stable global/per-regex top-N ranking and content-addressed image identity.
- optional JNI acceleration reuses the existing `M3CodeTextSignalBatchNative`. Synexia's separate
  recipe-mastery regex-pair native ABI is deliberately not ported.

This preserves the M3JDK rule that native acceleration must minimize ABI surface and remain
differentially equivalent to the Java signal owner. JNI does not execute regex semantics here.

Recipe crate:

```text
m3-regex-string-precompute-v2
com.m3.M3RegexStringPrecomputeSuperset
```

Focused proof:

```bash
python3 m3/ports/precompute/verify_regex_string_v2.py
mvn -B -ntp -f m3/ports/precompute/pom.xml \
  -Dtest=M3RegexStringSignalsTest,M3RegexStringTrialImageTest test
mvn -B -ntp -f m3/ports/precompute/pom.xml -Pnative \
  -Dtest=M3RegexStringTrialImageTest,M3NativeParityTest test
mvn -B -ntp -f m3/tooling/migration-recipes/pom.xml \
  -Dtest=M3RegexStringPrecomputeSupersetRecipeTest test
```

This is still an isolated port. It does not alter `java.lang.String`, `java.util.regex.Pattern`,
collections, bootstrap behavior or public JDK APIs.
