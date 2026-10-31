# Current OpenNLP source JMOD pack

This pack compiles the current `hsoliwal/opennlp` source implementation for Java 21 and
passes its class files to M3JDK21's existing `jmod_pack.py`. It does not substitute the
older OpenNLP 2.4 distribution and does not download an OpenNLP implementation JAR.

The donor baseline is commit `368337311ad94b665fd178f8d3ced005a73b2370`, version
`3.0.0-SNAPSHOT`. `source-lock.json` pins every production Java source and resource in
the inventory, the relevant Maven POMs, and the original LICENSE and NOTICE. A changed
or omitted file refuses the build until its canonical recipe and qualification are
updated. `dependency-lock.json` independently pins all 14 external dependency JARs.

## Source and module coverage

| Source module | Java sources | Coverage |
| --- | ---: | --- |
| `org.apache.opennlp.tools` | 923 | API, runtime, CLI, formats, model resolver, all five classic ML implementations, tools |
| `org.apache.opennlp.morfologik` | 10 | Dictionary compilation, tagging and lemmatization extension |
| `org.apache.opennlp.spellcheck` | 23 | Complete spellcheck extension |
| `org.apache.opennlp.uima` | 44 | Complete Java UIMA integration |

The core exports all 89 current packages. Its original Maven artifacts have split
packages, so they form one cohesive named module with the established
`org.apache.opennlp.tools` identity. The two zlibsvm artifacts also have split packages
and form one `zlibsvm.core` module. Every source resource and service configuration is
retained; the version resource receives the same project-version substitution as Maven.
Only JAR manifest packaging metadata is replaced by module descriptors.

The resulting pack contains four source modules and thirteen dependency modules.
Morfologik's FSA and stemming module descriptors are shared byte for byte with the
current Lucene pack. Dependency modules preserve their packages and their required
runtime closure. Original license, copyright and notice files are retained in JMOD
legal sections and in their class resources where originally supplied.

The 12 Java sources in `opennlp-dl` and the `opennlp-dl-gpu` alternative remain separate
optional distributions with their ONNX native dependencies. They are inventoried and
hash checked, and they are explicitly marked `OPTIONAL_SEPARATE_DISTRIBUTION` rather
than reported as built. The donor Maven definitions remain authoritative for them.

## Build and verify

Use Java 21 and, when resolving dependencies, Maven 3.9 or newer. Supply the existing
M3JDK21 packager from the same receiving source checkout used for the aggregate pack.

```sh
python3 dev/m3-jmod/build.py \
  --jdk-home "$JAVA_HOME" \
  --packager /path/to/M3jdk21/m3/tooling/jmod-recipes/tools/jmod_pack.py \
  --out /path/to/fresh/opennlp-pack
```

`--maven` and `--maven-settings` select a Maven executable and optional environment
settings. `--dependency-dir` uses already resolved JARs instead; each must match its
checked-in SHA-256. The output directory must be new. The build-control script only
compiles and packages sources; it never transforms donor source or generates module
descriptors.

The build writes:

* `BUILD.json`: exact source, resource, dependency and module coverage.
* `IMAGE.tsv` and `DESCRIPTORS.tsv`: inputs to the existing M3 packager.
* `pack/jmods`: all 17 named JMODs, with legal notices.
* `pack/image`: the independently linked and validated Java image.
* `jars/<module>.jar`: the exact JMOD class view as named JARs for downstream source
  compilers, including current Lucene's OpenNLP analysis module.
* Compilation, packaging and behavioral probe logs.

The standalone source-inventory check is:

```sh
python3 dev/m3-jmod/tests/test_inventory.py \
  --source-root . --templates .
```

