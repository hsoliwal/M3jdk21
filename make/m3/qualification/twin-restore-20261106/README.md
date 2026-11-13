<!-- SPDX-License-Identifier: Apache-2.0 -->
# Twin restore 2026-11-06: make/m3/tooling and .github (com-synexia-45 half of TWIN_CONTENT)

The input is `TWIN_CONTENT.tsv` from the loss-guard restore (#2517, com-synexia-0e). Each row is a retained twin whose declarations its live file lacks. This half covers 52 rows under `make/m3/tooling` and `.github`.

RETAINED_AUDIT (`retained_audit.py`, line-multiset containment against master) gave:
- 10 RETAINED_SUPERSET: the retained copy holds every live line plus more.
- 42 DIVERGENT.

Restored here (`RESTORED.tsv`):
- 7 `.github/workflows/m3-bulk-*.yml` (pr2216): the full workflows with the recipe-receiver, patch-smoke and jdk-build-jtreg jobs. Every repository path they reference exists on master.
- `M3ASTAuthoritativeModels` (pr1559, +119) and `M3LexerProfile` (pr1788).
- `M3TokenStream` (pr1788). It was DIVERGENT, but RETAINED_3WAY against pr1788's merge-base 0631c8648210 returns the retained copy exactly. Its 2 "live-only" lines were base lines that pr1788 changed. M3LexerProfile needs its `copyView()`.
- `M3CollectionsPackTest` (pr1608) and its helper `jdk.m3.util.M3PrimitiveSelectionWordContract`, which was missing live and retained only under pr1608.

Gates (`GATES.tsv`, javac --release 21, live vs restored):
- Parser pack main: the same 24 errors before and after, from external dependencies absent on this host. `errdiff.py` shows no new error.
- Collections pack tests: 0 errors before and after.

Not restored: 41 DIVERGENT rows, plus pr1608 `build.py` (DIVERGENT) and `test_build.py`. They go to RETAINED_3WAY next and are restored only where the merge is clean.
