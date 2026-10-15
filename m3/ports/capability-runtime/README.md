<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 CPU capability substrate (opt-in JPMS/JMOD candidate)

## Program boundary

This is a small **target-owned** runtime primitive, not a second CPULLM/FLLDCIM planner.
The connected Synexia `cpullm-java/control/CapabilityControlPlane`, `FuzzyDagResolver`,
`ToolAtomCatalog`, `DeviceFabric` and `SelfRagController` retain semantic/policy authority.
M3JDK21 owns the opt-in `com.m3.runtime.capability` module and its CPU/JNI ABI. **Nothing
is loaded into java.base or HotSpot, and no Synexia runtime dependency is introduced.**
The first self-contained atom supplies immutable source byte ownership and logical ranges,
explicit budgeted working-set leases, and source-byte LF scan. This supports typed tools,
source-coordinate retrieval and cold/warm policies without claiming to implement the
entire capability DAG or the node-local SQLite mesh.

## Contract

- `SourceArtifact.copyOf`: one immutable source-root SHA-256 over caller-copied bytes;
  at most 16 MiB per source; `Span` retains source+byte coordinates; empty spans and
  unpaired/invalid UTF-8 sequences are *legal bytes*, never transcoded to String.
- `WorkingSet.acquire`: reserve only declared **logical byte occupancy** with an
  atomic synchronized boundary. Lease operations fail after close; close is idempotent;
  interruption is respected. This is **not** a claim to bound all JVM heap/RSS or
  transparently page `Object[]`. The caller owns the artifact handle/lifetime.
- `LfCount.javaCount`: byte-stream UTF-8-independent LF counting over an *absolute*
  `[start,start+length)` window under ByteBuffer.limit; neither buffer position nor
  content is changed. No UTF-16 char offsets are inferred from byte coordinates.
- `LfCount.nativeCount`: opt-in direct buffers only, with SHA-256-pinned native library,
  post-load identity, explicit ABI version, JNI geometry validation, no Java array
  pinning, no retained native pointers and no hidden native/GPU dependency.

Java is the semantic oracle. JNI is a separately admitted **C11 scalar reference**;
no speedup or crossover is claimed. Source payload bytes are a separate artifact
store, not a new M3 String per-value backing/parallel Java String engine.

## Source-backed byte line indexing (second recipe wave)

`ByteLineIndex.javaIndex(span)` compiles exact source-relative line-start byte
coordinates with two bounded CPU passes, one `int[]` metadata directory, and
binary-search `lineAtOffset(byteOffset)` queries. `line(ordinal)` returns the
exact immutable `SourceArtifact.Span`, **including** its LF terminator, while
`content(ordinal)` excludes only LF and intentionally retains CR. An empty
source has one empty line; an LF-terminated source has a trailing empty line.
All indexes are source-byte coordinates; invalid UTF-8 and surrogate/CRLF
semantics are never rewritten or reinterpreted. A million LF boundaries is the
hard precomputation cap (4 MiB metadata upper bound plus headers).

`ByteLineIndex.nativeIndex(span)` is an explicit separately qualified variant:
materialize one temporary direct buffer from the immutable source range, then
use a new optional JNI `LfCount.lineEnds0()` symbol to count and emit line
boundaries in bounded C11 stack chunks. The old `abi0()` version-1 and
`countLf0()` semantics remain unchanged; an installed legacy native image
without the optional symbol refuses rather than fabricating positions. The
native caller never retains a ByteBuffer address or Java array pin. Neither
path creates a new source payload owner or scheduler, and no native speedup is
claimed without a setup-inclusive measurement.

The second recipe is developed and sealed in Synexia as
`m3-cpu-byte-line-index-20261015`. Its `ABSENT` and exact original SHA-256
preimages are an independently reviewable additive follow-up on the initial
capability-runtime recipe. `ByteLineIndexChecks` is run in *both* Java and JNI
proof lanes, including binary fixtures, CRLF, subranges, empty/trailing lines,
randomized offset oracles, memory-budget refusal, read-only direct buffers,
virtual threads, and source/line immutable identity.

## Local focused proof (JDK 21)

```sh
mvn -B -ntp -f m3/ports/capability-runtime/pom.xml verify
python3 m3/ports/capability-runtime/verify_native.py \
  --root m3/ports/capability-runtime --out /tmp/m3-capability-proof-fresh
```

On Windows without a C toolchain, run `--java-only --out C:/.../new-proof-dir`.
The standalone Python verifier invokes real Java 21 compiler with `-Xlint:all,-module
-Werror`, then `-Xcheck:jni`, C11 `-Wall -Wextra -Werror -fsanitize=undefined`,
actual Java/JNI differential tests, read-only direct buffers, source SHA pins and
source-drift checks. `--out` must be a **new** directory outside the owner root.
Run this in a JDK21 image; a regular stock Java21 runtime is enough for the
independent JPMS module, but NOT proof of integration into modified HotSpot.

For an individually linked JMOD after admission:

```sh
jmod create --class-path path/to/classes --module-version 0.1.0 path/to/com.m3.runtime.capability.jmod
jmod describe path/to/com.m3.runtime.capability.jmod
```

