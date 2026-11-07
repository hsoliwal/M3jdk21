"""restore_retained4.py <owner/repo> <base-branch-or-sha> <new-branch> <rows.tsv> <message-file> : RETAINED_RESTORE v4.
rows: tag<TAB>live-path  -> live path := bytes of retained-variants/<tag>/<live-path> (blob reused, no upload)
      FILE<TAB>repo-path<TAB>local-path -> repo path := bytes of a local file (gate script, evidence)
      BLOB<TAB>repo-path<TAB>blob-sha<TAB>mode -> repo path := an existing blob of the repository (byte-exact
                                                restore from history: no download, no upload; mode 100644/100755)
One commit on <new-branch> cut from <base>: one Trees API request (base_tree + nested blob paths, no root
rewrite), author Hitesh Soliwal, message read from <message-file> (UTF-8). Prints the commit sha.
v4 over v3: BLOB rows restore merge-dropped paths straight from their historical blob ids, and a FILE row keeps
the executable bit when its local mode column says 100755."""
import base64
import json
import re
import subprocess
import sys

PREFIX = "m3/qualification/master-landing-20261028/retained-variants/"
MODES = ("100644", "100755")


def gh(args, body=None):
    r = subprocess.run(["gh", "api", *args] + (["--input", "-"] if body is not None else []),
                       input=json.dumps(body) if body is not None else None, capture_output=True, text=True,
                       encoding="utf-8", check=True)
    return json.loads(r.stdout) if r.stdout.strip() else {}


def entry(path, sha, mode="100644"):
    if mode not in MODES:
        sys.exit(f"REFUSE mode {mode} for {path}")
    return {"path": path, "mode": mode, "type": "blob", "sha": sha}


repo, base, new, rows, msg_file = sys.argv[1:6]
head = base if re.fullmatch(r"[0-9a-f]{40}", base) else gh([f"repos/{repo}/git/ref/heads/{base}"])["object"]["sha"]
tree = gh([f"repos/{repo}/git/commits/{head}"])["tree"]["sha"]
entries, seen = [], set()
for line in open(rows, encoding="utf-8").read().splitlines():
    cols = line.split("\t")
    if cols[1] in seen:
        sys.exit(f"REFUSE duplicate path {cols[1]}")
    seen.add(cols[1])
    if cols[0] == "FILE":
        data = base64.b64encode(open(cols[2], "rb").read()).decode("ascii")
        blob = gh(["-X", "POST", f"repos/{repo}/git/blobs"], {"content": data, "encoding": "base64"})["sha"]
        entries.append(entry(cols[1], blob, cols[3] if len(cols) > 3 else "100644"))
    elif cols[0] == "BLOB":
        if not re.fullmatch(r"[0-9a-f]{40}", cols[2]):
            sys.exit(f"REFUSE blob id {cols[2]!r} for {cols[1]}")
        entries.append(entry(cols[1], cols[2], cols[3]))
    else:
        tag, path = cols[0], cols[1]
        blob = gh([f"repos/{repo}/contents/{PREFIX}{tag}/{path}?ref={head}"])["sha"]
        entries.append(entry(path, blob))
new_tree = gh(["-X", "POST", f"repos/{repo}/git/trees"], {"base_tree": tree, "tree": entries})["sha"]
msg = open(msg_file, encoding="utf-8").read().strip()
commit = gh(["-X", "POST", f"repos/{repo}/git/commits"],
            {"message": msg, "tree": new_tree, "parents": [head],
             "author": {"name": "Hitesh Soliwal", "email": "hsoliwal@gmail.com"}})["sha"]
gh(["-X", "POST", f"repos/{repo}/git/refs"], {"ref": f"refs/heads/{new}", "sha": commit})
print(commit)
