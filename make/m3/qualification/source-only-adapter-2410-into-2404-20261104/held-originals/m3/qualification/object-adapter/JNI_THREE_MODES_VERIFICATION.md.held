<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: GPL-2.0-only -->
# Three-mode JNI concurrency qualification — bounded receipt

This recipient receives only the two original jtreg Java/C test sources from
the hash-pinned canonical first-party Synexia recipe
[PR #16085](https://github.com/hsoliwal/com.synexia/pull/16085).
Existing parent M3JDK21 [PR #2393](https://github.com/hsoliwal/M3jdk21/pull/2393)
contains the original tests at Git blobs
`4bf0dab9b42fd66bf6f2b7fdb996415f407b4ae3` and
`00a03c61f8a1137d5d201b3009832d80efa545e8`.
This successor's product-test source blobs are **exact**:
`46f52646af859b6519a42cdb9cfe9b0682926e4e` (Java) and
`f69e4a4731d64cf601ce7a186d49d49eadff9283` (C).

## Actually executed 2026-11-04

- Canonical source templates generated from those preimages in isolated
  local input, Git blob hashes checked, then source-sealed into Synexia
  and received by exact source postimage here: **PASS**.
- Java21 model compiler `javac -proc:none -Xlint:all -Werror`:
  **PASS**.
- C17 product JNI fixture and stock-JDK test-only JNI library compiled with
  `-Wall -Wextra -Werror -Wpedantic -fPIC -shared`:
  **PASS**.
- Default Java21 `-Xcheck:jni`: **PASS**, 14,592 hot iterations
  plus 384 cold adapter results, with 6 workers and 3 native call modes.
- Interpreted Java21 `-Xint -Xcheck:jni`: **PASS**, same counts.
- Forced C2 Java21 `-Xcomp -Xbatch -XX:-TieredCompilation -Xcheck:jni`:
  **PASS**, same counts.
- Negative control: replaced canonical model owner lookup with
  `new M3Object(source)`. Javac **PASS**; execution **failed as intended**,
  `java.lang.AssertionError: native mode capability identity 0`.
- First model-only C translation failed `-Werror` for an unused local.
  It was corrected in **model C**, not product JNI C, then fully recompiled
  and rerun; failure was retained, not suppressed.

## Scope and limits

The model uses a static `AdapterOracle.adapt` method with an
`IdentityHashMap` as a behavioral stand-in. The model C JNI calls
`CallStaticObjectMethod*`, whereas the actual submitted native
jtreg C fixture calls **virtual** `CallObjectMethod*`.
These tests are useful for call-signature and native-reference
regressions but **do not exercise a modified JDK java.base, weak
M3Object owner lifetime or the actual virtual JNI target**.

The named OpenRewrite recipe has five authored JUnit tests and a
Maven21 POM; **Maven/OpenRewrite JUnit has NOT RUN locally**.
A matched source-built M3JDK21 image, full actual virtual JNI
with observation enabled/disabled, GC/weak lifetime,
JNI export/ABI/JCK/jtreg release, cross-platform and performance
checks remain **NOT RUN / HOLD**. The original public
`Object.toAdapter(Class<T>)`, the erased
`(Ljava/lang/Class;)Ljava/lang/Object;` JNI method descriptor,
all JDK/HotSpot runtime files and the historical evidence paths
are untouched. The two test sources alone were changed.

No LeetCode/HackerRank/GfG challenge source was copied. The JNI call
patterns are adapted from the existing first-party GPL-2.0-only
`libObjectAdapterNativeTest.c` (Git blob
`351ccaefd894bf28af3076306a273f4d9958914e`).
The new test source does not establish a runtime speedup.
