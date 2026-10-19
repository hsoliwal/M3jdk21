<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# Array comparison qualification — 2026-10-18

**This refinement strengthens the native and Java/JNI tests. The baseline runtime comparator remains selected.**

The target baseline is [M3JDK21 commit 2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49](https://github.com/hsoliwal/M3jdk21/tree/2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49). The two selected source changes are:

| Target path | Selected SHA-256 |
|---|---|
| m3/arrays/src/main/native/tests/m3_arrays_checks.c | 5bbaf29b0b184fdd97f6765843910fde094f7ebe416e7276578290afef036977 |
| m3/arrays/src/test/java/com/m3/arrays/M3ArrayNativeTest.java | a7138863de372351d655bd9be6d1d7bf7da9bf71c252d9187acf85e3c3bf3cef |

The retained comparator at m3/arrays/src/main/native/m3_arrays.c has SHA-256 0c7be7f8176ca235c40d88aad2cefab15e7ce7363d7fc648bafba4fdb535695d.

## Executed checks

The evolved native checks were compiled against that unchanged comparator with GCC 13.3.0, C17, optimization and NDEBUG enabled, and strict warnings treated as errors. They completed **187,008 differential cases with zero failures**, using seed 0x4d335554. Cases cover UTF-16 lanes and tails, offsets, exact allocation ends, overlapping ranges, null/empty boundaries and exact subtraction results.

The actual M3ArrayNativeTest class was compiled with Java 21 and real JUnit 5.10.2 assertions. A focused driver directly invoked its **five test methods**, including **4,096 seeded trials**, in Java fallback and actual JNI modes. The native run used the baseline comparator, unchanged JNI code, -Xcheck:jni and a required-provider check; native loading was confirmed. The exact methods, source hashes, flags and result digests are in qualification.json.

This focused execution is distinct from Maven/Surefire discovery. The module POM pins JUnit 5.10.2-m3-source-1; the recorded focused run used the official 5.10.2 assertion API. Full module dependency resolution and discovery remain a separate gate.

## Why the comparator experiment was rejected

A grouped-load comparator and two bounded repairs preserved the tested behavior but retained unfavorable tiny and immediate mismatch measurements. The final evaluated comparator had SHA-256 37bff7172793cdc12d9a8e9bec55f66bca24cb8a6c1f25feb1c3b8c27d193ab3 and is excluded from the selected runtime change.

The table shows candidate/baseline thread CPU ratios; values below one use less time.

| Surface and workload | Units 0–7 | Units 8–64 | Units 1023–1025 | Units 4097–65537 |
|---|---:|---:|---:|---:|
| Native equal | 0.9602 | 0.3706 | 0.2638 | 0.2421 |
| Native first mismatch | 1.3790 | 1.2496 | 1.4738 | 1.2740 |
| Native last mismatch | 1.1097 | 0.4378 | 0.2625 | 0.2480 |
| JNI equal | 0.9522 | 0.8701 | 0.5617 | 0.4959 |
| JNI first mismatch | 0.9265 | 0.9359 | 1.0098 | 0.9703 |
| JNI last mismatch | 0.9598 | 0.8863 | 0.5368 | 0.4774 |

Long-prefix gains did not meet the explicit tiny/early selection condition. Native immediate mismatch cohort medians were approximately 1.7–2.3 ns per baseline call and 2.5–2.7 ns per candidate call. Tiny middle mismatch had a ratio of 1.3303. These are historical measurements of a rejected experiment, not speedup claims for this test refinement.

Native ratios use per-configuration medians of five paired warm samples. JNI ratios compare median samples within corresponding JVM forks, then take the median ratio across three forks per configuration. Cohort summaries are unweighted; they do not model production traffic. Some independent process comparisons reverse the small nanosecond differences.

## Reproduce the selected native check

Run from the repository root with the selected test changes present. This is the recorded native source/flag combination expressed with repository-relative paths:

~~~sh
m3_arrays_qualification_dir=m3/arrays/target/catalogue-refinement-20261018
mkdir -p "$m3_arrays_qualification_dir"

gcc -std=c17 -O3 -DNDEBUG \
  -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic \
  -fno-lto -fstack-usage \
  -I m3/arrays/src/main/native/include \
  m3/arrays/src/main/native/m3_arrays.c \
  m3/arrays/src/main/native/tests/m3_arrays_checks.c \
  -o "$m3_arrays_qualification_dir/m3_arrays_checks"

"$m3_arrays_qualification_dir/m3_arrays_checks"
~~~

Expected output:

~~~text
M3_UTF16_NATIVE_CORPUS seed=0x4d335554 differential_cases=187008 failures=0
~~~

For repository-native Java/JNI discovery, use JDK 21, the repository's dependency setup, and the existing native profile. These standard reproduction commands were **not** executed as full Maven/Surefire/CMake gates in the recorded focused run:

~~~sh
mvn -f m3/arrays/pom.xml -Dtest=M3ArrayNativeTest test

mvn -f m3/arrays/pom.xml -Pnative-linux \
  -Dtest=M3ArrayNativeTest \
  -Dm3.arrays.native.required=true \
  -DargLine=-Xcheck:jni test
~~~

The native profile requires CMake 3.20 or later, a C compiler and JDK JNI headers.

## Limits and provenance

Measurements used Temurin JDK 21.0.12.1+1 and GCC 13.3.0 on shared x86_64 Linux 6.18.55 with an AMD EPYC 9V74. The final runner and its children were bound to one permitted logical CPU; this did not reserve a physical core. AArch64 and production workload qualification have not run.

ASan/UBSan passed on the evaluated candidate's native corpus with leak detection explicitly disabled. LeakSanitizer remains pending because the environment could not inspect the process task directory; that failure is not a pass. Full module Maven/CMake discovery and full JDK/jtreg qualification also remain pending.

The final benchmark processes each reported 13,056 KiB native peak RSS and eight bytes of compiler-reported comparator stack usage. All six JNI forks had zero median warm allocated bytes/op, with one retained 584-byte sample in each fork. These are whole-process and counter observations, not proof of zero allocation on every call or a production memory reduction.

The refinement uses the existing target-specific hash-pinned recipe machinery under identifier m3-catalogue-refinement-20261018. The public array owner remains com.m3.M3ArraySubstrate. No challenge submission code was copied. The runtime API, JNI implementation and existing array/text owners retain their current contracts.
