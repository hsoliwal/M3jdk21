<!-- Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project; SPDX-License-Identifier: Apache-2.0 -->
# Capability and qualification boundaries

## Port closure

The ownership boundary is M3JDK21's `jdk.m3fs` module and `com.m3.fs` package.
The [donor mapping](DONORS.tsv) connects each imported owner to pinned Synexia
source; [SOURCE.json](SOURCE.json) records inspected revisions and source roles.
The Eclipse provider is an original consumer of this backend and retains no
independent filesystem metadata graph.

The native runtime reports ABI **1**, capability mask **16383** (14 optional ABI
capabilities), and semantic feature mask **2097151** (21 feature bits).
The masks describe different domains; their bit positions are not interchangeable.
The source exports 42 filesystem JNI methods plus three native content methods.
[The C API](../../native/include/m3fs.h),
[semantic flags](../../native/include/m3fs_semantic.h), and
[Java capability checks](../../src/main/java/com/m3/fs/M3FsNative.java)
are the authoritative definitions.

| Semantic bits | Supported behavior |
| --- | --- |
| 0–4 | Exact block deduplication, sparse files, shared-block clone/COW, hard links, symbolic links. |
| 5–9 | Retained read snapshots, directory paging, scatter/gather writes, WAL checkpoint, integrity verification. |
| 10–14 | Orphan-block collection, hole punching, allocated ranges, scatter/gather reads, native indexed-image access. |
| 15–19 | Native compiled-regex execution, block scrub, metadata updates, atomic content-image replacement, checked-inode I/O. |
| 20 | Atomic no-replace namespace rename. |

The donor's feature ledger contained both implemented and planned rows; it was
not treated as a complete executable API inventory. The port includes operations
already present beyond that ledger, and recovers sparse-operation implementations
from the pinned archived source material. It does not claim implementation of
every planned donor feature.

## Corrections carried with the port

The port fixes the donor native compilation collision and warning, sparse
directory iteration, and native-content buffer-position/endian handling.
It removes retained row/reference wrappers from directory, manifest, generation,
precompute, mapped-load, and execution-path metadata internals. Public row
projections remain available and string columns remain `String[]`.

Committed inode allocation uses a persisted high-water mark. Expected-inode
reads, writes, append, and truncate reject a replaced path in the same native
transaction as their payload operation. Separate live owners therefore cannot
redirect an already opened EFS stream to a replacement inode. No-replace moves
protect an existing destination atomically; overwrite moves use one native
rename without a preceding delete.

EFS copy guards reject source/descendant and inode aliases, including separate
live mounts of the same physical database. Retained snapshots are distinct
views and can copy old content into a live destination. Symbolic links follow
the inspected EFS target-metadata contract: dangling entries remain enumerable,
carry link attributes, and report `exists=false`. Recursive deletion unlinks a
directory symlink without traversing its target. Copy and cross-provider move
remain composed EFS operations, not global multi-resource transactions.

## Acceptance scope and remaining gates

[QUALIFICATION.md](QUALIFICATION.md) binds the fresh source and artifact digests
to executed reports. The provider suite requires actual JNI and covers 14
functional groups. A separate real Equinox test executes 15 named workspace
checks, including extension discovery, linked folders, content updates, transfers
to/from local storage, move, refresh, timestamps, snapshots, and reopen. Both
the backend-as-OSGi-bundle mode and the linked `jdk.m3fs` mode have dedicated
entrypoints and separate receipts. The linked harness asserts runtime ownership
and class origin; the packaging smoke checks native library resolution.

Native CTest and sanitizer runs qualify the Linux build recorded in those
receipts. The optional FUSE executable is compiled; platform mounts are not
qualified. The recovered environment uses SQLite 3.40.1 for native linkage;
the test-only JDBC dependency is a separate fixture tool. Historical reports
for lost source bytes cannot qualify reconstructed source without a fresh run.

This acceptance does not include a complete modified-JDK build, HotSpot boot,
jtreg/CDS/GC gates, the entire Eclipse fork's Tycho suite, GUI/plugin coverage,
Windows/macOS integration, mounted FUSE behavior, filesystem throughput,
isolated M3FS native-allocation measurements, or GC-latency qualification. The
[bounded construction probe](evidence/resource/RESOURCE_QUALIFICATION.md) records
CPU, allocation, post-GC incremental owner heap, process RSS and whole-JVM NMT
for one 50,000-row workload. Those observations do not establish broad
performance or memory guarantees. Power-loss testing and hostile concurrent
mutation of the host artifact tree remain outside the publication tests. The target
compiler-to-regex-image producer bridge also remains outstanding.

