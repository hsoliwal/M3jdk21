<!-- SPDX-License-Identifier: Apache-2.0 -->
# jdk.m3.optane (module org.m3jdk.optane): Memory Mode planning, mapped-read advice and the PMem region core

Copyright 2026 Hitesh Soliwal and contributors.

Maven `com.m3:m3-optane:0.1.0-SNAPSHOT` (a default module of `m3/pom.xml`), Java 21, no dependency outside
`java.base`, two optional Linux JNI libraries under `src/main/native` (`m3optane`, `m3pmemsdk`; CMake project with the
option `M3_OPTANE_PMDK`, default OFF). Every FILE and planning path is exact without them.

## What this delivers

An independent Apache-2.0 named module, modular JAR and (Linux JNI enabled when built)
JMOD. It is **opt-in**, not a patched HotSpot or an automatic optimization of every
String/collection. Source generation of the planning owners belongs to the Synexia recipe crate
`synexia-openrewrite-recipes/recipes/optane-list-handoff-20261015`; the PMem region family is
relocated by `recipe/pmem-region-core-20261106` (below).

- MemoryModeProfile: explicitly operator-declared DRAM/PMem capacities. Never adds
  DRAM cache to OS-visible capacity and never claims hardware/BIOS verification.
- ScanPlan: lazy long-offset range traversal, at most 64 MiB per window, O(1)
  planner state, ordered callbacks and progress/cancellation. Window budgets account
  for the caller's worker and temporary-copy counts. The caller owns payloads,
  scheduling, CPU binding and source/publication authority.
- MappedReadAdvice: optional Linux JNI NORMAL/SEQUENTIAL/RANDOM/WILL_NEED advice
  on existing read-only mapped buffers. Page alignment is inward, ranges are
  validated in Java AND native code, buffers/cursors remain unchanged, and no pointer
  survives a call. Errors/unavailable native code are explicit, not fake success.
- Diagnostics launcher: prints a declared profile and native ABI availability, not
  a hardware detection report or a performance claim.
- The PMem region core (`M3PmemSdk`, `M3PmemRegion`, `M3PmemMapping`, `M3PmemTransfer`, `M3PmemSearch`,
  `M3PmemAccess`, `M3PmemBackend`, `M3PmemGranularity`, `M3PmemCapabilities`, `M3NativePmem`, `m3pmemsdk.c`):
  a low-level, fail-closed, file-backed mapping API over an existing regular file with 64-bit offsets.

| Owner | Role |
|---|---|
| `M3PmemSdk` | `capabilities()` (library loadability, never hardware proof); `open(path, offset, length, access, backend)` over an existing regular file, final symlink rejected, FILE identity captured across open |
| `M3PmemRegion` | the region contract: `length`, `fileOffset`, `path`, `access`, `backend`, `persistenceGranularity`, `isOpen`, `read`, `write`, `persist`, `close` (idempotent, waits for in-flight calls) |
| `M3PmemMapping` | the private owner: one fair `ReentrantReadWriteLock` lease authority over a native handle or a `FileChannel`; FILE positional I/O; the transfer atoms with ordered dual leases, memmove direction and file-identity alias refusal; streaming KMP with a per-thread 64 KiB window; sorted little-endian int64 bounds; `withReadLease` for multi-window consumers |
| `M3PmemTransfer` | `copy`, `equalRange`, `copyIfDifferent` between SDK regions; no pointer, view or array escapes |
| `M3PmemSearch` | `indexOf` (first exact byte sequence, `MAX_PATTERN_BYTES` 1 MiB), `lowerBoundLongLE`, `upperBoundLongLE` |
| `M3PmemBackend` | `AUTO` (PMDK when compiled, else POSIX), `POSIX` (Linux mmap/msync/fdatasync, granularity PAGE), `PMDK` (libpmem2, compile-time optional, fails closed when absent), `FILE` (Java `FileChannel`, no JNI, every OS) |
| `M3NativePmem`, `m3pmemsdk.c` | the 14 JNI exports `Java_jdk_m3_optane_M3NativePmem_*`; `System.loadLibrary("m3pmemsdk")` at class initialization, so the FILE backend never loads it |

