<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3File parser incremental edit frames (candidate)

`M3FileEditFrames.plan` exports an immutable, bounded sequence of
Tree-sitter-style UTF-8 `startByte`/`oldEndByte`/`newEndByte` edits with zero-based
row and **byte-column** points. Original `M3FileSource` and
`M3FileEditBatch.ChangeMap` supply the exact, content-sealed inputs. Edits are
emitted right-to-left, allowing disjoint original-source coordinates to remain
valid for each stage. The byte/line points are measured from real intermediate
source images, including multibyte UTF-8 and CR/LF/CRLF seams.

The implementation validates strict UTF-8 scalar boundaries and verifies that
replaying the edit frames reproduces the final source *exactly*. It rejects
stale generations, malformed input, mid-codepoint splits and exceeded explicit
byte-work budgets. The Java API is only a data transfer shape, not an ABI
for a native Tree-sitter library, an incremental parser, or a concurrency-safe
filesystem snapshot. **It currently costs O(number of edits × source length)**
because intermediate images are rematerialized to calculate exact points. Do not
use this adapter for huge files without replacing that bounded algorithm using
the existing M3File paged/segment owners and qualifying its CPU/heap behavior.

The existing `M3File`, stream observer, watcher, delta ledger and build-planner
remain authoritative. This candidate adds no new filesystem, JNI symbols or
whole-tree rebuilding policy. The original module's strict contract inventory
must include `M3FileEditFramesContract` before the new test can enter CI.

Canonical recipe: Synexia
`portable-ast-file-v3-20261026`, existing hash-pinned Java/text OpenRewrite owner.
Java 21 fixture tests passed; actual Maven/OpenRewrite SDK execution, original
product module/jlink, installed JDK and performance qualification remain PENDING.
