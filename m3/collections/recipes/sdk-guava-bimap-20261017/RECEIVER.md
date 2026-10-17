<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3JDK21 Guava BiMap product receiving

**Target public SDK:** `hsoliwal/M3jdk21` at base `master@e913ab89056657eca4009678cfe9a657c020e79b`.
**Canonical Synexia Maven/OpenRewrite source:** `hsoliwal/com.synexia` PR #12161, commit `db40b73740cb4ff1b4315cf92676d9bbf249c879`.
**Owner:** existing Apache-2.0 `m3/collections/interop`. No Synexia runtime dependency, no modified OpenJDK `java.util` name or backend. Original Guava `com.google.common.collect.BiMap` remains the public API, linked to actual `com.m3.collections.interop.M3GuavaBiMap`. No imitation Guava namespace.

## Exact source transformations — in serial order

Recipe and provenance precede materialized source. The Maven OpenRewrite SDK class `com.synexia.m3.GuavaBiMapSdk20261017` extends the existing first-party `M3GpuToolJoinRecipe` and reads the **same** sealed 5-target manifest. Its two source-replacement preimages are current M3JDK21 factory `8ca3906e4ca1e502fa65e8a4410f2a225c0a04e0` and factory map `603b0a40b859fd0340e0a88bd20d4955168fc4b2`. New Java source/test paths must be ABSENT. Changed source must be regenerated from Synexia templates; the recipe refuses unrelated owner drift.

- New implementation: `M3GuavaBiMap.java`, Git blob `50ed7fb7d179ce46d3849de107d0081bdf9d8be2`
- Standalone 324001-case Java reference oracle: `M3GuavaBiMapSelfTest.java`, blob `238ba78fafe131147dc7c0f079feb95a19441230`
- Actual provider API/HashBiMap JUnit oracle: `M3GuavaBiMapContractTest.java`, blob `02ff2add2e75121c255452bd05ccf4ca9e771a3d`
- Appended correctly named factory: `M3LibraryCollections.java`, blob `1bb400a037ac27f6f864f8e47fe15b89328578d6`
- 23rd mapped provider descriptor: `sdk-family-bindings.tsv`, blob `5455ec167816a66631b6abbe65ec597bf28e793a`

The recipe tests named SDK discovery, deterministic postimages, exact fixed point, and drift refusals. The new product class stores only the two already-existing M3 flat indexed map owners, one forward and one reverse. Iterators/view objects remain API-boundary allocations rather than resident Entry nodes. Mutable ordinary Guava `BiMap` semantics are not substituted with an immutable cached BitSet: live inverse and map-view mutations are preserved.

## Required verification and final programme boundary

Local direct Java 21 strict compile + 324001 randomized checks **PASS with a test-only JDK map and BiMap signature stub**. Therefore no claim of real target owner or real Guava provider proof. Actual Guava 33.4.8-jre JAR and actual M3 owners, true Maven/OpenRewrite named tests, full optional interop JUnit module, old JNI mask ABI under `-Xcheck:jni`, and merged/installed JDK acceptance are delegated to the attached exact-head workflow and must be inspected for actual success.

Native object hashing, Java equality, user callbacks and reference lifetime are deliberately **not** moved across JNI. Existing C17 primitive mask selection stays untouched and is checked as an independent bounded regression. No benchmark or whole-SDK speedup claim.

The programme objective remains all JDK, Guava, Eclipse Collections, Apache Commons Collections and Spring contracts, with each correctly named API paired to an M3 internal structure where provably substitutable. This one contract does not mark the other pending catalogue entries closed, does not alter String → arrays → collections → AST/compiler sequencing, and must remain a DRAFT until gates pass.
