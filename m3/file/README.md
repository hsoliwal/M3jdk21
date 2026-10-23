<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3File: NIO reads and source/class content observations

`com.m3.file.M3File` is the existing M3JDK21-owned Java 21 file access
boundary. Its original NIO read behavior was adapted from canonical Synexia
`synexia-mindex/resource/.../MIndexResourceAccess.java`, Apache-2.0,
Git blob `1b4eb18b102f52be2d1e13589195b47e37f3b090`.

The supplied `Path` instance and filesystem provider remain authoritative.
Metadata calls refresh with `NOFOLLOW_LINKS`; streams/channels remain
caller-owned; `readAllBytes(maxBytes)` checks both observed and actual length.
The final path component cannot be a symbolic link or directory. Ancestor
resolution and permission failures retain their NIO provider behavior.

## Source and class deltas

Build `mvn -B -f m3/file/pom.xml test` or the containing Eclipse JMOD
builder in `m3/eclipse/optional-jmod/build.py`.

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
```java
var file = M3File.of(path);
var before = file.snapshot(64L * 1024 * 1024);
// A later explicit observation follows the build's controlled input generation.
var after = file.snapshot(64L * 1024 * 1024);
var change = after.changeFrom(before);
```

`snapshot(long maxBytes)` hashes the complete raw file bytes with SHA-256.
Source files and binary `.class` files use the same operation; no decoding or
parser invocation occurs. Hashing retains one 8 KiB buffer and digest state,
independent of file length. The resulting `M3FileSnapshot` retains its original
Path, existing immutable metadata and four digest longs. It retains no input
bytes or open descriptor. `sha256()` returns an optional 64-character hex
identity; `sha256Bytes()` returns fresh caller-owned bytes. Missing files have
neither metadata nor a digest.

| Previous observation | Current observation | Change |
| --- | --- | --- |
| Missing | Missing | `UNCHANGED` |
| Missing | Present | `CREATED` |
| Present | Missing | `DELETED` |
| Present | Different byte length or complete SHA-256 | `CONTENT_CHANGED` |
| Present | Same byte identity, different existing metadata | `METADATA_CHANGED` |
| Present | Same byte identity and metadata | `UNCHANGED` |

Content change takes priority over metadata change. Same-length edits with
restored timestamps are still detected by their bytes. `changeFrom` requires
the same FileSystem instance and provider-defined Path equality. It does not
normalize paths, merge aliases or follow hard links to infer equality.
`METADATA_CHANGED` compares the existing `M3File.Metadata` fields: file-kind
flags, size and modification time.

## Observation and build ownership

An initial `NoSuchFileException` is an absent-file observation. Other failures
propagate, including permission failure, unsupported provider options, invalid
file type, failed reading, failed close, exceeded byte budget, and observed
replacement or mutation during reading. Before/after basic attributes compare
file type, size, modification time, creation time and provider file key; both
sizes must match the number of bytes consumed. A limit check may read one
extra byte to distinguish EOF from an oversized stream.

These checks are observations, not an atomic filesystem snapshot. Concurrent
writes restoring all observed metadata, ancestor replacement, and changes
after the observation can escape the checks. The build owns stable input and
output generations and must bind all relevant action inputs before reuse.

A raw-byte delta supplies a build fact. It does not identify Java API/ABI
changes, prove semantic equivalence, invalidate a dependency graph, remove stale
compiler outputs, or authorize reuse of a live AST. `M3AST` remains the owner of
sharing within the original compiler task. The existing M3FS manifest/content
stores remain the persistence owners. This file module adds no global cache,
file tree, compiler dependency or native symbols.

The broader Synexia `MIndexResourceWorkspace`, Eclipse markers/deltas, tree
scans, permissions and mutation APIs remain outside this receiver. Existing
`java.io.File` contracts are unchanged.

## Verification

Run `mvn -B -f m3/file/pom.xml test` with a complete Java 21 development kit.
The tests use real file bytes and JavaCompiler output, including timestamp
restoration, metadata-only changes, creation/deletion, strict limits, provider
path boundaries, symbolic links, and digest ownership. A deterministic NIO
provider exercises changed file keys, size/time changes, short or growing
streams, permission and I/O failures, and failed close. The containing Eclipse
JMOD builder remains `m3/eclipse/optional-jmod/build.py`.

The [naming and ownership mapping](../docs/name-mapping.d/m3-file-deltas-20261020.json)
records the existing file owner and added snapshot API. The
[installed-module probe](src/test/java/M3FileModuleProbe.java) checks all five
delta states through the named `com.m3.file` module. After the Maven test build,
this Linux example uses a fresh caller-selected proof directory:

```sh
M3_FILE_PROOF=/absolute/new-file-module-proof
mkdir -p "$M3_FILE_PROOF/jmods" "$M3_FILE_PROOF/legal" "$M3_FILE_PROOF/probe"
cp m3/LICENSE m3/NOTICE "$M3_FILE_PROOF/legal/"
"$JAVA_HOME/bin/jmod" create --class-path m3/file/target/classes \
  --legal-notices "$M3_FILE_PROOF/legal" "$M3_FILE_PROOF/jmods/com.m3.file.jmod"
"$JAVA_HOME/bin/jlink" --module-path "$JAVA_HOME/jmods:$M3_FILE_PROOF/jmods" \
  --add-modules com.m3.file --output "$M3_FILE_PROOF/image"
"$JAVA_HOME/bin/javac" --release 21 --module-path m3/file/target/classes \
  --add-modules com.m3.file -d "$M3_FILE_PROOF/probe" \
  m3/file/src/test/java/M3FileModuleProbe.java
"$M3_FILE_PROOF/image/bin/java" --add-modules com.m3.file \
  -cp "$M3_FILE_PROOF/probe" M3FileModuleProbe "$M3_FILE_PROOF/data"
```

Canonical Synexia recipes own source materialization and exact input/output
seals. Source-level file tests are separate from full M3JDK21 image, Maven
lifecycle invalidation, multi-platform filesystem, and performance acceptance.
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
