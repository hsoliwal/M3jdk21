# m3-vcpu provenance

| Field | Value |
|---|---|
| Donor | `hsoliwal/synexia-tornadovm`, module `m3/cpu-gpu` (artifact `com.synexia.m3:cpu-gpu-rv32im:1.0.0-SNAPSHOT`) |
| Commit | `3bf826714a48d7fdf346ffde8a0fc25c525572c5` |
| Bytes read | M3jdk21 staged copy `m3/vendor/synexia/synexia-tornadovm/m3/cpu-gpu/` (byte-identical to the donor at that commit; git blob ids below) |
| Licence | Apache-2.0. Every donor file carries `SPDX-License-Identifier: Apache-2.0` on line 1, and every ported file keeps it. |
| Port marker | Line 2 of each ported file: `/* Ported from hsoliwal/synexia-tornadovm m3/cpu-gpu@3bf82671… <donor path>; see PROVENANCE.md. */` |
| TornadoVM | None. The two TornadoVM-bound donor files (`Rv32iKernel`, `TornadoRv32iSession`) are re-expressed on `m3.parallel`. No `uk.ac.manchester.tornado` import or dependency remains. |

## File map

Change classes:

- `RENAME` means that only the package line (`com.synexia.m3.cpugpu` → `com.m3.vcpu`) changed and the port marker was added.
- `ADAPT` means the change is described in the Notes column.

Donor paths are relative to `m3/cpu-gpu/`.

| Port file (`src/…/com/m3/vcpu/`) | Donor file | Donor git blob | Class | Notes |
|---|---|---|---|---|
| `main/Rv32i.java` | `src/main/java/com/synexia/m3/cpugpu/Rv32i.java` | `daf69144` | RENAME | ISA constants, state/config layout, trap causes, `XM3_HALT`, immediate decoders |
| `main/Rv32iBatch.java` | `src/main/java/com/synexia/m3/cpugpu/Rv32iBatch.java` | `71716430` | RENAME | SoA batch state |
| `main/Rv32iAssembler.java` | `src/main/java/com/synexia/m3/cpugpu/Rv32iAssembler.java` | `46d77d05` | RENAME | encoders |
| `main/Elf32RiscVLoader.java` | `src/main/java/com/synexia/m3/cpugpu/Elf32RiscVLoader.java` | `43856345` | RENAME | ELF32 ET_EXEC PT_LOAD loader |
| `main/CpuRv32iEngine.java` | `src/main/java/com/synexia/m3/cpugpu/CpuRv32iEngine.java` | `51d7fb07` | ADAPT | Adds an explicit public no-arg constructor, because `-Xlint:all` reports `missing-explicit-ctor` for exported packages. The interpreter is unchanged. |
| `main/Rv32iKernel.java` | `src/main/java/com/synexia/m3/cpugpu/Rv32iKernel.java` | `f3c5289c` | ADAPT | Signature changed from `execute(KernelContext, IntArray×4)` to the m3.parallel kernel `slice(int index, IntBuffer... buffers)`. `context.globalIdx` became `index` and `IntArray` became `com.m3.parallel.IntBuffer` (same `get`/`set`). Adds `RV32IM_SLICE = KernelId("m3.rv32im.slice")`, the `ARG_*` binding indices and the `JavaKernels` registration. The step loop, decode, traps and helpers are unchanged. |
| `main/Rv32iSession.java` | `src/main/java/com/synexia/m3/cpugpu/TornadoRv32iSession.java` | `d8b09792` | ADAPT | The donor's `TaskGraph`/`ImmutableTaskGraph`/`WorkerGrid1D`/`GridScheduler`/`TornadoExecutionPlan` became `Plan.builder(backend)`. The transfer modes map as follows: `FIRST_EXECUTION` → `Transfer.ONCE` (memory, registers, state) and `EVERY_EXECUTION` → `Transfer.EACH_LAUNCH` (config). `transferToHost(UNDER_DEMAND)` became `Result.download`. The global size is rounded up to the local size by `Grid1D`. The constructor takes a `Backend`. `close()` is unchecked. |
| `main/Rv32iBulkExecutor.java` | `src/main/java/com/synexia/m3/cpugpu/Rv32iBulkExecutor.java` | `57c3b400` | ADAPT | `executeCpu`, `stateRoot`, `receiptRoot` and framing are unchanged. `executeVerifiedTornado(batch, budget, localWork)` became `executeVerified(batch, budget, Backend, localWork)`, which checks parity with `Verified` (backend against the CPU reference backend) and with the sequential oracle. Its parity root uses the new domain `M3_RV32I_CPU_PARALLEL_PARITY_V1` over the backend id, the `Verified` root, the oracle root, the output root, the cores and the budget. The donor domain strings `M3_RV32I_BULK_RECEIPT_V1`, `M3_RV32I_CPU_TORNADO_PARITY_V1` (CPU-only parity) and `M3_RV32I_BATCH_STATE_V1` are kept byte-for-byte, so CPU receipts are identical to the donor's. |
| `main/Rv32iBulkReceipt.java` | `src/main/java/com/synexia/m3/cpugpu/Rv32iBulkReceipt.java` | `b4066e7b` | ADAPT | `Mode.TORNADO_VERIFIED` became `Mode.PARALLEL_VERIFIED`. Javadoc was added. Root recomputation and validation are unchanged. |
| `test/Rv32iAssemblerTest.java` | `src/test/java/com/synexia/m3/cpugpu/Rv32iAssemblerTest.java` | `f3be1211` | RENAME | |
| `test/Elf32RiscVLoaderTest.java` | `src/test/java/com/synexia/m3/cpugpu/Elf32RiscVLoaderTest.java` | `ad064376` | RENAME | |
| `test/CpuRv32iEngineTest.java` | `src/test/java/com/synexia/m3/cpugpu/CpuRv32iEngineTest.java` | `fc52c6bf` | ADAPT | `Rv32iDemo.sumOneToHundredProgram()` became `Rv32iPrograms.sumOneToHundredProgram()`. |
| `test/Rv32iBulkExecutorTest.java` | `src/test/java/com/synexia/m3/cpugpu/Rv32iBulkExecutorTest.java` | `1e13ba96` | ADAPT | The GPU-gated Tornado test became the ungated `parallelModeRequiresFullCpuParity` on the always-present CPU reference backend (256 cores, budget 1024, local 64). The budget/localWork rejection tests now target `executeVerified`. A tamper test was added. |
| `test/Rv32iParallelDifferentialTest.java` | `src/test/java/com/synexia/m3/cpugpu/TornadoRv32iDifferentialTest.java` | `3f693094` | ADAPT | The donor 256-core test is ungated on the CPU reference backend. Full-ISA corpus differentials were added (≥256 cores). |
| `test/Rv32iPrograms.java` | `src/main/java/com/synexia/m3/cpugpu/Rv32iDemo.java` | `77fe244d` | ADAPT | Only `newBatch` and `sumOneToHundredProgram` are kept. The CLI (`main`, `printf`) is not ported because the module rule bans `System.out`. |

