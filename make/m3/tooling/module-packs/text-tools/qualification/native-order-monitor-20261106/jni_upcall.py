"""jni_upcall.py <text-tools-root> <java-src-dir>... : JNI_UPCALL gate v1. Every method a native looks up with
Get[Static]MethodID(env, cls, "name", "sig") in native/*.c must be declared, with the same name, parameter count
and void-ness as the descriptor, by some Java source under <text-tools-root>/src or the extra source dirs (the
java.base owners the pack passes into natives, e.g. java.util.concurrent.ProgressMonitor). Catches upcall names
left behind when a relocation changes the Java owner type but not the C string (donor checkCanceled vs java.base
checkCancelled). Prints UPCALL_BOUND / UPCALL_UNBOUND rows; exit 1 on any unbound lookup."""
import re
import sys
from pathlib import Path

DECL = re.compile(r"([\w<>\[\]?.]+)\s+([a-zA-Z_]\w*)\s*\(([^;{)]*)\)\s*(?:throws [\w., ]+)?\s*[{;]")
LOOKUP = re.compile(r"Get(?:Static)?MethodID\s*\([^,]+,[^,]+,\s*\"([\w<>]+)\"\s*,\s*\"([^\"]+)\"")
ARG = re.compile(r"\[*(?:L[^;]+;|[ZBCSIJFD])")


def arity(params):
    if not params.strip():
        return 0
    depth, n = 0, 1
    for ch in params:
        depth += (ch == "<") - (ch == ">")
        n += ch == "," and depth == 0
    return n


root, extra = Path(sys.argv[1]), [Path(p) for p in sys.argv[2:]]
declared = set()
for d in [root / "src", *extra]:
    for j in d.rglob("*.java"):
        code = re.sub(r"//[^\n]*|/\*.*?\*/", "", j.read_text(encoding="utf-8"), flags=re.S)
        for rt, name, params in DECL.findall(code):
            declared.add((name, arity(params), rt == "void"))
bad = 0
for c in sorted((root / "native").glob("*.c")):
    text = c.read_text(encoding="utf-8")
    for m in LOOKUP.finditer(text):
        name, sig = m.group(1), m.group(2)
        if name == "<init>":
            continue
        n = len(ARG.findall(sig[1:sig.index(")")]))
        ok = (name, n, sig.endswith(")V")) in declared
        bad += not ok
        line = text.count("\n", 0, m.start()) + 1
        print(f"{'UPCALL_BOUND' if ok else 'UPCALL_UNBOUND'}\t{c.name}:{line}\t{name}{sig}")
print(f"JNI_UPCALL unbound={bad}")
sys.exit(1 if bad else 0)
