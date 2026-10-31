# M3 filtered and sorted wrapper contract inventory

Status: bounded implementation intake; no target application or promotion implied.

Canonical repository: `hsoliwal/com.synexia`, baseline `11c94feb18155a53cc213708c353791132edc4a7`.
Target runtime delivery remains owned by M3JDK21. This intake does not advance the ordered target receiving gates.

## Instructions and custody

Read before candidate authoring: root `AGENTS.md` (blob `d22084442fb02dee892494d95408ad1865df8e81`), `.claude/skills/m3-recipe-mastery/SKILL.md`, `synexia-openrewrite-recipes/AGENTS.md` (blob `350073a8b810776df31ac52b022d8e45c96303cd`), `synexia-m3-recipe/docs/M3_RECIPE_FIRST_MASTERY_INVARIANT.md`, and the canonical recipe-home manifest. Full nontruncated Git subtree inventories contain no subordinate `AGENTS.md` within `synexia-common` or `synexia-primitives`.

Read-only inventory precedes this document. Exact source copies precede test execution. Candidate output will be materialized only by the canonical recipe engine under parent-agent control. Every changed source atom needs its own preimage/postimage identity and per-pass compiler/behavior proof. Newly authored evidence/tests are candidate fixtures until their actual execution is recorded. Preserve first-party Apache-2.0 attribution; no external donor body is copied.

## Existing public owners

Paths below are relative to the canonical repository.

| Family | Existing source owner | Sealed contract |
| --- | --- | --- |
| M3 read-only collection/set/sorted-set/navigable-set/sorted-map/navigable-map | `synexia-common/src/main/java/com/synexia/common/collections/M3FilteredViews.java` | Six overloads; live backed views; construction evaluates no predicate; no retained element copy; comparator, range checks, order and element identity belong to source; source iterator modification behavior retained; no atomic snapshot/thread-safety/serialization promise. |
| M3 mutable navigable set/map | `synexia-common/src/main/java/com/synexia/common/collections/M3MutableFilteredViews.java` | Adds/replacements must pass predicate; remove only visible values; bulk predicate preflight; range and descending operations remain backed; cursors permit removal after lookahead and do not promise fail-fast; map key-set additions must always be unsupported; entry projections are caller-owned and fresh. |
| Public legacy JDK-style wrappers | `synexia-common/src/main/java/com/synexia/common/collections/jdk/FilteredCollectionView.java`, `FilteredSetView.java`, `FilteredSortedSetView.java`, `FilteredNavigableSetView.java`, `FilteredViews.java` | Public classes and protected hooks already exist; retain each signature. Factory may preserve navigable capability for a sorted source. Legacy contains catches predicate `ClassCastException`; M3 contains propagates it. Legacy null bulk arguments are checked before read-only refusal; M3 directly refuses. These are distinct compatibility surfaces. |
| Primitive live long views | `synexia-common/src/main/java/com/synexia/common/collections/LongFilteredView.java`, `LongFilteredSortedSetView.java` | Primitive traversal and no selected-element copy; stable predicates/no concurrent mutation; long navigation uses explicit default values and disambiguates `Long.MIN_VALUE`/`Long.MAX_VALUE`; sorted wrapper reuses existing filtered traversal. |
| Explicit primitive snapshots | `synexia-common/src/main/java/com/synexia/common/collections/M3FilteredLongSnapshots.java` | Frozen result; validates signed ascending input before predicate evaluation; Java callback exactly once per element in source order; decision mask and stable compaction reuse canonical owners; JNI_REQUIRED requires an explicitly host-loaded existing library; no native Java predicate callbacks or retained pointers. |
| Primitive admission and indexed wrappers | `synexia-primitives/src/main/java/com/synexia/primitives/PrimitiveIndexView.java`, `PrimitiveFilteredList.java`, `PrimitiveFilteredSet.java`, `M3SortedIndexedLongList.java` | PrimitiveFilteredList/Set validate only new additions; preexisting delegate contents are not filtered. PrimitiveIndexView keeps a source-generation-checked int[] row projection; filtering is eager over row IDs, and mutations invalidate the view. M3SortedIndexedLongList retains stable issued IDs, tombstones, separate payload/sort lanes, and a flat-to-AVL ordering index. These contracts are distinct from live JDK wrappers. |
| Lazy/sorted selection | `synexia-primitives/src/main/java/com/synexia/primitives/M3LazyTopKWindow.java`, `M3SortableTopK.java`, `M3SortableTopKIds.java`, `M3SortableWindowOrder.java`, `M3SortableFirstSignalOrder.java`, `LongLazyPriorityFrontier.java` | Existing lazy frontier, top-K/window and signal owners; input generation/order/retention contracts must be checked before reuse. Their focused proof POMs already exist. |
| DOOP parent-owned tree | `synexia-primitives/src/main/java/com/synexia/primitives/M3DeferredChildTree.java`, `M3ParentOwned.java`, `M3TransientFacadeStore.java`, `M3DoopMaskedWindowScan.java`, `M3DoopFrozenMaskedViewport.java` | Parent-owned primitive state and bounded optional materialization already have source/recipe/native owners. Filtering a JDK collection does not move widget lifetime or canonical payload into a new wrapper framework. |

