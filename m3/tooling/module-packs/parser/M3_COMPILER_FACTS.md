<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 compiler-owned shared syntax fact image

M3JDK21 is the integrated Java/HotSpot/JNI SDK. The original
`com.sun.source.util.M3AST` remains the compiler event authority.
`M3JavacParser.projection` returns the same original PARSED-event preorder
projection to registered callers of that exact operation identity.
`M3JavacFactImage` encodes those existing primitive row lanes; it does not
parse a source file, run another javac, synthesize types, or retain a second
tree. Its retained representation is one byte array with 16 bytes per indexed
occurrence. Row lookups decode primitive offsets directly, without per-row
objects or ByteBuffer instances.

The versioned `m3jdk21.javac.geometry.v1` format records source URI,
UTF-16 source SHA-256 (including surrogate code units), exact caller-supplied
compilation-context SHA-256, compiler Tree.Kind enum-identity SHA-256, row
count, source length, kind/parent/start/end int lanes and final checksum.
Readers require the original M3File source identity and the full compiler
context, verify geometry and checksum, and refuse corrupt/stale facts.
They may request source text from the admitted original M3File, but the
image stores no second source payload.

This first image supports SYNTAX|POSITIONS|PARENTS only (mask 0x07).
It explicitly does NOT contain resolved Elements/Types, complete method
signatures, ABI declarations, bytecode CFG/dataflow or annotation processor
effects. A matching hash is not permission to skip compilation or to use this
image for another context. Maven and Gradle must retain their compiler,
source lifecycle and plugin contracts. JDK facts storage should use existing
M3FS/M3IndexDB, not an unrelated Maven SQLite database.

Original compiler image generation, durable M3FS publication, per-module
Maven/Gradle manifest admission, M3ASTIndex projection compatibility and
annotated semantic image evolution need separately qualified consumers.
The optional JNI backend is not changed by this slice: moving 16-byte rows
through JNI without evidence of a win would add unsupported symbols.

## Qualification

Standalone OpenJDK 21.0.12.1 Java compile with strict warnings and
`M3_JAVAC_FACT_IMAGE_PASS checks=16`: PASS. The compile used local API-shape
copies of M3File and M3JavacTree because it was **not** a source-built
M3JDK21 compiler image. Exact original owner build.py, Maven/OpenRewrite
Recipe.run, JMOD, HotSpot/jtreg, Maven and Gradle consumers: NOT RUN.
