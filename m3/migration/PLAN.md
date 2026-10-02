# MIndex-to-M3 migration: first dependency-ready slice

> Execute serially in this session. Read `../docs/name-mapping.json` and `RESUME.md`
> before subsequent enhancement work. The controlling specification is
> `../docs/shared-atom-concatenation.md` and the supplied 2026-10-02 migration task.

**Goal:** establish permanent source/target lineage, port an actual closed storage
kernel, and add an explicit UTF-16 value facade without changing the installed JDK.

**Architecture:** preserve the six existing Synexia frozen/joined owners in an
independent Java 21 module. Preserve legacy public behavior. An additive retained-
owner projection and M3 facade may use the same storage owner; they must not silently
change the legacy content-canonical tuple policy. Application/tooling dependencies
stay outside `java.base`. Extend the existing name map instead of a second canonical
mapping file. Compiler and VM routes remain independently gated.

**Global constraints:** source `75fb1abaecb56969bea2914520bbe819f130632b`;
target master baseline `3029cff40e927aefcca444a6ef759a562204579c`;
runtime candidate #6 `3776d6e674d6c9b04539ca24aca2504aa88d4a57` was separate at initial inspection. See publication reconciliation below.
Private source production ports explicitly requested by the owner are the publication
scope; do not publish unrelated private material or reference fixtures to bypass CI.
No default-branch writes, merges, full-JDK acceptance claim or installed-JDK changes.

## Review focus

Mutable admission and writable output isolation; owner identity versus equal text;
surrogate cuts and pairs across seams; read-only mapping lifetime versus protection
from external mutation; drift/partial install/rollback and exact-candidate evidence.

## Tasks and gates

1. **Pins and owner closure** — retain PR ancestry/status and source file hashes.
   Read existing owners, manifests and recipe infrastructure. The retrieved six-file
   closure is complete for its Java dependencies, not an exhaustive family inventory.
2. **Differential baseline** — run the same legacy contract program against actual
   pinned source and ported classes in separate classpaths, without interface stubs.
3. **Executable recipe** — implement a class-backed Java/Maven port recipe, exact
   hashes, eligibility checks, deterministic rendering, two-phase preflight, refusal,
   fixed point, partial-state checks and non-destructive rollback. Test before use.
4. **Port and explicit facade** — keep source names as compatibility projections;
   add retained range access/identity policy only additively. Test no joined payload,
   alternative segmentations, hashing, Unicode, mutable outputs, streams and regex.
5. **Permanent mapping** — evolve `m3/docs/name-mapping.json`, add schema/validator
   and generated coverage. Preserve prior prototype decisions; mark all unreviewed
   families pending, not implemented. Record recipe and target-only adaptations.
6. **Inventory expansion** — reuse existing source inventory inputs; produce a
   deterministic declaration/dependency report with explicit limits. A complete
   source checkout is required before the global coverage gate can pass.
7. **Verification and publication** — strict Java 21 compilation, module smoke,
   differential tests, recipe tests, optional native gates if available, exact file
   hashes, draft PR on the verified master baseline, no borrowed runtime receipts.
8. **Resume** — leave every global acceptance gate visible. Continue with structural
   owners/bridges and the runtime branch only after their dependency/contract review.

## Progress

- Source and target default heads plus all eight requested PRs inspected.
- Six production source files retrieved through authenticated connector; every local
  byte sequence matches the returned Git blob ID; source closure compiles with
  `javac --release 21 -Xlint:all -Werror`.
- Local workspace is a file-verified subset, NOT a complete Git checkout.
- Maven is absent; container DNS cannot fetch repository/dependency archives. These
  are local execution limits, not evidence of a semantic pass or a complete inventory.

Ruling: new module is com.mthree.indexstring; package names remain unchanged. Strict javac rejects a terminal-digit module component. No lint suppression. Initial recipe journal exposed unordered Map.of hashing across processes; JSON object serialization now sorts keys and child-JVM replay/rollback is tested. Old local postimages and journal are preserved in workspace evidence, not published as accepted code.

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
