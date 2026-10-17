# Received core qualification

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

All three assigned gates passed on the actual SDK-received M3jdk21 tree, frozen at master f3c615693c41bac1bf6ff49ba53e83f85425bca0. The executions are bound to canonical source commit 866e240ec1ac8600999d474eb7b8ce1930e326e3 and SDK_RESULTS SHA-256 cb219035b789abcdcc28be9149b8ecbb000247adfd9859c01cc3e1db8ae02076. This package copies the actual logs and original execution receipts; it does not rerun tests or replace their identity with a later workflow-only SDK revision.

## Results

| Gate | Actual result | Evidence |
| --- | --- | --- |
| Generated verify_donation.py with --cost-probe | PASS; 117 main and 107 test Java sources compiled under Java 21 with -Xlint:all,-module -Werror; 211 tests, zero failures/errors/skips | [Verifier qualification](verifier/qualification.json), [complete gate log](execution/donation_verifier.log) |
| Existing standalone self-tests | M3LazyTreeMapSelfTest and M3LazyValueStateSelfTest PASS | Named logs under verifier |
| Separate module packaging | Requires java.base only; original license/provenance resources and the admitted SDK catalogue included and checked | [Module descriptor](verifier/06-module-descriptor.log), verifier qualification |
| Segmented native gate | Strict C17 and UBSan builds; checked JNI passes 66 boundary ranges and 1,200 changing random ranges | [JNI parity](verifier/09-checked-jni-parity.log) |
| Count-owner native gate | All six count tests PASS in a fresh JVM with -Xcheck:jni and -Dm3.interop.requireNative=true, using the actual UBSan path-mask library | [Required-native JUnit log](verifier/10d-object-counts-checked-jni.log) |
| Prepared allocation probe | Five trials report zero bytes in the prepared loop on this JVM | [Allocation probe](verifier/10-prepared-allocation.log) |
| Source-admission regression | 34 tests, zero failures/errors/skips, actual received-source qualification | [Proof receipt](source-admission/source-admission-proof.json), [individual cases](source-admission/source-admission-tests.log) |
| Offline core Maven install | BUILD SUCCESS; 58 test suites / 211 tests with zero failures/errors/skips; install includes the verify lifecycle | [Maven log](execution/core_maven_install.log), [compact suite results](maven-suite-results.tsv) |

The verifier and Maven each ran the same 211-test suite; these are not 422 distinct tests. The six count tests were additionally repeated with JNI required. Cost measurements are diagnostics and do not establish universal performance superiority. Full OpenJDK reactor, jtreg and HotSpot gates were not run; this evidence covers the separate collection module and the named native owners. It does not advance the active STRING phase.

## Reproduction and source identity

Each file under execution ending in -execution.json preserves the exact argument vector, UTC times, exit status, source census, verifier digest, SDK result digest and log digest. JAVA_HOME was /tmp/m3-toolchain/jdk-21.0.12.1+1, Maven was /tmp/m3-toolchain/apache-maven-3.9.11/bin/mvn, the offline dependency repository was /tmp/m3-maven-repository, and JUNIT_CONSOLE_JAR was /tmp/m3-toolchain/junit-platform-console-standalone-1.12.2.jar. The original execution wrapper is retained for inspection.

[received-core-source-census.tsv](received-core-source-census.tsv) contains the exact 236-file source census in the same encoding sealed by all three executed gates; its SHA-256 is b8db6c7000d978ac967415777aee1fe071b049c78f84119a3a5afc04a14d1240. Every execution confirmed unchanged source bytes before and after running. This package rechecked the same identity. [received-core-build-inputs.tsv](received-core-build-inputs.tsv) separately records the core POM, generated verifier, admission driver and all receipt/helper inputs for comparison with any successor.

The built Maven jar and installed jar are byte-identical: SHA-256 0e19f670d75eab643dc047aae32975aeef1990d9dc005ce489dc4275aa9f47ec, 504,469 bytes. The generated core POM and installed POM are also byte-identical. [received-core-summary.json](received-core-summary.json) records their paths, complete artifact hashes and gate totals. Binary jars and verbose JUnit XML are not duplicated here; the compact suite table preserves each original XML path and SHA-256.

[BASELINE_REPAIRS.md](BASELINE_REPAIRS.md) explains the two current-master constructor warnings, four incorrect existing test expectations and prior source-admission gap, with failed logs and exact before/after receipts. COPIED_EVIDENCE.tsv gives every copied file's origin and digest. SHA256SUMS seals all package files except itself; the /tmp publication inventory includes its own digest and every relative evidence path.
