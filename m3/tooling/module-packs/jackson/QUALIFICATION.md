# Jackson JMOD qualification envelope

This optional module pack has a bounded executable acceptance contract. A successful
result identifies the source-built Jackson inputs, the integration implementation,
the selected JDK, the linked module closure and the test processes actually run.
It does not promote the repository-wide String/array/collection/AST sequence.

## Executed local candidate proof, 2026-10-18

| Gate | Result |
| --- | --- |
| Four exact existing donor Maven builds | PASS, including FastDoubleParser shading and its Java 11/17/21 overlays |
| Offline standalone Maven entry point | PASS using a previously resolved build-tool cache |
| Jackson upstream source/provenance verification | PASS: 777 source/build/legal files and 753 upstream provenance entries |
| Existing module graph selection and JMOD assembly | PASS: four runtime modules; five with the compiler bridge |
| Host Jackson compatibility corpus | PASS: 299 checks against the existing ObjectMapper |
| Linked-image Jackson compatibility corpus | PASS: the same 299 checks |
| Host indexed-file contract | PASS: 20,595 checks |
| Linked-image indexed-file contract | PASS: the same 20,595 checks |
| Actual M3 compiler source closure | PASS: M3AST, M3ASTPC, M3PrimitiveImageCompiler and JavaCompiler compiled together |
| Host and linked M3AST callback contract | PASS in both: one analyzed declaration, one projection computation, one projection hit |
| Full custom M3VM storage execution | NOT AVAILABLE on the selected stock Temurin 21.0.8+9 |

The M3File corpus covers adaptive-width thresholds, equal-hash unequal strings,
empty occurrences, UTF-16 and surrogate boundaries, slices, closed-reader behavior,
mark/reset/skip semantics, caller sequence mutation, randomized StringReader
differential execution and logical-length overflow without allocating a 2 GB string.
The Jackson corpus covers split input, malformed JSON, duplicate-key settings,
trailing-token settings, annotations, records, private properties, generic types,
custom serializers/deserializers, decimal precision, configuration copies, prepared
binding concurrency, physical files and UTF-16 BOM detection.

These candidate results qualify the implementation design. The canonical recipe
application must separately generate the receiver files, verify all postimages,
reach a complete second-pass fixed point and retain its SHA-256 application receipt.
The shared receiver workflow rebuilds those generated files and records its actual
commit in a separate CI receipt. Neither a candidate proof nor a queued workflow is
substituted for successful execution on the reviewed receiver commit.

## Reproduce the receiver proof

Run the commands in README.md and INTEGRATION.md with fresh output directories.
The runtime entry point emits SOURCE_RECEIPT.json, SOURCE_BUILD.json and RESULT.json.
The optional compiler entry point requires a successful runtime result and verifies
all four input JAR hashes before accepting that source-built runtime closure.
It emits COMPILER_RECEIPT.json with those hashes, the runtime result hash and the
four actual compiler source hashes. Both entry points reject failed or empty test
execution instead of deriving success from compilation alone.

The compiler proof patches the four actual M3 compiler source owners into the
selected JDK for qualification. The compiler JMOD contains the bridge, not that
patch. Production use requires the actual M3JDK compiler API. The runtime JSON JMOD
does not require the compiler bridge.

## Independent promotion gates

Run the runtime pack with a qualified M3JDK and `--require-m3-runtime` to require
successful host and linked-image contracts with UseM3StringStorage enabled and
disabled. Without that flag, a stock JDK result records this lane as unavailable.
The implementation adds no JNI entry points, but this fact does not establish the
underlying M3VM's GC, lifetime, native-memory or ABI qualification.

The full Jackson upstream suite, full OpenJDK/jtreg matrix and CPU, retained-heap
and process/native-memory measurements remain independent gates. No throughput,
allocation reduction or full-family completion claim follows from this corpus.
