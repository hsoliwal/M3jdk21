<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3JDK21 SDK five-provider receiver, pinned 2026-10-17

Canonical Synexia recipe: https://github.com/hsoliwal/com.synexia/pull/12116
Canonical recipe commit: `59ad4a609afcb5c4297a44f78b4293975a0d4618`.
Exact original target base: `master@4e29dc9d4141261e8763fbe9ffd6a20e017b1aff`.

This receiver has two commits: first the mirrored, source-sealed Maven recipe and workflow; second its exact four generated postimages. Keep existing `java.util`, `java.util.concurrent`, `M3LinkedHashMap`, `M3IndexMaskCompactor`, `M3CollectionViews` and their original contracts/ABI unchanged.

Catalogue covers 104 named upstream FQCN (41 JDK, 23 Guava, 17 Eclipse, 17 Apache Commons, 6 Spring). A catalogue entry is NOT an implemented upstream interface. New M3IndexedMultiValueMap is deliberately independent from Spring MultiValueMap, Guava Multimap, Eclipse MutableListMultimap and Apache MultiValuedMap.

Java 21 local selected-source contract test: PASS 42,897 assertions in both Java and checked JNI with strict C17 UBSan. CRITICAL: local map lookup used a JDK LinkedHashMap stub rather than the actual M3LinkedHashMap, and the local JNI backend is an earlier pinned M3 implementation. Target CI compiles the **actual** M3 owner and latest native code, and should be treated as authoritative; those target-head results remain PENDING. Local direct Java recipe self-test/fixed-point PASS, Maven not available locally. Full reactor, installed JDK, jtreg, ABI, provider tests, memory/CPU and JNI multi-platform results remain unproven. Do not promote String -> arrays -> collections phase or merge until necessary gates pass.

Donor rights: new Synexia first-party Apache-2.0 source Copyright 2026 Hitesh Soliwal and contributors; existing Synexia MatIntMultiMap/MatIdMultiMap reference-only and pinned, no upstream challenge/provider source copied.
