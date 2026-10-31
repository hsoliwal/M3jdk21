<!-- Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Eclipse Resources consumption of M3FS

This module registers the `m3fs` filesystem with Eclipse EFS. Eclipse Resources
can create linked projects/folders/files, read and write them, copy between M3FS
and local storage, move resources, and refresh workspace state through its
existing APIs. The filesystem implementation is owned by M3JDK21 in `m3/fs` and
the `jdk.m3fs` module. This Eclipse bundle contains the EFS compatibility API
objects and forwards their storage operations to that implementation.

## Storage and ownership

The native M3FS database and its array metadata are authoritative. The provider
keeps only parallel arrays for mounted image names, image owners and mount
generations. It does not build an object tree, an inode-object cache, a map of
file entries, or an independent content store.

`IFileStore` is materialized on an EFS API request and contains a path plus a
mount generation. `IFileInfo` is a fresh API result. Invalid filesystem URIs return an EFS null-filesystem handle; invalid child components fail validation. Directory enumeration reads
`M3DirectoryTable` char/int/long lanes and materializes names only for the
requested API result. Native inode identities are never recycled after a
committed allocation, which lets stream facades refuse a removed/replaced path
without silently reading or writing a different file.

Mounts are explicit and provider-owned. Successful `mount(name, image)` transfers
image lifetime to the provider. `unmount(name)`, `close()`, or OSGi bundle stop
closes owned image handles. Handles obtained before unmount remain invalid when
the same URI mount name is reused. URI equality continues to represent the
logical resource location; it does not grant an old handle access to a new
native image.

## Bundle installation

The provider bundle is `com.m3.eclipse.filesystem`; its `plugin.xml` registers
`org.eclipse.core.filesystem.filesystems` for scheme `m3fs`.

On an M3JDK21 image containing `jdk.m3fs`, resolve that module and make its exported
package visible to the Eclipse system bundle:

```text
--add-modules=jdk.m3fs
-Dorg.osgi.framework.system.packages.extra=com.m3.fs;version="0.1.0"
-Dorg.osgi.framework.bootdelegation=com.m3.fs
```

For the separate Maven qualification build, the same M3JDK21 backend jar is an
OSGi bundle named `com.m3.fs`, exporting `com.m3.fs`. The integration harness can
exercise both packaging forms. It uses the system module when it is present and
otherwise installs the backend jar as a bundle.

Native loading uses `-Dm3.fs.native.path=/absolute/path/libm3fs.so` or the backend's
normal `System.loadLibrary("m3fs")` lookup. Its native dependencies must be visible
to the operating system's dynamic loader.

## Opening a workspace location

Create or open the mount before opening projects that consume its URI. A mount
name has 1–64 lower-case ASCII letters/digits with optional `-` or `.` after the
first character. Paths use the native namespace, independently of the host's
drive letters or separators.

```java
M3EclipseFileSystem fs = (M3EclipseFileSystem) EFS.getFileSystem("m3fs");
IFileStore root = fs.open("project-data", Path.of("/data/project.m3fs"), false);
IFileStore sources = root.getChild("sources").mkdir(EFS.NONE, null);

IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("example");
project.create(null);
project.open(null);
IFolder linkedSources = project.getFolder("sources");
linkedSources.createLink(sources.toURI(), IResource.NONE, null);
```

The location has the form `m3fs://project-data/sources`. Eclipse API calls such as
`IFile.create`, `appendContents`, `setContents`, `copy`, `move`, `delete`, and
`IResource.refreshLocal` then reach M3FS through EFS. `getLocationURI()` retains
the M3FS URI; the provider does not expose a misleading host `File` location.
EFS's explicit cached-local-copy facility remains available through the normal
`FileStore` implementation.

New images use `fs.create(name, database)`. Read-only images use
`fs.open(name, database, true)`. A consistent live snapshot can be mounted with
`fs.mount(name, M3FS.openSnapshot(database))` and is closed by `unmount`.

## Observable behavior

| EFS operation | M3FS behavior |
| --- | --- |
| Metadata fetch | Fresh native stat; missing files return `exists=false`; failures with a progress-monitor overload remain `CoreException`. |
| Child enumeration | Native paged directory lanes; UTF-8 native cursor order; no retained per-child metadata object. |
| File streams | Positional native I/O; checked truncate on ordinary output open; checked append chooses EOF within the native write transaction, including between separately opened image owners. |
| Directory creation | `EFS.SHALLOW` requires an existing parent; ordinary mkdir creates missing ancestors; an existing directory is a no-op. |
| Copy/move | EFS copy semantics and cross-provider transfer; same-mount moves use atomic native rename, with a no-replace transaction when `OVERWRITE` is absent; copy into the source or a source inode alias is refused even through a second live mount of the same database. |
| Metadata updates | Explicit `SET_ATTRIBUTES`/`SET_LAST_MODIFIED` flags; native mode and modification-time transaction; nanoseconds project to Eclipse milliseconds. |
| Permissions | Read-only and executable attributes map to native mode bits; read-only image policy is enforced for every mutation. |
| Symbolic links | Bounded namespace-local resolution for reads; deletion removes the link; dangling links remain enumerable and project `exists=false` with link attributes. |
| Recursive deletion | Iterative array work stack, no recursion-dependent Java stack growth; best-effort errors are reported through Eclipse `MultiStatus`. |
| Refresh | No provider metadata cache; changes made outside Resources become visible to `refreshLocal`. |

