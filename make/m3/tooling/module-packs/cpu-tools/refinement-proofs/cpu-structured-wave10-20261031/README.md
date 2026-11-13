<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 sorted integer sets — Wave 10 candidate receiving scope

The admitted Java API contains one immutable, privately copied signed int[] owner
and caller-owned mutable array results. Inputs are strictly increasing and
limited to 2,000,000 elements; exact union output above 2,000,000 refuses.
`M3SortedIntSetNative` is an **optional JNI leaf**. Without an explicit
absolute `-Dm3.cpu.sorted.native.path=/.../libm3sortedset.so` setting, it
throws IllegalStateException when called; ordinary Java `M3SortedIntSet`
operations neither load nor depend upon a native library. Native ABI is
scoped to `Java_jdk_m3_tools_cpu_M3SortedIntSetNative_intersection0`.
This file is a reviewed output template, not proof that a JMOD accepts the module,
that OS-specific loading is wired, or that runtime performance improves.
No global 91-category completeness or Maven/OpenRewrite execution is asserted.
