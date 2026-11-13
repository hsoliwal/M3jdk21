<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 full collection JNI + cursor test repair — candidate superset (HOLD)

This receipt records a **two-parent, no-loss review branch**, not a qualified
default-branch merge or release.

## Source owners and chronological provenance

- Cursor-failure test correction: canonical `hsoliwal/com.synexia#14510`,
  executed recipe `com.synexia.m3.FilteredOracleRepair20261030`,
  canonical commit `26f1d4da6b58f7ed01bfb5e309867fecd9be5732`;
  exact result-only M3JDK21 PR #2174 head
  `a45b4cbb5b72378b0b225ad3a67553b0ebed0499`.
- Native collection build prerequisite: M3JDK21 PR #2227 head
  `d2e9d37e158f9ad08125b20db97044ff9594a745`;
  only `verify.py` and its qualified dependency receipt were changed.
- Historical failing CI: run `41040700975`, job `124012481459`,
  baseline `bb66a8c16642a4e898dbc5fc7c3af0d5ed572136`.
  Summary: 765 tests, 1 failure, 48 errors, 2 skips.
  The unique assertion failure is
  `M3FilteredCursorExceptionTest.cursorErrorsPropagateAndSuccessfulNavigationKeepsItsTrace`:
  `cursor failure swallowed: [owner.lower[3]]`.
  The errors mainly report an unconfigured M3 array JNI library.

## Exact result-only source seals

| Path | Required unchanged preimage Git blob | Canonical tested postimage Git blob |
|---|---|---|
| `m3/collections/src/test/java/jdk/m3/collections/M3FilteredCursorExceptionContract.java` | `a23f5bdc5bf8d4200e01903cf068921375f6e23a` | `e46503c9ec2c42c17d37a20fcc0d0cc3673c9ff2` |
| `m3/collections/src/test/java/jdk/m3/collections/M3FirstVisibleAtomContract.java` | `ca0a522f153f13c0889f4e09729961f5b2d7d93b` | `5039fef1934d156e7da31850cd13491b777d4ddd` |
| `m3/collections/qualification/filtered-oracle-current-20261030/RECEIVER_RECEIPT.json` | `ABSENT` on the #2227 target head | `5d321688ffb7c41faa9e0aa32a0bf87ab17f8840` |

The target preimage SHA-1s were re-read on #2227 before constructing the
additive merge. The Java and C17 runtime source files remain byte-identical
to that branch; only the two original per-atom test owners and passive
source-qualification receipt are received.

## Admission and test limits

Canonical cursor recipe results: strict Java 21 compilation of 326 implementation
and 466 test files; focused Java/JUnit filtered family 33 tests, zero failures,
errors, skips; real Maven/OpenRewrite SDK replay 5/5 tests for each
current/historical dataset; source drift and fixed-point refusal proven.
The original receipt holds this historical evidence.

Native-library configuration PR #2227 locally reproduced 13 JNI-library
errors and reduced the same focused six-class set to zero errors with its
own built library. That proof does not mean the complete JDK suite passes.

**NOT EXECUTED / HOLD on the combined tree:** full Maven
`m3/collections/qualification/full-java-jni-20261018/verify.py` on Linux
with exact library bindings; full JDK/JNI/jtreg, whole-reactor and installed
image, sanitizer/cross-platform, 183-repository provenance/merge audit and
all-domain per-atom JUnit coverage. Existing baseline showed 2 skips; each
must be reviewed before any zero-skip qualification claim.

## Release gate

Merge / promote into `master` only after the combined exact-head Actions run
has executed test steps and yielded a green Java/native suite, current
source/ABI identities have been reverified, and the original parent PRs'
required review/ownership approvals are complete. A queued workflow or a
syntactically mergeable branch is not proof. **No force push, no rebase,
no private Synexia recipe source shipped to M3JDK21.**
