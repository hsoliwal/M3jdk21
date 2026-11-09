<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3FS hierarchical file and archive streams

`com.m3.fs.MFiles` and `MFile` belong to the existing `jdk.m3fs` module. They expose
a bounded stream API over Apache Commons VFS 2.10.0, whose attributed provider API
is relocated to `com.m3.fs.vfs` and whose `VFS` entry point is `M3VFS`.

This guide describes the delivered source contract. Actual qualification results
are recorded in [qualification.json](../verification/vfs-20261020/qualification.json)
and [RESULTS.json](../verification/vfs-20261020/RESULTS.json). It does not assert a
pass while those receipts are absent or a required gate is pending.

## Directory selection and lambda projection

```java
import com.m3.fs.MFile;
import com.m3.fs.MFiles;
import java.nio.file.Path;
import java.util.stream.Stream;

MFiles.Plan sources = MFiles.from(Path.of("checkout"))
        .descendInto(folder -> !folder.name().equals(".git"))
        .maxDepth(8)
        .filter(entry -> entry.isFile() && entry.name().endsWith(".java"));

try (Stream<MFile> entries = sources.stream()) {
    entries.map(MFile::relativeName).forEach(System.out::println);
}
```

`from(Path)` and `from(String)` each select one root. Plan construction and its
configuration methods perform no provider I/O. Every `stream()` creates a new
manager and session, so the same plan can observe later source changes. Lambdas
and supplier objects are retained as supplied; their mutable captured state is
not copied, inspected or serialized.

`filter` selects emitted entries. `descendInto` decides whether a directory's
children are visited. Rejecting a directory in one does not reject it in the
other. Each family composes with logical AND. Traversal defaults to depth first
in provider encounter order, with a directory before its children and no implicit
sort. `order(MFiles.TraversalOrder.BREADTH_FIRST)` or `breadthFirst()` requests
breadth-first traversal; it retains the pending frontier and can use more memory.

`filterName(Predicate<String>)` and `extensions("txt", "json", "bin")` select by
decoded basename before constructing a full entry projection. Extension matching
is case-sensitive and accepts an optional leading dot. These are output selectors,
so rejecting a directory name does not prune its children. Use `descendInto` for
that separate decision. Provider traversal still needs type/link facts to decide
which entries can be descended into.

Depth-first traversal retains a child-array cursor for each active directory and
advances it on demand. It does not allocate an additional task for every child or
construct a complete facade tree. The provider may still return a complete child
array and retain its own caches; the facade does not promise memory proportional
only to depth.

The directory root is excluded from output, including when empty; a selected
file root is emitted. The root is depth zero: `maxDepth(0)` emits no directory
children and `maxDepth(1)` emits immediate children. A directory at the boundary
can be emitted without listing its children. The descent predicate sees the
selected directory root too, with an empty relative name. Directory symlinks are
not descended into when the provider identifies them. This is provider-level
classification, not a concurrent-filesystem snapshot or an OS no-follow guarantee.

The convenience overload applies an output predicate and a result transformer:

```java
try (Stream<String> names = MFiles.stream(Path.of("checkout"),
        entry -> entry.isFile(),
        entry -> entry.relativeName())) {
    names.forEach(System.out::println);
}
```

This transforms stream results. There is no current `Plan.map`, immutable rename,
content-transform operation, or directory/archive output terminal. Individual
entries support explicit provider output through `MFileContent`, described below.

## Read an explicit archive root

```java
import java.io.InputStream;

Path archive = Path.of("bundle.zip").toAbsolutePath();
String address = "zip:" + archive.toUri() + "!/";
try (Stream<MFile> entries = MFiles.from(address)
        .filter(MFile::isFile).stream()) {
    // Exhaustion keeps the owner open; retained entries can still be read here.
    for (MFile entry : entries.toList()) {
        try (InputStream content = entry.openInputStream()) {
            byte[] prefix = content.readNBytes(64);
            System.out.println(entry.relativeName() + ": " + prefix.length);
        }
    }
}
```

