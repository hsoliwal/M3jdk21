<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Actual five-library SDK receiving: Eclipse multimap gap

**Product parent:** https://github.com/hsoliwal/M3jdk21/pull/1174 at exact `58a72d0b2b8ffab220e3dc6e53ff7cc5d136bfb5`.
**Canonical Synexia:** https://github.com/hsoliwal/com.synexia/pull/12187 at exact `34d989ab261c85b4267fa49a6ff3582a8dd86a5d`.
This stacked target PR preserves all prior 33 factory/20 collector APIs and their indexed M3 owners and source/CI receipts.

**New product Java:** `M3EclipseListMultimap<K,V>`, `M3EclipseSetMultimap<K,V>`, `M3EclipseBagMultimap<K,V>`, `M3EclipseMultimaps` and real-provider JUnit `M3EclipseMultimapsTest`. Every postimage is an exact SHA-256 and Git blob match against the canonical source-sealed OpenRewrite template. The first commit mirrors the canonical Maven/OpenRewrite crate and full CI; the second materializes only the five generated Java files.

JDK `Map<K,List<V>>`, Guava `ListMultimap`, Eclipse `MutableListMultimap`, Apache `ListValuedMap`, and Spring `MultiValueMap` are tested side by side with their own actual public namespaces; no renamed impostors. The Eclipse interfaces own `get()` mutability, absent/empty and pair count; do not cache live predicate calls or substitute a detached `BitSet` view.

Existing M3LinkedHashMap, M3LinkedList, M3LinkedHashSet, M3EclipseBag and M3ObjectCounts remain the **sole runtime payload/index/count owners**; no entry-node store, new JNI symbol, or donor source fork. Existing C17 checked-JNI selector is retested independently, not used to call Java object methods.

**Status: source postimages committed; local real provider/JUnit/Maven unavailable** (outbound dependency DNS blocked). GitHub Actions must prove exact-head Maven recipe SDK, Eclipse 11.1.0 JAR compile/JUnit differential, all retained five-provider Maven tests, and checked-JNI. Full JDK image/jtreg, all donor API classes, serialization/concurrency and performance remain separate closure gates. No force updates to parent branches.
