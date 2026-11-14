<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Separate AST and build work at catalogue acceptance

This is a successor boundary record. The original `AST_BUILD_STATUS.md` and
`.tsv` retain their original audit epoch unchanged. Collection runtime acceptance
does not turn compiler/build source presence, a parser fixture, or a published
pull request into full AST/build runtime qualification.

| Work | Review references | Current evidence boundary |
| --- | --- | --- |
| Exact hierarchy identifiers | [Synexia PR #12252](https://github.com/hsoliwal/com.synexia/pull/12252), canonical core `9f500fff0bcdf96807899e96090e245fb530c48b`, published metadata head prefix `da720465`; [M3JDK21 PR #1265](https://github.com/hsoliwal/M3JDK21/pull/1265), head `77c89d24b801ae62e5cabbaa313564bf70010730` | Source-sealed canonical and target SDK lanes each pass five tests. The hierarchy task owns its actual regression/runtime proof and metadata custody cases. Those receipts qualify that finite hierarchy work; they do not establish the broader compiler/build requirements in the historical matrix. |
| AST cache lifecycle candidate | [Synexia PR #12261](https://github.com/hsoliwal/com.synexia/pull/12261), published head prefix `f2146bda` | Candidate only: actual SDK has five failures, and the real-parser compile closure reports 831 errors. A synthetic lifecycle fixture passes 14 candidate methods and retains four baseline counterexamples. Its fixture explicitly records `REAL_PARSER_RUNTIME_NOT_RUN` and `SDK_BLOCKED`; no product receiving or full parser-runtime pass is claimed. |

The cache candidate SHA-256 is
`93d7e46a4a94f2a5d1389a6b4eee1d97b0bba43454a98ed4c5043dfdd9faf3a3`.
Its baseline fixture stops at the LRU failure sites; the candidate's new methods
cover both supported cache policies. Fixture API equality is limited to that
synthetic compilation. Failed recipe or real-parser qualification is never
replaced by a passing synthetic lifecycle probe.

`LATEST_AST_SCOPE.json` records the separately owned local receipt paths and
hashes inspected for this boundary. Public PR identities were supplied by the
publishing parent agent; source-qualified core and local execution receipts are
listed independently. Broader CI investigation and target-wide build completion
remain separate and are not filled by the 255-test collections receipt.
