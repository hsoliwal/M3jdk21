<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: GPL-2.0-only -->

# JNI null receiver test boundary — executed and held gates

## Source custody

M3JDK21 [#2405](https://github.com/hsoliwal/M3jdk21/pull/2405)
exact parent `47b36ba4b86c5c193c1881a912db1836f9c1a5bb`;
canonical authoring [Synexia #16093](https://github.com/hsoliwal/com.synexia/pull/16093),
stacked on #16085. This source receiver changes exactly two
existing **jtreg test** files: Java and native C. No product
`Object`, `M3ObjectRuntime`, JNI product exports or HotSpot changed.

| File | v2 Git blob | v3 Git blob | v3 SHA-256 |
| --- | --- | --- | --- |
| `ObjectAdapterConcurrentIdentityTest.java` | `46f52646af859b6519a42cdb9cfe9b0682926e4e` | `e5755b524ca9b7608480462b29dc8d11388a52e4` | `8cffb2c1bd83b2a4a58d5a07d2ca9ea19217481fde266e55725d0c63be4bcc3e` |
| `libObjectAdapterConcurrentIdentityTest.c` | `f69e4a4731d64cf601ce7a186d49d49eadff9283` | `5cb3533c9ea53e59fe9e56cf40c4c7500c739040` | `70cec1d77ca548064dd4707a5ed6a24c1198fde707f0753b922d91799a38d6da` |

Both postimage Git blobs and contents were read back from GitHub
after each SHA-gated target edit and compared to Synexia source-sealed
templates and actual locally compiled files. Exact source custody: **PASS**.

## Executed 2026-11-04 in local OpenJDK 21 model

- `javac -proc:none -Xlint:all -Werror -h`: **PASS**,
  generated JNI signature matches the unchanged test-only export.
- `gcc -std=c17 -Wall -Wextra -Werror -Wpedantic -fPIC -shared`:
  **PASS** for both test-owned target C and static-model JNI bridge,
  using the installed OpenJDK21 headers.
- `java -Xcheck:jni`: **PASS**, 14,592 hot/384 cold results.
- `java -Xint -Xcheck:jni`: **PASS**, same counts.
- `java -Xcomp -Xbatch -XX:-TieredCompilation -Xcheck:jni`:
  **PASS**, same counts.
- **RED negative control:** the new Java preflight was executed with
  original *unguarded* C model library, and exited 1 with
  `AssertionError: native mode accepted null receiver 0`.
- Git-compatible two-source patch applied and reversed. Attempting
  the same patch a second time refused; restored source hashes match
  the preimages. **PASS**.

**Important:** The model JNI invokes **static**
`AdapterOracle.adapt` on unmodified JDK21; it has a test-only strong
`IdentityHashMap`, not real `M3ObjectRuntime`. This demonstrates
the null guard and native call shape, not actual product virtual JNI,
GC lifetime or optimized runtime throughput.

## Mandatory HOLD

The source-built **modified M3JDK21** native `CallObjectMethod*`
jtreg with both observation policies, classloader and GC stress,
full Java.base/HotSpot build, JCK/JNI ABI and release acceptance
were **NOT RUN**. Synexia `M3HashPinned*SnapshotRecipe` JUnit and
Maven proofs were **authored, NOT RUN locally** because Maven is
unavailable. Hosted CI must report actual success separately.

The public API, `Object.toAdapter(Class<T>)` spelling, erased
`(Ljava/lang/Class;)Ljava/lang/Object;` descriptor, all normal
exception paths and test result totals remain locked. Keep this PR
**draft / HOLD** until real runtime tests pass.
