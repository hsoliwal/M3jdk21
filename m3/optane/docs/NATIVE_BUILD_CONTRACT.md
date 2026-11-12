<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# PMem native build and installed-image contract

## Owners

The product module is `jdk.m3.io`, declared in
`src/jdk.m3.io/share/classes/module-info.java`. Its public PMem package is
`jdk.m3.optane`; tensor kernels remain in the unexported
`jdk.internal.m3.optane` package. `m3/optane/pom.xml` is the existing Maven
control bridge with historical module name `org.m3jdk.optane`.

The existing source owners are `NativeSupport`, `NativePmem`, and
`PmemAccelerator`. Their JNI libraries are `m3optane`, `m3pmemsdk`, and
`m3pmemaccel`. None of the class names or JNI exports has an `M3` prefix.
All three C sources reside under
`src/jdk.m3.io/share/native/libm3optane`.

`make/m3/optane/CMakeLists.txt` resolves the actual JDK source paths and
generates headers from those Java declarations. `make/modules/jdk.m3.io/Lib.gmk`
adds the same three libraries through the ordinary `SetupJdkLibrary` phase.
The existing JDK `ModuleWrapper.gmk` and `CreateJmods.gmk` own copying libraries
into the image and JMOD; no custom installation or loader is introduced.

The native product lane is Linux x86-64. Other platforms retain the existing
Java FILE backend and explicit native-unavailable behavior. The integrated
build uses the POSIX backend without an external library dependency. The
separate CMake `M3_OPTANE_PMDK=ON` option remains an explicit experiment against
the build host's `libpmem2`; it is not required or bundled by M3JDK21.

## Build gates

With a complete JDK 21 toolchain:

```sh
mvn -B -ntp -f m3/optane/pom.xml package
cmake -S make/m3/optane -B build/pmem-native
cmake --build build/pmem-native
mvn -B -ntp -Pm3-optane-native \
    -Dm3.optane.library.path="$PWD/build/pmem-native" \
    -f m3/optane/pom.xml test
```

The default Maven filter refers to the current `Pmem*` test names and keeps
the pre-existing FILE-only selection. The native profile still selects every
`*Test` and enables its existing native property gates. No native test is
deleted or changed to pass when its library is missing.

The C17 warnings-as-errors flags are retained. The Q8 kernel also retains
`-ffp-contract=off -fno-fast-math`, because its API promises strict FP32
column-order accumulation. CPU feature reporting does not establish that
an AVX-512 or VNNI kernel is implemented.

For a configured full M3JDK21 source build, run the ordinary `jdk.m3.io-libs`
and image/JMOD build phases and then the installed-image test:

```sh
make jdk.m3.io-libs
make images
make test TEST="test/jdk/jdk/m3/optane/PmemNativePackaging.java"
```

`PmemNativePackaging` loads all three libraries through the installed module,
performs a POSIX read/write/persist plus FILE reopen, and checks a hand-derived
signed-Q8 matrix/vector result under `-Xcheck:jni`. Its jtreg invocation does
not inject a development `java.library.path`, so a missing product library
is a failure. Existing correctness oracles remain separately required.

## Evidence boundary

A standalone CMake build, Maven bridge test, or JMOD linked with a stock JDK
does not prove a complete source-built M3JDK21 image. Record those outcomes
separately. Ordinary-file tests do not establish physical Optane residency,
FS-DAX, NUMA placement, power-loss durability, or inference throughput.

All transformation recipes and their correction memory are authored and
executed only in canonical Synexia. M3JDK21 receives the executed build/test
postimages and passive source/recipe/gate receipts. Historical recipes still
present under `m3/optane/recipe` are migration residue, not execution entrypoints.
