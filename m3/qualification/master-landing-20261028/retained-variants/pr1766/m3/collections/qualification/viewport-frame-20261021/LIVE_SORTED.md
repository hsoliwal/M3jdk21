<!-- SPDX-License-Identifier: Apache-2.0 -->
# Live primitive sorted/filtered viewport pagination

The original M3JDK21 `M3LongSortedViews` class now provides an optional
`copyPage(M3LongSortedSet,int,int,long[],int)`. It iterates the same backing
sorted/filtered source directly into a bounded destination long[] page.
The source's mutable-state/fail-fast rules remain authoritative and
predicates are evaluated only as needed to reach this visible page.
No boxed Entry nodes, new model store, full `toArray()`, JNI callback or
renderer-specific view is introduced.

A page deliberately differs from an immutable snapshot. The model owner
must prevent concurrent mutation for atomic frame consistency; if iterator
callbacks throw, partial writes follow the existing iterator's semantics.
No predicate is invoked for zero demand; invalid geometry refuses before
any writes. Theme/font/language change journals are still distinct from
logical collection membership and must invalidate rendered geometry.

Canonically authored *before* this product receipt:
https://github.com/hsoliwal/com.synexia/tree/m3/viewport-frame-lanes-recipes-20261021-chatgpt/synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017/datasets/m3-long-sorted-page-20261021

The added original-source test checks seeded live sorted/filtered/range/
unmodifiable compositions against the incumbent public toArray oracle.
The new Maven gate is committed, not yet demonstrated passing.
