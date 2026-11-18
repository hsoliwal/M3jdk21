<!-- SPDX-License-Identifier: Apache-2.0 -->

# Groovy 6 GDK → original M3 ObjectStream owner

**Admission: DRAFT / HOLD.** See `GROOVY6_CONVERGENCE.tsv` for the feature ledger.
This slice proposes two typed Java 21 `ObjectStream.groupByMany` overloads
inspired by Apache Groovy 6's many-to-many grouping operator. The execution
owner remains the original array-backed `M3LinkedHashMap` / `M3ArrayList` pair.
No standard `java.util` or `java.lang.String` API is changed; this is a separately
opted-in M3 API, with explicit source, distinct-key and total-membership budgets.

`tests/M3GroovyGroupByManyTest.java` checks 25 contract cases, while
`tests/M3GroovyGroupByManyFuzzTest.java` cross-checks 2,500 seeded trials.
The Groovy 6-only `.groovy` fixtures are kept outside the 4.x/5.x common
fixture folder, because those older releases need not implement `groupByMany`.

The existing Groovy interop gate has a `compare-locked` mode that refuses
unapproved/different jar bytes. Create that lock **only after** verifying the
Apache Groovy release signature from the official release archive. It records
input SHA-256 before/after both child executions and rejects changes. The old
unlocked `compare` command remains for development only.

## Qualification at this writing

- PASS: Candidate method compiles on JDK21 with local test stand-ins; 25 checks
  and 2,500 deterministic trials pass. These are **not** full-module tests.
- PASS: Gate compiles under `javac --release 21 -Xlint:all -Werror` and
  passes 14 self-tests. Mock-only subprocess and tampered-lock refusal pass.
- PASS: Existing Synexia recipe input-preparer fixed-point/drift tests (9).
- NOT_RUN: Exact original `jdk.m3.collections` source-built module compilation,
  original M3 map null-key implementation tests, Groovy 6 reference runtime,
  both JDK21 vs M3 image runs, Maven/OpenRewrite SDK, ABI/jtreg/JNI/GC,
  Windows/ARM, cross-language Kotlin/Scala method resolution.

**Do not merge until the original Java21 module and donor semantic gates pass.**
The known `String.capitalize()` extension collision remains separately blocked.

Source references:
- https://groovy-lang.org/releasenotes/groovy-6.0.html
- https://docs.groovy-lang.org/latest/html/gapi/org/codehaus/groovy/runtime/DefaultGroovyMethods.html
- https://github.com/hsoliwal/M3jdk21/pull/3344
- https://github.com/hsoliwal/com.synexia/pull/18656
