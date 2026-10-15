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
