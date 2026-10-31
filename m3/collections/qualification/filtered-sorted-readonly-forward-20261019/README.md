<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 forward sorted/filtered Java 21 strict read-only receiver

This exact-source candidate belongs to **M3JDK21, one integrated Java21/HotSpot/JNI SDK**. Reusable Maven/OpenRewrite recipes remain canonical in Synexia; no Synexia runtime jar, new JDK compatibility layer or alternative per-entry store is introduced.

The earlier redundant override repair packet based on Git blob `34fca25ba45467567213ed7a170bf4b27d6427e3` was independently superseded by SDK master. Those stale review PRs were **closed**, not applied over current source. This replacement starts from the newer exact source and preserves all newer modifications.

| Receipt | Value |
| --- | --- |
| SDK file owner | `m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java` |
| Canonical recipe | `com.synexia.m3.M3FilteredSortedReadOnlyForward20261019` in `hsoliwal/com.synexia@6139f93bf62ef249856dfeca98045e145b808776` |
| Input Git blob | `1a31827cd6274b5ad8eb62f3a5ce0e5e0667478f` |
| Input SHA-256 | `8419315c3058bce6add897b7ffa7c6763bbb6b85418bb35e190ba8a4d2dd70c4` |
| Generated output Git blob | `32dac5d7a73826ed79d59b27d83a6bab055f17c2` |
| Generated output SHA-256 | `af1752c4b02eb7a7753424727e58b7c9cd38c3e4c28191ce7499c12d54be7c68` |
| Local Java21 witness | PASS: JDK21 empty method defaults differ from strict read-only contract; repaired model passes with -Xlint:all -Werror |
| SDK JUnit | ADDED / NOT EXECUTED |

Original `M3FilteredViews.SortedSetView` now explicitly refuses `removeFirst/removeLast` even on empty filtered sources without running predicates, and supplies a strictly read-only **backed** `reversed()` view by reusing its existing source/predicate model. `NavigableSetView.reversed()` remains covariant through its existing `descendingSet()`. The JVM and native selectors, source item storage, private helper owners, null/comparator semantics and all other public interfaces remain unchanged.

**No JNI/C file changed:** arbitrary Java predicate/comparator callbacks cannot be moved into M3IndexPathMasks' checked JNI without different contracts. Existing JNI functionality remains within M3JDK21; separate primitive mask algorithm work may continue after Java oracle qualification.

Full Maven/OpenRewrite, target module/reflective API and JUnit, source-built JDK21, jtreg/HotSpot/JNI, installed SDK tests, five provider families (JDK, Guava, Eclipse Collections, Apache Commons Collections, Spring Framework), JMH CPU/heap/native memory **NOT EXECUTED**. No installed-product or phase completion claim. String -> arrays -> collections remains the acceptance order.

Problem category / algorithm / donor comparisons: `m3/collections/algorithm-catalog/filtered-sorted-readonly-forward-20261019.tsv`. Continue deterministic serial per-file recipe application; reject any source drift.
