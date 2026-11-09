"""lost_now.py <rows.tsv> <master-sha> <out.tsv> : MERGED_FOLD_LOST_NOW v1.
For each MERGED_FOLD_DROPS row, the parent-side file (parent:path) is compared with master's file at master_path
(path when master_path is empty). A lost line is a non-blank, non-comment line of the parent side, whitespace-
normalised, that master's file does not contain. Blobs come from the local store when present (no lazy fetch: the
store's disk is full), else from the GitHub contents API; every (rev, path) is read once."""
import functools
import os
import subprocess
import sys

rows_path, master, out_path = sys.argv[1:4]
GD = "H:/m3-scratch/repos/M3jdk21-release.git"
ENV = dict(os.environ, GIT_DIR=GD, GIT_NO_LAZY_FETCH="1")


@functools.lru_cache(maxsize=None)
def blob(rev, path):
    r = subprocess.run(["git", "show", f"{rev}:{path}"], capture_output=True, env=ENV)
    if r.returncode == 0:
        return r.stdout.decode("utf-8", "replace")
    r = subprocess.run(["gh", "api", f"repos/hsoliwal/M3jdk21/contents/{path}?ref={rev}", "-H",
                        "Accept: application/vnd.github.raw"], capture_output=True)
    return r.stdout.decode("utf-8", "replace") if r.returncode == 0 and not r.stdout.startswith(b'{"message"') else None


def norm(text):
    out = []
    for line in text.splitlines():
        s = " ".join(line.split())
        if s and not s.startswith(("//", "*", "/*", "#")):
            out.append(s)
    return out


rows = [l.rstrip("\n").split("\t") for l in open(rows_path, encoding="utf-8")]
col = {n: i for i, n in enumerate(rows[0])}
with open(out_path, "w", encoding="utf-8", newline="\n") as out:
    out.write("merge\tpr\tpath\tmaster_path\tstatus\tverdict\tparent\tparent_lines\tlost_now\tmaster_state\tfirst_lost\n")
    for r in rows[1:]:
        path, mpath = r[col["path"]], r[col["master_path"]] or r[col["path"]]
        if mpath == "-":
            mpath = path
        side = blob(r[col["parent"]], path)
        now = blob(master, mpath)
        if side is None:
            state, lost, n = "PARENT_UNREADABLE", [], 0
        elif now is None:
            state, n = "ABSENT_ON_MASTER", len(norm(side))
            lost = norm(side)
        else:
            have = set(norm(now))
            n = len(norm(side))
            lost = [s for s in norm(side) if s not in have]
            state = "PRESENT"
        out.write("\t".join([r[col["merge"]][:12], r[col["pr"]], path, mpath, r[col["status"]], r[col["verdict"]],
                             r[col["parent"]][:12], str(n), str(len(lost)), state,
                             (lost[0][:100] if lost else "-")]) + "\n")
print("done")
