# M3 Parallel API and JNI vCPU — design (M3SDK)

STATUS: DRAFT v1 (2026-10-12) | owner: M3jdk21 | licence: Apache-2.0 (m3/** independent module) | precedes code (D2C)

## 1. Goal

M3jdk21 is the M3SDK, a superset of JDK 21 that ships complete, as JMODs, with no downstream dependency. This design
introduces two first-party pieces for it:

1. **`m3.parallel`, a small M3 parallel API** that replaces TornadoVM as the way M3 code launches data-parallel work.
   TornadoVM is a donor for inspiration only and is not vendored: tornado-api 6.1.0 contains a GPL-2.0-only file, and
   its runtime and drivers are GPLv2+CE.
2. **A JNI vCPU layer, abstracted so the same program runs on CPU, Intel, NVIDIA (CUDA) and AMD.** Its first workload
   is the RV32IM virtual CPU from `cpu-gpu-rv32im` (one work-item = one virtual core). The vCPU lives in the JNI and
   native space. The native layer is a vendor-neutral C HAL whose backends are selected at runtime.

Evidence: `VCPU_DONOR_INVENTORY.md` (what the vCPU is; the exact tornado-api surface it uses, all Apache-2.0),
`GPU_BACKEND_SURVEY.md` (host probe and backend options), `FOSS_REUSE_DECISION.tsv` (20 donor rows; OpenCLAW searched
first = NO_FIT). All three sit beside this file in the crate.

## 2. What exists today (facts)

- **The vCPU.** RV32I 2.1 + M 2.0, FENCE/FENCE.I as no-ops, ECALL/EBREAK, plus the custom `XM3_HALT`
  (`0x0000000B`, exit code in a0). It has no C/A/F/D/CSR/MMU. State is four flat `int` arrays: `memory` (one RAM slice
  per core), `registers` (32 per core), `state` (PC, STATUS, RETIRED, TRAP_CAUSE, TRAP_VALUE, EXIT_CODE) and `config`
  (cores, words per core, budget). The status machine is READY / HALTED / TRAPPED / YIELDED. There are no barriers,
  atomics or shared memory. A CPU oracle (`CpuRv32iEngine`) is the reference, with receipts and parity checks.
- **TornadoVM usage.** Only `globalIdx`, `IntArray`, `TaskGraph`/`ImmutableTaskGraph`, the three DataTransferModes,
  `WorkerGrid1D`/`GridScheduler` and `TornadoExecutionPlan` are used, all from Apache-2.0 files. `tornadovm-bulk`
  reaches TornadoVM only through `Rv32iBulkExecutor`.
- **Native template.** `m3/ports/precompute` uses a `-Pnative` profile (cmake, build, ctest via exec-maven-plugin), a
  C11 core plus a `_jni.c` glue file built with `-Werror`, a loader chain (system property → generated path file →
  `System.loadLibrary`), and a Java reference with a parity test.
- **No GPU native code exists in `m3/`.** FFM is still a preview in JDK 21, so the native boundary is JNI.
- **Host.**
  - RTX 4060 Laptop: CUDA 13.2 driver, sm_89.
  - Intel UHD iGPU: no fp64.
  - i9-14900HX: AVX2, no AVX-512.
  - No AMD GPU.
  - With **no vendor SDK installed**, one OpenCL C kernel ran correctly on NVIDIA (CUDA Driver API, PTX from clang
    22.1), on NVIDIA and Intel (OpenCL), and on the CPU (as C).

## 3. Architecture

```text
Java  m3.parallel   (Apache-2.0, m3/ports/parallel -> JMOD m3.parallel)
        Backend / Plan / Buffer / Grid1D / Transfer / Result / KernelId
        |  pure Java CPU reference implementation (always available; parity oracle)
        |  JNI provider  (m3vcpu_jni: marshalling, pinned int[] / direct buffers, error -> exception; no policy)
        v
C99   m3vcpu HAL core (m3vcpu.h, versioned function table m3vcpu_backend_v1; registry, probe, score, select)
        |  kernel bundle: content-addressed blobs, one per format (PTX | OpenCL C | SPIR-V | CPU symbol)
        v
backends (one DLL/.so each, vendor runtime loaded at run time, never linked)
        cpu     threads + runtime ISA dispatch (scalar | AVX2+FMA | AVX-512), dynamic chunking (P/E cores)
        cuda    LoadLibrary(nvcuda) + PTX (ISA floor 7.0 / sm_75)            — NVIDIA
        opencl  LoadLibrary(OpenCL) + OpenCL C 1.2 source (+ binary cache)   — Intel, AMD, NVIDIA
        vulkan  volk -> vulkan-1 + SPIR-V (clspv)                            — v2, cross-vendor
        ze      LoadLibrary(ze_loader) + SPIR-V                              — v3 optional, Intel peak
        hip     LoadLibrary(amdhip64) + HSACO                                — v3 optional, AMD peak
```

### 3.1 Java API (`m3.parallel`, v1 surface: exactly what the vCPU needs)

| Type | Role | Replaces (TornadoVM) |
|---|---|---|
| `Backend` | `Backend.select(Policy)`, `Backend.of("cuda" / "opencl" / "cpu" …)`, `id()`, `capabilities()` | implicit runtime and driver |
| `IntBuffer` | off-heap `int` buffer: `allocate(n)`, `of(int[])`, `get`, `set`, `copyTo(int[])` | `IntArray` |
| `KernelId` | names a precompiled kernel in the bundle (`RV32IM_SLICE`) | `Task5` + Graal JIT |
| `Grid1D` | `Grid1D(global, local)`; global rounded up to local; kernel guards `index >= n` | `WorkerGrid1D` / `GridScheduler` |
| `Transfer` | `ONCE`, `EACH_LAUNCH`, `ON_DEMAND` per bound buffer | `DataTransferMode` |
| `Plan` | immutable record: backend + kernel + grid + bindings; `execute()` → `Result`; `close()` | `TaskGraph` / `ImmutableTaskGraph` / `TornadoExecutionPlan` |
| `Result` | `download(IntBuffer…)`, `upload(IntBuffer)` | `TornadoExecutionResult.transferToHost` |
| `Verified` | run on a backend and on the CPU reference, check full-buffer equality, emit a content-addressed receipt | `Rv32iBulkExecutor` / receipts |

Kernels are **precompiled per backend from one source**, not JIT-compiled from Java bytecode. That removes TornadoVM's
compiler, its `@Parallel` loops and its sealed plan hierarchy, and it is the main structural difference from the donor.

### 3.2 JNI and the native ABI

- Six JNI entry points: `open(backend, device)`, `alloc(bytes)`, `upload(buf, int[] | ByteBuffer, off, len)`,
  `launch(kernel, global, local, bufs[])`, `download(buf, int[], off, len)`, `close()`. Handles are `long`s, owned by a
  `Cleaner`.
- The C ABI is the `m3vcpu_backend_v1` function table: `abi` + `struct_size` for append-only evolution, plus `probe`,
  device enumeration, context, buffers, `kernel_load` from blobs, `launch_1d`, `finish` and `last_error`.
- Hard rules, each enforced in CI:
  1. **No vendor import-table entries.** Every backend links only kernel32/CRT (libc), and vendor runtimes are loaded
     at run time from System32 / by SONAME. Gate: `llvm-readobj --coff-imports` / `readelf -d` lists no vendor DLL.
  2. **Probe never crashes.** A missing driver means "unavailable" plus a reason. The CPU backend is always present.
  3. **Watchdog-aware launches.** The vCPU already slices execution by instruction budget. The scheduler keeps each
     launch well under the Windows TDR (~2 s). System TDR settings are never changed.
  4. **Parity before admission.** A backend/device pair is admitted only after the full RV32IM differential suite
     matches the CPU oracle. A receipt carries the state-root SHA-256.

### 3.3 One kernel source

"M3 kernel C" is a restricted OpenCL C 1.2 subset: global buffers, 32/64-bit integers, a 1-D NDRange, and no fp64
requirement (the Intel iGPU has none), plus a portability header (`M3_GID0`, address-space macros).

Build matrix, with content-addressed outputs in the kernel bundle:

| Target | Producer | Status here |
|---|---|---|
| OpenCL (all vendors) | the source as-is, built at run time and cached | proven (NVIDIA, Intel) |
| CUDA | clang `-target nvptx64-nvidia-nvcl -march=sm_75`, PTX 7.0 | proven (RTX 4060) |
| CPU | the same source as C through a macro shim, one build per ISA level | proven (AVX2) |
| Vulkan | clspv → SPIR-V | v2; toolchain to vendor |
| Level Zero | clang/llvm-spirv → SPIR-V | v3 |
| HIP | clang amdgcn + offload bundler | v3; no AMD GPU here |

AMD runs through OpenCL in v1. A native HIP backend is v3 and needs an AMD GPU runner to validate.

## 4. Where it lives

| Path | Content | Policy (`m3/docs/synexia-intake-policy.tsv`) |
|---|---|---|
| `m3/ports/parallel/` | `m3.parallel` Java API + CPU reference + tests | INDEPENDENT_APACHE_MODULE |
| `m3/ports/vcpu/` | RV32IM: Java oracle (from cpu-gpu, ported, Apache-2.0 provenance kept), kernel C source, JNI glue, HAL core, backends, CMake `-Pnative` | INDEPENDENT_APACHE_MODULE (+ native under the same licence) |
| `m3/vendor/synexia/synexia-tornadovm/m3/cpu-gpu/` | byte-for-byte cpu-gpu at `3bf82671` (staged) — the donor of the oracle and kernel semantics | Synexia intake (pinned, mirror test) |
| `m3/ports/tornadovm-bulk` | kept as an optional TornadoVM backend adapter (`-Pm3-tornado`), not on the default path | unchanged |

**Later**, after parity and performance evidence: a `BulkExecutor` provider in java.base (`jdk.internal.vm.parallel`)
and the `m3.parallel` JMOD in the SDK image. This is a TARGET_OWNED_PORT for anything under `src/**`.

## 5. Delivery plan (leaves)

| # | Leaf | Proof |
|---|---|---|
| P1 | `m3.parallel` API + pure-Java CPU reference + RV32IM oracle port | JUnit; existing cpu-gpu tests reused; receipts identical to the cpu-gpu oracle |
| P2 | `m3vcpu.h` ABI + CPU backend (threads, scalar and AVX2) + JNI glue, `-Pnative` CMake | ctest; parity with the Java oracle on the **full** RV32IM suite (ALU, M, loads/stores, traps, division corners, slicing) |
| P3 | OpenCL backend + kernel C | parity on NVIDIA and Intel OpenCL |
| P4 | CUDA backend (PTX via clang, pinned) | parity on the RTX 4060; launch-duration budget below the TDR limit |
| P5 | Throughput benchmark (cores × budget, CPU vs OpenCL vs CUDA) | measured numbers only; no speed-up claims without them |
| P6 | Vulkan / Level Zero / HIP backends | needs SPIR-V and AMD toolchains and an AMD runner |
| P7 | JMOD packaging: `m3.parallel` + `m3.vcpu` (+ native libs in the jmod) | `jlink` image runs the vCPU differential suite |

The cpu-gpu GPU tests cover only one program today. P2–P4 widen the device differential to every instruction class
before any backend is admitted.

## 6. SDK packaging as JMODs (all vendored modules)

The SDK target is JDK 21 plus every vendored library and M3 module as **named modules in jmods**, linked by `jlink`:

1. **Modules with a published descriptor** (≈68; root or versioned) are already reproduced and gate-proven:
   `jmod create` straight from the module jar.
2. **Modules without a descriptor** (automatic modules: `Automatic-Module-Name` only, or neither) cannot be linked.
   They need an explicit `module-info.java`, generated from `jdeps --generate-module-info` over the gate-proven jar,
   with requires/exports reviewed. The descriptor is an M3 refactor artefact, provenance kind
   `m3-descriptor-for-jmod`. This is the first place where the SDK legitimately departs from the published jar, and
   it is recorded as such.
3. **Split packages and duplicate versions** (e.g. three jackson versions) cannot coexist in one module graph. JMOD
   packaging therefore depends on **version convergence** (refactor track R2): one version per library in the image,
   and the other versions remain source modules outside the image.
4. Native libraries (snappy, sqlite-jdbc, jna, zstd, m3vcpu) go into the jmods' `lib/` sections, per platform.

## 7. Refactor tracks over the inlined donors

- **R1, shade instead of copy.** Where a donor embeds a relocated copy of another vendored library (byte-buddy's
  `net.bytebuddy.jar.asm`, the top reuse pair), build it from the single vendored module plus a shade step, as upstream
  does. The gate must stay IDENTICAL.
- **R2, one version per library.** Converge consumers to one version per artifact (input: `reuse/REUSE_CATALOG.tsv`
  CROSS_VERSION, `CONVERGENCE_PROPOSAL`). Proof is a consumer build and test per switch.
- **R3, refactored layer.** Recipe-driven atomize/patternize rewrites live in a separate M3 layer, never in the
  byte-for-byte upstream layer. Proof is differential against upstream's own test suites fetched at the release tags.
  "Donors are inspiration": once a library has been atomized and its patterns are catalogued, an M3-native API can
  replace it module by module. That replacement is validated by the same differential tests, then by the gate on its
  consumers.

## 8. Open decisions (owner)

1. Module and package names: `m3.parallel` / `com.m3.parallel` and `m3.vcpu` / `com.m3.vcpu` are proposed.
2. AMD validation: v1 reaches AMD only through OpenCL until an AMD GPU runner exists.
3. JMOD scope for the first SDK image: all converged libraries, or the M3 tooling closure first.
4. tornado-api: confirm that it stays out of the SDK. The optional `-Pm3-tornado` adapter remains for comparison.

## 9. Non-claims

No GPU run of the RV32IM kernel exists yet; only a vector-add proof ran on this host. No throughput or speed-up claims
are made. AMD is untested. CI GPU runners do not exist.
