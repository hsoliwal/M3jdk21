<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Guava 33.4.8 array storage (E1), successor of guava-33.4.8-20261018

The guava fork's array-storage work (`m3/array-storage-20261019`, PR #10; `codex/array-operations`, PR #12; fork
master `5a0ba1218ca6`) was made on upstream master. It lands on the received 33.4.8 copy
`implementations/guava-33.4.8` by 3-way merge (ours = 33.4.8, base and theirs = the fork's pre- and postimage). The
fork-first branch `hsoliwal/guava` `m3/guava-33.4.8-array-storage-20261024` (PR #16, commit `24cea316`) holds the
same bytes on the relocated 33.4.8 tree. The 20261018 receipt stays unchanged; `FULL_SOURCE_BINDINGS.tsv` binds the
21 changed or new paths. Every edited row's before-hash equals the 20261018 after-hash.

Resolution: 59 conflict hunks. Small hunks keep the 33.4.8 API (`Serialization.readCount`, `Hashing.closedTableSize`,
explicit casts) with the fork's storage change. HashBiMap is the fork postimage: upstream master rewrote it after
33.4.8, and the fork's array version builds on that rewrite. 33.4.8 behaviour is restored in three places:
- `TreeMultiset.setCount(e, old, new)` on an empty tree returns `false` without calling the comparator;
- the HashBiMap `InverseSerializedForm` stream id is the 33.4.8 value, so the two owners read each other's
  serialized inverses;
- AbstractMultiset hook names follow 33.4.8 (`entryIterator`, `distinctElements`).

The contract test `bimapMatchesOriginal...` rebuilds its 33.4.8 oracle when the oracle hits a known 33.4.8 defect
(entry-iterator `remove()` after `setValue()` dereferences a stale bucket entry). The array owner completes that
edit.

Gates (local, JDK 21.0.9):
- JRE profile (`guava/src` + `futures/failureaccess`, `-source 8 -target 8 -g`, annotation jars only): 618 sources,
  1,972 classes, 0 errors.
- Android: 616 sources, 1,933 classes, 0 errors.
- The four M3 array contract tests against the unmodified 33.4.8 classes: 32/32.
- `TopKArrayContract`: 0 retained discarded-tail references, against 1,067 for 33.4.8.
- `M3GuavaEvictingRelocationProof` compiles.

Not run:
- the donor guava-tests, because the relocated tests need a relocated Truth;
- `TopKArrayBenchmark` (JMH);
- a hash-pinned recipe, because the canonical engine admits only `src/main/java` and `src/test/java` roots.
