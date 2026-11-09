<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# M3FS provider defaults and template hooks

M3FS is the filesystem module of M3JDK21's integrated Java/HotSpot/JNI SDK. Its
existing provider API connects provider namespaces and content to shared
operations. Lazy metadata traversal, bounded work admission, explicit content
observation and native storage reuse belong to the existing `jdk.m3fs` product
module. Providers supply the operations specific to their backend.

## Provider coverage

"Available" means compiled and registered, or supplied by the caller through an
installed compatible implementation. Provider source custody alone does not make
a transport available. The capability of the mounted filesystem is the relevant
runtime contract; a provider-wide capability list can be a superset.

| Provider family | Entry point | Current boundary |
| --- | --- | --- |
| Default local filesystem | `MFiles.from(Path)` or `file:` | Existing local provider, including its parallel directory partitioning |
| RAM, temporary and resources | `ram:`, `tmp:`, `res:` | Existing compiled providers and their own lifetime/capability contracts |
| Archives and compression | `zip:`, `jar:` and aliases, `tar:`, `gz:`, `bz2:`, `tgz:`, `tbz2:` | Existing VFS layered content; archive providers retain their read-only contracts |
| Runtime URL resources | `jrt:` | Existing generic URL provider; unrelated network transports are not registered implicitly |
| Installed nondefault NIO filesystem | `MFiles.from(providerPath)` | New adapter borrows the exact live `FileSystem`; real ZIP and `jrt` paths are tested |
| Native database filesystem | `m3db:file:///absolute/store.sqlite!/path` | New provider over the existing SQLite/JNI `M3FS` owner; native library required at mount time |
| Additional compatible VFS implementation | `Plan.withProvider(scheme, factory)` or `META-INF/m3fs-vfs-providers.xml` | Existing extension mechanisms; supply fresh provider instances and required dependencies |
| FTP/FTPS, dedicated HTTP clients, SFTP and other excluded donor sources | Separate provider admission | Still outside the compiled core profile; remote credentials, dependencies and backend qualification are separate |

The relocated VFS API lives in `com.m3.fs.vfs`. A plugin compiled against
`org.apache.commons.vfs2` needs an explicit adaptation; package similarity is not
binary compatibility. Hadoop, object-store, repository and other database
integrations can use the same hooks when supplied as compatible providers. This
change does not silently install those systems or assign them shared semantics.

## Most operations are defaults

`MFileContent` remains a functional interface with one required method:

```java
InputStream openInputStream() throws IOException;
```

A read-only provider can implement that method alone. A writable provider adds
`openOutputStream(boolean append)`; observation/capture support adds
`observation()`. Existing progress, synchronous shadows and successful-read
capture compose around those hooks.

| Shared operation | Main overloads |
| --- | --- |
| Open replacing output | `openOutputStream()`, `openOutputStream(progress)` |
| Bounded complete bytes | `readAllBytes(maxBytes)`, `readAllBytes(maxBytes, progress)` |
| Bounded decoded text | `readString(maxBytes, charset)`, plus explicit progress |
| Replace or append bytes | `write(bytes)`, `append(bytes)`, plus explicit progress |
| Validated byte region | `write(bytes, offset, length, append)`, plus explicit progress |
| Transfer to caller output | `transferTo(destination)`, plus explicit progress |
| Transfer from caller input | `transferFrom(source)`, append selection, and explicit progress |

No-monitor overloads retain the monitor and shadows already attached to a view.
An explicit monitor replaces the bound monitor for that call. The monitor is
borrowed and never marked done by stream closure. Invalid arguments and already
canceled explicit operations are rejected before opening or truncating content.

The defaults close streams they open, preserve primary failures during cleanup,
and leave caller-supplied streams open. Transfer to a borrowed output does not
flush it. Transfer-to progress counts input bytes; transfer-from progress counts
successful output writes. Failure can leave partial provider effects, and a
successful Java output call does not independently prove durable storage.

Complete reads require an explicit byte limit. The implementation reads at most
one byte beyond it to distinguish exact EOF from excess and throws on overflow.
Arrays and decoded strings are intentional caller-owned materializations. Text
decoding uses the chosen charset's `String(byte[], Charset)` replacement
semantics. Capture requires EOF and successful closure; overflow does not publish
a complete capture.

