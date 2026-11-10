# MapGuard: mapped-source admission and failed-open ownership

## Existing owners and scope
Baseline: `7ead93addf4a7b9614d4ad34c8dd02f4b93b9c36`.
Reuse `jdk.internal.mindex.MIndexMappedStringBacking` and the existing Java snapshot
recipe. Do not add another parser, storage engine, function wrapper or API.
The existing interface and successful mapped-view lifetime remain unchanged.
This is an explicit internal parser/resource-safety repair, not a claim that
rejection of formerly dangerous corrupt inputs is behavior-identical.

## Contract
Before allocating row directories, bound the selected commit's row count by its
committed byte span and the minimum record header size: text120, bytes32.
Do not substitute an older commit when the selected CRC-valid newer commit is
semantically corrupt. Do not reject valid legacy/current-format images.
Reject impossible geometry with the existing corrupt-input exception family.

Construction transfers channels to the instance only on success. Every successfully
opened channel must be closed when later initialization fails, including a nested
byte-store failure. Preserve the primary failure and attach cleanup failures as
suppressed exceptions. Do not narrow the public close signature, add runtime
dependencies, or forcibly unmap successful views.

## Atoms/pattern/IOP
Existing readCommit is the CommitRecognizer/GeometryAdmission participant.
Existing outer/ByteStore constructors are the acquisition and ownership-transfer
participants. One private cleanup operation may reuse AutoCloseable if its narrow
role needs sharing; no new public functional type. Existing parse/view operations
remain the canonical implementation.

## Tests and proof order
Pin and fully read both production files and existing jtreg test before edits.
Reproduce the original count-allocation failure in a32MB Java21 child VM.
Retain and execute the original mapped-format, Unicode, alias and view test.
Add a jtreg main covering impossible text/byte counts, boundary geometry, commit
selection, empty valid images, reopening/close state, and channel failure cleanup.
Use real files and an isolated test module patch; do not replace String.class or
the VM, and do not represent the host runtime as a rebuilt M3JDK21.

Diff -> static -> strict javac -> direct execution of actual jtreg main classes.
Use the existing recipe's exact pre/postimages and real JUnit application/refusal/
fixed-point tests. Execute Maven/jtreg/full product gates when available; record
unexecuted gates separately. No threshold, linter, test or workflow weakening.

## Limits
The caller still guarantees immutable committed files; read-only mappings cannot
defend against malicious external truncation. Heap directories remain eager and
can legitimately exhaust memory for a large valid image. Count admission bounds
corrupt metadata by input size; it is not a general memory quota.
Channel cleanup does not promise synchronous unmapping of unpublished buffers;
mapped memory remains managed by existing JDK cleaners. Successful close/view
lifetime, concurrent-close guarantees and the public format remain unchanged.

## Existing recipe execution
`com.m3.rewrite.backport.MapGuard` configures the existing
`M3Jdk21HashPinnedSnapshotRecipe`, blob33783749e46474f2bdc68103e189fe2907a61fa9.
Its legacy crate-name fence requires a jdk22..27 prefix, so the resource directory
is `jdk22-map-guard`. This is a naming constraint, NOT a claim that this local
mapped-store repair is JEP work or was imported from OpenJDK22.
The manifest owns one production file and one additive jtreg test. Execution
root is the repository. JUnit recipe proof runs from the existing Maven module.

## Existing CI integration
Extend only the existing mapped-backing workflow's changed-test path filter and
TEST selection. Preserve its prior test, permissions, build commands and actions.
A separate `MapGuardWorkflow` configuration of the existing PlainText snapshot
engine owns this exact workflow edit. It is not a new CI system. Product build
acceptance still requires that workflow or equivalent exact-source execution
to actually run; a merge or jobless Actions failure supplies no such evidence.