Streams identify the opened native inode and mount generation. Removing,
renaming, or replacing their path causes further I/O to fail rather than retarget
another file. Native reads, writes, append, and initial truncation check that
inode inside the same SQLite transaction as their payload operation. They do not
retain a POSIX descriptor that continues to refer to an
unlinked file. The current facade does not provide asynchronous file watches,
Windows archive/hidden attributes, ACLs, user impersonation, or an automatic mount
discovery service. Unsupported attribute bits are not advertised.

## Verification

Build/install the owning `m3/fs` backend, then run:

```sh
mvn -f m3/eclipse/filesystem/pom.xml \
  -Dm3.fs.native.path=/absolute/path/libm3fs.so verify
```

To qualify an assembled M3JDK21 image, use that image for the forked test JVMs.
The host running Maven still needs a Java 21 development kit:

```sh
mvn -f m3/eclipse/filesystem/pom.xml \
  -Djvm=/absolute/path/m3jdk21-image/bin/java \
  -Dm3.eclipse.vmOptions=--add-modules=jdk.m3fs verify
```

This mode resolves `jdk.m3fs`, imports `com.m3.fs` from the Equinox system bundle,
and excludes the backend jar from the installed OSGi bundles. The workspace
scenario asserts the actual backend class is owned by the `jdk.m3fs` Java module.
Omit the native-path override in this mode to exercise the image's own native
library loading and dependency packaging.

The build uses Java 21 with `-Xlint:all -Werror`. Surefire and Failsafe both run
with `-Xcheck:jni`; tests require a working native backend and do not skip on
native absence.

`M3EclipseFileSystemTest` exercises URI/handle identity, stream semantics,
metadata flags, Unicode directory paging, recursive delete, copy/move refusal,
symlinks and loops, hard-link alias protection, mount-generation fencing,
inode-replacement fencing, aliased image mounts, retained-snapshot copying,
concurrent append through separately opened owners, read-only images,
cancellation and missing files.

`M3ResourcesIT` starts an actual Equinox framework, installs the EFS extension and
backend bundles, activates Eclipse Resources and runs `M3WorkspaceScenario`
inside OSGi. Its 15 scenario checks cover extension discovery, URI round-trip,
linked folders, resource create/read/append/replace, copies in both directions
between local and M3FS stores, move, external-change refresh and deletion,
timestamps, snapshot isolation, project reopen and durable image reopen.
The launcher prints the completed scenario names and the actual test JVM home.

Qualification pins Eclipse Resources `3.24.0`, EFS `1.11.400`, Equinox OSGi
`3.24.200`, and the remaining runtime versions in `pom.xml`. This focused
headless scenario does not stand in for the entire Eclipse Platform fork's
Tycho suite, GUI tests, every plugin consumer, or a Windows/macOS native matrix.
Performance, retained-heap and process/native-memory measurements remain separate
from these functional contracts.

## Primary API references and provenance

This is an original M3JDK21-owned EFS provider; no Eclipse implementation source
is embedded. API and integration behavior were inspected in these upstream-owned
files in `hsoliwal/eclipse.platform` at inspected commit
`2bfb19847bd09bf28749194d489e8696b4a997b1`:

- `resources/bundles/org.eclipse.core.filesystem/src/org/eclipse/core/filesystem/EFS.java`
- `resources/bundles/org.eclipse.core.filesystem/src/org/eclipse/core/filesystem/IFileStore.java`
- `resources/bundles/org.eclipse.core.filesystem/src/org/eclipse/core/filesystem/provider/FileSystem.java`
- `resources/bundles/org.eclipse.core.filesystem/src/org/eclipse/core/filesystem/provider/FileStore.java`
- `resources/bundles/org.eclipse.core.filesystem/schema/filesystems.exsd`
- `resources/tests/org.eclipse.core.tests.resources/src/org/eclipse/core/tests/resources/NonLocalLinkedResourceTest.java`

The runtime backend retains the full MIndexFS donor source mapping, original
copyright/license and target-owned JNI identity in the `m3/fs` port receipts.
The canonical Synexia-owned recipe materializes this module alongside that port.
