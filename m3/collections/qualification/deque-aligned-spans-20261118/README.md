<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# M3JDK21 original generic deque: aligned ring-span receiver

**M3JDK21 is the integrated Java21/HotSpot/JNI SDK.** The runtime authority is its original `jdk.m3.collections.M3ArrayDeque` Java source, not an M3-on-JDK layer, external collection adapter, copied donor class, or independent native heap.

## Exact source custody
- Product master parent `919058ad9c412e4ef799e167e58ef81e479fca33`; original source Git blob `4bef1d278e60f587488d12a7215c0ab0c39e6321`.
- Canonical Synexia Maven/OpenRewrite source-sealed recipe: [PR #18800](https://github.com/hsoliwal/com.synexia/pull/18800), commit `1c43394aedac92127b0db55ba11d468d09a34ee3`; named `com.synexia.m3.DequeAlignedSpans20261118`.
- Complete target preimage SHA-256 `353b9c0e130f09915c5c40b974a632cb64389549ff7b6dfb98caf3c8a07b7464`.
- Complete source postimage SHA-256 `250e797ecf6d8f677cc7aa1c45d636abaec57b657f74735a95b3e9c9c8d8ca08`.
- New contract SHA-256 `81991bc844d65f14c25acc6c4924e8f90e913cdb31d1e54ef9138435b63b8e3a` (ABSENT before).
- New JUnit SHA-256 `77eef48bdf316f83457e18ccf7dcd01a54091bc2bd21b96e2a8ad8457b83314b` (ABSENT before).

## Implementation and donor
- Original `erase(int)`: use shorter-front/back circular span shifts with `System.arraycopy` for **both** the owning Object[] and aligned byte[] state mask lane; one wrapped bridge, no extra entries/arrays, no GC-unsafe JNI references.
- Original `clear()`: up to two physical span zeroing operations for each lane; preserve all cold slots, zero only live coordinates, same empty/no-op and nonempty revision behavior.
- Reuses first-party already merged M3JDK21 `LongArrayDeque` PR #2985 algorithm, adapted for the generic deque's arbitrary (non-power-of-two) capacity and aligned state flags.
- Public/protected facade, indexed mask scan, iterator/reflection, serialization, equality callback order, JNI symbols and original licensing are unchanged.
- LeetCode, HackerRank and GeeksForGeeks used as **problem category** sources (circular buffer shift, interval algebra, bit masks), not copied solutions. Full catalogue and donor rights decision is retained in Synexia.

## Actual evidence / remaining gates
- Independently compiled Java21 `RingSpanOracle` (available with local handoff), original scalar vs candidate arrays: **187,080 exact entire-physical-ring cases passed in JIT**, **187,080 in `-Xint`**; includes identical per-slot object identity, byte masks, head/size/revision, full/empty, wrapped/nonwrapped, cold slots, larger random rings.
- **NOT EXECUTED**: new product JUnit against installed M3JDK21, canonical Maven/OpenRewrite recipe JUnit, full make/jtreg/JCK, JNI native backend parity, Eclipse/Tycho/OSGi/SWT/JFace/Windows/macOS/ARM/Android/iOS matrix or performance measurements. Keep draft and do not claim full Eclipse Platform/SDK release qualification.
- Existing API and original tests remain mandatory; the new test never invokes the JNI object callbacks and only uses existing JAVA indexed-selection backend.
