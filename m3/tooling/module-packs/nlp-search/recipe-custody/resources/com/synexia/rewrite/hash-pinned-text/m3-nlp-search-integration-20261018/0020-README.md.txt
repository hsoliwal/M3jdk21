<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0 -->
# M3File, Lucene and OpenNLP module pack

This receiver builds ten JMODs from the repository's complete library sources and
connects them to the existing M3JDK compiler frontend. Java source enters through
the complete M3File port, javac supplies the real M3AST events and sealed source
identity, OpenNLP supplies token spans, and Lucene writes and searches its normal
disk index. No Synexia runtime classpath, service, repository checkout or package
download is required by the resulting modules.

The canonical source owner is
`hsoliwal/com.synexia:synexia-openrewrite-recipes/crates/m3-nlp-search`.
The `integration-20261018` candidates are applied through the existing
`com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe`. The M3JDK receiver is a
hash-pinned recipe output. Make source improvements in that canonical recipe
owner and regenerate the receiver.

## Modules and source scope

| JMOD / module | Source implementation |
| --- | --- |
| `org.apache.lucene.core` | Complete Lucene core 9.10.0 and its existing multi-release implementations, with three bounded M3 fork backports |
| `org.apache.lucene.analysis.common` | Complete Lucene analysis-common 9.10.0 |
| `org.apache.lucene.queries` | Complete Lucene queries 9.10.0 |
| `org.apache.lucene.sandbox` | Complete Lucene sandbox 9.10.0 |
| `org.apache.lucene.queryparser` | Complete Lucene queryparser 9.10.0 |
| `org.apache.opennlp.tools` | Complete OpenNLP Tools 2.4.0, the existing named-module descriptor, and eight bounded fork overlay sources |
| `org.slf4j` | Existing SLF4J API 2.0.12 |
| `org.slf4j.simple` | Existing SLF4J Simple 2.0.13 |
| `org.m3jdk.file` | Complete 50-file MIndexFile donor family, 29 supporting Java sources and one module descriptor: 80 Java sources in total, plus three C workers |
| `org.m3jdk.tools.search` | M3AST consumer and CLI in `org.m3jdk.tools.search.M3SourceIndex` |

The file module exports `com.m3.file` and `com.m3.file.json`. Its source is under
[`m3/ports/file`](../../../ports/file). It retains the full mapped file family:
token stores, lexicons, kinds, frequencies, patterns, event streams, byte facts,
source facts, snapshots, mapped images, rolling indexes, token-range digests and
universe images. The supporting closure includes JSON structure, pageable trees,
progress, native loading and text descriptor operations.

[`FILE_PORT_MAP.json`](FILE_PORT_MAP.json) maps all 50 donor file-family sources.
[`FILE_API_PARITY.json`](FILE_API_PARITY.json) accounts for **709 public/protected
source declarations and record components** through explicit owner and package
mappings. This is a source API inventory; the separate executable contracts check
behavior, durable formats, ownership and error handling. Existing binary magic,
schema and domain constants remain intact, including historical MIndex names in
formats and original proof markers. Durable images retain their exact dictionary
coordinate/fingerprint rules and refuse live overlay IDs.

The Lucene backports add the fork's borrowed `CharSequence` reader and its public
`M3TextReaders.reader` entry point, and use the actual `String.hashCode()` for the
case-sensitive String lookup path in `CharArrayMap`. Legacy String reader
semantics, generic CharSequence lookup and case folding are preserved. The
OpenNLP overlays add the fork's `M3Tokenizers.simpleSpans` bridge and additive
wordpiece/normalization APIs while retaining the existing Tools 2.4 public APIs.
Their source bodies and compatibility adaptations are listed in
[`LUCENE_PROVENANCE.json`](LUCENE_PROVENANCE.json) and
[`OPENNLP_PROVENANCE.json`](OPENNLP_PROVENANCE.json).

The library baselines remain **Lucene 9.10.0 and OpenNLP Tools 2.4.0**. This pack
applies the specifically recorded compatible fork changes. A full fork-major
upgrade, the complete Lucene distribution and the OpenNLP 3 distribution have
separate source and Java-version requirements outside this receiver.

## String authority and callback ownership

Public `java.lang.String` is the sole admitted text-payload authority. The
matched M3JDK supplies its package-private `java.lang.M3String` implementation
behind that public boundary. The file module does not expose or replace that
internal class.

`M3File.asM3String()` returns an immutable `M3FileText` descriptor implementing
`CharSequence`. It retains resolver identity, packed atom IDs and composition or
metric metadata. Character payload remains in the resolver's ordinary public
Strings. Reads, hashes and encoders traverse those owners; explicit
`materialize()` / `asString()` / byte exports publish the representation required
at an API boundary. The resolver and its immutable existing ID bindings must
remain valid for the descriptor's lifetime.

