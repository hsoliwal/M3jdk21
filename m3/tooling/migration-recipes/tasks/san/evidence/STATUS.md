# Exact local proof status

Source is an authorized partial snapshot, not a full source checkout. M3JDK21 is a
sparse checkout of the pinned public target. These proofs do not certify whole
Synexia, all lean collection integration, or an Apache/Guava/Eclipse/JDK superset.

| Proof | Result |
| --- | --- |
| SAN Java donor recipe task | PASS: 5 JUnit tests, including scope/accounting; generated closure compiles with Java 21 all-lint/Werror |
| Generated donor contracts | PASS: 90 checks; five retained existing JUnit methods invoked; public Seed javap signatures unchanged |
| SAN JDK text recipe task | PASS: 3 JUnit tests; three exact postimages, fixed point and refusals |
| Installed inventory | PASS: all 12 tests, including five original tests |
| Existing M3JDK21 collection module | PASS: all 18 existing tests; no new collection runtime source installed |
| Complete common collection source acquisition | PASS: 301 main Java + 67 test Java + 8 native blob identities; only transport/accounting and lexical dependency review |
| Strict whole-repository admission / full reactor / delivered JDK image / jtreg / JIT-AOT / JNI platform matrix | NOT RUN |

First donor baseline compilation failed with two real missing/inaccessible helper
errors. JDK recipe cache preparation first failed on missing IntelliJ annotations,
then on absent SLF4J runtime; the POM declares both required dependencies now.
No tests/gates were weakened. Final Maven tasks ran offline after explicit cache
preparation. The cache hash inventory is evidence, not a vendored dependency mirror.

Logs normalize trailing line whitespace only; `logs.json` retains raw and stored
SHA-256 identities. `toolchain.json` binds the archives actually used. Companion
proof details live in the other SAN PR. Workflow wiring is locally inspected;
GitHub exact-head workflow results must be checked after publication.
