<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3 Collections — prepared filters and sorted views across five provider APIs

**Canonical recipe owner:** `hsoliwal/com.synexia` (append-only `develop`). **Product receiver:** independent M3JDK21 `m3/collections/interop`, no Synexia runtime dependency. **Admission stage:** additive proving ground until exact-head Maven/real donor tests and Java/JNI CI pass. String → arrays → collections promotion invariant remains unchanged.

`M3FiveLibraryPreparedRows<K,V>` is a new view atop the incumbent immutable `M3FiveLibraryIndexViews<K,V>`; it DOES NOT create a competing object arena or map engine. One `BitSet` addresses **logical source occurrences** and an `int[]` holds their selected, sorted or sliced logical ordinals. Underlying source reference payloads and physical row metadata are unchanged; duplicate physical rows remain distinct logical occurrences. Predicate capture evaluates once per source occurrence, refinement once per admitted occurrence; bitset algebra combines prior facts without new predicate passes.

`where`, `byState`, `fromBits`, `and`, `or`, `xor`, `minus`, `not`, `refine`, `slice`, `sortedByKey`, `sortedByValue`, stable tie handling, exact ordinal/source-physical-index readback and defensive exports are provided. Boolean algebra produces source-encounter ordering; explicit sorted/sub-sliced views retain their order until further algebra. `NATIVE_REQUIRED` checks existing `M3IndexMaskCompactor` availability **before any predicate callback** and calls only its existing Java/C17 JNI primitive selector. No objects/callbacks/Gc-unstable pointers cross JNI. An absent backend fails, not falls back.

Provider results are created independently from existing M3 owners with correct **upstream types**: `java.util.List/Map`, Guava `ListMultimap/SetMultimap/Multiset`, Eclipse Collections `MutableListMultimap/MutableSetMultimap/MutableBag`, Apache Commons `ListValuedMap/SetValuedMap/Bag`, Spring `MultiValueMap`. In particular Spring counts keys while Guava/Apache list multimaps count pairs. M3 supplies the indexed backing, not a renamed donor concrete class; inherited method and serialization semantics are not declared identical without real provider tests.

### Executed bounded evidence

- `javac --release 21 -proc:none -Xlint:all -Werror` against **test-only provider/JUnit scaffolds**: PASS.
- Four JUnit-shaped Java oracle methods run locally using those stubs: **36,554 assertions PASS**; 250 seeded trials, source snapshots, repeated physical indexes, states, stable sort/slices, algebra, no predicate reevaluation and five-provider result count semantics. This **does not prove actual donor/JUnit binary compatibility** or native parity.
- Hosted exact-head GitHub workflow must execute **real** Maven OpenRewrite SDK/JUnit recipe-generation/fixed-point/drift tests, actual `m3/collections` owner build, actual Guava/Eclipse/Spring/Commons JAR-backed JUnit test and checked-JNI run using the **unchanged** production C17 selector.
- Local Maven not installed; no integrated reactor/installed-JDK, full upstream API/JCK/jtreg/serialization/JMM or comparative memory/CPU benchmark claim.

### Catalogue / provenance

`CATALOG.tsv` separates provider contracts, 3 algorithm-challenge category references, complexity and rights. No third-party challenge/tutorial/provider implementation source copied. Existing M3 first-party Apache-2.0 owners and their source history are reused; external dependencies retain their separate upstream licenses and notices. This tranche is **not completion of the entire five-library SDK superset** and does not imply broad JDK `java.util` implementation replacement.
