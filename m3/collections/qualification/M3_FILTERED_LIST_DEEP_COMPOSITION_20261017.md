<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 filtered List — deep predicate composition receiving (2026-10-17)

**Convergence versus product:** Synexia and other private workspaces
are conceptual donor, category catalogue and reusable Maven/OpenRewrite
recipe homes. M3JDK21 is the public JDK runtime/product authority;
SWT separately owns its product/runtime. No private workspace becomes
a target java.base runtime service or library dependency.

Receiver parent: M3JDK21 #1032 at
`bea800fd8270389de387db88ca7d42c6c4b278fa`, class preimage
SHA-256 `94e3976440ee1cec2477e73d7a77caeda2523969d53a9407a03b8adbd5d765d9`. New class SHA-256 `5204f0a3eb29ce84107ba5fa40ee00943da277c99a37c8355bd1bc216ee074b5`.
Canonical Maven/OpenRewrite task:
`com.synexia.rewrite.M3Jdk21FilteredListStackSafe20261017`.
Private algorithm donor: Synexia-origin M3 CollectionViews first-party
Apache-2.0 code at M3JDK21 #1038,
`67feb628e55d886d4a82fd8bc4e8a3a63fe2c967`.
The #1038 *java.base* migration has an independent OpenJDK licensing
review gate; the reused private predicate-plan algorithm in this change
stays in the separate first-party Apache-2.0 `m3/collections` module.

No public factory/JDK List descriptors or backing field ownership
change. Replace only recursive predicate composition with immutable
linked FilterTerm nodes plus a lazily prepared ordered Predicate[] array.
O(1) nested composition, O(depth) cold metadata, O(depth) per value
with constant Java stack. This array caches predicate references,
never element values, membership outcomes or native addresses.
Preserve left-to-right short circuit and exception identity, null
handling, selected indexes, read-only/reversed/subList/live behavior,
unknown-size late-bound Spliterator, and backing failure semantics.
No JNI symbol or new native memory route.

All Java/metadata source preimages and postimages are exact
SHA-256 admitted via canonical Synexia Maven/OpenRewrite receipts.
Fail closed on unseen source or foreign target. Tests must reproduce
20k-depth original StackOverflowError at `-Xss256k` and prove corrected
strict Java21 execution, JUnit, SDK fixed point and refusal. Full
reactor, installed JDK jtreg/JNI/native and cold/warm CPU/retained-heap
proofs remain separate. The independent 605-case private-atom
probe was not a production-target compile or full JNI measurement.

Algorithm donor decisions are problem-category indexed: Synexia/JDK,
Guava/Apache/Eclipse design references, and LeetCode/HackerRank/
GeeksforGeeks as tests/taxonomy only; no third-party solutions copied.
Active M3JDK21 receiving phase remains STRING.
