<!-- SPDX-License-Identifier: Apache-2.0 -->
# Executed Java21 source-generation RED/GREEN and receiving HOLD

Parent PR #16571: `433a19772a84278dea5df71a0427df4bdd7f9bab`.
All original JNI/C17, PMEMSDK source-image, GramIndex and source-selected search owners stay unchanged.

## Exact two-owner source identities

| Owner | PRE SHA-256 | POST SHA-256 |
|---|---|---|
| PmemGramCampaignAdmission.java | b6f7ad4fc485e9c6b8bf0f54d5a5eb85e0f9edc959144db407e3639872f12905 | 9804dbbbb5b8b27ee811064a8367a395fd7881f92fc9f9992fdf4d4c55e6b44d |
| PmemGramCampaignAdmissionJUnitTest.java | 88e8b29fd1639c66f01d8ae97ee597fca09d796091385a6be340e5e120d32c63 | f7b04b3214e0da586864a86a5d2a79d1569678847de485a90f9922aed3e997a3 |

## Locally executed with OpenJDK 21.0.12.1

- `javac --release 21 -Xlint:all -Werror` for exact original and candidate source with API-compatible first-party stub signatures: **PASS**.
- Independent RED: original review-to-search source incorrectly accepted same-length external mutation of a gram-pruned source file: **REPRODUCED**.
- Independent GREEN: fixed source throws `SecurityException` both for mutation during reviewer callback and mutation during search: **PASS**.
- Deterministic 20,000-source synthetic worklist, **11 focused assertions**, **274 exact candidate work orders**, and **20,000 final source-integrity reads**: **PASS**; tested under JIT and interpreted Java.
- Original source campaign 20,000-worklist oracle, **846 assertions**, identical before/after outputs: **PASS**.
- Original and reviewed `javap -p -s` private and public method descriptors: **BYTE-IDENTICAL**.
- Six JUnit-shaped tests against a test-only packed FILE stand-in storing original hashes and mutable `source.bin`: **PASS**. This proves the test code and intended refusal, not production PMEMSDK/JNI/Jupiter integration.

## Explicitly NOT RUN / HOLD

Actual Maven/OpenRewrite `Recipe.run` with real SDK, Jupiter on original PMEMSDK FILE, actual C17/POSIX JNI/JMOD/linked image, full 20k physical 50KB source corpus and RSS, complete quality plugins, model-context and 20k live Chrome inferences, independent repository provenance and public receiving M3JDK21/Eclipse API/JNI tests. These require exact current-head receipts and are named CI gates, not implied successes.

No protected `develop` rebase, no force-push, no automatic PR merge, no third-party challenge solution code copied.
