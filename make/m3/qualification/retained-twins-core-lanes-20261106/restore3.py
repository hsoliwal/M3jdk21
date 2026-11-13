# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""restore3.py <repo> <AUDIT.tsv> <PR_SOURCES.tsv> <out-dir> [--blobs <cache>] [--apply <worktree>]
RETAINED_3WAY v1 (the lane owner's three_way_restore.py outcome names) plus the RETAINED_SUPERSET copy, per live path.

Per live path the audited labels are taken in ascending PR order against the running content (the audited master
blob first, then each accepted result), so several retained copies of one file chain deterministically:
  RETAINED_SUPERSET  -> result := the retained copy                    (action RETAINED_COPY)
  DIVERGENT          -> git merge-file -p --diff3 ours=running base=rewrite(T-path blob at the PR merge-base)
                        theirs=retained copy; CLEAN -> result (3WAY_CLEAN), CONFLICT -> CONFLICTS.tsv row, the
                        diff3 output (both hunks and the base) in <out>/conflicts/<label>/<path>.diff3, no resolution
  a label whose retained lines are all in the running content -> COVERED (no write)
  a CLEAN merge whose output equals the running content         -> 3WAY_CLEAN=live (no write; the owner tool's =ours)
The base path at T is the tree entry of T whose jdk_naming.target_path is the live path (the retained copies went
through the JDK-naming recipe); the base text goes through jdk_naming.rewrite_text like the live file did.
Results are written under <out>/result/<path>; with --apply they are copied into the worktree only when the file there
still has the ledger's sha256 pin (the audited blob, or the previous result of the chain) or already equals the result
(idempotent). RESTORE_LEDGER.tsv and CONFLICTS.tsv are rewritten in full on every run."""
import hashlib
import os
import subprocess
import sys
import tempfile
from collections import Counter
from pathlib import Path

HERE = Path(__file__).resolve()
sys.path.insert(0, str(HERE.parents[2] / "tooling" / "jdk-naming"))
import jdk_naming as jn  # noqa: E402

PREFIX = "m3/qualification/master-landing-20261028/retained-variants/"
LEDGER = ("action\tlabel\tpath\tbase_commit\tT_head\tbase_path_at_T\tpin_sha256_before\tresult_sha256\tbytes\t"
          "theirs_eq_renamed_head\tnote\n")
CONFLICTS = "label\tpath\tbase_commit\tT_head\tbase_path_at_T\tconflicts\tconflict_file\tretained_only\tlive_only\tnote\n"


def sha256(b):
    return hashlib.sha256(b).hexdigest()


def git(repo, *args, binary=False):
    r = subprocess.run(["git", "-C", repo, *args], capture_output=True)
    return r.stdout if binary else r.stdout.decode("utf-8", "replace")


def cat_batch(repo, shas, cache):
    out = {}
    todo = [s for s in shas if s and not (cache and (cache / s).exists())]
    for s in shas:
        if s and cache and (cache / s).exists():
            out[s] = (cache / s).read_bytes()
    if todo:
        with tempfile.NamedTemporaryFile("w", delete=False, dir=os.environ.get("TMP")) as f:
            f.write("".join(s + "\n" for s in todo))
        p = subprocess.Popen(["git", "-C", repo, "cat-file", "--batch"], stdin=open(f.name, "rb"), stdout=subprocess.PIPE)
        for s in todo:
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


def tree_map(repo, rev, prefixes):
    """{path: blob} of REV under PREFIXES (and their pre-rename spellings), trees only, no blob fetch."""
    spell = set()
    for p in prefixes:
        spell.add(p)
        a, _, b = p.partition("/")
        if b.startswith("m3-"):
            spell.add(a + "/synexia-" + b[3:])
    out = {}
    for line in git(repo, "ls-tree", "-r", rev, "--", *sorted(spell)).splitlines():
        meta, path = line.split("\t", 1)
        out[path] = meta.split()[2]
    return out


def base_entry(repo, T, mb, live_path, cache_trees):
    """(path at T, blob at T, blob at merge-base) for the live path: the T entry whose renamed path is the live path."""
    pfx = "/".join(live_path.split("/")[:2])
    key = (T, pfx)
    if key not in cache_trees:
        cache_trees[key] = tree_map(repo, T, [pfx])
    tmap = cache_trees[key]
    tpath = live_path if live_path in tmap else None
    if tpath is None:
        for p in tmap:
            if jn.target_path(p) == live_path:
                tpath = p
                break
    if tpath is None:
        return None, None, None
    bkey = (mb, pfx)
    if bkey not in cache_trees:
        cache_trees[bkey] = tree_map(repo, mb, [pfx])
    return tpath, tmap[tpath], cache_trees[bkey].get(tpath)


def covered(running, theirs):
    r, l = Counter(theirs.splitlines()), Counter(running.splitlines())
    return sum((r - l).values()) == 0


def merge(ours, base, theirs):
    with tempfile.TemporaryDirectory(dir=os.environ.get("TMP")) as d:
        fo, fb, ft = (Path(d) / n for n in ("ours", "base", "theirs"))
        fo.write_bytes(ours)
        fb.write_bytes(base)
        ft.write_bytes(theirs)
        r = subprocess.run(["git", "merge-file", "-p", "--diff3", "-L", "live", "-L", "base", "-L", "retained",
                            str(fo), str(fb), str(ft)], capture_output=True)
    return r.returncode, r.stdout


def main(argv):
    repo, audit_tsv, sources_tsv, out = argv[1], argv[2], argv[3], Path(argv[4])
    cache = Path(argv[argv.index("--blobs") + 1]) if "--blobs" in argv else None
    worktree = Path(argv[argv.index("--apply") + 1]) if "--apply" in argv else None
    sources = {}
    for l in list(open(sources_tsv, encoding="utf-8"))[1:]:
        pr, _retain, _p1, T, mb = l.rstrip("\n").split("\t")[:5]
        sources["pr" + pr] = (T, mb)
    rows = [l.rstrip("\n").split("\t") for l in list(open(audit_tsv, encoding="utf-8"))[1:]]
    rows = [r for r in rows if r[0] in ("RETAINED_SUPERSET", "DIVERGENT")]
    by_path = {}
    for r in rows:
        by_path.setdefault(r[2], []).append(r)
    blobs = cat_batch(repo, sorted({r[3] for r in rows} | {r[4] for r in rows}), cache)
    trees = {}
    base_shas = {}
    for path, group in by_path.items():
        for r in group:
            T, mb = sources[r[1]]
            base_shas[(r[1], path)] = base_entry(repo, T, mb, path, trees)
    extra = sorted({v[1] for v in base_shas.values() if v[1]} | {v[2] for v in base_shas.values() if v[2]})
    blobs.update(cat_batch(repo, extra, cache))
    ledger, conflicts, counts = [], [], Counter()
    results = {}
    for path in sorted(by_path):
        group = sorted(by_path[path], key=lambda r: int(r[1][2:]))
        running = blobs[group[0][3]]
        pin = sha256(running)
        for r in group:
            kind, label = r[0], r[1]
            T, mb = sources[label]
            theirs = blobs[r[4]]
            tpath, tblob, bblob = base_shas[(label, path)]
            renamed_head = jn.encoded(jn.rewrite_text(jn.decoded(blobs[tblob]))[0]) if tblob else None
            eq = "yes" if renamed_head == theirs else ("no" if renamed_head is not None else "no-T-entry")
            if covered(running, theirs):
                ledger.append(("COVERED", label, path, mb[:12], T[:12], tpath or "", pin, pin, str(len(running)), eq,
                               "every retained line already in the running content"))
                counts["COVERED"] += 1
                continue
            if kind == "RETAINED_SUPERSET":
                result, note, action = theirs, "live := retained copy", "RETAINED_COPY"
            else:
                base = jn.encoded(jn.rewrite_text(jn.decoded(blobs[bblob]))[0]) if bblob else b""
                rc, merged = merge(running, base, theirs)
                if rc != 0:
                    cf = out / "conflicts" / label / (path + ".diff3")  # not a source file: builds and scanners skip it
                    cf.parent.mkdir(parents=True, exist_ok=True)
                    cf.write_bytes(merged)
                    rl, ll = Counter(theirs.splitlines()), Counter(running.splitlines())
                    conflicts.append((label, path, mb[:12], T[:12], tpath or "NONE", str(rc),
                                      str(cf.relative_to(out)).replace("\\", "/"), str(sum((rl - ll).values())),
                                      str(sum((ll - rl).values())),
                                      "base=NONE: added by the PR, no common ancestor" if not bblob else
                                      ("base path not found at T" if not tpath else "owner review, no resolution")))
                    counts["CONFLICT"] += 1
                    continue
                result, action = merged, "3WAY_CLEAN"
                note = "git merge-file -p live base retained; base " + ("NONE (added by the PR)" if not bblob else "at merge-base")
                if merged == running:  # the owner tool's CLEAN=ours: the live side already made the retained change or
                    # removed the retained-only lines after the base; nothing to restore, no write
                    ledger.append(("3WAY_CLEAN=live", label, path, mb[:12], T[:12], tpath or "", pin, pin,
                                   str(len(running)), eq, note + "; result equals the running content"))
                    counts["3WAY_CLEAN=live"] += 1
                    continue
            rs = sha256(result)
            rf = out / "result" / path
            rf.parent.mkdir(parents=True, exist_ok=True)
            rf.write_bytes(result)
            ledger.append((action, label, path, mb[:12], T[:12], tpath or "", pin, rs, str(len(result)), eq, note))
            counts[action] += 1
            running, pin = result, rs
            results[path] = (group[0][3], result)
    applied = Counter()
    held_tsv = Path(audit_tsv).with_name("HELD.tsv")  # hold.py: gate-failing restores live under held/<path>, not live
    held = {l.split("\t")[0] for l in list(open(held_tsv, encoding="utf-8"))[1:]} if held_tsv.exists() else set()
    if worktree:
        for path, (live_blob, result) in sorted(results.items()):
            if path in held:
                applied["HELD"] += 1
                continue
            target = worktree / path
            current = target.read_bytes() if target.exists() else b""
            first_pin = sha256(blobs[live_blob])
            if current == result:
                applied["ALREADY"] += 1
            elif sha256(current) == first_pin:
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(result)
                applied["WRITTEN"] += 1
            else:
                applied["REFUSED_DRIFT"] += 1
                print("REFUSED_DRIFT", path, sha256(current)[:12], "expected", first_pin[:12], file=sys.stderr)
    out.mkdir(parents=True, exist_ok=True)
    with open(out / "RESTORE_LEDGER.tsv", "w", encoding="utf-8", newline="\n") as o:
        o.write(LEDGER)
        o.write("".join("\t".join(r) + "\n" for r in ledger))
    with open(out / "CONFLICTS.tsv", "w", encoding="utf-8", newline="\n") as o:
        o.write(CONFLICTS)
        o.write("".join("\t".join(r) + "\n" for r in conflicts))
    print("RETAINED_3WAY rows=%d paths=%d %s%s" % (len(rows), len(by_path), " ".join(f"{k}={v}" for k, v in sorted(counts.items())),
                                                  (" apply: " + " ".join(f"{k}={v}" for k, v in sorted(applied.items()))) if worktree else ""))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
