<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 Android NDK qualification: 2026-10-21

The existing common JNI owners have actual Android target compilation and link
evidence for **armeabi-v7a, arm64-v8a, x86 and x86_64**, at **API 21** with the
official pinned **NDK r28c / 28.2.13676358**, **CMake 3.28.3** and strict **C17**.
OpenJDK **21.0.2** generated the JNI declarations forced into compilation.
This receipt changes documentation only.

## Source and canonical evidence

The native commands ran on source snapshot `9441d86e6e0a699504ede20c0fd1df9a2febe033`.
This receipt is prepared for later receiving base `bbde385ab8890b2148fc1cb0deb35c59f19db959`;
an independent complete-body audit confirmed all nine source identities in the
[compact receipt](evidence/ndk-cross-build-20261021.json) match that base.
The audit SHA-256 is `72914512f89fd3abd082479e80010c15006538361d654f14d9765b7882c5b8cb`.
This does not describe a new native run at the later revision; final receiving
tree preservation remains required. The Java header-source identities are
historical proof bindings, not PlainText mutation targets.

The [immutable canonical proof](https://github.com/hsoliwal/com.synexia/tree/a137faea542c97fcdc1c8867a6b49565e358b45b/synexia-openrewrite-recipes/recipes/m3-android-ndk-20261021) at
`a137faea542c97fcdc1c8867a6b49565e358b45b` retains sources, reusable runners, commands, comparisons,
licenses, failures and independent review. The original **337-file** packet
manifest is `8766c652e364db81cb8dcfda2db1b16272e5be2afb0fa7d8dc6d7d99e3a8e235`; review/provenance supplements
at the same commit do not change that original packet.

## Executed native scope

| Evidence | Actual result |
| --- | --- |
| Current production `m3_arrays` CMake target | 4 Android ABI builds |
| Original arrays baseline | 4 comparative Android ABI builds |
| Baseline/current/portable state-relation C variants | 12 standalone proof libraries |
| Shared objects / inspected PT_LOAD segments | 20 / 52 |
| Build commands / layout inspection commands | 92 / 24 |
| Admission/layout guard tests | 15 passed; 0 failures, errors or skips |
| Inspectable binary mutation fixtures | 7 |

The twelve state-relation libraries prove leaf compilation and linkage; they
are not a complete Android `libjava` or M3JDK21 image. Exact exported JNI/C
symbol sets and authentic generated headers match. All twenty final artifact
hashes remain intact after inspection. Existing Android-target C test binaries
compile but were **not executed**. Historical runs and overlapping checks are
not added as unique semantic cases.

All four original arrays `FindJNI` configurations also succeed and select the
NDK JNI header. No universal old JNI-discovery failure is claimed. Current
target builds have no host JVM/AWT dependency.

## ELF layout and measured file costs

All sixteen current arrays and explicitly linked relation proof objects meet
the recorded 16 KiB LOAD alignment/congruence, RELRO-end and non-executable
stack checks. The 32-bit baseline has 4 KiB LOAD alignment; the 64-bit baseline
already has 16 KiB LOAD alignment with different RELRO-end layout. These are
measured layout properties, not inferred device failures. The 32-bit baseline
comparison is not a failed modern Android 64-bit requirement.

| ABI | Baseline/current stripped arrays file bytes |
| --- | ---: |
| armeabi-v7a | 5,480 / 5,480 |
| arm64-v8a | 7,456 / 7,456 |
| x86 | 5,844 / 5,844 |
| x86_64 | 6,992 / 6,992 |

The sum of declared LOAD memory sizes grows by **12,288 bytes** per arrays
object because of linker layout padding. Resident memory, CPU, startup and
mobile performance were not measured; this is not an RSS or speedup claim.

## Exact proof pins

| Canonical record | SHA-256 |
| --- | --- |
| Qualification | `b69637c64431629da430d033b790e76c1680820f2dead786ff626a570732a9fd` |
| Source/build freeze | `4b31629f75ed734b9d914a926b43f599c996faae0e6f8646724db8c86b7fdca7` |
| Final build summary | `b03c6ed5f2699192dc1897d1724d4e66751ef4c51e360f0b8084ba9370dfa95a` |
| Layout comparison | `74c5980c08c7f7646ede4fa3ab70fb9098ea6d1beaf45b73bb2a248aae684fd3` |
| Guard-test summary | `52740d986d8b91584d60b51aac3eb2f754a06f8cce7c9fd8c8a3b5c50104554f` |

The official NDK archive SHA-256 is
`dfb20d396df28ca02a8c708314b814a4d961dc9074f9a161932746f815aa552f`.
This historical toolchain pin does not claim latest-NDK support.

## Preserved failures and remaining gates

Canonical history retains failed download/setup attempts, the initial missing
official support-library subset, an incorrect baseline alignment threshold,
and an inspection-tool rewrite of two old attempt-3 objects. The corrected
inspector uses an explicit copy destination and checks preservation; fresh
attempt 4 supplies the successful final artifact seals. No failure is erased,
and no production source was changed to accommodate the proof.

Android VM/device/emulator tests, full M3JDK21/HotSpot/libjava images,
APK/AAB ZIP alignment and installation/signing, 4/16 KiB device behavior,
mobile resource measurements, jtreg/JCK, iOS VM/JNI/device execution and
SWT/Swing/Skija phone peers remain open. Preserve **STRING /
QUALIFICATION_REQUIRED** and the existing phase order. No platform-support
or runtime-mapping status is promoted by this documentation receipt.
