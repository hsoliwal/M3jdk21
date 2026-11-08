<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/36827ee8a798ef6ce07f6fc55a2ac371b1382c06/m3-pmem-sdk/docs/TRANSFER_CONTRACT.md blob ab0cd70c6a69; recipe m3/optane/recipe/pmem-region-core-20261106/relocate.py v2 -->
# Owner-scoped zero-heap-staging PMEMSDK transfer — v0.2

`PmemTransfer.copy(source, srcOffset, destination, destOffset, bytes)` copies
bytes directly between native file-backed mappings. No temporary Java `byte[]`,
raw native address, or escaping `ByteBuffer` is involved. `equalRange(...)`
compares bytes with `memcmp`. Both APIs use signed 64-bit relative offsets and
check range overflow before JNI. They require SDK-owned mappings, not arbitrary
user implementations of `PmemRegion`.

The two owners are leased in a monotonically assigned lock order. A source takes
a read lock; destination takes a write lock for copy. When source and destination
are one owner, one write lock covers `memmove`. Closing either region waits
until native work has returned, and operations on closed regions fail.

`memmove` handles overlapping addresses **within the same native mapping**.
Two independent mappings may alias the same file bytes through different virtual
addresses. For those, the JNI atom compares file IDs (`st_dev`, `st_ino`) and
absolute mapped file offsets and rejects overlapping file ranges. Adjacent or
disjoint file extents remain valid. This policy also catches hard links to the
same inode. There is no per-range wrapper allocation on the native hot path.

The destination data is NOT durable merely because `copy()` returned. Call
`destination.persist(destOffset, bytes)` after the copy. The operation is not
atomic, transactional or crash-recoverable. File replacement/truncation during
a live map remains unsafe and may SIGBUS. Do not share or write a mapped model
concurrently with unrelated processes without a higher-level protocol.

This is not a CPU-to-GPU transfer, a FSDAX/DEV DAX capability claim, or a model
inference engine. A future migration should reuse canonical M3File / DOOPS
leases rather than introducing a second long-lived memory owner.

## Validation

* Cross-file native copy and comparison, with reopen after explicit `persist`.
* Forward/backward self-overlap (`memmove`) and independent mapping aliases.
* Hard-link alias detection by device and inode.
* Two-way concurrent transfers in opposite directions with no deadlock.
* Close during in-flight copy, rejecting operations after close.
* Overflow and end-of-region handling, plus sparse mappings beyond 2 GiB.
* Forced recipe replay, unexpected source / API drift rejection, UBSan and JMOD/jlink.

PMDK backend selection is optional and not exercised on physical Optane hardware.

## v0.3: compare-before-copy

`PmemTransfer.copyIfDifferent(source, sourceOffset, destination, destinationOffset, bytes)`
returns `true` only when a copy occurred. It validates both native spans and
backing-file aliases *before* reading; byte-identical content avoids a write.
Both mapping leases are held throughout a single native JNI entry. It does **not**
call `persist`; callers must persist a changed destination explicitly. Contended
writers holding another owner for the same physical mapping remain outside the
SDK synchronization contract. Measure compare cost against avoided writes before
enabling this operation in a hot LLM inference loop.

## Explicit FILE transfers and stable alias identity

FILE or mixed FILE/native transfers use positional I/O with at most 64 KiB
aggregate scratch under the same ordered owner leases. Same-owner overlap uses
copy direction equivalent to memmove. Distinct owners compare captured provider
file keys within the same filesystem instance, so later pathname replacement
cannot make two already-open aliases appear unrelated. A null key or incomparable
filesystem identity refuses potentially overlapping distinct-owner copies before
reading/writing. Disjoint absolute spans and empty transfers remain supported.
Native/native transfers retain descriptor device/inode checks and JNI kernels.
FILE persistence uses `force(true)` separately from transfer; its reported PAGE
category is not physical PMem store-granularity or atomicity certification.

External file/path mutation during open or while a region is live remains outside
the synchronization contract. Java path attributes bracket open but cannot bind
a channel atomically to a provider key or defeat arbitrary concurrent ABA path
replacement. Callers must coordinate those external writers.
