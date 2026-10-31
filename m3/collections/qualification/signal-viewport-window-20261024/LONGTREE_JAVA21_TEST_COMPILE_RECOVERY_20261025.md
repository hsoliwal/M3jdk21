<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# Actual original collections test-compile blocker: reused approved successor

The hosted signal viewport run [40099828430](https://github.com/hsoliwal/M3jdk21/actions/runs/40099828430) compiled 296 real `m3-collections` production Java files, built and installed the existing `m3-arrays` module and source-vendored JUnit, then stopped before executing collection tests. Java 21 rejected the preexisting `M3LongTreeSetMaskCompactionContract.java:12` literal `0x4D33534F52544C` with `integer number too large`.

Do not skip tests, exclude the unrelated test source, relax compiler flags or introduce a second repair recipe. Reuse the **already-authored canonical Synexia source-sealed** `M3Jdk21LongTreeSeedSyntax20261023` from [Synexia PR #13198](https://github.com/hsoliwal/com.synexia/pull/13198), also received in [M3JDK21 #1869](https://github.com/hsoliwal/M3jdk21/pull/1869) and [#1881](https://github.com/hsoliwal/M3jdk21/pull/1881).

* Exact original product blob: `74369df9ef10241fb748b710ab50ec1075edbee6`; SHA-256 `7f1f00afbec94d40b24c25256ac531ec754fe6bae856f2001d6f8d3944576e25`.
* Exact approved product successor blob: `8a33b08f52ef2da14354499d577090b099e2c515`; SHA-256 `e2b20b7d2dc79bb821236e3c0f9459ea7db25b6d443c5047fe08118ce34f1afe`.
* The **only textual change** is appending `L` to the existing hexadecimal test seed: `0x4D33534F52544C;` → `0x4D33534F52544CL;`. Numeric value, tests and production source are unchanged.
* The target source was fetched back from this signal feature branch after transfer and matches the Synexia approved Git blob byte-for-byte.
* Independent Java 21 `--release 21 -proc:none -Xlint:all -Werror` grammar RED/GREEN probe (2026-10-25): old literal refused, corrected literal compiled and printed `4d33534f52544c`. This probe is **not** full collection JUnit acceptance.

The already-existing [collection result CI run 39971879002](https://github.com/hsoliwal/M3jdk21/actions/runs/39971879002) on another qualified branch reported 601 JUnit tests (0 failures, 0 errors, 2 skipped) using this exact corrected test blob. It does not certify the current signal branch.

**Signal receiving gate remains HOLD:** execute original module compiler and complete target JUnit tests on this PR's exact new head, plus canonical signal recipe SDK and modified JDK/JNI/GC/jtreg/JCK/OS/performance qualification. Retain original strict test inventory, live arrays build and canonical recipe ownership in Synexia.
