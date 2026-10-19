# Source-built producer and TQ sync fixture qualification

SPDX-License-Identifier: Apache-2.0

This qualification reuses the Maven recipe in `../source-reactor/pom.xml`, the existing vendored producer POMs, the existing source-hash helper and the monitored hosted execution lane. It adds no dependency resolver, compiler, recipe engine or runtime ownership protocol.

The hosted workflow first runs the six qualified model cases in a fresh repository, then expands the sparse checkout under its storage monitor. It verifies all selected module source/provenance bytes and metadata, installs 57 existing producer modules, checks their actual target/installed JARs, and executes the unchanged TQ sync fixture tests through the Maven recipe in the same explicitly isolated repository.

Producer roots are the existing `rewrite-java-21`, `rewrite-test`, `slf4j-api-2.0.9`, `junit-jupiter-5.10.2` and `junit-platform-launcher-1.10.2` module paths. The SLF4J root supplies TQ's declared runtime dependency. The launcher is an implicit Surefire prerequisite that cannot be distinguished by bare group/artifact when both launcher versions are collected. Producer POMs retain their own upstream-test policy; that does not qualify the complete donor test suites.

The consumer uses `source-reactor/pom.xml -Dm3.receiver.relative=../../../.. -pl com.m3:tq-sync -DskipTests=false -Dmaven.test.skip=false verify`. Its three required JUnit methods are `exactBytesAndFixedPoint`, `refusesDriftMissingAndDuplicate` and `retainsUnrelatedFiles`. All 24 generated fixture postimages must match their resource bytes and hashes. The tests replay fixtures beneath `target/generated`; they do not replace current product files.

The qualification imports only the guarded pure `verify_crate.sha256(Path)` API and the existing model custody helpers. It does not execute or claim success for the complete 131-row verifier or the known defective per-module verifier CLIs. All provenance kinds, including delomboked sources, resources, excluded source and donor notices, remain intact.

Admission requires the exact inventoried Maven 3.10.0 release, JDK 21, existing Python 3.12, an isolated Maven home/repository/temp, the 8 GiB after planned-output reserve, and the existing 512 MiB aggregate cap. The larger Git expansion and Maven children are monitored. Dependency/plugin downloads retain declared versions and remain inside the census; there is no warm-cache, substitute-fork, skipped-consumer-test or storage fallback.

Receipts retain complete bounded build logs, actual Surefire reports, source and artifact hashes, selected GAVs, toolchain identity and process/storage evidence. JARs and generated postimage payloads are verified in the job; the artifact exports their hashes and reports, not binary recovery.

A passing build qualifies producer build/installation and **TQ sync fixture JUnit** only. Fresh upstream API equivalence, complete donor suites, copied native-library rebuilding, current product custody, the separate runtime TQ/JNI harness, canonical overflow recipe, original private `-synexia-source-1` model, matched JDK and performance/retention remain independent gates. The standalone `verify-plan.py` owner/map reconciliation is separate and is not silently rehashed or called by this lane. Overall migration and optimization remain active.
