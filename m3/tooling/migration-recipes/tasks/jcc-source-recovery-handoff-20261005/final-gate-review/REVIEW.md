# E4 actual reference audit: independent execution review

**Disposition: the actual bounded reference audit passed and is consistent with the admitted checker, receiving receipt and frozen reference identities.** No concrete audit defect was found. This review admits the reported reference-audit outcome for packaging. It does not admit the pending donor-document supplement, the final publication manifest or any remote publication.

The actual audit is `reference-audit-v3/REFERENCE_AUDIT.json`, SHA-256 `5867bffd36e65f2fd755921d0dbf9981a006bc0e38134b2d0e4e6830030900ea` (769,381 bytes). It reports source commit `0c53b4f1dfe66c7eff05c8047b8fc681d26fecb8`, source root `0361e4a07b77a01df170523033fef01c4052ddc5`, E3 receiving preimage `0994ecd65e86600f417f4d8702838d8c2663af61`, receiving root `7e8f39ac999174f8d5b6028d638a68d8c317b070`, and plan seal `0aa1f4a3e080c36d233066f0a6e1dc8e1047e6f0592dba60829b8cca3177df64`.

## Actual invocation

The captured command runs the exact admitted checker `521c18944616fe46a3e347860c25a7283e1b74674c594e40996c787e01e96b81` through the configured Python with `-B`, in the E4 working directory. It records exit zero, 244 bytes of stdout and empty stderr. The stdout's audit hash and counts agree with the actual output. The audit timestamp falls inside the recorded invocation interval, 2026-10-05 17:42:54.173327–17:42:55.864392 UTC.

The invocation binds the actual receiving receipt `1f4ac9825d975520a04b7dc5ac1b282d96be32601c7abe2b0cfb676b0ff2df09`. The supplied invocation directory evidences one completed attempt and no failed checker invocation. The earlier readiness record remains explicitly a pre-execution record; it is not an additional run.

The companion invocation review independently verified the checker identity, command, working directory, timestamps, exit, log hashes, audit output identity and custody limits. Its report and seals are included in `COMPANION_BINDING.json`.

## Independently reconciled audit records

This review read the actual report and linked its identities to the bound inventories, installed binding, plan, prior manifests and exact receiving freeze. It directly compared all four installed owner bodies with their sealed after-resources and the audit's reported postimage identities. The four before-resources also match the reported preimage identities.

| Audit claim | Independent reconciliation |
|---|---|
| 19 source occurrences | Exact path set and byte/Git identities match the final source inventory and corresponding frozen external records |
| 55 proof occurrences | Exact path set and byte/Git identities match the proof inventory and corresponding frozen external records |
| 182 destination occurrences | All occurrence identities link to the receiving freeze or one of the four exact postimages; they cover 170 distinct paths |
| 134 copied receipts | Complete name set, origin paths, receiving paths and identities agree with the 131 additional origins plus three reserved origins, the installed binding and frozen copies |
| 89 preserved E3 files | Exact path set and identities match the retained E3 manifest and frozen receiving records |
| 56 preserved E2 files | Exact path set and identities match the retained E2 manifest and frozen receiving records |
| 1,008 frozen records | All paths, byte/Git identities, modes and group labels agree with the exact pre-run freeze: 391 fixed and 617 external records |
| 529 context/fixture paths | All 527 current-context paths and both separately declared malformed-UTF-8 fixtures occur in that frozen set |
| Root/mapping accounting | Reported 343 root entries, 201 declarations, 46 ordered mappings, 44 unchanged other records and 20 unchanged gates agree with the reviewed bound structure |

The 182 reference occurrences are 182 unique `(reference label, path)` pairs. There are 180 distinct labels because `blocked_record.observation` intentionally labels three different paths. Those repeated labels do not represent duplicate paths or missing coverage.

The destination basis counts reconcile exactly: 25 occurrences use the pinned destination acquisition, nine use preserved E2 payloads, 134 use exact E4 source-recovery copies, ten use current-context receipt copies, and four occurrences refer to materialized afterimages. The separately listed 89 preserved E3 payloads are not miscounted as an additional destination-reference category.

