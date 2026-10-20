<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3File: narrow NIO file-edge receiver

`com.m3.file.M3File` is a minimal target-owned, Java21/module-info-enabled
read-only access boundary adapted from the canonical Synexia
`synexia-mindex/resource/.../MIndexResourceAccess.java`, Apache-2.0,
Git blob `1b4eb18b102f52be2d1e13589195b47e37f3b090`.

It does **not** create a filesystem implementation or retain a directory
object graph. It preserves the supplied `Path` instance/provider; stat calls
always refresh with NOFOLLOW_LINKS; `readAllBytes(maxBytes)` checks the
visible file type, reads through a caller-closed channel, and enforces both
the observed-size and streamed-length limits. Symlinks and directories do
not become regular files. Path, link and permission behavior remains NIO's.

The broader Synexia `MIndexResourceSnapshot`, `MIndexResourceWorkspace`,
Eclipse markers/deltas, tree scans, permissions and mutation APIs remain
unported. This is not full Eclipse workspace parity, an OS-level file
interning service, or permission to change existing `java.io.File` semantics.

Build `mvn -B -f m3/file/pom.xml test` or the containing Eclipse JMOD
builder in `m3/eclipse/optional-jmod/build.py`.

## M3FileTree: the lane tree (slice 1, 2026-10-20)

`M3FileTree.open(Path root)` scans a directory tree once (`Files.walkFileTree`,
links never followed) into structure-of-arrays lanes: `parent`, `depth`, a
`char[]` name pool with offsets, CSR child slices, `size`, `mtime` and one
`BitSet` per `M3FileFlag`. Row ids are pre-order and dense; `0` is the root.
`M3File` is the entry type: `tree.file(id)` returns a thin `(tree, id)` facade
from a bounded residency table; identity is the row, so facades may be dropped
and recreated. `M3File.of(Path)` keeps the detached read edge above. Design,
invariants and the remaining slices (lazy scan, deltas, views, viewport row
host, lane image) are in [DESIGN.md](DESIGN.md); donor decisions in
`qualification/m3file-tree-20261020/FOSS_REUSE_DECISION.tsv`.
