# Checks superset

The fork's own per-topic check workflows (494 files in `.github/workflows`) no longer start a run each.
Their jobs run inside a few superset workflows, `.github/workflows/checks-superset-NN.yml`, so one push or
pull-request update starts a handful of runs instead of one run per topic. Nothing is dropped:

- Every former job is copied byte for byte. Only `name` (prefixed with the former workflow name), `needs`
  (the gate job first), `if` (the gate decision AND the job's own condition), and the former workflow-level
  `permissions`, `env` and `concurrency` (now on each job) are added.
- The former triggers are recorded in `filters.json`. A shard's own triggers are the union of its members'
  triggers, and its first job, `gate`, runs `gate.py`, which applies each member's exact filters (branches,
  tags, types, paths, paths-ignore, with `!` negation) to the event. Changed files come from plain git: a
  two-dot diff for pushes, the merge base with the default branch for a new branch, and the three-dot diff
  for pull requests. If git cannot compute them, every path filter counts as matched (fail open).
- The former workflow files stay, with only `workflow_dispatch` as trigger (their inputs kept) and the
  former `on:` block left as a comment, so each can still be run by hand. Their jobs are the source of truth.
- OpenJDK's own workflows (`main.yml`, `build-*.yml`, `test.yml`) and the tag-only release build
  (`m3-release.yml`) are not part of the superset and are unchanged.

Each shard cancels its own superseded run on a non-default branch (`cancel-in-progress`); runs on the
default branch are never cancelled by the superset (their concurrency group is the run id). A member's
former concurrency group stays on its jobs.

## Files

| File | Role |
|---|---|
| `filters.json` | Members, their shard and their former push / pull_request / workflow_dispatch triggers |
| `MAPPING.tsv` | Former workflow -> superset file and job ids, with the triggers it keeps (generated) |
| `gate.py` | Per-event member decision; writes `run={member: bool}` for the shard's jobs |
| `generate.py` | Builds the shard files from `filters.json` and the member files and verifies every job |
| `test_gate.py` | Unit tests: GitHub's filter pattern cheat sheet and the trigger decisions |

## Changing a member

Edit its jobs in its own file under `.github/workflows`, then run

    python3 .github/checks-superset/generate.py build    # rewrites the shard files and MAPPING.tsv
    python3 .github/checks-superset/generate.py check    # exit 1 when a shard file is stale
    python3 .github/checks-superset/test_gate.py

To change a member's triggers, edit its entry in `filters.json` and run `build`. To move a new stand-alone
workflow into the superset, run `generate.py init .github/workflows/<file>.yml` and then `build`.
