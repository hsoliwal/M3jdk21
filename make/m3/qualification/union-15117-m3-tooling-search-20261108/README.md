# Union: com.synexia #15117 onto make/m3/tooling/search

#15117 (`m3/sorted-int-adjacent-repeat-20261101`, head `81eadeefa85a`) edited the base crate
`m3jdk-sorted-batch-bounds-20261101` in place. It was fold-dropped from com.synexia develop together with that crate.
It branches from the same base as the received stack #15144 -> #15178 (receipt #2793), so `git merge-file`
conflicts in the Java and C sources. Its two features are orthogonal to that stack:

1. **Comparison budget for merge mode.** A nondecreasing batch of at least 128 keys uses the linear merge scan
   only when `N <= Q * bitLength(N)`. Otherwise it stays on binary search. Java: `mergeWithinBudget`. C:
   `m3_merge_within_budget`, with the multiplication widened in both.
2. **Adjacent duplicate reuse in binary mode.** An exact repeat of the previous key reuses its bounds. Java
   compares with `keys[i - 1]`; C keeps an invocation-local previous-key memo across 256-key windows.

## Resolution (`resolve15117.py`)

| File | Conflict | Taken |
|---|---|---|
| Java | 1 | master's empty-interval block, then #15117's budget guard on the merge-mode condition |
| Java | 2 | #15117's duplicate check around master's lower-bound-reusing search |
| C | 1 | master only: #15117's zero-key and empty-range blocks are #15108's, superseded by #15144/#15178 |
| C | 2 | #15117's memo around master's lower-bound-reusing search (cursors now hold absolute bounds) |

The self-test and module README are #15117's blobs, taken whole because master still has the base blobs. The
README credits the empty paths to #15108. In this module they come from #15144/#15178, which have the same behaviour.

## Gates (`GATES.tsv`, runner `gate.sh`)

The three step probes from the received stack ran against the union Java + C, with no edits. #15117's self-test
also ran. The public API is identical to master. Two mutants each break one #15117 feature, and both are rejected.

`SparseProbe.java` / `sparse-run.sh` measure the case the budget exists for: 128 sorted keys spread over a
16,777,216-element range. That is 6,348,258 ns with master's code and 36,309 ns with the union, with the same
checksum. One machine and nine timed calls, so the figures are indicative only. The bundled dense benchmark
(n=512, no duplicates) shows no difference beyond noise.
