"""census.py <tree.txt> <out-dir> : PACKAGE_CENSUS v1 over one M3jdk21 tree listing (lines: "<blob> <path>").

Source root = the directory before /src/main/java/ (Java main sources only). A root is REAL when it carries its own
pom.xml or module-info.java and no path segment marks a copy; it is a COPY when a segment names recipe custody,
fixtures, intakes, evidence, tasks, before/after/out images, receiving or retained-variant material. Writes:
  ROOTS.tsv     root, kind, java files, packages, M3-prefixed types
  SPLIT.tsv     package, number of REAL roots declaring it, the roots     (packages declared by more than one REAL root)
  PACKAGES.tsv  package, family (jdk.m3.<family> or the first two segments), REAL roots, java files
  FAMILIES.tsv  family, packages, REAL roots, java files
  SUMMARY.tsv   counts"""
import collections
import re
import sys
from pathlib import Path

COPY = re.compile(r"/(recipe-custody|custody|fixtures?|test-fixtures|intakes?|evidence|tasks|before|after|out|inverse|"
                  r"receiving|retained-variants|candidate|qualified|resources|proof|baseline|postimages?|preimages?|"
                  r"donor|donors-src|snapshots?|golden|expected|samples?|qualification|recipes?)/")
VENDOR = re.compile(r"^m3/vendor/")
paths = [line.split(" ", 1)[1] for line in Path(sys.argv[1]).read_text(encoding="utf-8").splitlines() if " " in line]
out = Path(sys.argv[2])
out.mkdir(parents=True, exist_ok=True)
all_paths = set(paths)
roots = collections.defaultdict(lambda: {"java": 0, "pkgs": set(), "m3types": 0})
for p in paths:
    m = re.match(r"^(.*)/src/main/java/(.+)/([^/]+)\.java$", p)
    if not m or m.group(3) in ("module-info", "package-info"):
        continue
    root, pkg, name = m.group(1), m.group(2).replace("/", "."), m.group(3)
    r = roots[root]
    r["java"] += 1
    r["pkgs"].add(pkg)
    if name.startswith("M3") and pkg.startswith(("jdk.m3.", "jdk.internal.m3.")):
        r["m3types"] += 1
kind = {}
for root in roots:
    marker = f"{root}/pom.xml" in all_paths or f"{root}/src/main/java/module-info.java" in all_paths
    kind[root] = ("VENDOR" if VENDOR.match(root) else "COPY" if COPY.search("/" + root + "/")
                  else ("REAL" if marker else "LOOSE"))
by_pkg = collections.defaultdict(set)
files_by_pkg = collections.Counter()
for root, r in roots.items():
    if kind[root] != "REAL":
        continue
    for pkg in r["pkgs"]:
        by_pkg[pkg].add(root)
for p in paths:
    m = re.match(r"^(.*)/src/main/java/(.+)/([^/]+)\.java$", p)
    if m and kind.get(m.group(1)) == "REAL":
        files_by_pkg[m.group(2).replace("/", ".")] += 1


def family(pkg):
    parts = pkg.split(".")
    if parts[:2] == ["jdk", "m3"] and len(parts) > 2:
        return "jdk.m3." + parts[2]
    if parts[:3] == ["jdk", "internal", "m3"] and len(parts) > 3:
        return "jdk.internal.m3." + parts[3]
    return ".".join(parts[:2])


with open(out / "ROOTS.tsv", "w", encoding="utf-8", newline="\n") as f:
    f.write("root\tkind\tjava_files\tpackages\tm3_prefixed_types\n")
    for root in sorted(roots):
        r = roots[root]
        f.write(f"{root}\t{kind[root]}\t{r['java']}\t{len(r['pkgs'])}\t{r['m3types']}\n")
with open(out / "SPLIT.tsv", "w", encoding="utf-8", newline="\n") as f:
    f.write("package\treal_roots\troots\n")
    for pkg, rs in sorted(by_pkg.items(), key=lambda kv: (-len(kv[1]), kv[0])):
        if len(rs) > 1:
            f.write(f"{pkg}\t{len(rs)}\t{' '.join(sorted(rs))}\n")
fam = collections.defaultdict(lambda: [set(), set(), 0])
with open(out / "PACKAGES.tsv", "w", encoding="utf-8", newline="\n") as f:
    f.write("package\tfamily\treal_roots\tjava_files\n")
    for pkg in sorted(by_pkg):
        fm = family(pkg)
        fam[fm][0].add(pkg)
        fam[fm][1].update(by_pkg[pkg])
        fam[fm][2] += files_by_pkg[pkg]
        f.write(f"{pkg}\t{fm}\t{len(by_pkg[pkg])}\t{files_by_pkg[pkg]}\n")
with open(out / "FAMILIES.tsv", "w", encoding="utf-8", newline="\n") as f:
    f.write("family\tpackages\treal_roots\tjava_files\n")
    for fm, (pk, rs, n) in sorted(fam.items(), key=lambda kv: -kv[1][2]):
        f.write(f"{fm}\t{len(pk)}\t{len(rs)}\t{n}\n")
real = [r for r in roots if kind[r] == "REAL"]
counts = collections.Counter(kind.values())
with open(out / "SUMMARY.tsv", "w", encoding="utf-8", newline="\n") as f:
    f.write("metric\tvalue\n")
    for k in ("REAL", "LOOSE", "COPY", "VENDOR"):
        f.write(f"source_roots_{k.lower()}\t{counts[k]}\n")
    f.write(f"packages_in_real_roots\t{len(by_pkg)}\n")
    f.write(f"split_packages\t{sum(1 for rs in by_pkg.values() if len(rs) > 1)}\n")
    f.write(f"families\t{len(fam)}\n")
    f.write(f"java_files_in_real_roots\t{sum(roots[r]['java'] for r in real)}\n")
    f.write(f"m3_prefixed_types_in_m3_packages\t{sum(roots[r]['m3types'] for r in real)}\n")
print(open(out / "SUMMARY.tsv", encoding="utf-8").read())
