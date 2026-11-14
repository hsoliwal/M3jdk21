# Local receiving proof

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

The final generated source bytes passed these local gates:

| Gate | Result |
| --- | --- |
| Canonical Maven/OpenRewrite | 3 JUnit tests; 7 Java and 4 text outputs; named composition, fixed point, serial/reversed/width-two replay and refusal checks passed |
| Java 21 strict compilation | All 134 production and 161 test sources; warnings as errors |
| Final complete discovered collection JUnit suite | 234 successful, zero failures, one optional native-mode assumption abort out of 235 tests |
| Selected required-native JUnit run | 15 successful, zero failures, zero skips/aborts; includes the optional test above with actual JNI required |
| New split regression | 6,324 assertions, including recursive child comparators and lazy comparator-getter effects |
| New mask differential | 12,000 cases each with Java and actual JNI |
| Existing native mask regression | 357,095 sparse checks; 28,914 bounded checks including 5,000 randomized cases |
| C/JNI | C17 warnings as errors; generated JNI headers; fatal UBSan; `-Xcheck:jni`; region fault probes; explicit missing-library refusal |
| API | Public inherited method/constructor descriptors unchanged for the five affected public APIs; native headers unchanged |
| Workflow | YAML parsed; existing PR filters cover paths; added job checks out and asserts exact PR head |

The initial full-suite run had one 30-second MPMC deque timeout. The unchanged test passed
in isolation on baseline and candidate classes, then the final full suite passed with a
bounded JVM (`-Xmx768m -XX:ActiveProcessorCount=2`). Both runs are retained in canonical
evidence. A first candidate also failed the new lazy-comparator-getter regression; the
corrected final recipe preserves that timing.

JNI instrumentation verifies copied payload counts: one row rather than 128 for the
single-match and dense-limit-one fixtures, and 130 rather than 300 for a sparse cross-chunk
fixture. This costs 128 additional stack bytes and makes no statistical timing claim.

The owning Maven command did **not** pass: its custom
`org.junit.jupiter:junit-jupiter:5.10.2-m3-source-1` artifact was absent from the available
offline cache. The standard JUnit console supplied the independent executable tests above.
Hosted head execution, full reactor, built M3JDK image, jtreg/JCK and non-Linux native gates
remain separate. No skipped, unavailable or zero-step Actions result is treated as success.

`RECEIVER.json` pins the Synexia recipe and all generated target hashes. The canonical
crate retains detailed logs, exact baselines, rejected candidates, donor decisions and
source identities. This is collection proof, not proof of the earlier 19,396-source corpus
or completion of the full Synexia receiving estate.
