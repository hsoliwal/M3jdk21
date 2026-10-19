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
