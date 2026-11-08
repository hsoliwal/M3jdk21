<!-- SPDX-FileCopyrightText: 2026 Synexia contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3 sorted int batch bounds — candidate Java/JNI primitive

**Admission status: REVIEW_ONLY / HOLD.** The M3JDK21 public receiver is unchanged. This leaf
adds a self-contained, opt-in **tooling** primitive; it does not replace `Arrays.binarySearch`,
change a Java SE/JDK API, or grant native integration into `java.base` or HotSpot. Synexia retains
the source-sealed OpenRewrite recipe; a receiving JDK may later receive only executed postimages.

## Exact behavioral contract: `M3-SORTED-INT-BOUNDS/1`

Given a **non-null, nondecreasing signed `int[]`**, a half-open range `[from,to)` and non-null
`int[] keys`, output `int[2 * keys.length]`: for each query `i`, `output[2*i]` is the least
index in `[from,to]` with `sorted[index] >= keys[i]`; `output[2*i+1]` is the least index with
`sorted[index] > keys[i]`. If no such index exists, the answer is `to`. Both positions are
absolute indices, not relative positions. Duplicates are therefore unambiguous; an empty range
produces `from` for both positions. `from==to==sorted.length` is valid.

The Java and native entry points both reject null `sorted` or `keys` with
`NullPointerException`, invalid half-open range with `IndexOutOfBoundsException`, and
`keys.length > Integer.MAX_VALUE/2` with `IllegalArgumentException` before output allocation.
The input arrays are never written. The API assumes sortedness and no concurrent mutation during
the call; unsorted and racy inputs are outside this contract. No caller-visible mutable native
view or C pointer survives the call. Every successful invocation returns a new output array.

**Runtime:** Java uses two independent overflow-safe binary-search passes per key for arbitrary
queries (`O(Q log (N+1))`, `O(Q)` output). For at least 128 **nondecreasing query keys**, it
selects a stable ascending-merge window (`O(N+Q)`), scanning source elements only once; an
unsorted query batch stays on the binary path. This is a first-party category-based
specialization, not borrowed challenge code. JNI copies the selected sorted range **once**
per call and processes 256 keys per bounded JNI region, with the same sorted-query merge
specialization (`O(N+Q)` when monotone; `O(N + Q log(N+1))` otherwise). Native scratch is
`O(N)` and Java output `O(Q)`; empty ranges copy nothing. The native path requires
explicit `-Dm3.native.sorted.search.library=/absolute/path/to/lib...`; it does not silently
claim acceleration or automatically dispatch. The JNI implementation preserves C17 pointer
lifetime, checks for pending JNI exceptions, and frees every owned allocation.

## Existing owners, category and provenance

Reuse: the existing test-only `M3JniFirstIndexFixture` from Synexia PR #15050 is the
unsorted first-occurrence oracle, **not** an owner of sorted batch bounds. JDK21
`java.util.Arrays.binarySearch` is the standard compatibility owner; it is not modified.
Problem **categories**, not copied solutions: LeetCode 704/34/35, HackerRank Intro to Tutorial
Challenges, GeeksforGeeks lower/upper bound. Algorithm alternatives and licence decisions are
recorded in the canonical Synexia crate `reviews/` ledger. No third-party source is included.

## Ordered verification

```text
source seal -> lint Java 21 -> javac -h -> C17 compilation -> exported JNI ABI
-> negative/positive JVM execution (interpreter, C1, default tiered)
-> deterministic output replay -> benchmark (observation only)
-> actual OpenRewrite SDK fixture -> target M3JDK compile/jtreg/JNI product
```

Use `bash verify.sh` from this directory for host-JDK-only fixture verification. The separate selftest uses an independent O(N) linear reference oracle and observes original inputs after every
invocation. JMH-calibrated performance and product-specific ABI/HotSpot qualification are
outside this leaf. A measured workload never proves universal speedup.

The host qualification script reports SHA-256 identities of the native source, generated
JNI header, emitted Java classes and compiled C17 library, then verifies library bytes
are unchanged after the JVM runtime tests. These are host-product artifacts only.
