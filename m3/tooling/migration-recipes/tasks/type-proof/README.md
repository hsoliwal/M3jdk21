# Type proof — continuation of M3JDK21 PR 89

Pinned input: c7a47ead1ea406ff79e5feca0d7c59005ba8d511.

## Remaining failure and contract

PR 89 moved withId to the end, but inherited Tree.withMarkers still erases the fluent
receiver to Tree. The subsequent SourceFile.withFileAttributes call cannot compile.
Reuse the typed assignment sequence already present in the repository's canonical
com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe. No wrapper, cast, functional
interface, dependency, public API or OpenJDK product source is introduced.

Keep the current metadata order: path, markers, file attributes, charset, BOM,
obsolete-checksum removal and identity. Only the first three fluent links become
SourceFile-target-typed assignments. The existing two PR-89 tests are unchanged:
their previously unsatisfied metadata assertions now describe the actual output.
Add actual parser/scheduler metadata and refusal tests, not SDK stubs.

## Recipe and proof authority

Update the existing m3-jdk21-hash-pinned-sourcefile-chain crate's reviewed output
and hash. Its original preimage stays immutable. Compilation of the corrected recipe
is the minimal bootstrap; then the existing snapshot engine must prove original
source -> reviewed source -> fixed point. The existing long recipe identifier is
retained for compatibility. No duplicate public TypeFix recipe is added. The existing Java repair runs from the migration-recipes module root; the separate `com.m3.rewrite.backport.ProofIO` text recipe runs from the repository root and owns only the workflow and this document.

The PR uses two additive commits: recipe resources/docs/tests first, exact matching
implementation second. No master/develop write, rebase, force push or automatic merge.

The new read-only workflow performs full migration-recipes clean verify without
lowering coverage thresholds or excluding any tests. It records real JUnit and
JaCoCo outputs and saves a source-only input archive for reproducible continuation.
A source archive or queued workflow is not build/test success. The workflow never
captures Maven settings, tokens, private keys or the Git credential configuration.

Standalone syntax checks are not framework compilation. Full modified-JDK builds,
JNI constructor repair, close-contract review, jtreg, all compatible backports,
module packs and platform acceptance remain separate obligations.
