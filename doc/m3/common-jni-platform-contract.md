<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# M3JDK21 common JNI and native platform contract

Status: **mandatory architecture requirement; implementation and target qualification remain per owner**.
This document specifies the common boundary for phone, tablet, desktop, server and embedded targets.
It does not claim that the current M3JDK21 tree implements or runs on every listed platform.

## One product-owned native boundary

M3JDK21 MUST share its JNI mechanics and native service contracts across platform ports.
Existing Java SE classes, HotSpot code, M3 internals and module-owned native bindings keep their
current ownership. Platform-specific implementations sit below the common contracts. Common
string, regex, array, collection and compute algorithms remain in their existing owners.

The responsibility split is:

| Responsibility | Common implementation | Platform implementation |
| --- | --- | --- |
| Java/native bindings | Typed declarations, generated headers, argument and exception contracts | Target registration and loading hooks |
| JNI support | Reference cleanup, thread attachment, checked ranges, buffer lifetimes and error translation | Qualified runtime-specific necessities |
| Native operations | Existing portable kernels, versioned service contracts and bounded descriptor batches | Qualified CPU/GPU kernels where appropriate |
| OS services | Memory, files, clocks, synchronization and event contracts | Actual OS calls and resource ownership |
| UI/device services | Surface, input, lifecycle and accessibility contracts | Original toolkit peers and target event loops |

The common layer is part of M3JDK21. It MUST NOT introduce a Synexia runtime service or dependency.
Synexia continues to own the reusable transformation recipes, source provenance and convergence work.
The existing String -> arrays -> collections -> AST/compiler receiving sequence remains authoritative.

Common ownership permits multiple module-owned libraries. Preserve native symbols, class-loader
ownership, initialization and existing loading contracts unless a separately reviewed migration
changes them. Avoid a mandatory public catch-all Java facade or an unrestricted opcode/pointer
dispatcher. HotSpot/native owners may call an admitted common native operation directly where
their existing VM contracts permit; they do not need an extra JNI round trip.

## Reuse and module boundaries

Inventory `src/java.base/share/native/libjava/jni_util.h` and `jni_util.c` before adding JNI helpers.
Reuse the existing helper when its semantics and linkage match. A common helper must have a
canonical owner and a dependency direction that respects JDK module and bootstrap boundaries.
`java.base` MUST NOT acquire dependencies on desktop, GPU or phone UI modules.

The version/size/status/function-table design in `m3/ports/vcpu/native/include/m3vcpu.h`, and portable
OS atoms under its `src/common/`, are existing patterns to assess. The vCPU owner remains responsible
for compute. Its registry MUST NOT become the owner of filesystems, windows or all phone services.
Shared implementation may be linked or generated into appropriate owners under the established
licensing rules; do not duplicate exported utility symbols or retain competing helper implementations.

## Existing JNI library donor pool

There are several existing JNI helper libraries, binding generators and foreign-function runtimes.
Inspect them before inventing another implementation. The common M3JDK21 contract may admit useful
parts from several donors, with one accountable owner for each responsibility. It does not require
one external library to supply every platform or require all donor runtimes to be installed together.

The following bounded survey records source candidates, not accepted dependencies. Revision pins,
license files, inspected paths and exclusions are recorded in the canonical task's
`EXTERNAL_DONORS.tsv`. The linked revisions are the inspected snapshots; this is not an exhaustive
catalogue or a claim that each snapshot is the newest release.

