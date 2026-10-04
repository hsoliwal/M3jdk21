# DB private atom pass: executed acceptance

## Changes

Four cohesive blocks were extracted into private operations of the existing
store/codec: size aggregation, atomic-move fallback, edge emission and framed
text reading. No public API, dependency, POM, workflow, persisted format or
coverage threshold changed in this atom pass. All 51 prior database JUnit tests
remain; ten private-atom/fault cases were added. DbAtom reuses the existing exact
Java snapshot engine. Historical preimages and Git ancestry remain retained.

## Actual hosted proof

Exact head: `565bc02094731fbe0dcf3a2fff75706be8d0a60b`.
Run: `37186615402`; DB job: `111389766782`.

All **61 JUnit tests passed**, with zero failures, errors or skipped cases.
JaCoCo measured **685/687 lines (99.7089%)**, **240/241 branches (99.5851%)**,
**124/124 methods** and **14/14 classes**. The log explicitly reports that both
unchanged **0.99** coverage checks passed and the Maven build succeeded.

Artifact: `11296887801`.
ZIP SHA-256: `020a44aec4d5fed09d46f581db2c5ada326d01a55cf822310ea255fbdbb06725`.
The downloaded archive hash, tested-sha.txt, Surefire XML and JaCoCo XML were
independently checked. Percentages come from actual integer report counters.

The separate recipe job compiled and executed ten tests. The DbAtom exact
repair/composed-fixed-point case passed, together with eight other cases. One
metadata test incorrectly required the absence of the scheduler's provenance
marker. That failure is corrected separately by DbMark; it does not invalidate
the observed DB or individual DbAtom recipe result, and cannot be ignored when
assessing the whole focused recipe suite. Its new head needs its own execution.

## Local differential evidence

Strict Java 21 compilation, eight public/protected API descriptor comparisons,
three Unicode/path wire comparisons, four bounded corruption probes and private
size/move/text/edge fault probes passed. Missing-SHA failures were tested only in
an isolated 32MB JVM. An initial combined ZIP/no-provider experiment failed due
unavailable RNG; the published digest probe avoids those unrelated operations.
ZIP movement executes with the normal provider configuration.

## Remaining boundaries

The all-recipe suite at the same head executed 77 tests with 12 failures. Its
remaining attribution, fixture, introspection and assertion failures are listed
in `m3/tooling/migration-recipes/tasks/db-mark/ACTION_QUEUE.tsv`. A green database
or individual recipe case does not certify that suite or the full Maven reactor.

This report supersedes this task's earlier pending DB/JUnit/coverage statements
with exact-head evidence; it does not erase previous failed or pre-job receipts.
No transactional SQL, Java source parser, modified-JDK build, native/JNI parity,
jtreg or exhaustive backport completion is claimed. Canonical branches were not
merged, rebased, squashed or force-pushed.
