<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# IndexDB persistence and V1 snapshot reference

[Guide](README.md) · [Semantic graph and fingerprints](indexdb-semantic-reference.md) · [Coverage](coverage.md)

## Scope and authority

This reference describes the complete public-repository bodies of `M3IndexDB`, `M3IndexDbArtifact` and package-private `M3IndexDbSemanticCodec`, with the six graph/fingerprint owners covered in the [companion reference](indexdb-semantic-reference.md#nine-owner-coverage-map). Source pin: `1dce82bfb7f1058d74f7503c4cd54fa7d431c032`, inspected 14 October 2026. All nine production blobs match the original `278b221e8365ebeb6095a3a5643b38a0564258b5` inventory.

This is a filesystem content store plus an in-memory semantic snapshot. There is no JDBC connection, SQL engine, transaction object, mmap coordinate table, incremental graph update or persistent query-index API in these bodies. Store handles retain paths and a closed flag; decoded graphs are detached Java values. Names such as “native”, “database” and “immutable refs” in older summaries do not add durability, transaction or lifetime guarantees to the implementation.

The public-target [porting invariant][policy], [naming map][names], [convergence model][convergence] and [receiver map][receiver] remain authoritative. The invariant classifies `m3/indexdb` as migration/proof residue of the reusable DB family, with M3JDK21 responsible for target acceptance. Keep the delivered `com.synexia.mindex.db` compatibility spelling. This documentation neither changes canonical ownership nor advances the ordered String → arrays → collections → AST/compiler → remaining-families receiving phases.

All builds, examples, repository tests, policy/recipe verifiers, benchmarks and runtime checks are **NOT_RUN** in this documentation pass. Source inspection and historical receipts are separate evidence.

## Build, schema and dependency boundary

The [module POM][pom] declares `com.m3:m3indexdb:0.1.0-SNAPSHOT`, Java release 21, UTF-8 source encoding and no parent POM. The [control reactor][reactor] lists `indexdb`; being a reactor child does not by itself establish POM inheritance. The complete module `src` tree contains nine production Java files and fourteen test-source files, with no `module-info.java`, package-info or external schema/resource file. The actual snapshot schema is implemented in the codec below.

Production imports are JDK IO/NIO, collections/streams, charset and security APIs. No database driver, network client or third-party runtime dependency is declared in this POM. Its test dependency is specifically `org.junit.jupiter:junit-jupiter:5.10.2-m3-source-1`, not the ordinary upstream `5.10.2` artifact. Dependency availability and the effective build have not been established here.

Declared build gates:

- Compiler plugin 3.13.0 uses `-Xlint:all -Werror`
- Surefire 3.2.5 disables the module path and fails if no tests are found
- JaCoCo 0.8.15 attaches its agent and, at `verify`, checks bundle LINE and BRANCH covered ratios against 0.99
- Optional `m3-synexia-atom-pattern` and `m3-synexia-atom-pattern-mastery` profiles use OpenRewrite 5.23.1 and the source-owned recipes artifact; they are authoring/verification dependencies, not dependencies of the production DB API

The retained [parse report][parse-report] gives `mvn -B -ntp -f m3/indexdb/pom.xml clean verify` as the module route. Resolve the pinned dependencies and use a suitable Java 21 toolchain before attempting it. That report and the later [atom report][atom-report] describe particular historical heads and mixed DB/recipe results; neither is refreshed into an exact-current-head pass by this guide. The [general build routes](../../m3/docs/native-build.md#current-checkout-verification-routes) separately cover the control reactor and OpenJDK image.

## Artifact values and validation

Source: [M3IndexDbArtifact][artifact].

The record carries `name`, `kind`, `formatVersion`, `contentSha256` and `payload`.

| Entry point / projection | Implemented contract |
| --- | --- |
| Canonical constructor | Requires non-null fields; name is nonblank, at most 512 Java UTF-16 units and NUL-free; kind has the same rules with a 64-unit maximum. Neither field is stripped. Version must be positive. SHA must be exactly 64 lowercase hexadecimal characters. Clones payload, hashes the clone and rejects a mismatch |
| `create(name, kind, formatVersion, payload)` | Clones the input, hashes it and invokes the validating constructor, which clones/hashes again. Empty payloads are allowed |
| `name()`, `kind()`, `formatVersion()`, `contentSha256()` | Generated record projections of validated metadata |
| `payload()` | Returns a fresh clone on every call |
| `payloadLength()` | Returns the array length widened to `long`; this is not support for a long-addressed payload |

Nulls produce `NullPointerException`; invalid text, version or hash/content agreement produces `IllegalArgumentException`. Missing SHA-256 support becomes `IllegalStateException` here. The record does not override generated `equals`, `hashCode` or `toString`: its byte-array component does not acquire deep array equality. Use an explicit digest/metadata comparison or byte comparison for the intended relation.

The content digest is SHA-256 over raw payload bytes. It differs from the semantic fingerprint's exact-source SHA, which hashes UTF-16 code units.

**Ref-format mismatch to account for:** artifact validation permits embedded CR/LF in otherwise valid names and kinds, but the ref writer uses unescaped lines. Such metadata can be accepted by creation/put and later fail the six-line reader. Name-to-path hashing also uses ordinary JDK UTF-8 encoding, not lossless raw UTF-16 framing. These are source-observed caller boundaries, not newly executed failures. For reliable use of this writer, supply single-line, well-formed text metadata; the API does not enforce that complete precondition.

## Store lifecycle and every public operation

Source: [M3IndexDB][store].

| Operation | Result, effects and failure boundary |
| --- | --- |
| `open(Path root)` | Rejects null, converts to absolute normalized path, and creates `blobs` then `refs` directories. Filesystem errors are `IOException`; a failure creating the second directory does not roll back the first. Normalization does not resolve real paths or establish a symlink/security sandbox |
| `root()` | Checks the handle is open, then returns its normalized root path |
| `putArtifact(name, kind, version, payload)` | Validates/snapshots an artifact, publishes or verifies its content-addressed blob, then publishes the named ref. Returns the artifact; metadata validation errors are not translated into `IOException` |
| `putSemanticIndex(name, index)` | Requires non-null index, encodes it, and delegates to put with kind `m3.semantic-index` and format version 1 |
| `containsArtifact(name)` | Checks only whether the hashed ref path is a regular file. It does not parse the ref or verify the blob, kind or version |
| `requireArtifact(name)` | Reads the complete ref and blob, verifies the envelope, length, hash and artifact metadata, then returns a detached artifact value; missing/corrupt data is rejected as described below |
| `requireSemanticIndex(name)` | Requires an artifact, checks semantic kind and outer version, then decodes it. A decoder `IllegalArgumentException` becomes `IOException("M3 semantic index payload is invalid", cause)` |
| `blobBytes()` | Sums sizes of immediate regular files under `blobs`, closing its listing stream. Does not recursively count directories or count refs; does not restrict names to `.blob`, so other regular files, including observable temporary files, count |
| `close()` | Sets the closed flag. Repeated calls are allowed. No data is deleted, no commit/flush happens and no shared connection is closed |

Every operation on an existing handle except `close` checks the open flag first; after close it throws `IllegalStateException` even if a later argument check would fail. Open-store lookup names are not routed through artifact text validation: null eventually fails during name encoding, while a non-null invalid artifact name can simply miss. No list/delete/GC/rename operation is exposed.

Put, require, contains, size and close methods synchronize on the individual handle. `root()` is unsynchronized and `closed` is not volatile. There is no common lock across separately opened handles or processes, no file locking and no caller-input concurrency protection. This is not a blanket thread-safety or concurrent-reader/writer guarantee. Treat input arrays, graph collections and mutable `CharSequence` inputs as stable while the relevant operation consumes them.

Closing a handle does not invalidate previously returned artifacts, graph snapshots or their immutable metadata. The caller owns the handle and chooses its lifetime; graph `store`/`load` adapters neither open nor close the supplied database.

## File layout, replacement and failure effects

For a supplied root, the paths are:

- `blobs/<sha256-of-payload-bytes>.blob`
- `refs/<sha256-of-name-UTF8-bytes>.ref`

The ref is UTF-8 text with these six lines and a final newline:

| Line | Content |
| ---: | --- |
| 1 | `M3INDEXDB-REF-1` |
| 2 | Original artifact name |
| 3 | Artifact kind |
| 4 | Decimal positive format version |
| 5 | Lowercase 64-character content SHA-256 |
| 6 | Decimal payload byte length |

A put first constructs a validated artifact. If the blob path is already a regular file, the implementation reads the entire existing blob and verifies its SHA against the expected digest. It does not do a byte-for-byte comparison with the new input. A bad existing digest fails before ref publication. Otherwise it publishes the new blob, then the ref.

Publication creates a temporary sibling file, writes the complete bytes and attempts a move with `ATOMIC_MOVE` and `REPLACE_EXISTING`. Only `AtomicMoveNotSupportedException` triggers a second move using `REPLACE_EXISTING` alone. Other move errors propagate. If publication has not completed, a `finally` block attempts to delete the temporary file; failure of that cleanup can itself propagate.

Calling put with an existing name can replace the ref with a different payload/kind/version. The earlier “immutable refs” wording is therefore not an immutable-name binding contract. Old blobs remain; no reachability reclamation runs.

Blob and ref publication are two separate operations. A ref failure can leave a newly published orphan blob; there is no multi-file rollback. A temporary-file/rename pattern does not establish crash durability: there is no explicit file/directory fsync, journal, recovery protocol, compare-and-set name update or multi-artifact transaction. Cross-process collisions and filesystem-specific replacement semantics require separate qualification.

### Reading and error translation

`requireArtifact` checks, in order:

1. Ref is a regular file
2. Exactly six decoded lines, correct magic and the requested name on line 2
3. Parseable integer version and long length
4. Lowercase 64-character SHA
5. Blob is a regular file
6. Entire blob length matches the declared length and its SHA matches the ref
7. Constructible artifact metadata, including positive version and valid kind

Structural, numeric, hash, missing-file and content failures are `IOException`. Numeric parsing errors and constructor `IllegalArgumentException` are retained as causes where wrapped. Filesystem read failures also propagate as IO errors. Integrity checking detects inconsistency; it does not authenticate an author or prevent a party able to replace both ref and blob from supplying a new consistent artifact.

The private size fold closes its owned stream before unwrapping a per-file `IOException` from `SizeFailure`. Other stream/provider runtime failures are not converted into that IO path. If a wrapped size failure and stream-close failure coexist, the code returns the original IO cause without copying suppressed exceptions off the wrapper; the private-atom test explicitly records this. Size summation uses ordinary long `sum()`, not checked overflow arithmetic.

## V1 semantic wire layout

Source: [M3IndexDbSemanticCodec][codec]. It is package-private; application entry points are `M3IndexDbSemanticIndex.encode()` and `decode(byte[])`. It has no public streaming encoder/decoder.

All numeric fields use Java data-stream big-endian order. SHA fields are 32 raw bytes. The kind is the unsigned interpretation of a one-byte enum ordinal, so the [kind declaration order](indexdb-semantic-reference.md#identity-records-and-kinds) is part of V1 compatibility.

| Region | Layout |
| --- | --- |
| 24-byte header | 8-byte magic `0x4d3349444253454d` (`M3IDBSEM`); 4-byte version 1; three 4-byte counts: strings, nodes, edges |
| String table | For each row: signed 4-byte byte length, then that many UTF-8 bytes |
| Each node: 173 bytes | Node SHA (32); kind (1); semanticKey/sourcePath/symbol/patternRole string IDs (4 × 4); exact/structural/logic SHA (3 × 32); structural64/logic64/sim64 (3 × 8); normalizedComposition string ID (4) |
| Each edge: 16 bytes | Parent node ordinal, child node ordinal, role string ID, producer/canonical edge ordinal (4 × 4) |

Encoding collects all node text fields, fingerprint normalized text and edge roles in a Java-sorted unique `TreeSet`; IDs index that sorted string table. Node ordinals follow the graph's node-ID-sorted list. Edge rows follow the graph's [composition ordering](indexdb-semantic-reference.md#graph-construction-order-and-recomposition). The private edge emitter validates that both endpoints exist before emitting any edge bytes.

### Decoder admission and corruption boundaries

The decoder consumes the caller's array directly without cloning or modifying it. Do not mutate it concurrently.

- Magic and version must match exactly; no migration or unknown-version fallback exists
- Each row count must be 0 through 50,000,000 inclusive
- Before count-sized list allocation, `4 × stringCount + 173 × nodeCount + 16 × edgeCount` must fit in remaining bytes; multiplication and addition use long arithmetic
- Each string length must be 0 through 16,777,216 bytes inclusive and no greater than the remaining input at that point
- A fresh UTF-8 decoder per string uses REPORT for malformed/unmappable input
- Node kinds, every string ID and both edge node ordinals are range checked; producer ordinals must be nonnegative
- Node/edge/fingerprint constructors validate their fields; `Index.of` then rejects conflicting identities, dangling endpoints, ordered-ordinal conflicts and cycles
- No trailing bytes are accepted

EOF becomes `IllegalArgumentException` with the truncation cause. Other IO decoding failures, including character-coding errors, become `IllegalArgumentException` with their cause. Semantic validation errors remain `IllegalArgumentException`. The store adds its IO wrapper on top. Null payload is `NullPointerException`, not a corrupt-frame exception.

Admission is not a whole-frame canonicality or authenticity check. The decoder does not require a unique/sorted/fully used string table, reject every reorder or compare re-encoded bytes to the original. Equal duplicate node/edge rows may collapse through graph construction. Parents are recomposed from children, so serialized parent fingerprints can be replaced rather than independently checked against their stored values. Leaf fingerprints are accepted after shape validation; they are not recomputed from source that the frame does not contain. There is no standalone codec checksum; the filesystem artifact adds an outer payload digest.

The encoder uses ordinary `String.getBytes(UTF_8)`, while the decoder rejects malformed UTF-8. Unpaired UTF-16 surrogates in accepted Java strings can therefore be replaced on encoding; all Java-string values are not guaranteed a lossless round trip. Do not confuse strict wire-byte decoding with a validating UTF-16 encoder.

### Size and resource limits

The decoder's constants and frame minimum protect against impossible declared capacities; they are not a 50-million-row capacity promise or a process-memory quota. String bodies, node/edge objects, maps, sorting, copies and child-first recomposition require additional memory. The encoder and in-memory `of` factory do not enforce the decoder's row/string limits, so constructing/encoding a value does not by itself prove the resulting bytes are admissible.

Everything is fully materialized in Java arrays/collections. Artifact creation and access clone payloads; store reads use `readAllLines` and `readAllBytes`; encoding builds a byte-array output stream and returns a byte array. There is no configurable total memory/disk budget, paging, cancellation, deadline or streaming backpressure. Large or deeply nested input can fail through allocation or recursion limits without becoming a checked corruption result.

## Test-source evidence and remaining gates

All fourteen test-source bodies were read: twelve JUnit classes with 69 declared `@Test` methods, plus `DbProbe` and `DbDigest` helpers. This is a source count, not a passing-test count. Several tests contain loops and some assertions exercise private helpers by reflection. Two null-element assertions in `M3IndexDbSemanticFailClosedTest` use `List.of(null)`, which can throw before the target method; the compatibility boundary test uses null-containing `Arrays.asList` to reach its factory.

| Read test owner | Concrete assertion scope |
| --- | --- |
| [M3IndexDBTest][test-store] | Content-hash deduplication, named retrieval, missing artifact and selected close refusals |
| [M3IndexDbArtifactTest][test-artifact] | Defensive array copies, metadata limits and mismatched digest refusal |
| [M3IndexDBIntegrityTest][test-integrity] | Metadata/null validation, all store close paths, existing-blob corruption, ref fields, missing/mismatched blob and semantic kind/version/payload |
| [M3IndexDbStoreBoundaryTest][test-store-boundary] | Empty/closed store, occupied root, all ref fields, missing/corrupt blob, IO cause, failed ref publication and temporary-file cleanup |
| [M3IndexDbCodecBoundaryTest][test-codec] | Every truncated prefix of a fixture, counts/version/text lengths, kind/string/node IDs, self-edge, trailing bytes and byte-stable canonical fixtures |
| [DbParseTest][test-parse] and [DbProbe][test-probe] | Impossible counts in 32 MiB child JVMs, row minima, hostile UTF-8, valid Unicode/empty/NUL paths, input nonmutation, 173-byte node size, IO cause chain and tie ordering |
| [DbAtomTest][test-atom] and [DbDigest][test-digest] | Private stream ownership/fault translation, ZIP/filesystem move fallback, failed fallback, pre-emission endpoint checks, interned strings, SHA widths, short-read fault and isolated missing-digest provider |
| [Semantic graph, fingerprint and compatibility tests](indexdb-semantic-reference.md#semantic-test-evidence) | The five remaining JUnit classes, including actual fixture/corpus boundaries |

Unexecuted and incompletely qualified surfaces remain explicit: cross-handle/process races, crash/power-loss durability, symlink/security policy, ref CR/LF round trips, unpaired-surrogate encoder behavior, hostile but structurally plausible large frames, deep DAGs, exact resource costs, caller/analyzer correctness, migration and complete runtime/platform acceptance. No source/config/workflow changes are made to address them in this documentation increment.

[store]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/main/java/com/m3/indexdb/M3IndexDB.java
[artifact]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/main/java/com/m3/indexdb/M3IndexDbArtifact.java
[codec]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/main/java/com/m3/indexdb/M3IndexDbSemanticCodec.java
[pom]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/pom.xml
[reactor]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/pom.xml
[policy]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/docs/m3jdk21-porting-invariant.md
[names]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/docs/name-mapping.json
[convergence]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/docs/SYNEXIA_CONVERGENCE_MODEL.md
[receiver]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/synexia-import/m3index-family-receiver.tsv
[parse-report]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/tasks/parse/FINAL_REPORT.md
[atom-report]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/tasks/atoms/FINAL_REPORT.md
[test-store]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/test/java/com/m3/indexdb/M3IndexDBTest.java
[test-artifact]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/test/java/com/m3/indexdb/M3IndexDbArtifactTest.java
[test-integrity]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/test/java/com/m3/indexdb/M3IndexDBIntegrityTest.java
[test-store-boundary]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/test/java/com/m3/indexdb/M3IndexDbStoreBoundaryTest.java
[test-codec]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/test/java/com/m3/indexdb/M3IndexDbCodecBoundaryTest.java
[test-parse]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/test/java/com/m3/indexdb/DbParseTest.java
[test-probe]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/test/java/com/m3/indexdb/DbProbe.java
[test-atom]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/test/java/com/m3/indexdb/DbAtomTest.java
[test-digest]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/indexdb/src/test/java/com/m3/indexdb/DbDigest.java
