# M3 five-provider direct SDK integration — actual source receiving

**Stacked target:** M3JDK21 PR #1174, head 58a72d0b2b8ffab220e3dc6e53ff7cc5d136bfb5. This is the five-provider Java21/JNI-qualified array/count/flag baseline; it already includes actual M3-owned Guava Multiset/BiMap, Eclipse MutableBag, and Apache MultiSet/Bag/BidiMap implementations, plus M3 backed JDK, Spring and other provider factories. No second runtime/index/count engine is created.

## Newly received direct M3 owners

Original first-party M3JDK21 #1154: `M3ArrayList<E>` owns one dynamic Object[] of GC-traced values, with indexed JDK List operations, stable two-pass bitmask removeIf and backed subList. `M3IndexedListSnapshot<E>` owns a detached Object[] and primitive int[] row permutation; filtering precomputes accepted positions once, sorting moves indexes not payload objects, slicing adjusts logical offsets. Original test and JUnit source copies are preserved byte-for-byte; donor originally proved 960955 Java differential assertions and real JNI parity on its own head, not automatically on this stacked head.

Original first-party M3JDK21 #1190: `M3SpringArrayMultiValueMap<K,V>` directly implements **org.springframework.util.MultiValueMap** using the existing M3LinkedHashMap key directory and M3LinkedList-created lists. It does not inherit Spring MultiValueMapAdapter. Supplied lists preserve their identity, including null/empty/absent distinctions. The SDK factory changes only its implementation expression; the earlier `M3SpringMultiValueMap` class remains for compatibility. The source-shared Spring6.2 JUnit differential exercises 4500 operations but the current integrated head still requires real provider CI qualification.

## All five exact API names

JDK java.util.* retain their names, realized by distinct M3 backends; Guava com.google.common.collect.* retain original interfaces with M3GuavaMultiset and M3GuavaBiMap over primitive counts and indexed maps; Eclipse org.eclipse.collections.api.* uses M3EclipseBag over the same count/flag lanes; Spring org.springframework.util.MultiValueMap now uses a direct M3 class; Apache org.apache.commons.collections4.* uses distinct M3ApacheBag/MultiSet/BidiMap and live multivalue adapters. **Guava Multiset is not Apache MultiSet, and neither equals Eclipse Bag.** Guava Multimap is not Spring MultiValueMap nor Apache MultiValuedMap.

## Mandatory reusable recipe

`com.synexia.m3.FiveProviderDirectSdk20261017` composes the existing Maven/OpenRewrite `M3HashPinnedJavaSnapshotRecipe` in explicit byte-exact PlainText SourceFile custody mode (8 exact Java targets, seven absent plus one modified), and existing OpenRewrite `FindAndReplace` for the single `sdk-family-bindings.tsv` Spring implementation field. The companion manifest SHA256-seals original/after full bytes and the tests refuse source drift, duplicates and missing predecessor. No direct target output edits: first commit recipe/templates/tests/catalogue; second commit matching generated source changes. If something fails, repair the recipe and regenerate.

Native JNI C source unchanged: existing precomputed primitive array/mask routines are reused; Java predicates and object references remain in Java. The full ABI/JNI/provider tests must run again on the stacked receiver, not inherit green labels from parent.

## Qualification boundary

PR #1174 reports 211 current core and 126 provider Maven/JUnit methods, canonical OpenRewrite replay and checked JNI/UBSan on its original exact head, plus 33 provider factory signatures and 20 collectors. The two newly received donor branches have their own narrower tests. **This new composite does not yet have a passing exact-head Maven/full provider/native/installed-JDK receipt.** Do not claim that every concrete class, serialization/JMM variant and overload of five entire libraries is complete. Source-phase promotion String → arrays → collections → AST/compiler remains HOLD. Catalogue retains per-operation choices, performance limitations, source Git pins, donor rights, and LeetCode/HackerRank/GeeksforGeeks classification only.
