<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project SPDX-License-Identifier: Apache-2.0 -->
# M3JDK Chrome LLM and summarization

`com.m3.llm` is the M3JDK-owned Java21 module for the received Synexia text,
evidence and Chrome inference capabilities. Its runtime dependencies are
JDK modules only: `java.base` and `jdk.httpserver`. It is an independently
Apache-2.0-licensed product module in the shared `m3/pom.xml` reactor.

This receiving change is a component candidate. Its Java/module tests and
linked-image checks are recorded separately from full source-built M3JDK
acceptance, live Chrome/Gemini Nano and Windows/native/GPU qualification.
The current broader JDK receiving phase is not advanced by this module.

## Received capabilities

| Method | Target owner | Behavior retained |
| --- | --- | --- |
| Frequency extraction | `com.m3.llm.extract.ExtractiveSummarizer` | Frequency-ranked source sentences with original ordering |
| Luhn-style extraction | `com.m3.llm.summary.LuhnSummary` | Existing token/stop-word scoring and deterministic ties; LF output contract |
| Source-anchored extraction | `com.m3.llm.evidence.SourceAnchoredSummary` | Exact source spans, hashes and changed-source refusal; existing Unicode-aware Luhn owner internally |
| Mixed CPU/model results | `MixedModeSummaryConsolidator` | CPU source root and omitted evidence retained; optional model proposals remain candidates |
| Native browser Summarizer | `ChromeBrowserSummarizerCandidateProvider` and browser resources | Native `Summarizer` request/result identity, availability and explicit abstention |
| Browser summary trees | `BrowserSummaryTreeCandidateProvider` and strict browser tree resources | Source/child/result hashes, plan integrity, bounded execution, checkpoint reuse and candidate authority |
| Chrome evidence selection | `com.m3.llm.chrome.ChromeMlContextPacket` / `ChromeMlContextAdvisor` | Immutable indexed evidence projection, request/output bounds and completion checks |
| Hierarchical prompt summaries | `com.m3.llm.chrome.aix.AiXChromeMlMultipass` | Exact source chunks, leaf/reduction lineage, bounded fanout and complete-status enforcement |
| Byte-budget projection | `com.m3.llm.projection.ExtractiveConsolidator` and `Bm25Index` | Whole UTF-8 passages, mandatory evidence, retained omissions, version-aware search and digest-checked snapshots |
| Task summary and key points | `ChromeMlTaskService` and `ChromeBuiltInAiAdapters` | Surrogate-safe exact coverage, three-way map/reduce, whole-job call-budget preflight and per-call receipts |
| Native inference process | `ChromeMlProcessRunner` and runtime/configuration owners | Explicit CPU/GPU/APU configuration, streaming, cancellation, bounded capture, asset pins and lifecycle checks |

The browser native `Summarizer` and prompt-based AiX reduction paths retain
different protocols. A successful native completion or valid result hash
does not establish factual accuracy or authorize promotion of model text.
Summarization projects the source; it does not delete the omitted source.

The projection consolidator uses caller order. Hierarchical `combine` rehydrates
both selected and omitted cells, rejects conflicting keys, and orders by
`CellKey`; it does not preserve a child BM25 ranking. Snapshot writes prevent
overwrite and reads verify digests, but writes are not crash-atomic. Public
projection records are data carriers, not independently verified receipts.

The task service retains all 13 existing task kinds. SUMMARY and KEY_POINTS
split at surrogate-safe UTF-16 boundaries and reduce in groups of three.
All reductions, including the final reduction, request at most 512 tokens
and accept at most 2,048 UTF-16 units. Its deadline checks surround synchronous
runner calls; the configured runner supplies actual cancellation behavior.
These adapters execute prompt workflows; the browser-native provider remains
a separate API path.

## Build and use

```sh
mvn -f m3/llm/pom.xml verify
# In a complete checkout, through the shared reactor:
mvn -f m3/pom.xml -pl llm verify
```

There are no runtime donor JARs. JUnit is a test-only dependency. Maven and
the Java21 toolchain are build inputs. The module follows the existing
`com.m3.*` module naming convention; compiler module-name style warnings
are disabled while other enabled warnings remain errors.

