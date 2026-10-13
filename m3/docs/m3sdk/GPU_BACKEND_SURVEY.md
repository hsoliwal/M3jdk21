# M3jdk21 vCPU / Parallel-Compute JNI Layer — GPU Backend Survey

Status: DRAFT (survey, research-only; no code admitted)
Date: 2026-10-12
Host: this Windows 11 Home 10.0.26300 laptop (Lenovo, SUBSYS 3E4517AA)
Method: read-only probes (CIM, nvidia-smi, `where`/`which`, registry, path checks, Python ctypes). Every command bounded by <=120 s timeout.
Scratch: `K:\m3-work\tmp\m3sdk-design\gpu-survey-scratch\` (scripts, kernels); probe logs `probe-*.txt` and ledger `FOSS_REUSE_DECISION.tsv` in this directory. Sibling inventory: `VCPU_DONOR_INVENTORY.md`.

---

## 1. Host hardware and toolchains (probed)

### 1.1 GPUs

| # | Vendor | Model | Driver (WDDM) | Vendor driver ver | VRAM | Notes |
|---|--------|-------|---------------|-------------------|------|-------|
| 0 | NVIDIA | GeForce RTX 4060 Laptop GPU (AD107, PCI 10DE:28E0) | 32.0.15.9579 (2026-03-04) | 595.79, CUDA driver API 13.2 | 8188 MiB | compute capability 8.9 (Ada); WDDM mode (not TCC) |
| 1 | Intel | UHD Graphics (Raptor Lake-S Mobile, PCI 8086:A788) | 32.0.101.6790 (2025-04-28) | — | shared (2 GiB reported) | Xe-LP iGPU; Level Zero + OpenCL capable via Intel driver |
| — | AMD | absent | — | — | — | no AMD GPU on this host; AMD backend cannot be runtime-tested here |

### 1.2 CPU and SIMD

| Item | Value |
|------|-------|
| Model | Intel Core i9-14900HX (Family 6 Model 183 Stepping 1, Raptor Lake-HX) |
| Cores / threads | 24 cores (8 P + 16 E) / 32 logical |
| Present | SSE2 SSE3 SSSE3 SSE4.1 SSE4.2 POPCNT AVX F16C FMA3 AVX2 |
| Absent | AVX-512 (all subsets: F/CD/BW/DQ/VL/VNNI/FP16 …) — fused off on hybrid Raptor Lake |
| Source | `IsProcessorFeaturePresent` (PF_AVX2=40 true, PF_AVX512F=41 false) + numpy 2.2.6 `__cpu_features__` |

Design consequence: CPU backend must dispatch at runtime (AVX2+FMA3 baseline path, AVX-512 path only on hosts that report it, scalar/SSE2 fallback). Hybrid P/E cores mean static equal-chunk partitioning is wrong; use work-stealing / dynamic chunking.

### 1.3 Native toolchains

| Tool | Status | Version | Path |
|------|--------|---------|------|
| MSVC (VS Build Tools 2022) | present (not on PATH; use vcvars / CMake VS generator) | Build Tools 17.14.29 (Mar 2026), MSVC toolset 14.44.35207, cl 19.44.35225 | `H:\BuildTools\VS2022\VC\Tools\MSVC\14.44.35207\bin\Hostx64\x64\cl.exe` |
| Windows SDK | present | 10.0.26100.0 | `C:\Program Files (x86)\Windows Kits\10` |
| VS-bundled clang-cl | absent (component not installed) | — | — |
| LLVM clang / clang-cl | present, on PATH | 22.1.0 (x86_64-pc-windows-msvc) | `C:\Program Files\LLVM\bin` |
| LLVM GPU targets in that build | **nvptx/nvptx64 only**; AMDGPU and SPIR-V targets **not** registered | — | `clang -print-targets`; `nvptx-arch.exe`, `amdgpu-arch.exe`, `clang-offload-bundler.exe` shipped |
| gcc / g++ (MinGW-w64) | present, on PATH | 15.2.0 (x86_64-posix-seh, MinGW-Builds), target x86_64-w64-mingw32 | `C:\ProgramData\mingw64\mingw64\bin` |
| CMake | present (2 copies) | 4.3.1 on PATH; 3.31.6-msvc6 bundled with Build Tools | `C:\Program Files\CMake\bin`; `H:\BuildTools\VS2022\Common7\IDE\CommonExtensions\Microsoft\CMake` |
| Ninja | present (2 copies) | 1.13.0 (pip, on PATH); 1.12.1 bundled with Build Tools | `C:\Program Files\Python312\Scripts\ninja`; Build Tools CMake\Ninja |
| GNU make | present | 4.4.1 | `C:\ProgramData\chocolatey\bin\make` |
| JDK 21 (JNI headers) | present | 21.0.9+7-LTS (Oracle) — `include\jni.h`, `include\win32\jni_md.h` | `C:\Program Files\Java\jdk-21` (JAVA_HOME currently points at MS JDK 17.0.19) |
| M3jdk21 tree | present; no existing GPU/vCPU native code found (`rg` for nvcuda / clCreateProgramWithSource / vkCreateComputePipelines / zeModuleCreate / m3vcpu: 0 hits) | branch `m3/equality-gates-20261009` | `K:\m3-work\M3jdk21` |

Note: `nvptx-arch.exe` hung >60 s under host load (killed by timeout); device arch was obtained from `nvidia-smi` and a direct `cuDeviceGetAttribute` probe instead.

### 1.4 GPU SDKs and runtime loaders

Runtime loaders (installed by Windows / GPU drivers — what an end-user machine has without any SDK):

| Runtime | Status | Version | Path / evidence |
|---------|--------|---------|-----------------|
| CUDA driver API `nvcuda.dll` | present | driver 595.79, `cuDriverGetVersion` = 13020 (CUDA 13.2) | `C:\Windows\System32\nvcuda.dll`; driver store also has `nvptxJitCompiler64.dll`, `nvvm64.dll`, `nvvm70.dll` (PTX/NVVM JIT inside the driver) |
| NVML `nvml.dll` | present | 595.79 | `C:\Windows\System32\nvml.dll` |
| NVRTC `nvrtc64_*.dll` | absent | — | not part of the driver; ships only with CUDA Toolkit |
| OpenCL ICD loader `OpenCL.dll` | present | 3.0.6.0 | `C:\Windows\System32\OpenCL.dll`; ICDs registered per-adapter (DCH style) in `HKLM\SYSTEM\CurrentControlSet\Control\Class\{4d36e968-…}\000N\OpenCLDriverName`, not in `HKLM\SOFTWARE\Khronos\OpenCL\Vendors` (absent) |
| OpenCL ICD NVIDIA | present | platform "NVIDIA CUDA", OpenCL 3.0 CUDA 13.2.51; device OpenCL C 1.2; fp64 yes; **no SPIR-V IL** (`CL_DEVICE_IL_VERSION` empty, no `cl_khr_il_program`) | `nvopencl64.dll` (driver store `nvltsi.inf_amd64_…`) |
| OpenCL ICD Intel | present | platform "Intel(R) OpenCL Graphics", OpenCL 3.0 NEO; device OpenCL C 1.2; **SPIR-V 1.0–1.3 IL accepted**; fp64 **no** | `igdrcl64.dll` (driver store `iigd_dch.inf_amd64_…`), IGC `igc64.dll`, `opencl-clang264.dll` |
| Level Zero loader `ze_loader.dll` | present | 1.21.1; `zeInit` OK, 1 driver, API 1.6, 1 device (Intel iGPU) | `C:\Windows\System32\ze_loader.dll`; GPU driver `ze_intel_gpu64.dll` via `LevelZeroDriverPath` |
| Vulkan loader `vulkan-1.dll` | present | 1.4.321 (instance 1.4.321) | `C:\Windows\System32\vulkan-1.dll`; `vulkaninfo.exe` 1.4.321 also in System32 |
| Vulkan ICD NVIDIA | present | apiVersion 1.4.329, driver 595.79, conformance 1.4.3.3, discrete | `nv-vk64.json` |
| Vulkan ICD Intel | present | apiVersion 1.4.311, driver 101.6790, conformance 1.4.0.0, integrated | `igvk64.json` |
| D3D12 / DirectML | present (Windows-only, out of scope for portable HAL) | D3D12 10.0.26100.9278; DirectML 1.15.5 | System32 |
| AMD HIP runtime `amdhip64*.dll`, `amdocl64.dll`, `amdvlk64.dll` | absent | — | no AMD GPU/driver on host |

Developer SDKs / compilers:

| SDK | Status | Version | Path / evidence |
|-----|--------|---------|-----------------|
| CUDA Toolkit (nvcc, cuda.h, nvrtc) | **absent (residue only)** | registry `FirstVersionInstalled=v13.4`; `version.json` says 13.4.1 (nvcc 13.4.59) but directory holds only 6 files (VS MSBuild integration + version.json); no `bin\`, no `include\`; `CUDA_PATH` unset; `nvcc` not on PATH | `C:\Program Files\NVIDIA GPU Computing Toolkit\CUDA\v13.4` |
| CUDA version skew warning | — | residue toolkit 13.4 > driver 13.2. If 13.4 is reinstalled, its PTX (ISA newer than driver) will fail `cuModuleLoadData` JIT on this driver; pin PTX ISA / target to the driver floor | — |
| AMD HIP SDK (hipcc, hiprtc) | absent | `C:\Program Files\AMD\ROCm` absent; no `HIP_PATH` | — |
| Intel oneAPI (icx/icpx/dpcpp, SYCL runtime) | absent | `C:\Program Files (x86)\Intel\oneAPI` absent; no `ONEAPI_ROOT` | — |
| Level Zero SDK headers | absent (loader present) | — | headers are MIT, vendorable from `oneapi-src/level-zero` |
| OpenCL SDK headers | absent (loader present) | — | headers Apache-2.0, vendorable from `KhronosGroup/OpenCL-Headers` |
| Vulkan SDK (glslc, glslangValidator, spirv-opt/val, dxc) | absent | `C:\VulkanSDK` absent; no `VULKAN_SDK`; none on PATH | — |
| SPIR-V producers on host | none | no `llvm-spirv`, no clang SPIR-V target, no glslc/clspv | must be added (vendored toolchain) or kernels pre-built in CI |
| PTX producer on host | present (proven, §1.5) | clang 22.1 `-target nvptx64-nvidia-nvcl` (OpenCL C) emits PTX without the CUDA Toolkit; CUDA-C input or libm calls would additionally need libdevice/CUDA headers | `C:\Program Files\LLVM\bin\clang.exe` |

### 1.5 Single-source kernel proof on this host (no vendor SDK installed)

One OpenCL C 1.2 file (`gpu-survey-scratch\m3k_add.cl`, 15 lines, `c[i] = a[i] + b[i]*3`, n = 65 536) run through three device paths plus the CPU, each compared element-wise with a host oracle:

| Path | Toolchain used | Result |
|------|----------------|--------|
| CUDA Driver API | `clang 22.1 -x cl -target nvptx64-nvidia-nvcl -march=sm_75 -Xclang -target-feature -Xclang +ptx70` → PTX 7.0 text → `cuModuleLoadData` (driver JIT) → `cuLaunchKernel` | 0 mismatches, PASS (`probe-single-source-cuda.txt`) |
| OpenCL (NVIDIA ICD) | `clCreateProgramWithSource` + `clBuildProgram("-cl-std=CL1.2")` | 0 mismatches, PASS (`probe-single-source-opencl.txt`) |
| OpenCL (Intel ICD) | same source, same calls | 0 mismatches, PASS |
| CPU (host C) | same `.cl` bytes `#include`d into `m3k_cpu_shim.c` with 4 macros (`__kernel`, `__global`, `get_global_id`), `clang -O2 -mavx2 -mfma` | 0 mismatches, PASS (`probe-single-source-cpu.txt`) |

