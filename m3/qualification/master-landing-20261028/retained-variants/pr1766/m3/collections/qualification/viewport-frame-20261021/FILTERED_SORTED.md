<!-- SPDX-License-Identifier: Apache-2.0 -->
# Filtered/sorted/sliced M3JDK21 physical index viewport page

This is a **direct original-owner enhancement**, not a compatibility facade.
The existing `M3IndexedListSnapshot` retains the private canonical `Object[]`
values and `int[]` occurrence positions. The additive `copyPhysicalWindow`
checks its requested slice and destination before a bounded
`System.arraycopy` from the existing physical index directory.
It preserves sort comparator results, filtering, occurrence identity, null
or duplicated payload values, and JDK collection immutability. The previous
public API `physicalIndices()` and live mutable filtered wrappers are
unchanged. No JNI is required for this O(page) copy, and no GPU or Java
callback enters a native library.

The exact Java pre/post sources and JUnit/differential test candidate were
published **before** product receiving in the canonical Synexia recipe
[dataset m3-index-page-20261021](https://github.com/hsoliwal/com.synexia/tree/m3/viewport-frame-lanes-recipes-20261021-chatgpt/synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017/datasets/m3-index-page-20261021).
The `M3IndexedPhysicalWindowContract` provides 4,000 randomized trials
of sorted, filtered, sliced and bounded physical index pages against the
existing unbounded `physicalIndices()` oracle, with invalid input
refusal and stable source checks; the JUnit entry point runs it.

Actual original-source Java21 Maven reactor, canonical OpenRewrite SDK and
receiving fixed-point verification remain pending. Standalone pre-existing
CPU/JNI measurements of the other viewport atom do **not** count as proof
for these newly received snapshot changes.
