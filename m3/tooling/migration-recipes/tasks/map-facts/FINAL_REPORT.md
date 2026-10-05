# MapFacts — publication and independently repeated verification, 2026-10-05

## Files changed

Resume the previously local-only MapFacts candidate on the exact still-current
PR99 head `3a433416ae7981301844682896e5aefc6f0f8901`. The runtime owner, product test,
recipe fixtures, six JUnit tests, verification script and 28 sealed inputs are
byte-for-byte those in the saved delivery. No replacement implementation was
invented. Documentation and evidence here describe this publication run rather
than relabelling earlier LOCAL_ONLY reports as current status.

The only existing product owner modified is
`src/java.base/share/classes/jdk/internal/mindex/MIndexMappedStringBacking.java`.
It validates six persisted scalar facts in the existing CRC traversal and checks
alias facts against already validated preceding rows. Existing MapGuard allocation
bounds, exception cleanup, original tests and signatures remain intact. Invalid
metadata is deliberately rejected; no claim of equivalent corrupt-input behavior.
The existing jtreg workflow adds MapFactsTest beside both preceding tests.
No native source, String.class, JDK installation, POM, dependency or threshold changes.

## Exact verification executed in this run

- Archive checksums and PR99 source preimage identity verified against GitHub.
- Original patch forward/reverse checks and whitespace checks passed in isolated exports.
- Shell syntax and YAML parsing passed; six active source/workflow manifest rows
  matched their exact postimages and actual materialized targets.
- Five real Java sources compiled with Java21 and `-Xlint:all -Werror`.
- Original mapped-format test and MapGuardTest passed on the pinned before state
  and on the candidate; MapGuard reports 78 assertions in default/interpreter modes.
- The twelve scalar/alias fact probes ran against the baseline. Eleven reproduced
  missing validation; the old alias-hash cross-check already rejected the twelfth.
- MapFactsTest passed with 370,960 assertions, 158 fixtures and 24 Linux descriptor
  checks per run in default, interpreter and noncompact-String modes.
- Three public/protected javap descriptor outputs matched byte-for-byte before/after.
- The original source-sealed verification script ran again from a fresh output directory.

Assertion totals include per-unit/slice checks, not independent JUnit case counts.
These runs patch only the isolated layout-independent backing package, not String
layout or HotSpot. They are not full JDK builds or jtreg harness execution.

## Exact blockers and completion boundary

Maven remains absent; the current local `mvn -version` attempt exits 127. Maven
Central DNS resolution also fails. The six new OpenRewrite/JUnit tests and the five
preceding recipe tests have not run here. Coverage remains unmeasured. Full lint,
reactor, configured JDK build, jtreg, matched JNI/GC/JIT/CDS and platform acceptance
remain required. No gate was disabled or weakened. A new review branch does not
constitute canonical promotion, and no master/develop update or history rewrite is
part of this delivery.

Unlike the previous run, GitHub write actions are available and this candidate is
being published using the real upstream parent, never the local export ancestry.
Exact-head source readback and hosted CI observations are recorded on the PR after
publication. Do not infer source-test success from a jobless CI failure.

## Reproduction and artifacts

Run from a complete checkout with the matching task inputs:

```sh
bash m3/tooling/migration-recipes/tasks/map-facts/verify.sh JDK21 REPOSITORY FRESH_OUTPUT
mvn -o -B -ntp -f m3/tooling/migration-recipes/pom.xml verify
make test TEST="test/jdk/java/lang/String/MIndexMappedStringBackingTest.java test/jdk/java/lang/String/MapGuardTest.java test/jdk/java/lang/String/MapFactsTest.java"
```

Only the first command ran successfully here. The latter two require the complete
approved build environment. Current raw proof is in `evidence/publication-20261005/`.
The prior delivered archive remains historical evidence, identified in PROVENANCE.tsv.
CONTINUATION.tsv retains the broader unfinished JDK work without shrinking its scope.
