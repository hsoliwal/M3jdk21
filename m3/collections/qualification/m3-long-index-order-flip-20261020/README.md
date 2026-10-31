# M3JDK21 native collections: stable opposite-order index permutation

Authority: M3JDK21 native parent-owned `long[]` payload, `int[]` physical-index directory, offsets and selection masks. This refines the **existing** M3LongIndexProjection, not a new wrapper or sorted collection owner.

Source refinement: `m3/collections/src/main/java/jdk/m3/collections/M3LongIndexProjection.java`; canonical hash-pinned Maven/OpenRewrite source and test recipe: `hsoliwal/com.synexia#12581`, `com.synexia.rewrite.M3LongIndexOrderFlip20261020`.

Preimage SHA-256: `6be07c18dbbbe9ae259ff85821bb3f4ec666fddd820b0950f4115c4256f45deb`.
Postimage SHA-256: `60dc7c8e33fd4aebe16f3d75204e5497fe806ece518bd0aa20f637beb1548a6c`.

One meaningful replaceable behavioral atom: ASCENDING↔DESCENDING on an immutable indexed projection. Reverse a freshly copied physical index window, then reverse each contiguous equal-value run. Stable ties retain current encounter order without rewriting the `long[]`, changing prior views, allocating per-element objects or JNI traversals. ENCOUNTER sorting and same-order no-ops continue unchanged.

## Validations
- Observed locally (Java 21): focused standalone two-pointer/differential harness completed **51,200 cases**, including Long.MIN_VALUE/MAX_VALUE, duplicates and filtered indices. This validates the algorithm shape, not the full SDK source.
- Exact target Java test `M3LongIndexOrderFlipProof` runs 3,072 deterministic randomized windows, mask/slice composition, stable consecutive sorts, signed extremes, unchanged payload/physical indices. Maven/JUnit wrapper includes it.
- Canonical recipe test checks full LST generation from pinned preimage, postimage fixed-point, hash seals and source drift refusal.
- **Not run here:** full `mvn -B -f m3/collections/pom.xml test`, recipe test `mvn -B -f m3/collections/recipe/long-projection-flip-20261020/pom.xml test`, actual JNI native mask parity, matched JDK/HotSpot build, JMH throughput/retained memory. No general speedup assertion. Keep draft until exact-head gates pass.

Java/JNI: this is Java 21 primitive array index manipulation. Existing primitive mask JNI backend and fallback contracts are left unchanged; JNI user-callback dispatch is unnecessary.

Third-party provenance: no external competitive-programming code copied. Original M3 first-party code and recipe are Apache-2.0. Donor idea is reverse-in-place plus stable run restoration, categorized as array/two-pointer.
