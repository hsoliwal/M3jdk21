<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: GPL-2.0-only -->

# JNI concurrent adapter: prevent NULL receiver virtual dispatch

Stacked exclusively on [M3JDK21 #2405](https://github.com/hsoliwal/M3jdk21/pull/2405)
at exact commit `47b36ba4b86c5c193c1881a912db1836f9c1a5bb`.
Canonical source authoring is the **separate**
Synexia recipe successor to [#16085](https://github.com/hsoliwal/com.synexia/pull/16085).
No default `master` or previously-reviewed PR source may be silently rewritten.

**Only two existing jtreg files may change**, exactly from v2 to v3:

| Source | Before Git blob | After Git blob |
| --- | --- | --- |
| `ObjectAdapterConcurrentIdentityTest.java` | `46f52646af859b6519a42cdb9cfe9b0682926e4e` | `e5755b524ca9b7608480462b29dc8d11388a52e4` |
| `libObjectAdapterConcurrentIdentityTest.c` | `f69e4a4731d64cf601ce7a186d49d49eadff9283` | `5cb3533c9ea53e59fe9e56cf40c4c7500c739040` |

The existing virtual `Object.toAdapter(Class<T>)` JNI calls use
`CallObjectMethod`, `CallObjectMethodA`, `CallObjectMethodV`.
A NULL jobject must be rejected *before* those instance calls. The
test-owned native bridge must throw `NullPointerException("receiver")`
and clean its local reference; the Java jtreg preflight must check each
mode 0, 1, 2 independently. This is the sole semantic addition.

The existing public `Object.toAdapter(Class<T>)` declaration,
erased `(Ljava/lang/Class;)Ljava/lang/Object;`, M3 object identity,
JDK/HotSpot structure, native **product** library, class loaders and
observation behavior remain LOCKED. All 14,592 hot iterations and
384 cold first-creation results stay intact.

## Donor and evidence boundaries

Existing first-party `libObjectAdapterNativeTest.c` at Git blob
`351ccaefd894bf28af3076306a273f4d9958914e` already enforces
nonnull receiver through its Java wrapper; this regression adds a
native guard to the separate concurrent test. LC706, HackerRank
Frequency Queries and GeeksforGeeks hashing are classification
references only; never copy solutions to change JNI contracts.

The candidate passed local strict Java21 and C17 compilation and
three `-Xcheck:jni` stock-JDK `AdapterOracle` model modes.
A deliberate native-guard removal failed as expected. This model is
not the modified JDK's virtual JNI method nor its weak owner lifetime.
The source-built M3JDK21 image, actual native virtual jtreg, Maven
OpenRewrite, GC, HotSpot/JCK and whole release remain UNVERIFIED.

Keep PR as **draft**, do not merge or claim production correctness
until the matched Java/JNI runtime gate passes.
