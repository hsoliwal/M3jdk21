<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3FixedSizeLongListView — candidate source and ABI receipt

**Draft candidate, NOT release qualified.** No standard JDK 21 type, module,
native symbol, HotSpot implementation or public Java SE contract is changed.

## Compatibility direction

M3JDK21 preserves source, binary, API, and behavior compatibility for ordinary
JDK 21 programs running on M3JDK21. Stock OpenJDK need not understand M3-only
extensions such as `com.m3.collections.M3FixedSizeLongListView`.

## Source and donor custody

- Target base: `hsoliwal/M3jdk21@8c8dd70e659b99c23a2d0e7daeb2adadf95429c2`.
- Original unimplemented source Git blob: `7233983f45bec901c0a54fabf7146e4864634e9a`; SHA-256 `2cc0f8997637362321105759afe3a987e320b14a81840217f73aa87652cc98bb`.
- Candidate Java source Git blob: `a27404f795613cdc3e1826b8a192b79bb614f5bb`; SHA-256 `2a09249453eb4e7f27d55ba348765b4d928569e8a0286062aadf732793967fd5`.
- New target JUnit source Git blob: `6b2ecded549c80ef34602f69840efa5521ee3ac5`; SHA-256 `e06e84af3cf85774e680cae4c826f1aa33c7bb869d642807f39bbe0905587631`.
- Both public Java files were read back as exact Git blob matches to private Synexia templates.
- Canonical private recipe source: `hsoliwal/com.synexia@d6256abccca04ab7f92802a66e5ca1abcbf65a16`,
  named `com.synexia.m3.M3FixedSizeLongView20261029` using the existing
  `M3HashPinnedJavaSnapshotRecipe` with one replacement and one new test.
- All recipe classes/YAML/templates, execution fixtures and memory remain in Synexia.
- Donor behavior: original JDK 21 `java.util.Arrays.asList` fixed-size backed-list semantics,
  plus conceptual C++ span / Rust mutable-slice reference. No foreign source copied.
  OpenJDK GPLv2+Classpath source stays under its original license.

## Behavioral scope

`of(long[])` retains the actual caller-owned primitive array. `get`/`set`
operate at the same physical slot; external writes remain observable.
`indexOf`/`contains` use primitive long equality. `toArray` produces a detached
copy; `add(long)` always raises `UnsupportedOperationException`. Null array
and bounds errors follow the corresponding Java exception classes.
No per-entry node, wrapper, state-mask lane or redundant size field is retained.
Single-threaded or externally synchronized semantics; not a concurrent view.

## Executed and outstanding gates

- **PASS:** OpenJDK 21.0.12.1 `javac --release 21 -Xlint:all,-serial,-processing -Werror`.
- **PASS:** 600 seeded array fixtures × 125 operations = 75,000 differential
  operations against boxed `Arrays.asList(Long[])`, with live alias changes,
  duplicate values, empty arrays and signed long extremes.
- **PASS:** four target regression methods executed with throwing local JUnit
  assertion stubs. This is not real Maven/JUnit Jupiter qualification.
- **PASS:** compiled exact original Git-blob source and new source independently;
  `javap -public -s` ABI descriptors are byte-identical.
- **PASS:** `javadoc -Xdoclint:all,-missing` on the candidate Java owner.
- **NOT RUN:** actual Synexia Maven/OpenRewrite execution and fixed-point tests;
  real JUnit Jupiter/Surefire, entire collections reactor, source-built
  M3JDK21 image, jtreg/JNI/GC/platform validation or CPU/heap benchmarking.

**Promotion remains HOLD** until these relevant gates pass on an exact
reviewed product head. Candidate hashes and a GitHub mergeability indicator
do not constitute release admission.
