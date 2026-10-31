# Executed CI repair: finite diagnostic results

The eight JUnit methods in `JccCiRepairTest` passed under the declared OpenRewrite
8.17.1 and JUnit 5.10.2 artifacts using Temurin JDK 21+35 and Maven 3.9.9.
The retained text recipe produced exactly three files; replay produced zero
changes. Those actual OpenRewrite results match the separately executed sealed
installer outputs byte for byte. The two complete repaired A3 test classes then
compiled with `javac --release 21` and the actual Surefire test classpath.

The first run is retained under `evidence/v01`: all eight methods ran, with one
failure in the new metadata oracle. OpenRewrite adds `RecipesThatMadeChanges`
provenance to changed sources. V02 preserves every pre-existing marker, checks
its identity, and explicitly requires the expected engine provenance. Its
positive inputs include non-empty `BuildMetadata`. Every other identity and
metadata check remains. All target recipe resources, original A3 test methods,
production owners, installer and dependency guards are unchanged by this oracle
correction. The failed and corrected test bytes, patch and receipts are retained.

The six Python installer methods passed with 33 refusal calls. Their lifecycle
record includes three apply writes, zero fixed-point writes, three rollback
writes, and three replay writes. This evidence was reused without rerunning
because all installer inputs remain byte-identical in V02. The foundation
environment propagation test explicitly mocked acquisition, Java and Maven; it
is a wiring control, not an execution of the GitHub-hosted workflow.

## Limits and remaining obligations

- The focused bootstrap ran `mvn -o ... test`. Whole-module verification and its
  unchanged coverage gates were not run by this lane.
- Both original repaired A3 classes compile; their four original behavioral test
  methods require their own complete fixture closure and were not executed here.
- The exact artifact cache is a finite diagnostic snapshot. Native, library-source
  and plugin-source custody remain separate admission obligations.
- The GCC 10 package pins are unchanged. Their failed CI acquisition remains an
  unresolved toolchain obligation; no native or rebuilt-JDK success is claimed.
- `M3ScopeInference` was already fixed in the tested baseline and is unchanged.

The executed baseline is `b71ee5bf88398fb80961c13db03e8c675fb445ce`. The separately
recorded tree-identity comparison at `87590cb96fb0e2dc0f88fae8e01957f7179cd255`
found all nine selected inputs unchanged and all 22 new candidate paths absent.
That comparison is applicability evidence, not a relabelled execution on a new
revision. The publishing integration must verify its own preimages again.
