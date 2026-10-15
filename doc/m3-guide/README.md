<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21 repository → project → module → package guide

This guide describes the source snapshot `278b221e8365ebeb6095a3a5643b38a0564258b5` inspected on 14 October 2026. Its production source is the same as the documentation PR base `d5abed865bd47c4dd5ad78e95704348fe2095ed2`; the intervening changes are the existing README and contributor guide. This is an additive navigation and contract-documentation pass, not a runtime qualification or a replacement for canonical manifests.

## Start with the boundary you own

1. [Repository and project architecture](projects.md): distinguish the OpenJDK image, independent M3 modules, authoring tools and source custody
2. [All 70 JDK modules](jdk-modules.md): purposes, real descriptor exports, dependency direction and service wiring
3. [M3 package responsibilities](m3-packages.md): 44 source-sampled roots, their APIs, effects, dependencies and explicit gaps
4. [Runtime owner boundaries](runtime-owners.md): String, regex, internal storage and bulk execution
5. [Complete package-directory accounting](packages/README.md): 13,793 rows, with declaration and semantic-review status
6. [Build/module descriptor ledger](descriptors.md): every tracked POM and JPMS descriptor, including fixtures and vendors
7. [Coverage, verification and next work](coverage.md): what was actually inspected, what remains unknown and how to continue

For the concrete interned-descriptor bridge and explicit precompute boundaries, see the [indexed-string API reference](indexed-string-reference.md). It distinguishes text equality from backing identity, cached facts from canonical storage, and retained views from materialized output.

## What this repository is

M3JDK21 is an OpenJDK 21-derived runtime research and engineering target. The root [README](../../README.md) and [runtime integration guide](../../m3/runtime-integration/README.md) distinguish current source, experimental feature gates and planned milestones. The full JDK image is built with OpenJDK configure/make; the Maven control reactor is a different authoring/proof artifact. Separate M3 libraries may also run on a stock Java 21 installation.

The official upstream baseline recorded by the runtime guide is OpenJDK `jdk-21+35`, commit `890adb6410dab4606a4f26a942aed02fb2f55387`. This guide does not claim the current fork is identical to that upstream release or that every later backport has been accepted.

The portable algorithm/recipe convergence authority remains Synexia. M3JDK21 owns its target Java/HotSpot/JNI integration and acceptance. A copied donor file, a source hash, a passing donor test or a merged PR is not a whole-runtime acceptance result. Source custody under `m3/vendor/` is not a second canonical recipe implementation.

## Build, test and contribution routes

