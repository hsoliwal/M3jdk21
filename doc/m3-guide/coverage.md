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
