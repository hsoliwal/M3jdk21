<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Native M3JDK21: reusable upper parser layers

M3JDK21 is the **single integrated Java 21/HotSpot/JNI SDK**, not an
M3 + JDK wrapper. A frontend is authoritative for the parse/analysis it
has *already* performed. Downstream M3 analyzers consume native
M3File/M3ASTIndex primitive coordinate lanes and provenance rather than
reconstructing Java, JSON, DSL or bytecode trees independently.

## New first-party ANTLR4 donor ingress

`M3ASTIngress.antlr4(M3File, original ParseTree, original Parser,
original CharStream, Limits)` delegates to `M3Antlr4Ingress.index(...)`
and adds `Domain.ANTLR4` **at the end** of the existing domain enum,
preserving all prior enum indices.

- Does not invoke ANTLR parsing, lexing, listeners, visitors or printing.
  Grammar-generated ParserRuleContext rule names and original token types
  supply occurrence kinds in the same existing M3ASTIndex owner.
- A single bounded preorder traversal publishes parent/child/sibling
  topology, exact UTF-16 half-open source ranges, line numbers and
  `RECOVERED` flags for ANTLR ErrorNode/exception-bearing rule contexts.
  Generated/synthetic tokens lacking original source have **unknown ranges**.
- Original CharStream `getText(Interval(0, size-1))` must equal the
  immutable M3File source. Its instance must be the original token stream
  identity; equal text on an unrelated CharStream never authorizes rows.
  If an unbuffered stream cannot prove its original complete input, refuse.
- Supports the modern ANTLR CodePointCharStream offsets (Unicode code points)
  and legacy ANTLRInputStream offsets (UTF-16 units). A temporary
  codepoint→UTF-16 `int[]` offset map is allocated only when supplementary
  characters make direct offsets incorrect. Each token span returns a packed
  `long`, not a per-row range object. Temporary parse-tree handles,
  reflection accessors and maps are released when projection completes.
- Reuses the original M3ASTIndex.Builder's source-range admission,
  immutable detached arrays/flags, and standard limits. It stores neither
  ANTLR syntax objects nor a second source string. This is **syntax**
  evidence, not type attribution, effective build model or ABI proof.

## Catalogue-driven donor upper-layer selection

| Category | Canonical donor | Native M3JDK21 receiving rule | Status |
| --- | --- | --- | --- |
| Java compile/attribution | Original javac/M3AST | Existing PARSED/ANALYZED compiler event and M3ASTIndex | Existing owner; do not reparse |
| Java editor/working copy | Eclipse JDT (EPL-2.0) | Original ASTNode, binding/recovery facts with generation proof | Existing JDT ingress; bindings only when JDT verifies them |
| Java recipe/refactoring | OpenRewrite core (Apache-2.0; individual recipes need license review) | Existing J.CompilationUnit with lossless printing retained by OpenRewrite | Existing typed companion; never invent source spans |
| Grammar-defined DSL | ANTLR4 (BSD-3-Clause) | Original ParserRuleContext + CharStream into M3ASTIndex | New bounded source-verified syntax ingress |
| Incremental editor parsing | Tree-sitter (MIT) | Accept original tree edits/changed byte ranges, convert UTF-8 bytes to exact UTF-16 source coordinates | Donor candidate only; no JNI claim |
| Java symbol libraries | JavaParser/Symbol Solver (Apache-2.0 or LGPL dual-license) | Prefer javac's attributed facts; borrow only independently verified unique capabilities | Donor candidate only |
| Structured JSON | Jackson (Apache-2.0) | Original Jackson token/AST owner and streaming events | Existing JSON ingress; no second JSON parser |
| Groovy/Kotlin DSL | Existing Groovy AST / Kotlin PSI | Original nodes and Gradle model, with exact source-generation constraints | Existing original model ingress |

All donor implementations, licenses and provenance must be pinned before
incorporating code. The references above designate **interfaces/algorithms**,
not imported foreign source. LeetCode, HackerRank and GeeksforGeeks supply
problem categories (array offsets, traversal, tries, bit masks, graph indexing);
no solution source has been copied.

## Scope and evidence

Executed locally: strict OpenJDK 21 `javac --release 21
-Xlint:all,-module -Werror`, and
`M3_ANTLR4_INGRESS_PASS checks=48` on ANTLR4 **public-API-shaped
test fixtures**, covering Unicode supplementary-plane conversion,
legacy UTF-16, CRLF lines, parent topology, grammar rule and token names,
error recovery, source mismatches, bounds, depth/node budgets and
detached immutability. The new Java ingress, test, and 13 original
minimal API-shape fixture source blobs are each byte-sealed against
the locally compiled files.

**NOT RUN**: real ANTLR4 4.13.x jar and generated grammar/parser
runtime, canonical Maven/OpenRewrite `Recipe.run` and receiving fixed
point, original parser module/JMOD build, full M3JDK21 source-built
HotSpot/JNI runtime, Maven/Gradle plugin migration and warm/cold
benchmarks. No speedup, correctness on untested donor runtimes, or
complete SDK acceptance is asserted.

Before promotion, compile/run the actual ANTLR4 runtime (including
CharStreams.fromString, CommonTokenStream and a generated parser)
against the original Java 21 module. Add adversarial source-recovery
fixtures and benchmark O(nodes) memory allocation relative to direct
ANTLR visitors. Keep required third-party packages out of the
core JMOD if an optional owner is needed.
