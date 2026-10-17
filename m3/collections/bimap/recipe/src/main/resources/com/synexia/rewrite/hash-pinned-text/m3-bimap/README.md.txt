<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3 bidirectional-map contract slice

`com.m3.collections.interop.M3BiMap<K,V>` implements Guava `BiMap` and the JDK `Map` contract with the existing `M3LinkedHashMap` storage in both directions. No second hash-table implementation, global interner, retained per-mapping Entry or node is introduced. Mutable key/value objects stay caller-owned and must have stable, nonthrowing, non-reentrant hash/equality behavior. Null is a valid key and value. This is an optional Apache-2.0 interoperability artifact, not a replacement for any concrete donor class or a java.base backend promotion.

## Implemented contract

Forward and inverse put/remove, strict duplicate-value refusal, forcePut, exact inverse-of-inverse identity, live remove-through key/value/entry views, entry setValue, iterator removal, map defaults, equality/hash, and per-direction encounter order are covered. Values traverse the corresponding forward key order. Inverse ordering is not equated with forward ordering. Entries are boundary projections; previously obtained entries become invalid on unrelated map mutation. Iterators are best-effort fail-fast, not synchronization.

`replaceAll` stages complete forward/reverse maps before publication. Cyclic permutations are supported. Duplicate output (including a value whose first owner is null), a callback exception, or staging allocation failure publishes no candidate index. A callback that itself mutates the receiver is detected, but the callback's own mutation is not rolled back. This is not an exact simulation of Guava's partial exceptional state. Ordinary put/forcePut/remove are not claimed atomic against arbitrary VM allocation failures or hostile callbacks. putAll retains ordinary sequential partial-success behavior.

`M3BiMaps` adds a detached M3 copy, a read-only independent copy using Guava's public unmodifiable wrapper, a public Guava synchronized wrapper, an ordered strict-key-and-value collector, Eclipse BiMap import, and a live Eclipse MutableMap adapter. The Eclipse bridge is **not** MutableBiMap substitutability. Spring's actual single-value bridge and Guava filtered/converter views are tested over this owner, not relabelled as new native implementations. Donor allocating-return methods can still return donor storage.

## Recipe-first reproduction

Canonical authoring owner: Synexia `synexia-openrewrite-recipes/recipe-crates/collections-library-interop/bimap`. The target contains an exact proof mirror. The POM references the already retained hash-pinned Java/text OpenRewrite providers; no engine is copied or modified. Java outputs are real Java LSTs. Native/document/data outputs use the existing exact text recipe. Each pass is hash-sealed and all filesystem paths are checked before writing. Use an exclusively owned checkout; per-file writes are not a multi-file transaction.

```sh
mvn -B -ntp -f m3/collections/bimap/recipe/pom.xml test
mvn -B -ntp -f m3/collections/bimap/recipe/pom.xml -Dm3.bimap.target="$PWD" test
```

In a standalone canonical checkout, set `-Dm3.snapshot.providers=/absolute/M3jdk21/m3/collections/counts/recipe/pinned-engine`. The receiver baseline is `9af26a07a296f665778972f46a2bb97bc4971940`; all inherited source/proof owners remain unchanged. See the workflow and actual generated reports for execution status; authored tests alone are not proof.

## Java and JNI verification

The Java suite exercises 48,000 seeded operations against real Guava HashBiMap in both directions, including forced hash collisions and nulls, plus bulk/view/default/replaceAll/collector/adapter cases. A separate **test-only JNI consumer** calls Java's real BiMap methods and validates exception identity, nulls, inverse lookup and mutations under -Xcheck:jni and fatal UBSan. It is not a production native lookup accelerator. Existing production count-lane/scalar JNI kernels and regression suites remain in the inherited verification path.

No throughput, retained-heap or universal speedup claim is made. Two indexes keep additional references and staged replaceAll temporarily holds old and new indexes. Source shape alone does not establish a memory win over Guava. Full Eclipse MutableBiMap, sorted/primitive/immutable/serialized bimap implementations, range families, general concurrent/reference/serialization closure, donor result-storage closure, installed-JDK release and whole-repository admission remain unqualified. Historical failing release gates are not weakened. String remains the active global receiving phase.
