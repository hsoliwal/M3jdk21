<!-- Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project; SPDX-License-Identifier: Apache-2.0 -->
# M3FS in M3JDK21

M3JDK21 owns M3FS as the Java module `jdk.m3fs`, exporting `com.m3.fs`.
The Maven artifact is `com.m3:m3-fs:0.1.0-SNAPSHOT`; its OSGi bundle name is
`com.m3.fs`. The runtime has no Synexia Java dependency. The pinned donor mapping,
retained licenses, and canonical materialization recipe are recorded in
[the port provenance](qualification/port-20261018/SOURCE.json) and
[DONORS.tsv](qualification/port-20261018/DONORS.tsv).

## Storage planes and object boundaries

| Plane | Owner and retained representation |
| --- | --- |
| Live namespace | [M3FS](src/main/java/com/m3/fs/M3FS.java) and native SQLite inode, directory-entry, and deduplicated block tables; 64 KiB block operations, links, sparse extents, snapshots, indexed images, and maintenance APIs. |
| Immutable host artifacts | [M3DirectoryContentStore](src/main/java/com/m3/fs/M3DirectoryContentStore.java) publishes exact content-addressed objects; [M3FileSystemStore](src/main/java/com/m3/fs/M3FileSystemStore.java) publishes manifests, generations, and a compare-and-swap `HEAD`; [M3PrecomputeCache](src/main/java/com/m3/fs/M3PrecomputeCache.java) indexes persisted result lanes. |

These planes have separate transaction boundaries. Publishing a host generation
does not commit a live SQLite namespace mutation. `M3SnapshotStore` retains
immutable artifacts; `M3FS.openSnapshot` retains one read-only SQLite view until
close and can retain WAL history while other owners write.

Metadata owners use flat primitive columns, packed content IDs, offsets,
integer lookup slots, and parallel `String[]` columns for names, paths, and
configuration. `M3DirectoryTable` stores directory names in `char[]` and metadata
in `int[]`/`long[]`. Mapped content remains owned by explicit mapped-buffer
objects in parallel arrays. Requested rows, references, directory entries, and
Eclipse handles are materialized at public API boundaries. This representation
does not assert that strings, mapped owners, or API results allocate no objects.
See [M3DirectoryTable](src/main/java/com/m3/fs/M3DirectoryTable.java),
[M3FileManifest](src/main/java/com/m3/fs/M3FileManifest.java), and
[M3FsPrecomputeBundle](src/main/java/com/m3/fs/M3FsPrecomputeBundle.java).

## Existing image and prepared-input contracts

The cold-file path admits existing `SMFILE01` format-1 images together with
authenticated `SMFSA001` format-1 source facts. Token-range SHA-256 uses the
original raw-byte span and its admitted offsets. It does not rebuild a token
dictionary or substitute re-encoded text. Validation includes bounds, token
geometry, source bindings, and checksums. See
[M3ColdFileImage](src/main/java/com/m3/fs/M3ColdFileImage.java),
[M3FileSourceFactsImage](src/main/java/com/m3/fs/M3FileSourceFactsImage.java), and
[M3FileTokenRangeSha256](src/main/java/com/m3/fs/M3FileTokenRangeSha256.java).

`M3PreparedByteSegments` and `M3PreparedBytePatternMachine` are getter contracts
for already prepared bytes, boundaries, n-gram masks, transition tables, and
outputs. The precompute writer persists those supplied lanes; catalog entries
describe reusable shapes and do not implement every named execution backend.
`M3FsIndexImage` preserves supplied token identities and UTF-16 content.
`M3FsRegexImage` accepts an already compiled automaton. A bridge from the target
regex compiler to that image producer is not implemented by this port.

For a valid capture-free compiled image, `M3FS.nativeRegex` can resolve native
unsupported values automatically using JDK Pattern or the internally owned
RE2J1.8 backend according to the image's semantics. It compiles the fallback
once per operation, after native matching and exact persisted-image binding,
and retains one read snapshot while consuming all values. The explicit caller
fallback overload remains available. The RE2 implementation is not exported by
the Java module or OSGi bundle; production still requires only `java.base`.
Original RE2J/Go copyright and BSD terms are packaged with the runtime. This
restores the donor fallback convenience contract without claiming a new regex
compiler or an array-only rewrite of the donated regex engine.

