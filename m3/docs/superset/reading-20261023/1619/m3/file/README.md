<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3File: narrow NIO file-edge receiver

`com.m3.file.M3File` is a minimal target-owned, Java21/module-info-enabled
read-only access boundary adapted from the canonical Synexia
`synexia-mindex/resource/.../MIndexResourceAccess.java`, Apache-2.0,
Git blob `1b4eb18b102f52be2d1e13589195b47e37f3b090`.

It does **not** create a filesystem implementation or retain a directory
object graph. It preserves the supplied `Path` instance/provider; stat calls
always refresh with NOFOLLOW_LINKS; `readAllBytes(maxBytes)` checks the
visible file type, reads through a caller-closed channel, and enforces both
the observed-size and streamed-length limits. Symlinks and directories do
not become regular files. Path, link and permission behavior remains NIO's.

The broader Synexia `MIndexResourceSnapshot`, `MIndexResourceWorkspace`,
Eclipse markers/deltas, tree scans, permissions and mutation APIs remain
unported. This is not full Eclipse workspace parity, an OS-level file
interning service, or permission to change existing `java.io.File` semantics.

Build `mvn -B -f m3/file/pom.xml test` or the containing Eclipse JMOD
builder in `m3/eclipse/optional-jmod/build.py`.

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
## Source and class content deltas

`fingerprint(long maxBytes)` admits one bounded raw-byte observation for either
source or binary files through this original file boundary. It retains a byte
count and four SHA-256 words, uses an 8192-byte scratch buffer, and keeps no file
payload. The streamed budget and the regular-file/link rules still apply. It
refuses detected size, modification-time or file-key changes, but does not claim
an atomic filesystem snapshot under concurrent writes.

`contentDelta(previous, current)` compares retained fingerprints without I/O or
parsing. `ADDED=1`, `REMOVED=2`, `CONTENT_CHANGED=4`; zero means matching
fingerprints or two known absences. Null is caller-established absence, never
an unread file or a failed observation. Existing admitted fingerprints should
be reused; calling `fingerprint` again deliberately reads again.

Raw byte fingerprints differ from the parser's UTF-16 and framed source-fact
identities. A matching digest does not certify collision-free equality, class
ABI, compiler bindings or complete task inputs. The existing build planner must
qualify those facts before skipping work. The [shared build contract](../docs/build-delta-invariant.md)
defines source, binary, dependency and configuration invalidation for Maven,
Gradle and the other fork consumers. The [mapping](../docs/name-mapping.d/m3file-build-delta-20261020.json)
keeps this extension with the original product module.

The `test` lifecycle now invokes `verify.py` against Maven's already compiled
module. It compiles the independent contract, checks the named module, builds
a linked image and reruns the same contract there. This includes real javac
compilations with unchanged dependency binaries and a constant-inlining witness.
Run `mvn -B -f m3/file/pom.xml verify` with Java 21 and Python 3.
Alternatively, `python3 m3/file/verify.py --java-home /path/to/jdk21 --output /path/to/proof`
also compiles the original module itself.

Canonical recipe: `com.synexia.m3jdk.CollectionsContractRefinement20261017`,
dataset `file-content-delta-20261020`, in Synexia's existing
`synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017`.
This is a new sealed dataset of the existing Java/text recipe composition.
Fork planner receivers and full M3JDK21 bootstrap qualification remain separate.
=======
## M3FileTree: the lane tree (slice 1, 2026-10-20)

`M3FileTree.open(Path root)` scans a directory tree once (`Files.walkFileTree`,
links never followed) into structure-of-arrays lanes: `parent`, `depth`, a
`char[]` name pool with offsets, CSR child slices, `size`, `mtime` and one
`BitSet` per `M3FileFlag`. Row ids are pre-order and dense; `0` is the root.
`M3File` is the entry type: `tree.file(id)` returns a thin `(tree, id)` facade
from a bounded residency table; identity is the row, so facades may be dropped
and recreated. `M3File.of(Path)` keeps the detached read edge above. Design,
invariants and the remaining slices (lazy scan, deltas, views, viewport row
host, lane image) are in [DESIGN.md](DESIGN.md); donor decisions in
`qualification/m3file-tree-20261020/FOSS_REUSE_DECISION.tsv`.

## Walks, progress and shadow streams (S2, S3)

`tree.walk()` / `file.walk()` return an immutable `M3FileWalk` (depth limits,
kind mask, JDK glob, path/name regex, `prune`, `filter`, lane-level `pruneIds`
and `filterIds`) whose `M3FileTraversal` default methods give `stream()`,
`parallelStream()`, `map`, `forEach`, `collect`, `count` and an ORDERED,
splittable spliterator (SIZED when unfiltered); `cursor()` adds `nextEntry()`
and `skipSubtree()`. Order is `Files.walk` order. `M3File.openInputStream` and
`openOutputStream` take an `M3FileProgress` sub-monitor (units = bytes,
`split`, cancellation) and `M3FileShadow` layers that move with the main
stream: SHA-256, CRC32C, line index, and a UTF-8 layer feeding
`M3FileCharShadow`s (token hash; an M3AST builder plugs in from `m3/ast`).

## Names, hashes, change detection and deltas (S4-S6)

Rows keep interned `nameId`/`typeId` (`tree.names()`, `tree.types()`), so
repeated segments are stored once and file types are small ids. `tree.hash`
fills CRC32C, SHA-256 (#1567 `Fingerprint` word order), token and structure
hash lanes in one shadow pass per file. `M3FileChangeProvider` is all default
methods: an attribute gate (kind, size, mtime, file key) decides without I/O,
and only rows whose gate trips are read; providers with their own change
mechanism (WatchService, zip CRC, git index, an M3AST structure layer)
override one method. `M3FileTreeDelta.between(before, after, ...)` merge-joins
the sorted child slices of two scans into change lanes and a
`Stream<M3FileChange>` (ADDED/REMOVED/CONTENT_CHANGED at the #1567 values plus
SIZE, MTIME, KIND, FILE_KEY, TOKENS, STRUCTURE, UNREADABLE); `paths(mask)` is
the hand-off to the build delta ledger (#1568).
>>>>>>> 0cd0944136c1a87b1bb8e15e8052ed1fbdfb74c5
