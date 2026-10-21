<!-- SPDX-License-Identifier: Apache-2.0 -->
# Source-sealed M3JDK21 bounded navigation receiver (2026-10-18)

Canonical converged recipe: [Synexia PR #12235](https://github.com/hsoliwal/com.synexia/pull/12235),
exact source commit `a5766cf91e6b6434950b2273eb8666d25d3bec20`. Target predecessor: public M3JDK21 `master@0d73745d7203123533dce8f8fb264ba2746f6d2d`.

The **first receiving commit** contains the byte-identical canonical recipe YAML, sealed
manifest, **both full current-master preimages**, **all four full postimage templates**,
algorithm-problem catalogue and Java21 CI declaration. This is a mirror of recipe DATA;
the reusable Java/OpenRewrite execution engine remains canonically owned in Synexia.

The **second receiving commit** installs each of the four exact postimages as target
Java source/test files, serially by file. The generated Java must byte-match the
canonical postimage templates and its manifest SHA-256. No target author hand-editing.

Changed behavioral atoms: read-only filtered NavigableMap out-of-range navigation
and mutable filtered NavigableSet out-of-range navigation, restoring the admitted
bounded-comparator recovery from earlier merged PR #1159. Preserve the independently
improved read-only unmodifiableMap boundary and bulk-key/value input capture.
The two adversarial JDK21 comparator tests are first-party originals from #1159.

No public API signature, Java/JNI C ABI, native callback, payload/storage owner,
JDK java.base, HotSpot, upstream SDK interface, or Synexia runtime dependency changes.
Pending admission: exact-head Maven/OpenRewrite at Synexia; target strict Java21 and
Maven/JUnit; installed JDK/JTReg/JCK; native ABI parity; platform/GC/JMM/throughput gates.
CI not assumed PASS on commit/push.
