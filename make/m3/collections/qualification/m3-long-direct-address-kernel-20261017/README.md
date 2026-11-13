<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 direct long address kernel — 2026-10-17

## Invariant

The repeated scalar operation is deliberately reduced to one primitive indirection:

```
physical = addresses[addressBase + logicalOrdinal]
value    = payload[physical]
```

Filtering, sorting, range composition, state masks and predicate evaluation happen before repeated lookup and produce/reuse the `int[]` address vector. The hot scalar atom performs no hashing, binary search, comparator, predicate, allocation, boxing, callback, JNI transition or object materialization.

This is the RAM-level shape available to safe Java arrays: HotSpot owns the actual heap base/scale calculation and can inline the tiny package-local kernel. A retained native heap pointer is intentionally not used because Java heap objects may move under GC.

## JNI policy

Scalar lookup stays Java. `M3LongAddressKernel.gather(..., NATIVE_REQUIRED)` is an explicit batch-only JNI path. The C kernel obtains bounded primitive array access for one batch, validates each physical address, writes caller-owned output, releases every JNI array before throwing, and retains no pointer. There is no Java predicate/comparator callback through JNI.

## Precomputation

Existing owners remain authoritative:

- `M3LongIndexProjection`: immutable payload + physical address vector + from/to offsets.
- `M3IndexMaskCompactor` / `M3StateMaskArrays`: state-mask selection before repeated lookup.
- `M3LongBitmapFilteredSnapshot`: one predicate pass cached as bitmap/address projection.
- `M3LongSortedSnapshot` / projection sort: ordering precomputed once.

This recipe only centralizes the final address atom and bulk gather. It does not create another filtered/sorted wrapper hierarchy.

## Recipe-first transformation

The Maven-bound source-sealed recipe:

```
m3/collections/qualification/m3-long-direct-address-kernel-20261017/recipe
```

preflights the exact existing `M3LongIndexProjection.java` preimage and all additive targets before writing. It refuses drift, symlinks and partial application; a second apply is a fixed point. The existing projection is transformed mechanically by four exact behavioral-atom substitutions; new Java/test/C/workflow files are copied from sealed templates.

OpenRewrite is not used for this bounded receiver because there is exactly one hash/byte-sealed existing file and no cross-file AST pattern to generalize. The recipe remains Maven-executable and deterministic. A future multi-owner semantic rewrite must graduate to an OpenRewrite visitor rather than multiplying textual patches.

## Donor disposition

LeetCode/HackerRank/GeeksforGeeks are problem-category references only. OpenJDK `ArraysSupport` is a design/reference comparison only. No external challenge/tutorial/OpenJDK implementation source is copied. See `CATALOGUE.tsv`.

## Gates

- recipe self-test: missing/drift/symlink/fixed-point/all-or-nothing;
- Java 21 `-Xlint:all -Werror` through the owning module;
- deterministic scalar/gather/mask/projection differential oracle;
- optional C17/JNI batch gather build and parity;
- public APIs of existing collection owners unchanged.

Whole-JDK image, jtreg, JCK, all GC/platform configurations and performance counters remain separate promotion gates.
