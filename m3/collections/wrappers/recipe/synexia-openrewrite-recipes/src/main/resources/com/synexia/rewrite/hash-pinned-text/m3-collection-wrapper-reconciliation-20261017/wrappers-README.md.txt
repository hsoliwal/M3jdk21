<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Filtered and sorted collection wrapper reconciliation

This packet receives the existing Synexia generic wrapper family into the exact M3JDK21 master tree `970a34350ebdf6a3f1c5ff5acc8445d0f3515f55`. It adds eight absent generic owners and restores three existing detached primitive snapshot factories in `M3LongSortedViews`. Current primitive owners and shared stores remain the receiving baseline. Historical PR #992 supplies reviewed source, but its divergent tree is not a replacement for current master.

## Recipe-first invariant

Every further assisted change to this packet starts in the canonical Synexia Maven recipe, its sealed templates and its tests. Qualify the recipe, apply its serial per-file transformation, verify the generated result, and retain that evidence with the change. Fix incorrect templates or recipe logic and replay; do not repair generated production files by hand.

The recipe is `com.synexia.rewrite.M3CollectionWrapperReconciliationRecipe`, in the existing `m3-purpose-wrapper-receiver-20261017/master-reconciliation` family. It composes the unchanged `M3HashPinnedJavaSnapshotRecipe` and `M3HashPinnedTextSnapshotRecipe`. Java targets are parsed as Java LSTs; C, build and custody text use the text provider. The application adapter validates complete admission, exact postimages and a second-pass fixed point before writing SDK results. Drift, unrecognized occupied additions, missing required preimages, symbolic target paths and receipt aliases fail. This is bounded packet qualification, not a transaction guarantee against arbitrary concurrent writes or I/O failure.

`recipe/canonical-source.json` binds the local qualification mirror to one immutable Synexia source revision. The mirror is build-time evidence; M3 collections have no runtime dependency on Synexia or OpenRewrite. Synexia retains recipe authority.

## Public capabilities and contract boundaries

| Owner | Received or refined behavior |
| --- | --- |
| `M3CollectionViews` | Live filtered collection/set/sorted/navigable views; detached filtered and sorted snapshots; saturated size with complete predicate evaluation. |
| `M3FilteredMaps` | Live filtered map/sorted/navigable views; typed checked, synchronized and unmodifiable composition; raw-entry read queries; retained direct projection cursor. |
| `M3CollectionWrappers` | Existing typed composition facade, delegating standard protection and synchronization contracts to JDK wrappers. |
| `M3AdmissionWrappers` | Admission filtering of writes, distinct from visibility filtering of existing content. |
| `M3BoundedLists`, `M3FixedSizeLists` | Existing capacity and structural-mutation contracts, including the later capacity-refusal correction. |
| `M3ConcatenatedViews`, `M3MappedViews` | Existing concatenation/mapping/flattening behavior; flattened size saturation and a lazy, unknown-size, unsplittable traversal cursor. |
| `M3LongSortedViews` | Existing `sortedSnapshot`, `filteredSortedSnapshot` and `maskedSortedSnapshot` donors; detached primitive storage, signed ordering and duplicate occurrences. |

For example, typed overloads preserve sorted/navigable capabilities:

```java
NavigableMap<Integer, String> owner = new TreeMap<>();
owner.put(1, "one");
owner.put(2, "two");
NavigableMap<Integer, String> visible =
    M3FilteredMaps.filtered(owner, (key, value) -> key % 2 == 0);
NavigableMap<Integer, String> readOnly = M3FilteredMaps.unmodifiableMap(visible);
owner.put(4, "four");                    // visible through both live views
Integer predecessor = readOnly.floorKey(3); // 2
```

Live views retain the backing owner's comparator, stored representative, null behavior, encounter order and alias liveness. A read-only facade does not freeze external aliases. Checked wrappers do not retroactively inspect existing contents. Iterating synchronized views requires the common root monitor. A detached snapshot has a different contract from a live wrapper. The refined count atoms saturate at `Integer.MAX_VALUE` while continuing predicate/mapper effects and retaining later exceptions. The flattened cursor intentionally supplies no parallel splitting or exact cardinality claim.

## Search and accumulate algorithm decisions

`catalogue.tsv` retains the earlier eight-column category catalogue and adds dated successor decisions. `purpose/CATEGORY_REVIEW.tsv` retains the existing broader purpose-wrapper review. `source-review.json` records immutable donor paths/blobs, candidate proof, license boundaries and controlled allocation observations. `FILE_PASSES.tsv` records all 125 Java source dispositions in the receiving module: reviewed owners are explicit; other owners are preserved and not marked as refined.

Search the catalogue with the existing Maven application adapter:

```sh
mvn -B -ntp -f recipe/synexia-openrewrite-recipes/recipes/m3-purpose-wrapper-receiver-20261017/master-reconciliation/pom.xml \
  test-compile exec:java \
  -Dexec.mainClass=com.synexia.rewrite.M3CollectionWrapperReconciliationApply \
  '-Dexec.args=--catalogue flatmap'
```

Run from `m3/collections/wrappers`. Search is literal and case-insensitive. Subsequent passes should append category candidates, rejection reasons, exact source custody, complexity and reproducible evidence before selecting a replacement atom. GitHub implementations are evaluated under their actual contracts and licenses. LeetCode, HackerRank and GeeksforGeeks contribute problem shapes and adversarial cases; no submission, editorial or tutorial implementation bodies were copied.

## Execute qualification

From the repository root with JDK 21, Maven and a Linux C17 compiler:

```sh
mvn -B -ntp -f m3/collections/wrappers/recipe/pom.xml verify
mvn -B -ntp -f m3/collections/wrappers/pom.xml -Pjni-linux verify
python3 -m unittest discover -s m3/collections -p 'test_verify_donation.py' -v
python3 m3/collections/verify_donation.py --cost-probe
```

The legacy verifier also needs its pinned JUnit console (`JUNIT_CONSOLE_JAR`); the existing collection workflow supplies it. The wrapper Maven proof compiles the actual current collection package with Java 21 warnings as errors. JNI peers compile against generated headers with C17 warnings as errors, fatal UBSan and `-Xcheck:jni`.

The two received C peers are verification-only: bounded run-search parity and the retained map cursor fold. The unchanged production mask selector supplies independent native parity for primitive snapshots. Java predicates remain Java callbacks. The run-search peer bounds its C array scratch at 4,096 bytes and validates the entire admitted run prefix; that bound excludes the frame, JVM/JNI storage and Java arrays.

Controlled allocation observations disable escape analysis and do not establish normal-JIT application performance. Whole-JDK source-derived Maven, installed-image, jtreg and phase promotion are separate gates. This packet leaves the ordered String-first phase state unchanged. Historical passing receipts are not promoted to results for a different receiving commit; the pull request records newly executed local and hosted results separately.

## License and source custody

Selected implementation bodies are existing first-party Apache-2.0 Synexia/M3 source with original notices retained; the three refined generic owners and new recipe/proof authoring carry Synexia contributor notices. `source-review.json`, the successor donation metadata and the canonical mirror record individual lineages. Guava is an Apache-2.0 comparison reference only. JDK wrapper calls use the installed JDK implementation; this packet does not copy JDK implementation bodies. Historical provenance records remain intact, and the new installed-source manifest is a separately sealed successor.
