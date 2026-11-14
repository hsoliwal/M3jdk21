# MapGuard executed report

## Changed files and exact delta
One existing java.base implementation changes: MIndexMappedStringBacking.
The selected text/byte commit count is bounded by committed bytes/minimum record
size before mapping or allocating its row arrays. Failed acquisition closes each
opened channel, retains the primary exception and suppresses cleanup failures.
No public/protected descriptor, successful format, existing String operation,
existing test, POM, dependency, native source or coverage threshold is changed.

The existing mapped-backing workflow retains its old test, permissions, actions
and build commands, and adds MapGuardTest to its path filter and TEST selection.
Two named configurations reuse the existing Java and text snapshot engines.
Five genuine parser/scheduler JUnit cases cover exact replay, refusal,
serialization, existing-source preservation and fixed points. The jdk22 resource
prefix is required by the legacy engine's name fence, not an upstreamJDK22 claim.

## Executed verification
Pinned sources were fully read and byte-reconstructed against Git identities.
Diff/static checks preceded strict Java21 compilation of actual owners/tests.

The original source reproduced three failure groups: impossible text count OOME,
impossible byte count OOME, and a failed-open file-descriptor leak. Each ran in a
separate32MB JVM. The original format/Unicode/alias test passed before the change.

With repaired source, the unchanged original test passes and the new actual
jtreg main passes78 assertions in normal and interpreter VMs. Empty/minimum
records, CRC slot recovery, selected corrupt commit refusal, source immutability,
close/reopen, exact rejection and cleanup suppression are exercised.
Linux descriptor probes cover16 outer failures,16 inner failures and4 nested
acquisition/parse failures. No GC is required to release those channels.

Three javap public/protected descriptor surfaces are byte-identical. The
reproduction script was then executed against a fresh four-source subset and
again passed both runtime modes while confirming source hashes stayed unchanged.

These are tests of the actual JDK-internal classes through a narrow, isolated
java.base package patch on Debian OpenJDK21.0.11. String.class and HotSpot are not
transplanted. Direct execution of an actual jtreg main is NOT full jtreg harness
execution and is NOT a full rebuilt M3JDK21 image.

## Unexecuted gates and programme state
Maven is absent and Maven Central DNS resolution failed. Recipe Java syntax
parsed successfully, but actual Maven/OpenRewrite/JUnit/JaCoCo is not claimed.
No full native/JDK build, all-tier jtreg, supported-platform or exhaustive
backport completion is claimed. Workflow run37192357698 had zero jobs and a403 retry
response; neither establishes a source-test verdict.

The latest inspected source already contains corrected JNI Handle assignments
and the narrowed backing close declaration. This packet does not repeat those
edits or claim their entire historical contract/product admission is proven.
ACTION_QUEUE.tsv preserves their remaining proof obligations and all other
discussion tracks under their existing owners.

## Reproduce
From a patched checkout:
```
bash m3/tooling/migration-recipes/tasks/map-guard/verify.sh JDK21 REPOSITORY FRESH_OUTPUT
mvn -B -ntp -f m3/tooling/migration-recipes/pom.xml clean verify
make test TEST="test/jdk/java/lang/String/MIndexMappedStringBackingTest.java test/jdk/java/lang/String/MapGuardTest.java"
```
Only the first command was executed here. It stages only the two layout-independent
owners to avoid implicit compilation/loading of modified String or VM-bound classes.
The latter two remain required hosted/product gates. Inputs must be trusted and
immutable; successful mapped-view lifetime and external-file-mutation requirements
remain unchanged. Large valid images can still require substantial heap.

## Artifact paths
This directory contains the six mandatory M3 artifacts, scoped inventory,
provenance and raw execution logs. The delivery bundle adds the verified Git patch
and exact original four-file reference subset. No installed JDK or canonical
branch was replaced, rebased, squashed, force-pushed or merged by this execution.
