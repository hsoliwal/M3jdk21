# VCPU_DONOR_INVENTORY

Read-only inventory. Date: 2026-10-12. No repository modified, nothing built.

## 0. Provenance of inspected bytes

| Item | Value |
|---|---|
| Donor module | `hsoliwal/synexia-tornadovm` `m3/cpu-gpu` (artifactId `cpu-gpu-rv32im`, `pom.xml:9`) |
| Pinned commit | `3bf826714a48d7fdf346ffde8a0fc25c525572c5` |
| Local copy read | `K:\m3-work\tmp\sdk-first-party\tv-cpugpu\` |
| Staged copy | `K:\m3-work\tmp\sdk-first-party\stage\m3\vendor\synexia\synexia-tornadovm\m3\cpu-gpu\` — `diff -rq` against `tv-cpugpu` = IDENTICAL |
| Blob proof | `gh api repos/hsoliwal/synexia-tornadovm/contents/m3/cpu-gpu/src/main/java/com/synexia/m3/cpugpu?ref=3bf82671…` SHA-1s equal `tv_cpugpu_tree.tsv` and local `git hash-object` (e.g. `Rv32iKernel.java f3c5289c…`, `TornadoRv32iSession.java d8b09792…`) |
| Licence | every cpu-gpu file carries `SPDX-License-Identifier: Apache-2.0` line 1 |
| Tornado dependency | `io.github.beehive-lab:tornado-api:6.1.0-jdk21` (`pom.xml:16,22-26`); JUnit 5.11.4 test-only; `-Dm3.gpu` gate (`pom.xml:18,55`) |

Below, paths without prefix are relative to `m3/cpu-gpu/src/main/java/com/synexia/m3/cpugpu/` (main) or `.../src/test/java/com/synexia/m3/cpugpu/` (test).

## 1. What the vCPU is (cpu-gpu-rv32im)

### 1.1 Files

| File | LOC | Role |
|---|---:|---|
| `Rv32i.java` | 86 | ABI constants: register count, state slots, status codes, trap causes, config slots, `XM3_HALT`, immediate decoders |
| `Rv32iBatch.java` | 256 | Host SoA state (`int[] memory/registers/state`) shared by CPU oracle and GPU session |
| `CpuRv32iEngine.java` | 478 | Independent heap interpreter = correctness oracle (Java 21 arrow-switch) |
| `Rv32iKernel.java` | 520 | TornadoVM kernel; GPU-friendly re-implementation over `KernelContext` + `IntArray` (classic `switch`/`if` chains) |
| `TornadoRv32iSession.java` | 116 | TaskGraph/ExecutionPlan/WorkerGrid wiring, resident device state, slice execution |
| `Rv32iBulkExecutor.java` | 166 | CPU or verified-Tornado bulk facade + SHA-256 state roots |
| `Rv32iBulkReceipt.java` | 50 | Content-addressed receipt record (mode, cores, budget, localWork, roots) |
| `Rv32iAssembler.java` | 318 | Tiny encoder for all RV32IM forms + fence/ecall/ebreak/halt; range checks |
| `Elf32RiscVLoader.java` | 115 | ELF32 LE EM_RISCV ET_EXEC PT_LOAD loader into one core's RAM |
| `Rv32iDemo.java` | 87 | CLI `cpu|gpu|verify [cores] [budget]`; sum 1..100 program |
| `recipe-bulk-provider/` | — | OpenRewrite ScanningRecipe that seals `Rv32iBulkExecutor/Receipt/ExecutorTest` by SHA-256 (`manifest.tsv`); no Tornado import |

### 1.2 ISA subset

- **RV32I 2.1 + M 2.0**, plus FENCE / FENCE.I, ECALL, EBREAK (README.md:19, DESIGN.md:35-39).
- Opcode dispatch (both engines): `0x37` LUI, `0x17` AUIPC, `0x6F` JAL, `0x67` JALR (funct3 must be 0), `0x63` BEQ/BNE/BLT/BGE/BLTU/BGEU, `0x03` LB/LH/LW/LBU/LHU, `0x23` SB/SH/SW, `0x13` OP-IMM (ADDI/SLTI/SLTIU/XORI/ORI/ANDI/SLLI/SRLI/SRAI), `0x33` OP (ADD/SUB/SLL/SLT/SLTU/XOR/SRL/SRA/OR/AND; funct7=`0x01` → MUL/MULH/MULHSU/MULHU/DIV/DIVU/REM/REMU), `0x0F` MISC-MEM (funct3 0/1), `0x73` SYSTEM (only exact `0x00000073` ECALL, `0x00100073` EBREAK). Kernel: `Rv32iKernel.java:73-245`; oracle: `CpuRv32iEngine.java:71-235`.
- **Custom**: `XM3_HALT = 0x0000000B` (custom-0 semihosting halt; exit code = x10/a0) — `Rv32i.java:44`, `Rv32iKernel.java:49-58`, `CpuRv32iEngine.java:47-56`. Retired, PC += 4, status HALTED.
- M corner cases per spec: div-by-zero → -1 / dividend; `MIN_VALUE / -1` → MIN_VALUE, rem 0 (`Rv32iKernel.java:398-434`).
- Not implemented (explicitly): C (compressed), A, F/D, Zicsr/CSRs, privilege modes, interrupts, MMU, devices (README.md:23, DESIGN.md:147-159). FENCE/FENCE.I are decoded no-ops (`CpuRv32iEngine.java:217-223`).
- Traps use RISC-V exception cause numbers 0-8 (`Rv32i.java:24-33`); `TRAP_NONE = -1`. Illegal instruction → cause 2, tval = raw instruction (`Rv32iKernel.java:247-249`). A faulting instruction is not retired (DESIGN.md:31).

### 1.3 State layout (flat structure-of-arrays ABI)

Four `IntArray`s on device / four `int[]` on host (`Rv32iBatch.java:15-17,29-31`; DESIGN.md:11-18):

| Array | Shape | Indexing |
|---|---|---|
| `memory` | `coreCount * memoryWordsPerCore` int words | core RAM = `[core*wordsPerCore, (core+1)*wordsPerCore)` (`Rv32iBatch.java:194-196`); little-endian bytes packed in ints |
| `registers` | `coreCount * 32` | `core*32 + r`; x0 read as 0 and rewritten 0 after every retire (`Rv32iKernel.java:258,436-444`) |
| `state` | `coreCount * 6` | `STATE_PC=0, STATUS=1, RETIRED=2, TRAP_CAUSE=3, TRAP_VALUE=4, EXIT_CODE=5`, stride 6 (`Rv32i.java:10-16`) |
| `config` | 3 ints | `CORE_COUNT=0, MEMORY_WORDS_PER_CORE=1, INSTRUCTION_BUDGET=2` (`Rv32i.java:35-38`) |

- PC is a byte address within the core's private RAM; fetch requires 4-byte alignment and `0 <= pc <= memBytes-4` (`Rv32iKernel.java:39-46`).
- Memory model: private, flat, physical, per-core; no sharing, no atomics (DESIGN.md:49-53). Byte/half loads/stores are read-modify-write on the containing int word with mask/shift (`Rv32iKernel.java:450-474`). Natural alignment enforced for H/W; misaligned → cause 4/6; out-of-range → cause 5/7.
- Status machine: `READY=0, HALTED=1, TRAPPED=2, YIELDED=3` (`Rv32i.java:18-21`); READY→YIELDED on budget exhaustion, →HALTED on Xm3halt, →TRAPPED on fault; HALTED/TRAPPED cores are skipped on next slice (`Rv32iKernel.java:29-32`; DESIGN.md:24-31).
- Host batch constructor sets `TRAP_CAUSE=-1` for every core (`Rv32iBatch.java:32-34`); `setProgramCounter` validates and resets status/trap (`Rv32iBatch.java:70-79`).

### 1.4 Parallelism

- **One GPU work-item = one complete virtual core** (README.md:8-15; DESIGN.md:43-47; `Rv32iKernel.java:8,16`). Core id = `context.globalIdx`; guard `core >= coreCount → return` (`Rv32iKernel.java:16-20`).
- Number of vCPUs = `batch.coreCount()` (any positive int; host arrays sized with `Math.multiplyExact`). Global work = `coreCount` rounded up to `localWorkSize` (default 64) (`TornadoRv32iSession.java:22,57-59,109-115`).
- Instances used: Demo default 1024 cores × 4096 B RAM × budget 1024 (`Rv32iDemo.java:8-10`); GPU differential test 256 cores × budget 1024 (`TornadoRv32iDifferentialTest.java:13-18`); bulk test 64 (CPU) and 256 (GPU, localWork 64).
- No barriers, no local memory, no atomics, no inter-core communication. Warp divergence accepted (DESIGN.md:47).
- CPU oracle runs cores **sequentially** in a host `for` loop (`CpuRv32iEngine.java:13-15`) — no host-side parallelism today.

### 1.5 Instruction dispatch

- Pure fetch–decode–execute **interpreter loop** per work-item: `for (step = 0; step < instructionBudget; step++)` (`Rv32iKernel.java:38`), word fetch `memory.get(memoryBase + (pc>>>2))` (`:48`), XM3_HALT fast check (`:49`), field extract (`:60-65`), single `switch (opcode)` with nested `if/else` on funct3/funct7 (`:73-245`), OP-IMM/OP delegated to `executeImmediate`/`executeRegister` (`:269-396`).
- Locals hold `pc`, `retired`; architectural state persisted to `state` after **every** retired instruction (`:256-260`) and at yield (`:263-266`). Register file lives in global `IntArray` (every read/write is a global memory op).
- Kernel avoids Java features Tornado cannot compile: no arrow-switch, no objects, no exceptions; all helpers are `private static` pure int functions; immediate decoders duplicated into the kernel class (`:483-519`) instead of calling `Rv32i` statics.
- Oracle uses Java 21 arrow-switch and nested `switch(funct3)` — different code shape on purpose (README.md:4,31).

### 1.6 Mapping onto TornadoVM

`TornadoRv32iSession` (`TornadoRv32iSession.java:19-116`):

1. Wrap host arrays off-heap: `IntArray.fromArray(batch.rawMemory()/rawRegisters()/rawState())` (`:44-46`), `IntArray.fromElements(coreCount, wordsPerCore, 1)` for config (`:47`).
2. `new KernelContext()` (`:49`).
3. `new TaskGraph("m3rv32i")` (`:50`)
   - `.transferToDevice(DataTransferMode.FIRST_EXECUTION, memory, registers, state)` — state stays resident (`:51`)
   - `.transferToDevice(DataTransferMode.EVERY_EXECUTION, config)` — budget per slice (`:52`)
   - `.task("execute", Rv32iKernel::execute, kernelContext, memory, registers, state, config)` — `TaskWithParameters`/`Task5` functional shape (`:53`)
   - `.transferToHost(DataTransferMode.UNDER_DEMAND, memory, registers, state)` (`:54`)
4. `graph.snapshot()` → `ImmutableTaskGraph` (`:55`).
5. `new WorkerGrid1D(globalWork)`, `grid.setLocalWork(localWorkSize,1,1)`, `new GridScheduler("m3rv32i.execute", grid)` (`:57-60`).
6. `new TornadoExecutionPlan(immutable).withGridScheduler(scheduler)` (`:61`).
7. Slice: `config.set(INSTRUCTION_BUDGET, n)`; `plan.execute()` → `TornadoExecutionResult`; `result.transferToHost(registers, state)`; copy back via `IntArray.toHeapArray()` (`:64-73`).
8. RAM download only on demand: `downloadMemory()` / `synchronizeAll()` call `lastResult.transferToHost(memory[, registers, state])` (`:79-91`).
9. `close()` → `plan.close()` throws `TornadoExecutionPlanException` (`:99-101`).

Design intent: RAM resident across slices, registers/state pulled each slice for scheduling, full RAM D2H explicit (DESIGN.md:57).

### 1.7 Verification/receipt layer

- `Rv32iBulkExecutor.executeCpu` → receipt mode `CPU` (`Rv32iBulkExecutor.java:24-40`).
- `executeVerifiedTornado(batch, budget, localWork)`: copies batch, runs CPU oracle on the copy, runs Tornado session + `synchronizeAll`, `Arrays.equals` on registers/state/memory, else `IllegalStateException("CPU/Tornado mismatch …")`; emits `TORNADO_VERIFIED` receipt (`:42-82`).
- `stateRoot` = SHA-256 over length-framed domain `M3_RV32I_BATCH_STATE_V1`, coreCount, memBytes, registers, state, memory (`:84-93`); receipt root domain `M3_RV32I_BULK_RECEIPT_V1` (`:95-112`); `Rv32iBulkReceipt` compact constructor recomputes and rejects root mismatch (`Rv32iBulkReceipt.java:27-49`).

### 1.8 Tests and what they prove

| Test | file:line | Proves | Needs GPU |
|---|---|---|---|
| `Rv32iAssemblerTest.roundTripsImmediateEncodings` | `Rv32iAssemblerTest.java:10-23` | I/S/B/J immediate encode↔decode at boundaries (-2048, 2047, -4096, 1048574) | no |
| `…rejectsOutOfRangeOrMisalignedEncodings` | `:25-33` | assembler range/alignment/register/shift guards | no |
| `…emitsCanonicalFenceAndSemihostingEncodings` | `:35-42` | FENCE `0x0F`, FENCE.I `0x100F`, ECALL, EBREAK, XM3_HALT encodings | no |
| `CpuRv32iEngineTest.executesRv32iAndMArithmetic` | `CpuRv32iEngineTest.java:11-43` | ALU + M ops, x0 hard-wired, exit code | no |
| `…executesLoadsStoresAndSignedness` | `:45-72` | LB/LBU/LH/LHU/LW, SB/SH/SW, sign/zero extension, LE packing | no |
| `…executesBranchesJumpsAndBoundedSlices` | `:74-87` | YIELDED after budget, resume across slices to HALTED with 5050 | no |
| `…reportsIllegalAndMisalignedTrapsWithoutRetiringFaultingInstruction` | `:89-106` | illegal (cause 2, tval=insn), load misaligned (cause 4), faulting insn not retired | no |
| `…implementsMExtensionDivisionCornerCases` | `:108-129` | div/rem by zero, MIN_VALUE/-1 | no |
| `Elf32RiscVLoaderTest.mapsLoadSegmentZeroFillsBssAndRunsEntry` | `Elf32RiscVLoaderTest.java:13-33` | PT_LOAD mapping, BSS zero-fill, entry=64, run to exit 77 | no |
| `…rejectsWrongMachineAndOutOfRangeSegment` | `:35-45` | EM_RISCV check, segment bound check | no |
| `Rv32iBulkExecutorTest.cpuReceiptsAreDeterministicForEquivalentBatches` | `Rv32iBulkExecutorTest.java:13-27` | identical batches → identical receipts; 64 cores reach 5050 | no |
| `…rejectsInvalidBudgetsBeforeExecution` | `:29-38` | budget 0 / localWork 0 rejected before any execution | no (localWork check precedes Tornado) |
| `…tornadoModeRequiresFullCpuParityWhenHardwareGateIsEnabled` | `:40-55` | 256 cores, budget 1024, localWork 64 → TORNADO_VERIFIED receipt | yes (`Assumptions.assumeTrue(Boolean.getBoolean("m3.gpu"))`) |
| `TornadoRv32iDifferentialTest.matchesIndependentCpuOracleAcrossManyCores` | `TornadoRv32iDifferentialTest.java:10-25` | 256-core GPU run bit-equal to oracle (registers, state, RAM) | yes (`@EnabledIfSystemProperty m3.gpu=true`) |
| `M3CpuGpuBulkProviderRecipeTest` | `recipe-bulk-provider/src/test/...` | recipe replay idempotence on the 3 sealed files | no |

What is **not** proven by the tests: kernel correctness on any real device is only asserted when `-Dm3.gpu=true` runs on a TornadoVM device (DESIGN.md:69); no evidence of such a run is in the inspected bytes. Kernel coverage of individual opcodes is only through the sum-1..100 program (ADDI/ADD/BLT/halt) in the GPU tests — loads/stores/M/traps are covered only on the CPU oracle.

## 2. tornado-api surface used

Full table: `K:\m3-work\tmp\m3sdk-design\TORNADO_API_SURFACE.tsv` (43 rows + header; columns `symbol, kind, used_in, tornado_source_file, license`). Source-file line refs are into `K:\m3-work\tmp\sdk-first-party\dl-tornado-api-src\x\` (extracted `tornado-api-6.1.0-jdk21-sources.jar`). Licence classification of all 216 jar sources: `scratch\tornado_api_610_licenses.tsv` (script `scratch\lic_classify.py`).

### 2.1 Summary

- **Only two cpu-gpu files touch Tornado**: `Rv32iKernel.java` (imports `KernelContext`, `IntArray`) and `TornadoRv32iSession.java` (10 imports). Every other file (`Rv32i`, `Rv32iBatch`, `CpuRv32iEngine`, `Rv32iAssembler`, `Elf32RiscVLoader`, `Rv32iBulkReceipt`) is Tornado-free; `Rv32iBulkExecutor`/`Rv32iDemo` touch Tornado only through `TornadoRv32iSession`.
- **tornadovm-bulk has zero direct Tornado imports.** `Rv32iBulkExecutorAdapter.java:4-13` imports `com.synexia.m3.cpugpu.{Rv32iBatch,Rv32iBulkExecutor,Rv32iBulkReceipt}` and `jdk.internal.vm.parallel.{BulkExecution,BulkExecutor,BulkTask}`; Tornado is reached transitively at `:88-91` → `Rv32iBulkExecutor.executeVerifiedTornado` → `TornadoRv32iSession`. Its pom depends on `com.synexia.m3:cpu-gpu-rv32im:1.0.0-SNAPSHOT` (`pom.xml:18-22`), which pulls `tornado-api:6.1.0-jdk21`.
- No Tornado annotations are used (`@Parallel`, `@Reduce` absent). Execution style is the **KernelContext (explicit-index) API**, not the loop-parallel (`@Parallel`) API.
- KernelContext members used: **only `globalIdx`** (`Rv32iKernel.java:16`; declared `public final Integer globalIdx = 0;` `KernelContext.java:67`, a placeholder the Tornado JIT replaces with `get_global_id(0)`). Not used: `localIdx`, `groupIdx`, `*GroupSize*`, `localBarrier`, `globalBarrier`, `allocate*LocalArray`, atomics.

| Group | Symbols | Tornado file (6.1.0) | Licence |
|---|---|---|---|
| Kernel context | `KernelContext`, `new KernelContext()`, `.globalIdx` | `api/KernelContext.java` | Apache-2.0 |
| Off-heap array | `IntArray`, `fromArray`, `fromElements`, `get`, `set`, `toHeapArray`; implicit `TornadoNativeArray` | `api/types/arrays/IntArray.java`, `TornadoNativeArray.java` | Apache-2.0 |
| Graph | `TaskGraph(String)`, `transferToDevice`, `task(String, Task5, …)`, `transferToHost`, `snapshot`; `ImmutableTaskGraph`; implicit `TornadoFunctions.Task5` | `api/TaskGraph.java`, `ImmutableTaskGraph.java`, `common/TornadoFunctions.java` | Apache-2.0 |
| Transfer modes | `DataTransferMode.FIRST_EXECUTION/EVERY_EXECUTION/UNDER_DEMAND` (int constants 0/1/2) | `api/enums/DataTransferMode.java` | Apache-2.0 |
| Grid | `WorkerGrid1D(int)`, `setLocalWork(long,long,long)`, `GridScheduler(String, WorkerGrid)`; implicit `WorkerGrid`, `AbstractWorkerGrid` | `api/WorkerGrid1D.java`, `AbstractWorkerGrid.java`, `WorkerGrid.java`, `GridScheduler.java` | Apache-2.0 |
| Plan | `TornadoExecutionPlan(ImmutableTaskGraph...)`, `withGridScheduler` (returns `plan/types/WithGridScheduler`), `execute`, `close`; `TornadoExecutionResult.transferToHost(Object...)`; `TornadoExecutionPlanException` | `api/TornadoExecutionPlan.java`, `ExecutionPlanType.java`, `plan/types/WithGridScheduler.java`, `TornadoExecutionResult.java`, `exceptions/TornadoExecutionPlanException.java` | Apache-2.0 |

### 2.2 Licence facts for tornado-api 6.1.0 (216 `.java` in the sources jar)

| Licence header | Files |
|---|---:|
| Apache-2.0 (APT Group, University of Manchester) | 214 |
| GPL-2.0-only, **no Classpath exception** — `types/matrix/Matrix8x8Float.java` (Copyright 2026) | 1 |
| No header — `plan/types/WithCUDAGraph.java` | 1 |

Every symbol actually used by cpu-gpu is in an Apache-2.0 file. The GPL-2.0-only file is in the same artifact but is not referenced; it is the reason the jar as a whole cannot be vendored under an Apache-only rule. Prior evidence `sdk-first-party\stage-work\tornado_used_surface_licences.tsv` agrees (18 used files, all Apache; Matrix8x8Float flagged GPL-2.0-only). Note: `TornadoExecutionPlan` is a `sealed` class (`permits ExecutionPlanType`), so a clean-room M3 API cannot subclass it; it must be replaced, not wrapped by inheritance.

## 3. Existing JNI / native layer in M3jdk21

Worktree `K:/m3-work/wt/m3jdk21-c7`, HEAD `d287e3888792b8c3d9c0e8e14642e403f72e9293` (2026-10-12). Name inventory: `git ls-tree -r --name-only HEAD -- m3/` → 34,311 paths (`scratch\m3_names.txt`); JNI/native name hits outside `m3/vendor/third_party/` → 195 (`scratch\m3_jni_native_names.txt`). Line refs below are into `git show HEAD:<path>`.

### 3.1 Build-and-load mechanisms found

| # | Location | Purpose | Native build | Load | Status |
|---|---|---|---|---|---|
| A | `m3/ports/precompute/` (`M3SimilarityBatchNative.java`, `M3CodeTextSignalBatchNative.java`; `src/main/native/{CMakeLists.txt, include/m3_precompute_signals.h, src/m3_precompute_signals.c, src/m3_precompute_signals_jni.c, tests/m3_precompute_signals_checks.c}`) | Optional JNI-C11 accelerator for text-signal and SimHash similarity batches; Java impl (`*Java.java`) is the semantic reference; `M3NativeParityTest.java` checks parity | Maven profile **`-Pnative`** (`pom.xml:47`): exec-maven-plugin `cmake -S src/main/native -B target/native -DCMAKE_BUILD_TYPE=Release -DBUILD_TESTING=ON` → `cmake --build` → `ctest` (`pom.xml:46-113`; executions `:56,:72,:85`; surefire `pathFile` `:107`). CMake: `find_package(JNI REQUIRED)`, `add_library(m3_precompute SHARED …)`, C11, `-Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic` or `/W4 /WX`, `file(GENERATE native-path-$<CONFIG>.txt)` holding the built library path; standalone C check executable via CTest | `M3CodeTextSignalBatchNative.loadLibrary()` (`:31-49`): `-Dm3.precompute.native.path` → `System.load`; else `-Dm3.precompute.native.pathFile` (surefire sets `target/native/native-path-Release.txt`) → `System.load`; else `System.loadLibrary("m3_precompute")`. Double-checked `volatile boolean loaded` | Standalone port (not in `m3/pom.xml` modules). Most complete template |
| B | `m3/arrays/` (`M3ArrayNative.java`; `src/main/native/{CMakeLists.txt, m3_arrays.c, m3_arrays_jni.c, include/m3_arrays.h, tests/m3_arrays_checks.c}`) | Optional UTF-16 compare/concatenate; Java fallback when library absent (`M3ArrayNative.java:10-11,34-39`) | Maven profile **`-Pnative-linux`** (`pom.xml:53-114`): same cmake/ctest triple; surefire sets `m3.arrays.native.path=target/native/libm3_arrays.so` (Linux-only name) | Explicit `M3ArrayNative.load(Path)` → `System.load(abs)` (`:18-22`) | Standalone module with own `LICENSE`/`NOTICE` |
| C | `src/java.base/share/native/libjava/M3BitLane28.c` + `src/java.base/share/classes/jdk/internal/mindex/M3BitLane28.java:218` (`private static native int cardinality0(long[][][],int,int)`) | **In-JDK** native: JNI symbol `Java_jdk_internal_mindex_M3BitLane28_cardinality0`; header `jdk_internal_mindex_M3BitLane28.h` is generated by the JDK build | OpenJDK make: `make/modules/java.base/lib/CoreLibraries.gmk:56-57` `SetupJdkLibrary BUILD_LIBJAVA, NAME := java` compiles `share/native/libjava/*.c` into `libjava` | None needed: `libjava` is loaded at VM bootstrap | The only M3 file in libjava; `SPDX Apache-2.0` header inside the GPL tree. Module twin: `m3/collections/src/main/native/collections/segmented_bit_lane.c` (`Java_com_m3_collections_M3BitLane28_cardinality0`) + `m3/collections/verification/SegmentedLaneNativeProbe.java` (`System.load(args[0])`, `-Xcheck:jni`) |
| D | HotSpot `src/hotspot/share/prims/jni.cpp` patch + jtreg native `test/jdk/jdk/internal/mindex/cb/libM3JNINull.c`, `JNINull.java` (custody in `m3/tooling/cb/jni/`, `plan.json`); `m3/runtime-integration/recipe/JNI_SUPERSESSION.md`; `.github/workflows/m3-jni-fix.yml` | M3 String admission + JNI String null-handle guard inside the VM's JNI implementation | OpenJDK make (jtreg `lib*.c` test-native convention); sealed install `m3.sealed-install/1` with before/after SHA-256 | VM-internal / jtreg native path | Recipe custody; current `jni.cpp` blob `81ace3e9…` classified *superseded* |
| E | `src/java.base/share/native/libjava/jni_util.c` (M3 field fix) | `InitializeEncoding` field lookup moved to `M3String` | libjava | bootstrap | Hash-pinned crate `m3-jni-sa-m3-field` (`JNI_SUPERSESSION.md`) |
| F | Probes: `m3/runtime-integration/tests/{MappedNativeProbe,SegmentedJniProbe,SegmentedJvmti,StringApiProbe}.java` + `string_api_probe.c`; `m3/runtime-segments/tests/*` + `segmented_jvmti.c`; `m3/runtime-stage1/tests/JoinJni.java` + `join_jni.c`; `m3/ports/indexstring/native-test/{NativeBoundaryTest.java,boundary.c}` | One-off JNI/JVMTI semantic probes for M3 String/segments | Ad-hoc compiler commands (logs under `*/evidence/*native*.log`) | `System.load(System.getProperty("m3.jni"/"m3.jvmti"/"probe.native"))` or `System.load(args[0])` | Evidence only |
| G | **JNI custody snapshot** under `m3/vendor/synexia/`, pinned to `hsoliwal/com.synexia@857c4cc0ddeb73e5ea5269a1d0ef2d30057dcfa8` (`m3/vendor/synexia/README.md:64-79`; intake `m3/synexia-import/intakes/jni-array-address-receiver-20261007/{README.md,SOURCE_PROVENANCE.tsv,TARGET_MAP.tsv}`) | 12 Apache-2.0 files of `synexia-indexstring`: `SharedArrayNative.java` (`compareUtf16Segments(ByteBuffer[],ByteBuffer[])`, `hash32Segments`, `scanUtf16Segments0`), `native/shared-arrays/{CMakeLists.txt, shared_arrays.cpp, shared_string_compare.cpp}`, plus the Mapbox `jni.hpp` ISC licence notice only (`m3/vendor/synexia/third-party/mapbox-jni-ownership/LICENSE.txt`) | CMake C/C++ with options `SYNEXIA_SHARED_ARRAYS_JNI` / `SYNEXIA_SHARED_ARENA_SHARED`, Android NDK and Apple-device branches, `find_package(JNI)`; **not buildable here** (Mapbox `jni/ownership.hpp` is not copied) | `SharedArrayNative.load(Path)` → `System.load` (explicit opt-in; "pure Java API never loads one") | Custody/proof only; mapped semantically to `java.lang.String.compareTo` / `M3String` |
| H | `m3/vendor/synexia/synexia-code-convergence/` `JniAlgorithmKernel.java` + `src/main/native/algorithm_kernel.c` (seed snapshot `62aea466…`) | JNI search kernels (lowerBound, upperBound, binarySearch, exponential, jump) behind the `AlgorithmKernel` interface | No build file in custody | `tryLoad(library, System::loadLibrary)` with injectable `NativeLibraryLoader` (`:14-34`) | Custody |
| I | Imaged third-party JNI donors: `m3/vendor/third_party-images/zstd-jni-1.5.6-4` (`util/Native.java` extracts a per-`os.name`/`os.arch` resource to a temp file, then `System.load`), `lz4-java-1.8.0` (`net/jpountz/util/Native.java`); `m3/vendor/third_party/jna-5.14.0`, `jna-5.18.1` (112 files), `snappy-java` | Packaging/loader patterns (multi-OS/arch resource extraction) | Java side only; "natives not built" (zstd `PROVENANCE.json`) | As upstream | Pattern donors only |
| J | Docs: `m3/docs/native-build.md` (full OpenJDK fastdebug build recipe; GCC 14, Debian 13 x86-64), `m3/docs/a3-native-jni.md` (native files are exact hash-pinned FILE atoms; suffixes `.c .cc .cpp .cxx .h .hh .hpp .s .asm`), `m3/docs/mindex-jni-string-ingress.md`, `m3/runtime-integration/recipe/JNI_SUPERSESSION.md`, `m3/runtime-integration/tasks/jni-build/PLAN.md` | Policy: native changes ride as content-addressed text atoms; the OpenJDK native build plus jtreg/JNI tests are the oracle | — | — | Doctrine |

No `.cu`, `.cl`, `.metal`, `.spv`, OpenCL, CUDA, Level Zero, HIP or Vulkan file exists under `m3/` outside `vendor/third_party`. The name scan `rv32|vcpu|gpu|opencl|cuda|tornado|vulkan|level.?zero|spir` hits only the tornadovm-bulk adapter and its recipe custody. **M3jdk21 has no GPU native layer today**; the only GPU path is the optional Tornado adapter.

### 3.2 Parallel contract already in java.base

`src/java.base/share/classes/jdk/internal/vm/parallel/` (GPLv2+CE header, "Copyright (c) 2026, Hitesh Soliwal"; not exported by `module-info.java`):

- `BulkTask(String operationId, long workItems, long localWork, List<String> inputs, List<String> outputs)` record — `BulkTask.java:24-34`; `workItems ∈ [1, Integer.MAX_VALUE]`, `localWork ∈ [0, workItems]`, identifiers `[A-Za-z0-9._-]{1,128}`, unique non-empty names.
- `BulkExecutor { BulkExecution execute(BulkTask, Map<String,Object>) }` — `BulkExecutor.java:22-24`.
- `BulkExecution(Map<String,Object> outputs, String provider, String receiptRoot)` record — `BulkExecution.java:23-36`; receiptRoot is 64 lowercase hex characters.
- Consumer: `m3/ports/tornadovm-bulk` (`Rv32iBulkExecutorAdapter implements BulkExecutor`; op `m3.rv32im.slice`; inputs `batch`, `instructionBudget`; output `batch`; CPU mode requires `localWork==0`; Tornado mode requires `localWork>=1` and must return `TORNADO_VERIFIED`; `Rv32iBulkExecutorAdapter.java:19-106`). Built by the `m3/pom.xml:33-35` profile **`-Pm3-tornado`** with `--patch-module java.base=…bulk-patch-classes --add-exports java.base/jdk.internal.vm.parallel=ALL-UNNAMED` (`m3/ports/tornadovm-bulk/pom.xml:30-94`). GPU gate: `.github/workflows/m3-tornadovm-bulk-adapter-gpu.yml` on `[self-hosted, linux, tornadovm-gpu]` installs `synexia-tornadovm@3bf82671` `m3/cpu-gpu`, then runs `mvn -Dm3.gpu=true test`. Compatibility receipt `m3/compatibility/synexia-tornadovm-bulk-adapter-20261009.tsv`: `CANDIDATE_OPTIONAL_PROVIDER`, `promotion_authority=false`.
- Adapter tests (`Rv32iBulkExecutorAdapterTest.java`): `cpuModeCopiesInputAndReturnsJdkReceipt` (:20), `rejectsWrongOperationGeometryAndSchema` (:49), `tornadoModeRequiresExistingProviderCpuParity` (:80, `assumeTrue(Boolean.getBoolean("m3.gpu"))` at :82).

### 3.3 FFM status (JNI vs FFM)

`src/java.base/share/classes/java/lang/foreign/Linker.java:487` is still `@PreviewFeature(FOREIGN)` (the JDK 21 third preview). `m3/backports/recipes/jep-454-ffm/README.md` classifies JEP 454 finalization as `OPT_IN_SE_API_EXTENSION`, risk HIGH, "default Java21: NO". A vCPU native layer that must run without `--enable-preview` therefore has to use **JNI**, which matches the user's request.

### 3.4 Where a vCPU JNI layer could sit

| Option | Slot | Build | Load | Fit |
|---|---|---|---|---|
| **P1 (recommended first leaf)** | New standalone port `m3/ports/vcpu/` mirroring A: `src/main/java/…` (Java SPI + pure-Java reference engine) + `src/main/native/{CMakeLists.txt, include/m3_vcpu.h, src/m3_vcpu_core.c, src/m3_vcpu_jni.c, backends/…, tests/m3_vcpu_checks.c}` | `-Pnative` cmake/ctest triple copied from `m3/ports/precompute/pom.xml:46-113`; `file(GENERATE native-path-$<CONFIG>.txt)` for test wiring | `System.load` via `-Dm3.vcpu.native.path` / `.pathFile` / `System.loadLibrary("m3_vcpu")`, as in `M3CodeTextSignalBatchNative.loadLibrary()` | Highest: proven pattern, outside java.base, no JDK rebuild, existing parity-test pattern (`M3NativeParityTest`) |
| P2 | Provider for the existing `jdk.internal.vm.parallel.BulkExecutor` contract: a sibling of `m3/ports/tornadovm-bulk` (e.g. `m3/ports/vcpu-bulk`) with the same op id `m3.rv32im.slice` and provider ids `jni-cpu-rv32im` / `jni-<backend>-rv32im` | `-Pm3-vcpu` profile like `m3/pom.xml:33-35` + `--patch-module` | As P1 | Keeps the JDK-side contract unchanged while Tornado is dropped |
| P3 | In-JDK: `src/java.base/share/native/libjava/`, or a new module native library via `SetupJdkLibrary` | OpenJDK make | Bootstrap (libjava) or boot-loader `loadLibrary` | Only after P1/P2 are proven; GPU SDK headers (CUDA/OpenCL/Level Zero/HIP) would become optional JDK build deps — high cost |

Recommended native shape (derived from A and the cpu-gpu ABI, not from TornadoVM code): one portable C11 interpreter core over the same flat SoA arrays (`memory`, `registers`, `state`, `config`), compiled as:

- (a) the host CPU backend (a plain loop over cores, optionally threaded);
- (b) an OpenCL C kernel source (Intel/AMD/NVIDIA/CPU ICDs);
- (c) a CUDA `.cu` kernel (NVIDIA);
- (d) optionally HIP (AMD).

The JNI library selects the backend at runtime through a vtable. Data moves by copy-in/copy-out with `Get/Set<Int>ArrayRegion` (pattern from `m3_precompute_signals_jni.c:112-170`), or by direct `ByteBuffer` + `GetDirectBufferAddress` for resident RAM. The Java `CpuRv32iEngine` stays the oracle, and every backend must pass the same full registers/state/RAM parity gate that `Rv32iBulkExecutor.executeVerifiedTornado` applies today (`Rv32iBulkExecutor.java:61-63`).

## 4. Parallel model implied: minimal M3 API surface that replaces TornadoVM for this workload

The vCPU workload needs the following from a parallel API, and nothing else. It is a single 1-D, embarrassingly parallel kernel; each work-item owns one core; there are no barriers, local memory, atomics or reductions; four `int` buffers are involved; RAM stays resident while small state is pulled back after each slice; a bounded budget is applied per launch; and every accelerated result must pass a CPU-oracle parity check.

1. **A device/backend handle.** Enumerate and select backends (`cpu`, `opencl`, `cuda`, `hip`/`level-zero`), each reporting an id and capabilities. This replaces Tornado's implicit runtime/driver choice.
2. **Typed off-heap device buffers.** The workload needs only `M3IntBuffer`, created by `allocate(n)`, `of(int[])` or `ofElements(int...)`, with `get(i)`, `set(i, v)` and `copyTo(int[])` (replaces `IntArray.fromArray/fromElements/get/set/toHeapArray`). In the kernel these are indexed by plain `int`.
3. **A kernel abstraction with one index intrinsic.** A kernel is a static, allocation-free function over `(index, buffers…)`. The vCPU uses only `globalIdx`, so the context can be a single `int index` parameter (replaces `KernelContext` + `Task5`).
4. **A parallel loop / launch shape.** `launch(kernel, globalSize, localSize, args…)` with a 1-D grid, global size rounded up to the local size, and an in-kernel `if (index >= n) return` guard (replaces `WorkerGrid1D`, `setLocalWork` and `GridScheduler("graph.task", grid)`).
5. **An immutable plan / task graph.** A built-once plan binds buffers to a kernel with a per-buffer transfer policy:
   - `ONCE` for resident state (= `FIRST_EXECUTION`);
   - `EACH_LAUNCH` for small config (= `EVERY_EXECUTION`);
   - `ON_DEMAND` for results (= `UNDER_DEMAND`).

   The plan exposes `execute()`, returning a result handle, and `close()`, which frees device memory. This replaces `TaskGraph.transferToDevice/task/transferToHost/snapshot`, `ImmutableTaskGraph` and `TornadoExecutionPlan(...).withGridScheduler(...).execute()/close()`.
6. **Explicit host/device transfer on the result.** `result.download(buffers…)` for a selective device-to-host copy (registers/state every slice; RAM only on request), and `upload(buffer)` for host-to-device patches between slices (replaces `TornadoExecutionResult.transferToHost(Object...)`).
7. **Verification and receipt hooks.** The plan runs on a backend; a CPU reference runs the same input; full-buffer equality is checked; a content-addressed receipt is issued (already `Rv32iBulkExecutor`/`Rv32iBulkReceipt` in cpu-gpu, and `BulkTask`/`BulkExecution` in java.base).

In JNI terms this collapses to roughly six native entry points:

- `open(backendId)`
- `alloc(bytes)`
- `upload(buf, int[] / direct ByteBuffer, off, len)`
- `launch(kernelId, global, local, buf[])`
- `download(buf, int[], off, len)`
- `close()`

The kernels themselves (the RV32IM step loop) are precompiled per backend (C for CPU, OpenCL C, CUDA/HIP) rather than JIT-compiled from Java bytecode. **Dropping bytecode-to-GPU compilation is the main structural difference from TornadoVM.** Because of it, the M3 API does not need TornadoVM's Graal-based compiler, its `@Parallel` loop API or its sealed `TornadoExecutionPlan` hierarchy.

### 4.1 Tornado → M3 concept map (for the vCPU only)

| TornadoVM 6.1.0 (used) | M3 replacement concept | Native entry |
|---|---|---|
| `IntArray.fromArray/fromElements` | `M3IntBuffer.of/ofElements` | `alloc` + `upload` |
| `IntArray.get/set` (kernel side) | plain indexed access in C/OpenCL/CUDA kernel | — |
| `IntArray.toHeapArray` | `M3IntBuffer.copyTo(int[])` | `download` |
| `KernelContext.globalIdx` | kernel `index` parameter | `get_global_id(0)` / `blockIdx.x*blockDim.x+threadIdx.x` / loop var |
| `TaskGraph.task(id, Task5, …)` | `plan.kernel(KernelId.RV32IM_SLICE, buffers…)` | `launch` |
| `DataTransferMode.FIRST_EXECUTION / EVERY_EXECUTION / UNDER_DEMAND` | `Transfer.ONCE / EACH_LAUNCH / ON_DEMAND` | `upload` policy |
| `WorkerGrid1D` + `setLocalWork` + `GridScheduler` | `Grid1D(global, local)` | `launch(global, local)` |
| `snapshot()` / `ImmutableTaskGraph` / `TornadoExecutionPlan` | immutable `M3Plan` (record) | `open` |
| `execute()` → `TornadoExecutionResult.transferToHost` | `M3Plan.execute()` → `M3Result.download(…)` | `launch` + `download` |
| `close()` / `TornadoExecutionPlanException` | `M3Plan.close()` (unchecked or own checked type) | `close` |

## 5. Observations relevant to the redesign

- **Kernel cost profile.** Registers live in global memory, and `state` is written after every retired instruction (`Rv32iKernel.java:258-260`). A native backend can keep `pc`, `retired` and the 32 registers in private memory/registers, and persist them only at slice end or trap. That is ABI-compatible, because only slice-boundary state is observable (DESIGN.md:57).
- **GPU test coverage is thin.** The GPU differential tests run only the sum-1..100 program (ADDI/ADD/BLT/halt). Loads, stores, M ops and traps are proven only on the CPU oracle. A new backend needs an opcode-coverage corpus for parity.
- **The oracle is single-threaded.** `CpuRv32iEngine.runSlice` loops cores sequentially (`CpuRv32iEngine.java:13-15`). A JNI CPU backend is the first real multi-core CPU path.
- **Licence boundary.** cpu-gpu is Apache-2.0 throughout. `jdk.internal.vm.parallel` is GPLv2+CE. The tornado-api jar mixes 214 Apache files with one GPL-2.0-only file and one header-less file. The used surface is entirely Apache-2.0 and is replaceable by roughly 10 M3-owned concepts (§4.1).
- **No GPU artifacts and no GPU run evidence** were found in M3jdk21 or in the cpu-gpu bytes inspected. Any CUDA/OpenCL/HIP parity claim requires a hardware run (DESIGN.md:69 states the same rule).
- **Donor gate still open.** Per the user's FOSS-donor-first rule, backend headers/loaders (Khronos OpenCL headers / ICD loader, HIP, oneAPI Level Zero loader, CUDA driver API headers) still need a `FOSS_REUSE_DECISION.tsv` with verified repo/commit/licence before any native backend is written. Their licences were **not** verified in this session.

## 6. Session notes

- All M3jdk21 reads used committed objects (`git show/ls-tree/grep HEAD`), not the working tree. The worktree `K:/m3-work/wt/m3jdk21-c7` was observed with `core.sparseCheckout=true`, 4,262 tracked paths reported ` D` (including `m3/ports/tornadovm-bulk/pom.xml`, `m3/ports/precompute/pom.xml`, `m3/arrays/pom.xml`; the `m3/ports/tornadovm-bulk/` directory is absent on disk), and 2 untracked dirs (`m3/vendor/third_party/caffeine/`, `m3/vendor/third_party/lz4-java/`). This state was not created or changed by this session and was left untouched.
- Scratch: `scratch/lic_classify.py`, `scratch/tornado_api_610_licenses.tsv`, `scratch/m3_names.txt`, `scratch/m3_jni_native_names.txt`, `scratch/section3.md`, `scratch/section4.md`.
