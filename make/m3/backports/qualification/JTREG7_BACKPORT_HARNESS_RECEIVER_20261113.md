<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# JTReg 7 harness — exact generated M3JDK21 source-built backport workflow receiver

**DRAFT / HOLD. This receipt is not a passing runtime test or product release.**

The original M3JDK21 `m3jdk21-backports.yml` Linux jobs could build
`make JOBS=2 images`, but the subsequent JTReg test command failed before
test execution with `Error: jtreg framework is not found` and requested
`configure --with-jtreg`.

The correction is authored and executed **only** by Synexia's canonical
`M3HashPinnedTextSnapshotRecipe`:
[Synexia PR #17292](https://github.com/hsoliwal/com.synexia/pull/17292).
The source template, recipe implementation, Maven and Jupiter test code
remain private in Synexia. The M3JDK21 receiver takes only the exact generated
workflow postimage and this passive acceptance receipt.

## Frozen exact-source identities

- Received branch originated from current product
  `hsoliwal/M3jdk21 master@65d2636ee26956bc638d56e5433c9e275f70997d`.
- The original `.github/workflows/m3jdk21-backports.yml` Git blob was
  `85549267946f4e4ee2ca1928b652c44edd657068`, unchanged since
  `master@0b7bb3ab0e0e1fec6c5818428dad81ae30e86fbb`.
- Canonical original SHA-256:
  `592369818ed986f1f3b30e36f4d0ee21d6647f4bfa9294d80a16d1dfbb5bb48f`.
- Canonical postimage SHA-256:
  `4badf60655fcb71a450b4398498b2806117a4d3058b294ae4d2de63e3141f30d`.
- Exact generated source/product Git blob:
  `ccab13210c39c103378a0f2285512a537460eec6`.
- Canonical source manifest Git blob:
  `4f55c1c4ac9c294aa8af8de91f2cac7ab46c084d`.

GitHub product readback verified the complete generated workflow byte-for-byte
against Synexia's source-owned postimage.

## Precisely four Ubuntu 22.04 test jobs

| Original job | Source-built JTReg target |
| --- | --- |
| `javadoc-8347112` | `test/langtools/jdk/javadoc/doclet/testCopyFiles/TestCopyFiles.java` |
| `security-properties-8364182` | `SecurityPropertiesTest.java`, `SystemPropertiesTest.java` |
| `keystore-instant-8374808` | `CreationInstant.java`, `TestKeyStoreBasic.java` |
| `open-fds-8359706-linux` | `test/jdk/sun/tools/jcmd/TestJcmdSanity.java` |

Only those original jobs receive `jtreg7` as an APT dependency,
`/usr/share/jtreg/lib/jtreg.jar` existence check, `dpkg-query -W jtreg7`,
and `--with-jtreg=/usr/share/jtreg` during `bash configure`.
The source-built JDK image and selected tests still run as before. No
standalone simulated JDK or copied test harness replaces the actual build.

The unrelated jcmd packaging job and macOS JDK job remain byte-for-byte
unchanged; no `src/java.*`, HotSpot, native/JNI, license, Maven reactor or
historic backport adaptation file was altered.

Ubuntu 22.04's official `jtreg7` package supplies this JTReg home. The
official OpenJDK 21 build guide requires pointing `--with-jtreg` to the
parent of `lib/jtreg.jar`.

## Independent acceptance still mandatory

- [ ] Actual canonical private Java 21 Maven/OpenRewrite/Jupiter fixed-point
      and source-drift refusal run, with non-skipped Surefire receipt
- [ ] Product JDK 21 `configure`, `make images` and the original JTReg
      test targets execute and report genuine results (not `jtreg.jar` presence alone)
- [ ] Javadoc, JBS-8364182, KeyStore, jcmd and other unmodified runtime
      backport gates pass independently
- [ ] Java 21 API/ABI, JNI/HotSpot, installed image/jlink and release
      qualification are reconciled against full current product source

No claim that all JEPs/JSRs/JBS fixes are implemented or accepted, or that a
Maven check qualifies the integrated JDK runtime. Preserve the protected
product branch and donor provenance until real gate closure.
