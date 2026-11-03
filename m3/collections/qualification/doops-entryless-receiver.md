<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
# DOOPS entryless Java/JNI receiving qualification — 2026-11-02

**STATE: DRAFT_HOLD — not approved for merging.**

Baseline: hsoliwal/M3jdk21 `ce005d25b29fb6246e013bffb462afd5ed070362`.
Canonical private recipe owner: hsoliwal/com.synexia,
branch `m3/doops-entryless-audit-20261102`, commit
`5f79091f3a6cb2b9160e9ea3c70d406ce50891b3`, PR #15359.

This receiving branch contains only exact reviewed Java/C17 postimages, product
test sources and passive hash/provenance receipts. The reusable Maven/OpenRewrite
implementations and challenge-category donor evaluations stay in canonical Synexia.

Selected recipe families:
- `m3-doops-filtered-tree-slot-scan-20261102`: read-only filtered M3TreeMap
  internal size/isEmpty direct key/value slot traversal.
- `m3-doops-tree-bulk-slots-20261102`: one owner-specific primitive slot cursor
  instead of a new public Map.Entry facade per mapping in sorted bulk transfer;
  preserves source cursor creation before transient allocation.
- `m3-doops-index-position-page-20261102`: caller-owned primitive result
  buffers, Java21/C17 JNI, versioned native capability probing.
- `m3-doops-index-position-page-native-20261102`: existing native C17
  compactor owner, no competing new allocator or JNI library.

Contracts remain authoritative: public Map.Entry/entrySet, comparator callbacks,
lazy values, fail-fast behavior, ordering, serialization, JNI ABI and GC ownership.
Existing methods are additive or unchanged. Unknown SortedMap implementations
retain their original entry iterator behavior.

**Acceptance status:** isolated source models were compiled/tested in canonical
development, but this exact M3JDK21 build, real Maven/OpenRewrite SDK/fixed-point
run, product JUnit, JNI/native packaging, platform/GC/serialization/concurrency
differentials and full hsoliwal/* fleet inventory are PENDING/NOT_RUN.
Copying hash-sealed templates is NOT execution of the canonical recipe and
does NOT certify production behavior. No merge without those receipts.

Source baseline Git blobs:
- 102fa70995471fefdd18edb6a58c6a85cf80fdb8 m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java
- dacd36880a3c5a116f191eca8191fdf595cf3252 m3/collections/src/main/java/com/m3/collections/M3TreeMap.java
- 9060424b921bac10dc382ba277a3aa840ea59529 m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java
- 5afeb41575e758b06882b3e3d8104b0c275f50ca m3/collections/src/main/native/collections/m3_index_mask_compactor.c

Product postimages (SHA-256 from exact canonical recipe manifest):
- 4aa120f4cd52b7857649e7b801294394dc0c61096ee2910f05a323beb07a5795 m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java
- d19caa06054877073f330c92c2b0a4f7a73db265df67878d6cbb99f3b98a5496 m3/collections/src/test/java/com/m3/collections/M3FilteredTreeSlotScanProof.java
- ed586c3bbe20b4d8656b7b0bc7a785b7c85ac333af56c7c063dfe012ff9c99dc m3/collections/src/test/java/com/m3/collections/M3FilteredTreeSlotScanTest.java
- 6261c26ea7f3a36be0497aad828b40f6c6d63954c7cd55b68dcc54bd1a7a1b1a m3/collections/src/main/java/com/m3/collections/M3TreeMap.java
- 266d35352a3762934f46d85fffbf2035f8586e887722c2a873084a03206546fa m3/collections/src/test/java/com/m3/collections/M3EntrylessSortedBulkProof.java
- 1db2a99125ea8e872fa9b7b9c87e31fd1556402ab50f8bb81e7ca7dde87baef3 m3/collections/src/test/java/com/m3/collections/M3EntrylessSortedBulkTest.java
- 01e147accf577784c97f2f6e3d3ed5415caf3619c4484f6d69450fd10a6e1570 m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java
- c99c7d00bb8343582d1144e607db180f0d64318ebd5b2d17419409237b597fbf m3/collections/src/test/java/com/m3/collections/M3IndexMaskPositionPageProof.java
- f890ad50c823d0129050dbafb15b8e3eced69c8ba1448178bd343a78f284b907 m3/collections/src/test/java/com/m3/collections/M3IndexMaskPositionPageTest.java
- 3c09398535aaa678714275a2ab7fd4f5a2e611cd150bdc5842da24b803f3374a m3/collections/src/main/native/collections/m3_index_mask_compactor.c

Algorithm donors: LeetCode/HackerRank/GeeksforGeeks by problem category only;
pinned Apache-2.0 fastutil and HPPC designs; EPL/EDL Eclipse Collections
contract-oracle references only. No challenge code copied.
