"""consolidate_search.py <src-module> <out-root> : CONSOLIDATE_PACKAGE v1, jdk.m3.tooling.search -> jdk.m3.search.

Folds the module make/m3/tooling/search (package jdk.m3.tooling.search, 14 files) into the existing module m3/search
(package jdk.m3.search), so one search package exists. Path map:
  src/{main,test}/java/jdk/m3/tooling/search/X.java -> m3/search/src/{main,test}/java/jdk/m3/search/X.java
  src/main/native/X.c                              -> m3/search/src/main/native/X.c (not in CMakeLists.txt: built
                                                      by the qualification scripts, which generate the JNI header)
  README*.md, verify*.sh                           -> m3/search/qualification/sorted-bounds/
Text rules, in order, on every file: jdk.m3.tooling.search -> jdk.m3.search; jdk_m3_tooling_search_ -> jdk_m3_search_
(JNI symbols and javac -h header names); jdk/m3/tooling/search -> jdk/m3/search; make/m3/tooling/search -> m3/search.
Scripts only: the module root is now two directories above the script, and the long-bounds script compiles the
sorted-bounds sources instead of every source of the (now larger) module. Prints old path, new path, rules applied."""
import sys
from pathlib import Path

SRC, OUT = Path(sys.argv[1]), Path(sys.argv[2])
RULES = [("jdk.m3.tooling.search", "jdk.m3.search"), ("jdk_m3_tooling_search_", "jdk_m3_search_"),
         ("jdk/m3/tooling/search", "jdk/m3/search"), ("make/m3/tooling/search", "m3/search")]
SCRIPT_RULES = [
    ('here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)', 'here=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)'),
    ("mapfile -t java_files < <(find \"$here/src/main/java\" \"$here/src/test/java\" -name '*.java' -type f",
     "mapfile -t java_files < <(find \"$here/src/main/java\" \"$here/src/test/java\" -name 'M3*Sorted*Bounds*.java' -type f"),
]


def target(rel):
    if rel.startswith(("src/main/java/", "src/test/java/")):
        return "m3/search/" + rel.replace("jdk/m3/tooling/search", "jdk/m3/search")
    if rel.startswith("src/main/native/"):
        return "m3/search/" + rel
    return "m3/search/qualification/sorted-bounds/" + rel


for f in sorted(p for p in SRC.rglob("*") if p.is_file()):
    rel = f.relative_to(SRC).as_posix()
    text = f.read_bytes().decode("utf-8")
    applied = []
    for a, b in RULES + (SCRIPT_RULES if rel.endswith(".sh") else []):
        if a in text:
            text = text.replace(a, b)
            applied.append(a.split("(")[0][:24])
    if rel.endswith(".sh") and 'here=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)' not in text:
        sys.exit(f"REFUSE script root not rewritten: {rel}")
    dst = OUT / target(rel)
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_bytes(text.encode("utf-8"))
    print(f"make/m3/tooling/search/{rel}\t{target(rel)}\t{';'.join(applied)}")