The compiler's existing retained-source hook publishes the public String required
to seal the exact characters handed to javac. `M3AST.Source.utf16Sha256()` hashes
big-endian UTF-16 code units, including unpaired surrogates. It identifies source
characters; classpath and attributed-type equivalence require their own context.

`M3SourceIndex` reads the actual callback `Unit`. Its projection key belongs to
that consumer instance, and the compiler memoizes results only during the current
event. The consumer retains no `Unit`, compiler tree, `Trees`, `Element`, compiler
context or task. It indexes during `PARSED`; later analysis of several top-level
types does not duplicate the source document.

The caller owns the `IndexWriter`, analyzer configuration, M3File resolver and
commit policy. The consumer neither commits nor closes them. The Lucene text
field uses the writer's analyzer over `M3TextReaders.reader(file.asM3String())`.
The token field consumes actual OpenNLP spans and preserves token positions and
UTF-16 offsets. Lucene owns the resulting term buffers and UTF-8 index format.
The index stores URI, source hash, package/type names, source length and token
count; its text and token fields do not store another complete source payload.

## Build with an existing M3JDK

Run on Linux with Python 3, a C11 compiler, and a complete Java 21 JDK containing
`java`, `javac`, `javap`, `jar`, `jmod`, `jlink`, JNI headers, JMODs and `lib/ct.sym`.
For a normal build, supply an already built M3JDK with the actual M3AST API and
matching compiler hook:

```sh
python3 m3/tooling/module-packs/nlp-search/build.py \
  --jdk-home "$M3JDK_HOME" \
  --out build/m3-nlp-search
```

The output directory must be new. The build checks the sealed source and native
inventories, builds unchanged baseline libraries independently, compiles the full
candidate libraries, compiles both M3 modules, packages all ten JMODs and links a
runtime image. `org.apache.opennlp.tools` is compiled against the same canonical
descriptor used by the final packager. The linked image includes the compiler,
JDK tools, management support and `jdk.charsets` for its complete available
charset-provider set.

## Explicit compiler-source proof on a stock Java 21 VM

CI and local integration work can use a stock Java 21 JDK by requesting the
bounded compiler proof explicitly:

```sh
python3 m3/tooling/module-packs/nlp-search/build.py \
  --jdk-home "$JAVA_HOME" \
  --compiler-source-proof \
  --out build/m3-nlp-search-proof
```

This mode compiles the **five exact entries** in
[`COMPILER_SOURCES.json`](COMPILER_SOURCES.json): `M3AST`, `M3ASTPC`,
`M3PrimitiveImageCompiler`, the actual `JavaCompiler` with its `retainSource`
hook, and `jdk.compiler/module-info.java`. It rebuilds the compiler JMOD,
recomputes the affected recorded module hashes and validates the linked image.
It preserves the supplied JDK executables and copies `ct.sym` for javac release
support. Thus the integration uses the real compiler callback implementation and
module services.

The VM and `java.lang.String` remain the supplied stock implementation in this
mode. Compiling these five compiler inputs does not build HotSpot, activate
M3String storage or certify the complete M3JDK. Normal product builds use the
already built M3JDK and omit `--compiler-source-proof`.

## Run the source-search CLI

For the proof build above:

```sh
m3_image="$PWD/build/m3-nlp-search-proof/pack/image"
"$m3_image/bin/java" --list-modules

"$m3_image/bin/java" \
  -m org.m3jdk.tools.search/org.m3jdk.tools.search.M3SourceIndex \
  index build/m3-source-index \
  m3/ports/file/src/main/java/com/m3/file/M3File.java

"$m3_image/bin/java" \
  -m org.m3jdk.tools.search/org.m3jdk.tools.search.M3SourceIndex \
  search build/m3-source-index 'token'

"$m3_image/bin/java" \
  -m org.m3jdk.tools.search/org.m3jdk.tools.search.M3SourceIndex \
  tokens build/m3-source-index 'class M3File'

"$m3_image/bin/java" \
  -m org.apache.opennlp.tools/opennlp.tools.cmdline.CLI
```

`index` snapshots physical UTF-8 inputs through M3File, parses with the existing
M3AST hook and updates documents by normalized source URI. Annotation processing
and class generation are disabled for this command. Only an explicit successful
commit publishes the batch; syntax errors preserve the previous commit.
`search` accepts Lucene query syntax with the same standard analyzer used by the
CLI indexer. `tokens` is an exact case-sensitive phrase under OpenNLP's current
simple-tokenizer configuration. Search reopens the disk index and prints score,
source URI and UTF-16 hash for up to twenty hits.

Applications can use `M3SourceIndex.source(Path, resolver, languageId)` to supply
the M3File-backed JavaFileObject to their own `JavacTask`, then register a
`M3SourceIndex` with `M3AST.open(task, true)` before parsing. Keep the writer and
resolver valid through callbacks and apply the application's own transaction
policy. Do not change the shared OpenNLP tokenizer configuration concurrently
with a scan. Cooperative checkpoints and thread interruption can cancel work;
the caller remains responsible for its transaction disposition.

