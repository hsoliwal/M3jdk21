"""three_way_rows.py <rows.tsv> <master-sha> <out-dir> : MERGED_FOLD_THREE_WAY v1.
For each MERGED_FOLD_DROPS row: ours = master:master_path (path when empty), base = merge_base:path,
theirs = parent:path; git merge-file --diff3. Verdicts per row:
  ALREADY        merged == ours (master already holds the parent-side change, or the change was empty)
  CLEAN          merges without conflict and adds the parent-side change to master (restorable as-is)
  CONFLICT=n     n conflict hunks (owner review)
  ABSENT_OURS    master has no file there (parent-side file never reached master; restore candidate)
  NO_BASE        the path did not exist at the merge base (added on the parent side): theirs vs ours compared
                 directly (SAME / DIFFERENT)
Blobs come from the local store when present (no lazy fetch), else the GitHub contents API; cached per (rev, path).
Writes <out-dir>/<n>.{ours,base,theirs,merged} for non-ALREADY rows and <out-dir>/VERDICTS.tsv."""
import functools
import os
import subprocess
import sys
import tempfile
from pathlib import Path

rows_path, master, out_dir = sys.argv[1], sys.argv[2], Path(sys.argv[3])
out_dir.mkdir(parents=True, exist_ok=True)
ENV = dict(os.environ, GIT_DIR="H:/m3-scratch/repos/M3jdk21-release.git", GIT_NO_LAZY_FETCH="1")


@functools.lru_cache(maxsize=None)
def blob(rev, path):
    r = subprocess.run(["git", "show", f"{rev}:{path}"], capture_output=True, env=ENV)
    if r.returncode == 0:
        return r.stdout
    r = subprocess.run(["gh", "api", f"repos/hsoliwal/M3jdk21/contents/{path}?ref={rev}", "-H",
                        "Accept: application/vnd.github.raw"], capture_output=True)
    if r.returncode != 0 or r.stdout.startswith(b'{"message"'):
        return None
    return r.stdout


rows = [l.rstrip("\n").split("\t") for l in open(rows_path, encoding="utf-8")]
col = {n: i for i, n in enumerate(rows[0])}
with open(out_dir / "VERDICTS.tsv", "w", encoding="utf-8", newline="\n") as out:
    out.write("n\tmerge\tpr\tpath\tmaster_path\tverdict\tdetail\n")
    for n, r in enumerate(rows[1:], 1):
        path = r[col["path"]]
        mpath = r[col["master_path"]] if r[col["master_path"]] not in ("", "-") else path
        ours, base, theirs = blob(master, mpath), blob(r[col["merge_base"]], path), blob(r[col["parent"]], path)
        detail = "-"
        if theirs is None:
            verdict = "NO_THEIRS"
        elif ours is None:
            verdict = "ABSENT_OURS"
        elif base is None:
            verdict = "NO_BASE"
            detail = "SAME" if theirs == ours else "DIFFERENT"
        else:
            with tempfile.TemporaryDirectory(dir=str(out_dir)) as d:
                o, b, t = (Path(d) / x for x in ("o", "b", "t"))
                o.write_bytes(ours); b.write_bytes(base); t.write_bytes(theirs)
                rc = subprocess.run(["git", "merge-file", "-p", "--diff3", str(o), str(b), str(t)],
                                    capture_output=True)
                merged = rc.stdout
            if rc.returncode == 0 and merged == ours:
                verdict = "ALREADY"
            elif rc.returncode == 0:
                verdict = "CLEAN"
            else:
                verdict = f"CONFLICT={rc.returncode}"
        if verdict != "ALREADY":
            for suffix, data in (("ours", ours), ("base", base), ("theirs", theirs)):
                if data is not None:
                    (out_dir / f"{n}.{suffix}").write_bytes(data)
            if verdict == "CLEAN" or verdict.startswith("CONFLICT"):
                (out_dir / f"{n}.merged").write_bytes(merged)
        out.write("\t".join([str(n), r[col["merge"]][:12], r[col["pr"]], path, mpath, verdict, detail]) + "\n")
print("done")
