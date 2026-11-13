# Exact restored candidate evidence

The executed candidate is `69667e3f1ce673aaded2ad93e4ae2ecf6c59668b`, tree
`406f4bd973d40c5c8ce1782e01745307aab008ff`. Its 25 production runtime files are
byte-identical to the public #6 donor. The separately preserved
`../20261002-rerun` receipts describe the earlier #6 reproduction, not this head.

`build-driver.log`, `source-commit.txt`, `source-tree.txt` and `image.sha256` bind
the incremental `make images JOBS=4` run to the complete matched Linux fastdebug
image. It reused the prior native configuration and compiled objects. Bootstrap
Java and the installed system runtime were not replaced.

`acceptance.json` records the completed focused subcases and the remaining gate
runs after an observed WSL reboot interrupted the first driver. The old process
was proven gone before resuming. API, native/JVMTI, GC, protocol rejection, startup
restrictions, weak admission, two-process read-only mapping and allocation are
scoped evidence. The five API modes produced the same stock semantic digest.
`allocation-summary.json` parses six original diagnostic outputs, retaining
their hashes and reporting the measured workload only. It does not establish
general speed, a total retained-memory budget or full Java conformance.

`jtreg-results.json` distinguishes completed upstream tests from pending tests.
Default scope has 86 Passed results; the smaller enabled scope has 16 Passed.
The two enabled 4GiB StringJoiner cases remain pending on this candidate.
Original testcase assertions and explicit heap requests are preserved. Any
resource cancellation and resumption remain explicit. The donor's two enabled
StringJoiner failures are still historical failures; pending new runs do not
turn those failures into passes.

`source-equivalence-mergeability.json` checks the latest observed master
`45f546ff5bcb06a1b2604f14baf998785d98d9a1`. The read-only merge tree preserves 143
untouched incoming paths, including the new internal backing classes; the one
manifest overlap merges cleanly. That resulting merge tree was not built or
tested by these candidate receipts.

Generated private owner classes, native test libraries and fixture arenas stay
local and are ignored. Public logs and hashes identify the exact private #7498
fixture source without publishing its implementation. Artifact hashes in the
execution receipt include those local artifacts for reproduction.

Structured autoreview did not reach its engine: both the original branch review
and the bounded donor-relative attempt failed during the helper's source-tree
fingerprinting with `MemoryError`. No structured review pass is claimed. Manual
owner review checked the five portability edits, source equivalence, preserved
test expectations, rollback and latest-master preservation. Further review and
the documented acceptance gates remain necessary before integration.
