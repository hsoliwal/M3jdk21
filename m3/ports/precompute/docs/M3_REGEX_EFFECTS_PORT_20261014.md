# M3JDK21 compositional DFA effect receiver — candidate 2026-10-14

Status: **isolated Maven/JUnit proof; NOT java.base/HotSpot/Pattern promotion**.

## Canonical ownership

Source: `hsoliwal/com.synexia@8200bd6a92b5e4f81522c177ed0e668bf03664aa`, `synexia-indexstring/src/main/java/com/synexia/indexstring/M3RegexDp.java` (Git blob `f4254d2359697ae3abdae5b811001e976818084f`). Apache-2.0, first-party copyright notice preserved.

Generated receiving package: `com.m3.precompute.M3RegexDfaEffects`, within the **existing** independent Apache-2.0 `m3/ports/precompute` module. This is a target-owned name/structure, not the source donor package and not another string payload. Canonical reusable recipe, source seals and whole-spectrum fixtures remain in Synexia's `synexia-openrewrite-recipes`. Source and target snapshots are linked by exact Git blob and SHA-256.

## What migrated

A pure primitive-array automaton effect engine, without a regex parser or regex syntax reinterpretation:

- immutable `Dfa` input contract; bounded `Limits`, local `Plan`, `Effect`, `Index`, `IndexLimits`;
- `summarize` converts any accepted immutable fragment to its per-incoming-state transition effect; `compose` and `composeAll` preserve source order and don't reread text;
- checked binary-lifting `repeat` and immutable index `range`/snapshot `replace` retain existing state/memory/cancellation bounds;
- cold `tryMatch` performs one-state DFA scanning without preparing all-entry effects;
- read-only Java API remains **package-private**. No public JDK signature, new cache, JNI symbol, native handle or `String` field.

The existing donor's exact Java proofs were rehomed and renamed with an explicit no-native main boundary; JUnit invokes both whole-match/fragment-composition and indexed-range/immutable-snapshot oracles. Java proofs compare DFA behavior with independent scalar transitions and a bounded set of JDK `Pattern.matches` fixtures. JNI providers require **separate** graduation.

## What remains donor-only

`M3RegexDpBridge` depends on `MIndexRegexProgram`/composite-span internals and cannot be mechanically moved without importing a parallel compiler. `M3RegexDpNative` is a test/candidate ABI, not a proven M3JDK native dispatch. Existing `M3PatternQuery`, `M3TQ`, `M3RegexShape`, `M3RegexOracleHarness`, `M3RegexStringSignals` and `M3RegexStringTrialImage` retain their separate target responsibilities. General captures, backreferences, lookbehind, Unicode and `Matcher.find`/region/replace/split stay with the JDK implementation.

## Required admission gates

Maven/JUnit: `mvn -B -ntp -f m3/ports/precompute/pom.xml -Dtest=M3RegexDfaEffectsPortTest test`.

Canonical Synexia recipe: `python3 synexia-openrewrite-recipes/recipes/m3-regex-effects-port-20261014/verify.py`. It checks exact file seals, fixed point, permutation/refusal, strict Java 21 and independent compiled proofs before target promotion.

Further stage gates **NOT YET PROVED**: target full Maven reactor, actual JDK java.base/HotSpot/jtreg, optional JNI C/JNI header/symbol/GC/lifetime, cold preparation vs warm reuse JMH and process RSS. This port does not warrant a claim that java.util.regex backtracking is eliminated or a new general-purpose runtime engine is installed.

LeetCode 10 and 44, HackerRank regex domains and GeeksforGeeks regex DP are category/adversarial-shape evidence only. No challenge submission/editorial source is copied.
