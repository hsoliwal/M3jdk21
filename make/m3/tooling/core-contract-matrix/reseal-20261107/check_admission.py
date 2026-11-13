"""check_admission.py <source-root> <qualify.py> <CURRENT_INPUTS.json> : runs qualify.py's own pre-build admission
(identity of every pinned file, exact Java set, build.sh program order, cohort-size asserts) without the build."""
import importlib.util, json, re, sys
from pathlib import Path
src, qpy, mj = Path(sys.argv[1]).resolve(), sys.argv[2], sys.argv[3]
spec = importlib.util.spec_from_file_location("qualify", qpy); q = importlib.util.module_from_spec(spec); spec.loader.exec_module(q)
manifest = json.loads(Path(mj).read_text(encoding="utf-8"))
text = Path(qpy).read_text(encoding="utf-8")
nfiles = int(re.search(r'len\(manifest\["files"\]\) == (\d+)', text).group(1)); nprog = int(re.search(r'len\(selected\) == (\d+)', text).group(1))
rows = manifest["files"]; assert len(rows) == nfiles, (len(rows), nfiles)
before = {r["path"]: q.regular(src / q.relative(r["path"])) for r in rows}
q.admit(rows, before)
java = {p.relative_to(src).as_posix() for d in ("m3/core/src", "m3/core/test") for p in (src / d).rglob("*.java")}
assert java == {p for p in before if p.endswith(".java")}, "unlisted Java source"
build = before["make/m3/build.sh"].decode("utf-8")
selected = re.findall(r'-cp build/tests ([A-Za-z0-9_]+) \| tee "build/logs/([^"]+)\-\$mode\.log"', build)
assert [r[0] for r in selected] == manifest["programs"] and len(selected) == nprog
assert set(manifest["markers"]) == set(manifest["programs"])
print(f"ADMISSION_PASS files={len(rows)} java={len(java)} programs={len(selected)}")
