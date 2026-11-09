<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/36827ee8a798ef6ce07f6fc55a2ac371b1382c06/m3-pmem-sdk/docs/API_CONTRACT.md blob 097bcdee8a76; recipe m3/optane/recipe/pmem-region-core-20261106/relocate.py v2 -->
# Master PMEMSDK JNI/Java 21 API — v0.2 contract

## Java module

`org.m3jdk.optane` exports `jdk.m3.optane` and deliberately has no dependency on libpmem2 at Java compile time.
It is a module of M3JDK 21 (m3/optane, Maven com.m3:m3-optane; verify.sh builds its JMOD).

```java
import jdk.m3.optane.*;
import java.nio.file.Path;

try (PmemRegion model = PmemSdk.open(Path.of("/pmem/models/weights.bin"),
        0, 8192, PmemAccess.READ_ONLY, PmemBackend.AUTO)) {
    byte[] header = new byte[32];
    model.read(0, header, 0, header.length);
}
```

`PmemSdk.capabilities()` returns JNI library loadability, whether the local JNI binary was built with libpmem2 support, and OS page size; it **does not** certify real Optane, namespace or DAX support.

`PmemSdk.open()` only accepts an **already existing regular file**, a nonnegative 64-bit offset, positive mapping length and a mapping range contained in that file. It deliberately does not provision PMem namespaces or silently grow files. A symlink in the final component is rejected: native backends use `O_NOFOLLOW`; FILE uses no-follow basic attributes and opens its channel with `LinkOption.NOFOLLOW_LINKS`. A writable mapping requires write permission. Files must not be truncated while mapped; external truncation can cause SIGBUS.

| API | Contract |
| --- | --- |
| `PmemSdk.open(Path,long,long,PmemAccess,PmemBackend)` | Open a range; backend selected at runtime |
| `PmemSdk.capabilities()` | Probe build/runtime capabilities, not hardware certification |
| `PmemRegion.read(long,byte[],int,int)` | Bounds-checked copy from mapping; no pointer escape |
| `PmemRegion.write(long,byte[],int,int)` | Bounds-checked write on read/write mapping |
| `PmemRegion.persist(long,long)` | Synchronous flush of a range; not atomic |
| `PmemRegion.close()` | Wait for in-flight SDK calls; idempotent |
| `PmemRegion.backend()` | Actual selected backend: POSIX, PMDK or explicit FILE |
| `PmemRegion.persistenceGranularity()` | BYTE, CACHE_LINE or PAGE; not transaction atomicity |
| `PmemRegion.length()`, `fileOffset()`, `isOpen()` | 64-bit mapping metadata and state |

## Backends / actual meaning

- `POSIX`: Linux file `mmap` with `MAP_SHARED`, `msync(MS_SYNC)` and `fdatasync`. Report granularity `PAGE`. Regular filesystem-backed mapping works on machines without Optane. Durability is subject to storage and filesystem guarantees.
- `PMDK`: Compiled only when `pkg-config --exists libpmem2` succeeds. Uses `pmem2_map_new`, `pmem2_map_get_store_granularity`, and `pmem2_get_persist_fn`. This path requires independent PMDK/DAX testing on physical hardware.
- `FILE`: Explicit ordinary-file backend using Java positional `FileChannel` reads/writes and `force(true)` for nonempty persist. It does not load JNI or memory-map the file. It reports `PAGE` only as a coarse file-I/O persistence category; this is not a measured device store granularity, page-size assertion, atomicity guarantee or physical PMem claim. Durability follows Java channel, filesystem and storage guarantees. A zero-byte persist validates open/write access and returns without forcing.
- `AUTO`: Prefer supported PMDK mapping when compiled, fall back to POSIX; it does not select FILE. `backend()` always reports the real selected backend. PMDK-only requests fail if it is not available.

**No `mmap` implementation can turn 2 TB of Optane into GPU VRAM.** The 64-bit API supports large regions, but each `byte[]` exchange is bounded by Java array dimensions; zero-copy native GGUF/JNI tensor integration needs owner-scoped operation APIs after canonical M3File lease integration.

## Safety and integration limits

- Read and write calls are protected by a fair Java `ReentrantReadWriteLock`; close obtains an exclusive lock and native addresses are never returned to callers.
- The Java SDK owns the native pointer but **does not create a new M3-wide canonical lease registry**. Replace the provisional private owner during M3JDK 21 integration using a recipe and regression gates.
- Caller must synchronize crash-consistency protocols, checksums and transactional metadata. `persist()` is not atomic and cannot prevent torn writes.
- PMDK backends, non-regular `/dev/dax` character devices, Windows, multi-socket placement, transactions, device badblock management and whole-model inference are not verified in this version.
- Do not infer persistence from Memory Mode. For real media durability, use an App Direct/DAX configuration and verify namespace/device paths explicitly with `ndctl`, `daxctl`, and server BIOS data.

## v0.2 additive mapped-region transfer

```java
try (PmemRegion weights = PmemSdk.open(sourcePath, 0, 8192, PmemAccess.READ_ONLY, PmemBackend.AUTO);
     PmemRegion staging = PmemSdk.open(stagePath, 0, 8192, PmemAccess.READ_WRITE, PmemBackend.AUTO)) {
    PmemTransfer.copy(weights, 128, staging, 256, 4096);
    staging.persist(256, 4096); // persistence is separate from copy
    boolean same = PmemTransfer.equalRange(weights, 128, staging, 256, 4096);
}
```

Native/native copy and compare allocate no Java heap staging array. FILE or mixed
operations use at most 64 KiB aggregate transfer scratch. Distinct file-backed
regions with overlapping physical file extents are rejected; same owner copies
use `memmove`. This is not an atomic transaction, does not bypass persistence
granularity, and does not represent a GPU transfer. See `TRANSFER_CONTRACT.md`.

## FILE identity and external path changes

Distinct-owner copies involving FILE use filesystem-scoped file keys captured
across open, rather than resolving current pathnames during transfer. This keeps
hard-link aliases identifiable after a pathname is renamed/replaced. If a provider
returns a null key, or identities belong to incomparable filesystem instances,
a copy whose absolute spans could overlap fails closed. Same-owner memmove,
zero-byte transfers, equality, and provably disjoint absolute spans remain valid.
Native/native alias checks continue to use the descriptor's native device/inode.
Native opens capture optional Java identity only for later mixed FILE/native copies.

Callers must prevent file replacement, truncation or mutation during open and
through the owner's lifetime. Before/after path attributes detect observed drift,
but Java provides no portable channel-to-file-key query and these checks are not
an atomic defense against concurrent path replacement or an ABA replacement.
The deterministic post-open replacement regression proves that stable captured
aliases are refused; it does not relax this external synchronization boundary.

FILE does not retry an open without NOFOLLOW_LINKS when a filesystem provider
cannot honor that option. Such providers may refuse open (for example the JDK21
ZIP provider rejects the channel option). If secure open is supported but a
provider supplies no file key, the existing conservative distinct-owner transfer
refusal still applies. No-follow final-component admission does not remove the
concurrent parent-path/ABA identity boundary described above.
