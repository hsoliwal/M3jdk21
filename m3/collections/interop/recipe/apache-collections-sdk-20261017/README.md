# Apache Commons Collections M3 SDK adapter — recipe first

This is an additive, separately built interoperability slice, stacked on M3JDK21 SDK names PR #1127. Existing JDK, Guava, Spring and Eclipse owners remain unchanged.

The Apache provider's correct Java interfaces stay `org.apache.commons.collections4.ListValuedMap<K,V>` and `org.apache.commons.collections4.SetValuedMap<K,V>`. `M3ApacheCollections` factories implement these through inherited upstream `AbstractListValuedMap` / `AbstractSetValuedMap` public behaviors. Storage uses original M3LinkedHashMap key-slot arrays and M3LinkedList/M3LinkedHashSet per present key. No Apache source copied, no new per-value storage Entry node. API-level entry adapters and a value collection per key may allocate. Not thread-safe or Serializable.

The optional `m3/collections/interop` module gains exactly one dependency, `org.apache.commons:commons-collections4:4.5.0`. The M3 `java.base`-only core does not gain provider dependencies. Apache MultiValuedMap is NOT Guava Multimap or Spring MultiValueMap. Nor are Guava Multiset, Apache MultiSet and Eclipse Bag interchangeable.

## Mandatory recipe-first procedure
Named YAML: `com.synexia.m3.ApacheCollectionsInterop20261017`. Reuses existing source-sealed `M3HashPinnedJavaSnapshotRecipe` for three ABSENT Java outputs and canonical `M3HashPinnedTextSnapshotRecipe` for the exact POM pre/post. Explicit PlainText SDK mode proves source identity, NOT attributed AST rewriting. All source hashes and POM digests are recorded in manifests and need revalidation before emitting final output. No manual production-file patches; fix the recipe/template and rerun.

Performed **locally**: Java 21.0.12.1, Maven3.9.9, OpenRewrite8.17.1/JUnit5.10.2: 4 SDK JUnit tests PASS, zero failures; 8 partial Java installation states under two orders, fixed point, drift/duplicate refusal; exact OpenRewrite FindAndReplace POM change with SHA-256, XML syntax and second-pass no-op. Strict Java21 compile against 77 archived M3 owners and **compile-only Apache API signature scaffolds** passed. This is NOT the actual Apache provider binary test.

Provider binary absent offline. **Not locally performed:** full named composite test against canonical text recipe, actual Commons4 4.5.0 compile, 4600-step seeded provider binary differential, whole M3 reactor, installed JDK/jtreg, native regression and JMH. Focused receiver CI requires real Apache binary and existing JNI tests. Keep draft until proved; do not claim every collection family is complete.

Apache's original code and notices remain upstream under Apache-2.0; this first-party adapter/catalogue/test is copyright 2026 Hitesh Soliwal and contributors, Apache-2.0. LeetCode/HackerRank/GeeksforGeeks are problem-category references only, no copied solution/editorial.
