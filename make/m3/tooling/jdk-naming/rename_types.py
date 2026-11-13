#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""DROP_PRODUCT_PREFIX v1.7: inside an m3 package the package already names the product, so a type carries no M3 prefix.

usage: rename_types.py --root DIR --package P [--package P2 ...] [--check] [--exclude PREFIX ...]
For every top-level type M3<Rest> declared in package P (main or test source, file M3<Rest>.java) the type becomes
<Rest>, its file <Rest>.java, and every reference in the tree follows:
  Java   whole-word M3<Rest> in files of package P and in files importing P.M3<Rest> or P.*; the qualified form
         P.M3<Rest> everywhere, string literals included (reflection, ServiceLoader, logs naming the class);
         other string literals and comments are kept byte for byte;
  C/H    JNI symbols Java_<p>_M3<Rest>_ and javac -h header names <p>_M3<Rest>.h, where <p> is P with '_' for '.';
  shell  qualified names P.M3<Rest> and bare class names in scripts under make/ (launchers name test mains).
  path   v1.7: the binary/resource path form <p>/M3<Rest> (p = P with '/' for '.') in every file outside evidence:
         jar include filters (jdk/m3/x/M3Rest*), launcher source lists, class-file paths;
  docs   v1.7: whole-word M3<Rest> in Markdown files under declared --doc prefixes (module READMEs, live docs).
v1.7: evidence segments count only outside Java package directories (a package jdk.m3.tooling.recipe.agent is
product, a module directory recipe/ or a resources tree before/ is evidence).
v1.4: files under evidence segments anywhere (qualification, recipe, custody, proofs, images, retained variants,
donors, fixtures) are never rewritten; v1.3: a bare name that is a java.* simple name of the running JDK (jimage list) keeps its prefix (KEEP row), the
standard's exception; v1.2: a type token qualified by another package (x.y.M3Rest) is left alone; --string-members also rewrites the
literals "M3<Rest>.member" and "M3<Rest>" of renamed types (catalog entry points), off by default.
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


_JAVA_NAMES = None


def java_simple_names():
    """Simple names of every java.* class of the running JDK (jimage list of lib/modules), cached; empty when the
    image is unreadable (then only the java.lang set above protects)."""
    global _JAVA_NAMES
    if _JAVA_NAMES is None:
        import os, shutil, subprocess
        home = os.environ.get("JAVA_HOME") or (shutil.which("javac") and str(Path(shutil.which("javac")).resolve().parent.parent)) or ""
        names = set()
        try:
            out = subprocess.run([str(Path(home) / "bin" / "jimage"), "list", str(Path(home) / "lib" / "modules")],
                                 capture_output=True, text=True, timeout=120).stdout
            for line in out.splitlines():
                s = line.strip()
                if s.startswith("java/") and s.endswith(".class") and "$" not in s:
                    names.add(s.rsplit("/", 1)[1][:-6])
        except (OSError, subprocess.SubprocessError):
            pass
        _JAVA_NAMES = names
    return _JAVA_NAMES


def masked_sub(text, rx, rep):
    """Apply rx->rep outside string/char literals and comments."""
    out, last = [], 0
    for m in re.finditer(r'"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])\'|//[^\n]*|/\*.*?\*/', text, re.S):
        out.append(rx.sub(rep, text[last:m.start()]))
        out.append(m.group(0))
        last = m.end()
    out.append(rx.sub(rep, text[last:]))
    return "".join(out)