```java
import com.m3.llm.evidence.SourceAnchoredSummary;
import com.m3.llm.summary.LuhnSummary;

String source = "Source evidence remains available. Summaries keep exact spans.";
SourceAnchoredSummary summary = SourceAnchoredSummary.summarize(source, 1);
String text = summary.render(source);
String luhn = LuhnSummary.luhnSummary(source, 1);
```

For a source file, build an `AiXSourceSnapshot`, then use
`AiXMechanicalPlan.build(snapshot)` and pass that plan plus an explicitly
configured `ChromeMlRunner` to `AiXChromeMlMultipass.run`. Caller-constructed
Plan/Chunk metadata does not gain source verification from `run` alone.
Keep source snapshots and candidate output records separately.

## Browser and native runtime boundary

The four first-party browser modules are under `browser/web/` and are also
packaged as `com/m3/llm/browser/` JAR resources. Use
`chrome_summary_tree_strict.mjs` for tree execution: it recomputes source,
plan and node integrity before the base executor runs. The native
Summarizer tool is `chrome_ai_summarizer_tool.mjs`.

The Java browser provider accepts the existing host-owned BrowserTransport
contract. A browser host supplies its configured CDP/WebView/desktop bridge.
This module does not silently launch or attach to a browser session.
Browser availability, user activation, model downloads, supported languages
and input quota remain capabilities of the actual browser environment.

The process runner requires explicit compatible native executable,
ChromeML/Dawn assets and model files. Those binaries/weights are not
included. The module retains the existing command/configuration spellings
and explicit native-execution opt-in. The recovery selects a pinned
historical source because current donor process sources do not compile;
`SOURCE_PROVENANCE.tsv` records the exact selection.

The runner rehashes configured assets before each request, which may be
expensive for large model files. Its timeout/cleanup does not promise
arbitrary callback termination or universal operating-system process-tree
containment. The AiX path currently retains byte, decoded-string and chunk
representations and has no independent whole-job deadline. These are
inherited contracts, not claims of zero-copy storage or measured speedup.

## Product qualification commands

The JUnit suite runs the received Java behavioral tests, including frozen
packet/prompt and successful-tree hashes. Linux additionally executes the
63-check native process protocol oracle with real synthetic child processes;
that test requires `/usr/bin/python3`. It does not run a model.

```sh
node m3/llm/browser/tests/test_chrome_ai_summarizer_tool.mjs
node m3/llm/browser/tests/test_chrome_summary_tree.mjs
node m3/llm/browser/tests/test_chrome_summary_tree_integrity.mjs

# Use the chosen Java21/M3JDK toolchain; output must be a fresh directory.
"$M3_JDK/bin/jlink" --module-path "$M3_JDK/jmods:m3/llm/target/m3-llm-0.1.0-SNAPSHOT.jar" \
  --add-modules com.m3.llm --output /path/to/fresh-image
/path/to/fresh-image/bin/java --add-modules com.m3.llm \
  -cp m3/llm/target/test-classes com.m3.llm.test.ModuleImageMain
```

The displayed module-path separator is for Unix; use `;` on Windows.
A linked image built from a stock JDK proves the module's packaging and
dependency closure, not acceptance of a source-built M3JDK distribution.
Live model behavior, native binaries and target platforms require their
own execution receipts.

## Scope and lineage

Exact source commits, source/target hashes and modifications are in
`SOURCE_PROVENANCE.tsv`; receiving gate dispositions are in
`RECEIVER_RECEIPT.json`. The canonical Maven/OpenRewrite recipe, original
source fixtures, failure counterexamples and private execution logs remain
in Synexia. Every target code file is an executed recipe output.

Two reproduced CanonicalJson defects are corrected in the canonical source
and the received implementation: duplicate object keys following a null
value, and unescaped JSON string control characters. Existing valid-input
observations and public signatures are preserved. This is not a claim of
complete JSON grammar qualification.

The distinct MIndex/WordKnowledge Luhn path, Cognix's Lucene-bound passage
summarizer, the Jackson-bound Java tree and additional Chrome service families,
and the standalone
ChromeLLM visual/media/diffusion repository remain recorded in
`PENDING_FAMILIES.tsv`. They are not silently replaced by similarly named
methods. This module does not claim that every Synexia summarizer or every
Chrome capability has been migrated.
