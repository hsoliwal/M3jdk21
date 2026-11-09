<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 five-provider indexed state-mask view receiver (2026-10-17)

This **additive implementation**, not a full replacement of five libraries, shares
one captured structure-of-arrays tuple: Object[] keys, Object[] values, byte[] unsigned
physical states and int[] occurrence-order rows. Physical row indexes (including repeated
ones) are validated before capture; all arrays are defensively copied once. Slices share
those immutable arrays. Sorted views permute only int indexes and keep comparator ties stable.

The optional JNI path **reuses** existing M3IndexMaskWindow / C17 mask-window library;
no Java keys, values, callbacks or object addresses enter JNI. Explicit NATIVE_REQUIRED
refuses a missing symbol/library, including empty masks and zero selected rows.

The public materializers genuinely return JDK List/Map/NavigableSet, Guava List/SetMultimap,
Eclipse Collections MutableList/MutableSortedSet, Spring MultiValueMap, and Apache
Commons ListValuedMap/SetValuedMap, using the incumbent M3LibraryCollections providers.
Each result is independently mutable and detached, **not** a live predicate wrapper.
Spring counts distinct keys; Guava/Apache multimaps count occurrences; set-valued projections
deduplicate and JDK Map uses last value. Existing live filtered/sorted owners remain separate.

Canonical recipe is the *existing* M3HashPinnedJavaSnapshotRecipe; named composite
`com.synexia.m3.FiveLibraryIndexViews20261017`. Two targets are ABSENT-before, SHA256-sealed.
Do not manually patch production outputs. Canonical Synexia recipes/fixtures precede the
M3JDK21 receiver, which mirrors exact source/templates and has no Synexia runtime dependency.

File pass order: catalogue/read existing owners; Java array/index/mask owner; Java vendor
adapter calls; standalone Java21/JUnit differential; prior JNI parity; full interop Maven;
OpenRewrite source/refusal/fixed point; CI and resource-cost benchmark. Preserve nullable
values, duplicate physical rows, fixed public vendor contracts, and donor notices.

Pending/NOT QUALIFIED until actually run: real Maven/OpenRewrite recipe test, Maven owning
module with actual five pinned dependencies, installed-JDK/jtreg/JCK, multi-OS JNI,
CPU/retained-heap/native memory comparisons and full provider API closure. A finite slice
is not five-provider superset parity. Other open SDK/factory PRs are not overwritten.