The contracts are in `docs/` (`API_CONTRACT.md`, `TRANSFER_CONTRACT.md`, `SEARCH_CONTRACT.md`,
`SORTED_LONG_SEARCH.md`, relocated donor text with the module statements as declared rows).

No allocator, arena, new interner, corpus store, forced unmap, persistence protocol,
GC policy, CPU-affinity scheduler or automatic source hook is added. `persist()` is not atomic and
not a transaction. There is no dependency on Synexia at runtime and no change to java.base.

## Memory Mode versus App Direct

On the current T7920 Memory Mode design, DRAM is a CPU-managed cache in front of
volatile PMem. Ordinary Java heap, native allocations and cached file pages already
use OS-visible memory. This module cannot reserve hidden DRAM, allocate an object
into it, make PMem persistent, or enlarge the hardware cache.

Locality and a bounded active set are the optimization hypothesis. File advice is
kernel page-cache/readahead policy, **not** an Intel DRAM-cache instruction. Repeated
index probes need a different hint from a one-pass scan; neither is universally faster.

App Direct/PMDK: `m3pmemsdk.c` carries the libpmem2 backend behind `M3_HAVE_LIBPMEM2`, compiled only with the
CMake option `M3_OPTANE_PMDK=ON` against the build host's `pkg-config libpmem2`. **libpmem2 (PMDK) is BSD-3-Clause,
it is never required, no PMDK source or binary is in this tree or shipped by the JMOD** (`legal/NOTICE`); the gates
of this module ran without it. The Synexia PMDK owners (MIndexPmem2Region -> ShardStager -> Tier/Corpus,
LangDexPmemImageSet) are retained there. There is no claim that DAX, App Direct, NUMA binding, power-loss recovery or
direct GPU access to PMem has been implemented/verified here. Do not change BIOS mode or reformat PMem namespaces to
run these tests.

## Donor and relocation (recipe, never by hand)

