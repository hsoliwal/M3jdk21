# Current Lucene fork as Java 21 JMODs

This optional build lane compiles the actual production sources from
[`hsoliwal/lucene`](https://github.com/hsoliwal/lucene) at
`d1b7235f1ba1c1eea5b04a5b7e6554595a90719e` into the original 29 named production
modules. It uses current source-built `org.apache.opennlp.tools` supplied by the
sibling OpenNLP lane. Maven resolves only the explicitly locked external
dependencies; it does not supply a published Lucene or OpenNLP replacement.

The donor's default Gradle/Java 25 build remains unchanged. Java 21 preview must
be enabled when compiling or running this lane because the real Lucene mmap
implementation uses the Java 21 preview Foreign Function and Memory API.

## Reproducible source ownership

`source-lock.json` seals every production `src/java` and `src/resources` input,
plus the original root and dependency license/notice files. Added, missing,
modified, or symlinked production inputs are refused. The source revision records
provenance; a later commit adding these controls does not require reverting HEAD.

The driver copies those exact inputs to a fresh output directory and invokes the
existing canonical Synexia Maven/OpenRewrite SDK. It never applies a Python
source transformation or writes donor production source. The SDK emits only the
176 reviewed postimages from
`m3-lucene-jdk21-current-fork-20261018`, with manifest SHA-256
`fd6b74ba45f3232ea5ff18679509039eff72d2e418bf28a4712d6f257452fbd9`.
The real SDK verifies first application, an exact fixed point, and refusal of
input drift, duplicate sources, and missing existing sources. The driver also
verifies every emitted and staged postimage hash.

The review-time `source-overlay/produce.py` creates the sealed recipe payload.
It is not part of build execution. The payload preserves original attribution,
comments, literals, package identity, and public API signatures. Its bounded
compatibility changes are:

- Give Java 22+ unnamed variables unique identifiers, skipping strings, text
  blocks, characters, and comments. A lexer fixture compiles on Java 21 without
  preview and checks unchanged noncode text and idempotence.
- Remove the unavailable `StackWalker.Option.DROP_METHOD_INFO`, retaining
  `RETAIN_CLASS_REFERENCE` where the original code uses it.
- Make the existing scalar vector implementation authoritative below Java 25,
  including when the incubating vector module is present. The donor's Java 25
  SIMD/native source lane remains in place and is not relabeled as Java 21 code.
- Express the sandbox record constructor and Foreign Memory calls with their
  equivalent Java 21 forms. Missing native-symbol errors retain the newer API's
  `NoSuchElementException` and message contract.
- Keep the actual current expression compiler, relocating the JDK ClassFile API
  imports to the qualified `io.github.dmlloyd:jdk-classfile-backport:25.1` module.
  Generated hidden classes explicitly use class-file version 21.

The named descriptors for dependencies that were automatic modules are emitted
by the existing `M3ModuleDescriptorRecipe`, with exact template hashes. They
retain each artifact's module name, all original class packages as exports, and
openness for reflection/resource access. The dependency classes and resources
are unchanged, using M3's existing release-21 multi-release JAR view. Existing
published named descriptors are retained.

## Build

Use an installed JDK 21 and the compiled canonical Maven/OpenRewrite runner. The
runner is owned by `synexia-openrewrite-recipes/crates/m3-nlp-search/` under
`current-forks-20261018/run_recipe.py`; its matching public M3 mirror may also be
used. Its Maven compilation and qualification must finish before invoking this
lane. The source recipe resource root must contain
`com/synexia/rewrite/hash-pinned-text/m3-lucene-jdk21-current-fork-20261018/`.

```sh
python3 .m3/current-jmods/build.py \
  --source /path/to/lucene \
  --jdk-home /path/to/jdk-21 \
  --out /fresh/output/lucene-current \
  --packager /path/to/M3jdk21/m3/tooling/jmod-recipes/tools/jmod_pack.py \
  --recipe-runner /path/to/canonical/current-forks-20261018/run_recipe.py \
  --recipe-resources /path/to/canonical/source-recipe/resources \
  --dependency-dir /path/to/hash-locked/external/jars \
  --opennlp-module /path/to/current/opennlp/jars/org.apache.opennlp.tools.jar \
  --opennlp-module /path/to/current/opennlp/jars/org.slf4j.jar
```

Repeat `--opennlp-module` for **every named module in the source-built OpenNLP
closure**, including its XML, ClassGraph, SVM, and selected extension dependencies.
The two arguments above illustrate the syntax, not the complete closure.
The driver refuses a missing tools module or automatic module. Shared dependencies
such as SLF4J and morfologik are reused only when their complete release-21
class/resource payload matches the locked dependency. A divergent implementation
with the same module name is refused.

Instead of `--dependency-dir`, use `--maven /path/to/mvn` and optionally
`--maven-settings /path/to/settings.xml`. The driver resolves `dependencies.xml`
through the pinned Maven dependency plugin and then verifies every exact SHA-256
in `dependency-lock.json`. Unrelated files in a supplied dependency directory are
never included. The current OpenNLP source module remains a required separate
input.

Child Java tools default to a 512 MiB maximum heap when `JAVA_TOOL_OPTIONS` is
not already configured. The canonical recipe runner retains its own explicit
768 MiB limit. The output directory must not exist. The build writes:

| Output | Meaning |
| --- | --- |
| `source/` | Complete sealed production inputs plus SDK-emitted Java 21 postimages |
| `source-overlay/SDK_RECEIPT.json` | Actual source SDK application, fixed point, and refusal checks |
| `dependency-descriptors/SDK_RECEIPT.json` | Actual canonical descriptor recipe evidence |
| `jars/` | All 29 real Lucene production modular JARs and complete named dependency closure |
| `JMOD_IMAGE.tsv` | Complete named-JAR input manifest for the existing M3 packager or aggregate merger |
| `pack/jmods/` | Real JMODs created by the existing M3 packager |
| `pack/image/` | Linked Java 21 image containing the complete supplied closure |
| `logs/` | Per-module compilation, packaging, recipe, and contract-test evidence |
| `BUILD_REPORT.json` | Success only after all module and linked-image tests pass; pins, counts, hashes, and qualifications |

JAR creation has a fixed timestamp. Original named external dependencies are
copied byte-for-byte. The existing JMOD packager owns JMOD and linked-image
metadata; compare normalized JMOD class/resource entries when checking repeated
payloads rather than claiming that its unmodified timestamps are reproducible.

## Runtime and contract tests

The authoring lexer fixture is also independently runnable:

```sh
M3_TEST_JDK=/path/to/jdk-21 python3 -m unittest discover \
  -s .m3/current-jmods/tests -p 'test_*.py'
```

The build executes the same three Java checks using the full named module path
and again using the linked JMOD image, with assertions and Java 21 preview enabled.

- `Jdk21ProviderCheck`: executes a real vector dot product with the incubating
  vector module present. The unmodified current donor fails its Java 25 runtime
  assertion under this setup; the qualified scalar fallback returns 11.
- `Jdk21ExpressionsCheck`: uses the real expression compiler for arithmetic,
  external variables, a custom MethodHandle function, IEEE double division,
  malformed syntax diagnostics, and invalid function signatures. Executing the
  generated hidden class also verifies its Java 21 target version.
- `M3LuceneJmodCheck`: creates and reopens an mmap-backed disk index, indexes
  borrowed immutable UTF-16 without `toString()` or `subSequence()`, checks stored
  fields and Unicode search, updates/deletes/commits, and verifies cloned-input
  lifetime. It compares token terms, positions, and UTF-16 offsets over valid
  Unicode, split surrogate pairs, and lone surrogates. It also checks mutable
  `CharArrayMap` lookup behavior, codec/tokenizer services, and actual Japanese
  and Korean dictionary resources through named module providers, plus ICU data
  and Polish/Ukrainian dictionaries (including the resource-only module).

The existing `M3TextReaders.reader(CharSequence)` remains the immutable borrowing
boundary. The caller retains the owner for the reader lifetime. Generic mutable
`CharSequence` and `char[]` APIs are preserved; this lane does not change their
observable conversion or mutation behavior. M3's receiver pack separately tests
the actual M3 String, M3File and retained M3AST integration against these modules.

This qualification is a complete source compilation and focused runtime contract
gate. The upstream Gradle 25 randomized suite remains a separate donor gate.
The Faiss Java binding is compiled, but its optional native library is not
bundled or executed. No throughput improvement is claimed from these checks.

## Dependency provenance and notices

`dependency-lock.json` records exact Maven coordinates, module identity, binary
hash, source-artifact URI, and descriptor-template hashes. Most dependency
versions are the pinned current donor catalog values. SLF4J is aligned with the
current OpenNLP closure at 2.0.20. Spatial4j's optional JTS, noggit, and Jackson
Java dependencies are included so its production classes retain their complete
Java dependency closure; Jackson uses the compatible 2.x line at 2.22.4.

The added ClassFile backport is pinned to source tag `25.1`, commit
`a35d959ac2f83c2473f6ca954deefbc5411a0696`, in
[SmallRye's source repository](https://github.com/smallrye/jdk-classfile-backport/tree/a35d959ac2f83c2473f6ca954deefbc5411a0696).
The source artifact hash and original `LICENSE.txt` / `COPYING.txt` are retained
under `legal/classfile-backport-25.1/`. This dependency is licensed under GPL 2
with the Classpath Exception. The original source attribution and dependency
notices are retained in the modular payloads and the JMOD legal notices.
