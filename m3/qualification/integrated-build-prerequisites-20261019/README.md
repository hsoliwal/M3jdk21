<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Integrated M3JDK21 build prerequisite evidence

The qualified build prerequisite sources produce a complete Linux x86-64 M3JDK21 image and both CDS archives. The source baseline is `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`; the canonical Synexia recipe source is [`d8426c06f67683763a8ddfc8bd99620500a0f536`](https://github.com/hsoliwal/com.synexia/commit/d8426c06f67683763a8ddfc8bd99620500a0f536).

This stage contains the Object bootstrap count correction, the existing native fixed-arena feature-macro guard, and the Java documentation/SOURCE-annotation corrections. `M3StringPool.java` remains at its original SHA-256 `b58c82047f795807ff32229146c370c9e596eff19942a92cd0128ce4987d20ca` in this preserved baseline image. The String hash index and fixed-lexicon JNI implementation are qualified in the subsequent String change.

## Build result

`make CONF=m3-p0 JOBS=4 ENABLE_JAVAC_SERVER=false images` completed successfully, including both normal and no-compressed-oops CDS archives. `IMAGE_IDENTITY.json` seals the executable, HotSpot, libjava, modules image, release metadata and archives. The separate Object packet records actual normal and interpreted runtime tests against the matching built VM. The documentation and annotation packets record their own serial source, API and emitted-class checks.

The build used the verified Temurin 21.0.12.1+1 boot JDK, GCC/G++ 13.3.0 and GNU Make 4.3. It retained the build warning gates. Temporary dependency headers and development links were supplied through the configured sysroot; `system-runtime-link-inputs.json` identifies the already-installed shared objects used to complete those links. The first image build failed on missing link targets, and the first resumed compilation encountered a stale javac server. The failed logs remain alongside the successful direct-javac build. These are environment/build-attempt records, not source changes.

## Acceptance boundary

Successful packaging and CDS generation establish this build gate. They do not establish complete String semantic acceptance, platform portability, or promotion of the repository's current String phase. The subsequent String review includes the unchanged baseline test matrix and final-image comparison, with all existing failures retained. Collection and lexicon owners outside the built JDK modules use their own strict compilation and behavioral evidence.

The reusable transformations remain in Synexia. This directory contains receiving evidence and does not add a runtime dependency or create a second recipe authority. The underlying OpenJDK Java, HotSpot and native source files retain their original per-file licenses; this explanatory record is Apache-2.0.
