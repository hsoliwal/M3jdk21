<!--
/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
-->
# Eclipse Collections source in the M3 namespace

[The implementation directory](../../implementations/eclipse-collections-14.0.0-snapshot-00a668d6/) holds all 3,480 files of
tree `97cd9b660586845b0015e4882d28af6834ed5527` of `hsoliwal/eclipse-collections`, branch
`m3/eclipse-collections-receive-prep-20261024`, commit `d7e0b3970e611dd6be1aeca56fe6d94cf8d34fa7`. The files were written byte
for byte from `git cat-file`, with no transformation, and staged as the exact blobs. The directory's tree id equals the fork tree id.

That commit is fork master `acefe9c622274d9b43db97291966cfcfbcf8bc88` plus one header-only commit. Fork master is upstream
`eclipse-collections/eclipse-collections` master `00a668d624c870652bf746d10f250a4d37a777f8` (2026-10-17, version
14.0.0-SNAPSHOT), relocated from `org.eclipse.collections` to `com.m3.collections.eclipse`, with the merged fork M3 work.

`FULL_SOURCE_BINDINGS.tsv` has one row for each received path: the upstream path at `00a668d6`, mode, Git blob, raw SHA-256
and byte count before and after. The 120 first-party fork files have `-` in the before columns. All 3,360 upstream paths are
received: 2,805 relocated, 391 edited in place and 164 unchanged. The 391 edited files are 373 generator templates, 12 Scala
files whose packages moved but whose paths did not, 2 `bnd.bnd`, 2 `pom.xml` and the 2 Checkstyle configuration files.

## Naming exception

