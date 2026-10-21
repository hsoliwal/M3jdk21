<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# M3JDK21 original-owner identity-index rank receiving

This is source received directly into the integrated M3JDK21 SDK's existing
M3LongStateArray and M3IndexPathRank; not an external Java-wrapper layer.

Canonical first-party Synexia recipe and source seals:
https://github.com/hsoliwal/com.synexia/pull/12849

Four generated Java postimages are byte-identical to the canonical recipe's
hash-pinned resources. The recipe's before-source pins were verified against
product master 7b1b7af779683418be9a2a5bf8ed203a0e316422 before this receiving commit. See the accompanying
CATALOGUE.tsv for each evaluated problem category, algorithm donor and refusal.

The implicit identity path removes the eagerly retained 0..n-1 int[] in
M3LongStateArray.Snapshot and the second cloned int[] in M3IndexPathRank;
general signed/duplicate/strided row IDs retain their original implementation.
Java primitive word masks, offset rank/select and sorted/filtered occurrences
remain authoritative. Existing M3IndexPathMasks C/JNI selector is reused for
NATIVE_REQUIRED without changing symbol names, export sets, ownership or
callback semantics. JNI path ordinals are temporary arrays used only for the
duration of the native selection and checked against the Java result.

Source and method contracts stay in place. This is an internal collection
owner candidate and does not promote any alternative public java.util type.
The canonical naming map and String -> arrays -> collections phase gate remain
authoritative. Coordinate additive forward-merges with collection rank-bulk
PR #1738 and other active source candidates rather than overwriting them.

## Status and gates

Java21 compiler, Maven/JUnit, source-sealed real OpenRewrite execution and
fixed point, original C/JNI backend parity, installed JDK/jtreg/GC/CDS,
multi-platform resource and CPU/heap/native benchmarks require actual CI runs.
They are NOT_RUN/UNVERIFIED until their receipts exist. Retained primitive
payload saving is structural (two int[n] lanes, approximately 8*n bytes);
it is not an empirically measured performance result.

## Standalone Java21 check (on an authorized checkout)

```sh
mkdir -p /tmp/m3-identity-rank
javac --release 21 -Xlint:all -Werror -d /tmp/m3-identity-rank \
  m3/collections/src/main/java/com/m3/collections/M3IndexPathRank.java \
  m3/collections/src/test/java/com/m3/collections/M3IndexPathIdentitySelfTest.java
java -cp /tmp/m3-identity-rank com.m3.collections.M3IndexPathIdentitySelfTest
mvn -B -ntp -f m3/collections/pom.xml -Dtest=M3IndexPathIdentityTest,M3LongStateArrayTest test
```

Check JNI parity under `-Xcheck:jni` using the existing backend on a build
where its native library is actually installed; do not interpret the
missing-native refusal as a successful native integration.
