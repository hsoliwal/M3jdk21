"""crate_edges.py <crate-dir-on-develop> <path-substring>... : prints every manifest edge (sub-crate, pre12 -> post12,
dataset) of a hash-pinned crate for targets containing one of the substrings, so forks can be read as a graph."""
import json
import subprocess
import sys

crate, subs = sys.argv[1], sys.argv[2:]
REPO = "hsoliwal/com.synexia"


def gh(path, raw=False):
    args = ["gh", "api", path] + (["-H", "Accept: application/vnd.github.raw"] if raw else [])
    r = subprocess.run(args, capture_output=True)
    return r.stdout if raw else json.loads(r.stdout)


tree = gh(f"repos/{REPO}/git/trees/develop:{crate}?recursive=1")["tree"]
for m in [t["path"] for t in tree if t["type"] == "blob" and t["path"].endswith("manifest.tsv")]:
    for row in gh(f"repos/{REPO}/contents/{crate}/{m}?ref=develop", raw=True).decode("utf-8").splitlines():
        cells = row.split("\t")
        if row.startswith("#") or len(cells) < 3 or not any(s in cells[0] for s in subs):
            continue
        print(f"{cells[0].rsplit('/', 1)[-1]}\t{m.split('/')[0]}\t{cells[1][:12]} -> {cells[2][:12]}\t{m.rsplit('/', 1)[0]}/{cells[3] if len(cells) > 3 else ''}")
