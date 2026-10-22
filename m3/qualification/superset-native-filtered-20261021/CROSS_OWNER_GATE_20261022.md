<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# 22 October 2026 — direct M3JDK21 Java/libjava and filtered/sorted verification

**M3JDK21 is the integrated JDK product**, not a Synexia adapter layer.
This verification branch is stacked on the existing original-owner Java/C
receiving [M3JDK21 PR #1797](https://github.com/hsoliwal/M3jdk21/pull/1797),
which is itself stacked on the M3File receiving PR #1782.
The new increment does **not** modify or clone its seven production Java/C
owners, nor does it introduce a collection payload, Entry/Node object store,
public JDK name, interface, package, HotSpot/JNI symbol or runtime dependency.

## Exact original owner inventory

Previously qualified first-party sources are retained from:
M3JDK21 #1760 (dynamic primitive index positions, array offsets,
stable sorted merges), #1761 (original `jdk.internal.m3.M3IndexMaskWindow`
and `src/java.base/share/native/libjava/M3IndexMaskWindow.c`), and
#1763 (original filtered/sorted backed collection owners).
Their canonical Synexia recipe custody is #12857, #12856 and #12860,
respectively. This branch reuses their exact source/receipt inventory
`RECEIVED_DONORS.tsv` and its read-only `verify_received_blobs.py`.
No external challenge/source body is imported.

The new Java test
`test/jdk/jdk/internal/m3/M3MaskCollectionViewIntegrationTest.java`
has Git blob `a5a7d4508513b829d1ac14b9562aa108998a0e3f`.
The new Linux host proof
`verify-integrated-java-jni.sh` has Git blob
`5da0a5f54f5830ad6cfd97b4b3d5adb09d6f92a4`.
Both are exact first-party Synexia Maven/OpenRewrite outputs
with SHA-256-pinned ABSENT-before receipts in
`com.synexia.m3.M3Jdk21NativeFilteredIntegration20261022`.
The GitHub branch independently read both product outputs back to
verify byte-exact canonical ownership.

## Executable cross-owner behavior

The source-native test uses only the existing Java 21 array-index and
byte-mask machinery: primitive `int[]` physical IDs (including duplicate
values), `byte[]` states and caller-owned bounded primitive output.
An `AbstractList` presents logical ordinals through Java Collection
API projection only; no retained item object or copied authoritative backing
is introduced.

The original `java.util.CollectionViews` live predicate and
`NavigableSet` sorted/descending wrappers are compared against a separate
bit-by-bit 8-bit mask oracle and the **original** JDK-owned Java/JNI
`M3IndexMaskWindow.scanBounded` and `countBounded` operations.
The fixture verifies counted matches, occurrence order, stable sorted
tie offsets, explicit 19-row examination budgets, 13-entry output windows,
cursor progress, canary safety, live source-mask changes and detached
prior result arrays. There is no Java callback or Java object transfer through
JNI.

The Linux script first refuses changed Java/C and test blob identities,
compiles the actual M3JDK21 `java.base` Java owners using Java 21
`--patch-module`, generates JNI headers with `javac -h`, compiles
the original `libjava` C with C17 and strict warnings, and executes
`-Xcheck:jni` actual native/Java comparisons. It also runs the
existing original M3MaskCountTest, M3MaskPreparedPredicateTest,
BootstrapViewsTest and CollectionViewsContract. It is an isolated
host-JDK module probe, **not** a complete matched OpenJDK image build.

Run on Linux with a full JDK 21 and C17 compiler:

```sh
bash m3/qualification/superset-native-filtered-20261021/verify-integrated-java-jni.sh
```

## Donor/category decisions and admission

The existing searchable
`ALGORITHM_CATALOGUE.tsv` is extended for a
`JAVA_BASE_FILTERED_JNI_PARITY` cross-owner contract category.
LeetCode 27/704, HackerRank Dynamic Array/Frequency Queries and
GeeksforGeeks bitset/stable sort examples are **problem categories
only**, not copied solutions. The actual runtime is owned by
M3JDK21 and reuses reviewed first-party original Java/C implementations.

**PASS:** source/seal inventory of original donor files; exact added
Java/shell Git-blob readback; shell syntax locally checked.

**NOT_RUN:** target PR CI running the host Java21 C17/JNI integration,
existing original filtered/sorted contracts and canonical Synexia
OpenRewrite fixed-point/refusal Maven JUnit. Do not claim green from a
queued/no-job workflow.

**NOT_RUN:** full source-built M3JDK21 installed image, HotSpot/GC/JNI
lifetime/platform matrix, jtreg/JCK, five-library provider family
regression, cold/warm CPU and retained heap/native/process memory.
A finite host proof or original donor PASS never authorizes automatic
backend promotion or advancing String → arrays → collections phases.

Keep this draft stacked on PR #1797 pending explicit test and parent
dependency gates; never rebase/force-push Synexia develop, or overwrite
newer corrected CollectionViews atoms from other reviewed PR stacks.

## Additional qualified scalar array-mask atom

This stacked receiver now also incorporates the previously qualified **actual
java.base M3CollectionMaskCompactor** scalar behavior atom from M3JDK21
[#1754](https://github.com/hsoliwal/M3jdk21/pull/1754) and the
canonical Synexia [#12851](https://github.com/hsoliwal/com.synexia/pull/12851).

The original `jdk.internal.mindex.M3CollectionMaskCompactor.java` Git blob
`51ea3a72bb191afc250a58be964363bd5680467c` was checked before applying
the exact existing postimage `f85e7359010bd25e0a909a145e283007cff25d7c`.
The source-sealed original jtreg test, including normal and interpreter
runs, has blob `d720912f0f57bcde9fc07a05f31869a19f3452eb`.
The earlier first-party verification script and report were received byte
identically; its original parent verifier script blob is
`60f757ba48acfeb77d8ac6c46ce36c94262eddec`.

This single private atom avoids reference self-writes before the first
deleted bit in a physical Object[] window. It keeps the original dynamic array,
long[] relative-bit-mask, offset/physical-slot and null/order/exceptions,
and leaves the existing System.arraycopy/GC-barrier bulk path intact.
No extra Entry/Node storage, public ABI, JNI symbol or separate runtime layer.

The earlier PR locally proved 2,371,785 assertions per baseline/candidate
execution and interpreter-mode candidate execution. **Those are historical
bounded proofs, not new PR CI results.** The combined workflow requires
the original `m3/qualification/mask-prefix-scalar-20261021/verify.sh` to
execute on this exact combined source tree in both normal and interpreter
modes, as well as the native `libjava`/filtered/sorted checks described above.

The two canonical recipe owners remain separate in Synexia for reuse and
counterexample correction: mask-prefix `com.synexia.m3.M3Jdk21MaskPrefixScalar20261021`
and this cross-owner integration
`com.synexia.m3.M3Jdk21NativeFilteredIntegration20261022`.
These form a **dependency DAG**, not competing Java class implementations.
Their tests and source receipts must be read together before product merge.
