# Resume: read the canonical name map before modifying code

**Migration is partial.** Read `../docs/name-mapping.json`, its schema, `COVERAGE.md`
and the controlling `../docs/shared-atom-concatenation.md` first. Do not inherit a
completed-state claim from this file or from merged documentation PR #7.

Implemented/tested locally: the six-source-owner dependency closure, retained-owner
projection and M3Text adapter, strict Java 21 tests, exact-source recipe and file
validator, syntax inventory, read-only three-way planner and a narrow allocation
probe. `Verify.java` is the executable acceptance entrypoint. Receipts cover exact
code-file hashes; no complete repository build or hosted PR-head build was made.

Source snapshot: 75fb1abaecb56969bea2914520bbe819f130632b.
Target master baseline: 3029cff40e927aefcca444a6ef759a562204579c.
Historical runtime #6 head: 3776d6e674d6c9b04539ca24aca2504aa88d4a57.
See the publication reconciliation below; do not infer code presence from PR state.
Target publication commit IDs must be read from the PR and map, not guessed.

## Next dependency-ready work

Obtain complete authorized source/target checkouts, run the existing inventory
control plane and the new syntax inventory, and reconcile every discovered item.
Read actual structural owner/bridge/compiler/word sources and tests behind the
four documented ownership leads. Fetch and compare both SubMIndexString contracts
before adapting them. Record all exclusions explicitly; do not create a competing
AST/DAG/dictionary owner or infer implementation from a catalogue name.

For each enhancement, pin the new source revision, compare it three-way with the
last synchronized source and target hashes, compute affected dependency closure,
review target-only adaptations, then replay an eligible recipe in a separate target
workspace. Conflict, deletion, unknown type or mixed unsafe state means refusal,
not overwrite. Keep source/target refs, hashes, schema/ABI differences and exact-head
test receipts together. Reverse ports remain reviewed proposals only.

## Mandatory open gates

Full family/symbol/resource/dependency coverage; shared immutable generation and
cross-process lifetime; bounded caches; full String/Unicode/regex matrix; Route B
compiler lowering and boundary behavior; Route C matched complete JDK and all VM/
native/JIT gates; exact-head hosted CI; broad performance/retention measurements;
source donor-license/catalogue review and independent audit.

Maven is absent in the local environment; repository/dependency archive fetches
through the container failed DNS. Authenticated GitHub file/API access worked.
These limitations do not mean semantic tests passed. The existing P0 downloader,
OpenJDK build, lints and tests were not disabled. No default-branch merge or installed
JDK replacement was performed. No network dependency or secret-sharing behavior
was added to the runtime.

`--complete` must continue refusing until these gates and all pending map entries
are actually reconciled. The map schema can represent lineage; this first recipe
is not an automatic multi-owner split/merge or whole-repository lowering engine.

## Publication reconciliation (2026-10-02)

Source `develop` advanced to `8830a34a042d79e2d1b89b850d177c3038bf975e`.
This port stays pinned to `75fb1abaecb56969bea2914520bbe819f130632b`;
later source synchronization is pending. PR #7526 is now merged at
`61a1b72ca791a33ad64187f681daccfbf6657414`, rather than its initially observed draft state.

Target `master` advanced to `8bb6215372e07712f1fdf5a0cb912af495007b19`,
whose tree is **identical** to original baseline `3029cff...`: both are
`26554d002cf158c97dd4ffbb06b3885b647c0a17`. Runtime #6 merged into its
feature-branch base at `305dc277139b0e7cff1f4e284e7019a381480153`; that
merge is in the newer master's ancestry. This is not evidence that the runtime
implementation is present in the master's unchanged foundation tree. The two
historical enabled StringJoiner OOME failures and missing JIT acceptance remain
open. The other PR states above are initial observations, not a fresh global scan.

Production port commit: `c68411504e9bf0fbefd1adc582f075d05bbd83e4`,
parented on the tree-equivalent newer master. Permanent mapping references pin
this production commit separately from later metadata commits, avoiding a
self-referential commit hash. No default branch was updated or merged by this task.
