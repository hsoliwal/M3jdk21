<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3File / M3FileTree — DESIGN

Status: DRAFT-3 | Module: `m3/file` (`com.m3.file`) | Branch: `m3/m3file-tree-20261020` | PR #1590 | Date: 2026-10-20

## 0. Names (locked)

Entry type is `M3File` everywhere. Small interfaces carry behaviour in default methods; implementations supply one or two abstract methods.

| Symbol | Kind | Role | Never |
|--------|------|------|-------|
| `M3File` | class | the entry type: thin facade per tree row `(tree, id)`; also the detached `M3File.of(Path)` edge | `MFile`, `M3Path`, `FileNode`, `M3FileEntry` |
| `M3FileTree` | class | owner of the lanes of one rooted tree; `M3FileTree.open(...)` is the only way to get a populated tree | `M3FileSystem`, `FileLanes` |
| `M3FileFlag` | enum | bit lanes, one `BitSet` per constant | `FileState` |
| `M3FileKind` | enum | kind mask bits FILE, DIRECTORY, SYMLINK, OTHER | `FileType` |
| `M3FileTreeScanner` | class | `Files.walkFileTree` visitor filling lanes in pre-order | `Crawler` |
| `M3FileResidency` | class | bounded table of live facades owned by the tree | `FileCache` |
| `M3FileTraversal` | interface | abstract `tree()` + `idSpliterator()`; defaults `stream`, `parallelStream`, `spliterator`, `ids`, `map`, `forEach`, `collect`, `toList`, `count`, `paths`, `first` | `FileStream` |
| `M3FileWalk` | class | immutable walk spec over a subtree: depth limits, kind mask, type ids, glob, regex, prune, filter; implements `M3FileTraversal` | `FileQuery` |
| `M3FileCursor` | class | pull cursor: `nextId()`, `nextEntry()`, `skipSubtree()` | `FileIterator` |
| `M3FileProgress` | interface | sub-progress monitor (units, split, cancel); Eclipse `SubMonitor` shape, no Eclipse dependency | `ProgressListener` |
| `M3FileShadow` | interface | observer that advances while a main stream is read or written; abstract `update(byte[],int,int)` | `TeeStream` |
| `M3FileCharShadow` | interface | char-level layer fed by a UTF-8 decoding shadow | `Decoder` |
| `M3FileShadowInputStream` / `M3FileShadowOutputStream` | class | main stream + progress + shadows; one I/O pass feeds every layer | `HashingStream` |
| `M3FileNames` | class | per-tree name/segment interner: name id -> pool range (TIL, int ids) | `String.intern` |
| `tree.types()` | `M3FileNames` | second interner: lower-case extension type ids (`txt`, `json`, `bin`; id 0 = none or directory); no separate class (S4 decision) | `MimeMap` |
| `M3FileChangeProvider` | interface | change detection; defaults = attribute gate then hash; native providers override | `Watcher` |
| `M3FileTreeDelta` | class | two scans -> change lanes + `Stream<M3FileChange>` | `FileDiff` |
| `M3FileChange` | class | facade over one delta row: before/after `M3File`, change bits | `FileEvent` |

## 1. Inventory (IFB)

