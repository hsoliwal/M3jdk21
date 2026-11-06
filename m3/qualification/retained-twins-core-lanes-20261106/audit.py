# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""audit.py <repo> <ROWS.tsv> <AUDIT.tsv> [--blobs <cache-dir>] : RETAINED_AUDIT v1 (the lane owner's verdicts,
S:/m3-scratch/converge-20261106/retained_audit.py) over the retained-variants source twins of this leaf.

ROWS.tsv: path, label, twin_missing_count, live_blob, retained_blob (blob ids of the live file <path> and of
m3/qualification/master-landing-20261028/retained-variants/<label>/<path> on the audited master). Both blobs come from
one git cat-file --batch (a cache directory of <sha1> files is honoured first), sha1 re-verified. Verdict by line
multiset containment:
  SAME              identical bytes
  LIVE_SUPERSET     every retained line is in live (the live side already holds the variant's content)
  RETAINED_SUPERSET every live line is in the retained copy, which has more lines -> live REGRESSED, restore candidate
  DIVERGENT         both sides have lines the other lacks -> RETAINED_3WAY (restore3.py)
  LIVE_MISSING / RETAINED_GONE  a blob is missing
The member delta (declarations of one side the other lacks: the twin_scan.py LSEM regexes, both directions) is
carried per row; it is evidence, not a verdict. Deterministic, idempotent: the output is a pure function of the blobs."""
import hashlib
import re
import subprocess
import sys
from collections import Counter
from pathlib import Path

JAVA = re.compile(r"^\s*(?:public|protected|private|static|final|native|synchronized|abstract|default|\s)*"
                  r"[\w<>\[\],.? ]+\s+(\w+)\s*\([^;]*$", re.M)
C = re.compile(r"^(?!\s)(?:[\w*]+\s+)+\**(\w+)\s*\([^;]*\)\s*\{?\s*$", re.M)
PY = re.compile(r"^\s*(?:def|class)\s+(\w+)", re.M)
SH = re.compile(r"^\s*(?:function\s+)?(\w+)\s*\(\)\s*\{", re.M)
YML = re.compile(r"^  (\w[\w-]*):\s*$", re.M)
KEYWORDS = {"if", "for", "while", "switch", "return", "new", "else", "catch", "synchronized", "sizeof", "throw"}
HEADER = ("verdict\tlabel\tpath\tlive_blob\tretained_blob\tlive_lines\tretained_lines\tretained_only\tlive_only\t"
          "live_sha256\tretained_sha256\tmembers_missing_in_live\tmembers_missing_in_retained\tnote\n")


def deferrals(out_tsv):
    """DEFER.tsv next to the output (path_regex, verdict, reason): rows another in-flight leaf owns keep their blob ids
    and take that verdict instead of a content verdict (2026-11-06: m3/ports/collections -> CONSOLIDATE_PORT_OWNERS v1)."""
    f = Path(out_tsv).with_name("DEFER.tsv")
    if not f.exists():
        return []
    return [(re.compile(a), v, r) for a, v, r in
            (l.rstrip("\n").split("\t") for l in list(open(f, encoding="utf-8"))[1:] if l.strip())]


def decls(path, text):
    ext = path.rsplit(".", 1)[-1]
    rx = {"java": JAVA, "c": C, "h": C, "cpp": C, "py": PY, "sh": SH, "yml": YML}.get(ext)
    return set() if rx is None else {m.group(1) for m in rx.finditer(text)} - KEYWORDS


def fetch(repo, shas, cache):
    """bytes per blob sha1; cache hits first, the rest through one git cat-file --batch (one lazy fetch batch)."""
    out = {}
    missing = []
    for s in shas:
        f = cache / s if cache else None
        if f is not None and f.exists():
            out[s] = f.read_bytes()
        else:
            missing.append(s)
    if missing:
        listing = Path(sys.argv[3]).with_suffix(".blobs.txt")
        listing.write_text("".join(s + "\n" for s in missing), encoding="utf-8", newline="\n")
        p = subprocess.Popen(["git", "-C", repo, "cat-file", "--batch"], stdin=open(listing, "rb"), stdout=subprocess.PIPE)
        for s in missing:
            hdr = p.stdout.readline().decode().split()
            if len(hdr) < 3 or hdr[1] == "missing":
                out[s] = None
                continue
            size = int(hdr[2])
            data = p.stdout.read(size)
            p.stdout.read(1)
            if hashlib.sha1(b"blob %d\0" % size + data).hexdigest() != s:
                raise SystemExit("REFUSED: blob sha1 mismatch " + s)
            out[s] = data
            if cache:
                (cache / s).write_bytes(data)
    return out


def verdict(live, ret):
    if live is None:
        return "LIVE_MISSING", 0, 0
    if ret is None:
        return "RETAINED_GONE", 0, 0
    if live == ret:
        return "SAME", 0, 0
    l, r = Counter(live.splitlines()), Counter(ret.splitlines())
    r_only, l_only = sum((r - l).values()), sum((l - r).values())
    kind = "LIVE_SUPERSET" if r_only == 0 else ("RETAINED_SUPERSET" if l_only == 0 else "DIVERGENT")
    return kind, r_only, l_only


def main(argv):
    repo, rows_tsv, out_tsv = argv[1:4]
    cache = Path(argv[argv.index("--blobs") + 1]) if "--blobs" in argv else None
    if cache:
        cache.mkdir(parents=True, exist_ok=True)
    rows = [l.rstrip("\n").split("\t") for l in list(open(rows_tsv, encoding="utf-8"))[1:]]
    blobs = fetch(repo, sorted({r[3] for r in rows} | {r[4] for r in rows}), cache)
    defer = deferrals(out_tsv)
    counts = Counter()
    lines = []
    for path, label, _twin_missing, live_b, ret_b in rows:
        live, ret = blobs.get(live_b), blobs.get(ret_b)
        kind, r_only, l_only = verdict(live, ret)
        note = ""
        for rx, v, reason in defer:
            if rx.search(path):
                kind, note = v, reason
                break
        counts[kind] += 1
        lt = live.decode("utf-8", "replace") if live is not None else ""
        rt = ret.decode("utf-8", "replace") if ret is not None else ""
        dl, dr = decls(path, lt), decls(path, rt)
        lines.append("\t".join((kind, label, path, live_b, ret_b, str(len(lt.splitlines())), str(len(rt.splitlines())),
                                str(r_only), str(l_only), hashlib.sha256(live or b"").hexdigest(),
                                hashlib.sha256(ret or b"").hexdigest(), ",".join(sorted(dr - dl))[:400] or "-",
                                ",".join(sorted(dl - dr))[:400] or "-", note or "-")))  # "-": no trailing tab
    with open(out_tsv, "w", encoding="utf-8", newline="\n") as o:
        o.write(HEADER)
        o.write("".join(l + "\n" for l in lines))
    print("RETAINED_AUDIT rows=%d %s" % (len(rows), " ".join(f"{k}={v}" for k, v in sorted(counts.items()))))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
