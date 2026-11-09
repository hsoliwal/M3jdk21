<!-- SPDX-License-Identifier: Apache-2.0 -->
# m3/ports/vcpu/native — provenance

Licence of every file in this tree: Apache-2.0 (SPDX header on each file). Written for leaf P2 of
`m3-parallel-vcpu-design.md` (2026-10-12). **No third-party source text is copied into this tree**; donors below were
read and their semantics or patterns re-expressed. Reuse decisions: `FOSS_REUSE_DECISION.tsv` (this directory) plus
the design ledger `m3/docs/m3sdk/FOSS_REUSE_DECISION.tsv` (OpenCLAW searched first = NO_FIT).

## Semantic donor (first party, ported)

| Donor | Commit | Files | Licence | Use |
|---|---|---|---|---|
| `hsoliwal/synexia-tornadovm` `m3/cpu-gpu` (cpu-gpu-rv32im) | `3bf826714a48d7fdf346ffde8a0fc25c525572c5` | `Rv32iKernel.java`, `Rv32i.java`, `CpuRv32iEngine.java` | Apache-2.0 | `kernels/rv32im_slice.cl` re-expresses `Rv32iKernel.execute` in M3 kernel C with identical observable semantics; `include/m3vcpu_rv32im.h` restates the `Rv32i` ABI constants; `tests/rv32_ref.c` follows the `CpuRv32iEngine` shape. |
| same | same | `Rv32i.java`, `Rv32iBatch.java`, `CpuRv32iEngine.java`, `Rv32iAssembler.java` | Apache-2.0 | Compiled **in place** (never copied) by the `m3vcpu_jni_oracle_parity` ctest via `-DM3VCPU_DONOR_ORACLE_DIR=<staged donor dir>`. |

Read from the staged SDK-intake copy `m3/vendor/synexia/synexia-tornadovm/m3/cpu-gpu/` (not yet in this repository;
byte-identical to the pinned commit per `m3/docs/m3sdk/VCPU_DONOR_INVENTORY.md` §0).

## Pattern donors (no code copied)

| Donor | Commit | Files read (sha256 of the bytes read) | Licence | Pattern adopted | Not adopted |
|---|---|---|---|---|---|
| `cnlohr/mini-rv32ima` | `84858f58cb41899705e2ff2d6ee3b2d5c0795bfe` | `mini-rv32ima/mini-rv32ima.h` (`049b916d9ad628fbe9f67fd3401412c0100b0369efc93acdf81c0c0d51293dc1`) | MIT (file header: BSD, MIT or CC0) | single opcode switch producing a pending `rd` value whose write-back is suppressed when the instruction traps | its trap model (`RAM_SIZE-3` bound, no misaligned-access traps, `mcause+1` codes), RV32A, Zicsr, MMIO, timer — all outside our RV32IM + XM3_HALT contract |
| `mirror2008/GPUOS` | `c06b8d155ce62c31570bfb5b6de5c74331cec6ed` | `gpu-rv32ima/rv32ima.cl` (`b0d2caef87fa9efaf6ff21f858311931cbc758d68c790dc3a67f6f248e6dde4e`), `gpu-rv32ima/rv32ima_fast.cl` (`e1b12b0462e1a52e5d2db58fb3d057e7dabc88a48ff4dd5f9771a3455aa502e1`), `gpu-rv32ima/gpuos.py` (`4307ad199c6e3b1907f9e8615ff4fc7392caafa177b26298460d4ed5ee29edb5`) | MIT (© 2026 Yanda2008) | one VM per work-item with an `id >= n` guard; per-VM base pointers with 64-bit offsets (`rv_run_multi`); VM state copied global → private at kernel entry and back once at exit; instruction-budget dispatch slicing under the Windows TDR with host re-launch; host batching bounded by `CL_DEVICE_MAX_MEM_ALLOC_SIZE` (the OpenCL backend reports an over-size buffer so the caller shards) | `__local` (SLM) register file with `reqd_work_group_size` (P5 performance candidate: needs a local-memory opt-in in M3 kernel C); instruction fusion; adaptive per-dispatch instruction count (belongs to the Java scheduler) |
| `gpu-survey-scratch/m3k_cpu_shim.c` (first party) | — | `m3k_cpu_shim.c`, `m3k_add.cl` | Apache-2.0 | same `.cl` bytes compiled as C through a macro shim | the process-global `get_global_id` (replaced by a per-call parameter `M3_GID_PARAM`, thread-safe) |
| M3jdk21 `m3/ports/precompute` (first party) | branch `m3/dependency-source-inline-20261010` HEAD | `src/main/native/CMakeLists.txt`, `*_jni.c`, `M3CodeTextSignalBatchNative.java` | Apache-2.0 | CMake layout, `/W4 /WX` and `-Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic`, `native-path-<CONFIG>.txt`, loader chain (property → path file → `System.loadLibrary`) | — |
| ggml `src/ggml-backend-reg.cpp`, Khronos OpenCL/Vulkan loaders, OpenVINO AUTO, IREE `cuda_dynamic_symbol_table.h`, Highway dispatch | per design ledger | per design ledger | MIT / Apache-2.0 (+LLVM exception) | backend DLL discovery and scoring; `struct_size`-versioned dispatch tables; AUTO selection policy; X-macro dynamic symbol table (`m3vcpu_cl_abi.h`); runtime ISA dispatch | — |
| Khronos OpenCL 1.2 specification / `KhronosGroup/OpenCL-Headers` `cl.h` | design ledger `30bc20a8` | — | Apache-2.0 | enum values and entry-point signatures restated in `src/backends/opencl/m3vcpu_cl_abi.h` (ABI facts, no header text) | vendoring the headers (not needed) |

