<!-- Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0 -->
# Word identity and precomputed-row qualification

This Maven crate changes no production implementation. It reuses the existing M3WordRows, M3WordRowsNative, M3TokenId and Synexia IndexHandle owners, plus the unchanged M3WordRowsImageRefusalContractTest fixture and accessor/generation/cursor helpers.

The dedicated pom.xml passed9 JUnit cases and49 assertions inside those unchanged donor helpers. Tests cover27 row-ID roundtrips across9 language boundaries, zero/overlay/foreign/out-of-range IDs, independent generation binding, owner-bound cursors, Java batch/read-only behavior and calibrated allocation.

In the declared source fixture, entry1 denotes a/targetrow0; its raw bits select ab/targetrow1. Equal packed IDs also exist across different fingerprints. These executed counterexamples show why a full snapshot-bound remap is still required. This crate does not implement that remap.

## Measured scope

The actual private decoder consumes the unchanged donor's3row heap fixture. Its filesystem-heavy main, FileChannel.open/map path and native library loading were not invoked. This does not qualify OS sharing, fixed virtual addresses, native JNI, all languages, the full JDK or other platforms.

The primary receipt records9PASS/0FAIL/0SKIP on the configured Windows11 amd64 Java21 toolchain, offline Maven3.9.14 and exec3.3.0. The warmed cursor window measured0 allocation bytes over300000 iterations. Elapsed time is one local smoke sample; no speedup or portable zero-allocation claim is made.

The original uncalibrated assertion observed56bytes. A diagnostic reproduced56bytes in its first zero-iteration window, then0 in empty/cursor windows through900000 iterations. Two explicit empty controls now warm the measurement boundary. Stable control and measured cursor loop still assert exactly0bytes; no baseline subtraction or nonzero allowance was introduced. The original QA input and recorded observations remain under proof/. Historical raw payload custody limits are explicit in calibration-custody.json. The fresh full receipt is authoritative for admission.

## Replay and provenance

input.json contains the exact five source snapshots and QA. WORK_ORDER.json and FOSS_REUSE_DECISION.tsv bind existing-owner reuse. Notices remain verbatim; LICENSE retains Apache2. No JAR/binary or production Synexia dependency is added to the JDK.

Read config.json as INPUT. Attach input.json["sources"], ["qa"] and ["workOrder"] to INPUT, then execute run.py with EMIT printing JSON. It runs the existing Maven/RAM compiler over sealed loopback packets. M3LexiconCpuProof.java preserves that driver's exact source. pom.xml also binds exec to test when the sealed exec.args are supplied; missing arguments fail rather than report an empty successful test run.

The recorded run uses provisioned read-only driver/POM/JAR/cache inputs at exact host paths; their contents and native temp routing are verified before children. Another host needs explicit cache/path qualification. This is a replayable host-bound proof, not a portable build claim.

A780byte dedicated descriptor occupied one4096byte temporary file under the authorized K: temp lane. Exclusive creation and ownership/path/hash checks guard cleanup after the child is reaped. The same545MiB cap and8GiB reserve remained active; the peak reached the cap exactly and zero build files remained. The F: charge is a sealed historical baseline, not a fresh allocation audit.

Verify the full receipt wrapper's zlib hex payload, expanded byte count and SHA256. Its JSON includes actual Maven arguments, source/packet hashes, logs, outcomes and storage observations.

Canonical donor path: synexia-openrewrite-recipes/crates/m3-word-identity-contract-r241/.
Public receiver path: m3/lexicon/recipe/word-identity-contract-r241/.
