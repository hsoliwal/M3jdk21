# Current M3JDK21 collection array/mask/wrapper convergence - 2026-10-27

This is one M3JDK21 product receiving plan, not an M3 plus JDK layer.

It combines independently qualified current-master behavioral atoms while
preserving the original owners:

- java.base M3IndexedArray: one Object[] payload plus primitive offset/index/state lanes
- java.util.CollectionViews: original JDK-owned live filtered/sorted views
- M3FilteredViews, M3FilteredLists and M3FilteredDeques
- M3IndexedLiveView sorted metadata
- M3NativeFiveLibrarySdk vendor-free M3 factories
- existing primitive JNI mask owner and arrays JNI allocator

Every external challenge site is taxonomy/reference only. No LeetCode,
HackerRank or GeeksforGeeks solution body is copied.

## Invariants

- public contracts are preserved unless an existing reviewed PR already adds an internal API
- no retained per-entry Node/Entry payload storage
- no second authoritative payload array
- state is primitive masks
- logical/physical identity is primitive offset/index lanes
- Java Predicate stays in Java; native code receives primitive arrays/masks only
- failure-atomic growth publishes all seven planes only after every allocation succeeds
- filtered/sorted wrappers remain backed/live according to their owner contracts
- source manifests advance through a successor; historical receipts are retained

## Recipe custody

The combined M3IndexedArray postimage is produced by
com.synexia.m3.M3Jdk21CurrentCollectionsConvergence20261027.

Other received owners keep their existing canonical recipes:
#13244, #13607/#13625, #13635, #13672, #13698 and #13704.

## Required gates

- successor source manifest
- actual java.base patch-module compilation and tests
- normal and -Xint fused array/mask view contract
- owning arrays-before-collections Maven/JUnit
- source-built M3 arrays JNI for vendor-free factory test
- existing merged #2045 direct filtered/sorted cursor-mask JNI workflow
- broader installed M3JDK21/jtreg/JCK/HotSpot/GC/platform gates remain separate
