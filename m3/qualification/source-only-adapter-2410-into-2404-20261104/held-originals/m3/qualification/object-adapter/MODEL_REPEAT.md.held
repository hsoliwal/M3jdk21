<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: GPL-2.0-only -->
# Object.toAdapter concurrent JNI — fresh stock-JDK model repeat (2026-11-04)

## Identity and scope

Review target: [M3JDK21 #2393](https://github.com/hsoliwal/M3jdk21/pull/2393),
baseline test-only PR head `eccd77b515e9a5f6977192e1c340fcbbfb5fd4aa`.
This note is **passive evidence only**: no product `Object.java`,
`M3ObjectRuntime.java`, HotSpot, `libjava` or JNI API was modified.
The target test sources remain at Git blobs
`4bf0dab9b42fd66bf6f2b7fdb996415f407b4ae3` (Java) and
`00a03c61f8a1137d5d201b3009832d80efa545e8` (C).

## What actually ran

A fresh bounded replay used only four **model** sources from the prior
2026-11-03 proposal artifact. The model's `AdapterOracle.adapt(Object,
Class<T>)` is a **static helper** that simulates current M3JDK behavior
on a stock OpenJDK 21 VM. The native bridge calls that helper with
`CallStaticObjectMethod`; it does **not** invoke the product's actual
`Object.toAdapter` virtual slot. The test-only `M3Object.of` uses a
synchronized strong `IdentityHashMap`, not the actual weak-lifetime
`M3ObjectRuntime` registry. This distinction is mandatory.

Toolchain: OpenJDK `21.0.12.1`, GCC `14.2.0`, Linux x86-64.

| Executed gate | Outcome |
| --- | --- |
| `javac -proc:none -Xlint:all -Werror` on the three Java model files | PASS, zero diagnostics |
| GCC `-std=c17 -Wall -Wextra -Werror -Wpedantic -fPIC -shared` using the real JDK JNI headers | PASS, linked native test library |
| Default Java 21 with `-Xcheck:jni` | PASS: 14,592 hot iterations; 384 cold-creation results; 6 workers |
| `-Xint -Xcheck:jni` | PASS: same deterministic counts |
| `-Xcomp -Xbatch -XX:-TieredCompilation -Xcheck:jni` | PASS: same deterministic counts |
| Mutation: replace cached canonical facade with a fresh `new M3Object(source)` | Java compile PASS; test **fails as intended** with `split cold capability at slot 0, worker 1` |

Test source SHA-256 preimages:

```text
AdapterOracle.java                       00b32c7beb5522e9138977d89e7e7b1829e335e786b894f97a51723765e958cc
M3Object.java                            96ec7b6fac44953e039b0e940cb44ef01dcac6ad05ca3fcac38bbec6944704c8
ObjectAdapterConcurrentIdentityTest.java 9318bca10c04a7ea43851763e1ea15869a8425a3f5b4908420f1cbb3f7171063
libObjectAdapterConcurrentIdentityTest.c 3631342535c6065cc9bef9b57c647ed8a6c70824cdde03c8445d136a22b18185
```

Negative-control changed file SHA-256: `7805745eb81dfd3da094e1ab47e96a84051216a18cfa811232ab229678d604ca`.
A human-readable run receipt, exact input sources, command lines, compiler
logs, three normal-run logs and the negative-control error log are captured
in archive SHA-256
`c9c370c9c4b415b6153d286cf148bfa77a397ab407d514e183b9777ff406b62d`.
The archive was locally ZIP-integrity verified. The portable runtime model
evidence is separately downloadable from the conversation containing this PR
update; no claim is made that the artifact is a GitHub release asset.

## Admission remains HOLD

**NOT EXECUTED:** recompilation of a modified M3JDK21 image, actual
`ObjectAdapterConcurrentIdentityTest` plus its virtual `CallObjectMethod`
native fixture on that image, M3JDK21 `M3ObjectRuntime` GC/weak-lifetime
behavior, original JNI notification exports, source ABI/HotSpot/vtable,
full jtreg/JCK, other platforms and benchmarks. Source-backed Synexia
Maven/OpenRewrite tests have independent gates.

The test verifies that the proposed concurrent identity regression can
distinguish a deliberately broken *model*. It does not promote the product
Java/JNI implementation and must not mask any failing CI check. Preserve
draft status.
