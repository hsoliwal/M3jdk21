<!-- SPDX-License-Identifier: Apache-2.0 -->
# Unsized, late-bound M3 flatMapped collection cursor — canonical recipe

**Receiver**: M3JDK21 isolated `m3/collections` purpose wrappers, stacked on
[M3JDK21 PR #992](https://github.com/hsoliwal/M3jdk21/pull/992) exact parent
`11dde1eac33cea2a8b90baba8c155eabec341144`.
**Canonical recipe**: Synexia Maven/OpenRewrite
`com.synexia.m3.FlatMapUnsizedMetadata20261017` delegating to the existing
`M3HashPinnedJavaSnapshotRecipe`. No new code generation engine.

## Why this leaf rather than another collection wrapper?

The first-party `M3CollectionWrappers.flatMappedView` facade already delegates to
`M3MappedViews.FlatMappedCollection`, where mapper results can contain zero,
one or many objects. The inherited `AbstractCollection.spliterator`
incorrectly advertises SIZED/SUBSIZED and discovers its estimate by calling
`size()`, traversing the mapper before actual stream consumption.

**Red baseline, actually executed** on exact M3MappedViews source Git blob
`4c2ffbfc358eb0c161227e46ed0b0cc6bfbeb85f`:
a 3-row, two-output-per-row oracle called the mapper **4 times** for
`stream().findFirst()` and **3 times during `estimateSize()`**. The test
`M3FlatMapCursorProof` fails at `flattened exact size was guessed` on
this original source.

**Green candidate, actually executed** Git blob
`908801c809684b58762357dba1afba3ab36aedd9`:
`M3_FLAT_MAP_CURSOR_PASS checks=9633`. Identical 3-row fixture
`stream().findFirst()` calls mapper **once**; `estimateSize()` and
`characteristics()` call mapper zero times and do not consult source.size().

The cohesive file-local private refinement reuses its original two-level
iterator, overriding only flattened `spliterator` and saturating `size()`:
- A one-traversal source cursor, instantiated only on first element request.
  The `ORDERED` bit follows `source.spliterator()`; no false
  `SIZED, SUBSIZED, NONNULL, DISTINCT, SORTED, IMMUTABLE, CONCURRENT`.
- Unknown estimate `Long.MAX_VALUE`; `trySplit()` returns null to prevent
  concealed chunk buffers. Parallel traversal remains **functionally correct,
  not optimized to run in parallel**.
- `Collection.size()` returns `Integer.MAX_VALUE` at the JDK-mandated ceiling
  rather than throwing on oversized flattening. Exact ordinary sizes remain
  unchanged. The 2.1B-element limit isn't directly load-tested; normal cases
  are checked, implementation visibly saturates before overflow.
- Original iterator(), mapped collection/read-only/List APIs, linked owners,
  metadata order, exception propagation, nulls and duplicates remain.
  No new product public API, JNI export, per-element native callback or
  resident projection store.

## Recipe-first serial delivery

Manifest `manifest.tsv`: one MODIFY of `M3MappedViews`, two ABSENT Java
proof/JUnit files, one MODIFY to **existing JNI Java test only**. Exact source
preimages, reviewed Java LST postimages, source-bound leaf digest and
first-party Apache-2.0 provenance are committed **before target code**.
JUnit OpenRewrite SDK must verify four complete Java postimages and a
zero-change second pass, partial/reordered inputs, missing/drift/occupied,
non-Java and duplicate refusal before receiver installation. If it fails,
correct the recipe/templates and rerun; never handpatch generated targets.

## Native and donor custody

Native C source `m3_map_cursor_jni.c` is reused unchanged from #988.
The Java JNI regression invokes `M3CollectionWrappers.flatMappedView`
over detached primitive shadow pages, then passes only materialized
`int[]` through the existing `GetIntArrayRegion` bounded 256-int peer.
No Java predicate or reference crosses JNI. Strict Java21 and C17/UBSan,
`-Xcheck:jni`, source SHA and API baseline are required.

`CATEGORY_REVIEW.tsv` records algorithm choices, alternative complexity,
donor licensing, JDK standards and challenge categories. OpenJDK source,
Guava, Eclipse, Apache and Spring are comparison references; LeetCode #341,
HackerRank Java Iterator and GeeksForGeeks Java flatMap contribute only
problem/test taxonomy. No third-party solution code was copied.

## Admission boundary

Do not label this universal M3JDK21 collection superset, five-provider
contract closure, full Maven reactor, source-built JDK/jtreg/JCK,
cross-platform JNI, concurrent linearizability, OOME prevention or
benchmarked throughput gain. A tiny diagnostic mapper-invocation reduction
is not a calibrated speedup. Preserve original JDK21 public signatures and
all M3 public methods; review as stacked draft.
