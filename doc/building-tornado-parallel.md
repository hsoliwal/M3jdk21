<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Build the Tornado-backed parallel API

M3JDK21 uses the `hsoliwal/synexia-tornadovm` fork as the source implementation
for this parallel-execution backend. The existing `jdk.m3.parallel` API,
`BackendProvider` service, Java reference implementation, verified-result
checks, and `java.base` bulk scheduler remain the API and execution owners.

The optional `jdk.m3.tornado` module registers backend ID `tornado` and connects
the existing integer-add and integer-bitcount operations to a real Tornado
execution plan. It retains the RV32IM classes already supplied by that module.
The existing module names `m3.parallel`, `tornado.api`, and `tornado.runtime`
remain unchanged for module-path compatibility.

## Source and toolchain

The build consumes a checkout of the fork at the exact revision in
`make/tornado/source-lock.json`. That lock seals the compiler, runtime, OpenCL
backend, resources, and notices actually used by this build. A source mismatch
is an error. Build-tool execution does not rewrite the fork's source.

Use a built M3JDK21 image containing
`java.base/jdk.internal.vm.parallel.BulkTask`. A stock JDK without the M3 bulk
contract is not an acceptable image input. Java 21 preview support is needed by
the fork's Foreign Function and Memory API implementation. An OpenCL loader and
a usable vendor implementation must be installed on the execution host; neither
a service-provider count nor the presence of a loader establishes a device.

From a configured M3JDK21 checkout:

```sh
make tornado-parallel-image TORNADO_SOURCE=/absolute/path/to/synexia-tornadovm
```

This builds the normal JDK image first, then compiles and links the selected
fork modules and the M3JDK21 parallel module into a separate image under the
configured build directory's `tornado-parallel/image`. The ordinary JDK build
does not acquire an implicit download or a dependency on a machine-local
Tornado installation. The historical Tornado 7.2 build lane remains available
through its existing entry point.

The output directory must be new. Move the preceding `tornado-parallel` output
before repeating the make target. To use an already built M3JDK21 image and
choose a separate output directory, run:

```sh
python3 make/tornado/build_parallel.py \
  --tornado-source /absolute/path/to/synexia-tornadovm \
  --jdk-home /absolute/path/to/m3jdk21-image \
  --out /absolute/path/to/new-tornado-parallel-output
```

Adding `--verify-only` validates the sealed source inputs and installed M3 bulk
API without creating the output directory.

The installed image includes `bin/m3-parallel`, which enables the required
modules and native access and disables Tornado's Java bailout and virtual-device
modes. Run parallel applications through this launcher. Ordinary Java programs
can continue to use the image's `bin/java`.

## Existing API behavior

Select the actual backend with `Backend.of("tornado")`, or use the existing
`Backend.Policy.ACCELERATOR` policy. The provider probes and retains the
runtime's actual default device. CPU, GPU, and accelerator capabilities reflect
that device; an OpenCL CPU is not reported as a GPU. An absent runtime, absent
device, virtual backend, or enabled bailout mode is reported as unavailable.
Explicit accelerator selection then fails. The Java reference backend remains
available through `Backend.reference()`.

The supported operation IDs are:

| Operation | Bindings | Result |
| --- | --- | --- |
| `m3.parallel.int-add.v1` | left, right, sum | Java `int` addition, including overflow |
| `m3.parallel.int-bitcount.v1` | bits, counts | `Integer.bitCount` for all 32-bit patterns |

Each binding has the logical grid length, which is at most 262,144 elements.
The provider checks shape and device work-group limits before execution.
Rounded work-items stay outside the logical array domain. Unsupported kernel
IDs are rejected rather than interpreted as arbitrary offloadable Java code.

Each `Plan` owns one persistent Tornado execution plan and bounded native
staging buffers. Device storage starts at zero. `Transfer.ONCE`,
`Transfer.EACH_LAUNCH`, and `Transfer.ON_DEMAND` retain their existing meanings;
downloads remain explicit. Later uploads refresh only the selected binding.
Closing a plan releases device allocations before its native staging arena.

The existing `M3ParallelIntAddProvider` and `M3ParallelBitCountProvider` can be
registered with `BulkScheduler`. Both continue to use `Verified.run`, comparing
all bound results with the independent Java reference before returning their
existing receipt. This is the existing scheduler and verification path, now
connected to Tornado's backend.

## Product acceptance

`test/jdk/jdk/m3/parallel/TornadoParallelTest.java` is an explicit test for the
optional image. Its unavailable mode checks deterministic refusal and Java
reference availability. Its device and image modes require real device
execution, test both kernels and transfer policies, and verify class/module
identity in the installed image. Run hardware tests with bailout recovery and
virtual-device modes disabled. A CPU OpenCL result is CPU-device evidence; GPU,
CUDA, Metal, and other operating systems require their own execution checks.

Canonical source transformations, exact preimages and postimages, recipe replay,
fixed-point and refusal checks, licensing records, and test logs remain in the
paired `com.synexia` recipe crate
`synexia-openrewrite-recipes/crates/m3jdk21-tornado-parallel-20261115`.
The integration adds no Synexia runtime dependency and makes no comparative
performance or complete TornadoVM API coverage claim.
