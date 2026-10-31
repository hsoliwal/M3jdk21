<!-- SPDX-License-Identifier: Apache-2.0 -->
# Direct java.base sorted/Set spliterator receiver — pass 2

This is the **second serial source-sealed FILE pass** after the one-shot admission and exact COW bulk owner. Synexia canonical recipe: `com.synexia.m3.CollectionViewsSortedMetadata20261023` at commit `c8a70cbb3ae85de5b783e22186edfa603fc1e491`.

The original `java.util.CollectionViews` stays the owner; no M3+JDK overlay, no public signature changes and no JNI symbols. `FilteredSet.spliterator` guarantees DISTINCT. `FilteredSortedSet.spliterator` guarantees ORDERED, SORTED and DISTINCT, retains the owner comparator and propagates these facts to split cursors through two primitive/reference metadata fields in the existing `SelectionSpliterator`. The original source iterator still selects values and owns traversal. Nothing is cached per selected element.

The new jtreg reuses the first-party regression from PR #1770: custom sorted owners with metadata-sparse spliterators, normal/reverse comparators, split propagation, Set distinctness and encounter-order differential.

Source preimage Git blob `3252ddc179bcde807785b13ef24dc6b3b6c3ce4a`, SHA-256 `d0713f7a8c949a6b11c89fbe637f1ccebb53d45f164a7c69029c517f5eb3eb2f`; postimage SHA-256 `4fc81eda7a7b1831f36abc20fca52f9ba898fa7a767f2c1b62f8eab872ec056f`; test SHA-256 `e3d8f4208b2f8d3f0b05f512d39e30e276ad4496632f917376f3ebb8e37957ad`.

This product branch contains only the generated source/test postimages and passive provenance. Run the two Synexia Maven recipes in order, then patched java.base/jtreg and actual installed-image runtime/ABI/JNI/GC/CPU/heap gates before promotion. Until then: DRAFT/HOLD.
