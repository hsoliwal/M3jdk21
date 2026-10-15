<!-- SPDX-License-Identifier: Apache-2.0 -->

# Coverage, evidence and continuation ledger

[Guide](README.md) · [Package accounting](packages/README.md) · [Descriptors](descriptors.md)

## Pinned scope and what the counts mean

Snapshot: `278b221e8365ebeb6095a3a5643b38a0564258b5`, documentation PR base source `d5abed865bd47c4dd5ad78e95704348fe2095ed2`. Root and source subtree identities were reconciled. Large recursive Git-tree responses were split into smaller subtrees until all accepted results had `truncated=false`. The merged inventory has 127,512 tracked entries: 104,606 blobs and 22,906 directories. There are no unresolved tree reads or submodule descendants in this repository inventory.

| Level / evidence unit | Discovered/accounted | Substantive state | Remaining work |
| --- | ---: | --- | --- |
| Repository | 1 | Additive draft guide | Maintainer review; exact-head CI; complete child semantic coverage |
| Workstream reading map | 23 | Partial responsibility/build/boundary summaries | Not an exact project denominator; reconcile script/native-only and nested build units |
| Direct JDK named-module descriptors | 70 / 70 read | Descriptor-level purpose, exports, requires and services documented | Full API/state/lifecycle, inbound consumers and package/test mapping |
| All tracked Maven descriptors | 266 / 266 indexed | Some active manifests read; others path-only | Effective reactor/profile/inheritance and retained-copy classification |
| All tracked JPMS descriptors | 565 / 565 indexed | 70 direct JDK descriptors plus sampled independent descriptors read | Distinguish active, generated, test and custody identities |
| Java files | 74,035 | File-presence accounting | No full-source or declared-namespace audit |
| Directories containing non-descriptor Java source | 13,793 / 13,793 indexed | Every row has purpose/pointer, entry candidate, dependencies, tests, evidence and gap fields | Most rows remain PATH_ONLY or DOC_LINKED, not documented packages |
| Module-info-only directories | 559 / 559 accounted by descriptors | Descriptor paths present | Resolve active membership and semantics |
| M3 independent/tooling/reference source roots | 44 sampled | Component-specific partial contract guide, imports, tests and inline gaps | Complete all type/method/consumer/error/effect contracts |
| M3 runtime package boundaries | 5 sampled | Partial current-source owner guide | Remaining inherited owners and exact target qualification |

Reviewed-complete substantive package coverage is **not established**. Do not report 13,793 packages documented, 49 packages complete, 100% semantic coverage or a passing M3/runtime run. The 44 and 5 sample groups are distinct physical source roots; identical namespace spellings are not merged across independent classpaths. The declared namespace denominator is unresolved, so a truthful package-completion percentage cannot yet be calculated.

## Evidence vocabulary

- PATH_ONLY: a tracked path/type exists at the pinned source revision
- DOC_LINKED: a native package-info.java/package.html is located; its full content and freshness are not automatically reviewed
- MANIFEST_READ: direct manifest declarations were read; effective build execution and behavior remain unverified
- OBSERVED_SAMPLE: a source declaration/body sample supports a bounded explanation; other package members remain open
- NOT_RUN: an executable check did not run in this documentation pass
- UNKNOWN: unresolved identity, contract or coverage, retained in the denominator rather than silently excluded

Existing package/API documentation is authoritative for the source revision where it lives. A linked test is evidence of a test file, not executed-test evidence. Existing receipt text retains its exact original head, environment, command and scope; this guide does not refresh old results by quoting them.

## Documentation task packet

