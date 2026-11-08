<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Immutable numeric range copying

This file-scoped refinement applies to `M3Arrays` at
`hsoliwal/M3jdk21@4b24824c3aa09d802121447f8970d442a855f064`. It reuses the existing
private int/long snapshots, slices and joined views. It changes the implementation
of their existing `copyTo` operation. The public array-view interfaces, factory
signatures, retained fields, snapshot admission and JNI owners keep their
existing contracts.

The current baseline already contains the duplicate cumulative-end lookup
repair. The retained empty-segment regression passes on that baseline; this
refinement is a distinct copy-cost improvement.

## Selected operation and alternatives

The problem category is `ARRAYS / IMMUTABLE_NUMERIC_RANGE_COPY`. The existing
interface default performs an accessor call for every copied element. On a
flat joined owner, each accessor locates its segment again. The refined private
join locates the starting segment once, walks the existing cumulative-end
directory in encounter order, and copies each covered run. Empty descriptors
remain present and are skipped when their run length is zero.

Private snapshots, and private slices directly backed by snapshots, use the
existing JDK `System.arraycopy` operation. This adapts the destination-offset
bulk-copy pattern already used by the module's Java concatenation fallback.
Other owner shapes retain the original element-accessor loop. In particular,
copying an external segment does not dispatch its possibly overridden `copyTo`
method. That alternative would change which client callbacks run, the identity
of thrown exceptions and the prefix written before a callback fails.

For a flat join of known immutable arrays, the selected route costs
`O(log s + k + n)` for `s` descriptors, `k` crossed descriptors and `n` copied
elements, with constant auxiliary space. A nested owner or arbitrary external
view retains its accessor cost within a run. No new array, descriptor lane,
cache, persistent index or object field is introduced. Flattening the joined
payload was rejected because it adds an unnecessary allocation and changes the
failure boundary.

The searchable entry `ALG-NUMERIC-COPY-RUNS` in
`m3/backports/ALGORITHM_CATALOGUE.tsv` and the adjacent `CATALOGUE.json` record
the candidates, exact source owners, license decisions and required proof.
LeetCode, HackerRank and GeeksforGeeks supply problem-category references;
their solutions and editorials are not copied. The existing first-party
implementation patterns and the existing JDK API supply the implementation.

## Preserved observations

Each override keeps the original order of checks: target nullity, source
window, then destination window. Negative values, overflow-sized windows and
zero-length copies retain the default's exception type and message. No write
occurs before those checks finish. The fallback retains the same external
accessor order, callback-visible target prefix and exact thrown object. It
does not reread external lengths after join admission. The implementation
adds no shared mutable state; concurrent copies from an immutable owner into
independent targets require no new synchronization.

The candidate changes one production Java source file and adds a focused
standalone test, its original-module JUnit entry point, and resource probes.
The JNI declarations, production C implementation and native build profile
remain the existing module owners. Their original Java/JNI regression gate
must still run on the received source tree.

## Qualification and reproduction

The transformation uses the existing canonical
`m3-collections-contract-refinement-20261017` Maven/OpenRewrite host, dataset
`m3jdk21-numeric-copy`, and the original four recipe/scope sources pinned by
`ENGINE_SOURCES.tsv`. The recipe is qualified before receiving any product
file. Original SDK tests exercise real OpenRewrite Results, input drift,
missing/occupied targets and a fixed point. Original Python contracts check
input sealing and the receiving transaction. Recipe input is repaired and
resealed if a result is incorrect; emitted target files are not hand-patched.

Run the original module gate with Java 21 and its existing source-built
`5.10.2-m3-source-1` test dependency closure:

```sh
mvn -f m3/arrays/pom.xml -Pnative-linux -DfailIfNoTests=true verify
"$JAVA_HOME/bin/java" -Xcheck:jni \
  -cp m3/arrays/target/classes:m3/arrays/target/test-classes \
  com.m3.arrays.M3ArrayNumericCopySelfTest
"$JAVA_HOME/bin/java" -Xcheck:jni \
  -cp m3/arrays/target/classes:m3/arrays/target/test-classes \
  com.m3.arrays.M3ArrayEmptySegmentsSelfTest \
  "$PWD/m3/arrays/target/native/libm3_arrays.so"
```

Repeat the standalone tests with `-Xint`, `-XX:TieredStopAtLevel=1`, and
`-XX:-TieredCompilation -Xbatch` for the interpreter, C1 and C2 observations.
The numeric corpus compares valid windows and guard elements, adjacent-range
composition, snapshot detachment, null/bounds ordering, exact failures and
partial writes, external accessor effects, maximum signed-int coordinates,
empty seams, nested owners and concurrent immutable reads. Its oracle uses
the unchanged interface default. The retained seam corpus covers all four
primitive families and, when supplied the module library, real JNI.

The qualification sources in `src/test/java/com/m3/arrays` include an adapted
allocation/CPU probe and an Instrumentation size agent. Compile those two
sources separately against the module classes on the class path, then package
`M3ArrayCopySizeAgent.class` in a jar whose manifest specifies
`Premain-Class: com.m3.arrays.M3ArrayCopySizeAgent`. Run the probe with
`-javaagent:<jar>` and one of `snapshot`, `slice`, `join`, or `nested`.
Each fresh JVM reports five warm samples per primitive width, cold construction
allocation and timing, exact owned-graph shallow size, primitive payload,
reference slots, and process/direct-buffer observations. Compare baseline and
actual SDK output under identical JVM flags and source closure. These finite
observations are not an application throughput or platform-wide speed claim.

Public `javap -public -s` surfaces and retained-field topology must match the
baseline. Fresh qualification receipts identify exact generated source hashes,
commands and results. Earlier recipe evidence remains historical evidence for
its own input hashes. This separately built array-module proof does not change
the active ordered String receiving phase or establish full JDK/JCK/platform
qualification.
