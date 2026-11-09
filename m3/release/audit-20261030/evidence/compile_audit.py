#!/usr/bin/env python
# Release-audit compile probe: javac --release 21 -Xlint:all,-module -Werror per module.
import os, re, subprocess, sys, time
from pathlib import Path

ROOT = Path(sys.argv[1]); REF = sys.argv[2]; OUT = Path(sys.argv[3]); CLS = Path(sys.argv[4])
JAVAC = r"C:\Program Files\Java\jdk-21\bin\javac.exe"
# (module id, src root relative to ROOT, [dep module ids], external deps note, werror)
MODULES = [
    ("vendor/asm", "m3/vendor/third_party/asm-9.10.1/src/main/java", [], "", False),
    ("vendor/asm-tree", "m3/vendor/third_party/asm-tree/src/main/java", ["vendor/asm"], "", False),
    ("vendor/asm-analysis", "m3/vendor/third_party/asm-analysis/src/main/java", ["vendor/asm", "vendor/asm-tree"], "", False),
    ("m3/arrays", "m3/arrays/src/main/java", [], "", True),
    ("m3/collections", "m3/collections/src/main/java", ["m3/arrays"], "", True),
    ("m3/mop", "m3/mop", [], "", True),
    ("module-packs/build-support", "m3/tooling/module-packs/build-support/src/main/java", [], "", True),
    ("module-packs/collections", "m3/tooling/module-packs/collections/src/main/java", ["m3/collections", "m3/arrays"], "", True),
    ("module-packs/jackson", "m3/tooling/module-packs/jackson/src/main/java", [], "", True),
    ("module-packs/kernel", "m3/tooling/module-packs/kernel/src/main/java", [], "", True),
    ("module-packs/nlp-search", "m3/tooling/module-packs/nlp-search", [], "", True),
    ("module-packs/parser", "m3/tooling/module-packs/parser/src/main/java", [], "", True),
    ("module-packs/parser-openrewrite", "m3/tooling/module-packs/parser-openrewrite/src/main/java", ["module-packs/parser"], "EXTERNAL:openrewrite", True),
    ("module-packs/recipes", "m3/tooling/module-packs/recipes/src/main/java", ["module-packs/kernel"], "EXTERNAL:openrewrite", True),
    ("ports/precompute", "m3/ports/precompute/src/main/java", [], "", True),
    ("ports/parallel", "m3/ports/parallel/src/main/java", [], "", True),
    ("ports/parallel-bulk", "m3/ports/parallel-bulk/src/main/java", ["ports/parallel"], "", True),
    ("ports/vcpu", "m3/ports/vcpu/src/main/java", ["ports/parallel"], "", True),
    ("ports/capability-runtime", "m3/ports/capability-runtime/src/main/java", [], "", True),
    ("ports/collections", "m3/ports/collections/src/main/java", [], "", True),
    ("ports/compiletime", "m3/ports/compiletime/src/main/java", ["ports/precompute", "vendor/asm", "vendor/asm-tree", "vendor/asm-analysis"], "", True),
    ("ports/file", "m3/ports/file/src/main/java", [], "", True),
    ("ports/indexstring", "m3/ports/indexstring/src/main/java", [], "", True),
    ("ports/regex-cursor", "m3/ports/regex-cursor/src/main/java", [], "", True),
    ("ports/swing-viewport", "m3/ports/swing-viewport/src/main/java", [], "", True),
    ("ports/jfx-viewport", "m3/ports/jfx-viewport/src/main/java", ["ports/swing-viewport"], "EXTERNAL:javafx-controls", True),
    ("ports/tornadovm-bulk", "m3/ports/tornadovm-bulk/src/main/java", [], "EXTERNAL:tornado-api", True),
]
ERR = re.compile(r"^(.*?\.java):(\d+): error: (.*)$")
WARN = re.compile(r"^(.*?\.java):(\d+): warning: (.*)$")

def java_files(root: Path, main_only: bool):
    out = []
    for p in root.rglob("*.java"):
        s = p.as_posix()
        if main_only and ("/src/test/" in s or "/qualification/" in s or "/recipe/" in s or "/datasets/" in s):
            continue
        if p.name == "module-info.java":
            continue
        out.append(p)
    return sorted(out)

def row(*cols):
    with OUT.open("a", encoding="utf-8", newline="") as f:
        f.write("\t".join(str(c).replace("\t", " ").replace("\n", " ") for c in cols) + "\n")

if not OUT.exists():
    row("ref", "module", "src_root", "java_files", "result", "error_count", "warning_count", "first_error", "deps", "notes")
for mid, rel, deps, ext, werror in MODULES:
    src = ROOT / rel
    t0 = time.time()
    if not src.is_dir():
        row(REF, mid, rel, 0, "ABSENT", 0, 0, "", ";".join(deps), "source root absent on this ref"); continue
    files = java_files(src, True)
    mi = list(src.rglob("module-info.java"))
    if not files:
        row(REF, mid, rel, 0, "NO_SOURCES", 0, 0, "", ";".join(deps), "no main java files"); continue
    dest = CLS / REF.replace("/", "_") / mid.replace("/", "_"); dest.mkdir(parents=True, exist_ok=True)
    cp = os.pathsep.join(str(CLS / REF.replace("/", "_") / d.replace("/", "_")) for d in deps)
    arg = dest.with_suffix(".args")
    opts = ["-d", str(dest), "--release", "21", "-encoding", "UTF-8", "-Xmaxerrs", "5000", "-Xmaxwarns", "5000", "-proc:none", "-Xlint:all,-module"]
    if werror: opts.append("-Werror")
    if cp: opts += ["-cp", cp]
    arg.write_text("\n".join(opts + ['"' + p.as_posix() + '"' for p in files]), encoding="utf-8")
    pr = subprocess.run([JAVAC, "@" + str(arg)], capture_output=True, text=True, encoding="utf-8", errors="replace")
    lines = (pr.stdout + pr.stderr).splitlines()
    errs = [m for m in (ERR.match(l) for l in lines) if m]
    warns = [m for m in (WARN.match(l) for l in lines) if m]
    log = dest.with_suffix(".log"); log.write_text("\n".join(lines), encoding="utf-8")
    first = ""
    if errs:
        m = errs[0]; first = f"{Path(m.group(1)).as_posix().replace(ROOT.as_posix()+'/', '')}:{m.group(2)}: error: {m.group(3)}"
    elif pr.returncode != 0:
        first = next((l for l in lines if "error" in l.lower()), lines[-1] if lines else "javac exit %d" % pr.returncode)
    res = "PASS" if pr.returncode == 0 else "FAIL"
    notes = []
    if mi: notes.append(f"module-info excluded ({len(mi)})")
    if ext: notes.append(ext)
    if not werror: notes.append("vendored dependency prep: no -Werror")
    notes.append("%.1fs" % (time.time() - t0))
    row(REF, mid, rel, len(files), res, len(errs), len(warns), first, ";".join(deps), "; ".join(notes))
    print(REF, mid, res, len(errs), "errors", len(warns), "warnings", flush=True)
