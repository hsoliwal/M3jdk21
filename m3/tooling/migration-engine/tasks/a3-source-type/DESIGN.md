# A3 SourceFile type repair

Parent: 7fffe6f14ea0073126e57a27ab7c2e333b5b8883 (PR103).
Observed hosted run 37212552006, job 111466504051, fetched the exact parent and
ran Maven 3.9.16 with Temurin21+35. Production recipe compilation failed at
M3A3SerialFileWorkRecipe.java:202: `withSourcePath(Path)` was looked up on Tree.
Tests never started. This is a source/compiler defect, not missing Maven or a
jobless infrastructure failure. The earlier local-Maven limitation remains a
separate historical/local fact.

The source owner (blob d92844ab899f6a1023d1b76b9e3543823ed701ee) and its complete
existing test (blob fd07a5848e7e41b9e00890c43928c7c315f67bc1) were read before
change. Generic `Tree.withId` in a receiver chain loses the target SourceFile
inference context. Assign its result to the already SourceFile-typed candidate,
then retain the existing ordered metadata setter chain. No cast, parser, API,
postimage, authority, workflow, dependency or threshold change is required.

This is an improvement to the existing reusable recipe, not a parallel recipe
engine. Strengthen its existing exact-preimage regression with ID, source path,
markers, attributes, charset and BOM preservation assertions. Keep every old
assertion, fixture and refusal test. The existing source-sealed recipe engine
already uses the same typed-assignment mechanism.

Scope: the one recipe owner, its one regression test and this task directory.
Canonical master/develop and PR103 are read-only. No rebase, force, squash or
merge. Verify diff/static review, hosted compiler, original and new recipe
checks; missing/failed gates block promotion. A green compiler alone does not
close full JDK/native/coverage acceptance. The broader TODO ledger remains
under tasks/map-utf/TODO.tsv, not replaced or narrowed here.
