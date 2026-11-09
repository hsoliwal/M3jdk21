"""restore_retained3.py <owner/repo> <base-branch-or-sha> <new-branch> <rows.tsv> <message-file> : RETAINED_RESTORE v3.
rows: tag<TAB>live-path  -> live path := bytes of retained-variants/<tag>/<live-path> (blob reused, no upload)
      FILE<TAB>repo-path<TAB>local-path -> repo path := bytes of a local file (gate script, evidence)
One commit on <new-branch> cut from <base>: one Trees API request (base_tree + nested blob paths, no root
rewrite), author Hitesh Soliwal, message read from <message-file> (UTF-8). Prints the commit sha.
v3 over v1: base may be a sha, the message is an argument, local files ride in the same commit."""
import base64
import json
import re
import subprocess
import sys

PREFIX = "m3/qualification/master-landing-20261028/retained-variants/"


def gh(args, body=None):
    r = subprocess.run(["gh", "api", *args] + (["--input", "-"] if body is not None else []),
                       input=json.dumps(body) if body is not None else None, capture_output=True, text=True,
                       encoding="utf-8", check=True)
    return json.loads(r.stdout) if r.stdout.strip() else {}


repo, base, new, rows, msg_file = sys.argv[1:6]
head = base if re.fullmatch(r"[0-9a-f]{40}", base) else gh([f"repos/{repo}/git/ref/heads/{base}"])["object"]["sha"]
tree = gh([f"repos/{repo}/git/commits/{head}"])["tree"]["sha"]
entries = []
for line in open(rows, encoding="utf-8").read().splitlines():
    cols = line.split("\t")
    if cols[0] == "FILE":
        data = base64.b64encode(open(cols[2], "rb").read()).decode("ascii")
        blob = gh(["-X", "POST", f"repos/{repo}/git/blobs"], {"content": data, "encoding": "base64"})["sha"]
        entries.append({"path": cols[1], "mode": "100644", "type": "blob", "sha": blob})
    else:
        tag, path = cols[0], cols[1]
        blob = gh([f"repos/{repo}/contents/{PREFIX}{tag}/{path}?ref={head}"])["sha"]
        entries.append({"path": path, "mode": "100644", "type": "blob", "sha": blob})
new_tree = gh(["-X", "POST", f"repos/{repo}/git/trees"], {"base_tree": tree, "tree": entries})["sha"]
msg = open(msg_file, encoding="utf-8").read().strip()
commit = gh(["-X", "POST", f"repos/{repo}/git/commits"],
            {"message": msg, "tree": new_tree, "parents": [head],
             "author": {"name": "Hitesh Soliwal", "email": "hsoliwal@gmail.com"}})["sha"]
gh(["-X", "POST", f"repos/{repo}/git/refs"], {"ref": f"refs/heads/{new}", "sha": commit})
print(commit)
