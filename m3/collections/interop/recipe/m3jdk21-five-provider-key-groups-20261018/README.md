<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Five-provider indexed key grouping — canonical Synexia Maven/OpenRewrite crate

Product target: `hsoliwal/M3jdk21`, independent optional interop module (`m3/collections/interop`).
Canonical transformation owner: `hsoliwal/com.synexia`. Input preimages of the two new Java files
are `ABSENT`. `com.synexia.m3.FiveProviderKeyGroups20261018` composes the existing
`M3HashPinnedJavaSnapshotRecipe`, without another transform engine. Java postimages are
byte-exact template outputs; test hash-pin/fixed-point/mixed-presence/occupied-source refusal.

Implementation is an immutable, non-owning group index over `M3FiveLibraryIndexViews`.
It retains one existing M3LinkedHashMap group directory, one Object[] distinct-key directory,
`int[]` group boundaries, `int[]` input occurrence ordinals and `int[]` group order. No retained
per-occurrence Entry/Node wrapper; original payload and state planes remain canonical in the
existing view. Stable hash equality grouping uses count -> prefix -> stable scatter; sorted
order changes only group IDs. Source-borrowing group windows and selected group filters never
change the original snapshot. Repeated physical rows remain separate occurrences.

Five *actual*, separately typed outputs: JDK Map<K,List<V>>, Guava ListMultimap<K,V>,
Eclipse MutableListMultimap<K,V>, Spring MultiValueMap<K,V>, Apache ListValuedMap<K,V>.
Group snapshots are detached; each materialized provider result is independently mutable
and follows its own size/null/view protocol. Java comparison and caller callbacks never enter JNI;
optional state-mask filtering delegates to the **existing** Java/C17 `M3IndexMaskWindow` contract.
No new ABI, no Synexia runtime dependency and no copy of external source implementations.

`CATALOGUE.tsv` preserves LeetCode/HackerRank/GeeksforGeeks as problem taxonomy references,
not source donors. Existing M3 code is the first-party implementation donor. Respect OpenJDK
GPLv2 with Classpath Exception, Guava/Spring/Apache Apache-2.0, and Eclipse EPL/EDL boundaries.

To run actual OpenRewrite fixture proof when Maven is available:
`mvn -B -ntp -f synexia-openrewrite-recipes/recipes/m3jdk21-five-provider-key-groups-20261018/pom.xml test`

Product receiver gates: source SHA/refusal/fixed point, Java 21 strict M3 core,
real pinned Guava/Eclipse/Spring/Commons JUnit, checked JNI C17 parity, source/lineage
provenance, independent API/ABI/equality/serialization tests, memory/CPU benchmarks,
installed JDK jtreg/JCK and non-Linux native gates. No full five-library completion,
installed JDK replacement or overall throughput win is claimed.
