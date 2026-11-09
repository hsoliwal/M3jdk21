<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# MFiles hierarchical stream facade

`com.m3.fs.MFiles` and `com.m3.fs.MFile` belong to the existing M3FS module.
They use the source-integrated `com.m3.fs.vfs` provider implementation. They do
not create an independent filesystem catalog, payload store, or archive parser.

```java
try (Stream<MFile> files = MFiles.from(Path.of("data"))
        .extensions(".txt", ".json", ".bin")
        .filter(MFile::isFile)
        .descendInto(folder -> !folder.name().equals(".git"))
        .maxDepth(8)
        .stream()) {
    var entries = files.iterator();
    while (entries.hasNext()) {
        try (InputStream input = entries.next().openInputStream()) {
            // Consume incrementally inside this owning stream scope.
        }
    }
}

try (Stream<String> names = MFiles.stream(
        "zip:file:///tmp/archive.zip!/",
        MFile::isFile,
        MFile::relativeName)) {
    names.forEach(System.out::println);
}
```

## Traversal contract

- `MFiles.from(Path)` and `MFiles.from(String)` construct immutable plans without
  resolving providers, reading metadata, or invoking provider factories.
- Every `Plan.stream()` opens a new `StandardFileSystemManager`; there is no
  shared global manager. Re-execution sees the provider's then-current state.
- The returned stream is sequential and its spliterator cannot split.
  Traversal defaults to depth first in the order returned by each provider.
  `order(MFiles.TraversalOrder.BREADTH_FIRST)` or `breadthFirst()` selects
  breadth first. No sorting beyond provider encounter order is promised.
  DFS keeps one child-array cursor per active directory, without allocating a
  pending task for every child. VFS `getChildren()` still allocates one array for
  a directory. BFS retains a pending directory frontier and can use memory
  proportional to tree width; neither order promises constant memory.
- A directory root is omitted. Its children and recursively visited descendants
  include both files and directories. A selected file root is emitted.
- Provider types that support both content and children have both `isFile()` and
  `isDirectory()` set. Their children can be traversed, and a selected hybrid root
  follows the directory-root exclusion rule.
- The selected root is depth zero. `maxDepth(0)` emits a file root or nothing
  for a directory root. `maxDepth(1)` emits only immediate directory children.
  A directory at the boundary is emitted but its children are not requested.
  Negative depths throw `IllegalArgumentException`; the default is
  `Integer.MAX_VALUE`.
- `filter` and `filterName` predicates compose with AND and select output only.
  `filterName` receives a decoded base name before full metadata/size projection.
  Type and symlink queries are still needed to route traversal. A rejected name
  does not trigger a size query unless an explicit traversal selector needs its
  metadata. `extensions(".txt", "json", "bin")` is a case-sensitive name filter;
  optional leading dots are normalized and suffixes within a call are ORed.
  An empty suffix list selects nothing. Repeated calls compose with AND.
- A rejected folder still permits traversal into its children. `descendInto`
  predicates compose with AND and govern child enumeration only. A pruned folder
  can still be emitted. The descent predicate applies to the selected directory
  root and each automatically mounted directory root too.
- A folder's children are requested when traversal advances past that folder,
  allowing a short-circuit caller to stop before child enumeration.
- Symbolic links are classified using `FileObject.isSymbolicLink()`. Directory
  links are never descended into, including a selected directory-link root.
  This is provider-level link detection, not an OS-level no-follow security
  boundary against concurrent replacement. A provider that cannot identify
  its links retains that limitation.
- Local archive paths are ordinary files by default. Explicit layered URIs
  select archive traversal, including `zip:zip:file:///tmp/outer.zip!/inner.zip!/`.
  `enterArchives(entry -> entry.name().endsWith(".zip"), 2)` additionally selects
  automatic layers through the manager's `canCreateFileSystem/createFileSystem`
  API. It adds no parser or suffix-to-provider table. Selection is independent
  of output/name filters; unsupported types remain ordinary entries.
- Automatic mounting is deferred until traversal continues after the archive
  file. A short-circuit result can therefore return the archive entry without
  selecting or mounting its content. Selected malformed archives fail lazily.
  Symbolic links are not mounted. Layer budgets must be nonnegative; zero
  disables automatic mounting. A later `enterArchives` call replaces the rule.
- The layer budget counts additional automatic mounts from the selected root;
  layers already explicit in its URI do not consume it. Archive children are
  one global depth below the archive entry, so an archive file root is depth 0,
  its entries depth 1, and a nested archive's entries depth 2. Directory layer
  roots are not emitted; their descent callback names end in `!/`.
  Relative descendant names include boundaries such as
  `outer.zip!/inner.zip!/deep.txt`. Resolvable identity remains `sourceAddress()`.
  Single-content layers expose their content root as a child of the input file.

## Metadata and content lifetime

Every emitted entry has its own `MFile` instance. Names, URI/address, type flags,
link flag, and optional size are captured as immutable values when encountered;
they remain readable after the stream closes. Names and relative names are
decoded according to VFS naming semantics. Relative names use `/` separators.
A root file uses its own name; the un-emitted directory root supplied to a
descent predicate has an empty relative name.