| Owner | Path / ref | Use here |
|-------|-----------|----------|
| `com.m3.file.M3File` | `m3/file/src/main/java/com/m3/file/M3File.java` @ `85bf188c14d` | detached read edge kept verbatim; gains tree-entry role |
| `module com.m3.file` | `m3/file/src/main/java/module-info.java` | unchanged; JDK-only (compiled into `m3/eclipse/optional-jmod/build.py:28`) |
| `com.m3.fs.M3DirectoryTable` | `m3/fs/src/main/java/com/m3/fs/` | pattern: `char[]` name pool + offsets |
| `org.m3.runtime.lazy.LazyObjectParent` | `m3/lazyobjects/` | pattern: parent-owned transient handles |
| `com.m3.viewport.M3RowHost` | `m3/viewport-core/` | row-host contract for the viewport slice |
| `java.nio.file.Files.walk` / `walkFileTree` / `PathMatcher` | JDK | scan, glob semantics, differential oracle |
| `java.security.DigestInputStream`, `java.util.zip.CheckedInputStream`, `CRC32C` | JDK | shadow-stream pattern; SHA-256 and fast hash (CRC32C intrinsic) |
| `com.synexia.common.utils.TokenInterner` | Synexia `synexia-common/.../TokenInterner.java` blob `5a4163518ab7` | TIL pattern (bounded, stats); adapted to int ids over a char pool |
| Eclipse `SubMonitor` | EPL-2.0 Eclipse Platform | shape only (split/worked/cancel); no code, no dependency |
| PR #1567 `codex/m3-file-content-delta-20261020-d6d76b56` | `M3File.fingerprint(long)`, `Fingerprint`, `ADDED=1`, `REMOVED=2`, `CONTENT_CHANGED=4`, `contentDelta` | reused vocabulary and record for raw-byte hash; **conflict**: it reads the `path` field, which is lazy for tree entries -> must use `path()` |
| PR #1568 `m3/native-source-class-delta-ledger-20261020` | `M3FileDeltaLedger` (sealed source/class build-fact snapshots) | downstream consumer; the tree delta hands it changed paths; no second ledger |

## 2. Laws applied

- DOOP: data in lanes (`int[]`, `long[]`, `short[]`, `char[]`, `BitSet`); `M3File` holds `tree`, `id`, a memoised `Path`.
- Never materialise the file system: only lanes, type ids, kind bits and traversal order; `M3File`, `String`, `Path` on demand.
- Object residency: facades transient, parent-owned, bounded, recreate-on-evict; identity `(tree, id)`.
- One model + deltas: scanner, walks, views, viewport, change detection share one lane model; changes travel as delta lanes.
- Default methods first: interfaces expose one or two abstract methods; everything derivable is a default.
- HP-LAW: id-level walk, flags, depth, names compare, shadows' `update` are allocation-free per entry/byte block.
- Java only, JDK only.

## 3. Lanes (`M3FileTree`)

Ids dense, pre-order (scan order = `Files.walk` order); `0` = root; `rows()` = next free id.

