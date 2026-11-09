# M3JDK primitive view receiving slice (candidate, draft)

Product owner: M3JDK21 `m3/collections` module, exported package `com.m3.collections`.
Donor/recipe owner: Synexia `LongFilteredView`, `LongSortedViews`,
`LongCompositeViews` (source-sealed receipts, Apache-2.0). No entry nodes,
value payload copies, or independent collection storage engine.

Implemented additive APIs:
- `M3LongFilteredView`: lazy predicate filter over `M3LongCollection`
- `M3LongSortedSet`: native primitive sorted-set contract
- `M3LongSortedViews`: filtered/range/read-only primitive sorted views
- `M3LongCompositeViews`: read-only concat (duplicates preserved) and
  sorted union (distinct, natural order) with primitive O(k) merge cursor

Existing `M3LongCollection`, `M3LongIterator`, other M3JDK classes,
`java.util`, HotSpot, JNI ABI and java.base were not modified. The M3JDK
module is a standalone product component and needs installed-image acceptance
before being called part of the built JDK. The existing String -> arrays ->
collections receiving order remains authoritative.

Source port: exact Java package/type mechanical rename plus portable Java
array capacity growth (not a new indexed engine). The sealed Synexia originals
and donor rights must be reconciled before promotion. Full Maven/OpenRewrite
SDK, module reactor, JNI-specific tests, installed JDK jtreg and all provider
binary contract checks remain OPEN. No claim that every JDK/Guava/Eclipse/
Spring/Apache collection family is complete. Keep PR draft.
