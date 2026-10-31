# M3JDK21 tombstone-reuse receiver — design / frozen behavior

Target: hsoliwal/M3jdk21 master@970a34350ebdf6a3f1c5ff5acc8445d0f3515f55.
Source owners: m3/collections/src/main/java/com/m3/collections/M3LongHashSet.java (Git blob 1fe9ac1bfa11cf6a626a749c37d64ccec0ed09df), M3LongLongHashMap.java (blob b1d85622c2e19db069d0b2a64fb8011b5c01be34).
Canonical algorithm/recipe owner: hsoliwal/com.synexia (probe-before-growth precedent #11988, generic M3HashPinnedJavaSnapshotRecipe). JDK-target adaptation only; no runtime dependency.

**Intent:** Avoid doubling primitive table payload when the pending key's legal probe sequence already contains a reusable DELETED slot before its first EMPTY slot. Execute additional probing *only* when the existing occupied-threshold would demand growth. No per-row Entry/Node allocation. Hold topology and public descriptors fixed.

**Target algorithm:** Preserve existing findIndex early-match. At the threshold, look for the first DELETED slot on the same key's linear-probe path *before first EMPTY*. If present, write keys/values/state there, increase size/modCount, do not increase occupied or allocate; otherwise follow the original rehash/insert path. Reuse is correct because `findIndex` already proved absent, and probing through the deleted slot is legal. The remaining ordinary insert path is unchanged.

**Contracts:** Every long including 0, MIN/MAX remains legal. Map returns boolean newly inserted, zero values are present, replacements do not change modCount. Remove/clear, iterator fail-fast, callback order on an unchanged table, no-entry-array fields, `java.util` source and JNI symbols unchanged. No concurrency promise. Domain/lifetime unchanged.

**Proof:** Test red legacy capacity=16 vs expected=8 for five colliding keys, one deletion and a sixth colliding insertion at occupied=5 of capacity 8; use TreeMap/HashMap? independent java.util HashMap/HashSet oracles for randomized operations. Before/after array payload map 8*(8+8+1)=136 vs 16*17=272 bytes; set 8*(8+1)=72 vs 16*9=144. These count only retained primitive lane payload, not array headers, temporary allocations, native memory or throughput. Strict Java21 compile; pinned-recipe fixed-point/refusal; installed JDK/jtreg/full Maven/CI retained as open gates.

**No automatic phase promotion:** current M3JDK21 receiving phase is still String. The independent collections module slice remains draft and candidate-only until exact target gates pass.
