#!/usr/bin/env python3
"""apply: remove the SECOND byte-identical definition of each PURE_DUP candidate.

Recipe DUP_GUARD_REMOVE_SECOND v1
  input   DUP_CANDIDATES.tsv (dup_guard.py scan), rows verdict == PURE_DUP
  action  per file: rescan with dup_guard, locate the second member of each
          PURE_DUP (kind, scope, signature), delete its source lines
          (leading javadoc/annotations included, one adjacent blank line
          collapsed), repeat until the file has no matching candidate.
  proof   file preimage/postimage sha256 pinned in RECIPE_LEDGER.tsv;
          removed member sha256 == blob_b of the candidate row; rescan of
          the postimage yields no PURE_DUP for the key.
  idempotent  current sha == recorded postimage -> ALREADY_APPLIED on stdout,
              ledger rows unchanged (no write to the source)
              current sha == recorded preimage  -> re-apply (same rows)
              otherwise with ledger rows present -> HOLD:PREIMAGE_MISMATCH
  never   DIVERGENT / FALSE_POSITIVE rows, --skip keys, non-identical texts,
          paths outside --include (no build gate available -> listed only).

Usage
  python apply.py --root <worktree> --candidates DUP_CANDIDATES.tsv \
      --ledger RECIPE_LEDGER.tsv [--include m3/ src/jdk.m3.graphics/] \
      [--skip path::signature ...] [--dry-run]
"""
from __future__ import annotations

import argparse
import hashlib
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dup_guard  # noqa: E402

LEDGER_HEADER = ["path", "kind", "signature", "scope", "span_removed", "member_sha256",
                 "file_preimage_sha256", "file_postimage_sha256", "status", "recipe"]
RECIPE = "DUP_GUARD_REMOVE_SECOND v1"


def sha256_bytes(b: bytes) -> str:
    return hashlib.sha256(b).hexdigest()


def read_tsv(path: str) -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    if not os.path.isfile(path):
        return rows
    with open(path, encoding="utf-8", newline="") as fh:
        header = None
        for line in fh:
            parts = line.rstrip("\r\n").split("\t")
            if header is None:
                header = parts
                continue
            rows.append(dict(zip(header, parts)))
    return rows


def write_tsv(path: str, header: list[str], rows: list[list[str]]) -> None:
    with open(path, "w", encoding="utf-8", newline="") as fh:
        fh.write("\t".join(header) + "\n")
        for r in rows:
            fh.write("\t".join(r) + "\n")


def members_of(rel: str, text: str) -> list[dup_guard.Member]:
    if rel.endswith(".java"):
        ms, _ = dup_guard.classify_java_file(rel, text)
    else:
        ms, _, _ = dup_guard.classify_c_file(rel, text)
    return ms


def key_of(kind: str, scope: str, sig: str) -> tuple[str, str, str]:
    return (kind, scope, sig)


