# M3JDK21 text parity execution report

Programme status: NOT COMPLETE. This is a source-verified and runtime-tested JDK
backing leaf, not full JDK acceptance. Canonical master/develop are not modified.

## Files changed
One existing product owner: jdk.internal.mindex.MIndexMappedStringBacking.
Recover the existing MapFacts fixture and fact guards from the user's saved
patch, and add MapUtfTest. Reuse the existing Java/text snapshot recipe engines;
MapGuard, MapFacts and MapUtf converge on one final owner without replacing any
original preimage/test. The JDK mapped-backing workflow only adds test paths and
test selection; actions, permissions and build commands are retained.

## Exact delta
Validate six text facts against the scalar, validate aliases against admitted
owners, and bind UTF8 bytes to the UTF16 payload using the existing UTF8 encoder.
A 4096-byte scratch buffer and encoder are reused across scalars in one open.
No public signature, retained field, native ABI, mutable payload, giant temporary
array, alternative String class or private Synexia implementation is introduced.
Unpaired UTF16 is preserved; only its UTF8 projection uses the existing Java
codec replacement. CRC/FNV consistency alone is not encoding equivalence.
Malformed input refusal intentionally changes; valid format remains preserved.
The added admission traversal is not claimed to improve performance.

## Exact verification executed
Linux x86_64; installed Debian OpenJDK21.0.11. A source subset was reconstructed
from exact Git blobs and the saved patches, NOT a complete repository checkout.
No installed JDK files were modified. The actual two backing sources were compiled
as a narrow java.base package patch; this is NOT a replaced String/HotSpot image.

- All 16 CRC-valid, correctly framed bad byte projections were accepted by the
  prior MapFacts kernel. Each failed the new regression as expected before repair.
- The valid UTF8 corpus passed before repair.
- Original mapped backing test and MapGuard passed unchanged after repair;
  MapGuard reported 78 checks in default and interpreter modes.
- MapFacts reported 370960 assertions / 158 fixtures / 24 failed-open FD checks
  per run in default, interpreter, noncompact and C2-focused modes.
- MapUtf reported 985026 assertions / 187 fixtures / 16 failed-open FD checks per
  run in those four modes. There are 171 valid fixtures and 16 malformed fixtures;
  these are NOT 985026 independent JUnit tests or a coverage percentage.
- Valid corpus includes the 65536 UTF16 unit alphabet, seeded generated text,
  surrogate pairs/unpaired units and 4096-byte output boundaries.
- Four separately compiled mutants were rejected: missing binding, missing tail
  validation, missing byte equality, and wrong malformed-input policy.
- Three public/protected javap surfaces are byte-identical before/after.
- Diff whitespace, YAML/shell syntax and recipe-manifest byte checks passed.
- Seven new real OpenRewrite/JUnit cases and six recovered cases are authored;
  the original five MapGuard cases remain unchanged. Parser-only javac accepts
  recipe test syntax. The actual scheduler/JUnit tests have NOT executed.
- The source-sealed verification entry point was executed from fresh output and
  reran the preceding gates plus UTF8 tests. Source hashes remain unchanged.

## Exact blockers and failures
Maven invocation failed with exit 127: mvn not found. Shell DNS resolution for
GitHub and Maven Central failed earlier in this execution environment. The full
checkout/dependency closure is unavailable locally. No Maven, Checkstyle, PMD,
SpotBugs, Spotless, JaCoCo, 99% coverage or full-reactor success is claimed.
GitHub publication is separate from build acceptance. Missing or jobless CI
cannot certify compilation or tests. The candidate must remain draft. TODO.tsv preserves the existing programme
packet IDs; NOT_CLOSED_HERE does not assert that an implementation is absent.

Required native configure/make/images, jtreg harness, matching modified JVM,
JNI/VM/StringTable/intrinsics/GC/CDS/platform suites remain open. All-JEP and
non-JEP absorption, collections/concurrency, compiler/tooling/partial-AST,
parser/DB integration, naming/IOP and JavaFX/framework tasks remain tracked
under existing work packets, not redefined as completed by this text patch.

The parser still assumes managed immutable files; external rewrite/truncation,
all unvalidated header fields, cross-platform mapping, synchronous unmapping
and all other wire corruption categories are not proved by this packet.

## Reproduction
Run in a matching patched FULL repository or exact supplied source slice:

    bash m3/tooling/migration-recipes/tasks/map-utf/verify.sh JDK21 REPOSITORY FRESH_OUTPUT

That is the executed kernel gate. In the actual complete repository, also run:

    mvn -o -B -ntp -f m3/tooling/migration-recipes/pom.xml verify
    make test TEST="test/jdk/java/lang/String/MIndexMappedStringBackingTest.java test/jdk/java/lang/String/MapGuardTest.java test/jdk/java/lang/String/MapFactsTest.java test/jdk/java/lang/String/MapUtfTest.java"

The latter commands remain unverified and need the normal complete native
configure/build/test prerequisites. Existing build scripts were not bypassed.

## Artifacts
This task directory contains the mandatory six reports plus scope/provenance,
source seals, verification entry point and raw receipts. The delivered archive
also retains the earlier MapFacts patch/report as historical evidence, clearly
separate from this execution's newly observed results. Local source-export Git
history is never a replacement parent for the canonical OpenJDK history.
