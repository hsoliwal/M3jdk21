# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Verify only source-retained try annotations: strict build, classfile and API parity."""
from pathlib import Path
import argparse, hashlib, json, subprocess

p = argparse.ArgumentParser()
p.add_argument("--jdk", required=True, type=Path)
p.add_argument("--candidate", required=True, type=Path)
args = p.parse_args()
crate = Path(__file__).resolve().parent.parent
resources = crate / "src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-progress-scope-lint-20261018"
work = crate / "target/scope-lint-proof"
work.mkdir(parents=True, exist_ok=True)
def digest(data):
    return hashlib.sha256(data).hexdigest()
manifest = (resources / "manifest.tsv").read_text()
context_manifest = crate / "contract/context/manifest.tsv"
context_pin = [line.split("\t")[1] for line in manifest.splitlines() if line.startswith("# doclint-context-sha256\t")]
assert len(context_pin) == 1 and digest(context_manifest.read_bytes()) == context_pin[0]
retention_pin = [line.split("\t")[1] for line in manifest.splitlines() if line.startswith("# suppression-source-sha256\t")]
retention = (crate / "contract/SuppressWarnings.java.txt").read_bytes()
assert len(retention_pin) == 1 and digest(retention) == retention_pin[0]
assert "@Retention(RetentionPolicy.SOURCE)" in retention.decode()
sources = {}
for line in context_manifest.read_text().splitlines():
    if not line or line.startswith("#"):
        continue
    path, expected, resource = line.split("\t")
    data = (context_manifest.parent / resource).read_bytes()
    assert digest(data) == expected, path
    sources[path] = data
assert len(sources) == 12
candidate_sources = dict(sources)
rows = [line.split("\t") for line in manifest.splitlines() if line and not line.startswith("#")]
assert len(rows) == 4
count = 0
for path, prehash, afterhash, name in rows:
    pre = (resources / ("before-" + name)).read_bytes()
    after = (args.candidate / path).read_bytes()
    assert digest(pre) == prehash and digest(after) == afterhash and sources[path] == pre, path
    marker = b'    @SuppressWarnings("try")\n'
    count += after.count(marker)
    assert pre == after.replace(marker, b""), path
    candidate_sources[path] = after
assert count == 5
class_sets = []
records = []
for stage, stage_sources, strict in [("baseline", sources, False), ("candidate", candidate_sources, True)]:
    src = work / stage / "src"
    classes = work / stage / "classes"
    classes.mkdir(parents=True, exist_ok=True)
    for path, data in stage_sources.items():
        target = src / Path(path).relative_to("src/java.base/share/classes")
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    flags = ["-g:none", "-XDrawDiagnostics", "-Xmaxwarns", "1000", "-Xlint:all", "-Xdoclint:all/protected"]
    if strict:
        flags.append("-Werror")
    command = [str(args.jdk / "bin/javac")] + flags + ["--patch-module", "java.base=" + str(src),
                    "-d", str(classes)] + [str(src / Path(path).relative_to("src/java.base/share/classes")) for path in stage_sources]
    result = subprocess.run(command, capture_output=True, text=True, timeout=60)
    output = result.stdout + result.stderr
    (work / (stage + ".log")).write_text(output)
    (work / (stage + ".command.json")).write_text(json.dumps(command, indent=2) + "\n")
    assert result.returncode == 0, output
    assert "compiler.warn.proc.messager" not in output and "compiler.err." not in output, output
    tries = output.count("compiler.warn.try.resource.not.referenced")
    assert tries == (0 if strict else 5), output
    assert output.count("compiler.warn.") == tries, output
    files = {str(f.relative_to(classes)): f for f in sorted(classes.rglob("*.class"))}
    assert len(files) == 15, files.keys()
    class_sets.append(files)
    records.append({"stage": stage, "strict": strict, "warning_count": tries, "class_count": len(files)})
before, after = class_sets
assert before.keys() == after.keys()
hashes = []
for name in before:
    assert before[name].read_bytes() == after[name].read_bytes(), name
    api = []
    for label, file in [("baseline", before[name]), ("candidate", after[name])]:
        command = [str(args.jdk / "bin/javap"), "-protected", str(file)]
        result = subprocess.run(command, capture_output=True, text=True, timeout=20)
        assert result.returncode == 0, result.stderr
        api.append(result.stdout)
        target = work / "api" / label / (name + ".txt")
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(result.stdout)
    assert api[0] == api[1], name
    hashes.append({"path": name, "class_sha256": digest(before[name].read_bytes()),
                   "public_protected_api_sha256": digest(api[0].encode())})
report = {"status": "PASS", "owners": 4, "annotations": 5, "context_sources": 12,
          "source_retention_verified": True, "byte_identical_classes": 15,
          "public_protected_apis_equal": 15, "candidate_warnings": 0,
          "baseline_werror_omission": "Witness-only to obtain original classfiles; exactly five try warnings required. Final candidate retains -Werror.",
          "javac_sha256": digest((args.jdk / "bin/javac").read_bytes()), "stages": records, "classes": hashes}
(work / "RESULT.json").write_text(json.dumps(report, indent=2) + "\n")
print("PASS five source-retained annotations; strict zero warnings; 15 byte-identical classfiles and APIs")
