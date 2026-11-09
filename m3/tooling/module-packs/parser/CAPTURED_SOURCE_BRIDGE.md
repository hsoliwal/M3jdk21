<!-- SPDX-License-Identifier: Apache-2.0 -->
# Original captured bytes to parser M3File and source map

An existing original provider may admit bytes once, then pass its completed
bounded byte array to jdk.m3.parser.M3File.fromCapturedBytes.
A caller with a direct, read-only, mapped or sliced NIO ByteBuffer can
instead use M3File.fromCaptured with its [position,limit) window.
Neither operation reopens the original filesystem provider nor retains
the mutable byte array or ByteBuffer storage. Only the original immutable
M3File source String and separate raw SHA256 / UTF16BE SHA256 are retained.

The original com.m3.file.M3File remains the filesystem owner and may call
readAllBytes(maxBytes) exactly once before the parser admission. There is
no new runtime JDK dependency on com.m3.file, no second filesystem owner,
no JNI heap pin and no M3+JDK compatibility layer.

Original M3Utf8SourceMap.verified now accepts the same ByteBuffer window
directly, verifying exact raw SHA256, strict UTF8 decoding and text identity
without allocating a second heap raw byte[]; byte[] callers retain their
original overload. Unchanged source stream ownership and close behavior
are tested by the original M3FileInputContract alongside the new contract.

Bytes must remain stable during their borrowed call; neither a path,
filesystem revision, mutable buffer nor source URI gives an atomic snapshot
across concurrent provider changes. The selected Charset defines BOM policy.
No parser-input seal is inferred from URI/length/hash equality.

Donor concepts (NO imported source): OpenJDK NIO buffer windows and charset
decoders; Tree-sitter byte syntax ranges; Clang SourceManager provenance;
Roslyn and rust-analyzer immutable source generations; OpenRewrite LST.

Required gates: original Java21 regression CI, Maven/OpenRewrite fixed point
and drift refusals, parser JMOD and jlink, full M3JDK HotSpot/JNI, native
M3FS read parity and performance measurements. No speedup is asserted.