| Donor | Useful responsibility | Admission constraint |
| --- | --- | --- |
| [fbjni](https://github.com/facebookincubator/fbjni/tree/f172ffddd426a71a882d46af5a0e1090c69e6182) | Owned JNI references, thread environments, exception translation and inferred registration descriptors | Reconcile Java helper classes and VM lifetime with existing owners. Its inspected CMake assumes C++20, exceptions and RTTI; its Windows header workaround must not replace target-runtime header qualification. |
| [JNI Bind](https://github.com/google/jni-bind/tree/e6fc6d3ff3ee1bf444a7dfc0bf01ac83b8bf362d) | Compile-time descriptions, signature checking, typed calls, references and array views | Requires C++20, or C++17 with Clang. It wraps an available JNI runtime; C++ declaration checks do not independently prove actual Java/class-loader compatibility. |
| [AOSP libnativehelper](https://android.googlesource.com/platform/libnativehelper/+/aef2939781fc0b57b4477df7160935cdf5697919/) | Scoped references, strings, arrays and JNI signature checks | Separate helpers using standard JNI from Android platform/ART invocation glue and Android-specific method conventions. Do not use its JNI headers as a universal runtime substitute. |
| [Mapbox jni.hpp](https://github.com/mapbox/jni.hpp/tree/57ca9ed4bbeb22ed8d20a55063dcaa217ba47f42) | Compact C++14 typed wrappers, ownership and checked size conversions | Repository archived on 2026-06-08. Treat it as reference material unless maintenance is assumed. Its bundled test JNI headers have separate licensing. |
| [Chromium JNI Zero](https://chromium.googlesource.com/chromium/src/third_party/jni_zero/+/9b814def50b5dac56d18ea041d11f074286013a2/) | Generated bidirectional stubs, native registration, conversions and generated-output tests | Preserve the established attributed-source workflow: its Java parser uses regular expressions. Review annotation, proxy, R8 and multiplexing assumptions before admitting any generated surface. |
| [Djinni generator](https://github.com/cross-language-cpp/djinni-generator/tree/29e129f3178ffe8c8f0290fba0fee83382753000) and [support library](https://github.com/cross-language-cpp/djinni-support-lib/tree/c197712e6061e8d45d3a90c22e2f43ce8fe71d26) | Coordinated C++, Java/JNI and Objective-C++ declarations and conversions from an interface definition | Objective-C++ is an Apple binding route, not provision of an iOS JVM. Reconcile proxy identity, collection copies and thread attachment with M3 ownership. |
| [JavaCPP](https://github.com/bytedeco/javacpp/tree/867ad70096386ae3b33342276d336b75a160b860) | C++ API mappings, JNI generation, callbacks and native lifetime patterns | Its Pointer and Loader runtime must not become a second canonical storage/loading system. Its README's iOS route requires a separate Gluon VM or RoboVM runtime. |
| [libGDX gdx-jnigen](https://github.com/libgdx/gdx-jnigen/tree/d80b501aa7013c9fad8b313ec28ec3ccc41a0ee2) | JNI and C-header generation, OS/ABI builds, Android packaging and iOS device/simulator packaging | Current scope includes a pointer/closure runtime. The inspected inline generator emits critical-array acquisition; reconcile blocking and callback paths before reusing that conversion policy. |
| [SWIG](https://github.com/swig/swig/tree/f088c5213bd87681e187d95d1a5298430676e80f) | Header/interface-driven JNI generation, typemaps and C++ callback directors | Control generated proxies, copies and lifecycle. The generator tool and reusable Lib/Examples templates have different licensing terms. |
| [JNA](https://github.com/java-native-access/jna/tree/a800ca8313076bc0375fed8bd4efd829eb61b0b0) | Dynamic C API calls, native mappings and callback/loader examples | An optional FFI donor requiring its native dispatch machinery; do not make it an automatic bootstrap dependency or assume universal phone support. |
| [JNR-FFI](https://github.com/jnr/jnr-ffi/tree/39ae9390be88ed16244efd03a78a90346a89f442) and [jffi](https://github.com/jnr/jffi/tree/5b4a42dbbc96bd80b565dab889535345b1586197) | Dynamic C calls, ABI mapping, invocation and closure machinery | Account for jffi, bytecode/assembler dependencies and executable callback trampolines. Qualify them against each target runtime rather than assuming that a JNI wrapper supplies those capabilities. |

First compare shared-reference, thread, exception and registration responsibilities against
`jni_util`, fbjni, JNI Bind and libnativehelper. Inspect gdx-jnigen for native target builds and
Djinni for coordinated Java/Apple bindings; assess JavaCPP when the receiving owner needs broad
C++ API generation. Mapbox, JNI Zero and SWIG provide additional implementation and test patterns.
JNA/JNR remain candidates for specifically owned FFI services rather than prerequisites for all JNI.
This ordering is an engineering assessment for this requirement, not a measured performance ranking.

Before importing a part, record its pinned source and per-file license, receiving owner, runtime
and build dependencies, JNI ABI/signatures, allocation/copy behavior, lifetime and failure contract,
and the actual tested OS/CPU/runtime/linkage combinations. Prefer existing attributed recipe inputs
and generated-output fixtures over a parallel parser or transformation engine. Preserve existing
Java SE behavior and M3 array/offset/mask/segment ownership when translating donor representations.
Helper, generator, linker and runtime evidence are separate; none alone establishes all-phone support.

## Native support and data-structure donors

The donor pool also includes native libraries whose algorithms and platform adapters can strengthen
the common implementation. These are not all JNI libraries. Their useful parts belong inside the
existing M3JDK21 native, storage or toolkit owner. The canonical task's `NATIVE_DONORS.tsv` records
seven additional pinned repositories in six families, including the user-nominated Abseil snapshot.

| Native donor | Useful inspiration | Required receiving decision |
| --- | --- | --- |
| [Abseil](https://github.com/abseil/abseil-cpp/tree/48c8fd3b582e33a302fb2dd59e9ca2676129c883) | Cord fragment traversal/sharing; Swiss-table control bytes and candidate masks; packed B-tree search; spans; status, cleanup, synchronization and time primitives | Translate to M3's canonical segment/index/mask owners and exact Java semantics. Pin the C++ dependency closure; Abseil's ABI and dynamic-unloading assumptions do not define the common JNI/C contract. |
| [Folly](https://github.com/facebook/folly/tree/4f25ddd32521c6e156bed6ae61052bb87e118340) | F14 tag filtering and vector indexes; sorted vectors; small vectors; borrowed ranges | Select representation deliberately. F14 node storage, moved packed values and default mobile intrinsics settings need separate treatment. The inspected revision requires C++20 and promises no ABI compatibility between commits. |
| [Boost.Container](https://github.com/boostorg/container/tree/23c12b6ab524d5e09dae1f70b43b6ece817f439e) and [Boost.Intrusive](https://github.com/boostorg/intrusive/tree/4f41b4ab1f7fd86c434fe30fe004d76a987ee330) | Flat sorted-sequence algorithms, bounded small/static vectors, and separation of balancing/traversal algorithms from node traits | Preserve array lanes, capacity failures and iterator contracts. Intrusive hooks still represent per-element linkage; adapt admitted algorithms to existing indexes instead of introducing retained Entry/Node objects. |
| [libuv](https://github.com/libuv/libuv/tree/bbd08cc8d56d8b440fcfe96256de0cca082f2f86) | Event-loop ownership, asynchronous requests, wakeups and completion-driven close | Keep the existing scheduler/event owner. Its support matrix places Android in community-maintained Tier 3 and does not explicitly qualify iOS. Native I/O support alone supplies no JNI/VM lifecycle. |
| [SDL](https://github.com/libsdl-org/SDL/tree/3b2bc951500ca6a1f428af77e036e732d9a8afe3) | Android JNI adapter, UIKit lifecycle transitions, subsystem initialization and main-thread dispatch | Its Android glue is tied to SDL activity/services, while iOS uses UIKit. Translate useful platform atoms into original toolkit owners; do not replace SWT/AWT/Swing identity or infer an iOS JVM. |
| [Apache APR](https://github.com/apache/apr/tree/c5db070f0800834ec9cbfef4a44862cdf87ccdf0) | Portable resource scopes, cleanup registration, pollset capabilities, synchronization and initialization contracts | Pool clear/destroy must not invalidate live JNI/M3 backing. The inspected platform documentation does not establish Android/iOS qualification; unsupported capabilities require explicit behavior. |

For Abseil, prioritize these comparisons:

1. **Composite storage:** `absl::Cord` shares reference-counted byte fragments and offers chunk
   traversal and external backing with a releaser. Borrow its fragment and lifetime patterns while
   retaining M3's existing interned payload owner. Cord sharing is not OS-wide canonical interning.
   Its unsigned-byte comparison and clamped subrange behavior must not replace Java String's UTF-16,
   bounds, exception or immutability contracts. A borrowed `Span` or `string_view` describes one
   contiguous range; composite operations still need the admitted segment traversal contract.
2. **Lookup metadata:** Swiss tables filter candidates through control-byte metadata; hash fragments
   and masks never replace the required equality check. Compare this with M3's primitive state/index
   lanes. Flat containers and packed B-tree algorithms are the stronger starting points for that
   representation; copying a node-based variant does not satisfy the canonical-storage requirement.
   Preserve required ordering and invalidation behavior, and do not persist `absl::Hash` as a stable
   identity: its values may differ across processes and loaded libraries.
3. **Native support:** assess `Status`, cleanup, synchronization and time atoms at their actual owner.
   Translate results into the established C status and Java exception contracts; keep C++ types out
   of the public native ABI. Abseil's inspected baseline is C++17, and selected containers explicitly
   lack exception-safety guarantees. Neither their dialect nor their failure behavior is imposed
   on HotSpot or existing JDK modules by this survey.

Folly's F14VectorMap offers a concrete comparison for lookup metadata containing 32-bit indexes into
a packed value vector. Its vector-intrinsics configuration is off by default under `FOLLY_MOBILE`
at the inspected revision; ARM64/NEON hardware alone does not qualify that path. Small/static-vector
patterns are suitable candidates for bounded descriptor batches, with checked capacity and explicit
spill/failure behavior rather than a blanket allocation-free claim.

The platform donors also sharpen lifecycle requirements. libuv's cross-thread wakeups may coalesce,
so an event signal must not be treated as one completion per queued operation. Close requests and
final resource release are distinct. SDL's main-thread dispatch must respect existing toolkit
ownership and avoid synchronous wait cycles. APR's resource scopes require a defined lifetime and
thread owner; unsupported pollset features are explicit errors. These are requirements to reconcile
with existing owners, not a decision to install three additional event/resource managers.

All native donor imports retain per-file provenance and licensing. C++ internal implementation
details may inform an admitted translation, but are not treated as stable upstream APIs. Qualify
initialization, shutdown/unloading, allocation failure, callbacks and target ABI before selecting a
dependency. Keep measured cost and target execution evidence separate from the source comparison.

## Typed and versioned native contracts

Portable kernel/service headers expose C-compatible operations. JNI types such as `JNIEnv` and
`jobject` stay at the JNI boundary; OS handles, Objective-C objects and C++ container layouts stay
inside their backends. Existing public JNI declarations retain the standard JNI types and ABI.

Each independently versioned service table records an ABI version and structure size. Validate
the admitted version and minimum size before reading fields or invoking functions. Additive optional
fields require explicit size rules. Unknown or incompatible versions fail before resource use.
Do not serialize process pointers or function tables as cross-process or persistent state.

Use fixed-width values for logical identifiers and portable data fields. Define byte/code-unit/
element units, encoding, alignment and endianness; check conversions to `jsize`, `size_t` and native
pointer arithmetic. Each operation specifies validation order, permitted aliasing, resource owner,
thread requirements, synchronous/asynchronous completion, cancellation and exact result/error behavior.
Preserve existing Java exceptions and effects. C++ exceptions must be caught and translated before
returning through JNI or a C ABI.

## Arrays, offsets, masks and composite storage

Native calls MUST preserve M3's existing array/offset/index/mask/segment representation. Reuse
canonical storage owners; no per-element retained Entry/Node store is introduced by this boundary.
Prefer bounded bulk operations over a JNI call per logical element.

Pass bounded spans or batches of segment descriptors where the receiving operation supports them.
Descriptors identify the backing owner, generation or equivalent validity evidence, offset, length,
and relevant encoding/state. Metadata may be arranged in primitive lanes. Composite operations
preserve logical order, segment seams, masks and UTF-16 behavior, including unpaired surrogates.

Materialize contiguous memory only at a contract that requires it, with explicit lifetime and
ownership. A native projection or cache does not become a second canonical spelling/payload owner.
Both sides validate malformed ranges, overflow, unsupported widths, mask extents and backing bounds
before memory access. Zero copying is a measured property of a particular path, not a JNI promise.

Every successful Java array/string acquisition has a matching release on every subsequent exit
path. A failed acquisition returning null is not released; preserve its exception/error.
`GetPrimitiveArrayCritical` regions stay short and obey the JNI restrictions: no arbitrary JNI calls
or calls that may block waiting for another Java thread. Release critical storage before deferred
work, platform event processing or backend transfers that can block. Never retain a borrowed heap
pointer after its acquisition scope or for asynchronous GPU completion.

Asynchronous work owns its memory or holds an explicit lease until completion. Direct buffers and
native mappings still require backing-lifetime, aliasing, alignment and concurrent-close protection;
a pointer or numeric handle alone does not supply that protection. Required shared/fixed-address
mapping capabilities must be probed and qualified; absence must not silently change canonical
M3String ownership or pointer-identity assumptions.

## References, callbacks and lifecycle

`JNIEnv*` is confined to its current thread. Store/share the appropriate `JavaVM*` only under a
defined VM lifetime. Native-created callback threads obtain their environment, attach if required,
and detach only when they own that attachment and JNI permits detachment.

Local references never cross thread or invocation boundaries. Retained objects/classes use the
appropriate global-reference ownership and cleanup. A JNI reference is not a stable Java object
address. A cached method/field ID must keep its class valid or be re-resolved when required.

Every native resource has an owner and a matching release operation. Handles reject stale reuse;
generation checks also require protection for operations already in flight when close races with
access. Reuse existing handle/storage owners before introducing another registry.

Callbacks run on their declared thread/executor. Prefer bounded completion records delivered to the
existing event owner. Preserve pending exceptions; test callback throws, reentrancy, cancellation,
shutdown and late completions. Do not execute arbitrary Java callbacks from unloading hooks.
Ordinary UTF-8 and JNI modified UTF-8 are distinct encodings and must never be interchanged implicitly.

UI backends retain original SWT/AWT/Swing ownership and platform thread requirements. Common native
support does not replace the original widget with an independent viewport or shadow object model.
Test focus, IME, accessibility, suspend/resume, resource/surface loss, recreation and disposal on the
actual host. UIKit UI work uses its required main-thread/main-queue context.

## Platform backends and capability reporting

Backends supply the platform-dependent implementation of memory mapping, files and I/O, clocks,
threads/synchronization, loading, event dispatch, surfaces, graphics, input, clipboard, accessibility,
audio and device services. Modules require only the services their contracts need. Platform choices
belong in backend/build selection and target lifecycle code, not repeated OS switches throughout
portable algorithms.

Capability reporting distinguishes implementation compiled, service available, initialization
succeeded, current permission/resource availability, and recorded qualification. A detected header,
symbol or OS name does not advertise a working service. Runtime availability may change after
permission denial, device removal or surface loss. Preserve the existing API's documented fallback
or explicit-unavailable behavior; never silently omit required work.

| Target profile | Required backend/runtime identity | Separate proof |
| --- | --- | --- |
| Android with M3JDK21 | NDK/OS backend plus actual M3JDK21 runtime port | ABI/API-level build, runtime launch, JNI and device lifecycle |
| Android ART library host | NDK library and named ART/API version | Library compatibility only; not modified M3JDK21 execution |
| iOS/iPadOS device | Apple-device backend plus selected M3JDK21 runtime | Device binary, JNI registration, lifecycle and installed execution |
| Apple simulator | Simulator-specific runtime, SDK and binary | Simulator execution independently of device results |
| Linux, Windows, macOS | Existing OS family and exact M3JDK21 image | M3-specific native/VM contracts on that image |
| AIX, BSD, other phones or embedded systems | Explicit OS/CPU/runtime/backend profile | Per-profile capabilities and execution evidence |

These rows specify port obligations, not current support claims. Additional phone systems, including
HarmonyOS/OpenHarmony targets, require their own inspected runtime and SDK profile; do not infer
Android/ART compatibility from a product-family name.

Cross-compilation MUST use the target runtime's JNI headers, target sysroot, architecture, calling
convention and deployment/API level. Host JDK headers are not a default substitute. Linker search
paths, ELF/Mach-O/PE formats and library hooks are target-specific. Apple-device and simulator
artifacts stay separate even when both target ARM64.

Dynamic and static JNI entry points reuse their registration logic where compatible, while retaining
required hooks and class-loader ownership. Negotiate the JNI version actually supported by the runtime
and needed by the functions used; static `JNI_OnLoad_<name>` follows the specification's minimum
JNI 1.8 requirement. Native OS loading alone does not finish JNI registration.

OpenJDK Mobile is a concrete iOS runtime donor: its currently published README documents a static
`libjvm.a` build with Zero by default. Pin a compatible donor revision, assess Java 21 backport fit,
then build and execute the M3JDK21 port. This document does not require AOT exclusively and does not
assume an iOS JVM already exists in M3JDK21. Skia/Skiko native packaging or an ART-hosted test alone
does not establish that M3JDK21's modified class library, String and VM execute.

## Current source findings and implementation order

The bounded source inventory used M3JDK21 `d6d76b563050fc13c4fa79bc494edf29f1614b37` and
Synexia `f04fbac91a7891e64b77c7b24cef1ab75ec88144`. It is not a census of every native file.

1. **Shared support inventory:** map each repeated JNI responsibility to `jni_util` or the appropriate
   existing domain owner. Qualify any shared extraction with unchanged declarations and symbol tables.
2. **Target build configuration:** the current vCPU CMake fallback uses host JNI include paths and
   applies `$ORIGIN` outside Windows. Replace those assumptions through the owning recipe and target
   profiles; retain build failures for unsupported runtime adapters. Synexia's existing
   `synexia-mindex/native-jni/CMakeLists.txt` demonstrates Android target-NDK header selection and
   explicit refusal of Apple-device JNI without a target adapter; it is donor evidence.
3. **Transfer lifetime:** the current vCPU upload/download wrappers call backend operations while
   holding critical arrays. Qualify bounded staging or another admitted lifetime model before treating
   those paths as safe for blocking backends. Preserve copy direction, offsets, errors and allocation
   failure behavior; this documentation change does not repair that source.
4. **Runtime and service backends:** implement Android and Apple-device runtime admission, then native
   services and original toolkit hosts through their owners. Reuse the common contracts for subsequent
   platforms. Preserve independently qualified features and the receiving-order gates.
5. **Target execution:** record compilation, linking, JNI registration/loading, device/simulator
   execution, contract tests and complete installed-runtime qualification separately. Identify OS,
   API/deployment level, CPU ABI, device/simulator, VM/version, linkage mode, features and source hashes.

Required native proofs include checked-JNI execution, generated-header/export comparison, invalid
and overflowing ranges, stale handles, concurrent close, allocation failures, exception cleanup,
callback cancellation, segment seams and target lifecycle transitions. Record comparative costs only
for measured workloads. Compilation or a passing finite kernel corpus never establishes all-platform
or whole-JDK acceptance. The current Android/iOS runtime and UI qualification remain open.

## Source and recipe references

- [Existing mobile owner plan](mobile-inplace-swt-swing-skija-20261019.md)
- [Native build evidence](native-build.md)
- [Porting and naming invariant](m3jdk21-porting-invariant.md)
- [JNI design](https://docs.oracle.com/en/java/javase/21/docs/specs/jni/design.html)
- [JNI functions and critical arrays](https://docs.oracle.com/en/java/javase/21/docs/specs/jni/functions.html)
- [JNI invocation and static libraries](https://docs.oracle.com/en/java/javase/21/docs/specs/jni/invocation.html)
- [Android JNI guidance](https://developer.android.com/ndk/guides/jni-tips)
- [Android ABIs](https://developer.android.com/ndk/guides/abis)
- [Android runtime](https://source.android.com/docs/core/runtime)
- [OpenJDK Mobile](https://github.com/openjdk/mobile)
- [Apple multiplatform packaging](https://developer.apple.com/documentation/xcode/creating-a-multi-platform-binary-framework-bundle)
- [UIKit](https://developer.apple.com/documentation/uikit)

Canonical task recipe: `com.synexia.rewrite.M3CommonJniPolicyRecipe`, with the existing
`M3GpuToolJoinRecipe`/`M3GpuToolJoinManifest` engine. Canonical fixtures and execution receipts live in
`hsoliwal/com.synexia`, under
`synexia-openrewrite-recipes/verification/common-jni-platform-contract-20261020`.
The source-sealed documentation recipe records this requirement; it does not implement a universal
runtime enforcer or qualify platform backends.