Additional physical sorted owners already include common `DenseArraySortedMap/Set`, `DenseSortedBuffer`, `DenseSparseSortedBuffer`, `PackedLong{Batch,Gap,Paged,Sparse,}SortedSet`, parallel long-long map variants and primitive sorted array/gap/multimap/table families. Their existence is an inventory fact, not a blanket compatibility or performance claim.

## Exact first candidate atoms

1. `M3FilteredViews.NavigableSetView.descendingIterator`: remove intermediate private filtered-view wrappers and call the existing filtering cursor over the **same** `nav.descendingSet().iterator()` path. Do not switch to `nav.descendingIterator()`, since a custom backing implementation can expose a different call trace. Result cursor type, predicate order, read-only exception and backing iterator policy remain unchanged.
2. `M3FilteredViews.SortedMapView.isEmpty`: use the existing filtering cursor over `sorted.entrySet().iterator()` directly. The current method creates an anonymous entry-set wrapper and an extra entry-iterator wrapper even though `hasNext` never exports an entry. Preserve exactly the backing entry-set/iterator calls and predicate key/value access order.

These are construction-count hypotheses, not measured speedups. Capture bytecode/allocation evidence separately; require exact external trace parity on frozen cases. No change to map entry export, mutation, null behavior, comparators, range policy or iterator state is admitted by these atoms.

## Behavior gaps to reproduce, not silently repair

- Read-only M3 and legacy navigable-set neighbor methods use `null` as exhaustion. A null-friendly comparator can store a rejected null that must be skipped to a visible neighbor. Existing `M3_FILTERED_WRAPPERS_AND_JNI_20261016.md` explicitly defers null navigation ambiguity to the JDK interface. Preserve the independently reproduced projected-TreeSet mismatch as a documented limitation under review, not an automatically authorized bug fix.
- Mutable navigable-set neighbor methods use `headSet`/`tailSet` for each query. A query outside an already bounded backing range may throw although navigation on the independently projected bounded set returns a boundary neighbor or null. Reproduce both orders and inclusive/exclusive ranges before authoring a change.
- Do not normalize all wrappers onto one implementation: predicate exceptions, unsupported mutations, source call traces and range return types differ.

## Existing recipe seams and historical custody

`M3FilteredCollectionViewsRecipe` extends the existing `M3GpuToolJoinRecipe` installer and binds `/com/synexia/rewrite/m3-filtered-collection-views-20261016/manifest.tsv`; template-04 is the exact M3 read-only source, template-07 its main contract, template-09 its focused proof POM. `M3MutableFilteredNavigableViewsRecipe` binds the parallel mutable packet; template-00 is source, template-01 main contract, template-05 focused POM. Legacy wrappers are already bound by `M3HashPinnedJavaSnapshotRecipe` crate `filtered-sorted-collection-views-20261016`.

Do not replay an ABSENT-only installer over evolved owners or weaken its refusal hashes. New private-atom postimages need a forward, exact-preimage crate and must retain existing recipe lineage. The reusable scanner/Java-LST renderer is already `M3HashPinnedJavaSnapshotRecipe`; source identity/syntax admission does not prove executable contracts. `M3SerialFileAtomConvergenceRecipe` composes catalogue and donor review into a work order and explicitly grants no mutation/promotion authority.

