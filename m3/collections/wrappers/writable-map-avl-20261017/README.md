<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21: writable filtered Maps and comparator-safe bulk staging

A bounded, separate collection extension, stacked on
[M3JDK21 #1025](https://github.com/hsoliwal/M3jdk21/pull/1025)
at `4b85e3f8dab4066f6d1ea744c528f46317bcc6de`.

Canonical source recipe and donor decisions:
`hsoliwal/com.synexia` branch `m3/m3jdk21-writable-map-recipe-20261017`.
Donors: Synexia #11976 first-party Apache-2.0
`M3WritableFilteredMaps` and `M3PendingSortedKeys`.
The target's existing `M3FilteredMaps` remains authoritative.

### Actual Java product change

- Existing `M3FilteredMaps` receives one package-local
  `storedRepresentativeKey(Map,probe)` helper. None of its previous
  methods, public/protected descriptors, read/write behavior or storage
  is changed. The strict before/after javap oracle proves descriptor parity.
- New **opt-in** `M3WritableFilteredMaps.filtered(Map|NavigableMap, predicate)`
  borrows current source and current M3 read projection. It permits
  admitted mutations and preserves hidden records. External weak,
  identity, concurrent and effectful predicates are not silently supported.
- New package-private `M3PendingSortedKeys` is a bounded-per-operation
  primitive AVL over alternating Object[] staged key/value *references*.
  No Map.Entry objects, owned payloads or long-lived comparator index.
  First staged comparator-equivalent key wins; existing source key wins
  over staged aliases; all predicate checks precede any batch publication.
  Complexity O(m log m) for m newly staged distinct keys, plus source
  comparator lookups.
- Java21 standalone `M3SortedBatchPutAllProof` tests hidden keys,
  case-insensitive aliases, null keys, descending/navigable behavior,
  comparator identity, 128 randomized differential rounds, 4,096-entry
  source updates, and a 65,536-key AVL fixture. Maven JUnit
  `M3WritableFilteredMapsTest` is borrowed with target-specific
  preservation of its current writable filtered-map predecessor.

### Recipe-first, serial file order

1. On the canonical Synexia PR, preserve the exact preimage of
   `M3FilteredMaps.java`, compile the qualified package helper, and
   freeze five postimage SHA-256 templates before touching the target.
2. Reuse the existing M3JDK21
   `m3/collections/wrappers/recipe/pom.xml` and
   `M3HashPinnedJavaSnapshotRecipe` to parse/emit the five Java LSTs.
3. Produce the exact target files from the same template Git blobs,
   run actual `Recipe.run`, source receipt/fixed point and API guards.
4. Run standalone compiler/JDK collections proof and existing
   C17/JNI regression from earlier filtered MAT receiver. Native code
   is not modified: calling arbitrary Java predicates through JNI is
   not a validated acceleration.
5. Re-run original installed-source inventory and publish a sealed
   successor using **existing** #1002 `M3InstalledSourceInventoryRecipe`
   and `M3DonationPointerRecipe` only after additive chronology merge.
   Historical manifests remain immutable. Do not skip exact source
   equality or change hashes by hand.

### Commands

`sh m3/collections/wrappers/writable-map-avl-20261017/verify.sh`

`mvn -B -ntp -f m3/collections/wrappers/recipe/pom.xml -Dtest=M3Jdk21WritableMapAvlRecipeTest test`

Whole-Maven collection module, source-built M3JDK21, JCK/jtreg, multi-OS
JNI and allocation/JMH remain hold gates. This PR is source-intake and
narrow Java21 proof, **not** implementation of global HotSpot reference
counting, full collection-family closure or a JVM GC replacement.
