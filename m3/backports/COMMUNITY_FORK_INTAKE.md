# M3JDK21 community-fork superset intake

Status: evidence-only intake plane for JDK21 derivative repositories.

## Purpose

M3JDK21 does not assume that all useful Java21-compatible work exists only in upstream
`openjdk/jdk21u`.

The fork intake inventories selected JDK21 derivative/product branches and compares their Git
history against the current pinned `openjdk/jdk21u` reference. The output nominates fork-unique
commits and paths for the same compatibility/recipe proof process used by normal upstream changes.

This plane never grants source-copy, semantic-equivalence, distribution, merge or promotion
authority.

## Current pinned comparison set

The machine-readable authority is `COMMUNITY_FORKS.tsv`.

The initial product/evidence set includes:

- Amazon Corretto 21;
- SAP SapMachine 21;
- Microsoft OpenJDK 21u;
- Tencent Kona 21;
- Alibaba Dragonwell Standard and Extended 21;
- JetBrains Runtime 21;
- selected Corretto and Microsoft experimental/patch lanes.

Each row pins one exact repository/ref/head. Ref movement is treated as evidence drift and fails CI
until explicitly reviewed and repinned.

## Upstream baseline

Comparison baseline:

- repository: `openjdk/jdk21u`;
- ref: `master`;
- exact head is pinned in `COMMUNITY_FORKS.tsv`.

This is a JDK21-update comparison plane. It complements the JDK22-27 released-GA inventory and does
not replace the JDK21 GA verbatim oracle.

## Patch-equivalence rule

A different Git commit hash is not automatically a fork innovation.

The inventory uses the symmetric Git history with `--cherry-pick` filtering. If a fork commit is
patch-equivalent to an upstream commit, it is removed from the fork-unique queue even when author,
timestamp or commit hash differs.

Only remaining fork-side changes become candidate evidence.

## Outputs

`community_fork_inventory.py` writes:

- `FORK_RELATIONSHIPS.tsv`
  - exact pinned/resolved heads;
  - merge base;
  - fork-unique/upstream-unique commit counts;
  - all authority flags false.
- `FORK_UNIQUE_CHANGES.tsv`
  - fork identity;
  - commit/JBS IDs/subject;
  - touched paths;
  - path-derived domain/risk/scope floor;
  - suggested recipe proof lane;
  - `PENDING_COMPATIBILITY_PROOF`.
- `FORK_PATH_CANDIDATES.tsv`
  - deterministic path roll-up across forks;
  - fork/commit counts;
  - evidence domains and recipe strategies;
  - no acceptance or source-copy authority.
- `summary.json`
  - deterministic counts for programme/status tooling.

## Classification is not admission

Path and subject analysis may classify a candidate as:

- `OPENREWRITE_JAVA_REVIEW`;
- `SOURCE_SEALED_NATIVE_REVIEW`;
- `VERBATIM_TEXT_REVIEW`;
- `COMPOSITE_OR_BINARY_REVIEW`.

These are routing hints only.

Every candidate remains:

`PENDING_COMPATIBILITY_PROOF`

until the normal M3 gates decide otherwise.

## Scope

The candidate scope floor is mechanically bounded:

`FILE -> PACKAGE -> MODULE -> MULTI_MODULE`

Visibility/API scope is never inferred from paths.

If a candidate changes visibility or public/exported contract, a later reviewed packet must
explicitly promote to:

`VISIBILITY` or `LIBRARY_API`.

## Provenance and licensing

A repository-level license is not assumed to authorize every copied/adapted path.

Every non-upstream fork candidate carries:

`REVIEW_REPOSITORY_AND_PATH_LICENSES`.

Before source adaptation, the packet must record the exact donor repository/ref/commit/path and the
applicable license/provenance evidence.

Fork intake may be used as an idea/mechanism catalogue even when source adaptation is not licensed
or desirable.

## Mechanical flow

    pinned fork heads
        -> Git common ancestry
        -> cherry-equivalence filtering
        -> fork-unique commit inventory
        -> path/domain/scope routing
        -> compatibility proof
        -> exact donor/baseline preimages
        -> OpenRewrite/source-sealed recipe
        -> recipe JUnit
        -> diff/lint
        -> Java21/OpenJDK compile
        -> jtreg/runtime/native parity where applicable
        -> serial promotion

No heuristic stage may skip the compatibility proof.

## CI

`.github/workflows/m3-jdk21-community-forks.yml`:

1. runs the synthetic Git-history unit tests;
2. fetches only the pinned branch histories, blobless;
3. verifies every remote head still matches the checked-in pin;
4. materializes fork relationship/unique-change/path evidence;
5. asserts all compatibility and authority fields remain fail-closed;
6. uploads the generated inventory as a review artifact.

## Adding another fork or branch

Add one row to `COMMUNITY_FORKS.tsv` with:

- stable fork ID;
- GitHub repository;
- exact branch/ref;
- exact current head;
- product/experimental/patch plane;
- priority;
- license-review policy;
- both authority flags false.

The next CI run validates the new branch against the same upstream baseline.

Do not add a branch merely because it is large or novel. Prefer product branches and explicit
capability lanes with a plausible Java21-compatible value proposition.

## Completion boundary

Community-fork review is not complete because the initial catalogue exists.

It is complete for a pinned snapshot only when:

1. every catalogued ref has been fetched and verified;
2. every fork-unique commit has a compatibility disposition or explicit pending proof;
3. every accepted capability has a source-bound recipe/provenance record;
4. Java21/OpenJDK build/test gates are green;
5. rerunning the fork inventory finds no unclassified compatible residue for the pinned refs.

The catalogue is intentionally extensible as additional relevant JDK21 forks/branches are
identified.
