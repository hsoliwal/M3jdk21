<!-- SPDX-License-Identifier: Apache-2.0 -->
# Original M3AST/M3File Unicode codepoint coordinate projection

M3CodePointSourceMap borrows the ONE existing immutable parser M3File
String. It provides exact Unicode scalar/codepoint indices <-> UTF16
code-unit source boundaries with two bounded primitive checkpoint lanes
every 128 scalars, plus explicit EOF. Binary search plus at most 128
scalar steps support each query. No second String, byte payload or AST.

All UTF16 source is validated before publishing the map. Unpaired high
or low surrogates refuse rather than creating ambiguous codepoint positions.
Supplementary pairs occupy two UTF16 units but one Unicode scalar.
CRLF remains two scalars for codepoint indexing, one logical line break.

For an ANTLR4 CodePointCharStream-like parser adapter, convert the
parser's exact codepoint half-open node offsets through
M3CodePointSourceMap.utf16Offset before supplying the existing
M3ASTExternalIngress.Nodes.startUtf16/endUtf16 implementation.
This is not an imported ANTLR parser or a claim about every ANTLR
input stream, including legacy UTF16-indexed or normalized sources.

An admitted UTF8 M3File can also use the existing M3Utf8SourceMap.
Thus codepoint -> UTF16 -> UTF8 offsets roundtrip at scalar boundaries.
The product test proves original M3ASTIndex geometry, child roles,
token/trivia source coverage and SHA identity on a source with
supplementary Unicode, accented text, currency and CRLF.
External SOURCE_UNVERIFIED remains set: coordinate equality does not
certify the foreign parser used that same file generation.

Design donors (no copied code): ANTLR CodePointCharStream, Tree-sitter
byte-based coordinates, Roslyn UTF16 source ranges, rust-analyzer/rowan
lossless CST, OpenRewrite LST and Clang SourceManager provenance.

Still required: real ANTLR/JDT/Tree-sitter runtime fixtures, optional
parser grammar mapping, full patched JMOD/HotSpot/JNI parity and
benchmarks before product admission. No performance speedup is claimed.
