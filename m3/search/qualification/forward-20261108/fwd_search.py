"""fwd_search.py <work-dir> : M3_SEARCH_FORWARD v1. Forward-ports com.synexia m3-fast-search changes made after the
M3jdk21 m3/search receipt (2026-11-06) onto master, file by file.

For each received file whose donor blob (provenance line) differs from develop's blob at the same donor path:
  base   = the donor blob named by master's provenance line, relocated by the recorded recipe;
  theirs = develop's blob, relocated by the same recipe;
  ours   = master's file.
The recipe is the one that produced master: rules_v1b.tsv (+ the rules_v2.tsv native/loader rows), the M3<Type>
rename of every relocated main type (from the relocation ledgers), JNI symbol renames for C, the JDK-naming
rewrite (a3/jn/jdk_naming.rewrite_text), and a provenance line under the SPDX header (base: master's own line;
theirs: the same line naming the develop blob). Writes <work>/<file>.{o,b,t,merged} and prints one row per file:
TAKE_THEIRS (ours == base), MERGED (clean diff3), CONFLICT, or BASE_MISMATCH (relocated base differs from master
outside the provenance line, i.e. master evolved by another route; diff3 still runs)."""
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, "S:/m3-scratch/converge-20261106/a3/jn")
import jdk_naming  # noqa: E402

WORK = Path(sys.argv[1])
GIT = {"GIT_DIR": "S:/m3-scratch/converge-20261106/syn.git", "GIT_NO_LAZY_FETCH": "1"}
Q = Path("m/m3/search/qualification")
RULES = [r.split("\t") for r in Path("rules_forward.tsv").read_text().splitlines() if r]  # forward-only rows first
RULES += [r.split("\t") for r in (Q / "relocation-20261106-bmh-cancellation/rules_v1b.tsv").read_text().splitlines() if r]
RULES += [r.split("\t") for r in (Q / "relocation-20261106-native/rules_v2.tsv").read_text().splitlines()
          if r and r.split("\t") not in RULES]
TYPES = {}
for line in Path("ledgers.tsv").read_text().splitlines():
    kind, src, _blob, out = line.split("\t")[:4]
    if kind == "main" and out.endswith(".java"):
        TYPES[Path(src).stem] = Path(out).stem
TYPES = {a: b for a, b in TYPES.items() if a != b}


def blob(sha):
    import os
    return subprocess.run(["git", "cat-file", "blob", sha], capture_output=True, check=True,
                          env=dict(os.environ, **GIT)).stdout.decode("utf-8")


def relocate(text, native, provenance):
    if native:  # JNI symbols first: the literal rule synexia_m3 -> m3search would otherwise rewrite them
        for a, b in TYPES.items():
            for prefix in ("Java_jdk_internal_m3_search_", "Java_com_synexia_m3_search_"):
                text = text.replace(f"{prefix}{a}_", f"Java_jdk_m3_search_{b}_")
    for kind, a, b in RULES:
        text = text.replace(a, b) if kind == "literal" else re.sub(rf"(?<![\w$]){re.escape(a)}(?![\w$])", b, text)
    for a, b in TYPES.items():
        if native:
            for prefix in ("Java_jdk_internal_m3_search_", "Java_com_synexia_m3_search_"):
                text = text.replace(f"{prefix}{a}_", f"Java_jdk_m3_search_{b}_")
        else:
            text = re.sub(rf"(?<![\w$]){re.escape(a)}(?![\w$])", b, text)
    if not native and "new M3DefaultProgressMonitor()" in text \
            and "import jdk.m3.collections.M3DefaultProgressMonitor;" not in text:
        text = text.replace("import jdk.m3.collections.M3ProgressMonitor;",
                            "import jdk.m3.collections.M3DefaultProgressMonitor;\nimport jdk.m3.collections.M3ProgressMonitor;", 1)
    text, _ = jdk_naming.rewrite_text(text)
    lines = text.split("\n")
    spdx = (lambda s: s.lstrip("/* ").startswith("SPDX-")) if native else (lambda s: s.startswith("// SPDX-"))
    at = next((i + 1 for i in range(len(lines) - 1, -1, -1) if spdx(lines[i])), 0)
    lines.insert(at, provenance)
    return "\n".join(lines)


def strip_prov(text):
    return "\n".join(line for line in text.split("\n") if "Relocated from com.synexia" not in line)


for row in Path("forward.tsv").read_text().splitlines():
    target, base_sha, dev_sha = row.split("\t")
    native = target.endswith(".c")
    if base_sha == "NEW":  # a donor file added after the receipt: relocate only, provenance names the develop blob
        name = Path(target).name
        kind = "test" if "/src/test/" in target else "main"
        prov = (f"// Relocated from com.synexia m3-fast-search/src/{kind}/java/jdk/internal/m3/search/"
                f"{name} (blob {dev_sha[:12]}) by M3_SEARCH_FORWARD v1.")
        (WORK / f"{name}.merged").write_text(relocate(blob(dev_sha), native, prov), encoding="utf-8", newline="")
        print(f"NEW\t{target}\t")
        continue
    ours = Path("m", target).read_text(encoding="utf-8")
    prov = next(line for line in ours.split("\n") if "Relocated from com.synexia" in line)
    theirs_prov = re.sub(r"\(blob [0-9a-f]{12}\) by M3_SEARCH_RELOCATE v\d+",
                         f"(blob {dev_sha[:12]}) by M3_SEARCH_FORWARD v1", prov)
    b = relocate(blob(base_sha), native, prov)
    t = relocate(blob(dev_sha), native, theirs_prov)
    name = Path(target).name
    for suffix, data in (("o", ours), ("b", b), ("t", t)):
        (WORK / f"{name}.{suffix}").write_text(data, encoding="utf-8", newline="")
    r = subprocess.run(["git", "merge-file", "-p", "--diff3", str(WORK / f"{name}.o"), str(WORK / f"{name}.b"),
                        str(WORK / f"{name}.t")], capture_output=True)
    (WORK / f"{name}.merged").write_bytes(r.stdout)
    state = "TAKE_THEIRS" if ours == b else ("MERGED" if r.returncode == 0 else f"CONFLICT={r.returncode}")
    note = "" if ours == b or strip_prov(ours) != strip_prov(b) else "base==ours modulo provenance"
    if ours != b and strip_prov(ours) != strip_prov(b):
        note = "BASE_MISMATCH (master evolved)"
    print(f"{state}\t{target}\t{note}")
