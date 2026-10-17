# M3 sorted prepared view — adaptive index directory

SPDX-License-Identifier: Apache-2.0

This target receiver is stacked on M3JDK21 #1116 exact head
`9a6621589c1974d9e8c9bdbf64770178ce80169f`.
Canonical source-first [Synexia PR #12131](https://github.com/hsoliwal/com.synexia/pull/12131),
commit `5b33c06d39c9fcbd7cbe3667074ce16661fada38`, name
`com.synexia.m3.SortedViewAdaptiveArray20261017`.

The only changing production file is `M3SortedView.java`, specifically the
private `select` atom. Existing `M3SlotArrays.grow` remains the sole growth
kernel. Public methods, comparator, revision, index owners, Java.util interfaces,
JNI exports, and concurrent/GC contracts remain untouched.

Previous code allocated `int[count]` even if the filter accepted zero or one
row. Adaptive primitive growth defers storage until a selected row actually
exists. Predicates remain called once each in input order; check/cancel
boundaries, stable tie behavior, and exception propagation are unchanged.
Memory is O(k) for k matches instead of O(n) upfront, with variable growth
and a final exact-length copy. Dense workloads can perform more copying.
No universal speedup or lower GC pause claim.

Qualification driver (new absolute output directory):
```sh
bash m3/collections/qualification/m3-sorted-view-adaptive-array-20261017/verify.sh /absolute/new-output
```
It verifies before/after source seals, compiles old/new owner with Java21
strict flags, checks public javap descriptors, runs the real target Java
contract in default, interpreted, C1 and C2 modes. Scoped Java21 source,
not a complete installed JVM image. Previous local atom-model differential:
5,224,315 checks per normal/-Xint mode. At 1,048,576 rows the model showed
zero-selected allocated bytes 4,200,496 original vs 6,296 adaptive; source
bench/JMH still PENDING. JNI backend is unmodified, not newly qualified here.

Do not merge/promote ahead of String/array receiving gates. Actual
Maven/OpenRewrite SDK, full reactor/JDK, native and JCK gates await real proof.