| Item | Value |
|---|---|
| donor | `hsoliwal/com.synexia` `m3-pmem-sdk`, develop `36827ee8a798ef6ce07f6fc55a2ac371b1382c06` (the merge of #16749, which restored the `NativePmem.java` blob `adb94eb2f091` that the superset landing had cut to 11 natives; `PROVENANCE.tsv`, all rows `base_kind=DEVELOP`); family commits `c499b6e38b0`, `3fe3997b800`, `cd52a10a650` (2026-11-01), `489b43816dd`, `4fdaa292540` (2026-11-02) |
| recipe | `python -I m3/optane/recipe/pmem-region-core-20261106/relocate.py --donor-git <clone> --donor-rev 36827ee8a798 [--check]` (`RELOCATE_PMEM_REGION_CORE` v2): header, package line, imports, M3 type names, JNI prefixes and declared TEXT rows; everything else byte for byte; one `FLLDCIM: RELOCATE` anchor per file; idempotent |
| map, provenance | `recipe/pmem-region-core-20261106/RELOCATION_MAP.tsv` (22 FILE rows, 2 PACKAGE, 14 TYPE, 2 EXTERNAL, 2 JNI, 4 TEXT, SKIP and DROP rows with reasons), `PROVENANCE.tsv` (donor commit, blob, sha256 pairs, `body_match`, `body_match_renamed`, `base_kind`) |
| naming | `python -I m3/tooling/jdk-naming/jdk_naming.py apply --root . --only m3/optane/`: 0 moves, 0 rewrites, 0 refusals |
| gates | `recipe/pmem-region-core-20261106/GATES.tsv`, lessons in `RECIPE_MEMORY.tsv` |

### Census pass-1 remainder: the PMem tensor accelerator and the CPU-list parser (`recipe/pmem-accelerator-20261107`)

<!-- FLLDCIM: RELOCATE - recipe/pmem-accelerator-20261107 (census L008 + L047): the non-exported package jdk.internal.m3.optane gains M3PmemAccelerator (JNI library m3pmemaccel) and M3NumaTopology -->
| Item | Value |
|---|---|
| `jdk.internal.m3.optane.M3PmemAccelerator` (L008) | read-only `mmap` of a tensor file, FS-DAX attribute through `statx`, NUMA pin of the calling thread, bounded signed-Q8 x FP32 matrix-vector kernel folded in strict FP32 column order (scalar/AVX2 dispatch; the AVX-512 kernel of crate `m3-pmem-ordered-avx512-20261030` is HOLD until an AVX-512 host runs it); JNI library `m3pmemaccel` (CMake target, `src/main/native/m3pmemaccel.c`); NEC with the SDK's `M3PmemRegion` family (same shape, other contract), nothing of the SDK edited |
| `jdk.internal.m3.optane.M3NumaTopology` (L047) | the strict one-pass Linux CPU-list parser `parseCpuList` (`0-3,7,10-11`, optional leading `+`, refusals for empty elements, inverted ranges, indexes above 65535 and bad separators); `discover()`/`nativeFeatures()` of the donor are HOLD (they need the accelerator jmod family's JNI and a multi-node host) |
| oracles | `M3PmemAcceleratorSelfTest` (donor, 2,048 FP32 column-order cases, 64-row tile boundaries, > 2 GiB mapping, FS-DAX gate) through `M3PmemAcceleratorJUnitTest` under `-Pm3-optane-native`; `M3NumaTopologyCpuListTest` (hand-derived table and a reference-model differential) on every build |
| recipe | `python -I m3/optane/recipe/pmem-accelerator-20261107/relocate.py --donor-git <clone> --donor-rev 56cbdc631259 [--check]` (`RELOCATE_CENSUS1_REMAINDER` v4) and `python -I m3/optane/recipe/pmem-accelerator-20261107/apply.py [--check]` for the three edited files (this section, the CMake target, the surefire filter); gates in `GATES.tsv`, decisions in `DECISION.tsv` |

## Build and use

| Step | Command |
|---|---|
| compile, test, jar | `JAVA_HOME=<JDK 21> mvn -B -ntp -f m3/optane/pom.xml package` (`-Xlint:all -Werror`, `proc none`; surefire runs the FILE oracle lanes and the Java-path replay on every OS; the native lanes are filtered) |
| native lanes | build the libraries, then `mvn -Pm3-optane-native -Dm3.optane.library.path=<build dir> -f m3/optane/pom.xml test` (Linux; `-Xcheck:jni`), or the JUnit console launcher over `target/classes:target/test-classes` |
| libraries | `cmake -S m3/optane/src/main/native -B <build> && cmake --build <build>` (JDK 21 `javac` and JNI headers; `-DM3_OPTANE_PMDK=ON` adds the libpmem2 backend from the build host) |
| from the control reactor | `mvn -f m3/pom.xml -pl optane package` |
| JMOD / jlink verifier | `bash m3/optane/verify.sh` (Linux x86_64; `M3_OPTANE_NO_NATIVE=1` for Java only; `M3_OPTANE_BUILD` selects the output) or `mvn -Pm3-optane-jmod -f m3/optane/pom.xml verify` |
| differential | `bash m3/optane/recipe/pmem-region-core-20261106/differential.sh <donor clone> <donor rev> <scratch> [operations]` after `package` |
| native matrix (Linux) | `bash m3/optane/recipe/pmem-region-core-20261106/native-matrix.sh jdk.m3.optane M3 jdk.m3.optane m3/optane/src/main/native/m3pmemsdk.c m3/optane/src/test/native/M3PmemKmpPrefix32Oracle.c <scratch> <operations> <the ten owners> <M3PmemAtomCases> <M3PmemDump>` |
| all Java gates | `bash m3/optane/recipe/pmem-region-core-20261106/verify.sh <donor clone> <donor rev> <scratch>` |

```java
try (M3PmemRegion model = M3PmemSdk.open(Path.of("/pmem/models/weights.bin"), 0, 8192,
        M3PmemAccess.READ_ONLY, M3PmemBackend.FILE)) {   // FILE needs no library; AUTO/POSIX need libm3pmemsdk
    byte[] header = new byte[32];
    model.read(0, header, 0, header.length);
}
```

Consumers depend on `com.m3:m3-optane` and `requires org.m3jdk.optane;`. The libraries are optional:
`System.loadLibrary("m3optane")` / `System.loadLibrary("m3pmemsdk")` at class initialization, `java.library.path`
names their directory. `M3_JDK=/path/to/a/complete/matched/M3JDK21` selects the product JDK for `verify.sh`; stock
Java 21 software tests do not prove a source-built M3JDK image.

## Existing owners retained; next integration gates

| Existing owner | Disposition |
|---|---|
| java.lang.M3StringOwner/Atom/Pool/Tuple, M3MappedStringBacking | No changes; opt-in call sites pending target lease/API proof |
| MIndexPmem2Region / mindex_pmem2_jni.c | Retain canonical native PMDK mapping/persistence ownership (Synexia) |
| MIndexPmem2Tier/Corpus | Retain verified SHA roots, fallback, shard and failure-domain decisions |
| LangDexCompiledImageSet / LangDexPmemImageSet | Retain immutable source images and verified lazy reopen |
| MIndexProgressMonitor | Adapt small dependency-free contract; no new global progress owner |
| M3 GPU precompute | Existing detached bounded batches and CPU parity remain required |
| PmemSourceImage/Slices/PathIndex, PmemMultiPattern, PmemTaskLog/Replay | Next leaves (plan fs-pmem ranks 3-5), SKIP rows of the map |

The target mapping is recorded in PORT-MAPPING.pending.json (the relocated owners as RELOCATED rows). It is still a
**PENDING optional-module admission**, not a new authority that supersedes the canonical m3/docs/name-mapping.json or
advances String -> arrays -> collections -> compiler receiving stages. Full registry admission and automatic callers
remain open.

## Qualification, measurements and limitations

The Maven build compiles Java 21 with every lint as an error and runs the donor's FILE oracle and a Java-path replay;
the native matrix compiles C17 with `-Wall -Wextra -Wpedantic -Werror`, checks the 14 exports and runs the 19 atoms,
the oracle lanes and 100,000 seeded operations under `-Xcheck:jni` in three JIT modes, byte-identical between the
donor and the relocated family; the JMOD verifier checks normal/interpreter behavior, Java-only/native paths and JNI
under -Xcheck:jni, creates the JAR/JMOD, then runs the same contracts from the linked image. The 4 TiB test
enumerates logical ranges; it **does not allocate 4 TiB**. Mapped-file tests use ordinary temporary files (sparse
above 2 GiB where the oracles say so), not Optane.

Actual run logs and machine/tool versions belong in the delivery evidence (GATES.tsv). No stub
PMDK/JUnit/device library is used. Real T7920 CPU/NUMA/cache-counter and performance qualification remain
separate gates. No speedup claim is made. The native artifacts are machine/platform-specific and should be
rebuilt for the user's deployment distribution.

Hardware acceptance should compare the same corpus and query order, 1/2/4/8/... workers,
local versus remote NUMA where verified, 64/128/256/512 GiB **real** DRAM configurations,
and normal/random/sequential hints. Record cold preparation separately from repeated
queries, p50/p95 latency, throughput, GC, RSS/native/page-table usage and actual memory
controller counters. A process restart alone does not establish cold page/cache state.

## Primary references

- https://www.intel.com/content/www/us/en/support/articles/000055901/technologies/memory-and-storage/intel-optane-persistent-memory.html
- https://docs.oracle.com/en/java/javase/21/docs/specs/man/jmod.html
- https://docs.oracle.com/en/java/javase/21/docs/specs/man/jlink.html
- https://man7.org/linux/man-pages/man2/madvise.2.html
- https://man7.org/linux/man-pages/man2/mmap.2.html
- https://pmem.io/pmdk/libpmem2/ (BSD-3-Clause; optional, never shipped)
- https://github.com/hsoliwal/com.synexia/blob/develop/synexia-mindex/native-jni/PMDK_SOURCE_SDK.md
- https://github.com/hsoliwal/M3jdk21/blob/master/m3/docs/m3jdk21-porting-invariant.md
