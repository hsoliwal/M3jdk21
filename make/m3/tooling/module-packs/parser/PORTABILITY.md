<!-- SPDX-License-Identifier: Apache-2.0 -->
# Cross-parser M3AST and source-aware M3File (candidate)

ONE owner: the existing M3ASTIndex retains parent/child/sibling geometry,
optional details and optional external parser roles/tags in primitive columns.
No parallel parser, per-node Entry objects, retained external AST or text store.
M3ASTExternalIngress.Nodes supplies a parser ID, native kind, child count/child,
optional child field, UTF-16 half-open ranges and token/trivia/missing/recovery flags.
Original parser owns parse/recovery/type semantics and mutation.
External source association remains SOURCE_UNVERIFIED until an independent
parser-owner generation seal is established.

M3File already owns the immutable text String and separate raw-byte/UTF-16 hashes.
New on-demand line-start int[] maps UTF-16 offsets to 1-based lines and 0-based
columns and back; CRLF is one break, supplementary code points still occupy
two UTF-16 code units and EOF has an address. This is **not** a byte offset map.

## Open-source donor contracts used without copied source code

- Tree-sitter: named and anonymous token nodes, field names, MISSING/ERROR;
  reparse/edit API and source byte positions remain outside this slice.
- Roslyn: immutable syntax payloads, lazy views and source ownership;
  green/red allocation mechanics not transplanted.
- rust-analyzer rowan: lossless CST and cheap typed projections;
  full trivia/token roundtrip requires a future coverage proof.
- OpenRewrite: whitespace/comments/type attribution remain with original
  lossless-semantic-tree owner, not inferred from these detached lanes.
- ANTLR: parser rules/terminals and hidden token channels can supply tags.
- Clang SourceManager: spelling/expansion provenance demands a separate
  explicit coordinate map; generated or macro ranges are never guessed.
- Babel/ESTree, JavaParser/Spoon, srcML: parser fields, lexical ranges and
  source roundtrip motivate adapters; none are claimed implemented here.

## Required future gates

1. Real parser-native adapters: JDT descriptor roles, Tree-sitter, ANTLR,
   Babel/ESTree, Kotlin, Gradle, OpenRewrite and Rust, with donor licenses.
2. Same-capture byte-to-UTF-16 tables and spelling/expansion source maps;
   zero fabricated source-binding trust.
3. Token/trivia coverage ledger, diagnostics and error-node fidelity; syntax
   roundtrip must be checked against the exact original bytes/source.
4. Incremental edit generations, source delta provenance, portable typed
   symbols and native/JNI/file-backed sidecar layouts where measured useful.
5. Serial source-sealed recipe replay, parser host + linked JMOD, custom JDK/
   JNI/HotSpot + jtreg, memory/profile benchmarks before promoting.

**DRAFT:** no claim of roundtrip, equivalence of all ASTs, speedup,
full-parser adapter support or passed product CI.
