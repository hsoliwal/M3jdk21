<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 CPU source-byte statistics and bounded run scanning

**Status:** opt-in Java 21 target-owned CPU leaf; **not** a blanket M3String/JDK
replacement, complete CPUTOOLS implementation, or JNI speedup claim.

## Existing owner and public contract

This code extends the existing `com.m3.runtime.capability` JPMS/JMOD module,
not `java.lang`, `java.util`, HotSpot, the JNI ABI, or the canonical Synexia
MIndexString resolver. It consumes immutable `SourceArtifact.Span` byte ranges,
shares their source identity, and stores exactly 256 primitive `long` counters
plus four 64-bit presence-mask lanes (2,080 retained primitive bytes). The
source owner retains its own immutable source-byte payload. There are **zero**
per-byte Entry objects and no new text buffers or independent interner.

`ByteStatistics.scan(span)` is one pass over exact bytes, `O(N + 256)` time
and `O(256)` fixed retained histogram/bitmask metadata. Its outputs are:

- `count(0..255)`, `histogramCopy()` (an explicit mutable compatibility copy),
  `presenceMaskWord(0..3)`, `distinctByteCount()`,
  `modalUnsignedByte()` (-1 on empty, smallest byte wins ties),
  `nonAsciiBytes()`, `shannonEntropyBitsPerByte()` (zero for empty).
- `jensenShannonBits(other)` computes bounded Jensen–Shannon divergence in
  bits between two **nonempty** source-byte histograms, symmetrically in
  `O(256)`. Empty distributions are undefined and rejected.
- `scanRuns(span, maxRuns, sink)` streams `(unsignedByte, from, toExclusive)`
  primitive run records with source-relative byte offsets. It is `O(N)` time,
  O(1) run-local memory and caps emitted runs at 1,000,000. A result reaching
  `maxRuns` may be a truncated prefix, not complete source coverage.

This family contributes **partial** deterministic operators for data quality,
statistical summaries, information-theoretic scoring, byte presence, and
run-length shapes. It **does not** implement a full compression codec, spatial
index, formal SAT/SMT solver, NLP parser, or model.

## M3String and native boundaries

OS utility streams and `SourceArtifact.Span` are uninterpreted **bytes**.
UTF-8 decoding, Unicode normalization, or projection into canonical M3String
must happen through the already-qualified receiving JDK owner, not by silently
interning byte values as text. `scan()` and `scanRuns()` neither decode nor
materialize a whole Java String. The current M3String stage remains the
active receiving gate. No process-global OS interning API is assumed.

Native/JNI byte operations already belong to the source-bound
`LfCount`/`BytePatternSearch` native library and separately qualified
Synexia JNI paths. This wave does **not** duplicate JNI kernels or claim a
faster native path: the Java 21 oracle is authoritative and compiled using the
existing `SourceArtifact.java` Git blob.

## Donor discovery, source custody and licensing

The 110-row `CPU_TOOLS_FAMILY_CATALOGUE_20261017.tsv` is based on the user's
`CPUTOOLS(6).txt` tool taxonomy (91 categories + 19 named Unix commands).
Each category has an explicit owner/candidate, data representation boundary,
algorithm complexity where established, contract gap, license disposition and
required proof. It records `CATALOGUED_ONLY` or `REUSE_SYNX_OWNER_NOT_RECEIVED`
for tools not implemented or admitted in the target.

Compared candidates include existing Synexia `synexia-algo` (graphs,
compression, scheduling, optimization), M3JDK21 byte-scanning owners, the
MIT Unix4j upstream donor, and problem-shape references such as LeetCode 28
(string search), HackerRank Sparse Arrays (counts), and GeeksforGeeks external
sorting. These sites and tool families provide **problem/test categories**;
none of their challenge submissions or original utility binaries is copied.
First-party files retain Apache-2.0, authorship and source-hash provenance.
OpenJDK source files remain under their original licenses.

## Verification

The focused Java 21 JDK compiler oracle uses exact current target
`SourceArtifact.java`, `ByteStatistics.java` and `ByteStatisticsChecks.java`.
The test suite independently compares full 256-bin histograms, presence masks,
byte counts, mode, entropy, source/offset/lifetime behavior, RLE, Jensen–Shannon,
interruptions, virtual threads and 1,200 seeded random immutable byte spans.
It passed **632,981 assertions** locally under `-Xmx40m` on stock OpenJDK
21.0.12.1, including `-Xlint:all,-module -Werror`. That proof is limited to
this Java leaf; the modified JDK image, JMOD build/linked image and JNI parity
remain separate gates.

The canonical Maven/OpenRewrite recipe and source templates are held by
`hsoliwal/com.synexia` under a separately reviewable recipe crate. This
receiving repository retains SHA-256s and a byte-exact postimage check, but
does not establish another canonical recipe or transformation framework.

**Do not promote this category beyond its proven FILE/module contracts**
without exact CI, API compatibility, resource measurements and a qualifying
M3JDK21 receiver image. `develop` in Synexia is never rebased.