# M3 port candidate evidence

Destination baseline: `45f546ff5bcb06a1b2604f14baf998785d98d9a1`.
Source provenance is recorded in `../provenance.json`. Linux x86_64, JDK 21.0.12.1,
Maven 3.9.9, real OpenRewrite 8.17.1 and JUnit dependencies; no stubs.

- PASS: actual Maven recipe, three execution/fixed-point/refusal tests; 21 Java target
  files (17 runtime classes/interfaces, module descriptor and three test files).
- PASS: clean Java 21 named-module compilation with all enabled warnings as errors,
  separately compiled tests/probes, modular runtime JAR and descriptor validation.
- PASS: 18 collection contract tests, both through the focused verifier and the standalone
  library Maven POM. `maven-library.log` records the independent Maven test build.
- PASS: C17 syntax/compile with warnings as errors and UBSan; checked-JNI parity over
  66 boundary ranges and 1,200 changing random ranges.
- PASS: five zero-thread-allocation trials for the warmed prepared scalar/page-copy loop.
  Growth, streams, snapshot writes and a general speedup are excluded from this measurement.
- PASS: tested recipe candidates materialized after hash checking the complete target set.

Exact final verifier commands/output/exits are under `final/`. The first module compile
failed on ten implicit exported constructors; documented equivalent constructors were added
to the sealed recipe templates and hashes, and actual recipe/compile/tests rerun. The failure
is retained in `compile.log`. Only the existing module-name convention's terminal-digit style
warning is excluded (`-Xlint:all,-module`); no missing-constructor warning was suppressed.

BLOCKED / not established: a full OpenJDK source build, jtreg, the complete existing M3 suite,
whole-repository lint, downstream compatibility and architecture-wide performance/linearizability.
Authenticated full checkout is unavailable in this workspace. The GitHub root/M3 subtree and
existing text/migration owners were inspected; this independently buildable additive module
does not claim to replace those gates or deliver every JDK collection and challenge algorithm.
