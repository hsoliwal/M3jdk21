<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 / real ANTLR4 Unicode parse and source-fidelity admission

This test-only Java 21 fixture generates its own ANTLR4 grammar and executes
the REAL org.antlr:antlr4-runtime:4.13.2 Java lexer AND parser. ANTLR4's
CharStreams.fromString provides Unicode code-point indexed input (including
supplementary characters). Its Token.getStartIndex/getStopIndex use inclusive
code-point positions; M3ASTIndex instead stores UTF-16 half-open ranges.

The fixture uses one original captured M3File and the existing
M3CodePointSourceMap plus M3Utf8SourceMap to convert ANTLR positions
without conflating bytes, Unicode scalars and Java UTF-16 code units.
TokenStream.getTokens includes hidden-channel whitespace; it checks a
contiguous lexical source and projects its token/trivia occurrences through
M3ASTExternalIngress into the original M3ASTIndex. Exact coverage is
qualified with M3ASTSyntaxCoverage, not an invented parallel AST.
The real ANTLR parser's document context is also parsed and checked.

ANTLR4's grammar is a small independently authored test fixture, NOT an
upstream grammar or donor code copy. ANTLR4 runtime/tools are BSD-3-Clause.
No ANTLR runtime dependency is added to jdk.m3.parser or the linked JDK;
its Maven pom, generated classes, and ANTLR runtime remain in integration/
antlr4 test-only scope. Maven downloads happen only in hosted proof.

An externally supplied parser is not certified to have consumed the M3File
by equal URI/length/text: SOURCE_UNVERIFIED remains set. ANTLR's whitespace
is off-channel in the parser tree, so lexeme/trivia source coverage is built
from the genuine token stream; we do NOT call the parser's semantic tree a
lossless replacement for the original AST. M3AST remains the one indexed
representation; native parser and M3File keep their own contracts.

Additional proof still required: complete real ANTLR grammar corpus, JDT/
OpenRewrite/Tree-sitter parity, source macro maps, actual JMOD/jlink,
custom HotSpot/JNI, compiler ABI effects, streaming large files, and measured
memory/performance. This fixture proves code-point/token-trivia interoperability
for only the included grammar and randomized Unicode inputs.

The canonical Synexia hash-pinned text recipe admits .txt, not arbitrary
.g4 files. The grammar is stored at integration/antlr4/grammar/M3UnicodeTiny.g4.txt
and copied to a temporary src/main/antlr4/M3UnicodeTiny.g4 only during CI;
no generic grammar-path mutation carve-out is added to the shared recipe.
