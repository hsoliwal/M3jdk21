<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Precompute production-source documentation coverage

This ledger routes all **23 production Java source files** physically under
`m3/ports/precompute/src/main/java/com/m3/precompute` at
[public source pin `13a70cdbbe7b0abc75df18c78e1ca044e0c74240`](https://github.com/hsoliwal/M3jdk21/tree/13a70cdbbe7b0abc75df18c78e1ca044e0c74240/m3/ports/precompute/src/main/java/com/m3/precompute).
The current directory identities match the earlier package inventory. This is
one package in one independently built module, not 23 packages, the full M3
precompute estate, or 23 public APIs: `M3Histogram` and `M3CpuToolNativeSignals`
are package-private implementation dependencies.

Every listed owner has a body-read documentation route, but that is **not** a
claim that every API, test, resource cost or runtime integration is qualified.
The earlier base-fact section is a contract overview with selected methods;
its systematic method/accessor enumeration remains open. The batch, regex and
CPU-tool references add dedicated public-method/result contracts for their
scoped owners and explicitly describe internal dependencies. Inspect each
reference's evidence and limits before using a result.

## Per-file routes and remaining work

All names below have the `.java` suffix. “Overview” and “dedicated reference”
describe documentation depth, not execution status. All builds/tests/examples/
policy and recipe verifiers/benchmarks for these documentation passes are
**NOT_RUN**; an assertion described from test source is not a passed test.

| Production source file | Visibility / role | Concrete documentation route | Remaining method, test or runtime qualification |
| --- | --- | --- | --- |
| `M3TextMetrics` | Public record, scalar geometry | Overview: [units, encoding and seams](m3-packages.md#m3textmetrics-units-encoding-and-seam-composition) | Systematic component/method enumeration; inconsistent direct records, mutable input and resource/runtime gates |
| `M3TextSignals` | Public text-fact/sketch owner | Overview: [exact facts versus candidates](m3-packages.md#m3textsignals-exact-facts-versus-candidate-signals); [batch snapshot dependency](precompute-batch-reference.md#code-text-compilation-and-snapshot-provenance) | Systematic flags/scalar-accessor enumeration; adversarial composition, collision/filter domains and measured cold/warm cost |
| `M3Histogram` | Package-private histogram dependency | Overview: [histogram ownership and composition](m3-packages.md#m3textsignals-exact-facts-versus-candidate-signals) | Internal-operation edge audit, overflow/allocation/resource tests; no standalone public API claim |
| `M3EditDistancePlan` | Public exact UTF-16 edit plan and workspace | Overview: [query/workspace contracts](m3-packages.md#m3editdistanceplan-exact-query-and-workspace-contract) | Finish systematic inherited/interface/accessor inventory; monitor/reentrancy, budget edges, real full-operation cost |
| `M3Progress` | Public callback interface | Overview: [progress and early returns](m3-packages.md#progress-cancellation-and-early-returns); [batch callback paths](precompute-batch-reference.md#progress-exceptions-and-cancellation) | Complete standalone declaration/default inventory; throwing callbacks, cancellation and each caller's early paths |
| `M3CodeTextSignals` | Public lexical/text facade and `Snapshot` | Dedicated: [compilation and snapshot provenance](precompute-batch-reference.md#code-text-compilation-and-snapshot-provenance), [packed layout](precompute-batch-reference.md#packed-lexical-layout-and-heuristics) | Mutable/custom sequences, fabricated record/provider results, lexical-lane boundary assertions and cost |
| `M3CodeTextSignalBatch` | Public provider interface and `Limits` | Dedicated: [raw batch contract](precompute-batch-reference.md#raw-lexical-batch-contract-and-accounting) | Independent custom-provider qualification; transfer geometry/budget and callback edges |
| `M3CodeTextSignalBatchJava` | Public Java lexical implementation | Dedicated: [raw contract/accounting](precompute-batch-reference.md#raw-lexical-batch-contract-and-accounting), [layout/heuristics](precompute-batch-reference.md#packed-lexical-layout-and-heuristics) | Saturation/whitespace/range boundaries, mutation and cancellation execution |
| `M3CodeTextSignalBatchNative` | Public explicit JNI lexical implementation | Dedicated: [native loading/lifetime](precompute-batch-reference.md#native-opt-in-loading-and-resource-lifetime), [JNI copies](precompute-batch-reference.md#jni-copying-is-not-zero-copy) | Real library loading, JNI failure/lifetime/platform parity and measured acceleration; no automatic fallback claim |
| `M3SimilarityPrecompute` | Public pairwise fact record | Dedicated: [pairwise record](precompute-batch-reference.md#pairwise-similarity-record) | Direct-constructor consistency/NaN and compare assertions; original-text verification still required |
| `M3SimilarityBatch` | Public approximate-score interface and `Limits` | Dedicated: [similarity batch contract](precompute-batch-reference.md#approximate-similarity-batch-contract) | Custom providers, work/transfer boundaries and execution; score is not exact edit distance |
| `M3SimilarityBatchJava` | Public Java score implementation | Dedicated: [similarity formula/order/allocation](precompute-batch-reference.md#approximate-similarity-batch-contract) | Full boundary/mutation/cancellation checks and cost; no semantic acceptance from ranking |
| `M3SimilarityBatchNative` | Public explicit JNI score implementation | Dedicated: [similarity batch](precompute-batch-reference.md#approximate-similarity-batch-contract), [native loading/lifetime](precompute-batch-reference.md#native-opt-in-loading-and-resource-lifetime) | Executed Java/native/platform parity, failure and process-memory gates |
| `M3RegexShape` | Public textual shape recognizer | Dedicated: [literal shapes/dialect limits](regex-precompute-reference.md#literal-shapes-and-dialect-limits) | Finite fixtures are not exhaustive Pattern equivalence; external flags are outside this API |
| `M3RegexStringSignals` | Public fact/snapshot/parity records | Dedicated: [fact packets](regex-precompute-reference.md#regexstring-fact-packets) | Malformed-surrogate literal parity, constructor inconsistency, maximum-count and callback tests |
| `M3RegexStringTrialImage` | Public eager finite-corpus image | Dedicated: [compile/budgets](regex-precompute-reference.md#eager-trial-image-compilation), [queries/masks/ranking](regex-precompute-reference.md#trial-queries-masks-and-ranking) | All flag/budget/mask/ranking edges, input mutation and hostile Pattern costs; no serialized-image API exists here |
| `M3RegexOracleCorpus` | Public generator/root functions | Dedicated: [corpus generation/roots](regex-precompute-reference.md#corpus-generation-and-roots) | Runtime root reproduction and broader corpus domains; arbitrary-list hashes are not corpus validation |
| `M3RegexOracleHarness` | Public sharded oracle and candidate checker | Dedicated: [harness access](regex-precompute-reference.md#harness-compilation-and-access), [candidate reports](regex-precompute-reference.md#candidate-checks-and-reports) | Actual oracle/candidate runs, callback failures and constructor consistency; zero finite contradictions is not universal soundness |
| `M3CpuToolPlan` | Public plan, steps and compiled metadata | Dedicated: [plan admission/ownership/identity](cpu-tool-precompute-reference.md#plan-admission-ownership-and-identity) | Plan shape and identity do not themselves qualify execution semantics, hostile inputs or resource behavior |
| `M3CpuToolImage` | Public serialized CPU image | Dedicated: [image format/validation](cpu-tool-precompute-reference.md#image-format-and-validation) | Actual corruption/round-trip and resource tests; codec validation is scoped to CPU images, not regex trial images |
| `M3CpuToolImageCompiler` | Public TSV/image compilation and file effects | Dedicated: [TSV/compiler effects](cpu-tool-precompute-reference.md#tsv-and-compiler-file-effects) | Real malformed-input, filesystem/failure and allocation behavior; no new file-write action was executed here |
| `M3CpuToolRuntime` | Public dispatch/runtime and execution result | Dedicated: [binary operators](cpu-tool-precompute-reference.md#binary-operator-contracts), [execution/receipts/budgets](cpu-tool-precompute-reference.md#execution-snapshots-receipts-and-budgets) | Executed operator/platform/resource gates; receipt fields do not establish timing, speedup or full semantic proof |
| `M3CpuToolNativeSignals` | Package-private Java-first JNI parity adapter | Dedicated: [text/optional-native boundary](cpu-tool-precompute-reference.md#text-and-optional-native-boundary) | Real linkage/native-presence and Java-only/required-load/mismatch error-path evidence; not a fifth public CPU-tool API |

## How to use this coverage boundary

- The base overview, eight-owner batch reference, five-owner regex reference and five-source CPU-tool reference account for 5 + 8 + 5 + 5 = 23 physical source files without dropping internal dependencies
- Earlier coverage statements that left regex/CPU documentation open describe their earlier snapshot; the later references add those source contracts without retroactively claiming tests ran
- Source hash agreement proves which bytes were read, not correct behavior, complete dependency closure, semantic parity, licence admission or a successful runtime port
- Test/linkage gaps belong to each concrete owner and operation; a module-level green result would still need the exact commit, selected tests/profiles, real native execution and relevant JDK target receipts
- Native C/JNI/build files, test sources and documentation are dependency/evidence surfaces, not extra Java production files in the 23-row denominator
- Remaining work includes systematic base-overview API enumeration, documented test gaps and actual Java/native/JDK/resource qualification; other precompute packages, vendor/custody trees and repositories require their own inventories

The [porting invariant](../../m3/docs/m3jdk21-porting-invariant.md) keeps
String → arrays → collections → AST/compiler → remaining families as the
receiving order. This ledger is documentation/navigation only and changes no
receiving status, code, build profile, runtime owner or provenance authority.
