<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3 optional collection modules

The existing `com.m3.collections` module owns generic read-only and mutable filtered/sorted views. This pack refines two private constructions in its current M3FilteredViews owner and reuses M3MutableFilteredViews unchanged. The optional `jdk.m3.collections` module exports only `com.m3.util` versioned primitive views and their existing-compatible support owners.

The Maven `m3-collections` profile composes the existing `m3/collections` project and this optional project; their product source paths and module identities remain authoritative. Both modules require only java.base at runtime. Generic oracle tests add an explicit test-only read edge from the optional module to the original module.

Run `mvn -f m3/tooling/module-packs/pom.xml -Pm3-collections verify` with the repository source-built JUnit artifacts available. Add `-Pm3-collections-image` for the existing JNI/JMOD/linked-image proof (Linux, Java21, C compiler, nm and objcopy). The image builder compiles all current original collection sources, retains the exact existing mask-compactor JNI ABI, and runs the same Java/native oracles on the host and linked image. No java.util or custom HotSpot promotion is asserted.

Fresh-base receipt pins M3jdk21@a918e588dd11d5ff7e0a6d4efaa89bb1679a9798. The earlier 970a343 no-owner generic projection remains historical unpromoted proof; its duplicate optional generic APIs are not delivered. Use canonical recipe `com.synexia.m3.M3JdkCollectionCurrentOwners20261017` with its exact current-base preimages. Original rights and notices remain in every source and in `legal/`; see PROJECTION_SOURCE_IDENTITY.json.

## Publication owner reconciliation

The final recipe is `com.synexia.m3.M3JdkCollectionPublicationOwners20261017`, pinned to `b42ab7605b862a5157997f1787c5032db2831f46`. It preserves the concurrently added stored-representative membership fixes and applies the same two private atoms to the existing generic owner. The current readonly family oracle passes before and after; the older lookup-cast oracle fails identically on the untouched current owner and final owner. Earlier image/JNI/Maven receipts retain their original pinned inputs and are not relabeled as a full build of this later repository head. The primitive sources, native ABI, module descriptor and build entry points are unchanged.
