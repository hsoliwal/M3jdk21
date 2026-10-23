# Audit of the whole-file "theirs" resolutions (2026-10-23)

23 PRs of this superset had their source conflicts resolved by taking the PR's whole files after the superset-side
candidate failed the module gate. `AUDIT_THEIRS.M3jdk21.tsv` recomputes each of those merges (merge-tree of the side
commit's two parents, rename detection off) and counts, per conflicted source file, the lines the superset had added
since the merge base and that the landed resolution no longer contains (`superset_lost` of `superset_added`), and the
same for the PR side.

Result: 68 of 69 conflicted source files lost superset lines, 3,703 lines in 23 PRs. The largest: PR 1559
(src/jdk.compiler M3ASTPC.java 628 of 629), 1611 (m3/file 530 lines over five files), 1602 (parser module 455), 1441
(java.util.concurrent progress-monitor additions and java.util.Strings, 266), 1332 (M3TreeMap, M3LinkedHashMap,
M3LFUCache, M3SlotMap, 319). The cause: the superset head's own compile errors (m3/collections, m3/file) made every
superset-side candidate fail, so the gate accepted the only candidate that compiled.

Repair: `repair_theirs.sh` (Synexia recipe-crates/m3-superset-merge-20261023/tools) re-merges each damaged file at
hunk level (superset side and PR side on their merge base), replays the restoration on the current head, and gates the
candidate against the head's own errors (new errors only). Its ledger is `REPAIRS_20261023.tsv` next to this file once
it has run. Until then, every RESOLVED row of `SUPERSET_20261023_RESOLUTIONS.tsv` whose note says `(theirs;` is a
resolution that dropped superset work.