def remove_second(text: str, rel: str, want: set[tuple[str, str, str]]):
    """Remove one duplicate (the second occurrence) for the first matching key.
    Returns (new_text, removed_member) or (text, None)."""
    ms = members_of(rel, text)
    seen: dict[tuple[str, str, str], dup_guard.Member] = {}
    for m in ms:
        if m.kind == "type":
            continue
        k = key_of(m.kind, m.scope, m.sig)
        if k in seen:
            if k in want and seen[k].text == m.text:
                li = dup_guard.LineIndex(text)
                start = li.line_start(m.span_start)
                end = li.starts[m.span_end] if m.span_end < len(li.starts) else len(text)
                # guard: nothing but whitespace/comments between line start and code start
                lead = text[start:m.code_off]
                if dup_guard.mask_java(lead).strip():
                    return text, None, f"HOLD:LEADING_CODE_ON_SPAN_LINE {rel}:{m.span_start}"
                if m.end_off + 1 > end or text[m.end_off + 1:end].strip():
                    return text, None, f"HOLD:TRAILING_CODE_ON_SPAN_LINE {rel}:{m.span_end}"
                new = text[:start] + text[end:]
                # collapse one blank line if both neighbours are blank
                before = new[:start]
                after = new[start:]
                if before.endswith("\n\n") and (after.startswith("\n") or after.startswith("\r\n")):
                    after = after[2:] if after.startswith("\r\n") else after[1:]
                    new = before + after
                return new, m, "APPLIED"
            # not identical / not wanted: keep scanning for another key
        else:
            seen[k] = m
    return text, None, "NONE"


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--root", required=True)
    ap.add_argument("--candidates", required=True)
    ap.add_argument("--ledger", required=True)
    ap.add_argument("--skip", nargs="*", default=[], help="path::signature[=REASON] keys never touched")
    ap.add_argument("--include", nargs="*", default=None,
                    help="path prefixes with a build gate; other rows -> SKIPPED:OUT_OF_GATE_SCOPE")
    ap.add_argument("--kinds", default="method,ctor,field,import,function")
    ap.add_argument("--dry-run", action="store_true")
    a = ap.parse_args(argv)
    root = os.path.abspath(a.root)
    kinds = set(a.kinds.split(","))
    skip: dict[str, str] = {}
    for s in a.skip:   # path::signature[=REASON]
        k, _, reason = s.partition("=")
        skip[k] = reason or "OTHER_PR"
    cands = [r for r in read_tsv(a.candidates) if r["verdict"] == "PURE_DUP" and r["kind"] in kinds]
    out_of_scope: list[dict[str, str]] = []
    if a.include is not None:
        out_of_scope = [r for r in cands if not any(r["path"].startswith(p) for p in a.include)]
        cands = [r for r in cands if any(r["path"].startswith(p) for p in a.include)]
    old_ledger = read_tsv(a.ledger)
    by_path: dict[str, list[dict[str, str]]] = {}
    for r in cands:
        by_path.setdefault(r["path"], []).append(r)
    out_rows: list[list[str]] = []
    counts: dict[str, int] = {}

    def bump(k: str) -> None:
        counts[k] = counts.get(k, 0) + 1

    for r in out_of_scope:
        out_rows.append([r["path"], r["kind"], r["signature"], r["scope"], "", r["blob_b"], "", "",
                         "SKIPPED:OUT_OF_GATE_SCOPE", RECIPE])
        bump("SKIPPED")

    for rel in sorted(by_path):
        rows = by_path[rel]
        full = os.path.join(root, rel)
        want: set[tuple[str, str, str]] = set()
        for r in rows:
            k = f"{rel}::{r['signature']}"
            if k in skip:
                out_rows.append([rel, r["kind"], r["signature"], r["scope"], "", r["blob_b"], "", "",
                                 f"SKIPPED:{skip[k]}", RECIPE])
                bump("SKIPPED")
                continue
            want.add(key_of(r["kind"], r["scope"], r["signature"]))
        if not want:
            continue
        raw = open(full, "rb").read()
        cur = sha256_bytes(raw)
        prior = [r for r in old_ledger if r["path"] == rel and r["status"] in ("APPLIED", "ALREADY_APPLIED")]
        if prior:
            if cur == prior[-1]["file_postimage_sha256"]:
                # fixed point: the ledger row is immutable (status stays as recorded); report only
                for r in prior:
                    out_rows.append([rel, r["kind"], r["signature"], r["scope"], r["span_removed"], r["member_sha256"],
                                     r["file_preimage_sha256"], r["file_postimage_sha256"], r["status"], RECIPE])
                    bump("ALREADY_APPLIED")
                continue
            if cur != prior[0]["file_preimage_sha256"]:
                for r in rows:
                    out_rows.append([rel, r["kind"], r["signature"], r["scope"], "", r["blob_b"], cur, "",
                                     "HOLD:PREIMAGE_MISMATCH", RECIPE])
                    bump("HOLD")
                continue
        text = raw.decode("utf-8", errors="surrogateescape")
        pre = cur
        steps: list[list[str]] = []
        while True:
            new, m, status = remove_second(text, rel, want)
            if m is None:
                if status.startswith("HOLD"):
                    steps.append([rel, "", "", "", "", "", pre, "", status, RECIPE])
                    bump("HOLD")
                break
            new_b = new.encode("utf-8", errors="surrogateescape")
            post = sha256_bytes(new_b)
            steps.append([rel, m.kind, m.sig, m.scope, f"{m.span_start}-{m.span_end}", dup_guard.sha256_text(m.text),
                          pre, post, "APPLIED", RECIPE])
            bump("APPLIED")
            text = new
            pre = post
        # proof: no remaining PURE_DUP for wanted keys
        ms = members_of(rel, text)
        remaining = [c for c in dup_guard.pair_candidates(rel, ms)
                     if c.verdict == "PURE_DUP" and key_of(c.kind, c.scope, c.sig) in want]
        if remaining:
            steps.append([rel, "", "", "", "", "", pre, "", f"HOLD:REMAINING_PURE_DUP {len(remaining)}", RECIPE])
            bump("HOLD")
        out_rows.extend(steps)
        if not a.dry_run and any(s[8] == "APPLIED" for s in steps):
            with open(full, "wb") as fh:
                fh.write(text.encode("utf-8", errors="surrogateescape"))
    out_rows.sort(key=lambda r: (r[0], r[4], r[2]))
    if not a.dry_run:
        write_tsv(a.ledger, LEDGER_HEADER, out_rows)
    for r in out_rows:
        print("\t".join(r[:5] + [r[8]]))
    print(f"summary {counts} dry_run={a.dry_run}")
    return 1 if counts.get("HOLD") else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
