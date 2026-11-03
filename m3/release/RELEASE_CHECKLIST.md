<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Release checklist: `m3jdk-21.0.14-m3.1-preview`

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

## 1. Merge the source PRs (user)

1. Review, approve and merge [#2177](https://github.com/hsoliwal/M3jdk21/pull/2177). It is the
   superset of #2168-#2171 and #2173-#2176. It is optional for the preview: if it is not merged,
   the release tree stays master plus #2184.
2. Review, approve and merge [#2184](https://github.com/hsoliwal/M3jdk21/pull/2184). It adds
   swing-viewport `-Xlint:all -Werror` (blocker 12), these notes and this checklist.

## 2. Repository secret (user)

Add `SYNEXIA_READ_TOKEN`: a fine-grained token with read-only Contents access to
`hsoliwal/com.synexia`. `m3-a3.yml` uses it to fetch the private A3 pin (audit blocker 10).

```sh
gh secret set SYNEXIA_READ_TOKEN -R hsoliwal/M3jdk21
```

## 3. Legacy recipe custody (user, after its dependency)

Merge the legacy-custody deletion PR (811 executable candidates under the nine legacy recipe
roots) once the Synexia importer repoint patch has landed. Until then, `legacy_recipe_custody`
stays HOLD and `--release` cannot report `release_ready: true`.

## 4. Release branch

The prepared branch is a candidate. It holds one commit whose tree is master `bb66a8c16642` plus
#2184 at `70c28a8bfadf`. Steps 1-3 change master's tree (the custody deletion always does), so
rebuild the commit from the final master tree. Never force-push: first delete the candidate
branch, which is the user's call, then push the rebuilt commit.

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
git push origin --delete release/m3jdk-21.0.14-m3.1-preview   # only when rebuilding
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
control-reactor. `main.yml` (OpenJDK sanity matrix) also runs on branch pushes. Then, on a
checkout of the release commit:

```sh
REL=$(git rev-parse origin/release/m3jdk-21.0.14-m3.1-preview)
test "$(git rev-parse "$REL^")" = 890adb6410dab4606a4f26a942aed02fb2f55387
python3 -I m3/release/verify_release_admission.py --root . --release \
  --evidence /absolute/path/to/evidence.json --json-out admission-release.json
```

The manifest binds the exact commit and tree. A receipt for another commit is refused. Release is
admissible when `policy_valid` and `release_ready` are both `true`. Publishing with gates still
PENDING is the user's decision. The release text must then list them, as `RELEASE_NOTES.md` does.

## 6. Tag (user)

```sh
git tag -a m3jdk-21.0.14-m3.1-preview "$REL" -m "M3JDK21 21.0.14 m3.1 technical preview"
git push origin m3jdk-21.0.14-m3.1-preview
```

After the tag, the released history is immutable (`released_history_immutable_after_tag`).

## 7. GitHub Release (user)

```sh
gh release create m3jdk-21.0.14-m3.1-preview -R hsoliwal/M3jdk21 --verify-tag --prerelease \
  --title "M3JDK21 21.0.14 m3.1 technical preview" --notes-file m3/release/RELEASE_NOTES.md \
  evidence.json admission-release.json SHA256SUMS
```

Attach the evidence pack: the manifest, each gate's receipt and log, the admission JSON and
`SHA256SUMS`. Attach JDK bundles only from a green `main.yml` run at the release commit.