## Commit and lifetime behavior

The native owner uses SQLite WAL and `synchronous=FULL`. Read and write operations
conditioned on an expected inode validate identity in the payload transaction.
Append chooses EOF in that transaction; checked truncate also remains atomic.
Committed inode IDs are not recycled. Native writes return the resulting file
size; native reads return zero at EOF. Namespace rename supports both atomic
replacement and atomic no-replace behavior.
See [m3fs.c](native/src/m3fs.c) and [m3fs.h](native/include/m3fs.h).

Host object and precompute-pointer publication forces staged data, uses
create-only hard links, verifies existing content before reuse, and synchronizes
the destination directory and newly created directory ancestry. Generation
promotion serializes cooperating JVM/process writers, forces the temporary
`HEAD`, requires atomic replacement, and synchronizes its directory. Unsupported
required filesystem primitives fail instead of silently selecting weaker
publication. These guarantees assume a private host tree and cooperating
writers. A failure after publication but before directory synchronization may
leave the new value visible with uncertain persistence; re-read and verify
before retrying. Functional tests do not establish power-loss durability for
every filesystem or device.

## Build and Eclipse consumption

Use a Java 21 development kit, CMake, a C toolchain, and SQLite development files:

~~~sh
mvn -f m3/fs/pom.xml install
mvn -f m3/eclipse/filesystem/pom.xml \
  -Dm3.fs.native.path=/absolute/path/m3/fs/target/native/libm3fs.so verify
~~~

The backend Maven lifecycle compiles JNI, runs Java tests with `-Xcheck:jni`,
and runs CTest during verification. Set `m3.fs.native.prefix` for a nonstandard
SQLite prefix and `m3.fs.native.fuse=ON` to build the optional FUSE executable.
The opt-in top-level `m3fs-eclipse` profile adds the Eclipse consumer to the
reactor. Native dependencies must be discoverable by the operating system.

The packaging wrapper delegates to M3JDK21's existing JMOD packer. It requires
Python 3.11+, a development kit with `jmod`/`jlink`, and a fresh output directory
and sibling receipts directory. On Linux, supply SQLite under its SONAME basename:

~~~sh
python3 m3/fs/tools/package_runtime.py \
  --jdk-home /absolute/jdk21 \
  --jar m3/fs/target/m3-fs-0.1.0-SNAPSHOT.jar \
  --native-library m3/fs/target/native/libm3fs.so \
  --sqlite-library /absolute/libsqlite3.so.0 \
  --out build/m3fs-runtime
mvn -f m3/eclipse/filesystem/pom.xml \
  -Djvm=/absolute/build/m3fs-runtime/image/bin/java \
  -Dm3.eclipse.vmOptions=--add-modules=jdk.m3fs verify
~~~

Run linked-image qualification with loader and Java injection environment
variables cleared and without `m3.fs.native.path`. That mode imports `com.m3.fs`
from the Equinox system bundle, omits the backend OSGi jar, and asserts module
ownership plus the `jrt:/jdk.m3fs` class origin. The provider's
[installation guide](../eclipse/filesystem/README.md) describes explicit mounts
and linked Resources locations. [CAPABILITIES.md](qualification/port-20261018/CAPABILITIES.md)
and [QUALIFICATION.md](qualification/port-20261018/QUALIFICATION.md) define the
functional acceptance scope, exact executed artifacts, and outstanding gates.


## Provider defaults and database mounts

[M3FS provider defaults](vfs/PROVIDER_DEFAULTS.md) describes the shared content
operations, the small VFS template-hook surface, installed NIO `Path` integration,
and the `m3db:` provider over the existing native SQLite owner. Ordinary local
paths keep their existing behavior. Nondefault paths borrow their caller-owned
filesystem. The current compiled provider profile and optional provider admission
remain explicit; an extension point does not establish remote qualification.
