#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""FOLD_PACKAGE v1: one package of one module joins another package of another module (approved layout 2026-11-08:
families fold into their owner, e.g. jdk.m3.nlp into jdk.m3.search).

usage: fold_package.py --root DIR --from-root A --from-package P --to-root B --to-package Q [--exclude PREFIX ...]
                       [--check]
Every file of P in module A (A/src/{main,test}/java/<P>/X.java and A/src/{main,test}/resources/<P>/...) moves to the
same place under module B and package Q. The module's own files outside those directories (pom, module-info, records)
are not moved; the caller retires the module. References follow for the types T of P and the resources of P only, so
another package sharing the prefix (P.sub) is untouched:
  moved Java files   the package declaration;
  files of Q         imports of Q.X (X in T) are dropped: same package now;
  every live file    the dotted form P.X and the path form P/X (X in T or a resource of P), string literals included,
                     with the module root of a full source path (A/src/main/java/P/X -> B/src/main/java/Q/X);
                     the JNI forms Java_<p>_X_ and <p>_X.h; `import P.*;` becomes `import Q.*;` (dropped in files of Q).
Refused (exit 2, nothing written): P has sub-packages in A; another live root declares P (a split package); a moved
file's destination exists (a type or resource of the same name in Q). Evidence segments above the Java source root
and --exclude prefixes keep their bytes. The M3 prefix is not touched: run rename_types.py on Q afterwards.
--retire-to E [--aggregator POM] first moves the rest of module A (pom, module descriptor, records, recipe/) byte for
byte under the evidence dir E (RETIRE rows) and removes A's <module> line from the reactor pom, so A's root is gone.
Prints MOVE and EDIT rows; --check writes nothing and exits 1 when anything would change (a second run: 0)."""
import argparse
import re
import sys
from pathlib import Path

EVIDENCE = re.compile(r"/(recipe-custody|custody|evidence|qualification|recipes?|retained-variants|proofs?|snapshots?|"
                      r"before|after|postimages?|preimages?|donors?|intakes?|fixtures?|golden|expected|samples?)/")
SOURCE_ROOT = re.compile(r"^(.*?/src/(?:main|test)/(?:java|resources)/|src/[^/]+/share/classes/|test/jdk/)")


def outside_packages(rel):
    """The part of a path the evidence segments are read in: above the source root (below it are packages)."""
    m = SOURCE_ROOT.match(rel)
    return m.group(1) if m else rel


def retire(root, module, evidence, pkg_path, aggregator, check):
    """Every file of the module outside src/{main,test}/java/<P>/ moves byte for byte under the evidence dir (the
    descriptor src/main/java/module-info.java to <evidence>/module-info.java, other src/ files to
    <evidence>/src-remainder/...); the aggregator's <module> line of the module goes. RETIRE rows."""
    keep = tuple(f"src/{k}/java/{pkg_path}/" for k in ("main", "test"))
    base = root / module
    native = [x for x in base.rglob("*") if x.is_file() and x.suffix in (".c", ".h", ".cc", ".cpp", ".hpp")
              and not EVIDENCE.search("/" + x.relative_to(root).as_posix())]
    if native:  # product code, not a record: its move is a decision of its own (move_module_area.py natives)
        sys.exit(f"REFUSE {module}: native sources {native[0].relative_to(root).as_posix()} cannot be retired")
    for p in sorted(x for x in base.rglob("*") if x.is_file()):
        rel = p.relative_to(base).as_posix()
        if rel.startswith(keep):
            continue
        dst = (f"{evidence}/module-info.java" if rel == "src/main/java/module-info.java"
               else f"{evidence}/src-remainder/{rel[4:]}" if rel.startswith("src/") else f"{evidence}/{rel}")
        if (root / dst).exists():
            sys.exit(f"REFUSE {module}/{rel}: {dst} exists")
        print(f"RETIRE\t{module}/{rel}\t{dst}")
        if not check:
            (root / dst).parent.mkdir(parents=True, exist_ok=True)
            p.rename(root / dst)
    if aggregator:
        pom = root / aggregator
        text = pom.read_bytes().decode("utf-8")
        name = Path(module).name
        line = re.compile(rf"^[ \t]*<module>{re.escape(name)}</module>[ \t]*\r?\n", re.M)
        if len(line.findall(text)) == 1:
            print(f"EDIT\t{aggregator}\t<module>{name}</module> removed")
            if not check:
                pom.write_bytes(line.sub("", text).encode("utf-8"))


