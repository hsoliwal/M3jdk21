<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Isolated comparison-kernel fixture audit

**This is isolated kernel evidence, not qualification of the real public API.**
The retained `KernelAudit.java`, `SortAlgorithm.java` and `M3SortContext.java`
are the exact sources used by the recorded 358,597-case audit. The enum contains
only the 28 comparison families. The context is an explicitly labeled fixture
that counts steps and throws after a chosen limit; it does not reproduce the
real ProgressMonitor integration. Do not put these fixtures on a production
classpath or substitute this audit for `SortsContract`.

The two kernels are read directly from this crate's qualified candidate payloads
at `../../payload/java/`. They are not independently forked in this audit subtree.
`recorded-audit.json` seals the two candidate payloads, all three Java fixtures,
and the recorded result. A source mismatch refuses a recorded-evidence replay;
a successor candidate needs new verification and an updated receipt.

## Rerun

Use a complete Java 21 JDK and Python 3. The build directory must be new or empty.
For example, from the recipe crate's root:

```bash
python3 verification/comparison-kernel-audit/run-audit.py \
  --java-home /path/to/jdk-21 \
  --build-dir /path/to/empty-comparison-audit-build
```

Omit `--java-home` to use `JAVA_HOME`, or the `java` and `javac` executables on
`PATH`. Omit `--build-dir` to create a fresh temporary build directory. The runner
checks all recorded source digests, creates an isolated java.base patch containing
only the two kernels and three audit fixtures, then compiles and runs the audit.
Compiler/runtime versions and output remain in the build directory. The expected
last line is:

```text
AUDIT_ONLY total cases=358597 PASS; production API/monitor/recipe qualification not implied
```

## Recorded scope

Every comparison family is checked against a stable JDK oracle over exhaustive
ternary keys through length 7, randomized inputs, shuffled labels and duplicate
labels. Checks cover sorting, multiplicity, declared stability, full-range
comparator signs, and fixture step exceptions. Both sorting networks additionally
pass exhaustive zero-one inputs through length 16 and power-of-two boundaries
through length 4,097. Thirteen scalable families are exercised at length 40,001
over ascending, descending, equal, 17-key and random inputs. The recorded run and
a complete rerun after the empty insertion-subrange overflow guard both passed.

This audit does not test the real API overloads, production cancellation or
monitor ownership, numeric distribution algorithms, recipe fixed-point/refusal,
or a complete M3JDK21 image. The target acceptance runner owns those applicable
gates separately.

## Ownership and licensing

Copyright 2026 Hitesh Soliwal and contributors. The original Java audit fixtures
are target-owned GPL-2.0-only WITH Classpath-exception-2.0 test expression. Their
source is retained byte for byte. The Python runner, JSON receipt and this audit
documentation are Apache-2.0 proof tooling. Each referenced kernel retains its
own per-file license, including the OpenJDK TimSort copyright and source lineage.
