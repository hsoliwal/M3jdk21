<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->

# M3 physical-slot qualification: additive recovery receipt

## Canonical owners retained
Target: `hsoliwal/M3jdk21` (`master` at `2779aba3f98f31c324be43f731be7914e1a05935`).
Donor: `hsoliwal/M3jdk21#1305` head `4b9db5e063e900a2012b7f161cb3b7a492877c47`.
The existing `M3ObjectIndexSnapshot.filteredByPhysicalStatePlane` method pair in `master` is byte-identical to the donor; do not duplicate it or its immutable overrides.
The Java class and both production-test source files are unchanged by this recovery.

## Exact retained donor additions
| Path | Original and restored Git blob |
| --- | --- |
| `.github/workflows/m3-object-physical-slot-20261018.yml` | `99a84028dce4aa8bc55978f6030df6352eb45fb5` |
| `m3/collections/algorithm-catalog/object-physical-state-32bit-20261018.tsv` | `420c3da10a275b95b86386d8df5191e9f35c9aa2` |
| `m3/collections/qualification/m3-object-physical-state-20261018/RECEIVER.md` | `d51f1785fd2a48da4cba158a2dd1b68583781d27` |

## Non-destructive mapping append
The preimage `m3/docs/name-mapping.json` Git blob `0df01e865044aa85bbf6f8a029edfb0e9c9198` contained 66 mappings, and the historical donor contained 60. Exactly one missing mapping for `com.m3.collections.M3ObjectIndexSnapshot.filteredByPhysicalStatePlane` was appended. Existing 66 mapping entries, all non-mapping keys and optional module mappings remain in their original order and values. Postimage blob `83c899ec3794199cb83dd80b4ec8ada1dfa78e9e`.

## Observed Git diff
Compared with pinned target head, the recovery prior to this receipt contained precisely 4 paths: three added, one append-only mapping modified; **0 removed paths and 0 deleted lines**. Existing production/test blobs verified unchanged:
- `M3ObjectIndexSnapshot.java`: `e2cff159750d9b7f45cd438a8d517aff8d5bff43`
- `M3ObjectPhysicalMaskSelfTest.java`: `01945ebc69137affa7ab3ef708fcbb0cd95a5c24`
- `M3ObjectPhysicalMaskTest.java`: `10931f3f8316b3cecd0f27cbf25e468175022b19`

## Gates and limits
This is a source-custody and qualification recovery, not new runtime implementation. Actual exact-head Java 21 Maven/JNI workflow execution, differential checks, C17 `NATIVE_REQUIRED` parity, source-built JDK/jtreg/JCK and performance tests must still be verified before merging. Do not infer those gates from source blobs or historical donor receipts.

No force push, rebase, squash, runtime dependency change or `java.base` promotion is authorized by this receipt. M3JDK21 owns the runtime; Synexia retains build-time recipe/provenance authority.
