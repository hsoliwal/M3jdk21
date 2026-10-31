# A3 source-type repair: initial publication state

## Files changed
The existing M3A3SerialFileWorkRecipe and its existing test, plus task evidence.
No product source, snapshot image, dependency, POM or workflow is changed.

## Exact delta
Move `candidate.withId(file.getId())` into assignment to the existing SourceFile
local before the unchanged ordered metadata setter chain. This restores generic
target typing without casts or new APIs. The existing exact-preimage/fixed-point
test now checks original ID, path, markers, attributes, charset and BOM as well.

## Exact verification executed
The baseline hosted workflow successfully downloaded the exact PR103 tree,
validated its pinned JDK21 archive and ran real Maven. Production compilation
then failed at recipe line202 because withSourcePath was resolved on Tree.
The full targeted owner and test were read. The proposed difference is limited
to two replacement lines and nine additive test lines. Corrected hosted build,
JUnit and coverage have not yet run at this publication checkpoint; later PR
comments/job receipts must be read before treating this state as current proof.

## Exact blockers
Baseline failure is a source compile error, not missing Maven on the hosted
runner. No test was reached there. The local environment still has no Maven.
Successful future compilation will not by itself prove recipe execution or
full-JDK image acceptance. Existing full-JDK backlog remains unchanged.

## Artifact paths
This directory contains the six required reports and task packet. Provenance
binds the parent source/test blobs and the actual baseline workflow failure.
No master/develop/PR103 writes or history rewrite are performed.
