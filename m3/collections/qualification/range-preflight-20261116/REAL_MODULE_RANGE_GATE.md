<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# CollectionViews range: real M3 module acceptance step

The Java 21 source-compatibility/oracle check in
`.github/workflows/m3-collectionviews-range-product-candidate-20261116.yml`
already compiled the full historical/current `CollectionViews` owner and
verified frozen old client linkage in a bounded stand-in classpath.

The existing `m3jdk-modules.yml` workflow builds the **real full**
`src/jdk.m3.collections/share/classes` module, including its module
descriptor, actual `CollectionViews`, `DefaultProgressMonitor`, and
dependent `jdk.m3.arrays` sources. Its helper writes the compiled module
into `$RUNNER_TEMP/m3-arrays/module`.

This additive acceptance step **reuses that built module** instead of
rebuilding another shadow CollectionViews implementation. It strictly
compiles the actual `test/jdk/jdk/m3/collections/CollectionViewsRangeCompatibilityTest.java`
using `javac --release 21 -Xlint:all,-module -Werror` with
`--module-path ... --add-modules jdk.m3.collections`.

The test class then runs on the *real compiled M3 module* in ordinary,
`-Xint` and C1-limited JVM modes, requiring
`M3_STATIC_RANGE_COMPAT_PASS checks=46` each time.
The fixture covers `ArrayList`, `LinkedList`, an external
`AbstractList` subclass implementing unrelated defaults, its old
dispatch, real borrowed `DefaultProgressMonitor` lifecycle,
cancellation before iterator acquisition and interruption retention.

**This is a new gate, not an execution receipt.** A source commit, a
queued workflow, or a previously successful general module compile is not
proof that this exact test ran against the real module. Preserve hosted
job IDs/logs when it executes.

An actual installed M3JDK21 image with jtreg, complete JDK/HotSpot/JNI
and platform tests, the canonical OpenRewrite SDK output provenance,
and the source-compatibility policy decision for wildcard static imports
remain independent release gates. Original JDK `List`/`AbstractList`
declarations and original collection runtime classes are untouched here.
