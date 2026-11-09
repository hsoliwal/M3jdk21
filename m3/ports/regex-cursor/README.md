<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3JDK regex cursor port

Status: isolated Apache-2.0 proving ground. Not on the bootstrap path, no `src/**` change, no
public JDK API. Design: [`m3/docs/regex-cursor/DESIGN.md`](../../docs/regex-cursor/DESIGN.md).
Donors: [`FOSS_REUSE_DECISION.tsv`](FOSS_REUSE_DECISION.tsv), [`NOTICE`](NOTICE).

| Class | Role |
|---|---|
| `M3ChunkCursor` | chunk cursor contract (regex-cursor `Cursor` + `Input::move_to`) |
| `M3StringCursor` | chunks = visible atom runs of the M3 tuple DAG (CodeUnitCursor descent) |
| `M3RopeString`, `M3RopeOwner`, `M3RopeAtom`, `M3RopeTuple` | mirror of the M3String owner/coordinate model; text readable only through a cursor |
| `M3CharSequenceCursor` | any `CharSequence` with an explicit chunk plan |
| `M3CursorCharSequence` | `charAt` view over a cursor for the stock engine; `toString` is the counted materialization |
| `M3CursorFacts`, `M3CursorProbe` | precompute facts gate; materialization/stock/gate/give-up counters |
| `M3CursorLiteralSearch` | chunk-spanning KMP with first-unit skip, forward and reverse |
| `M3RegexSubsetParser`, `M3RegexAst`, `M3RegexLiterals` | subset parser into int lanes; pure/prefix/required literal facts |
| `M3CursorNfa`, `M3UnitClasses` | Thompson NFA lanes (forward, unanchored, reverse); symbol classes with quit units |
| `M3CursorDfa` | lazy DFA: leftmost-first end, all-match reverse start, `matches()` |
| `M3CursorPikeVm` | captures and `^ $ \A \z \Z` with leftmost-first priority |
| `M3CursorRegex`, `M3CursorMatcher` | meta strategy and the `java.util.regex.Matcher`-shaped API |
| `M3TokenCursor`, `M3TokenPattern`, `M3TokenPatternParser` | token-aware regex over interned token-id lanes (`<id>`, `<a-b>`, `<a,b>`, `<#CLASS>`, `<!...>`, `<.>`); t-rex construct map in `TREX_PATTERN_MAP.tsv` |

## Build and test

```sh
mvn -B -ntp -f m3/ports/regex-cursor/pom.xml test     # JaCoCo LINE >= 0.99 gate
```

Without network or a Maven cache the same suite runs with `javac` plus the JUnit console
launcher and the JaCoCo agent (one JVM, `-XX:+UseSerialGC -Xmx512m`).