The directory name breaks naming rule R2, which allows release tags only. The owner approved this exception on 2026-10-24
([NAMING.md](../../NAMING.md), `NAMESPACE_EXCEPTION.json`). No release contains the fork's M3 work, and fork master compiles
with `--release 21` and `--release 17`. The name carries the snapshot version and the upstream commit. The first-party
classes keep their names: the M3 prefix (naming item C3 #9) is undecided and is not applied here.

## Fork-first prep

The prep commit changes 26 files and nothing else. Their bytes from the package line to the end of the file are unchanged.

- The canonical Synexia header (Apache-2.0 OR EPL-2.0) replaces the first-party licence lines of the 21 first-party `.java`
  files that the fork added. Provenance notes are kept, for example the Apache Commons Collections TreeBidiMap note in
  `IndexedRedBlackTree.java`.
- The 5 upstream files that the fork edited keep their Goldman Sachs EPL-1.0/EDL-1.0 header and get one
  `// Modified 2026 by Hitesh Soliwal and Synexia contributors: <what>.` line: `ArrayAdapter`, `ImmutableBooleanArrayList`,
  `FastList`, `TreeSortedMap` and `immutablePrimitiveArrayList.stg`. In the `.stg` the line is a group-file comment, so the
  generated sources do not change.

## Checks

The checks used JDK 21, offline Maven from `~/.m2`, and one JVM at a time with `-Xmx2g`.

- Generated sources: the fork's code generator ran offline on the prep tree. It produced 1,354 api and 1,478 impl `.java`
  files and 2,832 `.crc` files. The sorted manifest is identical to the one generated from fork master. Generated sources are
  not committed.
- javac on the prep tree, with `-proc:none -encoding UTF-8 -Xlint:all,-options`. The api and impl modules include their
  generated sources (1,607 and 2,147 files). With `--release 21` and with `--release 17`: 0 errors and 0
  `inherits abstract and default` diagnostics in api, impl, forkjoin and testutils. The same compile of the received
  directory with `--release 21` also has 0 errors.
- Unit tests on the received directory: `ComparatorPutAllCallbackOrderTest`, `IndexedSortedMapLifecycleTest`,
  `IndexedSortedMapViewProtocolTest` and `M3FastListArraySpanTest` ran 10 tests and all 10 passed.
  `M3FastListArraySpanTest` drives the `M3FastListArraySpanProof` harness against the frozen original `FastList`.
- UnifiedMap suite (`UnifiedMapTest`, unfiltered): 287 run, 287 passed, 0 failed. PR #1302 recorded 286 run, 285 passed and 1
  known failure on the 11.1 tree. Two changes upstream between 11.1 and this tree explain the difference:
  - The suite's test methods changed: `getIfAbsentPut_block_throws` and `getIfAbsentPutWith_block_throws` were removed, and
    `merge`, `mergeNullValue` and `mergeNullValueInMapWithCollisions` were added.
  - Upstream `ClassComparer` now ignores static methods. The JDK 19+ static `HashMap.newHashMap(int)` therefore no longer
    breaks `unifiedMapProperSuperSetOfHashMap`.
- Factory smoke: the API factories for Lists, Sets, Maps, Bags and Stacks, mutable and immutable, each resolve to an
  implementation class and build the expected collection. The tree has no `META-INF/services` files, so the factories resolve
  through the reflection fallback of `ServiceLoaderUtils`. The fork's `M3EclipseFactoryNamespaceProbe` passes all 255 factory
  mappings: 239 instantiated, and 16 pairs absent upstream as well.

## Indexed red-black custody packet

This receive supersedes `receiving/eclipse-indexed-rb-20261019`, which is kept unchanged. The plan names 12 of its source
files as in scope. One is byte-equal and 11 differ only by the fork-first header lines:

| File under `source/` | Status |
|---|---|
| `eclipse-collections/src/main/java/com/m3/collections/eclipse/impl/map/sorted/mutable/IndexedRedBlackMap.java` | header lines only |
| `eclipse-collections/src/main/java/com/m3/collections/eclipse/impl/map/sorted/mutable/IndexedRedBlackTree.java` | header lines only |
| `eclipse-collections/src/main/java/com/m3/collections/eclipse/impl/map/sorted/mutable/IndexedSortedMap.java` | header lines only |
| `eclipse-collections/src/main/java/com/m3/collections/eclipse/impl/map/sorted/mutable/IndexedTreeArena.java` | header lines only |
| `eclipse-collections/src/main/java/com/m3/collections/eclipse/impl/map/sorted/mutable/TreeSortedMap.java` | contributor line only |
| `m3/indexed-rb/proofs/IndexedRedBlackMapProof.java` | header lines only |
| `m3/indexed-rb/proofs/IndexedRedBlackTreeProof.java` | header lines only |
| `m3/indexed-rb/proofs/IndexedTreeArenaTest.java` | header lines only |
| `unit-tests/src/test/java/com/m3/collections/eclipse/impl/map/sorted/mutable/ComparatorPutAllCallbackOrderTest.java` | header lines only |
| `unit-tests/src/test/java/com/m3/collections/eclipse/impl/map/sorted/mutable/IndexedSortedMapLifecycleTest.java` | header lines only |
| `unit-tests/src/test/java/com/m3/collections/eclipse/impl/map/sorted/mutable/IndexedSortedMapViewProtocolTest.java` | header lines only |
| `unit-tests/src/test/resources/ComparatorPutAllCallbackOrderTest.expected.properties` | byte-equal |

The other 11 files of that packet, the `m3/indexed-rb` metadata, are also byte-equal to their received copies.

## Open

The source keeps the donor's algorithms and sits outside the M3 reactor. It does not advance JDK receiving. The 19 POMs
keep the donor coordinates `org.eclipse.collections:*:14.0.0-SNAPSHOT` as provenance. Default install/deploy skips come in
the build-quarantine successor leaf A2. `qualification/five-provider-inventory-20261018/PROVIDERS.tsv` still lists Eclipse
Collections (rows EA and EI) as `PATTERN_ONLY`. The owner has decided to switch those rows to received source, in a
follow-up leaf after this one.

Full Maven, OSGi and JPMS builds, the full donor tests, array/index/offset/mask storage conversion and JNI remain open. See
`TARGET_DISPOSITION.json` and `SOURCE_QUALIFICATION.json`.
