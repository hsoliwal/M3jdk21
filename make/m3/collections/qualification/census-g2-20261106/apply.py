#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe CENSUS_OWNER_EDITS v1 (census-g2-20261106): hash-pinned edits of existing jdk.m3.collections owners for M3JDK census leaves.

Derived from make/m3/qualification/progress-monitor-unify-20261106/unify.py v1 (hash-pinned preimage blobs, drift refusal,
one-anchor EDIT rules, LF payloads re-joined with the preimage's line ending, idempotent fixed point). Rules live in
EDIT_RULES.tsv next to this script (group data, no code: kind, path, preimage blob, note, JSON payload; PIN rows name
base_commit and donor_commit). A .tsv is a lineage file for make/m3/tooling/jdk-naming, so donor paths and package names
in the rules are never renamed; this script names none and is the same in every census group.

Rule kinds:
  EDIT   (path, preimage blob, note, [("sub", old, new) | ("re", pattern, repl)]): every anchor occurs exactly once.
  DONOR  (path, preimage blob, note, {"pre": donor path, "post": donor path, "package": [from, to],
          "rewrites": [[regex, replacement, reason], ...], "anchor": text}): the owner is replaced by the donor
          postimage under the declared transform T (package line/imports from -> to, then the declared rewrites),
          and T(donor preimage) must equal the pinned preimage byte for byte (PRE_MATCH, else REFUSED): the edit is
          exactly the donor's pre->post delta, nothing of the owner is lost. One //FLLDCIM: MRL anchor line naming the
          donor blobs is inserted before the top-level type declaration (CNPCDA).
  APPEND (path, preimage blob, note, text): text appended at the end.

A target must equal the preimage (rule applied) or the postimage (already applied, no-op); anything else is a drifted
preimage and the recipe refuses (exit 2) without writing. After the first run the preimage is read back with git
cat-file, so --check recomputes every postimage from pinned bytes. EDITS.tsv (file, kind, before_blob, before_sha256,
after_sha256, rule) is written next to this script and is a fixed point; a second run reports changed=0.

usage: python -I apply.py [--root DIR] [--donor-git DIR --donor-rev REV] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path

RECIPE = "apply.py"
VERSION = "v1"
RECIPE_DIR = Path(__file__).resolve().parent
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "kind", "before_blob", "before_sha256", "after_sha256", "rule")
TYPE_LINE = re.compile(r"^(?:(?:public|final|abstract|sealed|non-sealed|strictfp)\s+)*(?:class|interface|enum|record)\s+\w+",
                       re.MULTILINE)


class Refused(Exception):
    pass


def load_rules() -> tuple:
    pins, rules = {}, []
    for line in (RECIPE_DIR / "EDIT_RULES.tsv").read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#") or line.startswith("kind	"):
            continue
        kind, path, blob, note, payload = line.split("	")
        if kind == "PIN":
            pins[path] = blob
        elif kind in ("EDIT", "DONOR", "APPEND"):
            rules.append((kind, path, blob, note, json.loads(payload)))
        else:
            raise Refused("unknown rule kind %s in EDIT_RULES.tsv" % kind)
    return pins["base_commit"], pins["donor_commit"], tuple(rules)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def eol_of(data: bytes) -> bytes:
    return b"\r\n" if b"\r\n" in data else b"\n"


def with_eol(text: str, eol: bytes) -> str:
    return text.replace("\n", eol.decode())


def git_blob(root: Path, spec: str) -> bytes:
    try:
        return subprocess.run(["git", "-C", str(root), "cat-file", "blob", spec], check=True,
                              capture_output=True).stdout
    except (subprocess.CalledProcessError, OSError) as failure:
        raise Refused("blob %s is not in repository %s (%s)" % (spec, root, failure)) from failure


def transform(data: bytes, payload: dict) -> str:
    text = data.decode("utf-8").replace("\r\n", "\n")
    old, new = payload["package"]
    text, n = re.subn(r"^package " + re.escape(old) + r";$", "package " + new + ";", text, flags=re.MULTILINE)
    if n != 1:
        raise Refused("DONOR package line %s matched %d times" % (old, n))
    text = re.sub(r"^(import\s+(?:static\s+)?)" + re.escape(old) + r"\.", r"\g<1>" + new + ".", text, flags=re.MULTILINE)
    for regex, replacement, _reason in payload.get("rewrites", ()):
        text, n = re.subn(regex, replacement, text, flags=re.MULTILINE)
        if n == 0:
            raise Refused("DONOR rewrite %r matched nothing" % regex)
    return text


