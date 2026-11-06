"""fix_modules.py <text-tools-root> : M3_TEXT_TOOLS_CONVERGE v1 module step.
The donor keeps every tool in one package; M3 splits them into jdk.m3.text <- jdk.m3.io / jdk.m3.structured.
For each jdk.m3.io / jdk.m3.structured source, add explicit imports for the jdk.m3.text (and, for structured,
jdk.m3.io) classes it names; add `requires jdk.m3.io;` to jdk.m3.structured when it uses io classes, and
`requires java.management;` to a module whose sources import java.lang.management. Prints one row per change."""
import re
import sys
from pathlib import Path

root = Path(sys.argv[1]) / "src"


def classes(mod, pkg):
    return {p.stem for p in (root / mod / pkg.replace(".", "/")).glob("*.java")}


text, io = classes("jdk.m3.text", "jdk.m3.text"), classes("jdk.m3.io", "jdk.m3.io")
needs = {"jdk.m3.io": set(), "jdk.m3.structured": set()}
for mod, own, providers in (("jdk.m3.io", "jdk.m3.io", [("jdk.m3.text", text)]),
                            ("jdk.m3.structured", "jdk.m3.structured", [("jdk.m3.text", text), ("jdk.m3.io", io)])):
    mine = classes(mod, own)
    for f in sorted((root / mod / own.replace(".", "/")).glob("*.java")):
        s = f.read_text(encoding="utf-8")
        body = re.sub(r"//[^\n]*|/\*.*?\*/|\"(?:\\.|[^\"\\])*\"", "", s, flags=re.S)
        add = []
        local = set(re.findall(r"\b(?:class|record|interface|enum)\s+(\w+)", body))  # nested types shadow imports
        for pkg, names in providers:
            for n in sorted(names - mine - local):
                if re.search(rf"(?<![\w.]){n}\b", body) and f"import {pkg}.{n};" not in s:
                    add.append(f"import {pkg}.{n};")
                    if pkg == "jdk.m3.io":
                        needs[mod].add("jdk.m3.io")
        if "java.lang.management" in s:
            needs[mod].add("java.management")
        if add:
            s = re.sub(r"^(package [\w.]+;\n)", lambda m: m.group(1) + "\n" + "\n".join(add) + "\n", s, count=1, flags=re.M)
            f.write_text(s, encoding="utf-8", newline="")
            print(f"IMPORTS\t{f.name}\t{len(add)}")
for mod, reqs in needs.items():
    mi = root / mod / "module-info.java"
    s = mi.read_text(encoding="utf-8")
    for r in sorted(reqs):
        if f"requires {r};" not in s and f"requires transitive {r};" not in s:
            s = re.sub(r"(module [\w.]+\s*\{)", rf"\1 requires {r};", s, count=1)
            print(f"REQUIRES\t{mod}\t{r}")
    mi.write_text(s, encoding="utf-8", newline="")
