"""jni_bind.py <text-tools-root> : JNI_BIND gate. Every `native` method declared in src/** must have an exported
Java_<package>_<Class>_<method> symbol in native/*.c (JNI short names; '_' in names escaped as _1). Prints
BOUND/UNBOUND rows and exported symbols that no Java method uses (STALE)."""
import re
import sys
from pathlib import Path

root = Path(sys.argv[1])
exports = set()
for c in (root / "native").glob("*.c"):
    exports |= set(re.findall(r"\b(Java_\w+)\s*\(?", c.read_text(encoding="utf-8")))
wanted = set()
bad = 0
for j in (root / "src").rglob("*.java"):
    s = j.read_text(encoding="utf-8")
    pkg = re.search(r"^package ([\w.]+);", s, re.M)
    if not pkg:
        continue
    cls = j.stem
    code = re.sub(r"//[^\n]*|/\*.*?\*/|\"(?:\\.|[^\"\\])*\"", "", s, flags=re.S)
    decl = r"(?:(?:private|public|protected|static|final|synchronized)\s+)*native\s+[\w<>\[\]., ?]+?\s+(\w+)\s*\([^)]*\)\s*;"
    for m in re.finditer(decl, code):
        name = "Java_" + pkg.group(1).replace("_", "_1").replace(".", "_") + "_" + cls + "_" + m.group(1).replace("_", "_1")
        wanted.add(name)
        ok = name in exports
        bad += not ok
        print(f"{'BOUND' if ok else 'UNBOUND'}\t{name}\t{j.name}")
for e in sorted(exports - wanted):
    print(f"STALE\t{e}")
print(f"JNI_BIND native_methods={len(wanted)} unbound={bad} stale_exports={len(exports - wanted)}")
