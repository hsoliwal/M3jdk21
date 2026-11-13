# Additive exact-source fold M3JDK21 PR #2252 — Swing viewport atom tests

First parent: e1bf3da34dca17b60740848f3a5c4585a30a0667
Original donor PR #2252 head: 2112fe333575286bb387ed955f17074c666e0d08
PR: https://github.com/hsoliwal/M3jdk21/pull/2252
Patch: https://github.com/hsoliwal/M3jdk21/pull/2252.patch

Git source preflight: PR #2249 already contributed three files (`make/m3/tooling/atom-tests/CENSUS_UNION.tsv`, `census_union.py`, `literal_census.py`); all three original #2252 Git blob IDs exactly equal current receiver. Original #2252 PR has 80 files, all additions. Remaining 77 paths were absent on receiving head and retained by exact immutable donor Git subtrees:
- `make/m3/tooling/jdk-superset/viewport-swing` -> `9ef773a7319ab1832e8f3b40f87196c25e04532c`: 51 original files, 100644.
- `test/jdk/sun/swing` -> `131d559f3088abffd93af8cc98c7a5dac9ab9536`: 26 original files, 100644.

Root and recursive subtrees and full GitHub PR files checked for source Git blob SHA, type and mode. No preexisting file is overwritten. Require final two-parent commit with donor as second parent, and 77 unique additions plus one receipt; zero modified/deleted/renamed.

**DRAFT/HOLD** — no receiving-head Java21/jtreg/JUnit/JIT/HotSpot/JNI, source API/ABI or build/CI qualification has been executed. Do not promote to master until gated.