New files (no donor):

- `module-info.java` and `package-info.java`.
- The tests `Rv32iCorpus`, `Rv32iCorpusCoverageTest`, `Rv32iGoldenReceiptTest` and `Rv32iSessionTest`.

Not ported:

- `recipe-bulk-provider/`, an OpenRewrite recipe that seals the Tornado-path files by SHA-256.
- The donor `pom.xml`, which depends on `tornado-api`.
- `docs/`.

## Golden receipts

`Rv32iGoldenReceiptTest` holds roots produced by the **unmodified donor classes**: `Rv32i`, `Rv32iBatch`, `CpuRv32iEngine`, `Rv32iAssembler`, `Rv32iBulkReceipt` and `Rv32iBulkExecutor`. The only change was that the Tornado-only method `executeVerifiedTornado` was deleted from a scratch copy, so that the copy compiles without `tornado-api`.

The harness compiled that copy with JDK 21 together with this module's `Rv32iCorpus` source, renamed to the donor package; its corpus source had SHA-256 `c4ff7b2100843081cb1c4dda1c1fde5d2865cfc76ed4b5461a189ea1cf8d0ffa`. The harness ran `executeCpu` slice by slice on the following inputs:

- the corpus with seed `Rv32iCorpus.SEED`;
- the donor's 64-core sum-1..100 batch.

The ported oracle must reproduce every input root, output root and receipt root, and the `m3.parallel` CPU backend must reach the same state roots. Any change to `Rv32iCorpus` invalidates these goldens. Regenerate them with the donor harness instead of editing them by hand.