A local `.zip` Path remains a regular file unless archive traversal is explicitly
enabled. An explicit provider address such as `jar:file:///tmp/a.jar!/` or
`tar:file:///tmp/a.tar!/` selects the container directly. Nested roots also work,
for example `zip:zip:file:///tmp/outer.zip!/lib/inner.zip!/`. Use the provider's URI
encoding rules for names containing reserved characters.

## Selective layered traversal

```java
MFiles.Plan selected = MFiles.from(Path.of("inputs"))
        .descendInto(folder -> !folder.name().equals(".git"))
        .enterArchives(entry -> entry.name().endsWith(".zip")
                || entry.name().endsWith(".jar"), 2)
        .extensions("txt", "json", "bin")
        .filter(MFile::isFile)
        .maxDepth(12);

try (Stream<MFile> entries = selected.stream()) {
    entries.map(MFile::relativeName).forEach(System.out::println);
}
```

`enterArchives(predicate, maxArchiveLayers)` is opt-in and has a separate archive
layer budget. A later call replaces that selection and budget; `filter`,
`filterName` and `descendInto` instead compose with AND. The layer budget counts
automatic mounts from the selected root, including an archive file root; layers
already selected in an explicit URI do not consume it. The global `maxDepth`
still bounds logical entry depth. A selected
archive is emitted as an ordinary file when it passes the output filters; its
mount is deferred until a subsequent traversal advance. Stopping on that archive
with `findFirst()` therefore does not mount it. Excluding an archive from the
output does not itself prevent selected members from being visited.

Each archive layer keeps its own provider root and relative-name prefix. A nested
member can therefore be named `outer.zip!/lib/inner.zip!/data.json` without asking
one filesystem to relativize a different filesystem's entry. Format selection
uses the admitted provider registry's MIME/extension mappings; it is not an
arbitrary content-sniffing parser. An unsupported container is not entered; a
selected corrupt container can fail lazily when mounting is attempted.
Layer mounting can index or spool a container before its children can be listed.
Traversal order, archive expansion and content parsing are separate operations.

The bounded `m3-vfs-core` profile selects local, RAM, temporary, resource, generic
URL, ZIP/JAR, GZIP/BZIP2 and TAR/TGZ/TBZ2 provider sources. Available operations are
provider-specific; listing a provider does not promise every format feature,
encryption mode, random-access operation or write operation. Optional FTP, FTPS,
HTTP-client families and SFTP are excluded, as are the old HttpClient-3 URL types
and LRU cache implementation. Their retained source is not executable support.
HDFS, Jackrabbit/WebDAV and Ant are separate upstream modules outside this intake.
The generic URL provider uses explicitly registered schemes; `jrt` is registered
for resources in named JDK modules. Additional URL schemes require application
registration and a compatible installed URL handler. The naming checks do not
qualify remote HTTP clients or arbitrary unregistered-scheme dispatch.

## Logical namespace and content lifetime

Each encountered entry has its own immutable metadata projection. `name()` is the
decoded final component; `relativeName()` is the decoded provider-relative name
using `/`. A selected file root uses its own name. `uri()` and `sourceAddress()`
identify the provider namespace; an archive address is not a local filesystem
Path or a destination path. A source address may carry supplied credentials, so
use an appropriate display value when logging it.

`relativeName()` is a decoded display/traversal path, not a unique identity or a
validated destination mapping. A literal directory named `outer.zip!` can render
the same relative text as an archive layer boundary. Use the full `sourceAddress()`
for source identity and observation/delta comparison; no new escaping convention
is imposed on decoded names.

The facade exposes the provider's logical hierarchy. Duplicate physical archive
records with the same normalized name can collapse into one logical entry. No
physical-member ordinal or record-preserving enumeration is exposed. Donor
synthetic parent directories are likewise not tagged by a facade synthetic flag.
Consumers needing those stronger identities must wait for the richer contract;
metadata or successful ZIP reads do not prove it.

