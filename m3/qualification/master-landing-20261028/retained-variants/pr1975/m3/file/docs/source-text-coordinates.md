<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3File source byte access and codec coordinates — source-sealed candidate

`M3FileSource.byteAt` safely returns one immutable physical source byte without
copying the entire image. The original `M3File` remains the NIO provider,
identity and observation authority. It retains its source SHA-256, watcher,
delta ledger, progress and read budgets; no filesystem or content payload
is duplicated in this enhancement.

`M3FileTextCoordinates` overlays the existing source image with *declared*
UTF-8, UTF-8 with required BOM, UTF-16 little/big endian (optional matching
BOM) or Latin-1. It keeps only primitive sparse checkpoints and line origins:

- Half-open **byte** offsets for Tree-sitter-style parser nodes and row/byte
  columns; UTF-16 code-unit offsets for Java/JDT/editor nodes are separate.
- Exact bidirectional byte/UTF-16 and row/column conversions; strict rejection
  of overlong UTF-8, surrogate encodings, truncated code units, wrong BOMs,
  split Unicode scalars and mid-surrogate UTF-16 positions.
- CR, LF and CRLF are handled as logical line ends, with physical byte offsets
  retained. A source BOM is not an exposed decoded character; content begins
  after it. Native parser root spans that omit leading BOM or trivia should be
  received using the existing synthetic source root (`projectFragment`).
- The codec validates once and does not retain an independently decoded text
  or another copy of source bytes. `decodedText()` is a caller-owned temporary.

No charset guessing, Unicode-normalization, atomic OS snapshot, JNI symbol,
zero-copy foreign parser bridge, mmap/paging, incremental reparse or performance
advantage is claimed. The tested UTF-16 crosswalk and original source contract
are a **candidate**, not proof of all decoder/platform contracts.

The existing `m3/file/verify.py` exact contract list is expanded to include
`M3FileTextCoordinatesContract`, so host/linked-image verification must execute
it before admission. Source-sealed generation and full receiver CI are pending.
