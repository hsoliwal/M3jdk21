<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 array-backed Map.Entry facade cache — 2026-10-18

## Contract

Authoritative collection state remains in the owning M3 dynamic arrays and primitive lanes. This slice does **not** add one Entry/Node per mapping. A bounded M3LFUCache stores only API facade references in parallel arrays (Object[] keys/values, int[] frequencies, long[] recency stamps, byte[] occupancy mask). Entry iteration and sequenced endpoints reuse an existing live facade for the same canonical key reference.

The cache evicts least-frequently-used entries, breaking frequency ties by least-recent access; recency is rebased safely before a long overflow. Removing a mapping forgets its facade before freeing the slot; whole-map clear drops the facade cache. Cached facades dynamically resolve the key's current slot rather than retaining an array offset that could become stale after compaction or reuse.

M3WeakHashMap explicitly bypasses the strong cache. Caching a key and facade strongly would defeat weak-key semantics. Detached immutable snapshot APIs (firstEntry, lastEntry, pollFirstEntry, pollLastEntry) retain snapshot semantics and are intentionally not reused as mutable live facades.

## Transformation recipe and source custody

recipe/src/main/java/com/synexia/m3/M3EntryFacadeCacheRecipe.java is the canonical receiver-side transformation recipe for this narrow slice. It pins five base and postimage Git blob IDs and carries the complete preimage snapshots as fixtures. It applies exact single-occurrence source atoms, verifies the generated output hash, and installs the two new source files only from hash-pinned templates. All target states are preflighted before writes; any drift refuses without partial edits; a repeated apply is a fixed point.

The production slice, self-tests, recipe, full before-image fixtures, and output templates are reviewed together. If output behavior must change, update the recipe and fixtures, then regenerate/requalify rather than manually editing generated postimages.

## Reproduce

From a Java 21 + Maven environment:

    mvn -B -ntp -f m3/collections/qualification/m3-entry-facade-cache-20261018/recipe/pom.xml verify
    mvn -B -ntp -f m3/collections/pom.xml test
    java -cp m3/collections/qualification/m3-entry-facade-cache-20261018/recipe/target/classes com.synexia.m3.M3EntryFacadeCacheRecipe --check .

These are the intended admission gates. Local Maven execution and exact-head hosted CI must be reported separately from source-level seals.