`size()` is empty for entries without file content or provider-reported unknown
sizes; zero means a known empty file. A provider can expose both file content
and directory children on the same entry, in which case both type flags are true
and the file size can be present. Metadata is captured on encounter and stays readable after
stream closure. It does not prove that a later content read matches those facts.
No content snapshot, pinned generation or source/AST cache is created.

`openInputStream()` opens content on demand and throws `IOException` for an
expired owner, a non-file entry, or a provider read-open failure.
`openOutputStream(boolean append)` explicitly requests provider output and has
the same owner scope; unsupported or read-only providers report a checked failure.
Close each reader and writer promptly. Closing the owning stream closes all
outstanding content streams and the manager; retained metadata survives, while
subsequent content opens, reads or writes fail.
Always use try-with-resources, including after `findFirst`, `limit`, normal
exhaustion and exceptions. A terminal operation does not close a Java Stream.

Opening/root-metadata failures are checked `IOException`s. Later provider
traversal failures become `UncheckedIOException`. Traversal and callbacks invoked
during traversal close the session on failure and suppress cleanup failures onto
the primary error. Failures in buffered downstream stages, such as a sorted
comparator, require explicit stream closure; try-with-resources covers them too.
Stream-close I/O failures are unchecked. The traversal does
not split; use it sequentially and do not assume provider payload concurrency.

Facade projection does not request an entry payload stream, but provider metadata
can open connections. Directory child enumeration can allocate a complete child
array. Archive mounting can build indexes, decode data or replicate nested/remote
archives into local temporary files. The source import and facade therefore make
no array-only, zero-copy, constant-memory, or measured memory-gain claim.


## Default I/O methods, progress and synchronous shadows

`MFile` implements `MFileContent`. That interface centralizes monitored input,
monitored output and `transferTo` as default methods. A read-only implementation
needs only `openInputStream()`; writable providers override the raw output method.
Provider capability and consistency remain provider decisions.

```java
M3FsProgress progress = M3FsProgress.of(child::isCanceled, child::worked);
try (MFileStreams.Input input = entry.openInputStream(
        progress, shadowSink, "stored-bytes")) {
    existingParser.parse(input);
}
```

Here `child` is a child created by the existing M3 progress-monitor owner, for
example `parent.split(weight)`, and `shadowSink` is a caller-owned `OutputStream`.
M3FS borrows the callbacks and does not finish a child merely because an input is
closed or reaches EOF. The caller marks the child's task complete after its own
success condition. This bridge adds no dependency from M3FS to the collections
module.

The shadow receives bytes synchronously when the primary read or successful write
advances. It cannot pull independently, read ahead or retain an implicit replay
history. The wrapper owns its primary stream; it never closes or flushes the
borrowed shadow. Cancellation, primary I/O, shadow and progress failures stop and
close the primary, retaining suppressed cleanup errors. Skipping reads through
the observation, while mark/reset is unsupported.

For layered parsing, wrap both sides of a decoder with distinct layer labels,
monitors and shadows. Compressed-byte position, decoded-byte position and parser
token/AST commits are different domains. Decoder lookahead can advance an outer
layer before the inner layer emits data. Decoder EOF does not prove transport EOF.

`position()`, `complete()`, `failed()` and `contentHash()` expose each wrapper's
observation. Input hashes require that wrapper to observe its own EOF. Early
close, a failed operation or a failed close never establishes a complete hash.
Output hashes are finalized after successful flush and close and identify the
confirmed write sequence, not the entire destination file or an append result.
An input opened at a nonzero position identifies its observed suffix. Attaching
a full-file raw identity requires the caller to establish coverage and generation.
See [the full stream contract](../verification/streams/README.md).

## Observations and downstream deltas