Observations:
- clang's default PTX ISA for `sm_75` was rejected (`PTX version 4.2 does not support target 'sm_75'`); the PTX ISA version must be pinned explicitly (`+ptx70`). PTX 7.0 / sm_75 is a safe floor for a CUDA 13.x driver (CUDA 13 dropped offline targets below sm_75).
- The NVPTX path needs only a 3-line `#ifdef __NVPTX__` shim for the global id (NVVM special-register builtins) — no libclc, no CUDA Toolkit, no NVRTC.
- All calls were made through `LoadLibrary` + `GetProcAddress` (Python ctypes), i.e. exactly the binding model proposed for the HAL.

Runtime-probe evidence: `gpu-survey-scratch/probe_runtimes.py` (ctypes `LoadLibrary` only, no SDK headers/libs) enumerated CUDA, OpenCL and Level Zero devices — demonstrates the "no link-time vendor dependency" loading model works on this host. Logs: `probe-cuda.txt`, `probe-opencl.txt`, `probe-ze.txt`, `probe-vulkaninfo.txt`, `probe-vswhere.txt`.


---

## 2. Backend options for a portable vCPU JNI layer

Workload assumption (from sibling inventory `VCPU_DONOR_INVENTORY.md`): the first kernel is the RV32IM interpreter — one work-item = one virtual core, int32-only, global memory only, 1-D grid, no barriers / local memory / atomics, budget-sliced launches. Every backend below can express that; the differences are reach, loading, kernel format and toolchain cost.

