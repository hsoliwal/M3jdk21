<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Physical-state selection and stable sorting: deferred private-directory candidate

**Decision: keep the incumbent production implementation.** A proposed reuse of the
unexposed selected `int[]` directory preserves the tested contract and reduces allocation,
but fails the resource admission rule recorded before experiments. Only this receipt,
two regression sources and two appended catalogue rows are admitted here. The original
86 catalogue rows and all production/native owners remain byte-identical.

The receiver is M3JDK21 `9441d86e6e0a699504ede20c0fd1df9a2febe033`.
The unchanged snapshot owner is SHA-256
`ff3d8802a77311de3e89094f34bb8b01df42e13cc74c96531b8801c7e08f5cc4`.
The **experimental, non-admitted** SDK owner is SHA-256
`b75ce17bae2e5a5174c1651586afa75b7d5bedb561d094439ad3f3f9e64d4b47`.
It reuses the first-party Apache-2.0 `filteredSorted` composition from Synexia
`74d5a8e17e130c65dc3b57fae8d23c7e243a2fdd`, donor blob
`eacc774655a75fe2bc884aa37fd5bfbc67ac5d5d`. No challenge/editorial source was copied.
The existing catalogue's GitHub, LeetCode, HackerRank and GeeksforGeeks evaluations and
all previous rejected alternatives are preserved.

## What was established

Actual Java 21/OpenRewrite 8.17.1 replay emitted the reviewed trial and two regression
files. Ten SDK tests covered exact guards, fixed point, all partial states, 16 shuffled
input-order/worker trials, drift/missing/duplicate/occupied inputs and cancellation.
Both runtime images passed strict Java/C compilation, generated-header JNI checks,
`-Xcheck:jni`, fatal UBSan and the three unchanged existing collection oracles.
The full targeted comparator tapes match byte for byte: Java 11,476,492 bytes and loaded
JNI 11,476,387 bytes. The same new proof passes the incumbent with 3,015,748 Java checks
and 3,015,746 loaded-JNI checks. These overlapping checks are not unique test counts.
All six public/protected API dumps, three JNI headers, both native libraries and their
exports match. Both new files also ran through actual JUnit/Surefire on incumbent and
experimental images in Java and loaded JNI, four successful forks.

## Why the candidate is deferred

The predeclared veto is a candidate CPU median regression above 10%, with regressions
above 10% in at least two of three paired fresh forks. The first complete host run vetoed
empty selection at length 0 (+23.31%) and all-selected length 8193 (+18.63%). A separately
recorded diagnostic rerun pinned all child forks to allowed CPU 0, warmed every one of
the same 14 shapes for at least 300ms of thread CPU and targeted 500ms measurements.
It still vetoed length 0/none (+24.51%, 3/3 pairs) and length 8193/none (+17.25%, 2/3).
Both complete runs and the initial diagnostic self-attach failure are retained.

Measured controlled allocation savings include approximately 4,160 bytes/operation at
1025/all and 32,832 bytes/operation at 8193/all. The warmed empty case saved zero bytes.
Retained result-directory lengths and payload sharing match. These allocation gains do
not override the CPU veto. Post-GC heap, RSS and NMT observations are bounded process
observations, not a retained-object census or mobile performance proof.

## Reproducible evidence and receiving boundary

Canonical evidence resides in `hsoliwal/com.synexia`, under
`synexia-openrewrite-recipes/recipes/m3-snapshot-sorted-selection-20261021`.
The immutable design was published at `e1fc3ce8f1a641e947dd57930cc744ff6de976f7`.
Use the selected `com.synexia.m3.SnapshotSortedSelectionEvidence20261021` descriptor for
this catalogue/test-only receiving packet. The similarly named optimization descriptor
is a deferred trial and must not be used for receiving production changes.

This is sealed PlainText source replay and a bounded six-owner host proof. It is not
Java-LST, full-module/reactor, installed-JDK/jtreg/JCK, phone VM or device/UI qualification.
The bounded JUnit leaf used 5.10.2; the receiver's 5.10.2-m3-source-1 module remains a
separate full-module gate. String phase and qualification-required status remain unchanged.
