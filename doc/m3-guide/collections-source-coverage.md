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
| [M3AdaptiveEnumLongMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3AdaptiveEnumLongMap.java) | Public declaration | [Enum specializations](specify-enum-specialization.md) | `67a1bf136c21ab2b347f0a33ec4784e17c0eb245` |
| [M3Address28.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3Address28.java) | Public declaration | [Stable segmented storage](specify-stable-segmented-storage.md) | `2dda612d7cfdc39d8138c17a85308dc9d084b28b` |
| [M3BitLane28.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3BitLane28.java) | Public declaration | [Stable segmented storage](specify-stable-segmented-storage.md) | `bf44fbf34b893f3919fa1919064869c5aac7d0c3` |
| [M3BlockingDeque.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3BlockingDeque.java) | Public declaration | [Blocking and transfer queues](specify-blocking-and-transfer-queues.md) | `b2c730f6d666a81fb1ea1bd2a8f220820c1dfa35` |
| [M3ChildProgressMonitor.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3ChildProgressMonitor.java) | Package-private helper | [Progress ownership](specify-progress-ownership.md) | `44a791db2fac00214479e7020ec3c509f70d7ef2` |
| [M3Collections.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3Collections.java) | Public declaration | [Factories and delivery scope](reconcile-factories-and-delivery-scope.md) | `3d2f5aa53bda55293b3254bed3d526b896cbd044` |
| [M3ConcurrentBitSet.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3ConcurrentBitSet.java) | Public declaration | [Atomic primitive storage](specify-atomic-primitive-storage.md) | `feb93280cbc1481c40c52156ffc50e2be5e35b54` |
| [M3ConcurrentIntLane28.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3ConcurrentIntLane28.java) | Public declaration | [Atomic primitive storage](specify-atomic-primitive-storage.md) | `bc2004e2c4802c3338c0099d537b94fb49dc4363` |
| [M3ConcurrentLongLane28.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3ConcurrentLongLane28.java) | Public declaration | [Atomic primitive storage](specify-atomic-primitive-storage.md) | `176991710420d96aace2774c59fe0137d2b1ba4e` |
| [M3ConcurrentLongMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3ConcurrentLongMap.java) | Public declaration | [Primitive and snapshot long maps](primitive-map-reference.md) | `bbe25dfd008fe143806c8c3aefac6d33e413aec2` |
| [M3ConcurrentLongSet.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3ConcurrentLongSet.java) | Public declaration | [Primitive set variants](specify-primitive-set-variants.md) | `1dd5331b42efa855a51ba31d40380b0064231983` |
| [M3DefaultProgressMonitor.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3DefaultProgressMonitor.java) | Public declaration | [Progress ownership](specify-progress-ownership.md) | `2c5f52b913ed7d4207645871f522ad63c2bf57df` |
| [M3EnumLongMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3EnumLongMap.java) | Public declaration | [Enum specializations](specify-enum-specialization.md) | `43623113339ffff197d682f2c497b0f07abd4b25` |
| [M3EnumSet.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3EnumSet.java) | Public declaration | [Enum specializations](specify-enum-specialization.md) | `3b9cd8179549116524b7f9252a29ba185c22fc98` |
| [M3IntCollection.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3IntCollection.java) | Public declaration | [Primitive set variants](specify-primitive-set-variants.md) | `bd6aff905cf47e64ccd76bc19acc899ef93742af` |
| [M3IntHashSet.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3IntHashSet.java) | Public declaration | [Primitive set variants](specify-primitive-set-variants.md) | `e4272ab154175d115873d38f305a8fdf380d1f7d` |
| [M3IntIntConsumer.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3IntIntConsumer.java) | Public declaration | [Int-key map specializations](specify-int-key-map-specializations.md) | `f1329b55c234a65faad61be2a1a557b2a9b2dcd8` |
| [M3IntIntHashMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3IntIntHashMap.java) | Public declaration | [Int-key map specializations](specify-int-key-map-specializations.md) | `f944165b0ddd1b4c45b656a3e522e0038676d17e` |
| [M3IntIterator.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3IntIterator.java) | Public declaration | [Primitive set variants](specify-primitive-set-variants.md) | `366f92e9da842e00e346872f386ecfede38c5cb6` |
| [M3IntLane28.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3IntLane28.java) | Public declaration | [Stable segmented storage](specify-stable-segmented-storage.md) | `4e1e7eb7f2212493eecd544c3e7174ac1ca33232` |
| [M3IntLongConsumer.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3IntLongConsumer.java) | Public declaration | [Int-key map specializations](specify-int-key-map-specializations.md) | `49404c6e9b450da64228ddb4af958770f9060382` |
| [M3IntLongHashMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3IntLongHashMap.java) | Public declaration | [Int-key map specializations](specify-int-key-map-specializations.md) | `00e9190be49f7071504a477f0aad4eabd46f22d1` |
| [M3IntPriorityQueue.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3IntPriorityQueue.java) | Public declaration | [Primitive heaps](specify-primitive-heaps.md) | `3ee7233f27105d326832e212b01b8da17142569e` |
| [M3LazyEventBits.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LazyEventBits.java) | Public declaration | [Trees and lazy values](specify-trees-and-lazy-values.md) | `e0605534823bd2933b9a3f92f2481a1fef794b33` |
| [M3LazyEventGranularity.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LazyEventGranularity.java) | Public declaration | [Trees and lazy values](specify-trees-and-lazy-values.md) | `7e031ea2e06900aa6326d6d587ad66f290911d6a` |
| [M3LazyEventPolicy.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LazyEventPolicy.java) | Public declaration | [Trees and lazy values](specify-trees-and-lazy-values.md) | `aa79f1cc24bc83a5c2db5f7427a3a461715af380` |
| [M3LazyEventSink.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LazyEventSink.java) | Public declaration | [Trees and lazy values](specify-trees-and-lazy-values.md) | `f0085ec59bbedda0ca18d752996ad489ec976d5c` |
| [M3LazyEventSource.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LazyEventSource.java) | Public declaration | [Trees and lazy values](specify-trees-and-lazy-values.md) | `a21d4701650880ae5db7c569427ff64ec2195b92` |
| [M3LazyEvents.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LazyEvents.java) | Public declaration | [Trees and lazy values](specify-trees-and-lazy-values.md) | `c2a0aa479a4aa89b029ab9d99e22a3a27557b392` |
| [M3LazyOperation.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LazyOperation.java) | Public declaration | [Trees and lazy values](specify-trees-and-lazy-values.md) | `9d8939d6d768c3c1861a532d185e8c27afa35994` |
| [M3LazyValueProvider.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LazyValueProvider.java) | Public declaration | [Trees and lazy values](specify-trees-and-lazy-values.md) | `5bb4a035f57206bb82dc6b19657a0f39dc2d5fe8` |
| [M3LazyValueState.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LazyValueState.java) | Package-private helper | [Trees and lazy values](specify-trees-and-lazy-values.md) | `d13625a82d623a5aa99fee498e7b5afe793f504a` |
| [M3LinkedHashMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LinkedHashMap.java) | Public declaration | [Linked-hash maps and shared slots](collections-reference.md) | `ade59880b8c9b2bee1b32ae67ca9d29b7382d998` |
| [M3LinkedHashSet.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LinkedHashSet.java) | Public declaration | [Linked-hash maps and shared slots](collections-reference.md) | `e1ac806d1ebe8aac1b9efe7bcfbaa3f675e7d0d6` |
| [M3LinkedList.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LinkedList.java) | Public declaration | [Object linked sequences](specify-object-linked-sequences.md) | `da39d792baf7311691d2d6e8398adf62f3aa88a8` |
| [M3LockedMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LockedMap.java) | Public declaration | [Locked map views](specify-locked-map-views.md) | `17cdc6d3bba0093fba9f598f74219c90706f05be` |
| [M3LockedTreeMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LockedTreeMap.java) | Public declaration | [Locked map views](specify-locked-map-views.md) | `374960e9edfd4c1bbcc0951c4592d3ffd1b31f70` |
| [M3LockedTreeSet.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LockedTreeSet.java) | Public declaration | [Locked map views](specify-locked-map-views.md) | `fb472120f4e93fcf8ca474e390c89aaef63fc9a6` |
| [M3LongArena28.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongArena28.java) | Public declaration | [Stable segmented storage](specify-stable-segmented-storage.md) | `b265551ff952606326e25c0361bc6f35c72adedf` |
| [M3LongArrayDeque.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongArrayDeque.java) | Public declaration | [Primitive long sequences](primitive-sequence-reference.md) | `82975f80ff5a3728dff54e1157c9d7f50d95241b` |
| [M3LongArrayList.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongArrayList.java) | Public declaration | [Primitive long sequences](primitive-sequence-reference.md) | `9f6ca2c726083921373f203be74095f8691de3f8` |
| [M3LongCollection.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongCollection.java) | Public declaration | [Primitive long sequences](primitive-sequence-reference.md) | `d0b389681ade738e97426d950b6291c746912cf0` |
| [M3LongDeque.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongDeque.java) | Public declaration | [Primitive long sequences](primitive-sequence-reference.md) | `7fb1dc4bcd5275c703a19f36062ae5c66b3d2f43` |
| [M3LongHashSet.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongHashSet.java) | Public declaration | [Primitive set variants](specify-primitive-set-variants.md) | `1fe9ac1bfa11cf6a626a749c37d64ccec0ed09df` |
| [M3LongIterator.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongIterator.java) | Public declaration | [Primitive and snapshot long maps](primitive-map-reference.md) | `44778a63d99c5b1f99adee536b954a4ebb8c64f3` |
| [M3LongLane28.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongLane28.java) | Public declaration | [Stable segmented storage](specify-stable-segmented-storage.md) | `8616ab397be434e947a3148ea7b02e946c710e99` |
| [M3LongLaneStore28.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongLaneStore28.java) | Public declaration | [Stable segmented storage](specify-stable-segmented-storage.md) | `bc67c1f6d4da60c330c2d130cfa69b9c05d5b814` |
| [M3LongLinkedHashSet.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongLinkedHashSet.java) | Public declaration | [Primitive set variants](specify-primitive-set-variants.md) | `28c864a69a384dbfe6fbc499eff7da4015264a3f` |
| [M3LongLinkedList28.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongLinkedList28.java) | Public declaration | [Stable segmented storage](specify-stable-segmented-storage.md) | `f92fd3b564b53ee7bee422db4ce7b6b63a0860eb` |
| [M3LongList.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongList.java) | Public declaration | [Primitive long sequences](primitive-sequence-reference.md) | `df2d2741b67987d93deefcd75f139d5648eb4dc2` |
| [M3LongListSpliterator.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongListSpliterator.java) | Package-private helper | [Primitive long sequences](primitive-sequence-reference.md) | `a1a620d464f7b29bb8a5e801e51a1ce9592d3f7f` |
| [M3LongLongConsumer.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongLongConsumer.java) | Public declaration | [Primitive and snapshot long maps](primitive-map-reference.md) | `b1c01d1afacc76bd338179398ab42835e16992fa` |
| [M3LongLongHashMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongLongHashMap.java) | Public declaration | [Primitive and snapshot long maps](primitive-map-reference.md) | `b1d85622c2e19db069d0b2a64fb8011b5c01be34` |
| [M3LongPriorityQueue.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongPriorityQueue.java) | Public declaration | [Primitive heaps](specify-primitive-heaps.md) | `2fb403549f2baf0afe5b04743647c26ebd4cce80` |
| [M3LongQueue.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongQueue.java) | Public declaration | [Primitive long sequences](primitive-sequence-reference.md) | `8fcb61bd6a8a5077008ac0102f44f28035096bec` |
| [M3LongSequence.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongSequence.java) | Public declaration | [Primitive long sequences](primitive-sequence-reference.md) | `26ca657bd372e3a70ed4ee84a216e9a78b95dba0` |
| [M3LongStack.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3LongStack.java) | Public declaration | [Primitive long sequences](primitive-sequence-reference.md) | `94ff060e1294197863e3676632e9809cfc6d8593` |
| [M3ObjectIdentityMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3ObjectIdentityMap.java) | Public declaration | [Identity specializations](specify-identity-specialization.md) | `18f9277389f7fa5ab8ddbff9971bb0f941659750` |
| [M3ObjectIdentitySet.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3ObjectIdentitySet.java) | Public declaration | [Identity specializations](specify-identity-specialization.md) | `6f20a64f8d7e9dc6bbc9880396c502349e2e3d7a` |
| [M3ObjectResidency.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3ObjectResidency.java) | Public declaration | [Residency lifecycle](specify-residency-lifecycle.md) | `0281f85fe543bee202632cae5cf79b536b2a1d70` |
| [M3OrderedSlotMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3OrderedSlotMap.java) | Package-private helper | [Linked-hash maps and shared slots](collections-reference.md) | `e7be2297d19f22cf4dd4ef8b7d1424fe963063d9` |
| [M3Progress.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3Progress.java) | Public declaration | [Progress ownership](specify-progress-ownership.md) | `5728ff52b138121791fd2b12c923a883d52327bc` |
| [M3ProgressAdapters.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3ProgressAdapters.java) | Public declaration | [Progress ownership](specify-progress-ownership.md) | `87d3fa14669325d26d0fe1f9a626fce43b357a51` |
| [M3ProgressMonitor.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3ProgressMonitor.java) | Public declaration | [Progress ownership](specify-progress-ownership.md) | `fbaaf21bb7f013bebbc32d0d43dbf71e47b90059` |
| [M3ProgressScope.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3ProgressScope.java) | Public declaration | [Progress ownership](specify-progress-ownership.md) | `591223844f3cfd50fdf5431deb3bc2b1c9d7b5f0` |
| [M3ProgressState.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3ProgressState.java) | Public declaration | [Progress ownership](specify-progress-ownership.md) | `23e877a0862e2b9a9a83f247e6b0b499a0757d9e` |
| [M3Residency.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3Residency.java) | Public declaration | [Residency lifecycle](specify-residency-lifecycle.md) | `34c18f45632096c3bd4f0df199fe99a1a59c2f16` |
| [M3SlotAllocator28.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3SlotAllocator28.java) | Public declaration | [Stable segmented storage](specify-stable-segmented-storage.md) | `3aa639f8fa88c948babfacb5b34511dd2f18442d` |
| [M3SlotArrays.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3SlotArrays.java) | Package-private helper | [Linked-hash maps and shared slots](collections-reference.md) | `e337c69710c871f8caa77a1afe53149f1fd705cc` |
| [M3SlotMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3SlotMap.java) | Package-private helper | [Linked-hash maps and shared slots](collections-reference.md) | `a7d20300717661c26897f0b69e1e5a83400a62ad` |
| [M3SlotSpliterators.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3SlotSpliterators.java) | Package-private helper | [Linked-hash maps and shared slots](collections-reference.md) | `30cf137538353ac8e91514fff7c5db3903da8819` |
| [M3SystemProgress.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3SystemProgress.java) | Public declaration | [Progress ownership](specify-progress-ownership.md) | `66984feed7d22b8e047f69de0862b7ade9083ede` |
| [M3TransferQueue.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3TransferQueue.java) | Public declaration | [Blocking and transfer queues](specify-blocking-and-transfer-queues.md) | `54c577fcbd5d43959b748e734603f40370d98e78` |
| [M3TreeMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3TreeMap.java) | Public declaration | [Trees and lazy values](specify-trees-and-lazy-values.md) | `8a85b86c027bc031ef096ad94de2747b8fd28f9c` |
| [M3TreeSet.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3TreeSet.java) | Public declaration | [Trees and lazy values](specify-trees-and-lazy-values.md) | `e414f679a341392770fb482377a51fe2f5e6518f` |
| [M3WeakHashMap.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/M3WeakHashMap.java) | Public declaration | [Weak-key lifetime](specify-weak-key-lifetime.md) | `5d7eb065aeca2cecc1d939532d154c87bbb01c2c` |
| [package-info.java](https://github.com/hsoliwal/M3jdk21/blob/33cd1758a4a4613d3868ecde31d6d83400f66aed/m3/collections/src/main/java/com/m3/collections/package-info.java) | Package descriptor | [Factories and delivery scope](reconcile-factories-and-delivery-scope.md) | `3998c9f78a8617aa3b593c353f7d7ac48e93a52b` |

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

