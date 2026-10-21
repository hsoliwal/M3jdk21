<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Current-master additive reconciliation (20 October 2026)

This is a source-preserving follow-up to [PR #1305](https://github.com/hsoliwal/M3jdk21/pull/1305), not a replay of its old source file over current `master`.

## Inventory and authority

- Frozen pre-convergence `master`: `d6d76b563050fc13c4fa79bc494edf29f1614b37`.
- Original PR head: `4b9db5e063e900a2012b7f161cb3b7a492877c47`.
- Current `M3ObjectIndexSnapshot.java` Git blob: `e2cff159750d9b7f45cd438a8d517aff8d5bff43`.
- Original PR's source Git blob: `f2c20cd4ac57561f0a36d3a2a2c5fcc83f3ff8f8`.
- Original recipe's SHA-256 pre/post pins remain in `RECEIVER.md`. They describe the historical qualified transformation, not the current-master bytes.

The current owner already implements `filteredByPhysicalStatePlane(int[],int,int,Backend)` and its Java-only overload with the same physical-slot range check, `M3IndexPathMasks.select` delegation, stable-order `withRows` result and required/excluded full-width int masks. The current source has additional independently evolved code; it must **not** be replaced by the older PR source.

The two original focused test files are already on `master` at exact identical Git blob IDs:
- `M3ObjectPhysicalMaskSelfTest.java`: `01945ebc69137affa7ab3ef708fcbb0cd95a5c24`.
- `M3ObjectPhysicalMaskTest.java`: `10931f3f8316b3cecd0f27cbf25e468175022b19`.

## Recovered additive omissions

The earlier PR contains three proof artifacts absent from current `master`: its Java/JNI workflow, category-ledger TSV and receiver qualification description. This follow-up installs them without modifying existing Java or JNI source and without removing or replacing any master files. The catalogue and receiver description are copied byte-for-byte from the historical head. The workflow is carried forward with one mandatory reconciliation: the source guard checks the *current* Git blob rather than the *historical* postimage SHA-256. The historical SHA-256 stays in the receipt.

## Gates

Run the new GitHub workflow at this exact head:
1. Exact Git blob custody for the current owner, and SHA-256 custody for both test sources.
2. Strict Java 21 `javac -Xlint:all -Werror` and the existing Java oracle.
3. C17 JNI selector build and `java -Xcheck:jni` native-required parity.
4. Owning M3 collections Maven/JUnit test.

A GitHub workflow being created, queued or failing before runner steps execute does not establish a PASS or a source failure. Do not claim the full M3JDK/JDK image, jtreg/JCK, cross-OS ABI or performance gates here.

## Contract

**CODE PRESENT != ARTIFACTS PRESENT; ANCESTRY PRESERVED != ACTIVE TREE INTEGRATED.**
This PR fills the proof-artifact gap while leaving current M3Java/JNI owners authoritative. The original PR's history, attribution and failed/pending receipts remain inspectable.
