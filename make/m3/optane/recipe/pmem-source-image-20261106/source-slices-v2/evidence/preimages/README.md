<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Synexia Master PMEMSDK — Java 21 / JNI v0.2

A standalone, **Linux** Java 21 JNI module/JMOD for safe file-backed PMem mappings, intended as a donor for integrated M3JDK21 runtime services. It is not a universal persistent-memory or LLM inference runtime.

- `com.synexia.m3.pmem.PmemSdk.open()` / `PmemRegion`: long-offset mapping, read/write/persist/close, explicit selected backend and persistence granularity.
- `PmemTransfer.copy(...)` / `equalRange(...)`: direct native mapped-region transfer and equality, with bounded spans and ordered owner leases. No intermediate Java arrays. Caller separately persists destination bytes.
- `PmemSourceSlices`: read-only, primitive offset/length/SHA-256 views over a packed M3File-owned source image; one bounded SHA-verified file materialization for the existing Chrome/M3Scale one-call workflow. No new file index or model scheduler.
- Native C uses page-aligned POSIX `mmap` + `msync` + `fdatasync`. Optional **libpmem2** map and `persist` function when installed at build time; no unverified cache-flush instructions.
- No native address or `DirectByteBuffer` is returned to Java callers. Fair Java read/write leases synchronize close with operations.
- Fail-closed recipe-generated `PmemRegion` API; manifest, Maven phases, smoke tests, JMOD and jlink.

## Build

```bash
bash scripts/test-recipe-fail-closed.sh
bash scripts/test-transfer-recipe.sh
bash scripts/test-source-slices-recipe.sh
bash scripts/build-and-test.sh
bash scripts/check-ubsan.sh
mvn -f pom.xml verify  # where Maven is installed
```

Set `M3_USE_PMDK=off` for POSIX-only build, or `M3_USE_PMDK=required` to require installed libpmem2 development headers and linker libraries. Actual PMDK path **requires separately executed hardware-backed tests**.

For host discovery use `bash scripts/probe-hardware.sh` (read-only). For the Java API and limitations, see [docs/API_CONTRACT.md](docs/API_CONTRACT.md). Algorithm donor evaluations are in [recipes/DONOR_CATALOGUE.tsv](recipes/DONOR_CATALOGUE.tsv).

This is a foundation for connecting the merged quantized kernels to canonical M3File/DOOPS leases and PMem App Direct. It does not manage namespace creation, address-space interning, NUMA placement, GPU execution, model KV tiering or complete trained-model token generation.

See [docs/TRANSFER_CONTRACT.md](docs/TRANSFER_CONTRACT.md) for cross-mapping alias rejection, JNI ownership ordering and durability semantics.

See [docs/SOURCE_SLICES_CONTRACT.md](docs/SOURCE_SLICES_CONTRACT.md) for 20,000-entry/1GB sparse-image geometry, snapshot lease requirements and fail-closed limits.
