<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3File / M3FileTree — DESIGN

Status: DRAFT-2 | Module: `m3/file` (`com.m3.file`) | Branch: `m3/m3file-tree-20261020` | Date: 2026-10-20

## 0. Names (locked)

| Symbol | Role | Never |
|--------|------|-------|
| `M3File` | the entry type: one thin facade per tree row `(tree, id)`; also keeps the existing detached `M3File.of(Path)` read edge | `MFile`, `M3Path`, `FileNode`, `M3FileEntry` |
| `M3FileTree` | owner of the lanes for one rooted directory tree; `M3FileTree.open(Path root)` is the only way to get a populated tree | `M3FileSystem`, `FileLanes` |
| `M3FileFlag` | bit lanes (one `BitSet` per flag) | `FileState` |
| `M3FileTreeScanner` | `Files.walkFileTree` visitor that fills the lanes in pre-order | `Crawler` |
| `M3FileResidency` | bounded table of live `M3File` facades owned by the tree | `FileCache` |
| `M3FileDelta` (slice 3) | immutable change record applied to lanes | `FileEvent` |
| `M3FileView` (slice 4) | filtered/sorted int-index wrapper over a tree | `FilteredTree` |

## 1. Inventory (IFB) — what exists on `origin/master` at `85bf188c14d`

| Owner | Path | Use here |
|-------|------|----------|
| `com.m3.file.M3File` | `m3/file/src/main/java/com/m3/file/M3File.java` | detached Path read edge (`of`, `path`, `metadata`, `openReadChannel`, `openInputStream`, `readAllBytes`, `Metadata`). Kept verbatim; the class gains the tree-entry role. |
| `module com.m3.file` | `m3/file/src/main/java/module-info.java` | unchanged (exports `com.m3.file`) |
| `m3/eclipse/optional-jmod/build.py:28` | compiles `m3/file/src/main/java` into the optional JMOD | new sources must stay JDK-only, no deps |
| `com.m3.fs.M3DirectoryTable` | `m3/fs/src/main/java/com/m3/fs/M3DirectoryTable.java` | pattern donor: names as one `char[]` pool + `int[] offsets`, stats as flat lanes; entries exist only at API access |
| `org.m3.runtime.lazy.LazyObjectParent` | `m3/lazyobjects/.../LazyObjectParent.java` | pattern donor: parent-owned transient handles, eviction = recreate; here reduced to an int-keyed bounded table |
| `com.m3.viewport.M3RowHost` | `m3/viewport-core/.../M3RowHost.java` | slice 5 target: rows are logical ints, work per frame O(window) |
| `com.m3.arrays` pom | `m3/arrays/pom.xml` | build template: junit-jupiter 5.10.2, surefire `useModulePath=false`, `-Xlint:all,-module -Werror` |

Not reused: `m3/fs` native/JNI (Java-only law), any interner (names live in the pool; interning deferred).

## 2. Laws applied

- DOOP: data in lanes (`int[]`, `long[]`, `short[]`, `char[]`, `BitSet`); `M3File` holds `tree`, `id` and a lazily memoised `Path`.
- Object residency: facades are transient, parent-owned by `M3FileTree`, created on `file(id)`, bounded by `M3FileResidency`; eviction = drop, recreation = free; identity is `(tree, id)`, never the facade instance.
- One model + deltas: one lane model for scanner, views, viewport binding; later changes arrive as `M3FileDelta` in id order; no per-consumer maps.
- Bits/ints: flags are bitsets, names are pool ranges, children are CSR ranges; `String`/`Path` only when a caller asks (cold).
- HP-LAW: `open`, `childIds`, `parent`, `depth`, flag probes are allocation-free; `name()`, `toPath()` are cold.
- Java only, JDK only: scan = `Files.walkFileTree`, no native walker, no third-party dependency.

## 3. Lanes (`M3FileTree`)

Ids are dense, allocated in pre-order; `0` = root; `size()` = next free id. Capacity doubles.

| Lane | Type | Meaning |
|------|------|---------|
| `parent` | `int[]` | parent id; `-1` for root |
| `depth` | `short[]` | root `0` |
| `nameOff` | `int[size+1]` | name of `id` = `namePool[nameOff[id] .. nameOff[id+1])` (UTF-16 units) |
| `namePool` | `char[]` | all names, appended in id order |
| `childStart` | `int[]` | start of the CSR slice in `childIds` |
| `childCount` | `int[]` | direct children; `0` until scanned |
| `childIds` | `int[]` | CSR payload, one entry per non-root id |
| `size` | `long[]` | byte length; `-1` unknown |
| `mtime` | `long[]` | last-modified epoch millis; `Long.MIN_VALUE` unknown |

