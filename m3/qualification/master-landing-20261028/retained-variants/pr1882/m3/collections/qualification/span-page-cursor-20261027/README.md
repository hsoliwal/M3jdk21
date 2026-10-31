<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original span JNI cross-page cursor result — 2026-10-27

This belongs to the original **M3JDK21** collection/JNI source, not a
compatibility wrapper or an M3-on-JDK layer. The transformation source is
Synexia PR #13224, commit f8d967ffbec8a2be3f9cc8412322a700db103ea4;
recipes are NOT imported into the product.

## Exact source postimages
| Product owner | Before Git blob | Received Git blob |
| --- | --- | --- |
| `m3/collections/src/main/native/collections/m3_span_index_jni.c` | `3dccc1694fb8dd7c3148bc7f59b1cc4391d37077` | `ebfc70cb030aa62a0197caf6dcb4990a37820e45` |
| `m3/collections/src/test/java/com/m3/collections/M3SpanIndexSelfTest.java` | `2def1962e7db62293ef4c21d6b5206b7563b3b8f` | `521a80c189cb12722d2836d7d413a2e398ff72c5` |
| `m3/collections/src/test/native/m3_span_page_cursor_contract.c` | ABSENT | `3025ce6129fbba4e4b97291cb82cd61e9a655c64` |
| `.github/workflows/m3-span-page-cursor-java21.yml` | ABSENT | `b6134167fc544498947996631c868b8592ed64e4` |

The Synexia canonical `M3HashPinnedTextSnapshotRecipe` C/CI crate and
`M3HashPinnedJavaSnapshotRecipe` test crate pin UTF-8 SHA-256 preimages
and postimages and retain the original tests. No Java product API,
JNI export, native signature, bundled library identity, native mask,
module descriptor, or normal scalar behavior changes.

## LeetCode-indexed algorithm category
- LC35: first prefix maximum end >= query (the unchanged scalar binary fallback).
- LC88: merge-style forward sweep for ordered query pages.
- The existing 256-query native page and cost gate are retained. When
  contiguous pages are individually selected for sweep, the native primitive
  prefix cursor persists ONLY if the next page's first row >= the prior
  page's final row. Otherwise it resets or chooses the original binary path.
- No wrapper objects, per-entry entries, query sorting, Java callbacks,
  extended array pins or native allocations. JNI critical pairs are
  nested as allowed by the Oracle JNI specification, with no arbitrary
  JNI calls during either critical interval.
- `CATALOGUE.tsv` with complexity and donor evaluation exists in Synexia.
  LeetCode, HackerRank and GeeksforGeeks are category references; no
  challenge solution code is imported.

## Executed vs not executed
An isolated sanitizer-backed C model checked 5000 queries in ascending,
descending, mixed, periodic and extreme-value patterns, with identical
answers. For one monotone 1024-prefix input, prefix increments fell
from 9471 to 1023. **That is not a wall-clock benchmark nor complete
production/native JNI execution evidence.**

The new exact-head workflow must compile the ACTUAL production C file
as an included source in the C17 UBSan oracle, separately produce the
actual `libm3_collections_span.so`, strictly compile the existing
M3SlotArrays, M3SpanIndex and enlarged M3SpanIndexSelfTest Java21
original owners, and execute under `-Xcheck:jni` with
`m3.span.paging.native.required=true`. Tests span 255/256/257,
511/512/513 and 5000 query vectors, including output-window padding.
Real Synexia Java/Text OpenRewrite SDK fixed point/refusal tests must
also pass at its exact current head.

DRAFT/HOLD: This result is not whole-JDK java.base integration proof.
The JDK build, jtreg/JCK, Maven full collection/JNI provider,
installed-JDK, cross-platform and benchmark gates remain independent.
No user-visible speedup/release claim until measured and qualified.
