<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Historical receipt unit tests and installed-source scope

The unchanged baseline's owning metadata invocation runs 15 unit tests and fails three assertions. An independent fixture containing exact baseline Git files reproduced those failures. Two test-only canonical recipe outputs correct their expectations, producing 15 passing tests with zero failures or errors.

The filtered and object-state receipts seal historical postimages. Their tests must verify those immutable path/hash tuples, receipt hashes and successor composition; later evolution of the Java files does not rewrite history. The approved object-state successor also adds a second test row, which must remain in the expected result. The corrections retain fixed historical hashes, mismatched-preimage and duplicate-path refusals, malformed/missing/reordered/alias checks and all existing verifier code. No historical receipt or production source check changes. `METADATA_EVIDENCE.json` contains the baseline/candidate and actual target command receipts.

## Separate historical installed-source gate

The installed-source verifier retains a sealed 99-row inventory and four approved successor stages, composing 113 paths including 107 Java files. Against exact receiving baseline `677437be295b23f7ccf7409be287008944d0e8b0`, 18 declared source hashes differ. The baseline has 503 Java source files; 396 are unlisted and none of the declared Java files is missing. The first mismatch in gate order is `M3BitLane28.java`. `INSTALLED_SOURCE_LIMITATIONS.json` records every mismatch and unlisted path.

This was a read-only audit using the existing verifier functions and exact Git blobs. It did not run the installed-source verifier's main qualification or write its target tree. The two receipt-test corrections cannot make that historical installed-source job pass. Its hash, source-set, path, duplicate and alias checks remain strict. A current successor requires its own qualified recipe and target evidence; refreshing hashes alone is not admission. The full current Java/JNI owning gate qualifies this PR's module scope separately. Report the actual hosted status of each job and do not imply that local metadata success makes all CI green.
