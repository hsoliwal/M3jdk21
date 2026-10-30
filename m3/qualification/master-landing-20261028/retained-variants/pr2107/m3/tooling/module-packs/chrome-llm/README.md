<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK Chrome LLM and summary module

`jdk.m3.llm` receives existing Synexia summarization and Chrome candidate machinery
as an M3JDK-owned SDK module. Its sole Java runtime dependency is `java.base`.
The exported packages are `jdk.m3.llm.chrome`, `jdk.m3.llm.summary`,
`jdk.m3.llm.facets` and `jdk.m3.llm.document`; shared implementation helpers in
`jdk.m3.llm.internal` remain unexported.

## Existing implementations carried forward

- Chrome's mechanical evidence extraction, candidate protocol, omission repair,
  conflict-preserving consolidation, content checkpoints and bounded summary tree.
- Deterministic hierarchical extraction using Luhn, SumBasic, Edmundson, LexRank
  and query relevance, with reciprocal-rank fusion and preserved evidence citations.
- Facet projection over immutable byte-source slices, required facets, explicit
  budgets, conflicts, mandatory evidence and bounded exact-coverage repair.
- DESM document structure and lexical projection over the same source spans.

CPU evidence and model-produced candidate summaries retain distinct identities and
authority. Chrome inference is invoked explicitly through provider ports and the
browser client resources. The deterministic provider used by regression fixtures
does not establish live Chrome or model execution. Browser-specific capability,
model-download and user-activation requirements are surfaced by the browser client.

The runtime is a receiving product. Canonical transformation ownership is
`com.synexia.m3jdk.ChromeSummary20261029`, in Synexia's
`synexia-openrewrite-recipes/recipe-crates/m3-chrome-summary-20261029`.
Corrections are applied by improving and replaying that recipe. `SOURCE_REF.json`,
the canonical target naming map and the receiving receipt record exact lineage and
executed gates. This module does not advance the separate ordered qualification
of String, arrays, collections and other standard JDK internals.

## Progress-aware entry point

`Summaries.hierarchical(records, query, options, monitor)` delegates to the
existing summary engine and returns the same schema and roots. `Options` is an
immutable record with the original numeric budget minima. `Method.scoreKey()`
names the five existing score maps; every method still participates in the same
fused ranking. A null monitor means `ProgressMonitor.NONE`.

The caller owns its input snapshots and monitor. The facade credits two phase
units and checks cancellation around validation, computation and callbacks; it
never begins or completes the borrowed monitor. The underlying algorithm remains
unchanged, so boundary cancellation does not promise mid-computation interruption.

## Build the named module and a local image

Use an explicit Java 21 development image. An M3 progress-aware facade requires
that image to provide `java.util.concurrent.ProgressMonitor` and `ProgressMonitors`.
The product POM has no library dependencies or Synexia recipe profiles.

```sh
JAVA_HOME=/path/to/m3jdk21 mvn -f m3/tooling/module-packs/chrome-llm/pom.xml test

python3 m3/tooling/module-packs/chrome-llm/build.py \
  --jdk-home /path/to/m3jdk21 \
  --out /new/output/chrome-llm
```

The driver requires a fresh output directory. It compiles Java with strict lint,
copies declared resources, creates the modular JAR and JMOD, links an image, then
uses that image's compiler and runtime to exercise the named module boundary.
`--test-main` may be repeated for additional dependency-free regression main
classes already present under `src/test/java`. Each `--test-arg MAIN=ARG` supplies
one positional argument to a selected test main; repeated arguments keep their
order. Files under `src/test/resources` are copied into the test classpath, with
resource/class collisions refused. Existing tests in runtime packages use an
explicit test-only module patch, preserving package-private oracle access without
adding exports. Supply `--node /absolute/path/to/node` to run the four browser
mock suites from exactly the received resources. These suites do not invoke Chrome.

An already compiled facet JNI library can be packaged using
`--native-library /absolute/path/to/library`. The receiving library and its hash
are recorded. Native packaging is separate from actual JNI differential testing.
The existing explicit native-loading API retains control over when it is loaded.

For a stock Java 21 fixture, `--progress-patch /path/to/progress-source` accepts
exactly the two actual M3 progress API sources. It compiles them as a test-only
`java.base` patch and supplies the patch to module/test compilation and execution.
The patch stays outside the linked image. The receipt records
`PATCHED_STOCK_FIXTURE`; this is local module proof, not an unpatched M3JDK image.

## Evidence boundaries

`RECEIPT.json` in the selected output contains commands, exit statuses, source and
resource hashes, native input hashes, toolchain release identity and an image
inventory. A successful run is `LOCAL_MODULE_IMAGE_PROVEN`. The driver does not
claim whole-product M3JDK build/jtreg, live Chrome/Gemini Nano execution,
cross-platform JNI qualification or performance improvements. Those gates need
their own actual receiving evidence.

## Java usage

The extraction input retains the donor record schema. For example:

```java
import java.util.List;
import java.util.Map;
import jdk.m3.llm.summary.Data;
import jdk.m3.llm.summary.Summaries;
import jdk.m3.llm.summary.Summary;

String body = "The result preserves evidence. Important claims retain citations.";
Map<String, Object> record = Map.of(
    "record_id", "example-1", "source_uri", "example://document-1",
    "profile_id", "example-v1", "title", "Example", "body", body,
    "content_sha256", Data.sha(body));
var result = Summaries.hierarchical(List.of(record), "evidence",
    Summaries.Options.defaults());
System.out.println(Summary.cited(result));
```

The caller supplies authoritative source identifiers, profile and content hashes.
The returned result retains all five method score families, sentence/evidence IDs,
node inputs/outputs and the overall semantic root. This is extractive summarization;
it does not call a model or require Chrome.

For a browser-backed tree, construct `ChromeBrowserPort` with the host's existing
JSON transport and pass it to `SummaryTreeExecutor`. The host relays each string,
unchanged, to the packaged `chrome-protocol-bridge.mjs` adapter's `exchange` method.
A user gesture must first call the adapter's `prepare('summarize', options, consent)`.
The port also offers an independent `openPrompt()` session. No listener or browser
process is started by the Java module. Host routing, authentication, deadlines and
transport cancellation belong to the embedding application.

The browser resources are under `jdk/m3/llm/browser` in the module. Use the existing
module-resource API to serve/extract the four `.mjs` files together. Their client
retains the seven existing operations: summarize, prompt, write, rewrite,
proofread, translate and detect. Capability checks determine availability in the
actual browser. Only summarize and prompt are exposed through the Java candidate
bridge in this increment; the other operations remain available in the browser client.