The facade does not request an entry payload stream during metadata projection
or filtering. It asks VFS for type, link, name, and file-size metadata. Provider
metadata and archive mounting can themselves perform IO; this contract does
not override provider implementations. In particular, arbitrary URL providers
can open a connection while retrieving size. Built-in local and ZIP metadata
lookup does not open the selected entry's content stream. The existing archive
providers can replicate remote or nested archives to temporary local files while
mounting them; the facade does not promise copy-free or constant-memory mounting.

`size()` is empty for entries without content and provider-reported
negative/unknown sizes. A hybrid entry that permits both content and children
can have a captured file size.
Provider metadata failures propagate; the facade does not turn a failed size
query into a fabricated zero. `sourceAddress()` is the provider's normalized,
resolvable address and can contain credentials. `uri()` is the provider URI, suitable for the
provider's URI namespace, and does not turn archive entries into local paths.

`MFile` implements the reusable `MFileContent` interface. `openInputStream()`
is a checked-`IOException` operation. It opens a provider reader on demand and
never buffers the entire payload in the facade. `openOutputStream(false)`
explicitly requests replacement/truncation; `openOutputStream(true)` requests
append. Read-only providers reject output with checked IO. Traversal itself
never opens writers or changes file content. Captured metadata stays unchanged
by a later write.

Readers and writers should be closed promptly. Closing the owning stream closes
all outstanding content streams and its manager, even when an earlier close
throws. Reads/writes/flushes on expired streams, and new content opens on a
retained `MFile`, fail with `IOException`. The interface defaults add borrowed
`M3FsProgress`, optional caller-owned shadow outputs, and explicit stream-layer
labels through `MFileStreams`; their separate observation contract governs
byte counts and complete-content hashes.
An owned reader retains a primary-close failure and reports it on a later close
without retrying provider cleanup. This includes a close first performed by its
traversal owner: a surrounding monitoring wrapper cannot subsequently treat that
failed primary close as successful completion. Other outstanding readers and
writers are still closed when one fails. Reentrant close during primary cleanup
reports checked IO; a wrapper cannot publish a capture before that cleanup has
finished.

Normal traversal exhaustion does not close the owner: collected entries remain
readable while the stream scope is open. Callers must use try-with-resources,
including after `findFirst`, `limit`, and other short-circuit operations. A later
content read can see changed provider state; captured metadata is not a file
snapshot. No changes are made to the donor `FileObject.close()` semantics.

Opening/initial-root-type errors are checked `IOException`s. Lazy metadata and enumeration failures are
`UncheckedIOException`s with their original checked causes. Traversal and
callback failures during source advancement close the session and attach cleanup
failures as suppressed exceptions. Failures in buffered later stages, such as a
sorted comparator, still require the caller's try-with-resources scope.
Stream-close IO failures are reported as `UncheckedIOException`.

`observation()` explicitly refreshes metadata while the owner is open and returns
an immutable `MFileObservation`, keyed by the full provider `sourceAddress()`.
It captures existence/type/size and capability-supported modification time,
plus an optional provider `MFileChangeSource` stamp. It preserves valid signed
pre-epoch times; the ZIP/JAR `ZipEntry.getTime() == -1` sentinel alone is treated
as unknown. Zero remains a known timestamp. Both file and directory capability
flags can coexist. A confirmed absent entry is distinct from a failed query;
provider IO failures propagate and never manufacture deletion observations.
The call is not an atomic snapshot. Original `MFile` metadata and returned
observations remain available after close, while new observations require an
open owner. Change classification and optional existing M3 content/manifest
attachments are owned by `MFileObservation`/`MFileDelta`, not a new facade store.

## Reusable monitoring and complete-read capture

`MFileContent` defaults compose reusable views:

```java
MFileContent content = file.progressify(progress)
        .shadow(byteObserver)
        .captureReads(capture -> {
            MFileObservation observed = capture.observation();
            M3ObjectRef raw = capture.manifest().raw();
            // Retain detached facts or pass these references to an existing owner.
        });
try (InputStream input = content.openInputStream()) {
    input.transferTo(destination);
}
```

Configuration does not open content or query metadata. Every input open through
an opted-in capture view samples fresh provider metadata before opening the raw
input, then observes the bytes advanced by the main reader. The same byte stream
drives progress, borrowed shadows, SHA-256, and byte count. Output opens can use
the same progress and shadow defaults, but never create whole-file read captures:
their completed hashes describe only the write sequence.

A capture callback runs only after the reader actually observed EOF, the owned
primary closed successfully, and earlier user-shadow completion callbacks
succeeded. Closing early, reading exactly a known size without observing EOF,
provider failures, cancellation, and failed cleanup do not produce a capture.
Callback chains are ordered and fail fast; an already invoked observer may have
side effects that a later failure cannot roll back. Completion callbacks are
never retried by closing the wrapper again.

