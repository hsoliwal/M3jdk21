<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 strict filtered-List depth refinement — recipient qualification

Canonical reusable source recipe: Synexia
`com.synexia.rewrite.M3Jdk21FilteredReadonlyDepth20261017`.
Source owner/parent: M3JDK21 #1045 exact commit
`7e8c646ea27e439a15377d1b332e6d08aa2b9c48`.
Old source SHA-256: `5204f0a3eb29ce84107ba5fa40ee00943da277c99a37c8355bd1bc216ee074b5`.
Reviewed new source SHA-256: `b2dc49d3a9ec43b6e6bd34cf5c6dac53057cf90f42cc9ed506d54822a760fc2d`.
Existing original donor: Synexia #11977/#12012, Apache-2.0,
Copyright 2026 Hitesh Soliwal and contributors. All public
JDK/third-party source retains its own license. This recipient
continues the separately built `m3/collections` module, not
OpenJDK java.util promotion or an SWT contribution.

**Contract:** `readOnlyFiltered(readOnlyFiltered(...))` must stay
stack-safe. Unwrap only the internal `StrictReadOnlyList` when its
delegate is exactly the same utility's `View`. This reuses the
already qualified `FilterChain` private predicate directory.
NEVER unwrap in writable `filtered(List,...)`; doing so could
bypass the read-only owner's UnsupportedOperationException boundary.
Do not automatically unwrap JDK unmodifiable, reversed or arbitrary
foreign List implementations. Public signatures/descriptors and
observable selected order/exception/last-delivered iterator mutation
remain unchanged.

A 20,000-level read-only nesting oracle must reproduce old
StackOverflowError at `-Xss256k` and pass with the new factory
under Java21 default and interpreted modes. Test source edits before
or after binding, zero eager predicate calls, 128-step predicate
short circuit and exact user-thrown exception identities,
null/duplicates, comparator-sorted backing, reversed/subList,
strict empty/no-op/bidirectional iterator mutation refusal and
protected source write rejection. No retained per-element storage.
All existing native/JNI symbol and Java predicate callback boundaries
remain unchanged; primitive JNI may be tested as inherited regression
but cannot evaluate arbitrary Java predicates.

A byte-identical recipe template and exact mapping/doc SHA-256
do not constitute completed Maven/OpenRewrite SDK execution. Target
review requires real SDK fixed point/refusal, module Java21/JUnit,
public ABI comparison, installed JDK / jtreg / inherited JNI checks,
plus realistic CPU/retained heap/native memory measurements.
No phase advancement from STRING has occurred.

Problem category ledger in canonical Synexia recipe records internal
algorithm reuse, JDK/Commons/Guava/Eclipse contract comparison, and
LeetCode/HackerRank/GeeksforGeeks adversarial taxonomy (no source copied).
