<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Receiving CI follow-up

This four-file maintenance change addresses two reproduced failures in
[PR #1522](https://github.com/hsoliwal/M3jdk21/pull/1522) at source head
`066b7a15c29388f3444ab3cf0550ecf1df698c6d`. `CHANGES.json` records exact before/after
seals and source/receipt lineage. The implementation and test postimages from the
canonical collection manifest remain unchanged.

## Dependency closure

The full collection job compiled all 268 production and 264 test Java sources,
then Surefire could not resolve the repository-specific source-built JUnit
Platform Launcher from Central. Add the already-vendored launcher after Platform
Engine in the existing dependency tuple. Its unchanged POM and all three direct
dependencies retain their exact coordinates and build order.

Reuse the working Platform Engine source verifier for the launcher, changing its
module-specific resource prefix. This repairs the repository-root calculation
and uses the manifest's existing coordinate/source-JAR fields. The original
verifier's failure was reproduced; the candidate verifies all 88 sealed files
and the complete 80-file production Java inventory. Both event filters now watch
that dependency. Every workflow job and consumer assertion stays unchanged.

## Physical-state source admission

The current-owner workflow still pinned the snapshot preimage. Advance that one
literal to the qualified receiver Git blob `2caf4faf78ea20ccb41de9a61b08790855b4edd4`.
All six exact-source guards now match the received owners; the other five pins,
Java proof mains and strict native commands are unchanged. The original preimage
is retained in the canonical recipe fixture. The published qualification already
records 13,578 byte-equal physical-state assertions and selected API parity.

## Evidence and remaining gates

`LAUNCHER_REPAIR.json`, `PHYSICAL_SOURCE_PIN.json` and `INDEPENDENT_REVIEW.json`
record the bounded source/metadata checks. `QUALIFIED_SOURCE_READBACK.json` binds
the earlier 60 guarded targets, 88 mirrors and complete 122-file publication to
the exact prior head. Its historical CI snapshot and both actual failure logs
are retained here. The two launcher source-verifier outputs preserve the failed
baseline and passing candidate.

The refreshed local environment has no JDK 21 compiler or Maven. The full
Java21/Maven/JNI owning workflow must execute this follow-up; no new runtime pass
is claimed by these source/metadata checks. The earlier 657-file full-module
receipt remains tied to its recorded inventory. This commit changes its CI
qualification script, while all production/test Java and C source identities
remain exact. Repository-wide CI remains a separate, unmet acceptance gate.

STRING remains QUALIFICATION_REQUIRED. Installed M3JDK21, jtreg/JCK, native
desktop UI and Android/iOS runtime/host/device qualification remain open.
