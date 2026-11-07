#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Hash-pinned aggregator pom edits for reactor-wiring-20261107 (recipe REACTOR_WIRING_EDITS).

EDITS.tsv is a linear chain per file: seq, file, before_sha256, after_sha256, op, anchor, payload, note (anchor and
payload are JSON strings). The only op is insert-before: the payload lines go in front of the one line that equals the
anchor (0 or >1 matches: refused). Bytes are kept as they are; the inserted lines take the anchor line's EOL.
apply: for each file, if its sha256 is the chain's last after_sha256 nothing is done (idempotent); if it is the first
before_sha256 or any intermediate after_sha256 the chain resumes from there; any other sha256 is drift and is refused
(exit 3), nothing is written. verify: exit 0 only when every file is at its chain's last after_sha256.
edit: one unrecorded insert-before (the gate's temporary wiring); record: the same insert, appended to EDITS.tsv.
CLI: python -I apply.py apply|verify [--root DIR]
     python -I apply.py edit|record --root DIR --file REL --anchor TEXT (--text TEXT | --text-file F) [--note TEXT]
"""
from __future__ import annotations
import argparse
import hashlib
import json
import sys
from pathlib import Path

EDITS = Path(__file__).resolve().parent / "EDITS.tsv"
HEADER = ("seq", "file", "before_sha256", "after_sha256", "op", "anchor", "payload", "note")


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def insert_before(data: bytes, anchor: str, payload: str) -> bytes:
    lines = data.split(b"\n")
    hits = [i for i, line in enumerate(lines) if line.rstrip(b"\r").decode("utf-8") == anchor]
    if len(hits) != 1:
        raise SystemExit(f"REFUSED anchor matches {len(hits)} lines (need exactly 1): {anchor!r}")
    i = hits[0]
    eol = b"\r\n" if lines[i].endswith(b"\r") else b"\n"
    new = [p.encode("utf-8") + eol for p in payload.split("\n")]
    return b"\n".join(lines[:i]) + (b"\n" if i else b"") + b"".join(new) + b"\n".join(lines[i:])


def delete_block(data: bytes, payload: str) -> bytes:
    lines = data.split(b"\n")
    want = payload.split("\n")
    n = len(want)
    hits = [i for i in range(len(lines) - n + 1)
            if all(lines[i + k].rstrip(b"\r").decode("utf-8") == want[k] for k in range(n))]
    if len(hits) != 1:
        raise SystemExit(f"REFUSED block matches {len(hits)} places (need exactly 1): {want[0]!r}")
    i = hits[0]
    return b"\n".join(lines[:i] + lines[i + n:])


def transform(data: bytes, op: str, anchor: str, payload: str) -> bytes:
    if op == "insert-before":
        return insert_before(data, anchor, payload)
    if op == "delete-block":
        return delete_block(data, payload)
    raise SystemExit(f"REFUSED unknown op {op!r}")


def read_rows() -> list[dict]:
    if not EDITS.exists():
        return []
    rows = [r.rstrip("\r\n").split("\t") for r in EDITS.read_text(encoding="utf-8").splitlines() if r.strip()]
    assert tuple(rows[0]) == HEADER, "EDITS.tsv header mismatch"
    return [dict(zip(HEADER, r)) for r in rows[1:]]


def append_row(row: dict) -> None:
    new = not EDITS.exists()
    with EDITS.open("a", encoding="utf-8", newline="\n") as f:
        if new:
            f.write("\t".join(HEADER) + "\n")
        f.write("\t".join(row[k] for k in HEADER) + "\n")


def do_edit(args, record: bool) -> int:
    path = Path(args.root) / args.file
    text = args.text if args.text is not None else Path(args.text_file).read_text(encoding="utf-8").rstrip("\n")
    before = path.read_bytes()
    after = transform(before, args.op, args.anchor or "", text)
    path.write_bytes(after)
    if record:
        rows = read_rows()
        mine = [r for r in rows if r["file"] == args.file]
        if mine and mine[-1]["after_sha256"] != sha256(before):
            path.write_bytes(before)
            raise SystemExit(f"REFUSED {args.file} is not at the chain's last after_sha256 (drift); edit reverted")
        append_row({"seq": str(len(rows) + 1), "file": args.file, "before_sha256": sha256(before),
                    "after_sha256": sha256(after), "op": args.op, "anchor": json.dumps(args.anchor or ""),
                    "payload": json.dumps(text), "note": args.note or ""})
    print(f"{'RECORDED' if record else 'EDITED'} {args.file} {sha256(before)[:12]} -> {sha256(after)[:12]}")
    return 0


def chains(rows: list[dict]) -> dict[str, list[dict]]:
    out: dict[str, list[dict]] = {}
    for r in rows:
        out.setdefault(r["file"], []).append(r)
    return out


def do_apply(args, verify_only: bool) -> int:
    rc = 0
    for file, chain in chains(read_rows()).items():
        path = Path(args.root) / file
        data = path.read_bytes()
        cur = sha256(data)
        if cur == chain[-1]["after_sha256"]:
            print(f"OK {file} at final {cur[:12]} ({len(chain)} edits)")
            continue
        if verify_only:
            print(f"NOT_FINAL {file} {cur[:12]} != {chain[-1]['after_sha256'][:12]}")
            rc = 2
            continue
        starts = [i for i, r in enumerate(chain) if r["before_sha256"] == cur]
        if not starts:
            print(f"REFUSED {file} sha256 {cur[:12]} is not on the chain (drift); nothing written")
            rc = 3
            continue
        for r in chain[starts[0]:]:
            data = transform(data, r["op"], json.loads(r["anchor"]), json.loads(r["payload"]))
            if sha256(data) != r["after_sha256"]:
                print(f"REFUSED {file} edit {r['seq']} produced {sha256(data)[:12]} != {r['after_sha256'][:12]}")
                return 3
        path.write_bytes(data)
        print(f"APPLIED {file} edits {chain[starts[0]]['seq']}..{chain[-1]['seq']} -> {sha256(data)[:12]}")
    return rc


def main(argv=None) -> int:
    p = argparse.ArgumentParser()
    p.add_argument("mode", choices=("apply", "verify", "edit", "record"))
    p.add_argument("--root", default=".")
    p.add_argument("--file")
    p.add_argument("--op", default="insert-before", choices=("insert-before", "delete-block"))
    p.add_argument("--anchor")
    p.add_argument("--text")
    p.add_argument("--text-file")
    p.add_argument("--note")
    a = p.parse_args(argv)
    if a.mode in ("edit", "record"):
        if not (a.file and a.anchor is not None and (a.text is not None or a.text_file)):
            p.error("edit/record need --file, --anchor and --text or --text-file")
        return do_edit(a, a.mode == "record")
    return do_apply(a, a.mode == "verify")


if __name__ == "__main__":
    sys.exit(main())
