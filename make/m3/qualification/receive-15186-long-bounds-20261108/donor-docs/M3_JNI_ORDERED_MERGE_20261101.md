<!-- SPDX-FileCopyrightText: 2026 Synexia contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK signed-long ordered-native query merge atom (2026-11-01)

## Identity, owner and locked contracts

Canonical recipe: `m3jdk-sorted-long-batch-bounds-v1`, in Synexia. Source authority:
`hsoliwal/com.synexia@e7256247dc57f5feafe612d91100854cde78dd51`.
The ONLY production owner changed by this atom is the internal implementation of
`make/m3/tooling/search/src/main/native/m3_native_sorted_long_bounds.c`; the exported JNI
symbol, public Java methods, argument/exception order, Java-only implementation,
caller-visible indices, output ownership and native opt-in contract remain fixed.

## Category / donor decision

The existing first-party Java `M3SortedLongBounds` forward-only lower/upper
search is the reference algorithm. Related problem categories: binary search,
search insert position, first/last occurrence, sorted two-pointer merge,
LeetCode 34/35/704, HackerRank search and GeeksforGeeks lower/upper bounds.
There is no copied challenge statement, submission, editorial or external code.

## Invariant and exact selected path

For one nondecreasing signed-long slice `sorted[from,to)` of length 1..1024 and
>=128 query keys, the native owner already reads the first query chunk (<=256)
into a call-local `jlong[256]`. The new selector checks this **already-read**
chunk for full nondecreasing order and at least eight strict increases. Such
batches may use the existing 8 KiB stack cache to hold the complete source slice
in a single `GetLongArrayRegion` call. A prefix qualification is *not* a proof
that all later queries are sorted: the processing loop checks `key >= previous`
before every forward-only step. On the first descending key, the entire rest of
the invocation uses the existing binary search against the cached source; no
forward cursor is used thereafter. Unordered but diverse first chunks use the
prior conditional pure-C binary path; unqualified inputs use the original
four-window JNI path. An invalid JNI array read still fails before any result
is observed, and no object or Java array pointer escapes the native call.

If `key >= previous`, prior lower/upper cursors are <= the new lower/upper
positions. The next pair is computed with two forward-only loops on the cached
values. For an all-nondecreasing query batch this costs O(source_length +
query_count) after one bounded native copy, including repeated keys. If query
order breaks at any point, output remains correct by switching to binary
bounds for that and all subsequent keys. Java/JNI previous-key memoization and
256-query chunk boundaries are preserved.

## Required checks and limitations

- Exact preimage identity and Apache-2.0 first-party provenance.
- `git diff --check`, Java 21 strict javac-h + C17 strict compiler, `nm` exported
  symbol, original JNI header SHA-256, strict Javadoc, independent Java linear
  oracle in interpreter/C1/tiered JVM, case sizes 127/128/129 and
  255/256/257/1023/1024/1025, slices and late descents after chunk boundary.
- Paired baseline versus candidate host benchmarks using identical Java
  classes and separately compiled native libraries. Record all regressions;
  do not auto-enable native dispatch.
- Reusable existing Maven/OpenRewrite Java/text source-sealed recipe, drift
  refusal, fixed-point replay; **SDK_EXECUTION** is independent and defaults
  NOT_RUN unless a real SDK run succeeds.
- M3JDK source-built java.base/C1/C2, jtreg, product JNI/ABI and platform
  performance remain NOT_RUN until executed on the exact product receiver.
