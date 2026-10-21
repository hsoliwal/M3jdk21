# M3FS adaptation of Apache Commons VFS 2.10.0

Copyright 2026 Hitesh Soliwal and contributors for the M3FS modifications.
SPDX-License-Identifier: Apache-2.0

Original project: Apache Commons VFS, The Apache Software Foundation.
Release tag: rel/commons-vfs-2.10.0.
Commit: 685b3b723a83e56034391350ea1a02231411142f.
The adjacent LICENSE.txt and NOTICE.txt retain the exact upstream text.

The Java namespace is com.m3.fs.vfs and the former VFS facade is M3VFS.
Code remains in M3JDK21's existing jdk.m3fs module. MFile and MFiles provide
an additional session-owned stream API; the relocated FileObject API keeps
its original mutable handle and reusable-close semantics.

The URL fallback and UrlFileName wrapper use the donor's GenericURLFileName/GenericURLFileNameParser,
whose URIUtils implementation removes the legacy HttpClient 3 dependency.
The manager prepares cleanup prerequisites after partial initialization, releases
failed component initialization, and detaches each component before invoking its
close callback. A reentrant shutdown leaves cleanup with the outer owner. Shutdown
attempts remaining components after a failure and retains suppressed causes.
Standard initialization keeps its original customization order.
The qualified dependency override set is Logging 1.2, Lang 3.18.0, IO 2.21.0,
Compress 1.28.0 and Codec 1.19.0. Upstream release defaults were Logging 1.3.4,
Lang 3.17.0, IO 2.18.0, Compress 1.27.1 and Codec 1.18.0 respectively. These
bounded overrides use the exact archive hashes in tools/vfs-dependencies.lock.json.
M3 extension descriptors are META-INF/m3fs-vfs-providers.xml, avoiding accidental
discovery of providers compiled against the original Apache namespace.
The core registry explicitly registers the JDK jrt scheme with its generic URL
provider so named-module resources resolve in the linked image. Unknown URL
schemes still require explicit provider registration; omitted transport clients
are not enabled by the URL naming checks.

The core/archive build profile explicitly excludes dependency-blocked transports,
legacy URLFileName types and LRUFilesCache. Their complete relocated source is
retained for separately qualified admission. The selected registry includes only
compiled providers. This is not binary-compatible with original-namespace plugins.

No source file loses its original license header. The M3 changes do not claim
authorship of the original Apache implementation or endorsement by Apache.
