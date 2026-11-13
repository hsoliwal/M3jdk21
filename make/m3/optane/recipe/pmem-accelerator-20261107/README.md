<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# recipe pmem-accelerator-20261107: the PMem tensor accelerator and the CPU-list parser relocated into jdk.internal.m3.optane

Census pass-1 leaves **L008** and **L047** (`C:/m3-superset/conv/record/census/LEAVES_PROPOSED.tsv`; ledger row 110, lane
com-synexia-0e; qualification record `m3/qualification/census1-remainder-20261107a/`): the read-only PMem tensor
accelerator of `hsoliwal/com.synexia` (`synexia-m3-jmods/accelerator-pmem` and its JNI library) and the strict Linux CPU-list
parser of the recipe crate `m3-pmem-16source-numa-convergence-20261031` become part of `m3/optane` (module
`org.m3jdk.optane`) by recipe, in the NON-EXPORTED package `jdk.internal.m3.optane`. Hardware the gate machine lacks
(AVX-512, a second NUMA node) is HOLD, not ported: only the Java and self-test parts that run here were relocated.

## What was relocated

| Donor (develop `56cbdc631259`) | M3JDK (`m3/optane/`) | Role |
|---|---|---|
| `synexia-m3-jmods/accelerator-pmem/src/main/java/.../PmemAccelerator.java` (blob `fd3669994196`) | `src/main/java/jdk/internal/m3/optane/M3PmemAccelerator.java` | L008: `open(path, requireFsDax)` -> `Mapping` (synchronized, use-after-close refused), `read` with long bounds before allocation, `matVecQ8` (signed row-major Q8, one per-tensor scale, strict FP32 column order), `cpuFeatureMask`, `pinCurrentThreadToNode` |
| `synexia-m3-jmods/native/accelerator-pmem/m3pmem.c` (blob `6a49c7787c76`) | `src/main/native/m3pmemaccel.c` + CMake target `m3pmemaccel` | the JNI library: `mmap` PROT_READ, `statx` DAX attribute, scalar/AVX2 kernels (SIMD products, scalar-ordered fold, no FMA reduction), 64-float output tile, cpulist parse + `sched_setaffinity`; compiled `-ffp-contract=off -fno-fast-math` as the donor `build.sh` does |
| `.../PmemAcceleratorSelfTest.java` (blob `fbebe6786ee9`) | `src/test/java/jdk/internal/m3/optane/M3PmemAcceleratorSelfTest.java` | the donor oracle (`PASS m3-pmem Java21/JNI cases=7879`): 2,048 FP32 column-order cases, 64-row tile boundaries x 4 scales (raw bits), 44 random parities, > 2 GiB sparse mapping, close twice, FS-DAX gate |
| (M3JDK-authored) | `src/test/java/jdk/internal/m3/optane/M3PmemAcceleratorJUnitTest.java` | surefire entry of the oracle under `-Pm3-optane-native` (`-Dpmem.accelerator.native=true`, `libm3pmemaccel.so` on `java.library.path`); excluded by the default filter |
| recipe crate `.../NumaTopology.java.template` (blob `4972234a700a`, the crate's postimage `79e34f0fc305`) | `src/main/java/jdk/internal/m3/optane/M3NumaTopology.java` | L047: `parseCpuList(String) -> BitSet` (ranges, optional leading `+`, ASCII trim; refusals for empty elements, missing indexes, indexes above 65535, inverted ranges, bad or trailing separators) and the `Node` record; `discover()` and `nativeFeatures()` are DROP_METHOD rows (HOLD) |
| (M3JDK-authored) | `src/test/java/jdk/internal/m3/optane/M3NumaTopologyCpuListTest.java` | the reference model: the donor SelfTest's eleven parser checks as a hand table, an edge table, a 4,000-set canonical-formatter round trip, a 200,000-string regex-grammar fuzz (accept/refuse and sets compared) |

Edited existing files (`apply.py`, hash-pinned hunks in `hunks/L008.edits`, `EDITS.tsv`): `src/main/native/CMakeLists.txt`
(the `m3pmemaccel` target), `pom.xml` (the default surefire filter excludes `M3PmemAcceleratorJUnitTest`; the native profile
passes `-Dpmem.accelerator.native=true`), `README.md` (one section). `module-info.java` is unchanged (exports
`jdk.m3.optane` only; the jar `contains jdk.internal.m3.optane`).

## Decisions (`DECISION.tsv`, `FOSS_REUSE_DECISION.tsv`)

- **Owner (L008)**: the SDK's `M3PmemRegion` family is a read-write persistent-memory region (FILE/POSIX/PMDK backends,
  transfers, search); the accelerator maps a read-only tensor file, classifies DAX, pins a thread and runs a kernel.
  Same shape, other contract: **NEC**, both kept; the SDK library `m3pmemsdk` and its exports are untouched.
