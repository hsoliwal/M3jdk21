<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3 file, text and compiler integration for Lucene and OpenNLP

This successor extends the existing full-source JMOD pack. It preserves Apache
library identities and the existing JMOD assembler. Canonical recipe ownership is
`hsoliwal/com.synexia`, at
`synexia-openrewrite-recipes/crates/m3-nlp-search/m3-source-jmods-20261018`.
The class-backed OpenRewrite recipe is
`com.synexia.rewrite.M3NlpSearchSourceJmodsRecipe`.

## Modules and source versions

| Module | Actual source and role |
| --- | --- |
| `org.apache.lucene.core` | Existing source-sealed 9.10.0; exact fork `M3TextReaders` and `ReusableStringReader` overlays |
| `org.apache.lucene.analysis.common` | Existing source-sealed 9.10.0 analyzers |
| `org.apache.lucene.queries` | Existing source-sealed 9.10.0 |
| `org.apache.lucene.sandbox` | Existing source-sealed 9.10.0 |
| `org.apache.lucene.queryparser` | Existing source-sealed 9.10.0 and previously qualified internal overlay |
| `org.apache.lucene.analysis.opennlp` | Complete production module from `hsoliwal/lucene@d1b7235f1ba1c1eea5b04a5b7e6554595a90719e`; compiled for Java 21 with original descriptor and services |
| `org.apache.opennlp.tools` | Existing complete 2.4.0 toolkit; exact fork `M3Tokenizers` and `SimpleTokenizer` overlays from `hsoliwal/opennlp@368337311ad94b665fd178f8d3ced005a73b2370` |
| `org.slf4j`, `org.slf4j.simple` | Existing source-sealed API 2.0.12 and Simple 2.0.13 |
| `jdk.m3.search` | M3File, source facts, borrowed readers, source-bound token spans and Lucene source documents |
| `jdk.m3.search.compiler` | Optional consumer of the actual `com.sun.source.util.M3AST` PARSED event |

The full current Lucene fork requires Java 25. The OpenNLP main 3.0 reactor
splits packages across artifacts and has a different dependency closure. Those
entire distributions are not silently substituted for this existing Java 21 pack.
Every additional module or API family requires its own source and compatibility
proof. The full old MIndexFile token/posting image and MIndexAST cache/codec
families remain under their existing canonical owners and migration ledger.

## Build and use

The following uses the actual M3JDK development image, including its existing
M3AST compiler API and matching JMODs. A fresh output directory is required.

```sh
python3 m3/tooling/module-packs/nlp-search/build.py \
  --jdk-home /path/to/m3jdk21 \
  --with-compiler \
  --out build/m3-source-search

build/m3-source-search/pack/image/bin/java --list-modules
build/m3-source-search/pack/image/bin/java \
  -m org.apache.opennlp.tools/opennlp.tools.cmdline.CLI
```

Omit `--with-compiler` to build ten JMODs with a standard Java 21 JDK. That build
still includes the full Apache libraries, their OpenNLP bridge, and `jdk.m3.search`.
The compiler option checks for the real M3AST class before creating build output;
it does not compile against a substitute API or invoke another parser.

JMODs are produced under `pack/jmods`. Run application code using the linked image
under `pack/image`; a JMOD is a link-time archive, not a runtime classpath JAR.
Source-built JARs are retained under `tools`; the named OpenNLP compilation view
is under `sdk-dependency/opennlp-tools.jar`. The linked image resolves the explicit
OpenNLP descriptor preserved by the existing packager.

```java
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import jdk.m3.search.M3File;
import jdk.m3.search.M3FileSourceFacts;
import jdk.m3.search.M3Search;

M3File source = M3File.read(Path.of("Example.java"), StandardCharsets.UTF_8, 8_000_000);
String context = M3FileSourceFacts.contextSha256(List.of("example-source-index", "v1"));
M3Search.Tokens tokens = M3Search.tokens(source);
// The application supplies and owns an ordinary Lucene IndexWriter.
writer.updateDocument(M3Search.sourceTerm(source, context), M3Search.document(source, context));
writer.commit();
```

For compiler capture, give javac the same frozen file and register the consumer
before compiling. The caller controls the compiler options and diagnostics.

