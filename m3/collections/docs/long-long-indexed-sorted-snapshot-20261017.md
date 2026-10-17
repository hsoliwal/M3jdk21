<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Indexed sorted long-to-long maps: array/offset/mask proving backend

Status: separately built Apache-2.0 M3 collections candidate; not java.util
TreeMap or ConcurrentSkipListMap replacement. Canonical recipe owner is
Synexia at m3-longlong-indexed-sorted-20261017; M3JDK21 receives the exact
Java source and test templates. No third-party source is copied.

## Storage and composition

M3LongLongIndexedSortedView.snapshot(long[] keys, long[] values) defensively
copies the two aligned primitive payload arrays once and sorts only physical
int indexes. Signed-long duplicate keys are refused, not silently overwritten.
Logical slices and key ranges share the immutable backing arrays by offsets
[from,to). Indexed getOrDefault, rank, floor/ceiling and predecessor/successor
are O(log n); admission sorting is O(n log n) with O(n) primitive scratch.

filtered(byte[] alignedStates, requiredMask, Backend) consumes flags in the
*current logical sorted view's order*, not the original physical order. All
required bits must occur in the unsigned byte (requiredMask in [0,255]).
Java selection reuses M3IndexMaskCompactor's existing match rules without
allocating per-entry objects. When every row is selected, the same immutable
view may be reused. Selective results retain the original parallel key/value
lanes and own only a smaller sorted physical-index vector.

Backend.NATIVE_REQUIRED explicitly calls the existing bounded
M3IndexMaskCompactor JNI selector with only primitive index/state arrays.
The native code is not copied or independently modified in this task. If
JNI is missing, native-required fails instead of silently switching to Java.
Optional JNI parity must be tested with the installed target ABI and library.

## Contract and resource boundaries

- Source arrays are never retained. The reported retainedPrimitiveBytes is
  the combined primitive lane capacities of this view; it does not include
  object headers, transient sorting scratch, optional native memory or GC.
- Neither caller masks nor mutable arrays are retained. Concurrent mutation
  of masks while filtering is unsupported; count/selection disagreement is
  rejected. A stable input is required during the operation.
- This is a detached snapshot, not a live java.util.Map; it does not implement
  Map, Iterator.remove, entrySet, mutability, serialization, comparator
  aliases, or JDK TreeMap's duplicate-key last-write-wins semantics.
- Numeric long keys/values remain primitive; M3String is relevant at separate
  canonical text-label boundaries, not needed for raw numeric map payloads.
- Do not claim this upgrades the installed java.base TreeMap/TreeSet. The
  repository's String -> arrays -> collections -> compiler receiving order
  remains authoritative.

## Local and hosted verification

The exact Java-source candidate was compiled with
javac --release 21 -Xlint:all -Werror and executed at -Xmx48m against
a *local mask-selector API shim*, with 1,368,500 seeded evaluations of
TreeMap ordering, lookups, floor/ceiling/navigation, rank, slicing, ranges,
duplicate refusal, extrema, and composed masks. This proves the new
candidate's core Java control flow, not the real M3JDK21 module or JNI ABI.

GitHub Actions will compile the exact candidate against the existing real
M3IndexMaskCompactor and execute the same proof. Maven/JUnit executes the
JUnit wrapper against the receiving module. Exact-head OpenRewrite SDK
application, first/second-pass fixed point, drift refusal, source hashes
and relevant target collection tests remain separate proof authorities.

## Candidate comparisons

See m3/collections/algorithm-catalog/longlong-indexed-sorted-20261017.tsv
for pinned existing Synexia/M3JDK21 owners and separately evaluated
LeetCode/HackerRank/GeeksforGeeks *problem categories*. No challenge source,
editorial or code is reused without explicit rights. Preserve existing
Apache-2.0/Synexia copyright and OpenJDK GPL boundaries.
