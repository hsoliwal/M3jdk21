<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3 Collections SDK -- named provider capabilities

This is one additional SDK facade inside the existing Apache-2.0 M3JDK21
com.m3.collections module. It is not a new framework package, new payload
owner, Synexia runtime dependency, or blanket java.util replacement.

The machine-readable SDK_FAMILY_MAP.tsv retains correct original API FQNs
for JDK, Guava, Eclipse Collections, Spring Framework, and Apache Commons
Collections, with exact contract dispositions. A primitive M3LongBag is a
counted-value analogue of Multiset/Bag/MutableLongBag: it does not implement
their external interfaces. Spring MultiValueMap (Map<K,List<V>>) and Guava/
Commons multimaps have different semantics; all are PENDING, not aliases.

New M3IndexPartitions is an O(1) metadata live partition descriptor, backed
by the original JDK List.subList, with strictly unmodifiable outer list and
source RandomAccess marker preserved. M3LongBag delegates storage to existing
M3LongLongHashMap primitive parallel key/value/occupancy arrays, with long
overflow/negative refusal and no per-occurrence Entry objects. Other SDK
methods use existing M3Collections, M3FilteredLists, M3FilteredViews,
M3MutableFilteredViews, M3LongIndexProjection, and M3IndexPathMasks.

C17 JNI is reused only through M3IndexPathMasks with explicitly requested
NATIVE_REQUIRED; no Java predicate, comparator or object pointer enters JNI.
No new native symbol, zero-copy, performance, linearizability, serialization
or direct installed-JDK claim is made.

External frameworks are only contract/name/problem-category evidence:
first-party copyright 2026 Hitesh Soliwal and contributors; Apache-2.0.
JDK retains OpenJDK per-file terms, and third-party code/license is not copied.
LeetCode/HackerRank/GeeksforGeeks problem statements/categories are evidence
only, never transplanted solution source.

Canonical recipe/code/test/catalogue owner is hsoliwal/com.synexia. Source
is emitted as exact Java-LST postimages from M3HashPinnedJavaSnapshotRecipe;
supporting scripts/metadata use the existing hash-pinned text recipe.
Retain historic manifests; the additive SDK source overlay is strict.
Run target module Java21/Maven/JUnit and real -Xcheck:jni gates. Full
source-built JDK/jtreg, performance/retained-heap and String->arrays->
collections promotion are separate pending gates.
