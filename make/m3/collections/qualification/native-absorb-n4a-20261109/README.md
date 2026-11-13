<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# NATIVE_ABSORB N4a: IndexMaskCompactor.firstEqualBits0 (2026-11-09)

The M3 owner `jdk.m3.collections.IndexMaskCompactor` already unioned the Synexia `firstEqualBits` API (see the
`//FLLDCIM` anchor in the class). Its `native firstEqualBits0` had no C, so it was the one ABSENT method of a class
whose other 21 natives are served by `m3_collections_mask`.

## Source

The source is com.synexia develop 537b10a9de1f, file `synexia-primitives/src/main/native/m3_index_mask_compactor.c`.
`NATIVE_RELOCATE.tsv` records the blob. That file holds two sections:

| Lines | Content | Disposition |
|---|---|---|
| 3-133 | count and select kernels | not absorbed: M3 already owns them as `nativeCount` and `nativeSelect` in its own `m3_index_mask_compactor.c` |
| 135-278 | bounded first-occurrence search, `firstEqualBits0` | absorbed |

The staged donor is lines 1-2 (license), `#include <jni.h>`, line 11 (`#define M3_CHUNK 256`, which the search
uses), and lines 135-278. NATIVE_SYMBOL_RELOCATE v1.1 writes it as `collections/m3_index_mask_search.c`, and
make/m3/collections adds that file to `m3_collections_mask`.

## Semantic check

The kernel's kind ABI is `PrimitiveKind.ordinal()`, which it requires to be BOOLEAN=0, BYTE=1, SHORT=2, CHAR=3,
INT=4, LONG=5, FLOAT=6, DOUBLE=7. M3's `jdk.m3.collections.PrimitiveKind` declares its constants in exactly that
order. `IndexMaskCompactorM3ApiTest.firstEqualBitsMatchesNaiveScanForEveryKind` checks the result against a naive
scan for every kind.

## Census

BOUND 186 -> 187, ABSENT 17 -> 16.

## Gates

See GATES.tsv.