| Field | Editorial scope |
| --- | --- |
| Mode | Additive documentation candidate on the existing draft PR |
| Targets | doc/m3-guide/*.md and doc/m3-guide/packages/*.md |
| Public API / behavior / build scripts | Locked; no source, descriptor, workflow or runtime flag changes |
| Protected inputs | m3/README.md, m3/docs/native-build.md, generated custody documents and source-sealed recipe payloads unchanged |
| Allowed delta | Repository/project/module/package navigation, observed source contracts, coverage and gap ledger |
| Forbidden | Merge/rebase, generated custody rewrite, source/API edits, guessed passing tests, donor-phase promotion, private material publication |
| Verification order | Review documentation delta; structural/link/scope checks; compiler/test/runtime stages remain NOT_RUN |
| Stop condition | Draft documentation published with exact counts and unresolved scope/semantic gates explicit; no claim of whole-task or whole-runtime completion |

This bounded editorial pass is not a completed full M3 execution run. Full compiler/runtime proof artifacts, dedup clusters, semantic fixed points and topology/compute receipts were not produced. Hashes identify inspected bytes; they do not establish behavioral equivalence.

## Prioritized continuation

| Priority | Work | Current disposition | Evidence needed to close |
| --- | --- | --- | --- |
| P0 | Confirm every declared namespace and source-set owner | OPEN | Declaration extraction over the complete pinned tree; distinguish unnamed tests, platform overlays, vendor and generated copies |
| P0 | Finish M3 String/regex owner contracts | PARTIAL | Complete method/API, failure, Unicode, owner/GC/JNI and consumer/test mapping; existing canonical recipe process for any inline edits |
| P0 | Reconcile every active M3 package | PARTIAL | Read all public types and key internal owners, dependencies, state/effects, tests and build boundaries |
| P1 | Reconcile inherited package documentation | LINKED / UNREVIEWED | Review native package-info/package.html and key API contracts; record source-version drift and precise gaps |
| P1 | Resolve all project/module descriptors | INDEXED | Active reactor/profile/JPMS identity; separate fixtures/custody from runnable projects without erasing them |
| P1 | Review inline documentation | OPEN | Method parameters, returns, exceptions, ownership and thread-safety; perform source changes only through owning recipes and refreshed seals |
| P1 | Expand native/script architecture | OPEN | HotSpot/C/C++/JNI/Python/shell entry points, ABI/effects and their actual module boundaries |
| P1 | Map and execute applicable tests | NOT_RUN | Exact final-head build/test environment and separate Java/native/provider/platform receipts |
| P2 | Reconcile historical guides | OPEN | Preserve historical scope while adding current-source successor links; do not rewrite old evidence |

## Second-pass semantic review

The [text/storage and precompute API sections](m3-packages.md#textstorage-api-contracts) now explain eleven complete production owner bodies (six text/storage, five precompute including internal histogram support) and three selected test files. They add source-backed units, ownership, failure, concurrency, allocation, callback and usage contracts. This deepens two package roots; it does not add eleven complete packages or change the unresolved package denominator.

The new review specifically distinguishes immutable results from mutable edit-distance workspaces, Java UTF-8 replacement semantics from UTF-16 units, read-only mapping from immutable-file publication, and DP-cell budgets from total CPU/allocation cost. Tests/examples remain NOT_RUN. Remaining API owners and exact-head execution retain their prior open status.

## Publication and source safety

Only new Markdown under doc/m3-guide is added. Uploaded design essays, private donor content and local working paths are not part of the public payload. All repository citations resolve to the public M3JDK21 tree or existing relative documentation. OpenJDK, third-party and scoped first-party notices remain in place. The catalogue is not a new ownership, licensing or receiving authority.

The PR description records the final documentation commit and checks observed after publication. Merge, release readiness and runtime acceptance remain separate decisions.

## Lexicon catalog contract review

The [lexicon catalog reference](lexicon-catalog-reference.md) adds complete source-body review of six `com.m3.text` catalog/payload/typed-value owners at `c038343521587d96c9fa190715ce2f4b9f8c490a`, reusing the earlier image/storage reference. Public methods and nested records are mapped to source and actual assertions in ten read Java test files, with two selected Python test boundaries and explicit source-only gaps. Eager/lazy admission, failed-first-touch retry, close/mapping lifetime, scoped identity, optional sidecars and selective builder validation are documented. Tests, examples and runtime checks remain NOT_RUN. This does not close all 17 core files, exporter/authenticity proofs or package/runtime qualification; instance-index, number and language siblings remain outside this packet.

## Primitive and snapshot map contract review

The [primitive and snapshot map reference](primitive-map-reference.md) adds complete source-body review of `M3LongLongHashMap`, `M3ConcurrentLongMap`, their primitive iterator/consumer contracts and the relevant factory at `c038343521587d96c9fa190715ce2f4b9f8c490a`. It explains differing `put` results, absence, mutable tombstones and fail-fast traversal, signed-key immutable snapshots, CAS retry/copy costs and stream ownership. Six complete test files were read; direct map assertions in two are distinguished from unrelated broad suites and source-only gaps. All examples/tests remain NOT_RUN. Other collection owners, racing-writer/resource qualification and JDK backend promotion remain open.

## Precompute batch and similarity contract review

The [precompute batch and similarity reference](precompute-batch-reference.md) adds complete source-body review of eight lexical/similarity Java owners at `c038343521587d96c9fa190715ce2f4b9f8c490a`, including their public methods, nested records and required progress/text-fact/shape/JNI/C/build dependencies. It distinguishes stable input from an atomic snapshot, provider accounting from whole-call allocation/work, approximate scores from exact verification, direct-record validation from derived facts, and explicit native loading from measured acceleration. Three Java test files and one native C check file were read; specific assertions and gaps are recorded, with examples/builds/tests/recipe and policy checks all NOT_RUN. This deepens the existing precompute package without claiming package completion, exhaustive native parity or runtime promotion; the broader regex/trial/oracle and CPU-tool families remain open.

## Primitive long sequence contract review

The [primitive sequence reference](primitive-sequence-reference.md) adds complete body review of nine non-descriptor collection sources at `c038343521587d96c9fa190715ce2f4b9f8c490a`: two public owners, six public interfaces and one package-private list spliterator. The already reviewed iterator/factory boundary is reused rather than counted again. It maps concrete and inherited methods, aliases, capacity/materialization, callback partial effects, fail-fast versus fixed-fence traversal and queue-default progress. Direct list/deque tests, all three methods of the differential suite and selected JDK-shaped sequence assertions are distinguished from source-only observations and generic object-factory tests. All tests/examples remain NOT_RUN. This does not close all 76 collection bodies, concurrent/reservation qualification, JDK replacement, installed delivery or whole-package coverage.

## Joined-array package contract review

The [joined-array reference](joined-array-reference.md) adds complete body review of all seven public non-descriptor sources in `m3/arrays/src/main/java/com/m3/arrays` at `1eac151fe56bccdc33b24a0155cc1ac5b9959005`, with private snapshot/slice/join implementations, the module/build declarations, both Java test classes and native bridge/C checks. It covers declared public methods, record components and inherited boundaries, ownership, export, UTF-16 facts/seams and JNI failures. The missing byte/char snapshot `segmentCount()` implementations and one exact duplicate-end lookup trace are source-observed gaps, not executed compiler/test failures. Existing test assertions and untested contracts are separated; all examples, builds, Java/native tests, recipe checks, jtreg and benchmarks remain NOT_RUN. This bounded body review does not close caller qualification, inline package documentation, installed-image delivery or the ordered arrays receiving gates.

## Prefix and Fenwick contract review

The [indexed algorithm reference](indexed-algorithm-reference.md) adds complete source-body review of `M3PrefixZ` and `M3LongFenwickTree` at `1eac151fe56bccdc33b24a0155cc1ac5b9959005`: eight PrefixZ methods, two Fenwick constructors and ten Fenwick methods. It reuses five existing text-owner dependencies and distinguishes exact facts, primitive-payload counts, empty-input cancellation, checked intermediate arithmetic, fixed-size half-open indexing, failure-atomic mutation and unsynchronized shared scratch. Three full test bodies and the distinct foundation/scoped-runner/Maven routes were read. Source-observed huge-index wrap and intermediate-overflow gaps are not runtime-validated findings. All examples, builds, tests, policy/recipe checks and measurements remain NOT_RUN; other owners and package/runtime qualification remain open.

## Regex precompute and oracle contract review

The [regex precompute and oracle reference](regex-precompute-reference.md) covers the declared public methods, nested records, result components and enum values of five complete public production bodies at `13a70cdbbe7b0abc75df18c78e1ca044e0c74240`: M3RegexShape, M3RegexStringSignals, M3RegexStringTrialImage, M3RegexOracleCorpus and M3RegexOracleHarness. It distinguishes String literal facts, lexical features, flags-blind shapes, JDK Pattern trial outcomes, approximate ranking and finite-corpus pruning evidence. It records masks/ranges, UTF-16 versus UTF-8 identity, constructor validation limits, budgets/cancellation, stable-input obligations and top-N full-sort allocation. The trial image has no public serializer/loader; no restore-validation claim is made. Four direct JUnit tests, one separate differential driver and public build/native/policy/provenance dependencies were inspected. Examples, builds, tests, verifiers and benchmarks are NOT_RUN; JDK/native/runtime promotion and exhaustive semantic/resource qualification remain open.

The [precompute production-source ledger](precompute-source-coverage.md) reconciles the base overview, batch, regex and CPU-tool references against all 23 physical production Java files in the package. This is path-level documentation routing, not complete API/runtime qualification: base-overview method enumeration and the per-owner test/resource/runtime gaps remain explicit. M3Histogram and M3CpuToolNativeSignals are package-private dependencies, not public API owners.

## CPU-tool precompute contract review

The [CPU-tool plan, image and runtime reference](cpu-tool-precompute-reference.md) adds complete source-body review of four public CPU-tool owners and their package-private `M3CpuToolNativeSignals` adapter at `13a70cdbbe7b0abc75df18c78e1ca044e0c74240`. It covers every declared public method/type, record projections, graph preparation versus subject-time work, canonical image/version checks, TSV/compiler effects, all 13 byte operators, ownership/encoding/indexing, budget and cancellation limits, hash-versus-exactness boundaries, and native loading/copying. CPU self-test/JUnit, lexical Java/JNI/C, native parity/check and build sources were read; documented assertions and missing gates remain distinct. Examples, builds, tests, policy/recipe checks and performance qualification are NOT_RUN. This is documentation coverage of five package bodies, not runtime promotion, whole-repository completion or operating-system-wide interning.

## Bootstrap String ownership contract review

The [bootstrap String ownership and admission reference](bootstrap-string-ownership.md) adds full source-body contract coverage of package-private `java.lang.M3StringOwner`, `M3StringAtom`, `M3StringPool` and `M3StringTuple`, including their declared constructors/methods, state and nested helpers, plus the bounded `M3String` descriptor/admission boundary. The source pin is `ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2`. It distinguishes exact content from tuple layout identity, local budget refusal from allocation failure, the non-refusing dedicated unit lane from ordinary one-unit input, weak native cleanup from immediate freeing, logical coder from mapped physical width/endian, lexicon snapshot lifetime, owner-local facts and JNI shadow boundaries. Five relevant test bodies and selected native/String callers were source-reviewed. Builds, tests, policy/recipe checks, performance and matched-runtime qualification are NOT_RUN; whole P01 remains PARTIAL.

### P01 inherited package-documentation correction, 2026-10-14

This is additive evidence; the original inventory snapshot and its `native_package_doc=false` field remain unchanged. The original calculation selected M3-prefixed filenames and therefore excluded inherited package documentation.

- Original inventory pin: `278b221e8365ebeb6095a3a5643b38a0564258b5`
- Audit pin: `ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2`
- `physical_package_info_present`: true
- `physical_package_info_path`: `src/java.base/share/classes/java/lang/package-info.java`
- Blob at both pins: `882a610c1a08e37cbea6c7b49dc23f8672237b53`
- `m3_specific_package_documentation_present`: false

The [unchanged inherited package-info](https://github.com/hsoliwal/M3jdk21/blob/ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2/src/java.base/share/classes/java/lang/package-info.java) describes general `java.lang` and character encodings, with no M3-specific prose. Physical presence does not close M3 method coverage or whole-package documentation. This correction records a selection error, not a newly added package file or new runtime acceptance.
### Bootstrap String-facing contracts: bounded source review, 2026-10-14

The [bootstrap String-facing reference](bootstrap-string-contracts.md) covers the complete public-target bodies of `java.lang.M3String` and `M3StringFacts` at `ed53dc8ab4fb6fd93ad1ed0c762546bdda475bb2`. It maps all 120 explicitly declared constructor/method entries in those two files: 80 methods and one constructor directly in `M3String`, 12 methods and three constructors in its cursor/spliterator helpers, and 23 methods plus one constructor in `M3StringFacts`. Fields are accounted for separately, including the descriptor's two instance fields and the 23 primitive instance fact fields. This is internal implementation documentation, not a new public API or inherited-member audit.

The companion [ownership reference](bootstrap-string-ownership.md) covers the four owner/atom/pool/tuple bodies. Together the two pages provide six complete production-body reviews, selected `String`/`System`/HotSpot/JNI boundaries, ten complete Java test-source reviews and the modified-UTF-8 native test helper. The new contract reference distinguishes UTF-16 coordinates from code points, negative candidate gates from exact comparison, caller output from retained canonical storage, standard from modified UTF-8, and public String exception/identity behavior from trusted helpers. The current generic-Charset lineage bullet is narrowly corrected to `CharBuffer.wrap(this)`; historical provenance is preserved.

The source tests cover finite fact-composition/window/seam cases, equality gates, stream traversal, shadows and logical overflow. Their limits remain explicit: mixed-representation labels do not prove every operand stays flat; the modified-UTF-8 test scalarizes its verification inputs, and its standalone long-valued overflow oracle is not oversized runtime or tuple/range JNI cache proof. All builds, repository/native tests, recipes, policy verifiers, benchmarks and runtime qualification are **NOT_RUN** for this documentation pass.

`M3CodeTextFacts`, `M3CodeTextSignalBatch`, `M3StringCodePointPrecompute`, `M3StringPositionPrecompute`, `M3StringRegexDp` and `M3StringSearchPrecompute` remain **DECLARATIONS_ONLY**. Their full cache, budget, concurrency, fallback and consumer contracts still need separate review. The `jdk.internal.mindex` StringBacking family is independent. Whole `java.lang`, `java.base`, GC/JIT/CDS/SA/native-platform and repository completion remain open; no receiving phase or runtime acceptance status advances.

## Bootstrap precompute contracts and P01 source-file reconciliation

The [bootstrap precompute reference](bootstrap-string-precompute.md) adds complete body-contract review of the six remaining P01 files at `68bfe667943f4a231f3627b56d34b0be53a4e4af`: M3CodeTextFacts, M3CodeTextSignalBatch, M3StringCodePointPrecompute, M3StringPositionPrecompute, M3StringRegexDp and M3StringSearchPrecompute. It covers 72 explicitly declared methods, 19 constructors and 18 named types including private/nested helpers, with 100 fields and three record components accounted for separately. Eight direct/relevant Java test bodies and selected caller, M3TQ and standalone Java/JNI provider boundaries were read.

The [twelve-owner map](bootstrap-string-source-coverage.md) is the additive successor to the preceding six-body/declarations-only status. All twelve original P01 M3-prefixed physical source files now have full-body contract references at unchanged source blobs. This is source-file documentation coverage, not all java.lang, Java SE public API qualification, inline-Javadoc completeness, the StringBacking family, native/runtime acceptance or repository completion. The inherited package-info correction above remains unchanged.

The new reference records exact owner/range identity, eager versus lazy preparation, direct-mapped weak caches, lane-only memory bounds, approximate versus exact decisions, limited dot/star syntax/default dot behavior, provider/cancellation separation, and precisely scoped OOM recovery. An unresolved Objects name-resolution concern and a two-backslash/one-count test contradiction are source-observed, independently checked and unexecuted. No source/test implementation is changed. Builds, repository tests, native libraries, policy/recipe gates, benchmarks and matched-runtime qualification remain **NOT_RUN by this authoring pass**; no ordered receiving phase advances.

## Bootstrap Pattern query contract review

The [Pattern query and Boolean-plan reference](bootstrap-pattern-query-contracts.md) adds complete body coverage for P02's one M3-prefixed file, package-private `java.util.regex.M3PatternQuery`, at `1dce82bfb7f1058d74f7503c4cd54fa7d431c032` (blob `741864212f351408f5ff18b5a37671cebc0c1632`, unchanged from the original inventory). It accounts for three declared classes, five fields, two explicit constructors and ten methods. The reference distinguishes conservative trigram absence queries from the bounded exact compiled-node Boolean plan, including flag/root/node refusal, region/input identity, cache reuse/invalidation, failed-search end flags/captures, source-only resource bounds and precise exception catches.

Selected direct Pattern/Matcher/String/M3TQ integration paths, six complete regex test bodies and the separate precompute differential driver were read. This is not complete Pattern/Matcher/other inherited-regex member coverage or a full M3TQ API audit. The String-local dot/star helper remains a separate P01 owner linked through the bootstrap precompute reference. Builds, tests, examples, policy/recipe verifiers, benchmarks and matched-runtime/platform qualification are **NOT_RUN by this authoring pass**. P02's adapter source-contract gap is documented; whole regex-package/module/repository completion, inline M3 package prose and runtime acceptance remain open. No receiving phase advances.

## IndexDB persistence and semantic contract review

The [persistence and V1 snapshot reference](indexdb-persistence-reference.md) and [semantic graph/fingerprint reference](indexdb-semantic-reference.md) add complete body review of all nine production Java files in `m3/indexdb` at `1dce82bfb7f1058d74f7503c4cd54fa7d431c032`. Their hashes match the original inventory. The [nine-owner map](indexdb-semantic-reference.md#nine-owner-coverage-map) distinguishes seven public types plus the package-private codec in P13 from the separate public `com.synexia.mindex.db.M3IndexDbFingerprintV1` record in P14. It maps declared methods, constructors, record projections, enum values and internal implementation boundaries.

The references cover caller-owned handle/graph lifetimes, filesystem publication and ref replacement, absent transaction/durability guarantees, versioned framing and bounded corruption admission, semantic identity and cycle/order validation, parent recomposition, fingerprint framing and candidate-versus-equivalence limits. Source-observed CR/LF ref metadata and unpaired-surrogate encoding limits remain explicit. The POM, control-reactor membership, public policies and fourteen complete module test-source bodies were read: twelve JUnit classes contain 69 declared test methods, with two helper programs. This is source accounting, not executed-test evidence; historical reports retain their original heads and scope.

All builds, examples, tests, policy/recipe verifiers, benchmarks and runtime checks are **NOT_RUN in this documentation authoring/review pass**. Hosted CI attempts and results are separate exact-head evidence; this statement does not erase them or characterize their outcome. These two physical package roots now have complete bounded production-body/declared-API reference coverage, while inline package documentation, full inbound callers/analyzers, cross-process/crash/resource/platform qualification and exact-head acceptance remain open. No package denominator, source/config/workflow, canonical ownership or receiving phase is changed.

## Mapped String backing and logical-coordinate contract review

The [mapped String backing, logical addresses and lifetime reference](mapped-string-storage-reference.md) adds complete source-body contract review of six P03 owners at `1dce82bfb7f1058d74f7503c4cd54fa7d431c032`: `M3StringBacking`, `M3MappedStringBacking` (including nested views, byte-store, page and commit helpers), `M3Boundary`, `M3Address28`, `M3Release` and package-private `M3Bits`. All six blobs match the original package inventory pin. This is an additive coverage increment; the original 24-file inventory and earlier sampled coverage remain intact.

It records selected-commit and cross-file snapshot limits, exact text/byte format and UTF-8 admission, scalar/alias facts, ignored metadata, ID/handle/offset distinctions, null and bounds behavior, non-direct empty buffers, channel-close versus mapping lifetime, weak JVM boundary/count semantics, caller-declared provenance and unchecked bit helpers. It distinguishes this reader from the bootstrap lexicon/native-address path and the actual JNI compatibility boundary. M3CI/M3CB/M3TQ/lane/bootstrap/HotSpot/JNI reads are scoped dependencies, not additional fully reviewed package owners. Four direct or integration Java test bodies and the selected native/harness sources were inspected; assertion scope and missing failure/platform cases remain explicit. Builds, repository tests, native execution, policy/recipe checks and resource measurements were **NOT_RUN for this authoring/review pass**; hosted CI remains separate exact-head evidence. Whole package, `java.base`, runtime and repository completion remain open.

## Internal class and member view contract review

The [class and member view reference](class-member-view-reference.md) adds complete source-body review of nine P03 owners at `e7d21edf0f998c3c53ab0d0909972a4a97dfc471`: M3Class, M3Member, M3Field, M3Method, M3Constructor, M3RecordComponent, M3ClassFlags, M3MemberFlags and M3MemberKind. Declared constructors/methods/fields and enum codes are accounted for; dependency-only M3CI/M3CB/M3Boundary/M3Descriptor/M3TQ/Class/VM sections are not counted as primary-body closure here. The reference covers owner/row identity versus typed-wrapper Object identity, snapshot-local names/descriptors/hashes, ordinal checks, optional metadata, stored order (including the record-component order caveat), lazy wrapper/reflection materialization, compact/weak-boundary refusal and real runtime prerequisites. CBProbe, its VmFixture, DescriptorTest and three CB tooling test bodies were read; historical backing/JNI recipe tests and stale harness paths are separated from current view tests. Repository builds, tests, recipes, native/GC/platform checks and benchmarks remain NOT_RUN. Source-body coverage does not imply whole-Class/reflection parity or package/runtime acceptance.

## Internal primitive-lane and metadata-helper contract review

The [internal primitive lanes and metadata helpers reference](internal-metadata-lanes-reference.md) adds seven fully reviewed P03 source bodies at `e7d21edf0f998c3c53ab0d0909972a4a97dfc471`: `M3IntLane28`, `M3LongLane28`, `M3BitLane28`, `M3CB`, `M3Descriptor`, `M3PC` and `M3VI`. All seven match the original inventory blobs. The review includes explicit constructors/methods/overloads/nested types, sparse allocation and overlap, integer widths/overflow, native copied-word counting, single-thread versus synchronized state, bridge declarations/retirement, hidden descriptors, ordered context identity, version bounds/tie order and retained scheme/value references.

Six complete test/probe/harness bodies were inspected, including the generated VI probe resource and its recipe harness; assertion coverage and unsupported/unexercised paths are recorded separately. Supporting address/weak-boundary/release/native bodies and M3CI/M3TQ excerpts are not double-counted as new primary-owner coverage. Builds, compilation, JUnit/Maven/jtreg, policy/recipe execution, native image linkage, lifecycle tests and resource measurements were **NOT_RUN for this documentation authoring/review pass**; hosted CI is separate exact-commit evidence. The [24-file package source map](internal-mindex-source-coverage.md) reconciles this increment with the mapped-storage, class/member and class-index/trigram references without promoting runtime, module or repository verification.

## Internal class-index and trigram-query contract review

The [class-index reference](class-index-reference.md) covers the complete M3CI body and its six nested types: explicit live-root closure, local loader/text/row identities, immutable metadata/member CSR, optional weak round trips, numeric ordering, implementation-defined hashes, allocation accounting and rejection/progress behavior. The [trigram reference](trigram-query-reference.md) covers the complete M3TQ body, Op, Facts and temporary backing reader: conservative Boolean normalization/evaluation, exact code-unit sets, bounded range scans, metadata-only seams, sparse/dense subset tests and caller-owned reuse. Neither owner declares a serialized metadata image format. Actual CB and TQ test bodies were read, but repository execution in this documentation pass is NOT_RUN; hosted CI remains separate exact-commit evidence.

Together with the six mapped-storage owners, nine class/member owners and seven lane/bridge/helper owners, the [P03 source map](internal-mindex-source-coverage.md) gives complete body-contract routing for all 24 physical Java sources in jdk.internal.mindex at e7d21edf0f998c3c53ab0d0909972a4a97dfc471. The full unfiltered directory has no additional files or package-info.java at that pin. This updates exact source-documentation coverage only: package API/behavior acceptance, method-level executable coverage, matched-runtime/platform/GC/native proof and repository completion remain open.

## Independent text projections and seventeen-source reconciliation

The [text projection, tool and receipt reference](text-projection-reference.md) adds complete body and declared-member review of the five remaining `m3/core/src/com/m3/text` owners at `8758991645037126985d56f16a208fec8a1eef38`: `LexiconTool`, `M3NumberSpace`, `M3InstanceIndexPrecompute`, `M3LanguageGrammarSupport` and `SynexiaLangDexSourceEnvelope`. The [seventeen-source routing map](text-source-coverage.md) reconciles the complete directory with six existing storage-owner reviews and six existing catalog-owner reviews. All seventeen current source blobs match both earlier review pins; existing content, original scope statements and provenance remain intact.

The new reference distinguishes generated numeric source IDs from family keys/storage identities, CLI file effects from build-script publication, target-owned instance schemas from admitted data, fixed grammar signals from rules, and exact-wire LangDex receipts from producer authentication or catalog admission. It records the instance implementation's differing forward UTF-8 and reverse Java String ordering without changing source. Eight complete Java test/fixture bodies, the numeric Python integration test, the core build/JPMS and reactor boundaries, two CLI call paths and relevant manifest/field-map/export policy were inspected. Assertions and source-only cases are attributed separately; the LangDex expected-wire fixture is not represented as a captured producer execution.

All builds, repository tests, examples, scripts, native/runtime checks, policy/recipe verifiers and measurements are **NOT_RUN by this documentation authoring/review pass**. Hosted CI remains separate exact-head evidence. Every physical source in this bounded text directory now has a body-reference route; complete inline package documentation, full inbound callers, external dataset/license admission, resource/platform qualification, whole package/module/project/repository completion and receiving acceptance remain open. No source/configuration/workflow/license, denominator or receiving phase changes.

## Independent compile-time source-contract reconciliation

### Compile-time package documentation routes

The [complete 19-source map](compiletime-source-coverage.md) routes every
production Java owner under `m3/ports/compiletime/src/main/java/com/m3/compiletime`
at `8758991645037126985d56f16a208fec8a1eef38`, including package-private helpers.
Its separate [regex reference](compiletime-regex-reference.md) covers six
analysis/key/image/gate owners and the [harvest reference](compiletime-harvest-reference.md)
covers two CLI/receipt owners. The inventory and residency references cover the
other eleven owners without overlap.

This is source-body/member documentation and navigation, not completion of
runtime, resource, malformed-input, concurrency or integration qualification.
All builds, tests, examples, benchmarks and custody/recipe verifiers in these
documentation passes remain **NOT_RUN**. The standalone Java-21 Maven classpath
artifact, separate ASM/precompute dependencies, no root-reactor membership and
no JPMS descriptor remain explicit. The three sibling `com.m3.residency`
annotations and hash-pinned custody copies are outside the 19-owner count.

### Inventory and residency owner detail

| Source owner | Visibility | Reference | Review scope |
| --- | --- | --- | --- |
| [M3CallSiteCatalog](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3CallSiteCatalog.java) | public | [compiletime-inventory-reference](compiletime-inventory-reference.md) | Complete owner body and declared members; bounded callers/tests read; NOT_RUN |
| [M3ClassScan](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ClassScan.java) | package-private | [compiletime-inventory-reference](compiletime-inventory-reference.md) | Complete owner body and declared members; bounded callers/tests read; NOT_RUN |
| [M3CompileTimeStringInventory](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3CompileTimeStringInventory.java) | public | [compiletime-inventory-reference](compiletime-inventory-reference.md) | Complete owner body and declared members; bounded callers/tests read; NOT_RUN |
| [M3FormatPlan](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3FormatPlan.java) | public | [compiletime-inventory-reference](compiletime-inventory-reference.md) | Complete owner body and declared members; bounded callers/tests read; NOT_RUN |
| [M3InventoryReport](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3InventoryReport.java) | package-private | [compiletime-inventory-reference](compiletime-inventory-reference.md) | Complete owner body and declared members; bounded callers/tests read; NOT_RUN |
| [M3InventoryState](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3InventoryState.java) | package-private | [compiletime-inventory-reference](compiletime-inventory-reference.md) | Complete owner body and declared members; bounded callers/tests read; NOT_RUN |
| [M3SiteRecorder](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3SiteRecorder.java) | package-private | [compiletime-inventory-reference](compiletime-inventory-reference.md) | Complete owner body and declared members; bounded callers/tests read; NOT_RUN |
| [M3Tsv](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3Tsv.java) | public | [compiletime-inventory-reference](compiletime-inventory-reference.md) | Complete owner body and declared members; bounded callers/tests read; NOT_RUN |
| [M3BytecodeOffsets](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3BytecodeOffsets.java) | package-private | [compiletime-residency-reference](compiletime-residency-reference.md) | Complete owner body and declared members; bounded callers/tests read; NOT_RUN |
| [M3EscapeFlow](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3EscapeFlow.java) | package-private | [compiletime-residency-reference](compiletime-residency-reference.md) | Complete owner body and declared members; bounded callers/tests read; NOT_RUN |
| [M3ItemReferenceScan](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ItemReferenceScan.java) | public | [compiletime-residency-reference](compiletime-residency-reference.md) | Complete owner body and declared members; bounded callers/tests read; NOT_RUN |

These 11 owners are disjoint from the eight regex/image/harvest owners. Annotation dependencies in `com.m3.residency` do not add to the 19-owner denominator. Source documentation coverage does not claim test execution, runtime promotion or whole-repository closure.

## Collection factories and delivery-scope continuation

[Collection factories and delivery scope](reconcile-factories-and-delivery-scope.md) reviews the complete `M3Collections` and `package-info.java` bodies at `7abdf87cb497d0d94abeb723d70c50f856ecc381`: one private constructor and all 58 factory overloads, exact M3/JDK/wrapper delegates, immutable shallow-copy/null/order boundaries, and owner-specific qualifications to the package description. Three complete test bodies, selected delegate constructors and build/receiving metadata were inspected. Existing linked-hash, primitive-map and primitive-sequence references are reused, not counted again as newly closed owners.

The pinned package has 77 Java files, plus its separate module descriptor; the full src tree has 78 main and 30 test Java files. The selected installed manifest contains 98 rows (68 main Java, 25 test Java, two probes, three non-Java inputs), with a checksum matching donation metadata. Its linked dated receipt retains a different 95-row/66-main/24-test qualification scope. The source-set verifier compares 93 declared src Java files with 108 present files; a reached equality guard would refuse before compilation. This is source-backed inference, not an executed verifier failure or checksum mismatch.

Evidence state: complete primary bodies and bounded source contracts reviewed; delegate-wide contracts, current-source successor qualification, compilation, recipe replay, tests, JNI, full reactor, JDK compatibility and installed-runtime gates remain open; repository execution is NOT_RUN in this documentation authoring/review pass. Hosted CI is separate exact-commit evidence. Two source files reviewed here do not establish collection-package completion or change the receiving phase.

## Tree and lazy-value contract review

The [tree and lazy-value reference](specify-trees-and-lazy-values.md) adds complete source-body and declared-member review of eleven collection files at `1011050da4205a901ad079d7711b6b8efd8a9af7`: M3TreeMap, M3TreeSet, M3LazyValueProvider, package-private M3LazyValueState, and the seven event/operation owners. The full unfiltered 77-file production-package listing is unchanged at `7abdf87cb497d0d94abeb723d70c50f856ecc381`. Shared slot/progress/factory/locked-owner dependencies are reused without double-counting. Comparator/range contracts, entry lifetime, old-value materialization before mutation, state/retry/reentrancy, event filtering/order/exception precedence, partial preload progress and storage/concurrency boundaries are documented. Nine test bodies were read, including a JUnit wrapper distinguished from direct assertions; builds, examples, repository tests, recipes and performance qualification are NOT_RUN in this documentation authoring/review pass. Hosted CI for a subsequently published commit is separate evidence and must be reported for its exact commit. Complete repository-wide caller mapping, all collection owners, inline package documentation and runtime/receiving acceptance remain open.

## Locked collection and queue contract continuation

At source pin `1011050da4205a901ad079d7711b6b8efd8a9af7`, five additional
`com.m3.collections` owners have complete body/member source references:
`M3LockedMap`, `M3LockedTreeMap`, `M3LockedTreeSet`, `M3BlockingDeque` and
`M3TransferQueue`. [Locked views](specify-locked-map-views.md) and
[blocking/transfer queues](specify-blocking-and-transfer-queues.md) document
lock ownership and scope, callbacks, snapshots, mutation/error behavior,
capacity, waits, interruption, timeout cleanup and transfer acknowledgement.
The [member/test/hash companion](locked-queue-source-coverage.md) records
the exact primary sources and dependency reading boundaries. Reused slot,
array, spliterator, tree and factory sources are not counted again as new
primary owners. All five primary blobs were rechecked unchanged at
`7abdf87cb497d0d94abeb723d70c50f856ecc381`.

Tests and the allocation probe were inspected but **NOT_RUN** in this
documentation pass. No Maven, recipe, native, jtreg, benchmark or runtime
gate was executed as part of this source-documentation review; hosted
exact-head CI remains separate evidence. This continuation does
not establish whole-package completion, JDK replacement, general fairness,
universal no-loss behavior, measured allocation savings or receiving-phase
advancement. Existing broader counts and pending qualification remain intact.

## Collection progress-owner continuation

[Collection progress ownership](specify-progress-ownership.md) reviews all declared members, nested implementations and state fields of eight files at `7abdf87cb497d0d94abeb723d70c50f856ecc381`: `M3ProgressMonitor`, `M3ProgressState`, `M3Progress`, `M3DefaultProgressMonitor`, package-private `M3ChildProgressMonitor`, `M3ProgressAdapters`, `M3ProgressScope` and `M3SystemProgress`. Record components/generated accessors, int/long conversion and validation, cumulative weighting, root/child generation checks, cancellation, local versus aggregate observations, callback errors and scope ownership are included.

Source-observed limitations remain explicit: default-root cross-generation checks are separate from credit; legacy child rewinds/reopens can repeat credits; adapter percentage multiplication can overflow; and exceptional owned-scope close marks the child done and attempts remaining credit subject to parent checks. Existing lazy-provider tests establish specific assertions in source, not executed coverage of all progress contracts. The TreeMap/lazy-state caller review is bounded context and does not double-count those owners.

Evidence state: all eight bodies/member routes source-reviewed, three complete lazy test bodies and their JUnit wrapper read, repository execution is NOT_RUN in this documentation authoring/review pass. Hosted CI is separate exact-commit evidence. Direct concurrent/restart/overflow/listener/scope regressions, recipe/runtime qualification and complete collection-package semantics remain open. This progress family is separate from `com.m3.precompute.M3Progress`.

## Public segmented collection storage contract review

The [stable segmented primitive storage reference](specify-stable-segmented-storage.md) adds complete body and declared-member review of eight public `com.m3.collections` owners at `7abdf87cb497d0d94abeb723d70c50f856ecc381`: `M3Address28`, `M3BitLane28`, `M3IntLane28`, `M3LongLane28`, `M3SlotAllocator28`, `M3LongArena28`, `M3LongLaneStore28` and `M3LongLinkedList28`. All eight blobs and the two direct JUnit test blobs match audit pin `1011050da4205a901ad079d7711b6b8efd8a9af7`. The review covers eight constructors, 127 explicitly declared methods (including the private native declaration and nested callback), 38 fields and nine named types; internal mindex names and the separately documented factory/atomic families are not double-counted.

It records checked 28-bit addressing, sparse nonzero allocation and overlap-safe copies, bitmap occupancy/rank/select, generation changes at release, missing cross-allocator namespace identity, arena offsets versus row/list handles, live-before-initialization ordering, retained leaves, callback mutation and nontransactional error effects. In particular, allocator generation-exhaustion refusal occurs before allocator mutation, while store/list callers can already have cleared payload or relinked nodes. These are source observations, not injected-failure test results.

Both direct JUnit bodies (13 tests), the complete optional JNI C peer, native parity probe, prepared allocation probe, bounded caller chains and build/JNI orchestration were read. Assertion scope and remaining arithmetic/exhaustion/ownership/resource/callback/native-platform gaps are explicit. Builds, tests, probes, recipes, repository scripts, benchmarks and native/runtime qualification are **NOT_RUN in this documentation authoring/review pass**; hosted CI remains separate exact-head evidence. This increment does not claim whole collection-package, JDK compatibility, installed-runtime or repository completion, alter manifest/source denominators, or advance the ordered receiving phase.

## Atomic primitive storage continuation

[Atomic primitive storage](specify-atomic-primitive-storage.md) reviews the complete bodies and every declared member of `M3ConcurrentIntLane28`, `M3ConcurrentLongLane28` and `M3ConcurrentBitSet` at `7abdf87cb497d0d94abeb723d70c50f856ecc381`, with all three blobs unchanged from audit snapshot `1011050da4205a901ad079d7711b6b8efd8a9af7`. Coverage includes nested spliterators, sparse page publication/retention, bounds, wrapping arithmetic, actual CAS versus acquire-only no-op BitSet paths, per-location consistency, range/word traversal and callback consumption. The address/factory/module/build dependencies and complete `M3ConcurrentLaneContractTest` body were read; dependency owners are not counted again as new primary coverage.

Evidence state: bounded source contracts documented; repository code, compilation, tests, scripts, JMM stress, native/runtime integration and memory/performance checks were NOT_RUN in this documentation authoring/review pass; hosted CI is separate exact-commit evidence. Test assertions are enumerated separately from their names, and finite race tests are not treated as universal progress proof. Current-source target qualification, broader consumer protocols and package/repository completion remain open. No receiving phase is advanced.

## Parent-owned residency continuation

[Parent-owned residency lifecycle](specify-residency-lifecycle.md) reviews the complete bodies and every declared member of `M3ObjectResidency` and `M3Residency` at `7abdf87cb497d0d94abeb723d70c50f856ecc381`, unchanged from audit snapshot `1011050da4205a901ad079d7711b6b8efd8a9af7`. Nested interfaces, records, enums, exceptions, state/accounting helpers, callback effects, flags and annotation defaults are included. Two complete JUnit bodies, the pressure probe and workflow, build metadata and existing design/recipe planning evidence were inspected. No separate store implementation, compiler transformation, JNI backend or transparent Java-object eviction is inferred.

The reference distinguishes estimates from measured heap bytes, configured `Decision.targetBytes` from the effective pressure target, conditional pinning from deep escape analysis, and normal transition recovery from source-level overflow/Error/reentrancy limits. These limits are source-backed inferences, not newly executed failures. The named active-slot test only checks oversized first admission; the pressure probe uses declared weights rather than 16 KiB projection allocations. Repository code, tests, scripts and workflows were NOT_RUN in this documentation authoring/review pass; hosted CI is separate exact-commit evidence. Compiler/recipe, actual heap/resource, concurrent/reentrant failure recovery, native/runtime/GUI and wider consumer qualification remain open. Two primary files do not establish package/repository completion or advance the receiving phase.

## Weak-key, object-sequence and identity contract review

The [weak-key lifetime](specify-weak-key-lifetime.md), [object linked-list/deque](specify-object-linked-sequences.md) and [identity specialization](specify-identity-specialization.md) references add complete source-body and declared-member review of four owners at `1011050da4205a901ad079d7711b6b8efd8a9af7`: M3WeakHashMap, M3LinkedList, M3ObjectIdentityMap and M3ObjectIdentitySet. Each exact body is unchanged at `7abdf87cb497d0d94abeb723d70c50f856ecc381`. Shared slot-map/capacity/spliterator/factory dependencies are reused without double-counting; bounded JDK inheritance checks cover linked-list indexing, sublists, arrays and traversal. Seven relevant JUnit source bodies and two standalone probe bodies were read; builds, tests, examples, GC experiments, recipes and measurements are NOT_RUN in this documentation authoring/review pass. Weak-key queue cleanup, strong value/iterator/entry retention, null sentinel versus identity sidecars, reverse/sublist versioning, iterator error order, callback partial effects and allocation limits are documented. Factory identityMap still returns the JDK IdentityHashMap. These references establish neither whole-JDK substitutability nor backend promotion. Other collection owners, repository-wide caller mapping, exhaustive aliasing/GC/allocation-failure behavior, inline package documentation and runtime/receiving acceptance remain separate work. Hosted CI, if run for a later documentation commit, is separate exact-commit evidence.

## Primitive sets, int traversal and heap contract review

The [primitive set reference](specify-primitive-set-variants.md) adds complete-body/member review of `M3IntCollection`, `M3IntIterator`, `M3IntHashSet`, `M3LongHashSet`, `M3LongLinkedHashSet` and `M3ConcurrentLongSet`; the [primitive heap reference](specify-primitive-heaps.md) adds `M3IntPriorityQueue` and `M3LongPriorityQueue`. These are eight primary production files at `33cd1758a4a4613d3868ecde31d6d83400f66aed`, unchanged from audit pin `1011050da4205a901ad079d7711b6b8efd8a9af7`. The existing long collection/iterator, factory and standalone build references are reused dependencies, not additional owner counts. The pages distinguish mutable fail-fast cursors, unsplittable primitive fallback streams, ordered links and immutable snapshot publication, and record concrete method/test/hash evidence. For this documentation authoring/review pass, finite differential and stream assertions are NOT_RUN; the concurrent lane race test is not miscounted as a set-writer race. No repository executable, recipe gate, jtreg, native test or benchmark ran in this pass. Hosted CI is separate evidence recorded against its exact commits, not represented by these source-read statuses. This is documentation coverage, not full package acceptance, JDK replacement, progress/performance qualification or installed-runtime delivery.

## Int-key map specialization continuation

[Int-key primitive map specializations](specify-int-key-map-specializations.md) covers every declared member and complete body of M3IntIntHashMap, M3IntLongHashMap, M3IntIntConsumer and M3IntLongConsumer at `33cd1758a4a4613d3868ecde31d6d83400f66aed`. The four primary blobs match `1011050da4205a901ad079d7711b6b8efd8a9af7`. The M3IntIterator dependency and three complete test files were also read, without counting them as new production owners. The reference distinguishes zero-key storage, absence/default returns, backward-shift deletion without tombstones, exact float-based sizing, partial growth-failure effects, structural key-cursor checks and unguarded callback traversal. It reuses the long-map and factory guides without transferring their differing contracts.

Evidence state: full-body/member source contracts and exact finite test assertions inspected. Tests, builds, recipes, verifiers and runtime experiments are NOT_RUN in this documentation authoring/review pass; hosted CI is separate exact-commit evidence. Callback-mutation regression execution, capacity/failure-injection, concurrency/JMM, heap/performance, full Java SE compatibility and installed-runtime gates remain open. Four source files documented do not independently qualify the whole package or advance the receiving phase.

## Enum specialization continuation

[Enum primitive maps and bit-word sets](specify-enum-specialization.md) covers every declared member and complete body of M3EnumLongMap, M3AdaptiveEnumLongMap and M3EnumSet, including both nested consumer interfaces, at `33cd1758a4a4613d3868ecde31d6d83400f66aed`; all three primary blobs match `1011050da4205a901ad079d7711b6b8efd8a9af7`. Three complete test files were read. The int-long sparse dependency is reused from its own primary reference, not counted again. The guide specifies enum class/null/foreign-type handling, independent presence bits, ordinal arrays/callbacks, exact sparse/direct payload choice and explicit compaction, callback/migration lifetime limits, and the specialized set's word operations, bounded bulk API and non-JDK equality contract. The dense owner's stale deferred-adaptive comment is qualified without changing source.

Evidence state: source contracts and finite test assertions inspected; empty-domain/constant-specific-class deductions and callback-migration hazards remain source-derived, not executed regression results. Tests, builds, recipes, verifiers and runtime experiments are NOT_RUN in this documentation authoring/review pass; hosted CI is separate exact-commit evidence. Concurrency/failure/heap/performance gates, complete JDK compatibility and current-source installed-runtime qualification remain open. These three specialized files and their JDK-returning factory names are not interchangeable coverage units.

## Complete physical collections routing

[Collections physical source coverage](collections-source-coverage.md) gives one primary body-contract route for every file in `m3/collections/src/main/java/com/m3/collections/`: 77 Java files, comprising 76 owner/helper sources plus package-info.java. Complete package paths and Git blobs at `33cd1758a4a4613d3868ecde31d6d83400f66aed` match `1011050da4205a901ad079d7711b6b8efd8a9af7`. The three earlier primary references cover 19 distinct sources; the remaining references route 58. The previously complete read of M3Collections raised the earlier full-body-read count to 20, but its partial factory documentation was not a twentieth fully routed contract. The full factory guide now supplies that route.

The 77-file routing denominator excludes module-info, tests, native code, metadata, same-short-name owners in other packages and catalog-only PENDING specializations. This is complete physical documentation navigation, with per-guide source/member/test limits preserved. It is not blanket behavioral/runtime completion: the separate module has 78 main and 30 test Java files, and current-source successor qualification, receiver checks, recipe replay, full reactor/JDK compatibility, concurrency/GC/resource evidence and installed-image delivery remain independently open. No repository execution or receiving-phase change is claimed by the routing map.


## Portable collection receiver continuation, 15 October 2026

[Portable primitive collection contracts](portable-primitive-collection-reference.md) and its [source/member ledger](portable-primitive-collection-source-coverage.md) review the complete six-file com.m3.util package under m3/ports/collections at c842fab8cf5b799257e3c9ea07f8baddde80db09: M3PrimitiveKind, package-private M3DenseHash, M3PrimitiveArrayList, M3PrimitiveArrayDeque, M3IntArrayDeque and M3PrimitiveMinMaxQueue. All 97 explicitly declared public constructors/methods, eight enum constants, state and non-public helpers are routed. The separate com.m3.collections module and already documented com.m3.ds.M3LongFenwickTree are not double-counted.

Coverage includes lane narrowing/encoding, NaN/signed-zero semantics, default and overflow capacities, snapshots/aliasing, live traversal and callback failure, occurrence movement, lazy predicate masks, throwing empty aliases, specialized int null behavior and the two queue bulk validation orders. The complete five-method package test body, receiver build/workflow and published provenance/recipe/verifier bodies were read. The exact dependency is JUnit 5.11.4-m3-source-1; classpath resolution is unverified. Tests cover selected INT happy paths, not the full behavior matrix.

Builds, tests, examples, verifiers, scripts, recipes, benchmarks and native/runtime qualification are NOT_RUN in this documentation authoring/review pass. Hosted CI is separate exact-commit evidence, and guide-only changes do not match the collection-port workflow's path triggers. Inline package documentation, exhaustive callers, broader test coverage, dependency resolution and target/receiving acceptance remain open. Historical sampled package rows, inventories and sealed recipe payloads remain unchanged; no phase or whole-repository completion is claimed.

## Frozen-text compatibility fixture continuation

The [frozen-text compatibility reference](frozen-text-compatibility-reference.md) and [four-source/member/hash map](frozen-text-compatibility-source-coverage.md) cover all four complete bodies in `m3/compatibility/synexia-reference/src/com/synexia/indexstring` at `c842fab8cf5b799257e3c9ea07f8baddde80db09`: two pinned storage snapshots, one throwing compile-only seam and one executable main test. The 25 explicit public callables and private/package-private support members are mapped, with exact byte/UTF-16 units, cloning versus retained aliases, trusted hashes, range/materialization costs, conditional compaction, errors and lifetime limits. These are four physical fixture files, not four production owners; similarly named retained-port, vendor and canonical String roots are separate scopes.

Both actual test bodies, the compatibility script, foundation build/workflow and public provenance/license/NOTICE were read. The source map resolves the earlier fixture test-routing gap without rewriting historical sampled inventory status. The mapped test uses allocateDirect rather than a mapped file and never invokes the addressed seam; no JNI, alias-exclusion, concurrency or API/owner-equivalence proof is inferred. All repository compilation/tests/scripts/workflows/examples/native/runtime gates and benchmarks were NOT_RUN in this documentation pass. Hosted CI is separate exact-commit evidence. Broader consumer/custody review, inline package documentation, error/alias/resource coverage and target receiving qualification remain open; no package-wide claim across duplicate roots, whole-repository completion or receiving-phase promotion follows.

## Retained regex body coverage (2026-10-15)

- Retained regex root: seven complete source bodies and nested/member boundaries documented at `363c02caa2e5af1df9460b927ecd78f6a9dbcefb` in [reference](retained-regex-reference.md) and [coverage](retained-regex-reference-coverage.md). Public historical oracle bodies read; repository execution NOT_RUN in this documentation authoring/review pass. FS/generic-precompute/shared-array siblings and complete module/runtime admission remain separate

Hosted exact-commit CI is reported separately

## Retained typed precompute supplement — 15 October 2026

[Contract reference](retained-precompute-reference.md) and
[exact coverage companion](retained-precompute-reference-coverage.md) route all
eight public retained `com.synexia.indexstring.precompute` source bodies at
`363c02caa2e5af1df9460b927ecd78f6a9dbcefb`: 39 explicitly declared callables on
public types, generated members, nested support and retained state. The scope
covers identity/validation, callback and artifact aliasing, deterministic CSR
planning, direct dependency access and declared budget/work lifecycle limits.
It does not include a generic executor, cache, persistence codec, package test
body, source-complete module or runtime admission. All repository execution is
NOT_RUN in this documentation authoring/review pass; hosted exact-commit CI is
separate evidence. Historical custody/qualification records retain their scope.
Earlier inventory rows remain historical; this additive map does not claim
whole-repository semantic completion or new product authority.

## Retained shared-array source-review increment

The [retained shared-array reference](retained-shared-array-reference.md) and
[coverage companion](retained-shared-array-reference-coverage.md) account for
all eight physical Java bodies in the retained
`com.synexia.indexstring.shared` package at
`363c02caa2e5af1df9460b927ecd78f6a9dbcefb`, including their nested/private
support. Two complete C++ bodies provide supporting JNI evidence, outside that
eight-file primary denominator. Namespace/content/descriptor/language identity,
input stability, mapping lifetime, copying, conversion caches, corruption and
quota effects are source-reviewed. No retained shared-array tests are present;
missing native inputs and a conditional registration-signature mismatch remain
explicit. Runtime/build/JNI/cross-process/GC/resource verification is NOT_RUN in this
documentation pass; hosted CI remains separate exact-commit evidence.
This does not promote the custody slice, fixture, separate bridge, or canonical
JDK String owner into interchangeable implementation authorities.

<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->

## Retained AST ownership coverage (2026-10-15)

The [ownership and lifetime reference](retained-ast-ownership-reference.md) and [source coverage companion](retained-ast-ownership-reference-coverage.md) cover nine nonoverlapping primary retained AST owners: MIndexAST, MIndexASTPool, MIndexASTDocument, MIndexASTParser, MIndexASTDiagnostic, MIndexASTOccurrenceSource, MIndexASTEditSession, MIndexASTPartialCache and package-private MIndexASTResidentCache.

The companion records 67 member/support groups, both dedicated cache test bodies, exact source hashes/intake rows, missing text/context/grammar/backend dependencies and build/license boundaries. The historical AST intake excludes cache/edit/resident lifecycle qualification. All tests/builds/repository scripts/native loads/benchmarks were NOT_RUN during documentation authoring/review; hosted exact-commit CI remains separate. This does not mark the retained 37-file AST slice source-complete or admit it to the JDK runtime.


## Retained AST navigation source-body coverage (2026-10-15)

The [navigation reference](retained-ast-navigation-reference.md) and
[exact source/member/assertion map](retained-ast-navigation-reference-coverage.md)
cover ten retained `com.synexia.indexstring.ast` bodies at public pin
`363c02caa2e5af1df9460b927ecd78f6a9dbcefb`: Diff, ReuseKey, Query,
PoolPrecompute, Precompute, PrecomputeCodec, LcaSparseTable, SourceIndex,
KindPattern and KindAutomaton, all with the `MIndexAST` prefix. This is bounded
source documentation, not complete package/module or runtime qualification.
The three linked test bodies were read and NOT_RUN; the historical four-method
navigation receipt does not qualify absent general-AST/LCA codecs, hostile-image
geometry, lifecycle, native, matched-runtime or performance gates.

Authoring/review execution is NOT_RUN; hosted CI remains separate exact-commit
evidence.

<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->

## Retained AST specification and routing coverage (2026-10-15)

The [specification reference](retained-ast-specification-reference.md) and
[source/evidence companion](retained-ast-specification-reference-coverage.md)
route eighteen complete public retained bodies through 110 explicit declared-member
and support groups. They preserve actual validation, first-token operator mapping,
registry collisions, ordered multi-candidate dispatch, caller-owned batch outputs,
formal-tree snapshot limits, non-atomic image writes and absent native/image dependencies.

The [37-file AST routing map](retained-ast-routing-map.md) is the unfiltered physical
production-file inventory: nine ownership, ten navigation and eighteen specification
owners, with no overlap. The separately documented filesystem/content-store family
is outside that count. Direct spec/dispatch/image/native test coverage is not present
in retained src/test; historical AST/importer receipts are attributed only to their
bounded scope. Source-body coverage is not source-complete module, consumer, runtime,
performance or promotion qualification. All execution in this authoring pass is NOT_RUN.

Hosted CI is separate exact-commit evidence, not runtime qualification of these retained owners.


## Retained filesystem content-store contract increment (2026-10-15)

The [content-store reference](retained-content-store-reference.md) and its
[eight-source/member/provenance map](retained-content-store-reference-coverage.md)
add primary documentation for exactly eight Java files under
`m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs`.
Coverage includes ID/reference contracts; staged hard-link object publication;
copied, streamed and mapped reads; primitive callback/geometry/profile/text-bundle
semantics; canonical operation/result encodings; cache lock/pointer/mapping flows;
and explicit error, concurrency, atomicity, path-security and recovery limits.

The eight bodies match the original public v7 export. They remain separate from
retained regex, AST, typed-precompute/shared arrays, canonical IndexDB and source
recipe admission. Three referenced fs owners are absent from the retained
subtree, which has no standalone POM/module descriptor or fs test bodies.
Compilation, repository tests/scripts/policy verifiers, recipes, native/JNI,
runtime qualification and benchmarks were NOT_RUN in this documentation authoring/review pass.
Hosted CI, if run, is separate evidence for its exact commit.
No build closure, target admission, phase advancement or wider family completion
is inferred from this documentation increment.
