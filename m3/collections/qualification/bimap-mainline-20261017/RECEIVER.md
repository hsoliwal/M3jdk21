<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3JDK21 actual Guava BiMap mainline SDK receiver

**Canonical recipe:** https://github.com/hsoliwal/com.synexia/pull/12166 at source commit `55b5d85d357806a05edd8b24059b318b7671dacb`.
**Historical qualified donor:** https://github.com/hsoliwal/M3jdk21/pull/970 at `f7b9062f6c84b9d11ae7d9d3e895f33f695f4c99`.
**Product receiver preimage:** `master@e913ab89056657eca4009678cfe9a657c020e79b`.

This receiver adds the existing already-qualified Java Guava `BiMap<K,V>` source and helper into current optional `m3/collections/interop` rather than building another map. Its dependencies `M3LinkedHashMap` and `M3SlotMap` are source-identical to the historical donor; the current optional interop POM is deliberately retained. The public Guava type remains `com.google.common.collect.BiMap`, with M3-owned adapter `com.m3.collections.interop.M3BiMap`. No JDK/Guava/Eclipse/Spring/Apache public class is renamed.

Exact matching code and JUnit tests are generated postimages in the recipe, using five ABSENT-only SHA-256 sealed entries and existing Synexia OpenRewrite source custody. The first target commit carries the recipe, catalogue, rights and workflow. The second receives the original exact already-generated donor source/test Git objects with no production edits. Changing those Java files requires a corrected canonical recipe, not hand patches.

**Native:** the C17 `m3_bimap_consumer.c` is *test only*. It invokes the Java BiMap API over JNI, validates exception identity and call boundaries. No native object-store alias or new production JNI symbol is introduced, and no throughput claim is made.

**Evidence authority:** donor head previously completed 25 Maven/OpenRewrite methods and 107 runtime tests, 48,000 operations vs real Guava, and checked JNI C17. That proof applies to the historical donor head, not the current receiving head. New head Actions must execute and pass the Maven recipe, actual M3 Java modules, real Guava/Eclipse/Spring tests and JNI test-only consumer before this receiving PR may be promoted. Concurrent, weak-reference, all-library, JDK-image, jtreg/JCK, platform and performance gates remain open. String → arrays → collections → AST/compiler order unchanged.
