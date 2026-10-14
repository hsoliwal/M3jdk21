# M3String restricted dot-star DP JDK21 receiver — 2026-10-14

Status: DRAFT / promotion HOLD. Base: `master@f9b3a0ce84dcdbd0cc75df956def7710ac5d04fd`.

Canonical source authority: Synexia OpenRewrite recipe PR #11281, stacked on the
independent LeetCode-10 contract oracle PR #11274. This receiver contains
only the four reviewed source-sealed postimages.

## Locked surfaces

- `String.java` existing Git blob `80d1953d9198eadc54470aba0731262531321875`.
- String workflow existing Git blob `75b06e079db97696f8a75f06c9993e7d47b4a3d7`.
- Existing `java.lang.String.matches(String)` descriptor, NPE/invalid-pattern order,
  conservative literal path and `Pattern.matches` fallback stay unchanged.
- No new public Java API, M3String field, canonical spelling owner, char[] text copy,
  JNI native symbol, HotSpot flag, JVMTI/GC/CDS change or regex cache.

## New exact internal fast path

`java.lang.M3DotStarRegex.tryFullMatch(M3String, regex)` receives logical
UTF-16 units through `CharSequence.charAt` without flattening canonical
atom/tuple owners. Two-row boolean DP over pre-tokenized ASCII literals, dot
and postfix star has O(text units x tokens) time and O(tokens) workspace.

Return `-1` (unsupported), `0` (no match) or `1` (match).
Unsupported/invalid grammar, surrogate text, >512-unit patterns and >4M DP
cells take the unchanged JDK Pattern route. Java-default dot excludes
LF, CR, U+0085, U+2028 and U+2029. LeetCode #10's DOTALL-like dot
is NOT silently substituted for ordinary Java semantics.

## Source identity

Recipe manifest lives at:
`synexia-openrewrite-recipes/recipes/m3jdk21-dotstar-receiver-20261014/manifest.tsv`.

| Target | Preimage Git blob | Postimage Git blob |
|---|---|---|
| `String.java` | `80d1953d9198eadc54470aba0731262531321875` | `b9452c19e7b2307aa33ddfd5897b9fcbe8b6cfbe` |
| `M3DotStarRegex.java` | ABSENT | `febd5b61a987a0f241340ad07913c40ce9fe429f` |
| `M3StringDotStarDpTest.java` | ABSENT | `86d2b2b679df1b181dca6f11fe17a7e637f46c3e` |
| `mindex-string-backing.yml` | `75b06e079db97696f8a75f06c9993e7d47b4a3d7` | `8130658aacc45185f0bb0231042fb4f940f91023` |

Existing workflow steps/selectors are preserved and the new jtreg is appended.
Its `@run` directives exercise the matched experimental M3 image with and without
CompactStrings.

## Actually executed before receiver promotion

OpenJDK 21.0.12.1, exact helper Git blob `febd5b61...`, strict
`javac --patch-module java.base -Xlint:all -Werror` PASS.
Generated admitted grammar x text corpus: 342,225 full-match comparisons
against stock `java.util.regex.Pattern`, zero mismatches.

Not executed: canonical Synexia OpenRewrite Maven scheduler, actual
matched M3JDK21 build/jtreg, JNI/GC/JIT/CDS, full reactor and setup-inclusive
performance. No speedup or full Java regex equivalence is claimed.
