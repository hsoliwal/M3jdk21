<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Empty-segment lookup and Release native checks

This bounded refinement repairs the existing immutable array module at M3JDK21
baseline `e913ab89056657eca4009678cfe9a657c020e79b`. It restores the missing byte
and UTF-16 snapshot `segmentCount()` implementations, makes joined scalar lookup
correct when cumulative segment ends repeat, and keeps the native comparison
test executable in Release builds.

The four source/test changes were emitted by the existing Synexia
Maven/OpenRewrite recipe engines, qualified, and received serially. The
canonical source, task crate and dataset are pinned in [SOURCE.json](SOURCE.json).
The local acceptance results below apply to those received bytes.

## Problem and selected atoms

| File | Existing failure | Received behavior |
| --- | --- | --- |
| `src/main/java/com/m3/arrays/M3Arrays.java` | `ByteSnapshot` and `CharSnapshot` omit the interface's required `segmentCount()` method; the module fails strict Java compilation. | Each snapshot reports zero nonempty buffers for empty payload and one otherwise, matching the existing int/long behavior. |
| The same file, private `locate(int[], int)` | `Arrays.binarySearch(ends, index + 1)` may choose any matching duplicate end. Lengths `[1, 0, 0, 1]` produce ends `[1, 1, 1, 2]`; a valid read at index zero can select an empty component. | A half-open upper-bound search returns the first cumulative end strictly greater than the logical index. All four byte/UTF-16/int/long join owners use the repaired atom. |
| `src/main/native/tests/m3_arrays_checks.c` | Release defines `NDEBUG`, which removes the old `assert` expressions and triggers unused-variable errors under `-Werror`. | Eight explicit comparison checks report failures and return `EXIT_FAILURE` in Release as well as debug builds. |
| `src/test/java/com/m3/arrays/M3ArrayEmptySegmentsSelfTest.java` and `M3ArrayEmptySegmentsTest.java` | Empty descriptors and their composition were not covered across the four families. | A standalone differential contract test and its JUnit entry point exercise the received implementation, including real JNI. |

The algorithm catalogue records this operation as
`ARRAYS / SEGMENTED_INDEXING / DUPLICATE_CUMULATIVE_BOUNDARIES`. The selected
control structure adapts the existing Synexia suffix-range upper-bound atom and
the target's `M3LongSortedSnapshot.upperBound`. Lookup remains `O(log s)` for
`s` segment descriptors with `O(1)` auxiliary space. It adds only primitive local
variables. Snapshot segment counts are `O(1)`.

The comparison rejected a linear rewind after binary search because a long run
of empty descriptors would make lookup linear in that run. Synexia's joined-byte
owner already documents and avoids repeated ends by removing empty descriptors;
this receiver preserves its existing descriptor owners and fixes the search
instead. Guava's duplicate-aware `SortedLists` bounds informed the comparison;
its implementation and dependency are not copied. [CATALOGUE.json](CATALOGUE.json)
records exact donor revisions, source paths, licenses, alternatives and links to
the LeetCode, HackerRank and GeeksforGeeks problem categories. Problem-site
solutions and editorials were not copied.

## Contract and ownership

All seven top-level public Java API surfaces match the compile-repaired
baseline. `M3Arrays` changes only its private implementation classes and lookup
atom. Production C, JNI declarations, exported JNI symbols and the existing
native module interface retain their original bytes. The C edit affects the
test executable.

Mutable input arrays still enter through a snapshot copy. Slices and joins keep
their existing immutable payload owners, descriptor arrays, fields and identity
rules. Explicit `copy`, `copyTo`, and JNI concatenation remain materialization
boundaries. Existing validation handles null inputs, negative/out-of-range
coordinates and logical-length overflow before the lookup atom. No runtime
dependency, cache, native pointer lifetime or retained payload owner is added.

## Acceptance evidence

| Gate | Result and evidence |
| --- | --- |
| Original preimage | Strict Java compile fails for the two missing methods; Release C build fails after assertions disappear. [baseline-java.log](baseline-java.log.txt), [baseline-native-build.log](baseline-native-build.log.txt). |
| Isolated lookup counterexample | Restoring only the two methods makes the original owner compilable; the valid index-zero read still fails in all four families. [baseline-empty-segment-witness.log](baseline-empty-segment-witness.log.txt). |
| Recipe qualification | Five real SDK tests, eight sealing tests and nine receiving contract cases pass. Actual SDK Results define the four outputs; replay is a fixed point and drift/tampering is refused. [recipe-sdk-tests.txt](recipe-sdk-tests.txt), [RECIPE_RECEIVER_RECEIPT.json](RECIPE_RECEIVER_RECEIPT.json), and the canonical evidence pinned by [SOURCE.json](SOURCE.json). |
| Original module build on actual receiver | Existing `native-linux` Maven profile completes `verify`: nine JUnit tests, zero failures/errors/skips; real Release CTest passes its comparison executable. [MAVEN_COMMAND.json](MAVEN_COMMAND.json), [maven-native-verify.log](maven-native-verify.log.txt). |
| Received-source Java/JNI differential test | **417,595 checks across 912 cases in each of three fresh JVM modes**: Java fallback, `-Xcheck:jni`, and interpreter `-Xint -Xcheck:jni`. [RECEIVED_RECEIPT.json](RECEIVED_RECEIPT.json). |
| Release oracle sensitivity | A separately compiled always-zero comparator is rejected by the explicit C checks with `NDEBUG` set. [release-oracle-mutation-refused.log](release-oracle-mutation-refused.log.txt). |
| Public API | Before/after `javap` surfaces are identical for all seven top-level public types. [before-api.log](before-api.log.txt), [after-api.log](after-api.log.txt). |
| Resource proof | Nine fresh JVM probes compare the compile-repaired baseline and receiver and exercise repeated endpoints. [RESOURCE_RECEIPT.json](RESOURCE_RECEIPT.json). |

