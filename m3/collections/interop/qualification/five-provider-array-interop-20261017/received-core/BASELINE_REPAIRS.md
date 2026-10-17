# Baseline failures and scoped repairs

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

The final received tree uses master f3c615693c41bac1bf6ff49ba53e83f85425bca0. These repairs preserve strict compiler and regression gates. The core task receipt records six changed or added source paths: the count owner and its six-test class, two explicit constructors, and two corrected test-oracle files. It does not replace M3ObjectStateSlots28.

## Compiler failures on the frozen master

The isolated current-master diagnostic failed strict Java 21 compilation because the exported public classes M3ObjectSlotStore28 and M3SpanIndex had implicit public constructors. The compiler reported missing-explicit-ctor warnings, and the existing -Werror requirement correctly rejected them. Each source postimage adds only an explicit public empty constructor, preserving its previous construction behavior and all existing fields, methods and APIs. The exact command and warnings are retained in [the failure log](baseline/f3c61569-strict-compile-failure.log); before and after digests are retained in [the constructor repair receipt](baseline/current-master-constructor-repairs.json).

An earlier 4e29dc9d baseline had the same warning on M3ObjectStateSlots28. Frozen master f3c61569 already contains that owner's explicit constructor and newer matchingPositions and updateHandles APIs. Its current source is retained byte-for-byte; applying the earlier whole-file postimage would have lost those merged APIs. The earlier failed build remains in [historical diagnostic evidence](baseline/4e29dc9d-constructor-compile-failure.log), not as an outstanding current-master defect.

## Four incorrect existing assertions

The earlier full 193-test diagnostic exposed four failures in two test files whose preimage hashes also match the frozen master. The entire failed Maven log is retained in [the four-oracle failure log](baseline/4e29dc9d-four-oracle-failures.log). The received production collection behavior was not changed to satisfy these assertions.

| Test | Existing expectation and actual semantics | Retained regression correction |
| --- | --- | --- |
| M3LongTreeSetTest.growthSplitsPagesAndRemovalMergesAndTrimsThem | The input explicitly excludes zero, but the expected-value loop required zero between -21 and 21. | Construct the same operations in a JDK TreeSet and compare the complete result array, explicitly retaining the zero exclusion. |
| M3LongTreeSetTest.filterInPlaceCommitsPriorRejectionsAndKeepsTheThrowingSuffix | Ten sequential values with capacity four produce four pages through the owner's half-split rule; the assertion expected three before filtering even ran. | Correct the page-count expectation to four and retain the exception-identity and committed-prefix/suffix assertions. |
| M3LongTreeSetTest.sortedViewsAndUnionsWorkOverTheOwnerAsBackingSet | The backing set already contains seven, so adding seven through its subset returns false under both JDK and M3 semantics. | Assert false for existing seven, then assert true for absent eight, checking both existing-member and insertion behavior. |
| M3StoredRepresentativeContract.identityAndDuplicateMembers | ArrayList.remove(Object) removes the first equals-match. The hidden and visible members compare equal, so removing visible by value removes the earlier hidden member and leaves the actual visible reference. | Assert the exact surviving reference and continued view membership, then remove that reference by index and assert the resulting absence. |

The receipt [source-manifest.tsv](source-admission/source-manifest.tsv) retains exact before/after hashes. Final verification runs all 211 tests with zero failures, errors or skips. The stored-representative contract itself records 69 assertions in the retained verifier output.

## Existing source-admission gap

The frozen master had 115 Java files, nine native files and one resource outside the old effective receipt chain, plus eight Java digests that were stale. The new [master reconciliation receipt](source-admission/master-source-manifest.tsv) explicitly records this existing gap against exact commit f3c61569 and exact Git blobs; the adapters did not create it. Historical receipt and helper bytes remain pinned. The separate six-row task successor applies only after that reconciliation and retains before-hash checks.

The final effective census has 240 receipt rows, including 236 files under src (224 Java, 11 C and one resource) and four historical files outside src. The 34 admission tests exercise exact-source acceptance and refusal of changed, missing, duplicate, unknown, noncanonical and symlinked sources or receipts. These checks qualify custody of all 11 C files; the executed runtime-native gates specifically exercise the segmented and index-path-mask owners.