```java
try (var entries = MFiles.from(root).filter(MFile::isFile).stream()) {
    var iterator = entries.iterator();
    while (iterator.hasNext()) {
        MFile file = iterator.next();
        String text = file.readString(1_048_576, StandardCharsets.UTF_8);
        consume(file.relativeName(), text);
    }
}
```

Keep content operations inside the owning traversal session. Detached metadata
can outlive that session; a live content handle cannot.

## The abstract filesystem needs a small hook surface

The existing attributed `AbstractFileSystem` and `AbstractFileObject` remain the
template-method owners. No parallel abstract filesystem hierarchy is introduced.

| Owner | Provider supplies | Shared implementation supplies |
| --- | --- | --- |
| `FileSystem` | Existing namespace resolution and file operations | `executeM3Batch`, `statAll`, `createFolders` and `deletePaths` defaults |
| `AbstractFileSystem` | `createFile`, `addCapabilities`, communication cleanup where needed; an optional `doExecuteM3Batch` override for a qualified backend | Name resolution, object cache, listener/event and stream lifecycle; ordered bulk execution through the interface default |
| `AbstractFileObject` read hooks | Type, size, child listing and input stream | Content facade, selection/traversal, copy composition and resource handling |
| Optional mutation hooks | Folder creation, output, delete, rename and timestamp update | Common public overloads, recursive operations and content buffering, subject to backend limits |
| `MFileContent` | One input hook; optional output/observation | The content operations above, progress, shadows and captures |

Mutating providers must also preflight source identity and unsupported move
cases before inherited operations can delete an existing destination. The new
adapters reject unsafe overlapping moves. Generic VFS move can predelete its
destination, so it does not establish atomic replacement. Explicit content copy
does not become a cross-filesystem transaction.

## Bulk filesystem requests

The existing `FileSystem.executeM3Batch` default accepts an `M3FsBulk.Batch` of
ordered stat, directory creation, deletion, recursive deletion, copy and move
requests. `statAll`, `createFolders` and `deletePaths` build these requests as
convenience overloads. `AbstractFileSystem.executeM3Batch` delegates through its
protected `doExecuteM3Batch` template hook; the default hook uses the existing
`M3FsBulk.fallback` implementation.

Both the database and NIO adapters inherit this execution path unchanged. Each
entry resolves in the mounted filesystem and calls the same `FileObject` methods
used by an individual operation. Database read-only and owner-lifetime checks,
root protection, and both adapters' move restrictions continue to apply. The
fallback records checked failures by entry; fail-fast mode leaves later entries
skipped. Progress and cancellation use the batch's borrowed monitor.

The fallback is sequential and nontransactional. An atomic batch request is
rejected before provider I/O. Database operations retain their individual native
transaction boundaries, and NIO operations retain their provider contracts.
Neither adapter overrides the hook to coalesce calls or execute one transaction
for a batch. A future provider-specific override requires separate evidence for
its advertised behavior.

## Borrowed NIO paths

Default-filesystem paths keep their existing local route. Other paths retain
their actual filesystem instance and use its NIO operations through the VFS
template machinery. The adapter never creates, remounts or closes that caller's
filesystem. Providers still need to implement the operations requested by the
caller; unsupported operations fail explicitly.

`MFile.uri()` is the native `Path.toUri()`. Its resolvable `sourceAddress()` uses
an internal `m3nio:` address containing a weak filesystem-instance token, native
root and encoded path components. Two live filesystem instances with identical
native URIs remain distinct. Reopening that address requires the same live, open
instance in the same JVM; it is not a persistent cross-process locator.

The registry stores weak filesystem references, not a second file namespace.
The existing VFS cache owns resolved path objects. Nondefault roots remain single
owner regions in parallel traversal. Native append, atomic move, locking,
snapshots and random access are not advertised by this adapter. Cross-filesystem
move is refused before changing either side; explicit content copy remains
available through the shared API. Same-instance file moves use the provider;
directory sources and nonempty destination directories are refused. Real ZIP
provider probes showed directory moves retaining the original directory, so the
bridge does not infer a portable directory-move capability.

## Database-backed filesystem example

Here DBFS means a database-backed filesystem. `DatabaseFileProvider` uses the
existing native `M3FS` SQLite namespace, inode identities and deduplicated blocks.
It issues no SQL and adds no second JDBC payload store. Create a database
explicitly, then mount it through the ordinary manager or `MFiles`.

