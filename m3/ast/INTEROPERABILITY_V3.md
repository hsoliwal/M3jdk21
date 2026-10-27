<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Portable syntax delta and multi-origin maps (candidate)

`M3SyntaxDelta` is an optional source-edit projection for two *already parsed* immutable
`M3SyntaxTree`/`M3SyntaxIndex` generations. Its four-method `SourceEditMap` lets a
caller adapt any source-delta owner (including `M3FileEditBatch.ChangeMap`) without
installing an M3File dependency in the AST product module. Its primitive arrays join
unchanged syntax occurrences by generation-sealed byte offsets, native grammar kinds,
field roles, flags, source bytes and position-independent subtree hashes. Ambiguous
multiple matching occurrences are refused. **A match is only an index-reuse candidate;
it is not a semantic type/binding, compiler-behavior, or reparse-skip proof.**

`M3SyntaxOrigins` adds a projection-sealed, multi-origin source-location sidecar for
macros, generated syntax and embedded languages. It stores source content hashes,
source-relative coordinates and provenance relations in primitive columns. A generated
span can have several origin spans; a point maps back only when there is *one*
`EXACT_BYTES` correspondence with equal byte widths. All other relationships remain
explicitly ambiguous or derived. It neither owns source payload nor replaces an
upstream parser's source manager.

Existing `M3SyntaxTree` remains the sole occurrence representation. Existing
`M3ASTHierarchy` remains the L0–L5 cross-scope proof hierarchy. Optional sidecars
neither duplicate source bytes nor create per-occurrence object graphs.

## Verification / constraints

Canonical source-sealed dataset: Synexia
`portable-ast-file-v3-20261026` in the existing Maven/OpenRewrite recipe crate.
Compiled with Java 21 `--release 21 -Xlint:all -Werror` and verified against
synthetic sources and real `jdk.compiler` parses; the cross-module fixture remains in
Synexia. Product CI/Maven, actual OpenRewrite SDK Results, installed JDK image,
real Tree-sitter/JDT/OpenRewrite provider adapters and performance benchmarks remain
PENDING. Source and test files here are PR candidates, not admitted JDK backends.