### 2.1 Summary matrix

| Backend | Vendors reached | Headers licence | Runtime / loader licence | Runtime DLL (Win / Linux) | Kernel format consumed | Runtime compiler in driver? | On this host |
|---------|-----------------|-----------------|--------------------------|---------------------------|------------------------|-----------------------------|--------------|
| CUDA Driver API | NVIDIA | `cuda.h`: NVIDIA CUDA EULA (proprietary) — avoid by self-declaring the ~25 prototypes (cuew / IREE style) | proprietary, ships with NVIDIA display driver (not redistributed by us) | `nvcuda.dll` / `libcuda.so.1` | PTX text (driver JIT) or cubin/fatbin (SASS) | yes (PTX JIT) | **yes** — 595.79, CUDA 13.2, RTX 4060 cc 8.9; PTX path PASS |
| HIP (ROCm / HIP SDK for Windows) | AMD (RDNA2+ on Windows officially) | MIT (`ROCm/hip`, `ROCm/clr`) | Windows: runtime DLL shipped by AMD Adrenalin driver (binary); Linux: `clr` MIT source | `amdhip64_<major>.dll` (name changes per major ABI) / `libamdhip64.so.<major>` | AMDGPU code object (HSACO) per `gfx` arch in a clang-offload-bundler fatbin; `hiprtc` (SDK only) for source; newer generic / `amdgcnspirv` targets reduce per-arch builds (verify driver floor) | no (only via hiprtc from SDK, or comgr for amdgcnspirv) | **absent** — no AMD GPU, no SDK; host LLVM has no AMDGPU target |
| Intel Level Zero | Intel GPUs (and Intel NPU for graph ext, not general compute) | MIT (`oneapi-src/level-zero`) | loader MIT, ships with Intel graphics driver; GPU driver `intel/compute-runtime` MIT (binary on Windows) | `ze_loader.dll` / `libze_loader.so.1` | SPIR-V (OpenCL "Kernel" flavour, physical addressing) or native binary (cacheable via `zeModuleGetNativeBinary`) | yes (IGC in driver) | **yes** — loader 1.21.1, API 1.6, UHD iGPU |
| SYCL (DPC++ / AdaptiveCpp) | Intel native; NVIDIA/AMD via plugins | spec Khronos; DPC++ `intel/llvm` Apache-2.0 WITH LLVM-exception; AdaptiveCpp BSD-2-Clause | DPC++ runtime + Unified Runtime adapters; oneAPI binary toolkit under Intel EULA | `sycl*.dll` + `ur_adapter_*.dll` | compiler-embedded fat binary (SPIR-V + optional PTX/AMDGCN) | n/a (single-source C++) | **absent** — no oneAPI |
| OpenCL 1.2 / 3.0 | NVIDIA, Intel, AMD (+ CPU runtimes: PoCL MIT, Intel CPU RT) | Apache-2.0 (`KhronosGroup/OpenCL-Headers`) | ICD loader Apache-2.0 (Khronos), ships as system `OpenCL.dll`; vendor ICDs proprietary | `OpenCL.dll` / `libOpenCL.so.1` | OpenCL C source (universal); SPIR-V via `clCreateProgramWithIL` (optional in 3.0); cached binaries | yes (every ICD) | **yes** — NVIDIA (OpenCL C 1.2, no IL) + Intel (OpenCL C 1.2, SPIR-V 1.0–1.3, no fp64); source path PASS on both |
| Vulkan compute | NVIDIA, Intel, AMD (+ macOS via MoltenVK Apache-2.0, CPU via lavapipe/SwiftShader) | Apache-2.0 OR MIT (`KhronosGroup/Vulkan-Headers`) | loader Apache-2.0 (Khronos), ships as system `vulkan-1.dll`; `volk` MIT for SDK-free loading | `vulkan-1.dll` / `libvulkan.so.1` | SPIR-V, Vulkan "Shader" flavour (logical addressing; pointers only via `VK_KHR_buffer_device_address`) | yes (pipeline compile in driver) | **yes** — loader 1.4.321; NVIDIA 1.4.329, Intel 1.4.311 |
| CPU (SIMD + threads) | any x86-64 / AArch64 | own code; optional Highway (Apache-2.0 OR BSD-3-Clause), SLEEF (BSL-1.0), ISPC (BSD-3-Clause) | none | `m3vcpu_cpu_<isa>.dll` (own) | AOT native code per ISA level | n/a | **yes** — i9-14900HX, AVX2+FMA3, no AVX-512, 32 threads; CPU-shim PASS |