```java
Path database = Path.of("/absolute/workspace.sqlite");
try (M3FS owner = M3FS.create(database)) {
    owner.mkdir("/documents", 0755);
    owner.createFile("/documents/report.txt", 0644);
}

String address = "m3db:" + database.toUri() + "!/documents/report.txt";
try (var entries = MFiles.from(address).stream()) {
    MFile file = entries.findFirst().orElseThrow();
    file.write("first\n".getBytes(StandardCharsets.UTF_8));
    file.append("second\n".getBytes(StandardCharsets.UTF_8));
    String text = file.readString(4096, StandardCharsets.UTF_8);
}

try (var manager = new StandardFileSystemManager()) {
    manager.init();
    try (var file = manager.resolveFile(address, DatabaseFileProvider.options(true))) {
        // This mount rejects writes; its capabilities reflect read-only access.
    }
}
```

Mounting requires an existing regular local M3FS database. Missing, foreign or
nonlocal backing is rejected, and a symbolic-link backing leaf is not followed.
The provider does not replicate a database to a temporary file. Literal percent,
space, Unicode and layer-separator characters are preserved in supported names;
native backslash basenames cannot be represented by VFS and are rejected.

Database streams check the opened inode for each native transfer. A replaced or
renamed path fails instead of switching to a different inode. Append chooses
EOF in the existing native transaction. A native write's return value is the
resulting file size, so the stream advances by the requested length instead.
Each native mutation has the native owner's transaction boundary; a complete
Java stream or recursive operation is not one new transaction.

Native timestamps are nanoseconds; VFS getters expose milliseconds. Writable
timestamp setters require the native metadata capability. Mode bits remain
metadata, without POSIX user/group authorization. Symbolic links are visible
with link metadata but their content is not dereferenced. Root deletion/movement
and unsafe moves are refused. Rename requires the same mounted database
filesystem; resolving the identical native inode through another mount is a
harmless no-op. Cross-mount/provider move is refused before destination mutation;
use explicit copy when its partial-effect contract is appropriate.
No random-access, permission-enforcement or
snapshot capability is invented.

Each database mount owns a native handle. Communication-link release can reopen
the mount on a later operation; old streams remain tied to their old handle.
Filesystem or manager closure permanently ends that mount's native access.
Buffered input can still contain bytes already read. Close output streams before
the manager so buffered output can finish. External writers require the existing
VFS refresh/cache policy; metadata plus a later content read is not a snapshot.

## M3JDK21 boundaries and qualification

The implementation reuses physical storage, immutable encountered metadata,
borrowed monitors and the existing bounded parallel traversal. It does not claim
zero-copy content, array-only provider internals, constant-memory directory
listing, automatic native bulk batching or a benchmark improvement. The VFS
child-list contract can still allocate an array for a directory.

The canonical Synexia recipe crate is
`synexia-openrewrite-recipes/recipes/m3fs-provider-defaults-20261021`. It uses the
existing hash-pinned Java/text recipe owners, checks exact original and output
bytes, refuses drift and tests a second-run no-op. Qualification records identify
the executed Java/JNI, native and linked-image checks. A linked supplied JDK is
not a source-built M3JDK21 qualification. Other operating systems, optional remote
credentials/backends, power-loss behavior and performance remain separately
qualified. The global String/arrays/collections receiving stage is unchanged.

The authoritative naming admission is capability `m3.fs.provider-defaults` in
`m3/docs/name-mapping.json` under `migration.records[]`. The companion
`m3/docs/name-mapping.d/m3fs-provider-defaults-20261021.json` supplies provider
details for that record. Public Java SE names and the existing module exports
remain unchanged; database and NIO helpers stay internal to `com.m3.fs`.

### Provider references

- [Java 21 FileSystemProvider](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/spi/FileSystemProvider.html): required primitives and optional/default operations.
- [Apache Commons VFS providers](https://commons.apache.org/proper/commons-vfs/filesystems.html): provider-dependent capability and URI contracts; this product's compiled profile is narrower than the upstream catalogue.
- [Hadoop BulkDelete](https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/filesystem/bulkdelete.html): explicit batch bounds and per-path failures illustrate why a common bulk API must not imply transactionality.