Relevant bounded history: M3 read-only owner was introduced at `72e1c0bded4806643790fd22f96f6d61b6cafb97`; mutable owner at `0c371e84fc4e7e7693e1770cd0f1e5fe7fd4df84`, followed by map-key-set unsupported-add correction `047171f56c0b211e7f1d8abaafe1bb9300bd504f`. The latest immutable baseline retains that correction. API and behavior parity must preserve it.

## Frozen verification plan

Run Java 21 lint with warnings as errors before executing probes. Reuse existing `M3FilteredViewsContract`, `M3MutableFilteredViewsContract`, primitive and legacy contracts. Add bounded independently modeled wrapper probes covering empty/all-rejected/all-visible/sparse predicates, ascending/reverse/null-friendly comparators, collections vs sets vs sorted/navigable sets and maps, live owner changes, original reference identity, repeated lookahead, exhaustion, exceptions, range/descending composition, unsupported mutations, nullable map values and caller-owned entry identity.

For both private construction atoms freeze a boundary-event oracle before authoring payloads: log backing collection method calls, source iterator events, entry getter access, predicate evaluations and injected exception positions. Replay each candidate from the exact baseline, serially per file, then replay at fixed point and refuse source drift. Do not infer JNI parity from Java wrapper tests. Existing JNI snapshot/DOOP proof remains a separate gate. Root owns actual Maven/OpenRewrite SDK execution, reactor integration and remote PR publication.

## Catalogue and donor direction

The existing searchable owners are in `synexia-algo/src/main/java/com/synexia/algorithms/corpus`: `AlgorithmCatalogue`, `ChallengeCatalog`, `ChallengeSearchIndex`, `ChallengeSearchCli`, `ChallengeCategoryCatalog`, `ChallengeDonorReviewPlan`, and `ProblemShapeExecutionCatalog`. Extend their evidence through existing full-review/serial-file recipes; no new algorithm registry is introduced.

Category review uses primary problem pages as metadata/adversarial shapes only: LeetCode Peeking Iterator (https://leetcode.com/problems/peeking-iterator/), HackerRank Java Iterator (https://www.hackerrank.com/challenges/java-iterator/problem), and GeeksforGeeks Floor in Sorted Array (https://www.geeksforgeeks.org/problems/floor-in-a-sorted-array-1587115620/1). They do not specify this complete mutable/live/null/range contract. The GeeksforGeeks page returned only its shell during initial retrieval; no claim of reviewing its unseen statement is made.

Guava filtered-set APIs are a semantic donor comparison, not an implementation replacement: https://guava.dev/releases/33.4.8-jre/api/docs/com/google/common/collect/Sets.html. Source revision pinned for review is `google/guava@5e82f2f528fbbd970e00748025b48e3eb42ed702`; license/source inspection is recorded separately before any source admission. All challenge statements, editorial and submission bodies remain uncopied.

## Additional primitive index atom intake

Parent-authorized bounded candidate: `synexia-primitives/src/main/java/com/synexia/primitives/PrimitiveIndexView.java`, baseline blob `66f57ee526387826f55a13f9737f0cb8732e6b45`. In `filterLane` and `filterStateMask`, the temporary selected-row array is already exact length when every input row passes. Returning that call-local array in the new private view can avoid an identical `Arrays.copyOf`; all partial-selection branches retain the copy. Neither the source projection array nor caller-owned exports may be reused. Public/protected API, callback count/order, the pre/post source-version checks, exception precedence, primitive lane contents, metadata and defensive exports remain sealed.

Freeze an independent fixture before authoring its payload. The oracle will cover all eight primitive kinds; zero, sparse and full admission; ascending/reverse/sliced row projections; signed mask behavior; null-predicate versus stale-view precedence; invalid-lane behavior including empty views; callback failure identity/order; mutations during callbacks including changed later lane values; source-generation invalidation; and defensive `toIndexArray` / `materialize` exports. Compile the exact six-file dependency closure (PrimitiveIndexView, PrimitiveSoaStore, PrimitiveKind, M3SortableTopK, DenseHash, M3RuntimeHeap); no stubs. Each method atom gets its own exact-hash intermediate crate; a final baseline-to-final crate is the convergent delivery unit. Existing LongFilteredView exact-fit copy is a first-party algorithm-shape reference, not permission to weaken the primitive view contract.
