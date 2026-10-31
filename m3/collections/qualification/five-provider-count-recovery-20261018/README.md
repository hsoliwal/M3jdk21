<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3 five-provider shared indexed counts — product receiver

Source/recipe authority: [Synexia #12259](https://github.com/hsoliwal/com.synexia/pull/12259), canonical commit `f15d6cc01a781eee1ccffa9770abde2cdc2da1aa`.
Receiver preimage `M3jdk21 master@2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`.

Serial commits: full source-sealed Maven/OpenRewrite recipe and qualification workflow FIRST; seven generated Java/test output files SECOND. The original `M3ObjectCounts.java` and `M3ObjectCountsTest.java` are byte-identical to the separately qualified M3JDK21 #1174 original sources. Existing modern M3CountedBag-backed Guava/Eclipse/Apache adapters keep their public APIs; selected captured 32-bit flags, including bit31, reuse existing M3IndexPathMasks Java/C17 JNI. These three real provider facades share one M3CountedBag; JDK Map and Spring MultiValueMap are independent correctly named projections.

This product remains a separately packaged Apache-2.0 M3 collections/interop SDK receiver, not a rename of `java.util` or entire Guava/Eclipse/Commons/Spring provider libraries. Maven, full Java and JNI head CI must execute before any compatibility/completeness/performance claim. No new JNI ABI, no per-collection Entry/Node store and no privately invented alternative recipe engine.