For the focused, **stock-JDK21 developer image** and actual compiler/API smoke,
run `verify_native.py --developer-image` with a fresh `--out`. This compiles
*production Java only* into a versioned target-owned JMOD, verifies
`jmod describe`, `jlink`s a developer image with javac/jmod/jlink/jpackage/jdeps,
then compiles and runs `CapabilityImageSmoke.java` using that image's own
`javac`/`java`. The local stock-image lane passes; this is **not** a matched
M3 HotSpot image or comprehensive M3 String/jtreg certification.

## Recipe-first & provenance

Synexia canonical source and OpenRewrite implementation lives in
`hsoliwal/com.synexia/synexia-openrewrite-recipes`, crate
`m3-cpu-capability-runtime-20261015`. It uses existing
`M3HashPinnedJavaSnapshotRecipe` for target Java/module/test source and
`M3HashPinnedTextSnapshotRecipe` for native C. Targets must start ABSENT, exact
Java/C template SHAs match output, second SDK pass reaches fixed point and an
unknown occupied target refuses. A later improvement **changes/retests the recipe**,
not hand-edits a compiled product output. GitHub donor comparison and problem-family
crosswalk live with the recipe.

Original new code: Apache-2.0 (see `NOTICE.md`). Standard JDK ByteBuffer/SHA256
are reused; competitive-programming pages are pattern references only; no
TheAlgorithms or site solution text has been copied. OpenJDK GPL+Classpath
sources and standalone M3 String owners are untouched.

## Unqualified stages

- No full M3 String/regex/capture/JIT/GC/CDS/JFR/JVMTI/jtreg/runtime matrix.
- No experimental `UseM3StringStorage` activation claim.
- No MeshLLM node-local SQLite database or CPULLM continuation-state compiler.
- No GPU/NPU availability assumption or measured CPU/native throughput advantage.
- No `jlink` developer image or full Synexia/M3JDK reactor admission claim.


## P3: immutable byte-pattern search (2026-10-15)

`BytePatternSearch` adds explicit compile-once KMP lookup into existing
`SourceArtifact.Span` immutable source coordinates. It is an opt-in leaf for
CPULLM/FLLDCIM source-evidence lookup, **not** a replacement for M3 String,
`java.util.regex`, or Synexia's composite-aware `MIndexStringSearchPlan`.
It accepts 1..4,096 source **bytes**, returns overlapping sorted offsets
relative to the selected span, and refuses incomplete results above an
explicit 0..1,000,000 match cap. The byte search neither normalizes Unicode
nor interprets line endings: callers may combine it with `ByteLineIndex`
for source-backed line provenance.

The optimized Java plan stores a cloned needle plus a KMP prefix table;
matching makes two sequential passes to allocate an exact result `int[]`.
The **optional** JNI extension lives in the same SHA-pinned `LfCount` C11
image and independently recomputes the KMP failure table from one copied
bounded pattern. Native uses `GetDirectBufferAddress` within a synchronous
call, bounded stack scratch and fixed-size `SetIntArrayRegion` transfers;
it retains no JVM pointer and does not change `abi0()`, `countLf0()`, or
`lineEnds0()`. Native is caller-selected, never automatic. The JVM remains
CPU-first even when JNI is unavailable.

A synthetic independent naive oracle and 6,000 seeded binary/unicode/overlap
cases compare Java and C11 JNI results, rejection budgets and immutability.
The existing `verify_native.py` also compiles the new class, executes these
proofs and validates the JMOD developer image. Full M3JDK/HotSpot/jtreg,
actual OpenRewrite Maven SDK and setup-inclusive performance crossover remain
independent release gates.


## P4: bounded, proof-carrying byte evidence (2026-10-15)

`ByteEvidenceQuery` composes the **existing** `SourceArtifact.Span`, immutable
`ByteLineIndex` and compiled `BytePatternSearch` into an opt-in source-evidence
receipt. It does not copy source bytes per hit, invent text encodings or
implement an additional search algorithm. Each result owns **one `int[]` of
sorted byte offsets** and an immutable SHA-256 receipt binding original source
ID/root, exact span root/range, compiled pattern root, request limit, and every
matched offset. The query precomputes line boundaries once, and `firstLine`,
`lastLine`, `firstColumnByte`, and `lastColumnByteExclusive` then recover **byte**
coordinates by binary search. A match crossing a newline is represented as
one immutable `Span` plus start/end line coordinates; no semantic flattening.

`execute()` uses the existing CPU KMP reference; `executeNative()` reuses its
already pinned JNI library/ABI. `verify(result)` independently replays via Java,
refusing a result from another query owner. No extra C/JNI methods, native
buffers, registered services, Java String or HotSpot changes are introduced.
The native path is never selected automatically or claimed faster without a
setup-inclusive measurement. Only the existing `LfCount.install()` loads JNI.

The standalone `ByteEvidenceQueryChecks` uses 3,500 seeded source-range cases
and an independent naive matcher/byte-by-byte line oracle, plus binary UTF-8,
CRLF, cross-line, overlap, source/needle mutation, cancellation, concurrent
query reuse, exact receipt equality, wrong-owner refusal and budget boundaries.
`verify_native.py` invokes it in both Java-only and JNI modes and checks the
stock-JDK21 linked image's `CapabilityImageSmoke`. This is a testable JPMS/JMOD
consumer building block for CPULLM evidence retrieval; **integration with the
actual CPULLM/FLLDCIM resolver and full modified M3JDK HotSpot remains gated**.