def main():
    ap = argparse.ArgumentParser()
    for name in ("--root", "--from-root", "--from-package", "--to-root", "--to-package"):
        ap.add_argument(name, required=True)
    ap.add_argument("--exclude", action="append", default=["m3/evidence/", "m3/qualification/"])
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--retire-to", help="evidence dir: the rest of module A (pom, descriptor, records) moves there "
                                        "byte for byte first, and A's root is gone after the fold")
    ap.add_argument("--aggregator", help="reactor pom whose <module> line for A is removed with --retire-to")
    a = ap.parse_args()
    root = Path(a.root)
    src_a, src_b = a.from_root.rstrip("/"), a.to_root.rstrip("/")
    if a.retire_to:
        retire(root, src_a, a.retire_to.rstrip("/"), a.from_package.replace(".", "/"), a.aggregator, a.check)
        a.exclude.append(a.retire_to.rstrip("/") + "/")  # retired records keep their bytes
    p_pkg, q_pkg = a.from_package, a.to_package
    p_path, q_path = p_pkg.replace(".", "/"), q_pkg.replace(".", "/")
    p_jni, q_jni = p_pkg.replace(".", "_"), q_pkg.replace(".", "_")
    files = [p for p in root.rglob("*") if p.is_file()]
    rel = {p: p.relative_to(root).as_posix() for p in files}
    live = [p for p in files if not rel[p].startswith(tuple(a.exclude)) and not EVIDENCE.search("/" + outside_packages(rel[p]))]
    moves, types, resources = [], set(), set()
    for kind in ("main", "test"):
        for lang in ("java", "resources"):
            base = f"{src_a}/src/{kind}/{lang}/{p_path}/"
            for p in files:
                r = rel[p]
                if not r.startswith(base):
                    continue
                tail = r[len(base):]
                if lang == "java" and p.suffix == ".java":
                    if "/" in tail:
                        sys.exit(f"REFUSE {r}: {p_pkg} has a sub-package in {src_a}")
                    types.add(p.stem)
                else:
                    resources.add(tail)
                moves.append((p, root / f"{src_b}/src/{kind}/{lang}/{q_path}/{tail}"))
    if not moves:  # folded already: the fixed point
        print(f"FOLD\t{p_pkg}\t{q_pkg}\tnothing under {src_a}", file=sys.stderr)
        return 0
    for p in live:
        if p.suffix == ".java" and not rel[p].startswith(src_a + "/") and re.search(
                rf"^package\s+{re.escape(p_pkg)}\s*;", p.read_text(encoding="utf-8", errors="replace"), re.M):
            sys.exit(f"REFUSE {rel[p]}: {p_pkg} is also declared outside {src_a} (split package)")
    for src, dst in moves:
        if dst.exists():
            sys.exit(f"REFUSE {rel[src]}: {dst.relative_to(root).as_posix()} exists")
    names = "|".join(sorted((re.escape(t) for t in types), key=len, reverse=True))
    res = "|".join(sorted((re.escape(r) for r in resources), key=len, reverse=True))
    dotted = re.compile(rf"(?<![\w$]){re.escape(p_pkg)}\.({names})(?![\w$])")
    path_form = re.compile(rf"(?<![\w$]){re.escape(p_path)}/({names}|{res})(?![\w$])" if res
                           else rf"(?<![\w$]){re.escape(p_path)}/({names})(?![\w$])")
    # a full source path names the module too: A/src/<kind>/<lang>/P/X -> B/src/<kind>/<lang>/Q/X (before path_form)
    full_path = re.compile(rf"(?<![\w$.-]){re.escape(src_a)}/src/(main|test)/(java|resources)/{re.escape(p_path)}/"
                           rf"({names}|{res})(?![\w$])" if res else
                           rf"(?<![\w$.-]){re.escape(src_a)}/src/(main|test)/(java|resources)/{re.escape(p_path)}/"
                           rf"({names})(?![\w$])")
    jni = re.compile(rf"Java_{re.escape(p_jni)}_({names})_")
    header = re.compile(rf"(?<![\w$]){re.escape(p_jni)}_({names})\.h\b")
    wildcard = re.compile(rf"^import\s+{re.escape(p_pkg)}\.\*\s*;[ \t]*\r?\n", re.M)
    same_pkg = re.compile(rf"^import\s+{re.escape(q_pkg)}\.({names})\s*;[ \t]*\r?\n", re.M)
    moved = {src for src, _ in moves}
    changed = {}
    for p in live:
        try:
            data = p.read_bytes()
            text = data.decode("utf-8")
        except (UnicodeDecodeError, OSError):
            continue
        new = dotted.sub(lambda m: f"{q_pkg}.{m.group(1)}", text)
        new = full_path.sub(lambda m: f"{src_b}/src/{m.group(1)}/{m.group(2)}/{q_path}/{m.group(3)}", new)
        new = path_form.sub(lambda m: f"{q_path}/{m.group(1)}", new)
        new = jni.sub(lambda m: f"Java_{q_jni}_{m.group(1)}_", new)
        new = header.sub(lambda m: f"{q_jni}_{m.group(1)}.h", new)
        if p.suffix == ".java":
            in_q = p in moved or re.search(rf"^package\s+{re.escape(q_pkg)}\s*;", text, re.M)
            if p in moved:
                new = re.sub(rf"^package\s+{re.escape(p_pkg)}\s*;", f"package {q_pkg};", new, count=1, flags=re.M)
            new = wildcard.sub("" if in_q else f"import {q_pkg}.*;\n", new)
            if in_q:
                new = same_pkg.sub("", new)
        if new != text:
            changed[p] = new
    for src, dst in moves:
        print(f"MOVE\t{rel[src]}\t{dst.relative_to(root).as_posix()}")
    for p in sorted(changed, key=lambda x: rel[x]):
        print(f"EDIT\t{rel[p]}")
    print(f"FOLD\t{p_pkg}\t{q_pkg}\ttypes={len(types)}\tresources={len(resources)}\tedits={len(changed)}", file=sys.stderr)
    if a.check:
        return 1 if changed or moves else 0
    for p, text in changed.items():
        p.write_bytes(text.encode("utf-8"))
    for src, dst in moves:
        dst.parent.mkdir(parents=True, exist_ok=True)
        src.rename(dst)
    base = root / src_a
    if base.is_dir():  # empty directories of A, deepest first (A itself too once nothing is left)
        for d in sorted((x for x in base.rglob("*") if x.is_dir()), key=lambda x: -len(x.parts)) + [base]:
            if not any(d.iterdir()):
                d.rmdir()
    return 0


if __name__ == "__main__":
    sys.exit(main())
