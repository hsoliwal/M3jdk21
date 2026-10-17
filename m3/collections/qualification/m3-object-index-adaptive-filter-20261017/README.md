<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 object-index adaptive filtered-snapshot receiver

**Source-first canonical Synexia:** [PR #12158](https://github.com/hsoliwal/com.synexia/pull/12158),
exact `7a2a41aae5453be6959b37faa676f19ed5f2645c`.
Named Maven/OpenRewrite recipe `com.synexia.m3.ObjectIndexAdaptiveFilter20261017`;
provider `M3HashPinnedJavaSnapshotRecipe` unchanged.
**Parent:** M3JDK21 [PR #1095](https://github.com/hsoliwal/M3jdk21/pull/1095)
at `1753cced8048590d39616006b25aef33170256e3`.

Only one existing production file is modified:
`M3ObjectIndexSnapshot.filtered(Predicate)` changes BitSet + exact int[]
into an allocation-deferred adaptive primitive index directory.
It builds retained physical slots only after the first rejected candidate,
using existing `M3SlotArrays` growth; no new object/payload/state/native owner.
Predicate evaluation stays once per logical row; reference identity,
null handling, stable selected order, original physical indices, byte-state
flags, offset slices, immutable descriptor and sorted metadata are preserved.
Public API descriptors are unchanged. Original BitSet was a transient local,
not versioned reusable state; byte-mask JNI state filters are untouched.
Trade-offs: dense subsets may resize/copy; resource/OOME timing changes;
no cross-thread or production GC performance claim.

Target test `M3ObjectIndexAdaptiveFilterTest` is ABSENT-before; both product
postimage and test are byte-identical to canonical source-sealed templates.

Reproduction on Linux Java21 and C17 GCC from the PR head:
```
bash m3/collections/qualification/m3-object-index-adaptive-filter-20261017/verify.sh /absolute/new-proof
```
The script checks all three source seals, strict Java21 compile of actual old
and new production owners with the same dependency closure, public JVM
descriptors, inherited and new Java behavior in four HotSpot modes and
unchanged C17/JNI libraries under `-Xcheck:jni`. It compares old/new outputs.
The canonical Synexia head has the actual Maven/OpenRewrite SDK/JUnit workflow.

**Actually executed locally** (standalone model only): Java21 strict
`--release 21 -Xlint:all -Werror`; 528419 checks in EACH of default,
interpreter, C1 and C2. ThreadMXBean at n=1048576:
all selected BitSet 131112B vs adaptive 0B; none selected 131128B vs 0B;
one selected last 131136B vs 72B. This is NOT full M3JDK, JMH, GC or JNI.
The source/product test workflow and full Maven/reactor/JDK release gates
are unverified until their actual completed receipts. Keep DRAFT. String
predecessor phase remains active; Collections still WAITING_FOR_STRING.
