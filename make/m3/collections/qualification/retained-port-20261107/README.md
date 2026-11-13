# m3/collections retained-port-20261107 (record only)

Decision record for the 79 `m3/collections` production paths that retain / ancestry-only superset merges kept out of
master (`S:/m3-scratch/eb-conv/folddrop/retained/m3__collections.tsv`: merges `0890555a47c2 365af9b42716 58deceae31b8
9528ffa49f7c a918e588dd11 b42ab7605b86 da44c2ee7e9a e913ab890566 f4fbe3333946`, PRs #1000 #1084 #1335 #1733 and
unnumbered stacked heads). Decided against master `937732141da8` (2026-11-07).

**Nothing is applied by this leaf.** Absorption into `jdk.m3.collections` was paused on 2026-11-07 while the flat
exported package is curated (public vs `jdk.internal.m3.collections`, subpackages, JDK-style names). This directory
holds only `DECISION.tsv`, `RESTORE.tsv` and this file; no source file is added or changed.

## Files

| file | content |
|------|---------|
| `DECISION.tsv` | one row per retained path (79): decision, donor blob + sha256, planned target path, declared and proposed visibility, compile finding, status |
| `RESTORE.tsv` | parent blob ids a future apply would write: 29 production files (RESTORE_EXACT, one row per basename) and the PRs' own 11 tests found at the same parents |

Every blob id and sha256 was read from the shared blobless clone with `git rev-parse <parent>:<path>` and
`git cat-file blob <id> | sha256sum`; nothing was checked out there.

## Decision counts

