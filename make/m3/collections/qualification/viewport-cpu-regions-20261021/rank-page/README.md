# M3JDK21 sparse extent and page-density review

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0

Canonical implementation/recipe: Synexia PR 12855, commit
663743b6ab686f8dcb41abefb55bfd9df2c7a8bc. Existing product PR 1759 remains a
review proposal. Runtime Java/C sources are NOT changed by this packet.

## Refined existing owners

M3SparseExtents.rowAt descends its existing Fenwick tree directly, comparing
reconstructed measured-row ends rather than nonmonotone signed deltas. Lookup
changes from O(log squared m) to O(log m) for m measured rows; no new fields,
cache, payload owner or public API. Zero-height plateaus and implicit gaps keep
the existing behavior.

Java projectPage recognizes a contiguous page inside sparse input and uses
overlap-safe arraycopy, avoiding alias scratch for that case. Native projectPage
uses the same page-local test to skip only redundant state reads. Complete input
validation, page-sized native output scratch, fixed input regions and one output
write remain. No native method or export is added in this pass.

## Exact review boundary

owner-refinement.patch is an inert delta against the preceding candidate (and
original extent owner), NOT a patch to stack directly onto product runtime files.
Its SHA-256 is in RECEIPT.json. The canonical nine-target dataset includes exact
original/postimage sources and the complete test. Existing Java/text snapshot
recipes and receive.py remain authoritative; no recipe engine is forked here.

RANK_PAGE_RECEIPT.json and RANK_PAGE_RAW_LOGS.json in the canonical dataset retain
103 raw log entries and source-bound observations. The final host verifier exited
zero. The independent extent oracle passed 2,192,618 checks per execution; the
expanded page oracle passed 19,330 Java and 38,659 JNI checks. Original/candidate
and preceding-page comparisons ran normally and interpreted, with strict lint,
Java21/C compilation, ABI checks, real -Xcheck:jni and native sanitizers.

The tree-read probe observed 395,284 -> 131,040 accesses for 4,096 queries over
65,535 measurements. A bounded 20,000-call contiguous alias-page JVM workload
observed 5,440,000 -> 0 thread-allocated bytes. Instrumented native page tests
passed 664,508 checks over 5,848 cases. These are specific observations, not
throughput, total-process memory or unrestricted equivalence claims.

## Pending acceptance

Actual Maven/OpenRewrite SDK replay, fixed-point/refusal and product receiving
remain NOT_RUN. Full provider/wrapper contracts, source-built JDK/HotSpot/jtreg,
SWT/Nebula/GEF/RWT, GPU/theme/language/IME/accessibility and cross-platform/resource
qualification remain pending. Do not advance the ordered receiving phase.
Original history and prior receipts remain; no merge or default-branch write.
