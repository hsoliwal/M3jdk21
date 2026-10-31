<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3AST source-bound binary syntax interchange

`com.m3.ast.M3SyntaxSnapshot` is a compact, bounded handoff of the existing
`M3SyntaxTree` occurrence columns. It does **not** instantiate or replace a
parser. Use the existing `ForeignNode` shape to project Java/JDT/Tree-sitter or
other ASTs once, then persist their grammar-specific immutable syntax facts
without provider lifetime references.

## Binary format 1 (network/big-endian primitive widths)

```
8 bytes  ASCII "M3SYNAST"
4 bytes  version = 1
4 bytes  independent source byte length
32 bytes SHA-256 of independent original source bytes
32 bytes SHA-256 of M3SyntaxTree.projectionSha256() canonical occurrence shape
4 bytes  syntax node count
4 bytes  grammar string UTF-16 code-unit count; 2-byte big-endian units
4 bytes  unique native kind count; repeat length+UTF-16-unit names
4 bytes  unique edge field count; repeat length+UTF-16-unit names
nodeCount * 24 bytes:
  int kindId, fieldId, flags, parentOccurrenceId, startByte, endByte
```

Names are exact UTF-16 code units; byte spans refer to the **original** source
image. All rows are preorder and have a unique parent. Zero-width missing/error
nodes, comments, whitespace, syntax recovery and raw input not covered by child
nodes round-trip because the source is held independently. Deduplicated native
kind/field names eliminate repeated string payloads; the snapshot has no
source bytes or Java object serialization.

## Sealed M3File generation boundary

`M3FileSource` already retains an immutable source generation and SHA-256. A
caller passes a copy of its source bytes to `M3SyntaxSnapshot.decode`; decoding
refuses the wrong content hash, wrong byte length or mismatched syntax
projection. After M3File edits, reparse and emit a **new** snapshot. Do not
carry the old symbol/type/ABI semantic sidecar across a changed source seal.
The codec lives inside `m3/ast`, with **no static dependency on m3/file**.
Cross-module contract tests remain in Synexia's convergence verification.

## Admission, limitations and security

`encode(tree, maximumPacketBytes)` and
`decode(packet, source, maximumNodes, maximumPacketBytes)` are bounded by the
explicit packet/node budgets. The caller supplies an independent, trusted
source image and controls its memory budget. The decoder checks format,
parent/child ordering, source spans, flags, corrupt payload, trailing bytes,
source SHA-256, and complete reconstituted projection SHA-256. SHA-256 is an
integrity identifier, **not** an authenticated signature or semantic proof.

This is a versioned, original Java implementation, not a copy of external
parser code. Inspired conceptually by rowan/rust-analyzer (immutable syntax),
Roslyn (positionless green nodes), Tree-sitter (source coordinate discipline),
SwiftSyntax (error recovery) and OpenRewrite (lossless syntax/semantic sidecars).
No external donor implementation was imported; attribution is recorded in the
corresponding Synexia recipe dataset.

Not implemented here: actual binary-file persistence transport, cross-process
signature/authentication, incremental parser execution, native mmap/JNI zero-copy,
semantic type proofs, huge-file paging, or comparative memory/CPU measurements.