The linked image launches probes from an unrelated temporary working directory with
`CLASSPATH` removed. They validate all built production classes and resource bytes;
custom and built-in provider discovery; actual tokenizer training, model save/reload
and probabilities; secure XML dictionaries; WordPiece offsets; immutable document
ownership; 2,000 composite-input model comparisons across both newline modes; actual
morfologik dictionary compilation and lemmatization; and a real UIMA analysis engine
whose annotation offsets match the original tokenizer. The core behavior digest is
recorded from all 923 unmodified donor sources compiled before applying the recipe.

The original Maven test reactor remains a separate required compatibility gate:

```sh
mvn -B -ntp \
  -pl opennlp-tools,opennlp-extensions/opennlp-morfologik,opennlp-extensions/opennlp-spellcheck,opennlp-extensions/opennlp-uima \
  -am -Dopennlp.forkCount=1 test
```

## Immutable source contracts

`Document.of(CharSequence)` retains its snapshot and null-handling behavior.
`Document.ofImmutableView(CharSequence)` instead retains the supplied sequence without
calling `toString()` or `subSequence()`. The caller must keep its contents and length
stable for the lifetime of all derived documents. `with()` and `merge()` retain the
same owner; merges compare UTF-16 contents directly. Layer immutability and duplicate
layer rules are unchanged.

`TokenizerME.tokenizePosImmutableView(CharSequence)` scans the supplied sequence and
returns original UTF-16 offsets. Its model feature generator and protected abbreviation
hooks receive the same per-whitespace-token String inputs as the original API. This
retains model decisions, newline behavior and per-thread probability publication.
The method does not call the source's `toString()` or `subSequence()`. Token features
still require String storage; a document consisting of one whitespace token can require
a token String as large as the document. The method makes no claim of allocation-free
model inference.

Existing `M3Tokenizers.simpleSpans(CharSequence)`,
`WordpieceEncoder.encodeImmutableView(CharSequence)` and
`SentenceDetectorME.sentPosDetect(CharSequence)` are reused. M3's receiving integration
keeps the source owner and maps these UTF-16 ranges into its existing string pieces and
AST ranges.

`Providers` retains its generic provider contract in JPMS by registering each caller's
SPI with its own module before invoking `ServiceLoader`. A separate named probe module
supplies an SPI that is unknown to the static OpenNLP descriptor, so this contract is
tested beyond the built-in stemmer provider.

## Recipe ownership and historical repair

These files are receiver outputs of canonical Maven/OpenRewrite recipes. Java
postimages use `M3HashPinnedJavaSnapshotRecipe`; build controls and locks use
`M3HashPinnedTextSnapshotRecipe`; every `module-info.java` uses the existing
`com.m3.rewrite.jmod.M3ModuleDescriptorRecipe`. The current-fork crate is
`m3-opennlp-jmod-current-20261018`. The M3 receiver's public SDK mirror supplies the
runner, recipe classes, resources and original preimage fixtures for replay without
a private checkout.

The SDK qualification checks first application, exact postimage hashes, a zero-change
second application, and refusal of source drift, duplicate input and missing required
preimages before exposing output. `--source-overrides` is available only as a
prepublication qualification input; the final receiver build uses installed recipe
outputs with the same source locks.

The baseline also contained six Checkstyle failures in the existing regex helper and
its test and had lost an isolated null-diagnostic test probe. The successor recipe
restores exact reviewed postimages from the existing
`cputools-qualification-20261015/opennlp` owner in Synexia PR #11556, head
`44b5edf35db510887632a2ff6d2ef0f4b49b12ac`. It does not suppress those checks or alter
the production tokenizer's null policy. The newer current WordPiece range test is
preserved because it no longer matches the historical recipe's preimage. All original
Apache attribution remains in the source templates and packaged legal files.

The full reactor also exposed a contradictory BPE test fixture in the untouched donor:
its reference algorithm and production implementation returned `ab, a` for `aba`, but
its next assertion expected `a, ba`. The corrected fixture uses `abab`, which places
both competing pairs before the end-of-word marker and therefore exercises the stated
last-duplicate-rank rule. All 3,000 seeded oracle cases still run; no BPE production
algorithm or assertion has been removed.
