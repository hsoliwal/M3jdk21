<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Primitive long filtered/sorted view: balanced conjunction receiver

## Names, ownership and contract

- The product runtime owner is `com.m3.collections` in M3JDK21, with the existing
  `M3LongFilteredView` and `M3LongSortedViews` public surfaces unchanged.
- The recipe/donor source authority is Synexia, via the first-party Apache-2.0
  recipe `M3LongFilterBalanceReceiverRecipe`, adapted from reviewed Synexia
  #12027 and M3JDK21 #1049, not independently copied from JDK/Guava/Apache.
- The helper `M3LongPredicates` is package-private, has no payload storage,
  introduces no JNI symbol and does not replace `java.util` public classes.
- Maintains natural long sorting, live backing, duplicate and hidden-element
  semantics, stable short-circuit evaluation and the JDK LongPredicate contract.

## Exact reviewed target preimages and generated postimages

| Target | Before Git blob | Before SHA-256 | Postimage SHA-256 |
| --- | --- | --- | --- |
| M3LongFilteredView.java | `9ccb1286c23e4532585541ad212b08eb82fe2f67` | `cf9f15e9712b169768be2611a9eae98d317d47b07b7494b168bf81988e678341` | `f2a4d038413e8ddd294be088f8bff9ab127ee63706df1f9e16ef57f1e4b44355` |
| M3LongSortedViews.java | `1c79d9437acc209a1ed30e90f400f308c2804169` | `43a8b64af2ff4c014910d8b769615d5b60bcb23d52962c58c72050dd3e6f6c48` | `7a42630fbffd2c5875d8a7e4258540454879117a52bd6fe52c3cde4ce07b4d99` |
| M3LongPredicates.java | ABSENT | ABSENT | `e5d9d7fca94c4eef0843bb7eb7929c2baba05892207f9336a6da3a089bc882ad` |
| M3LongDeepFilterSelfTest.java | ABSENT | ABSENT | `a5df095ddaf792e8dacdb93d674e1dd90294786684af5ca5851951d9d0a9fd6a` |

The helper is reused from target historical first-party Git blob `2367a2e5f7437ffe340d800a1a5c949cef54adc4`
and self-test from `7e0567a9cbd4adeaf7a458fad20174079a22606e` (PR #1049).
Only one call in each existing wrapper is changed: default conjunction delegates
to the established helper. Every output file is an exact recipe postimage.

## Required promotion gates

1. Canonical Maven/OpenRewrite SDK generation and SHA-256 preimage guard,
   invalid/duplicate refusal and second-pass no-change fixed point.
2. Java 21 strict compilation and 100,000-level deep predicate tests with `-Xss256k`
   in generic and sorted live wrappers.
3. Cross-family null/visibility, comparator, set/map and concurrent interference
   contracts separately; not implied by a primitive-long test.
4. The earlier source-side native `-Xcheck:jni` test is useful reference evidence
   but current full JDK JNI, jtreg, JCK, HotSpot/GC and installed M3JDK parity are PENDING.
5. Construction allocation **regresses** in the historical 1,024-layer oracle
   (49,144 -> 107,432 bytes); stack safety is the purpose, not a claimed speedup.

LeetCode Valid Parentheses, HackerRank Balanced Brackets and GeeksforGeeks AVL
rotations are **problem taxonomy**, not code donors. No challenge source copied.

The requested global receiving order remains STRING -> arrays -> collections ->
AST/compiler, with String and arrays proof gates not bypassed by this draft.
