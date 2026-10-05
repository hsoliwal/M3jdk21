# A3 replay: execute and repair the existing recipe

Pinned repository base: db2017c1aa4aace99543417fba79e3f7a307c26b.

The latest proof-kit artifact (run 37251872807, artifact 11321049911) preserves real Maven 3.9.16, dependencies and full source at b0c2e495b8465dc802ef309c06810844b04e08e9. Its ZIP SHA-256 is 4ec6f65f360bd830500bd22316714af7367b3779614bd52b9ace3c7577e225ee. Its reconstructed source tree is a6977f429d9e1482018391d9fb8bd28ad4b40d36. This is not silently treated as the complete current-master tree.

The hosted full M3 reactor failed in M3A3SerialFileWorkRecipe at withFileAttributes: the preceding generic withMarkers invocation inferred Tree. The complete current owner and test were read; they match the selected archive files. The same real Maven compiler failure was reproduced locally before edits.

## Scope and contract

Repair the existing source-sealed A3 recipe and its tests, rather than adding a new transformation engine. Retain public signatures, all source manifests and exact reviewed templates, parser ownership, existing authority flags, non-target preservation, duplicate/drift/missing-owner refusal and the second-run fixed point. Preserve source ID, path, markers, file attributes, charset and BOM; discard only stale checksums after replacement.

If real scheduler tests expose another defect in this same recipe's generation/visit lifecycle, fix that bounded lifecycle and strengthen refusal regressions without expanding mutation authority. A generated exact ABSENT postimage is distinct from a required preimage that went missing. Scanned source must still match the scan at visitation.

Existing tests, dependency versions, POMs, exclusions and coverage thresholds are not to be weakened. Run real Maven compilation, targeted recipe JUnit, then the complete available reactor. Distinguish exact-current results from checks on the preserved earlier archive.

## Atom and pattern roles

The existing scanner, source-identity guard, replacement/metadata operation, generator and fixed-point check remain the semantic atoms. ScanningRecipe remains the lifecycle/pipeline owner; no lambda/function framework or public runtime name is introduced.

## Remaining programme

This is a build/replay closure leaf in the whole-JDK programme, not completion of all compatible JEP/non-JEP/fork backports, M3 String/JNI/GC/CDS integration, modules or platform acceptance. Preserve the existing tasks/map-utf/TODO.tsv denominator. No canonical branch writes, force-push, squash, rebase or automatic merge.