Use the existing [first-build checklist](../../CONTRIBUTING.md#first-build-prerequisites-outputs-and-test-selection), [OpenJDK building guide](../building.md), [testing guide](../testing.md), [M3 foundation guide](../../m3/README.md) and [native build guide](../../m3/docs/native-build.md). The last two are hash-sealed inputs and were not edited by this pass.

The current-source runtime guide identifies `UseM3StringStorage` as experimental, default-off and interpreter-constrained. Select a complete matched JDK image; do not transplant String.class into another VM. Feature flags, image selection, JNI/native libraries and test configuration are separate prerequisites.

Commands linked here are source-backed reproduction instructions. No compiler, Maven reactor, shell build, jtreg suite, native test, benchmark or runtime was executed for this guide.

## Authority, licensing and evidence

- [AGENTS](../../AGENTS.md), [porting invariant](../../m3/docs/m3jdk21-porting-invariant.md) and [name mapping](../../m3/docs/name-mapping.json) control placement and receiving decisions
- Receiving remains **String → arrays → collections → AST/compiler → remaining families**; preparatory documentation does not advance a phase
- [OpenJDK license](../../LICENSE), [additional license information](../../ADDITIONAL_LICENSE_INFO), [M3/Synexia notice](../../M3-SYNEXIA-NOTICE.md), [rights](../../M3-SYNEXIA-RIGHTS.md) and per-file notices remain authoritative
- Existing package Javadocs are linked instead of copied wholesale. Missing or unreviewed inline documentation remains an explicit backlog item
- Proposed mechanical-agent architecture is design input, not evidence that this repository implements every proposed engine or invariant

The inventory is complete for the pinned tracked tree. Substantive package documentation is still partial. See the [coverage ledger](coverage.md) before making a completion claim.

For the other public forks, use the [public fork documentation index](public-forks.md) for pinned guides, module/package maps, open coverage and separate CI evidence.

For the linked-hash map/set family, see the [collection API and storage reference](collections-reference.md): parallel payload/control arrays, bucket masks and tombstones, access order, live/reversed views, entry projections, callback effects, and explicit JDK-compatibility boundaries.

For primitive long-to-long maps, see the [primitive and snapshot map reference](primitive-map-reference.md): differing insertion/replacement results, mutable tombstones and fail-fast traversal, immutable signed-key snapshots, copy/CAS costs, stream lifetime and bounded test evidence.

For lexicon export identity, eager/lazy admission and derived-family scope, see the [lexicon catalog reference](lexicon-catalog-reference.md): source records versus physical coordinates, sidecar verification, close/retry behavior and typed precompute contracts.

For packed code/regex features and candidate ranking, see the [precompute batch and similarity reference](precompute-batch-reference.md): stable-input obligations, packed lanes, heuristic versus exact facts, budget accounting, progress/cancellation, explicit native loading and JNI copying/lifetimes.

For primitive long lists, stacks, queues and deques, see the [primitive sequence reference](primitive-sequence-reference.md): method aliases and empty results, capacity retention, callback partial effects, iterator versus spliterator mutation rules, batch defaults and polling progress limits.

For immutable byte/UTF-16/int/long views, see the [joined-array reference](joined-array-reference.md): every public member, snapshot versus retained ownership, copy and buffer boundaries, UTF-16 seams, explicit JNI loading/materialization, actual tests and two source-observed implementation gaps.

For exact UTF-16 prefix facts and mutable point sums, see the [indexed algorithm reference](indexed-algorithm-reference.md): every PrefixZ/Fenwick entry point, budget and cancellation checkpoints, half-open indexing, checked intermediate overflow, failure-atomic staging, memory accounting and explicit test gaps.

For literal recognition, regex/String facts, finite trial matrices and candidate-pruning checks, see the [regex precompute and oracle reference](regex-precompute-reference.md): flags and dialect boundaries, mode/class masks, exact versus approximate evidence, input/hash identity, budgets/cancellation, ranking materialization and the absence of a trial-image serialization API.

The [precompute production-source coverage ledger](precompute-source-coverage.md) links all 23 production Java files, distinguishes public owners from internal dependencies and records remaining method/test/runtime gaps.

For the isolated CPU-tool DAG, see the [CPU-tool plan, image and runtime reference](cpu-tool-precompute-reference.md): immutable declarations and prepared KMP state, versioned image/TSV validation, all 13 binary operators, snapshot/materialization boundaries, digest receipts, heap admission, and the optional Java/JNI lexical parity route.

For bootstrap String internals in `java.base`, read [ownership and admission](bootstrap-string-ownership.md) for native/mapped atoms, exact tuple-layout reuse, weak cleanup and refusal, then [String operations and facts](bootstrap-string-contracts.md) for descriptor-backed operations, facts, encoding and stream boundaries. Both are source-reviewed implementation references with execution gates still NOT_RUN.

For the remaining bootstrap precompute owners, see [String precompute contracts](bootstrap-string-precompute.md): exact UTF-16 search and code-point geometry, lazy position masks, the bounded dot/star decision, heuristic code-text receivers and the separate Java/JNI provider boundary. The [twelve-owner source map](bootstrap-string-source-coverage.md) records complete body-contract routing for the twelve P01 M3-prefixed files while keeping public API, whole-package and runtime qualification open.

For the bootstrap regex adapter, see [Pattern query and Boolean-plan contracts](bootstrap-pattern-query-contracts.md): complete package-private M3PatternQuery member coverage, compiled-node admission/refusal, region/input-bound trigram reuse, failed-search state, restricted Boolean DP and exact fallback/error boundaries. Direct Pattern/Matcher/String paths and six regex test bodies are source-reviewed; wider regex APIs and execution remain open.

For the independent IndexDB receiver, read [persistence and V1 snapshots](indexdb-persistence-reference.md) for artifact/ref replacement, handle lifetime, publication/error effects, wire framing, corruption checks and resource limits; then [semantic graphs and fingerprints](indexdb-semantic-reference.md) for all nine production owners, stable row identity, ordered roles, child-first recomposition, candidate-only queries and the retained `com.synexia.mindex.db` compatibility type.

For internal text-v3/byte-v1 mapped storage, see the [mapped String backing, logical addresses and lifetime reference](mapped-string-storage-reference.md): complete contracts for six `jdk.internal.mindex` owners, commit/CRC/UTF-8 admission, aliases and ignored metadata, buffer/close behavior, snapshot-local class boundaries, exact release labels and 28-bit arithmetic. The bootstrap lexicon/native path and its JNI/VM prerequisites remain explicitly separate.

For frozen class/member metadata and explicit reflection boundaries, see the [internal class and member view reference](class-member-view-reference.md): nine complete source bodies, owner/row versus typed-wrapper identity, optional metadata, enumeration order, descriptors and flags, weak/compact lifetime, live lookup refusal and actual test limits.

The [internal primitive lanes and metadata helpers reference](internal-metadata-lanes-reference.md) covers seven complete `jdk.internal.mindex` owners: sparse mutable int/long/bit lanes, their native count boundary, the synchronized class bridge, VM descriptor encoding, exact-input context keys and bounded version indexing. It distinguishes snapshot coordinates, aliasing, lifecycle and comparison semantics from portable IDs or automatic JDK consumers.

For the immutable Class snapshot and UTF-16 candidate facts, see [class-index contracts](class-index-reference.md) and [trigram query/fact contracts](trigram-query-reference.md). The [complete internal mindex source map](internal-mindex-source-coverage.md) routes all 24 physical Java files to bounded body-level references, while keeping public JDK compatibility, executable coverage and runtime qualification separate.

For the remaining independent text owners, see the [text projection, tool and receipt reference](text-projection-reference.md): generated numeric views and explicit materialization, operator-supplied instance indexing, fixed grammar-support signals, CLI image effects and LangDex verification without catalog admission. The [seventeen-source text map](text-source-coverage.md) routes every physical `com.m3.text` owner to its bounded source-contract review while preserving the earlier storage/catalog references and unexecuted-test status.

## Compile-time inventory, regex and residency contracts

- [Compile-time source coverage](compiletime-source-coverage.md): all 19 physical
  `com.m3.compiletime` production source owners, including internal helpers,
  with separate inventory, regex, receipt and residency routes and runtime gaps
- [Compile-time regex analysis, image and gate](compiletime-regex-reference.md):
  necessary formulas versus exact literals, content keys, packed lanes,
  verification/retained-storage limits and mutable gate scratch
- [Constant-regex harvest and differential receipts](compiletime-harvest-reference.md):
  CLI/file schemas, exact discoveries versus sampled inputs, separate
  harvested/oracle comparisons, measurement scopes and custody limits
- [Compile-time inventory and report reference](compiletime-inventory-reference.md): catalogue matching, constant operands, Formatter plans, exact TSV schemas, state/resource/error boundaries and eight-owner declared-member map
- [Compile-time item-residency reference](compiletime-residency-reference.md): hierarchy/escape rules, exact 14-column site TSV, summary/meta records, bytecode offsets, binary sidecar and three-owner declared-member map

For every collection factory overload, read [collection factories and delivery scope](reconcile-factories-and-delivery-scope.md): exact M3/JDK/wrapper routing, immutable shallow-copy/null/order boundaries, package concurrency qualifications, and the distinct current-source, installed-manifest and historical-receipt inventories. The source-set verifier's conditional refusal is documented as a static inference; no verification command was run.

For AVL maps/sets and explicitly deferred values, see the [tree and lazy-value reference](specify-trees-and-lazy-values.md): comparator-equivalent keys, nested/descending ranges, key-only views, provider state/retry, callback guards, event masks/order, progress ownership and failure effects. Eleven source bodies and focused unrun test assertions are mapped without implying collection-package or runtime completion.

## Locked collection and queue contracts

- [Locked maps, sorted sets and backed views](specify-locked-map-views.md): shared-monitor scope, callbacks, snapshot entry lifetime and conditional removal
- [Blocking deque and transfer queue contracts](specify-blocking-and-transfer-queues.md): capacity, waits, interruption, rendezvous visibility, acknowledgements and drain failure
- [Locked collection and queue source coverage](locked-queue-source-coverage.md): five full-body owner reads, declared-member routes, source hashes and inspected tests; repository execution NOT_RUN in this documentation pass, with hosted exact-head CI tracked separately

For the collection progress SPI and its legacy adapter, read [progress ownership, cancellation and observation](specify-progress-ownership.md): all eight source owners, weighted child generations, borrowed/owned scope close, long versus int totals, cancellation propagation, snapshot consistency, callback failures, and explicit source-observed arithmetic and restart limits.

For public `com.m3.collections` address, lane, slot, arena, row and handle-linked list owners, see [stable segmented primitive storage](specify-stable-segmented-storage.md): 8/8/12 checked geometry, sparse zero/bulk overlap, occupancy/rank/select, allocator-local generation handles, allocation versus publication, callback/partial-failure behavior, retained-payload accounting and explicit JNI counting. These are distinct from the similarly named internal mindex owners.

- [Atomic primitive storage](specify-atomic-primitive-storage.md): concurrent int/long lane publication, retained sparse pages, scalar memory effects, fixed-range streams and fixed-capacity BitSet word semantics; matching no-op bit CAS is acquire-only, and multiword scans are not atomic snapshots

- [Parent-owned residency lifecycle](specify-residency-lifecycle.md): canonical parent words, scoped projection access, conditional pinning, estimate-based admission, swap/restore/recreate, monitor-held callbacks, source-level failure limits and inert CLASS-retained annotation metadata

- [Weak-key map lifetime](specify-weak-key-lifetime.md): equality, GC reachability and queue cleanup, backed views, live-entry/key retention, callback errors and storage limits
- [Object linked-list and deque](specify-object-linked-sequences.md): nullable endpoints, reversed/sublist ownership, reusable slots, iterator state, bounds and late-binding traversal
- [Reference-identity map and set](specify-identity-specialization.md): identity versus equality, null sidecars, strong snapshots, narrow API/factory boundaries and growth failure order

For primitive membership and min-heaps, see [primitive set variants](specify-primitive-set-variants.md) and [primitive heaps](specify-primitive-heaps.md): zero-sidecar/backshift versus tombstone sets, encounter-order moves, immutable concurrent snapshots, exact int traversal defaults, signed heap order, capacity and failure boundaries.

- [Int-key primitive map specializations](specify-int-key-map-specializations.md): zero-key/value presence, exact sizing, backward-shift deletion, primitive return conventions and the different cursor/callback mutation contracts of M3IntIntHashMap and M3IntLongHashMap
- [Enum primitive maps and bit-word sets](specify-enum-specialization.md): enum-domain/null validation, ordinal traversal, dense and adaptive payload rules, explicit compaction, reentrant-migration limits and M3EnumSet's bounded equality/bulk API
- [Collections physical source coverage](collections-source-coverage.md): exact primary-reference routing for all 77 package Java files, reconciling 19 earlier body-contract routes with 58 remaining files while separating documentation from runtime and receiver qualification


## Portable collection receiver contracts, 15 October 2026

- [Portable primitive collection contracts](portable-primitive-collection-reference.md): exact-width carriers, list and ring-deque ownership, specialized int-deque legacy behavior, sorted min/max queue merging, capacity/error/callback contracts and the independent ports/collections build
- [Portable collection source coverage](portable-primitive-collection-source-coverage.md): all six com.m3.util production bodies, 97 explicit public callable routes, implementation helpers, exact target hashes and five inspected test methods; repository execution NOT_RUN

The sibling Fenwick owner remains covered by [indexed algorithms](indexed-algorithm-reference.md). These receiver utilities are distinct from the main com.m3.collections module and its [primitive sequence](primitive-sequence-reference.md) and [linked-hash](collections-reference.md) contracts.


## Frozen-text compatibility fixture contracts, 15 October 2026

For the separate test-only frozen-text fixture, see the [frozen-text compatibility reference](frozen-text-compatibility-reference.md): byte/UTF-16 ownership and sharing, trusted direct admission, copy/materialization and compaction boundaries, the throwing addressed-view seam, and the actual differential/direct-memory tests. The [four-source/member/hash map](frozen-text-compatibility-source-coverage.md) covers all 25 explicit public callables without conflating retained-port or canonical runtime owners; repository execution remains NOT_RUN.

## Retained regex contracts (2026-10-15)

- [Retained regex programs, images and candidate facts](retained-regex-reference.md): seven original-package custody owners, compile/session and workspace lifetime, v1–v7 codec transport, persisted request identity and UTF-16 trigram facts; [exact coverage and evidence](retained-regex-reference-coverage.md)

Repository tests/builds were NOT_RUN in this documentation authoring/review pass; hosted exact-commit CI is reported separately

## Retained typed precompute contracts — 15 October 2026

- [Typed keys, scopes, passes, graph plans, direct-dependency contexts and budget helpers](retained-precompute-reference.md)
- [Exact eight-source/member/hash, caller, test and provenance map](retained-precompute-reference-coverage.md)

This retained source slice has no executor/cache/codec or standalone module;
source routing does not promote it to the canonical JDK runtime. Execution is
NOT_RUN in this documentation authoring/review pass; hosted exact-commit CI is
separate evidence.

## Retained shared-array custody contracts

The [retained shared-array reference](retained-shared-array-reference.md) covers
arena admission, namespace/range handles, language aliases, immutable segment
views, copies, conversion-result caching and optional JNI boundaries. Its
[eight-body source map](retained-shared-array-reference-coverage.md) records
exact provenance, missing build dependencies and a conditional native
registration mismatch. The retained slice is not a complete module or JDK
runtime dependency; tests and native loads are NOT_RUN in this documentation
pass. Hosted CI remains separate exact-commit evidence.

<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->

## Retained AST ownership and lifetimes (2026-10-15)

- [Retained AST ownership and lifetime reference](retained-ast-ownership-reference.md): pool-local atom identity, immutable document occurrences, parse-only partial trees, cache load/upgrade/pressure/eviction boundaries and retained single-source edit sessions
- [Exact source/member/test/custody coverage](retained-ast-ownership-reference-coverage.md): nine primary owners at the pinned public revision, complete member/support routes, two cache test bodies and explicit dependency/qualification limits

These are retained-source contracts, not source-complete module or JDK runtime admission. Tests/builds/repository scripts/native loads/benchmarks were NOT_RUN during documentation authoring/review; hosted exact-commit CI is separate evidence.


## Retained AST navigation and candidate queries (2026-10-15)

The [retained AST navigation reference](retained-ast-navigation-reference.md)
explains occurrence/preorder/source-offset coordinates, canonical pool postings,
query planning, exact versus candidate reuse, kind-pattern scans and the bounded
navigation image format. The [source and assertion map](retained-ast-navigation-reference-coverage.md)
records ten complete public source-body reviews and keeps the four-method
historical oracle, missing codec bodies and current NOT_RUN gates distinct.

Authoring/review execution is NOT_RUN; hosted CI remains separate exact-commit
evidence.

<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->

## Retained AST specifications and complete routing (2026-10-15)

- [Retained AST specifications, production plans and dispatch](retained-ast-specification-reference.md): eighteen retained owners covering Java language metadata, grammar-derived facts, ambiguity-preserving dispatch, formal-tree geometry, snapshot limits, image writing and the optional native boundary
- [Specification source and evidence coverage](retained-ast-specification-reference-coverage.md): exact public source/member/hash/provenance routes, test limits and unresolved dependency/qualification gates
- [Complete retained AST production-source routing](retained-ast-routing-map.md): all 37 physical files assigned once across ownership, navigation and specification references, including non-prefix and package-private owners

These are custody-source references, not source-complete build or JDK runtime admission. Examples, tests, native providers and benchmarks were not run in this documentation pass.

Hosted CI is separate exact-commit evidence, not runtime qualification of these retained owners.


## Retained filesystem content-store contracts (2026-10-15)

- [Retained content objects, mappings and precompute persistence](retained-content-store-reference.md) explains all eight retained `com.synexia.indexstring.fs` source owners: IDs, exact-byte object publication, primitive lane/bundle formats, mapped ownership, operation keys, positive/negative cache pointers and failure/recovery limits
- [Exact source/member/evidence map](retained-content-store-reference-coverage.md) records every primary body and member group, hashes, public v7 provenance, two caller boundaries and absent test/dependency closure. This is retained source documentation, not a standalone module, canonical IndexDB or JDK runtime admission

## Lucene canonical String opportunity map — 15 October 2026

For Lucene integration opportunities, see the [canonical String opportunity reference](lucene-string-opportunity-reference.md) and [source/version ledger](lucene-string-opportunity-source-coverage.md): mutable token buffers versus retained atoms, exact UTF-8/offset/graph contracts, the verified Lucene 10.3.1 JDK21 lane, and a bounded recipe/test/benchmark plan. This is a source-backed proposal; no adapter, performance gain or runtime qualification is claimed.


## OpenNLP string opportunity research — 2026-10-15

- [OpenNLP string opportunity reference](opennlp-string-opportunity-reference.md): source-pinned token/span, dictionary, feature and normalization candidates; separates the existing 2.4 tool pack from the 3.x research fork, with model/state/offset boundaries and matched-interpreter qualification criteria
- [OpenNLP opportunity source coverage](opennlp-string-opportunity-source-coverage.md): exact public source identities and explicit unexecuted gates
