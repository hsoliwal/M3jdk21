# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Account for all 121 sealed baseline diagnostics in serial per-file source passes."""
from pathlib import Path
import argparse, collections, hashlib, json, re, subprocess

p = argparse.ArgumentParser()
p.add_argument("--jdk", required=True, type=Path)
p.add_argument("--candidate", required=True, type=Path)
args = p.parse_args()
crate = Path(__file__).resolve().parent.parent
resources = crate / "src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-java-base-doclint-20261018"
work = crate / "target/doclint-proof"
work.mkdir(parents=True, exist_ok=True)
expected = {"Runnable.java": 2, "Strings.java": 72, "Callable.java": 3, "Executor.java": 3,
            "ExecutorService.java": 7, "MonitoredCallable.java": 3, "MonitoredRunnable.java": 1,
            "ProgressMonitor.java": 13, "ProgressMonitors.java": 10, "Consumer.java": 2,
            "Function.java": 3, "Supplier.java": 2}
def digest(data):
    return hashlib.sha256(data).hexdigest()
rows = [line.split("\t") for line in (resources / "manifest.tsv").read_text().splitlines()
        if line and not line.startswith("#")]
assert len(rows) == 12
sources = {}
post = {}
for path, prehash, afterhash, name in rows:
    pre = (resources / ("before-" + name)).read_bytes()
    candidate = (args.candidate / path).read_bytes()
    assert digest(pre) == prehash and digest(candidate) == afterhash, path
    relative = Path(path).relative_to("src/java.base/share/classes")
    sources[relative] = pre
    post[relative] = candidate
src = work / "src"
src.mkdir(exist_ok=True)
for name, data in sources.items():
    target = src / name
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(data)
flags = ["-g:none", "-XDrawDiagnostics", "-Xmaxwarns", "1000", "-Xlint:all", "-Werror",
         "-Xdoclint:all/protected", "--patch-module", "java.base=" + str(src)]
records = []
remaining = dict(expected)
for index in range(13):
    changed = None
    if index:
        changed = list(sources)[index - 1]
        (src / changed).write_bytes(post[changed])
        remaining.pop(changed.name)
    classes = work / ("classes-%02d" % index)
    classes.mkdir(exist_ok=True)
    command = [str(args.jdk / "bin/javac")] + flags + ["-d", str(classes)] + [str(src / n) for n in sources]
    result = subprocess.run(command, capture_output=True, text=True, timeout=60)
    output = result.stdout + result.stderr
    (work / ("pass-%02d.log" % index)).write_text(output)
    (work / ("pass-%02d.command.json" % index)).write_text(json.dumps(command, indent=2) + "\n")
    observed = collections.Counter(re.findall(r"^([^:\n]+\.java):\d+:\d+: compiler\.warn\.proc\.messager:", output, re.M))
    assert dict(observed) == remaining, (index, dict(observed), remaining, output)
    assert result.returncode == 1 and "compiler.err.warnings.and.werror" in output, output
    assert output.count("compiler.err.") == 1, output
    tries = output.count("compiler.warn.try.resource.not.referenced")
    if index == 12:
        assert tries == 2, output
        assert all(line.startswith("ProgressMonitors.java:") for line in output.splitlines()
                   if "compiler.warn.try.resource.not.referenced" in line), output
    else:
        assert tries == 0, output
    records.append({"pass": index, "owner": None if changed is None else str(changed),
                    "doc_warnings": sum(observed.values()), "try_warnings": tries,
                    "returncode": result.returncode})
    print(json.dumps(records[-1]), flush=True)
report = {"status": "PASS", "baseline_doc_warnings": 121, "owners": 12, "serial_owner_passes": 12,
          "remaining_doc_warnings": 0, "remaining_flow_try_warnings": 2,
          "strict_flags_retained": flags, "remaining_warning_recipe": "M3ProgressScopeLintRecipe",
          "javac_sha256": digest((args.jdk / "bin/javac").read_bytes()), "passes": records}
(work / "RESULT.json").write_text(json.dumps(report, indent=2) + "\n")
print("PASS serial doclint 12 owners 121 -> 0; two next-stage lexical-scope warnings remain")
