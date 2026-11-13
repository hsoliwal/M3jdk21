"""reseal_ccm.py <source-root> <old CURRENT_INPUTS.json> <old qualify.py> <out-dir> <source-commit> : CORE_CONTRACT_RESEAL v1.
Re-seals make/m3/tooling/core-contract-matrix to the current m3/core cohort:
  - files: make/m3/build.sh, every *.java under m3/core/src and m3/core/test (qualify.py requires exactly that Java set),
    and every non-Java input the old manifest pinned (the m3/lexicon data the tests read),
    each with bytes, sha256 and git blob id (qualify.identity);
  - programs: the programs build.sh selects, in its order (qualify.py's own regex);
  - markers: the old markers kept; a new program's marker is read from its test source ("<MARKER> checks=");
  - qualify.py: the three cohort-size constants (file count, program count, executions = programs x modes) follow.
Every other manifest field and every other qualify.py byte is kept. Refuses when a new program has no marker."""
import hashlib
import json
import re
import sys
from pathlib import Path

src, old_json, old_py, out, commit = Path(sys.argv[1]), Path(sys.argv[2]), Path(sys.argv[3]), Path(sys.argv[4]), sys.argv[5]
out.mkdir(parents=True, exist_ok=True)


def identity(raw):
    return dict(bytes=len(raw), sha256=hashlib.sha256(raw).hexdigest(),
                gitBlob=hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\0" + raw).hexdigest())


manifest = json.loads(old_json.read_text(encoding="utf-8"))
current_java = {p.relative_to(src).as_posix() for d in ("m3/core/src", "m3/core/test") for p in (src / d).rglob("*.java")}
# Old order is kept for paths that remain (minimal diff); vanished Java paths drop; new Java paths append sorted.
old_order = [f["path"] for f in manifest["files"]
             if not f["path"].endswith(".java") or f["path"] in current_java]
paths = old_order + sorted(current_java - set(old_order))
manifest["files"] = [{"path": p, **{k: identity((src / p).read_bytes())[k] for k in ("gitBlob", "bytes", "sha256")}} for p in paths]
build = (src / "make/m3/build.sh").read_text(encoding="utf-8")
selected = re.findall(r'-cp build/tests ([A-Za-z0-9_]+) \| tee "build/logs/([^"]+)\-\$mode\.log"', build)
programs = [name for name, _ in selected]
markers = {}
for name in programs:
    if name in manifest["markers"]:
        markers[name] = manifest["markers"][name]
        continue
    text = (src / "m3/core/test" / (name + ".java")).read_text(encoding="utf-8")
    found = re.findall(r'"([A-Z0-9_]+_PASS) checks=', text)
    if len(set(found)) != 1:
        sys.exit(f"REFUSE marker for {name}: {found}")
    markers[name] = found[0]
manifest["programs"], manifest["markers"], manifest["sourceCommit"] = programs, markers, commit
manifest["scope"] = re.sub(r"selects \w+ original programs", f"selects {len(programs)} original programs", manifest["scope"])
(out / "CURRENT_INPUTS.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8", newline="\n")
py = old_py.read_text(encoding="utf-8")
executions = len(programs) * len(manifest["modes"])
for old, new in ((r'len\(manifest\["files"\]\) == \d+', f'len(manifest["files"]) == {len(paths)}'),
                 (r'len\(selected\) == \d+', f'len(selected) == {len(programs)}'),
                 (r'"all\d+ original test logs required"', f'"all{executions} original test logs required"'),
                 (r'originalExecutions=\d+', f'originalExecutions={executions}')):
    py, n = re.subn(old, new, py)
    if n != 1:
        sys.exit(f"REFUSE qualify.py site {old!r}: {n} hits")
(out / "qualify.py").write_text(py, encoding="utf-8", newline="\n")
print(f"files={len(paths)} programs={len(programs)} executions={executions} new_markers="
      + ",".join(f"{k}={v}" for k, v in markers.items() if k not in json.loads(old_json.read_text())["markers"]))