`entry.observation()` explicitly captures fresh provider facts while the entry's
owner is open. The resulting `MFileObservation` survives owner closure. Unknown
lengths/timestamps remain distinct from known zero; I/O failures are not treated
as deletion. Valid pre-epoch timestamps are retained; the ZIP/JAR provider's
`-1` unknown-time sentinel remains unknown. The opt-in `MFileChangeSource` SPI
lets a provider expose a scoped
change stamp with its actual authority.

Callers can attach complete raw content through existing `M3ObjectRef` and
`M3ContentId` identities and attach a named structure through the existing
`M3FileManifest` and `M3PrecomputeKey`. A structural observation must match the
observed raw identity and its algorithm/version/configuration provenance. M3FS
does not parse an AST or create a second payload cache to produce these facts.

`MFileDelta.between(before, after)` compares two observations for the same source
address, including archive layers. A consumer overload delivers the validated
delta downstream. Metadata, provider stamp, raw content and structure remain
separate evidence: equal timestamp and length do not prove equal bytes; changed
bytes can still have an equal qualified structure. Unavailable evidence remains
unknown instead of becoming an unchanged verdict. Contradictory provider/hash
evidence cannot silently establish unchanged content.

The observations reuse compact M3FS identities and shared manifest/precompute
owners. They do not introduce a global file registry or turn the separate
`com.m3.file.M3File` Path wrapper into an interned identity service.
See [the observation and delta contract](../verification/OBSERVATIONS.md).

## Register an application provider

`Plan.withProvider` accepts a lower-case URI scheme and a supplier of the relocated
`com.m3.fs.vfs.provider.FileProvider`. For example, inside application code with a
`Supplier<? extends FileProvider> providerFactory`:

```java
MFiles.Plan custom = MFiles.from("custom:/root")
        .withProvider("custom", providerFactory);
try (Stream<MFile> entries = custom.stream()) {
    entries.map(MFile::relativeName).forEach(System.out::println);
}
```

The application supplies the real provider implementation. The factory is invoked
once per stream opening and must return a fresh instance owned by that session.
Duplicate custom registrations are rejected when configuring the plan; collisions
with built-in schemes fail when opening the stream. Plugins must target the M3
namespace; original Apache-namespace plugins are not binary-compatible. Automatic
extension descriptors use `META-INF/m3fs-vfs-providers.xml`.

A Git working tree needs no special provider: pass its checkout Path. The bytes
are whatever currently exists on disk, including unhydrated LFS pointer files.
Git commit/ref/tree access, object-store identity and LFS hydration are future
provider work; registering a scheme alone does not implement those behaviors.

## Provenance, packaging and future work

[Source custody](SOURCE.json) and [the selected profile](SOURCE_PROFILE.tsv) record
the exact Apache revision and source disposition. The 311 core Java inputs remain
in custody; the bounded profile selects 237 of them. Original Apache license
headers and [LICENSE/NOTICE](../legal/commons-vfs2/NOTICE.txt) are retained alongside
[the M3 modification record](../legal/commons-vfs2/CHANGES-M3FS.md).

The current provider closure uses the exact archives admitted by
`m3/fs/tools/vfs-dependencies.lock.json`. They remain separate Commons modules;
the M3 runtime remains `jdk.m3fs`. See [the packaging procedure](../verification/VFS_PACKAGING.md)
for JAR/JMOD/legal-byte checks, the linked-image consumer and distinct Maven,
native SQLite and source-built M3JDK21 gates. Source custody or a successful bounded
Java image does not establish those broader gates.

The [enhanced design](../../docs/mfile-streams/DESIGN.md) retains proposals for
content predicates, richer archive occurrence identities, immutable transforms,
multi-source comparison/deduplication, directory/archive output transactions, and
context-qualified source/AST reuse. Its 56 future acceptance
rows remain `NOT_RUN_RUNTIME`; current receipts do not mark the full proposal done.
