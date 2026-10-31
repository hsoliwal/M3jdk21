<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Canonical Synexia BiMap mainline receiving recipe (2026-10-17)

This is an **actual Java/optional-JNI product handoff**, not a new collection implementation or a completion declaration for all JDK, Guava, Eclipse Collections, Spring and Apache Commons collections.

## Donor authority / ancestry
- Previously qualified donor M3JDK21 PR: https://github.com/hsoliwal/M3jdk21/pull/970
- Exact donor source commit: `f7b9062f6c84b9d11ae7d9d3e895f33f695f4c99`.
- Canonical original reusable recipe: Synexia https://github.com/hsoliwal/com.synexia/pull/11863 .
- Target receiver baseline: `hsoliwal/M3jdk21 master@e913ab89056657eca4009678cfe9a657c020e79b`.
- Underlying M3LinkedHashMap and M3SlotMap source Git blobs are **identical** on donor head/current receiver master: `ade59880b8c9b2bee1b32ae67ca9d29b7382d998` and `a7d20300717661c26897f0b69e1e5a83400a62ad`. Optional interop POM has evolved; retain the current owner, never replace it with old donor POM.

## Admission contract
The named recipe `com.synexia.m3.BiMapMainline20261017` extends the existing Synexia `M3GpuToolJoinRecipe` plus `M3GpuToolJoinManifest` unchanged. Only **five ABSENT** file paths are admitted: exact donor `M3BiMap`, `M3BiMaps`, the real Guava-vs-M3 differential Java test, test-only native JNI consumer/test, and its C source. SHA-256 manifest guards each template and drift/occupied targets refuse. Existing production Java, JNI ABI and public `java.util` signatures are untouched. No Synexia runtime dependency is added to M3JDK21.

The original Guava AbstractBiMap donor is Apache-2.0. The original Guava author copyright and 2026 adaptation notice are preserved byte-identically. Eclipse and Spring code is accessed only through existing provider APIs in the optional interop dependency; no upstream Eclipse/Spring source copied. JDK public names remain JDK-owned.

## Behavior and tests
The Guava `BiMap` contract is provided via the two existing M3 stable-slot maps. This includes bijection checks, nulls, `forcePut`, both inverse directions, live views, guarded entry changes, staged `replaceAll`, map defaults, equality/hash and collectors. Native C is a **test-only JNI Java API consumer**; it does not accelerate map lookups or receive Java heap pointers. No new production native symbol or per-entry Java owner.

The historical original donor head completed hosted Maven/OpenRewrite/Java/JNI qualification (link in donor PR). **Those receipts belong to the donor head**, not this fresh master receiver. The newly authored head workflow must complete actual recipe, module, real-Guava-JUnit and JNI testing before this PR can be considered verified. Current project-wide String → arrays → collections → AST/compiler promotion sequencing remains mandatory.

The source-aligned category catalogue documents donor hashes, competition-task references as taxonomy only, alternative choices, and unresolved resource contracts. No LeetCode, HackerRank or GeeksforGeeks solution source has been copied.
