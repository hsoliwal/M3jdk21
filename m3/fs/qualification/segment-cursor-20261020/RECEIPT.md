<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 M3FS indexed-run UTF-16 comparison — 2026-10-20

M3FS is an M3JDK21-owned `jdk.m3fs` module; this leaf is stacked on hsoliwal/M3JDK21#1425 at `23edf362f6852295f92a4ec65164f6a777b3613a`, not on an independent adaptor runtime.

Canonical Synexia recipe: https://github.com/hsoliwal/com.synexia/pull/12561 at `110366f92a637461dc74ef2d0607a7116579b195`, named `com.synexia.m3.M3Jdk21M3FsSegmentCursor20261020`. It contains the same exact received Java source and JUnit test.

Inside existing `M3FsPrecomputeBundle.SegmentedUtf16String`, sequential `contentEquals` reads direct `ByteBuffer` UTF-16 units by each immutable parent index/start/count, O(total code units + segment count), rather than invoking `segmentAt` binary lookup for every unit. Random `charAt` and ranged `copyChars` retain their existing exact segment geometry and offset semantics. No new `String`/char[] copy, page owner, UTF-16 normalization, Java/JNI ABI, or per-element object. Zero-length segments and split/unpaired surrogates remain legal.

Sealed file SHA-256:
- M3FsPrecomputeBundle.java: `9b7c35f30a3c402393b3941e1c46d828f362ca2c266b41eed3445c0465bf26ce` → `07a5b271d3c1f4d268f3844d889623e29a2626ef5941bd0b806bfc34d148fd9f`.
- M3SegmentedUtf16FsTest.java: `557b52db349ff06240ffdb1a62612b5dc30a37436a5002e39852c236f4b6f7ab` → `67f1dc7f49e4048408821e3e19d1a67b4fb9c2f86ed8aa9d1dc170ba2d8aaff6`.

Standalone stock Java21 indexed-run model passed 585,058 independent assertions. Actual M3FS module Maven/JNI/CTest and the extended source JUnit test have NOT been run on this exact receiver; full modified-JDK/JMOD/GC/RSS and benchmark gates also remain open. Preserve the existing #1425 native ABI/format, donor license, and performance evidence as historical, not fresh qualification. Do not merge out of stack order.
