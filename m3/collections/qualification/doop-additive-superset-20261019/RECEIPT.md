<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 is the product — additive array/offset/mask collection superset

**Integration target**: `hsoliwal/M3jdk21`, branch `m3/collections-additive-superset-20261019`. This work does not wrap a JDK with a separate M3 layer. All original `m3/collections` storage and `src/java.base/java.util` runtime owners remain authoritative.

## Two-parent source-only merge provenance

The branch begins on [product PR #1428](https://github.com/hsoliwal/M3jdk21/pull/1428), which includes [#1426](https://github.com/hsoliwal/M3jdk21/pull/1426) and its original AVL maps, sets, masks and direct patched java.base sources. It then has three **true two-parent Git merges** of the complete source of:
1. [#1434](https://github.com/hsoliwal/M3jdk21/pull/1434): original M3PrimitiveFilteredSequences deque add throws correctly, canonical [Synexia #12439](https://github.com/hsoliwal/com.synexia/pull/12439).
2. [#1431](https://github.com/hsoliwal/M3jdk21/pull/1431): original M3LinkedList/M3LinkedHashMap sparse-clear active-lane optimization, canonical [Synexia #12421](https://github.com/hsoliwal/com.synexia/pull/12421).
3. [#1429](https://github.com/hsoliwal/M3jdk21/pull/1429): attributed provider collection fork source audit, no imported production code.

**No** production source was manually edited to integrate those donors: only unchanged named Git blobs from their existing recipe-authoritative heads were admitted where the entire target class matched the shared original master preimage. New paths were verified ABSENT-before. All source histories are reachable through the preserved additional merge parents.

## Single array/index/mask substrate

Original `M3TreeMap` and `M3TreeSet`: GC-traced `Object[]` payload, primitive parent/child AVL offsets and mask state. Original live `M3FilteredViews` / `M3MutableFilteredViews`: read-only or mutable backed and sorted range projections traversing those same slots, entry objects materialized **only when the public Java API requires them**, not internally. `M3PrimitiveFilteredSequences`: one original Deque, predicate admission then matching throwing add/boolean offer dispatch. `M3LinkedList` and `M3LinkedHashMap`: keep existing array/free-slot chain, clear only live references when very sparse and still clear map buckets/tombstones. Original patched `EnumMap` and `IdentityHashMap` belong to `java.base`, not an adapter package. Existing C17 JNI primitive mask selection is an optional batch kernel, never a jobject heap-address owner.

The **unmerged #1427** no-copy M3LinkedList spliterator also edits M3LinkedList and needs a source-level arbitration with the sparse-clear test's post-clear bound Spliterator behavior. It is recorded as HOLD, not silently overwritten/discarded. Similarly five-provider interop receipts with 31 missing or unadmitted files must be fixed through their canonical source recipes, never by loosening tests.

## Earlier, narrowly scoped evidence

- #1428 original combined run [39200001645](https://github.com/hsoliwal/M3jdk21/actions/runs/39200001645): 1,011,273 collection/actual java.base/JNI focused assertions passing. **These tests precede subsequent merges; they cannot qualify this new head by themselves.**
- #1434 original head's `m3-primitive-filtered-sequences-java21` and `m3-object-filtered-deques-java21` Actions **PASS**; source JUnit/upstream OpenRewrite 8.17.1 five tests and local 80,162 assertions. Full original custom `-synexia-source-1` SDK remains unproven.
- #1431 source-bounded existing recipe five real SDK tests, 1,472,788 differential checks per version and synthetic sparse-clear workloads; full target JDK/jtreg not proven.
- #1429 catalogued donors/source rights only, **not** implementation benchmark results.

This source-sealed canonical Synexia leaf emits `SOURCE_PINS.tsv`, `MERGE_PARENTS.tsv`, `CATEGORY_EVALUATION.tsv`, `RECEIPT.md`, a fail-closed ancestry/blob validator and one **target** hosted workflow for exact-head Java21 and existing C17/JNI differential suites. It reuses `M3GpuToolJoinRecipe`, not a second transformation framework. The original production source came from the donors' own already-pinned and separately reviewable Maven/OpenRewrite recipes.

## Explicit promotion HOLD

The target union requires: its own named Maven/OpenRewrite replay/fixed-point and refusal tests; source-identity/ancestor/provenance gate; exact-head Java21 filtered/sorted/deque/linked clear tests; test-only native C17 required mask parity; patched actual java.base 21 diff tests; the source-census repair for the missing interop owners; full JDK image/HotSpot/GC/JNI/jtreg and provider matrix/ABI. **No default branch merge is authorized without these gates.** The user requested merging the PR backlog; this is an additive integration-branch merge of compatible candidates, **not** a claim that a failing exact-head master build is safe to promote. Do not rebase Synexia `develop`, force-update the product branch or discard original donor PR histories.
