<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3JDK21 Tornado RV32IM provider

M3JDK21 builds and delivers this provider as the named module `jdk.m3.tornado`.
The source closure is in this directory. Building or running the delivered module
does not require a Synexia checkout, Maven artifact, runtime service or application.
The module consumes the Tornado API built from the product's pinned Tornado source
and uses the existing `jdk.internal.vm.parallel` contract in `java.base`.

## Compatibility and ownership

`com.m3.tornado.Rv32iBulkExecutorAdapter` retains its existing implementation and
public signatures. The historical `com.synexia.m3.cpugpu` package is an explicit
compatibility namespace inside the M3JDK21-owned module. Its ten original source
classes are received without changing their bodies, accepted types or return
types. In particular, the adapter still accepts the same `Rv32iBatch` class and
uses that class as the single owner of guest memory, registers and state.
The image must not also contain the old `cpu-gpu-rv32im` artifact: a second copy
would split class identity and duplicate the package. No renamed payload store
or competing interpreter is introduced.

The module opens only the historical provider package to `tornado.runtime`.
Tornado resolves the original task method reference through its serialized
lambda's private `writeReplace` method; an export alone does not permit that
reflection. The qualified open enables the existing task contract while keeping
the access recipient explicit. A test-only kernel in `com.m3.tornado` receives
its own qualified `--add-opens` in the image harness, without expanding the
production module's opens. Original implementation bodies remain unchanged.

This compatibility exception is recorded in the target naming map. Reusable
recipes remain canonical in Synexia; M3JDK21 owns the delivered module, packaging,
target API compatibility and acceptance. Source custody records the complete
donor closure and the older CI pin, whose provider source tree is identical.

## Execution contract

The adapter accepts one operation identity, `m3.rv32im.slice`, with inputs
`batch` (`Rv32iBatch`) and `instructionBudget` (positive `Integer`). It returns
`batch`, a separately executed copy; the caller's input remains unchanged.
CPU mode requires `localWork == 0`. Tornado mode requires positive local work
and returns only after the provider's full independent CPU/Tornado comparison
of registers, architectural state and RAM issues a `TORNADO_VERIFIED` receipt.
An unavailable or failed accelerator does not become an accelerated success.

The existing host interpreter and device kernel remain separate implementations.
Virtual cores have private RAM and a bounded instruction budget. RV32IM guest
execution is this provider's admission boundary; arbitrary Java callbacks and
filesystem operations are not automatically device kernels.

`TornadoRv32iSession` retains its original explicit transfer/lifetime contract:
device state may remain resident between slices, registers/state transfer back
after a slice, and RAM transfers back on demand. Host-side changes after session
creation are not automatically resubmitted. Full parity mode synchronizes all
three planes before publication. These are transfer semantics, not a zero-copy
or speedup claim.

## Building and testing

The existing `m3-tornado` reactor profile selects this module. Its Tornado API
dependency is version-pinned to `7.2.0` and is supplied by the integrated
source build. This is the actual root and API Maven coordinate at pinned source
`87a465f9185eb8e822f9532076fb4a5bc44fdf2a`, including its `jdk21` profile;
the profile does not append `-jdk21` to that coordinate. Java21 preview remains
an independent compiler/runtime requirement. The dependency is not a released-binary substitute for that source
build. The existing target build/image workflow owns compiler/runtime/driver
assembly and the exact source and toolchain receipts.

Java 21 preview is required by the pinned Tornado API. Compile against the
existing bulk contract with the qualified export
`java.base/jdk.internal.vm.parallel=jdk.m3.tornado`; ordinary adapter callers
already require the corresponding export to their module. The Maven proof lane
compiles an isolated five-class bulk patch for a stock JDK21 test host. That
patch is test evidence only. Final product verification must use the complete
source-built M3JDK21 image with its own bulk classes.

The receiver's named-module compilation uses
`-Xlint:all,-preview,-module,-exports,-missing-explicit-ctor -Werror`.
The first strict candidate emitted exactly seven compatibility diagnostics:
one numeric `m3` module-name advisory, four warnings about the adapter's existing
internal bulk types, and two warnings about the original public default
constructors. Those three categories are explicitly excluded to preserve the
reviewed module name, existing internal API boundary and exact donor bodies.
Preview warnings are excluded for the pinned Java21 API. All remaining lint
categories remain errors. This is a scoped compatibility policy, not a claim
that an unexcluded `-Xlint:all` build passed.

The five original donor JUnit classes and existing adapter tests are retained.
`com.m3.tornado.Rv32iImageProbe` supplies an additional executable named-module
and installed-image gate. `cpu` exercises the actual provider, legacy type
identity, deterministic scheduling/receipts, isolation, traps and refusal.
`gpu` additionally requires real Tornado execution and full CPU parity. Skipping
the GPU argument does not qualify that gate.

The current `UseM3StringStorage` mode rejects JVMCI and forces interpretation.
Keep that guard intact and leave that flag disabled for the Tornado compiler.
No combined M3-enabled String/JVMCI configuration is admitted by this module.

## Licensing and acceptance

The received RV32IM implementation and target provider are Apache-2.0; their
attribution, original source paths and exact revisions are retained in `NOTICE`
and `source-custody/rv32im-provider.json`. Tornado API/runtime/driver sources keep
their own scoped licenses in the integrated source distribution. This module's
license does not relicense them or the `java.base` bulk contract.

The existing `m3/runtime-integration/FOSS_REUSE_DECISION.tsv` registers the
separate Tornado source-build intake and this exact compatibility provider.
This provider uses the existing `INDEPENDENT_APACHE_MODULE` intake mode: its
Apache sources and notices stay in a separately built `m3/**` module. There is
no new copy into an OpenJDK `src/**` owner and no change to the historical
direct-copy review ledger. The upstream runtime keeps its separate per-file
licenses and source/dependency custody; neither registration is a release pass.

Source receiving, compilation, CPU tests, accelerator tests and source-built
installed-image execution have separate receipts. Presence of these files or a
successful CPU test is not full Tornado/whole-JDK qualification. The global
receiving phase and unrelated release claims remain unchanged.