```java
import com.sun.source.util.M3AST;
import jdk.m3.search.compiler.M3ASTSearch;

var projection = new M3ASTSearch(context, 1_000_000, List.of(source), null);
try (M3AST ast = M3AST.open(task, true)) {
    ast.addConsumer(projection.consumer(writer));
    task.call();
}
```

`context` above is an explicit index namespace. Reusable semantic metadata would
need a complete caller-supplied compiler/options/classpath/module-path/processor
and source-generation identity. This consumer indexes syntax observations only.

## Payload, offset and lifetime contracts

`java.lang.M3String` remains M3JDK's internal String owner. Its public ingress
remains `String` and `CharSequence`. The new modules neither import that
package-private implementation nor introduce another interner.

`M3File` ports the existing small Synexia compiler-source snapshot and retains
its contracts. Mutable character input is snapshotted once; an immutable String
is reused. File reading has an explicit byte budget and strict caller-selected
decoding. The SHA-256 of original file bytes is distinct from the domain-separated
UTF-16 source digest. A character-only source makes no byte-encoding claim.

Lucene's reader and OpenNLP's span scanner accept stable immutable composite
`CharSequence` views directly. They do not call `toString()` to flatten those
borrowed views. Inputs must remain stable until consumption finishes. File token
results retain the original M3File and immutable spans; token text is materialized
only when requested. Original Apache String APIs retain their original behavior.

The AST consumer uses the actual javac tree and source seal inside its PARSED
callback. An existing M3AST projection key shares capture among consumers of the
same event. An iterative, budgeted traversal emits primitive UTF-16 start/end
lanes with declaration kind/name metadata. Comments and text blocks containing
code-looking text are not declarations. No Tree, Unit, Element or task escapes
into the snapshot. Supplying frozen inputs preserves original-byte evidence only
when their String owner is identical to the source the parser actually consumed.

Full source text is analyzed once per document group. Declaration rows reuse
precomputed source metadata. A successful capture publishes its group through
Lucene's existing `IndexWriter.updateDocuments`; budget/cancellation failures
occur before that update. Source URI plus context defines the replacement key,
so a new generation replaces stale declaration rows in that namespace. Writer
commit, rollback, concurrency and durability remain the caller's responsibility.

The compiler's raw UTF-16BE digest is stored separately from M3File's framed
UTF-16 digest. Equal spelling or a digest alone never asserts equal attributed
facts across compiler contexts. `M3File` is a bounded compiler input, not a claim
to implement every operation of the larger legacy MIndexFile token/posting image.

## Reproducible qualification

The build source-checks the existing library and overlay locks, source-checks the
SDK/fixture lock, compiles every included library, retains legal resources, calls
the existing JMOD packager, validates the linked module graph, and runs:

- The unchanged full-tool behavior/CLI probe, including model training and disk search.
- File/source hash, strict-decoding, offset, snapshot and disk-index replacement checks.
- Real sentence/tokenizer training, model reload, direct OpenNLP versus Lucene bridge
  tokens/offsets/sentence identifiers, composite seams and SPI lookups.
- With the compiler option, actual M3AST capture, primitive coordinates, live-view
  refusal, projection reuse, bounded/cancelled capture, context separation and
  classfile parity with normal compilation.

`SOURCE_INTEGRATION.json` records the actual supplied JDK, source hashes, included
JMOD hashes and executed probes. Canonical recipe evidence also contains the
independent original-reader/original-tokenizer differential tests. Exception-message
comparison pins `-XX:-OmitStackTraceInFastThrow` so JVM fast-throw elision cannot
change the diagnostic oracle. SDK replay and refusal tests are separate from
runtime tests and must both pass.

These are selected-module proofs. Full custom M3JDK VM/String/HotSpot/JNI/GC/CDS,
the complete upstream suites, additional model families, other operating systems
and representative CPU/heap/native-memory performance remain separate gates.
The existing ordered String qualification stage does not advance from this pack.

## Source and license custody

The Apache library source trees remain unchanged. New fork source and generated
overlays carry exact source commits, SHA-256 and original notices. Existing
source-lock and overlay entries are preserved. The canonical successor recipe
owns the new manifests, templates, tests and source seals. `SOURCE_REF.json` and
`m3/docs/name-mapping.d/m3-nlp-source-jmods-20261018.json` record the handoff.
First-party integration is Apache-2.0 with Hitesh Soliwal and contributor
attribution. Apache, embedded third-party, SLF4J and OpenJDK rights remain scoped
to their actual source and payloads.
