# M3JDK21 21u hosted-platform build repair

Status: **source and GitHub Actions bootstrap repair**, not a certified
Java SE or mobile runtime release.

## Why this change exists

The original OpenJDK 21 GA workflow still requested retired `macos-11`
and `windows-2019` runner images, Xcode 11.7/12.5.1 and VS2019.
Another M3JDK21 hosted Windows JNI check failed during checkout before
any compiler was invoked because historical, deeply nested M3 custody
paths exceeded Git for Windows defaults. Do not classify these failures
as Java source compilation defects.

## Existing donor borrowed, not reinvented

- `openjdk/jdk21u-dev`, `.github/workflows/main.yml`,
  `build-macos.yml`, `build-windows.yml`, and `test.yml`: borrowed the
  current runner/toolchain selection pattern, but retained M3JDK21's
  existing platform job graph, test suites and bundle usage.
- `openjdk/jdk21u-dev:make/conf/github-actions.conf`, source Git blob
  `e1001dd6c4fea3491d3f1d6775141286059a5110`, received
  byte-identically. Its BootJDK URL/SHA256 pins include macOS ARM64.
- `openjdk/jdk21u-dev:.github/actions/get-bootjdk/action.yml`, source
  blob `278aad7dcb38a6fd2ae4add9972234e9d9d9a2e0`, received
  byte-identically, including `actions/cache@v5` and absolute path output.
- Existing M3-specific JTReg/GTest actions retained their acquisition
  behavior but upgraded outdated cache/checkout action versions.

**Do not copy the upstream 21u JTReg artifact downloader alone.**
It assumes an upstream artifact-producing build job that this existing
M3JDK21 job graph does not yet have. That would break a working fallback.

## Platform and acceptance boundaries

| Target | First product-build gate | Runtime claim |
| --- | --- | --- |
| Linux x64 | Host `configure`, release/fastdebug image and tier1 | Pending hosted results |
| Linux ARM64 | Cross-build and/or native ARM64 runner, then real image jtreg | Pending |
| Windows x64 | VS2022, Git long paths before checkout, image + tier1 | Pending |
| Windows ARM64 | Explicit cross-target HotSpot build; not x64 parity | Pending |
| macOS Intel | Native `macos-15-intel`, Xcode 16.4 and tier1 | Pending |
| macOS Apple Silicon | Native `macos-15`, Xcode 16.4, ARM64 BootJDK + tier1 | Pending |
| Android ARM64 | Existing Android NDK C11/JNI-owner compile only | No ART-backed M3JDK21 runtime yet |
| iOS ARM64 | iPhoneOS C11 cross-compile-only PR; AOT/runtime route needed | No iOS JVM/device runtime yet |

Android is not a substitute for desktop Java `java`/HotSpot integration.
iOS target success requires an actual compatible AOT/runtime provider,
device execution and Apple distribution compliance. Preserve evidence
independently for JNI/native build and actual Java runtime tests.

## Release-oriented fix order

1. Borrow upstream 21u defect fixes via the existing M3JDK21
   [PR #2101](https://github.com/hsoliwal/M3jdk21/pull/2101),
   review the two Java String/AbstractStringBuilder ports and validate
   full M3 image behavior and JNI consequences before merging.
2. Qualify this host-bootstrap change on its exact new Git SHA;
   remove obsolete host configurations and ensure all build/test jobs
   actually execute, not merely queue.
3. Run `make images`, targeted jtreg (java/lang/String, arrays, collections)
   and tier1 on the matching product image; compare OpenJDK 21u as oracle.
4. Publish a checksum-pinned **technical preview** for each platform
   only after its own successful build/install/startup and legal gates.
5. Keep Android/iOS as separately qualified packaging/runtime projects;
   do not advertise a full mobile JDK based on a C11 compilation receipt.

## Source-only regressions

Run `python3 -m unittest discover -s m3/release
-p 'test_hosted_jdk_toolchains.py' -v` on a full Git checkout.
The existing first-release workflow automatically discovers these tests.
This verifies workflow invariants; GitHub-hosted build and JDK runtime
reports, not these tests, determine platform availability.

## Licensing and ownership

The OpenJDK 21u donor keeps its original GPLv2/ClassPath notices.
Reusing other JDK/vendor sources is by pinned file, license and ABI
review; do not import an entire vendor VM into HotSpot merely for speed.
Public Java 21 APIs and JDK module names remain unchanged. Synexia
continues to own reusable transformations/recipes; the M3JDK21 tree
receives compatible, reviewed results. User-maintained independent
packaging remains possible under the applicable source licenses.
