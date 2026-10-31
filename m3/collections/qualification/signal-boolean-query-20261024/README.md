<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# Original M3JDK21 exact Boolean signal query receiver

**M3JDK21 is the integrated modified JDK product, not an M3+JDK overlay.** The only canonical Maven/OpenRewrite author and permanent recipe/correction catalogue owner is Synexia `M3SignalBooleanQueryRecipe` (crate `m3-signal-boolean-query-20261024`). This public receiver includes only exact generated Java production/test and source-qualification CI results.

`M3SignalBooleanQuery` reuses existing `M3SignalExactRangeIndex`, `M3SignalQualitySchema`, `M3SignalColumns` and `M3SignalDependencyEpochs`. Retains **no copied value column, node/entry per row, new JNI export, JVM public class change or new source parser**. One independently materialized `long[]` word-mask result per query selects physical row IDs by exact AND/OR across typed inclusive bounds or explicit missing coordinates, in ascending physical row order. Signed-zero and NaN use JDK Double.compare semantics; signed full-width long values are never converted to double.

Product source and test blobs are identical to the canonical recipe output; full SHA-256 postimages in the Synexia manifest. Local Java21 compatible-contract harness passed normal and -Xint **27,921 seeded assertions each** but did **not** execute against a complete modified-JDK checkout. The new product CI runs strict original-source Java21 and original Maven/JUnit with original arrays bootstrap; hosted proof remains pending until its jobs actually complete.

Source SHA/version and monotonic invalidation epoch protect snapshots only if the caller reports changes, including `invalidateAll()` for unknown dependencies. The result is **not** a transactional lock or a semantic equality certificate. The modified JDK/jmods, jtreg/JCK, JNI/HotSpot/GC/CDS, Windows/Linux and comparative CPU/heap/RSS admission are pending. **DRAFT/HOLD**, do not merge solely on local finite tests.
