<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3AST byte-indexed ingress and M3File immutable generations

M3ASTByteIngress reuses existing M3ASTExternalIngress and M3ASTIndex.
Tree-sitter-like UTF-8 byte ranges convert through a source-bound
M3Utf8SourceMap, preserving native child-role and token flags.
External parser source binding remains SOURCE_UNVERIFIED.
No new parser tree, original source buffer or JNI owner is retained.

M3FileEdits.apply validates sorted disjoint original-source UTF-16 edits
before producing one new M3File source generation. Original raw-byte SHA
is not copied. Four primitive int[] lanes hold source-generation edits;
offset translation and closed-boundary touch tests are O(log editCount).
Unknown AST node source ranges are conservatively dirty. A clean bit
is never permission to skip the source parser, compiler or tests.

FOSS design inputs: Tree-sitter byte positions and edit envelope;
Roslyn immutable full-fidelity syntax; rust-analyzer/rowan green/red
views; OpenRewrite source LST and type markers; Clang spelling/expansion
provenance; SwiftSyntax and LibCST exact trivia. No donor code copied.

Pending: real native parser adapters, identity attestation, generated
code provenance, incremental reparse, custom HotSpot/JNI, real JMOD,
whole-reactor CI, ABI and performance. All are separate proof gates.
