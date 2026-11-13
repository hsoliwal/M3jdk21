<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Source-built JDK21 JTReg test-harness qualification — 2026-11-13

**Candidate/HOLD; this document is a passive source and build proof receipt, not a claim of passing source-built JTReg or releasing M3JDK21.**

Previous M3JDK21 CI completed `make JOBS=2 images` on Ubuntu 22.04 but failed
`make JOBS=2 test TEST=test/hotspot/jtreg/serviceability/dcmd/vm/SecurityPropertiesTest.java`
before executing tests: `Error: jtreg framework is not found`. The OpenJDK build
requires a JTReg installation containing `lib/jtreg.jar` and the
`--with-jtreg=<jtreg home>` configuration option:
https://github.com/openjdk/jdk21/blob/master/doc/building.md .
Ubuntu Jammy provides versioned `jtreg7`, installing
`/usr/share/jtreg/lib/jtreg.jar`:
https://packages.ubuntu.com/jtreg7 .

Only four current Linux backport jobs are modified in
`.github/workflows/m3jdk21-backports.yml`: `javadoc-8347112`,
`security-properties-8364182`, `keystore-instant-8374808`, and
`open-fds-8359706-linux`. They now install `jtreg7`, verify its JAR
exists, log the package version and configure the same JDK21 source with
`--with-jtreg=/usr/share/jtreg`. Existing target tests, build commands,
M3 String/JNI/HotSpot code, the jcmd non-JTReg job, macOS job and Maven POMs
remain unchanged.

## Exact source authority

- Public product baseline: `hsoliwal/M3jdk21 master@0b7bb3ab0e0e1fec6c5818428dad81ae30e86fbb`.
- Old public workflow Git blob: `85549267946f4e4ee2ca1928b652c44edd657068`.
- Current product workflow Git blob: `9348c09b9817dd44846f6481aabd906d7c8d698d`.
- Original complete workflow SHA-256:
  `592369818ed986f1f3b30e36f4d0ee21d6647f4bfa9294d80a16d1dfbb5bb48f`.
- Exact candidate complete workflow SHA-256:
  `52199e91330e75bb4bda2bd4beba221d6902b299a1fa6e191327beba205ad10d`.
- Private source-owning canonical recipe: [Synexia PR #17276](https://github.com/hsoliwal/com.synexia/pull/17276),
  using the **existing** `M3HashPinnedTextSnapshotRecipe` engine and its
  `m3jdk21-backport-jtreg7-harness-20261113` two-state manifest.
- GitHub preimage/postimage byte-for-byte readback:
  **verified** between private canonical resource and draft public receiver.

## Acceptance gates — not skipped

1. Canonical private Java 21 Maven/OpenRewrite first apply, second-pass fixed
   point, source drift refusal, four-job scope and unchanged unrelated jobs
   (dedicated CI, **NOT YET VERIFIED**).
2. Product Ubuntu 22.04 package installation, `/usr/share/jtreg/lib/jtreg.jar`
   presence and configure output — **NOT YET VERIFIED**.
3. Current M3JDK21 image built from updated source and actual JTReg execution
   for `TestCopyFiles`, `SecurityPropertiesTest`, `SystemPropertiesTest`,
   `CreationInstant`, `TestKeyStoreBasic` and `TestJcmdSanity` —
   **NOT YET VERIFIED**.
4. Full product Maven control-reactor, JDK/JNI/HotSpot/GC/jlink/CDS,
   cross-platform ABI, source provenance, release gates —
   **NOT YET VERIFIED**.

The test harness repair is independent of the exact JDK-8364182 vmSymbols
source-lineage correction also carried on this **draft** branch. Do not infer
passing JDK behavior from the presence of a test-harness package, nor equate
failed pre-harness CI with red JDK tests. No protected master mutation or
unqualified release is authorized by this receipt.