- **HOLD (hardware)**: the ordered-avx512 crate's `m3pmem.c` postimage (`d912c43c3d47`, `dot_avx512_ordered`) and its
  self-test postimage (`f2c2188dc578`): the gate machine (i9-14900HX: avx2, fma, no avx512*) cannot execute the kernel.
  The live `m3pmem.c` is that chain's preimage, so the hunk applies to `m3pmemaccel.c` on an AVX-512 host. The q8 and
  q8-ordered-reduction chains are SUPERSEDED (their preimages predate develop's #14554/#14573 corrections).
- **HOLD (L047)**: `discover()`/`nativeFeatures()` and the rest of the 465-line jmod SelfTest need the accelerator jmod
  family (`CpuAffinity`, `NativeBindings`, `NumaExecutor`, `PmemRegion`, `GgufInspector`; not on master) and a multi-node
  host (one NUMA node here). Only the parser and its eleven checks run.
- **Package**: `jdk.internal.m3.optane` (internal form of the naming standard; the donor Javadoc calls the class a
  "Candidate for a future jdk.m3.accelerator.pmem product JMOD": no public API yet), as `recipe/pmem-gram-index-20261106`
  did for the campaign package.

## Recipe

`python -I m3/optane/recipe/pmem-accelerator-20261107/relocate.py --donor-git <clone> --donor-rev 56cbdc631259 [--check]`
(`RELOCATE_CENSUS1_REMAINDER` v4: pmem-gram-index v3 plus DROP_METHOD rows, one-line methods included, and a
module-derived recipe path; the same text as `m3/ast/recipe/null-flow-20261107/relocate.py`). Map: 4 FILE rows, 2 PACKAGE,
3 TYPE, 1 JNI (`Java_..._PmemAccelerator_` -> `Java_jdk_internal_m3_optane_M3PmemAccelerator_`), 1 TEXT (the library
name), 2 DROP_METHOD, 3 MERGE, SKIP rows for the HOLD/SUPERSEDED chains and the crate records. `PROVENANCE.tsv`: 4 rows
`body_match_renamed=TRUE` (`M3NumaTopology` `dropped_lines=25`). Then
`python -I m3/optane/recipe/pmem-accelerator-20261107/apply.py [--root DIR] [--check]` for the three edited files.

## Gates (2026-11-07, `GATES.tsv`)

| Gate | Result |
|---|---|
| recipes fixed point | `relocate.py --check` files=4 changed=0; `apply.py --check` rules=3 changed=0; also after `jdk_naming apply` |
| `mvn -f m3/optane/pom.xml clean package` (Windows, Java path) | BUILD SUCCESS, 0 compiler warnings; 61 tests, 0 failures, 4 skipped (master 57/0/4): + `M3NumaTopologyCpuListTest` 4 |
| untouched class files | 45/45 main and 40/40 test byte-identical with the master build |
| native gate (WSL Ubuntu 24.04, gcc 13.3.0, Temurin 21.0.13; `native-gate.sh`) | donor and relocated families: 8 JNI exports each, `PASS m3-pmem Java21/JNI cases=7879` under `-Xcheck:jni -XX:-UsePerfData -Xmx64m`, byte-identical in default, `-Xint` and `-XX:TieredStopAtLevel=1` and between the families (sha256 `98f4f98a2a51...`), temp files under `/mnt/s/m3-scratch/eb-conv/c1rem/tmp` |
| `javap -p` / `-p -c` parity (`javap_parity.sh`) | see `GATES.tsv` (owner, `Mapping`, `DaxState`, the self-test from source, `Node`, the `parseCpuList` section) |
| mutants (`mutants.py`) | 6/6 parser mutants killed by `M3NumaTopologyCpuListTest` (inverted range, 65536 bound, trailing separator, leading plus, any separator, exclusive high) |
| naming | `jdk_naming.py apply --only m3/optane/`: fixed point 0/0/0 (after the CMake comment and the `apply.py` prose stopped spelling donor kebab names, which the KEBAB rule rewrites in scripts) |

## Files

| File | Purpose |
|---|---|
| `relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | the recipe (v4), its map and its pins |
| `apply.py`, `hunks/L008.edits`, `EDITS.tsv` | the hash-pinned edits of the three existing files |
| `donor.sh`, `native-gate.sh`, `javap_parity.sh`, `mutants.py` | donor extraction, the Linux native gate (one family), javap parity, the parser mutants |
| `DECISION.tsv`, `FOSS_REUSE_DECISION.tsv`, `GATES.tsv`, `RECIPE_MEMORY.tsv` | decisions, donor-first table, the gate record, the lessons |
