<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 CPU Wave23 — original signed half-open interval algebra

## Recipe-first upstream and exact receiver custody

Canonical first-party source owner: [Synexia #15316](https://github.com/hsoliwal/com.synexia/pull/15316)
`410447bffd288d49f91473a230254654fde65992`,
based on Synexia Wave22 #15301. Original Maven/OpenRewrite
`com.synexia.m3.CpuStructuredWave23_20261102` uses existing
`M3HashPinnedJavaSnapshotRecipe` and `M3HashPinnedTextSnapshotRecipe`.
Both original manifests, full source images and Maven/JUnit fixtures
are unchanged and retained in Synexia; no competing recipe engine.

Downstream source parent: [M3JDK21 #2306](https://github.com/hsoliwal/M3jdk21/pull/2306)
`4e38481b3793d1b8a6635188d6d54085d74fed4e`; its exact Wave20–22 scoped source/Java21/C17/JNI
qualification executed SUCCESS. The full JMOD/jtreg/HotSpot
product gate remains separate and HOLD.

Eight intended target paths were genuinely ABSENT on that parent.
Each original source was read from the canonical first-party Maven
resource and independently SHA256-checked against the declared
after-image, then Git blob-checked. SOURCE_RECEIPT.tsv records
the exact destination, ABSENT preimage, original Git blob and
full 64-hex SHA-256. Do not infer equivalence from path names.

## Actual additive source contracts

`M3IntIntervalAlgebra` consumes the unchanged `M3IntIntervalSet`
immutable signed half-open int[] normalized endpoints. `union`,
`intersection`, `difference` and `complementWithin` perform
two-pass primitive endpoint sweeps. No boxed entry/nodes, retaining
caller backing arrays or changes to the original interval owner.
All results are new immutable original `M3IntIntervalSet` instances.
Adjacent intervals merge, empty half-open intersections vanish,
MAX_PAIRS overflow refuses, and reversed/invalid queries fail closed.

`M3IntIntervalAlgebraNative` is an optional JNI intersection
facade requiring an **absolute** `m3.cpu.interval.algebra.native.path`
property; absence refuses with IllegalStateException.
New original C17 `m3_interval_algebra.c` JNI export
`Java_com_m3_tools_cpu_M3IntIntervalAlgebraNative_intersect0`
produces a detached primitive jint[]; source borrowed lanes
are released `JNI_ABORT`, then the Java owner revalidates
its result using `M3IntIntervalSet.copyOf`.
Prior Wave20/21/22 public Java methods, JNI exports,
module-info.java, builder and historical source remain unchanged.

Three independent original Java/JNI oracles, one C17 kernel
oracle and the complete 91-row Wave23 category/status catalogue
are source-exact. All fourteen Wave22 columns remain identical
before three new Wave23 columns. No new platform-specific
library is packaged automatically; JNI is an explicit test sidecar.

## Category-first comparison and license boundary

LeetCode #986 interval list intersection and #56 merging use
closed/inclusive point intervals. M3 uses **signed half-open**
`[start,end)`: touching endpoints are empty intersection.
HackerRank Gridland Metro uses 1-indexed inclusive segments;
GeeksforGeeks sort/merge describes a problem shape only.
RoaringBitmap has distinct bitmap membership semantics.
All are REFERENCE_ONLY: no submissions, editorials or external
library source copied. Actual code donor is first-party Synexia.

## Evidence order and HOLD

Inventory/source SHA -> documentation -> canonical Maven recipe
-> exact sequential Java receiver -> exact C JNI receiver
-> test/catalogue receivers -> patch/static + strict javac/C17
-> original Java and C differential under JIT/-Xint and
`-Xcheck:jni` -> actual Maven SDK two-pass/refusal
-> complete JMOD/jtreg/native packaging and platform ABI.

Never claim source-only receipts prove runtime. Keep DRAFT/HOLD
until every relevant compiled and runtime job executes successfully.
Do not rewrite/rebase protected develop, original JNI exports or
other hsoliwal/* repositories automatically.
