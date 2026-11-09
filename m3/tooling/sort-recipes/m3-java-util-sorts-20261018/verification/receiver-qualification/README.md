<!-- SPDX-License-Identifier: Apache-2.0; Copyright 2026 Hitesh Soliwal and contributors -->
# Actual generated java.util sorting qualification

All results in this directory were produced from the 14 files emitted by the real
OpenRewrite recipe into the target checkout at the pinned source base. The eight
Java targets comprise six runtime sources and two standalone test sources.
The two pre-existing progress owners were copied byte-for-byte from the same base
and compiled alongside those additions. No fixture replaces a sorting API,
algorithm kernel or progress owner in this qualification.

## Executed gates

| Gate | Observed result |
| --- | --- |
| Java compiler | Temurin javac 21.0.12.1 |
| Existing progress source compilation | Pass; -Xlint:all,-try -Werror, preserving its deliberate unused try resources |
| Six new java.base source files | Pass; --release 21 -proc:none -Xlint:all -Werror |
| Two actual test sources | Pass under the same strict compiler flags |
| Default VM contract suite | 34 algorithms; 11,398,470 checks; seed 868245229 |
| Interpreted contract suite | 34 algorithms; 171,243 checks; same seed; explicit smoke profile |
| Public API extraction | javap succeeded; complete output retained in public-api.txt |
| Exploratory measurement | 140 rows: Arrays.sort plus 34 algorithms, four 512-element input shapes, nine measured repetitions |

The contract suite verifies all eight primitive types, object identity stability,
signed and floating-point order, full-domain and histogram-domain admission,
absolute permutations, arbitrary repeated index labels, subranges and surrounding
values, key evaluation once per successful admission, comparator/key/progress
failure before payload writes, borrowed monitor ownership, cancellation and
interruption behavior, network boundaries, structured inputs, and independent
concurrent invocations. The interpreted profile is smaller and is labeled as such.

source-hashes.json binds every generated target and all six unchanged baseline
sorting/progress dependencies. qualification.json records the original target
Git base, checked revision and executed scope. Empty compile logs represent
successful commands with no diagnostic output; the verifier fails immediately
on any nonzero compiler or test exit.

The separate verification/comparison-kernel-audit directory retains an additional
358,597-case kernel audit and its explicitly labeled context/enum fixtures.
Its claims must not be conflated with the actual public API qualification here.

## Reproduce

From the generated M3JDK21 checkout, with a Java 21 JDK:

~~~sh
python3 m3/verification/sorting/verify.py --java-home "$JAVA_HOME"
~~~

The default recipe location is the exact canonical proof-tooling mirror under
m3/tooling/sort-recipes/m3-java-util-sorts-20261018. The local execution selected
the canonical crate explicitly with --recipe-crate and retained build output
outside the product checkout. Those path choices do not change the sources.

## Interpreting the measurements

benchmark.csv contains first measured call wall time, warmed median wall time,
median current-thread CPU time and median current-thread allocation. Array cloning,
ordering-key/permutation preparation and payload commit are inside the timed and
allocated operation. Sorted-result verification is outside it.

Each algorithm's first measured call occurs in one shared JVM; it is not an
independent cold-process startup measurement. Twelve warm-up calls precede the
nine measured repetitions. Compilation, scheduling and preceding algorithms can
affect these small samples. The inputs are random keys in a 256-key domain,
ascending, descending and equal; all have 512 elements. This is an exploratory
single-VM observation, not JMH, a universal algorithm ranking, a full retained-heap
measurement, or evidence for changing the JDK's default sorting policy.

## Remaining acceptance

The actual generated sources have been compiled and executed as a java.base patch
on the recorded Java 21 host. A full source-built M3JDK21 image, actual product
jtreg harness, JCK, cross-platform JNI/GC/CDS runs and ordered receiving-stage
acceptance remain separate gates. This evidence does not advance those stages.
