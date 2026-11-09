<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# TornadoVM in M3JDK21

## Product contract

M3JDK21 owns the delivered accelerator compiler, runtime, selected backend modules,
bulk-provider module, launcher and release image. The source-build lane receives
TornadoVM 7.2.0 into `m3/vendor/tornadovm` and builds from those checked-in sources.
The installed image contains the resulting named modules. It does not fetch a
runtime or require a separate Synexia service, workspace or installed Tornado SDK.

The canonical integration recipe remains in `hsoliwal/com.synexia`, under
`synexia-openrewrite-recipes/recipes/m3jdk21-tornado-integration-20261021`.
The recipe generates exact reviewed target postimages; M3JDK21 owns its target
module boundaries, acceptance evidence and release decisions. Correct the recipe
and regenerate its outputs. Preserve failed attempts and corrections in its
append-only improvement memory, following `SYNEXIA-MOP-RECIPE-MEMORY-1`.

## Pinned source and dependency decision

The target baseline is `2779aba3f98f31c324be43f731be7914e1a05935`.
The Tornado source is upstream commit
`87a465f9185eb8e822f9532076fb4a5bc44fdf2a`, tree
`56c06ae90c9330bd1243bdf75d2cf653e8611d41` (7.2.0).

The inspected Synexia integration recorded a 4be6c4 fork source pin while using
7.2.0-jdk21 runtime artifacts. The inspected fork tip cc7984 lacks APIs used by
that newer runtime adapter. This integration therefore builds the coherent 7.2.0
source reactor. It does not certify the mismatched source/runtime combination or
silently treat unported fork changes as included. The legacy RV32IM provider has
its own exact source pin and custody record.

`m3/runtime-integration/tornado/source-lock.json` records every selected donor
path, original Git mode, blob identity and raw SHA-256, all excluded paths and
their reasons, and the exact runtime dependency JAR hashes. Upstream source bytes
and headers are preserved. Compiler support dependencies are explicitly pinned;
their binary admission is distinct from compiling the Tornado source modules.
Maven plugins and build-time transitive dependencies have their own build logs.

The source selection excludes upstream repository automation and agent prompts,
documentation artwork, and precompiled SPIR-V demonstration kernels. These are
not runtime modules. Their exclusions are recorded individually. Runtime,
compiler, backend Java/native source, build descriptors and source tests remain
available for the selected lane. An excluded sample is never counted as executed.

## Existing ownership and compatibility

The internal `jdk.internal.vm.parallel` task, result, provider and scheduler
owners remain authoritative. Their public descriptors and behavior are unchanged.
There is no second bulk scheduler and no accelerator dependency in `java.base`.

The existing `com.m3.tornado.Rv32iBulkExecutorAdapter` and operation
`m3.rv32im.slice` keep their input schema, copied-batch behavior, geometry checks,
provider ordering and verified-receipt requirement. The historical
`com.synexia.m3.cpugpu` class names are retained as a compatibility namespace
inside the M3JDK21-owned `jdk.m3.tornado` module. Their in-tree implementation
replaces the external `com.synexia.m3:cpu-gpu-rv32im` artifact. This deliberate
naming-map exception preserves the actual accepted batch type and one payload
owner; it does not introduce a Synexia runtime service or a package dependency
from Java SE/HotSpot onto an external Synexia installation.

Tornado's original `uk.ac.manchester.tornado` API names and module names remain
intact. The source-build integration does not authorize arbitrary Java lambdas,
filesystem operations or callbacks for device execution. Kernels must satisfy
the selected backend's compiler and execution contracts.

## Build, image and execution boundaries

Use the ordered phases documented by
[`BUILD_RUNTIME.md`](../runtime-integration/tornado/BUILD_RUNTIME.md):
patch, lint, compile, tests, installed-image runtime, receipt and recipe replay.
Every phase checks the admitted source/dependency identities, preserves command
logs and requires its predecessor's successful receipt. A source change requires
requalification from lint. An occupied output, altered dependency, missing source,
unknown backend or mismatched receipt fails closed.

The existing JMOD/module-graph packager builds the image. Packaging uses the
source-built M3JDK21 image for final target admission. A bootstrap-JDK image is
useful for bounded compiler/module/runtime qualification but is explicitly
provisional; it cannot establish matched HotSpot or full M3JDK21 acceptance.
The build never overwrites the supplied JDK or mutates vendored source inputs.

The OpenCL, CUDA and Metal source backends retain separate platform/device gates.
OpenCL execution on a CPU device proves that tested OpenCL path, not a physical
GPU, CUDA, Metal or cross-device execution. Missing hardware is reported as
unverified; it is never converted to a passing accelerated receipt. Exact integer
results are compared with an independent host calculation, and the existing RV32IM
provider retains its full CPU/device parity requirement.

Tornado uses JVMCI. The existing M3JDK21 guard against combining JVMCI with
`UseM3StringStorage` remains in force. Execute this lane with M3 String storage
disabled. Neither a module build nor a successful device kernel authorizes
removing that VM guard or claiming M3-enabled optimizing compiler support.

## Release evidence and limits

Release admission is conditional on the exact source/dependency/recipe seals,
actual compiler and test exits, module descriptors and closure, installed-image
execution, native-access configuration, device identity, independent parity,
compatibility/refusal cases and preserved legal notices. The release claim file
points at these requirements without upgrading unrelated String, collection,
full-jtreg or performance claims.

Elapsed time, process CPU and process RSS observations describe the recorded
workload and environment. They do not establish a speedup, zero-copy behavior,
bounded device memory for arbitrary workloads, interruption of an executing
kernel, universal backend support or whole-repository qualification.

## Licensing and attribution

Preserve Tornado's per-file Apache-2.0, GPL-2.0-with-Classpath-exception and MIT
notices and the dependency notices in the installed distribution. The recipe's
Apache-2.0 license does not relicense donor source or runtime dependencies.
First-party recipe and integration expression retains the recorded Hitesh Soliwal
and Synexia attribution. Existing OpenJDK rights and licenses remain unchanged.
