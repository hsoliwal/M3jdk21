<!-- SPDX-License-Identifier: Apache-2.0 -->
# Original provider → immutable parser M3File one-capture contract

One original filesystem provider read uses com.m3.file.M3File.readAllBytes.
Its caller passes the same captured bytes to jdk.m3.parser.M3File.fromCapturedBytes
and M3Utf8SourceMap.verified. The parser never reopens the Path or retains
the raw byte array. A caller can instead lend a read-only/direct ByteBuffer
window through the existing fromCaptured and verified overloads.

The standalone Java 21 test compiles the actual product classes on a
consumer classpath (no module-info.java or production cross-module edge).
It checks raw SHA-256 and decoded UTF-16 source identity, exact Unicode scalar
coordinates, direct/read-only buffer position/limit invariants, immutable
source snapshots across physical file edits, explicit later provider version
observations, malformed-input and byte-budget refusals, symlink non-following
on Linux, and harmless mutation of released capture arrays.

Only the initial read supplies the retained parser generation. A later
intentional provider read compares a DIFFERENT physical version; it is not
another admission read. Neither metadata nor matching URI/hash proves an
atomic filesystem snapshot across concurrent changes.

Concepts compared without donor-source imports: OpenJDK NIO, Tree-sitter
byte syntax coordinates, Clang source spelling/expansion, Roslyn immutable
source generations, rust-analyzer rowan, and OpenRewrite lossless source.
Full original parser JMOD/linked-image, nondefault M3FS providers, Windows,
native HotSpot/JNI and performance remain independent qualification gates.