### 2.2 Per-backend assessment

**CUDA Driver API (runtime-loaded `nvcuda.dll`, PTX JIT)**
- Load: `LoadLibraryExW(L"nvcuda.dll", NULL, LOAD_LIBRARY_SEARCH_SYSTEM32)`; resolve symbols via `GetProcAddress` on the versioned `_v2` names or `cuGetProcAddress_v2` (CUDA 12+). No `cudart`, no toolkit at runtime. Linux: `dlopen("libcuda.so.1")`.
- Kernel: ship PTX with pinned ISA (`.version 7.0`, `.target sm_75` floor); driver JITs forward to any newer arch and caches (`%APPDATA%\NVIDIA\ComputeCache`). Optionally add SASS cubins for hot arches. Never emit a PTX ISA newer than the oldest supported driver (toolkit 13.4 PTX on a 13.2 driver would fail).
- Pros: best NVIDIA performance and feature set (streams, events, graphs, managed memory); tiny surface for our needs (init, ctx, mem alloc/copy, module load, launch, sync ≈ 20–25 symbols); PTX producible by stock clang (proven).
- Cons: NVIDIA-only; proprietary headers (self-declare to avoid EULA questions); WDDM launch latency and **TDR** watchdog (default 2 s) on Windows → kernels must be budget-sliced (the RV32 design already slices).

