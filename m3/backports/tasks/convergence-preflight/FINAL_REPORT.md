# Normalize-before-absorb repair — execution report

## Files changed
Two existing MODULE-scope tooling owners are repaired:
`m3/backports/source_convergence_gate.py` and
`m3/backports/materialize_source_convergence.py`.

One additive Python regression file, one JUnit recipe test, a named recipe YAML,
three exact postimages and two pinned preimage fixtures form the executable repair
packet. Documentation, task/source/proof inventory and the full discussion-track
continuation ledger live here. Existing tests, POMs, JDK product source, installed
JDKs, branch permissions and canonical branches are not changed.

## Exact delta
Manifest parsing/semantic consistency uses one shared recognizer. The materializer
preflights every selected source/worktree/candidate/receipt before target mutation.
It rejects invalid row widths, booleans, aliases, selected symlinks and conflicting
receipts. Per-file staging/replacement avoids truncating canonical hard-linked
inodes. Exact reviewed postimages allow resumable and no-write completed replay.
Original V1 hashes/output columns for valid manifests remain unchanged.

This is a tooling correctness repair with explicit invalid-input tightening,
not a Java API change or a claim of arbitrary donor equivalence. Full-workspace
atomicity is NOT claimed: I/O failure can leave a partial reviewed postimage set;
the success marker is published last and replay verifies before resume.
Exclusive workspace ownership and immutable inputs remain preconditions.

## Executed proof
Four observable baseline failures were reproduced against the pinned original
scripts, in disposable fixtures. All four stop reproducing on repaired sources.
Final Python unittest execution passes42 tests: all12 original tests unchanged,
plus30 new boundary/replay/failure-injection cases.

The source diff, Python syntax, taskJSON, manifest ordering and pre/post SHA-256
were checked. Original preimage Gitblob identities matched connector reads.
No fake dependency stubs or hand-translated equivalent were used as runtime proof.

The repair is represented by the EXISTING
`M3Jdk21HashPinnedTextSnapshotRecipe("convergence-preflight")`, registered as
`com.m3.rewrite.backport.ConvergencePreflight`. The supplied JUnit executes actual
OpenRewrite replay/refusal/composition and named-recipe discovery when dependencies
are available. The existing Maven Python discovery includes the new test.

## Blockers and honest completion boundary
Maven is absent; dependency hosts cannot be resolved from the build sandbox.
OpenRewrite/JUnit/JaCoCo/full reactor execution is NOT claimed. No modified JDK
image, JNI parity, jtreg, JavaFX launch, complete donor normalization, full String
integration or exhaustive backport completion was executed in this repair.

ACTION_QUEUE.tsv covers the discussion's other workstreams and links their existing
owners. It is a continuation projection, NOT a competing canonical feature registry.
Old branch catalogue counts are not independently re-certified here.
This draft repair closes four tested control-plane defects, not the whole programme.

## Reproduce
From the patched checkout:
```
cd m3/backports
python3 -m unittest test_source_convergence_gate test_materialize_source_convergence test_convergence_preflight -v
```
The same tests are already discovered by the Maven backport module:
```
mvn -B -ntp -f m3/pom.xml clean verify
```
The Maven command is a REQUIRED UNEXECUTED gate here, not a successful run.
The recipe requires complete PlainText inputs for its existing target paths.
Use a fresh review workspace and preserve the source mirror; never promote
on an unexecuted recipe test or incomplete source inventory.
