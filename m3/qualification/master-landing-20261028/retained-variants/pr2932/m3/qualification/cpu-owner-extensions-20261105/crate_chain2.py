"""crate_chain2.py <crate-dir-on-develop> <m3jdk-git-dir> <m3jdk-sha> <rename-parent-sha> <naming-dir> <out-dir>
HASH_PINNED_CHAIN v2: crate_chain.py for crates sealed before the JDK naming rename (fe35ea98841).
Manifests pin pre-rename paths and bytes. Per crate target T:
  start  = sha256 of <rename-parent>:T (ABSENT if missing there), which must be a pinned preimage of T;
  chain  = follow pre->post edges while exactly one leaves the current hash; the final dataset bytes are checked
           against their pinned postimage sha256;
  named  = jdk_naming.rewrite_text over the start bytes and the final bytes; master path = jdk_naming.target_path(T).
Verdicts per target:
  APPLY        master bytes == named(start): master has not drifted since the rename; writes named(final) to
               <out-dir>/<master path>
  NEW          start ABSENT and master path absent: writes named(final)
  DRIFT        master bytes != named(start) (owner changed after the rename): no write, owner review / 3-way
  NO_START     the rename-parent bytes match no pinned preimage
  FORK/BAD_PIN as in v1."""
import hashlib
import json
import os
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

crate, gd, ref, rparent, naming_dir, out = sys.argv[1:7]
out = Path(out)
sys.path.insert(0, naming_dir)
import jdk_naming  # noqa: E402

REPO = "hsoliwal/com.synexia"


def gh_json(path):
    r = subprocess.run(["gh", "api", path], capture_output=True, text=True, encoding="utf-8")
    return json.loads(r.stdout) if r.returncode == 0 and r.stdout.strip() else None


def gh_raw(path):
    r = subprocess.run(["gh", "api", path, "-H", "Accept: application/vnd.github.raw"], capture_output=True)
    return r.stdout if r.returncode == 0 else None


def blob(rev, path):
    r = subprocess.run(["git", "cat-file", "blob", f"{rev}:{path}"], capture_output=True, env=dict(os.environ, GIT_DIR=gd))
    return r.stdout if r.returncode == 0 else None


def named(data):
    return jdk_naming.encoded(jdk_naming.rewrite_text(jdk_naming.decoded(data))[0])


tree = gh_json(f"repos/{REPO}/git/trees/develop:{crate}?recursive=1")["tree"]
edges = defaultdict(list)
for m in [t["path"] for t in tree if t["type"] == "blob" and t["path"].endswith("manifest.tsv")]:
    base = m.rsplit("/", 1)[0]
    for row in gh_raw(f"repos/{REPO}/contents/{crate}/{m}?ref=develop").decode("utf-8").splitlines():
        cells = row.split("\t")
        if row.startswith("#") or len(cells) < 3 or len(cells[2]) != 64:
            continue
        name = cells[3] if len(cells) > 3 and cells[3] else cells[0].rsplit("/", 1)[-1] + ".txt"
        edges[(cells[0], cells[1])].append((cells[2], f"{base}/{name}", m.split("/")[0]))
for target in sorted({t for t, _ in edges}):
    start_bytes = blob(rparent, target)
    cur = hashlib.sha256(start_bytes).hexdigest() if start_bytes is not None else "ABSENT"
    mpath = jdk_naming.target_path(target)
    if (target, cur) not in edges:
        print(f"NO_START\t{mpath}\trename-parent={cur[:12]}")
        continue
    steps, last = [], None
    while True:
        nxt = list({e[0]: e for e in edges.get((target, cur), [])}.values())
        if len(nxt) != 1 or len(steps) > 20:
            if len(nxt) > 1:
                print(f"FORK\t{mpath}\tat {cur[:12]}\t{[e[2] for e in nxt]}")
                last = None
            break
        cur, last = nxt[0][0], nxt[0]
        steps.append(nxt[0][2])
    if last is None:
        continue
    final = gh_raw(f"repos/{REPO}/contents/{crate}/{last[1]}?ref=develop")
    if final is None or hashlib.sha256(final).hexdigest() != last[0]:
        print(f"BAD_PIN\t{mpath}\t{last[1]}")
        continue
    master = blob(ref, mpath)
    if start_bytes is None:
        verdict = "NEW" if master is None else "DRIFT"
    else:
        verdict = "APPLY" if master == named(start_bytes) else "DRIFT"
    if verdict in ("APPLY", "NEW"):
        dst = out / mpath
        dst.parent.mkdir(parents=True, exist_ok=True)
        dst.write_bytes(named(final))
    print(f"{verdict}\t{mpath}\tsteps={'>'.join(steps)}\tfinal={last[0][:12]}")