Excluded by licence and not read: `gaidardzhiev/vertigo`, `chimera` (GPL-3.0).

## WebGPU backend (leaf P3w, 2026-10-12)

Files: `src/backends/webgpu/` (backend, WGSL-twin table header, embed script), `kernels/rv32im_slice.wgsl`,
`tests/test_webgpu.c`, `tests/java/NativeVcpuOracleParityWebgpu.java`. All Apache-2.0, written for P3w.

| Donor | Commit / version | Licence | Use |
|---|---|---|---|
| `google/dawn` (Chrome's WebGPU engine) | `c943ea32b614ff53e4fe410fc8f4968b6b8fe46d`, release `v20261009.190412` (Windows, Release, static `lib/webgpu_dawn.lib`) | BSD-3-Clause (`third_party/` parts carry their own licences) | Linked statically into `m3vcpu_webgpu.dll` when `M3VCPU_BUILD_WEBGPU=ON` (`M3VCPU_DAWN_DIR`). Only standard `webgpu.h` entry points are called. No Dawn source or header text is copied into this tree; the header is used in place. `bin/tint.exe` from the same release was used offline to validate the WGSL (WGSL, SPIR-V and HLSL+DXC outputs). |
| `webgpu-native/webgpu-headers` `webgpu.h` (as shipped by Dawn) | same release | BSD-3-Clause | The C ABI the backend codes against, so wgpu-native stays an alternate provider. |
| First-party `m3/docs/m3sdk/webgpu/rv32im_alu.wgsl`, `dawn_alu_probe.c` | branch `m3/parallel-vcpu-20261012` (PR #779) | Apache-2.0 | MULHU from four 16-bit partial products, the MULH/MULHSU signed corrections and the RISC-V div/rem corner guards are restated in `rv32im_slice.wgsl`; the probe's `webgpu.h` sequence (instance, adapter, device, WGSL module, compute pipeline, bind group, dispatch, map) is the template of the backend. |
| First-party `m3/docs/m3sdk/webgpu/msvc_stl_vectorized_shim.c` | same | Apache-2.0 | Compiled in place via `M3VCPU_DAWN_STL_SHIM` only to link the prebuilt Dawn with MSVC 14.44; drops out with a source-built Dawn. |
| `kernels/rv32im_slice.cl` (this tree) | sha256 `8ecfc67e…cfe66` | Apache-2.0 | Semantic source of the WGSL twin. The pin `m3k-twin-of: rv32im_slice.cl sha256=…` in the WGSL is checked by `m3vcpu_webgpu_embed.cmake`; a changed `.cl` stops the webgpu build until the twin is restated and re-proven. At run time the backend serves the twin only for the byte-identical `M3K_OPENCL_C` bundle blob it was built with. |

Run-time binaries (not in the tree, not redistributed): Dawn's D3D12 path loads `dxcompiler.dll` + `dxil.dll`
(tests copy Chrome 154.0.8037.100's into the build directory via `M3VCPU_WEBGPU_RUNTIME_DLLS`; licence review row in
`FOSS_REUSE_DECISION.tsv`), and its Vulkan path loads `vulkan-1.dll` from the backend's own directory (tests copy the
System32 loader there). The prebuilt Dawn also needs an in-process `msvcp140.dll` >= 14.40 (MSVC's std::mutex change);
the stock JDK 21.0.9 launcher preloads its own 14.36 copy, so the probe reports the backend unavailable there
(`M3VCPU_WEBGPU_MIN_MSVCP`) instead of crashing.

## THIRD_PARTY_NOTICES

Not required for this tree: no third-party code is copied or adapted, only patterns and specification values. If a
later leaf adapts code from mini-rv32ima or GPUOS, that file must carry the donor's MIT notice and a
`THIRD_PARTY_NOTICES.md` entry with repo, commit and path.