**HIP / ROCm on Windows**
- Load: `LoadLibraryExW` over a probe list `amdhip64_7.dll`, `amdhip64_6.dll`, `amdhip64.dll` (Blender `hipew` pattern); API mirrors CUDA driver API 1:1 (`hipModuleLoadData`, `hipModuleLaunchKernel`).
- Kernel: HSACO per gfx target bundled into an offload fatbin; needs an AMDGPU-enabled clang (ROCm LLVM or upstream with AMDGPU target — the host's LLVM 22.1 build lacks it).
- Pros: peak AMD performance; CUDA-shaped API → the CUDA backend source can be mechanically mirrored.
- Cons: per-arch binary matrix; DLL name/ABI churn per ROCm major; Windows support narrower than Linux (consumer RDNA2+ subset, fewer runtime features); cannot be tested on this host. Treat as an optional performance backend; AMD is already reachable through Vulkan and OpenCL.

**Intel Level Zero and SYCL**
- Level Zero load: `ze_loader.dll` (system, MIT); `zeInit` → `zeDriverGet` → `zeDeviceGet` → `zeModuleCreate(ZE_MODULE_FORMAT_IL_SPIRV)` → immediate command lists. Proven loadable here (1 driver, API 1.6).
- Level Zero kernel: OpenCL-flavour SPIR-V — needs a SPIR-V producer (clang with SPIR-V backend or `llvm-spirv` translator); none on host today.
- Level Zero pros: lowest-overhead Intel path, explicit memory (USM host/device/shared), MIT end to end. Cons: Intel-only in practice; adds a SPIR-V toolchain to CI; Intel is already covered by OpenCL (same IGC compiler underneath) and Vulkan.
- SYCL: a C++ single-source programming model, not a loader API. Its runtime is large, compiler-version-bound, and its C++ ABI is a poor fit behind a C/JNI boundary; NVIDIA/AMD reach needs extra plugins. **Not recommended as a HAL backend**; its Unified Runtime adapter layer is, however, the closest donor for the HAL shape (§4).

**OpenCL 1.2 / 3.0 (one backend → Intel, AMD, NVIDIA)**
- Load: `LoadLibraryExW(L"OpenCL.dll", …SYSTEM32)` — the Khronos ICD loader, which itself dispatches to vendor ICDs registered by DCH drivers (`OpenCLDriverName` under the display-class key). Proven here for two vendors.
- Kernel: OpenCL C source compiled at run time (`clBuildProgram`) works on every vendor; cache with `CL_PROGRAM_BINARIES` keyed by (device, driver, source hash). SPIR-V IL is **not** portable (NVIDIA rejects it — verified), so ship source.
- Pros: one backend reaches all three GPU vendors plus CPU runtimes; no build-time vendor toolchain; pointer-based kernels map directly to the RV32 SoA layout; TornadoVM's most mature backend uses it.
- Cons: NVIDIA capped at OpenCL C 1.2 language features; vendor investment uneven (AMD Windows OpenCL in maintenance, Apple deprecated); runtime-compile latency on first run (mitigated by binary cache); usually slower than CUDA on NVIDIA.

**Vulkan compute (SPIR-V, cross-vendor)**
- Load: `volk` (MIT) loads `vulkan-1.dll` at run time — no import-lib link. Proven present (1.4) for NVIDIA + Intel here; AMD ships Vulkan in every Adrenalin driver.
- Kernel: Vulkan-flavour SPIR-V produced offline (GLSL via glslang/shaderc, HLSL via DXC, Slang, or **OpenCL C via `clspv`** to keep one kernel language). Binary is vendor-neutral; driver compiles at pipeline creation (pipeline cache).
- Pros: widest long-term cross-vendor reach (Windows, Linux, Android, macOS via MoltenVK); offline SPIR-V = deterministic artefact, no compiler at run time; strong driver quality.
- Cons: most host-side boilerplate (instance/device/queue, descriptor sets, pipelines, barriers); SPIR-V flavour differs from L0/OpenCL SPIR-V; optional features (`shaderInt64`, `shaderFloat64`, 8/16-bit storage, subgroup ops) must be queried; buffer range limits (`maxStorageBufferRange`); no SPIR-V toolchain on this host yet (clspv/glslang must be vendored or run in CI).

**CPU fallback (SIMD + threads)**
- Always available; it is also the correctness oracle (matches the existing `CpuRv32iEngine` + SHA-256 state-root receipts).
- ISA dispatch: either one DLL per ISA level loaded by score (ggml `ggml-cpu-*` pattern: scalar / SSE4.2 / AVX2+FMA / AVX-512) or in-DLL dynamic dispatch (Highway `HWY_DYNAMIC_DISPATCH`). Host proves the need: AVX2 yes, AVX-512 no.
- Threads: own work-stealing pool with dynamic chunking (hybrid P/E cores); expose a "max threads" knob so Java `ForkJoinPool` users can avoid oversubscription.
- Kernel source: the same OpenCL C file compiled as C through a macro shim (proven, §1.5) — one kernel definition, the loop index plays `get_global_id`.
- Note: the RV32 interpreter is branch/gather-heavy; most CPU speed-up comes from threads (32 here), SIMD matters more for future bulk data kernels. JDK 21's incubating Vector API is an optional pure-Java tier with no JNI at all.

---

## 3. Recommended layering

### 3.1 Stack

```text
Java (M3jdk21)   BulkExecutor-style API (jdk.internal.vm.parallel.* is referenced by the
                 tornadovm-bulk adapter per VCPU_DONOR_INVENTORY.md; not present on M3jdk21
                 branch m3/equality-gates-20261009 — rg 0 hits)
      |  JNI, thin: m3vcpu_jni.dll  (arg marshalling, direct ByteBuffer / pinned arrays,
      |  error-code -> exception mapping; no policy, no vendor code)
      v
m3vcpu HAL core: m3vcpu.dll  (C99 ABI, m3vcpu.h)
      |  registry + selector: enumerate backend DLLs next to itself, call probe(),
      |  score, pick (policy AUTO | env M3VCPU_BACKEND=cuda,vulkan,... | explicit)
      |  kernel bundle loader: content-addressed blobs, picks the format each backend accepts
      v
backend DLLs (each exports one symbol: m3vcpu_backend_entry_v1)
  m3vcpu_cpu.dll      always present; ISA variants scored (scalar / AVX2+FMA / AVX-512)
  m3vcpu_cuda.dll     LoadLibrary nvcuda.dll        <- PTX (pinned ISA floor)
  m3vcpu_opencl.dll   LoadLibrary OpenCL.dll        <- OpenCL C source (+ binary cache)
  m3vcpu_vulkan.dll   volk -> vulkan-1.dll          <- Vulkan SPIR-V
  m3vcpu_ze.dll       LoadLibrary ze_loader.dll     <- OpenCL-flavour SPIR-V   (optional)
  m3vcpu_hip.dll      LoadLibrary amdhip64_N.dll    <- HSACO fatbin            (optional)
```

Hard rules:
1. **No vendor import-table entries.** Every backend DLL links only `kernel32`/CRT; vendor runtimes are `LoadLibraryExW(..., LOAD_LIBRARY_SEARCH_SYSTEM32)` (Linux: `dlopen` by SONAME). CI gate: `dumpbin /dependents` (or `llvm-readobj --coff-imports`) must list no `nvcuda`, `OpenCL`, `vulkan-1`, `ze_loader`, `amdhip64`.
2. **Probe never crashes.** A missing driver returns "unavailable" with a reason string; the CPU backend is the guaranteed floor.
3. **Backends own nothing Java-specific.** JNI lives only in `m3vcpu_jni.dll`; the HAL is usable from FFM (JDK 22+ final, preview in 21), Python ctypes, or C tests.
4. **Watchdog-aware launches.** Windows TDR (≈2 s) applies to CUDA, OpenCL, Vulkan and L0 under WDDM; the HAL exposes budget slicing and the scheduler keeps each launch well under the limit. System TDR settings are never changed.
5. **CPU oracle parity.** Every device result can be re-checked against the CPU backend (existing SHA-256 state-root receipts); a backend is admitted per device only after a differential pass.

### 3.2 C ABI sketch (versioned function table)

```c
/* m3vcpu.h — C99, no vendor types. */
#define M3VCPU_ABI_V1 1u
typedef enum { M3K_PTX = 1, M3K_OPENCL_C, M3K_SPIRV_VK, M3K_SPIRV_CL, M3K_HSACO_FATBIN, M3K_CPU_SYMBOL } m3k_format;
typedef struct { m3k_format format; uint32_t min_isa; const uint8_t *bytes; size_t size; uint8_t sha256[32]; } m3k_blob;
typedef struct m3vcpu_ctx m3vcpu_ctx; typedef struct m3vcpu_buf m3vcpu_buf; typedef struct m3vcpu_kernel m3vcpu_kernel;

typedef struct m3vcpu_backend_v1 {
  uint32_t abi, struct_size; const char *name;        /* "cpu","cuda","opencl","vulkan","ze","hip" */
  int  (*probe)(m3vcpu_probe *out);                    /* score + reason; must be safe without driver */
  int  (*device_count)(void);
  int  (*device_info)(int idx, m3vcpu_device_info *out);
  int  (*ctx_create)(int idx, const m3vcpu_ctx_opts *o, m3vcpu_ctx **out);
  void (*ctx_destroy)(m3vcpu_ctx *c);
  int  (*buf_alloc)(m3vcpu_ctx *c, size_t bytes, uint32_t flags, m3vcpu_buf **out);
  int  (*buf_write)(m3vcpu_ctx *c, m3vcpu_buf *b, size_t off, const void *src, size_t n);
  int  (*buf_read)(m3vcpu_ctx *c, m3vcpu_buf *b, size_t off, void *dst, size_t n);
  void (*buf_free)(m3vcpu_ctx *c, m3vcpu_buf *b);
  int  (*kernel_load)(m3vcpu_ctx *c, const m3k_blob *blobs, size_t n, const char *entry, m3vcpu_kernel **out);
  int  (*launch_1d)(m3vcpu_ctx *c, m3vcpu_kernel *k, uint64_t global, uint32_t local, const m3vcpu_arg *a, uint32_t na);
  int  (*finish)(m3vcpu_ctx *c);
  const char *(*last_error)(m3vcpu_ctx *c);
} m3vcpu_backend_v1;

M3VCPU_EXPORT const m3vcpu_backend_v1 *m3vcpu_backend_entry_v1(void);
```

`struct_size` + `abi` allow appending members without breaking older backends (Vulkan/OpenXR loader style). 3-D launches, events and async copies are v2 additions, not v1.

### 3.3 One portable kernel description

Recommended: **"M3 kernel C" = a restricted OpenCL C 1.2 subset** (global buffers, int32/uint32/int64, 1-D NDRange; local memory/barriers/atomics opt-in by feature flag; no images, no fp64 requirement — Intel iGPU here lacks fp64) plus a tiny portability header (`M3_GID0`, address-space macros). Build matrix (CI, content-addressed outputs in the kernel bundle):

| Target | Producer | Status |
|--------|----------|--------|
| OpenCL (all vendors) | ship source as-is; runtime `clBuildProgram`; cache binaries | proven on NVIDIA + Intel |
| CUDA | `clang -x cl -target nvptx64-nvidia-nvcl -march=sm_75 +ptx70` → PTX | proven on RTX 4060 |
| CPU | same source compiled as C via macro shim, per ISA level | proven (AVX2 build) |
| Vulkan | `clspv` (Apache-2.0) OpenCL C → Vulkan SPIR-V; validate with `spirv-val` | toolchain to vendor (absent here) |
| Level Zero / OpenCL-IL | clang `-target spirv64` (LLVM SPIR-V backend) or `llvm-spirv` | toolchain to vendor (absent here) |
| HIP | clang `-target amdgcn-amd-amdhsa -mcpu=gfx…` (or generic / amdgcnspirv) + offload bundler | needs AMDGPU-enabled LLVM (absent here) |

Rejected alternatives for the kernel description: TornadoVM-style Java-bytecode JIT (large Graal dependency; drivers GPLv2+CE), MLIR/IREE compiler (heavy for a handful of kernels), GLSL/HLSL as master language (no pointers; does not feed OpenCL/PTX/CPU).

### 3.4 Recommended backend set

| Phase | Backends | Vendor coverage | Rationale |
|-------|----------|-----------------|-----------|
| v1 | **CPU** (threads + AVX2/AVX-512 dispatch), **CUDA Driver API** (PTX), **OpenCL** (source) | NVIDIA via CUDA; Intel + AMD + NVIDIA-fallback via OpenCL; all hosts via CPU | every path already proven on this host with zero SDKs; one kernel source; smallest toolchain |
| v2 | **Vulkan compute** (clspv SPIR-V) | Intel, AMD, NVIDIA, + macOS/Android reach | long-term cross-vendor path as OpenCL investment declines (AMD/Apple); needs SPIR-V toolchain in CI |
| v3 (optional, perf) | **Level Zero** (Intel), **HIP** (AMD) | vendor-peak paths | only if profiling shows OpenCL/Vulkan gaps; HIP cannot be validated on this host |
| no | SYCL, DirectML/D3D12 | — | SYCL: heavy C++ runtime/compiler lock-in (use as pattern donor only); D3D12: Windows-only |

Default `AUTO` order on a host like this: `cuda (dGPU) > vulkan > opencl > ze > hip > cpu`, with integrated GPUs scored below the CPU for interpreter workloads unless benchmarked otherwise (UHD iGPU, 32-thread CPU).

---

## 4. Donors (inspiration / pattern sources)

Ledger with commit SHAs and decisions: `FOSS_REUSE_DECISION.tsv` (20 rows; OpenCLAW searched first — local harvest `E:\openclaw-harvest`, 0 GPU-runtime hits → NO_FIT). Commits are `git ls-remote HEAD` on 2026-10-12; licences from the GitHub licence API / LICENSE files at that HEAD.

| Donor | Licence | Commit (HEAD 2026-10-12) | What to borrow |
|-------|---------|--------------------------|----------------|
| TornadoVM (`beehive-lab/TornadoVM`) | per module: Apache-2.0 (tornado-api, annotation, matrices, OpenCL headers…); **GPL-2.0 with Classpath exception (tornado-runtime, tornado-drivers)** | `87a465f9` | backend split `tornado-drivers/{opencl,ptx,spirv,cuda,metal}`: one IR, three codegens (OpenCL C / PTX / SPIR-V), SPIR-V dispatched through Level Zero or OpenCL; per-backend JNI shim. Pattern only (drivers are GPLv2+CE). Sibling note: tornado-api 6.1.0 jar contains one GPL-2.0-only file (`Matrix8x8Float.java`). |
| levelzero-jni (`beehive-lab/levelzero-jni`) | MIT | `5f78a367` | thin JNI-over-L0 shape |
| Vulkan Kompute (`KomputeProject/kompute`) | Apache-2.0 | `267e019e` | `Manager` / `Algorithm` (pipeline + SPIR-V + push constants) / `Sequence` (recorded command buffer) / `Tensor` (buffer) — the minimal Vulkan-compute object model |
| oneDNN (`uxlfoundation/oneDNN`) | Apache-2.0 | `3971fb1f` | `engine` / `stream` / `memory` separation; vendor subtrees `src/gpu/{intel,nvidia,amd,generic}`; runtime chosen at **build** time (`DNNL_GPU_RUNTIME`) — counter-example for our runtime loading |
| OpenVINO (`openvinotoolkit/openvino`) | Apache-2.0 | `34f9af8d` | device plugins as separate shared libraries loaded on demand (`src/inference/src/dev/{core_impl.cpp,plugin.cpp,iplugin.cpp}`, `src/plugins/{intel_cpu,intel_gpu,intel_npu}`); `AUTO` / `HETERO` meta-devices = selection policy |
| oneAPI Unified Runtime (`intel/llvm` `unified-runtime/`) | Apache-2.0 WITH LLVM-exception | `3ae8b8c5` | closest HAL shape: C ABI + loader + runtime-loaded adapters `source/adapters/{cuda,hip,level_zero,opencl,native_cpu}` |
| IREE (`iree-org/iree`) | Apache-2.0 WITH LLVM-exception | `3fd498c0` | HAL objects (device / allocator / buffer / executable / command buffer) and drivers `runtime/src/iree/hal/drivers/{cuda,hip,vulkan,local_task}`; X-macro symbol tables `cuda_dynamic_symbol_table.h` for SDK-free `nvcuda` loading; executable-format tags per target |
| ggml (`ggml-org/ggml`) | MIT | `ffa4e8b8` | `src/ggml-backend-reg.cpp` + `ggml-backend-dl.cpp`: backend DLLs discovered and scored at run time; per-ISA CPU backend variants (`src/ggml-cpu`) |
| Blender Cycles `cuew` / `hipew` (`blender/cycles` `third_party/`) | Apache-2.0 (verify per file) | `a456b761` | SDK-free typedefs + `LoadLibrary` of `nvcuda.dll` / `amdhip64_*.dll` with DLL-name probe lists |
| volk (`zeux/volk`) | MIT | `0dc3ce00` | direct-import candidate: Vulkan meta-loader, no link to `vulkan-1.lib` |
| Khronos OpenCL-Headers / Vulkan-Headers / OpenCL-ICD-Loader / Vulkan-Loader | Apache-2.0; Vulkan-Headers Apache-2.0 OR MIT; Vulkan-Loader Apache-2.0 (+ more-permissive exceptions) | `30bc20a8` / `c4685086` / `5192c84f` / `6b45d762` | headers = direct-import candidates; loaders = vendor-dispatch pattern (`struct_size`-versioned dispatch tables) |
| Level Zero (`oneapi-src/level-zero`) | MIT | `9e66ef89` | `include/ze_api.h` direct-import candidate |
| Highway (`google/highway`) | Apache-2.0 OR BSD-3-Clause | `b93ad441` | CPU runtime ISA dispatch (`HWY_DYNAMIC_DISPATCH`, `foreach_target.h`) |
| clspv (`google/clspv`) | Apache-2.0 | `f2b01dd6` | build-time OpenCL C → Vulkan SPIR-V (keeps one kernel language) |
| chipStar (`CHIP-SPV/chipStar`) | MIT | `278ec566` | reference only: HIP/CUDA over OpenCL/L0 SPIR-V (too heavy for the HAL) |
| first-party `hsoliwal/synexia-tornadovm` `m3/cpu-gpu` | Apache-2.0 | `3bf82671` | kernel contract + CPU oracle + receipts (see `VCPU_DONOR_INVENTORY.md`) |

All rows are recorded as pattern/inspiration or direct-import *candidates*; no donor code has been copied. Any later copy must keep the licence header and add a `THIRD_PARTY_NOTICES.md` entry.

---

## 5. Risks and open items

| # | Item | Impact | Action |
|---|------|--------|--------|
| R1 | No AMD hardware on this host | HIP / AMD-Vulkan / AMD-OpenCL paths unverified | run the single-source probe on an AMD node (fleet) before admitting AMD |
| R2 | CUDA toolkit residue v13.4 newer than driver 13.2 | PTX from toolkit 13.4 would fail JIT on this driver | pin PTX ISA (7.0) in the build; record driver floor in kernel bundle metadata |
| R3 | NVIDIA OpenCL rejects SPIR-V | cannot unify OpenCL + L0 on SPIR-V | ship OpenCL C source for the OpenCL backend |
| R4 | No SPIR-V toolchain on host | Vulkan / L0 kernels cannot be built locally | vendor clspv + SPIRV-Tools (Apache-2.0) or build in CI |
| R5 | Windows TDR watchdog | long kernels reset the GPU | budget-sliced launches (already in RV32 design); never touch TdrDelay |
| R6 | Hybrid P/E CPU, no AVX-512 | static partitioning and AVX-512-only paths underperform / crash | dynamic chunking; runtime ISA dispatch with scalar floor |
| R7 | `cuda.h` licence | EULA terms on proprietary header | self-declare the needed prototypes (cuew / IREE pattern) |
| R8 | Intel iGPU lacks fp64 | fp64 kernels fail on Intel | keep fp64 an optional feature flag in M3 kernel C |
| R9 | `nvptx-arch.exe` hung > 60 s under load | tooling probes can stall | keep timeouts on every device probe; HAL `probe()` must be time-bounded |

---

## Appendix — evidence files (this directory)

`probe-vswhere.txt`, `probe-vulkaninfo.txt`, `probe-cuda.txt`, `probe-opencl.txt`, `probe-ze.txt`, `probe-single-source-{cuda,opencl,cpu}.txt`, `probe-donor-heads-{1,2}.txt`, `probe-donor-licenses-{1,2,3}.txt`, `probe-donor-paths-{1,2,3}.txt`, `FOSS_REUSE_DECISION.tsv`; scripts and kernels in `gpu-survey-scratch/` (`probe_runtimes.py`, `run_single_source.py`, `m3k_add.cl`, `m3k_add.ptx`, `m3k_cpu_shim.c`, `probe_donor_paths.py`, `scratch-ledger-rows.sh`).
