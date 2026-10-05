# A3 metadata-copy followthrough

The earlier initial publication report is historical, not the latest verification state.

The actual PR111 run `37251872807`, job `111581002095`, reached the complete current M3 reactor at `b0c2e495b8465dc802ef309c06810844b04e08e9`. Module-pack `verify` and IndexDB passed. Migration recipes failed production compilation at A3 line204: `withFileAttributes` was resolved on `Tree` after the generic `withMarkers` call. A3 tooling and backport DAG were consequently skipped by Maven; that is not successful verification.

Artifact `11321049911`, ZIP SHA-256 `4ec6f65f360bd830500bd22316714af7367b3779614bd52b9ace3c7577e225ee`, contains the exact current complete source and dependency cache. Its internal hashes were checked. It supersedes the earlier missing-Maven/current-source limitations, not earlier source history.

Approved leaf: complete the existing A3 SourceFile-typed local assignments through every metadata copy. Preserve setter order, source guards, parser, public signatures, all authority methods, before/after source resources and checksum invalidation. Keep all current tests, strengthen their concrete metadata inputs, and capture this exact compiler repair as a reusable crate of the existing hash-pinned Java recipe rather than another transformer. Scope includes that owner/test, the additive crate resources/alias and recipe regression tests only.

Verify the exact source diff and static checks, compile using the real Java21/OpenRewrite dependencies, execute the existing A3 tests and new replay/refusal tests, then rerun full M3 reactor `clean verify` without excluding failing tests or weakening existing thresholds. Independent failures remain typed blocking work. This is neither native VM product proof nor full-JDK completion.
