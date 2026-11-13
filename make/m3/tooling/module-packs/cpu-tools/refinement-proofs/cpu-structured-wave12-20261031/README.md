<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Wave12 sorted signed multiset — product output only

This result-only package adds immutable `M3SortedIntMultiset` and explicitly opt-in `M3SortedIntMultisetNative` to the **existing** `jdk.m3.tools.cpu` module. It does not replace the Wave10 unique-set API, add a new structural entry/node store or introduce any new `M3String` payload owner. `m3_sorted_int_multiset.c` implements the private JNI symbol; Java remains the reference oracle.

Sorted signed duplicate-preserving intersection is `min(countA,countB)`; difference is `max(countA-countB,0)`, union is `max(countA,countB)`; missing-distinct compares two multisets by frequency but outputs each deficit value once. API and exception/copy/resource contracts are in the canonical Synexia recipe. Actual product JMOD/JNI/jtreg acceptance, target runtime, CI, and benchmarks remain independent, not asserted here.