def donor_postimage(pre: bytes, payload: dict, donor: dict, path: str) -> bytes:
    if donor.get("git") is None:
        raise Refused("DONOR rule %s needs --donor-git and --donor-rev" % path)
    donor_pre = git_blob(Path(donor["git"]), donor["rev"] + ":" + payload["pre"])
    donor_post = git_blob(Path(donor["git"]), donor["rev"] + ":" + payload["post"])
    eol = eol_of(pre)
    if with_eol(transform(donor_pre, payload), eol).encode("utf-8") != pre:
        raise Refused("PRE_MATCH failed for %s: T(donor preimage %s) differs from the pinned preimage" % (path, payload["pre"]))
    text = transform(donor_post, payload)
    found = TYPE_LINE.search(text)
    if found is None:
        raise Refused("DONOR postimage %s has no top-level type declaration" % payload["post"])
    anchor = payload["anchor"] % {"post": blob_sha(donor_post)[:12], "pre": blob_sha(donor_pre)[:12],
                                  "commit": donor["commit"][:12]}
    text = text[:found.start()] + anchor + "\n" + text[found.start():]
    return with_eol(text, eol).encode("utf-8")


def postimage(kind: str, pre: bytes, payload, path: str, donor: dict) -> bytes:
    eol = eol_of(pre)
    if kind == "DONOR":
        return donor_postimage(pre, payload, donor, path)
    text = pre.decode("utf-8")
    if kind == "APPEND":
        if not text.endswith(eol.decode()):
            text += eol.decode()
        return (text + with_eol(payload, eol)).encode("utf-8")
    for edit in payload:
        text = apply_edit(text, edit, eol, path)
    return text.encode("utf-8")


def apply_edit(text: str, edit: tuple, eol: bytes, path: str) -> str:
    mode, old, new = edit
    if mode == "sub":
        old, new = with_eol(old, eol), with_eol(new, eol)
        count = text.count(old)
        if count != 1:
            raise Refused("anchor occurs %d times (need 1) in %s: %r" % (count, path, old[:60]))
        return text.replace(old, new)
    text, count = re.subn(old, new, text, flags=re.MULTILINE)
    if count != 1:
        raise Refused("pattern matched %d times (need 1) in %s: %r" % (count, path, old[:60]))
    return text


def run(root: Path, check: bool, donor: dict) -> int:
    base, donor_commit, rules = load_rules()
    if donor.get("git"):
        donor["commit"] = subprocess.run(["git", "-C", donor["git"], "rev-parse", donor["rev"] + "^{commit}"],
                                         check=True, capture_output=True).stdout.decode().strip()
        if donor["commit"] != donor_commit:
            raise Refused("donor revision %s is not the pinned donor commit %s" % (donor["commit"], donor_commit))
    rows, changed = [], 0
    for kind, rel, blob, note, payload in rules:
        target = root / rel
        on_disk = target.read_bytes() if target.exists() else None
        pre = on_disk if on_disk is not None and blob_sha(on_disk) == blob else git_blob(root, blob)
        post = postimage(kind, pre, payload, rel, donor)
        if on_disk is None:
            raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
        if blob_sha(on_disk) != blob and on_disk != post:
            raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (rel, blob_sha(on_disk), blob))
        state = "UNCHANGED"
        if on_disk != post:
            state = "CHANGED"
            if not check:
                target.write_bytes(post)
        changed += state != "UNCHANGED"
        rows.append((rel, kind, blob, sha256(pre), sha256(post), note))
        print("%s %s %s" % (kind, state, rel))
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    table_path = RECIPE_DIR / EDITS_NAME
    table_state = not table_path.exists() or table_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        table_path.write_bytes(table.encode("utf-8"))
    print("CENSUS_OWNER_EDITS recipe=%s %s base=%s donor=%s rules=%d changed=%d edits=%s" % (
        RECIPE, VERSION, base[:12], donor_commit[:12], len(rows), changed, "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[4]), help="repository root (default: four up)")
    parser.add_argument("--donor-git", help="donor git directory (DONOR rules)")
    parser.add_argument("--donor-rev", help="donor revision (DONOR rules)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check, {"git": args.donor_git, "rev": args.donor_rev})
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
