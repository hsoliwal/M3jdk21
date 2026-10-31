<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 source-sealed signed-long indexed set algebra receiver

Canonical Synexia source: [#12286](https://github.com/hsoliwal/com.synexia/pull/12286),
head `a9e923011e7a04ece86901bc64b2e38e105ec540`. Pinned public M3JDK21 base `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`.
Source-first product commit installs the original preimage, three exact canonical
postimage templates, category/rights table and runnable head CI before any
production edit. A separate later commit receives all three generated Java files
exactly (no hand-edited production target).

One bounded atom in `M3LongCompositeViews.BinarySetAlgebra.seek`: direct
signed-long lower/higher for two existing final M3LongTreeSet owners, existing
iterator path for arbitrary/filtered input. Reuses original primitive index
storage and natural signed order, no new object entries or native ABI.
Algorithm improvement is conditional on sparse/late searches and must be
benchmarked before any general speed claim. Null/boxed JDK only in test oracle.
