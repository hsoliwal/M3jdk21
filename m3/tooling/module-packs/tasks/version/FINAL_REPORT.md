# Version repair and JDK proof continuation

One existing private header recognizer changes; two test files are added. The existing snapshot
engine is reused at kernel module scope. Standard JDK and existing public M3 names, all POMs and
previous tests stay unchanged. Invalid-input admission corrections are intentional, not an
assertion that every externally observed old result is preserved.

Freshly executed this turn: exact source identities; original archive/VM discrepancy reproduced;
Java21 compilation of actual production and corpus; 639 assertions each in JIT and interpreter;
41 old checks; identical javap public declarations. Input byte-preservation checks and real jmod
control are part of the corpus. These are assertions, not JUnit count or 99% coverage measurement.

The new proof-kit workflow was published and both initial push/PR runs returned failure with zero
jobs. Retrying the PR failed-jobs run returned403, cannot retry. These are not Maven/test failures
or successful execution. Local Maven is absent and direct dependency download fails DNS.

The new real recipe tests and JUnit wrapper remain unexecuted. The production API is unchanged;
only the archive admission predicate differs. No JDK java.base/HotSpot change, native ABI change,
new function framework, whole-JDK build, jtreg, JEP-completion, release or canonical merge is claimed.

The saved source-export Git history was not spliced into upstream. New source/template blobs are
identical and documented before materialization on the retained real upstream feature ancestry.

Recipe execution root: m3/tooling/module-packs/kernel. Named recipe: com.m3.rewrite.packs.Version.
Three targets: one inspector update and two explicit absent-before Java test additions. The existing
M3HashPinnedJavaSnapshotRecipe remains the engine; the old unpublished custom inspector recipe is
not duplicated into this repository.
