<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21: one native/JNI contract inside the SDK

**M3JDK21 is the Java 21 SDK product.** Its native functionality is owned by
the SDK's Java/HotSpot/JNI components. This is not an M3+JDK overlay, not a
second framework-specific native API, and not a Synexia runtime dependency.

## Shared C11 atoms

The common `include/m3_jni_core.h` is directly included by existing native
owners in `m3/arrays` and `m3/collections`; their Java public APIs and
exported JNI symbols do not change. These atoms check:

- dynamic-array half-open windows using subtraction to avoid integer overflow;
- required/excluded object-state masks of unsigned byte, int and long widths;
- JNI error construction without discarding a pending Java exception.

Native code handles physical offsets and logical indices, not a second store
of per-element objects. A selection view retains its canonical parent owner,
slot/generation and mask metadata. No JNI array pointer, JNIEnv*, global
reference or Java object is retained by these helpers. Native region copies
remain bounded and contract-dependent, not an unqualified zero-copy claim.

## Platform contract (not platform-completion claims)

| Target | JNI/runtime route | Native binding |
| --- | --- | --- |
| Android | Android Runtime (ART), Android NDK | NDK JNI headers; platform-specific .so packaging |
| iOS / iPadOS | Compatible Java AOT/runtime providing JNI | App-bundled static/dynamic native linkage subject to Apple/runtime rules |
| HarmonyOS | Depends on device's Java/JNI-capable runtime | Runtime-specific native loading and package rules |
| Windows | Java 21 JNI-compatible runtime | Windows DLL, MSVC/clang-cl |
| Linux / embedded Linux | Java 21 JNI-compatible runtime | ELF shared library |
| macOS | Java 21 JNI-compatible runtime | Mach-O dynamic library and platform signing |

These are potential build targets, **not** demonstrated portability or
Android/iOS implementations. JNI needs a compatible Java VM/runtime;
C11 portability by itself does not supply one. Platform adapters may resolve
library loading, OS services, ABI calling conventions and capabilities, but
must not duplicate the M3 index/mask algorithms.

## Host C11 proof

```sh
cmake -S m3/native -B m3/native/build -DCMAKE_BUILD_TYPE=Release
cmake --build m3/native/build
ctest --test-dir m3/native/build --output-on-failure
```

Supply `JAVA_HOME` for host JNI discovery. Cross-builds can supply
`M3_JNI_INCLUDE_DIRS`; Android uses the NDK sysroot. The C contract
executes a bounded window test matrix, all 256×256×256 unsigned-byte
state/required/excluded combinations, and selected signed int/long masks.
C tests operate on the same predicates as native owners but do not replace
full Java/JNI checks. Cross-compiled test binaries are not auto-executed.

## Admission and source custody

Canonical source recipe: private Synexia PR
[hsoliwal/com.synexia#12548](https://github.com/hsoliwal/com.synexia/pull/12548),
directory `synexia-openrewrite-recipes/recipes/m3jni-unified-20261020`.
The recipe pins all before/after blobs; fixes must be made in the recipe and
replayed serially. This target keeps public names and ABI stable.

Pending evidence: platform device builds (Android/iOS/HarmonyOS/Windows/macOS),
installed matched source-built JDK/JNI and GC tests, broad jtreg and
comparative CPU/retained-heap/native-memory measurements. The existing
String → arrays → collections receiving gate is not advanced by host C11 tests.

## Windows checkout repair and iOS source compilation

**Observed error on 2026-10-29:** M3's common C11/JNI Windows CI checkout
failed *before the compiler ran* with `Filename too long` on historical
`m3/arrays/qualification/**/recipe-custody/**` source paths.
The original Git blobs are not defective. The Windows CI now sets
`git config --global core.longpaths true` **before** `actions/checkout`.
Keep this gate ahead of every full checkout: a post-checkout Git setting
cannot repair checkout failure. This fixes a path creation prerequisite;
it does **not** certify MSVC/CMake, Java/JNI or Windows runtime behavior.

An opt-in iPhoneOS ARM64 compile-only job now reuses the canonical
`m3/native/CMakeLists.txt` OBJECT library and actual `m3/arrays` and
`m3/collections` C owners with `CMAKE_SYSTEM_NAME=iOS`, the Xcode iPhoneOS
SDK and the existing `M3_JNI_INCLUDE_DIRS` escape hatch. Its host JDK 21
JNI declarations are only a compile-time input, **not** an iOS runtime.
`BUILD_TESTING=OFF` prevents treating an iOS cross-binary as host runnable.

The iOS job is not a claim of an iOS JVM, AOT integration, App Store
eligibility, native loading, device JNI/GC tests or successful app signing.
Actual Android/iOS product qualification requires a selected runtime
(Android ART or an eligible iOS AOT/runtime), native library packaging,
ABI/runtime parity, signed device execution and platform policy review.
Native host/macOS and Android C11 jobs remain independently tracked.