| decision | rows | distinct basenames | note |
|----------|-----:|-------------------:|------|
| RESTORE_EXACT | 47 | 29 | compiles after package relocation, no ADAPT edit (18 rows are the same blob reached through a second or third parent) |
| SUPERSEDED | 30 | 7 (+2 rows) | 28 rows: the seven #1084 files master already owns since PR #2741 (merge `60f3387c6dd4`); 2 rows: older `M3MappedViews` variants behind the `689292a5b535` blob |
| HOLD | 2 | 1 | `M3ApiEntryFacades` (#1335 and #1733 rows), M3LFUCache API drift |
| ADAPT / MEMBER_GAP | 0 | 0 | the seven owned files differ from their donors only by FLLDCIM anchors and the M3LongBag call renames `size()->totalCount()`, `distinctSize()->distinctCount()`, `add(k)->add(k, 1L)`: 0 missing members |

Basenames: 37 = 29 + 7 + 1.

### HOLD

`m3/collections/src/main/java/com/m3/collections/M3ApiEntryFacades.java` (blob `d7bb2c9875b8`, parents `c4b5e7098ef1`
#1335 and `1d2cf4335e62` #1733; the master-landing retained-variants copy
`m3/qualification/master-landing-20261028/retained-variants/pr1733/.../M3ApiEntryFacades.java` is byte-identical after
package relocation). It binds `M3LFUCache<V>` keyed by a `long` handle with `peek/get/put(long, V)`,
`invalidate(long)`, `setMaximumEntries`, `compactStorage`, `hits`, `evictions`, `verifyInvariants`. Master's
`M3LFUCache<K, V>` is identity-keyed and offers only `getOrCreate(K, Function)`, `removeIdentity(Object)`, `clear`,
`size`, `capacity`: 8 of the 9 bound members are absent and a boxed `Long` handle cannot key an identity cache.
javac reports `wrong number of type arguments; required 2` (the #2517 note) and then the missing members. An
adaptation would be a new stats-bearing, handle-keyed LFU, not a port, so the file stays HELD together with
`M3ArrayOnlyFacadeSelfTest` (retained-variants pr1733), which binds the same single-type-argument API.

### SUPERSEDED citations

| donor basename | owner on master | landed by |
|----------------|-----------------|-----------|
| M3CountedCatalogue.java (counts/ module) | `m3/collections/src/main/java/jdk/m3/collections/tooling/M3CountedCatalogue.java` | #2741 `60f3387c6dd4` |
| M3GuavaLongMultiset.java | `m3/collections/interop/src/main/java/jdk/m3/collections/interop/M3GuavaLongMultiset.java` | #2741 |
| M3LibraryBags.java | `m3/collections/interop/src/main/java/jdk/m3/collections/interop/M3LibraryBags.java` | #2741 |
| M3CountLanes.java | `m3/collections/src/main/java/jdk/m3/collections/M3CountLanes.java` | #2741 |
| M3CountTotals.java | `m3/collections/src/main/java/jdk/m3/collections/M3CountTotals.java` | #2741 |
| m3_count_lanes.c, m3_count_totals.c | `m3/collections/src/main/native/collections/` | #2741 |
| M3MappedViews.java blobs `908801c80968` (9df1506e9625), `5aca2339b08f` (d5076934bf43) | newer blob `4c2ffbfc358e` at `689292a5b535` (6+/42- and 5+/39- lines behind) | this record |

## Planned targets and visibility under the coming curation

Planned target path = donor path with `com/m3/` -> `jdk/m3/` (jdk_naming rules PKG_COM_M3, PKG_PATH_COM_M3,
JNI_COM_M3; the `counts/` and `wrappers/` donor modules have no pom on master and join the real module tree, as
#2741 did for M3CountedCatalogue). `declared_visibility` is the donor's top-level modifier. `curation_visibility` is a
mechanical proposal for the curation owner, not a decision: `internal` when the type is package-private or its name
ends in `Kernel|Admission|OrderedStore|IndexRange|PendingSortedKeys|Lanes|Totals|Algebra` or starts with `MIndex`;
`public` otherwise. `curation_package` follows (`jdk.internal.m3.collections[.interop]` vs `jdk.m3.collections[.interop]`).

| proposed package | files |
|------------------|------:|
| jdk.m3.collections (public) | 8: M3CollectionWrappers, M3FilteredSortedMultisets, M3IndexNavigableSet, M3IndexSortedMultiset, M3LongBiMap, M3LongCompiledMembership, M3SignalSortedViews, M3WritableFilteredMaps |
| jdk.internal.m3.collections | 12: M3AdmissionWrappers, M3BoundedLists, M3ConcatenatedViews, M3FixedSizeLists, M3MappedViews, M3PendingSortedKeys, M3IndexCountKernel, M3IndexOrderedStore, M3IndexRange (package-private); M3IndexHeapAdmission, M3IntFilterKernel, MIndexSpace (public today) |
| jdk.m3.collections.interop (public) | 4: M3EclipseMultimaps, M3FiveProviderDirectSdk, M3Multiset, M3Multisets |
| jdk.internal.m3.collections.interop | 2: M3ApacheCountIterator (package-private), M3CountAlgebra (public today) |
| native (no package) | 3: m3_count_kernel.c, m3_int_filter_jni.c, m3_collection_counts.c (interop; master has no interop native directory yet) |

Peer note: `M3LongBiMap`, `M3LongCompiledMembership`, `M3IntFilterKernel` (+ `m3_int_filter_jni.c`) carry the
`M3Long*` / `M3Int*` prefixes of the primitive-lane owner family. They have no owner on master and modify none;
the owner of that family should confirm their names before any apply.

## Compile findings (this session)

- javac probe, 26 relocated donors (every RESTORE_EXACT `.java`) compiled in one batch against master `937732141da8`
  `m3/collections` main sources (module-info excluded), `m3-arrays 0.1.0-SNAPSHOT`, `guava 33.4.8-jre`,
  `eclipse-collections 11.1.0`, `spring-core 6.2.0`, `commons-collections4 4.5.0` and Guava's annotation
  dependencies: **0 errors, 0 warnings with `-Xlint:all,-module,-processing -Werror`**, 85 class files. Without Guava's
  transitive annotation jars the only warnings are Guava's own `Cannot find annotation method` (not donor code).
- Prose-only identifiers that resolve to no type on master or at the parents (`M3Index`, `MIndex`, `MIndexFrozenBag`,
  `M3RowSelectionKernel`, `MatIntOrder`) occur only in Javadoc and exception strings: no dependency.
- Live dependencies all have owners on master: `M3ObjectCounts`, `M3LinkedHashMap`, `M3LongBag`, `M3CollectionViews`,
  `M3FilteredMaps`, `M3LongLongHashMap`, `M3LongLongConsumer`, and the interop providers `M3Apache*`, `M3Eclipse*`,
  `M3Guava*`, `M3Spring*`, `M3LibraryCollections`.
- Native: `m3/collections/pom.xml` builds no native code (`-Pm3-collections-native` only loads `m3_arrays`), so the three
  `.c` files are text-relocated only (`Java_com_m3_collections_*` -> `Java_jdk_m3_collections_*`, headers
  `com_m3_collections_*.h` -> `jdk_m3_collections_*.h`), as #2741 did for `m3_count_lanes.c`.
- Baseline (run once before the pause, untouched base `937732141da8`, head repo `S:/m3-scratch/eb-conv/collret/m2`
  with tail `S:/m3-scratch/eb-conv/m2`, `-T1`, tmpdir on `S:`): `m3/collections` BUILD SUCCESS, tests run 2278,
  failures 0, errors 0, skipped 135; `m3/collections/interop` BUILD SUCCESS, tests 26, failures 0. No further
  Maven run was made.
- The HOLD file's error is the API drift described above.

## Tests found at the same parents (RESTORE.tsv kind=test, not compiled)

| parent | test | blob |
|--------|------|------|
| 689292a5b535 | M3FilteredSortedMultisetsTest, M3SignalSortedViewsSelfTest, M3WritableFilteredMapsTest, wrappers/ M3CollectionWrappersSelfTest, M3CollectionWrappersTest | e1518bb1f575, 09e391244cd2, 2ebd6f6ee8b1, fc4a2a8db8a7, 7140481d7ad5 |
| 2709e27b5f8c | interop M3MultisetTest | a90f01cf115f |
| e14c41691025 | interop M3EclipseMultimapsTest, M3FiveProviderDirectSdkTest | 6a693ecf7a7f, 765e2160a30b |
| 76d0894de5a5 | M3LongBiMapSelfTest, M3LongBiMapTest | 76f644fabaff, bfc613672b65 |
| f51a3c4b6f52 | M3LongCompiledMembershipTest | 165aa99d8c36 |

No test exists at the parents for M3ApacheCountIterator (b05e16aa026f) or the #1000 wrapper heads other than the
wrappers/ module tests above; the interop `multiset-recipe` module tests (2709e27b5f8c) belong to the recipe, not
to the collection, and are not listed.

## When the curation lifts

Apply order: write the 29 RESTORE.tsv blobs to their planned (curated) targets, run
`python make/m3/tooling/jdk-naming/jdk_naming.py apply --only m3/collections/src/main/java/jdk/m3/collections` (stage new
files first) until it is a fixed point, port the 11 tests, then the gates of the original brief: real pom
`-Xlint:all -Werror` 0 warnings, failing set equal to the baseline above, donor-vs-restored differential on a seeded
corpus, javap -p for adapted classes (none planned), untouched class files byte-identical, 0 deletions.
