<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Collections physical source coverage

[Guide](README.md) · [Package catalog](m3-packages.md#package-3) · [Factories and delivery](reconcile-factories-and-delivery-scope.md) · [Coverage](coverage.md)

This map routes every physical Java file directly in
`m3/collections/src/main/java/com/m3/collections/` to one primary body-contract
reference. At source pin `33cd1758a4a4613d3868ecde31d6d83400f66aed` the complete
unfiltered directory has **77 files: 76 owner/helper declarations and
package-info.java**. There are no subdirectories or other files in that package
directory. Its 77 paths and Git blobs match the earlier
`1011050da4205a901ad079d7711b6b8efd8a9af7` snapshot; full source bytes were
hash-checked. A nested interface/class or an anonymous cursor remains part of its
physical source file, not an additional file in this denominator.

Each linked reference states which complete bodies, declared members, dependencies
and test assertions it inspected. **A documentation route is not a runtime,
compatibility, concurrency, resource or receiving-qualification result.** No
repository build, test, recipe, verifier, JNI/native run, jtreg suite or benchmark
was executed for this mapping pass. Historical source/test receipts and hosted
CI must retain their own exact commit, scope and status.

## Reconcile earlier and remaining routes

The earlier linked-hash, primitive-map and primitive-sequence references supply
primary body-contract routes for **19 distinct files**: six, four and nine
respectively. They also reused the factory facade. In particular,
`M3Collections` had already been read in full, making **20 previously
full-body-read files**, but only selected factory contracts were documented.
That earlier read was not a twentieth completed primary route.

The remaining references below route **58 distinct files**, including the
facade and package descriptor. The complete factory reference supplies all 58
declared factory overloads and qualifies the descriptor's broad wording. The
19 earlier routes plus these 58 routes cover the 77 physical files exactly once.
Cross-reference/dependency reads, inherited methods, callbacks and test files
are not counted again as new primary production files.

| Primary reference | Distinct physical files |
| --- | --- |
| [Linked-hash maps and shared slots](collections-reference.md) | 6 |
| [Primitive and snapshot long maps](primitive-map-reference.md) | 4 |
| [Primitive long sequences](primitive-sequence-reference.md) | 9 |
| [Factories and delivery scope](reconcile-factories-and-delivery-scope.md) | 2 |
| [Trees and lazy values](specify-trees-and-lazy-values.md) | 11 |
| [Locked map views](specify-locked-map-views.md) | 3 |
| [Blocking and transfer queues](specify-blocking-and-transfer-queues.md) | 2 |
| [Progress ownership](specify-progress-ownership.md) | 8 |
| [Stable segmented storage](specify-stable-segmented-storage.md) | 8 |
| [Atomic primitive storage](specify-atomic-primitive-storage.md) | 3 |
| [Residency lifecycle](specify-residency-lifecycle.md) | 2 |
| [Weak-key lifetime](specify-weak-key-lifetime.md) | 1 |
| [Object linked sequences](specify-object-linked-sequences.md) | 1 |
| [Primitive set variants](specify-primitive-set-variants.md) | 6 |
| [Primitive heaps](specify-primitive-heaps.md) | 2 |
| [Int-key map specializations](specify-int-key-map-specializations.md) | 4 |
| [Enum specializations](specify-enum-specialization.md) | 3 |
| [Identity specializations](specify-identity-specialization.md) | 2 |

The first three rows are the 19 earlier routes. The remaining rows sum to 58;
the factories row includes `package-info.java`. This is a bounded source
documentation index, not a statement that every collection behavior or every
part of the repository has been qualified.

## Exact physical source routing

Every source link uses the same immutable pin. The Git blob column identifies
the full file, independent of line anchors in the references. Public declaration
means a public Java declaration in an exported package at this pin; it does not
promise API stability, Java SE conformance or installed-image delivery.

| Physical source file | Role | Primary body-contract reference | Git blob |
| --- | --- | --- | --- |
| [M3AdaptiveEnumLongMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3AdaptiveEnumLongMap.java) | Public declaration | [Enum specializations](specify-enum-specialization.md) | `bc836b4db9ad07a2f10ff1456b170162dd488cd3` |
| [M3Address28.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3Address28.java) | Public declaration | [Stable segmented storage](specify-stable-segmented-storage.md) | `ae1e43c52351d205ebfc7e57ca5a230a49410399` |
| [M3BitLane28.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3BitLane28.java) | Public declaration | [Stable segmented storage](specify-stable-segmented-storage.md) | `8c0ede05cb02ef87fb052090fa14f05db079acfc` |
| [M3BlockingDeque.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3BlockingDeque.java) | Public declaration | [Blocking and transfer queues](specify-blocking-and-transfer-queues.md) | `7dac1b0557defd213dc7cb571ff56a3e81c38bc2` |
| [M3ChildProgressMonitor.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3ChildProgressMonitor.java) | Package-private helper | [Progress ownership](specify-progress-ownership.md) | `a62a6ace1ec939a0eeba7b49e6369a9d081d544b` |
| [M3Collections.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3Collections.java) | Public declaration | [Factories and delivery scope](reconcile-factories-and-delivery-scope.md) | `9b49f723dfa91628978e26db2564e0282a3e78a9` |
| [M3ConcurrentBitSet.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3ConcurrentBitSet.java) | Public declaration | [Atomic primitive storage](specify-atomic-primitive-storage.md) | `e6ddbdfc21f48995d00f3a940a78d34fe30a743a` |
| [M3ConcurrentIntLane28.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3ConcurrentIntLane28.java) | Public declaration | [Atomic primitive storage](specify-atomic-primitive-storage.md) | `c1ec6d7a13943ea937ba457580605588c96dc41f` |
| [M3ConcurrentLongLane28.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3ConcurrentLongLane28.java) | Public declaration | [Atomic primitive storage](specify-atomic-primitive-storage.md) | `baa2c6e3beab9d29d054e2707d2f6e1adca0deee` |
| [M3ConcurrentLongMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3ConcurrentLongMap.java) | Public declaration | [Primitive and snapshot long maps](primitive-map-reference.md) | `380a8ef113170833ae3151cd67f73f46fe49eae8` |
| [M3ConcurrentLongSet.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3ConcurrentLongSet.java) | Public declaration | [Primitive set variants](specify-primitive-set-variants.md) | `9f1bda51b120ce2e02252d877d07c008ea9f5795` |
| [M3DefaultProgressMonitor.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3DefaultProgressMonitor.java) | Public declaration | [Progress ownership](specify-progress-ownership.md) | `bd71a5b28d5d78f2fdd4540207c695df9b16cd93` |
| [M3EnumLongMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3EnumLongMap.java) | Public declaration | [Enum specializations](specify-enum-specialization.md) | `a57ee7d571db8dc71179e725741b3b3b78f059f1` |
| [M3EnumSet.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3EnumSet.java) | Public declaration | [Enum specializations](specify-enum-specialization.md) | `17ac92820dd49ed0a4db7c477e85d17c6859c31e` |
| [M3IntCollection.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3IntCollection.java) | Public declaration | [Primitive set variants](specify-primitive-set-variants.md) | `e0242a99ee9a7039a297d7312fbcccec0f6af131` |
| [M3IntHashSet.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3IntHashSet.java) | Public declaration | [Primitive set variants](specify-primitive-set-variants.md) | `764e435a38821b8295fcc750d3502cb724af4822` |
| [M3IntIntConsumer.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3IntIntConsumer.java) | Public declaration | [Int-key map specializations](specify-int-key-map-specializations.md) | `27cf3e485304c752cc278b620853f2faf751d056` |
| [M3IntIntHashMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3IntIntHashMap.java) | Public declaration | [Int-key map specializations](specify-int-key-map-specializations.md) | `f1e3c1df5e174136778232c8596b85eab3453bfd` |
| [M3IntIterator.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3IntIterator.java) | Public declaration | [Primitive set variants](specify-primitive-set-variants.md) | `e7e8deda81aea3130615ca420734047bb5c34aed` |
| [M3IntLane28.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3IntLane28.java) | Public declaration | [Stable segmented storage](specify-stable-segmented-storage.md) | `08f1ac25be41f517f91052ef2e5289854c50f0e3` |
| [M3IntLongConsumer.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3IntLongConsumer.java) | Public declaration | [Int-key map specializations](specify-int-key-map-specializations.md) | `425c3868c59af76d12edf6c725bec12bb216fe88` |
| [M3IntLongHashMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3IntLongHashMap.java) | Public declaration | [Int-key map specializations](specify-int-key-map-specializations.md) | `9ed0f60d04556e8547cf424f9624bcd16caeb858` |
| [M3IntPriorityQueue.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3IntPriorityQueue.java) | Public declaration | [Primitive heaps](specify-primitive-heaps.md) | `7816135483c11e4c898e9ecd79443ac96085fa99` |
| [M3LazyEventBits.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LazyEventBits.java) | Public declaration | [Trees and lazy values](specify-trees-and-lazy-values.md) | `5215c0fc0cb2f1a126ffb6a7efc776c9a922304b` |
| [M3LazyEventGranularity.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LazyEventGranularity.java) | Public declaration | [Trees and lazy values](specify-trees-and-lazy-values.md) | `4697fe359fab94d1b16e8e88116c0e66cec87f2f` |
| [M3LazyEventPolicy.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LazyEventPolicy.java) | Public declaration | [Trees and lazy values](specify-trees-and-lazy-values.md) | `648fe4d9f6827bf0eb89a993a8cc4099d424de59` |
| [M3LazyEventSink.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LazyEventSink.java) | Public declaration | [Trees and lazy values](specify-trees-and-lazy-values.md) | `8bbec2e416bf4b5071718c06bff04ffda2d295c4` |
| [M3LazyEventSource.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LazyEventSource.java) | Public declaration | [Trees and lazy values](specify-trees-and-lazy-values.md) | `6f3d78be857b5d30971ed5cd303164e34a1b6bdb` |
| [M3LazyEvents.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LazyEvents.java) | Public declaration | [Trees and lazy values](specify-trees-and-lazy-values.md) | `ec38c2141a1c9f201b6130a3eab260c0109423f5` |
| [M3LazyOperation.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LazyOperation.java) | Public declaration | [Trees and lazy values](specify-trees-and-lazy-values.md) | `38103fb1364800438930619d278c2ea9ecc8851d` |
| [M3LazyValueProvider.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LazyValueProvider.java) | Public declaration | [Trees and lazy values](specify-trees-and-lazy-values.md) | `08eea75b8fa34523bfc6c2cd76a01080c1db4554` |
| [M3LazyValueState.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LazyValueState.java) | Package-private helper | [Trees and lazy values](specify-trees-and-lazy-values.md) | `d530ca74329fc2a6d0210cb7e525c838e88c4928` |
| [M3LinkedHashMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LinkedHashMap.java) | Public declaration | [Linked-hash maps and shared slots](collections-reference.md) | `e0957cfd8a7a5abfa1c0c5e7bade32997e5ed657` |
| [M3LinkedHashSet.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LinkedHashSet.java) | Public declaration | [Linked-hash maps and shared slots](collections-reference.md) | `58fd72f6e1464a2c6e2c26a1519ee6cd226a3924` |
| [M3LinkedList.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LinkedList.java) | Public declaration | [Object linked sequences](specify-object-linked-sequences.md) | `a3721fc90071d6915ab6682ad4cb9d9147ea0b1d` |
| [M3LockedMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LockedMap.java) | Public declaration | [Locked map views](specify-locked-map-views.md) | `d19d5dae538692fbc54b30bb971dcaf708a12120` |
| [M3LockedTreeMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LockedTreeMap.java) | Public declaration | [Locked map views](specify-locked-map-views.md) | `65c544c78092f4f365c26904aac45b11e574760a` |
| [M3LockedTreeSet.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LockedTreeSet.java) | Public declaration | [Locked map views](specify-locked-map-views.md) | `8a4428a9c535b5e136851f7ca040959a7aff4170` |
| [M3LongArena28.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LongArena28.java) | Public declaration | [Stable segmented storage](specify-stable-segmented-storage.md) | `6dc6137c55c290dd8c618f1588c84d98b1d853e6` |
| [M3LongArrayDeque.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LongArrayDeque.java) | Public declaration | [Primitive long sequences](primitive-sequence-reference.md) | `985f858e0e6af79ad6a251a6341cad20194a2223` |
| [M3LongArrayList.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LongArrayList.java) | Public declaration | [Primitive long sequences](primitive-sequence-reference.md) | `97a16e5da6d8fa57ae18e1165f94869b99736397` |
| [M3LongCollection.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LongCollection.java) | Public declaration | [Primitive long sequences](primitive-sequence-reference.md) | `a2cf7fb8a1635eb0360cdc3f40052e3565dce23b` |
| [M3LongDeque.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LongDeque.java) | Public declaration | [Primitive long sequences](primitive-sequence-reference.md) | `76ffb9aa270f267077903c200fd17133085d358f` |
| [M3LongHashSet.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LongHashSet.java) | Public declaration | [Primitive set variants](specify-primitive-set-variants.md) | `e2e4d1fe15fab3d407a28f4291be7fe51a98dfeb` |
| [M3LongIterator.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LongIterator.java) | Public declaration | [Primitive and snapshot long maps](primitive-map-reference.md) | `3027524899d7280b43d24ac41ba9bdd237c4ff0d` |
| [M3LongLane28.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LongLane28.java) | Public declaration | [Stable segmented storage](specify-stable-segmented-storage.md) | `1aa4b6452f3f2be307c60b14c4843bef7999e9e2` |
| [M3LongLaneStore28.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LongLaneStore28.java) | Public declaration | [Stable segmented storage](specify-stable-segmented-storage.md) | `25d020f16a6c519b3ed65d9796a68abea61bf312` |
| [M3LongLinkedHashSet.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LongLinkedHashSet.java) | Public declaration | [Primitive set variants](specify-primitive-set-variants.md) | `2ecca7cdfbdb8564ba0714abed92af1b881c4efc` |
| [M3LongLinkedList28.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LongLinkedList28.java) | Public declaration | [Stable segmented storage](specify-stable-segmented-storage.md) | `261754cd670fd1e34ef67c5a4363f31729c4279f` |
| [M3LongList.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LongList.java) | Public declaration | [Primitive long sequences](primitive-sequence-reference.md) | `38b57efc731704c8c74391026c51bc49804e0fd6` |
| [M3LongListSpliterator.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LongListSpliterator.java) | Package-private helper | [Primitive long sequences](primitive-sequence-reference.md) | `d1b1fd808c0c40f4e670dc14be1075a422991dd2` |
| [M3LongLongConsumer.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LongLongConsumer.java) | Public declaration | [Primitive and snapshot long maps](primitive-map-reference.md) | `8ce5558a5d660b7f8a206dde3b59d1b30c45aab4` |
| [M3LongLongHashMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LongLongHashMap.java) | Public declaration | [Primitive and snapshot long maps](primitive-map-reference.md) | `4c9ffd7033def03f2ddd58c1c550e5fa3176524e` |
| [M3LongPriorityQueue.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LongPriorityQueue.java) | Public declaration | [Primitive heaps](specify-primitive-heaps.md) | `7ea8b9cd3ebe1d582026a98649d8598c72789cc3` |
| [M3LongQueue.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LongQueue.java) | Public declaration | [Primitive long sequences](primitive-sequence-reference.md) | `bb0a61f6e8eeb93f565a4b9fd29a0029baf3872c` |
| [M3LongSequence.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LongSequence.java) | Public declaration | [Primitive long sequences](primitive-sequence-reference.md) | `3589d0814b375dba23618ae3279c95c1ddd8a027` |
| [M3LongStack.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3LongStack.java) | Public declaration | [Primitive long sequences](primitive-sequence-reference.md) | `b2564e9678f554006ffb66b55ead93c131781795` |
| [M3ObjectIdentityMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3ObjectIdentityMap.java) | Public declaration | [Identity specializations](specify-identity-specialization.md) | `3ce45a6f05500530856d587d933abcbce22d57b9` |
| [M3ObjectIdentitySet.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3ObjectIdentitySet.java) | Public declaration | [Identity specializations](specify-identity-specialization.md) | `0267aeb9a81c0a59c92827f615a697969428bc08` |
| [M3ObjectResidency.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3ObjectResidency.java) | Public declaration | [Residency lifecycle](specify-residency-lifecycle.md) | `0281f85fe543bee202632cae5cf79b536b2a1d70` |
| [M3OrderedSlotMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3OrderedSlotMap.java) | Package-private helper | [Linked-hash maps and shared slots](collections-reference.md) | `ce5fa2ec4306af03239ab7539ad91acaeaaa8245` |
| [M3Progress.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3Progress.java) | Public declaration | [Progress ownership](specify-progress-ownership.md) | `a968e424fe3b6439cb55aca48fd1908396fd40bd` |
| [M3ProgressAdapters.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3ProgressAdapters.java) | Public declaration | [Progress ownership](specify-progress-ownership.md) | `2d3e4c33e61a02eea44afb46b3f97dc57d41b0b9` |
| [M3ProgressMonitor.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3ProgressMonitor.java) | Public declaration | [Progress ownership](specify-progress-ownership.md) | `fbaaf21bb7f013bebbc32d0d43dbf71e47b90059` |
| [M3ProgressScope.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3ProgressScope.java) | Public declaration | [Progress ownership](specify-progress-ownership.md) | `3b9505cfe94c04747a4071ffc04401f74669b78d` |
| [M3ProgressState.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3ProgressState.java) | Public declaration | [Progress ownership](specify-progress-ownership.md) | `8e15d49507c1890385d57e090dfcc2a0f16f71f0` |
| [M3Residency.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3Residency.java) | Public declaration | [Residency lifecycle](specify-residency-lifecycle.md) | `bdc8ad0d22a3c4522532b1678f0a35c0f068ea40` |
| [M3SlotAllocator28.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3SlotAllocator28.java) | Public declaration | [Stable segmented storage](specify-stable-segmented-storage.md) | `cbd0a33b1bc3fbbd25fc9a724998dddfc27b57c8` |
| [M3SlotArrays.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3SlotArrays.java) | Package-private helper | [Linked-hash maps and shared slots](collections-reference.md) | `e337c69710c871f8caa77a1afe53149f1fd705cc` |
| [M3SlotMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3SlotMap.java) | Package-private helper | [Linked-hash maps and shared slots](collections-reference.md) | `a10ee53abb325476ac32c7210712e30cc304182c` |
| [M3SlotSpliterators.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3SlotSpliterators.java) | Package-private helper | [Linked-hash maps and shared slots](collections-reference.md) | `dab4d257bf82c8dbdf8fec04e1ac0d84ac7e4a83` |
| [M3SystemProgress.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3SystemProgress.java) | Public declaration | [Progress ownership](specify-progress-ownership.md) | `fb91e213206441647810a720fa8f841e07ab83cd` |
| [M3TransferQueue.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3TransferQueue.java) | Public declaration | [Blocking and transfer queues](specify-blocking-and-transfer-queues.md) | `f9f2eac06609acbe5a4c88b02f21ddc4e3c6f0ea` |
| [M3TreeMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3TreeMap.java) | Public declaration | [Trees and lazy values](specify-trees-and-lazy-values.md) | `d306dacbb73d48c955fa0b932acc2c3d1eae5626` |
| [M3TreeSet.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3TreeSet.java) | Public declaration | [Trees and lazy values](specify-trees-and-lazy-values.md) | `e414f679a341392770fb482377a51fe2f5e6518f` |
| [M3WeakHashMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/M3WeakHashMap.java) | Public declaration | [Weak-key lifetime](specify-weak-key-lifetime.md) | `8fcde012cb5cf38061475e365b90e6f1ca4b0a3d` |
| [package-info.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/jdk/m3/collections/package-info.java) | Package descriptor | [Factories and delivery scope](reconcile-factories-and-delivery-scope.md) | `cb31702d369e5a371879468f84918e066bc88dce` |

## What this denominator excludes

- The separate [module-info.java][module] is outside this package directory.
  Including it gives **78 main Java files** for the module; it exports this
  package and declares only implicit `java.base`
- The complete module `src` subtree has **30 test Java files**, plus a separate
  native C source. Thirty is a file count, not a suite, assertion, executed-test
  or coverage percentage. Self-tests, JUnit wrappers and probes retain different
  roles
- The [module POM][pom], licenses, provenance, recipes, qualification manifests,
  family catalog and guides are not extra package Java owners. The source-tree
  identity is `c9dce538dc75eb2ca5fe0b048b35e5698f5c0394`
- Public/proving helpers with the same short names in `java.lang`,
  `jdk.internal.mindex`, `com.m3.precompute`, vendored donor packages or other
  projects are different physical owners and are not silently included here
- [JDK_FAMILY_MAP.tsv][families] PENDING names without physical files here, such
  as `M3LongLongLinkedHashMap`, `M3LongLongTreeMap`, `M3LongTreeSet`,
  `M3LongCopyOnWriteArrayList`, `M3LongLongConcurrentSkipListMap`,
  `M3LongConcurrentSkipListSet` and `M3WeakObjectMap`, are catalog proposals.
  Related current object/primitive names do not complete those rows
- A factory returning a JDK owner is not a new physical M3 implementation.
  Specifically enum and identity factory routes do not instantiate the similarly
  named M3 proving specializations

## Documentation coverage and delivery remain separate

The [factory/delivery reference](reconcile-factories-and-delivery-scope.md)
compares the physical tree with the currently selected installed manifest and
its dated receipt. Those are different evidence sets: the physical tree has
108 main-plus-test Java files, while the selected installed manifest declares
93 `src` Java paths among its 98 total rows. Its checksum agrees with its
metadata, but that does not make its path set match the current tree. The
verifier's reached source-set equality predicate would refuse that mismatch;
the documentation pass did not execute it. Preserve the historical receipt's
original scope rather than expanding old pass counts to these 77 owners.

The linked body-contract references distinguish API shape, storage ownership,
null/type policies, view/cursor lifetime, reentrancy, mutation/partial effects,
concurrency and callback boundaries where applicable. Test assertions read from
source are evidence of intended finite cases, not executed results. Full API
compatibility, concurrency/JMM/GC behavior, resource measurements, failure
injection, recipe replay, current-source successor qualification and
installed-runtime promotion remain independent gates.

The [porting invariant][policy] retains canonical Synexia recipe/source
ownership, target-specific M3JDK runtime authority, source/license provenance
and the ordered **String → arrays → collections → AST/compiler → remaining
families** receiving sequence. This physical routing map changes no source,
policy, recipe, manifest or receiving phase.

[module]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/module-info.java
[pom]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/pom.xml
[families]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/JDK_FAMILY_MAP.tsv
[policy]: https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/docs/m3jdk21-porting-invariant.md

