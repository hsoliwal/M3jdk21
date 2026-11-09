<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21 unified file and archive streams

Status: BOUNDED_CORE_BRIDGE; richer contracts remain runtime-pending.

The `m3fs-vfs-20261020` integration supplies a bounded `com.m3.fs.MFiles`/`MFile`
facade in the existing `jdk.m3fs` module, backed by the attributed Apache Commons
VFS 2.10.0 core relocated to `com.m3.fs.vfs`. A reusable single-root plan configures
name/extension and metadata filters, subtree pruning, traversal order, selective
archive layers, depth and custom providers. Each `stream()`
opens a fresh execution; ordinary `Stream.map` and the generic convenience overload
project results with lambdas. The source/recipe changes do not by themselves prove
that a runtime or packaging gate has passed.

Read [the current API guide](../../fs/vfs/README.md) for callable examples and limits.
Archive containers use explicit layered addresses such as `zip:file:///tmp/a.zip!/`
or opt-in `enterArchives` selection with an archive-layer limit; by default a `.zip`
Path remains a file. The provider exposes a logical hierarchy and can
collapse duplicate physical archive records. Stable metadata is retainable, while
content requires the owning stream to remain open. Git working trees use the file
provider; Git commit/LFS hydration support remains future work.

## Read the richer proposal

- [Design, current-core bridge and proposed contracts](DESIGN.md)
- [Capability catalogue](CAPABILITIES.tsv)
- [56 future runtime acceptance cases](ACCEPTANCE.tsv)
- [Remaining implementation plan](IMPLEMENTATION_PLAN.md)
- [Pinned owners, source references and evidence scope](SOURCE.json)

The bounded runtime also supplies `MFileContent` default methods, monitored input
and output, synchronous per-layer shadows, explicit `MFileObservation` facts and
evidence-qualified `MFileDelta` values. These reuse existing M3FS progress,
content, manifest and precompute owners.

Content predicates, physical archive occurrence identity, immutable entry/content
transforms, multiple sources, joins/deduplication, context-qualified source/AST
reuse, directory/archive output terminals and automatic incremental reconciliation
remain the richer proposal. Existing methods
with overlapping behavior do not implement those full contracts.

## Ownership and evidence

M3JDK21 owns runtime; Synexia owns reusable Maven/OpenRewrite recipes and sealed
postimages. The current facade belongs to `m3/fs`; extend it there. The distinct
`com.m3.file.M3File` Path/provider boundary and compiletime source snapshot owner
retain their existing roles. The VFS mutable `FileObject` is separate from `MFile`'s
immutable encountered metadata. No new compiler or canonical payload owner is added.

All 56 rows in `ACCEPTANCE.tsv` remain exactly `NOT_RUN_RUNTIME`. All 38 catalogue
statuses remain `SPECIFIED_RUNTIME_PENDING`; annotations identify bounded-source
overlap without turning a stronger future contract green. Documentation recipe
checks verify source delivery only. They do not establish archive-record identity,
JNI, output transactions, source snapshots or performance.

The authoritative bounded-image evidence is
[`qualification.json`](../../fs/verification/vfs-20261020/qualification.json).
The separate [`RESULTS.json`](../../fs/verification/vfs-20261020/RESULTS.json)
records the final disposition of each gate. Missing receipts or missing gate
results mean no success is established by this packet. The provider intake makes
no array-only, zero-copy, constant-memory or measured memory-gain claim.
