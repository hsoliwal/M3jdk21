<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Five-fork prepared selection receiver, 2026-10-18

Canonical source-first recipe: https://github.com/hsoliwal/com.synexia/pull/12360 exact head 3c98eb5dc9739249b095460a1f1f96ab1d1b7a10.
M3JDK21 product base master@2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49.

Two serial receiving commits: 1) exact Synexia SHA-256 templates, Maven generator, catalogue and fork-provenance verification first; 2) exact generated class and two Java oracles. No existing com.m3.collections implementation, JDK java.util owner, JNI C native symbols, module graph or imported provider namespace altered.

This new M3-owned selection plan composes stored predicate and mask facts over existing M3FiveLibraryIndexViews. Primitive index/offset array and dynamically-sized logical BitSet storage. Java/native projection uses the existing M3IndexMaskCompactor kernel, without object callbacks or retained JNI heap addresses. Materializations use the existing per-provider M3 factories rather than reimplementing five unrelated multimap engines.

M3JDK21 Java namespace: com.m3.collections.interop. Exact original donor type names, fork source revision and license are in FORK_DONORS.tsv. **Package relocation is not vendor API equivalence:** full fork Eclipse sources have com.m3.collections.eclipse namespaces, and Commons sources com.m3.collections.apache, so the original Eclipse/Apache dependency API jars in interop are separately qualified. Guava fork includes an isolated package-first queue prototype, not its entire relocated library. Spring original namespace remains org.springframework.util.

Local Java21 strict compile + 34,932 tests passed with test-only M3 and third-party signature stand-ins. Direct Java recipe generator missing/drift/fixed-point PASS. Not local Maven, not real vendor JUnit, not actual checked JNI. The head GitHub workflow must run real dependencies, existing M3 core, M3 native and the pinned fork SHA checks; do not claim green or installed-JDK substitutability without its receipts. Retain STRING→arrays→collections promotion order. DRAFT/HOLD until required gates pass.
