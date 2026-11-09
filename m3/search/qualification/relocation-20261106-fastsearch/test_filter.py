"""test_filter.py <test-root> <leaf simple names...> : donor tests that exercise the leaf and import nothing outside
java.*, org.junit.*, the leaf classes and the mapped progress monitor. Prints IN/OUT rows with the blocking import."""
import re
import sys
from pathlib import Path

root, leaf = Path(sys.argv[1]), set(sys.argv[2:])
allowed = re.compile(r"^(java\.|javax\.|org\.junit\.|com\.synexia\.common\.progress\.ProgressMonitor$)")
for f in sorted(root.rglob("*.java")):
    s = f.read_text(encoding="utf-8")
    body = re.sub(r"//[^\n]*|/\*.*?\*/|\"(?:\\.|[^\"\\])*\"", "", s, flags=re.S)
    if not any(re.search(rf"\b{n}\b", body) for n in leaf):
        continue
    imps = re.findall(r"^import (?:static )?([\w.]+?)(?:\.\*)?;", s, re.M)
    bad = [i for i in imps if not allowed.match(i) and i.rsplit(".", 1)[-1] not in leaf
           and not (i.startswith("jdk.internal.m3.fastsearch.") and i.rsplit(".", 1)[-1] in leaf)]
    print(f"{'OUT' if bad else 'IN'}\t{f.relative_to(root).as_posix()}\t{','.join(bad[:3])}")
