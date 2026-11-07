<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# FOLDAUDIT_1755 v1: fold-close audit leaves L0035 and L0063 of closed PR #1755 (2026-11-07)

Fold-close audit of the closed-unmerged PRs (ledger `foldaudit/LEAVES.tsv`, `HUNK_VERDICTS.tsv` filtered `pr=1755`).
PR #1755 ("isolate offline Maven/Gradle scan generations in original project SQLite", head `3e70045b5ed2`, base
#1724 `cc367db1e42a`) was closed on 2026-10-28 with "Contained in master through #2070 (merge commit 2028b1fb)".
Its head is an ancestor of master, but the #2070 landing kept master's `M3BuildStateStore` live and the PR's files
byte for byte under `m3/qualification/master-landing-20261028/retained-variants/pr1755/` (`SOURCE_SHA256.tsv`).
The audit found six MISSING hunks (57 + 6 lines, 30 + 6 signature lines) in two leaves:

| leaf | PR file | master owner | missing members |
|---|---|---|---|
| L0035 | `com/m3/fs/M3BuildStateStore.java` (blob `2dc5678c1bc3`) | `jdk/m3/fs/M3BuildStateStore.java`, ON master | keyed `readOffline(fs, contextSha256, ..)`, `profileHead`, keyed `publishOffline(fs, contextSha256, ..)` -> `publishAt` |
| L0063 | `com/m3/fs/M3OfflineProjectScan.java` (blob `eec4e61de8ea`) | none, NEVER_ON_MAINLINE | the two keyed store calls; nested `/pom.xml` -> `BUILD_INPUT` |

## Decision: nothing to restore

Every hunk is SUPERSEDED or PRESENT_EVOLVED on master; per-hunk rows with line numbers are in
[DECISION.tsv](DECISION.tsv).

- **L0035 (3 hunks) SUPERSEDED by `b2321d7b550`** (#1758 "master V2: isolate offline observed profiles", merged
  `de37098a75af`, 2026-10-23). The V1 hunks keyed an offline head per 32-byte scan context (`/m3-build/offline-<64hex>`).
  Master carries the same concern on the V2 store: `readObserved(snapshot, expectedSuccessful, profileSha256, ..)`,
  `observe(fs, completed, expectedSuccessful, expectedObserved, profileSha256, ..)` and
  `promoteObserved(.., profileSha256, ..)` with `observedPath(successful, profile)` and `requiredProfile(profile)`
  (`/m3-build/observed-<successful>-<profile>`). Tested on master by
  `M3BuildStateTest.independentObservedProfilesShareOneSuccessfulBuildHead` (`866716d02a8`). Master has no caller of
  `readOffline`/`publishOffline` (`git grep`, m3/fs/src). The owner directed this outcome in the PR itself: #1755
  body "do not overwrite newer master with this v1 feature branch"; #1755 comment "#1758 ... never graft the V1 file
  store over master"; #1724 comment "Do not merge old V1 over newer master"; #1758 README on master
  (`../masterv2-observed-profiles-20261021/README.md`) "deliberately does not copy the older file/tree scanner or
  older V1 build-state classes over master".
- **L0063 H1, H2 SUPERSEDED by `0a89ef92ed4`** (#1782 "one original M3File byte stream feeds completed V2 inventory
  and native SQLite", ancestor of master via #2070) together with `b2321d7b550`. The scanner never landed; its
  successor `M3HostFileInventory.capture(project, entries, .., toolContextSha256, previous, ..)` takes the prior
  generation from the caller (`M3BuildState.deltaFrom`) and `stage(database, result, ..)` observes it through
  `M3BuildStateStore.observe(.., result.inventory().contextSha256(), ..)`: the inventory's own context is the profile,
  exactly the keying the hunks introduced.
- **L0063 H3 PRESENT_EVOLVED in `M3HostFileInventory.role(String)`** (line 216, since `0a89ef92ed4`):
  `lower.equals("pom.xml") || lower.endsWith("/pom.xml")` -> `BUILD_INPUT`, case-folded with `Locale.ROOT`. Tested by
  `M3HostFileInventoryTest` (lines 54-55, 72-73) and `M3HostFileInventoryProof` (lines 37-38, 50): `module/pom.xml`
  is `BUILD_INPUT`.
- The audit's TRIVIAL hunks (import `java.util.HexFormat`, a javadoc, one test line), the never-landed
  `M3OfflineProjectScanTest` and the V1 qualification documents follow their members (DECISION.tsv rows TRIVIAL,
  TEST, DOCS). #2663 (merge-loss leaves 30, 31) restored `M3HostFileInventory`'s shared scratch and the
  `M3FS`/`M3DirectoryTable` page identity, no store member: nothing here duplicates or conflicts with it.

RESTORE needs positive evidence: origin tests, master callers or a receipt. None exists for the V1 members, and the
owner's later deliberate commits (#1758, #1782) replaced them. Restoring them would re-add a second checkpoint
namespace next to the V2 observed profiles, which #1758 refused by design.

## Recipe

```text
python -I m3/fs/qualification/foldaudit-1755-20261107/apply.py          # OWNER .. PINNED x2, restored=0, EDITS.tsv
python -I m3/fs/qualification/foldaudit-1755-20261107/apply.py --check  # exit 0; 1 when EDITS.tsv would change; 2 REFUSED
```

The recipe edits no source. It pins the two master owner files (git blob + sha256 at base `74402d981fa`), requires
every superseding member with its exact count, requires every superseded V1 member (`readOffline(`,
`publishOffline(`, `profileHead(`, `publishAt(`, `OFFLINE_HEAD`) and the never-landed scanner files to be absent,
and writes [EDITS.tsv](EDITS.tsv) with before == after. Any drift is listed in full and refused (exit 2), which
re-opens the decision instead of silently carrying it. Gates, with the WSL Maven run of the real pom, are in
[GATES.tsv](GATES.tsv).
