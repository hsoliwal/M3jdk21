Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0

The Unicode-order approach is adapted from Apache Lucene CharsRef.UTF16SortedAsUTF8Comparator, apache/lucene-solr@17dee71932c683e345508113523e764c3e4c80fa (release8.11.2), source blob3c2a62d17b9ebb96045eebff7db1e0de75574ec0. The original ASF header, complete source, LICENSE and NOTICE are retained under donor/. The extracted comparator body is compiled verbatim in a qualification-only adapter; its FutureArrays.mismatch binding delegates to standard Java21 Arrays.mismatch. It supplies a valid-Unicode sign oracle, not a malformed-string oracle.

The production code uses standard String.codePointAt/scalar order and retains the entire incumbent byte-comparison fallback. It adds no Lucene runtime dependency or new array/storage/cache owner. Existing Hitesh Soliwal and Apache-2.0 source notices remain intact.

Canonical Maven/OpenRewrite engine sources are retained with their existing Apache-2.0 notices. Exact engine pre/post images bind the one-owner registry admission; no arbitrary source root is admitted. The two incumbent M3 test programs are reused unchanged.

LeetCode, HackerRank and GeeksforGeeks are discovery/category sources only; no implementation from those sites is copied or assigned a license here.
