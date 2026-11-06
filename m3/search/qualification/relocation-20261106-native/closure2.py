"""closure2.py : LEAF_CLOSURE for leaf 2 (native search engine). Main = the five classes that reach JniSearchEngine;
tests = donor tests not in leaf 1 whose module references stay inside leaf1+leaf2 and whose only non-JDK imports are
the progress monitor and the native-interop loader pair. Writes leaf2_main.txt / leaf2_test.txt."""
import re
from pathlib import Path

D = Path(__file__).parent
main = {p.stem: p.read_text(encoding="utf-8") for p in (D / "fs").glob("*.java")}
tests = {p.stem: p.read_text(encoding="utf-8") for p in (D / "fst").glob("*.java")}
leaf1 = set((D / "leaf_main.txt").read_text().replace("fs/", "").replace(".java", "").split())
leaf2 = {"JniSearchEngine", "ByteOccurrences", "M3BatchOffsetSearch", "M3BatchReverseSearch", "SearchEngines"}
allm = leaf1 | leaf2
OK = re.compile(r"^(java\.|javax\.xml|org\.w3c|org\.xml|org\.junit\.jupiter\.|com\.synexia\.job\.IProgressMonitor$"
                r"|com\.synexia\.nativeinterop\.(JniLibraryLoader|NativeLibrarySpec)$)")
names = set(main)


def deps(src):
    imps = [m.group(2) for m in re.finditer(r"^import (static )?([\w.]+?)(\.\*)?;", src, re.M)]
    bad = [i for i in imps if not OK.match(i) and not i.startswith("jdk.internal.m3.search")]
    body = re.sub(r"//[^\n]*|/\*.*?\*/|\"(?:\\.|[^\"\\])*\"", "", src, flags=re.S)
    return bad, {n for n in names if re.search(rf"\b{n}\b", body)}


for n in sorted(leaf2):
    b, r = deps(main[n])
    print("MAIN", n, "bad=", b, "outside=", sorted(r - allm))
done = set((D / "leaf_test.txt").read_text().replace("fst/", "").replace(".java", "").split())
t2 = []
for n, s in sorted(tests.items()):
    if n in done:
        continue
    b, r = deps(s)
    if not b and r <= allm:
        t2.append(n)
    else:
        print("TEST_OUT", n, b[:2], sorted(r - allm)[:3])
(D / "leaf2_main.txt").write_text("\n".join(f"fs/{n}.java" for n in sorted(leaf2)) + "\n")
(D / "leaf2_test.txt").write_text("\n".join(f"fst/{n}.java" for n in t2) + "\n")
print("TESTS2", len(t2), " ".join(t2))
