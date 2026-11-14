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
- 102fa70995471fefdd18edb6a58c6a85cf80fdb8 m3/collections/src/main/java/jdk/m3/collections/M3FilteredViews.java
- dacd36880a3c5a116f191eca8191fdf595cf3252 m3/collections/src/main/java/jdk/m3/collections/M3TreeMap.java
- 9060424b921bac10dc382ba277a3aa840ea59529 m3/collections/src/main/java/jdk/m3/collections/M3IndexMaskCompactor.java
- 5afeb41575e758b06882b3e3d8104b0c275f50ca m3/collections/src/main/native/collections/m3_index_mask_compactor.c

Product postimages (SHA-256 from exact canonical recipe manifest):
- 93d39c6f5544ed56a8c18fb965073da738fb9e5c203503f34bd06fb9a5460ad0 m3/collections/src/main/java/jdk/m3/collections/M3FilteredViews.java
- d7fc17832b15924ca291ca2b5e797c7c450d2fbb11a6a7234e7e673e73b3d732 m3/collections/src/test/java/jdk/m3/collections/M3FilteredTreeSlotScanProof.java
- d2a2be337899fdbe0cd0a8f65c53744aa8014efdbaac91e92ba1cc311fea402d m3/collections/src/test/java/jdk/m3/collections/M3FilteredTreeSlotScanTest.java
- d73f1e3cb0adc1ecd1857e8196dc3efd4c8e0a8a1a0bbb8f38fa59cbac6d9e34 m3/collections/src/main/java/jdk/m3/collections/M3TreeMap.java
- f65466dbaa66bbf45f52c639889ef86b6135faf01fb021f6705d116d1fd38fab m3/collections/src/test/java/jdk/m3/collections/M3EntrylessSortedBulkProof.java
- ece540cfcff50926665a79cd91d18a011299e48086eda37c1e7570c3fc180f7c m3/collections/src/test/java/jdk/m3/collections/M3EntrylessSortedBulkTest.java
- 01e147accf577784c97f2f6e3d3ed5415caf3619c4484f6d69450fd10a6e1570 m3/collections/src/main/java/jdk/m3/collections/M3IndexMaskCompactor.java
- 0df42d8b04243d236990d6b73a289ae380192c6d3dc6d34d8a73a291a47473e6 m3/collections/src/test/java/jdk/m3/collections/M3IndexMaskPositionPageProof.java
- 55864df5ad0f73561c6c00a1be630a5637c0fd9c9bf61d936a040f6898cb7d68 m3/collections/src/test/java/jdk/m3/collections/M3IndexMaskPositionPageTest.java
- 3c09398535aaa678714275a2ab7fd4f5a2e611cd150bdc5842da24b803f3374a m3/collections/src/main/native/collections/m3_index_mask_compactor.c

Algorithm donors: LeetCode/HackerRank/GeeksforGeeks by problem category only;
pinned Apache-2.0 fastutil and HPPC designs; EPL/EDL Eclipse Collections
contract-oracle references only. No challenge code copied.
