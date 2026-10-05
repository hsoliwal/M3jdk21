# Exact local proof status

Source is an authorized partial snapshot, not a full source checkout. M3JDK21 is a
sparse checkout of the pinned public target. These proofs do not certify whole
Synexia, all lean collection integration, or an Apache/Guava/Eclipse/JDK superset.

| Proof | Result |
| --- | --- |
| SAN Java donor recipe task | PASS: 5 JUnit tests, including scope/accounting; generated closure compiles with Java 21 all-lint/Werror |
| Generated donor contracts | PASS: 90 checks; five retained existing JUnit methods invoked; public Seed javap signatures unchanged |
| SAN JDK text recipe task | PASS initially: 3 JUnit tests; final combined task: 5 JUnit tests plus six retained A3 test methods; six exact postimages across two packets, fixed point and refusals |
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

## A3 CI repair and broader gate result

Initial public GitHub A3 run `37254813807` failed on the two pre-existing generic
Tree/SourceFile call chains. The pinned Java repair retains typed metadata,
tracks exact absent-before generation and corrects a test's expectation of the
scheduler-added change marker. The final offline SAN task passes all five JUnit
tests; all six retained A3 methods run against actual generated/compiled owners.
No gate or recipe source seal was disabled.

The existing `mvn -f m3/pom.xml -pl tooling/a3 -am verify` gate was then attempted
with explicit online cache preparation. It compiled the migration recipe module
and ran 133 tests: 3 failures and 3 errors, no skips. The failures are in unmodified
areas: malformed-descriptor refusal (one), unregistered RemoveUnusedImports scope
(one), String-owner type attribution (two), and JNI String-owner parsing (two).
The downstream A3 module and coverage/remaining reactor stages did not complete.
The full gate is still failing; narrow SAN/A3 proof is not substituted for it.
Full JDK image and runtime matrix remain unexecuted. Keep this PR draft.