Flags (`M3FileFlag`, one `BitSet` each): `DIR`, `LINK`, `HIDDEN`, `SCANNED`, `RESIDENT`, `DIRTY`, `DELETED`.

Invariants (asserted by the slice-1 test):
1. `parent[id] < id` for all `id > 0`.
2. `depth[id] == depth[parent[id]] + 1`.
3. Every CSR slice is sorted by name (UTF-16 unit order, then length) — deterministic, locale-free.
4. `childIds` contains every id except `0` exactly once.
5. `DELETED` rows keep their lanes and are skipped by `ids()`.

## 4. `M3File`

```java
public final class M3File {
    private final M3FileTree tree;   // M3FileTree.detached() for of(Path)
    private final int id;            // -1 when detached
    private Path path;               // of(Path): eager; tree entry: memoised on first toPath()/path()

    // existing edge (unchanged behaviour): of(Path), path(), metadata(), openReadChannel(),
    // openInputStream(), readAllBytes(int), record Metadata
    // tree entry: id(), isTreeEntry(), tree(), name(), parent() : Optional<M3File>,
    // childIds() : IntStream, children() : Stream<M3File>, isDirectory(), isSymbolicLink(),
    // isHidden(), size(), lastModified(), depth(), toPath(), equals/hashCode on (tree, id | path)
}
```

Lane accessors on a detached file throw `IllegalStateException`; the Path edge works on both kinds (`path()` of an entry = `root.resolve(names joined)`).

## 5. `M3FileTree`

```java
public static M3FileTree open(Path root) throws IOException;   // slice 1: eager, NOFOLLOW_LINKS, unbounded depth
public static M3FileTree detached();                            // shared empty tree for M3File.of(Path)
public Path root(); public M3File rootFile(); public M3File file(int id);
public int size(); public int liveCount(); public IntStream ids();
public int parent(int id); public int depth(int id); public int childCount(int id); public IntStream childIds(int id);
public String name(int id); public long size(int id); public long mtime(int id);
public boolean flag(M3FileFlag f, int id); public int compareNames(int a, int b); public Path path(int id);
```

Scanner: `preVisitDirectory` appends the row and opens a frame; `visitFile`/`visitFileFailed` append rows (failed = unknown size/mtime, no flags); `postVisitDirectory` sorts the frame's ids in place (heap sort over `compareNames`, no allocation) and writes the CSR slice, then sets `SCANNED`. Hidden = dot-name or `DosFileAttributes.isHidden()` from the attributes the walk already read (no second stat).

Residency: `M3FileResidency` is a direct-mapped table (`capacity` power of two, default 4096): `slot = id & mask`; a request overwrites the slot's previous occupant, `RESIDENT` mirrors the table. Replacement policy upgrades (LRU/clock) are a slice-2 option only if a measured consumer needs them.

## 6. Slices (one commit + test each)

| Slice | Leaf | Done when |
|-------|------|-----------|
| 1 | `M3FileFlag`, `M3FileTree.open`, `M3FileTreeScanner`, `M3FileResidency`, `M3File` entry role, pom test wiring, `M3FileTreeTest` | `mvn -f m3/file/pom.xml test` green on JDK 21; invariants 1-5 asserted |
| 2 | `M3FileScanSpec` (maxDepth, lazy dirs), `scanChildren(id)`, `SCANNED` semantics, optional `subtreeSize` lane | lazy expansion test; scanned dirs never re-scanned |
| 3 | `M3FileDelta` ADD/REMOVE/UPDATE, `apply`, `DIRTY`/`DELETED`, `compact()`, `M3FileWatcher` (WatchService) | replay test: scan == scan + deltas |
| 4 | `M3FileView` filtered/sorted int-index wrapper; name/size/mtime orders; delta re-sort for the visible window only | view test; stable; no full rebuild on delta |
| 5 | `M3FileRowHost` binding of tree rows to `com.m3.viewport.M3RowHost` | 100k-entry tree, O(window) per frame |
| 6 | `M3FileImage` little-endian lane snapshot (names pool + lanes), content-addressed | round-trip equal; open from image without re-scan |

Out of scope: content I/O beyond the existing edge, permissions/ACLs, following links, native watchers, interning service.

## 7. Slice-1 test plan

Fixture: temp dir, 3 levels, 12 files, 1 dot-file, 1 empty dir. Assert `liveCount`, root depth 0, invariants 1-5, `toPath()` equals the real path for every id, `isDirectory` matches `Files.isDirectory`, hidden flag, `file(id)` identity while resident, `equals` across eviction, residency capacity 2 -> `RESIDENT` cardinality 2 after three distinct requests, detached `of(Path)` edge still reads bytes.
