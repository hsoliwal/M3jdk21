#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""DROP_PRODUCT_PREFIX v1: inside an m3 package the package already names the product, so a type carries no M3 prefix.

usage: rename_types.py --root DIR --package P [--package P2 ...] [--check] [--exclude PREFIX ...]
For every top-level type M3<Rest> declared in package P (main or test source, file M3<Rest>.java) the type becomes
<Rest>, its file <Rest>.java, and every reference in the tree follows:
  Java   whole-word M3<Rest> in files of package P and in files importing P.M3<Rest> or P.*; the qualified form
         P.M3<Rest> everywhere, string literals included (reflection, ServiceLoader, logs naming the class);
         other string literals and comments are kept byte for byte;
  C/H    JNI symbols Java_<p>_M3<Rest>_ and javac -h header names <p>_M3<Rest>.h, where <p> is P with '_' for '.';
  shell  qualified names P.M3<Rest> and bare class names in scripts under make/ (launchers name test mains).
Refused (exit 2, nothing written): <Rest> empty or not UpperCamel; <Rest> already a type of P; <Rest> a java.lang
type; a file that references M3<Rest> while importing another package that declares the same simple name.
Evidence records (paths under --exclude prefixes, default m3/evidence/ and m3/qualification/) are history and stay.
Prints one row per renamed type and per changed file; --check writes nothing and exits 1 when anything would change."""
import argparse
import re
import sys
from pathlib import Path

JAVA_LANG = {"Object", "String", "Class", "System", "Math", "StrictMath", "Integer", "Long", "Short", "Byte",
             "Character", "Boolean", "Double", "Float", "Number", "Void", "Thread", "Runtime", "Process",
             "Record", "Enum", "Module", "Package", "Iterable", "Comparable", "Runnable", "Exception", "Error",
             "Throwable", "StringBuilder", "StringBuffer", "CharSequence", "Override", "Deprecated", "ClassLoader",
             "AutoCloseable", "Cloneable", "Appendable", "Readable", "SecurityManager", "StackWalker", "ThreadLocal",
             "ProcessBuilder", "ModuleLayer", "ScopedValue", "StringTemplate", "MatchException"}
STRING = re.compile(r'"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])\'')
COMMENT = re.compile(r"//[^\n]*|/\*.*?\*/", re.S)


def masked_sub(text, rx, rep):
    """Apply rx->rep outside string/char literals and comments."""
    out, last = [], 0
    for m in re.finditer(r'"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])\'|//[^\n]*|/\*.*?\*/', text, re.S):
        out.append(rx.sub(rep, text[last:m.start()]))
        out.append(m.group(0))
        last = m.end()
    out.append(rx.sub(rep, text[last:]))
    return "".join(out)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", required=True)
    ap.add_argument("--package", action="append", required=True)
    ap.add_argument("--exclude", action="append", default=["m3/evidence/", "m3/qualification/"])
    ap.add_argument("--check", action="store_true")
    a = ap.parse_args()
    root = Path(a.root)
    files = [p for p in root.rglob("*") if p.is_file() and not any(
        p.relative_to(root).as_posix().startswith(x) for x in a.exclude)]
    java = [p for p in files if p.suffix == ".java"]
    pkg_of = {}
    for p in java:
        m = re.search(r"^package\s+([\w.]+)\s*;", p.read_text(encoding="utf-8", errors="replace"), re.M)
        if m:
            pkg_of[p] = m.group(1)
    renames = {}  # package -> {old: new}
    for pkg in a.package:
        declared = {p.stem for p, k in pkg_of.items() if k == pkg}
        plan = {}
        for t in sorted(declared):
            if not t.startswith("M3") or t in ("M3",):
                continue
            rest = t[2:]
            if not re.match(r"^[A-Z][A-Za-z0-9]*$", rest):
                sys.exit(f"REFUSE {pkg}.{t}: '{rest}' is not UpperCamel")
            if rest in declared:
                sys.exit(f"REFUSE {pkg}.{t}: {rest} is already a type of {pkg}")
            if rest in JAVA_LANG:
                sys.exit(f"REFUSE {pkg}.{t}: {rest} would shadow java.lang.{rest}")
            plan[t] = rest
        renames[pkg] = plan
    other_decl = {}  # simple name -> packages declaring it (for ambiguity checks)
    for p, k in pkg_of.items():
        other_decl.setdefault(p.stem, set()).add(k)
    changed, moves = {}, []
    for p in files:
        rel = p.relative_to(root).as_posix()
        try:
            text = p.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        new = text
        for pkg, plan in renames.items():
            if not plan:
                continue
            jni = pkg.replace(".", "_")
            for old, rest in plan.items():
                q = re.compile(rf"(?<![\w$]){re.escape(pkg)}\.{old}(?![\w$])")
                new = q.sub(f"{pkg}.{rest}", new)  # qualified, everywhere (strings included)
                if p.suffix in (".c", ".h", ".cpp", ".hpp"):
                    new = new.replace(f"Java_{jni}_{old}_", f"Java_{jni}_{rest}_").replace(f"{jni}_{old}.h", f"{jni}_{rest}.h")
                elif p.suffix == ".java":
                    in_pkg = pkg_of.get(p) == pkg
                    imports = re.search(rf"^import\s+{re.escape(pkg)}\.({old}|\*)\s*;", new, re.M)
                    if in_pkg or imports:
                        clash = [k for k in other_decl.get(old, ()) if k != pkg and re.search(
                            rf"^import\s+{re.escape(k)}\.({old}|\*)\s*;", new, re.M)]
                        if clash:
                            sys.exit(f"REFUSE {rel}: {old} also imported from {clash}")
                        new = masked_sub(new, re.compile(rf"(?<![\w$]){old}(?![\w$])"), rest)
                elif rel.startswith("make/") and p.suffix in (".sh", ".gmk", ".txt"):
                    new = re.sub(rf"(?<![\w$]){old}(?![\w$])", rest, new)
        if new != text:
            changed[p] = new
    for pkg, plan in renames.items():
        for p, k in pkg_of.items():
            if k == pkg and p.stem in plan:
                moves.append((p, p.with_name(plan[p.stem] + ".java")))
    for pkg, plan in renames.items():
        for old, rest in plan.items():
            print(f"TYPE\t{pkg}.{old}\t{pkg}.{rest}")
    for p in sorted(changed):
        print(f"EDIT\t{p.relative_to(root).as_posix()}")
    for src, dst in moves:
        print(f"MOVE\t{src.relative_to(root).as_posix()}\t{dst.relative_to(root).as_posix()}")
    if a.check:
        return 1 if changed or moves else 0
    for p, text in changed.items():
        p.write_bytes(text.encode("utf-8"))
    for src, dst in moves:
        src.rename(dst)
    return 0


if __name__ == "__main__":
    sys.exit(main())
