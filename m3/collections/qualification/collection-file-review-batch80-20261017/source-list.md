# Exact 80-file Java source inventory

These existing source files were inventoried and compiled with strict Java 21.
The list includes collection owners, interfaces, helpers and two descriptors.
It does not describe 80 new implementations or a complete latest-master tree.
Each path/hash and its unadmitted behavior/interface status appears in the TSV.

| # | Source path |
|---:|---|
| 1 | `m3/collections/src/main/java/com/m3/collections/M3AdaptiveEnumLongMap.java` |
| 2 | `m3/collections/src/main/java/com/m3/collections/M3Address28.java` |
| 3 | `m3/collections/src/main/java/com/m3/collections/M3BitLane28.java` |
| 4 | `m3/collections/src/main/java/com/m3/collections/M3BlockingDeque.java` |
| 5 | `m3/collections/src/main/java/com/m3/collections/M3ChildProgressMonitor.java` |
| 6 | `m3/collections/src/main/java/com/m3/collections/M3Collections.java` |
| 7 | `m3/collections/src/main/java/com/m3/collections/M3ConcurrentBitSet.java` |
| 8 | `m3/collections/src/main/java/com/m3/collections/M3ConcurrentIntLane28.java` |
| 9 | `m3/collections/src/main/java/com/m3/collections/M3ConcurrentLongLane28.java` |
| 10 | `m3/collections/src/main/java/com/m3/collections/M3ConcurrentLongMap.java` |
| 11 | `m3/collections/src/main/java/com/m3/collections/M3ConcurrentLongSet.java` |
| 12 | `m3/collections/src/main/java/com/m3/collections/M3DefaultProgressMonitor.java` |
| 13 | `m3/collections/src/main/java/com/m3/collections/M3EnumLongMap.java` |
| 14 | `m3/collections/src/main/java/com/m3/collections/M3EnumSet.java` |
| 15 | `m3/collections/src/main/java/com/m3/collections/M3ForwardingLongList.java` |
| 16 | `m3/collections/src/main/java/com/m3/collections/M3ForwardingLongQueue.java` |
| 17 | `m3/collections/src/main/java/com/m3/collections/M3IntCollection.java` |
| 18 | `m3/collections/src/main/java/com/m3/collections/M3IntHashSet.java` |
| 19 | `m3/collections/src/main/java/com/m3/collections/M3IntIntConsumer.java` |
| 20 | `m3/collections/src/main/java/com/m3/collections/M3IntIntHashMap.java` |
| 21 | `m3/collections/src/main/java/com/m3/collections/M3IntIterator.java` |
| 22 | `m3/collections/src/main/java/com/m3/collections/M3IntLane28.java` |
| 23 | `m3/collections/src/main/java/com/m3/collections/M3IntLongConsumer.java` |
| 24 | `m3/collections/src/main/java/com/m3/collections/M3IntLongHashMap.java` |
| 25 | `m3/collections/src/main/java/com/m3/collections/M3IntPriorityQueue.java` |
| 26 | `m3/collections/src/main/java/com/m3/collections/M3LazyEventBits.java` |
| 27 | `m3/collections/src/main/java/com/m3/collections/M3LazyEventGranularity.java` |
| 28 | `m3/collections/src/main/java/com/m3/collections/M3LazyEventPolicy.java` |
| 29 | `m3/collections/src/main/java/com/m3/collections/M3LazyEventSink.java` |
| 30 | `m3/collections/src/main/java/com/m3/collections/M3LazyEventSource.java` |
| 31 | `m3/collections/src/main/java/com/m3/collections/M3LazyEvents.java` |
| 32 | `m3/collections/src/main/java/com/m3/collections/M3LazyOperation.java` |
| 33 | `m3/collections/src/main/java/com/m3/collections/M3LazyValueProvider.java` |
| 34 | `m3/collections/src/main/java/com/m3/collections/M3LazyValueState.java` |
| 35 | `m3/collections/src/main/java/com/m3/collections/M3LinkedHashMap.java` |
| 36 | `m3/collections/src/main/java/com/m3/collections/M3LinkedHashSet.java` |
| 37 | `m3/collections/src/main/java/com/m3/collections/M3LinkedList.java` |
| 38 | `m3/collections/src/main/java/com/m3/collections/M3LockedMap.java` |
| 39 | `m3/collections/src/main/java/com/m3/collections/M3LockedTreeMap.java` |
| 40 | `m3/collections/src/main/java/com/m3/collections/M3LockedTreeSet.java` |
| 41 | `m3/collections/src/main/java/com/m3/collections/M3LongArena28.java` |
| 42 | `m3/collections/src/main/java/com/m3/collections/M3LongArrayDeque.java` |
| 43 | `m3/collections/src/main/java/com/m3/collections/M3LongArrayList.java` |
| 44 | `m3/collections/src/main/java/com/m3/collections/M3LongCollection.java` |
| 45 | `m3/collections/src/main/java/com/m3/collections/M3LongDeque.java` |
| 46 | `m3/collections/src/main/java/com/m3/collections/M3LongHashSet.java` |
| 47 | `m3/collections/src/main/java/com/m3/collections/M3LongIterator.java` |
| 48 | `m3/collections/src/main/java/com/m3/collections/M3LongLane28.java` |
| 49 | `m3/collections/src/main/java/com/m3/collections/M3LongLaneStore28.java` |
| 50 | `m3/collections/src/main/java/com/m3/collections/M3LongLinkedHashSet.java` |
| 51 | `m3/collections/src/main/java/com/m3/collections/M3LongLinkedList28.java` |
| 52 | `m3/collections/src/main/java/com/m3/collections/M3LongList.java` |
| 53 | `m3/collections/src/main/java/com/m3/collections/M3LongListSpliterator.java` |
| 54 | `m3/collections/src/main/java/com/m3/collections/M3LongLongConsumer.java` |
| 55 | `m3/collections/src/main/java/com/m3/collections/M3LongLongHashMap.java` |
| 56 | `m3/collections/src/main/java/com/m3/collections/M3LongPriorityQueue.java` |
| 57 | `m3/collections/src/main/java/com/m3/collections/M3LongQueue.java` |
| 58 | `m3/collections/src/main/java/com/m3/collections/M3LongSequence.java` |
| 59 | `m3/collections/src/main/java/com/m3/collections/M3LongStack.java` |
| 60 | `m3/collections/src/main/java/com/m3/collections/M3ObjectIdentityMap.java` |
| 61 | `m3/collections/src/main/java/com/m3/collections/M3ObjectIdentitySet.java` |
| 62 | `m3/collections/src/main/java/com/m3/collections/M3OrderedSlotMap.java` |
| 63 | `m3/collections/src/main/java/com/m3/collections/M3Progress.java` |
| 64 | `m3/collections/src/main/java/com/m3/collections/M3ProgressAdapters.java` |
| 65 | `m3/collections/src/main/java/com/m3/collections/M3ProgressMonitor.java` |
| 66 | `m3/collections/src/main/java/com/m3/collections/M3ProgressScope.java` |
| 67 | `m3/collections/src/main/java/com/m3/collections/M3ProgressState.java` |
| 68 | `m3/collections/src/main/java/com/m3/collections/M3ReadOnlyLongList.java` |
| 69 | `m3/collections/src/main/java/com/m3/collections/M3ReadOnlyLongQueue.java` |
| 70 | `m3/collections/src/main/java/com/m3/collections/M3SlotAllocator28.java` |
| 71 | `m3/collections/src/main/java/com/m3/collections/M3SlotArrays.java` |
| 72 | `m3/collections/src/main/java/com/m3/collections/M3SlotMap.java` |
| 73 | `m3/collections/src/main/java/com/m3/collections/M3SlotSpliterators.java` |
| 74 | `m3/collections/src/main/java/com/m3/collections/M3SystemProgress.java` |
| 75 | `m3/collections/src/main/java/com/m3/collections/M3TransferQueue.java` |
| 76 | `m3/collections/src/main/java/com/m3/collections/M3TreeMap.java` |
| 77 | `m3/collections/src/main/java/com/m3/collections/M3TreeSet.java` |
| 78 | `m3/collections/src/main/java/com/m3/collections/M3WeakHashMap.java` |
| 79 | `m3/collections/src/main/java/com/m3/collections/package-info.java` |
| 80 | `m3/collections/src/main/java/module-info.java` |
