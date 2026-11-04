<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: GPL-2.0-only -->

# Concurrent virtual JNI invocation variants — received source contract

This is a **test-only, source-sealed** continuation of
[M3JDK21 #2393](https://github.com/hsoliwal/M3jdk21/pull/2393)
at `5eabd0b28758e203c749efea7e8165b6aaee6220`.
Canonical authoring recipe:
[Synexia #16085](https://github.com/hsoliwal/com.synexia/pull/16085)
stacked on #16019. Original tests, donor catalogue and named
`Object.toAdapter(Class<T>)` public API remain in their established owners.

Exactly two existing jtreg implementation files are allowed to change:

| Source | Original Git blob | Reviewed new Git blob |
| --- | --- | --- |
| `test/jdk/java/lang/Object/ObjectAdapterConcurrentIdentityTest.java` | `4bf0dab9b42fd66bf6f2b7fdb996415f407b4ae3` | `46f52646af859b6519a42cdb9cfe9b0682926e4e` |
| `test/jdk/java/lang/Object/libObjectAdapterConcurrentIdentityTest.c` | `00a03c61f8a1137d5d201b3009832d80efa545e8` | `f69e4a4731d64cf601ce7a186d49d49eadff9283` |

The test JNI native symbol takes a new *test-only* integer selector.
The product method remains the same erased descriptor
`(Ljava/lang/Class;)Ljava/lang/Object;`, and JNI still resolves it
virtually on `java/lang/Object`. Mode 0 uses `CallObjectMethod`;
mode 1 uses `CallObjectMethodA` with one `jvalue.l` argument;
mode 2 uses `CallObjectMethodV` with paired `va_start/va_end`.
Each worker holds only JNI-local references. Unsupported requests
return null, null class raises the existing `NullPointerException`,
and invalid fixture mode raises `IllegalArgumentException`.

Six hot concurrent workers choose `workerIndex % 3`;
three JNI cold workers (1, 3, 5) choose
`(workerIndex / 2) % 3`. Exactly 14,592 hot and
384 cold results are retained as the acceptance workload.

## Evidence and hold

The complete source candidate was compiled locally under Java21
model declarations with `-Xlint:all -Werror` and JNI C17 native
`-Wall -Wextra -Werror -Wpedantic`. A **stock JDK test-only**
static AdapterOracle model passed default, `-Xint`, forced C2,
all `-Xcheck:jni`, and a deliberately broken canonical identity
mutant failed as expected. The model does **not** exercise
M3JDK21 `Object.toAdapter` or its real weak owner cache.

Matched image build, full virtual JNI `jtreg`, GC/weak lifetime,
native libjava exports, JDK binary/HotSpot compatibility,
JCK and release gates are **NOT RUN**. Keep this receiving PR
**draft / HOLD**; do not merge or claim product runtime passing.

Donor evaluation: use the existing GPL-2.0 first-party
`libObjectAdapterNativeTest.c` JNI A/V invocation patterns.
LeetCode 706, HackerRank Frequency Queries and GeeksforGeeks
hashing are *problem-category metadata*, not imported JNI code.
No new `toAdpater` alias, product JNI symbol, `Object` field,
or HotSpot vtable slot is authorized.
