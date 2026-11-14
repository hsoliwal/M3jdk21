<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 AND MIT -->

# Residual vector quantization provenance and numerical contract

The Java `M3NcmQuantizer` is a scalar adaptation of the multilevel residual
quantization design reviewed in `HazyResearch/ncm`. The optional C++ nearest-code
kernel and JNI boundary are original Synexia implementation of squared Euclidean
selection. The model is immutable, uses primitive arrays, and does not load a
Python, PyTorch or other third-party runtime.

## Immutable donor custody

Repository: https://github.com/HazyResearch/ncm

Reviewed commit: `c70274273f489abc73832b2b98584a8483b6b203`

Reviewed tree: `0e108d439909caf135d8985636c1925ed7c3dd05`

| Source | Git blob |
| --- | --- |
| `models/quant.py` | `8262ab80e782ff6e59a60ee59cff58eb0c2ec804` |
| `models/next_concept.py` | `25ee56d5bbcaa21558768b279490ad21f5a12cb9` |
| `models/vqvae.py` | `2aa3236a1e4b2f9a7bc7945ec592d5b087ec67a5` |
| `tokenization/vqvae_tokenizer.py` | `711866d77c3a6b2ffb5ebd300eeb4689cfa8c4f0` |
| `LICENSE` | `8c9c7f4ff5fc026b93c55dcce141d142bd3b650c` |

Paper: https://arxiv.org/abs/2610.14162v1

Modified 2026-11-14 by Hitesh Soliwal and Contributors to the Synexia Project:
translated the reviewed numerical design into an immutable Java primitive-array
owner, defined deterministic scalar arithmetic and exact configuration hashes,
added namespaced code results, bounded validation, partial reconstruction,
explicit backend selection, a primitive-only JNI kernel and differential tests.
The supplied implementation is not a copy of the complete donor framework.

## Supported numerical scope

Callers supply position-major/channel-minor float32 latent arrays. At each level,
the current residual is area-pooled using floor/ceiling bins, the nearest vector
in the level-specific codebook is selected, and that code vector field is
upsampled using half-pixel linear coordinates with edge clamping. Optional
frozen Conv1d residual transforms use zero padding and output/input/kernel
weight order. Contributions accumulate into the reconstruction and are removed
from the residual. Equal-resolution successive levels are supported.

Nearest distance is accumulated in ascending channel order with binary64
intermediates and no floating-point contraction. Exact ties choose the lowest
code index; signed zeros tie numerically. Float32 rounding happens at produced
components. Nonfinite inputs and nonfinite results are rejected. Codebook roots
include dimensions, every raw float bit, level order, transform parameters and
the algorithm's semantic version; backend selection is excluded because it
must preserve these semantics.

This component does not implement a trained text encoder, learned resampling,
GELU residual convolution stacks, a transformer, a text decoder, stochastic
top-k/top-p generation, checkpoint loading, training or RLCD. It does not turn
text identities into latent embeddings or establish that caller-supplied
latents came from the associated text. Those are separate model/provenance
obligations. It does not claim bitwise PyTorch checkpoint parity, lossless text
reconstruction, semantic accuracy, or a measured speedup.

The donor's top-k sampler retains all values tied at the cutoff. This primitive
selects a deterministic nearest code and does not purport to implement that
sampling contract.

`JAVA` always uses the scalar Java oracle. `AUTO` checks whether a library was
explicitly loaded before the invocation; it never loads one automatically.
`NATIVE` refuses an invocation if no library has been loaded. A load, ABI or
native invocation failure is propagated and is not converted into silent
fallback. Native calls copy primitive array regions, own all scratch storage
within the call, and retain no Java pointers, references or `JNIEnv`.

The CMake `M3_NCM_TESTS` option builds the standalone C ABI oracle. The
`M3_NCM_SANITIZERS` option instruments that executable with address and
undefined-behavior sanitizers on supported non-MSVC toolchains. JVM JNI parity
is a separate JUnit invocation using the explicit `m3.ncm.library`
system property and `-Xcheck:jni`.

## Projection primitives in the same native owner

`ncm_projection_jni.cpp` serves `jdk.m3.llm.projection.M3NcmNativeOps` from this
library (`m3_ncm`); there is no second NCM native library. Its area/half-pixel
linear resize, three-tap Phi transform and tied-codebook logit batches are a
numerical translation of `models/quant.py` and `models/next_concept.py` at the
reviewed commit above, received from hsoliwal/com.synexia#17414 (canonical
source `d7564cadc43864f0c9c94a7129b2e0fdfa089218`) through M3jdk21 #3033. Its
nearest-code entry calls `m3_ncm_nearest_expanded_f32`, the second entry of the
same `ncm_kernel.cpp` (sharing layout and finiteness validation with
`m3_ncm_nearest`): the donor's FP32 expanded-norm distance
`(rowNorm + codeNorm) - 2 * dot` with channel-ascending binary32 sums and the
lowest index on exact ties, in the operation order of `M3NcmOps.nearest`, so
native and `Mode.JAVA` select the same codes. `NcmNativeOpsNearestParityTest`
pins a general and a near-tie corpus to the received kernel's outputs.

Modified 2026-11-14 by Hitesh Soliwal and Contributors to the Synexia Project:
moved the received C17 JNI (`m3/llm/native/ncm_jni.c`, library `m3_llm_ncm`)
into this C++20 target under the same strict warning and floating-point flags,
moved nearest-code selection into the shared kernel file, and loaded the library from
`-Dm3.ncm.library` or `System.loadLibrary("m3_ncm")`. Tied scores use libc
`exp` against Java `StrictMath.exp` with the `2e-5` parity tolerance.

## Upstream MIT notice

MIT License

Copyright (c) 2025 hermann kumbong

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
