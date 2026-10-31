<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 platform build previews

M3JDK21 is one integrated Java 21 SDK. Its existing OpenJDK build produces the
actual VM, class libraries, tools and native libraries from this repository.
The boot JDK is a build input, not the distributed M3JDK21 runtime.

## Obtain a retained development build

Run the existing **OpenJDK GHA Sanity Checks** workflow manually on the intended
revision. Set `platforms` to the required native targets, for example
`linux-x64, macos-x64, macos-aarch64, windows-x64`, and set `keep-bundles` to
`true`. The build retains its `bundles-*` artifacts for 14 days. Ordinary runs
retain the existing cleanup behavior. Repository retention limits still apply.

Each artifact contains the original JDK archive, applicable test/symbol archives,
`SHA256SUMS`, and `BUILD-PREVIEW.txt` with repository, revision, workflow URL and
platform. Verify the archive digest before extracting it. Run the extracted
`bin/java -version` and `bin/javac -version` (the `.exe` tools on Windows).
Use the linked workflow to inspect the real build and runtime test results.

Retaining an archive does not certify it: the preview label records build output
only. Failed, skipped, incomplete or unavailable test lanes remain unqualified.
The existing `m3/release/release-contract.json` and M3 target acceptance gates
continue to govern release admission. No test suite is bypassed by `keep-bundles`.
GitHub Actions artifacts are temporary previews, not permanent public releases.

## Native desktop lanes

| Target | Build host | Bootstrap | Runtime tests |
| --- | --- | --- | --- |
| Linux x86-64 | Existing Ubuntu lane | Existing pinned x64 boot JDK | Existing tier-1 partitions |
| macOS Intel | `macos-15-intel`, Xcode 16.4 | Pinned OpenJDK 20 macOS x64 | Existing tier-1 partitions |
| macOS Apple Silicon | `macos-15`, Xcode 16.4 | Pinned OpenJDK 20 macOS AArch64 | Same tier-1 partitions on the native ARM64 host |
| Windows x86-64 | `windows-2022`, discovered VS2022, selected MSVC v142 | Existing pinned Windows x64 boot JDK | Existing tier-1 partitions |

Release and fastdebug builds remain in the original matrix. macOS ARM64 reuses
the architecture-matched boot JDK to build JTReg on a cold cache and obtains GNU
make through the actual Homebrew prefix. Windows checks the selected architecture's
exact MSVC component on the selected VS2022 instance before configuring the JDK.

The existing Linux cross-compilation lanes remain compile evidence until their
binaries are executed on the target. The existing Windows ARM64 lane builds
`hotspot` only; it must not be presented as a complete installable ARM64 SDK.

## Android and iOS are separate runtime targets

The shared native/JNI contract remains
[`common-jni-platform-contract.md`](../docs/common-jni-platform-contract.md).
An Android NDK library build or ART-hosted AAR does not establish an installable
standalone M3JDK21 runtime. The Android runtime port must qualify the actual VM,
Java 21 class libraries, Bionic/NDK integration, native ABI and device execution.

For iOS, [OpenJDK Mobile](https://github.com/openjdk/mobile) provides a concrete
static `libjvm.a`/Zero-interpreter donor. Import its compatible changes through
the canonical Synexia recipes, preserve Java 21 contracts and qualify static native
linking, JNI bootstrap, signing and device execution. The upstream mobile source
uses a newer JDK baseline; it is not an already-qualified Java 21 backport.
Device and simulator targets have separate evidence. A macOS build does not
establish iOS support, and a working native runtime does not establish UI parity.

Reuse suitable source from other JDK implementations and distribution build tools
by exact revision and behavioral atom, retaining per-file rights and attribution.
Synexia owns donor evaluation, recipes, tests and execution evidence; this product
receives only qualified generated outputs and target-specific acceptance evidence.
