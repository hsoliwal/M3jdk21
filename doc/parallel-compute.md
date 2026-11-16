<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Parallel compute providers

M3JDK's parallel compute API uses the existing `m3.parallel` module and
`jdk.m3.parallel` packages. A `Backend` selects a compute engine, a `Plan` binds
the kernel and buffers, and its `Session` owns that engine's device storage.
The TornadoVM fork supplies the primary heterogeneous compute implementation.
Other engines implement the same `BackendProvider` and `DeviceProgram` SPI.

## Select an engine

| Selection | Meaning |
| --- | --- |
| `Backend.reference()` | Built-in Java CPU implementation and parity oracle. |
| `Backend.of("tornado")` | Explicitly select the installed TornadoVM provider. |
| `Backend.fromProvider(provider)` | Use a caller-supplied engine without changing process-wide provider discovery. |
| `Backend.select(policy, kernel)` | Select an available engine that supports this kernel, applying the existing policy and deterministic score/id ordering. |

`REFERENCE` selects the Java reference; `BEST` considers it alongside other
available providers; `ACCELERATOR` requires a non-reference provider. The
existing one-argument `Backend.select(policy)` retains its original behavior.
Use the kernel-aware overload when choosing an engine for a particular plan.

The engine provider advertises static capabilities and availability separately
from `supports(kernel)`. Supporting the SPI does not imply that an engine can
execute every kernel. A provider must admit only the operations, argument shapes,
numeric behavior and transfer semantics that it implements.

## Keep the plan independent of engine selection

```java
import jdk.m3.parallel.Backend;
import jdk.m3.parallel.Grid1D;
import jdk.m3.parallel.IntBuffer;
import jdk.m3.parallel.JavaKernels;
import jdk.m3.parallel.KernelId;
import jdk.m3.parallel.Plan;
import jdk.m3.parallel.Transfer;
import jdk.m3.parallel.Verified;

KernelId addition = JavaKernels.intAdd();
Backend engine = Backend.reference();
IntBuffer left = IntBuffer.of(Integer.MAX_VALUE, -7, 3);
IntBuffer right = IntBuffer.of(1, 5, -3);
IntBuffer output = IntBuffer.allocate(3);

try (Plan plan = Plan.builder(engine)
        .kernel(addition)
        .grid(Grid1D.of(3, 1))
        .bind(left, Transfer.ONCE)
        .bind(right, Transfer.ONCE)
        .bind(output, Transfer.ON_DEMAND)
        .build()) {
    Verified receipt = Verified.run(plan);
    // output contains Integer.MIN_VALUE, -2, 0: ordinary Java int wraparound.
}
```

Choose a different `Backend` to build the next plan. The plan construction and
kernel/buffer contract stay the same. Each existing plan retains its original
backend and device program until it is closed. Device state is never transferred
implicitly between engines. Explicit selection, preparation and execution errors
are reported to the caller; the operation is not retried on another engine.

`JavaKernels.intAdd()` and `JavaKernels.intBitCount()` expose the existing versioned
integer operation identities with one canonical Java reference body per identity.
The TornadoVM provider admits those same operations and supplies its device bodies.
Java reference registration remains explicit and lazy. Provider authors can add
further operation contracts through the existing `KernelId` and SPI.

## Supply another engine

Implement `jdk.m3.parallel.spi.BackendProvider` as a stateless factory. Provide a
stable, valid engine id, an immutable capability set, a selection score, a runtime
probe, a kernel-admission method and `prepare(kernel, grid, bufferLengths)`.
Preparation returns a `DeviceProgram` with separate zero-filled storage for every
buffer binding. Implement synchronous `upload`, `download`, `launch`, and
idempotent `close` according to the existing SPI contracts.

For application-scoped selection, pass that provider to `Backend.fromProvider`.
For process discovery, declare a JPMS `provides ... with ...` entry or the
classpath `META-INF/services/jdk.m3.parallel.spi.BackendProvider` resource.
Discovery retains the existing invalid/reserved/duplicate-id refusal rules.
The Java reference id and reference capability are reserved for the built-in
oracle. The factory snapshots identity, score and capabilities; constructing it
does not replace another provider or change existing plans.

The existing `jdk.m3.tornado` module supplies the TornadoVM provider. Build its
optional image from the pinned `hsoliwal/synexia-tornadovm` source using
`make/tornado/build_parallel.py`, as described in
[building-tornado-parallel.md](building-tornado-parallel.md). That image includes
the public API and the independent bulk bridge. The Tornado module reads the
bridge transitively, so applications requiring `jdk.m3.tornado` can continue to
use the bulk types. The bulk classes now report `m3.parallel.bulk.provider` from
`Class.getModule()`; tools that address their owning module must use that name.
The optional builder reuses both promoted modules from a supplied source-built
base image when its complete `lib/src.zip` Java inventories and module license
and notice entries match this source tree. It preserves those JMODs and the
hashes recorded in `java.base`. A partial module set or source or notice drift
is rejected. A base with neither module uses the existing source compilation path.
Follow the image's documented preview, native-access and
device launcher settings. A failed runtime or device probe reports the engine as
unavailable.

## Internal bulk scheduler integration

The existing `m3.parallel.bulk.provider` module connects these engines to
`jdk.internal.vm.parallel.BulkExecutorProvider`. The JDK base module grants that
bridge a qualified export; it has no dependency on an external compute engine.

`BulkScheduler.withProvider(id)` returns an immutable selection over the original
registered providers. It retains their frozen identities and priorities and
performs the existing input-binding, provider-identity and result-shape checks.
Calling it on an already selected scheduler can select a different registered
provider for later work. The original scheduler and all running calls keep their
selection. Unsupported operations and execution failures never trigger an
implicit fallback from an explicitly selected provider.

## Source and build ownership

The existing API and bridge implementations live in
`src/m3.parallel/share/classes` and `src/m3.parallel.bulk.provider/share/classes`.
Their package names and existing public method descriptors are preserved.
The bridge also retains its legacy Maven artifact's automatic module name,
`m3.parallel.bulk.provider`, as its explicit module identity.
The legacy Maven entry points use those same sources. The modules retain their
original source licenses and module legal notices.

Canonical source transformations, source seals and execution evidence live in
`hsoliwal/com.synexia`. This repository contains the emitted product sources and
target tests. Hardware execution must be qualified on the actual selected device;
a successful CPU test or provider compilation is recorded separately from that
device result.
