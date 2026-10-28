<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 is the JDK: direct java.base mask-compaction refinement

Authoritative reusable Synexia Maven/OpenRewrite recipe, stacked on [M3JDK21 #1286](https://github.com/hsoliwal/M3jdk21/pull/1286). This changes ONLY the existing `jdk.internal.mindex.M3CollectionMaskCompactor` inside `java.base`, which `java.util.ArrayList` and `java.util.Vector` already call. No M3-plus-JDK adapter, new public JDK name, retained Entry node, extra Object[] payload or JNI object-pointer crossing.

The named source-sealed `M3ArrayMaskSparseRecipe` extends the original `M3GpuToolJoinRecipe`/manifest scanning engine. Exact original SHA-256: `6d26934bab87d6bc68a0c38f0c8ceeec72dc5817e5fca12cfafbc276bafb133d`; exact generated SHA-256: `050f051ebf615ed27a8089fa0874a398029fbfeabc25d4a310af1e1b2a0261b8`; JDK source remains GPLv2 with Classpath Exception, with original copyright and modification header preserved.

Selective algorithm: keep scalar compact for small spans and medium-density masks; preserve the incumbent contiguous-deletion-run path; admit 64-bit word-local System.arraycopy copying only when deletion density is <=12.5% or >=98.4375%. Direct payload compaction runs only *after* the original JDK predicate/callback/CME phase. ArrayList, Vector and subList own synchronization, modCount, exceptions, iterator fail-fast and tail zeroing.

Local selected Java21 patch-module `javac -Xlint:all -Werror`: PASS; 1,954,952 deterministic differential/identity/offset checks comparing the incumbent to candidate: PASS. Cold/warm synthetic timing is provisional, workload/CPU dependent, not a universal performance win. Maven/OpenRewrite scheduler, full modified JDK image, JCK/jtreg/GC/JMH and exact hosted target CI remain PENDING; no String->arrays->collections stage promotion.

Donor/category ledger covers LeetCode Remove Element, HackerRank Bit Manipulation and GeeksforGeeks bitset/window families as **reference only**. No external editorial source copied. Synexia recipe/tests Apache-2.0, JDK product file GPL2+Classpath unchanged.

Canonical Synexia PR: https://github.com/hsoliwal/com.synexia/pull/12367 . Product receiver stacked on #1286; applies directly to java.base.
