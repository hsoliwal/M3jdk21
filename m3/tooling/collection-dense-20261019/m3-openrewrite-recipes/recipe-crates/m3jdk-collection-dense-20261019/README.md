<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 integrated collection refinement

Canonical recipe: `com.synexia.m3.CollectionDense20261019`.
This continues M3JDK21 PR 1435 at `2717d156ad2aba73dfdfcfc6a58b59a82371798e`.
The existing `java.util.Collections` factories, package-private `M3CollectionViews`
and `M3CollectionMasks`, and libjava JNI kernel remain the runtime owners.
No separate SDK layer, public API, runtime dependency or native symbol is added.

The source-sealed recipe composes the unchanged canonical Java and text snapshot
engines. Java is parsed through OpenRewrite Java 21; C is a sealed native text
postimage. Actual SDK execution emits three production files and one new test.
The independent custody recipe emits additive owner/provenance and target mapping
records. Global Synexia ledgers and their fixtures are not exported to the product.

Read CONTRACT.md before the implementation; SOURCE_PINS.json records exact inputs,
outputs and dependency fixtures. CATALOGUE.tsv retains existing donor/category
decisions and the rejected per-word dispatch. No challenge implementation is copied.
NOTICE and the scoped licenses distinguish authoring code from GPL target fixtures.

## Runtime changes

- Fully accepted filtered snapshots retain their already detached reference array,
  avoiding an identity int[] and a duplicate Object[]. Stable Arrays.sort is reused.
- Fully selected mask ranges use consecutive-index filling in Java and JNI. A second
  word count retains best-effort drift refusal. Partial masks retain the old sparse loops.
- Existing live filtered collections, lists, sets, sorted/navigable sets, deques,
  maps, sorted/navigable maps and sorted-list observations are preserved.

## Executed qualification

Both candidate trials retain original logs. The final trial compiles and checks the
baseline and each cumulative production-file pass. Public Collections descriptors
and javac-generated JNI headers match byte-for-byte. Each state passes 15,288 view
checks in mixed and interpreter modes, 50,266 existing mask checks with real JNI,
12,667 consumer checks in each Java/native/missing-symbol-fallback mode, and the
new dense corpus: 365,344 checks in Java/interpreter and 374,432 with native enabled.
The final baseline/candidate also pass the separate >2^31-bit capacity fixture.
C17 builds use warnings-as-errors, -Xcheck:jni and UBSan; ordinary optimized native
binaries are separately rechecked before measurement. Product compilation is warning-free.

Allocation, CPU and retained structure are measured independently in BENCHMARKS.md
and BENCHMARKS.json. Both rejected and final timing series, process RSS, all sparse
regressions, single-workload follow-up, and proof-runner corrections remain visible.
Results do not justify changing Java-default native routing or a universal speedup claim.

## Reproduce

Use Java 21, Maven 3.9.9 and the approved offline Maven cache. The pom pins OpenRewrite
8.17.1, JUnit 5.10.2, compiler 3.14.1 and Surefire 3.2.5. Checkstyle dependencies are
enumerated by proof/lint-classpath.txt. From this crate, use a fresh external WORK:

```sh
python3 proof/qualify.py recipe --work "$WORK" --jdk "$JDK" --maven "$MVN" --m2 "$M2"
python3 proof/qualify.py product --work "$WORK" --jdk "$JDK" --maven "$MVN" --m2 "$M2"
python3 proof/measure.py --work "$WORK" --jdk "$JDK" --m2 "$M2"
python3 proof/focused.py --work "$WORK" --jdk "$JDK"
```

The work directory is explicit scratch; source inputs and generated outputs are separate.
Each failing stage stops. Never edit generated files. A second recipe run must be empty.
The measurement resume switch accepts only hash-verified completed compile/runtime logs.

This is Java 21 patch-module and Linux JNI qualification, not a full product-image build.
Full jtreg/tier/platform, JCK, reactor and release acceptance remain pending. Parent PR
1435 is a dependency; no merge or global String -> arrays -> collections phase promotion
is performed. The entire Eclipse framework and the complete library/JDK superset remain open.