The 912 behavior cases include every empty/nonempty placement across eight
components in both flat and nested joins, plus 400 seeded ragged layouts. Checks
cover scalar reads, segment counts, slices and nested slices, guarded copy
offsets, read-only buffers, snapshot/singleton identity, surrogate pairs and
unpaired UTF-16 units across seams, hash/search/equality/code-point behavior,
maximum signed-int logical coordinates, overflow and existing bounds contracts.
The JNI cases include zero length and lengths around the 1,024-unit chunk
boundary, using the actual module library.

The historical [TEMPLATE_RECEIPT.json](TEMPLATE_RECEIPT.json) describes authoring
preflight and retains its original pending-reception label. The later
[RECEIVED_RECEIPT.json](RECEIVED_RECEIPT.json) identifies the actual recipe
receiver and supersedes that label for this local receiving gate. These are
distinct stages of evidence.

### Resource result

The finite int-join workload retains 256 segment descriptors, constructs 512
cold joins, and performs 2,000,000 warm scalar reads. Three fresh JVMs per
scenario gave the same allocation and retained-topology counts:

| Measurement | Compile-repaired baseline, unique ends | Recipe receiver, unique ends |
| --- | ---: | ---: |
| Allocated bytes for 512 cold joins | 2,163,024 | 2,163,024 |
| Allocated bytes for 2,000,000 warm reads | 0 | 0 |
| Retained primitive-array payload bytes | 17,408 | 17,408 |
| Retained reference slots | 256 | 256 |
| Retained arrays / view objects | 258 / 257 | 258 / 257 |

The repeated-end receiver workload also allocates zero bytes during warm reads
and keeps the same descriptor/object topology. Retained primitive-byte counts
exclude JVM array/object headers. This Java lookup workload makes no JNI call
and allocates no native memory. CPU and wall times are recorded in the receipt;
they do not establish a throughput or application speedup. The original full
source could not compile, so the comparison baseline contains only the two
required snapshot methods and preserves its original lookup implementation.

## Reproduce the module gate

Run from the repository root with Java 21, Maven, CMake, a C11 compiler and JNI
headers available. `JAVA_HOME` should identify the JDK being qualified. Maven
must resolve the module's existing source-built test dependency
`org.junit.jupiter:junit-jupiter:5.10.2-m3-source-1`; the module POM and dependency
versions are unchanged.

```sh
mvn -f m3/arrays/pom.xml -Pnative-linux -DfailIfNoTests=true verify

"$JAVA_HOME/bin/java" \
  -cp m3/arrays/target/classes:m3/arrays/target/test-classes \
  com.m3.arrays.M3ArrayEmptySegmentsSelfTest

"$JAVA_HOME/bin/java" -Xcheck:jni \
  -cp m3/arrays/target/classes:m3/arrays/target/test-classes \
  com.m3.arrays.M3ArrayEmptySegmentsSelfTest \
  "$PWD/m3/arrays/target/native/libm3_arrays.so"

"$JAVA_HOME/bin/java" -Xint -Xcheck:jni \
  -cp m3/arrays/target/classes:m3/arrays/target/test-classes \
  com.m3.arrays.M3ArrayEmptySegmentsSelfTest \
  "$PWD/m3/arrays/target/native/libm3_arrays.so"
```

Each standalone process must report `checks=417595 cases=912` and the expected
`native=false` or `native=true`. The recorded qualification used the actual
source-built M3 image identified by the receipts and the original POM's Release
native profile. The canonical crate's README supplies the recipe qualification
and receiving commands for dataset `m3jdk21-arrays`.

This is local Linux qualification of the separately licensed array module.
String remains the active ordered receiving phase. Hosted CI, other native
platforms, full JDK/JCK qualification and array-family runtime promotion require
their own evidence.

The public authoring package and portable recipe commands are retained in [recipe-custody/CUSTODY.md](recipe-custody/CUSTODY.md).

## Standalone proof source layout

The runnable [allocation probe](src/test/java/M3ArrayLookupCostProbe.java) and
[empty-segment witness](src/test/java/M3EmptySegmentWitness.java) live in the
recognized `src/test/java` proof layout. Their bytes match the original evidence
sources. `recipe-custody/OWNER_CUSTODY.json` records the delivery aliases.
The initial root-level Java packaging was refused by the unchanged recipe path
admission rule before target mutation; no admission rule or Java lane was changed.

From this qualification directory, compile the allocation probe after building
the owning array module:

```sh
mkdir -p target/probe-classes
javac --release 21 -cp ../../target/classes -d target/probe-classes \
  src/test/java/M3ArrayLookupCostProbe.java
java -Xbatch -cp target/probe-classes:../../target/classes M3ArrayLookupCostProbe
```
