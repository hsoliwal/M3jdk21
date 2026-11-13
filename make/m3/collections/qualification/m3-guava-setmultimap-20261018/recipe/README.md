<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 Guava SetMultimap direct M3 backing — canonical recipe

## Five-library SDK objective and actual boundary
Keep source/public names from **JDK**, **Guava**, **Eclipse Collections**, **Apache Commons Collections** and **Spring Framework** distinct, while consolidating compatible storage atoms in public M3JDK21. This task installs a missing dedicated **M3GuavaSetMultimap<K,V>** whose *authoritative storage* is existing first-party `M3LinkedHashMap<K, Collection<V>>` plus per-key `M3LinkedHashSet<V>`. It does not create another index engine or copy Guava implementation source.

The exact `com.google.common.collect.SetMultimap<K,V>` surface is implemented through the official Guava `ForwardingSetMultimap`/ `Multimaps.newSetMultimap` public contract machinery, which sees only the **M3** map/set owners. This preserves live get/asMap/entries/keySet/keys semantics instead of approximating those callbacks and mutating views. The already present `M3LibraryCollections.guavaSetMultimap()` also borrows M3 map/set owners and is intentionally **not** replaced by this add-only atom; the new public `M3GuavaSetMultimap.create()` returns the specifically named M3 implementation.

The new opt-in `selectedByStates(int[] flags, int required, int forbidden, M3IndexPathMasks.Backend backend)` makes a detached M3-owned selection, using one 32-bit application mask per *entry occurrence* and the existing Java/C17 `M3IndexPathMasks` JNI selector. This does not move objects, predicates, comparators or Java heap references into JNI; no new native ABI. Full signed masks, duplicate rejection, nulls and order-preserving extraction are covered by real-Guava JUnit.

## Canonical recipe invariant
Reuse the **exact unchanged** first-party `M3GpuToolJoinRecipe` / `M3GpuToolJoinManifest` OpenRewrite SourceFile engine as SHA-pinned source blobs; the new named subclass supplies only a resource root. Two **ABSENT-before** Java postimage templates are sealed with SHA-256. Source path, occupied/drift, duplicate, template mismatch and second-run fixed-point are subject to actual OpenRewrite JUnit. No existing target production file is edited by hand or by this recipe. Later mutation of existing Java AST must use an admitted OpenRewrite Java visitor or existing hash-sealed engine with exact input.

Local selected-source Java21 `javac --release 21 -Xlint:all,-rawtypes,-unchecked -Werror` and basic Java mask smoke passed with **test-only Guava/M3 stand-ins**. This is NOT a real Guava binary/JNI or complete M3 module proof. Maven was not installed locally. The receiver CI compiles the true Guava 33.4.8-jre dependency against actual M3 source, executes 7,000 seeded differential operations, builds the unchanged native C17 mask symbol and reruns JUnit under `-Xcheck:jni`.

## Rights
Original code Copyright 2026 Hitesh Soliwal and contributors, Apache-2.0. Existing M3 map/set/JNI code retained unchanged and source pinned; Guava Apache-2.0 and its original notices remain in the optional interop dependency. Guava, LeetCode, HackerRank and GeeksforGeeks reference-only algorithms are **not** copied into this source. Complete all-five-library API substitutability, installed-JDK tests, resource/performance, concurrent mutation and upstream concrete serialized identity are not established by this bounded atom.
