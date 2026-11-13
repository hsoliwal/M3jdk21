<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Release checklist: `m3jdk-21.0.14-m3.1-preview`

> [!WARNING]
> **Historical October 31 publication procedure — use the
> [current candidate checklist](RELEASE_CHECKLIST_CANDIDATE_2756.md) for preparation
> (reviewed 2026-11-07).** The current machine contract and release notes agree on
> `release/m3jdk-21.0.14-m3.1-preview`; the earlier branch-identity conflict is
> resolved. **Do not delete or recreate a branch, mint the tag, or run the
> historical commit-tree/push commands below** as part of preparation. The final
> candidate still requires exact source, licensing, all eleven mandatory gate
> receipts and the existing ancestry policy. Historical dates and commands remain
> for traceability; they do not authorize rewriting history or establish a release.

These are the user's actions, in order. Agents prepare branches, evidence and notes. Approvals,
merges, repository secrets, the tag and the GitHub Release are the user's. Every command runs from
a normal (non-shallow) clone of `hsoliwal/M3jdk21`. The [release notes](RELEASE_NOTES.md) carry
the claims, non-claims, platform matrix and known issues.

## Fixed parameters

| Parameter | Value |
| --- | --- |
| Upstream baseline (release commit parent) | `890adb6410dab4606a4f26a942aed02fb2f55387` (`jdk-21+35`) |
| Release branch | `release/m3jdk-21.0.14-m3.1-preview` |
| Tag | `m3jdk-21.0.14-m3.1-preview` (annotated) |
| Release commit identity | author and committer `Hitesh Soliwal <hsoliwal@gmail.com>`, both dates `2026-10-31T00:00:00+00:00` |
| Release commit subject | `M3JDK21 21.0.14 m3.1 technical preview` |

With these parameters, the release commit is a pure function of the release tree. Anyone can
recompute it and get the same SHA.

## 1. Review the current source head and already-merged PRs

As of 2026-11-07, [#2177](https://github.com/hsoliwal/M3jdk21/pull/2177)
(merged 2026-11-04) and [#2184](https://github.com/hsoliwal/M3jdk21/pull/2184)
(merged 2026-11-03) are **historical inputs, not pending merge actions**.
Inspect current `master`, its ancestry, the incoming release-preparation PRs,
and the no-loss/source-retention queue. Never base a release on the older
`bb66a8c16642` snapshot or assume that its earlier preview tree is still current.

## 2. Review upstream recipe and credential boundary

The historical `m3-a3.yml` workflow references a private Synexia A3 pin and
may require `SYNEXIA_READ_TOKEN` with read-only Contents scope. **Do not add
new credentials as part of this release-preparation PR.** The current contract
and `AGENTS.md` place canonical recipe execution in Synexia; product acceptance
remains M3JDK21-owned and requires zero executable external dependencies. Any
credential for a separately reviewed historical build is a separate owner action
and cannot substitute for the current receipt and standalone gates.
Do not print the token or embed it in release artifacts.

## 3. Legacy recipe custody (user, after its dependency)

Review the historic 2026-10-31 custody inventory (811 candidates under nine
legacy roots) against the *current* source tree. Only remove migrated files in a
separate reviewed, content-preserving migration with sealed owner/consumer closure;
do not assume that the old count or unmerged-PR list is still current.
`legacy_recipe_custody` remains HOLD until current release evidence closes it.

## 4. Release branch

The preview commit/tree described in the historical 2026-10-31 notes is **stale** after
subsequent merges. Recompute from the final reviewed `master` tree, preserve the exact
OpenJDK baseline and source/evidence identity, and use a new candidate branch when possible.
Never force-push or delete an existing release branch without explicit review. The
commands below are a manual publication procedure, not an action performed by this PR.

```sh
git fetch origin master
SRC=$(git rev-parse origin/master); TREE=$(git rev-parse "$SRC^{tree}")
export GIT_AUTHOR_NAME="Hitesh Soliwal" GIT_AUTHOR_EMAIL=hsoliwal@gmail.com \
  GIT_COMMITTER_NAME="Hitesh Soliwal" GIT_COMMITTER_EMAIL=hsoliwal@gmail.com \
  GIT_AUTHOR_DATE=2026-10-31T00:00:00+00:00 GIT_COMMITTER_DATE=2026-10-31T00:00:00+00:00
REL=$(git commit-tree "$TREE" -p 890adb6410dab4606a4f26a942aed02fb2f55387 \
  -m "M3JDK21 21.0.14 m3.1 technical preview" \
  -m "Single release commit above openjdk/jdk21 jdk-21+35 (890adb6410dab4606a4f26a942aed02fb2f55387). Release tree $TREE from hsoliwal/M3jdk21 $SRC.")
git rev-parse "$REL^" "$REL^{tree}"   # must print the baseline and $TREE
# If this branch already exists, STOP and review; do not delete or force-push it.
git push origin "$REL:refs/heads/release/m3jdk-21.0.14-m3.1-preview"
```

Optional: record the release in master's history with a PR from the release branch. After step 1
its tree diff is empty.

```sh
gh pr create -R hsoliwal/M3jdk21 --base master --head release/m3jdk-21.0.14-m3.1-preview \
  --title "Release m3jdk-21.0.14-m3.1-preview" --body "Single release commit; tree equals master."
```

## 5. Evidence and admission

A push to `release/**` runs `m3-first-release-admission.yml`: contract, foundation, precompute and
control-reactor. `main.yml` (OpenJDK sanity matrix) also runs on branch pushes.
The `M3_CHANGED_ATOM_COVERAGE` receipt remains PENDING until a real instrumented
line/branch report, complete changed-atom inventory and source SHA-256s are
recorded. A synthetic verifier test does not meet this gate. Then, on a
checkout of the release commit:

```sh
REL=$(git rev-parse origin/release/m3jdk-21.0.14-m3.1-preview)
test "$(git rev-parse "$REL^")" = 890adb6410dab4606a4f26a942aed02fb2f55387
python3 -I make/m3/release/verify_release_admission.py --root . --release \
  --evidence /absolute/path/to/evidence.json --json-out admission-release.json
```

The manifest binds the exact commit and tree. A receipt for another commit is refused. Release is
admissible when `policy_valid` and `release_ready` are both `true`. Any mandatory gate still
PENDING keeps the current candidate on HOLD; listing the missing evidence does not satisfy it.

## 6. Tag (user)

```sh
git tag -a m3jdk-21.0.14-m3.1-preview "$REL" -m "M3JDK21 21.0.14 m3.1 technical preview"
git push origin m3jdk-21.0.14-m3.1-preview
```

After the tag, the released history is immutable (`released_history_immutable_after_tag`).

## 7. GitHub Release (user)

```sh
gh release create m3jdk-21.0.14-m3.1-preview -R hsoliwal/M3jdk21 --verify-tag --prerelease \
  --title "M3JDK21 21.0.14 m3.1 technical preview" --notes-file make/m3/release/RELEASE_NOTES.md \
  evidence.json admission-release.json SHA256SUMS
```

Attach the evidence pack: the manifest, each gate's receipt and log, the admission JSON and
`SHA256SUMS`. Attach JDK bundles only from a green `main.yml` run at the release commit.