## Executable proof and artifacts

The driver runs the existing frozen full-tools behavior test, OpenNLP CLI,
independent OpenNLP baseline comparisons, Lucene reader differential oracles,
real analyzer and disk-index contracts, all file contracts and the actual shared
compiler contracts. It accepts success only after each declared completion
marker and final module validation.

[`FILE_PROBES.json`](FILE_PROBES.json) runs all **sixteen** executable file
fixtures in fresh Java and required-JNI processes: thirty-two runs. Thirteen
fixtures reuse the donor proofs; the additional fixtures cover mapped/native
search and descriptor ownership, JSON/pageable/native metrics, and JDK charset
and custom-encoder behavior. The source API inventory covers all fifty donor
family files. The charset fixture exercises every available encodable provider;
the current full Temurin Java 21 reference has 171, with 6,840 randomized cases.
Provider counts are reported for the supplied image rather than treated as a
portable constant.

The Linux native library is packaged as `libm3file.so` in the file JMOD and linked
image. Required-JNI cases use the explicit `{image}/lib/libm3file.so` path, enable
file and text-metric dispatch, lower the dispatch threshold and run with
`-Xcheck:jni`. The mapped-file and JSON probes require the real native symbols and
compare their results against Java oracles. Missing native loading cannot qualify
as the Java fallback. Java cases use `-Dm3.file.native=false`. These three C
workers are file/JSON/metric operations with synchronous borrowed-pointer
lifetimes; their proof is separate from native M3String VM-storage certification.

[`FRONTEND_PROBES.json`](FRONTEND_PROBES.json) runs source indexing in both modes.
It checks physical snapshot identity after a disk edit, malformed UTF-16 source
hashes, real parse and analysis events, per-instance projection ownership,
expired and foreign-thread callbacks, caller-owned writer lifetime, cancellation,
all indexed OpenNLP positions/offsets, index reopening, URI updates and failed
CLI-batch rollback. The unchanged fixture's qualified completion records 976
assertions, 57 token postings and 40 refused expired accesses.

Reader baselines and candidates run on the **same final image with identical VM
options**. The baseline process uses `--limit-modules java.se,jdk.unsupported`
and only unchanged library jars on its classpath. Both use
`-XX:-OmitStackTraceInFastThrow`; exact exception messages, return values and
buffer effects remain part of comparison. `jlink --strip-debug` changes helpful
NPE variable names, so digests from stripped and unstripped JDKs are qualified
separately. The OpenNLP baseline uses an independent class loader with a null
parent and checks that it did not load the candidate class.

The build's `RESULT.json`, `COMMANDS.json`, per-stage logs, compiler-proof receipt
and compiler-contract receipt are the evidence for a particular run. JMODs are
under `pack/jmods`; the executable runtime is under `pack/image`. Additional
`--runtime-java-arg` options apply to the primary candidate launches and matching
reader baseline. They are not a claim that every child JVM receives those
options: the OpenNLP diagnostic subprocess and shared-compiler verification
harness have separately recorded invocation scopes.

Global M3String/native-JDK certification remains separately gated. A receiver
PASS does not imply a complete M3JDK configure/make/jtreg run, complete upstream
library test suites, other-platform JNI qualification or representative
performance results. This receiver does not advance the global String-runtime
acceptance phase.

## Source custody and notices

[`SOURCE_REF.json`](SOURCE_REF.json) records the known source identities:

| Owner | Exact source revision |
| --- | --- |
| M3JDK21 compiler/receiver base | `f29b0afe7ad253cc0562bc8dfdd48fc1acec710e` |
| Complete Synexia file family and mapped supporting donors | `50f48cbbe11f17fb8dc209bac3812f8940a1e267` |
| hsoliwal/lucene fork backports | `d1b7235f1ba1c1eea5b04a5b7e6554595a90719e` |
| hsoliwal/opennlp fork backports | `368337311ad94b665fd178f8d3ced005a73b2370` |
| Original OpenNLP Tools 2.4 source vendor | `8c586ddebd84db4d66582aab3a885bf02c75e41f` |

The source-build owner under `m3/vendor/synexia/nlp-search` retains the original
full source/resource lock and the executed-overlay lock. `INPUTS.json` seals
receiver inputs and complete source inventories. Exact API, donor, native and
recipe receipts remain authoritative; this documentation does not substitute
for an executed source-custody or runtime proof.

First-party integration and mapped Apache-licensed Synexia work retain their
Apache-2.0 notices. Original Apache and embedded Lucene/OpenNLP notices, SLF4J MIT
terms and the linked JDK's original licenses remain with their source and
resources. See [`LICENSE.txt`](LICENSE.txt), [`NOTICE.txt`](NOTICE.txt), and each
library's retained `META-INF` legal resources. No blanket relicensing applies to
the linked runtime.
