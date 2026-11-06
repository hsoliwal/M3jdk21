<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/docs/SOURCE_SLICES_CONTRACT.md blob 84cc9a869c7c; recipe m3/optane/recipe/pmem-source-image-20261106/relocate.py v3 -->
# PMEMSDK read-only source slices for M3File / M3Scale

## Ownership

M3PmemSourceSlices is a primitive-offset adapter to an **already packed**, immutable
M3File-owned source image. It does not enumerate repositories, generate the
image, own paths, infer missing source, parse ASTs, collect Maven quality findings,
control Chrome/MLLM sessions, or promote source changes. M3File retains the
snapshot/lease until the single-file model work order has completed.

## Geometry and admission

* One SDK-owned M3PmemRegion mapping, opened READ_ONLY; long[] offsets,
  int[] byte lengths and 32 digest bytes per entry. Arrays are copied and
  checked for monotonic nonoverlapping spans, overflow and mapping bounds.
* File-level SHA-256 covers **the exact bytes returned to the caller**, not
  merely the path/metadata. A changed source image is refused before dispatch.
* Default materialization budget is 1 MiB per file (caller may choose a limit
  up to 16 MiB); bigger files must use the canonical M3File dossier/chunk plan
  instead of silently truncating. This does not guarantee any particular model
  context window or tokenizer capacity.
* This type does not close its caller-owned mapping, expose raw native pointers,
  or guarantee an immutable backing file. Concurrent external mutation may
  invalidate a mapping; never truncate/reallocate mapped files. A stable
  snapshot lease is required at the higher layer.

## Scaling example

A 1,000,000,000-byte packed source payload with 20,000 slices of 50,000 bytes
uses about 160 KB (long[] offsets) + 80 KB (int[] lengths) + 640 KB
(SHA-256 byte lane) of primitive index payload, excluding array headers and
M3File's canonical path/AST/quality indices. Mapped pages are demand-paged;
**a 1GB mapping does not prove 1GB physically resident RAM**.

M3PmemSourceSlices.readSealed(i) makes one bounded file byte array, validates
its SHA-256 and returns it. The canonical M3Scale quality campaign then
combines source identity, complete findings, AST/atom facts and one per-file
LLD/recipe work order. One model call is an upper bound / workflow contract,
not a promise of model eligibility, token fit, runtime throughput or successful
semantic equivalence.

## Tests

PmemSourceSlicesSmokeTest (CPU) covers exact two-file reads, metadata isolation,
range refusal, budgets, wrong access, drift, and close. The Linux JNI
PmemSourceSlicesNativeSmokeTest creates a **sparse** 1GB backing file with
20,000 x 50KB entries and 20,000 source hashes; it reads four entries and
refuses an out-of-band mutation. It does not prefetch or scan a physical 1GB.
Local Java NIO-map fixture runs are explicitly distinct from native JNI proofs.
