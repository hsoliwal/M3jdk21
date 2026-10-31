# Current-fork NLP and search recipe qualification

This task extends the existing `crates/m3-nlp-search` owner with an isolated Maven proof for the current Lucene and OpenNLP source handoffs. The runtime sources belong to the receiving repositories. This task supplies build-time OpenRewrite recipes, source seals, and replay checks; it introduces no external Synexia runtime artifact.

## Build the SDK runner

Use a JDK 21 installation for Maven and the runner. From a repository root containing `synexia-openrewrite-recipes`:

```sh
mvn -B -ntp \
  -f synexia-openrewrite-recipes/crates/m3-nlp-search/current-forks-20261018/pom.xml \
  test org.apache.maven.plugins:maven-dependency-plugin:3.11.0:build-classpath \
  -Dmdep.outputFile=target/sdk-classpath.txt
```

The public M3 mirror uses the same relative structure below `m3/vendor/synexia`. Its POM has no private parent or repository dependency. It compiles five existing recipe owners and two task-local test classes against OpenRewrite 8.17.1. Dependencies resolve through Maven Central or the caller's configured Maven settings.

## Run a qualified snapshot

```sh
python3 synexia-openrewrite-recipes/crates/m3-nlp-search/current-forks-20261018/run_recipe.py \
  --kind text \
  --crate m3-lucene-jdk21-current-fork-20261018 \
  --resources synexia-openrewrite-recipes/src/main/resources \
  --input-root /path/to/pinned/lucene \
  --output-root /path/to/fresh/overlay \
  --jdk /path/to/jdk-21
```

`--kind java` invokes `M3HashPinnedJavaSnapshotRecipe`; `--kind text` invokes `M3HashPinnedTextSnapshotRecipe`. Both run through the OpenRewrite SDK with `InMemoryLargeSourceSet`. The Java runner supplies its validated dependency classpath to the existing parser-context hook. The resource root precedes built-in resources on the classpath so an explicitly supplied resource revision cannot be silently shadowed by an older build output.

Each sorted four-column manifest records the repository-relative path, exact before SHA-256 or `ABSENT`, exact after SHA-256, and a template basename beside the manifest. The existing maximum of 256 rows applies to each crate. Text snapshots normally reject Java sources. The Lucene source bootstrap admits Java text only for the exact crate name and reviewed full-manifest SHA-256 recorded in `SOURCE_PROVENANCE.json`. Changing even a manifest comment invalidates that bootstrap admission. The public mirror admits `.java.template` only as data inside that exact Lucene recipe resource directory.

The runner verifies every manifest target before returning a product overlay. It runs first application, exact fixed point, and scanner refusal checks for changed, duplicated, and missing required inputs. It rehashes all emitted targets, then writes a fresh overlay and `SDK_RECEIPT.json`. A manifest containing only additions has no required before-image to remove, so its receipt records zero missing-input refusal cases. A differing existing destination refuses before any output write.

## Run module descriptors

```sh
python3 synexia-openrewrite-recipes/crates/m3-nlp-search/current-forks-20261018/run_recipe.py \
  --kind descriptor \
  --crate synexia-openrewrite-recipes/crates/m3-nlp-search/current-forks-20261018/descriptors/DESCRIPTOR_MANIFEST.tsv \
  --resources synexia-openrewrite-recipes/src/main/resources \
  --input-root /path/to/receiver \
  --output-root /path/to/fresh/descriptor-overlay \
  --jdk /path/to/jdk-21
```

Descriptor mode invokes the existing `com.m3.rewrite.jmod.M3ModuleDescriptorRecipe`. Its six-column manifest names each module, artifact membership, unit-JAR membership, target, SHA-256, and generated descriptor file. Template basenames resolve relative to the manifest directory. The optional receiver modules are `jdk.m3.compiletime` and `jdk.m3.nlp.search`. The former retains the original eight package owners, provides the existing javac plugin, and declares the tensor-backend service it consumes.

## Ownership and restoration

The AST restoration crate contains exactly the 113 Java sources proved by public M3 PR #1192. Restoration accepts absence or an exact original postimage, and refuses any different existing owner. The two receiving `M3FileSourceFacts` owners then advance through a separate before/after recipe. Their new immutable-view digest uses the existing UTF-16 framing and domain, while the previous snapshot method remains available. The canonical Synexia owner has its own one-file successor recipe.

The six receiver Java additions use a narrow path admission and resolve to the existing `jdk.m3.nlp.search` module scope. The public SDK mirror preserves its three known receiving preimages explicitly. The older receiving Java snapshot and scope owners are subsets of the canonical owners used here; the mirrored successors retain their original paths and add the already qualified source layouts and parser-context support.

## Qualification boundary

The Maven suite verifies Lucene's 176-source first application and fixed point, required-input refusal, exact receiver scope, an adjacent unapproved receiver path, the exact public data directory, an adjacent data directory, a differently named Java-text crate, and a mutated same-name manifest. This suite qualifies recipe admission and replay. The donor and receiver builders separately compile and execute their actual JMOD images and semantic probes. Their receipts establish runtime qualification; this Maven suite alone does not claim those runtime results.

Source and template license headers are retained. The original source revisions, Git blobs, and manifest seals are recorded alongside this task. Public receiving builds use the mirrored SDK and publicly pinned donor sources, without a private repository checkout.
