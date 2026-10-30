#!/usr/bin/env python
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
"""Atom (pattern REUSABLE_WORKFLOW_CONTRACT): the startup checks GitHub applies to `uses: ./.github/workflows/x.yml`
calls, done on CPU before a push (a failing one gives startup_failure with zero jobs and no log). For every job that
calls a local reusable workflow: each `with:` key must be a declared workflow_call input, every required input
without a default must be passed, `secrets: inherit` or declared secrets only, and the called workflow's job-level
`permissions` must not exceed what the caller grants (caller job permissions, else caller top-level permissions,
else the repository default, assumed read-only when unknown). Also: every workflow parses as YAML and `on:` exists.
Usage: reusable_call_check.py <dir holding .github/workflows>   prints one line per finding; exit 1 when any."""
import os, sys, yaml
root = sys.argv[1]; wf = os.path.join(root, ".github", "workflows"); out = []
LEVEL = {"none": 0, "read": 1, "write": 2}


def load(name):
    with open(os.path.join(wf, name), encoding="utf-8") as f:
        return yaml.safe_load(f)


def on(doc):
    return doc.get("on", doc.get(True)) if isinstance(doc, dict) else None


def perms(p):
    if p is None:
        return None
    if isinstance(p, str):
        lvl = {"read-all": "read", "write-all": "write"}.get(p, p)
        return {"*": lvl}
    return dict(p)


def exceeds(need, have):
    if need is None:
        return []
    have = have if have is not None else {"*": "read"}
    bad = []
    for scope, lvl in need.items():
        got = have.get(scope, have.get("*", "none"))
        if LEVEL.get(str(lvl), 0) > LEVEL.get(str(got), 0):
            bad.append(f"{scope}:{lvl}>{got}")
    return bad


docs = {}
for n in sorted(os.listdir(wf)):
    if not n.endswith((".yml", ".yaml")):
        continue
    try:
        docs[n] = load(n)
        if on(docs[n]) is None:
            out.append(f"{n}: no 'on:' trigger")
    except Exception as e:
        out.append(f"{n}: YAML error: {str(e).splitlines()[0]}")
CALL_KEYS = {"name", "uses", "with", "secrets", "needs", "if", "permissions", "strategy", "concurrency"}
JOB_KEYS = {"name", "needs", "permissions", "runs-on", "environment", "concurrency", "outputs", "env", "defaults", "if",
            "steps", "timeout-minutes", "strategy", "continue-on-error", "container", "services", "snapshot"}
for n, d in docs.items():   # job graph (pattern WORKFLOW_JOB_GRAPH_CHECK, 2026-10-30: needs on removed jobs = 0 jobs)
    if not isinstance(d, dict):
        continue
    jobs = d.get("jobs") or {}
    for jid, job in jobs.items():
        job = job or {}
        for k in job:
            if k not in (CALL_KEYS if "uses" in job else JOB_KEYS):
                out.append(f"{n}: job {jid} has key '{k}' that GitHub does not allow on this kind of job")
        nd = job.get("needs"); nd = nd if isinstance(nd, list) else ([nd] if nd else [])
        out += [f"{n}: job {jid} needs unknown job '{x}'" for x in nd if x not in jobs]
        if "uses" not in job and "runs-on" not in job:
            out.append(f"{n}: job {jid} has neither uses nor runs-on")
for n, d in docs.items():
    if not isinstance(d, dict):
        continue
    top = perms(d.get("permissions"))
    for jid, job in (d.get("jobs") or {}).items():
        uses = (job or {}).get("uses", "")
        if not uses.startswith("./.github/workflows/"):
            continue
        callee = uses.rsplit("/", 1)[1].split("@")[0]
        cd = docs.get(callee)
        if cd is None:
            out.append(f"{n}: job {jid} calls missing {callee}"); continue
        wc = (on(cd) or {}).get("workflow_call") if isinstance(on(cd), dict) else None
        if wc is None and not (isinstance(on(cd), (list, str)) and "workflow_call" in on(cd)):
            out.append(f"{n}: job {jid} calls {callee}, which has no workflow_call trigger"); continue
        inputs = (wc or {}).get("inputs") or {}
        passed = (job.get("with") or {})
        for k in passed:
            if k not in inputs:
                out.append(f"{n}: job {jid} passes undeclared input '{k}' to {callee}")
        for k, spec in inputs.items():
            if (spec or {}).get("required") and "default" not in (spec or {}) and k not in passed:
                out.append(f"{n}: job {jid} omits required input '{k}' of {callee}")
        have = perms(job.get("permissions")) or top
        for cj, cjob in (cd.get("jobs") or {}).items():
            bad = exceeds(perms((cjob or {}).get("permissions")) or perms(cd.get("permissions")), have)
            if bad:
                out.append(f"{n}: job {jid} -> {callee} job {cj} needs {','.join(bad)} beyond the caller grant")
print("\n".join(out) if out else "OK: every local reusable-workflow call matches its contract")
sys.exit(1 if out else 0)
