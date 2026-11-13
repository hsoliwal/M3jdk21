#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""split_contract.py <named-contract> <out-file> [--check]
CONTRACT_SPLIT v1: split one HASH_PINNED_CHAIN v2 contract (crate_chain2.py output) by the declared rows of SPLIT.tsv
(next to this script). SOURCE pins the input sha256; DROP removes a pinned 1-based inclusive line span; EDIT replaces
pinned exact text in the kept lines; ABSENT names tokens the output must not contain. Any pin mismatch, span overlap or
leftover token refuses with exit 2 and writes nothing. --check compares with <out-file> instead of writing (exit 1 when
it differs). Prints one summary line with the assertion-site census (check(...) and refuses(...) call sites, helper
definitions excluded): source, dropped and kept sites; kept sites are byte-identical to the source lines."""
import hashlib
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
SITE = re.compile(rb"(?<![A-Za-z0-9_])(check|refuses)\(")
DEFN = re.compile(rb"\bvoid (check|refuses)\(")


def refuse(message):
    print("REFUSED\t" + message)
    sys.exit(2)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def sites(lines):
    return sum(len(SITE.findall(line)) - len(DEFN.findall(line)) for line in lines)


def rows():
    for raw in (HERE / "SPLIT.tsv").read_text(encoding="utf-8").split("\n"):
        cells = raw.split("\t")
        if raw.startswith("#") or len(cells) != 8 or cells[0] == "kind":
            continue
        yield dict(zip(("kind", "id", "where", "count", "sha", "value", "new", "reason"), cells))


def split(data):
    lines = data.splitlines(keepends=True)
    table = list(rows())
    source = [r for r in table if r["kind"] == "SOURCE"]
    if len(source) != 1 or sha(data) != source[0]["sha"]:
        refuse("SOURCE sha256 " + sha(data))
    dropped = set()
    for r in (r for r in table if r["kind"] == "DROP"):
        first, last = (int(v) for v in r["where"].split("-"))
        span = set(range(first, last + 1))
        if not 1 <= first <= last <= len(lines) or span & dropped:
            refuse("DROP " + r["id"] + ": span out of range or overlapping")
        if not sha(b"".join(lines[first - 1:last])).startswith(r["sha"]):
            refuse("DROP " + r["id"] + ": span bytes differ from the pin")
        dropped |= span
    kept = [line for number, line in enumerate(lines, 1) if number not in dropped]
    gone = [line for number, line in enumerate(lines, 1) if number in dropped]
    text = b"".join(kept)
    for r in (r for r in table if r["kind"] == "EDIT"):
        old, new = r["value"].encode("utf-8"), b"" if r["new"] == "-" else r["new"].encode("utf-8")
        if text.count(old) != int(r["count"]) or not sha(old).startswith(r["sha"]):
            refuse("EDIT " + r["id"] + ": occurs " + str(text.count(old)) + " times or pin differs")
        text = text.replace(old, new)
    for r in (r for r in table if r["kind"] == "ABSENT"):
        if r["value"].encode("utf-8") in text:
            refuse("ABSENT " + r["id"] + ": " + r["value"] + " left in the output")
    census = (sites(lines), sites(gone), sites(kept))
    if census[0] != census[1] + census[2] or sites(text.splitlines(keepends=True)) != census[2]:
        refuse("assertion-site census does not add up: " + str(census))
    return text, len(dropped), census


def main():
    if len(sys.argv) not in (3, 4) or (len(sys.argv) == 4 and sys.argv[3] != "--check"):
        refuse(__doc__.splitlines()[0])
    source, out = Path(sys.argv[1]), Path(sys.argv[2])
    text, dropped, census = split(source.read_bytes())
    state = "WRITTEN"
    if len(sys.argv) == 4:
        state = "UNCHANGED" if out.is_file() and out.read_bytes() == text else "DIFFERS"
    else:
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_bytes(text)
    print(f"CONTRACT_SPLIT v1\t{state}\tdropped_lines={dropped}\tsites source={census[0]} dropped={census[1]}"
          f" kept={census[2]}\tout_sha256={sha(text)}")
    sys.exit(1 if state == "DIFFERS" else 0)


if __name__ == "__main__":
    main()