| Lane | Type | Meaning | Slice |
|------|------|---------|-------|
| `parent` | `int[]` | parent id; `-1` root | S1 |
| `depth` | `short[]` | root `0` | S1 |
| `nameOff` / `namePool` | `int[rows+1]` / `char[]` | per-row name chars; removed in S4 (now `nameId` + `M3FileNames` pool) | S1 |
| `childStart` / `childCount` / `childIds` | `int[]` | CSR, slices sorted by name | S1 |
| `subtreeEnd` | `int[]` | exclusive end position of the pre-order subtree `[rank(id), subtreeEnd)` | S2 |
| `order` / `rank` | `int[]` | position -> id and id -> position; identity for eager scans, spliced by lazy expansion (ids stay stable) | S8 |
| `size` / `mtime` | `long[]` | bytes; epoch millis | S1 |
| `nameId` | `int[]` | interned name id (`M3FileNames`) | S4 |
| `typeId` | `int[]` | interned extension type id (`tree.types()`) | S4 |
| `fileKey` | `int[]` | interned `BasicFileAttributes.fileKey()` id, `0` none | S5 |
| `crc` | `long[]` | CRC32C of raw bytes (fast hash) | S5 |
| `sha` | `long[4*rows]` | SHA-256 words of raw bytes, big-endian (#1567 `Fingerprint` order) | S5 |
| `tokenHash` | `long[]` | whitespace-insensitive token-stream hash | S5 |
| `structHash` | `long[]` | structured-view hash (M3AST etc.) supplied by a shadow | S5 |

Flags: `DIR`, `REGULAR`, `LINK`, `HIDDEN`, `SCANNED`, `RESIDENT`, `DIRTY`, `DELETED` (+ `HASHED`, `TOKEN_HASHED`, `STRUCT_HASHED` in S5).

Invariants: (1) `parent[id] < id`; (2) `depth[id] == depth[parent[id]] + 1`; (3) CSR slices sorted by name; (4) `childIds` holds every non-root id once; (5) `DELETED` rows keep lanes, skipped by iteration; (6) `subtreeEnd[id] > rank[id]` and the subtree of `id` is exactly the positions `[rank[id], subtreeEnd[id])`; (7) `rank[parent[id]] < rank[id]`, `order` and `rank` are inverse.

## 4. `M3File`

Detached edge (unchanged): `of(Path)`, `path()`, `metadata()`, `openReadChannel()`, `openInputStream()`, `readAllBytes(int)`, `Metadata`.
Tree entry: `id()`, `isTreeEntry()`, `tree()`, `name()`, `parent()`, `depth()`, `childCount()`, `childIds()`, `children()`, `isDirectory()`, `isRegularFile()`, `isSymbolicLink()`, `isHidden()`, `flag()`, `kind()`, `size()`, `lastModified()`, `toPath()`, `walk()`.
Streams (S3): `openInputStream(M3FileProgress, M3FileShadow...)`, `openOutputStream(M3FileProgress, M3FileShadow...)`; byte limits as `readAllBytes`.

## 5. Traversal (S2)

`tree.walk()` / `tree.walk(id)` / `file.walk()` -> `M3FileWalk` (immutable; each `with` returns a copy):
`minDepth(int)`, `maxDepth(int)`, `kinds(M3FileKind...)`, `glob(String)` (JDK `PathMatcher` over the path relative to the walk root), `regex(String|Pattern)` (relative path string, platform separator), `nameRegex(...)` (name only, no allocation), `prune(Predicate<M3File>)` / `pruneIds(IntPredicate)` (do not descend; the entry itself still meets the filters, `find -prune` semantics), `filter(Predicate<M3File>)` / `filterIds(IntPredicate)`.
Order: pre-order scan order, identical to `Files.walk` over the same directory state. Lane tests run before facade tests.
Spliterator: `ORDERED | DISTINCT | NONNULL`, plus `SIZED | SUBSIZED` when the walk is unfiltered and the tree has no tombstones. `trySplit` halves the id range and moves the split point past the highest closed (pruned or depth-capped) ancestor using `subtreeEnd`, so halves never overlap or enter closed subtrees.
Cursor: `nextId()` (`-1` at end), `nextEntry()` (`Optional<M3File>`), `hasNext()`, `skipSubtree()` (dynamic prune of the last returned entry).

## 6. Design directions (user, 2026-10-20) and consequences

| # | Direction (user) | Consequence | Slice |
|---|------------------|-------------|-------|
| D1 | "default methods are under-used" | small interfaces (`M3FileTraversal`, `M3FileProgress`, `M3FileShadow`, `M3FileCharShadow`, `M3FileChangeProvider`) with one or two abstract methods; behaviour in defaults | S2+ |
| D2 | "both input and output streams should receive a sub-progress monitor" | every M3File input/output stream takes an optional `M3FileProgress` (bytes as units, `split`, cancellation -> `InterruptedIOException`) | S3 |
| D3 | "shadow streams ... move as and when the main stream is read; brilliant for layered parsing; layered traversal" | shadows observe one main stream, no second I/O: SHA-256, CRC32C, byte count, line-offset index, UTF-8 char layer -> token shadow; M3AST shadow plugs in from `m3/ast` through `M3FileCharShadow` (no dependency here); `skip` reads through so every layer sees every byte | S3 |
| D4 | "use attributes like change timestamp and length to detect change; providers that have a change-attribute mechanism are a different story" | cheapest first: size + mtime + fileKey gate, hash only when the gate trips; `M3FileChangeProvider` defaults implement the gate; WatchService, zip central-directory CRC/size, git index stat data override `knownContentHash` / `changed` | S5, S7 |
| D5 | "record not only updates and hashes, but other hashes of its structured file (like M3AST)"; "M3File is small due to interning" | hash lanes raw (`sha`, `crc`) and structured (`tokenHash`, `structHash`); names and segments interned to int ids (TIL), type ids by extension | S4, S5 |
| D6 | "can be used to generate deltas so downstream can consume the delta" | `M3FileTreeDelta`: two trees -> merge-join of sorted CSR slices (no hash maps) -> change lanes; `Stream<M3FileChange>` with bits ADDED/REMOVED/CONTENT_CHANGED (#1567 values) + SIZE/MTIME/KIND/TYPE/TOKEN/STRUCT; changed paths feed #1568 `M3FileDeltaLedger` | S6 |
| D7 | "we don't need the whole file system at all, apart from the file type (.txt, .json, .bin), the order of traversal" | no per-entry objects or path strings retained; only type ids, kind bits, pre-order ids, lanes | all |

## 7. Slices (one commit + push + STATUS row each)

| Slice | Leaf | Done when |
|-------|------|-----------|
| S1 | lanes, scanner, residency, `M3File` entry role | done `ed2036b9f92`, 6/6 tests |
| S2 | `M3FileTraversal`, `M3FileWalk`, `M3FileCursor`, `M3FileKind`, id/facade spliterators, `subtreeEnd` lane, `REGULAR` flag | differential vs `Files.walk`, `walkFileTree` SKIP_SUBTREE, `PathMatcher`, `Pattern`; parallel == sequential |
| S3 | `M3FileProgress`, `M3FileShadow`, `M3FileCharShadow`, shadow input/output streams, SHA-256/CRC32C/count/line-index/UTF-8/token shadows | shadows equal `MessageDigest`/`CRC32C`/line scan of the same bytes in one pass; progress units == bytes; cancel aborts |
| S4 | `M3FileNames` interner, `M3FileTypes`, `nameId`/`typeId` lanes, `types(...)` walk filter | repeated names stored once; type ids stable; S1/S2 tests unchanged |
| S5 | change gate + hash lanes, `M3FileChangeProvider`; rebase onto #1567 `Fingerprint` once merged | gate skips reads for unchanged attributes; hash lanes equal #1567 fingerprint |
| S6 | `M3FileTreeDelta`, `M3FileChange` | delta of scan/edit/rescan equals expected ADDED/REMOVED/CHANGED; paths feed #1568 ledger |
| S7 | `M3FileSource` (one abstract method; FILESYSTEM and zip sources), zip/jar roots through `M3FileTree.open` (central directory, implied dirs, nested zip-in-zip sub-trees via streaming, `ARCHIVE` flag, `entryCrc` lane gating), entry content via `ZipFile.getInputStream` with byte limits | differential vs `ZipFile.entries()` + `ZipInputStream` |
| S8 | `M3FileScanSpec` maxDepth, lazy `scanChildren(id)`; `expandArchives` (archives below a directory root) moved to S9 | done: random-order full expansion == eager scan == `Files.walk`; ids stable; fail-fast traversals |
| S9a | unix-style ops as traversal defaults (`bytes` = du, `wc`, `grep`), `render` (tree), `zipTo` (any source; repack archives) | done: differential vs POSIX wc rules, BufferedReader lines, ZipFile round trip |
| S9b | error lane + `M3FileLimitException`, `expandArchives` (archives below a directory root as sub-trees; `M3FileArchiveContent` serves both sources) | done: NOT_FOUND/LIMIT lanes set and cleared, classification table, expanded rows and bytes equal the archive-root tree, entry-level delta |
| S10 | viewport row host over `com.m3.viewport.M3RowHost` | 100k rows, O(window) per frame |
| S11 | lane image (little-endian lanes + name pool), content-addressed | round-trip equal; open without re-scan |
| S12 | Synexia crate custody `crates/m3-file/file-tree` (stacked on Synexia #12520), output = this branch | recipe fixed point; draft PR |

Out of scope: permissions/ACLs, following links, native watchers, JNI.

## 8. Test plans

S1: temp tree (3 levels, 12 files, dot-file, empty dir): invariants, `toPath()` == real path, flags, residency eviction identity, detached edge unchanged.
S2: same fixture + optional symlink (assumption-guarded): walk order == `Files.walk(root, n)`; glob == `PathMatcher` over `root.relativize`; regex == `Pattern` over the relative string; prune == `walkFileTree` with `SKIP_SUBTREE`; kinds == `Files.isRegularFile/isDirectory/isSymbolicLink` (NOFOLLOW); sub-root walks; parallel `toList()` == sequential; SIZED exactness; cursor `skipSubtree`.
