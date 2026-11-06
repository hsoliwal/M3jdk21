"""closure.py : LEAF_CLOSURE for the m3-fast-search -> M3jdk21 m3/search leaf.
Seeds = the pure-Java kernels; a main class joins only if every module class it names is in the set and its only
non-JDK import is jdk.internal.m3.job.IProgressMonitor (mapped to jdk.m3.collections.M3ProgressMonitor). A test joins
when the same holds for it (JUnit 5 allowed). Prints the main set, the test set and why each excluded file is out."""
import re
from pathlib import Path

D = Path(__file__).parent
main = {p.stem: p.read_text(encoding="utf-8") for p in (D / "fs").glob("*.java")}
tests = {p.stem: p.read_text(encoding="utf-8") for p in (D / "fst").glob("*.java")}
names = set(main)
ALLOWED = re.compile(r"^(java\.|javax\.xml|org\.w3c|org\.xml|org\.junit\.jupiter\.|com\.synexia\.job\.IProgressMonitor$)")


def deps(src):
    imports = [m.group(2) for m in re.finditer(r"^import (static )?([\w.]+?)(\.\*)?;", src, re.M)]
    bad = [i for i in imports if not ALLOWED.match(i) and not i.startswith("jdk.internal.m3.search")]
    body = re.sub(r"//[^\n]*|/\*.*?\*/|\"(?:\\.|[^\"\\])*\"", "", src, flags=re.S)
    refs = {n for n in names if re.search(rf"\b{n}\b", body)}
    return bad, refs


seed = {"SearchEngine", "JavaAdaptiveSearchEngine", "CompiledSearch", "KmpSearch", "BoyerMooreHorspoolSearch",
        "ByteOccurrences", "BitapExactSearch", "MinimumCoverWindow", "FixedWordConcatenation", "M3BatchOffsetSearch",
        "M3BatchReverseSearch", "BatchSubsequenceMatch", "MinimumSubsequenceWindow", "package-info"}
keep = set(seed)
changed = True
while changed:
    changed = False
    for n in sorted(keep):
        bad, refs = deps(main[n])
        if bad or not refs <= keep:
            keep.discard(n); changed = True
            print(f"MAIN_OUT\t{n}\tbad={bad}\tneeds={sorted(refs - keep)}")
print("MAIN\t" + " ".join(sorted(keep)))
tin = []
for n, src in sorted(tests.items()):
    bad, refs = deps(src)
    if bad or not refs <= keep or re.search(r"\b(Jni|Native)\w*", re.sub(r"//[^\n]*", "", src)) and "Native" in n:
        print(f"TEST_OUT\t{n}\tbad={bad[:3]}\tneeds={sorted(refs - keep)[:4]}")
    else:
        tin.append(n)
print("TESTS\t" + " ".join(tin))