`MFileCapture` contains a detached observation, an existing `M3FileManifest`, and
`capturedAtMillis()`, the wall-clock time associated with sampling metadata. This
time is separate from the provider's modification timestamp. The observation
retains the original sampled attributes and attaches a complete `RAW_BYTES`
reference; that reference supplies observed byte count and SHA-256. If a known
sampled size differs from the completed byte count, close throws `IOException`
and the capture callback does not run. Matching size still does not establish an
atomic snapshot or prove that provider metadata describes the same instant as
the bytes. A source can change during a read, including changes that keep size.

The initial capture manifest contains the raw reference and empty structure,
relation, and precompute maps. It does not imply payload persistence, and it does
not change `MFileObservation.manifest()` semantics: that optional accessor remains
empty until a specific structure is selected. Captures survive traversal close.
Calling `MFileCapture.fromInput` directly attests that the completion covers the
full selected source layer from offset zero; an arbitrary stream's current
position or a descriptive layer label is insufficient evidence of that binding.

`capture.withStructure(name, reference, key)` adds an already computed structure
through the existing manifest/observation owners. It retains other references
and validates that the derivation key's first input is the captured raw identity.
Word IDs, source metadata images, and MAST-related outputs remain explicit
owner-produced structures. No tokenizer, AST parser, payload store, or history
is introduced. SHA-256 visits the bytes already consumed; additional parsing
and structure-production cost is incurred only by the observers or existing
owners the caller explicitly selects.

## Provider extension

```java
MFiles.Plan plan = MFiles.from("custom:/root")
        .withProvider("custom", CustomVfsProvider::new);
```

The supplier must return a fresh `com.m3.fs.vfs.provider.FileProvider` for each
open. It is invoked once per opened session and never during plan construction.
The manager owns the resulting provider through the existing VFS component
lifecycle. Registration names must be lower-case URI schemes. Duplicate custom
registrations are rejected by the plan; collisions with configured built-ins
fail on stream opening before the factory is invoked. Component initialization
and partial-failure cleanup are owned by the adapted manager; the facade does
not close a rejected component a second time. Shutdown continues after provider
failures, preserves suppressed errors, and guards nested manager close calls.
This API does not claim an implemented Git or Git-LFS provider.

## Archive identity boundary

`relativeName()` is a decoded display/traversal path, not a unique key or a safe
destination mapping. A literal directory named `outer.zip!` can render the same
`outer.zip!/file.txt` name as an entry in `outer.zip`; their full `sourceAddress()`
values stay distinct. No new filename escaping scheme is introduced.

This facade exposes the provider's logical hierarchy. It does not promise a
separate identity for duplicate physical archive records, and it does not
fabricate an ordinal or claim record-preserving enumeration. VFS providers can
collapse duplicate normalized entry names. Physical-record support requires a
separate capability or rejection policy in the integration contract.

## Verification

`MFilesBehaviorTest` is a reflection-based real consumer so exactly the same
behavior suite can run against the pre-facade baseline. The initial baseline run
compiled the existing M3FS Java sources with Java 21 and `-Xlint:all -Werror` and
recorded 18 assertion failures identifying the absent facade. It did not modify
the baseline receiver.

The suite exercises actual local trees, Unicode/literal-percent names, ZIP roots,
nested ZIP URIs, output filtering versus pruning, depth boundaries, lambda
transforms, plan repeatability, symlink descent, retained metadata, expired
content, and outstanding-reader closure. `MFilesProviderTest` adds real local
provider instrumentation for per-open ownership, metadata versus payload access,
unknown sizes, combined file/folder types, controlled provider IO failures,
failed-initialization cleanup, callback cleanup, and cleanup continuation after
an unchecked content-reader close failure. The extended suite covers cheap name
filtering, both traversal orders, deferred automatic ZIP layers and layer/depth
budgets, real output replacement/append, writer cleanup, manager reentry and
plugin configuration compatibility, fresh observations, hybrid capability flags,
provider change stamps, pre-epoch times, and a real ZIP unknown-time fixture.
`MFileCaptureTest` adds real-file complete-read captures, known SHA-256 values,
capture timing, fresh metadata on reopen, early-close exclusion, deterministic
size drift, metadata/open/close failures, ordered shadow and capture callbacks,
output exclusion, and optional existing structure bindings. The provider suite
also checks capture suppression after an owner-triggered raw close fails and
during provider reentry before the primary close returns.
There are 69 tests across these three classes.

The receiver and its tests are emitted from these authored templates only by
the canonical Synexia JavaSnapshot integration recipe. Generated-receiver test
results are recorded by the parent integration workflow; source templates alone
are not a green build claim.

## Verification commands

Run the generated receiver's complete compiled classes and its generated
resources (the helper reads and checks its dependency lock):

```sh
python3 authored/facade/verification/run_behavior.py --classes qualification-final/classes \
    --java-home toolchain/jdk21
```

During template development `--compile-owned` deliberately compiles the
facade templates into an isolated test output. Streams/facts support and donor
adaptation classes must be supplied explicitly on its diagnostic classpath;
that mode is not generated-only integration evidence. The parent runs the same
suite after canonical recipe generation before declaring integration green.
