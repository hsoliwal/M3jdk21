# SPDX-License-Identifier: Apache-2.0
# Serial pass 4 consumes pass-3 JNI+JUnit postimages; original licensing unchanged.
language	repository	path	preimage_git_blob	preimage_sha256	postimage_sha256	role
C11_JNI	hsoliwal/M3jdk21	m3/arrays/src/main/native/m3_arrays_jni.c	db01d8f3c3e07a4b65d76215d40fea2f31759262	09857bf085779e99244929de6471385a7867e89d18060bff842101e313f7e7e8	4882cb58c2d0496d073e6ad98a4ab3e0036d7b54aed06e490353ed62fbeb29fa	PRODUCTION_NATIVE_CANDIDATE
JAVA21_JUNIT	hsoliwal/M3jdk21	m3/arrays/src/test/java/com/m3/arrays/M3ArrayNativeTest.java	5add4eebf4202a222edcf13fd4e70f5a96f58e7c	26d13911379cfd4b1c570f9935f7d1b44b0aefab0c550ee3b3888ab9ea1912f7	34336380a4f20755e502f02b2e2cfe9537185f0f4b9f44cc2a8d50c89376d29c	CONTRACT_TEST_CANDIDATE

## Fourth serial source-sealed Java/JNI pass: equal UTF-16 chunks (2026-11-01)

The next pass follows canonical private [Synexia PR #15080](https://github.com/hsoliwal/com.synexia/pull/15080) `m3-utf16-equal-chunk-native-20261101` and `m3-utf16-equal-chunk-java-20261101` recipes. Both require the exact prior stage-3 C/JUnit source SHA-256 values; no earlier recipe output or original licensing is overwritten.

The existing JNI `nativeCompareUtf16` still validates nulls and both ranges, handles zero-length and `IsSameObject` identity, and fetches only bounded `GetCharArrayRegion` stack buffers. For a fully initialized chunk of at least 32 UTF-16 units, `memcmp` is used **only to establish equality**. If any byte differs, the existing `m3_compare_utf16` scalar code-unit path returns the original exact signed first-difference result. This is portable to little- and big-endian JNI layouts; no Java pointers escape and no extra runtime library is introduced beyond the platform C library. Public Java API/JNI symbol names remain byte-for-byte unchanged.

The existing `M3ArrayNativeTest` adds direct-JNI seeded tests for equal windows, offsets, 31/32 and 1023/1024/1025 boundaries, unsigned Unicode code units, mismatch position/sign and buffer immutability. An **independent** locally executed Java21 + C11 `java -Xcheck:jni` probe passed **46,336** comparisons (source details in canonical crate). This does **not** establish a speedup. Actual target CMake, CTest, native-mode Maven/JUnit, Gradle/JDK integration and full JNI memory/GC and performance gates remain **PENDING**. The unrelated historical `java.util.Arrays` name-map source pin remains a separate blocker, not suppressed or silently updated.
