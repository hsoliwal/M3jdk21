<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 object-state masks receiver — Java21 array/index/offset/bit state

Canonical source/recipe/test authority: hsoliwal/com.synexia PR #12064, commit 505c323d3161c8954318f6c27ec163764e6aa847.

**Only two additive Java files:** M3ObjectStateSlots28 and M3ObjectStateSlots28SelfTest. Both are byte-identical to Maven/OpenRewrite SDK output templates, with source hash receipts here. Original M3Address28, M3IntLane28, M3BitLane28, M3SlotAllocator28 and M3IndexMaskCompactor owners are unchanged; original native C/JNI ABI and java.util remain unchanged.

The class delegates occupancy/recycled-generation handles to the existing allocator. Up to eight M3 application state flags are stored in one lazy byte per touched slot; there is no Object[] payload, retained Entry node, per-object state wrapper or additional slot allocator. Physical slot, input ordinal and output offset are intentionally separate. Invalid/stale handles are fully prevalidated before any stable handle compaction. All mutations are single-threaded; no native pointer/GC pin semantics are added. Requesting PIN_REQUEST is only a bit of application metadata.

The standalone test checks 4095/4096 boundaries, stale generation reuse, exact flags and bitCount, raw owner memory shape, O(1) state update, sparse scan, range bounds, ordered duplicates, no-mutation invalid mask/stale handle refusal and source->ordinal mask export. The seed corpus finished with 1,209,244 Java checks over 98,304 operations. This is a finite contract oracle, not all APIs or JMH. Existing M3StateMaskArrays, M3IndexMaskCompactor Java/JNI parity and their owner tests remain unchanged. The focused workflow compiles and exercises those existing Java/JNI peers with the new Java class, without adding a native ABI.

Algorithm catalogue in Synexia compares first-party owners, LeetCode #27/#283/#191/#303, HackerRank BitSet/Arraylist and GeeksforGeeks bit masking by category. No solution implementation copied. Apache-2.0 and M3 donor provenance retained.

**HOLD:** Full m3/collections reactor, installed JDK image/jtreg/JCK, all providers, all platforms, concurrent/GC contracts and JMH not admitted by local focused Maven proof. Keep reviewed source and runtime receipt separate from product release. Never rebase Synexia develop, no default-branch direct edits.
