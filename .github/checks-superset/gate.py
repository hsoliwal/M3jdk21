#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Checks superset gate: decide which member workflows of one superset shard run for this event.

Each member is a former stand-alone workflow whose push / pull_request / workflow_dispatch triggers are
recorded in filters.json next to this file. This script applies GitHub's own trigger rules to them:
branch and tag filters (with '!' negation, in order), the event's pull_request types (default opened,
synchronize, reopened), and paths / paths-ignore against the changed files. The changed files come from
plain git: a two-dot diff before..after for a push to an existing branch, the merge base with the default
branch for a new branch, and the three-dot diff (merge base of base and head) for a pull request. Path
filters are not applied to tag pushes, as on GitHub. When the changed files cannot be computed the gate
fails open (every path filter counts as matched), so no member check is dropped.

Usage: gate.py <shard>     writes run=<json {member: bool}> to $GITHUB_OUTPUT and a table to the summary.
"""
import json
import os
import re
import subprocess
import sys

ZERO = "0" * 40
DEFAULT_TYPES = ("opened", "synchronize", "reopened")
HERE = os.path.dirname(os.path.abspath(__file__))


def glob_regex(pattern):
    """GitHub filter pattern -> regex: * (no /), ** (any), **/ (zero or more dirs), ? and + quantify."""
    out, i = [], 0
    while i < len(pattern):
        if pattern.startswith("**/", i):
            out.append("(?:.*/)?"); i += 3
        elif pattern.startswith("**", i):
            out.append(".*"); i += 2
        elif pattern[i] == "*":
            out.append("[^/]*"); i += 1
        elif pattern[i] in "?+":
            out.append(pattern[i]); i += 1
        elif pattern[i] == "[":
            end = pattern.find("]", i + 1)
            out.append(pattern[i:end + 1] if end > i else re.escape("[")); i = end + 1 if end > i else i + 1
        elif pattern[i] == "\\" and i + 1 < len(pattern):
            out.append(re.escape(pattern[i + 1])); i += 2
        else:
            out.append(re.escape(pattern[i])); i += 1
    return "".join(out)


def matched(patterns, value):
    """Ordered evaluation: a later '!pattern' removes what an earlier pattern added."""
    state = False
    for p in patterns:
        negate = p.startswith("!")
        if re.fullmatch(glob_regex(p[1:] if negate else p), value):
            state = not negate
    return state


def git(*args):
    return subprocess.run(["git", *args], capture_output=True, text=True)


def fetch(depth, *shas, history=False):
    """history=True: commits only (tree:0) to walk to a merge base; else the commits' trees (no blobs),
    refetched even when the commit is already present without its trees."""
    how = ["--filter=tree:0"] if history else ["--filter=blob:none", "--refetch"]
    return git("fetch", "--no-tags", "--quiet", *how, f"--depth={depth}", "origin", *shas).returncode == 0


def merge_base(a, b):
    for depth in (64, 512, 4096):
        if not fetch(depth, a, b, history=True):
            return None
        r = git("merge-base", a, b)
        if r.returncode == 0:
            return r.stdout.strip() if fetch(1, r.stdout.strip(), b) else None
    return None


def names(a, b):
    if a is None:
        return None
    r = git("diff", "--name-only", "--no-renames", a, b)
    return [n for n in r.stdout.splitlines() if n] if r.returncode == 0 else None


def default_sha(payload):
    branch = (payload.get("repository") or {}).get("default_branch", "master")
    r = git("ls-remote", "origin", f"refs/heads/{branch}")
    return r.stdout.split()[0] if r.returncode == 0 and r.stdout.strip() else None


def changed_files(event, payload):
    if event == "pull_request":
        pr = payload["pull_request"]
        return names(merge_base(pr["base"]["sha"], pr["head"]["sha"]), pr["head"]["sha"])
    before, after = payload.get("before", ZERO), payload.get("after", ZERO)
    if after == ZERO:
        return []
    if before == ZERO:
        base = default_sha(payload)
        return names(merge_base(base, after) if base else None, after)
    return names(before, after) if fetch(1, before, after) else None


def ref_ok(spec, kind, value):
    inc, exc = ("branches", "branches-ignore") if kind == "branch" else ("tags", "tags-ignore")
    other = ("tags", "tags-ignore") if kind == "branch" else ("branches", "branches-ignore")
    if inc in spec:
        return matched(spec[inc], value)
    if exc in spec:
        return not matched(spec[exc], value)
    return not any(k in spec for k in other)


def paths_ok(spec, files):
    if "paths" in spec:
        return files is None or any(matched(spec["paths"], f) for f in files)
    if "paths-ignore" in spec:
        return files is None or any(not matched(spec["paths-ignore"], f) for f in files)
    return True


class Event:
    def __init__(self):
        self.name = os.environ["GITHUB_EVENT_NAME"]
        with open(os.environ["GITHUB_EVENT_PATH"], encoding="utf-8") as f:
            self.payload = json.load(f)
        self.ref = os.environ.get("GITHUB_REF", "")
        self._files, self.fetched = None, False

    def files(self):
        if not self.fetched:
            self._files, self.fetched = changed_files(self.name, self.payload), True
            n = "unknown (fail open)" if self._files is None else len(self._files)
            print(f"gate event={self.name} ref={self.ref} changed_files={n}")
        return self._files

    def selected(self, key, member):
        wanted = ((self.payload.get("inputs") or {}).get("select") or "all").replace(",", " ").split()
        return "all" in wanted or key in wanted or member["file"] in wanted


def decide(key, m, ev):
    if ev.name == "push" and "push" in m:
        if ev.ref.startswith("refs/tags/"):
            return ref_ok(m["push"], "tag", ev.ref[len("refs/tags/"):])
        return ref_ok(m["push"], "branch", ev.ref[len("refs/heads/"):]) and paths_ok(m["push"], ev.files())
    if ev.name == "pull_request" and "pull_request" in m:
        spec = m["pull_request"]
        if ev.payload.get("action") not in spec.get("types", DEFAULT_TYPES):
            return False
        return ref_ok(spec, "branch", os.environ.get("GITHUB_BASE_REF", "")) and paths_ok(spec, ev.files())
    if ev.name == "workflow_dispatch" and "workflow_dispatch" in m:
        return ev.selected(key, m)
    if ev.name == "schedule":
        return ev.payload.get("schedule") in m.get("schedule", [])
    return False


def main(shard):
    with open(os.path.join(HERE, "filters.json"), encoding="utf-8") as f:
        filters = json.load(f)
    ev, run = Event(), {}
    for key in filters["shards"][shard]:
        run[key] = bool(decide(key, filters["members"][key], ev))
        print(f"gate member={key} decision={'run' if run[key] else 'skip'}")
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as f:
        f.write("run=" + json.dumps(run, separators=(",", ":")) + "\n")
    rows = [f"| {filters['members'][k]['name']} | `{filters['members'][k]['file']}` |" for k, v in run.items() if v]
    with open(os.environ.get("GITHUB_STEP_SUMMARY", os.devnull), "a", encoding="utf-8") as f:
        f.write(f"### Shard {shard}: {len(rows)} of {len(run)} member workflows run\n\n| Workflow | Former file |\n|---|---|\n")
        f.write("\n".join(rows) + "\n")


if __name__ == "__main__":
    main(sys.argv[1])