def comment_sub(text, rx, rep):
    """Apply rx->rep inside comments only (v1.7); string/char literals and code are kept."""
    def one(m):
        t = m.group(0)
        return rx.sub(rep, t) if t.startswith("/") else t
    return re.sub(r'"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])\'|//[^\n]*|/\*.*?\*/', one, text, flags=re.S)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", required=True)
    ap.add_argument("--package", action="append", default=[])
    # v1.5: a jtreg-style test directory whose Java files declare no package is one pseudo-package "dir:<path>"
    # (simple names in its own files and in launchers only; workflows name these mains by bare class name).
    ap.add_argument("--test-dir", action="append", default=[])
    ap.add_argument("--exclude", action="append", default=["m3/evidence/", "m3/qualification/"])
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--comment-words", action="store_true",
                    help="v1.7: inside comments of files of the package or importing it, rename every whole-word M3Rest of a renamed type ({@link M3Rest}, @see, prose)")
    # v1.7: Markdown under these prefixes names renamed types by simple name (module READMEs, live docs)
    ap.add_argument("--doc", action="append", default=[])
    ap.add_argument("--string-words", action="store_true",
                    help="v1.6: inside string literals of files of the package or importing it, rename every whole-word M3Rest of a renamed type (class names built at runtime \".M3Rest$Inner\", toString expectations \"M3Rest[...]\")")
    ap.add_argument("--string-members", action="store_true",
                    help="also rewrite the literals M3Rest.member and M3Rest of renamed types in files of the package or importing it (v1.2)")
    a = ap.parse_args()
    root = Path(a.root)
    # v1.4: evidence records anywhere in the tree are history and stay byte for byte: the segments the package gate
    # treats as copies (per-module qualification/recipe directories, custody, proofs, images, retained variants)
    evidence = re.compile(r"/(recipe-custody|custody|evidence|qualification|recipes?|retained-variants|proofs?|snapshots?|"
                          r"before|after|postimages?|preimages?|donors?|intakes?|fixtures?|golden|expected|samples?)/")
    # v1.7: below a Java source root the directories are packages (jdk.m3.tooling.recipe.agent), not evidence
    source_root = re.compile(r"^(.*?/src/(?:main|test)/java/|src/[^/]+/share/classes/|test/jdk/)")

    def outside_packages(rel):
        m = source_root.match(rel)
        return m.group(1) if m else rel

    files = [p for p in root.rglob("*") if p.is_file() and not any(
        p.relative_to(root).as_posix().startswith(x) for x in a.exclude)
        and not evidence.search("/" + outside_packages(p.relative_to(root).as_posix()))]
    java = [p for p in files if p.suffix == ".java"]
    pkg_of = {}
    for p in java:
        m = re.search(r"^package\s+([\w.]+)\s*;", p.read_text(encoding="utf-8", errors="replace"), re.M)
        if m:
            pkg_of[p] = m.group(1)
        elif p.parent.relative_to(root).as_posix() in {d.rstrip("/") for d in a.test_dir}:
            pkg_of[p] = "dir:" + p.parent.relative_to(root).as_posix()
    a.package = a.package + ["dir:" + d.rstrip("/") for d in sorted(a.test_dir)]
    renames = {}  # package -> {old: new}
    for pkg in a.package:
        declared = {p.stem for p, k in pkg_of.items() if k == pkg}
        plan = {}
        for t in sorted(declared):
            if not t.startswith("M3") or t in ("M3",):
                continue
            rest = t[2:]
            if not re.match(r"^[A-Z][A-Za-z0-9_]*$", rest):  # v1.5: donor names such as CityAndFarmHash_1_1
                sys.exit(f"REFUSE {pkg}.{t}: '{rest}' is not UpperCamel")
            if rest in declared:
                sys.exit(f"REFUSE {pkg}.{t}: {rest} is already a type of {pkg}")
            if rest in JAVA_LANG:
                sys.exit(f"REFUSE {pkg}.{t}: {rest} would shadow java.lang.{rest}")
            if rest in java_simple_names():
                # the standard's exception: a bare name that is a java.* simple name keeps its prefix (M3Arrays vs
                # java.util.Arrays); recorded, not refused (v1.3)
                print(f"KEEP	{pkg}.{t}	java.* simple name {rest}")
                continue
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
            pdir = pkg.replace(".", "/")
            pseudo = pkg.startswith("dir:")
            for old, rest in plan.items():
                if pseudo:  # v1.5: default-package test directory
                    if p.suffix == ".java" and pkg_of.get(p) == pkg:
                        new = masked_sub(new, re.compile(rf"(?<![\w$.]){old}(?![\w$])"), rest)
                    elif p.suffix in (".sh", ".gmk", ".yml", ".yaml") or (rel.startswith("make/") and p.suffix == ".txt"):
                        new = re.sub(rf"(?<![\w$]){old}(?![\w$])", rest, new)
                    continue
                q = re.compile(rf"(?<![\w$]){re.escape(pkg)}\.{old}(?![\w$])")
                new = q.sub(f"{pkg}.{rest}", new)  # qualified, everywhere (strings included)
                # v1.7: binary/resource path form, everywhere outside evidence (jar includes, launcher source lists)
                new = re.sub(rf"(?<![\w$]){re.escape(pdir)}/{old}(?![\w$])", f"{pdir}/{rest}", new)
                if p.suffix == ".md" and any(rel.startswith(d) for d in a.doc):
                    # v1.7 declared docs: a simple name not qualified by another package nor a path segment (donor
                    # URLs stay; paths of this tree follow by the path form above)
                    new = re.sub(rf"(?<![\w$./]){old}(?![\w$])", rest, new)
                if p.suffix in (".c", ".h", ".cpp", ".hpp"):
                    new = new.replace(f"Java_{jni}_{old}_", f"Java_{jni}_{rest}_").replace(f"{jni}_{old}.h", f"{jni}_{rest}.h")
                elif p.suffix == ".java":
                    in_pkg = pkg_of.get(p) == pkg
                    # Imports are read from the original text: the qualified rule above has already rewritten them.
                    imports = re.search(rf"^import\s+{re.escape(pkg)}\.({old}|\*)\s*;", text, re.M)
                    if in_pkg or imports:
                        clash = [k for k in other_decl.get(old, ()) if k != pkg and re.search(
                            rf"^import\s+{re.escape(k)}\.({old}|\*)\s*;", text, re.M)]
                        if clash:
                            sys.exit(f"REFUSE {rel}: {old} also imported from {clash}")
                        # v1.2: a token qualified by another package (x.y.M3Rest) is not this package's type
                        new = masked_sub(new, re.compile(rf"(?<![\w$.]){old}(?![\w$])"), rest)
                        if a.string_words:
                            # v1.6 declared opt-in: a renamed type named inside this file's string literals (runtime class
                            # names pkg + ".M3Rest$Inner", record toString expectations "M3Rest[...]") follows the rename
                            word = re.compile(rf"(?<![\w$]){old}(?![\w])")
                            new = STRING.sub(lambda m: word.sub(rest, m.group(0)), new)
                        if a.comment_words:
                            # v1.7 declared opt-in: Javadoc links and prose in this file's comments ({@link M3Rest},
                            # @see M3Rest) name the renamed type; a token qualified by another package stays
                            # (a token after / is a path segment: donor URLs in //FLLDCIM anchors are history)
                            new = comment_sub(new, re.compile(rf"(?<![\w$./]){old}(?![\w$])"), rest)
                        if a.string_members:
                            # declared opt-in: string literals naming a renamed type's member ("M3Rest.member") or the
                            # bare type ("M3Rest") follow the rename (catalog entry points resolved by reflection)
                            new = re.sub(rf'"{old}(\.[A-Za-z_])', rf'"{rest}\1', new)
                            new = re.sub(rf'"{old}"', f'"{rest}"', new)
                elif p.suffix in (".sh", ".gmk", ".yml", ".yaml") or (rel.startswith("make/") and p.suffix == ".txt"):
                    # Launchers and gates name test mains, source files and JNI headers: rename the JNI/header
                    # form, then the bare type name (whole word; a path segment M3X.java becomes X.java).
                    new = new.replace(f"Java_{jni}_{old}_", f"Java_{jni}_{rest}_").replace(f"{jni}_{old}.h", f"{jni}_{rest}.h")
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
