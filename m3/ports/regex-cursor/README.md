<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3JDK regex cursor port

Status: part of `java.base` (package `jdk.m3.regexcursor`, not exported; the Apache-2.0 headers
are kept). No public JDK API. Design: [`m3/docs/regex-cursor/DESIGN.md`](../../docs/regex-cursor/DESIGN.md).
Donors: [`FOSS_REUSE_DECISION.tsv`](FOSS_REUSE_DECISION.tsv), [`NOTICE`](NOTICE).

## Location

| What | Where |
|---|---|
| Java sources | `src/java.base/share/classes/jdk/m3/regexcursor`, compiled into java.base by the JDK build |
| JUnit tests and proofs | `test/jdk/jdk/m3/regexcursor` (grammar test data in its `examples/`), run by this folder's `pom.xml` (not by jtreg) |
| Maven build, test-only JNI oracle (`src/test/native`), proof scripts, records | this folder; `pom.xml` builds the same artifact `com.m3:m3-regex-cursor` from the two folders above |

On an M3JDK image the package is loaded from java.base and is not exported, so a program on the class path
that uses it needs `--add-exports java.base/jdk.m3.regexcursor=ALL-UNNAMED`. On any other JDK the Maven
artifact behaves as before. The scripts in `proofs/` still name `src/main/java` and the `M3`-prefixed
class names of before the rename; they were not changed by the move.

## Language-spec grammar precompute

From com.synexia #18253: `GrammarSpecification` (versioned lexical rules over `StaticGrammarLexicon`
and an LL(1) prediction table in packed arrays, with int, `IntBuffer`, long-index and streaming token
views), `EbnfGrammar` (bounded BNF/EBNF import), `GrammarImageTool` (writes `.m3grammar` images) and
`Rfc8259JsonGrammar` (RFC 8259 JSON); `GrammarTokenProjection` gained `projectPrepared` and
`projectPreparedCompleteCursor`. A grammar image is read back only against a SHA-256 pinned outside the
image. java.base copies only the resource extensions its build lists, so the RFC 8259 image is the
Base64 constant in `Rfc8259JsonGrammarImage`. `GrammarSpecificationProof`, `GrammarEbnfProof`,
`GrammarImageProof`, `GrammarImageV2Proof`, `GrammarStreamingProof` and `Rfc8259JsonProof` are standalone
`main` programs, run from `test/jdk/jdk/m3/regexcursor` (they read `examples/`).

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
