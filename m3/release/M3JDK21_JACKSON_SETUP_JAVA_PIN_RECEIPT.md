<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3JDK21 JMOD setup-java exact-version repair receiver receipt

Date: 2026-10-29. Repository: `hsoliwal/M3jdk21`, receiving branch
`fix/setup-java-exact-semver-20261029`. This file contains **data-only
execution evidence**, not a Maven/OpenRewrite recipe or executable authoring asset.

## Root cause and source pin

GitHub run 40804547171, job 123220229504 failed in
`actions/setup-java@v5` before source build. The workflow requested Temurin
`21.0.8+9`; that runner enumerated `21.0.8+9.0.LTS` but not the former.
No other job's Java selector is in scope.

| Field | Pinned value |
| --- | --- |
| Product base commit | `669a949d192345acefcfc5e9bb0b3724174d270c` |
| Receiver path | `.github/workflows/m3sdk-jmod-receiver.yml` |
| Source Git blob | `50f25264d66c89dcef941c2fb23d81936e1514a6` |
| Source SHA-256 | `dda5c5b17a16ab37d0ab04cd562703cfb3271fbf17535c432857ef36d5f4cec3` |
| Source bytes | 12172 |
| Postimage Git blob | `2fc6414024b10befee1a3a5aded9c630d91b669c` |
| Postimage SHA-256 | `5d82628a940f755a1d565098c8d0e9ee00d0c7893ab7b9313629578acb0f81d8` |
| Postimage bytes | 12178 |
| Canonical recipe | `M3JDK21_TEMURIN_SETUP_SEMVER_1` in `hsoliwal/com.synexia` |
| Synexia source blob | `8e795e89c6894dac105d1c8ccef0b4effd85afa8` |
| Synexia review | PR #13981 (draft) |
| Changed YAML scalar count | 1 |

## Executed stages

- SOURCE PIN: PASS. The exact Git blob and file byte count were verified
  independently against the connected GitHub checkout.
- SYNEXIA RECIPE COMPILE: PASS using local Java 21, `javac --release 21
  -Xlint:all -Werror`; source blob matched the pushed Synexia Java source.
- SYNTHETIC PERMUTATION HARNESS: PASS (1036 checks including fixed-point,
  refusal, decoy-code and ambiguity cases).
- EXACT PREIMAGE RECIPE KERNEL REPLAY: PASS. The canonical Java class executed
  on the verified original 12172-byte receiver file and produced the recorded
  12178-byte candidate. Replaying the candidate is byte-identical.
- YAML PARSE: PASS. Both versions parse and four job owners remain present;
  only the Jackson setup-java version scalar changed.
- RECEIVER FILE COMMIT: PENDING at the time this receipt was authored.
- CANONICAL MAVEN/JUNIT: NOT RUN in this environment (the remote Synexia
  PR CI was queued); there is no claim of OpenRewrite SDK execution.
- RECEIVER GITHUB-HOSTED CI: NOT RUN at receipt creation.
- JDK SOURCE BUILD / JTREG / JNI / ANDROID / IOS: NOT RUN; unrelated to this
  one-line environment selector change and cannot be inferred as passing.

## Independent blockers intentionally retained

`M3String.java` source-seal drift (run 40804547130);
ClassVer missing `02-compile.stdout.log` (40804547292);
JMOD receiver ledger drift in `m3/tooling/jmod-recipes/pom.xml`
(40804547171); and public naming rejection of a date-stamped experimental
directory (40804547149). This repair may unblock the Jackson job's setup-java
step; it does not resolve those other errors, production readiness, or any
full-platform release claims.

No Synexia recipe Java, YAML, manifest, execution engine or memory store is
copied into this receiver. Product admission remains gated by its own CI.
