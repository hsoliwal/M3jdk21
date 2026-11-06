"""find_pre.py <git-dir> <ref> <manifest.tsv> <dataset-dir> <out-dir> [max-commits] : HASH_PINNED_3WAY v1.
For every manifest row (path, before_sha256 | ABSENT, after_sha256, template) of a hash-pinned crate whose paths
already use M3jdk21's names: verifies the template bytes hash to after_sha256, then
  master == after          -> APPLIED (nothing to do)
  master == before         -> APPLY (write the template)
  before == ABSENT         -> NEW if master lacks the path, else DRIFT_NEW (owner review)
  otherwise                -> walks `git log <ref> -- <path>` (newest first, up to max-commits) for a version whose
                              sha256 equals before_sha256, uses it as the 3-way base (ours = master, theirs =
                              template): MERGED_CLEAN writes the merge, CONFLICT=<n> writes it with markers,
                              NO_BASE when the preimage never existed on <ref>'s history.
Outputs go to <out-dir>/<path>; one verdict row per path is printed."""
import hashlib
import os
import subprocess
import sys
import tempfile
from pathlib import Path

gd, ref, manifest, dataset, out = sys.argv[1:6]
limit = int(sys.argv[6]) if len(sys.argv) > 6 else 80
env = dict(os.environ, GIT_DIR=gd)


def git(*args, binary=False):
    r = subprocess.run(["git", *args], capture_output=True, env=env)
    return (r.stdout if binary else r.stdout.decode()) if r.returncode == 0 else None


def sha(b):
    return hashlib.sha256(b).hexdigest()


for row in Path(manifest).read_text(encoding="utf-8").splitlines():
    if not row.strip() or row.startswith("#"):
        continue
    path, before, after, template = row.split("\t")[:4]
    post = (Path(dataset) / template).read_bytes()
    if sha(post) != after:
        print(f"BAD_PIN\t{path}")
        continue
    live = git("cat-file", "blob", f"{ref}:{path}", binary=True)
    dst = Path(out) / path
    if live is not None and sha(live) == after:
        print(f"APPLIED\t{path}")
        continue
    if before == "ABSENT":
        verdict = "NEW" if live is None else "DRIFT_NEW"
        if verdict == "NEW":
            dst.parent.mkdir(parents=True, exist_ok=True)
            dst.write_bytes(post)
        print(f"{verdict}\t{path}")
        continue
    if live is not None and sha(live) == before:
        dst.parent.mkdir(parents=True, exist_ok=True)
        dst.write_bytes(post)
        print(f"APPLY\t{path}")
        continue
    base = None
    for commit in (git("log", f"-{limit}", "--format=%H", ref, "--", path) or "").split():
        blob = git("cat-file", "blob", f"{commit}:{path}", binary=True)
        if blob is not None and sha(blob) == before:
            base = (commit, blob)
            break
    if base is None:
        print(f"NO_BASE\t{path}")
        continue
    with tempfile.TemporaryDirectory(dir=os.environ.get("TMP")) as d:
        o, b, t = (Path(d) / n for n in "obt")
        o.write_bytes(live or b""); b.write_bytes(base[1]); t.write_bytes(post)
        r = subprocess.run(["git", "merge-file", "-p", "--diff3", str(o), str(b), str(t)], capture_output=True)
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_bytes(r.stdout)
    print(f"{'MERGED_CLEAN' if r.returncode == 0 else 'CONFLICT=' + str(r.returncode)}\t{path}\tbase={base[0][:12]}")
