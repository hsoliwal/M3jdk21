<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0 -->
# Common JNI contract for M3JDK21 phone and other targets

M3JDK21 must maintain one shared Java/native contract for each existing owner
across Android, iOS/iPadOS and other supported native targets. The integrated
runtime owns identity, arrays, mapped payloads, offsets, masks and lifecycle.
The canonical transformation workspace supplies evaluated implementations and
recipes under the existing M3 porting invariant.

Shared Java declarations, C ABI/JNI entry points and behavioral atoms remain
common. Platform-specific code supplies the target VM's JNI environment,
thread attachment, platform resources, graphics surfaces and event/lifecycle
integration while honoring the same owner contract. A platform cannot be
declared supported merely because host headers compile.

The full evaluated owner/adapter matrix, exact donor source closure and remaining
gates are pinned in the [canonical common platform contract](https://github.com/hsoliwal/com.synexia/tree/517d49739dca95ba5caad4a3f14f94171a984b71/synexia-openrewrite-recipes/recipes/m3-android-efficiency-20261020/COMMON_JNI_PLATFORM_CONTRACT.md).

## Current receiving implementation

- The existing Java-base native `libjava` build remains the integrated authority.
  Shared M3 state-relation C and Java declarations remain at their current paths.
- The existing `m3_arrays` / `com.m3.arrays.M3ArrayNative` boundary remains common.
  Its Android CMake branch now uses NDK sysroot JNI headers, without host JVM/AWT
  discovery, and receives the existing canonical target-scoped 16 KiB link flags.
- Every non-Android JNI discovery path is preserved. No new Apple refusal is
  added to the current receiving owner. The rejected narrower proposal is
  retained in canonical evidence.
- Existing source/header/rights/test bytes, JNI exports, array transfer,
  exceptions, pointer lifetime and fallback behavior remain unchanged.

## Platform qualification boundaries

| Target | Executed receiving evidence | Open platform work |
| --- | --- | --- |
| Linux x64 Java 21 host | Exact recipe outputs, real Java/JNI/C checks, sanitizers and actual arrays CMake/CTest/JUnit | Source-built integrated M3JDK21 image, full libjava, jtreg/JCK |
| Android | NDK routing/link configuration received as source; host behavior unchanged | Actual NDK cross-build, ABI/ELF checks, 4/16 KiB device/package validation, VM/runtime and mobile UI execution |
| iOS/iPadOS | Common contract and preserved supplied non-Android configurations | Target Java 21 VM/JNI adapter, Apple SDK/static packaging decisions, signing/device and mobile UI execution |
| Other native targets | Shared owner/API contract retained | Target toolchain, runtime, architecture and JNI qualification |

Canonical Apple C/C++ packaging does not establish an M3 Java 21 VM adapter.
The actual canonical SDK's Apple gate stays in that owner; this receiving change
preserves existing M3 discovery instead of narrowing its contract.

## Efficiency rules carried by this receiver

Reuse current primitive arrays and masks; traverse present states rather than
unrelated capacity when the contract and measured workload justify it. Read
mapped metadata at checked absolute offsets without allocating per-scalar views.
Keep native calls at the existing bounded operations and preserve validation
before output and existing region-copy/lifetime rules. Borrow ART/DEX/AOSP ideas
through catalogue comparison without changing existing payload/wire contracts.

The [receiving report](README.md) provides exact host measurements and the
retained original timing hold. Performance evidence and platform support are
separate admission gates. SWT/Swing/Skija phone peers must eventually use the
same common boundary and target resource/lifecycle contracts; those peers are
not implemented by this three-atom receiver.

## Android NDK qualification checkpoint — 2026-10-21

The platform table above is the earlier receiving checkpoint and remains
unchanged as historical context. Actual NDK compilation, linkage, generated
JNI-header/export checks and ELF inspection are now recorded in
[NDK_QUALIFICATION_20261021.md](NDK_QUALIFICATION_20261021.md) and the
[compact source/artifact receipt](evidence/ndk-cross-build-20261021.json).

The [immutable Synexia proof](https://github.com/hsoliwal/com.synexia/tree/a137faea542c97fcdc1c8867a6b49565e358b45b/synexia-openrewrite-recipes/recipes/m3-android-ndk-20261021) is commit
`a137faea542c97fcdc1c8867a6b49565e358b45b`. Its original 337-file packet manifest SHA-256 is
`8766c652e364db81cb8dcfda2db1b16272e5be2afb0fa7d8dc6d7d99e3a8e235`. Native execution used source snapshot
`9441d86e6e0a699504ede20c0fd1df9a2febe033`; documentation delivery is prepared for
`bbde385ab8890b2148fc1cb0deb35c59f19db959` with an independent audit of nine exact source bindings.

Four current arrays targets and four arrays baselines built for Android API 21
across four ABIs. Twelve additional state-relation libraries are standalone
leaf/link proofs. Twenty shared objects and 52 LOAD segments were inspected;
fifteen proof guards passed. The original baseline FindJNI also succeeds.
The current explicit 16 KiB layout preserves stripped file sizes and adds
12,288 bytes of declared LOAD padding per arrays object, not measured RSS.

Android VM/device execution, full JDK/libjava images, package alignment and
installation, mobile resource measurements, iOS and mobile SWT/Swing/Skija
remain open. The recipe changes three documentation files only; shared native
owners, JNI contracts and **STRING / QUALIFICATION_REQUIRED** remain intact.
