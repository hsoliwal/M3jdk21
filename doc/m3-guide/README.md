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
