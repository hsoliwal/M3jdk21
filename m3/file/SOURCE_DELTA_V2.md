<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3File source generation batch edits (candidate v2)

`M3FileEditBatch` composes ordered, disjoint edits against the **same admitted**
`M3FileSource` byte generation. In contrast to repeatedly invoking
`M3FileSource.replace`, it copies the base image in one pass, computes one
new SHA-256 source image and returns stable old/new byte-span columns. This
is a transient in-memory editor and parser input, **not** a new persistent
filesystem, watcher, build ledger or file scanner. The existing `M3File`
NIO owner, `M3FileDeltaLedger`, stream observers, provider and build-impact
recipes remain untouched.

Half-open byte offsets are authoritative. Java/JDT UTF-16 edits are accepted
only through strict `M3FileSource.byteAtUtf16` coordinates and well-formed
replacement strings. The API exposes `LEFT` and `RIGHT` endpoint affinity at
insertions/deletions, returns -1 for changed interiors, and refuses unordered,
overlapping or duplicate ambiguous insertion coordinates. `ChangeMap` is bound
to both source generation SHA-256 identities; callers check these before
reusing parsed subtrees. A changed parent can be reparsed while unaffected
child shape/content hashes are only reuse candidates, never proof of ABI or
semantic equality. Source data may be arbitrary bytes through `replaceBytes`,
but a subsequent UTF-16 operation fails if the image is not strict UTF-8.

The implementation currently uses a contiguous `byte[]` source and an output
materialization. No mmap, copy-free `char[]` virtualization, persistent piece
table, atomic filesystem snapshot, direct native buffer ownership, incremental
parser engine or B-tree paging is claimed. A mapped/paged source arena must
use the existing M3File provider and pass independent lifetime/locking/
GC/native-memory and exact-delta performance gates before being admitted.

Focused qualification exercises 300 fixed-seed randomized byte edit sequences,
Unicode surrogate split refusal, two-edit byte/UTF-16 conversion, left/right
mapping, reversed/overlapping edits, sealed generation checks and original
source immutability. Target `verify.py` is extended to execute both the
previous `M3FileSourceContract` and the new `M3FileEditBatchContract` on the
host and linked-image `com.m3.file` module, **when CI actually runs**. Local
standalone JDK21 tests used the recorded test-only M3File shim; target module
verification, Maven and installed-JDK tests remain pending until reported by
exact-head CI receipts.

Canonical recipe custody: Synexia dataset `portable-ast-file-v2-20261026` in
the existing Maven/OpenRewrite source-sealed family; product receives only
postimages and independent contracts.