The actual checker reports reconstructing 936 recorded Git tree objects, traversing the final source/proof paths, comparing 121 carried custody originals and checking 132 explicitly omitted originals through their bound pointers. This review verified the executed checker and the resulting record relationships; it did not rerun those operations or repeat source publication verification.

`RECEIPT_RELATIONSHIPS.json` records these comparisons. Its 28 input files remained stable between reads. The independent invocation and receiving-execution reviews are bound separately, preserving their own input seals and scopes.

## Separate receiving-execution authority

The receiving execution review by `/root/current_mastery_contracts` is an independently sealed input: `execution-review-v3/REVIEW.md`, SHA-256 `1fa3be68d87d495a6458b778061d9bd670db574cc702e3f0ad9f00f64f392361`, with file seals `ba38a0fc867672d1fb4b0b6b757c0aef613594768ffcc94b5b71c2a4dd1c0479`. This report was read in full.

That review establishes the ten actual receiving stages, twelve Java tests, six Python methods, the 132 loop-derived refusal checks, four-file apply/fixed-point/rollback/replay behavior and completion's expected blocked exit. It independently re-read all 1,008 frozen bodies and checked the four actual postimages. The exact freeze is `6367c7d343ece1f1547e4db7873180f4dfae6dda83212edadf0564e0a93bc341`.

This reference review compares every audit freeze record to that document but does not duplicate the complete 1,008-body/runtime review. It relies on that separate authority for the full body-stability finding. The two reviews concern distinct operations: the actual focused receiving proof and the subsequent local reference audit. Neither reruns or newly qualifies the original four-test receiver fixture or a Java/JNI source lane.

## Custody, qualification and post-run additions

The audit custody object is exactly equal to the receiving receipt, final inventory and final input custody object. Six mandatory direct bodies and 21 observed direct bodies remain distinct from the two malformed-UTF-8 identity-only fixtures. Both exceptions retain false raw-body-readback and remote-SHA-observed flags. Their failed reads remain recorded. No lossless remote body or remote SHA-256 is invented for either fixture.

The 132 omitted originals remain 42 original tree-call envelopes, one redundant readback and 89 transport batches. The complete-envelope-reproduction claim remains false. Recorded hashes and carried-document pointers are checked by the admitted audit; the omitted originals are not silently counted as packaged copies.

All acceptance/export/full-module/JDK promotion flags remain false. Empty capability test lists and blocked mappings remain explicit. Source qualification still has the executed upstream 66/67 result with one error alongside the independent native 19 passes. A reference-audit PASS does not change that conjunction or grant source export.

The receiving execution review separately records 107 additive evidence paths observed after its freeze. It does not admit those paths as executed inputs. The final manifest review must account for all selected post-run evidence and any subsequent additions under their actual publication epoch.

In particular, the coordinating parent is preparing five historical donor-catalogue documents in a companion source PR and a receiving `donor-catalogue/` package with an actual component binding. Those documents and that future binding are **outside this executed reference audit and the 1,008-input freeze**. This report does not assert they were present at source `0c53…`, that they were tested by this execution, or that they are already published. Their exact source/document custody and receiving copies require a separate bounded final publication review. The four tested E4 owner files and the source code execution epoch remain the authorities recorded above.

Two reviewer-only metadata collectors stopped before producing a result: the first incorrectly required a mode field in proof-occurrence records that intentionally omit it; the second incorrectly assumed globally unique reference labels. The completed review uses the actual schemas, compares all 1,008 frozen modes and all 182 unique label/path pairs, and records both observations in `RECEIPT_RELATIONSHIPS.json`. Neither event was an owner checker/test failure, and no candidate, receipt or owner program was changed or rerun.

No checker, helper, build, test, recipe, validator, native command or remote operation was executed by this review. Final publication admission is deliberately separate, so the owner can include this sealed report without creating a manifest/report hash cycle.
