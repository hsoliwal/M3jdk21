#!/usr/bin/env python
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
"""Recipe (pattern WORKFLOW_CONCURRENCY_CANCEL): GitHub Actions starves when every push queues every workflow again
(2026-10-29: 461 workflows, 553 queued runs, 127 of them superseded by a newer run of the same workflow and ref).
Inserts the block upstream OpenJDK main.yml uses,
    concurrency:
      group: ${{ github.workflow }}-${{ github.ref }}
      cancel-in-progress: true
before the top-level `jobs:` line of every workflow that has no top-level `concurrency:`. Reusable workflows
(`workflow_call`) are skipped: inside a called workflow github.workflow is the caller's name, so the same group would
cancel the caller. Text-only edit (comments and layout kept); the file must still parse as YAML with the block.
Usage: add_workflow_concurrency.py <workflow files...>   prints "<file>: added | kept (has concurrency) | skipped (...)"."""
import re, sys, yaml
BLOCK = "concurrency:\n  group: ${{ github.workflow }}-${{ github.ref }}\n  cancel-in-progress: true\n\n"
for p in sys.argv[1:]:
    raw = open(p, "rb").read(); nl = "\r\n" if b"\r\n" in raw else "\n"; s = raw.decode("utf-8").replace("\r\n", "\n")
    if re.search(r"(?m)^concurrency:", s):
        print(f"{p}: kept (has concurrency)"); continue
    try:
        doc = yaml.safe_load(s); on = doc.get("on", doc.get(True)) if isinstance(doc, dict) else None
    except Exception as e:
        print(f"{p}: skipped (YAML error {str(e).splitlines()[0]})"); continue
    if (isinstance(on, dict) and "workflow_call" in on) or (isinstance(on, (list, str)) and "workflow_call" in on):
        print(f"{p}: skipped (reusable workflow)"); continue
    m = re.search(r"(?m)^jobs:", s)
    if not m:
        print(f"{p}: skipped (no top-level jobs:)"); continue
    t = s[:m.start()] + BLOCK + s[m.start():]
    d2 = yaml.safe_load(t)
    assert d2.get("concurrency", {}).get("cancel-in-progress") is True and d2.get("jobs") == doc.get("jobs"), p
    open(p, "wb").write(t.replace("\n", nl).encode("utf-8"))
    print(f"{p}: added")
